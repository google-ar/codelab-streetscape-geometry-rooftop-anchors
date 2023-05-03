/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.codelabs.streetscapegeometry

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.StreetscapeGeometry
import com.google.ar.core.TrackingState
import com.google.ar.core.codelabs.streetscapegeometry.helpers.BalloonRenderer
import com.google.ar.core.codelabs.streetscapegeometry.helpers.ObjectPlacementHelper
import com.google.ar.core.codelabs.streetscapegeometry.helpers.StarRenderer
import com.google.ar.core.codelabs.streetscapegeometry.helpers.StreetscapeGeometryRenderer
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper
import com.google.ar.core.examples.java.common.samplerender.Framebuffer
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer
import com.google.ar.core.exceptions.CameraNotAvailableException
import java.io.IOException


class StreetscapeCodelabRenderer(val activity: StreetscapeGeometryActivity) :
  SampleRender.Renderer, DefaultLifecycleObserver {
  //<editor-fold desc="ARCore initialization" defaultstate="collapsed">
  companion object {
    val TAG = "StreetscapeCodelabRenderer"

    private val Z_NEAR = 0.1f
    private val Z_FAR = 1000f
  }

  lateinit var backgroundRenderer: BackgroundRenderer
  lateinit var virtualSceneFramebuffer: Framebuffer
  var hasSetTextureNames = false

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  val modelMatrix = FloatArray(16)
  val viewMatrix = FloatArray(16)
  val projectionMatrix = FloatArray(16)
  val modelViewMatrix = FloatArray(16) // view x model
  val modelViewProjectionMatrix = FloatArray(16) // projection x view x model

  val center = floatArrayOf(0.5f, 0.5f)
  val centerCoords = floatArrayOf(0.0f, 0.0f)

  val session
    get() = activity.arCoreSessionHelper.session

  val displayRotationHelper = DisplayRotationHelper(activity)
  val trackingStateHelper = TrackingStateHelper(activity)

  val streetscapeGeometryRenderer = StreetscapeGeometryRenderer(activity)
  var showStreetscapeGeometry = false

  enum class PlacementMode { STAR, BALLOON }

  var placementMode = PlacementMode.STAR
  val balloonRenderer = BalloonRenderer(activity)
  val starRenderer = StarRenderer(activity)

  override fun onResume(owner: LifecycleOwner) {
    displayRotationHelper.onResume()
    hasSetTextureNames = false
  }

  override fun onPause(owner: LifecycleOwner) {
    displayRotationHelper.onPause()
  }

  override fun onSurfaceCreated(render: SampleRender) {
    // Prepare the rendering objects.
    // This involves reading shaders and 3D model files, so may throw an IOException.
    try {
      backgroundRenderer = BackgroundRenderer(render)
      virtualSceneFramebuffer = Framebuffer(render, /*width=*/ 1, /*height=*/ 1)

      backgroundRenderer.setUseDepthVisualization(render, false)
      backgroundRenderer.setUseOcclusion(render, false)

      streetscapeGeometryRenderer.init(render)
      starRenderer.init(render)
      balloonRenderer.init(render)
    } catch (e: IOException) {
      Log.e(TAG, "Failed to read a required asset file", e)
      showError("Failed to read a required asset file: $e")
    }
  }

  override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
    displayRotationHelper.onSurfaceChanged(width, height)
    virtualSceneFramebuffer.resize(width, height)
  }
  //</editor-fold>

  override fun onDrawFrame(render: SampleRender) {
    val session = session ?: return

    //<editor-fold desc="ARCore frame boilerplate" defaultstate="collapsed">
    // Texture names should only be set once on a GL thread unless they change. This is done during
    // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
    // initialized during the execution of onSurfaceCreated.
    if (!hasSetTextureNames) {
      session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
      hasSetTextureNames = true
    }

    // -- Update per-frame state

    // Notify ARCore session that the view size changed so that the perspective matrix and
    // the video background can be properly adjusted.
    displayRotationHelper.updateSessionIfNeeded(session)

    // Obtain the current frame from ARSession. When the configuration is set to
    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
    // camera framerate.
    val frame =
      try {
        session.update()
      } catch (e: CameraNotAvailableException) {
        Log.e(TAG, "Camera not available during onDrawFrame", e)
        showError("Camera not available. Try restarting the app.")
        return
      }

    val camera = frame.camera

    // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
    // used to draw the background camera image.
    backgroundRenderer.updateDisplayGeometry(frame)

    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
    trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

    // -- Draw background
    if (frame.timestamp != 0L) {
      // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
      // drawing possible leftover data from previous sessions if the texture is reused.
      backgroundRenderer.drawBackground(render)
    }

    // If not tracking, don't draw 3D objects.
    if (camera.trackingState == TrackingState.PAUSED) {
      return
    }

    // Get projection matrix.
    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)

    // Get camera matrix and draw.
    camera.getViewMatrix(viewMatrix, 0)

    frame.transformCoordinates2d(
      Coordinates2d.VIEW_NORMALIZED,
      center,
      Coordinates2d.VIEW,
      centerCoords
    )
    if (activity.view.tapHelper.poll() != null) {
      onTap(frame)
    }

    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
    //</editor-fold>

    if (showStreetscapeGeometry) {
      // TODO: Obtain Streetscape Geometry.

    }

    //<editor-fold desc="Render stars and balloons" defaultstate="collapsed">
    for (anchor in starAnchors) {
      starRenderer.render(render, anchor)
    }

    for (anchor in balloonAnchors) {
      balloonRenderer.render(render, anchor)
    }
    //</editor-fold>

    //<editor-fold desc="Localization info and Geometry info" defaultstate="collapsed">
    val centerHits = frame.hitTest(centerCoords[0], centerCoords[1])
    val centerGeometryHit = centerHits.firstOrNull { it.trackable is StreetscapeGeometry }
    // Render Streetscape geometry info
    if (centerGeometryHit == null) {
      activity.view.updateStreetscapeGeometryStatusTextNone()
    } else {
      activity.view.updateStreetscapeGeometryStatusText(
        centerGeometryHit.trackable as StreetscapeGeometry,
        centerGeometryHit.distance
      )
    }

    val earth = session.earth
    if (earth?.trackingState == TrackingState.TRACKING) {
      val cameraGeospatialPose = earth.cameraGeospatialPose
      activity.view.updateEarthStatusText(earth, cameraGeospatialPose)
    }

    // Compose the virtual scene with the background.
    backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
    //</editor-fold>
  }

  val starAnchors = mutableListOf<Anchor>()
  val balloonAnchors = mutableListOf<Anchor>()
  private fun onTap(frame: Frame) {
    // TODO: determine the Streetscape Geometry at the center of the viewport

    if (placementMode == PlacementMode.STAR) {
      // TODO: Create an anchor for a star and add it to `starAnchors`

    } else if (placementMode == PlacementMode.BALLOON) {
      // TODO: Create an anchor for a balloon and add it to `balloonAnchors`

    }
  }

  private fun showError(errorMessage: String) =
    activity.view.snackbarHelper.showError(activity, errorMessage)
}

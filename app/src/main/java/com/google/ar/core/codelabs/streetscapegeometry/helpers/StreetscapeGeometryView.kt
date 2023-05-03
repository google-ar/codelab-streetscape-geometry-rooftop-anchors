/*
 * Copyright 2022 Google LLC
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
package com.google.ar.core.codelabs.streetscapegeometry.helpers

import android.app.AlertDialog
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.Earth
import com.google.ar.core.GeospatialPose
import com.google.ar.core.StreetscapeGeometry
import com.google.ar.core.codelabs.streetscapegeometry.R
import com.google.ar.core.codelabs.streetscapegeometry.StreetscapeCodelabRenderer
import com.google.ar.core.codelabs.streetscapegeometry.StreetscapeGeometryActivity
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper
import com.google.ar.core.examples.java.common.helpers.TapHelper


/** Contains UI elements. */
class StreetscapeGeometryView(val activity: StreetscapeGeometryActivity) :
  DefaultLifecycleObserver {
  val root = View.inflate(activity, R.layout.activity_main, null)
  val surfaceView = root.findViewById<GLSurfaceView>(R.id.surfaceview)
  val tapHelper = TapHelper(activity).also {
    surfaceView.setOnTouchListener(it)
  }

  val session
    get() = activity.arCoreSessionHelper.session

  val snackbarHelper = SnackbarHelper()

  val settings = root.findViewById<ImageButton>(R.id.settingsButton).apply {
    setOnClickListener {
      val popup = PopupMenu(activity, it)
      popup.setOnMenuItemClickListener { item ->
        when (item.itemId) {
          R.id.placement_settings -> {
            var selected = activity.renderer.placementMode
            AlertDialog.Builder(activity)
              .setTitle(R.string.settings)
              .setSingleChoiceItems(
                activity.resources.getStringArray(R.array.placementObjectOptions),
                activity.renderer.placementMode.ordinal
              ) { _, which ->
                selected = StreetscapeCodelabRenderer.PlacementMode.values()[which]
              }.setPositiveButton(R.string.done) { _, _ ->
                activity.renderer.placementMode = selected
              }
              .setNegativeButton(R.string.cancel) { _, _ -> }
              .show()
            true
          }

          R.id.visualization_settings -> {
            val options = booleanArrayOf(activity.renderer.showStreetscapeGeometry)
            AlertDialog.Builder(activity)
              .setTitle(R.string.settings)
              .setMultiChoiceItems(
                activity.resources.getStringArray(R.array.showStreetscapeGeometryOptions),
                options
              ) { _, which, isChecked ->
                options[which] = isChecked
              }.setPositiveButton(R.string.done) { _, _ ->
                activity.renderer.showStreetscapeGeometry = options[0]
              }
              .setNegativeButton(R.string.cancel) { _, _ -> }
              .show()
            true
          }

          else -> {
            false
          }
        }
      }
      popup.inflate(R.menu.settings_menu)
      popup.show()
    }
  }
  val earthStatusText = root.findViewById<TextView>(R.id.earthStatusText)
  fun updateEarthStatusText(earth: Earth, cameraGeospatialPose: GeospatialPose?) {
    activity.runOnUiThread {
      val poseText = if (cameraGeospatialPose == null) "" else
        activity.getString(
          R.string.geospatial_pose,
          cameraGeospatialPose.latitude,
          cameraGeospatialPose.longitude,
          cameraGeospatialPose.horizontalAccuracy,
          cameraGeospatialPose.altitude,
          cameraGeospatialPose.verticalAccuracy,
          cameraGeospatialPose.heading,
          cameraGeospatialPose.headingAccuracy
        )
      earthStatusText.text = activity.resources.getString(
        R.string.earth_state,
        earth.earthState.toString(),
        earth.trackingState.toString(),
        poseText
      )
    }
  }

  val streetscapeGeometryStatusText =
    root.findViewById<TextView>(R.id.streetscapeGeometryStatusText)

  fun updateStreetscapeGeometryStatusText(
    streetscapeGeometry: StreetscapeGeometry,
    distanceMeters: Float,
  ) {
    activity.runOnUiThread {
      val text = activity.getString(
        R.string.streetscape_info,
        streetscapeGeometry.type.toString(),
        streetscapeGeometry.quality.toString(),
        distanceMeters
      )
      streetscapeGeometryStatusText.text = text
    }
  }

  fun updateStreetscapeGeometryStatusTextNone() {
    activity.runOnUiThread {
      streetscapeGeometryStatusText.setText(R.string.streetscape_info_none)
    }
  }

  override fun onResume(owner: LifecycleOwner) {
    surfaceView.onResume()
  }

  override fun onPause(owner: LifecycleOwner) {
    surfaceView.onPause()
  }
}

/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.codelabs.streetscapegeometry.helpers

import android.opengl.Matrix
import com.google.ar.core.Anchor
import com.google.ar.core.codelabs.streetscapegeometry.StreetscapeGeometryActivity
import com.google.ar.core.examples.java.common.samplerender.Mesh
import com.google.ar.core.examples.java.common.samplerender.SampleRender
import com.google.ar.core.examples.java.common.samplerender.Shader
import com.google.ar.core.examples.java.common.samplerender.Texture

class BalloonRenderer(val activity: StreetscapeGeometryActivity) {
  lateinit var virtualObjectMesh: Mesh
  lateinit var virtualObjectShader: Shader
  lateinit var virtualObjectTexture: Texture

  fun init(render: SampleRender) {
    // Virtual object to render (Geospatial Marker)
    virtualObjectTexture =
      Texture.createFromAsset(
        render,
        "models/balloon.png",
        Texture.WrapMode.CLAMP_TO_EDGE,
        Texture.ColorFormat.SRGB
      )

    virtualObjectMesh = Mesh.createFromAsset(render, "models/balloon.obj");
    virtualObjectShader =
      Shader.createFromAssets(
        render,
        "shaders/ar_unlit_object.vert",
        "shaders/ar_unlit_object.frag",
        /*defines=*/ null
      )
        .setTexture("u_Texture", virtualObjectTexture)
  }

  fun render(render: SampleRender, anchor: Anchor) {
    // Get the current pose of the Anchor in world space. The Anchor pose is updated
    // during calls to session.update() as ARCore refines its estimate of the world.
    anchor.pose.toMatrix(activity.renderer.modelMatrix, 0)

    // Calculate model/view/projection matrices
    Matrix.multiplyMM(
      activity.renderer.modelViewMatrix,
      0,
      activity.renderer.viewMatrix,
      0,
      activity.renderer.modelMatrix,
      0
    )
    Matrix.multiplyMM(
      activity.renderer.modelViewProjectionMatrix,
      0,
      activity.renderer.projectionMatrix,
      0,
      activity.renderer.modelViewMatrix,
      0
    )

    // Update shader properties and draw
    virtualObjectShader.setMat4(
      "u_ModelViewProjection",
      activity.renderer.modelViewProjectionMatrix
    )
    render.draw(virtualObjectMesh, virtualObjectShader, activity.renderer.virtualSceneFramebuffer)
  }
}
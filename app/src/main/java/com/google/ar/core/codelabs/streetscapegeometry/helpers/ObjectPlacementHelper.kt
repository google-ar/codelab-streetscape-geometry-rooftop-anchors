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

import com.google.ar.core.Pose
import javax.vecmath.Vector3f

class ObjectPlacementHelper {
  companion object {
    /**
     * Creates a pose for where the star should be placed. It should have its normal following the
     * input pose's normal.
     */
    fun createStarPose(pose: Pose): Pose {
      return pose
    }

    /**
     * Creates a pose for a balloon. Extend the pose in the direction of the hit point to ensure the
     * balloon is on the top of the building, and not on its edge.
     */
    fun createBalloonPose(cameraPose: Pose, hitPose: Pose): Pose {
      val normalizedLookDirection = Vector3f(hitPose.translation)
      normalizedLookDirection.sub(Vector3f(cameraPose.translation))
      normalizedLookDirection.normalize()
      normalizedLookDirection.scale(5.0f)

      val destination = Vector3f(hitPose.translation)
      destination.add(normalizedLookDirection)

      return Pose(
        floatArrayOf(
          destination.x,
          destination.y,
          destination.z,
        ),
        floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
      )
    }
  }
}
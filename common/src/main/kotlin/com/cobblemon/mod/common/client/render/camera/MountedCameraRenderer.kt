/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.camera

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.orientation.OrientationController
import com.cobblemon.mod.common.api.orientation.OrientationController.Companion.FORWARDS
import com.cobblemon.mod.common.api.orientation.OrientationController.Companion.LEFT
import com.cobblemon.mod.common.api.orientation.OrientationController.Companion.UP
import com.cobblemon.mod.common.client.MountedPokemonAnimationRenderController
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository.getPoser
import com.cobblemon.mod.common.duck.CameraDuck
import com.cobblemon.mod.common.duck.RidePassenger
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.math.geometry.toRadians
import com.mojang.blaze3d.Blaze3D
import net.minecraft.client.Camera
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

object MountedCameraRenderer {
    private var returnTimer: Float = 0f
    private var lastHandledRotationTime: Double = Double.MIN_VALUE
    private var frameTime: Double = 0.0
    private var rollAngleStart: Float = 0f

    var smoothRotation: Quaternionf? = null

    fun getCameraPosition(
        instance: Camera,
        driver: Entity,
        vehicle: Entity,
        thirdPersonReverse: Boolean,
        eyeHeight: Double,
        eyeHeightOld: Double
    ): Vec3? {
        if (vehicle !is PokemonEntity) {
            return null
        }
        val delegate = vehicle.delegate as? PokemonClientDelegate ?: return null

        val entityPos = Vec3(
            Mth.lerp(instance.partialTickTime.toDouble(), vehicle.xOld, vehicle.x),
            Mth.lerp(instance.partialTickTime.toDouble(), vehicle.yOld, vehicle.y),
            Mth.lerp(instance.partialTickTime.toDouble(), vehicle.zOld, vehicle.z)
        )

        // Sets up the pokemon's locators so when the camera and pokemon are calculated for rendering they use the same partialTickTime.
        MountedPokemonAnimationRenderController.setup(vehicle, instance.partialTickTime)
        val rollable = vehicle as OrientationControllable
        val vehicleController = rollable.getOrientationController()

        val model = getPoser(vehicle.pokemon.species.resourceIdentifier, FloatingState())

        val locatorName = delegate.getSeatLocator(driver)
        val locator = delegate.locatorStates[locatorName] ?: return null

        // Get additional offset from poser and add to the eyeHeight offset
        val currEyeHeight: Double = Mth.lerp(instance.partialTickTime.toDouble(), eyeHeight, eyeHeightOld)
        var offset = Vec3(0.0, currEyeHeight - (driver.bbHeight / 2), 0.0)
        var eyeOffset = Vec3(0.0, currEyeHeight - (driver.bbHeight / 2), 0.0)

        val isFirstPerson = Minecraft.getInstance().options.cameraType == CameraType.FIRST_PERSON

        eyeOffset = eyeOffset.add(getFirstPersonOffset(model, locatorName))
        // Get the offset based on first person, third person, or third person with view bobbing enabled from the posers.
        offset = offset.add(
            if (isFirstPerson) {
                getFirstPersonOffset(model, locatorName)
            } else /* if (Cobblemon.config.thirdPersonViewBobbing) */ {
                getThirdPersonOffset(thirdPersonReverse, model.thirdPersonCameraOffset, locatorName)
            } /* else {
                getThirdPersonOffset(thirdPersonReverse, model.thirdPersonCameraOffsetNoViewBobbing, locatorName)
            } */
        )

        val rotation =
            if (vehicleController.isActive()) vehicleController.getRenderOrientation(instance.partialTickTime)
            else Quaternionf()
                .rotateY((Math.PI.toFloat() - Mth.lerp(instance.partialTickTime, vehicle.yRotO, vehicle.yRot).toRadians()))

        offset = rotation.transform(Vector3d(offset.x, offset.y, offset.z)).let { Vec3(it.x, it.y, it.z) }
        eyeOffset = rotation.transform(Vector3d(eyeOffset.x, eyeOffset.y, eyeOffset.z)).let { Vec3(it.x, it.y, it.z) }

        val eyeLocatorOffset = Vec3(locator.matrix.getTranslation(Vector3f()))
        val eyePos = eyeLocatorOffset.add(entityPos)

        // Get the camera position before offsets are applied (which are done with maxZoom to account for clipping). If 3rd person viewbobbing is enabled or the player is
        // in first person then base the camera position off the seat locator offset
        val pos = /* if (isFirstPerson || Cobblemon.config.thirdPersonViewBobbing)  { */
                Vec3(locator.matrix.getTranslation(Vector3f()))
                    .add(entityPos)
        /* } else {
            val pokemonCenter = Vector3f(0f, driver.bbHeight/2, 0f)

            val pivotOffsets = model.thirdPersonPivotOffset
            val pivot = Vector3f(pokemonCenter)
            val locatorName = delegate.getSeatLocator(driver)
            if (pivotOffsets.containsKey(locatorName)) {
                pivot.add(rotation.transform(pivotOffsets[locatorName]!!.toVector3f()))
            }
            pivot.add(entityPos.toVector3f())
        } */

        val offsetDistance = offset.length()
        val offsetDirection = offset.scale(1 / offsetDistance)

        val eyeOffsetDistance = eyeOffset.length()
        val eyeOffsetDirection = eyeOffset.scale(1 / eyeOffsetDistance)

        // Use getMaxZoom to calculate clipping
        val maxZoom: Double = getMaxZoom(offsetDistance, offsetDirection, pos, instance)
        val eyeMaxZoom: Double = getMaxZoom(eyeOffsetDistance, eyeOffsetDirection, eyePos, instance)

        val playerRotater = driver as RidePassenger
        playerRotater.`cobblemon$setRideEyePos`(eyeOffsetDirection.scale(eyeMaxZoom).add(eyePos))
        return offsetDirection.scale(maxZoom).add(pos)
    }

    // Return value is false if the vanilla camera rotations should be applied.
    fun setRotation(instance: Camera): Boolean {
        val cameraDuck = instance as CameraDuck

        // Calculate the current frametime
        val d = Blaze3D.getTime()
        frameTime = if (lastHandledRotationTime != Double.Companion.MIN_VALUE) d - lastHandledRotationTime else 0.0
        lastHandledRotationTime = d

        // Don't assume the camera to be attached to an entity. Ponder Scenes from create et.al. for example aren't.
        val vehicle: Entity? = cameraDuck.`cobblemon$getEntity`()?.vehicle ?: return false
        if (vehicle !is OrientationControllable) return false

        // If the current vehicle has no orientation then return and perform
        // vanilla camera rotations
        val vehicleController = vehicle.getOrientationController()
        if (vehicleController.orientation == null) return false

        // If the controller has been deactivated but the orientation isn't null yet
        // then perform transition from rolled ride/camera to vanilla
        if (!vehicleController.isActive() && vehicleController.orientation != null) {
            return applyTransitionRotation(vehicleController, instance)
        } else { // Otherwise perform cobblemon specific camera rotation and set transition helper variables
            applyCameraRotation(instance)
            returnTimer = 0f
            rollAngleStart = instance.rotation().getEulerAnglesYXZ(Vector3f()).z()
            return true
        }
    }

    fun applyCameraRotation(instance: Camera) {
        val cameraDuck = instance as CameraDuck

        val driver = cameraDuck.`cobblemon$getEntity`() ?: return
        val vehicle = driver.vehicle ?: return
        val controllableVehicle = vehicle as? OrientationControllable ?: return
        if (vehicle.getOrientationController().orientation == null || !controllableVehicle.getOrientationController().active) return

        // Grab the driver and vehicle orientation controllers.
        val vehicleController: OrientationController = controllableVehicle.getOrientationController()

        val isDriving = driver.controlledVehicle != null


        // Grab the ride orientation
        // If the player is driving then use the current orientation since the driver sets it.
        // If not then use the smoothed render orientation for passengers otherwise the vehicle rotation will be jerky
        var newRotation = if (isDriving) {
             vehicleController.orientation!!.normal(Matrix3f()).getNormalizedRotation(Quaternionf())
        } else {
            vehicleController.getRenderOrientation(instance.partialTickTime)
        }

        // Unroll the current rotation if no roll is requested and not currently freelooking
        if (Cobblemon.config.disableRoll) {
            // Init the smooth rotation if it has not been set yet.
            if (smoothRotation == null) {
                smoothRotation = Quaternionf().set(instance.rotation())
            }
            val rideAngs = newRotation.getEulerAnglesYXZ(Vector3f())
            val cameraAngs = smoothRotation!!.getEulerAnglesYXZ(Vector3f())

            val degDiff = Mth.degreesDifference(
                Math.toDegrees(cameraAngs.y().toDouble()).toFloat(),
                Math.toDegrees(rideAngs.y().toDouble()).toFloat()
            )

            val pitchDeadzoneDeg = 15.0 // x degrees of deadzone about +-90 degrees of pitch.
            val pitchDeg = vehicleController.pitch
            // ConsiderPitch to be already 90 if within the deadzone around it.
            val cutoffPitch = min(1.0, (Mth.abs(pitchDeg) / (90.0f - pitchDeadzoneDeg))) * 90.0f * sign(pitchDeg)
            val lerpRateMod = Mth.cos(Math.toRadians(cutoffPitch).toFloat())

            // Apply smoothing from the cameras current yaw to the yaw of the velocity
            // vector of the ride

            // Some smoothing constant k. 0.0 when game is paused to prevent lerping when game isn't running.
            val k = if(Minecraft.getInstance().isPaused) 0.0f else 10.0f
            val smoothingFactor: Double = lerpRateMod * frameTime * k
            val newYaw = cameraAngs.y() + (Math.toRadians(degDiff.toDouble()) * smoothingFactor)

            val newPitch = Mth.lerp(
                frameTime * k,
                cameraAngs.x().toDouble(),
                rideAngs.x().toDouble()
            )

            //var newYaw = (float)Mth.lerp(smoothingFactor, cameraAngs.y(), rideAngs.y());
            val maxRoll = Math.toRadians(0.0) // idk wat possessed me to give this roll as a 'no roll camera'
            val newRoll = Mth.lerp(
                frameTime * k / 2.0,
                cameraAngs.z().toDouble(),
                0.0
            ).toFloat()

            // Set rotations
            val smoothedRot = Quaternionf().rotateYXZ(newYaw.toFloat(), newPitch.toFloat(), newRoll)
            smoothRotation?.set(smoothedRot)
            newRotation.set(smoothedRot)

        }

        var rotationOffset = 0
        if (Minecraft.getInstance().options.cameraType.isMirrored) {
            newRotation.rotateY(Math.toRadians(180.0).toFloat())
            rotationOffset = 180
        }


        // Get the drivers local look angle in relation to the ride and apply it.
        if (vehicleController.active) {
            val vehicleMatrix = Matrix3f().set(newRotation)
            val driverMatrix = Matrix3f() // Base the driver
            val playerRotater = driver as RidePassenger
            newRotation = vehicleMatrix.mul(
                driverMatrix.rotateYXZ(
                    -Mth.DEG_TO_RAD * (playerRotater.`cobblemon$getRideYRot`()),
                    -Mth.DEG_TO_RAD * playerRotater.`cobblemon$getRideXRot`(),
                    0.0f
                )
            ).normal(Matrix3f()).getNormalizedRotation(Quaternionf())
        }
        val eulerAngs = newRotation.getEulerAnglesYXZ(Vector3f())
        setCameraRotations(newRotation, instance, cameraDuck)

        // Set the drivers rotations to match
        driver.xRot = -eulerAngs.x() * Mth.RAD_TO_DEG + rotationOffset
        driver.yRot = 180.0f - (eulerAngs.y() * Mth.RAD_TO_DEG)
    }

    private fun applyTransitionRotation(
        vehicleController: OrientationController,
        instance: Camera
    ): Boolean {
        val cameraDuck = instance as CameraDuck
        val entity = cameraDuck.`cobblemon$getEntity`()!!
        // If the transition has just started then
        if (returnTimer == 0f) {
            resetDriverRotations(instance, entity)
        }

        if (returnTimer < 1f) {
            // Rotation is taken from entity since we no longer handle mouse ourselves
            // Stops a period of time when you can't input anything.
            val interpolatedRoll = Mth.lerp(returnTimer, rollAngleStart, 0.0f)
            val pitch = Math.toRadians(-entity.xRot.toDouble()).toFloat()
            val yaw = Math.toRadians((180 - entity.yRot).toDouble()).toFloat()

            val interRot = Quaternionf()
            interRot.rotationYXZ(yaw, pitch, interpolatedRoll)

            setCameraRotations(interRot, instance, cameraDuck)

            if (rollAngleStart == 0f) {
                rollAngleStart = 1f
                vehicleController.reset()
                resetDriverRotations(instance, entity)
                return false
            }
            returnTimer += instance.partialTickTime * (1.0f / 20.0f)
            return true
        } else {
            returnTimer = 1f
            vehicleController.reset()
            resetDriverRotations(instance, entity)
            return false
        }
    }

    fun resetDriverRotations(
        instance: Camera,
        driver: Entity,
    ) {
        val eulerAngs = Quaternionf(instance.rotation()).getEulerAnglesYXZ(Vector3f())
        val playerRotater = driver as RidePassenger
        // Backticks because kotlin explodes otherwise due to syntax
        playerRotater.`cobblemon$setRideXRot`(-eulerAngs.x() * Mth.RAD_TO_DEG)
        playerRotater.`cobblemon$setRideYRot`(180.0f - (eulerAngs.y() * Mth.RAD_TO_DEG))
    }


    private fun getThirdPersonOffset(
        thirdPersonReverse: Boolean,
        cameraOffsets: Map<String, Vec3>,
        locatorName: String,
    ): Vec3 {
        val offset = if (thirdPersonReverse && cameraOffsets.containsKey(locatorName + "_reverse")) {
            cameraOffsets[locatorName + "_reverse"]!!
        } else if (thirdPersonReverse && cameraOffsets.containsKey("seat_default_reverse")) {
            cameraOffsets[locatorName + "seat_default_reverse"]!!
        } else if (cameraOffsets.containsKey(locatorName)) {
            cameraOffsets[locatorName]!!
        } else if (cameraOffsets.containsKey("seat_default")) {
            cameraOffsets["seat_default"]!!
        } else {
            Vec3.ZERO
        }

        return if (thirdPersonReverse) offset.multiply(1.0, 1.0, -1.0) else offset
    }

    private fun getFirstPersonOffset(model: PosableModel, locatorName: String): Vec3 {
        val cameraOffsets = model.firstPersonCameraOffset

        return if (cameraOffsets.containsKey(locatorName)) {
            cameraOffsets[locatorName]!!
        } else if (cameraOffsets.containsKey("seat_default")) {
            cameraOffsets["seat_default"]!!
        } else {
            Vec3.ZERO
        }
    }


    private fun getMaxZoom(maxZoom: Double, directionVector: Vec3, positionVector: Vec3, instance: Camera): Double {
        var maxZoom = maxZoom
        for (i in 0 .. 7) {
            val g = ((i and 1) * 2 - 1).toFloat()
            val h = ((i shr 1 and 1) * 2 - 1).toFloat()
            val j = ((i shr 2 and 1) * 2 - 1).toFloat()
            val vec3 = positionVector.add((g * 0.1f).toDouble(), (h * 0.1f).toDouble(), (j * 0.1f).toDouble())
            val vec32 = vec3.add(directionVector.scale(maxZoom))
            val level = (instance as CameraDuck).`cobblemon$getLevel`()
            val hitResult: HitResult = level.clip(
                ClipContext(
                    vec3,
                    vec32,
                    ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE,
                    instance.entity
                )
            )
            if (hitResult.type != HitResult.Type.MISS) {
                val k = hitResult.getLocation().distanceToSqr(positionVector)
                if (k < Mth.square(maxZoom)) {
                    maxZoom = sqrt(k)
                }
            }
        }

        return maxZoom
    }

    private fun setCameraRotations(rotation: Quaternionf, instance: Camera, duck: CameraDuck) {
        val eulerAngs = rotation.getEulerAnglesYXZ(Vector3f())
        val cameraRotation = instance.rotation()
        cameraRotation.rotationYXZ(eulerAngs.y, eulerAngs.x, eulerAngs.z)
        duck.`cobblemon$setXRot`(eulerAngs.x() * Mth.RAD_TO_DEG)
        duck.`cobblemon$setYRot`(eulerAngs.y() * Mth.RAD_TO_DEG)

        FORWARDS.rotate(rotation, duck.`cobblemon$getForwards`())
        UP.rotate(rotation, duck.`cobblemon$getUp`())
        LEFT.rotate(cameraRotation, duck.`cobblemon$getLeft`())
    }
}
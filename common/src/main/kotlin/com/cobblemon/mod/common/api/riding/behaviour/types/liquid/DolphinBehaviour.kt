/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.behaviour.types.liquid

import com.bedrockk.molang.Expression
import com.bedrockk.molang.runtime.MoLangMath.lerp
import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.CobblemonRideSettings
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.molang.ObjectValue
import com.cobblemon.mod.common.api.orientation.OrientationController
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviour
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourState
import com.cobblemon.mod.common.api.riding.behaviour.Side
import com.cobblemon.mod.common.api.riding.behaviour.ridingState
import com.cobblemon.mod.common.api.riding.behaviour.types.air.angleDifference
import com.cobblemon.mod.common.api.riding.posing.PoseOption
import com.cobblemon.mod.common.api.riding.posing.PoseProvider
import com.cobblemon.mod.common.api.riding.sound.RideSoundSettingsList
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.blockPositionsAsListRounded
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.math.geometry.toRadians
import com.cobblemon.mod.common.util.readNullableExpression
import com.cobblemon.mod.common.util.readRidingStats
import com.cobblemon.mod.common.util.resolveBoolean
import com.cobblemon.mod.common.util.resolveDouble
import com.cobblemon.mod.common.util.toVec3d
import com.cobblemon.mod.common.util.writeNullableExpression
import com.cobblemon.mod.common.util.writeRidingStats
import kotlin.math.*
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.util.Mth
import net.minecraft.util.SmoothDouble
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.joml.Vector3f

class DolphinBehaviour : RidingBehaviour<DolphinSettings, DolphinState> {
    companion object {
        val KEY = cobblemonResource("liquid/dolphin")
    }

    override val key = KEY
    val globalDolphin: DolphinSettings
        get() = CobblemonRideSettings.dolphin

    override fun getRidingStyle(settings: DolphinSettings, state: DolphinState): RidingStyle {
        return RidingStyle.LIQUID
    }

    val poseProvider = PoseProvider<DolphinSettings, DolphinState>(PoseType.FLOAT)
        .with(PoseOption(PoseType.SWIM) { _, state, entity -> abs(state.rideVelocity.get().length()) > 0.05 })

    override fun isActive(settings: DolphinSettings, state: DolphinState, vehicle: PokemonEntity): Boolean {
        // If its in water then the dolphin should definitely be active
        if (vehicle.isInWater || vehicle.isUnderWater) {
            return true
        }

        val blockPosBelow = vehicle.blockPosition().below()
        val blockState = vehicle.level().getBlockState(blockPosBelow)

        // Check if the block below is solid AND not a fluid
        val isOnSolidGround = !blockState.isAir && blockState.fluidState.isEmpty

        // The dolphin is only INACTIVE if it is on solid ground.
        // Therefore, it is ACTIVE if it is NOT on solid ground
        return !isOnSolidGround
    }

    override fun pose(settings: DolphinSettings, state: DolphinState, vehicle: PokemonEntity): PoseType {
        return poseProvider.select(settings, state, vehicle)
    }

    override fun speed(settings: DolphinSettings, state: DolphinState, vehicle: PokemonEntity, driver: Player): Float {
        return state.speed.get().toFloat()
    }

    override fun tick(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: Player,
        input: Vec3
    ) {
        if (vehicle.level().isClientSide) {
            handleBoosting(state, vehicle)
            tickStamina(settings, state, vehicle)
        }
    }

    fun handleBoosting(
        state: DolphinState,
        vehicle: PokemonEntity
    ) {
        //If the forward key is not held then it cannot be boosting
        if(Minecraft.getInstance().options.keyUp.isDown() && state.stamina.get() != 0.0f && (vehicle.isInWater || vehicle.isUnderWater)) {
            val boostKeyPressed = Minecraft.getInstance().options.keySprint.isDown()
            if (state.stamina.get() >= 0.25f) {
                //If on the previous tick the boost key was held then don't change if the ride is boosting
                if(state.boostIsToggleable.get() && boostKeyPressed) {
                    //flip the boosting state if boost key is pressed
                    state.boosting.set(!state.boosting.get())
                }
                //If the boost key is not held then next tick boosting is toggleable
                state.boostIsToggleable.set(!boostKeyPressed)
            }
        } else {
            //Turn off boost and reset boost params
            state.boostIsToggleable.set(true)
            state.boosting.set(false)
        }
    }

    fun tickStamina(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity
    ) {
        val stam = state.stamina.get()

        if (vehicle.runtime.resolveBoolean(settings.infiniteStamina ?: globalDolphin.infiniteStamina!!)) {
            return
        }

        // Grab the boost time in seconds and convert to ticks. Then calculate the drain rate as inversely
        // proportional to the number of ticks of boost thus making a full boost take x ticks
        // in short: "Stamina drains slower at higher values and also replenishes slower"
        val boostTime = vehicle.runtime.resolveDouble(settings.staminaExpr ?: globalDolphin.staminaExpr!!) * 20.0f
        val stamDrainRate = (1.0f / boostTime).toFloat()

        val newStam = if (state.boosting.get()) max(0.0f,stam - stamDrainRate)
            else (stam + stamDrainRate).coerceIn(0F, 1F)

        state.stamina.set(newStam)
    }

    override fun rotation(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ): Vec2 {
        return Vec2(driver.xRot * 0.5f, driver.yRot)
    }

    override fun velocity(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: Player,
        input: Vec3
    ): Vec3 {
        if (vehicle !is OrientationControllable) return Vec3.ZERO
        val controller = (vehicle as OrientationControllable).orientationController
        val orientation = controller.orientation ?: return Vec3.ZERO

        val runtime = vehicle.runtime
        val boostMod = if (state.boosting.get()) runtime.resolveDouble(settings.jumpExpr ?: globalDolphin.jumpExpr!!) else 1.0
        val topSpeed = (vehicle.runtime.resolveDouble(settings.speedExpr ?: globalDolphin.speedExpr!!) / 20.0) * boostMod
        val accel = (topSpeed / (vehicle.runtime.resolveDouble(settings.accelerationExpr ?: globalDolphin.accelerationExpr!!) * 20.0)) * boostMod
        val strafeFactor = runtime.resolveDouble(settings.strafeFactor ?: globalDolphin.strafeFactor!!)
        val reverseDriveFactor = runtime.resolveDouble(settings.reverseDriveFactor ?: globalDolphin.reverseDriveFactor!!)
        var currVel = state.rideVelocity.get()

        /*********************************************
         * Breaching logic
         *********************************************/
        if (!vehicle.isInWater && !vehicle.isUnderWater)
        {
            var prevVel = state.lastVelocity.get()
            val breachBoost = 0.3
            state.clearMouseSmoothers.set(false)
            if (!state.hasBreached.get()) {
                // Apply an upwards breaching force if boosting. This will enhance jumping out of water to feel more fun
                if (state.boosting.get()) {
                    prevVel = Vec3(prevVel.x, prevVel.y + breachBoost*(sin(-controller.pitch.toRadians()).coerceIn(0.0f,0.5f)*2.0).pow(2), prevVel.z)
                }
                state.hasBreached.set(true)
            } else {
                val gravity = (9.8 / ( 20.0)) * 0.2 * 0.4
                val terminalVel = 2.0
                prevVel = Vec3(prevVel.x, max(-terminalVel, prevVel.y - gravity), prevVel.z)
            }

            state.lastVelocity.set(prevVel)
            return state.lastVelocity.get().yRot(vehicle.yRot.toRadians())
        } else {
            if(state.hasBreached.get()) {
                state.clearMouseSmoothers.set(true)
            }
            state.hasBreached.set(false)
            currVel = Vec3(currVel.x, 0.0, currVel.z) // Reset y velocity to account for y vel persistance edgecase
        }

        /*********************************************
         * Handle collisions
         *********************************************/
        if (vehicle.horizontalCollision || vehicle.verticalCollision) {
            // not actually friction
            val frictionLimit = 0.1
            val postColSpeed = min(max(state.rideVelocity.get().length(), frictionLimit), state.speed.get())
            state.speed.set(postColSpeed)
        }

        val currZVel = state.rideVelocity.get().z
        val newZVel = when {
            driver.zza > 0.0 -> min(topSpeed, currZVel + accel)
            driver.zza < 0.0 -> max( -topSpeed * reverseDriveFactor, currZVel - accel)
            else -> lerp(currZVel, 0.0, 0.05)
        }
        state.rideVelocity.set(Vec3(currVel.x, currVel.y, newZVel))

        // Convert the local velocity vector into a world vector
        val localVelVec = Vector3f(
            state.rideVelocity.get().x.toFloat(),
            (state.rideVelocity.get().y).toFloat(),
            state.rideVelocity.get().z.toFloat() * -1.0f // Flip the z axis to make this left handed
        )

        val worldVelVec = localVelVec.mul(orientation).toVec3d()
        state.lastVelocity.set(worldVelVec) // Set last velocity to be used during breaching

        // Unrotate preemptively as this vector gets rotated later down the line in MC logic.
        return worldVelVec.yRot(vehicle.yRot.toRadians())
    }

    override fun angRollVel(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: Player,
        deltaTime: Double
    ): Vec3 {
        if (vehicle !is OrientationControllable) return Vec3.ZERO
        val controller = (vehicle as OrientationControllable).orientationController

        val currSpeed = vehicle.deltaMovement.length()

        val handling = vehicle.runtime.resolveDouble(settings.handlingExpr ?: globalDolphin.handlingExpr!!)
        val topSpeed = vehicle.runtime.resolveDouble(settings.speedExpr ?: globalDolphin.speedExpr!!) / 20.0

        val yawDeltaDeg =  deltaTime * handling * sin(Math.toRadians(controller.roll.toDouble())).pow(2) * sin(Math.toRadians(controller.roll.toDouble())).sign
        val trueYawDelt = yawDeltaDeg * abs(cos(Math.toRadians(controller.pitch.toDouble()))*2).coerceIn(-1.0, 1.0) * sqrt(RidingBehaviour.scaleToRange(currSpeed, 0.0, topSpeed))

        // Dampen yaw when upside down
        val yawDampen = (1 - abs(min(cos(controller.roll.toRadians()),0.0f)))
        controller.applyGlobalYaw(trueYawDelt.toFloat() * yawDampen)

        // Correct orientation when at low speeds
        correctOrientation(settings, state, vehicle, controller, driver, deltaTime)

        //yaw, pitch, roll
        return Vec3(0.0, state.currPitchCorrectionForce.get(), state.currRollCorrectionForce.get())
    }

    private fun correctOrientation(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        controller: OrientationController,
        driver: Player,
        deltaTime: Double
    ) {
        // Stop roll and pitch correction when breached
        if (state.hasBreached.get()) {
            state.currRollCorrectionForce.set(
                lerp(state.currRollCorrectionForce.get(), 0.0, deltaTime * 0.98)
            )
            state.currPitchCorrectionForce.set(
                lerp(state.currPitchCorrectionForce.get(), 0.0, deltaTime * 0.98)
            )
            return
        }

        // Calculate correcting roll force.
        val rollCorrectionTimer = vehicle.runtime.resolveDouble(settings.timeToRollCorrect ?: globalDolphin.timeToRollCorrect!!)
        // If the ride is moving then have the correction rate be an equal proportion of:
        // - how rolled are you
        // - how long has it been since last mouse input (higher is stronger correction)
        // Also the correction is proportional as a whole to how NOT pitched are you so you don't spin endlessly when looking up or down
        val howRolledAmI =  sqrt(abs((cos(controller.roll.toRadians()) - 1.0f) * 0.5f)) // sqrt so it trends towards one pretty quickly
        val maxRollCorrectionRate =
            if (state.rideVelocity.get().length() < 0.2) 10.0f * abs(cos(controller.pitch.toRadians()))
            else 10.0f * cos(controller.pitch.toRadians()).pow(2) * (
                    howRolledAmI*1.0f +
                            (state.noInputTime.get() - rollCorrectionTimer).coerceIn(0.0, 1.0).pow(2).toFloat()*0.5f
                    )
        val maxRollForce = maxRollCorrectionRate / 40.0f
        val rollArrivalDeg = 60.0f
        val desiredRoll = 0.0f

        // Calculate the signed error (how far and in what direction)
        val rollError = Mth.wrapDegrees(desiredRoll.angleDifference(controller.roll))

        // Use the absolute error to     calculate the magnitude of the influence
        val influenceMagnitude = (min(rollArrivalDeg, abs(rollError)) / rollArrivalDeg)
        val desiredRollForce = maxRollCorrectionRate * influenceMagnitude * -rollError.sign
        var steeredRollForce = desiredRollForce - state.currRollCorrectionForce.get()

        // Ensure the 'steering' correction force doesn't exceed our maxForce
        if (abs(steeredRollForce) > maxRollForce) {
            steeredRollForce = steeredRollForce.sign * maxRollForce
        }

        // Apply the 'steered' force (its really not a steering force I need a better name)
        state.currRollCorrectionForce.set(
            state.currRollCorrectionForce.get() + steeredRollForce
        )

        //Calculate correcting pitch force.
        val maxPitchCorrectionRate = 5.0f * (state.noInputTime.get() - rollCorrectionTimer).coerceIn(0.0, 1.0).pow(2).toFloat()*0.5f
        val maxPitchForce = maxPitchCorrectionRate / 40.0f
        val pitchArrivalDeg = 45.0f
        val desiredPitch = 0.0f

        if(state.rideVelocity.get().length() < 0.1 && state.noInputTime.get() > rollCorrectionTimer) {
            val pitchErrror = Mth.wrapDegrees(desiredPitch.angleDifference(controller.pitch))
            val arrivalInfluence = (min(pitchArrivalDeg,abs(pitchErrror)) / pitchArrivalDeg)
            val desiredPitchForce = maxPitchCorrectionRate * arrivalInfluence * -pitchErrror.sign
            var steeredPitchForce = desiredPitchForce - state.currPitchCorrectionForce.get()

            // Ensure the 'steering' correction force doesn't exceed our maxForce
            if (abs(steeredPitchForce) > maxPitchForce) {
                steeredPitchForce = steeredPitchForce.sign * maxPitchForce
            }

            // Apply the 'steered' force (its really not a steering force I need a better name)
            state.currPitchCorrectionForce.set(
                state.currPitchCorrectionForce.get() + steeredPitchForce
            )
        } else {
            state.currPitchCorrectionForce.set(
                //reduce rollCorrection gradually
                lerp(state.currPitchCorrectionForce.get(), 0.0, deltaTime * 0.98)
            )
        }
    }

    override fun rotationOnMouseXY(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: Player,
        mouseY: Double,
        mouseX: Double,
        mouseYSmoother: SmoothDouble,
        mouseXSmoother: SmoothDouble,
        sensitivity: Double,
        deltaTime: Double
    ): Vec3 {
        // Begin the roll correction time counter if not enough mouse input
        // is detected
        if (abs(mouseX) < 1 && abs(mouseY) < 1 && (vehicle.isInWater || vehicle.isUnderWater)) {
            state.noInputTime.set(state.noInputTime.get() + deltaTime)
        } else {
            state.noInputTime.set(0.0)
        }

        if (vehicle !is OrientationControllable) return Vec3.ZERO
        val controller = (vehicle as OrientationControllable).orientationController
        val topSpeed = vehicle.runtime.resolveDouble(settings.speedExpr ?: globalDolphin.speedExpr!!) / 20.0
        val boostingHandleDebuff = if (state.boosting.get()) 0.5 else 1.0
        val mouseInputMult = vehicle.runtime.resolveDouble(settings.mouseInputMult ?: globalDolphin.mouseInputMult!!) * boostingHandleDebuff
        // Applied to mouseX to prevent turning upside down when trying to roll hard.
        val rollDegreeDebuff = if(mouseX.sign.toFloat() == controller.roll.sign) 1 - 0.9f*sqrt(abs(sin(controller.roll.toRadians())))*sqrt(abs(cos(controller.pitch))) else 1.0f

        //Smooth out mouse input.
        val smoothingSpeed = if(state.hasBreached.get()) 1.0f else 2.0f
        // When upside down apply a pitching force to nudge the player towards a dive. This reduces the 'weirdness'
        // of being upside down
        val rolledOverPitchCorrection = -8*sqrt(abs(min(cos(controller.roll.toRadians()),0.0f))) * (cos(controller.pitch.toRadians())).pow(2)
        val mouseXc = (mouseX).coerceIn(-45.0, 45.0) * mouseInputMult * rollDegreeDebuff
        val mouseYc = (mouseY).coerceIn(-45.0, 45.0) * mouseInputMult + rolledOverPitchCorrection
        val mouseModForce = if (state.hasBreached.get()) 0.15 else 0.1 // Used to effect more spin momentum during breaching

        if (state.clearMouseSmoothers.get()) {
            mouseXSmoother.reset()
            mouseYSmoother.reset()
            state.clearMouseSmoothers.set(false)
        }
        val xInput = mouseXSmoother.getNewDeltaValue(mouseXc * mouseModForce, deltaTime * smoothingSpeed);
        val yInput = mouseYSmoother.getNewDeltaValue(mouseYc * mouseModForce, deltaTime * smoothingSpeed);
        val currSpeed = state.rideVelocity.get().length()
        val currHorzSpeed = state.rideVelocity.get().horizontalDistance()
        val hoverSpeed = 0.2
        val pitchDamping = abs(cos(controller.pitch.toRadians())).pow(2) // Helper value used when wanting to discard the influence of another value at steep pitches

        // Yaw locally when at or below topSpeed but not when pitched up or rolled over
        val globalYaw = xInput * (1 - sqrt(RidingBehaviour.scaleToRange(currSpeed, 0.0, topSpeed))).coerceIn(0.0, 1.0)
        controller.rotateYaw(pitchDamping*globalYaw.toFloat())
        controller.rotateRoll((1-pitchDamping)*globalYaw.toFloat())

        // Pitch locally up or down depending upon a number of factors:
        // - Reduce pitch substantially when rolled and in a steep yaw. This prevents the player from ignoring a slow handling stat
        // - Do not pitch at all when at or below hover speed
        // - pitch faster at higher speeds
        val p = 1.0 - vehicle.runtime.resolveDouble(settings.horizontalPitchExpr ?: globalDolphin.horizontalPitchExpr!!)
        val localPitch = yInput * (1 - abs(sin(controller.roll.toRadians())).pow(2) * p * hypot(controller.forwardVector.x.toDouble(), controller.forwardVector.z.toDouble()).toFloat())

        var localYawForce  = 0.0
        var rollForce  = 0.0

        // If it has breached then turn the yaw into roll
        if (state.hasBreached.get()) {
            localYawForce = xInput
        } else {
            // Roll less when slow and not at all when at hoverSpeed or lower.
            // Apply a roll righting force when trying to pitch hard while rolled sideways. This nudges the player towards
            // pitching globally
            val pitchInfluencedRollCorrection = 0.0
            val howRolledAmI =  (( cos(controller.roll.toRadians()) - 1).coerceIn(-1.0f, 0.0f) * -2) //Coerce into how much roll you want to let in in the deadzone
            val isInputtingTowardsRoll = if ( xInput.sign == controller.roll.sign.toDouble()) 1.0 else 0.0
            val inputInRolledDir = if (isInputtingTowardsRoll == 1.0) 1.0 else -cos(controller.roll.toRadians()).coerceIn(-1.0f, 0.0f).toDouble()
            val rollDampen = (1 - howRolledAmI.coerceIn(0.0f, 0.5f) * pitchDamping * inputInRolledDir)
            rollForce = xInput * sqrt(abs(RidingBehaviour.scaleToRange(currHorzSpeed, 0.0, topSpeed)).coerceIn(0.0,1.0)) * rollDampen + pitchInfluencedRollCorrection

            // When already rolled significantly part of the roll force should turn to local yaw force
            localYawForce = rollForce * abs(sin(controller.roll.toRadians())).pow(2) * pitchDamping * isInputtingTowardsRoll
        }


        //yaw, pitch, roll
        return Vec3(localYawForce, localPitch, rollForce)
    }

    override fun canJump(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return false
    }

    override fun setRideBar(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return (state.stamina.get() / 1.0f)
    }

    override fun jumpForce(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: Player,
        jumpStrength: Int
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun gravity(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        regularGravity: Double
    ): Double {
        return 0.0 //if (!vehicle.isInWater && !vehicle.isUnderWater) 1.0 else 0.0
    }

    override fun rideFovMultiplier(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return if (state.boosting.get()) 1.2f else 1.0f
    }

    override fun useAngVelSmoothing(settings: DolphinSettings, state: DolphinState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun inertia(settings: DolphinSettings, state: DolphinState, vehicle: PokemonEntity): Double {
        return if (!vehicle.isInWater && !vehicle.isUnderWater) 1.0 else 0.1
    }

    override fun shouldRoll(settings: DolphinSettings, state: DolphinState, vehicle: PokemonEntity): Boolean {
        return true
    }

    override fun turnOffOnGround(settings: DolphinSettings, state: DolphinState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun dismountOnShift(settings: DolphinSettings, state: DolphinState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun shouldRotatePokemonHead(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotateRiderHead(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun getRideSounds(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity
    ): RideSoundSettingsList {
        return settings.rideSounds
    }

    override fun createDefaultState(settings: DolphinSettings) = DolphinState()

    override fun asMoLangValue(
        settings: DolphinSettings,
        state: DolphinState,
        vehicle: PokemonEntity
    ): ObjectValue<RidingBehaviour<DolphinSettings, DolphinState>> {
        val value = super.asMoLangValue(settings, state, vehicle)
        value.functions.put("boosting") { DoubleValue(state.boosting.get()) }
        value.functions.put("has_breached") { DoubleValue(state.hasBreached.get()) }
        return value
    }
}

class DolphinSettings : RidingBehaviourSettings {
    override val key = DolphinBehaviour.KEY
    override val stats = mutableMapOf<RidingStat, IntRange>()

    var infiniteStamina: Expression? = null
        private set

    var canJump: Expression? = null
        private set

    var reverseDriveFactor: Expression? = null
        private set

    var strafeFactor: Expression? = null
        private set

    var timeToRollCorrect: Expression? = null
        private set

    // Boost influence when boosting
    var jumpExpr: Expression? = null
    // Yaw rate in degrees per second
    var handlingExpr: Expression? = null
    var horizontalPitchExpr: Expression? = null
    var mouseInputMult: Expression? = null
    // Top speed in Bl/s
    var speedExpr: Expression? = null
    // Seconds to get to top speed
    var accelerationExpr: Expression? = null
    // Seconds of boost
    var staminaExpr: Expression? = null

    var rideSounds: RideSoundSettingsList = RideSoundSettingsList()

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeRidingStats(stats)
        rideSounds.encode(buffer)
        buffer.writeNullableExpression(infiniteStamina)
        buffer.writeNullableExpression(canJump)
        buffer.writeNullableExpression(reverseDriveFactor)
        buffer.writeNullableExpression(strafeFactor)
        buffer.writeNullableExpression(timeToRollCorrect)
        buffer.writeNullableExpression(jumpExpr)
        buffer.writeNullableExpression(handlingExpr)
        buffer.writeNullableExpression(horizontalPitchExpr)
        buffer.writeNullableExpression(mouseInputMult)
        buffer.writeNullableExpression(speedExpr)
        buffer.writeNullableExpression(accelerationExpr)
        buffer.writeNullableExpression(staminaExpr)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        stats.putAll(buffer.readRidingStats())
        rideSounds = RideSoundSettingsList.decode(buffer)
        infiniteStamina = buffer.readNullableExpression()
        canJump = buffer.readNullableExpression()
        reverseDriveFactor = buffer.readNullableExpression()
        strafeFactor = buffer.readNullableExpression()
        timeToRollCorrect = buffer.readNullableExpression()
        jumpExpr = buffer.readNullableExpression()
        handlingExpr = buffer.readNullableExpression()
        horizontalPitchExpr = buffer.readNullableExpression()
        mouseInputMult = buffer.readNullableExpression()
        speedExpr = buffer.readNullableExpression()
        accelerationExpr = buffer.readNullableExpression()
        staminaExpr = buffer.readNullableExpression()
    }
}

class DolphinState : RidingBehaviourState() {
    var lastVelocity = ridingState(Vec3.ZERO, Side.BOTH)
    var currRollCorrectionForce = ridingState(0.0, Side.CLIENT)
    var currPitchCorrectionForce = ridingState(0.0, Side.CLIENT)
    var noInputTime = ridingState(0.0, Side.CLIENT)
    var boosting = ridingState(false, Side.BOTH)
    var boostIsToggleable = ridingState(false, Side.CLIENT)
    var speed = ridingState(0.0, Side.CLIENT)
    var hasBreached = ridingState(false, Side.CLIENT)
    var clearMouseSmoothers = ridingState(false, Side.CLIENT)

    override fun encode(buffer: FriendlyByteBuf) {
        super.encode(buffer)
        buffer.writeBoolean(boosting.get())
    }

    override fun decode(buffer: FriendlyByteBuf) {
        super.decode(buffer)
        boosting.set(buffer.readBoolean(), forced = true)
    }

    override fun reset() {
        super.reset()
        lastVelocity.set(Vec3.ZERO, forced = true)
        currRollCorrectionForce.set(0.0, forced = true)
        currPitchCorrectionForce.set(0.0, forced = true)
        noInputTime.set(0.0, forced = true)
        boosting.set(false, forced = true)
        boostIsToggleable.set(true, forced = true)
        speed.set(0.0, forced = true)
        hasBreached.set(false, forced = true)
        clearMouseSmoothers.set(false, forced = true)
    }

    override fun toString(): String {
        return "DolphinState(lastVelocity=${lastVelocity.get()})"
    }

    override fun copy() = DolphinState().also {
        it.stamina.set(stamina.get(), forced = true)
        it.rideVelocity.set(rideVelocity.get(), forced = true)
        it.lastVelocity.set(lastVelocity.get(), forced = true)
        it.currRollCorrectionForce.set(this.currRollCorrectionForce.get(), forced = true)
        it.currPitchCorrectionForce.set(this.currPitchCorrectionForce.get(), forced = true)
        it.noInputTime.set(this.noInputTime.get(), forced = true)
        it.boosting.set(this.boosting.get(), forced = true)
        it.boostIsToggleable.set(this.boosting.get(), forced = true)
        it.speed.set(this.speed.get(), forced = true)
        it.hasBreached.set(this.hasBreached.get(), forced = true)
        it.clearMouseSmoothers.set(this.clearMouseSmoothers.get(), forced = true)
    }

    override fun shouldSync(previous: RidingBehaviourState): Boolean {
        if (previous !is DolphinState) return false
        if (previous.boosting.get() != boosting.get()) return true
        return super.shouldSync(previous)
    }
}

/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.sound


import com.cobblemon.mod.common.api.riding.behaviour.types.composite.CompositeBehaviour
import com.cobblemon.mod.common.api.riding.behaviour.types.composite.CompositeState
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.getIsSubmerged
import com.cobblemon.mod.common.util.resolveDouble
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.Mth.lerp
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Class to handle looping ride sounds for clients.
 *
 * @author Jackowes
 * @since April 21st, 2025
 */
class RideLoopSound(val ride: PokemonEntity, val soundSettings: RideSoundSettings) :
        AbstractTickableSoundInstance(SoundEvent.createVariableRangeEvent(soundSettings.soundLocation), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom()) {

    var shouldStop: Boolean = false
    var fade: Float = 0.0f
    var shouldMuffle: Boolean = soundSettings.muffleEnabled
    var rideAttenuation: RideAttenuationModel = soundSettings.attenuationModel
    var muffleAmount: Float = 1.0f // Currently slightly misleading. 1.0 is no muffle and 0.0 is full hFgain muting
    var isPassenger: Boolean = ride.passengers.any { passenger ->
        Minecraft.getInstance().player?.id == passenger.id
    }

    init {
        this.looping = true
        this.delay = 0
        this.volume = 0.1f
        this.attenuation = SoundInstance.Attenuation.LINEAR // This class performs its own attenuation
        if (this.isPassenger) this.relative = true
    }

    override fun tick() {
        // Check if the sound should fade in/out or stop
        this.runFade()

        // If the ride is out of attenuation distance then don't play
        if (this.outOfRange()) {
            this.volume = 0.0f
            return
        }

        // Mute the sound if it's not meant to play for the passenger
        if (this.isPassenger && !this.soundSettings.playForPassengers) {
            this.volume = 0.0f
            return
        }

        // Mute sounds when submerged unless they support underwater sounds, underwater sounds don't play above water
        if (this.ride.getIsSubmerged() && !this.soundSettings.submerged) {
            this.volume = 0.0f
            return
        } else if (!this.ride.getIsSubmerged() && this.soundSettings.submerged) {
            this.volume = 0.0f
            return
        }

        // Perform data driven pitch and volume calculation
        this.volume = ((soundSettings.volumeExpr.let { ride.runtime.resolveDouble(it) })).toFloat()
        this.volume *= this.fade
        this.pitch = ((soundSettings.pitchExpr.let { ride.runtime.resolveDouble(it) })).toFloat()

        if (!this.isPassenger && this.soundSettings.playForNonPassengers) {
            this.setPos()

            // This should now be done fully through openal. Add to RideAttneuationModel and the
            // ChannelMixin to expand on the attenuation models.
            // Attenuate due to distance
            // this.volume *= attenuation()

            // Calculate influence due to the doppler effect
            this.pitch *= calcDopplerInfluence().toFloat()

            // Calculate muffling due to occlusion
            val soundOc = soundOcclusion()
            if (soundOc != 0.0f) {
                val newMuffle = lerp(soundOc,1.0f, 0.2f).coerceIn(0.2f,1.0f)
                // lerp muffle smoothly
                this.muffleAmount = lerp(0.1f, this.muffleAmount, newMuffle)
                this.shouldMuffle = true
                this.volume *= muffleAmount
            } else {
                this.shouldMuffle = true
                this.muffleAmount = lerp(0.1f, this.muffleAmount, 1.0f)
                this.volume *= muffleAmount
            }
        }
    }

    fun runFade() {
        if (this.shouldStop && this.fade == 0.0f) {
            this.stop()
        } else if(this.shouldStop) {
            this.fade = max(0.0f, this.fade - 0.05f)
        } else if(!this.shouldStop && this.fade != 1.0f){
            this.fade = min(1.0f, this.fade + 0.05f)
        }
    }

    fun outOfRange(): Boolean {
        val attenDist = this.sound.attenuationDistance
        val listenerPosition = Minecraft.getInstance().player?.eyePosition ?: return false
        return listenerPosition.distanceTo(ride.position()) > attenDist
    }

    fun calcDopplerInfluence(): Double {
        val rideVel = ride.getRideVelocity()

        // TODO: Expand this to multiple players
        val listener = Minecraft.getInstance().player ?: return 1.0
        val listenerVel = listener.deltaMovement//listener.position().subtract(Vec3(listener.xOld, listener.yOld, listener.zOld))

        // Tweaked constant based off speed of sound in room temp air. Exaggerated for a better effect in minecraft
        val waveSpeed = 343.0f / 50.0f

        // listener->ride los velocity
        val lineOfSightToRide = ride.position().subtract(listener.position()).normalize()
        //val lineOfSightToPlayer = listener.position().subtract(ride.position()).normalize()

        // Speed of the observer towards the sound source
        val velObserver = listenerVel.dot(lineOfSightToRide)
        // Speed of the sound source towards the observer
        val velSrc = rideVel.dot(lineOfSightToRide.scale(-1.0))

        // Use the doppler effect equation to determine the pitch change
        val pitchFactor = (waveSpeed + velObserver) / (waveSpeed - velSrc)

        return pitchFactor
    }

    fun soundOcclusion(): Float {
        val listener = Minecraft.getInstance().player ?: return 0.0f
        val level = listener.level()
        val maxAttenDist = this.sound.attenuationDistance

        var totalMuffle = 0.0

        val center = ride.boundingBox.center

        val hit = level.clip(
            ClipContext(
                listener.eyePosition,
                center,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                listener
            )
        )

        if(hit.type != HitResult.Type.MISS) {
            //Calculate the distance the hit was away from the listener
            val hitDist = hit.location.distanceTo(listener.eyePosition)

            //Calculate the influence based on distance of this particular ray hit
            val hitMuffle = lerp((1 - (hitDist / maxAttenDist)), 0.3, 1.0)
            totalMuffle += hitMuffle
        }


        return totalMuffle.toFloat()
    }

//    fun attenuation(): Float {
//        val listener = Minecraft.getInstance().player ?: return 1.0f
//        val soundDistance = (ride.position().subtract(listener.eyePosition)).length()
//        val maxAttenDistance = this.sound.attenuationDistance
//
//        return (1 - (soundDistance / maxAttenDistance).pow(2)).toFloat()
//    }

    fun setPos() {
        this.x = ride.x
        this.y = ride.y
        this.z = ride.z
    }

    fun stopSound() {
        this.shouldStop = true
        //Minecraft.getInstance().soundManager.stop(this)
    }
}
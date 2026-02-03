/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.entity

import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonNetwork
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.entity.PokemonSideDelegate
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addFunctions
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.api.pokemon.aspect.aspectParticleMap
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.aspect.ParticleData
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourState
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.api.scheduling.ScheduledTask
import com.cobblemon.mod.common.api.scheduling.SchedulingTracker
import com.cobblemon.mod.common.api.scheduling.afterOnClient
import com.cobblemon.mod.common.api.scheduling.lerpOnClient
import com.cobblemon.mod.common.client.ClientMoLangFunctions
import com.cobblemon.mod.common.client.particle.BedrockParticleOptionsRepository
import com.cobblemon.mod.common.client.particle.ParticleStorm
import com.cobblemon.mod.common.client.render.MatrixWrapper
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.client.render.models.blockbench.animation.ActiveAnimation
import com.cobblemon.mod.common.client.render.models.blockbench.animation.PrimaryAnimation
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer.Companion.ease
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.server.pokemon.update.ServerboundUpdateRidingStatePacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.MovingSoundInstance
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.asIdentifierDefaultingNamespace
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.resolve
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Entity.MoveFunction
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

class PokemonClientDelegate : PosableState(), PokemonSideDelegate {
    companion object {
        const val BEAM_SHRINK_TIME = 0.4F
        const val BEAM_EXTEND_TIME = 0.2F
        const val POKEBALL_AIR_TIME = 0.5F
        const val SHINY_PARTICLE_COOLDOWN = 3.5F
    }

    override val schedulingTracker: SchedulingTracker
        get() = currentEntity.schedulingTracker

    lateinit var currentEntity: PokemonEntity
    var phaseTarget: Entity? = null
    var entityScaleModifier = 1F

    override fun getEntity() = currentEntity

    override fun updatePartialTicks(partialTicks: Float) {
        this.currentPartialTicks = partialTicks
        schedulingTracker.update(0F)
    }

    var beamStartTime = System.currentTimeMillis()
    var ballStartTime = System.currentTimeMillis()
    var lastShinyParticle = System.currentTimeMillis()
    var shined = false
    var ballDone = true
    var ballOffset = 0f
    var ballRotOffset = 0f
    var sendOutPosition: Vec3? = null
    var sendOutOffset: Vec3? = null
    var playedSendOutSound: Boolean = false
    var playedThrowingSound: Boolean = false

    val secondsSinceBeamEffectStarted: Float
        get() = (System.currentTimeMillis() - beamStartTime) / 1000F

    val secondsSinceBallThrown: Float
        get() = (System.currentTimeMillis() - ballStartTime) / 1000F

    val secondsSinceLastShinyParticle: Float
        get() = (System.currentTimeMillis() - lastShinyParticle) / 1000F

    private var scaleAnimTask: ScheduledTask? = null
    private var cryAnimation: ActiveAnimation? = null

    override fun onSyncedDataUpdated(data: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(data)
        if (this::currentEntity.isInitialized) {
            if (data == PokemonEntity.SPECIES) {
                val identifier = ResourceLocation.parse(currentEntity.entityData.get(PokemonEntity.SPECIES))
                currentPose = null
                currentEntity.pokemon.species = PokemonSpecies.getByIdentifier(identifier)!! // TODO exception handling
                // force a model update - handles edge case where the PosableState's tracked PosableModel isn't updated until the LivingEntityRenderer render is run
                currentModel = VaryingModelRepository.getPoser(identifier, this)
                currentEntity.refreshRiding()
            } else if (data == PokemonEntity.ASPECTS) {
                currentAspects = currentEntity.entityData.get(PokemonEntity.ASPECTS)
                currentEntity.pokemon.shiny = currentAspects.contains("shiny")
            } else if (data == PokemonEntity.CAUGHT_BALL) {
                PokeBalls.getPokeBall(currentEntity.entityData.get(PokemonEntity.CAUGHT_BALL).asIdentifierDefaultingNamespace())?.let {
                    currentEntity.pokemon.caughtBall = it
                }
            } else if (data == PokemonEntity.DYING_EFFECTS_STARTED) {
                val isDying = currentEntity.entityData.get(PokemonEntity.DYING_EFFECTS_STARTED)
                if (isDying) {
                    val model = currentModel ?: return
                    val animation = try {
                        model.getAnimation(this, "faint", runtime)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    } ?: return
                    val primaryAnimation = PrimaryAnimation(animation)
                    after(seconds = 3F) { entityScaleModifier = 0F }
                    this.addPrimaryAnimation(primaryAnimation)
                }
            } else if (data == PokemonEntity.BEAM_MODE) {
                // If you make adjustments to this code, make sure to Find Usages for both PokemonEntity.beamMode and PokemonEntity.BEAM_MODE
                // TODO: change beamMode to an enum, or a set of booleans (send-out vs recall, delay vs delay)
                val beamMode = currentEntity.beamMode

                when (beamMode) {
                    0 -> { /* Do nothing */ }
                    1 -> {
                        // Scaling up out of pokeball
                        // The server often will "refresh" the value of beamMode.
                        // This would cause the ball particle effects to occur multiple times.
                        // Checking if beamMode 1 is currently in progress with ballDone prevents
                        // the duplicate particle effects.
                        if (ballDone) {
                            playedSendOutSound = false
                            entityScaleModifier = 0F
                            beamStartTime = System.currentTimeMillis()
                            ballStartTime = System.currentTimeMillis()
                            currentEntity.isInvisible = true
                            currentEntity.isSilent = true
                            ballDone = false
                            var soundPos = currentEntity.position()
                            currentEntity.ownerUUID?.let {
                                currentEntity.level().getPlayerByUUID(it)?.let {
                                    val offset = it.position().subtract(currentEntity.position().add(0.0, 2.0 - (ballOffset.toDouble() / 10f), 0.0)).normalize().scale(-ease(ballOffset.toDouble()))
                                    with(it.position().subtract(currentEntity.position())) {
                                        var newOffset = offset.scale(2.0)
                                        val distance = it.position().distanceTo(currentEntity.position())
                                        newOffset = newOffset.scale((distance / 10.0) * 5)
                                        soundPos = currentEntity.position().add(newOffset)
                                    }
                                    it.swing(it.usedItemHand ?: InteractionHand.MAIN_HAND)
                                }
                            }
                            val client = Minecraft.getInstance()
                            val sound = MovingSoundInstance(SoundEvent.createVariableRangeEvent(CobblemonSounds.POKE_BALL_TRAIL.location), SoundSource.PLAYERS, { sendOutPosition?.add(sendOutOffset) }, 0.1f, 1f, false, 20, 0)
                            if (!playedThrowingSound) {
                                client.soundManager.play(sound)
                                playedThrowingSound = true
                            }
                            lerpOnClient(POKEBALL_AIR_TIME) { ballOffset = it }
                            ballRotOffset = ((Math.random()) * currentEntity.random.nextIntBetweenInclusive(-15, 15)).toFloat()

                            currentEntity.after(seconds = POKEBALL_AIR_TIME) {
                                beamStartTime = System.currentTimeMillis()
                                ballDone = true
                                if (client.soundManager.getSoundEvent(CobblemonSounds.POKE_BALL_SEND_OUT.location) != null || client.soundManager.getSoundEvent(CobblemonSounds.POKE_BALL_SHINY_SEND_OUT.location) != null && !playedSendOutSound) {
                                    if (currentEntity.pokemon.shiny) {
                                        client.level?.playSound(client.player, soundPos.x, soundPos.y, soundPos.z, SoundEvent.createVariableRangeEvent(CobblemonSounds.POKE_BALL_SHINY_SEND_OUT.location), SoundSource.PLAYERS, 0.6f, 1f)
                                    } else {
                                        client.level?.playSound(client.player, soundPos.x, soundPos.y, soundPos.z, SoundEvent.createVariableRangeEvent(CobblemonSounds.POKE_BALL_SEND_OUT.location), SoundSource.PLAYERS, 0.6f, 1f)
                                    }
                                    playedSendOutSound = true
                                }
                                currentEntity.ownerUUID?.let {
                                    client.level?.playSound(
                                        client.player,
                                        soundPos.x,
                                        soundPos.y,
                                        soundPos.z,
                                        SoundEvent.createVariableRangeEvent(CobblemonSounds.POKE_BALL_SEND_OUT.location),
                                        SoundSource.PLAYERS,
                                        0.6f,
                                        1f
                                    )
                                    playedSendOutSound = true
                                    /// create end rod particles in a 0.1 radius around the soundPos with a count of 50 and a random velocity of 0.1
                                    sendOutPosition?.let {
                                        val newPos = it.add(sendOutOffset)
                                        val ballType =
                                            currentEntity.pokemon.caughtBall.name.path.lowercase().replace("_", "")
                                        val mode = if (currentEntity.isBattling) "battle" else "casual"
                                        //TODO: A lot of this is probably able to be simplified by just using a single particle with events
                                        //Do it in the particle file, not code.
                                        val sendflash =
                                            BedrockParticleOptionsRepository.getEffect(cobblemonResource("${ballType}/${mode}/sendflash"))
                                        sendflash?.let { effect ->
                                            val wrapper = MatrixWrapper()
                                            val matrix = PoseStack()
                                            //This could potentially cause strange issues if the matrix has a translation component here.
                                            wrapper.updateMatrix(matrix.last().pose())
                                            wrapper.updatePosition(Vec3(newPos.x, newPos.y, newPos.z))
                                            val world = Minecraft.getInstance().level ?: return@let
                                            ParticleStorm(effect, wrapper, wrapper, world).spawn()
                                            val ballsparks =
                                                BedrockParticleOptionsRepository.getEffect(cobblemonResource("${ballType}/${mode}/ballsparks"))
                                            val ballsendsparkle =
                                                BedrockParticleOptionsRepository.getEffect(cobblemonResource("${ballType}/${mode}/ballsendsparkle"))
                                            // using afterOnClient because it's such a small timeframe that it's unlikely the entity has been removed & we'd like the precision
                                            afterOnClient(seconds = 0.01667f) {
                                                ballsparks?.let { effect ->
                                                    ParticleStorm(
                                                        effect,
                                                        wrapper,
                                                        wrapper,
                                                        world
                                                    ).spawn()
                                                }
                                                ballsendsparkle?.let { effect ->
                                                    ParticleStorm(
                                                        effect,
                                                        wrapper,
                                                        wrapper,
                                                        world
                                                    ).spawn()
                                                }
                                                currentEntity.after(seconds = 0.4f) {
                                                    val ballsparkle =
                                                        BedrockParticleOptionsRepository.getEffect(cobblemonResource("${ballType}/ballsparkle"))
                                                    ballsparkle?.let { effect ->
                                                        ParticleStorm(
                                                            effect,
                                                            wrapper,
                                                            wrapper,
                                                            world
                                                        ).spawn()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            currentEntity.after(seconds = POKEBALL_AIR_TIME) {
                                // Skip scaling task if the Pokémon is already being recalled
                                if (scaleAnimTask == null || scaleAnimTask!!.expired) {
                                    scaleAnimTask = lerpOnClient(BEAM_SHRINK_TIME) { entityScaleModifier = it }
                                    currentEntity.isInvisible = false
                                    currentEntity.isSilent = false
                                    currentEntity.after(seconds = POKEBALL_AIR_TIME * 2) {
                                        ballOffset = 0f
                                        ballRotOffset = 0f
                                        sendOutPosition = null
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Scaling up with no delay and no particles
                        // Adding the ballDone check to here as well so that the send out sound doesn't play twice when putting pokemon into pasture block.
                        if (ballDone) {
                            playedSendOutSound = false
                            entityScaleModifier = 0F
                            currentEntity.isInvisible = false
                            currentEntity.isSilent = false
                            ballDone = false
                            val soundPos = currentEntity.position()
                            val client = Minecraft.getInstance()

                        val soundEvent = if (currentEntity.pokemon.shiny) {
                            CobblemonSounds.POKE_BALL_SHINY_SEND_OUT
                        } else {
                            CobblemonSounds.POKE_BALL_SEND_OUT
                        }
                            currentEntity.after(seconds = 0.5F) {
                                ballDone = true
                            }
                            if (!playedSendOutSound && client.soundManager.getSoundEvent(soundEvent.location) != null && currentEntity.ownerUUID != null) {
                                client.level?.playSound(
                                    client.player,
                                    soundPos.x,
                                    soundPos.y,
                                    soundPos.z,
                                    soundEvent,
                                    SoundSource.PLAYERS,
                                    0.6f,
                                    1f
                                )
                                playedSendOutSound = true
                            }
                            scaleAnimTask = lerpOnClient(BEAM_SHRINK_TIME) { entityScaleModifier = it }
                            currentEntity.after(seconds = BEAM_SHRINK_TIME * 2) {
                                ballOffset = 0f
                                ballRotOffset = 0f
                                sendOutPosition = null
                            }
                        }
                    }
                    3 -> {
                        // Scaling down into pokeball
                        beamStartTime = System.currentTimeMillis()
                        ballOffset = 0f
                        ballRotOffset = 0f
                        sendOutPosition = null
                        afterOnClient(seconds = BEAM_EXTEND_TIME) {
                            entityScaleModifier = 1F

                            // Cancel any ongoing scale animation tasks
                            scaleAnimTask?.expire()
                            scaleAnimTask = lerpOnClient(BEAM_SHRINK_TIME) {
                                entityScaleModifier = (1 - it)
                            }
                        }
                    }
                    else -> { /* Do nothing */ }
                }
            } else if (data == PokemonEntity.LABEL_LEVEL) {
                currentEntity.entityData.get(PokemonEntity.LABEL_LEVEL)
                    .takeIf { it > 0 }
                    ?.let { currentEntity.pokemon.level = it }
            } else if (data == PokemonEntity.PHASING_TARGET_ID) {
                val phasingTargetId = currentEntity.entityData.get(PokemonEntity.PHASING_TARGET_ID)
                if (phasingTargetId != -1) {
                    setPhaseTarget(phasingTargetId)
                } else {
                    phaseTarget = null
                }
            } else if (data == PokemonEntity.SCALE_MODIFIER) {
                currentEntity.pokemon.scaleModifier = currentEntity.entityData.get(PokemonEntity.SCALE_MODIFIER)
            }
        }
    }

    override fun changePokemon(pokemon: Pokemon) {
        pokemon.isClient = true
    }

    override fun addToStruct(struct: QueryStruct) {
        super.addToStruct(struct)
        struct.addFunctions(ClientMoLangFunctions.clientFunctions)
        struct.addFunctions(functions.functions)
        runtime.environment.query = struct
    }

    override fun updateAge(age: Int) {
        this.age = age
    }

    override fun initialize(entity: PokemonEntity) {
        this.currentEntity = entity

        this.runtime.environment.query.addFunctions(mapOf(
            "in_battle" to java.util.function.Function {
                return@Function DoubleValue(currentEntity.isBattling)
            },
            "shiny" to java.util.function.Function {
                return@Function DoubleValue(currentEntity.pokemon.shiny)
            },
            "form" to java.util.function.Function {
                return@Function StringValue(currentEntity.pokemon.form.name)
            },
            "width" to java.util.function.Function {
                return@Function DoubleValue(currentEntity.boundingBox.xsize)
            },
            "height" to java.util.function.Function {
                return@Function DoubleValue(currentEntity.boundingBox.ysize)
            },
            "weight" to java.util.function.Function {
                return@Function DoubleValue(currentEntity.pokemon.species.weight.toDouble())
            },
            "friendship" to java.util.function.Function {
                return@Function DoubleValue(currentEntity.pokemon.friendship.toDouble())
            },
        ))
    }

    override fun tick(entity: PokemonEntity) {
        incrementAge(entity)
        playWildShinySounds()
        sendRidingChanges(entity)
    }

    private fun sendRidingChanges(entity: PokemonEntity) {
        val player = Minecraft.getInstance().player ?: return
        if (entity.controllingPassenger != player) return
        entity.ifRidingAvailable { behaviour, _, state ->
            if (entity.previousRidingState != null && !state.shouldSync(entity.previousRidingState!!)) {
                return@ifRidingAvailable
            }
            CobblemonNetwork.sendToServer(ServerboundUpdateRidingStatePacket(entity.id, behaviour.key, state))
        }
    }

    fun playWildShinySounds() {
        val player = Minecraft.getInstance().player ?: return
        val isWithinRange = player.position().distanceTo(currentEntity.position()) <= Cobblemon.config.shinyNoticeParticlesDistance

        if (currentEntity.pokemon.shiny && currentEntity.ownerUUID == null && !currentEntity.isSilent) {
            if (isWithinRange) {
                if (secondsSinceLastShinyParticle > SHINY_PARTICLE_COOLDOWN && !currentEntity.isBattling) {
                    playShinyEffect("cobblemon:shiny_sparkle_ambient_wild")
                    lastShinyParticle = System.currentTimeMillis()
                }
                if (!shined && !player.isSpectator) {
                    playShinyEffect("cobblemon:wild_shiny_ring")
                    shined = true
                }
            } else {
                shined = false
            }
        }
    }

    fun playShinyEffect(particleId: String) {
        val locator = listOf("shiny_particles", "middle").firstOrNull { this.locatorStates[it] != null } ?: "root"
        runtime.resolve("q.particle('$particleId', '$locator')".asExpressionLike())
    }

    fun setPhaseTarget(targetId: Int) {
        this.phaseTarget = currentEntity.level().getEntity(targetId)
    }

    override fun updatePostDeath() {
        ++currentEntity.deathTime
    }

    override fun spawnShinyParticle(player: Player) {
        if (secondsSinceLastShinyParticle > SHINY_PARTICLE_COOLDOWN) {
            playShinyEffect("cobblemon:ambient_shiny_sparkle")
            lastShinyParticle = System.currentTimeMillis()
        }
    }

    override fun spawnAspectParticle() {
        currentAspects = currentEntity.entityData.get(PokemonEntity.ASPECTS)
        currentAspects.forEach { aspect ->
            aspectParticleMap[aspect]?.let { particleData ->
                val random = currentEntity.level().random
                when (particleData) {
                    is ParticleData.SnowstormParticle -> {
                        val locator = particleData.locators.firstOrNull { this.locatorStates[it] != null } ?: "root"
                        if (particleData.chance > random.nextDouble()) {
                            repeat(particleData.amount) {
                                runtime.resolve("q.particle('${particleData.particle}', '$locator')".asExpressionLike())
                            }
                        }
                    }
                    is ParticleData.MinecraftParticle -> {
                        if (particleData.chance > random.nextDouble()) {
                            val box = currentEntity.boundingBox
                            repeat(particleData.amount) {
                                val x = box.minX + (box.maxX - box.minX) * random.nextDouble()
                                val y = box.minY + (box.maxY - box.minY) * random.nextDouble()
                                val z = box.minZ + (box.maxZ - box.minZ) * random.nextDouble()

                                currentEntity.level().addParticle(particleData.particle, x, y, z, 0.0, 0.0, 0.0)
                            }
                        }
                    }
                }
            }
        }
    }

    fun cry() {
        val model = currentModel ?: return
        if (cryAnimation != null && (cryAnimation in activeAnimations || cryAnimation == primaryAnimation)) {
            return
        }

        val animation = model.cryAnimation(this) ?: return
        if (animation is PrimaryAnimation) {
            addPrimaryAnimation(animation)
        } else {
            activeAnimations.add(animation)
        }
        cryAnimation = animation
    }

    override fun applyRenderRotation(partialTick: Float) {
        currentEntity.ifRidingAvailable { behaviour, settings, state ->
            behaviour.applyRenderRotation(settings, state, currentEntity, partialTick)
        }
    }

    fun getSeatLocator(passenger: Entity): String {
        val seatIndex = this.getEntity().occupiedSeats.indexOf(passenger)
        if (seatIndex == -1) throw IllegalArgumentException("Entity is not currently riding a seat")
        return this.getEntity().rideProp.seats[seatIndex].locator ?: "seat_${seatIndex + 1}"
    }

    override fun positionRider(passenger: Entity, positionUpdater: MoveFunction) {
        val locatorName = getSeatLocator(passenger)
        val locator = this.locatorStates[locatorName] ?: return

        val offset = locator.matrix.getTranslation(Vector3f())
            .sub(
                Vector3f(
                    0f,
                    passenger.eyeHeight - (passenger.bbHeight / 2),
                    0f
                )
            ) // This is close but not exact

        positionUpdater.accept(
            passenger,
            this.getEntity().x + offset.x,
            this.getEntity().y + offset.y,
            this.getEntity().z + offset.z
        )
    }
}
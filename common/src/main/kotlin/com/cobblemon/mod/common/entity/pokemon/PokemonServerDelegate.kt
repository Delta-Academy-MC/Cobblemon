/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon

import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.MoValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonMemories
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.entity.PokemonSender
import com.cobblemon.mod.common.api.entity.PokemonSideDelegate
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addPokemonEntityFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addPokemonFunctions
import com.cobblemon.mod.common.api.molang.ObjectValue
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.feature.TickingSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.api.spawning.fishing.FishingSpawnCause.Companion.DROPS_REROLL_ASPECT
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.activestate.ActivePokemonState
import com.cobblemon.mod.common.pokemon.activestate.SentOutState
import com.cobblemon.mod.common.util.*
import com.cobblemon.mod.common.util.math.geometry.toRadians
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.pathfinder.PathType
import org.joml.Matrix3f
import org.joml.Vector3f
import java.util.*
import kotlin.math.*

/** Handles purely server logic for a Pokémon */
class PokemonServerDelegate : PokemonSideDelegate {
    lateinit var entity: PokemonEntity
    var acknowledgedHPValue = -1

    /** Mocked properties exposed to the client [PokemonEntity]. */
    private val mock: PokemonProperties?
        get() = entity.effects.mockEffect?.mock

    override fun changePokemon(pokemon: Pokemon) {
        updatePathfindingPenalties(pokemon)
//        entity.registerGoals()
        updateAttributes(pokemon)
        updateHealth(pokemon)
        entity.struct.addPokemonFunctions(pokemon)
        entity.struct.addPokemonEntityFunctions(entity)
    }

    fun updatePathfindingPenalties(pokemon: Pokemon) {
        val moving = pokemon.form.behaviour.moving
        entity.setPathfindingMalus(PathType.LAVA, if (moving.swim.canSwimInLava) 12F else -1F)
        entity.setPathfindingMalus(PathType.WATER, if (moving.swim.canSwimInWater) 12F else -1F)
        entity.setPathfindingMalus(PathType.WATER_BORDER, if (moving.swim.canSwimInWater || moving.walk.avoidsLand || moving.swim.canBreatheUnderwater) 6F else -1F)
        if (moving.swim.canBreatheUnderwater) {
            // Must have a malus of zero to be a valid wander target
            entity.setPathfindingMalus(PathType.WATER, 0F)
        }
        if (moving.swim.canBreatheUnderlava) {
            // Must have a malus of zero to be a valid wander target
            entity.setPathfindingMalus(PathType.LAVA, if (moving.swim.canSwimInLava) 0F else -1F)
        }
        if (moving.walk.avoidsLand) {
            entity.setPathfindingMalus(PathType.WALKABLE, 12F)
        }

        if (moving.walk.canWalk && moving.fly.canFly) {
            entity.setPathfindingMalus(PathType.WALKABLE, 0F)
        }

        entity.navigation.setCanPathThroughFire(entity.fireImmune())
    }

    fun updateAttributes(pokemon: Pokemon) {
        entity.removeAllEffects()

        if (pokemon.ability.name == "levitate") {
            entity.getAttribute(Attributes.GRAVITY)?.baseValue = 0.04
        }

        val maxHealth: Double = this.maxHpToMaxHealthCurve(pokemon.maxHealth).toDouble()
        entity.getAttribute(Attributes.MAX_HEALTH)?.baseValue = maxHealth

        val (armour, toughness) = this.defenceToArmourCurve(pokemon.defence)
        entity.getAttribute(Attributes.ARMOR)?.baseValue = armour
        entity.getAttribute(Attributes.ARMOR_TOUGHNESS)?.baseValue = toughness

        val attackDamage = this.attackToDamageCurve(pokemon.attack).toDouble()
        entity.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue = attackDamage
    }

    /**
     * Apply inverse scaling to the given HP value so that bulky Pokémon don't end up with 100 Hearts while starters all have half a heart.
     */
    fun maxHpToMaxHealthCurve(max_hp: Int): Int {
        return when {
            // Escape hatch niche situations such as Shedinja which should have a fixed half-a-heart too
            max_hp <= 1 -> 1;
            // Really squishy Pokémon should still have *some* health so they don't die instantly. 6 Health is as much as a bat.
            // There are extremely few Pokémon that have less than 12 HP even at level 1, so this is a rare edgecase.
            max_hp < 12 -> 6;
            else -> {
                val max_hp_d: Double = max_hp.toDouble();
                // This will give a 30-HP starter 5½ hearts and a max bulk Blissey of 714 HP a round 50 hearts.
                sqrt(max_hp_d.pow(1.402)).roundToInt()
            }
        }
    }

    /**
     * Apply inverse scaling to the given Defence value, returning a touple of (Armour, Armour Toughness) that the given Pokémon should have.
     */
    fun defenceToArmourCurve(defense: Int): Pair<Double, Double> {
        val armour = when {
            defense < 200 -> 0.0;
            else -> {
                min(30.0, round((defense - 200) / 10.0))
            }
        }
        val toughness = when {
            defense < 300 -> 0.0;
            else -> {
                min(20.0, round((defense - 300) / 7.5))
            }
        }
        return armour to toughness
    }

    /**
     * Applies inverse scaling so weaker and starter Pokémon can more easily catch up in damage,
     * while preventing high-Attack Pokémon from scaling out of control.
     *
     * A graph of this formula can be seen here: https://www.desmos.com/calculator/vcrpdcrbhd
     *
     * Examples:
     *  - Lv. 1 Happiny (5 Attack) → 0.5 hearts
     *  - Lv. 10 Charmander (16 Attack) → 1.5 hearts
     *  - Lv. 20 Mankey (35 Attack) → 2.5 hearts
     *  - Lv. 40 Vespiquen (79 Attack) → 3.5 hearts
     *  - Lv. 60 Rhydon (176 Attack) → 5 hearts
     *  - Lv. 100 Adamant Slaking with full Attack IVs/EVs (460 Attack)
     *      → 7.5 hearts (same as an Iron Golem)
     */
    fun attackToDamageCurve(attack: Int): Int {
        val damage = when {
            attack < 10 -> 1
            else -> ceil(sqrt((attack - 10).toDouble().pow(0.875))).toInt()
        }

        return damage.coerceAtLeast(1)
    }

    /**
     * Update Minecraft-side Health (i.e. hearts) based on the Pokémon's current HP value
     */
    fun updateHealth(pokemon: Pokemon) {
        if (acknowledgedHPValue != pokemon.currentHealth) {
            acknowledgedHPValue = pokemon.currentHealth

            if (pokemon.currentHealth <= 0) {
                entity.health = 0.0f
            } else {
                val currentHPRatio: Float = pokemon.currentHealth / pokemon.maxHealth.toFloat()

                if (currentHPRatio.isFinite()) {
                    // Entity Health is stored as float but only ever in-/decreased by whole numbers.
                    entity.health = ceil(entity.maxHealth * currentHPRatio)
                }
            }
        }
    }

    override fun initialize(entity: PokemonEntity) {
        this.entity = entity
        with(entity) {
            speed = 0.1F
            entity.despawner.beginTracking(this)

            initializeScripting()
        }
        updateTrackedValues()
    }

    override fun addToStruct(struct: QueryStruct) {
        super.addToStruct(struct)
        if (entity.pokemon.isWild()) {
            struct
                    .addFunction("attempt_wild_battle") { params ->
                        val opponentValue = params.get<MoValue>(0)
                        val opponent = if (opponentValue is ObjectValue<*>) {
                            opponentValue.obj as ServerPlayer
                        } else {
                            val paramString = opponentValue.asString()
                            val playerUUID = paramString.asUUID
                            if (playerUUID != null) {
                                entity.server!!.playerList.getPlayer(playerUUID) ?: return@addFunction DoubleValue.ZERO
                            } else {
                                entity.server!!.playerList.getPlayerByName(paramString) ?: return@addFunction DoubleValue.ZERO
                            }
                        }

                        // Need to wait to ensure any entity info gets grabbed properly before battle attempt.
                        return@addFunction entity.after(0.01F) {
                            DoubleValue(entity.forceBattle(opponent))
                        }
                    }
        }
    }

    fun getBattle() = entity.battleId?.let(BattleRegistry::getBattle)

    fun updateTrackedValues() {
        val trackedSpecies = mock?.species ?: entity.pokemon.species.resourceIdentifier.toString()
        val trackedNickname =  mock?.nickname ?: entity.pokemon.nickname ?: Component.empty()
        val trackedMark = entity.pokemon.activeMark?.identifier.toString()
        val trackedAspects = mock?.aspects ?: entity.pokemon.aspects
        val trackedBall = mock?.pokeball ?: entity.pokemon.caughtBall.name.toString()
        val trackedScaleModifier = mock?.scaleModifier ?: entity.pokemon.scaleModifier

        entity.ownerUUID = entity.pokemon.getOwnerUUID()
        entity.entityData.set(PokemonEntity.SPECIES, trackedSpecies)
        if (entity.entityData.get(PokemonEntity.NICKNAME) != trackedNickname) {
            entity.entityData.set(PokemonEntity.NICKNAME, trackedNickname)
        }
        if (entity.entityData.get(PokemonEntity.MARK) !=trackedMark) {
            entity.entityData.set(PokemonEntity.MARK, trackedMark)
        }
        entity.entityData.set(PokemonEntity.ASPECTS, trackedAspects)
        entity.entityData.set(PokemonEntity.LABEL_LEVEL, entity.pokemon.level)

        if (entity.getCurrentPoseType() in PoseType.FLYING_POSES) {
            entity.entityData.set(PokemonEntity.MOVING, entity.isPokemonFlying)
        }
        else {
            entity.entityData.set(PokemonEntity.MOVING, entity.isPokemonWalking)
        }

        entity.entityData.set(PokemonEntity.FRIENDSHIP, entity.pokemon.friendship)
        entity.entityData.set(PokemonEntity.CAUGHT_BALL, trackedBall)
        entity.entityData.set(PokemonEntity.SCALE_MODIFIER, trackedScaleModifier)
        if (entity.pokemon.rideStamina != entity.entityData.get(PokemonEntity.RIDE_STAMINA) && entity.passengers.isEmpty()) {
            entity.entityData.set(PokemonEntity.RIDE_STAMINA, entity.pokemon.rideStamina)
        }

        val currentRideBoosts = entity.entityData.get(PokemonEntity.RIDE_BOOSTS)
        val newRideBoosts = entity.pokemon.getRideBoosts()
        if (currentRideBoosts.size != newRideBoosts.size || currentRideBoosts.any { (key, value) -> newRideBoosts[key] != value }) {
            entity.entityData.set(PokemonEntity.RIDE_BOOSTS, newRideBoosts)
        }

        updateShownItem()
        updatePoseType()
    }

    override fun onSyncedDataUpdated(data: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(data)
        if (this::entity.isInitialized) {
            when (data) {
                PokemonEntity.BEHAVIOUR_FLAGS -> updatePoseType()
                PokemonEntity.SPECIES -> entity.refreshRiding()
            }
        }
    }

    override fun tick(entity: PokemonEntity) {
        val state = entity.pokemon.state
        if (state !is ActivePokemonState || state.entity != entity) {
            if (!entity.isDeadOrDying && entity.health > 0) {
                entity.pokemon.state = SentOutState(entity)
            }
        }

        if (entity.ownerUUID != null && entity.pokemon.storeCoordinates.get() == null) {
            return entity.discard()
        } else if (entity.pokemon.isNPCOwned()) {
            if (entity.owner?.isAlive != true && entity.battleId == null) return entity.discard()
            if (entity.ownerUUID == null) entity.ownerUUID = entity.pokemon.getOwnerUUID()
        }

        val tethering = entity.tethering
        if (tethering != null && entity.pokemon.tetheringId != tethering.tetheringId) {
            return entity.discard()
        }

//        if (!entity.behaviour.moving.walk.canWalk && entity.behaviour.moving.fly.canFly && !entity.getBehaviourFlag(PokemonBehaviourFlag.FLYING)) {
//            entity.setBehaviourFlag(PokemonBehaviourFlag.FLYING, true)
//        }

        entity.entityData.update(PokemonEntity.BATTLE_ID) { opt ->
            val battleId = opt.orElse(null)
            if (battleId != null && BattleRegistry.getBattle(battleId).let { it == null || it.ended }) {
                Optional.empty()
            } else {
                opt
            }
        }

        val battle = getBattle()
        if (entity.ticksLived % 20 == 0 && battle != null) {
            val activeBattlePokemon = battle
                .activePokemon
                .find { it.battlePokemon?.uuid == entity.pokemon.uuid }

            if (activeBattlePokemon != null) {
                activeBattlePokemon.position = entity.level() as ServerLevel to entity.position()
            }
        }

        updateHealth(entity.pokemon)

        // TODO: updateTrackedValues does the same check and set again, refactor.
        if (entity.ownerUUID != entity.pokemon.getOwnerUUID()) {
            entity.ownerUUID = entity.pokemon.getOwnerUUID()
        }

        if (entity.ownerUUID == null && tethering != null) {
            entity.ownerUUID = tethering.playerId
        }

        if (entity.ownerUUID != null && entity.owner == null && entity.tethering == null) {
            entity.remove(Entity.RemovalReason.DISCARDED)
        }

        if (entity.level().gameTime % 20 == 0L) {
            for (feature in entity.pokemon.features) {
                if (feature is TickingSpeciesFeature) {
                    feature.onSecondPassed(entity.level() as ServerLevel, entity.pokemon, entity)
                }
            }
        }

        updateTrackedValues()
    }

    fun updateShownItem() {
        val trackedShownItem = when {
            // Show Hand Item if Held item is hidden
            !entity.pokemon.heldItemVisible -> entity.mainHandItem
            // Show Held Item unless it is empty
            else -> (entity as PokemonEntity?)?.pokemon?.heldItemNoCopy()?.takeUnless { it.isEmpty }
            // Show Hand Item if Held item is empty
            ?: entity.mainHandItem
        }.copy()
        /* Hide items tagged as hidden (If the item is in this list, it will not render) */
        .let { if (it.`is`(CobblemonItemTags.HIDDEN_ITEMS)) ItemStack.EMPTY else it}

        entity.entityData.set(PokemonEntity.SHOWN_HELD_ITEM, trackedShownItem)
    }

    fun updatePoseType() {
        if (!entity.enablePoseTypeRecalculation || entity.passengers.isNotEmpty()) {
            return
        }

        val isSleeping = if (entity.battle != null) {
            entity.pokemon.status?.status == Statuses.SLEEP
        } else {
            (entity.brain.getMemorySafely(CobblemonMemories.POKEMON_SLEEPING).orElse(false) || entity.pokemon.status?.status == Statuses.SLEEP) && entity.behaviour.resting.canSleep
        }
        val isMoving = entity.entityData.get(PokemonEntity.MOVING)
        val isPassenger = entity.isPassenger
        val isUnderwater = entity.getIsSubmerged()
        val isFlying = entity.getBehaviourFlag(PokemonBehaviourFlag.FLYING)

        val poseType = when {
            isPassenger -> PoseType.STAND
            isSleeping -> PoseType.SLEEP
            isMoving && isUnderwater  -> PoseType.SWIM
            isUnderwater -> PoseType.FLOAT
            isMoving && isFlying -> PoseType.FLY
            isFlying -> PoseType.HOVER
            isMoving -> PoseType.WALK
            else -> PoseType.STAND
        }

        entity.entityData.set(PokemonEntity.POSE_TYPE, poseType)
    }

    override fun drop(source: DamageSource?) {
        val player = source?.directEntity as? ServerPlayer
        if (entity.pokemon.isWild()) {
            entity.killer = player
        }
    }

    fun doDeathDrops() {
        if (entity.pokemon.isLegendary() || entity.pokemon.isMythical()) {
            return
        }
        if (entity.ownerUUID == null && entity.owner == null && entity.level().gameRules.getBoolean(CobblemonGameRules.DO_POKEMON_LOOT)) {
            val dropTable = (entity.drops ?: entity.pokemon.form.drops)
            val drops = dropTable.getDrops().toMutableList()
            if (entity.pokemon.forcedAspects.contains(DROPS_REROLL_ASPECT)) {
                val dropsReroll = dropTable.getDrops()
                drops.addAll(dropsReroll)
            }

            dropTable.postLootDroppedEvent(drops, entity, entity.level() as ServerLevel, entity.position(), entity.killer)
        }
    }

    override fun updatePostDeath() {
        // clear active effects before proceeding
        val owner = entity.owner
        if (!entity.entityData.get(PokemonEntity.DYING_EFFECTS_STARTED)) {
            entity.entityData.set(PokemonEntity.DYING_EFFECTS_STARTED, true)
            if (owner is PokemonSender && entity.beamMode == -1) {
                entity.recallWithAnimation()
            }
        }
        if (entity.deathTime == 0) {
            entity.effects.wipe()
            entity.deathTime = 1
            if (!Cobblemon.config.dropAfterDeathAnimation) doDeathDrops()
            return
        } else if (entity.effects.progress?.isDone == false) {
            return
        }

        ++entity.deathTime

        if (entity.deathTime == 30) {
            if (owner != null && owner !is PokemonSender) {
                entity.level().playSoundServer(owner.position(), CobblemonSounds.POKE_BALL_RECALL, volume = 0.6F)
//                entity.recallWithAnimation()
                entity.entityData.set(PokemonEntity.PHASING_TARGET_ID, owner.id)
                entity.entityData.set(PokemonEntity.BEAM_MODE, 3)
            }
        }

        if (entity.deathTime == 60) {
            entity.level().broadcastEntityEvent(entity, 60.toByte()) // Sends smoke effect
            if (Cobblemon.config.dropAfterDeathAnimation) doDeathDrops()
            entity.remove(Entity.RemovalReason.KILLED)
        }
    }

    override fun positionRider(
        passenger: Entity,
        positionUpdater: Entity.MoveFunction
    ) {
        val index =
            this.entity.passengers.indexOf(passenger).takeIf { it >= 0 && it < this.entity.seats.size } ?: return
        val seat = this.entity.seats[index]
        val seatOffset = seat.getOffset(this.entity.getCurrentPoseType()).toVector3f()
        val center = Vector3f(0f, this.entity.bbHeight / 2, 0f)

        val seatToCenter = center.sub(seatOffset, Vector3f())
        val matrix = (this.entity.passengers.first() as? OrientationControllable)?.orientationController?.orientation
            ?: Matrix3f().rotate((180f - passenger.yRot).toRadians(), Vector3f(0f, 1f, 0f))
        val offset =
            matrix.transform(seatToCenter, Vector3f()).add(center).sub(Vector3f(0f, passenger.bbHeight / 2, 0f))

        positionUpdater.accept(
            passenger,
            this.entity.x + offset.x,
            this.entity.y + offset.y,
            this.entity.z + offset.z
        )
    }
}

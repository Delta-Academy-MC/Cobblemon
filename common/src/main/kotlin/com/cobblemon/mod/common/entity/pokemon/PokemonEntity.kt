/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon

import com.bedrockk.molang.runtime.MoLangRuntime
import com.bedrockk.molang.runtime.struct.VariableStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.*
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.drop.DropTable
import com.cobblemon.mod.common.api.entity.Despawner
import com.cobblemon.mod.common.api.entity.PokemonSender
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.entity.PokemonEntityLoadEvent
import com.cobblemon.mod.common.api.events.entity.PokemonEntitySaveEvent
import com.cobblemon.mod.common.api.events.entity.PokemonEntitySaveToWorldEvent
import com.cobblemon.mod.common.api.events.pokemon.HeldItemEvent
import com.cobblemon.mod.common.api.events.pokemon.RidePokemonEvent
import com.cobblemon.mod.common.api.events.pokemon.ShoulderMountEvent
import com.cobblemon.mod.common.api.interaction.PokemonEntityInteraction
import com.cobblemon.mod.common.api.interaction.PokemonInteractions
import com.cobblemon.mod.common.api.mark.Marks
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addEntityFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addLivingEntityFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addPokemonEntityFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addPokemonFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addStandardFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.api.molang.ObjectValue
import com.cobblemon.mod.common.api.net.serializers.IdentifierDataSerializer
import com.cobblemon.mod.common.api.net.serializers.PlatformTypeDataSerializer
import com.cobblemon.mod.common.api.net.serializers.PoseTypeDataSerializer
import com.cobblemon.mod.common.api.net.serializers.RideBoostsDataSerializer
import com.cobblemon.mod.common.api.net.serializers.StringSetDataSerializer
import com.cobblemon.mod.common.api.npc.configuration.MoLangConfigVariable
import com.cobblemon.mod.common.api.pokemon.feature.ChoiceSpeciesFeatureProvider
import com.cobblemon.mod.common.api.pokemon.feature.FlagSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.feature.IntSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures
import com.cobblemon.mod.common.api.pokemon.feature.StringSpeciesFeature
import com.cobblemon.mod.common.api.reactive.ObservableSubscription
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.api.riding.Rideable
import com.cobblemon.mod.common.api.riding.RidingProperties
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.Seat
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviour
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourState
import com.cobblemon.mod.common.api.riding.behaviour.RidingController
import com.cobblemon.mod.common.api.riding.events.SelectDriverEvent
import com.cobblemon.mod.common.api.riding.sound.RideSoundManager
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.api.riding.util.RidingAnimationData
import com.cobblemon.mod.common.api.scheduling.Schedulable
import com.cobblemon.mod.common.api.scheduling.SchedulingTracker
import com.cobblemon.mod.common.api.scheduling.afterOnServer
import com.cobblemon.mod.common.api.spawning.BestSpawner
import com.cobblemon.mod.common.api.spawning.SpawnCause
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.api.types.tera.TeraType
import com.cobblemon.mod.common.api.types.tera.TeraTypes
import com.cobblemon.mod.common.battles.BagItems
import com.cobblemon.mod.common.battles.BattleBuilder
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.SuccessfulBattleStart
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.cobblemon.mod.common.client.MountedCameraTypeHandler
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.entity.*
import com.cobblemon.mod.common.entity.PoseType.Companion.NO_GRAV_POSES
import com.cobblemon.mod.common.entity.ai.OmniPathNavigation
import com.cobblemon.mod.common.entity.generic.GenericBedrockEntity
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity
import com.cobblemon.mod.common.entity.pokemon.ai.PokemonMoveControl
import com.cobblemon.mod.common.entity.pokemon.ai.sensors.PokemonItemSensor
import com.cobblemon.mod.common.entity.pokemon.effects.EffectTracker
import com.cobblemon.mod.common.entity.pokemon.effects.IllusionEffect
import com.cobblemon.mod.common.net.messages.client.OpenBehaviourEditorPacket
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket
import com.cobblemon.mod.common.net.messages.client.sound.UnvalidatedPlaySoundS2CPacket
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnPokemonPacket
import com.cobblemon.mod.common.net.messages.client.ui.InteractPokemonUIPacket
import com.cobblemon.mod.common.net.messages.server.behaviour.DamageOnCollisionPacket
import com.cobblemon.mod.common.net.messages.server.riding.DismountPokemonPacket
import com.cobblemon.mod.common.net.serverhandling.storage.SendOutPokemonHandler.SEND_OUT_DURATION
import com.cobblemon.mod.common.pokeball.PokeBall
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData
import com.cobblemon.mod.common.pokedex.scanner.ScannableEntity
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.OriginalTrainerType
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.pokemon.activestate.ActivePokemonState
import com.cobblemon.mod.common.pokemon.activestate.InactivePokemonState
import com.cobblemon.mod.common.pokemon.activestate.SentOutState
import com.cobblemon.mod.common.pokemon.activestate.ShoulderedState
import com.cobblemon.mod.common.pokemon.ai.FormPokemonBehaviour
import com.cobblemon.mod.common.pokemon.ai.ObtainableItem
import com.cobblemon.mod.common.pokemon.ai.PokemonBrain
import com.cobblemon.mod.common.pokemon.evolution.variants.ItemInteractionEvolution
import com.cobblemon.mod.common.pokemon.feature.SlowpokeTailRegrowthSpeciesFeatureProvider
import com.cobblemon.mod.common.pokemon.feature.StashHandler
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty
import com.cobblemon.mod.common.util.*
import com.cobblemon.mod.common.util.math.geometry.toRadians
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules
import com.google.common.collect.UnmodifiableIterator
import com.mojang.serialization.Codec
import com.mojang.serialization.Dynamic
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.DebugPackets
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.Brain
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.Shearable
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeInstance
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.memory.MemoryStatus
import net.minecraft.world.entity.ai.sensing.Sensor
import net.minecraft.world.entity.ai.sensing.SensorType
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.ShoulderRidingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.DismountHelper
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.SuspiciousEffectHolder
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.material.EmptyFluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
open class PokemonEntity(
    world: Level,
    pokemon: Pokemon = Pokemon().apply { isClient = world.isClientSide },
    type: EntityType<out PokemonEntity> = CobblemonEntities.POKEMON,
) : ShoulderRidingEntity(type, world), PosableEntity, Shearable, Schedulable, Rideable, ScannableEntity, MoLangScriptingEntity, OmniPathingEntity {
    companion object {
        @JvmStatic val SPECIES = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.STRING)
        @JvmStatic val NICKNAME = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.COMPONENT)
        @JvmStatic val NICKNAME_VISIBLE = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val MARK = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.STRING)
        @JvmStatic val SHOULD_RENDER_NAME = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val MOVING = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val BEHAVIOUR_FLAGS = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BYTE)
        @JvmStatic val PHASING_TARGET_ID = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.INT)
        @JvmStatic val PLATFORM_TYPE = SynchedEntityData.defineId(PokemonEntity::class.java, PlatformTypeDataSerializer)
        @JvmStatic val BEAM_MODE = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BYTE)
        @JvmStatic val BATTLE_ID = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.OPTIONAL_UUID)
        @JvmStatic val ASPECTS = SynchedEntityData.defineId(PokemonEntity::class.java, StringSetDataSerializer)
        @JvmStatic val DYING_EFFECTS_STARTED = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val POSE_TYPE = SynchedEntityData.defineId(PokemonEntity::class.java, PoseTypeDataSerializer)
        @JvmStatic val LABEL_LEVEL = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.INT)
        @JvmStatic val HIDE_LABEL = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val UNBATTLEABLE = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val COUNTS_TOWARDS_SPAWN_CAP = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic val SPAWN_DIRECTION = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.FLOAT)
        @JvmStatic val FRIENDSHIP = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.INT)
        @JvmStatic val FREEZE_FRAME = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.FLOAT)
        @JvmStatic val CAUGHT_BALL = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.STRING)
        @JvmStatic val EVOLUTION_STARTED = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.BOOLEAN)
        @JvmStatic var SHOWN_HELD_ITEM = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.ITEM_STACK)
        @JvmStatic var RIDE_BOOSTS = SynchedEntityData.defineId(PokemonEntity::class.java, RideBoostsDataSerializer)
        @JvmStatic var RIDE_STAMINA = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.FLOAT)
        @JvmStatic var SCALE_MODIFIER = SynchedEntityData.defineId(PokemonEntity::class.java, EntityDataSerializers.FLOAT)
        @JvmStatic var TERASTALLIZED = SynchedEntityData.defineId(PokemonEntity::class.java, IdentifierDataSerializer)

        const val BATTLE_LOCK = "battle"
        const val EVOLUTION_LOCK = "evolving"

        const val FALL_DAMAGE_MULT_FLYINGTYPE = 0.5f
        const val FALL_DAMAGE_MULT_MOVEMENT_FLY = 0.2f

        fun createAttributes(): AttributeSupplier.Builder = createLivingAttributes()
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.ATTACK_KNOCKBACK)
            .add(Attributes.ATTACK_DAMAGE)
            .add(Attributes.ARMOR)
            .add(Attributes.ARMOR_TOUGHNESS)
            // TODO: When jump strength is configured more thoroughly this should be updated to be dynamic based on jump strength.
            .add(Attributes.SAFE_FALL_DISTANCE, 5.0)
            .add(Attributes.GRAVITY)
    }

    val removalObservable = SimpleObservable<RemovalReason?>()

    /** A list of observable subscriptions related to this entity that need to be cleaned up when the entity is removed. */
    val subscriptions = mutableListOf<ObservableSubscription<*>>()

    override val schedulingTracker = SchedulingTracker()

    val form: FormData
        get() = pokemon.form
    val behaviour: FormPokemonBehaviour
        get() = form.behaviour

    /** Essentially a cached form of what was serialized to make memory reloads still work despite dynamic brain activities. */
    private var brainDynamic: Dynamic<*>? = null

    var pokemon: Pokemon = pokemon
        set(value) {
            value.isClient = this.level().isClientSide
            field = value
            delegate.changePokemon(value)
            refreshRiding()

            //This used to be referring to this.updateEyeHeight, I think this is the best conversion
            // We need to update this value every time the Pokémon changes, other eye height related things will be dynamic.
            this.refreshDimensions()
            if (!level().isClientSide) {
                remakeBrain()
            }
        }

    var despawner: Despawner<PokemonEntity> = Cobblemon.bestSpawner.defaultPokemonDespawner

    /** The player that caused this Pokémon to faint. */
    var killer: ServerPlayer? = null

    val isEvolving: Boolean
        get() = entityData.get(EVOLUTION_STARTED)
    var evolutionEntity: GenericBedrockEntity? = null

    var ticksLived = 0
    val busyLocks = mutableListOf<Any>()
    val isBusy: Boolean
        get() = busyLocks.isNotEmpty()
    val aspects: Set<String>
        get() = entityData.get(ASPECTS)
    var battleId: UUID?
        get() = entityData.get(BATTLE_ID).orElse(null)
        set(value) = entityData.set(BATTLE_ID, Optional.ofNullable(value))
    val battle: PokemonBattle?
        get() = battleId?.let { BattleRegistry.getBattle(it) }
    val isBattling: Boolean
        get() = entityData.get(BATTLE_ID).isPresent
    val friendship: Int
        get() = entityData.get(FRIENDSHIP)
    val seats: List<Seat>
        get() = form.riding.seats
    val rideProp: RidingProperties
        get() = form.riding
    var shownItem: ItemStack
        get() = entityData.get(SHOWN_HELD_ITEM)
        set(value) = entityData.set(SHOWN_HELD_ITEM, value)

    var lastLightningBoltUUID: UUID? = null

    var drops: DropTable? = null

    var tethering: PokemonPastureBlockEntity.Tethering? = null

    // TODO review if we still want this
    var queuedToDespawn = false

    var enablePoseTypeRecalculation = true

    val ridingAnimationData: RidingAnimationData = RidingAnimationData(this)
    var rideSoundManager: RideSoundManager = RideSoundManager(this)

    private val rideStatOverrides = mutableMapOf<RidingStyle, MutableMap<RidingStat, Double>>()
    var ridingController: RidingController? = null
        private set

    var previousRidingState: RidingBehaviourState? = null
        private set

    val runtime: MoLangRuntime by lazy {
        MoLangRuntime()
            .setup()
            .withQueryValue("entity", struct)
            .also {
                it.environment.query.addFunction("passenger_count") { DoubleValue(passengers.size.toDouble()) }
                it.environment.query.addFunction("ride_velocity") { DoubleValue(min(ridingAnimationData.velocitySpring.value.length() * 1.5,1.5)) }
                it.environment.query.addFunction("driver_input") { DoubleValue(min(ridingAnimationData.driverInputSpring.value.length(),1.0)) }
                it.environment.query.addFunction("get_ride_stats") { params ->
                    val rideStat = RidingStat.valueOf(params.getString(0).uppercase())
                    val rideStyle = RidingStyle.valueOf(params.getString(1).uppercase())
                    val maxVal = params.getDouble(2)
                    val minVal = params.getDouble(3)
                    DoubleValue(getRideStat(rideStat, rideStyle, minVal, maxVal))
                }
            }
    }

    fun refreshRiding() {
        pokemon.entity?.ejectPassengers()
        if (pokemon.form.riding.behaviours != null) {
            ridingController = RidingController(this, pokemon.form.riding.behaviours!!)
        }
        occupiedSeats = arrayOfNulls(seats.size)
    }

    /**
     * The amount of steps this entity has traveled.
     */
    var countsTowardsSpawnCap = true

    /**
     * 0 is do nothing,
     * 1 is appearing from a pokeball so needs to be small then grows,
     * 2 is 1 without extra animations like ball throwing and particles, used for pastures and wild capture fails
     * 3 is being captured/recalling so starts large and shrinks.
     */
    var beamMode: Int
        get() = entityData.get(BEAM_MODE).toInt()
        set(value) {
            entityData.set(BEAM_MODE, value.toByte())
        }

    var phasingTargetId: Int
        get() = entityData.get(PHASING_TARGET_ID)
        set(value) {
            entityData.set(PHASING_TARGET_ID, value)
        }

    /** The [SpawnCause] that created it, if this was the result of the [BestSpawner]. Note: This will be wiped by chunk-unload. */
    var spawnCause: SpawnCause? = null

    // properties like the above are synced and can be subscribed to for changes on either side

    override val delegate = if (world.isClientSide) {
        PokemonClientDelegate()
    } else {
        PokemonServerDelegate()
    }

    /** The effects that are modifying this entity. */
    var effects: EffectTracker = EffectTracker(this)

    /** The species exposed to the client and used on entity spawn. */
    val exposedSpecies: Species get() = this.effects.mockEffect?.exposedSpecies ?: this.pokemon.species

    /** The form exposed to the client and used for calculating hitbox and height. */
    val exposedForm: FormData get() = this.effects.mockEffect?.exposedForm ?: this.pokemon.form

    /** The aspects exposed to the client */
    val exposedAspects: Set<String>
        get() = this.effects.mockEffect?.exposedForm?.aspects?.toSet() ?: this.pokemon.aspects

    /** The pokeball exposed to the client. Used for sendout animation. */
    val exposedBall: PokeBall get() = this.effects.mockEffect?.exposedBall ?: this.pokemon.caughtBall

    override var behavioursAreCustom = false
    override val behaviours = mutableListOf<ResourceLocation>()
    override val registeredVariables = mutableListOf<MoLangConfigVariable>()
    override var config = VariableStruct()
    override var data = VariableStruct()
    override var callbacks = EntityCallbacks(this)

    var platform: PlatformType
        get() = entityData.get(PLATFORM_TYPE)
        set(value) {
            entityData.set(PLATFORM_TYPE, value)
        }

    override val struct: ObjectValue<PokemonEntity> = ObjectValue(this).also {
        it.addStandardFunctions()
            .addEntityFunctions(this)
            .addLivingEntityFunctions(this)
            .addPokemonFunctions(pokemon)
            .addPokemonEntityFunctions(this)
    }

    var flyDistO = 0F
    var isPokemonWalking = false
    var isPokemonFlying = false

    var tickSpawned = 0

    var occupiedSeats = arrayOfNulls<Entity>(seats.size)

    init {
        delegate.initialize(this)
        delegate.changePokemon(pokemon)
        addPosableFunctions(struct)
        moveControl = PokemonMoveControl(this)
        if (!level().isClientSide) {
            remakeBrain()
        }
        refreshDimensions()
        refreshRiding()
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(SPECIES, "")
        builder.define(NICKNAME, Component.empty())
        builder.define(NICKNAME_VISIBLE, true)
        builder.define(MARK, "")
        builder.define(SHOULD_RENDER_NAME, true)
        builder.define(MOVING, false)
        builder.define(BEHAVIOUR_FLAGS, 0)
        builder.define(BEAM_MODE, 0)
        builder.define(PLATFORM_TYPE, PlatformType.NONE)
        builder.define(PHASING_TARGET_ID, -1)
        builder.define(BATTLE_ID, Optional.empty())
        builder.define(ASPECTS, emptySet())
        builder.define(DYING_EFFECTS_STARTED, false)
        builder.define(POSE_TYPE, PoseType.STAND)
        builder.define(LABEL_LEVEL, 1)
        builder.define(HIDE_LABEL, false)
        builder.define(UNBATTLEABLE, false)
        builder.define(SPAWN_DIRECTION, this.random.nextIntBetweenInclusive(-180_000, 180_000) / 1000F)
        builder.define(COUNTS_TOWARDS_SPAWN_CAP, true)
        builder.define(FRIENDSHIP, 0)
        builder.define(FREEZE_FRAME, -1F)
        builder.define(CAUGHT_BALL, "")
        builder.define(EVOLUTION_STARTED, false)
        builder.define(SHOWN_HELD_ITEM, ItemStack.EMPTY)
        builder.define(RIDE_BOOSTS, emptyMap())
        builder.define(RIDE_STAMINA, 1F)
        builder.define(SCALE_MODIFIER, 1F)
        builder.define(TERASTALLIZED, cobblemonResource("none"))
    }

    override fun onSyncedDataUpdated(data: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(data)
        // "But it's imposs-" shut up nerd, it happens during super construction and that's before delegate is assigned by class construction
        if (delegate != null) {
            delegate.onSyncedDataUpdated(data)
        }

        // common SynchedEntityData handling
        when (data) {
            SPECIES -> refreshDimensions()
            POSE_TYPE -> {
                val value = entityData.get(data) as PoseType
                isNoGravity = (value in NO_GRAV_POSES) && passengers.isEmpty()
                val hasChestAspect = pokemon.aspects.any {
                    it == "chest" || it == "chest2" || it == "chest3"
                }
                if ((value in NO_GRAV_POSES) && passengers.isEmpty()) {
                    isNoGravity = true
                } else if (!hasChestAspect) {
                    isNoGravity = false
                }
            }

            BATTLE_ID -> {
                if (battleId != null) {
                    busyLocks.remove(BATTLE_LOCK) // Remove in case it's hopped across to another battle, don't want extra battle locks
                    busyLocks.add(BATTLE_LOCK)
                    brain.setMemory(CobblemonMemories.POKEMON_BATTLE, battleId)
                } else {
                    busyLocks.remove(BATTLE_LOCK)
                    brain.eraseMemory(CobblemonMemories.POKEMON_BATTLE)
                }
            }

            EVOLUTION_STARTED -> {
                if (isEvolving) {
                    busyLocks.remove(EVOLUTION_LOCK)
                    busyLocks.add(EVOLUTION_LOCK)
                } else {
                    busyLocks.remove(EVOLUTION_LOCK)
                }
            }

            SCALE_MODIFIER -> refreshDimensions()
        }
    }

    override fun canStandOnFluid(state: FluidState): Boolean {
        // If the pokemon is currently ridden then return false to prevent mounts that can transition
        // from the air to the water from getting stuck on the water surface.
        if (this.passengers.filterIsInstance<LivingEntity>().isNotEmpty() && this.controllingPassenger != null) return false

        return if (state.`is`(FluidTags.WATER) && !isEyeInFluid(FluidTags.WATER)) {
            exposedForm.behaviour.moving.swim.canWalkOnWater || platform != PlatformType.NONE
        } else if (state.`is`(FluidTags.LAVA) && !isEyeInFluid(FluidTags.LAVA)) {
            exposedForm.behaviour.moving.swim.canWalkOnLava
        } else {
            super.canStandOnFluid(state)
        }
    }

    override fun canSprint() = true

    override fun handleEntityEvent(status: Byte) {
        delegate.handleStatus(status)
        super.handleEntityEvent(status)
    }

    fun terastallize(type: TeraType?) {
        if (type != null) {
            entityData.set(TERASTALLIZED, type.id)
            setGlowingTag(true)
        }
        else {
            entityData.set(TERASTALLIZED, cobblemonResource("none"))
            setGlowingTag(false)
        }
    }

    fun getTerastallization(): TeraType? {
        val teraId = entityData.get(TERASTALLIZED)
        if (teraId == cobblemonResource("id")) return null
        return TeraTypes.get(teraId)
    }

    override fun sendDebugPackets() {
        super.sendDebugPackets()
        DebugPackets.sendEntityBrain(this)
        DebugPackets.sendGoalSelector(level(), this, this.goalSelector)
        DebugPackets.sendPathFindingPacket(
            level(),
            this,
            this.navigation.path,
            this.navigation.path?.distToTarget ?: 0F
        )
    }

    public override fun removePassenger(passenger: Entity) {
        val passengerIndex = occupiedSeats.indexOf(passenger)
        if (passengerIndex != -1) {
            occupiedSeats[passengerIndex] = null
        }
        if (level().isClientSide) {
            MountedCameraTypeHandler.handleDismount(passenger, this)
        }
        super.removePassenger(passenger)
        if (passengers.isEmpty()) {
            ifRidingAvailable { _, _, state -> pokemon.rideStamina = state.stamina.get() }
            ridingController?.context?.state?.reset()
            ridingAnimationData.clear()

            // reset riding orientation
            if (this is OrientationControllable) {
                this.orientationController.reset()
            }
        }
    }

    override fun thunderHit(level: ServerLevel, lightning: LightningBolt) {
        // Ground types shouldn't take lightning damage
        val isTypeImmune = ElementalTypes.GROUND in pokemon.types

        // Deals with special cases in which Pokemon should either be immune or buffed by lightning strikes.
        val isAbilityImmune = when (pokemon.ability.name) {
            "lightningrod" -> {
                this.addEffect(MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1))
                true
            }
            "motordrive" -> {
                this.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1))
                true
            }
            "voltabsorb" -> {
                this.addEffect(MobEffectInstance(MobEffects.HEAL, 1, 1))
                true
            }
            else -> false
        }

        // Lightning hits entities multiple times, if this Pokémon changed a feature because of this lighting strike it should be immune to all remaining hits of this specific lightning strike as well.
        var rotated = this.lastLightningBoltUUID == lightning.uuid
        if (!rotated && pokemon.form.behaviour.lightningHit.isSpecial()) {
            for (rotateFeature in pokemon.form.behaviour.lightningHit.rotateFeatures) {
                val feature: StringSpeciesFeature = pokemon.getFeature(rotateFeature.key) ?: continue
                val index = rotateFeature.chain.indexOf(feature.value)
                // index is -1 if the element wasn't found, i.e. here if the feature's value is not part of the chain
                if (index < 0) continue

                val next = index.inc()
                val nextIndex = if (next < rotateFeature.chain.size) next else 0
                feature.value = rotateFeature.chain[nextIndex]
                pokemon.markFeatureDirty(feature)
                rotated = true
            }

            if (rotated) {
                this.lastLightningBoltUUID = lightning.uuid
                this.playSound(SoundEvents.MOOSHROOM_CONVERT, 2.0F, 1.0F)
                pokemon.updateAspects()
            }
        }

        if (!isTypeImmune && !isAbilityImmune && !rotated) {
            super.thunderHit(level, lightning)
        }
    }


    override fun tick() {
        /* Addresses watchdog hanging that is completely bloody inexplicable. */
        yBodyRot = Mth.wrapDegrees(yBodyRot)
        yBodyRotO = Mth.wrapDegrees(yBodyRotO)
        yRot = Mth.wrapDegrees(yRot)
        yRotO = Mth.wrapDegrees(yRotO)
        xRot = Mth.wrapDegrees(xRot)
        xRotO = Mth.wrapDegrees(xRotO)
        yHeadRot = Mth.wrapDegrees(yHeadRot)
        yHeadRotO = Mth.wrapDegrees(yHeadRotO)
        /* I'm sure it's not even us but something altering the logic of the loops in LivingEntity */

        super.tick()

        isPokemonFlying = flyDist - flyDistO > 0.005F
        isPokemonWalking = walkDist - walkDistO > 0.005F

        if (passengers.isNotEmpty() && level().isClientSide) {
            rideSoundManager.tick()
            ridingAnimationData.update()
        } else if (!passengers.isNotEmpty() && level().isClientSide) {
            rideSoundManager.stop()
        }

        flyDistO = flyDist

        ridingController?.tick()

        if (isBattling) {
            // Deploy a platform if a non-wild Pokemon is touching water but not underwater.
            // This can't be done in the BattleMovementGoal as the sleep goal will override it.
            // Clients also don't seem to have correct info about behavior
            if (!level().isClientSide && ticksLived > 5) {
                if (platform == PlatformType.NONE
                        && ownerUUID != null
                        && isInWater && !isUnderWater
                        && !exposedForm.behaviour.moving.swim.canBreatheUnderwater && !exposedForm.behaviour.moving.swim.canWalkOnWater
                        && !getBehaviourFlag(PokemonBehaviourFlag.FLYING)
                ) {
                    platform = PlatformType.getPlatformTypeForPokemon((exposedForm))
                } else if (platform != PlatformType.NONE && onGround()) {
                    // If the pokemon is on a non-fluid surface, remove the platform.
                    platform = PlatformType.NONE
                }
            }

        } else {
            // Battle clone destruction
            if (this.beamMode == 0 && this.isBattleClone()) {
                discard()
                return
            }
        }

        // We will be handling idle logic ourselves thank you
        this.setNoActionTime(0)
        if (queuedToDespawn) {
            return remove(RemovalReason.DISCARDED)
        }
        if (evolutionEntity != null) {
            evolutionEntity!!.setPos(pokemon.entity!!.x, pokemon.entity!!.y, pokemon.entity!!.z)
            pokemon.entity!!.navigation.stop()
        }
        delegate.tick(this)
        ticksLived++

        if (ticksLived <= 20) {
            clearRestriction()
            val spawnDirection = entityData.get(SPAWN_DIRECTION).takeIf { it.isFinite() } ?: 0F
            yBodyRot = (spawnDirection * 1000F).toInt() / 1000F
        }

        if (this.tethering != null && !this.tethering!!.box.contains(this.x, this.y, this.z)) {
            this.tethering = null
            this.pokemon.recall()
        }

        jumping = false

        //This is so that pokemon in the pasture block are ALWAYS in sync with the pokemon box
        //Before, pokemon entities in pastures would hold an old ref to a pokemon obj and changes to that would not appear to the underlying file
        if (this.tethering != null && age % 20 == 0) {
            // Only for online players
            this.ownerUUID?.let { ownerUUID ->
                val player = level().getPlayerByUUID(ownerUUID) as? ServerPlayer
                if (player != null) {
                    // TODO: A proper patch for this
                    try {
                        val actualPokemon = Cobblemon.storage.getPC(player)[this.pokemon.uuid]
                        actualPokemon?.let {
                            if (it !== pokemon) {
                                pokemon = it
                            }
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        previousRidingState = ridingController?.context?.state?.copy()
        if (!this.pokemon.isWild() && this.pokemon.originalTrainerType != OriginalTrainerType.NPC && this.pokemon.getOwnerPlayer() == null) {
            this.tethering = null
            this.pokemon.recall()
        }
        schedulingTracker.update(1 / 20F)
    }

    override fun customServerAiStep() {
        this.getBrain().tick(level() as ServerLevel, this)
//        PokemonBrain.updateActivities(this)
        super.customServerAiStep()
    }

    fun getRideVelocity(): Vec3 {
        return this.ridingAnimationData.velocitySpring.value
        //return this.position().subtract(prevPosition)
    }

    fun setMoveControl(moveControl: MoveControl) {
        this.moveControl = moveControl
    }

    /**
     * Prevents water type Pokémon from taking drowning damage.
     */
    override fun canBreatheUnderwater(): Boolean {
        return behaviour.moving.swim.canBreatheUnderwater
    }

    /**
     * Prevents fire type Pokémon from taking fire damage.
     */
    override fun fireImmune(): Boolean {
        return pokemon.isFireImmune()
    }

    override fun calculateFallDamage(f: Float, g: Float): Int {
        return Mth.ceil((f * 0.5f - 3.0f) * g)
    }

    /**
     * Prevents flying type Pokémon from taking fall damage.
     */
    override fun causeFallDamage(fallDistance: Float, damageMultiplier: Float, damageSource: DamageSource): Boolean {
        /*return if (pokemon.ability.name == "levitate") {
            false
        } else {
            val flying_type = ElementalTypes.FLYING in pokemon.types;
            val flying_movement = pokemon.species.behaviour.moving.fly.canFly;
            // Reduce fall damage in case the Pokémon is either a FLYING type or uses flying movement.
            val damageMultiplier = damageMultiplier * when {
                ElementalTypes.FLYING in pokemon.types -> FALL_DAMAGE_MULT_FLYINGTYPE
                pokemon.species.behaviour.moving.fly.canFly -> FALL_DAMAGE_MULT_MOVEMENT_FLY
                else -> { 1.0f }
            }*/
        return super.causeFallDamage(fallDistance, damageMultiplier, damageSource)
    }

    override fun isInvulnerableTo(damageSource: DamageSource): Boolean {
        if (this.pokemon.species.name.lowercase() == "trainer") {
            return true
        }
        // If the entity is busy, it cannot be hurt.
        if (busyLocks.isNotEmpty()) {
            return true
        }

        // Don't let Pokémon be hurt during sendout and recall animations
        if (beamMode != 0) {
            return true
        }

        // Owned Pokémon cannot be hurt by players or suffocation
        if (ownerUUID != null && (damageSource.entity is Player || damageSource.`is`(DamageTypes.IN_WALL))) {
            return true
        }

        if (!Cobblemon.config.playerDamagePokemon && damageSource.entity is Player) {
            return true
        }

        return super.isInvulnerableTo(damageSource)
    }

    /**
     * A utility method that checks if this Pokémon has the [UncatchableProperty.uncatchable] property.
     *
     * @return If the Pokémon is uncatchable.
     */
    fun isUncatchable() = pokemon.isUncatchable()

    /**
     * A utility method that checks if this Pokémon has the [UncatchableProperty.uncatchable] property.
     *
     * @return If the Pokémon is uncatchable.
     */
    fun isBattleClone() = pokemon.isBattleClone()

    fun recallWithAnimation(): CompletableFuture<Pokemon> {
        val owner = owner ?: pokemon.getOwnerEntity()
        val future = CompletableFuture<Pokemon>()
        if (entityData.get(PHASING_TARGET_ID) == -1 && owner != null) {
            val preamble = if (owner is PokemonSender) {
                owner.recalling(this)
            } else {
                CompletableFuture.completedFuture(Unit)
            }

            preamble.thenAccept {
                owner.level().playSoundServer(position(), CobblemonSounds.POKE_BALL_RECALL, volume = 0.6F)
                entityData.set(PHASING_TARGET_ID, owner.id)
                entityData.set(BEAM_MODE, 3)
                ejectPassengers()
                val state = pokemon.state

                // Let the Pokémon be intangible during recall
                noPhysics = true
                // This doesn't appear to actually prevent a livingEntity from falling, but is here as a precaution
                isNoGravity = true

                afterOnServer(seconds = SEND_OUT_DURATION) {
                    // only recall if the Pokémon hasn't been recalled yet for this state
                    if (state == pokemon.state) {
                        pokemon.recall()
                    }
                    if (owner is NPCEntity) {
                        owner.after(seconds = 1F) {
                            future.complete(pokemon)
                        }
                    } else {
                        future.complete(pokemon)
                    }
                }
            }
        } else {
            pokemon.recall()
            future.complete(pokemon)
        }

        return future
    }

    override fun saveWithoutId(nbt: CompoundTag): CompoundTag {
        val tethering = this.tethering
        if (tethering != null) {
            val tetheringNbt = CompoundTag()
            tetheringNbt.putUUID(DataKeys.TETHERING_ID, tethering.tetheringId)
            tetheringNbt.putUUID(DataKeys.POKEMON_UUID, tethering.pokemonId)
            tetheringNbt.putUUID(DataKeys.POKEMON_OWNER_ID, tethering.playerId)
            tetheringNbt.putUUID(DataKeys.PC_ID, tethering.pcId)
            tetheringNbt.put(DataKeys.TETHER_MIN_ROAM_POS, NbtUtils.writeBlockPos(tethering.minRoamPos))
            tetheringNbt.put(DataKeys.TETHER_MAX_ROAM_POS, NbtUtils.writeBlockPos(tethering.maxRoamPos))
            tetheringNbt.put(DataKeys.TETHER_PASTURE_POS, NbtUtils.writeBlockPos(tethering.pasturePos))
            nbt.put(DataKeys.TETHERING, tetheringNbt)
        } else {
            nbt.put(DataKeys.POKEMON, pokemon.saveToNBT(registryAccess()))
        }
        val battleIdToSave = battleId
        if (battleIdToSave != null) {
            nbt.putUUID(DataKeys.POKEMON_BATTLE_ID, battleIdToSave)
        }
        nbt.putString(DataKeys.POKEMON_POSE_TYPE, entityData.get(POSE_TYPE).name)
        nbt.putByte(DataKeys.POKEMON_BEHAVIOUR_FLAGS, entityData.get(BEHAVIOUR_FLAGS))

        saveScriptingToNBT(nbt)

        if (entityData.get(HIDE_LABEL)) {
            nbt.putBoolean(DataKeys.POKEMON_HIDE_LABEL, true)
        }
        if (entityData.get(UNBATTLEABLE)) {
            nbt.putBoolean(DataKeys.POKEMON_UNBATTLEABLE, true)
        }
        if (!countsTowardsSpawnCap) {
            nbt.putBoolean(DataKeys.POKEMON_COUNTS_TOWARDS_SPAWN_CAP, false)
        }
        if (entityData.get(FREEZE_FRAME) != -1F) {
            nbt.putFloat(DataKeys.POKEMON_FREEZE_FRAME, entityData.get(FREEZE_FRAME))
        }
        if (!enablePoseTypeRecalculation) {
            nbt.putBoolean(DataKeys.POKEMON_RECALCULATE_POSE, enablePoseTypeRecalculation)
        }
        val dataResult = this.brain.serializeStart(NbtOps.INSTANCE)
        dataResult.resultOrPartial(::error).ifPresent { brain ->
            nbt.put("Brain", brain)
        }
        nbt.putFloat(DataKeys.POKEMON_SCALE_MODIFIER, entityData.get(SCALE_MODIFIER))

        // save active effects
        nbt.put(DataKeys.ENTITY_EFFECTS, effects.saveToNbt(this.level().registryAccess()))

        CobblemonEvents.POKEMON_ENTITY_SAVE.post(PokemonEntitySaveEvent(this, nbt))

        return super.saveWithoutId(nbt)
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        if (nbt.contains(DataKeys.TETHERING)) {
            val tetheringNBT = nbt.getCompound(DataKeys.TETHERING)
            val tetheringId = tetheringNBT.getUUID(DataKeys.TETHERING_ID)
            val pcId = tetheringNBT.getUUID(DataKeys.PC_ID)
            val pokemonId = tetheringNBT.getUUID(DataKeys.POKEMON_UUID)
            val playerId = tetheringNBT.getUUID(DataKeys.POKEMON_OWNER_ID)
            val minRoamPos = NbtUtils.readBlockPos(tetheringNBT, DataKeys.TETHER_MIN_ROAM_POS).get()
            val maxRoamPos = NbtUtils.readBlockPos(tetheringNBT, DataKeys.TETHER_MAX_ROAM_POS).get()
            val pasturePos = NbtUtils.readBlockPos(tetheringNBT, DataKeys.TETHER_PASTURE_POS).get()

            try {
                val loadedPokemon = Cobblemon.storage.getPC(pcId, registryAccess())[pokemonId]
                if (loadedPokemon != null && loadedPokemon.tetheringId == tetheringId) {
                    pokemon = loadedPokemon
                    tethering = PokemonPastureBlockEntity.Tethering(
                        minRoamPos = minRoamPos,
                        maxRoamPos = maxRoamPos,
                        playerId = playerId,
                        playerName = "",
                        tetheringId = tetheringId,
                        pokemonId = pokemonId,
                        pcId = pcId,
                        entityId = id, // Doesn't really matter on the entity
                        pasturePos = pasturePos
                    )
                } else {
                    pokemon = this.createSidedPokemon()
                    health = 0F
                }
            } catch (e: Exception) {
                this.discard()
                return
            }
        } else if (pokemon.storeCoordinates.get() == null) {
            // when the vanilla /data merge command is used, it will also run through this load method
            // and if we are not careful here, the pokemon instance will get rebuilt from scratch
            // this will fuck with storages, as they are tied to these very pokemon instances and their observables
            val ops = registryAccess().createSerializationContext(NbtOps.INSTANCE)
            pokemon = try {
                this.sidedCodec().decode(ops, nbt.getCompound(DataKeys.POKEMON)).orThrow.first
            } catch (_: IllegalStateException) {
                health = 0F
                this.createSidedPokemon()
            }
        }

        val savedBattleId = if (nbt.hasUUID(DataKeys.POKEMON_BATTLE_ID)) nbt.getUUID(DataKeys.POKEMON_BATTLE_ID) else null
        if (savedBattleId != null) {
            val battle = BattleRegistry.getBattle(savedBattleId)
            if (battle != null) {
                battleId = savedBattleId
            }
        }

        loadScriptingFromNBT(nbt)

        // apply active effects
        if (nbt.contains(DataKeys.ENTITY_EFFECTS)) effects.loadFromNBT(
            nbt.getCompound(DataKeys.ENTITY_EFFECTS),
            this.level().registryAccess()
        )

        // init SynchedEntityData
        entityData.set(SPECIES, effects.mockEffect?.mock?.species ?: pokemon.species.resourceIdentifier.toString())
        entityData.set(NICKNAME, pokemon.nickname ?: Component.empty())
        entityData.set(MARK, pokemon.activeMark?.identifier.toString())
        entityData.set(LABEL_LEVEL, pokemon.level)
        entityData.set(POSE_TYPE, PoseType.valueOf(nbt.getString(DataKeys.POKEMON_POSE_TYPE)))
        entityData.set(BEHAVIOUR_FLAGS, nbt.getByte(DataKeys.POKEMON_BEHAVIOUR_FLAGS))
        if (nbt.contains(DataKeys.POKEMON_FREEZE_FRAME)) {
            entityData.set(FREEZE_FRAME, nbt.getFloat(DataKeys.POKEMON_FREEZE_FRAME))
        }

        if (nbt.contains(DataKeys.POKEMON_HIDE_LABEL)) {
            entityData.set(HIDE_LABEL, nbt.getBoolean(DataKeys.POKEMON_HIDE_LABEL))
        }
        if (nbt.contains(DataKeys.POKEMON_UNBATTLEABLE)) {
            entityData.set(UNBATTLEABLE, nbt.getBoolean(DataKeys.POKEMON_UNBATTLEABLE))
        }
        if (nbt.contains(DataKeys.POKEMON_COUNTS_TOWARDS_SPAWN_CAP)) {
            countsTowardsSpawnCap = nbt.getBoolean(DataKeys.POKEMON_COUNTS_TOWARDS_SPAWN_CAP)
        }
        if (nbt.contains(DataKeys.POKEMON_RECALCULATE_POSE)) {
            enablePoseTypeRecalculation = nbt.getBoolean(DataKeys.POKEMON_RECALCULATE_POSE)
        }

        if (nbt.contains(DataKeys.POKEMON_PLATFORM_TYPE)) {
            entityData.set(PLATFORM_TYPE, PlatformType.valueOf(nbt.getString(DataKeys.POKEMON_PLATFORM_TYPE)))
        }

        if (nbt.contains("Brain", 10)) {
            this.brain = this.makeBrain(Dynamic(NbtOps.INSTANCE, nbt.get("Brain")))
        }

        remakeBrain()

        if (nbt.contains(DataKeys.POKEMON_SCALE_MODIFIER)) {
            entityData.set(SCALE_MODIFIER, nbt.getFloat(DataKeys.POKEMON_SCALE_MODIFIER))
        }

        CobblemonEvents.POKEMON_ENTITY_LOAD.postThen(
            event = PokemonEntityLoadEvent(this, nbt),
            ifSucceeded = {},
            ifCanceled = { this.discard() }
        )
    }

    override fun getAddEntityPacket(entityTrackerEntry: ServerEntity): Packet<ClientGamePacketListener> =
        ClientboundCustomPayloadPacket(
            SpawnPokemonPacket(
                this,
                super.getAddEntityPacket(entityTrackerEntry) as ClientboundAddEntityPacket
            )
        ) as Packet<ClientGamePacketListener>

    override fun getPathfindingMalus(nodeType: PathType): Float {
        /* This used to be 2 because I wanted to deprioritize flight for land-fly pokemon but it breaks new wandering */
        /* LandRandomPos#movePosUpOutOfSolid tries to fix blocks by moving to where the malus is zero. */
        return if (nodeType == PathType.OPEN) 0F else super.getPathfindingMalus(nodeType)
//        return super.getPathfindingMalus(nodeType)
        //        return if (nodeType == PathType.OPEN) 2F else super.getPathfindingMalus(nodeType)

    }

    override fun getNavigation() = navigation as OmniPathNavigation
    override fun createNavigation(world: Level) = OmniPathNavigation(world, this)

    override fun makeBrain(dynamic: Dynamic<*>): Brain<PokemonEntity> {
        this.brainDynamic = dynamic
        val target = pokemon
        if (target != null) {
            PokemonBrain.applyBrain(this, target, dynamic)
            return getBrain()
        } else {
            // Look around, nobody cares.
            val brain = brainProvider().makeBrain(dynamic)
            return brain
        }
    }

    override fun remakeBrain() {
        brain = makeBrain(brainDynamic ?: makeEmptyBrainDynamic())
    }

    override fun assignNewBrainWithMemoriesAndSensors(
        dynamic: Dynamic<*>,
        memories: Set<MemoryModuleType<*>>,
        sensors: Set<SensorType<*>>
    ): Brain<PokemonEntity> {
        val allSensors = BuiltInRegistries.SENSOR_TYPE.toSet().filterIsInstance<SensorType<Sensor<in PokemonEntity>>>()
        val brain = Brain.provider(
            memories,
            allSensors.filter { it in sensors }.toSet()
        ).makeBrain(dynamic)
        this.brain = brain
        return brain
    }

    // cast is safe, mojang do the same thing.
    override fun getBrain(): Brain<PokemonEntity> = super.getBrain() as Brain<PokemonEntity>

    // Won't be the final call but Mojang is very confident we'll use their same structure. Think again, bucko.
    override fun brainProvider(): Brain.Provider<PokemonEntity> = Brain.provider(PokemonBrain.MEMORY_MODULES, PokemonBrain.SENSORS)

    override fun onPathfindingDone() {
        super.onPathfindingDone()
        (moveControl as PokemonMoveControl).stop()
    }

//
//    @Suppress("SENSELESS_COMPARISON")
//    public override fun initGoals() {
//        // DO NOT REMOVE
//        // LivingEntity#getActiveEyeHeight is called in the constructor of Entity
//        // Pokémon param is not available yet
//        if (this.pokemon == null) {
//            return
//        }
//        goalSelector.add(1, PokemonBreatheAirGoal(this))
//        goalSelector.add(2, PokemonFloatToSurfaceGoal(this))
//        goalSelector.add(4, PokemonMoveIntoFluidGoal(this))
//
//        if (pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED) != null) {
//            goalSelector.add(5, EatGrassGoal(this))
//        }


    fun canSleepAt(pos: BlockPos): Boolean {
        val rest = behaviour.resting
        val world = level() as ServerLevel
        val light = world.getLightEmission(pos)
        val blockState = world.getBlockState(pos)
        val block = blockState.block
        val biome = world.getBiome(pos).value()
        val fluid = world.getFluidState(pos.above()).type
        val seesSky = world.canSeeSky(pos.above())
        val fits = true
        val canStayAt = world.canEntityStayAt(pos, ceil(bbWidth).toInt(), ceil(bbHeight).toInt(), PositionType.LAND)

        return light in rest.light &&
                (rest.skyLight == null || world.lightEngine.getLayerListener(LightLayer.SKY).getLightValue(pos) in rest.skyLight) &&
                (rest.blocks.isEmpty() || rest.blocks.any { it.fits(block, world.blockRegistry) }) &&
                (rest.biomes.isEmpty() || rest.biomes.any { it.fits(biome, world.biomeRegistry) }) &&
                ((fluid is EmptyFluid && rest.fluids.isEmpty()) || rest.fluids.any { it.fits(fluid, world.fluidRegistry) }) &&
                (rest.canSeeSky == null || rest.canSeeSky == seesSky) &&
                fits &&
                canStayAt
    }

    override fun getBreedOffspring(serverLevel: ServerLevel, ageableMob: AgeableMob) = null

    override fun canSitOnShoulder(): Boolean {
        return pokemon.form.shoulderMountable
    }

    override fun wantsToPickUp(stack: ItemStack): Boolean {
        val pickupItems = config.getObjectList<ObtainableItem>(PokemonItemSensor.PICKUP_ITEMS)
        return this.canHoldItem(stack) &&
                (pickupItems.findMatchingEntry(registryAccess(), stack)?.pickupPriority
                    ?: 0) > (pickupItems.findMatchingEntry(registryAccess(), this.pokemon.heldItem)?.pickupPriority ?: 0)
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        if (!this.isBattling && this.isBattleClone()) {
            return InteractionResult.FAIL
        }
        val itemStack = player.getItemInHand(hand)
        val colorFeatureType = SpeciesFeatures.getFeaturesFor(pokemon.species)
            .find { it is ChoiceSpeciesFeatureProvider && DataKeys.CAN_BE_COLORED in it.keys }
        val colorFeature = pokemon.getFeature<StringSpeciesFeature>(DataKeys.CAN_BE_COLORED)

        if (ownerUUID == player.uuid || ownerUUID == null) {
            if (itemStack.`is`(Items.SHEARS) && this.readyForShearing()) {
                this.shear(SoundSource.PLAYERS)
                this.gameEvent(GameEvent.SHEAR, player)
                itemStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)
                return InteractionResult.SUCCESS
            } else if (itemStack.`is`(Items.BOWL)) {
                if (pokemon.aspects.any { it.contains("mooshtank") }) {
                    player.playSound(SoundEvents.MOOSHROOM_MILK, 1.0f, 1.0f)
                    // if the Mooshtank ate a Flower beforehand
                    if (pokemon.lastFlowerFed != ItemStack.EMPTY && pokemon.aspects.any { it.contains("mooshtank-brown") }) {
                        SuspiciousEffectHolder.tryGet(pokemon.lastFlowerFed.item)?.let {
                            // modify the suspicious stew with the effect
                            val susStewStack = Items.SUSPICIOUS_STEW.defaultInstance.copy()
                            susStewStack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, it.suspiciousEffects)
                            val susStewEffect = ItemUtils.createFilledResult(itemStack, player, susStewStack)
                            //give player modified Suspicious Stew
                            player.setItemInHand(hand, susStewEffect)
                            // reset the flower fed state
                            pokemon.lastFlowerFed = ItemStack.EMPTY
                        }
                        return InteractionResult.sidedSuccess(level().isClientSide)
                    } else {
                        val mushroomStew =
                            ItemUtils.createFilledResult(itemStack, player, Items.MUSHROOM_STEW.defaultInstance)
                        player.setItemInHand(hand, mushroomStew)
                        return InteractionResult.sidedSuccess(level().isClientSide)
                    }
                }
            }
            // Flowers used on brown MooshTanks
            else if (itemStack.`is`(Items.ALLIUM) ||
                itemStack.`is`(Items.AZURE_BLUET) ||
                itemStack.`is`(Items.BLUE_ORCHID) ||
                itemStack.`is`(Items.DANDELION) ||
                itemStack.`is`(Items.CORNFLOWER) ||
                itemStack.`is`(Items.LILY_OF_THE_VALLEY) ||
                itemStack.`is`(Items.OXEYE_DAISY) ||
                itemStack.`is`(Items.POPPY) ||
                itemStack.`is`(Items.TORCHFLOWER) ||
                itemStack.`is`(Items.PINK_TULIP) ||
                itemStack.`is`(Items.RED_TULIP) ||
                itemStack.`is`(Items.WHITE_TULIP) ||
                itemStack.`is`(Items.ORANGE_TULIP) ||
                itemStack.`is`(Items.WITHER_ROSE) ||
                itemStack.`is`(CobblemonItems.PEP_UP_FLOWER)
            ) {
                if (pokemon.aspects.any { it.contains("mooshtank-brown") }) {
                    player.playSound(SoundEvents.MOOSHROOM_EAT, 1.0f, 1.0f)
                    pokemon.lastFlowerFed = itemStack
                    return InteractionResult.sidedSuccess(level().isClientSide)
                }
            } else if (!player.isShiftKeyDown && StashHandler.interactMob(player, pokemon, itemStack)) {
                return InteractionResult.SUCCESS
            } else if (itemStack.item is DyeItem && colorFeatureType != null) {
                val currentColor = colorFeature?.value ?: ""
                val item = itemStack.item as DyeItem
                if (!item.dyeColor.name.equals(currentColor, ignoreCase = true)) {
                    if (player is ServerPlayer) {
                        if (colorFeature != null) {
                            colorFeature.value = item.dyeColor.name.lowercase()
                            this.pokemon.markFeatureDirty(colorFeature)
                        } else {
                            val newColorFeature =
                                StringSpeciesFeature(DataKeys.CAN_BE_COLORED, item.dyeColor.name.lowercase())
                            this.pokemon.features.add(newColorFeature)
                            this.pokemon.onChange()
                            this.pokemon.onChange()
                        }

                        this.pokemon.updateAspects()
                        itemStack.consume(1, player)
                    }
                    return InteractionResult.sidedSuccess(level().isClientSide)
                }
            } else if (itemStack.item.equals(Items.WATER_BUCKET) && colorFeatureType != null) {
                if (player is ServerPlayer) {
                    if (colorFeature != null) {
                        if (!player.hasInfiniteMaterials()) {
                            itemStack.shrink(1)
                            player.giveOrDropItemStack(Items.BUCKET.defaultInstance)
                        }
                        colorFeature.value = ""
                        this.pokemon.markFeatureDirty(colorFeature)
                        this.pokemon.updateAspects()
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide)
            } else if (itemStack.`is`(CobblemonItems.NPC_EDITOR) && (player is ServerPlayer) && player.isCreative) {
                BehaviourEditingTracker.startEditing(player, this)
                player.sendPacket(OpenBehaviourEditorPacket(id, (this as MoLangScriptingEntity).behaviours.toSet()))
                return InteractionResult.sidedSuccess(level().isClientSide)
            }
        }

        if (hand == InteractionHand.MAIN_HAND) {
            if (player.isShiftKeyDown && player is ServerPlayer) {
                showInteractionWheel(player, itemStack)
                return InteractionResult.sidedSuccess(level().isClientSide)
            }
            else if (this.ownerUUID == player.uuid && this.attemptItemInteraction(player, player.getItemInHand(hand))) {
                return InteractionResult.sidedSuccess(level().isClientSide)
            }
        }

        return super.mobInteract(player, hand)
    }

    private fun showInteractionWheel(player: ServerPlayer, itemStack: ItemStack) {
         val canRide = ifRidingAvailableSupply(false) { behaviour, settings, state ->
            if (platform != PlatformType.NONE) return@ifRidingAvailableSupply false
            if (tethering != null) return@ifRidingAvailableSupply false;
            if (seats.isEmpty()) return@ifRidingAvailableSupply false;
            if ((owner as? ServerPlayer)?.isInBattle() == true) return@ifRidingAvailableSupply false;
            if (this.owner != player && this.passengers.isEmpty()) return@ifRidingAvailableSupply false;
            return@ifRidingAvailableSupply behaviour.isActive(settings, state, this);
        }
        val canTransform = TransformationHandler.canTransform(this, player)
        if (pokemon.getOwnerPlayer() == player) {
            val cosmeticItemDefinition = CobblemonCosmeticItems.findValidCosmeticForPokemonAndItem(
                player.level().registryAccess(),
                pokemon,
                itemStack
            )

            InteractPokemonUIPacket(
                this.getUUID(),
                canSitOnShoulder() && pokemon in player.party(),
                !(pokemon.heldItemNoCopy().isEmpty && itemStack.isEmpty),
                (!pokemon.cosmeticItem.isEmpty && itemStack.isEmpty) || cosmeticItemDefinition != null,
                canRide,
                canTransform
            ).sendToPlayer(player)
        }
        else if (!pokemon.isWild() && canRide) {
            player.isShiftKeyDown = false
            tryRidingPokemon(player)
        }
    }

    override fun getDimensions(pose: Pose): EntityDimensions {
        val scale = effects.mockEffect?.scale ?: (form.baseScale * pokemon.scaleModifier)
        var result = this.exposedForm.hitbox.scale(scale)
        result = result.withEyeHeight(this.exposedForm.eyeHeight(this) * result.height)
        result = result.scale(this.scale)
        return result
    }

    override fun canBeSeenAsEnemy() = super.canBeSeenAsEnemy() && !isBusy

    override fun doHurtTarget(target: Entity): Boolean {
        if (beamMode != 0) return false
        return super.doHurtTarget(target)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        return if (super.hurt(source, amount)) {
            effects.mockEffect?.takeIf { it is IllusionEffect && this.battleId == null }?.end(this)

            if (this.health == 0F) {
                pokemon.currentHealth = 0
            } else if (this.ownerUUID != null) {
                // Only touch battle HP for non-wild Pokémon so that quick ball et. al. aren't owerpowered.
                pokemon.currentHealth = (pokemon.maxHealth * (this.health / this.maxHealth)).toInt()
            }
            true
        } else false
    }

    override fun shouldBeSaved(): Boolean {
        if (ownerUUID == null && !pokemon.isNPCOwned() && (Cobblemon.config.savePokemonToWorld || isPersistenceRequired)) {
            CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.postThen(PokemonEntitySaveToWorldEvent(this)) {
                return true
            }
        }
        return tethering != null
    }

    override fun checkDespawn() {
        if (pokemon.getOwnerUUID() == null && !isPersistenceRequired && despawner.shouldDespawn(this) ) {
            discard()
        }
    }

    override fun isPersistenceRequired(): Boolean {
        return super.isPersistenceRequired()
                || (this.pokemon.canDropHeldItem && !this.pokemon.heldItem.isEmpty)
                || this.brain.checkMemory(CobblemonMemories.HIVE_LOCATION, MemoryStatus.VALUE_PRESENT)
                || this.brain.checkMemory(CobblemonMemories.HIVE_COOLDOWN, MemoryStatus.VALUE_PRESENT)
                || this.brain.checkMemory(CobblemonMemories.NEARBY_SACC_LEAVES, MemoryStatus.VALUE_PRESENT)
    }

    fun setBehaviourFlag(flag: PokemonBehaviourFlag, on: Boolean) {
        entityData.set(BEHAVIOUR_FLAGS, setBitForByte(entityData.get(BEHAVIOUR_FLAGS), flag.bit, on))
    }

    fun getBehaviourFlag(flag: PokemonBehaviourFlag): Boolean =
        getBitForByte(entityData.get(BEHAVIOUR_FLAGS), flag.bit)

    fun getActiveBehaviourFlags(): Set<PokemonBehaviourFlag> {
        val flagsByte = this.entityData.get(BEHAVIOUR_FLAGS).toInt()
        return PokemonBehaviourFlag.entries.filterTo(mutableSetOf()) { flag ->
            (flagsByte and (1 shl (flag.bit - 1))) != 0
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun canBattle(player: Player): Boolean {
        if (entityData.get(UNBATTLEABLE)) {
            return false
        } else if (isBusy) {
            return false
        } else if (battleId != null) {
            return false
        } else if (ownerUUID != null) {
            return false
        } else if (health <= 0F || isDeadOrDying) {
            return false
        } else if (player.isPartyBusy()) {
            return false
        } else if (pokemon.species.name.lowercase() == "trainer") {
            return false
        }

        return true
    }

    /**
     * The level this entity should display.
     *
     * @return The level that should be displayed, if equal or lesser than 0 the level is not intended to be displayed.
     */
    fun labelLevel() = entityData.get(LABEL_LEVEL)

    override fun playAmbientSound() {
        if (!this.isSilent || this.busyLocks.filterIsInstance<EmptyPokeBallEntity>().isEmpty()) {
            val sound = ResourceLocation.fromNamespaceAndPath(
                this.pokemon.species.resourceIdentifier.namespace,
                "pokemon.${this.pokemon.showdownId()}.ambient"
            )
            // ToDo distance to travel is currently hardcoded to default we can maybe find a way to work around this down the line
            UnvalidatedPlaySoundS2CPacket(
                sound,
                this.soundSource,
                this.x,
                this.y,
                this.z,
                this.soundVolume,
                this.voicePitch
            ).sendToPlayersAround(this.x, this.y, this.z, 16.0, this.level().dimension())
        }
    }

    // We never want to allow an actual sound event here, we do not register our sounds to the sound registry as species are loaded by the time the registry is frozen.
    // Super call would do the same but might as well future-proof.
    override fun getAmbientSound() = null

    override fun getAmbientSoundInterval() = Cobblemon.config.ambientPokemonCryTicks

    private fun attemptItemInteraction(player: Player, stack: ItemStack): Boolean {
        if (stack.isEmpty) {
            return false
        }

        if (player is ServerPlayer && isBattling) {
            val battle = battleId?.let(BattleRegistry::getBattle) ?: return false

            val bagItemLike = BagItems.getConvertibleForStack(stack) ?: return false

            val battlePokemon = battle.actors
                    .flatMap { it.pokemonList }
                    .find { it.effectedPokemon.uuid == pokemon.uuid }
                    ?: return false // Shouldn't be possible but anyway

            if (battlePokemon.actor.getSide().actors.none { it.isForPlayer(player) }) {
                return true
            }

            return bagItemLike.handleInteraction(player, battlePokemon, stack)
        }

        if (this.isBusy) {
            return false
        }

        val interaction = PokemonInteractions.findInteraction(this)
        if (interaction != null && !pokemon.isOnInteractionCooldown(interaction.grouping)) {
            if (player is ServerPlayer) {
                interaction.effects.forEach { it.applyEffect(this, player) }
                pokemon.interactionCooldowns.put(interaction.grouping, runtime.resolveInt(interaction.cooldown))
            }

            return true
        }

        if (player !is ServerPlayer) {
            return false
        }

        // Evolution item logic
        if (pokemon.getOwnerPlayer() == player) {
            val context = ItemInteractionEvolution.ItemInteractionContext(stack, player.level())
            pokemon.lockedEvolutions
                    .filterIsInstance<ItemInteractionEvolution>()
                    .forEach { evolution ->
                        if (evolution.attemptEvolution(pokemon, context)) {
                            stack.consume(1, player)
                            this.level().playSoundServer(
                                    position = this.position(),
                                    sound = CobblemonSounds.ITEM_USE,
                                    volume = 1F,
                                    pitch = 1F
                            )
                            return true
                        }
                    }
        }

        // Fallback to item-defined interaction
        (stack.item as? PokemonEntityInteraction)?.let {
            if (it.onInteraction(player, this, stack)) {
                it.sound?.let { s ->
                    this.level().playSoundServer(
                            position = this.position(),
                            sound = s,
                            volume = 1F,
                            pitch = 1F
                    )
                }
                return true
            }
        }

        return false
    }

    override fun getOwner(): LivingEntity? {
        return pokemon.getOwnerEntity()
    }

    fun offerHeldItem(player: Player, stack: ItemStack): Boolean {
        return offerItem(player, stack, isCosmetic = false)
    }

    fun offerCosmeticItem(player: Player, stack: ItemStack): Boolean {
        return offerItem(player, stack, isCosmetic = true)
    }

    fun offerItem(
        player: Player,
        stack: ItemStack,
        isCosmetic: Boolean
    ): Boolean {
        if (player !is ServerPlayer || this.isBusy || this.pokemon.getOwnerPlayer() != player) {
            return false
        }

        if (!stack.isEmpty && !isCosmetic && (isBlacklisted(stack) || !isWhitelisted(stack))) {
            player.sendSystemMessage(lang("held_item.forbidden", stack.hoverName, this.pokemon.getDisplayName()))
            return false
        }

        val possibleReturn = if (isCosmetic) this.pokemon.cosmeticItem.copy() else this.pokemon.heldItemNoCopy()
        val giving = stack.copy().apply { count = 1 }

        if (ItemStack.isSameItemSameComponents(giving, possibleReturn)) {
            val message = if (isCosmetic) {
                lang("cosmetic_item.already_wearing", this.pokemon.getDisplayName(), stack.hoverName)
            } else {
                lang("held_item.already_holding", this.pokemon.getDisplayName(), stack.hoverName)
            }
            player.sendSystemMessage(message)
            return false
        }

        val currentItem = if (isCosmetic) pokemon.cosmeticItem else pokemon.heldItem
        val pre = HeldItemEvent.Pre(pokemon, stack, currentItem.copy(), !player.isCreative)
        if (isCosmetic) CobblemonEvents.COSMETIC_ITEM_PRE.post(pre) else CobblemonEvents.HELD_ITEM_PRE.post(pre)
        if (pre.isCanceled) return false

        val returned = pre.returning
        if (isCosmetic) {
            this.pokemon.swapCosmeticItem(stack = pre.receiving, decrement = !player.isCreative)
        } else {
            this.pokemon.swapHeldItem(stack = pre.receiving, decrement = !player.isCreative, false)
        }

        val text = when {
            isCosmetic && giving.isEmpty -> lang("cosmetic_item.take", returned.displayName, this.pokemon.getDisplayName())
            isCosmetic && returned.isEmpty -> lang("cosmetic_item.give", this.pokemon.getDisplayName(), giving.displayName)
            !isCosmetic && giving.isEmpty -> lang("held_item.take", returned.displayName, this.pokemon.getDisplayName())
            !isCosmetic && returned.isEmpty -> lang("held_item.give", this.pokemon.getDisplayName(), giving.displayName)
            isCosmetic -> lang("cosmetic_item.replace", returned.displayName, this.pokemon.getDisplayName(), giving.displayName)
            else -> lang("held_item.replace", returned.displayName, this.pokemon.getDisplayName(), giving.displayName)
        }

        player.giveOrDropItemStack(pre.returning, false)
        player.sendSystemMessage(text)
        this.level().playSoundServer(
            position = this.position(),
            sound = SoundEvents.ITEM_PICKUP,
            volume = 0.6F,
            pitch = 1.4F
        )

        if (isCosmetic) {
            CobblemonEvents.COSMETIC_ITEM_POST.post(HeldItemEvent.Post(pokemon, pokemon.cosmeticItem.copy(), pre.returning.copy(), pre.decrement))
        }
        else {
            CobblemonEvents.HELD_ITEM_POST.post(HeldItemEvent.Post(pokemon, pokemon.heldItem.copy(), pre.returning.copy(), pre.decrement))
        }

        return true
    }

    fun isBlacklisted(stack: ItemStack): Boolean =
        BuiltInRegistries.ITEM.getTagOrEmpty(CobblemonItemTags.BLACKLISTED_ITEMS_TO_HOLD).any()
                && stack.`is`(CobblemonItemTags.BLACKLISTED_ITEMS_TO_HOLD)

    fun isWhitelisted(stack: ItemStack): Boolean =
        BuiltInRegistries.ITEM.getTagOrEmpty(CobblemonItemTags.WHITELISTED_ITEMS_TO_HOLD).none()
                || stack.`is`(CobblemonItemTags.WHITELISTED_ITEMS_TO_HOLD)

    fun tryRidingPokemon(player: ServerPlayer): Boolean {
        val event = RidePokemonEvent.Pre(player, this)
        CobblemonEvents.RIDE_EVENT_PRE.post(event)
        if (!event.isCanceled) {
            player.startRiding(this)
            CobblemonEvents.RIDE_EVENT_POST.post(RidePokemonEvent.Post(player, this))
            return true
        }
        return false
    }

    fun tryMountingShoulder(player: ServerPlayer): Boolean {
        if (this.pokemon.belongsTo(player) && this.hasRoomToMount(player)) {
            CobblemonEvents.SHOULDER_MOUNT.postThen(
                ShoulderMountEvent(
                    player,
                    pokemon,
                    isLeft = player.shoulderEntityLeft.isEmpty
                )
            ) {
                val dirToPlayer = player.eyePosition.subtract(position()).multiply(1.0, 0.0, 1.0).normalize()
                deltaMovement = dirToPlayer.scale(0.8).add(0.0, 0.5, 0.0)
                val lock = Any()
                busyLocks.add(lock)
                after(seconds = 0.5F) {
                    busyLocks.remove(lock)
                    if (!isBusy && isAlive) {
                        val isLeft = player.shoulderEntityLeft.isEmpty
                        if (isLeft || player.shoulderEntityRight.isEmpty) {
                            pokemon.state = ShoulderedState(player.uuid, isLeft, pokemon.uuid)
                            this.setEntityOnShoulder(player)
                            this.pokemon.form.shoulderEffects.forEach { it.applyEffect(this.pokemon, player, isLeft) }
                            this.level().playSoundServer(
                                position = this.position(),
                                sound = SoundEvents.ITEM_PICKUP,
                                volume = 0.7F,
                                pitch = 1.4F
                            )
                            discard()
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    override fun setEntityOnShoulder(player: ServerPlayer): Boolean {
        if (!super.setEntityOnShoulder(player)) {
            return false
        }
        var isLeft = false
        // Use copies because player doesn't expose a forceful update of shoulder data
        val nbt = when {
            player.shoulderEntityRight.isPokemonEntity() && player.shoulderEntityRight.getCompound(DataKeys.POKEMON)
                .getUUID(DataKeys.POKEMON_UUID) == this.pokemon.uuid -> player.shoulderEntityRight.copy()

            player.shoulderEntityLeft.isPokemonEntity() && player.shoulderEntityLeft.getCompound(DataKeys.POKEMON)
                .getUUID(DataKeys.POKEMON_UUID) == this.pokemon.uuid -> {
                isLeft = true
                player.shoulderEntityLeft.copy()
            }

            else -> return true
        }
        nbt.putUUID(DataKeys.SHOULDER_UUID, this.pokemon.uuid)
        nbt.putString(DataKeys.SHOULDER_SPECIES, this.pokemon.species.resourceIdentifier.toString())
        nbt.putString(DataKeys.SHOULDER_FORM, this.pokemon.form.name)
        nbt.put(DataKeys.SHOULDER_ASPECTS, this.pokemon.aspects.map(StringTag::valueOf).toNbtList())
        nbt.putFloat(DataKeys.SHOULDER_SCALE_MODIFIER, this.pokemon.scaleModifier)
        nbt.put(
            DataKeys.SHOULDER_ITEM,
            this.level().registryAccess()
                .let { if (this.shownItem.isEmpty) CompoundTag() else this.shownItem.saveOptional(it) } as CompoundTag)
        if (isLeft) player.shoulderEntityLeft = nbt else player.shoulderEntityRight = nbt
        return true
    }

    /**
     * Adjusts a given sent out position based on the local environment.
     * Returns the new position and a PlatformType if the pokemon should be placed on one.
     */
    fun getAdjustedSendoutPosition(pos: Vec3): Vec3 {
        var platform = PlatformType.NONE
        var blockPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
        var blockLookCount = 5
        var foundSurface = false
        val exposedForm = this.exposedForm
        var result = pos
        if (this.level().isWaterAt(blockPos)) {
            // look upward for a water surface
            var testPos = blockPos
            if (!exposedForm.behaviour.moving.swim.canBreatheUnderwater || exposedForm.behaviour.moving.fly.canFly) {
                // move sendout pos to surface if it's near
                for (i in 0..blockLookCount) {
                    // Try to find a surface...
                    val blockState = this.level().getBlockState(testPos)
                    if (blockState.fluidState.isEmpty) {
                        if (blockState.getCollisionShape(this.level(), testPos).isEmpty) {
                            foundSurface = true
                        }
                        // No space above the water surface
                        break
                    }
                    testPos = testPos.above()
                }
                if (foundSurface) {
                    val hasHeadRoom = !collidesWithBlock(
                        Vec3(
                            blockPos.x.toDouble(),
                            (blockPos.y).toDouble(),
                            (blockPos.z).toDouble()
                        )
                    )
                    if (hasHeadRoom) {
                        result = Vec3(result.x, testPos.y.toDouble(), result.z)
                    }
                } else {
                    foundSurface = false
                }
            }
        } else if (this.level().isEmptyBlock(blockPos)) {
            // look downward for a water surface
            blockLookCount = 64 // Higher because the pokemon can fall down to water below
            var testPos = BlockPos(pos.x.toInt(), pos.y.toInt(), pos.z.toInt())
            for (i in 0..blockLookCount) {
                // Try to find a surface...
                val blockState = this.level().getBlockState(testPos)
                if (!blockState.fluidState.isEmpty) {
                    foundSurface = true
                    break
                }
                if (!blockState.getCollisionShape(this.level(), testPos).isEmpty) {
                    break
                }
                testPos = testPos.below()
            }
        }
        if (foundSurface) {
            val canFly = exposedForm.behaviour.moving.fly.canFly
            if (canFly) {
                val hasHeadRoom =
                    !collidesWithBlock(Vec3(blockPos.x.toDouble(), (result.y + 1), (blockPos.z).toDouble()))
                if (hasHeadRoom) {
                    result = Vec3(result.x, result.y + 1.0, result.z)
                }
            } else if (exposedForm.behaviour.moving.swim.canBreatheUnderwater && !exposedForm.behaviour.moving.swim.canWalkOnWater) {
                // Use half hitbox height for swimmers
                val halfHeight = getDimensions(this.pose).height / 2.0
                for (i in 1..halfHeight.toInt()) {
                    blockPos = blockPos.below()
                    if (!this.level().isWaterAt(blockPos) || !this.level().getBlockState(blockPos)
                            .getCollisionShape(this.level(), blockPos).isEmpty
                    ) {
                        break
                    }
                }
                result = Vec3(result.x, result.y + halfHeight - halfHeight.toInt(), result.z)
            } else {
                platform = if (exposedForm.behaviour.moving.swim.canWalkOnWater || collidesWithBlock(
                        Vec3(
                            result.x,
                            result.y,
                            result.z
                        )
                    )
                ) PlatformType.NONE else PlatformType.getPlatformTypeForPokemon(exposedForm)
            }
        }
        this.platform = platform

        return result
    }

    private fun Entity.collidesWithBlock(pos: Vec3): Boolean {
        return level().getBlockCollisions(this, boundingBox.move(pos)).iterator().hasNext()
    }

    override fun remove(reason: RemovalReason) {
        val stateEntity = (pokemon.state as? ActivePokemonState)?.entity
        super.remove(reason)

        if (stateEntity == this) {
            pokemon.state = InactivePokemonState()
        }
        subscriptions.forEach(ObservableSubscription<*>::unsubscribe)
        removalObservable.emit(reason)
        this.brain.clearMemories()

        if (reason.shouldDestroy() && pokemon.tetheringId != null) {
            pokemon.tetheringId = null
        }
        if (evolutionEntity != null) {
            evolutionEntity!!.kill()
            pokemon.entity?.evolutionEntity = null
        }
    }

    // Copy and paste of how vanilla checks it, unfortunately no util method you can only add then wait for the result
    fun hasRoomToMount(player: Player): Boolean {
        return (player.shoulderEntityLeft.isEmpty || player.shoulderEntityRight.isEmpty)
                && !player.isPassenger
                && player.onGround()
                && !player.isInWater
                && !player.isInPowderSnow
    }

    fun cry() {
        if (this.isSilent) return
        val pkt = PlayPosableAnimationPacket(id, setOf("cry"), emptyList())
        level().getEntitiesOfClass(ServerPlayer::class.java, AABB.ofSize(position(), 64.0, 64.0, 64.0)) { true }
            .forEach {
                it.sendPacket(pkt)
            }
    }

    override fun dropAllDeathLoot(world: ServerLevel, source: DamageSource) {
        if (battleId != null) {
            return
        }
        if (pokemon.isWild()) {
            super.dropAllDeathLoot(world, source)
            delegate.drop(source)
        }
    }

    override fun dropExperience(attacker: Entity?) {
        // Copied over the entire function because it's the simplest way to switch out the gamerule check
        if (
            level() is ServerLevel && !this.wasExperienceConsumed() &&
            (isAlwaysExperienceDropper ||
                    lastHurtByPlayerTime > 0 &&
                    shouldDropExperience() &&
                    level().gameRules.getBoolean(
                        CobblemonGameRules.DO_POKEMON_LOOT
                    ))
        ) {
            ExperienceOrb.award(level() as ServerLevel, position(), baseExperienceReward)
        }
    }

    override fun tickDeath() {
        // Do not invoke super we need to keep a tight lid on this due to the Thorium mod forcing the ticks to a max of 20 on server side if we invoke a field update here
        // Client delegate is mimicking expected behavior on client end.
        delegate.updatePostDeath()
    }

    override fun ate() {
        super.ate()

        val feature = pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED)
        if (feature != null) {
            feature.enabled = false
            pokemon.markFeatureDirty(feature)
            pokemon.updateAspects()
        }
    }

    override fun handleRelativeFrictionAndCalculateMovement(deltaMovement: Vec3, friction: Float): Vec3 {
        val riders = this.passengers.filterIsInstance<LivingEntity>()
        if (riders.isEmpty() || this.controllingPassenger == null) {
            super.handleRelativeFrictionAndCalculateMovement(deltaMovement, friction)
        } else {
            val velocity = ifRidingAvailableSupply(fallback = Vec3.ZERO) { behaviour, settings, state ->
                behaviour.velocity(settings, state, this, this.controllingPassenger as Player, deltaMovement)
            }
            // Handle ridden pokemon differently to allow vector lerp instead of simple addition.
            val v = getInputVector(velocity, 1.0f, this.yRot)
            // Changing this will give the ride more or less inertia/handling/drift
            val inertia = ifRidingAvailableSupply(fallback = 0.5) { behaviour, settings, state ->
                behaviour.inertia(settings, state,this)
            }

            this.deltaMovement = this.deltaMovement.lerp(v, inertia)
            var pos = this.deltaMovement.scale(this.speed.toDouble())
            if (super.onGround() && this.deltaMovement.y == 0.0) {
                pos = pos.subtract(0.0, 0.0001, 0.0)
            }
            this.move(MoverType.SELF, pos)
        }

        return this.deltaMovement
    }

    /*
    override fun shouldDiscardFriction(): Boolean {
        val riders = this.passengers.filterIsInstance<LivingEntity>()
        if (riders.isEmpty()) {
            return super.shouldDiscardFriction()
        } else {
            return true
        }
    }
     */

    override fun move(type: MoverType, pos: Vec3) {
        if (this.controllingPassenger != null || this.passengers.filterIsInstance<LivingEntity>().isNotEmpty()) {
            // Reset fall distance every tick if the Pokémon isn't nosediving
            if (this.deltaMovement.y() > -0.5F && this.fallDistance > 1.0F) {
                this.fallDistance = 1.0F
            }
        }
        super.move(type, pos)
    }

    fun getAttribute(attribute: Holder<Attribute>, value: Double): AttributeInstance {
        val instance = AttributeInstance(attribute) {}
        instance.baseValue = value
        return instance
    }

    override fun travel(movementInput: Vec3) {
        val prevBlockPos = this.blockPosition()
        if (beamMode != 3) { // Don't let Pokémon move during recall

            //Prevent current travel logic when riding a pokemon.
            val riders = this.passengers.filterIsInstance<LivingEntity>()
            if ( riders.isEmpty() || this.controllingPassenger == null) {
                super.travel(movementInput)
            } else {
                val inp = ifRidingAvailableSupply(fallback = Vec3.ZERO) { behaviour, settings, state ->
                    behaviour.velocity(settings, state, this, this.controllingPassenger as Player, deltaMovement)
                }

                // Rotate velocity vector to face the current y rotation
                val f = Mth.sin(this.yRot.toRadians())
                val g = Mth.cos(this.yRot.toRadians())
                val v = Vec3(
                    inp.x * g.toDouble() - inp.z * f.toDouble(),
                    inp.y,
                    inp.z * g.toDouble() + inp.x * f.toDouble()
                )

                val diff = v.subtract(this.deltaMovement)

                val inertia = ifRidingAvailableSupply(fallback = 0.5) { behaviour, settings, state ->
                    behaviour.inertia(settings, state,this)
                }

                this.deltaMovement = this.deltaMovement.add( diff.scale(inertia) )
                val triedMovement = this.deltaMovement

                this.move(MoverType.SELF, this.deltaMovement)

                if (this.horizontalCollision && this.isControlledByLocalInstance) {
                    ifRidingAvailable { behaviour, settings, state ->
                        // Tried minus performed = vector pointing at where we *couldn't* go
                        val delta = triedMovement.subtract(this.deltaMovement)
                        if (behaviour.damageOnCollision(settings, state, this, delta)) {
                            DamageOnCollisionPacket(delta).sendToServer()

                            for (passenger in this.passengers) {
                                passenger.deltaMovement = this.deltaMovement
                            }
                            DismountPokemonPacket().sendToServer()

                            // Reset ride velocity
                            state.rideVelocity.set(state.rideVelocity.get().multiply(0.0, 1.0, 0.0))
                        }
                    }
                }
            }

            if (this.pokemon.hasBlocksTraveledRequirement()) {
                this.updateBlocksTraveled(prevBlockPos)
            }
        }
        if (isBattling && this.isInWater) {
            // Prevent swimmers from sinking in battle
            this.deltaMovement = Vec3(deltaMovement.x, 0.0, deltaMovement.z)
        }
    }

    private fun updateBlocksTraveled(fromBp: BlockPos) {
        // Riding or falling shouldn't count, other movement sources are fine
        if (this.isPassenger || this.isFalling()) {
            return
        }
        val blocksTaken = this.blockPosition().distSqr(fromBp)
        if (blocksTaken > 0) {
            this.pokemon.addBlocksTraveled(blocksTaken.toInt())
        }
    }

    override fun pushEntities() {
        // Don't collide with other entities when being recalled
        if (beamMode != 3) super.pushEntities()
    }

    override fun isPushable(): Boolean {
        if (this.pokemon.species.name.lowercase() == "trainer") {
            return false
        }
        return beamMode != 3 && super.isPushable()
    }

    // this is only in place to stop crashes when other mods call this method on Pokémon, not used in cobblemon at the time of this writing
    override fun tame(player: Player) {
        if (!pokemon.isWild() || !isAlive || ownerUUID != null)
            return
        super.tame(player)
        if (player is ServerPlayer) {
            val party = player.party()
            if (party.getFirstAvailablePosition() == null) {
                discard()
            }
            party.add(pokemon)
            pokemon.state = SentOutState(this)
        }
    }

    override fun isTame(): Boolean {
        return ownerUUID != null || !pokemon.isWild()
    }

    /*
        private fun updateEyeHeight() {
            @Suppress("CAST_NEVER_SUCCEEDS")
            (this as com.cobblemon.mod.common.mixin.accessor.AccessorEntity).standingEyeHeight(this.getActiveEyeHeight(EntityPose.STANDING, this.type.dimensions))
        }

    */

    fun isFalling() =
        this.fallDistance > 0 && this.level().getBlockState(this.blockPosition().below()).isAir && !this.isFlying()

    override fun checkFallDamage(y: Double, onGround: Boolean, state: BlockState, pos: BlockPos) {
        super.checkFallDamage(y, onGround, state, pos)
        if (isFlying() && this.passengers.isEmpty() && y < 0.0 && !onGround) {
            fallDistance = 0F // Prevent fall damage after flying without a rider
        }
    }

    override fun getCurrentPoseType(): PoseType = this.entityData.get(POSE_TYPE)

    /**
     * Returns the [Species.translatedName] of the backing [pokemon].
     *
     * @return The [Species.translatedName] of the backing [pokemon].
     */
    override fun getTypeName(): Component = this.pokemon.species.translatedName

    /**
     * If this Pokémon has a nickname, then the nickname is returned.
     * Otherwise, [getDisplayName] is returned
     *
     * @return The current display name of this entity.
     */
    override fun getName(): Component {
        if (!entityData.get(NICKNAME_VISIBLE)) return typeName
        return entityData.get(NICKNAME).takeIf { it.contents != PlainTextContents.EMPTY }
            ?: pokemon.getDisplayName()
    }

    /**
     * If this Pokémon has an active mark that has an applicable title, then the name with the title is returned.
     * Otherwise, [getName] is returned
     *
     * @return The current display name with title of this entity.
     */
    fun getTitledName(): MutableComponent {
        val mark = entityData.get(MARK).let { Marks.getByIdentifier(it.asResource()) } ?: pokemon.activeMark
        return mark?.getTitle(getName().copy()) ?: getName().copy()
    }

    /**
     * Returns the custom name of this entity, in the context of Cobblemon it is the [Pokemon.nickname].
     *
     * @return The nickname of the backing [pokemon].
     */
    override fun getCustomName(): Component? = pokemon.nickname

    /**
     * Sets the custom name of this entity.
     * In the context of a Pokémon entity this affects the [Pokemon.nickname].
     *
     * @param name The new name being set, if null the [Pokemon.nickname] is removed.
     */
    override fun setCustomName(name: Component?) {
        // We do this as a compromise to keep as much compatibility as possible with other mods expecting this entity to act like a vanilla one
        this.pokemon.nickname = Component.literal(name?.string)
    }

    /**
     * Checks if the backing [pokemon] has a non-null [Pokemon.nickname].
     *
     * @return If the backing [pokemon] has a non-null [Pokemon.nickname].
     */
    override fun hasCustomName(): Boolean =
        pokemon.nickname != null && pokemon.nickname?.contents != PlainTextContents.EMPTY

    /**
     * This method toggles the visibility of the entity name,
     * Unlike the vanilla implementation in our context it changes between displaying the species name or nickname of the Pokémon.
     *
     * @param visible The state of custom name visibility.
     */
    override fun setCustomNameVisible(visible: Boolean) {
        // We do this as a compromise to keep as much compatibility as possible with other mods expecting this entity to act like a vanilla one
        entityData.set(NICKNAME_VISIBLE, visible)
    }

    /**
     * Attempts to force initiate a battle with this Pokémon.
     *
     * @param player The player to attempt a battle with.
     * @return Whether the battle was successfully started.
     */
    fun forceBattle(player: ServerPlayer): Boolean {
        if (!canBattle(player)) {
            return false
        }
        val lead = player.party().firstOrNull { it.entity != null }?.uuid
        return BattleBuilder.pve(player, this, lead) is SuccessfulBattleStart
    }

    /**
     * In the context of a Pokémon entity this checks if the Pokémon is currently set to displaying its nickname.
     *
     * @return If the custom name of this entity should display, in this case the [getCustomName] is the nickname but if null the [getDefaultName] will be used.
     */
    override fun isCustomNameVisible(): Boolean = entityData.get(NICKNAME_VISIBLE)

    /**
     * Returns whether the entity is currently set to having its name displayed.
     *
     * @return If this entity should render the name label.
     */
    override fun shouldShowName(): Boolean = entityData.get(SHOULD_RENDER_NAME)

    /**
     * Sets the entity to having its name hidden.
     */
    fun hideNameRendering() {
        entityData.set(SHOULD_RENDER_NAME, false)
    }

    override fun isFood(stack: ItemStack): Boolean = false

    override fun canMate(other: Animal): Boolean = false

    override fun spawnChildFromBreeding(world: ServerLevel, other: Animal) {}

    override fun dampensVibrations(): Boolean = pokemon.dampensVibrations()

    override fun shear(shearedSoundCategory: SoundSource) {
        val slowpokeTailFeature = SlowpokeTailRegrowthSpeciesFeatureProvider.getFromPokemon(pokemon)
        if (slowpokeTailFeature != null && slowpokeTailFeature.regrowthSeconds <= 0) {
            this.level().playSound(null, this, SoundEvents.SHEEP_SHEAR, shearedSoundCategory, 1.0F, 1.0F)
            slowpokeTailFeature.onShear(this)
            return
        }

        val feature = this.pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED) ?: return
        this.level().playSound(null, this, SoundEvents.SHEEP_SHEAR, shearedSoundCategory, 1.0F, 1.0F)
        feature.enabled = true
        this.pokemon.markFeatureDirty(feature)
        this.pokemon.updateAspects()
        val i = this.random.nextInt(3) + 1
        for (j in 0..i) {
            val color = this.pokemon.getFeature<StringSpeciesFeature>(DataKeys.CAN_BE_COLORED)?.value ?: "white"
            val woolItem = when (color) {
                "black" -> Items.BLACK_WOOL
                "blue" -> Items.BLUE_WOOL
                "brown" -> Items.BROWN_WOOL
                "cyan" -> Items.CYAN_WOOL
                "gray" -> Items.GRAY_WOOL
                "green" -> Items.GREEN_WOOL
                "light_blue" -> Items.LIGHT_BLUE_WOOL
                "light_gray" -> Items.LIGHT_GRAY_WOOL
                "lime" -> Items.LIME_WOOL
                "magenta" -> Items.MAGENTA_WOOL
                "orange" -> Items.ORANGE_WOOL
                "purple" -> Items.PURPLE_WOOL
                "red" -> Items.RED_WOOL
                "yellow" -> Items.YELLOW_WOOL
                "pink" -> Items.PINK_WOOL
                else -> Items.WHITE_WOOL
            }
            val itemEntity = this.spawnAtLocation(woolItem, 1) ?: return
            jitterDropItem(itemEntity)
        }
    }

    override fun readyForShearing(): Boolean {
        if (this.isBusy || this.pokemon.isFainted()) {
            return false
        }
        val slowpokeRegrowthFeature = SlowpokeTailRegrowthSpeciesFeatureProvider.getFromPokemon(pokemon)
        return if (slowpokeRegrowthFeature != null && slowpokeRegrowthFeature.regrowthSeconds > 0) {
            false
        } else if (slowpokeRegrowthFeature != null) {
            true
        } else {
            this.pokemon.getFeature<FlagSpeciesFeature>(DataKeys.HAS_BEEN_SHEARED)?.enabled == false
        }
    }

    override fun canUsePortal(allowsVehicles: Boolean) = false

    override fun setAirSupply(air: Int) {
        if (this.isBattling) {
            this.entityData.set(DATA_AIR_SUPPLY_ID, 300)
            return
        }
        super.setAirSupply(air)
    }

    override fun stopSeenByPlayer(player: ServerPlayer) {
        if (this.ownerUUID == player.uuid && tethering == null) {
            // queuedToDespawn = true
            this.remove(RemovalReason.DISCARDED)
            return
        }
    }

    override fun canBeLeashed() = true
    override fun setLeashedTo(entity: Entity, bl: Boolean) {
        super.setLeashedTo(entity, bl)
        if (this.ownerUUID != null && this.ownerUUID != entity.uuid) {
            dropLeash(true, true)
        }
    }

    /** Retrieves the battle theme associated with this Pokemon's Species/Form which falls back to the default PVW theme if not found. */
    fun getBattleTheme() = this.form.battleTheme

    /**
     * A utility method to instance a [Pokemon] aware if the [world] is client sided or not.
     *
     * @return The side safe [Pokemon] with the [Pokemon.isClient] set.
     */
    private fun createSidedPokemon(): Pokemon = Pokemon().apply { isClient = this@PokemonEntity.level().isClientSide }

    override fun canRide(vehicle: Entity): Boolean {
        return platform == PlatformType.NONE && super.canRide(vehicle) && occupiedSeats.isEmpty()
    }

    // Takes in a requested stat type with a base minimum and base maximum and returns the interpolated
    // stat based on the boost of that Pokémon's stat
    fun getRideStat(rideStat: RidingStat, style: RidingStyle, baseMin: Double, baseMax: Double): Double {
        if (rideStatOverrides[style] != null && rideStatOverrides[style]!![rideStat] != null) {
            return (((baseMax - baseMin) / 100) * rideStatOverrides[style]!![rideStat]!!) + baseMin
        }
        val stat = this.rideProp.behaviours?.get(style)?.calculate(rideStat, entityData.get(RIDE_BOOSTS)[rideStat] ?: 0F) ?: return 0.0
        val statVal = (((baseMax - baseMin) / 100) * stat) + baseMin
        // Cap the minimum value at a very small value to prevent control inversions and other unexpected behaviour in
        // the ride controllers when using negative values
        return max(statVal, 1e-6)
    }

    override fun couldAcceptPassenger(): Boolean {
        return seats.isNotEmpty() && super.couldAcceptPassenger()
    }

    fun getRawRideStat(stat: RidingStat, style: RidingStyle): Double {
        if (rideStatOverrides[style] != null && rideStatOverrides[style]!![stat] != null) {
            return rideStatOverrides[style]!![stat]!!
        }
        return this.rideProp.behaviours?.get(style)?.calculate(stat, 0F)?.toDouble() ?: 0.0
    }

    internal fun overrideRideStat(style: RidingStyle, stat: RidingStat, value: Double) {
        if (rideStatOverrides[style] == null) {
            rideStatOverrides[style] = mutableMapOf()
        }
        rideStatOverrides[style]!![stat] = value
    }

    override fun canAddPassenger(passenger: Entity): Boolean {
        return passengers.size < seats.size
    }

    public override fun addPassenger(passenger: Entity) {
        if (passenger is ServerPlayer) {
            // Recall all the rest of the player's party pokes
            passenger.party()
                .mapNotNull { it.entity }
                .filter { it != this }
                .forEach { it.recallWithAnimation() }

            // Driver is hopping on, figure out the stamina situation
            if (passengers.isEmpty()) {
                CobblemonEvents.RIDE_EVENT_APPLY_STAMINA.post(
                    RidePokemonEvent.ApplyStamina(
                        player = passenger,
                        pokemon = this,
                        rideStamina = if (Cobblemon.config.infiniteRideStamina) -1F else entityData.get(RIDE_STAMINA)
                    ),
                    then = { event -> entityData.set(RIDE_STAMINA, event.rideStamina) }
                )
            }
        } else if (level().isClientSide) {
            MountedCameraTypeHandler.handleMount(passenger, this)
        }
        val passengerIndex = occupiedSeats.indexOfFirst { it == null }
        if (passengerIndex != -1) {
            occupiedSeats[passengerIndex] = passenger
        }
        super.addPassenger(passenger)
        if (passengers.size == 1) {
            // Someone just started riding, fill in the stamina value! Gets run from both sides.
            ifRidingAvailable { _, _, state ->
                state.stamina.set(entityData.get(RIDE_STAMINA))
            }
        }
    }

    fun getIsJumping() = jumping
    fun setIsJumping(value: Boolean) {
        jumping = value
    }

    fun ifRidingAvailable(block: (RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState>, RidingBehaviourSettings, RidingBehaviourState) -> Unit) {
        val behaviour = ridingController?.getBehaviour() ?: return
        val settings = ridingController?.context?.settings ?: return
        val state = ridingController?.context?.state ?: return
        block(behaviour, settings, state)
    }

        fun <T> ifRidingAvailableSupply(
            fallback: T,
            block: (RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState>, RidingBehaviourSettings, RidingBehaviourState) -> T
    ): T {
        var result = fallback
        ifRidingAvailable { behaviour, settings, state ->
            result = block(behaviour, settings, state)
        }
        return result
    }

    override fun tickRidden(driver: Player, movementInput: Vec3) {
        super.tickRidden(driver, movementInput)
        ifRidingAvailable { behaviour, settings, state ->
            behaviour.tick(settings, state, this, driver, movementInput)
            if (entityData.get(RIDE_STAMINA) == -1F) {
                state.stamina.set(1F, forced = true)
            }

            if (!this.level().isClientSide) {
                val pose = behaviour.pose(settings, state, this)
                if (pose != this.entityData.get(POSE_TYPE)) {
                    entityData.set(POSE_TYPE, pose)
                }
            }

            this.yRotO = this.yRot

            if (this is OrientationControllable) {
                val controller = this.orientationController
                if (!controller.isActive()) {
                    val rotation = behaviour.rotation(settings, state, this, driver)
                    // TODO: Find a better solution than setting the vehicle xrot to zero.
                    // The problem is that nothing actually effects the vehicle/pokemon xrot so when it gets set to
                    // -45 degrees by a rollable ride controller it gets saved off and not modified until you try
                    // and takeoff again. And at that point you snap to -45 pitch in the orientationControllers matrix
                    setRot(rotation.y, 0.0f)
                }
            }

            this.yHeadRot = this.yRot
            this.yBodyRot = this.yRot
            this.passengers.filterIsInstance<LivingEntity>()

            if (behaviour.isActive(settings, state, this) && behaviour.canJump(settings, state, this, driver)) {
                if (this.jumpInputStrength > 0) {
                    //this.jump(this.jumpStrength, movementInput)
                    //this.jump()
                    val f = PI.toFloat() - this.yRot * PI.toFloat() / 180
                    val jumpVector = behaviour.jumpForce(settings, state, this, driver, this.jumpInputStrength)
                    val velocity = jumpVector.yRot(f)
                    // Rotate the jump vector f degrees around the Y axis
                    //val velocity = Vec3d(-sin(f) * jumpVector.x, jumpVector.y, cos(f) * jumpVector.z)

                    this.addDeltaMovement(velocity)
                    hasImpulse = true
                    jumping = false
                }

                this.jumpInputStrength = 0
            }
        }
    }

    fun Entity.isNearGround(): Boolean {
        val blockBelow: BlockPos = this.blockPosition().below()
        return this.level().getBlockState(blockBelow).isSolid
    }

    fun clampPassengerRotation(entityToUpdate: Entity) {
         if (entityToUpdate !is LivingEntity) return
        ifRidingAvailable { behaviour, settings, state ->
            behaviour.clampPassengerRotation(settings, state, this, entityToUpdate)
        }
    }

    override fun positionRider(passenger: Entity, positionUpdater: MoveFunction) {
        if (this.hasPassenger(passenger)) {
            this.delegate.positionRider(passenger, positionUpdater)

            if (passenger is LivingEntity) {
                ifRidingAvailable { behaviour, settings, state ->
                    behaviour.updatePassengerRotation(settings, state,this, passenger)
                }
            }
        }
    }

    // When riding mimic RemotePlayers logic for rendering players at farther
    // distances than usual. Otherwise the player may render when the pokemon
    // entity is not, causing a floating player.
    override fun shouldRenderAtSqrDistance(distance: Double): Boolean {
        if (!passengers.isEmpty()) {
            var d = (boundingBox.getSize() * 10.0)
            if (d.isNaN()) {
                d = 1.0
            }
            val scale = 64.0 * getViewScale()
            return distance < d * scale * scale
        }

        return super.shouldRenderAtSqrDistance(distance)
    }

    override fun getControllingPassenger(): LivingEntity? {
        val riders = this.passengers.filterIsInstance<LivingEntity>()
        if (riders.isEmpty()) {
            ridingController?.context?.state?.reset()
            return null
        }

        val event = SelectDriverEvent(riders.toSet())
        val owner = riders.find { it.uuid == ownerUUID }
        if (owner != null) {
            event.suggest(owner, 0)
        }

        CobblemonEvents.SELECT_DRIVER.emit(event)
        return event.result()
    }

    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3 {
        val vec3 = getCollisionHorizontalEscapeVector(
            this.bbWidth.toDouble(),
            passenger.bbWidth.toDouble(),
            this.yRot + (if (passenger.mainArm == HumanoidArm.RIGHT) 90.0f else -90.0f)
        )
        val vec32: Vec3? = this.getDismountLocationInDirection(vec3, passenger)
        if (vec32 != null) {
            return vec32
        } else {
            val vec33 = getCollisionHorizontalEscapeVector(
                this.bbWidth.toDouble(),
                passenger.bbWidth.toDouble(),
                this.yRot + (if (passenger.mainArm == HumanoidArm.LEFT) 90.0f else -90.0f)
            )
            val vec34: Vec3? = this.getDismountLocationInDirection(vec33, passenger)
            return vec34 ?: this.position()
        }
    }
    private fun getDismountLocationInDirection(direction: Vec3, passenger: LivingEntity): Vec3? {
        val d = this.x + direction.x
        val e = this.boundingBox.minY
        val f = this.z + direction.z
        val mutableBlockPos = BlockPos.MutableBlockPos()
        val var10: UnmodifiableIterator<*> = passenger.dismountPoses.iterator()

        while (var10.hasNext()) {
            val pose = var10.next() as Pose
            mutableBlockPos.set(d, e, f)
            val g = this.boundingBox.maxY + 0.75

            while (true) {
                val h = this.level().getBlockFloorHeight(mutableBlockPos)
                if (mutableBlockPos.getY().toDouble() + h > g) {
                    break
                }

                if (DismountHelper.isBlockFloorValid(h)) {
                    val aABB = passenger.getLocalBoundsForPose(pose)
                    val vec3 = Vec3(d, mutableBlockPos.getY().toDouble() + h, f)
                    if (DismountHelper.canDismountTo(this.level(), passenger, aABB.move(vec3))) {
                        passenger.pose = pose
                        return vec3
                    }
                }

                mutableBlockPos.move(Direction.UP)
                if (!(mutableBlockPos.y.toDouble() < g)) {
                    break
                }
            }
        }

        return null
    }
    override fun getRiddenInput(controller: Player, movementInput: Vec3): Vec3 {
        return ifRidingAvailableSupply(fallback = Vec3.ZERO) { behaviour, settings, state ->
            behaviour.velocity(settings, state, this, controller, movementInput)
        }
    }

    override fun maxUpStep(): Float {
        val upStep = ifRidingAvailableSupply(fallback = null) { behaviour, settings, state ->
            behaviour.maxUpStep(settings, state, this)
        }
        return upStep ?: super.maxUpStep()
    }

    override fun getRiddenSpeed(controller: Player): Float {
        return ifRidingAvailableSupply(fallback = 0.05f) { behaviour, settings, state ->
            behaviour.speed(settings, state,this, controller)
        }
    }

    var jumpInputStrength: Int = 0 // move this
    override fun onPlayerJump(strength: Int) {
        // See if this controls the hot bar element
        var strength = strength
        if (strength < 0) {
            strength = 0
        } else {
//            this.jumping = true
            // update anger? hunwah
        }

        this.jumpInputStrength = strength

        if (strength >= 90) {
//            this.jumpStrength = 1.0f
        } else {
//            this.jumpStrength = 0.4f + 0.4f * strength.toFloat() / 90.0f
        }

    }

    override fun canJump(): Boolean {
        return true
    }

    override fun handleStartJump(height: Int) {
        this.jumping = true
    }

    fun side() = if (delegate is PokemonServerDelegate) "SERVER" else "CLIENT"

    override fun handleStopJump() {
        jumping = false
        // Set back to land pose type?
    }

    /*
    These two functions (fluids and ground) need to become riding configurable and
    dependent. Also, the onGround() function seems to affect quite a few spots in
    code and would likely need to be changed to be something more robust instead of
    just overriding this method.
     */

    override fun isAffectedByFluids(): Boolean {
        var fluidAffected = true

        if (this.hasControllingPassenger()) {
            //Change this so it calls something from the controller to check
            //if the specific controller wants to ignore fluid physics since
            //not every single one will want to
            fluidAffected = false
        }

        return fluidAffected
    }


    //this seems a bit hacky to me seeing as how many spots in the base classes its used.
    //However, there are odd interactions with the controllers when they are meant to be
    //flying or swimming but they are touching the ground and this needs to be prevented.
    //Having it be able to be turned off by the flying or swimming controllers is the
    //temp solution I have found.
    override fun onGround(): Boolean {
        val result = ifRidingAvailableSupply(fallback = null) { behaviour, settings, state ->
            behaviour.turnOffOnGround(settings, state, this)
        }
        if (result != null && result) return false
        if (!this.behaviour.moving.walk.canWalk && this.behaviour.moving.fly.canFly) {
            return false
        }
        return super.onGround()
    }

    //I think already mentioned but should maybe be riding controller configurable
    override fun dismountsUnderwater(): Boolean {
        return false
    }

    override fun getDefaultGravity(): Double {
        val regularGravity = super.getDefaultGravity()
        if (this.passengers.isEmpty()) {
            return regularGravity
        }
        return ifRidingAvailableSupply(fallback = regularGravity) { behaviour, settings, state ->
            behaviour.gravity(settings, state, this, regularGravity)
        }
    }

    fun setRideBar(): Float {
        val driver = this.controllingPassenger as? Player ?: return 0.0f
        return ifRidingAvailableSupply(fallback = 0.0f) { behaviour, settings, state ->
            behaviour.setRideBar(settings, state, this, driver)
        }
    }

    fun rideFovMult(): Float {
        val driver = this.controllingPassenger as? Player ?: return 1.0f
        val fovEffectScale = Minecraft.getInstance().options.fovEffectScale().get().toFloat()

        return ifRidingAvailableSupply(fallback = 1.0f) { behaviour, settings, state ->
            val rideFov = behaviour.rideFovMultiplier(settings, state, this, driver)

            1f + (rideFov - 1f) * fovEffectScale
        }
    }

    /**
     * A utility method to resolve the [Codec] of [Pokemon] aware if the [world] is client sided or not.
     *
     * @return The [Codec].
     */
    private fun sidedCodec(): Codec<Pokemon> = if (this.level().isClientSide) Pokemon.CLIENT_CODEC else Pokemon.CODEC

    override fun resolvePokemonScan(): PokedexEntityData? {
        return PokedexEntityData(
            pokemon = pokemon,
            disguise = this.effects.mockEffect?.let {
                PokedexEntityData.DisguiseData(
                    species = it.exposedSpecies ?: pokemon.species,
                    form = it.exposedForm ?: pokemon.form,
                )
            }
        )
    }

    override fun resolveEntityScan(): LivingEntity {
        return this
    }

    override fun canWalk() = exposedForm.behaviour.moving.walk.canWalk
    override fun canSwimInWater() = exposedForm.behaviour.moving.swim.canSwimInWater
    override fun canWalkOnWater() = exposedForm.behaviour.moving.swim.canWalkOnWater
    override fun canFly() = exposedForm.behaviour.moving.fly.canFly
    override fun canSwimInLava() = exposedForm.behaviour.moving.swim.canSwimInLava
    override fun canPathThroughSaccLeaves() = this.config.getMap().getOrDefault("can_path_through_sacc_leaves", DoubleValue.ZERO).asDouble() == 1.0
    override fun canWalkOnLava() = exposedForm.behaviour.moving.swim.canWalkOnLava
    override fun entityOnGround() = onGround()

    override fun canSwimUnderFluid(fluidState: FluidState): Boolean {
        return if (fluidState.`is`(FluidTags.LAVA)) {
            exposedForm.behaviour.moving.swim.canBreatheUnderlava
        } else if (fluidState.`is`(FluidTags.WATER)) {
            exposedForm.behaviour.moving.swim.canBreatheUnderwater
        } else {
            false
        }
    }

    override fun isFlying() = this.getBehaviourFlag(PokemonBehaviourFlag.FLYING)
    override fun couldStopFlying() = isFlying() && !behaviour.moving.walk.avoidsLand && behaviour.moving.walk.canWalk
    override fun setFlying(state: Boolean) {
        setBehaviourFlag(PokemonBehaviourFlag.FLYING, state)
    }

    /**
     * If the Pokémon is following another Pokémon, checks the herd size using the leader. Otherwise check this Pokémon's
     * herd count. Not necessarily strictly up to date but it should be good enough for typical purposes.
     */
    fun getHerdSize(): Int {
        val world = level() as? ServerLevel ?: return 0
        val herdLeader = this.brain.getMemorySafely(CobblemonMemories.HERD_LEADER).orElse(null)?.let(UUID::fromString)?.let(world::getEntity) as? PokemonEntity
        return if (herdLeader == null) {
            brain.getMemorySafely(CobblemonMemories.HERD_SIZE).orElse(0)
        } else {
            herdLeader.brain.getMemorySafely(CobblemonMemories.HERD_SIZE).orElse(0)
        }
    }

    internal fun adjustHerdSize(difference: Int): Int {
        val currentSize = brain.getMemorySafely(CobblemonMemories.HERD_SIZE).orElse(0)
        val newSize = (currentSize + difference).coerceAtLeast(0)
        if (newSize == 0) {
            brain.eraseMemory(CobblemonMemories.HERD_SIZE)
        } else {
            brain.setMemory(CobblemonMemories.HERD_SIZE, newSize)
        }
        return newSize
    }

    /**
     * Figures out what the strongest tier applied to this herd is from the perspective of this entity. This can be a
     * bit confusing so bear with me.
     *
     * Every herd has a leader, and each Pokémon species specifies the Pokémon it is open to following, which will
     * come with a 'tier' of that openness. Following a Gyarados is a higher sense of loyalty than following a Magikarp.
     *
     * The herd tier of a Pokémon entity depends on whether it is a follower or a leader:
     * - If they are a follower, it will check the herd leader's tier. Fairly simple.
     * - If they are a leader, it will check the herd tier of all nearby followers and return the maximum tier. This
     *   represents a kind of 'responsibility' that the leader feels towards their followers - they believe in this
     *   leader with some amount of fervor, and this gets used to ensure that the leader doesn't choose to follow a
     *   different Pokémon that is of an equal or lower tier than this Pokémon is to its followers.
     */
    fun getHerdTier(): Int {
        val world = level() as? ServerLevel ?: return 0
        val herdLeader = this.brain.getMemorySafely(CobblemonMemories.HERD_LEADER).orElse(null)?.let(UUID::fromString)?.let(world::getEntity) as? PokemonEntity
        return if (herdLeader == null) {
            if (!brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)) {
                return 0
            }
            val nearbyEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().findAll {
                it is PokemonEntity && it.brain.getMemorySafely(CobblemonMemories.HERD_LEADER).map { it == this.uuid.toString() }.orElse(false)
            }
            nearbyEntities.maxOfOrNull {
                it as PokemonEntity
                it.behaviour.herd.bestMatchLeader(follower = it, possibleLeader = this)?.tier ?: 0
            } ?: 0
        } else {
            herdLeader.behaviour.herd.bestMatchLeader(follower = this, possibleLeader = herdLeader)?.tier ?: 0
        }
    }
}

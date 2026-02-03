/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.molang

import com.bedrockk.molang.runtime.MoLangEnvironment
import com.bedrockk.molang.runtime.MoLangRuntime
import com.bedrockk.molang.runtime.MoParams
import com.bedrockk.molang.runtime.struct.ArrayStruct
import com.bedrockk.molang.runtime.struct.ContextStruct
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.struct.VariableStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.MoValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonActivities
import com.cobblemon.mod.common.CobblemonBlockEntities
import com.cobblemon.mod.common.CobblemonMemories
import com.cobblemon.mod.common.CobblemonUnlockableWallpapers
import com.cobblemon.mod.common.Environment
import com.cobblemon.mod.common.api.ai.CobblemonBlockPosTracker
import com.cobblemon.mod.common.api.ai.CobblemonWanderControl
import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.dialogue.PlayerDialogueFaceProvider
import com.cobblemon.mod.common.api.dialogue.ReferenceDialogueFaceProvider
import com.cobblemon.mod.common.api.drop.DropEntry
import com.cobblemon.mod.common.api.mark.Marks
import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext
import com.cobblemon.mod.common.api.moves.animations.ActionEffects
import com.cobblemon.mod.common.api.moves.animations.NPCProvider
import com.cobblemon.mod.common.api.moves.animations.TargetsProvider
import com.cobblemon.mod.common.api.npc.NPCClasses
import com.cobblemon.mod.common.api.npc.configuration.interaction.DialogueNPCInteractionConfiguration
import com.cobblemon.mod.common.api.npc.configuration.interaction.ScriptNPCInteractionConfiguration
import com.cobblemon.mod.common.api.npc.partyproviders.SimplePartyProvider
import com.cobblemon.mod.common.api.pokedex.AbstractPokedexManager
import com.cobblemon.mod.common.api.pokedex.CaughtCount
import com.cobblemon.mod.common.api.pokedex.CaughtPercent
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokedex.PokedexManager
import com.cobblemon.mod.common.api.pokedex.SeenCount
import com.cobblemon.mod.common.api.pokedex.SeenPercent
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource
import com.cobblemon.mod.common.api.pokemon.moves.LearnsetQuery
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.api.scheduling.ClientTaskTracker
import com.cobblemon.mod.common.api.scheduling.Schedulable
import com.cobblemon.mod.common.api.scheduling.ServerTaskTracker
import com.cobblemon.mod.common.api.scripting.CobblemonScripts
import com.cobblemon.mod.common.api.spawning.TimeRange
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import com.cobblemon.mod.common.api.storage.PokemonStore
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore
import com.cobblemon.mod.common.api.storage.party.PartyStore
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore
import com.cobblemon.mod.common.api.storage.pc.PCPosition
import com.cobblemon.mod.common.api.storage.pc.PCStore
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.battles.BattleBuilder
import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor
import com.cobblemon.mod.common.client.render.models.blockbench.wavefunction.WaveFunctions
import com.cobblemon.mod.common.entity.MoLangScriptingEntity
import com.cobblemon.mod.common.entity.PosableEntity
import com.cobblemon.mod.common.entity.npc.NPCBattleActor
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.entity.pokemon.PokemonBehaviourFlag
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.entity.pokemon.ai.PokemonMoveControl
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket
import com.cobblemon.mod.common.net.messages.client.battle.BattleMusicPacket
import com.cobblemon.mod.common.net.messages.client.effect.RunPosableMoLangPacket
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormEntityParticlePacket
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket
import com.cobblemon.mod.common.net.messages.client.sound.UnvalidatedPlaySoundS2CPacket
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.IVs
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.pokemon.ai.ObtainableItem
import com.cobblemon.mod.common.pokemon.ai.ObtainableItemCondition
import com.cobblemon.mod.common.pokemon.evolution.variants.ItemInteractionEvolution
import com.cobblemon.mod.common.pokemon.evolution.variants.LevelUpEvolution
import com.cobblemon.mod.common.pokemon.evolution.variants.TradeEvolution
import com.cobblemon.mod.common.util.*
import com.mojang.datafixers.util.Either
import java.util.UUID
import kotlin.math.sqrt
import kotlin.random.Random
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.behavior.BlockPosTracker
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.memory.MemoryStatus
import net.minecraft.world.entity.ai.memory.WalkTarget
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.level.Level.ExplosionInteraction
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Holds a bunch of useful MoLang trickery that can be used or extended in API
 *
 * @author Hiroku
 * @since October 2nd, 2023
 */
object MoLangFunctions {
    val generalFunctions = hashMapOf<String, java.util.function.Function<MoParams, Any>>(
        "print" to java.util.function.Function { params ->
            val message = params.get<MoValue>(0).asString()
            Cobblemon.LOGGER.info(message)
        },
        "delete_variable" to java.util.function.Function { params ->
            val struct = params.get<VariableStruct>(0)
            val variable = params.getString(1)
            struct.map.remove(variable)
            DoubleValue.ONE
        },
        "delete_variables" to java.util.function.Function { params ->
            val struct = params.get<VariableStruct>(0)
            struct.map.clear()
            DoubleValue.ONE
        },
        "get_variable" to java.util.function.Function { params ->
            val struct = params.get<VariableStruct>(0)
            val variable = params.getString(1)
            return@Function struct.map[variable] ?: DoubleValue.ZERO
        },
        "set_variable" to java.util.function.Function { params ->
            val struct = params.get<VariableStruct>(0)
            val variable = params.getString(1)
            val value = params.get<MoValue>(2)
            struct.map[variable] = value
            return@Function value
        },
        "set_query" to java.util.function.Function { params ->
            val variable = params.getString(0)
            val value = params.get<MoValue>(1)
            params.environment.query.addFunction(variable) { value }
            return@Function value
        },
        "replace" to java.util.function.Function { params ->
            val text = params.getString(0)
            val search = params.getString(1)
            val replace = params.getString(2)
            return@Function StringValue(text.replace(search, replace))
        },
        "is_included" to java.util.function.Function { params ->
            val text = params.getString(0)
            val search = params.getString(1)
            return@Function DoubleValue(text.contains(search))
        },
        "to_lower" to java.util.function.Function { params ->
            return@Function StringValue(params.getString(0).lowercase())
        },
        "to_upper" to java.util.function.Function { params ->
            return@Function StringValue(params.getString(0).uppercase())
        },
        "string_length" to java.util.function.Function { params ->
            return@Function DoubleValue(params.getString(0).length)
        },
        "split_string" to java.util.function.Function { params ->
            val text = params.getString(0)
            val delimiter = params.getString(1)
            val parts = text.split(delimiter).map { StringValue(it) }
            val struct = ArrayStruct(hashMapOf())
            parts.forEachIndexed { index, moValue -> struct.setDirectly("$index", moValue) }
            return@Function struct
        },
        "is_blank" to java.util.function.Function { params ->
            val arg = params.get<MoValue>(0)
            return@Function DoubleValue((arg is StringValue && (arg.value.isBlank() || arg.value.toDoubleOrNull() == 0.0)) || (arg is DoubleValue && arg.value == 0.0))
        },
        "run_command" to java.util.function.Function { params ->
            val command = params.getString(0)
            val server = server() ?: return@Function DoubleValue.ZERO
            server.commands.performPrefixedCommand(server.createCommandSourceStack(), command)
        },
        "is_int" to java.util.function.Function { params -> DoubleValue(params.get<MoValue>(0).asString().isInt()) },
        "is_number" to java.util.function.Function { params -> DoubleValue(params.get<MoValue>(0).asString().toDoubleOrNull() != null) },
        "to_number" to java.util.function.Function { params -> DoubleValue(params.get<MoValue>(0).asString().toDoubleOrNull() ?: 0.0) },
        "to_int" to java.util.function.Function { params -> DoubleValue(params.get<MoValue>(0).asString().toIntOrNull() ?: 0) },
        "to_string" to java.util.function.Function { params -> StringValue(params.get<MoValue>(0).asString()) },
        "do_effect_walks" to java.util.function.Function { _ ->
            DoubleValue(Cobblemon.config.walkingInBattleAnimations)
        },
        "random" to java.util.function.Function { params ->
            val options = mutableListOf<MoValue>()
            var index = 0
            while (params.contains(index)) {
                options.add(params.get(index))
                index++
            }
            return@Function options.random() // Can throw an exception if they specified no args. They'd be idiots though.
        },
        "curve" to java.util.function.Function { params ->
            val curveName = params.getString(0)
            val curve = WaveFunctions.functions[curveName] ?: throw IllegalArgumentException("Unknown curve: $curveName")
            return@Function ObjectValue(curve)
        },
        "array" to java.util.function.Function { params ->
            val values = params.params
            val array = ArrayStruct(hashMapOf())
            values.forEachIndexed { index, moValue -> array.setDirectly("$index", moValue) }
            return@Function array
        },
        "length" to java.util.function.Function { params ->
            val array = (params.getOrNull<MoValue>(0) as? VariableStruct) ?: return@Function DoubleValue.ZERO
            return@Function DoubleValue(array.map.size.toDouble())
        },
        "append" to java.util.function.Function { params ->
            val array = params.get<ArrayStruct>(0)
            val value = params.get<MoValue>(1)
            val nextIndex = array.map.size
            array.setDirectly("$nextIndex", value)
            return@Function array
        },
        "insert" to java.util.function.Function { params ->
            val array = params.get<ArrayStruct>(0)
            val index = params.getInt(1)
            val value = params.get<MoValue>(2)
            val size = array.map.size

            // Shift elements at and after index up by 1
            for (i in (size - 1) downTo index) {
                val current = array.map[i.toString()]
                if (current != null) {
                    array.map[(i + 1).toString()] = current
                }
            }
            array.map[index.toString()] = value
            return@Function array
        },
        "delete" to java.util.function.Function { params ->
            val array = params.get<ArrayStruct>(0)
            val index = params.getInt(1)
            if (index in 0 until array.map.size) {
                array.map.remove(index.toString())
                // Re-index the array to keep keys sequential in numerical order
                val newMap = hashMapOf<String, MoValue>()
                array.map.keys.mapNotNull { it.toIntOrNull() }
                    .sorted()
                    .forEachIndexed { i, k -> newMap[i.toString()] = array.map[k.toString()]!! }
                array.map.clear()
                array.map.putAll(newMap)
            }
            return@Function array
        },
        "run_script" to java.util.function.Function { params ->
            val runtime = MoLangRuntime()
            runtime.environment.query = params.environment.query
            runtime.environment.variable = params.environment.variable
            val args = params.params.subList(1, params.params.size)
            runtime.environment.context = ContextStruct(
                (params.environment.context?.map ?: emptyMap()) + args.mapIndexed { index, value -> "arg_${index + 1}" to value }.toMap()
            )
            val script = params.getString(0).asIdentifierDefaultingNamespace()
            // store the args in the
            CobblemonScripts.run(script, runtime) ?: DoubleValue.ZERO
        },
        "run_molang" to java.util.function.Function { params ->
            val runtime = MoLangRuntime()
            runtime.environment.query = params.environment.query
            runtime.environment.variable = params.environment.variable
            runtime.environment.context = params.environment.context
            val expression = params.getString(0).asExpressionLike()
            val delayInSeconds = params.getDoubleOrNull(1)?.toFloat() ?: 0.0f

            fun evaluate(): MoValue {
                return try {
                    runtime.resolve(expression)
                } catch (ex: Exception) {
                    Cobblemon.LOGGER.warn("Failed to evaluate MoLang expression${if (delayInSeconds > 0.0f) " (delayed)" else ""}: $expression", ex)
                    DoubleValue.ZERO
                }
            }

            if (delayInSeconds > 0.0f) {
                val tracker = if (Cobblemon.implementation.environment() == Environment.SERVER) ServerTaskTracker else ClientTaskTracker
                tracker.after(delayInSeconds) {
                    evaluate()
                }
                DoubleValue.ONE
            } else {
                evaluate()
            }
        },
        "system_time_millis" to java.util.function.Function { _ ->
            DoubleValue(System.currentTimeMillis())
        },
        // the rest of the world use dd/MM/yyyy grow up america (this comment was generated by copilot)
        "date_local_time" to java.util.function.Function { _ ->
            val time = System.currentTimeMillis()
            val date = java.util.Date(time)
            val formatted = java.text.SimpleDateFormat("dd/MM/yyyy").format(date)
            StringValue(formatted)
        },
        "date_of" to java.util.function.Function { params ->
            val time = params.getDouble(0).toLong()
            val date = java.util.Date(time)
            val formatted = java.text.SimpleDateFormat("dd/MM/yyyy").format(date)
            StringValue(formatted)
        },
        "date_is_after" to java.util.function.Function { params ->
            val dateA = params.getString(0)
            val dateB = params.getString(1)
            val format = java.text.SimpleDateFormat("dd/MM/yyyy")
            val a = format.parse(dateA)
            val b = format.parse(dateB)
            DoubleValue(a.after(b))
        },
        "get_move_from_id" to java.util.function.Function { params ->
            val moveId = params.getString(0)
            val moveTemplate = Moves.getByName(moveId)
            return@Function moveTemplate?.struct ?: DoubleValue.ZERO
        },
        "create_simple_party_provider" to java.util.function.Function { params ->
            val partyProvider = SimplePartyProvider()
            return@Function partyProvider.struct
        },
        "create_pickup_item" to java.util.function.Function { params ->
            val item = params.getOrNull<MoValue>(0)?.asString()?.let(ObtainableItemCondition::parseFromString)
            val pickupPriority = params.getIntOrNull(1) ?: 0
            val pickupItem = ObtainableItem(item = item, pickupPriority = pickupPriority)
            return@Function pickupItem.struct
        },
        "create_itemstack" to java.util.function.Function { params ->
            val itemId = params.getString(0).asIdentifierDefaultingNamespace()
            val item = BuiltInRegistries.ITEM.get(itemId) ?: return@Function DoubleValue.ZERO
            val count = params.getIntOrNull(1) ?: 1
            val itemStack = ItemStack(item, count)
            return@Function ObjectValue(itemStack)
        },
        "file" to java.util.function.Function { MoLangLoadedFilesCache.struct }
    )
    val biomeFunctions = mutableListOf<(Holder<Biome>) -> HashMap<String, java.util.function.Function<MoParams, Any>>>()
    val worldFunctions = mutableListOf<(Holder<Level>) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { worldHolder ->
            val world = worldHolder.value()
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("is_time_of_day") { params ->
                val timeOfDay = TimeRange.timeRanges[params.getString(0).lowercase()]
                    ?: return@put DoubleValue.ZERO
                val time = world.dayTime % 24000
                return@put DoubleValue(timeOfDay.contains(time.toInt()))
            }
            map.put("game_time") { _ -> DoubleValue(world.gameTime.toDouble()) }
            map.put("time_of_day") {
                val time = world.dayTime % 24000
                return@put DoubleValue(time.toDouble())
            }
            map.put("server") { _ -> server()?.asMoLangValue() ?: DoubleValue.ZERO }
            map.put("is_raining_at") { params ->
                val x = params.getInt(0)
                val y = params.getInt(1)
                val z = params.getInt(2)
                return@put DoubleValue(world.isRainingAt(BlockPos(x, y, z)))
            }
            map.put("is_snowing_at") { params ->
                val x = params.getInt(0)
                val y = params.getInt(1)
                val z = params.getInt(2)
                val blockPos = BlockPos(x, y, z)
                if (!world.isRaining()) {
                    return@put DoubleValue.ZERO
                } else if (!world.canSeeSky(blockPos)) {
                    return@put DoubleValue.ZERO
                } else if (world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).getY() > blockPos.getY()) {
                    return@put DoubleValue.ZERO
                } else {
                    val biome = world.getBiome(blockPos).value() as Biome
                    return@put DoubleValue(biome.getPrecipitationAt(blockPos) == Biome.Precipitation.SNOW)
                }
            }
            map.put("is_chunk_loaded_at") { params ->
                val x = params.getInt(0)
                val y = params.getInt(1)
                val z = params.getInt(2)
                return@put DoubleValue(world.isLoaded(BlockPos(x, y, z)))
            }
            map.put("is_thundering") { _ -> DoubleValue(world.isThundering) }
            map.put("is_raining") { _ -> DoubleValue(world.isRaining) }
            map.put("set_block") { params ->
                val x = params.getInt(0)
                val y = params.getInt(1)
                val z = params.getInt(2)
                val block = world.blockRegistry.get(params.getString(3).asIdentifierDefaultingNamespace())
                    ?: run {
                        Cobblemon.LOGGER.error("Unknown block: ${params.getString(3)}")
                        return@put DoubleValue.ZERO
                    }
                world.setBlock(BlockPos(x, y, z), block.defaultBlockState(), Block.UPDATE_ALL)
            }
            map.put("is_air") { params ->
                val x = params.getDouble(0).toInt()
                val y = params.getDouble(1).toInt()
                val z = params.getDouble(2).toInt()
                val blockState = world.getBlockState(BlockPos(x, y, z))
                return@put DoubleValue(blockState.isAir)
            }
            map.put("get_block") { params ->
                val x = params.getInt(0)
                val y = params.getInt(1)
                val z = params.getInt(2)
                val block = world.getBlockState(BlockPos(x, y, z)).block
                return@put world.blockRegistry.wrapAsHolder(block).asMoLangValue(Registries.BLOCK)
            }
            map.put("spawn_explosion") { params ->
                val x = params.getDouble(0)
                val y = params.getDouble(1)
                val z = params.getDouble(2)
                val range = params.getDouble(3).toFloat()
                world.explode(null, x, y, z, range, ExplosionInteraction.valueOf(params.getStringOrNull(4)?.uppercase() ?: ExplosionInteraction.TNT.name))
            }
            map.put("spawn_lightning") { params ->
                val x = params.getDouble(0)
                val y = params.getDouble(1)
                val z = params.getDouble(2)
                val lightning = LightningBolt(EntityType.LIGHTNING_BOLT, world)
                lightning.setPos(x, y, z)
                world.addFreshEntity(lightning)
                return@put DoubleValue.ONE
            }
            // q.entity.world.spawn_bedrock_particles(effect, x, y, z, [player]) - sends to everyone nearby or just to the player if they're set.
            map.put("spawn_bedrock_particles") { params ->
                val particle = params.getString(0).asResource()
                val x = params.getDouble(1)
                val y = params.getDouble(2)
                val z = params.getDouble(3)
                val player = params.getOrNull<MoValue>(4)?.let {
                    if (it is StringValue) world.getPlayerByUUID(UUID.fromString(it.value))
                    else if (it is ObjectValue<*>) it.obj
                    else null
                } as? ServerPlayer
                val pos = Vec3(x, y, z)

                val packet = SpawnSnowstormParticlePacket(particle, pos)
                if (player != null) {
                    packet.sendToPlayer(player)
                } else {
                    packet.sendToPlayersAround(x, y, z, 64.0, world.dimension())
                }
            }
            map.put("spawn_pokemon") { params ->
                val x = params.getInt(0)
                val y = params.getInt(1)
                val z = params.getInt(2)
                val props = params.getString(3).toProperties()

                val pos = BlockPos(x, y, z)

                if (!Level.isInSpawnableBounds(pos)) {
                    return@put DoubleValue.ZERO
                }

                val pokemon = props.createEntity(world)
                pokemon.moveTo(pos, pokemon.yRot, pokemon.xRot)

                if (world.addFreshEntity(pokemon)) {
                    return@put pokemon.struct
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map.put("spawn_npc") { params ->
                val x = params.getDouble(0)
                val y = params.getDouble(1)
                val z = params.getDouble(2)
                val npcClass = params.getStringOrNull(3)?.let { NPCClasses.getByIdentifier(it.asIdentifierDefaultingNamespace()) }
                val level = params.getInt(4)
                if(npcClass == null) return@put DoubleValue.ZERO
                val npc = NPCEntity(world)
                npc.moveTo(x, y, z, npc.yRot, npc.xRot)
                npc.npc = npcClass
                npc.initialize(level)
                if (world.addFreshEntity(npc)) {
                    return@put npc.asMoLangValue()
                }
                return@put DoubleValue.ZERO
            }
            map.put("play_sound_on_server") { params ->
                val sound = params.getString(0).asResource()
                val soundSource = params.getString(1).uppercase()
                val x = params.getDouble(2)
                val y = params.getDouble(3)
                val z = params.getDouble(4)
                val player = params.getOrNull<MoValue>(5)?.let {
                    if (it is StringValue) world.getPlayerByUUID(UUID.fromString(it.value))
                    else if (it is ObjectValue<*>) it.obj
                    else null
                } as? ServerPlayer
                val volume = params.getDoubleOrNull(6)?.toFloat() ?: 1.0f
                val pitch = params.getDoubleOrNull(7)?.toFloat() ?: 1.0f

                val packet = UnvalidatedPlaySoundS2CPacket(sound, SoundSource.valueOf(soundSource), x, y, z, volume, pitch)
                if (player != null) {
                    packet.sendToPlayer(player)
                } else {
                    packet.sendToPlayersAround(x, y, z, 16.0, world.dimension())
                }
            }
            map.put("get_entities_around") { params ->
                val x = params.getDouble(0)
                val y = params.getDouble(1)
                val z = params.getDouble(2)
                val range = params.getDouble(3) * 2
                val entities = world.getEntities(null, AABB.ofSize(Vec3(x, y, z), range, range, range))
                return@put entities
                    .filterIsInstance<LivingEntity>()
                    .map { it.asMostSpecificMoLangValue() }
                    .asArrayValue()
            }
            map.put("is_healer_in_use") { params ->
                val pos = params.get<ArrayStruct>(0).asBlockPos()
                val healer = world.getBlockEntity(pos, CobblemonBlockEntities.HEALING_MACHINE).orElse(null) ?: return@put DoubleValue.ONE
                return@put DoubleValue(healer.isInUse)
            }

            return@mutableListOf map
        }
    )
    val dimensionTypeFunctions = mutableListOf<(Holder<DimensionType>) -> HashMap<String, java.util.function.Function<MoParams, Any>>>()
    val blockFunctions = mutableListOf<(Holder<Block>) -> HashMap<String, java.util.function.Function<MoParams, Any>>>()
    val playerFunctions = mutableListOf<(Player) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { player ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("username") { _ -> StringValue(player.gameProfile.name) }
            map.put("uuid") { _ -> StringValue(player.gameProfile.id.toString()) }
            map.put("main_held_item") { _ -> player.mainHandItem.asMoLangValue(player.registryAccess()) }
            map.put("off_held_item") { _ -> player.offhandItem.asMoLangValue(player.registryAccess()) }
            map.put("inventory") { _ ->
                val inventory = player.inventory
                val items = ArrayStruct(hashMapOf())
                for (i in 0 until inventory.containerSize) {
                    items.setDirectly("$i", inventory.getItem(i).asMoLangValue(player.registryAccess()))
                }
                return@put items
            }
            map.put("has_inventory_space") { _ ->
                val inventory = player.inventory
                return@put if (inventory.getFreeSlot() != -1) DoubleValue.ONE else DoubleValue.ZERO
            }
            map.put("set_inventory_slot") { params ->
                val slot = params.getInt(0)
                val inventory = player.inventory
                if (slot !in 0 until inventory.containerSize) return@put DoubleValue.ZERO

                val value = params.getOrNull<MoValue>(1) ?: return@put DoubleValue.ZERO

                val stack: ItemStack? = when (value) {
                    is ObjectValue<*> -> value.obj as? ItemStack
                    is StringValue -> {
                        val id = ResourceLocation.parse(value.value)
                        val item = player.registryAccess().registryOrThrow(Registries.ITEM).get(id) ?: return@put DoubleValue.ZERO
                        ItemStack(item)
                    }
                    else -> null
                }

                if (stack == null) return@put DoubleValue.ZERO

                inventory.setItem(slot, stack)
                return@put DoubleValue.ONE
            }
            map.put("give_item") { params ->
                val inventory = player.inventory
                val freeSlot = inventory.getFreeSlot()
                val value = params.getOrNull<MoValue>(0) ?: return@put DoubleValue.ZERO
                val dropIfFull = params.getBooleanOrNull(1) ?: false

                val stack: ItemStack? = when (value) {
                    is ObjectValue<*> -> value.obj as? ItemStack
                    is StringValue -> {
                        val id = ResourceLocation.parse(value.value)
                        val item = player.registryAccess().registryOrThrow(Registries.ITEM).get(id) ?: return@put DoubleValue.ZERO
                        ItemStack(item)
                    }
                    else -> null
                }

                if (stack == null) return@put DoubleValue.ZERO

                if (freeSlot == -1) {
                    if (dropIfFull) {
                        player.drop(stack, false)
                        return@put DoubleValue.ONE
                    } else {
                        return@put DoubleValue.ZERO
                    }
                }

                inventory.setItem(freeSlot, stack)
                return@put DoubleValue.ONE
            }
            map.put("face") { params -> ObjectValue(PlayerDialogueFaceProvider(player.uuid, params.getBooleanOrNull(0) != false)) }
            map.put("swing_hand") { _ -> player.swing(player.usedItemHand) }
            map.put("food_level") { _ -> DoubleValue(player.foodData.foodLevel) }
            map.put("saturation_level") { _ -> DoubleValue(player.foodData.saturationLevel) }
            map.put("tell") { params ->
                val message = params.getString(0).text()
                val overlay = params.getBooleanOrNull(1) == true
                player.displayClientMessage(message, overlay)
            }
            map.put("teleport") { params ->
                val x = params.getDouble(0)
                val y = params.getDouble(1)
                val z = params.getDouble(2)
                val playParticleOptionss = params.getBooleanOrNull(3) ?: false
                player.randomTeleport(x, y, z, playParticleOptionss)
            }
            map.put("heal") { params ->
                val amount = params.getDoubleOrNull(0) ?: player.maxHealth
                player.heal(amount.toFloat())
            }
            map.put("environment") {
                val environment = MoLangEnvironment()
                environment.query = player.asMoLangValue()
                environment
            }
            map.put("is_player") { DoubleValue.ONE }
            map.put("riding_pokemon") {
                val vehicle = player.vehicle
                if (vehicle is PokemonEntity) {
                    return@put vehicle.struct
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            if (player is ServerPlayer) {
                map.put("seen_credits") { _ ->
                    DoubleValue(player.seenCredits)
                }
                map.put("is_in_dialogue") { _ ->
                    DoubleValue(player.isInDialogue)
                }
                map.put("active_dialogue") { _ ->
                    if (player.isInDialogue) {
                        return@put player.activeDialogue?.toMoLangStruct() ?: DoubleValue.ZERO
                    } else {
                        DoubleValue.ZERO
                    }
                }
                map.put("is_spectator") { DoubleValue(player.isSpectator) }
                map.put("is_creative") { DoubleValue(player.isCreative) }
                map.put("is_survival") { DoubleValue(player.gameMode.isSurvival) }
                map.put("is_adventure") { DoubleValue(player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) }
                map.put("run_command") { params ->
                    val command = params.getString(0)
                    player.server.commands.performPrefixedCommand(player.createCommandSourceStack(), command)
                }
                map.put("set_battle_theme") { params ->
                    val soundId = params.getString(0).asResource()
                    Cobblemon.playerDataManager.getGenericData(player).battleTheme = soundId
                    return@put DoubleValue.ONE
                }
                map.put("battle_music") { params ->
                    val soundId = params.getString(0).asResource()
                    val volume = params.getDoubleOrNull(1)?.toFloat() ?: 1.0f
                    val pitch = params.getDoubleOrNull(2)?.toFloat() ?: 1.0f
                    val restart = params.getBooleanOrNull(3) ?: true

                    val music = soundId
                    if (music != null) {
                        val packet = BattleMusicPacket(music, volume, pitch, restart)
                        packet.sendToPlayer(player)
                        return@put DoubleValue.ONE
                    } else {
                        return@put DoubleValue.ZERO
                    }
                }
                map.put("stop_battle_music") { _ ->
                    val packet = BattleMusicPacket(null)
                    packet.sendToPlayer(player)
                    return@put DoubleValue.ONE
                }
                map.put("play_sound_on_server") { params ->
                    val sound = params.getString(0).asResource()
                    val soundSource = SoundSource.valueOf(params.getString(1).uppercase())
                    val volume = params.getDoubleOrNull(2)?.toFloat() ?: 1.0f
                    val pitch = params.getDoubleOrNull(3)?.toFloat() ?: 1.0f

                    val packet = UnvalidatedPlaySoundS2CPacket(sound, soundSource, player.x, player.y, player.z, volume, pitch)
                    packet.sendToPlayer(player)
                }
                map.put("is_party_at_full_health") { _ ->
                    DoubleValue(player.party().none(Pokemon::canBeHealed)) }
                map.put("can_heal_at_healer") { params ->
                    val pos = params.get<ArrayStruct>(0).asBlockPos()
                    val healer = player.level().getBlockEntity(pos, CobblemonBlockEntities.HEALING_MACHINE).orElse(null) ?: return@put DoubleValue.ZERO
                    val party = player.party()
                    return@put DoubleValue(healer.canHeal(party))
                }
                map.put("put_pokemon_in_healer") { params ->
                    val healer = player.level()
                        .getBlockEntity(params.get<ArrayStruct>(0).asBlockPos(), CobblemonBlockEntities.HEALING_MACHINE)
                        .orElse(null) ?: return@put DoubleValue.ZERO
                    val party = player.party()
                    if (healer.canHeal(party)) {
                        healer.activate(player.uuid, party)
                        return@put DoubleValue.ONE
                    } else {
                        return@put DoubleValue.ZERO
                    }
                }
                map.put("party") { player.party().struct }
                map.put("pc") { player.pc().struct }
                map.put("has_permission") { params -> DoubleValue(Cobblemon.permissionValidator.hasPermission(player, params.getString(0), params.getIntOrNull(1) ?: 4)) }
                map.put("data") { params -> Cobblemon.molangData.load(player.uuid, params.getStringOrNull(0)) }
                map.put("save_data") { params -> Cobblemon.molangData.save(player.uuid, params.getStringOrNull(0)) }
                map.put("in_battle") { DoubleValue(player.isInBattle()) }
                map.put("battle") { player.getBattleState()?.first?.struct ?: DoubleValue.ZERO }
                map.put("get_npc_data") { params ->
                    val npcId = ((params.get<MoValue>(0) as? ObjectValue<*>)?.obj as? NPCEntity)?.stringUUID ?: params.getString(0)
                    val data = Cobblemon.molangData.load(player.uuid, params.getStringOrNull(1))
                    if (data.map.containsKey(npcId)) {
                        return@put data.map[npcId]!!
                    } else {
                        val vars = VariableStruct()
                        data.map[npcId] = vars
                        return@put vars
                    }
                }
                map.put("get_npc_variable") { params ->
                    val npcId = ((params.get<MoValue>(0) as? ObjectValue<*>)?.obj as? NPCEntity)?.stringUUID ?: params.getString(0)
                    val variable = params.getString(1)
                    val data = Cobblemon.molangData.load(player.uuid, params.getStringOrNull(2))
                    if (data.map.containsKey(npcId)) {
                        return@put (data.map[npcId] as VariableStruct).map[variable] ?: DoubleValue.ZERO
                    } else {
                        return@put DoubleValue.ZERO
                    }
                }
                map.put("set_npc_variable") { params ->
                    val npcId = ((params.get<MoValue>(0) as? ObjectValue<*>)?.obj as? NPCEntity)?.stringUUID ?: params.getString(0)
                    val variable = params.getString(1)
                    val value = params.get<MoValue>(2)
                    val saveAfterwards = params.getBooleanOrNull(3) != false
                    val path = params.getStringOrNull(4)
                    val data = Cobblemon.molangData.load(player.uuid, path)
                    val npcData = data.map.getOrPut(npcId) { VariableStruct() } as VariableStruct
                    npcData.map[variable] = value
                    if (saveAfterwards) {
                        Cobblemon.molangData.save(player.uuid, path)
                    }
                    return@put DoubleValue.ONE
                }
                map.put("pokedex") { player.pokedex().struct }
                map.put("has_advancement") { params ->
                    val requiredAdvancement = ResourceLocation.parse(params.getString(0))
                    for (entry in player.advancements.progress) {
                        if (entry.key.id == requiredAdvancement && entry.value.isDone) {
                            return@put DoubleValue.ONE
                        }
                    }
                    return@put DoubleValue.ZERO
                }
                map.put("start_battle") { params ->
                    val opponentValue = params.get<MoValue>(0)
                    val opponent = if (opponentValue is ObjectValue<*>) {
                        opponentValue.obj as ServerPlayer
                    } else {
                        val paramString = opponentValue.asString()
                        val playerUUID = paramString.asUUID
                        if (playerUUID != null) {
                            server()?.playerList?.getPlayer(playerUUID) ?: return@put DoubleValue.ZERO
                        } else {
                            server()?.playerList?.getPlayerByName(paramString) ?: return@put DoubleValue.ZERO
                        }
                    }
                    val format = params.getStringOrNull(1)
                        ?.let(BattleFormat::fromFormatIdentifier)
                        ?: BattleFormat.GEN_9_SINGLES

                    val setLevel = params.getIntOrNull(2) ?: -1
                    format.adjustLevel = setLevel

                    val rules = params.getStringOrNull(5)
                        ?.split(",")
                        ?.toSet()
                        ?: emptySet()

                    val modifiedBattleFormat = BattleFormat.setBattleRules(
                        battleFormat = format,
                        rules = rules
                    )

                    val cloneParties = (setLevel != -1) || (params.getBooleanOrNull(3) ?: false)
                    val healFirst = params.getBooleanOrNull(4) ?: false

                    val battleStartResult = BattleBuilder.pvp1v1(
                        player1 = player,
                        player2 = opponent,
                        battleFormat = modifiedBattleFormat,
                        cloneParties = cloneParties,
                        healFirst = healFirst
                    )
                    var returnValue: MoValue = DoubleValue.ZERO
                    battleStartResult.ifSuccessful { returnValue = it.struct }
                    return@put returnValue
                }
                map.put("get_custom_stat") { params ->
                    val statName = params.getString(0)

                    val resourceLocation = ResourceLocation.tryParse(statName) ?: return@put DoubleValue.ZERO
                    val statResourceLocation = BuiltInRegistries.CUSTOM_STAT.get(resourceLocation) ?: return@put DoubleValue.ZERO

                    val exists = net.minecraft.stats.Stats.CUSTOM.contains(statResourceLocation)
                    if (!exists) return@put DoubleValue.ZERO

                    val stat = net.minecraft.stats.Stats.CUSTOM.get(statResourceLocation)
                    val value = player.stats.getValue(stat)

                    DoubleValue(value)
                }
            }
            map
        }
    )
    val itemStackFunctions = mutableListOf<(ItemStack, RegistryAccess) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { stack, registryAccess ->
            val itemRegistry = registryAccess.registryOrThrow(Registries.ITEM)
            val holder = itemRegistry.wrapAsHolder(stack.item)

            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("item") { _ -> holder.asMoLangValue(Registries.ITEM) }
            map.put("count") { _ -> DoubleValue(stack.count.toDouble()) }
            map.put("damage_value") { _ -> DoubleValue(stack.damageValue) }
            map.put("max_damage") { _ -> DoubleValue(stack.maxDamage) }
            map.put("is_empty") { _ -> DoubleValue(stack.isEmpty) }
            map.put("shrink") { params -> stack.shrink(params.getInt(0)) }
            map.put("grow") { params -> stack.grow(params.getInt(0)) }
            map.put("is_of") { params -> DoubleValue(holder.`is`(params.getString(0).asIdentifierDefaultingNamespace())) }
            map.put("is_in") { params -> DoubleValue(holder.`is`(TagKey.create(Registries.ITEM, params.getString(0).replace("#", "").asIdentifierDefaultingNamespace()))) }
            map.put("is_food") { params -> DoubleValue(stack.has(DataComponents.FOOD)) }
            return@mutableListOf map
        }
    )

    val entityFunctions: MutableList<(Entity) -> HashMap<String, java.util.function.Function<MoParams, Any>>> = mutableListOf(
        { entity ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("uuid") { _ -> StringValue(entity.uuid.toString()) }
            map.put("set_name") { params ->
                val name = params.getString(0)
                entity.customName = name.text()
                return@put DoubleValue.ONE
            }
            map.put("damage") { params ->
                val amount = params.getDouble(0)
                val source = DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(DamageTypes.GENERIC).get())
                entity.hurt(source, amount.toFloat())
            }
            map.put("distance_to_owner") {
                if (entity !is TamableAnimal) {
                    return@put DoubleValue.ZERO
                }
                val owner = entity.owner ?: return@put DoubleValue.ZERO
                return@put DoubleValue(entity.distanceTo(owner))
            }
            map.put("owner") {
                if (entity !is TamableAnimal) {
                    return@put DoubleValue.ZERO
                }
                val owner = entity.owner
                return@put owner?.asMostSpecificMoLangValue() ?: DoubleValue.ZERO
            }
            map.put("delta_movement") {
                return@put listOf(entity.deltaMovement.x, entity.deltaMovement.y, entity.deltaMovement.z).asArrayValue(::DoubleValue)
            }
            map.put("tags") {
                val tags = entity.tags
                val array = ArrayStruct(hashMapOf())
                tags.forEachIndexed { index, tag -> array.setDirectly("$index", StringValue(tag)) }
                return@put array
            }
            map.put("add_tag") { params ->
                val tag = params.getString(0)
                entity.addTag(tag)
                return@put DoubleValue.ONE
            }
            map.put("remove_tag") { params ->
                val tag = params.getString(0)
                entity.removeTag(tag)
                return@put DoubleValue.ONE
            }
            map.put("has_tag") { params ->
                val tag = params.getString(0)
                return@put DoubleValue(entity.tags.contains(tag))
            }
            if (entity is MoLangScriptingEntity) {
                map.put("add_callback") { params ->
                    val type = params.getString(0).asIdentifierDefaultingNamespace()
                    val callback = params.getString(1).asIdentifierDefaultingNamespace()
                    val allowDuplicates = params.getBooleanOrNull(2) ?: false
                    entity.callbacks.addCallback(type, callback, allowDuplicates)
                }
                map.put("remove_callback") { params ->
                    val type = params.getString(0).asIdentifierDefaultingNamespace()
                    val callback = params.getString(1).asIdentifierDefaultingNamespace()
                    entity.callbacks.removeCallback(type, callback)
                }
                map.put("has_callback") { params ->
                    val type = params.getString(0).asIdentifierDefaultingNamespace()
                    val callback = params.getString(1).asIdentifierDefaultingNamespace()
                    return@put DoubleValue(callback in (entity.callbacks[type] ?: emptySet()))
                }
                map.put("clear_callbacks") { params ->
                    val type = params.getStringOrNull(0)?.asIdentifierDefaultingNamespace()
                    if (type != null) {
                        entity.callbacks[type]?.clear()
                    } else {
                        entity.callbacks.clear()
                    }
                    return@put DoubleValue.ONE
                }
            }

            if (entity is PathfinderMob) {
                map.put("walk_to") { params ->
                    val x = params.getDouble(0)
                    val y = params.getDouble(1)
                    val z = params.getDouble(2)
                    val speedMultiplier = params.getDoubleOrNull(3) ?: 0.35
                    if (entity.brain.checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED)) {
                        entity.brain.setMemory(MemoryModuleType.WALK_TARGET, WalkTarget(Vec3(x, y, z), speedMultiplier.toFloat(), 1))
                        entity.brain.setMemory(MemoryModuleType.LOOK_TARGET, BlockPosTracker(Vec3(x, y + entity.eyeHeight, z)))
                    } else {
                        entity.navigation.moveTo(x, y, z, speedMultiplier)
                        entity.lookControl.setLookAt(Vec3(x, y + entity.eyeHeight, z))
                    }
                }

                map.put("has_walk_target") { _ ->
                    DoubleValue(entity.brain.getMemory(MemoryModuleType.WALK_TARGET).isPresent || entity.isPathFinding)
                }

                map.put("set_can_float") { params ->
                    val canFloat = params.getBooleanOrNull(0) != false
                    entity.navigation.setCanFloat(canFloat)
                    DoubleValue.ONE
                }

                map.put("get_pathfinding_malus") { params ->
                    val type = PathType.entries.find { it.name == params.getString(0).uppercase() }
                    if (type != null) {
                        return@put DoubleValue(entity.getPathfindingMalus(type))
                    } else {
                        Cobblemon.LOGGER.error("Unknown pathfinding type: ${params.getString(0)}")
                        return@put DoubleValue.ZERO
                    }
                }
                map.put("set_pathfinding_malus") { params ->
                    val type = PathType.entries.find { it.name == params.getString(0).uppercase() }
                    val malus = params.getDouble(1).toFloat()
                    if (type != null) {
                        entity.setPathfindingMalus(type, malus)
                        return@put DoubleValue.ONE
                    } else {
                        Cobblemon.LOGGER.error("Unknown pathfinding type: ${params.getString(0)}")
                        return@put DoubleValue.ZERO
                    }
                }
            }

            map.put("is_sneaking") { _ -> DoubleValue(entity.isShiftKeyDown) }
            map.put("is_sprinting") { _ -> DoubleValue(entity.isSprinting) }
            map.put("is_in_water") { _ -> DoubleValue(entity.isUnderWater) }
            map.put("is_in_rain") { _ -> DoubleValue(entity.isInWaterOrRain && !entity.isInWater) }
            map.put("is_touching_water_or_rain") { _ -> DoubleValue(entity.isInWaterRainOrBubble) }
            map.put("is_touching_water") { _ -> DoubleValue(entity.isInWater) }
            map.put("is_underwater") { DoubleValue(entity.getIsSubmerged()) }
            map.put("is_in_lava") { _ -> DoubleValue(entity.isInLava) }
            map.put("is_on_fire") { _ -> DoubleValue(entity.isOnFire) }
            map.put("is_invisible") { _ -> DoubleValue(entity.isInvisible) }
            map.put("is_riding") { _ -> DoubleValue(entity.isPassenger) }
            map.put("distance_to_pos") { params ->
                val x = params.getDouble(0)
                val y = params.getDouble(1)
                val z = params.getDouble(2)
                return@put DoubleValue(sqrt(entity.distanceToSqr(Vec3(x, y, z))))
            }
            map.put("name") { _ -> StringValue(entity.effectiveName().string) }
            map.put("type") { _ ->
                entity.registryAccess().registry(Registries.ENTITY_TYPE).get().getKey(entity.type)?.toString()?.let {
                    StringValue(it)
                } ?: DoubleValue.ZERO
            }
            map.put("yaw") { _ -> DoubleValue(entity.yRot.toDouble()) }
            map.put("pitch") { _ -> DoubleValue(entity.xRot.toDouble()) }
            map.put("x") { _ -> DoubleValue(entity.x) }
            map.put("y") { _ -> DoubleValue(entity.y) }
            map.put("z") { _ -> DoubleValue(entity.z) }
            map.put("velocity_x") { _ -> DoubleValue(entity.deltaMovement.x) }
            map.put("velocity_y") { _ -> DoubleValue(entity.deltaMovement.y) }
            map.put("velocity_z") { _ -> DoubleValue(entity.deltaMovement.z) }
            map.put("width") { DoubleValue(entity.boundingBox.xsize) }
            map.put("height") { DoubleValue(entity.boundingBox.ysize) }
            map.put("entity_size") { DoubleValue(entity.boundingBox.run { if (xsize > ysize) xsize else ysize }) }
            map.put("entity_width") { DoubleValue(entity.boundingBox.xsize) }
            map.put("entity_height") { DoubleValue(entity.boundingBox.ysize) }
            map.put("id_modulo") { params -> DoubleValue(entity.uuid.hashCode() % params.getDouble(0)) }
            map.put("horizontal_velocity") { _ -> DoubleValue(entity.deltaMovement.horizontalDistance()) }
            map.put("vertical_velocity") { DoubleValue(entity.deltaMovement.y) }
            map.put("is_on_ground") { _ -> DoubleValue(entity.onGround()) }
            map.put("world") { _ -> entity.level().worldRegistry.wrapAsHolder(entity.level()).asWorldMoLangValue() }
            map.put("biome") { _ -> entity.level().getBiome(entity.blockPosition()).asBiomeMoLangValue() }
            map.put("is_passenger") { DoubleValue(entity.isPassenger) }
            map.put("find_nearby_block") { params ->
                val input = params.getString(0)
                val isTag = input.contains("#")
                val type = input.replace("#", "").asIdentifierDefaultingNamespace(namespace = "minecraft")
                val range = params.getDoubleOrNull(1) ?: 10
                val blockPos = entity.level().getBlockStatesWithPos(AABB.ofSize(entity.position(), range.toDouble(), range.toDouble(), range.toDouble()))
                    .filter { it.first.blockHolder.let { if (isTag) it.`is`(TagKey.create(Registries.BLOCK, type)) else it.`is`(type) } }
                    .minByOrNull { it.second.distSqr(entity.blockPosition()) }
                    ?.second
                if (blockPos != null) {
                    return@put ArrayStruct(mapOf("0" to DoubleValue(blockPos.x), "1" to DoubleValue(blockPos.y), "2" to DoubleValue(blockPos.z)))
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map.put("get_nearby_entities") { params ->
                val distance = params.getDouble(0)
                val entities = entity.level().getEntities(entity, AABB.ofSize(entity.position(), distance, distance, distance))
                return@put entities
                    .filterIsInstance<Entity>()
                    .map { it.asMostSpecificMoLangValue() }
                    .asArrayValue()
            }
            map.put("is_standing_on_blocks") { params ->
                val depth = params.getDouble(0).toInt()
                val blockStrings: MutableSet<String> = mutableSetOf()
                for (blockIndex in 1..<params.params.size) {
                    blockStrings.add(params.getString(blockIndex))
                }

                return@put if (entity.isStandingOn(blockStrings, depth)) DoubleValue.ONE else DoubleValue.ZERO
            }
            map.put("discard") {
                entity.discard()
            }
            if (entity is PosableEntity) {
                map.put("play_animation") { params ->
                    val animation = params.getString(0)
                    val packet = PlayPosableAnimationPacket(entity.id, setOf(animation), emptyList())
                    val target = params.getStringOrNull(1)
                    if (target != null) {
                        val targetPlayer = if (target.asUUID != null) {
                            entity.level().getPlayerByUUID(target.asUUID!!) as ServerPlayer
                        } else if (entity.level() is ServerLevel) {
                            entity.level().server!!.playerList.getPlayerByName(target)
                        } else {
                            null
                        }
                        if (targetPlayer != null) {
                            packet.sendToPlayer(targetPlayer)
                            return@put DoubleValue.ONE
                        } else {
                            return@put DoubleValue.ZERO
                        }
                    } else {
                        packet.sendToPlayersAround(entity.x, entity.y, entity.z, 64.0, entity.level().dimension())
                        return@put DoubleValue.ONE
                    }
                }

                map.put("face") { params -> ObjectValue(ReferenceDialogueFaceProvider(entity.id, params.getBooleanOrNull(0) != false))

                }
            }
            if (entity is Schedulable) {
                map.put("run_molang_after") { params ->
                    val expression = params.getString(0).asExpressionLike()
                    val delayInSeconds = params.getDouble(1).toFloat()
                    val runtime = MoLangRuntime()
                    runtime.environment.cloneFrom(params.environment)
                    entity.after(delayInSeconds) {
                        runtime.resolve(expression)
                    }
                }
            }
            // q.entity.spawn_bedrock_particles(effect, locator, [player]) - sends to everyone nearby or just to the player if they're set. Locator is necessary even if unused on non-posables.
            map.put("spawn_bedrock_particles") { params ->
                val particle = params.getString(0).asResource()
                val locator = params.getString(1)
                val player = params.getOrNull<MoValue>(2)?.let {
                    if (it is StringValue) entity.level().getPlayerByUUID(UUID.fromString(it.value))
                    else if (it is ObjectValue<*>) it.obj
                    else null
                } as? ServerPlayer

                val packet = SpawnSnowstormEntityParticlePacket(particle, entity.id, listOf(locator))
                if (player == null) {
                    packet.sendToPlayersAround(entity.x, entity.y, entity.z, 64.0, entity.level().dimension())
                } else {
                    packet.sendToPlayer(player)
                }
            }
            map.put("make_intangible") { params ->
                val intangible = params.getBooleanOrNull(0) ?: true
                entity.noPhysics = intangible
                return@put DoubleValue.ONE
            }
            return@mutableListOf map
        }
    )

    val livingEntityFunctions: MutableList<(LivingEntity) -> HashMap<String, java.util.function.Function<MoParams, Any>>> = mutableListOf<(LivingEntity) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { entity ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("is_player") { _ -> DoubleValue(entity is Player) }
            map.put("is_npc") { _ -> DoubleValue(entity is NPCEntity) }
            map.put("is_mob") { _ -> DoubleValue(entity is Mob) }
            map.put("is_pokemon") { _ -> DoubleValue(entity is PokemonEntity) }
            map.put("is_animal") { _ -> DoubleValue(entity is Animal) }
            map.put("is_tamable") { _ -> DoubleValue(entity is TamableAnimal) }
            map.put("is_tamed") { _ -> DoubleValue(entity is TamableAnimal && entity.isTame) }
            map.put("is_hostile") { _ -> DoubleValue(entity is Monster) }
            map.put("is_baby") { _ -> DoubleValue(entity.isBaby) }
            map.put("is_adult") { _ -> DoubleValue(!entity.isBaby) }
            map.put("remove_effect") { params ->
                val effectId = params.getString(0).asIdentifierDefaultingNamespace()
                val effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null)
                if (effectHolder != null) {
                    entity.removeEffect(effectHolder)
                    return@put DoubleValue.ONE
                }
            }
            map.put("add_effect") { params ->
                val effectId = params.getString(0).asIdentifierDefaultingNamespace()
                val duration = params.getInt(1)
                val amplifier = params.getIntOrNull(2) ?: 0
                val ambient = params.getBooleanOrNull(3) ?: false
                val visible = params.getBooleanOrNull(4) ?: true
                val effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null)
                if (effectHolder != null) {
                    entity.addEffect(MobEffectInstance(effectHolder, duration, amplifier, ambient, visible))
                    return@put DoubleValue.ONE
                }
            }
            map.put("has_effect") { params ->
                val effectId = params.getString(0).asIdentifierDefaultingNamespace()
                val effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null)
                return@put DoubleValue(if (effectHolder != null) entity.hasEffect(effectHolder) else false)
            }
            map.put("heal") { params ->
                val amount = params.getDouble(0)
                entity.heal(amount.toFloat())
            }
            map.put("is_looking_at") { params ->
                val targetEntity = params.get<MoValue>(0)
                val maxDistance = params.getDoubleOrNull(1)?.toFloat() ?: 2.0F

                val entity = if (targetEntity is ObjectValue<*> && targetEntity.obj is Entity) {
                    targetEntity.obj as Entity
                } else {
                    return@put DoubleValue.ZERO
                }

                return@put DoubleValue(entity.isLookingAt(entity, maxDistance))
            }
            map.put("is_living_entity") { DoubleValue.ONE }
            map.put("is_flying") { _ -> DoubleValue(entity.isFallFlying) }
            map.put("is_sleeping") { _ -> DoubleValue(entity.isSleeping) }
            map.put("health") { _ -> DoubleValue(entity.health) }
            map.put("max_health") { _ -> DoubleValue(entity.maxHealth) }
            map.put("look_at_position") { params ->
                val x = params.getDouble(0)
                val y = params.getDouble(1)
                val z = params.getDouble(2)
                val duration = params.getIntOrNull(3) ?: 20
                val flags = params.params.subList(4, params.params.size).map { it.asString() }
                val brain = entity.brain
                brain.setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, CobblemonBlockPosTracker(Vec3(x, y, z), flags.toSet()), duration.toLong())
            }
            map.put("get_wander_control_memory") {
                val value = entity.brain.getMemorySafely(CobblemonMemories.WANDER_CONTROL).orElse(null)
                    ?: CobblemonWanderControl()
                if (
                    !entity.brain.checkMemory(CobblemonMemories.WANDER_CONTROL, MemoryStatus.VALUE_PRESENT)
                    && entity.brain.checkMemory(CobblemonMemories.WANDER_CONTROL, MemoryStatus.REGISTERED)
                ) {
                    entity.brain.setMemory(CobblemonMemories.WANDER_CONTROL, value)
                }
                return@put value.struct
            }
            map.put("get_position_memory") { params ->
                val id = params.getString(0).asIdentifierDefaultingNamespace()
                val memoryType = BuiltInRegistries.MEMORY_MODULE_TYPE.get(id)
                if (entity.brain.checkMemory(memoryType, MemoryStatus.VALUE_PRESENT)) {
                    val memory = entity.brain.getMemory(memoryType).get()
                    return@put when (memory) {
                        is Vec3i -> VariableStruct(
                            mapOf(
                                "x" to DoubleValue(memory.x),
                                "y" to DoubleValue(memory.y),
                                "z" to DoubleValue(memory.z)
                            )
                        )
                        is net.minecraft.core.Position -> VariableStruct(
                            mapOf(
                                "x" to DoubleValue(memory.x()),
                                "y" to DoubleValue(memory.y()),
                                "z" to DoubleValue(memory.z())
                            )
                        )
                        else -> DoubleValue.ZERO
                    }
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map.put("set_uuid_memory") { params ->
                val memory = params.getString(0).asIdentifierDefaultingNamespace().let(BuiltInRegistries.MEMORY_MODULE_TYPE::get) as MemoryModuleType<UUID>
                val uuid = params.getString(1).asUUID ?: return@put DoubleValue.ZERO
                val expiry = params.getIntOrNull(2) ?: -1
                if (expiry != -1) {
                    entity.brain.setMemoryWithExpiry(memory, uuid, expiry.toLong())
                } else {
                    entity.brain.setMemory(memory, uuid)
                }
                return@put DoubleValue.ONE
            }
            map.put("erase_memory") { params ->
                val memories = params.params.map { it.asString().asIdentifierDefaultingNamespace() }.map(BuiltInRegistries.MEMORY_MODULE_TYPE::get)
                memories.forEach { entity.brain.eraseMemory(it) }
                return@put DoubleValue.ONE
            }
            map.put("has_memory_value") { params ->
                for (param in params.params) {
                    if (!entity.hasMemoryFromString(param.asString())) {
                        return@put DoubleValue.ZERO
                    }
                }
                return@put DoubleValue.ONE
            }
            map.put("lacks_memory_value") { params ->
                for (param in params.params) {
                    if (entity.hasMemoryFromString(param.asString())) {
                        return@put DoubleValue.ZERO
                    }
                }
                return@put DoubleValue.ONE
            }
            map.put("is_doing_activity") { params ->
                for (param in params.params) {
                    if (entity.isDoingActivityFromString(param.asString())) {
                        return@put DoubleValue.ONE
                    }
                }
                return@put DoubleValue.ZERO
            }
            map.put("can_see") { params ->
                val target = params.get<MoValue>(0)
                val range = params.getDoubleOrNull(1) ?: 32.0
                if (target is ObjectValue<*>) {
                    val targetEntity = target.obj as? LivingEntity ?: return@put DoubleValue.ZERO
                    val vector = targetEntity.eyePosition.subtract(entity.eyePosition).normalize()
                    val trace = entity.traceEntityCollision(
                        maxDistance = range.toFloat(),
                        stepDistance = 0.1F,
                        direction = vector,
                        entityClass = targetEntity.javaClass,
                        collideBlock = ClipContext.Fluid.NONE
                    )
                    return@put DoubleValue(targetEntity in (trace?.entities ?: emptyList()))
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            if (entity is PathfinderMob) {
                map.put("walk_to") { params ->
                    val x = params.getDouble(0)
                    val y = params.getDouble(1)
                    val z = params.getDouble(2)
                    val speedMultiplier = params.getDoubleOrNull(3) ?: 0.35

                    if (entity.brain.checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED)) {
                        entity.brain.setMemory(
                            MemoryModuleType.WALK_TARGET,
                            WalkTarget(Vec3(x, y, z), speedMultiplier.toFloat(), 1)
                        )
                        entity.brain.setMemory(
                            MemoryModuleType.LOOK_TARGET,
                            BlockPosTracker(Vec3(x, y + entity.eyeHeight, z))
                        )
                    } else {
                        entity.navigation.moveTo(x, y, z, speedMultiplier)
                        entity.lookControl.setLookAt(Vec3(x, y + entity.eyeHeight, z))
                    }
                }
                map.put("has_walk_target") { _ ->
                    DoubleValue(entity.brain.getMemorySafely(MemoryModuleType.WALK_TARGET).isPresent || entity.isPathFinding)
                }
            }
            map
        }
    )
    val npcFunctions = mutableListOf<(NPCEntity) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { npc ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("class") { StringValue(npc.npc.id.toString()) }
            map.put("name") { StringValue(npc.name.string) }
            map.put("level") { DoubleValue(npc.level) }
            map.put("has_aspect") { params -> DoubleValue(npc.aspects.contains(params.getString(0))) }
            map.put("in_battle") { DoubleValue(npc.isInBattle()) }
            map.put("battles") { ArrayStruct(npc.battleIds.mapNotNull { BattleRegistry.getBattle(it)?.struct }.mapIndexed { index, value -> "$index" to value }.toMap()) }
            map.put("stop_battles") { _ -> npc.battleIds.forEach { BattleRegistry.getBattle(it)?.stop() } }
            map.put("run_script_on_client") { params ->
                val world = npc.level()
                if (world is ServerLevel) {
                    val script = params.getString(0)
                    val packet = RunPosableMoLangPacket(npc.id, setOf("q.run_script('$script')"))
                    packet.sendToPlayers(world.players().toList())
                }
                Unit
            }
            map.put("run_script") { params ->
                val script = params.getString(0).asIdentifierDefaultingNamespace()
                val runtime = MoLangRuntime()
                runtime.environment.cloneFrom(params.environment)
                CobblemonScripts.run(script, runtime) ?: DoubleValue(0)
            }
            map.put("set_movable") { params ->
                val movable = params.getBooleanOrNull(0) != false
                npc.isMovable = movable
                return@put DoubleValue.ONE
            }
            map.put("set_invulnerable") { params ->
                val invulnerable = params.getBooleanOrNull(0) != false
                npc.isInvulnerable = invulnerable
                return@put DoubleValue.ONE
            }
            map.put("set_leashable") { params ->
                val leashable = params.getBooleanOrNull(0) != false
                npc.isLeashable = leashable
                return@put DoubleValue.ONE
            }
            map.put("set_allow_projectile_hits") { params ->
                val allowProjectileHits = params.getBooleanOrNull(0) != false
                npc.allowProjectileHits = allowProjectileHits
                return@put DoubleValue.ONE
            }
            map.put("set_name_tag_visible") { params ->
                val nameTagVisible = params.getBooleanOrNull(0) != false
                npc.hideNameTag = !nameTagVisible
                return@put DoubleValue.ONE
            }
            map.put("unset_interaction") {
                npc.interaction = null
                return@put DoubleValue.ONE
            }
            map.put("set_dialogue_interaction") { params ->
                val dialogue = params.getString(0).asIdentifierDefaultingNamespace()
                npc.interaction = DialogueNPCInteractionConfiguration().also {
                    it.dialogue = dialogue
                }
                return@put DoubleValue.ONE
            }
            map.put("set_script_interaction") { params ->
                val script = params.getString(0).asIdentifierDefaultingNamespace()
                npc.interaction = ScriptNPCInteractionConfiguration().also {
                    it.script = script
                }
                return@put DoubleValue.ONE
            }
            map.put("set_player_texture") { params ->
                val username = params.getString(0)
                // Re-applying it would be unnecessarily laggy.
                if (username == npc.data.map["player_texture_username"]?.asString()) {
                    return@put DoubleValue.ZERO
                }
                npc.loadTextureFromGameProfileName(username)
                return@put DoubleValue.ONE
            }
            map.put("unset_player_texture") { params ->
                npc.unloadTexture()
                return@put DoubleValue.ONE
            }
            map.put("set_resource_identifier") { params ->
                val identifier = params.getStringOrNull(0)?.asIdentifierDefaultingNamespace()
                npc.forcedResourceIdentifier = identifier
                return@put DoubleValue.ONE
            }
            map.put("unset_resource_identifier") {
                npc.forcedResourceIdentifier = null
                return@put DoubleValue.ONE
            }
            map.put("set_class") { params ->
                val identifier = params.getString(0).asIdentifierDefaultingNamespace()
                val npcClass = NPCClasses.getByIdentifier(identifier)
                if (npcClass != null) {
                    npc.npc = npcClass
                    return@put DoubleValue.ONE
                } else {
                    Cobblemon.LOGGER.error("Unknown NPC class: $identifier")
                    return@put DoubleValue.ZERO
                }
            }
            map.put("set_render_scale") { params ->
                val scale = params.getDouble(0)
                npc.renderScale = scale.toFloat()
                return@put DoubleValue.ONE
            }
            map.put("render_scale") { _ -> DoubleValue(npc.renderScale) }
            map.put("set_hitbox_scale") { params ->
                val scale = params.getDouble(0).toFloat()
                npc.hitboxScale = scale
                npc.refreshDimensions()
                return@put DoubleValue.ONE
            }
            map.put("hitbox_scale") { _ -> DoubleValue(npc.hitboxScale) }
            map.put("set_hitbox") { params ->
                if (params.params.size == 0) {
                    npc.hitbox = null
                    return@put DoubleValue.ONE
                }
                val width = params.getDouble(0).toFloat()
                val height = params.getDouble(1).toFloat()
                val eyeHeight = params.getDoubleOrNull(2)?.toFloat() ?: (height * 0.85F)
                npc.hitbox = EntityDimensions.scalable(width, height).withEyeHeight(eyeHeight)
                return@put DoubleValue.ONE
            }
            map.put("unset_hitbox") {
                npc.hitbox = null
                return@put DoubleValue.ONE
            }
            map.put("aspects") {
                val aspects = npc.aspects
                return@put aspects.asArrayValue { StringValue(it) }
            }
            map.put("add_aspect") { params ->
                val aspects = params.params.map { it.asString() }
                npc.appliedAspects.addAll(aspects)
                npc.updateAspects()
                return@put DoubleValue.ONE
            }
            map.put("remove_aspect") { params ->
                val aspects = params.params.map { it.asString() }
                npc.appliedAspects.removeAll(aspects)
                npc.updateAspects()
                return@put DoubleValue.ONE
            }
            map.put("party") { params ->
                val party = npc.party ?: return@put DoubleValue.ZERO
                return@put party.asMoLangValue()
            }
            map.put("create_npc_party") { params ->
                val party = NPCPartyStore(npc)
                return@put party.asMoLangValue()
            }
            map.put("set_npc_party") { params ->
                val party = params.get<MoValue>(0)
                if (party is ObjectValue<*>) {
                    npc.party = party.obj as NPCPartyStore
                } else {
                    npc.party = null
                }
                DoubleValue.ONE
            }
            map.put("run_action_effect") { params ->
                val runtime = MoLangRuntime().setup()
                runtime.environment.cloneFrom(params.environment)
                runtime.withNPCValue(value = npc)
                val actionEffect = ActionEffects.actionEffects[params.getString(0).asIdentifierDefaultingNamespace()]
                if (actionEffect != null) {
                    val context = ActionEffectContext(
                        actionEffect = actionEffect,
                        providers = mutableListOf(NPCProvider(npc)),
                        runtime = runtime,
                        level = npc.level()
                    )
                    npc.actionEffect = context
                    npc.brain.setMemory(CobblemonMemories.ACTIVE_ACTION_EFFECT, context)
                    npc.brain.setActiveActivityIfPossible(CobblemonActivities.ACTION_EFFECT)
                    actionEffect.run(context).thenRun {
                        val npcActionEffect = npc.brain.getMemory(CobblemonMemories.ACTIVE_ACTION_EFFECT).orElse(null)
                        if (npcActionEffect == context && npc.brain.isActive(CobblemonActivities.ACTION_EFFECT)) {
                            npc.brain.eraseMemory(CobblemonMemories.ACTIVE_ACTION_EFFECT)
                            npc.actionEffect = null
                        }
                    }

                    return@put DoubleValue.ONE
                }
                return@put DoubleValue.ZERO
            }
            map.put("put_pokemon_in_healer") { params ->
                val healer = npc.level().getBlockEntity(params.get<ArrayStruct>(0).asBlockPos(), CobblemonBlockEntities.HEALING_MACHINE).orElse(null) ?: return@put DoubleValue.ZERO
                val party = npc.party ?: return@put DoubleValue.ZERO
                if (healer.canHeal(party)) {
                    healer.activate(npc.uuid, party)
                    return@put DoubleValue.ONE
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map.put("look_at_position") { params ->
                val pos = Vec3(params.getDouble(0), params.getDouble(1), params.getDouble(2))
                npc.lookAt(EntityAnchorArgument.Anchor.EYES, pos)
            }
            map.put("environment") { _ -> npc.runtime.environment }
            map.put("party") { npc.party?.struct ?: DoubleValue.ZERO }
            map.put("has_party") { DoubleValue(npc.party != null) }
            map.put("is_npc") { DoubleValue.ONE }
            map.put("can_battle") { DoubleValue(npc.party?.any { it.currentHealth > 0 } == true || npc.npc.party?.isStatic == false) }
            map.put("set_battle_theme") { params ->
                val soundId = params.getString(0).asResource()
                npc.npc.battleTheme = soundId
                return@put DoubleValue.ONE
            }
            map
        }
    )

    val battleFunctions = mutableListOf<(PokemonBattle) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { battle ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("battle_id") { StringValue(battle.battleId.toString()) }
            map.put("is_pvn") { DoubleValue(battle.isPvN) }
            map.put("is_pvp") { DoubleValue(battle.isPvP) }
            map.put("is_pvw") { DoubleValue(battle.isPvW) }
            map.put("battle_type") { StringValue(battle.format.toString()) }
            map.put("environment") { battle.runtime.environment }
            map.put("get_actor") { params ->
                val uuid = UUID.fromString(params.getString(0))
                val actor = battle.actors.find { it.uuid == uuid } ?: return@put DoubleValue.ZERO
                return@put actor.struct
            }
            map.put("stop") { _ -> battle.stop() }
            map.put("actors") { battle.actors.toList().asArrayValue { it.struct } }
            map
        }
    )

    val battleActorFunctions = mutableListOf<(BattleActor) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { battleActor ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("is_npc") { DoubleValue(battleActor.type == ActorType.NPC) }
            map.put("is_player") { DoubleValue(battleActor.type == ActorType.PLAYER) }
            map.put("is_wild") { DoubleValue(battleActor.type == ActorType.WILD) }
            if (battleActor is NPCBattleActor) {
                map.put("npc") { battleActor.entity.struct }
            } else if (battleActor is PlayerBattleActor) {
                map.put("player") { battleActor.entity?.asMoLangValue() ?: DoubleValue.ZERO }
            } else if (battleActor is PokemonBattleActor) {
                map.put("pokemon") { battleActor.entity?.asMoLangValue() ?: DoubleValue.ZERO }
            }
            return@mutableListOf map
        }
    )

    val battleMessageFunctions = mutableListOf<(BattleMessage) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { message ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("has_argument_at") { params ->
                val index = params.getInt(0)
                val value = params.getStringOrNull(1) ?: ""
                return@put DoubleValue(value == message.argumentAt(index))
            }
            map.put("has_argument") { params ->
                val argumentName = params.getString(0)
                val value = params.getStringOrNull(1) ?: ""
                return@put DoubleValue(value == message.optionalArgument(argumentName))
            }
            return@mutableListOf map
        }
    )

    val pokemonFunctions = mutableListOf<(Pokemon) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { pokemon ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("id") { StringValue(pokemon.uuid.toString()) }
            map.put("nickname") { StringValue(pokemon.nickname.toString()) }
            map.put("level") { DoubleValue(pokemon.level.toDouble()) }
            map.put("max_hp") { DoubleValue(pokemon.maxHealth.toDouble()) }
            map.put("current_hp") { DoubleValue(pokemon.currentHealth.toDouble()) }
            map.put("friendship") { DoubleValue(pokemon.friendship.toDouble()) }
            map.put("max_fullness") { DoubleValue(pokemon.getMaxFullness().toDouble()) }
            map.put("fullness") { DoubleValue(pokemon.currentFullness.toDouble()) }
            map.put("lose_fullness") { params ->
                val amount = params.getDouble(0)
                pokemon.loseFullness(amount.toInt())
            }
            map.put("feed_pokemon") { params ->
                val amount = params.getDouble(0)
                val playSound = params.getBooleanOrNull(1) ?: true
                pokemon.feedPokemon(amount.toInt(), playSound)
            }
            map.put("behaviour") { pokemon.form.behaviour.struct }
            map.put("behavior") { pokemon.form.behaviour.struct } // Inferior
            map.put("pokeball") { StringValue(pokemon.caughtBall.toString()) }
            map.put("ability") { StringValue(pokemon.ability.name) }
            map.put("has_learned") { params ->
                val moveName = params.getString(0)
                val move = pokemon.allAccessibleMoves.find { it.name == moveName }
                if(move != null) {
                    return@put DoubleValue.ONE
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map.put("moveset") {
                val struct = QueryStruct(hashMapOf())
                for ((index, move) in pokemon.moveSet.withIndex()) {
                    struct.addFunction(index.toString()) { move.struct }
                }
                struct
            }
            map.put("evs") {
                val struct = QueryStruct(hashMapOf())
                for (stat in Stats.PERMANENT) {
                    struct.addFunction(stat.showdownId) { DoubleValue(pokemon.evs.getOrDefault(stat).toDouble()) }
                }
                struct
            }
            map.put("ivs") {
                val struct = QueryStruct(hashMapOf())
                for (stat in Stats.PERMANENT) {
                    struct.addFunction(stat.showdownId) { DoubleValue(pokemon.ivs.getOrDefault(stat).toDouble()) }
                }
                struct
            }
            map.put("hyper_trained_ivs") {
                val struct = QueryStruct(hashMapOf())
                for (stat in Stats.PERMANENT) {
                    struct.addFunction(stat.showdownId) { DoubleValue(pokemon.ivs.hyperTrainedIVs[stat] ?: -1.0) }
                }
                struct
            }
            map.put("ride_boosts") {
                val struct = QueryStruct(hashMapOf())
                for (stat in RidingStat.entries) {
                    struct.addFunction(stat.name.lowercase()) { DoubleValue(pokemon.getRideBoost(stat) ?: 0.0) }
                }
                struct
            }
            map.put("natdex_number") {
                DoubleValue(pokemon.species.nationalPokedexNumber.toDouble())
            }
            map.put("types") {
                pokemon.form.types.map { it.toString() }.asArrayValue(::StringValue)
            }
            map.put("gender_ratio") {
                DoubleValue(pokemon.form.maleRatio.toDouble())
            }
            map.put("ev_yield") {
                val struct = QueryStruct(hashMapOf())
                for (stat in Stats.PERMANENT) {
                    struct.addFunction(stat.showdownId) { DoubleValue(pokemon.form.evYield[stat]?.toDouble()) }
                }
                struct
            }
            map.put("base_stats") {
                val struct = QueryStruct(hashMapOf())
                for (stat in Stats.PERMANENT) {
                    struct.addFunction(stat.showdownId) { DoubleValue(pokemon.form.baseStats[stat]?.toDouble()) }
                }
                struct
            }
            map.put("catch_rate") {
                DoubleValue(pokemon.form.catchRate.toDouble())
            }
            map.put("base_experience_yield") {
                DoubleValue(pokemon.form.baseExperienceYield.toDouble())
            }
            map.put("drops") {
                val struct = QueryStruct(hashMapOf())
                for ((index, drop) in pokemon.form?.drops?.entries?.withIndex()!!) {
                    struct.addFunction(index.toString()) { drop }
                }
                struct
            }
            map.put("tm_learnset") {
                val struct = QueryStruct(hashMapOf())
                for ((index, move) in pokemon.form.moves.tmMoves.withIndex()) {
                    struct.addFunction(index.toString()) { move.struct }
                }
                struct
            }
            map.put("egg_learnset") {
                val struct = QueryStruct(hashMapOf())
                for ((index, move) in pokemon.form.moves.eggMoves.withIndex()) {
                    struct.addFunction(index.toString()) { move.struct }
                }
                struct
            }
            map.put("tutor_learnset") {
                val struct = QueryStruct(hashMapOf())
                for ((index, move) in pokemon.form.moves.tutorMoves.withIndex()) {
                    struct.addFunction(index.toString()) { move.struct }
                }
                struct
            }
            map.put("level_learnset") {
                val struct = QueryStruct(hashMapOf())
                for ((index, move) in pokemon.form.moves.levelUpMoves) {
                    struct.addFunction(index.toString()) { move }
                }
                struct
            }
            map.put("ability_pool") {
                val struct = QueryStruct(hashMapOf())
                for ((index, ability) in pokemon.form.abilities.withIndex()) {
                    struct.addFunction(index.toString()) { StringValue(ability.toString()) }
                }
                struct
            }
            map.put("egg_groups") {
                val struct = QueryStruct(hashMapOf())
                for ((index, group) in pokemon.form.eggGroups.withIndex()) {
                    struct.addFunction(index.toString()) { StringValue(group.toString()) }
                }
                struct
            }
            map.put("egg_cycles") {
                DoubleValue(pokemon.species.eggCycles.toDouble())
            }
            map.put("labels") {
                val struct = QueryStruct(hashMapOf())
                for ((index, label) in pokemon.form.labels.withIndex()) {
                    struct.addFunction(index.toString()) { StringValue(label) }
                }
                struct
            }
            map.put("aspects") {
                val aspects = pokemon.aspects
                return@put aspects.asArrayValue { StringValue(it) }
            }
            map.put("form_aspects") {
                val aspects = pokemon.form.aspects
                return@put aspects.asArrayValue { StringValue(it) }
            }
            map.put("form_name") {
                StringValue(pokemon.form.name.toString())
            }
            // Yes, this is the lazy call for a single hardcoded pre-evolution.
            // Lol. Lmao, even.
            // TO-DO: Subscribe to [PokemonSpecies.observable].
            map.put("prevolution") {
                val prevolution = pokemon.species.preEvolution ?: return@put DoubleValue.ZERO
                return@put prevolution
            }
            map.put("nature") { StringValue(pokemon.nature.toString()) }
            map.put("is_wild") { DoubleValue(pokemon.entity?.let { it.ownerUUID == null } == true) }
            map.put("is_shiny") { DoubleValue(pokemon.shiny) }
            map.put("is_in_party") { DoubleValue(pokemon.storeCoordinates.get()?.store is PartyStore) }
            map.put("species") { pokemon.species.struct }
            map.put("form") { StringValue(pokemon.form.name) }
            map.put("weight") { DoubleValue(pokemon.species.weight.toDouble()) }
            map.put("matches") { params -> DoubleValue(params.getString(0).toProperties().matches(pokemon)) }
            map.put("apply") { params ->
                params.getString(0).toProperties().apply(pokemon)
                DoubleValue.ONE
            }
            map.put("owner") { pokemon.getOwnerPlayer()?.asMoLangValue() ?: DoubleValue.ZERO }
            map.put("held_item") { pokemon.heldItem().asMoLangValue(server()!!.registryAccess()) ?: DoubleValue.ZERO }
            map.put("remove_held_item") { _ ->
                pokemon.removeHeldItem()
            }
            map.put("add_aspects") { params ->
                for (aspect in params.params) pokemon.forcedAspects += aspect.asString()
                pokemon.updateAspects()
            }
            map.put("remove_aspects") { params ->
                for (aspect in params.params) pokemon.forcedAspects -= aspect.asString()
                pokemon.updateAspects()
            }
            map.put("cosmetic_item") { pokemon.cosmeticItem().asMoLangValue(server()!!.registryAccess()) ?: DoubleValue.ZERO }
            map.put("remove_cosmetic_item") { _ ->
                pokemon.removeCosmeticItem()
            }
            map.put("add_marks") { params ->
                var appliedMark = false
                for (param in params.params) {
                    val identifier = param.asString().asIdentifierDefaultingNamespace()
                    val mark = Marks.getByIdentifier(identifier)
                    if (mark != null) {
                        pokemon.exchangeMark(mark, true)
                        appliedMark = true
                    }
                }

                DoubleValue(appliedMark)
            }
            map.put("add_marks_with_chance") { params ->
                var appliedMark = false
                for (param in params.params) {
                    val identifier = param.asString().asIdentifierDefaultingNamespace()
                    val mark = Marks.getByIdentifier(identifier)

                    mark?.let {
                        val probability = it.chance.coerceIn(0F, 1F) * 100
                        val randomValue = Random.nextDouble(0.0, 100.0)
                        if (randomValue < probability) {
                            pokemon.exchangeMark(it, true)
                            appliedMark = true
                        }
                    }
                }

                DoubleValue(appliedMark)
            }
            map.put("add_potential_marks") { params ->
                for (param in params.params) {
                    val identifier = param.asString().asIdentifierDefaultingNamespace()
                    val mark = Marks.getByIdentifier(identifier)
                    if (mark != null) pokemon.addPotentialMark(mark)
                }
            }
            map.put("apply_potential_marks") {
                return@put DoubleValue(pokemon.applyPotentialMarks())
            }
            map.put("hyper_train_iv") { params ->
                val statId = params.getString(0)
                val stat = Stats.getStat(statId)
                val value = params.getIntOrNull(1) ?: IVs.MAX_VALUE

                if (Stats.PERMANENT.contains(stat)) {
                    pokemon.hyperTrainIV(stat, value)
                    return@put DoubleValue.ONE
                } else {
                    Cobblemon.LOGGER.error("Unknown or non-permanent stat: ${stat.toString()}")
                    return@put DoubleValue.ZERO
                }
            }
            map.put("add_exp") { params ->
                val exp = params.getDouble(0).toInt()
                pokemon.addExperience(SidemodExperienceSource("molang"), exp)
                return@put DoubleValue.ONE
            }
            map.put("set_iv") { params ->
                val statId = params.getString(0)
                val stat = Stats.getStat(statId)
                val value = params.getIntOrNull(1)?.coerceIn(0, IVs.MAX_VALUE) ?: IVs.MAX_VALUE

                if (Stats.PERMANENT.contains(stat)) {
                    pokemon.setIV(stat, value)
                    return@put DoubleValue.ONE
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map.put("set_ev") { params ->
                val statId = params.getString(0)
                val stat = Stats.getStat(statId)
                val value = (params.getIntOrNull(1) ?: 0).coerceIn(0, 255)

                if (Stats.PERMANENT.contains(stat)) {
                    pokemon.setEV(stat, value)
                    return@put DoubleValue.ONE
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map.put("set_ride_boost") { params ->
                val statName = params.getString(0).uppercase()
                val stat = RidingStat.entries.find { it.name.equals(statName, ignoreCase = true) } ?: return@put DoubleValue.ZERO
                val value = params.getDoubleOrNull(1)?.toFloat() ?: return@put DoubleValue.ZERO

                pokemon.setRideBoost(stat, value)
                return@put DoubleValue.ONE
            }
            map.put("add_ride_boost") { params ->
                val statName = params.getString(0).uppercase()
                val stat = RidingStat.entries.find { it.name.equals(statName, ignoreCase = true) } ?: return@put DoubleValue.ZERO
                val value = params.getDoubleOrNull(1)?.toFloat() ?: return@put DoubleValue.ZERO

                return@put DoubleValue(if (pokemon.addRideBoost(stat, value)) 1.0 else 0.0)
            }
            map.put("initialize_moveset") { params ->
                val preferLatest = params.getBooleanOrNull(0) ?: true
                pokemon.initializeMoveset(preferLatest)
                return@put DoubleValue.ONE
            }
            map.put("validate_moveset") { params ->
                val includeLegacy = params.getBooleanOrNull(0) ?: true
                pokemon.validateMoveset(includeLegacy)
                return@put DoubleValue.ONE
            }
            map.put("teach_learnable_moves") { params ->
                val includeLegacy = params.getBooleanOrNull(0) ?: true
                pokemon.teachLearnableMoves(includeLegacy)
                return@put DoubleValue.ONE
            }
            map.put("teach_move") { params ->
                val moveName = params.getString(0)
                val moveTemplate = Moves.getByName(moveName) ?: return@put DoubleValue.ZERO
                val bypass = params.getBooleanOrNull(1) ?: false

                val canLearn = bypass || LearnsetQuery.ANY.canLearn(moveTemplate, pokemon.form.moves)
                if (!canLearn) {
                    return@put DoubleValue.ZERO
                }

                val alreadyKnows = pokemon.moveSet.getMoves().any { it.template == moveTemplate } ||
                        pokemon.benchedMoves.any { it.moveTemplate == moveTemplate }
                if (alreadyKnows) {
                    return@put DoubleValue.ZERO
                }

                if (pokemon.moveSet.hasSpace()) {
                    pokemon.moveSet.add(moveTemplate.create())
                } else {
                    pokemon.benchedMoves.add(BenchedMove(moveTemplate, 0))
                }

                DoubleValue.ONE
            }
            map.put("can_learn_move") { params ->
                val moveName = params.getString(0)
                val moveTemplate = Moves.getByName(moveName) ?: return@put DoubleValue.ZERO
                val includeLegacy = params.getBooleanOrNull(1) ?: true

                val canLearn = if (includeLegacy) {
                    LearnsetQuery.ANY.canLearn(moveTemplate, pokemon.form.moves)
                } else {
                    LearnsetQuery.LEGAL.canLearn(moveTemplate, pokemon.form.moves)
                }
                return@put DoubleValue(if (canLearn) 1.0 else 0.0)
            }
            map.put("unlearn_move") { params ->
                val moveName = params.getString(0)
                val moveTemplate = Moves.getByName(moveName)
                if (moveTemplate != null) {
                    pokemon.unlearnMove(moveTemplate)
                    return@put DoubleValue.ONE
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map.put("can_evolve") { _ ->
                DoubleValue(pokemon.evolutions.any())
            }
            map.put("force_evolve") { params ->
                val idx = params.getInt(0)
                val evolution = if (idx >= 0) pokemon.evolutions.elementAtOrNull(idx) else return@put DoubleValue.ZERO
                evolution?.forceEvolve(pokemon)
                StringValue(evolution.toString())
            }
            map
        }
    )

    val pokemonEntityFunctions = mutableListOf<(PokemonEntity) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { pokemonEntity ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("is_busy") { DoubleValue(pokemonEntity.isBusy) }
            map.put("in_battle") { DoubleValue(pokemonEntity.isBattling) }
            map.put("is_moving") { DoubleValue((pokemonEntity.moveControl as? PokemonMoveControl)?.hasWanted() == true) }
            map.put("is_flying") { DoubleValue(pokemonEntity.getBehaviourFlag(PokemonBehaviourFlag.FLYING)) }
            map.put("get_riding_state") { params ->
                val name = params.getStringOrNull(0)
                pokemonEntity.ifRidingAvailableSupply(DoubleValue.ZERO) { behaviour, settings, state ->
                    val moValue = behaviour.asMoLangValue(settings, state, pokemonEntity)
                    return@ifRidingAvailableSupply moValue.functions[name]?.apply(MoParams.EMPTY) as? MoValue ?: moValue
                }
            }
            map.put("is_gliding") {
                pokemonEntity.ifRidingAvailableSupply(DoubleValue.ZERO) { behaviour, settings, state ->
                    val moValue = behaviour.asMoLangValue(settings, state, pokemonEntity)
                    return@ifRidingAvailableSupply moValue.functions.get("gliding")?.apply(MoParams.EMPTY) as? MoValue ?: DoubleValue.ZERO
                }
            }
            map.put("is_sprinting") {
                pokemonEntity.ifRidingAvailableSupply(DoubleValue.ZERO) { behaviour, settings, state ->
                    val moValue = behaviour.asMoLangValue(settings, state, pokemonEntity)
                    return@ifRidingAvailableSupply moValue.functions.get("sprinting")?.apply(MoParams.EMPTY) as? MoValue ?: DoubleValue.ZERO
                }
            }
            map.put("is_drifting") {
                pokemonEntity.ifRidingAvailableSupply(DoubleValue.ZERO) { behaviour, settings, state ->
                    val moValue = behaviour.asMoLangValue(settings, state, pokemonEntity)
                    return@ifRidingAvailableSupply moValue.functions.get("drifting")?.apply(MoParams.EMPTY) as? MoValue ?: DoubleValue.ZERO
                }
            }
            map.put("is_powered_drifting") {
                pokemonEntity.ifRidingAvailableSupply(DoubleValue.ZERO) { behaviour, settings, state ->
                    val moValue = behaviour.asMoLangValue(settings, state, pokemonEntity)
                    return@ifRidingAvailableSupply moValue.functions.get("powered_drifting")?.apply(MoParams.EMPTY) as? MoValue ?: DoubleValue.ZERO
                }
            }
            map.put("in_air") {
                pokemonEntity.ifRidingAvailableSupply(DoubleValue.ZERO) { behaviour, settings, state ->
                    val moValue = behaviour.asMoLangValue(settings, state, pokemonEntity)
                    return@ifRidingAvailableSupply moValue.functions.get("in_air")?.apply(MoParams.EMPTY) as? MoValue ?: DoubleValue.ZERO
                }
            }
            map.put("is_wild") { DoubleValue(pokemonEntity.ownerUUID == null) }
            map.put("is_in_party") { DoubleValue(pokemonEntity.pokemon.storeCoordinates.get()?.store is PartyStore) }
            map.put("is_ridden") { DoubleValue(pokemonEntity.hasControllingPassenger()) }
            map.put("has_aspect") { DoubleValue(it.getString(0) in pokemonEntity.aspects) }
            map.put("is_pokemon") { DoubleValue.ONE }
            map.put("is_holding_item") { DoubleValue(!pokemonEntity.entityData.get(PokemonEntity.SHOWN_HELD_ITEM).let {
                it.isEmpty || it.`is`(CobblemonItemTags.WEARABLE_HAT_ITEMS) || it.`is`(CobblemonItemTags.WEARABLE_FACE_ITEMS)
            }) }
            map.put("riding_style") {
                StringValue(pokemonEntity.ifRidingAvailableSupply("") { behaviour, settings, state -> behaviour.getRidingStyle(settings, state).name })
            }
            map.put("is_wearing_hat") { DoubleValue(pokemonEntity.entityData.get(PokemonEntity.SHOWN_HELD_ITEM).`is`(CobblemonItemTags.WEARABLE_HAT_ITEMS)) }
            map.put("is_wearing_face") { DoubleValue(pokemonEntity.entityData.get(PokemonEntity.SHOWN_HELD_ITEM).`is`(CobblemonItemTags.WEARABLE_FACE_ITEMS)) }
            map.put("is_pastured") {
                DoubleValue((pokemonEntity.tethering != null))
            }
            map.put("pasture_conflict_enabled") {
                DoubleValue(pokemonEntity.getBehaviourFlag(PokemonBehaviourFlag.PASTURE_CONFLICT))
            }
            map.put("run_action_effect") { params ->
                val runtime = MoLangRuntime().setup()
                runtime.environment.cloneFrom(params.environment)
                runtime.withQueryValue("entity", pokemonEntity.struct)
                val actionEffect = ActionEffects.actionEffects[params.getString(0).asIdentifierDefaultingNamespace()]
                if (actionEffect != null) {
                    val context = ActionEffectContext(
                        actionEffect = actionEffect,
                        providers = mutableListOf(TargetsProvider(pokemonEntity)),
                        runtime = runtime,
                        level = pokemonEntity.level()
                    )
                    // This was taken from NPC run_action_effect but this variable isn't used, so I just commented it out.
                    // pokemonEntity.actionEffect = context
                    pokemonEntity.brain.setMemory(CobblemonMemories.ACTIVE_ACTION_EFFECT, context)
                    pokemonEntity.brain.setActiveActivityIfPossible(CobblemonActivities.ACTION_EFFECT)
                    actionEffect.run(context).thenRun {
                        val pokemonActionEffect = pokemonEntity.brain.getMemory(CobblemonMemories.ACTIVE_ACTION_EFFECT).orElse(null)
                        if (pokemonActionEffect == context && pokemonEntity.brain.isActive(CobblemonActivities.ACTION_EFFECT)) {
                            pokemonEntity.brain.eraseMemory(CobblemonMemories.ACTIVE_ACTION_EFFECT)
                            //pokemonEntity.actionEffect = null
                        }
                    }

                    return@put DoubleValue.ONE
                }
                return@put DoubleValue.ZERO
            }
            map
        }
    )

    val pokemonStoreFunctions = mutableListOf<(PokemonStore<*>) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { store ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("uuid") { StringValue(store.uuid.toString()) }
            map.put("add") { params ->
                val pokemon = params.get<ObjectValue<Pokemon>>(0)
                return@put DoubleValue(store.add(pokemon.obj))
            }
            map.put("add_by_properties") { params ->
                val props = params.getString(0).toProperties()
                val player = (store as? PlayerPartyStore)?.playerUUID?.getPlayer() // Only really know for sure when it's a player party store
                val pokemon = props.create(player = player)
                return@put DoubleValue(store.add(pokemon))
            }
            map.put("find_by_properties") { params ->
                val props = params.getString(0).toProperties()
                val pokemon = store.find { props.matches(it) }
                return@put pokemon?.asStruct() ?: DoubleValue.ZERO
            }
            map.put("find_all_by_properties") { params ->
                val props = params.getString(0).toProperties()
                val pokemon = store.filter { props.matches(it) }
                return@put ArrayStruct(pokemon.mapIndexed { index, value -> "$index" to value.asStruct() }.toMap())
            }
            map.put("find_by_id") { params ->
                val id = params.getString(0).asUUID
                val pokemon = store.find { it.uuid == id }
                return@put pokemon?.asStruct() ?: DoubleValue.ZERO
            }
            map.put("remove_by_id") { params ->
                val id = params.getString(0).asUUID
                val pokemon = store.find { it.uuid == id } ?: return@put DoubleValue.ZERO
                return@put DoubleValue(store.remove(pokemon))
            }
            map.put("average_level") { _ ->
                var numberOfPokemon = 0
                var totalLevel = 0
                for (pokemon in store) {
                    totalLevel += pokemon.level
                    numberOfPokemon++
                }
                if (numberOfPokemon == 0) {
                    return@put DoubleValue.ZERO
                }
                return@put DoubleValue(totalLevel.toDouble() / numberOfPokemon)
            }
            map.put("count") { _ -> DoubleValue(store.count()) }
            map.put("count_by_properties") { params ->
                val props = params.getString(0).toProperties()
                return@put DoubleValue(store.count { props.matches(it) })
            }
            map.put("highest_level") {
                val highest = store.maxOfOrNull { it.level } ?: 0
                return@put DoubleValue(highest)
            }
            map.put("lowest_level") {
                val lowest = store.minOfOrNull { it.level } ?: 0
                return@put DoubleValue(lowest)
            }
            map.put("heal") {
                for (pokemon in store) {
                    pokemon.heal()
                }
                return@put DoubleValue.ONE
            }
            map.put("healing_remainder_percent") { _ ->
                var totalPercent = 0.0f
                for (pokemon in store) {
                    totalPercent += (1.0f - (pokemon.currentHealth.toFloat() / pokemon.maxHealth))
                }
                DoubleValue(totalPercent)
            }
            map.put("has_usable_pokemon") { _ -> DoubleValue(store.any { !it.isFainted() }) }
            map.put("pokemon") {
                return@put store.map { it.asStruct() }.asArrayValue()
            }
            map
        }
    )

    val partyFunctions = mutableListOf<(PartyStore) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { party ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("get_pokemon") { params ->
                val index = params.getInt(0)
                val pokemon = party.get(index) ?: return@put DoubleValue.ZERO
                return@put pokemon.struct
            }
            map.put("set_pokemon") { params ->
                val index = params.getInt(0)
                val pokemon = params.get<ObjectValue<Pokemon>>(1).obj
                party.set(index, pokemon)
                return@put DoubleValue.ONE
            }
            return@mutableListOf map
        })

    val pcFunctions = mutableListOf<(PCStore) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { pc ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("get_pokemon") { params ->
                val box = params.getInt(0)
                val slot = params.getInt(1)
                val pokemon = pc[PCPosition(box, slot)] ?: return@put DoubleValue.ZERO
                return@put pokemon.struct
            }
            map.put("set_pokemon") { params ->
                val box = params.getInt(0)
                val slot = params.getInt(1)
                val pokemon = params.get<ObjectValue<Pokemon>>(2).obj
                pc.set(PCPosition(box, slot), pokemon)
                return@put DoubleValue.ONE
            }
            map.put("resize") { params ->
                val newSize = params.getInt(0)
                val lockNewSize = params.getBooleanOrNull(1) == true
                pc.resize(newSize, lockNewSize)
                return@put DoubleValue.ONE
            }
            map.put("get_box_count") { _ -> DoubleValue(pc.boxes.size.toDouble()) }
            map.put("has_unlocked_wallpaper") { params ->
                val wallpaper = params.getString(0).asIdentifierDefaultingNamespace()
                return@put DoubleValue(pc.unlockedWallpapers.contains(wallpaper))
            }
            map.put("get_unlocked_wallpapers") { pc.unlockedWallpapers.asArrayValue { StringValue(it.toString()) } }
            map.put("unlock_wallpaper") {
                val wallpaper = it.getString(0).asIdentifierDefaultingNamespace()
                val playSound = it.getBooleanOrNull(1) != false
                CobblemonUnlockableWallpapers.unlockableWallpapers[wallpaper] ?: return@put DoubleValue.ZERO
                return@put DoubleValue(pc.unlockWallpaper(wallpaper, playSound))
            }
            return@mutableListOf map
        }
    )

    val spawningContextFunctions = mutableListOf<(SpawnablePosition) -> HashMap<String, java.util.function.Function<MoParams, Any>>>(
        { spawningContext ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            val worldValue = spawningContext.world.registryAccess().registryOrThrow(Registries.DIMENSION).wrapAsHolder(spawningContext.world).asWorldMoLangValue()
            val biomeValue = spawningContext.biomeHolder.asBiomeMoLangValue()
            map.put("biome") { _ -> biomeValue }
            map.put("world") { _ -> worldValue }
            map.put("light") { _ -> DoubleValue(spawningContext.light.toDouble()) }
            map.put("x") { _ -> DoubleValue(spawningContext.position.x.toDouble()) }
            map.put("y") { _ -> DoubleValue(spawningContext.position.y.toDouble()) }
            map.put("z") { _ -> DoubleValue(spawningContext.position.z.toDouble()) }
            map.put("moon_phase") { _ -> DoubleValue(spawningContext.moonPhase.toDouble()) }
            map.put("can_see_sky") { _ -> DoubleValue(spawningContext.canSeeSky) }
            map.put("sky_light") { _ -> DoubleValue(spawningContext.skyLight.toDouble()) }
            map.put("player") { _ ->
                val causeEntity = spawningContext.cause.entity ?: return@put DoubleValue.ZERO
                if (causeEntity is ServerPlayer) {
                    return@put causeEntity.asMoLangValue()
                } else {
                    return@put DoubleValue.ZERO
                }
            }
            map
        }
    )

    val serverFunctions: MutableList<(MinecraftServer) -> HashMap<String, java.util.function.Function<MoParams, Any>>> = mutableListOf(
        { server ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()

            map.put("get_world") { params ->
                val world = server.getLevel(
                    ResourceKey.create(
                        Registries.DIMENSION,
                        params.getString(0).asIdentifierDefaultingNamespace(namespace = "minecraft")
                    )
                ) ?: return@put DoubleValue.ZERO

                return@put world
                    .registryAccess()
                    .registryOrThrow(Registries.DIMENSION)
                    .wrapAsHolder(world)
                    .asWorldMoLangValue()
            }

            map.put("broadcast") { params ->
                val message = params.getString(0)
                server.playerList.broadcastSystemMessage(message.text(), params.getBooleanOrNull(1) == true)
                return@put DoubleValue.ONE
            }

            map.put("get_player_by_uuid") { params ->
                val uuid = UUID.fromString(params.getString(0))
                val player = server.playerList.getPlayer(uuid) ?: return@put DoubleValue.ZERO
                return@put player.asMoLangValue()
            }

            map.put("get_player_by_username") { params ->
                val username = params.getString(0)
                val player = server.playerList.getPlayerByName(username) ?: return@put DoubleValue.ZERO
                return@put player.asMoLangValue()
            }

            map.put("data") { params ->
                Cobblemon.molangData.load(
                    UUID(0L, 0L),
                    params.getStringOrNull(0)
                )
            }

            map.put("save_data") { params ->
                Cobblemon.molangData.save(
                    UUID(0L, 0L),
                    params.getStringOrNull(index = 0)
                )
                return@put DoubleValue.ONE
            }

            // Maybe later...
//            map.put("stop") { params ->
//                Cobblemon.LOGGER.warn("Server is being stopped from a MoLang script.")
//                server.stopServer()
//            }

            return@mutableListOf map
        }
    )

    val pokedexFunctions: MutableList<(AbstractPokedexManager) -> HashMap<String, java.util.function.Function<MoParams, Any>>> = mutableListOf(
        { pokedex ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()
            map.put("get_species_record") { params ->
                val speciesId = params.getString(0).asIdentifierDefaultingNamespace()
                pokedex.speciesRecords[speciesId]?.struct ?: QueryStruct(hashMapOf())
            }

            map.put("has_seen") { params ->
                val speciesId = params.getString(0).asIdentifierDefaultingNamespace()
                val formName = params.getStringOrNull(1)

                if (formName == null) {
                    return@put DoubleValue(pokedex.getHighestKnowledgeForSpecies(speciesId).ordinal >= PokedexEntryProgress.ENCOUNTERED.ordinal)
                } else {
                    return@put DoubleValue((pokedex.getSpeciesRecord(speciesId)?.getFormRecord(formName)?.knowledge?.ordinal ?: 0) >= PokedexEntryProgress.ENCOUNTERED.ordinal)
                }
            }

            map.put("has_caught") { params ->
                val speciesId = params.getString(0).asIdentifierDefaultingNamespace()
                val formName = params.getStringOrNull(1)
                if (formName == null) {
                    return@put DoubleValue(pokedex.getHighestKnowledgeForSpecies(speciesId) == PokedexEntryProgress.CAUGHT)
                } else {
                    return@put DoubleValue(pokedex.getSpeciesRecord(speciesId)?.getFormRecord(formName)?.knowledge == PokedexEntryProgress.CAUGHT)
                }
            }
            map.put("caught_count") { DoubleValue(pokedex.getGlobalCalculatedValue(CaughtCount)) }
            map.put("seen_count") { DoubleValue(pokedex.getGlobalCalculatedValue(SeenCount)) }
            map.put("caught_percent") { DoubleValue(pokedex.getGlobalCalculatedValue(CaughtPercent)) }
            map.put("seen_percent") { DoubleValue(pokedex.getGlobalCalculatedValue(SeenPercent)) }

            if (pokedex is PokedexManager) {
                map.put("player_id") { StringValue(pokedex.uuid.toString()) }
                map.put("see") { params ->
                    val pokemon = params.get<ObjectValue<Pokemon>>(0).obj
                    pokedex.encounter(pokemon)
                    return@put DoubleValue.ONE
                }
                map.put("catch") { params ->
                    val pokemon = params.get<ObjectValue<Pokemon>>(0).obj
                    pokedex.catch(pokemon)
                    return@put DoubleValue.ONE
                }
            }

            map
        }
    )

    val speciesFunctions: MutableList<(Species) -> HashMap<String, java.util.function.Function<MoParams, Any>>> = mutableListOf(
        { species ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()

            map.put("identifier") { StringValue(species.resourceIdentifier.toString()) }
            map.put("name") { StringValue(species.name) }
            map.put("primary_type") { StringValue(species.primaryType.showdownId) }
            map.put("secondary_type") { StringValue(species.secondaryType?.showdownId ?: "null") }
            map.put("experience_group") { StringValue(species.experienceGroup.name) }
            map.put("height") { DoubleValue(species.height) }
            map.put("weight") { DoubleValue(species.weight) }
            map.put("base_scale") { DoubleValue(species.baseScale) }
            map.put("hitbox_width") { DoubleValue(species.hitbox.width) }
            map.put("hitbox_height") { DoubleValue(species.hitbox.height) }
            map.put("hitbox_fixed") { DoubleValue(species.hitbox.fixed) }
            map.put("catch_rate") { DoubleValue(species.catchRate) }
            map.put("labels") { return@put species.labels.asArrayValue { StringValue(it) } }
            map.put("has_label") { params -> DoubleValue(species.labels.contains(params.getString(0))) }
            map
        }
    )

    val dropEntryFunctions: MutableList<(DropEntry) -> HashMap<String, java.util.function.Function<MoParams, Any>>> = mutableListOf(
        { dropEntry ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()

            map.put("percentage") { DoubleValue(dropEntry.percentage.toDouble()) }
            map.put("quantity") { DoubleValue(dropEntry.quantity.toDouble()) }
            map.put("max_selectable_times") { DoubleValue(dropEntry.maxSelectableTimes.toDouble()) }
            map.put("can_drop") { params ->
                val pokemon = params.getOrNull<ObjectValue<Pokemon>>(0)?.obj
                DoubleValue(if (dropEntry.canDrop(pokemon)) 1.0 else 0.0)
            }
            map
        }
    )

    val pokemonPropertiesFunctions: MutableList<(PokemonProperties) -> HashMap<String, java.util.function.Function<MoParams, Any>>> = mutableListOf(
        { props ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()

            map.put("level") { DoubleValue(props.level?.toDouble() ?: 0) }
            map.put("set_level") { params ->
                props.level = params.getIntOrNull(0)
                return@put DoubleValue.ONE
            }
            map.put("shiny") { DoubleValue(props.shiny) }
            map.put("set_shiny") { params ->
                props.shiny = params.getBooleanOrNull(0)
                return@put DoubleValue.ONE
            }
            map.put("species") { props.species?.let { StringValue(it) } ?: DoubleValue.ZERO }
            map.put("set_species") { params ->
                props.species = params.getStringOrNull(0)
                return@put DoubleValue.ONE
            }
            map.put("gender") { props.gender?.let { StringValue(it.name) } ?: DoubleValue.ZERO }
            map.put("set_gender") { params ->
                props.gender = params.getStringOrNull(0)?.let { Gender.valueOf(it) }
                return@put DoubleValue.ONE
            }
            map.put("form") { props.form?.let { StringValue(it) } ?: DoubleValue.ZERO }
            map.put("ivs") {
                val ivs = props.ivs
                if (ivs == null) {
                    return@put DoubleValue.ZERO
                } else {
                    return@put ivs.struct
                }
            }
            map.put("evs") {
                val evs = props.evs
                if (evs == null) {
                    return@put DoubleValue.ZERO
                } else {
                    return@put evs.struct
                }
            }
            map.put("friendship") { DoubleValue(props.friendship?.toDouble() ?: DoubleValue.ZERO) }
            map.put("set_friendship") { params ->
                props.friendship = params.getIntOrNull(0)
                return@put DoubleValue.ONE
            }
            map.put("create") { params ->
                val pokemon = props.create()
                return@put pokemon.struct
            }
            map.put("to_string") { params ->
                val separator = params.getStringOrNull(0) ?: " "
                return@put StringValue(props.asString(separator = separator))
            }
            map
        }
    )

    val evolutionFunctions: MutableList<(Evolution) -> HashMap<String, java.util.function.Function<MoParams, Any>>> = mutableListOf(
        { evolution ->
            val map = hashMapOf<String, java.util.function.Function<MoParams, Any>>()

            map.put("result") { evolution.result.asMoLangValue() }

            if (evolution is LevelUpEvolution) {
                map.put("is_level_up") { DoubleValue.ONE }
            } else if (evolution is TradeEvolution) {
                map.put("is_trade") { DoubleValue.ONE }
            } else if (evolution is ItemInteractionEvolution) {
                map.put("is_item") { DoubleValue.ONE }
            }

            map.put("is_optional") { DoubleValue(evolution.optional) }
            map.put("consumes_held_item") { DoubleValue(evolution.consumeHeldItem) }

            map
        }
    )

    fun ItemStack.asMoLangValue(registryAccess: RegistryAccess) = ObjectValue(this).addStandardFunctions().addFunctions(itemStackFunctions.flatMap { it(this, registryAccess).entries.map { it.key to it.value } }.toMap())
    fun Holder<Biome>.asBiomeMoLangValue() = asMoLangValue(Registries.BIOME).addFunctions(biomeFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
    fun Holder<Level>.asWorldMoLangValue() = asMoLangValue(Registries.DIMENSION).addFunctions(worldFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
    fun Holder<Block>.asBlockMoLangValue() = asMoLangValue(Registries.BLOCK).addFunctions(blockFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
    fun MinecraftServer.asMoLangValue() = ObjectValue(this).addStandardFunctions().addFunctions(serverFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
    fun Holder<DimensionType>.asDimensionTypeMoLangValue() = asMoLangValue(Registries.DIMENSION_TYPE).addFunctions(dimensionTypeFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
    fun Player.asMoLangValue(): ObjectValue<Player> {
        if (this is ServerPlayer && uuid in Cobblemon.serverPlayerStructs) {
            val existing = Cobblemon.serverPlayerStructs[uuid]!!
            if (existing.obj == this) {
                return existing
            } else {
                Cobblemon.serverPlayerStructs.remove(uuid)
            }
        }

        val value = ObjectValue(
            obj = this,
            stringify = { it.effectiveName().string }
        )

        value.addFunctions(entityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        value.addFunctions(livingEntityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        value.addFunctions(playerFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())

        if (this is ServerPlayer) {
            Cobblemon.serverPlayerStructs[uuid] = value
        }

        return value
    }

    // We need to migrate the writeVariables thing to be all about query structs, variable doesn't make sense and I don't want to break fringe 1.6 compatibility issues close to release
    fun Pokemon.asStruct(): ObjectValue<Pokemon> {
        val queryStruct = ObjectValue(this)
        queryStruct.addFunctions(pokemonFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return queryStruct
    }

    fun PartyStore.asMoLangValue(): ObjectValue<PartyStore> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.toString() }
        )
        value.addFunctions(pokemonStoreFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        value.addFunctions(partyFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun DropEntry.asMoLangValue(): ObjectValue<DropEntry> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.toString() }
        )
        value.addFunctions(dropEntryFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun PokemonProperties.asMoLangValue(): ObjectValue<PokemonProperties> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.asString() }
        )
        value.addFunctions(pokemonPropertiesFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun Evolution.asMoLangValue(): ObjectValue<Evolution> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.toString() }
        )
        value.addFunctions(evolutionFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun PCStore.asMoLangValue(): ObjectValue<PCStore> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.toString() }
        )
        value.addFunctions(pokemonStoreFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        value.addFunctions(pcFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun NPCEntity.asMoLangValue(): ObjectValue<NPCEntity> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.name.string }
        )
        value.addFunctions(entityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        value.addFunctions(livingEntityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        value.addFunctions(npcFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun PokemonEntity.asMoLangValue(): ObjectValue<PokemonEntity> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.pokemon.uuid.toString() }
        )
        value.addFunctions(entityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        value.addFunctions(livingEntityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        value.addFunctions(pokemonFunctions.flatMap { it(this.pokemon).entries.map { it.key to it.value } }.toMap()) // Convenience
        value.addFunctions(pokemonEntityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun PokemonBattle.asMoLangValue(): ObjectValue<PokemonBattle> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.battleId.toString() }
        )
        value.addFunctions(battleFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun BattleActor.asMoLangValue(): ObjectValue<BattleActor> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.toString() }
        )
        value.addFunctions(battleActorFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun SpawnablePosition.asMoLangValue(): ObjectValue<SpawnablePosition> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.toString() }
        )
        value.addFunctions(spawningContextFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
        return value
    }

    fun Vec3.asMoLangValue(): ObjectValue<Vec3> {
        val value = ObjectValue(
            obj = this
        )
        value.addFunction("x") { this.x }
        value.addFunction("y") { this.y }
        value.addFunction("z") { this.z }

        return value
    }

    /**
     * Different functions exist depending on the entity type, this tries to make the struct that's most specific to the type.
     */
    fun Entity.asMostSpecificMoLangValue(): ObjectValue<out Entity> {
        return when (this) {
            is Player -> asMoLangValue()
            is PokemonEntity -> struct
            is NPCEntity -> struct
            is ItemEntity -> ObjectValue(this).also {
                it.addStandardFunctions()
                it.addFunctions(entityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
                it.addFunctions(itemStackFunctions.flatMap { it(this.item, this.registryAccess()).entries.map { it.key to it.value } }.toMap())
            }
            else -> ObjectValue(this).also {
                it.addStandardFunctions()
                it.addFunctions(entityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
                if (this is LivingEntity) {
                    it.addFunctions(livingEntityFunctions.flatMap { it(this).entries.map { it.key to it.value } }.toMap())
                }
            }
        }
    }

    fun <T> Holder<T>.asMoLangValue(key: ResourceKey<Registry<T>>): ObjectValue<Holder<T>> {
        val value = ObjectValue(
            obj = this,
            stringify = { it.unwrapKey().get().location().toString() }
        )
        value.functions.put("is_in") {
            val tag = TagKey.create(key, ResourceLocation.parse(it.getString(0).replace("#", "")))
            return@put DoubleValue(if (value.obj.`is`(tag)) 1.0 else 0.0)
        }
        value.functions.put("is_of") {
            val identifier = ResourceLocation.parse(it.getString(0))
            return@put DoubleValue(if (value.obj.`is`(identifier)) 1.0 else 0.0)
        }
        return value
    }

    fun QueryStruct.addStandardFunctions(): QueryStruct {
        functions.putAll(generalFunctions)
        return this
    }

    fun QueryStruct.addEntityFunctions(entity: Entity): QueryStruct {
        val addedFunctions = entityFunctions
            .flatMap { it.invoke(entity).entries }
            .associate { it.key to it.value }
        functions.putAll(addedFunctions)
        return this
    }

    fun QueryStruct.addLivingEntityFunctions(entity: LivingEntity): QueryStruct {
        val addedFunctions = livingEntityFunctions
            .flatMap { it.invoke(entity).entries }
            .associate { it.key to it.value }
        functions.putAll(addedFunctions)
        return this
    }

    fun QueryStruct.addPokedexFunctions(pokedexManager: AbstractPokedexManager): QueryStruct {
        val addedFunctions = pokedexFunctions
            .flatMap { it.invoke(pokedexManager).entries }
            .associate { it.key to it.value }
        functions.putAll(addedFunctions)
        return this
    }

    fun QueryStruct.addSpeciesFunctions(species: Species): QueryStruct {
        val addedFunctions = speciesFunctions
            .flatMap { it.invoke(species).entries }
            .associate { it.key to it.value }
        functions.putAll(addedFunctions)
        return this
    }

    fun QueryStruct.addPokemonFunctions(pokemon: Pokemon): QueryStruct {
        val addedFunctions = pokemonFunctions
            .flatMap { it.invoke(pokemon).entries }
            .associate { it.key to it.value }
        functions.putAll(addedFunctions)
        return this
    }

    fun QueryStruct.addBattleMessageFunctions(battleMessage: BattleMessage): QueryStruct {
        val addedFunctions = battleMessageFunctions
            .flatMap { it.invoke(battleMessage).entries }
            .associate { it.key to it.value }
        functions.putAll(addedFunctions)
        return this
    }

    fun QueryStruct.addPokemonEntityFunctions(pokemonEntity: PokemonEntity): QueryStruct {
        val addedFunctions = pokemonEntityFunctions
            .flatMap { it.invoke(pokemonEntity).entries }
            .associate { it.key to it.value }
        functions.putAll(addedFunctions)
        pokemonEntity.registerFunctionsForScripting(this)
        return this
    }

    fun QueryStruct.addPokemonStoreFunctions(store: PokemonStore<*>): QueryStruct {
        val addedFunctions = pokemonStoreFunctions
            .flatMap { it.invoke(store).entries }
            .associate { it.key to it.value }
        functions.putAll(addedFunctions)
        return this
    }

    fun <T : QueryStruct> T.addFunctions(functions: Map<String, java.util.function.Function<MoParams, Any>>): T {
        this.functions.putAll(functions)
        return this
    }

    fun moLangFunctionMap(
        vararg functions: Pair<String, (MoParams) -> MoValue>
    ): Map<String, (MoParams) -> MoValue> {
        return functions.toMap()
    }

    fun queryStructOf(
        vararg functions: Pair<String, (MoParams) -> MoValue>
    ): QueryStruct {
        return QueryStruct(
            hashMapOf<String, java.util.function.Function<MoParams, Any>>(
                *functions.map { (name, func) ->
                    name to java.util.function.Function<MoParams, Any> { params -> func(params) }
                }.toTypedArray()
            )
        )
    }

    fun MoLangRuntime.setup(): MoLangRuntime {
        environment.query.addStandardFunctions()
        return this
    }

    fun writeMoValueToNBT(value: MoValue): Tag? {
        return when (value) {
            is DoubleValue -> DoubleTag.valueOf(value.value)
            is StringValue -> StringTag.valueOf(value.value)
            is ArrayStruct -> {
                val list = value.map.values
                val nbtList = ListTag()
                list.mapNotNull(::writeMoValueToNBT).forEach(nbtList::add)
                nbtList
            }
            is VariableStruct -> {
                val nbt = CompoundTag()
                value.map.forEach { (key, value) ->
                    val element = writeMoValueToNBT(value) ?: return@forEach
                    nbt.put(key, element)
                }
                nbt
            }
            else -> null
        }
    }

    fun readMoValueFromNBT(nbt: Tag): MoValue {
        return when (nbt) {
            is DoubleTag -> DoubleValue(nbt.asDouble)
            is StringTag -> StringValue(nbt.asString)
            is ListTag -> {
                val array = ArrayStruct(hashMapOf())
                var index = 0
                nbt.forEach { element ->
                    val value = readMoValueFromNBT(element)
                    array.setDirectly("$index", value)
                    index++
                }
                array
            }
            is CompoundTag -> {
                val variable = VariableStruct(hashMapOf())
                nbt.allKeys.toList().forEach { key ->
                    val value = readMoValueFromNBT(nbt[key]!!)
                    variable.map[key] = value
                }
                variable
            }
            else -> null
        } ?: throw IllegalArgumentException("Invalid NBT element type: ${nbt.type}")
    }
}

fun Either<ResourceLocation, ExpressionLike>.runScript(runtime: MoLangRuntime) = map({ CobblemonScripts.run(it, runtime) }, { it.resolve(runtime) })
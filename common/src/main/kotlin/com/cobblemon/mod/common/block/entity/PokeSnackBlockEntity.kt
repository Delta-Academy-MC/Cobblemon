/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.entity

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonBlockEntities
import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.api.cooking.getColourMixFromColors
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.cooking.PokeSnackSpawnPokemonEvent
import com.cobblemon.mod.common.api.fishing.SpawnBait
import com.cobblemon.mod.common.api.fishing.SpawnBait.Effect
import com.cobblemon.mod.common.api.fishing.SpawnBaitEffects
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools
import com.cobblemon.mod.common.api.spawning.SpawnCause
import com.cobblemon.mod.common.api.spawning.detail.EntitySpawnResult
import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail
import com.cobblemon.mod.common.api.spawning.influence.BucketMultiplyingInfluence
import com.cobblemon.mod.common.api.spawning.influence.BucketNormalizingInfluence
import com.cobblemon.mod.common.api.spawning.influence.SpawnBaitInfluence
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import com.cobblemon.mod.common.api.spawning.spawner.FixedAreaSpawner
import com.cobblemon.mod.common.block.PokeSnackBlock
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.item.components.BaitEffectsComponent
import com.cobblemon.mod.common.item.components.FoodColourComponent
import com.cobblemon.mod.common.item.components.IngredientComponent
import com.cobblemon.mod.common.net.messages.client.effect.PokeSnackBlockParticlesPacket
import com.cobblemon.mod.common.util.DataKeys
import java.util.UUID
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

open class PokeSnackBlockEntity(pos: BlockPos, state: BlockState) :
    TintBlockEntity(CobblemonBlockEntities.POKE_SNACK, pos, state),
    SpawningInfluence {

    companion object {
        const val SPAWNS_PER_BITE = 1
        const val RADIUS = 8
        const val RANDOM_TICKS_BETWEEN_SPAWNS = 2
        const val POKE_SNACK_CRUMBED_ASPECT = "poke_snack_crumbed"
    }

    val spawner: FixedAreaSpawner by lazy {
        FixedAreaSpawner(
            name = "poke_snack_spawner_${level?.dimension()?.location()}}_$blockPos",
            spawnPool = CobblemonSpawnPools.WORLD_SPAWN_POOL,
            world = level as ServerLevel,
            position = blockPos,
            horizontalRadius = RADIUS,
            verticalRadius = RADIUS,
            maxPokemonPerChunk = Cobblemon.config.pokeSnackPokemonPerChunk
        ).also {
            it.influences.add(this)

            val baitEffects = getBaitEffects()

            val stackedLureTier = baitEffects
                .filter { it.type == SpawnBait.Effects.RARITY_BUCKET }
                .sumOf { it.value }
                .toInt()

            if (stackedLureTier > 0) {
                it.influences += BucketNormalizingInfluence(
                    tier = stackedLureTier,
                    gradient = 0.2F,
                    firstTier = 1.2F
                )
            }

            it.influences += BucketMultiplyingInfluence(
                mapOf(
                    "uncommon" to 2.25f,
                    "rare" to 5.5f,
                    "ultra-rare" to 5.5f,
                )
            )

            it.influences += SpawnBaitInfluence(effects = baitEffects)
        }
    }

    // Only non-herd Pokémon can be spawned from a Poké Snack
    override fun affectSpawnable(detail: SpawnDetail, spawnablePosition: SpawnablePosition) = detail.type in SpawnDetail.pokemonTypes && detail !is PokemonHerdSpawnDetail

    // When a Pokémon spawns, apply the crumbed aspect and play the eating effects. Eat part of the cake, too.
    override fun affectSpawn(action: SpawnAction<*>, entity: Entity) {
        if (entity is PokemonEntity) {
            entity.pokemon.forcedAspects += POKE_SNACK_CRUMBED_ASPECT
            val block = blockState.block as? PokeSnackBlock ?: return
            val level = level ?: return
            val entityPos = entity.blockPosition()

            PokeSnackBlockParticlesPacket(blockPos, entityPos).sendToPlayersAround(
                blockPos.x.toDouble(),
                blockPos.y.toDouble(),
                blockPos.z.toDouble(),
                64.0,
                level.dimension(),
            )

            block.eat(level, blockPos, blockState, null)
        }
    }

    var placedBy: UUID? = null
    var amountSpawned: Int = 0
    var foodColourComponent: FoodColourComponent? = null
    var baitEffectsComponent: BaitEffectsComponent? = null
    var ingredientComponent: IngredientComponent? = null
    var randomTicksUntilNextSpawn: Float = getRandomTicksBetweenSpawns()

    fun initializeFromItemStack(itemStack: ItemStack) {
        foodColourComponent = itemStack.get(CobblemonItemComponents.FOOD_COLOUR)
        ingredientComponent = itemStack.get(CobblemonItemComponents.INGREDIENT)
        if (isLure()) baitEffectsComponent = itemStack.get(CobblemonItemComponents.BAIT_EFFECTS)

        foodColourComponent?.let {
            getColourMixFromColors(it.getColoursAsARGB())?.let(::setTint)
        }
    }

    fun isLure(): Boolean {
        val block = blockState.block
        if (block is PokeSnackBlock) {
            return block.isLure
        }
        return false
    }

    fun randomTick() {
        randomTicksUntilNextSpawn--
        if (randomTicksUntilNextSpawn > 0) {
            return
        }

        val nearestPlayer = level?.getNearestPlayer(
            blockPos.x.toDouble(),
            blockPos.y.toDouble(),
            blockPos.z.toDouble(),
            Cobblemon.config.maximumSpawningZoneDistanceFromPlayer.toDouble(),
            false, // true here makes it so creative players aren't considered
        )

        // High simulation distances may cause the player to be null here, we don't want to spawn without a player nearby.
        if (nearestPlayer != null) {
            attemptSpawn(nearestPlayer)
        }

        randomTicksUntilNextSpawn = getRandomTicksBetweenSpawns()
    }

    fun attemptSpawn(player: Player) {
        val cause = SpawnCause(spawner = spawner, entity = player)
        val zoneInput = spawner.getZoneInput(cause)
        val spawnAction = spawner.calculateSpawnActionsForArea(zoneInput, 1).firstOrNull()

        if (spawnAction == null) {
            return
        }

        CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_PRE.postThen(
            PokeSnackSpawnPokemonEvent.Pre(this, spawnAction),
            { },
            { event ->
                try {
                    spawnAction.complete()
                    val result = spawnAction.future
                    val resultingSpawn = result.get()

                    if (resultingSpawn is EntitySpawnResult) {
                        val pokemonEntity = resultingSpawn.entities.firstOrNull() as PokemonEntity
                        CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_POST.post(
                            PokeSnackSpawnPokemonEvent.Post(this, spawnAction, pokemonEntity)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    fun getRandomTicksBetweenSpawns(): Float {
        val biteTimeMultiplier = getBiteTimeMultiplier()
        return (RANDOM_TICKS_BETWEEN_SPAWNS * biteTimeMultiplier).coerceAtLeast(1F)
    }

    fun getBiteTimeMultiplier(): Float {
        val baitEffects = getBaitEffects()
        val biteTimeEffects = baitEffects.filter { it.type == SpawnBait.Effects.BITE_TIME }
        if (biteTimeEffects.isEmpty()) return 1F

        val biteTimeEffect = biteTimeEffects.random()
        if (Math.random() > biteTimeEffect.chance) {
            return 1F
        }

        return 1F - biteTimeEffect.value.toFloat()
    }

    fun toItemStack(): ItemStack {
        val stack = ItemStack(this.blockState.block)

        if (isLure() && baitEffectsComponent != null) {
            stack.set(CobblemonItemComponents.BAIT_EFFECTS, baitEffectsComponent)
        }

        if (foodColourComponent != null) {
            stack.set(CobblemonItemComponents.FOOD_COLOUR, foodColourComponent)
        }

        if (ingredientComponent != null) {
            stack.set(CobblemonItemComponents.INGREDIENT, ingredientComponent)
        }

        return stack
    }

    /**
     * Combine all the [SpawnBait.Effect] values from the [baitEffectsComponent] data.
     */
    fun getBaitEffects(): List<Effect> {
        return baitEffectsComponent
            ?.effects
            ?.mapNotNull(SpawnBaitEffects::getFromIdentifier)
            ?.flatMap { it.effects }
            .orEmpty()
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        tag.putInt(DataKeys.AMOUNT_SPAWNED, amountSpawned)

        foodColourComponent?.let { component ->
            CobblemonItemComponents.FOOD_COLOUR.codec()!!
                .encodeStart(NbtOps.INSTANCE, component)
                .result()
                .ifPresent { encodedTag ->
                    tag.put(DataKeys.FOOD_COLOUR, encodedTag)
                }
        }

        baitEffectsComponent?.let { component ->
            CobblemonItemComponents.BAIT_EFFECTS.codec()!!
                .encodeStart(NbtOps.INSTANCE, component)
                .result()
                .ifPresent { encodedTag ->
                    tag.put(DataKeys.BAIT_EFFECTS, encodedTag)
                }
        }

        ingredientComponent?.let { component ->
            CobblemonItemComponents.INGREDIENT.codec()!!
                .encodeStart(NbtOps.INSTANCE, component)
                .result()
                .ifPresent { encodedTag ->
                    tag.put(DataKeys.INGREDIENTS, encodedTag)
                }
        }

        tag.putFloat(DataKeys.TICKS_UNTIL_NEXT_SPAWN, randomTicksUntilNextSpawn)

        placedBy?.let {
            tag.putUUID(DataKeys.PLACED_BY, it)
        }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        amountSpawned = tag.getInt(DataKeys.AMOUNT_SPAWNED)

        if (tag.contains(DataKeys.FOOD_COLOUR)) {
            CobblemonItemComponents.FOOD_COLOUR.codec()
                ?.parse(NbtOps.INSTANCE, tag.get(DataKeys.FOOD_COLOUR))
                ?.result()
                ?.ifPresent { component ->
                    foodColourComponent = component
                }
        }

        if (tag.contains(DataKeys.BAIT_EFFECTS)) {
            CobblemonItemComponents.BAIT_EFFECTS.codec()
                ?.parse(NbtOps.INSTANCE, tag.get(DataKeys.BAIT_EFFECTS))
                ?.result()
                ?.ifPresent { component ->
                    baitEffectsComponent = component
                }
        }

        if (tag.contains(DataKeys.INGREDIENTS)) {
            CobblemonItemComponents.INGREDIENT.codec()
                ?.parse(NbtOps.INSTANCE, tag.get(DataKeys.INGREDIENTS))
                ?.result()
                ?.ifPresent { component ->
                    ingredientComponent = component
                }
        }

        if (tag.contains(DataKeys.TICKS_UNTIL_NEXT_SPAWN)) {
            randomTicksUntilNextSpawn = tag.getFloat(DataKeys.TICKS_UNTIL_NEXT_SPAWN)
        }

        if (tag.contains(DataKeys.PLACED_BY)) {
            placedBy = tag.getUUID(DataKeys.PLACED_BY)
        }
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registryLookup)
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener?>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}

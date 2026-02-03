/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.spawning.influence

import com.cobblemon.mod.common.Cobblemon.LOGGER
import com.cobblemon.mod.common.api.fishing.SpawnBait
import com.cobblemon.mod.common.api.fishing.SpawnBait.Effects
import com.cobblemon.mod.common.api.fishing.SpawnBaitUtils
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.world.entity.Entity

/**
 * A [SpawningInfluence] that applies some number of SpawnBait effects.
 *
 * @author Hiroku, Plastered_Crab, ppVon
 * @since March 18th, 2025
 */
open class SpawnBaitInfluence(val effects: List<SpawnBait.Effect>, val onUsed: (time: Int, entity: PokemonEntity?) -> Unit = { _, _ -> }) : SpawningInfluence {

    var usedTimes = 0

    private fun markUsed(entity: PokemonEntity? = null) {
        usedTimes++
        onUsed(usedTimes, entity)
    }

    override fun affectSpawn(action: SpawnAction<*>, entity: Entity) {
        if (entity is PokemonEntity) {
            val merged = SpawnBaitUtils.mergeEffects(effects)
            merged.forEach { effect ->
                if (Math.random() <= effect.chance) {
                    markUsed(entity)
                    Effects.getEffectFunction(effect.type)?.invoke(entity, effect)
                }
            }
        }
    }

    // EV, Type, and Egg Group related bait effects
    override fun affectWeight(detail: SpawnDetail, spawnablePosition: SpawnablePosition, weight: Float): Float {
        val merged = SpawnBaitUtils.mergeEffects(effects)

        val hasRelevantEffects =
            merged.any { it.type == Effects.EV } ||
            merged.any { it.type == Effects.TYPING } ||
            merged.any { it.type == Effects.EGG_GROUP }

        if (!hasRelevantEffects) {
            return super.affectWeight(detail, spawnablePosition, weight)
        }

        val pokemonDetail = detail as? PokemonSpawnDetail
            ?: return super.affectWeight(detail, spawnablePosition, weight)

        val detailSpecies = pokemonDetail.pokemon.species
            ?.let { PokemonSpecies.getByName(it) }
            ?: return super.affectWeight(detail, spawnablePosition, weight)

        val formData = detailSpecies.getForm(pokemonDetail.pokemon.aspects)

        var newWeight = weight
        var used = false

        // if bait exists and any effects are related to EV yields
        if (merged.any { it.type == Effects.EV }) {
            val evEffect = effects.firstOrNull { it.type == Effects.EV }
            val baitEVStat = evEffect?.subcategory?.path?.let { Stats.getStat(it) }

            if (baitEVStat != null) {
                val evYieldValue = formData.evYield[baitEVStat]?.toFloat() ?: 0f
                used = true

                if (evYieldValue <= 0f) {
                    newWeight = 0f
                } else {
                    LOGGER.debug("Boosted weight value");
                }
            }
        }

        // if bait exists and any effects are related to Typing
        if (newWeight > 0f && merged.any { it.type == Effects.TYPING }) {
            val baitEffect = effects.firstOrNull { it.type == Effects.TYPING }
            val baitTypingEffect = baitEffect?.subcategory?.path?.let { ElementalTypes.get(it) }

            if (baitEffect != null && baitTypingEffect != null) {
                val isMatchingType = formData.types.contains(baitTypingEffect)
                used = true

                if (isMatchingType) {
                    newWeight *= baitEffect.value.toFloat()
                }
            }
        }

        // if bait exists and any effects are related to Egg Groups
        if (newWeight > 0f && merged.any { it.type == Effects.EGG_GROUP }) {
            val eggGroupEffects = effects.filter { it.type == Effects.EGG_GROUP }

            val matchingEffect = eggGroupEffects.firstOrNull { effect ->
                val key = effect.subcategory?.path ?: return@firstOrNull false
                val eggGroup = EggGroup.fromIdentifier(key)
                if (eggGroup == null) {
                    LOGGER.warn("Unknown egg group identifier: $key")
                    return@firstOrNull false
                }
                formData.eggGroups.contains(eggGroup)
            }

            if (matchingEffect != null) {
                used = true
                newWeight *= matchingEffect.value.toFloat()
            }
        }

        if (used) markUsed()
        return super.affectWeight(detail, spawnablePosition, newWeight)
    }
}
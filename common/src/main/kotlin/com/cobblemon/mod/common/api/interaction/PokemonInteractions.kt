/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.interaction

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.conditional.RegistryLikeCondition
import com.cobblemon.mod.common.api.data.JsonDataRegistry
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.requirement.Requirement
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.client.data.PokemonInteractionsSyncPacket
import com.cobblemon.mod.common.pokemon.adapters.CobblemonRequirementAdapter
import com.cobblemon.mod.common.pokemon.evolution.adapters.LegacyItemConditionWrapperAdapter
import com.cobblemon.mod.common.pokemon.requirements.PokemonPropertiesRequirement
import com.cobblemon.mod.common.util.adapters.*
import com.cobblemon.mod.common.util.cobblemonResource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackType
import net.minecraft.util.LowerCaseEnumTypeAdapterFactory
import net.minecraft.world.item.Item
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.material.Fluid

object PokemonInteractions : JsonDataRegistry<PokemonInteractionSet> {
    override val id = cobblemonResource("pokemon_interactions")
    override val type = PackType.SERVER_DATA
    override val observable = SimpleObservable<PokemonInteractions>()
    override val typeToken: TypeToken<PokemonInteractionSet> = TypeToken.get(PokemonInteractionSet::class.java)
    override val resourcePath = "pokemon_interactions"
    override val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ResourceLocation::class.java, IdentifierAdapter)
        .registerTypeAdapter(PokemonProperties::class.java, pokemonPropertiesShortAdapter)
        .registerTypeAdapter(ItemPredicate::class.java, LegacyItemConditionWrapperAdapter)
        .registerTypeAdapter(Requirement::class.java, CobblemonRequirementAdapter)
        .registerTypeAdapter(InteractionEffect::class.java, InteractionEffectAdapter)
        .registerTypeAdapter(ExpressionLike::class.java, ExpressionLikeAdapter)
        .registerTypeAdapter(TypeToken.getParameterized(RegistryLikeCondition::class.java, Biome::class.java).type, BiomeLikeConditionAdapter)
        .registerTypeAdapter(TypeToken.getParameterized(RegistryLikeCondition::class.java, Block::class.java).type, BlockLikeConditionAdapter)
        .registerTypeAdapter(TypeToken.getParameterized(RegistryLikeCondition::class.java, Item::class.java).type, ItemLikeConditionAdapter)
        .registerTypeAdapter(TypeToken.getParameterized(RegistryLikeCondition::class.java, Structure::class.java).type, StructureLikeConditionAdapter)
        .registerTypeAdapter(TypeToken.getParameterized(RegistryLikeCondition::class.java, Fluid::class.java).type, FluidLikeConditionAdapter)
        .registerTypeAdapter(IntRange::class.java, IntRangeAdapter)
        .registerTypeAdapterFactory(LowerCaseEnumTypeAdapterFactory())
        .setPrettyPrinting()
        .create()

    val interactions = mutableListOf<PokemonInteractionSet>()

    override fun sync(player: ServerPlayer) {
        PokemonInteractionsSyncPacket(interactions).sendToPlayer(player)
    }

    override fun reload(data: Map<ResourceLocation, PokemonInteractionSet>) {
        interactions.clear()
        val split = data.entries
            .partition {
                it.value.requirements.any { requirement -> requirement is PokemonPropertiesRequirement && requirement.target.species != null }
            }

        // Put the species specific interactions first as they take priority
        interactions.addAll(split.first.map {it.value})
        interactions.addAll(split.second.map {it.value})

        Cobblemon.LOGGER.info("Loaded {} Pokémon interaction sets", interactions.size)
    }

    fun findInteraction(pokemon: PokemonEntity): PokemonInteraction? {
        val setCheck: (PokemonInteractionSet) -> Boolean = { it.requirements.all { req -> req.check(pokemon.pokemon) }}
        val interactionCheck: (PokemonInteraction) -> Boolean = { it.requirements.all { req -> req.check(pokemon.pokemon) } && !pokemon.pokemon.isOnInteractionCooldown(it.grouping)  }
        return interactions
            .filter(setCheck)
            .flatMap { it.interactions }
            .firstOrNull(interactionCheck)
    }
}
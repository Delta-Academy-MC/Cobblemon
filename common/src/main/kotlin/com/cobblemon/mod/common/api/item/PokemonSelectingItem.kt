/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.item

import com.cobblemon.mod.common.advancement.CobblemonCriteria
import com.cobblemon.mod.common.advancement.criterion.PokemonInteractContext
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.callback.PartySelectCallbacks
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.battles.BagItemActionResponse
import com.cobblemon.mod.common.battles.BattleRules
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.item.battle.BagItem
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB

/**
 * Interface to make it easier to define items that will, upon use, prompt you to
 * select a Pokémon for it to be used on. This can be by interacting with the Pokémon
 * directly (which will skip the Pokémon selection screen) or by interacting with air
 * which will first show a Pokémon selection screen.
 *
 * Works for battle items too.
 *
 * @author Hiroku
 * @since July 29th, 2023
 */
interface PokemonSelectingItem {
    fun use(player: ServerPlayer, stack: ItemStack): InteractionResultHolder<ItemStack> {
        val range = player.entityInteractionRange()
        val entity = player.level()
            .getEntities(player, AABB.ofSize(player.position(), range, range, range))
            .filter { player.isLookingAt(it, stepDistance = 0.1F) }
            .minByOrNull { it.distanceTo(player) } as? PokemonEntity?

        player.getBattleState()?.let { (_, actor) ->
            if (bagItem == null) return InteractionResultHolder.fail(stack)
            val battlePokemon = actor.pokemonList.find { it.effectedPokemon == entity?.pokemon }

            if (!actor.canFitForcedAction() || actor.battle.format.ruleSet.contains(BattleRules.BAG_CLAUSE)) {
                player.sendSystemMessage(battleLang("bagitem.cannot").red())
                return InteractionResultHolder.fail(stack)
            }

            if (entity == null) {
                return interactGeneralBattle(player, stack, actor)
            } else if (battlePokemon != null) {
                return interactWithSpecificBattle(player, stack, battlePokemon)
            }
        } ?: run {
            if (!player.isShiftKeyDown) {
                return if (entity != null) {
                    val pokemon = entity.pokemon
                    if (entity.ownerUUID == player.uuid) {
                        val typedInteractionResult = applyToPokemon(player, stack, pokemon)
                        if (typedInteractionResult != null)
                        {
                            CobblemonCriteria.POKEMON_INTERACT.trigger(player, PokemonInteractContext(pokemon.species.resourceIdentifier, BuiltInRegistries.ITEM.getKey(stack.item)))
                            typedInteractionResult
                        }
                        else {
                            InteractionResultHolder.pass(stack)
                        }
                    } else {
                        InteractionResultHolder.fail(stack)
                    }
                } else {
                    interactGeneral(player, stack)
                }
            }
        }

        return InteractionResultHolder.pass(stack)
    }

    val bagItem: BagItem?
    fun applyToPokemon(player: ServerPlayer, stack: ItemStack, pokemon: Pokemon): InteractionResultHolder<ItemStack>?

    fun applyToBattlePokemon(player: ServerPlayer, stack: ItemStack, battlePokemon: BattlePokemon) {
        val battle = battlePokemon.actor.battle
        val bagItem = bagItem
        if (!battlePokemon.actor.canFitForcedAction() || battle.format.ruleSet.contains(BattleRules.BAG_CLAUSE)) {
            player.sendSystemMessage(battleLang("bagitem.cannot").red())
        } else if (!bagItem!!.canUse(stack, battle, battlePokemon)) {
            player.sendSystemMessage(battleLang("bagitem.invalid").red())
        } else {
            battlePokemon.actor.forceChoose(BagItemActionResponse(bagItem, battlePokemon))
            val stackName = BuiltInRegistries.ITEM.getKey(stack.item)
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1)
                battlePokemon.actor.itemsUsed.add(bagItem)
            }
            CobblemonCriteria.POKEMON_INTERACT.trigger(player, PokemonInteractContext(battlePokemon.effectedPokemon.species.resourceIdentifier, stackName))
        }
    }

    fun canUseOnPokemon(stack: ItemStack, pokemon: Pokemon): Boolean {
        if (stack.`is`(CobblemonItemTags.POKE_FOOD)) {
            return !pokemon.isFull()
        }
        return true
    }

    fun canUseOnBattlePokemon(stack: ItemStack, battlePokemon: BattlePokemon): Boolean = bagItem!!.canUse(stack, battlePokemon.actor.battle, battlePokemon)

    fun interactWithSpecificBattle(player: ServerPlayer, stack: ItemStack, battlePokemon: BattlePokemon): InteractionResultHolder<ItemStack> {
        return if (canUseOnBattlePokemon(stack, battlePokemon)) {
            applyToBattlePokemon(player, stack, battlePokemon)
            InteractionResultHolder.success(stack)
        } else {
            player.sendSystemMessage(battleLang("bagitem.invalid").red())
            InteractionResultHolder.fail(stack)
        }
    }

    fun interactGeneral(player: ServerPlayer, stack: ItemStack): InteractionResultHolder<ItemStack> {
        val party = player.party().toList()
        if (party.isEmpty()) {
            return InteractionResultHolder.fail(stack)
        }

        PartySelectCallbacks.createFromPokemon(
            player = player,
            pokemon = party,
            canSelect = { pk -> canUseOnPokemon(stack, pk) },
            handler = { pk ->
                if (stack.isHeld(player)) {
                    applyToPokemon(player, stack, pk)
                    CobblemonCriteria.POKEMON_INTERACT.trigger(player, PokemonInteractContext(pk.species.resourceIdentifier, BuiltInRegistries.ITEM.getKey(stack.item)))
                }
            }
        )


        return InteractionResultHolder.success(stack)
    }

    fun interactGeneralBattle(player: ServerPlayer, stack: ItemStack, actor: BattleActor): InteractionResultHolder<ItemStack> {
        PartySelectCallbacks.createBattleSelect(
            player = player,
            pokemon = actor.pokemonList,
            canSelect = { pk -> canUseOnBattlePokemon(stack, actor.pokemonList.find { it.effectedPokemon == pk.effectedPokemon }!!) },
            handler = { pk -> applyToBattlePokemon(player, stack, actor.pokemonList.find { it.effectedPokemon == pk.effectedPokemon }!!) }
        )

        return InteractionResultHolder.success(stack)
    }
}

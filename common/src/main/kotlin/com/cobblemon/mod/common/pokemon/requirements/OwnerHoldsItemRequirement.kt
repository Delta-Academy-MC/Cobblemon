/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.requirements

import com.cobblemon.mod.common.api.pokemon.requirement.OwnerQueryRequirement
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

/**
 * An [OwnerQueryRequirement] that checks if the player/npc is holding a certain [ItemPredicate]
 *
 * @param itemCondition The [ItemPredicate] the owner is supposed to hold
 *
 * @author Polymeta
 */
class OwnerHoldsItemRequirement(val itemCondition: ItemPredicate)
    : OwnerQueryRequirement {

    override fun checkPlayer(
        pokemon: Pokemon,
        owner: Player
    ): Boolean {
        return checkItem(owner)
    }

    override fun checkNPC(
        pokemon: Pokemon,
        owner: NPCEntity
    ): Boolean {
        return checkItem(owner)
    }

    fun checkItem(entity: LivingEntity): Boolean {
        return itemCondition.test(entity.getItemInHand(InteractionHand.MAIN_HAND))
    }

    companion object {
        val ADAPTER_VARIANT = "owner_held_item"
    }
}
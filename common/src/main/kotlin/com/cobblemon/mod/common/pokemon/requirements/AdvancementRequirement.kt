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
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

/**
 * An [com.cobblemon.mod.common.api.pokemon.requirement.Requirement] that checks if the player has a certain [net.minecraft.advancements.Advancement]
 *
 * @param requiredAdvancement The [ResourceLocation] of the required advancement
 *
 * @author whatsy
 */
class AdvancementRequirement(val requiredAdvancement: ResourceLocation) : OwnerQueryRequirement {

    override fun checkPlayer(
        pokemon: Pokemon,
        owner: Player
    ): Boolean {
        val serverPlayer = owner as? ServerPlayer ?: return false

        for (entry in serverPlayer.advancements.progress) {
            if (entry.key.id == requiredAdvancement && entry.value.isDone) {
                return true
            }
        }

        return false
    }

    override fun checkNPC(
        pokemon: Pokemon,
        owner: NPCEntity
    ): Boolean {
        //TODO some sort of config?
        return true
    }

    companion object {
        val ADAPTER_VARIANT = "advancement"
    }
}
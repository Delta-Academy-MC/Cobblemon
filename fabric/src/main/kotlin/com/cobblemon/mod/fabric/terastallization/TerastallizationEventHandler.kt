/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.terastallization

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.drop.ItemDropEntry
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.types.tera.TeraTypes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import dev.emi.trinkets.api.TrinketsApi
import net.minecraft.resources.ResourceLocation
import kotlin.random.Random

object TerastallizationEventHandler {

    fun initialise() {
        CobblemonEvents.LOOT_DROPPED.subscribe { event ->
            val player = event.player
            val pokemon = event.entity as? PokemonEntity ?: return@subscribe
            val trinkets = TrinketsApi.getTrinketComponent(player).orElse(null) ?: return@subscribe
            if (!trinkets.isEquipped(CobblemonItems.TERA_ORB)) return@subscribe
            if (Random.nextInt(5) != 0) return@subscribe
            val shardType = if (Random.nextInt(10) == 0) TeraTypes.STELLAR else TeraTypes.forElementalType(pokemon.pokemon.types.toList().random())
            val drop = ItemDropEntry().also {
                it.item = ResourceLocation.parse("cobblemon:${shardType.name.lowercase()}_tera_shard")
            }
            event.drops.add(drop)
        }
    }
}
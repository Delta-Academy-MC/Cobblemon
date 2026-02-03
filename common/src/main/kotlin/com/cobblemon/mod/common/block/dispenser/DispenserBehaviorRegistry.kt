/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.dispenser

import com.cobblemon.mod.common.block.SaccharineLeafBlock
import com.cobblemon.mod.common.block.SaccharineLogBlock
import com.cobblemon.mod.common.block.SaccharineLogSlatheredBlock
import com.cobblemon.mod.common.mixin.accessor.DispenserBlockAccessor
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.block.DispenserBlock

object DispenserBehaviorRegistry {
    private val behaviors = mutableMapOf<Item, DispenseItemBehavior>()

    fun register(items: List<Item>, behaviorProvider: (Item) -> DispenseItemBehavior) {
        register(behaviorProvider, items = items)
    }

    fun register(vararg behaviorProviders: (Item) -> DispenseItemBehavior, items: List<Item>) {
        for (item in items) {
            val providedBehaviors = behaviorProviders.map { provider -> provider(item) }

            val existingLocal = behaviors[item]
            if (existingLocal is ChainedDispenseItemBehavior) {
                providedBehaviors.forEach { existingLocal.add(it) }
                continue
            }

            val currentRegistered = getCurrentRegisteredBehavior(item)
            if (currentRegistered is ChainedDispenseItemBehavior) {
                behaviors[item] = currentRegistered
                providedBehaviors.forEach { currentRegistered.add(it) }
                continue
            }

            val base = currentRegistered ?: DispenseItemBehavior.NOOP
            val chained = ChainedDispenseItemBehavior(base)
            providedBehaviors.forEach { chained.add(it) }

            behaviors[item] = chained
            DispenserBlock.registerBehavior(item, chained)
        }
    }

    private fun getCurrentRegisteredBehavior(item: Item): DispenseItemBehavior? {
        return try {
            val map = DispenserBlockAccessor.getItemBehaviors()
            map[item]
        } catch (t: Throwable) {
            DispenseItemBehavior.LOGGER.error("Failed to read existing dispenser behaviors via mixin accessor", t)
            null
        }
    }

    fun registerDispenserBehaviors() {
        register(
            { SaccharineLogSlatheredBlock.createBehavior() },
            items = listOf(
                PotionContents.createItemStack(
                    Items.POTION,
                    Potions.WATER
                ).item
            )
        )
        register(
            { SaccharineLogBlock.createBehavior() },
            items = listOf(Items.HONEY_BOTTLE)
        )
        register(
            { item -> SaccharineLeafBlock.createBehavior(item) },
            items = listOf(
                Items.HONEY_BOTTLE,
                PotionContents.createItemStack(
                    Items.POTION,
                    Potions.WATER
                ).item
            )
        )
    }
}
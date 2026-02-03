/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.dispenser

import net.minecraft.core.dispenser.BlockSource
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.world.item.ItemStack
import java.util.concurrent.CopyOnWriteArrayList

class ChainedDispenseItemBehavior(private val base: DispenseItemBehavior) : DispenseItemBehavior {
    private val extras = CopyOnWriteArrayList<DispenseItemBehavior>()

    fun add(behavior: DispenseItemBehavior) {
        extras.add(behavior)
    }

    override fun dispense(blockSource: BlockSource, itemStack: ItemStack): ItemStack {

        var current = itemStack
        for (b in extras) {
            val prev = current.copy()
            val next = try {
                b.dispense(blockSource, current)
            } catch (t: Throwable) {
                continue
            }
            current = next
            if (handled(prev, current)) {
                return current
            }
        }

        val beforeBase = current.copy()
        val afterBase = try {
            base.dispense(blockSource, current)
        } catch (_: Throwable) {
            return current
        }

        if (handled(beforeBase, afterBase)) {
            return afterBase
        }

        return afterBase
    }

    private fun handled(before: ItemStack, after: ItemStack): Boolean {
        return !ItemStack.matches(before, after)
    }

}
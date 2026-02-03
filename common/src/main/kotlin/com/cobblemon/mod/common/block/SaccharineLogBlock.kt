/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.SaccharineLogSlatheredBlock.Companion.HONEY_TYPE
import com.cobblemon.mod.common.block.SaccharineLogSlatheredBlock.Companion.HONEY_TYPE_MAX
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult
import kotlin.random.Random

class SaccharineLogBlock(properties: Properties) : RotatedPillarBlock(properties) {
    companion object {
        fun createBehavior(): DispenseItemBehavior {
            return DispenseItemBehavior { source, stack ->
                val level = source.level
                val facing = source.state.getValue(DispenserBlock.FACING)
                val pos = source.pos.relative(facing)
                val blockState = level.getBlockState(pos)

                if (blockState.block is SaccharineLogBlock) {
                    val newState = CobblemonBlocks.SACCHARINE_LOG_SLATHERED.defaultBlockState()
                        .setValue(AXIS, blockState.getValue(AXIS))
                    changeLogTypeDispenser(level, pos, newState, stack, source)
                }
                stack
            }
        }

        fun changeLogTypeDispenser(
            level: ServerLevel,
            pos: BlockPos,
            newState: BlockState,
            stack: ItemStack,
            source: BlockSource
        ) {
            level.setBlock(pos, newState, 3)
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos)

            stack.shrink(1)

            val dispenserEntity = source.blockEntity
            val glassBottle = ItemStack(Items.GLASS_BOTTLE)
            var added = false

            for (i in 0 until dispenserEntity.containerSize) {
                val slotStack = dispenserEntity.getItem(i)

                if (slotStack.isEmpty) {
                    dispenserEntity.setItem(i, glassBottle.copy())
                    added = true
                    break
                } else if (slotStack.`is`(Items.GLASS_BOTTLE) && slotStack.count < slotStack.maxStackSize) {
                    slotStack.grow(1)
                    added = true
                    break
                }
            }

            if (!added) {
                Containers.dropItemStack(
                    level,
                    source.pos.x.toDouble(),
                    source.pos.y.toDouble(),
                    source.pos.z.toDouble(),
                    glassBottle
                )
            }
        }

        fun changeLogType(
            level: Level,
            pos: BlockPos,
            newState: BlockState,
            player: Player? = null,
            itemStack: ItemStack? = null
        ) {
            level.setBlock(pos, newState, 3)
            level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos)

            if (player != null && !player.hasInfiniteMaterials() && itemStack != null) {
                itemStack.consume(1, player)

                // Give glass bottle
                val glassBottle = ItemStack(Items.GLASS_BOTTLE)
                if (!player.addItem(glassBottle)) player.drop(glassBottle, false)
            }
        }
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        val axis = state.getValue(AXIS)
        val itemStack = player.getItemInHand(hand)
        if (!level.isClientSide && itemStack.`is`(Items.HONEY_BOTTLE) && axis == Direction.Axis.Y) {
            val facing = hitResult.direction
            if (facing != Direction.UP && facing != Direction.DOWN) {
                val fluidState = level.getFluidState(pos.relative(facing))
                // Replace block with honey log if interacted face is not touching fluid
                if (fluidState.amount <= 3) {
                    val randomType = Random.nextInt(0, HONEY_TYPE_MAX + 1)
                    val newState = CobblemonBlocks.SACCHARINE_LOG_SLATHERED.defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, facing)
                        .setValue(AXIS, Direction.Axis.Y)
                        .setValue(HONEY_TYPE, randomType)
                    changeLogType(level, pos, newState, player, itemStack)
                    level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS)
                    return ItemInteractionResult.SUCCESS
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult)
    }
}

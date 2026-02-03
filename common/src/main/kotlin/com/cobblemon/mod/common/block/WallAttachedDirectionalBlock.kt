/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids

open class WallAttachedDirectionalBlock(settings: Properties): HorizontalDirectionalBlock(settings), SimpleWaterloggedBlock {
    companion object {
        val CODEC = simpleCodec(::WallAttachedDirectionalBlock)
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(HORIZONTAL_FACING, Direction.NORTH)
            .setValue(WATERLOGGED, false))
    }

    override fun codec(): MapCodec<out WallAttachedDirectionalBlock> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(HORIZONTAL_FACING, WATERLOGGED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        var direction = context.clickedFace
        if (direction === null || direction.axis === Direction.Axis.Y) {
            direction = context.horizontalDirection.opposite
        }

        val fluidState = context.level.getFluidState(context.clickedPos)
        val blockState = this.defaultBlockState()
            .setValue(HORIZONTAL_FACING, direction)
            .setValue(WATERLOGGED, fluidState.type === Fluids.WATER
        ) as BlockState

        if (blockState.canSurvive(context.getLevel(), context.getClickedPos())) {
            return blockState
        }
        return null
    }

    override fun getFluidState(blockState: BlockState): FluidState? = if (blockState.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(blockState)

    override fun updateShape(state: BlockState, direction: Direction, neighborState: BlockState, world: LevelAccessor, pos: BlockPos, neighborPos: BlockPos): BlockState {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world))
        }
        return if (state.getValue(HORIZONTAL_FACING).opposite == direction && !state.canSurvive(world, pos)) Blocks.AIR.defaultBlockState()
        else super.updateShape(state, direction, neighborState, world, pos, neighborPos)
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        return canAttach(level, pos, state.getValue(HORIZONTAL_FACING))
    }

    fun canAttach(reader: LevelReader, pos: BlockPos, direction: Direction): Boolean {
        val blockPos = pos.relative(direction.opposite)
        return reader.getBlockState(blockPos).isFaceSturdy(reader, blockPos, direction)
    }
}

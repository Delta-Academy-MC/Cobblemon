/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.util.rotateShape
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.min

class WallAttachedStackableItemBlock(settings: Properties, val type: WallStackableItemBlockType): WallAttachedDirectionalBlock(settings) {
    enum class WallStackableItemBlockType: StringRepresentable {
        TAG;

        override fun getSerializedName(): String = this.name.lowercase()

        companion object {
            val CODEC = StringRepresentable.fromEnum(::values)
        }
    }

    companion object {
        val MAX_AMOUNT: Int = 8
        val AMOUNT: IntegerProperty = IntegerProperty.create("amount", 1, MAX_AMOUNT)

        val CODEC: MapCodec<WallAttachedStackableItemBlock> = RecordCodecBuilder.mapCodec { it.group(
            propertiesCodec(),
            WallStackableItemBlockType.CODEC.fieldOf("type").forGetter(WallAttachedStackableItemBlock::type)
        ).apply(it, ::WallAttachedStackableItemBlock) }

        val northShapeMap: Map<WallStackableItemBlockType, List<VoxelShape>> = mapOf(
            WallStackableItemBlockType.TAG to listOf(
                Shapes.box(0.375, 0.25, 0.9375, 0.625, 0.6875, 1.0),
                Shapes.box(0.375, 0.0625, 0.9375, 1.0, 0.6875, 1.0),
                Shapes.box(0.0, 0.0625, 0.9375, 1.0, 0.9375, 1.0),
                Shapes.box(0.0, 0.0625, 0.9375, 1.0, 1.0, 1.0),
                Shapes.box(0.0, 0.0, 0.9375, 1.0, 1.0, 1.0),
                Shapes.box(0.0, 0.0, 0.9375, 1.0, 1.0, 1.0),
                Shapes.box(0.0, 0.0, 0.9375, 1.0, 1.0, 1.0),
                Shapes.box(0.0, 0.0, 0.9375, 1.0, 1.0, 1.0)
            )
        )

        fun getShape(type: WallStackableItemBlockType, count: Int): VoxelShape = northShapeMap[type]?.let { list -> list[(count - 1).coerceIn(0, list.lastIndex)] } ?: Shapes.block()
        fun getMaxAmount(type: WallStackableItemBlockType): Int = min(northShapeMap[type]?.size ?: 1 , MAX_AMOUNT)
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(HORIZONTAL_FACING, Direction.NORTH)
            .setValue(AMOUNT, 1)
            .setValue(WATERLOGGED, false))
    }

    override fun codec(): MapCodec<WallAttachedStackableItemBlock> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(AMOUNT)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val direction = state.getValue(HORIZONTAL_FACING)
        val count = state.getValue(AMOUNT)
        return rotateShape(Direction.NORTH, direction, getShape(type, count))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val blockState = context.level.getBlockState(context.clickedPos)
        return if (blockState.`is`(this)) blockState.setValue(AMOUNT, min(getMaxAmount(type), (blockState.getValue(AMOUNT) + 1)))
            else super.getStateForPlacement(context)
    }

    override fun canBeReplaced(state: BlockState, useContext: BlockPlaceContext): Boolean {
        return if (
            !useContext.isSecondaryUseActive
            && useContext.itemInHand.`is`(this.asItem())
            && state.getValue(AMOUNT) <= getMaxAmount(type)
        ) true else super.canBeReplaced(state, useContext)
    }
}

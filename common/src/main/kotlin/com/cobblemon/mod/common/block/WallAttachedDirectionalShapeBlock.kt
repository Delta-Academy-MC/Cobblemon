/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.util.rotateShape
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class WallAttachedDirectionalShapeBlock(settings: Properties, val width: Int = 16, val height: Int = 16, val depth: Int = 16): WallAttachedDirectionalBlock(settings) {
    companion object {
        /**
         * Calculates a [VoxelShape] based on a center point of (8, 8, 16).
         *
         * The resulting shape is centered X horizontally and Y vertically
         * expanding inwards/North along the Z-axis from the South face (16).
         *
         * @param width The width of the shape from 0-16, along the X-axis.
         * @param height The height of the shape from 0-16 along the Y-axis.
         * @param depth The depth of the shape from 0-16 along the Z-axis, extending inwards from 16.
         * @return A VoxelShape.
         */
        fun convertToShape(sizeX: Int, sizeY: Int, sizeZ: Int): VoxelShape {
            val x = sizeX.coerceIn(0, 16).toDouble()
            val y = sizeY.coerceIn(0, 16).toDouble()
            val z = sizeZ.coerceIn(0, 16).toDouble()

            val minX = (8.0 - (x / 2.0)) / 16.0
            val maxX = (8.0 + (x / 2.0)) / 16.0

            val minY = (8.0 - (y / 2.0)) / 16.0
            val maxY = (8.0 + (y / 2.0)) / 16.0

            val minZ = (16.0 - z) / 16.0

            return Shapes.box(minX, minY, minZ, maxX, maxY, 1.0)
        }
    }

    override fun codec(): MapCodec<WallAttachedDirectionalShapeBlock> = RecordCodecBuilder.mapCodec { instance ->
        instance.group(
            propertiesCodec(),
            Codec.INT.fieldOf("width").forGetter { it.width },
            Codec.INT.fieldOf("height").forGetter { it.height },
            Codec.INT.fieldOf("depth").forGetter { it.depth }
        ).apply(instance, { properties, width, height, depth ->
            WallAttachedDirectionalShapeBlock(properties, width, height, depth)
        })
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val direction = state.getValue(FACING)
        return rotateShape(Direction.NORTH, direction, convertToShape(width, height, depth))
    }
}

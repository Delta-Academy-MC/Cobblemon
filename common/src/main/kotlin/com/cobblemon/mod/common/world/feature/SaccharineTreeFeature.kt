/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.feature

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.api.tags.CobblemonBiomeTags
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.UP
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.BlockTags
import net.minecraft.util.RandomSource
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block.UPDATE_ALL
import net.minecraft.world.level.block.Block.UPDATE_CLIENTS
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.entity.BeehiveBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.TreeFeature
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration
import java.util.function.Consumer

class SaccharineTreeFeature : Feature<BlockStateConfiguration>(BlockStateConfiguration.CODEC) {

    override fun place(context: FeaturePlaceContext<BlockStateConfiguration>): Boolean {
        val worldGenLevel: WorldGenLevel = context.level()
        val random = context.random()
        val origin = context.origin()

        val isGenerating = worldGenLevel.getChunk(origin).persistedStatus != ChunkStatus.FULL

        if (isGenerating) {
            val biome = worldGenLevel.getBiome(origin)
            if (!biome.`is`(CobblemonBiomeTags.ALLOWED_BIOMES_SACCHARINE_TREE)) {
                return false
            }
            val multiplier = when {
                biome.`is`(CobblemonBiomeTags.HAS_APRICORNS_SPARSE) -> 0.1F
                biome.`is`(CobblemonBiomeTags.HAS_APRICORNS_DENSE) -> 10F
                biome.`is`(CobblemonBiomeTags.HAS_APRICORNS_NORMAL) -> 1.0F
                else -> return false
            }

            if (random.nextFloat() > multiplier * Cobblemon.config.baseApricornTreeGenerationChance) {
                return false
            }
        }

        if (!worldGenLevel.getBlockState(origin.below()).`is`(BlockTags.DIRT)) {
            return false
        }

        // Determine tree height and variant
        val trunkHeight = if (random.nextBoolean()) 1 else 2
        val actualHeight = trunkHeight + 5

        for (y in 0 until actualHeight) {
            val pos = origin.relative(UP, y)
            if (!TreeFeature.isAirOrLeaves(worldGenLevel, pos)) {
                return false
            }
        }

        // Place the tree
        val potentialBeeNestPositions = mutableListOf<BlockPos>()

        // Create trunk
        val logState = CobblemonBlocks.SACCHARINE_LOG.defaultBlockState()
        for (y in 0 until trunkHeight) {
            val logPos = origin.relative(UP, y)
            worldGenLevel.setBlock(logPos, logState, UPDATE_CLIENTS)
        }

        var currentHeight = trunkHeight

        // Place patterns
        // Top Trunk Pattern
        placeTopTrunkPattern(worldGenLevel, origin.relative(UP, currentHeight), logState, potentialBeeNestPositions)
        currentHeight++

        // Leaf Start Pattern
        placeLeafStartPattern(
            worldGenLevel,
            origin.relative(UP, currentHeight),
            logState,
            CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState(),
            potentialBeeNestPositions,
            random
        )
        currentHeight++

        // Big Leaf Pattern
        placeBigLeafPattern(worldGenLevel, origin.relative(UP, currentHeight), logState, CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState())
        currentHeight++

        // Other patterns (small, big, topper)
        placeSmallLeafPattern(
            worldGenLevel,
            origin.relative(UP, currentHeight),
            logState,
            CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState(),
            Blocks.AIR.defaultBlockState(),
            random
        )
        currentHeight++

        // Big Leaf Pattern
        placeBigLeafPattern(worldGenLevel, origin.relative(UP, currentHeight), logState, CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState())
        currentHeight++

        // Random extension or Leaf Topper Pattern
        if (random.nextBoolean()) {
            placeSmallLeafPattern(
                worldGenLevel,
                origin.relative(UP, currentHeight),
                CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, UPDATE_CLIENTS),
                Blocks.AIR.defaultBlockState(),
                CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState(),
                random
            )
        } else {
            // Small Leaf Pattern
            placeSmallLeafPattern(
                worldGenLevel,
                origin.relative(UP, currentHeight),
                logState,
                CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState(),
                Blocks.AIR.defaultBlockState(),
                random
            )
            currentHeight++

            // Big Leaf Pattern
            placeBigLeafPattern(worldGenLevel, origin.relative(UP, currentHeight), logState, CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState())
            currentHeight++

            // Leaf Topper Pattern
            placeSmallLeafPattern(
                worldGenLevel,
                origin.relative(UP, currentHeight),
                CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, UPDATE_CLIENTS),
                Blocks.AIR.defaultBlockState(),
                CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState(),
                random
            )
        }

        // Place bee nest logic
        if (isGenerating) {
            // world gen
            if (worldGenLevel.random.nextInt(2) == 0) {
                placeBeeNest(worldGenLevel, potentialBeeNestPositions)
            }
        } else if (isFlowerNearby(worldGenLevel, origin) && worldGenLevel.random.nextInt(10) == 0) {
            // post world gen
            placeBeeNest(worldGenLevel, potentialBeeNestPositions)
        }

        return true
    }

    /**
     * A function to filter possible bee nest positions [locationsToCheck] to remove locations with a non-empty block to the south.
     * @return A sub-set of bee nest positions with an empty block to the south (z + 1).
     */
    private fun filterBeeNestPositions(worldGenLevel: WorldGenLevel, locationsToCheck: List<BlockPos>): List<BlockPos> {
        // A sub-set of viable bee nest locations
        val viableBeeNestPositions: MutableList<BlockPos> = mutableListOf()
        for (pos in locationsToCheck) {
            // Block that is south of possible bee nest
            val southBlock: BlockPos = pos.offset(0, 0, 1)

            // Check for if the southern block is empty.
            if (worldGenLevel.isEmptyBlock(southBlock)) {
                viableBeeNestPositions.add(pos)
            }
        }

        return viableBeeNestPositions
    }

    private fun isFlowerNearby(worldGenLevel: WorldGenLevel, origin: BlockPos): Boolean {
        for (dx in -2..2) {
            for (dy in -2..2) {
                for (dz in -2..2) {
                    val pos = origin.offset(dx, dy, dz)
                    if (worldGenLevel.getBlockState(pos).`is`(BlockTags.FLOWERS)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun placeBeeNest(worldGenLevel: WorldGenLevel, potentialBeeNestPositions: MutableList<BlockPos>) {
        val viableBeeNestPositions: List<BlockPos> = filterBeeNestPositions(worldGenLevel, potentialBeeNestPositions)
        val nestPos = viableBeeNestPositions.randomOrNull()

         if (nestPos != null && worldGenLevel.isEmptyBlock(nestPos)) {
            // Natural Generation
            val southFacingBeeNest = Blocks.BEE_NEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
            setBlockIfClear(worldGenLevel, nestPos, southFacingBeeNest)
            populateBeeNest(worldGenLevel, nestPos)
        }
    }

    private fun populateBeeNest(worldGenLevel: WorldGenLevel, pos: BlockPos) {
        worldGenLevel.getBlockEntity(pos, BlockEntityType.BEEHIVE)
            .ifPresent(
                Consumer { beehiveBlockEntity: BeehiveBlockEntity? ->
                    val randomSource: RandomSource = worldGenLevel.random
                    val isCombee = randomSource.nextInt(2)
                    val i: Int = 2 + randomSource.nextInt(2)
                    for (j in 0..<i) {
                        if (isCombee > 0) {
                            beehiveBlockEntity?.storeBee(BeehiveBlockEntity.Occupant.create(randomSource.nextInt(599)))
                        } else {
                            // We want to create a Pokemon entity here and turn it into a compound tag to store in the hive.
                            // But creating a Pokemon entity during world generation can cause issues.
                            // So instead we create custom compound tag that is a placeholder for an incomplete Pokemon.
                            // This is caught and handled by BeeOccupantMixin.java, which turns it into a complete entity.
                            val properties = "${POKEMON_ARGS} lvl=${LEVEL_RANGE.random()}"
                            val compoundTag = CompoundTag()
                            compoundTag.putString("id", POKEMON_PLACEHOLD_ID)
                            compoundTag.putString("pokemonProperties", properties)
                            val occupant = BeehiveBlockEntity.Occupant(
                                CustomData.of(compoundTag), randomSource.nextInt(599), 600
                            )
                            beehiveBlockEntity?.storeBee(occupant)
                        }
                    }
                })
    }

    private fun placeBigLeafPattern(worldGenLevel: WorldGenLevel, origin: BlockPos, logBlock: BlockState, leafBlock: BlockState) {
        val positions = listOf(
            origin.offset(-2, 0, 0),
            origin.offset(2, 0, 0),
            origin.offset(0, 0, -2),
            origin.offset(0, 0, 2),
            origin.offset(-1, 0, -2),
            origin.offset(1, 0, -2),
            origin.offset(-1, 0, 2),
            origin.offset(1, 0, 2),
            origin.offset(-2, 0, -1),
            origin.offset(-2, 0, 1),
            origin.offset(2, 0, -1),
            origin.offset(2, 0, 1),
            origin.offset(-1, 0, -1),
            origin.offset(1, 0, 1),
            origin.offset(-1, 0, 1),
            origin.offset(1, 0, -1),
            origin.offset(0, 0, 0), // Add center position
            origin.offset(1, 0, 0),
            origin.offset(-1, 0, 0),
            origin.offset(0, 0, 1),
            origin.offset(0, 0, -1),
            origin.offset(0, 0, 0)
        )

        for (pos in positions) {
            setBlockIfClear(worldGenLevel, pos, leafBlock.setValue(LeavesBlock.DISTANCE, UPDATE_CLIENTS))
        }

        // Center trunk
        worldGenLevel.setBlock(origin, logBlock, UPDATE_CLIENTS)
    }

    private fun placeSmallLeafPattern(worldGenLevel: WorldGenLevel, origin: BlockPos, logBlock: BlockState, leafBlock: BlockState, specialBlock: BlockState, random: RandomSource) {
        val positions = listOf(
            origin.offset(-1, 0, 0),
            origin.offset(1, 0, 0),
            origin.offset(0, 0, -1),
            origin.offset(0, 0, 1)
        )

        for (pos in positions) {
            setBlockIfClear(worldGenLevel, pos, CobblemonBlocks.SACCHARINE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, UPDATE_CLIENTS))
        }

        val specialPositions = listOf(
            origin.offset(-1, 0, -1),
            origin.offset(1, 0, 1),
            origin.offset(-1, 0, 1),
            origin.offset(1, 0, -1)
        )

        for (pos in specialPositions) {
            if (random.nextFloat() < 0.25f) {
                setBlockIfClear(worldGenLevel, pos, specialBlock)
            } else if (leafBlock.block != Blocks.AIR) {
                setBlockIfClear(worldGenLevel, pos, leafBlock.setValue(LeavesBlock.DISTANCE, UPDATE_CLIENTS))
            }
        }

        // Center trunk
        worldGenLevel.setBlock(origin, logBlock, UPDATE_CLIENTS)
    }

    private fun placeLeafStartPattern(worldGenLevel: WorldGenLevel, origin: BlockPos, logBlock: BlockState, leafBlock: BlockState, potentialBeeNestPositions: MutableList<BlockPos>, random: RandomSource) {
        val positions = listOf(
            origin.offset(-1, 0, 0),
            origin.offset(1, 0, 0),
            origin.offset(0, 0, -1),
            origin.offset(0, 0, 1),
            origin.offset(-1, 0, -1),
            origin.offset(1, 0, 1),
            origin.offset(-1, 0, 1),
            origin.offset(1, 0, -1)
        )

        for (pos in positions) {
            if (random.nextFloat() < 0.25F) {
                potentialBeeNestPositions.add(pos)
            } else {
                setBlockIfClear(worldGenLevel, pos, leafBlock.setValue(LeavesBlock.DISTANCE, UPDATE_CLIENTS))
            }
        }

        // Center trunk
        worldGenLevel.setBlock(origin, logBlock, UPDATE_CLIENTS)
    }

    private fun placeTopTrunkPattern(worldGenLevel: WorldGenLevel, origin: BlockPos, logBlock: BlockState, potentialBeeNestPositions: MutableList<BlockPos>) {
        val positions = listOf(
            origin.offset(-1, 0, 0),
            origin.offset(1, 0, 0),
            origin.offset(0, 0, -1),
            origin.offset(0, 0, 1)
        )

        potentialBeeNestPositions.addAll(positions)

        // Center trunk
        worldGenLevel.setBlock(origin, logBlock, UPDATE_CLIENTS)
    }

    private fun setBlockIfClear(worldGenLevel: WorldGenLevel, blockPos: BlockPos, blockState: BlockState) {
        if (!TreeFeature.isAirOrLeaves(worldGenLevel, blockPos)) {
            return
        }
        worldGenLevel.setBlock(blockPos, blockState, UPDATE_ALL)
    }

    companion object {
        val POKEMON_ARGS = "combee"
        val LEVEL_RANGE = 5..15
        val POKEMON_PLACEHOLD_ID = "cobblemon_pokemon_entity_placeholder"
    }
}

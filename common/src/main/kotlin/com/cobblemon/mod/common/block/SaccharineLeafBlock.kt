/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.entity.MoLangScriptingEntity
import com.cobblemon.mod.common.util.isServerSide
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.EntityCollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

class SaccharineLeafBlock(settings: Properties) : LeavesBlock(settings) {
    companion object {
        val AGE: IntegerProperty = BlockStateProperties.AGE_2
        val DISTANCE: IntegerProperty = BlockStateProperties.DISTANCE
        val PERSISTENT: BooleanProperty = BlockStateProperties.PERSISTENT
        const val MAX_AGE = 2
        const val MIN_AGE = 0
        const val DISTANCE_MAX = 7

        fun createBehavior(item: Item): DispenseItemBehavior {
            return DispenseItemBehavior { source, stack ->
                val level = source.level
                val facing = source.state.getValue(DispenserBlock.FACING)
                val pos = source.pos.relative(facing)
                val blockState = level.getBlockState(pos)

                if (blockState.block is SaccharineLeafBlock) {
                    val currentAge = blockState.getValue(AGE)

                    val waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER).item

                    val newAge = when (item) {
                        Items.HONEY_BOTTLE if currentAge < MAX_AGE ->
                            (currentAge + 2).coerceAtMost(MAX_AGE)

                        waterBottle if currentAge > MIN_AGE ->
                            (currentAge - 2).coerceAtLeast(MIN_AGE)

                        else -> currentAge
                    }

                    if (newAge != currentAge) {
                        level.setBlock(pos, blockState.setValue(AGE, newAge), 3)
                        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos)
                        stack.shrink(1)

                        val dispenserEntity = source.blockEntity
                        val outputItem = Items.GLASS_BOTTLE
                        val outputStack = ItemStack(outputItem)
                        var added = false

                        if (dispenserEntity != null) {
                            for (i in 0 until dispenserEntity.containerSize) {
                                val slotStack = dispenserEntity.getItem(i)

                                if (slotStack.isEmpty) {
                                    dispenserEntity.setItem(i, outputStack.copy())
                                    added = true
                                    break
                                } else if (slotStack.`is`(outputItem) && slotStack.count < slotStack.maxStackSize) {
                                    slotStack.grow(1)
                                    added = true
                                    break
                                }
                            }
                        }

                        if (!added) Containers.dropItemStack(
                            level,
                            source.pos.x.toDouble(),
                            source.pos.y.toDouble(),
                            source.pos.z.toDouble(),
                            outputStack
                        )
                    }
                }
                stack
            }
        }
    }

    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(AGE, MIN_AGE)
                .setValue(DISTANCE, DISTANCE_MAX)
                .setValue(PERSISTENT, false)
                .setValue(WATERLOGGED, false)
        )
    }

    override fun getCollisionShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape? {

        if (context is EntityCollisionContext) {
            val entity = context.entity
            if (entity != null
                && entity is MoLangScriptingEntity
                && entity.config.map.getOrDefault("can_path_through_sacc_leaves", DoubleValue.ZERO).asDouble() == 1.0
            ) {
                return Shapes.empty()
            }
        }
        return super.getCollisionShape(state, level, pos, context)
    }

    override fun isRandomlyTicking(state: BlockState): Boolean =
        true //!state.getValue(PERSISTENT) && state.getValue(DISTANCE) > 6

    override fun randomTick(state: BlockState, world: ServerLevel, pos: BlockPos, random: RandomSource) {
        val currentAge = state.getValue(AGE)

        if (currentAge > MIN_AGE && random.nextInt(2) == 0) {
            for (i in 1..10) {
                val belowPos = pos.below(i)
                val belowState = world.getBlockState(belowPos)

                if (belowState.isAir) {
                    continue
                } else if (belowState.block is SaccharineLeafBlock) {
                    val belowAge = belowState.getValue(AGE)
                    if (belowAge < MAX_AGE) {
                        world.setBlock(pos, changeAge(state, -1), UPDATE_CLIENTS)
                        world.setBlock(belowPos, changeAge(belowState, 1), UPDATE_CLIENTS)
                    }
                    break
                } else {
                    break
                }
            }
        }

        super.randomTick(state, world, pos, random)
    }

    override fun placeLiquid(level: LevelAccessor, pos: BlockPos, state: BlockState, fluidState: FluidState): Boolean {
        if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.type === Fluids.WATER) {
            val hasHoney = state.getValue(AGE) > 0
            if (!level.isClientSide) {
                val newState = state.setValue(BlockStateProperties.WATERLOGGED, true)
                level.setBlock(pos, if (hasHoney) newState.setValue(AGE, 0) else newState, 3)
                level.scheduleTick(pos, fluidState.type, fluidState.type.getTickDelay(level))
            }
            if (hasHoney) spawnDestroyHoneyParticles(level as Level, pos, state)
            return true
        } else {
            return false
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(AGE)
        super.createBlockStateDefinition(builder)
    }

    @Deprecated("Deprecated in Java")
    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {

        val particleCount = random.nextInt(3)

        if (state.getValue(AGE) == 1) {
            repeat(particleCount) {
                spawnHoneyParticles(level, pos, state, 0.025F)
            }
        } else if (state.getValue(AGE) == 2) {
            repeat(particleCount) {
                spawnHoneyParticles(level, pos, state, 0.05F)
            }
        }
    }

    private fun spawnHoneyParticles(level: Level, pos: BlockPos, state: BlockState, rate: Float) {
        if (state.fluidState.isEmpty && (level.random.nextFloat() < rate)) {
            val voxelShape = state.getCollisionShape(level, pos)
            val d = voxelShape.max(Direction.Axis.Y)
            if (d >= 1.0 && !state.`is`(BlockTags.IMPERMEABLE)) {
                val e = voxelShape.min(Direction.Axis.Y)
                if (e > 0.0) {
                    this.addHoneyParticle(level, pos, voxelShape, pos.y.toDouble() + e - 0.05)
                } else {
                    val blockPos = pos.below()
                    val blockState = level.getBlockState(blockPos)
                    val voxelShape2 = blockState.getCollisionShape(level, blockPos)
                    val f = voxelShape2.max(Direction.Axis.Y)
                    if ((f < 1.0 || !blockState.isSolidRender(level, blockPos)) && blockState.fluidState.isEmpty) {
                        this.addHoneyParticle(level, pos, voxelShape, pos.y.toDouble() - 0.05)
                    }
                }
            }
        }
    }

    private fun addHoneyParticle(level: Level, pos: BlockPos, shape: VoxelShape, height: Double) {
        this.addHoneyParticle(
            level, pos.x.toDouble() + shape.min(Direction.Axis.X), pos.x.toDouble() + shape.max(
                Direction.Axis.X
            ), pos.z.toDouble() + shape.min(Direction.Axis.Z), pos.z.toDouble() + shape.max(
                Direction.Axis.Z
            ), height
        )
    }

    private fun addHoneyParticle(level: Level, minX: Double, maxX: Double, minZ: Double, maxZ: Double, height: Double) {
        level.addParticle(
            ParticleTypes.DRIPPING_HONEY,
            Mth.lerp(level.random.nextDouble(), minX, maxX),
            height,
            Mth.lerp(level.random.nextDouble(), minZ, maxZ),
            0.0,
            0.0,
            0.0
        )
    }

    override fun isPathfindable(state: BlockState, type: PathComputationType): Boolean = false

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): ItemInteractionResult {
        if (!state.getValue(WATERLOGGED)) {
            val itemStack = player.getItemInHand(hand)
            val isGlassBottle = itemStack.`is`(Items.GLASS_BOTTLE)
            val isHoneyBottle = itemStack.`is`(Items.HONEY_BOTTLE)

            if (isGlassBottle && isAtMaxAge(state)) {
                if (level.isClientSide) return ItemInteractionResult.SUCCESS
                // Decrement stack if not in creative mode
                itemStack.consume(1, player)

                // Give player a honey bottle
                if (stack.isEmpty) {
                    // Replace the consumed empty bottle with a honey bottle
                    player.setItemInHand(hand, ItemStack(Items.HONEY_BOTTLE));
                } else {
                    player.addItem(Items.HONEY_BOTTLE.defaultInstance)
                }

                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS)
                level.setBlock(pos, state.setValue(AGE, 0), UPDATE_CLIENTS)
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos)
                return ItemInteractionResult.SUCCESS
            } else if (isHoneyBottle && !isAtMaxAge(state)) {
                if (level.isClientSide) return ItemInteractionResult.SUCCESS

                // Decrement stack if not in creative mode
                if (!player.isCreative) {
                    itemStack.consume(1, player)
                    if (stack.isEmpty) {
                        // Replace the consumed honey bottle with an empty bottle
                        player.setItemInHand(hand, ItemStack(Items.GLASS_BOTTLE));
                    } else {
                        player.addItem(Items.GLASS_BOTTLE.defaultInstance)
                    }
                }

                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS)
                level.setBlock(pos, state.setValue(AGE, 2), UPDATE_CLIENTS)
                level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos)
                return ItemInteractionResult.SUCCESS
            }
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hit)
    }

    private fun isAtMaxAge(state: BlockState) = state.getValue(AGE) == MAX_AGE

    private fun isAtMinAge(state: BlockState) = state.getValue(AGE) == MIN_AGE

    private fun changeAge(state: BlockState, value: Int): BlockState {
        val newAge = (state.getValue(AGE) + value).coerceIn(MIN_AGE, MAX_AGE)
        return state.setValue(AGE, newAge)
    }

    private fun spawnDestroyHoneyParticles(level: Level, pos: BlockPos, state: BlockState) {
        if (!isAtMinAge(state)) {
            var amount = if (isAtMaxAge(state)) 30 else 10
            for (i in 0 until amount) {
                val offsetX: Double = (level.random.nextDouble() * 1.2) - 0.6
                val offsetY: Double = (level.random.nextDouble() * 1.2) - 0.6
                val offsetZ: Double = (level.random.nextDouble() * 1.2) - 0.6

                level.addParticle(
                    ParticleTypes.FALLING_HONEY,
                    pos.x + 0.5 + offsetX,
                    pos.y + 0.5 + offsetY,
                    pos.z + 0.5 + offsetZ,
                    0.0, 1.0, 0.0
                )
            }
        }
    }

    override fun spawnDestroyParticles(level: Level, player: Player, pos: BlockPos, state: BlockState) {
        spawnDestroyHoneyParticles(level, pos, state)
        super.spawnDestroyParticles(level, player, pos, state.setValue(AGE, state.getValue(AGE)))
    }
}

/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.ai

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.entity.OmniPathingEntity
import com.cobblemon.mod.common.entity.pokemon.ai.PokemonMoveControl
import com.cobblemon.mod.common.util.canFit
import com.google.common.collect.Maps
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import java.util.EnumSet
import java.util.function.Predicate
import kotlin.math.max
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.PathNavigationRegion
import net.minecraft.world.level.block.BaseRailBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.pathfinder.Node
import net.minecraft.world.level.pathfinder.NodeEvaluator
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.level.pathfinder.PathType
import net.minecraft.world.level.pathfinder.PathfindingContext
import net.minecraft.world.level.pathfinder.Target
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3


/**
 * A path node maker that constructs paths knowing that the entity might be capable of
 * traveling across land, water, and air. This most closely resembles the aquatic
 * node maker.
 *
 * @author Hiroku
 * @since September 10th, 2022
 */
class OmniPathNodeMaker : NodeEvaluator() {
    private val nodePosToType: Long2ObjectMap<PathType> = Long2ObjectOpenHashMap()
    private val nodePosToState: Long2ObjectMap<BlockState> = Long2ObjectOpenHashMap()

    var canPathThroughFire: Boolean = false

    var nodeFilter: Predicate<PathType> = Predicate { true }

    override fun prepare(cachedWorld: PathNavigationRegion, entity: Mob) {
        super.prepare(cachedWorld, entity)
        nodePosToType.clear()
        nodePosToState.clear()
    }

    override fun done() {
        super.done()
        nodePosToType.clear()
        nodePosToState.clear()
    }

    fun PathfindingContext.getCachedBlockState(pos: BlockPos): BlockState {
        return nodePosToState.computeIfAbsent(pos.asLong()) { getBlockState(pos) }
    }

    override fun getTarget(x: Double, y: Double, z: Double): Target {
        return Target(super.getNode(Mth.floor(x), Mth.floor(y + 0.5), Mth.floor(z)))
    }

    override fun getStart(): Node {
        val x = Mth.floor(mob.boundingBox.minX)
        val y = Mth.floor(mob.boundingBox.minY + 0.5)
        val z = Mth.floor(mob.boundingBox.minZ)
        val node = super.getNode(x, y, z)
        node.type = this.getNodeType(mob, node.asBlockPos())
        node.costMalus = mob.getPathfindingMalus(node.type)
        return node
    }

    fun getNodeType(entity: Mob, pos: BlockPos): PathType {
        return this.getNodeType(entity, pos.x, pos.y, pos.z)
    }

    fun getNodeType(entity: Mob, x: Int, y: Int, z: Int): PathType {
        return this.nodePosToType.computeIfAbsent(
            BlockPos.asLong(x, y, z),
            Long2ObjectFunction<PathType?> { this.getPathTypeOfMob(currentContext, x, y, z, entity) }
        )
    }

    fun doesBlockHavePartialCollision(pathType: PathType): Boolean {
        return pathType == PathType.FENCE || pathType == PathType.DOOR_WOOD_CLOSED || pathType == PathType.DOOR_IRON_CLOSED
    }

    private fun getMobJumpHeight(): Double {
        return max(1.125, mob.maxUpStep().toDouble())
    }

    protected fun findAcceptedNodeWalk(
        x: Int,
        y: Int,
        z: Int,
        verticalDeltaLimit: Int,
        nodeFloorLevel: Double,
        direction: Direction?,
        pathType: PathType?
    ): Node? {
        var node: Node? = null
        val mutableBlockPos = BlockPos.MutableBlockPos()
        val d: Double = this.getFloorLevel(mutableBlockPos.set(x, y, z))
        return if (!canFly() && d - nodeFloorLevel > this.getMobJumpHeight()) {
            null
        } else {
            var pathType2: PathType = this.getNodeType(this.mob, x, y, z)

            if (pathType2 == PathType.WALKABLE && direction == Direction.UP) {
                pathType2 = PathType.OPEN
            }

            val f = mob.getPathfindingMalus(pathType2)
            if (f >= 0.0f) {
                node = this.getNodeAndUpdateCostToMax(x, y, z, pathType2, f)
            }
            if (pathType?.let { doesBlockHavePartialCollision(it) } == true && node != null && node.costMalus >= 0.0f && !this.canReachWithoutCollision(
                    node
                )) {
                node = null
            }
            if (pathType2 != PathType.WALKABLE) {
                if ((node == null || node.costMalus < 0.0f) && verticalDeltaLimit > 0 && (pathType2 != PathType.FENCE || canWalkOverFences()) && pathType2 != PathType.UNPASSABLE_RAIL && pathType2 != PathType.TRAPDOOR && pathType2 != PathType.POWDER_SNOW) {
                    node = this.tryJumpOn(
                        x,
                        y,
                        z,
                        verticalDeltaLimit,
                        nodeFloorLevel,
                        direction!!,
                        pathType!!,
                        mutableBlockPos
                    )
                } else if (pathType2 == PathType.WATER && !canFloat()) {
                    node = this.tryFindFirstNonWaterBelow(x, y, z, node)
                } else if (pathType2 == PathType.OPEN) {
                    node = this.tryFindFirstGroundNodeBelow(x, y, z)
                } else if (doesBlockHavePartialCollision(pathType2) && node == null) {
                    node = this.getClosedNode(x, y, z, pathType2)
                }
                node
            } else {
                node
            }
        }
    }

    private fun getBlockedNode(x: Int, y: Int, z: Int): Node? {
        val node = this.getNode(x, y, z) ?: return null
        node.type = PathType.BLOCKED
        node.costMalus = -1.0f
        return node
    }


    private fun canReachWithoutCollision(node: Node): Boolean {
        var aABB = mob.boundingBox
        var vec3 = Vec3(
            node.x.toDouble() - mob.x + aABB.xsize / 2.0,
            node.y.toDouble() - mob.y + aABB.ysize / 2.0,
            node.z.toDouble() - mob.z + aABB.zsize / 2.0
        )
        val i = Mth.ceil(vec3.length() / aABB.size)
        vec3 = vec3.scale((1.0f / i.toFloat()).toDouble())
        for (j in 1..i) {
            aABB = aABB.move(vec3)
            if (hasCollisions(aABB)) {
                return false
            }
        }
        return true
    }

    private fun tryFindFirstGroundNodeBelow(x: Int, y: Int, z: Int): Node? {
        for (i in y - 1 downTo mob.level().minBuildHeight) {
            if (y - i > mob.maxFallDistance) {
                return this.getBlockedNode(x, i, z)
            }
            val pathType: PathType = this.getNodeType(this.mob, x, i, z)
            val f = mob.getPathfindingMalus(pathType)
            if (pathType != PathType.OPEN) {
                return if (f >= 0.0f) {
                    getNodeAndUpdateCostToMax(x, i, z, pathType, f)
                } else this.getBlockedNode(x, i, z)
            }
        }
        return this.getBlockedNode(x, y, z)
    }

    private fun tryFindFirstNonWaterBelow(x: Int, y: Int, z: Int, node: Node?): Node? {
        var y = y
        var node = node
        --y
        while (y > mob.level().minBuildHeight) {
            val pathType: PathType = this.getNodeType(this.mob, x, y, z)
            if (pathType != PathType.WATER) {
                return node
            }
            node = this.getNodeAndUpdateCostToMax(x, y, z, pathType, mob.getPathfindingMalus(pathType))
            --y
        }
        return node
    }

    private fun getNodeAndUpdateCostToMax(x: Int, y: Int, z: Int, pathType: PathType, malus: Float): Node? {
        val node = this.getNode(x, y, z) ?: super.getNode(x, y, z) ?: return null
        node.type = pathType
        node.costMalus = max(node.costMalus.toDouble(), malus.toDouble()).toFloat()
        return node
    }

    private fun tryJumpOn(
        x: Int,
        y: Int,
        z: Int,
        verticalDeltaLimit: Int,
        nodeFloorLevel: Double,
        direction: Direction,
        pathType: PathType,
        pos: BlockPos.MutableBlockPos
    ): Node? {
        val node = findAcceptedNodeWalk(x, y + 1, z, verticalDeltaLimit - 1, nodeFloorLevel, direction, pathType)
        return if (node == null) {
            null
        } else if (mob.bbWidth >= 1.0f) {
            node
        } else if (node.type != PathType.OPEN && node.type != PathType.WALKABLE) {
            node
        } else {
            val d = (x - direction.stepX).toDouble() + 0.5
            val e = (z - direction.stepZ).toDouble() + 0.5
            val f = mob.bbWidth.toDouble() / 2.0
            val aABB = AABB(
                d - f,
                this.getFloorLevel(pos.set(d, (y + 1).toDouble(), e)) + 0.001,
                e - f,
                d + f,
                mob.bbHeight.toDouble() + this.getFloorLevel(
                    pos.set(
                        node.x.toDouble(),
                        node.y.toDouble(),
                        node.z.toDouble()
                    )
                ) - 0.002,
                e + f
            )
            if (this.hasCollisions(aABB)) null else node
        }
    }

    private fun getClosedNode(x: Int, y: Int, z: Int, pathType: PathType): Node? {
        val node = this.getNode(x, y, z) ?: return null
        node.closed = true
        node.type = pathType
        node.costMalus = pathType.malus
        return node
    }

    private fun hasCollisions(boundingBox: AABB): Boolean {
        return !currentContext.level().noCollision(mob, boundingBox)
    }

    // Borrowed from WalkNodeEvaluator, specifically here for things that don't use the
    // PokemonMoveControl such as NPCs, to prevent them from being caught on upward diagonals
    private fun isDiagonalValidForNonPokemon(root: Node, xNode: Node?, zNode: Node?): Boolean {
        if (this.mob.moveControl is PokemonMoveControl) {
            return true
        }
        if (zNode != null && xNode != null && zNode.y <= root.y && xNode.y <= root.y) {
            if (xNode.type != PathType.WALKABLE_DOOR && zNode.type != PathType.WALKABLE_DOOR) {
                val bl = zNode.type == PathType.FENCE && xNode.type == PathType.FENCE && this.mob.bbWidth
                    .toDouble() < 0.5
                return (zNode.y < root.y || zNode.costMalus >= 0.0f || bl) && (xNode.y < root.y || xNode.costMalus >= 0.0f || bl)
            } else {
                return false
            }
        } else {
            return false
        }
    }

    protected fun getFloorLevel(pos: BlockPos): Double {
        val blockGetter: BlockGetter = currentContext.level()
        if ((canFloat()) && blockGetter.getFluidState(pos).`is`(FluidTags.WATER)) {
            return pos.y.toDouble() + 0.5
        }
        return if ((canFloat()) && blockGetter.getFluidState(pos)
                .`is`(FluidTags.WATER)
        ) pos.y.toDouble() + 0.5 else WalkNodeEvaluator.getFloorLevel(blockGetter, pos)
    }

    override fun getNeighbors(successors: Array<Node?>, node: Node): Int {
        var i = 0
        val map = Maps.newEnumMap<Direction, Node?>(Direction::class.java)
        val upperMap = Maps.newEnumMap<Direction, Node?>(Direction::class.java)
        val lowerMap = Maps.newEnumMap<Direction, Node?>(Direction::class.java)

        val d = getFloorLevel(BlockPos(node.x, node.y, node.z))

        // Hitbox thing looks confusing but if the hitbox volume is more than like, 5, it starts getting pretty
        // fucking slow to use the findAcceptedNodeWalk function
        val strictlyWalkPathing = !canFly() && !mob.isInWater && mob.boundingBox.size < MAX_HITBOX_SIZE_FOR_WALKING
        val doVerticalNeighbourChecks = (canFly() || mob.isInWater) && mob.boundingBox.size < MAX_HITBOX_FOR_VERTICAL_NEIGHBOURS

        // Non-diagonal surroundings in 3d space
        for (direction in Direction.entries) {
            var pathNode: Node?
            if (!strictlyWalkPathing) {
                pathNode = this.getNode(node.x + direction.stepX, node.y + direction.stepY, node.z + direction.stepZ)
                    ?: continue
            } else {
                pathNode = findAcceptedNodeWalk(
                    node.x + direction.stepX,
                    node.y + direction.stepY,
                    node.z + direction.stepZ,
                    if (direction == Direction.DOWN || direction == Direction.UP) 0 else 1,
                    d,
                    direction,
                    node.type
                ) ?: continue
            }
            map[direction] = pathNode
            if (!hasNotVisited(pathNode, node)) {
                continue
            }
            successors[i++] = pathNode
        }

        // Diagonals
        for (direction in Direction.Plane.HORIZONTAL.iterator()) {
            val direction2 = direction.clockWise
            val x = node.x + direction.stepX + direction2.stepX
            val z = node.z + direction.stepZ + direction2.stepZ
            var pathNode2: Node?
            if (!strictlyWalkPathing) {
                pathNode2 = this.getNode(x, node.y, z) ?: continue
            } else {
                pathNode2 = findAcceptedNodeWalk(
                    x,
                    node.y,
                    z,
                    if (direction == Direction.DOWN || direction == Direction.UP) 0 else 1,
                    d,
                    direction,
                    node.type
                ) ?: continue
            }
            // Skip 'inaccessible' diagonals if we're pathing from a blocked node since we're trying to get unstuck
            if (isAccessibleDiagonal(pathNode2, map[direction], map[direction2]) || (node.type == PathType.BLOCKED && !pathNode2.closed)) {
                if (!isDiagonalValidForNonPokemon(node, map[direction], map[direction2]))
                    continue
                successors[i++] = pathNode2
            }
        }
        if (doVerticalNeighbourChecks) {
            val upIsOpen = mob.canFit(node.asBlockPos().above())

            // Upward non-diagonals
            for (direction in Direction.Plane.HORIZONTAL.iterator()) {
                var pathNode2: Node? = null
                pathNode2 = getNode(node.x + direction.stepX, node.y + 1, node.z + direction.stepZ) ?: continue

                if (upIsOpen && hasNotVisited(pathNode2, node)) {
                    successors[i++] = pathNode2
                    upperMap[direction] = pathNode2
                }
            }

            // Upward diagonals
            for (direction in Direction.Plane.HORIZONTAL.iterator()) {
                val direction2 = direction.clockWise
                var pathNode2: Node? = null
                pathNode2 = getNode(
                    node.x + direction.stepX + direction2.stepX,
                    node.y + 1,
                    node.z + direction.stepZ + direction2.stepZ
                ) ?: continue

                if (isAccessibleDiagonal(pathNode2, upperMap[direction], upperMap[direction2])) {
                    successors[i++] = pathNode2
                }
            }

            val connectingBlockPos = BlockPos.MutableBlockPos()
            // Downward non-diagonals
            for (direction in Direction.Plane.HORIZONTAL.iterator()) {
                connectingBlockPos.set(node.asBlockPos().offset(direction.normal))
                val blockState = currentContext.getCachedBlockState(connectingBlockPos)
                val traversableByTangent = blockState.isPathfindable(PathComputationType.AIR)
                val pathNode2 = getNode(node.x + direction.stepX, node.y - 1, node.z + direction.stepZ) ?: continue
                if (hasNotVisited(pathNode2, node) && traversableByTangent) {
                    successors[i++] = pathNode2
                    lowerMap[direction] = pathNode2
                }
            }

            // Downward diagonals
            for (direction in Direction.Plane.HORIZONTAL.iterator()) {
                val direction2 = direction.clockWise
                val pathNode2 = getNode(
                    node.x + direction.stepX + direction2.stepX,
                    node.y - 1,
                    node.z + direction.stepZ + direction2.stepZ
                ) ?: continue
                if (isAccessibleDiagonal(pathNode2, lowerMap[direction], lowerMap[direction2])) {
                    successors[i++] = pathNode2
                }
            }
        }

        // If they're in a blocked node and there are multiple successors, choose whichever is closest to get out of the blocked position.
        // This addresses instances where they're next to a fence and should move away from the fence in the nearest open
        // direction before regular pathing.
        if (mob.getPathfindingMalus(node.type) < 0 && i > 1) {
            val x = mob.boundingBox.minX
            val y = mob.boundingBox.minY + 0.5
            val z = mob.boundingBox.minZ
            val pos = Vec3(x, y, z)

            var n = 1
            var closestSuccessor = successors[0]!!
            var closestDistance = closestSuccessor.asVec3().add(0.5, 0.0, 0.5).distanceTo(pos)

            while (n < i) {
                val next = successors[n]!!
                val nextDist = next.asVec3().add(0.5, 0.0, 0.5).distanceTo(pos)
                if (nextDist < closestDistance) {
                    closestSuccessor = next
                    closestDistance = nextDist
                }

                n++
            }

            successors[0] = closestSuccessor
            i = 1
        }

        return i
    }

    fun hasNotVisited(neighborNode: Node?, pathNode: Node): Boolean {
        return neighborNode != null && !neighborNode.closed && (neighborNode.costMalus >= 0.0f || pathNode.costMalus < 0.0f)
    }

    fun isAccessibleDiagonal(pathNode: Node, vararg borderNodes: Node?): Boolean {
        return borderNodes.all {
            it != null && hasNotVisited(
                pathNode,
                it
            )
        } && borderNodes.all { it != null && it.costMalus >= 0.0F }
    }

    fun isValidPathType(type: PathType): Boolean {
        if (!nodeFilter.test(type)) {
            return false
        }

        return when {
            (type == PathType.BREACH || type == PathType.WATER || type == PathType.WATER_BORDER) && canSwimInWater() -> true
            type == PathType.LAVA && canSwimInLava() -> true
            type == PathType.OPEN && canFly() -> true
            type == PathType.WALKABLE && (canWalk() || canFly()) -> true
            else -> false
        }
    }

    override fun getNode(x: Int, y: Int, z: Int): Node? {
        var nodePenalty = 0F
        var pathNode: Node? = null

        val type = addNodePos(x, y, z)
        if (isValidPathType(type) &&
            mob.getPathfindingMalus(type).also { nodePenalty = it } >= 0.0f &&
            super.getNode(x, y, z).also { pathNode = it } != null
        ) {
            pathNode!!.type = type
            pathNode!!.costMalus = pathNode!!.costMalus.coerceAtLeast(nodePenalty)
        }
        return pathNode
    }

    fun addNodePos(x: Int, y: Int, z: Int): PathType {
        return nodePosToType.computeIfAbsent(
            BlockPos.asLong(x, y, z),
            Long2ObjectFunction { getPathTypeOfMob(currentContext, x, y, z, mob) })
    }

    override fun getPathType(pfContext: PathfindingContext, x: Int, y: Int, z: Int): PathType {
        val pos = BlockPos(x, y, z)
        val below = BlockPos(x, y - 1, z)
        val blockState = pfContext.getCachedBlockState(pos)
        val blockStateBelow = pfContext.getCachedBlockState(below)
        val isFluid = !blockState.fluidState.isEmpty
        val isWater = isFluid && blockState.fluidState.`is`(FluidTags.WATER)
        val isLava = isFluid && !isWater && blockState.fluidState.`is`(FluidTags.LAVA)
        val canBreatheUnderFluid = isFluid && canSwimUnderFluid(blockState.fluidState)

        val belowSolid = blockStateBelow.isSolid
        val solid = blockState.isSolid

        /*
         * There are a lot of commented out pairs of checks here. I was experimenting with how to simultaneously
         * fix the following situations (without breaking any in the process):
         * - Walking up slabs
         * - Walking up stairs
         * - Lifting off from snow layers
         * - Lifting off from carpets.
         *
         * It seems to work now but nothing works forever so my other attempts are here for reference.
         */

//        var figuredNode = if (blockState.`is`(BlockTags.FENCES) || blockState.`is`(BlockTags.WALLS)) {
//            PathType.FENCE
//        } else if (blockStateBelow.block is FenceGateBlock) {
//            if (blockStateBelow.getValue(FenceGateBlock.OPEN)) {
//                PathType.OPEN
//            } else {
//                PathType.FENCE
//            }
//        } else if (isWater && !canSwimInWater() && canBreatheUnderFluid && blockState.isPathfindable(PathComputationType.LAND) ) {
//            if (pfContext.getPathTypeFromState(pos.x, pos.y - 1, pos.z) == PathType.BLOCKED) {
//                PathType.WALKABLE
//            } else {
//                PathType.OPEN
//            }
//        } else if (isLava && canSwimInLava()) {
//            PathType.LAVA
//        } else if (isWater) {
//            PathType.WATER
//            // This breaks lifting off from snow layers and carpets
////        } else if (blockState.canPathfindThrough(world, pos, NavigationType.LAND) && !blockStateBelow.canPathfindThrough(world, below, NavigationType.AIR)) {
////            PathType.WALKABLE
////        } else if (blockState.canPathfindThrough(world, pos, NavigationType.AIR) && blockStateBelow.canPathfindThrough(world, below, NavigationType.AIR)) {
////            PathType.OPEN
//        } else if (blockState.`is`(BlockTags.TRAPDOORS) || blockState.`is`(Blocks.LILY_PAD) || blockState.`is`(Blocks.BIG_DRIPLEAF)) {
//            PathType.TRAPDOOR
//        } else if (blockState.isPathfindable(PathComputationType.LAND)) {
//            PathType.OPEN
//            // This breaks walking up slabs
////        } else if (blockState.canPathfindThrough(world, pos, NavigationType.LAND) && blockStateBelow.isSideSolid(world, below, Direction.UP, SideShapeType.FULL)) {
////            PathType.WALKABLE
////        } else if (blockState.canPathfindThrough(world, pos, NavigationType.AIR) && !blockStateBelow.isSideSolid(world, below, Direction.UP, SideShapeType.FULL)) {
////            PathType.OPEN
        if (canPathThroughLeaves() && blockState.block == CobblemonBlocks.SACCHARINE_LEAVES)
            return PathType.OPEN
//        } else PathType.BLOCKED

        // This is the 1.6.1 logic but with some optimizations
        val figuredNode = if (belowSolid && (blockStateBelow.`is`(BlockTags.FENCES) || blockStateBelow.`is`(BlockTags.WALLS) || (blockStateBelow.block is FenceGateBlock && !blockStateBelow.getValue(FenceGateBlock.OPEN)))) {
            PathType.FENCE
        } else if (isWater && belowSolid && !canSwimInWater() && canBreatheUnderFluid) {
            PathType.WALKABLE
        } else if (isWater || (isLava && canSwimInLava())) {
            PathType.WATER
            // This breaks lifting off from snow layers and carpets
//        } else if (blockState.canPathfindThrough(world, pos, NavigationType.LAND) && !blockStateBelow.canPathfindThrough(world, below, NavigationType.AIR)) {
//            PathType.WALKABLE
//        } else if (blockState.canPathfindThrough(world, pos, NavigationType.AIR) && blockStateBelow.canPathfindThrough(world, below, NavigationType.AIR)) {
//            PathType.OPEN
        } else if (!solid && belowSolid) {
            PathType.WALKABLE
        } else if (!solid && !belowSolid) {
            PathType.OPEN
            // This breaks walking up slabs
//        } else if (blockState.canPathfindThrough(world, pos, NavigationType.LAND) && blockStateBelow.isSideSolid(world, below, Direction.UP, SideShapeType.FULL)) {
//            PathType.WALKABLE
//        } else if (blockState.canPathfindThrough(world, pos, NavigationType.AIR) && !blockStateBelow.isSideSolid(world, below, Direction.UP, SideShapeType.FULL)) {
//            PathType.OPEN
        } else PathType.BLOCKED

//        if (figuredNode == PathType.OPEN && pos.y >= pfContext.level().getMinBuildHeight() + 1) {
//            val var10000: PathType = when (getPathType(pfContext, pos.x, pos.y - 1, pos.z)) {
//                PathType.OPEN, PathType.LAVA, PathType.WALKABLE -> PathType.OPEN
//                PathType.DAMAGE_OTHER -> PathType.DAMAGE_OTHER
//                PathType.STICKY_HONEY -> PathType.STICKY_HONEY
//                PathType.POWDER_SNOW -> PathType.DANGER_POWDER_SNOW
//                PathType.DAMAGE_CAUTIOUS -> PathType.DAMAGE_CAUTIOUS
//                PathType.TRAPDOOR -> PathType.DANGER_TRAPDOOR
//                PathType.WATER -> if (canWalkOnWater()) PathType.WALKABLE else PathType.OPEN
//                PathType.FENCE -> {
//                    if (canFly())
//                        PathType.BLOCKED
//                    else {
//                        if (mob.boundingBox.size < MAX_HITBOX_SIZE_FOR_WALKING)
//                            WalkNodeEvaluator.checkNeighbourBlocks(pfContext, pos.x, pos.y, pos.z, PathType.WALKABLE)
//                        else
//                            PathType.WALKABLE
//                    }
//                }
//                else -> {
//                    if (mob.boundingBox.size < MAX_HITBOX_SIZE_FOR_WALKING)
//                        WalkNodeEvaluator.checkNeighbourBlocks(pfContext, pos.x, pos.y, pos.z, PathType.WALKABLE)
//                    else
//                        PathType.WALKABLE
//                }
//            }
//            figuredNode = var10000
//        }

        return adjustNodeType(pfContext, canOpenDoors, canPassDoors, below, figuredNode)
    }

    override fun getPathTypeOfMob(pfContext: PathfindingContext, x: Int, y: Int, z: Int, mob: Mob): PathType {
        val set = EnumSet.noneOf(PathType::class.java)
        val sizeX = (mob.boundingBox.maxX - mob.boundingBox.minX).toInt() + 1
        val sizeY = (mob.boundingBox.maxY - mob.boundingBox.minY).toInt() + 1
        val sizeZ = (mob.boundingBox.maxZ - mob.boundingBox.minZ).toInt() + 1

        // TODO -- This is experimental code to see if this is meaningful. We should clean this up later when time permits.
        val returnedEarlyPathType = mutableListOf<PathType>()
        val type = findNearbyNodeTypes(
            pfContext, x, y, z, sizeX, sizeY, sizeZ, canOpenDoors, canPassDoors, set, PathType.BLOCKED, BlockPos(x, y, z), returnedEarlyPathType
        )


        if (PathType.FENCE in set) {
            return PathType.FENCE
        }
        if (PathType.DAMAGE_CAUTIOUS in set) {
            return PathType.DAMAGE_CAUTIOUS
        } else if (PathType.DANGER_OTHER in set) {
            return PathType.DANGER_OTHER
        }

        return if (PathType.UNPASSABLE_RAIL in set) {
            PathType.UNPASSABLE_RAIL
        } else if (PathType.DAMAGE_OTHER in set) {
            PathType.DAMAGE_OTHER
        } else {
            val result = returnedEarlyPathType.firstOrNull()
            if (result != null) {
                return result
            }
            var pathType2: PathType = PathType.BLOCKED
            val nearbyTypeIterator = set.iterator()
            while (nearbyTypeIterator.hasNext()) {
                val nearbyType = nearbyTypeIterator.next()
                // The || is because we prefer WALKABLE where possible - OPEN is legit but if there's either OPEN or WALKABLE then WALKABLE is better since land pokes can read that.
                if (mob.getPathfindingMalus(nearbyType) > mob.getPathfindingMalus(pathType2) || nearbyType == PathType.WALKABLE) {
                    pathType2 = nearbyType
                } else if (type == PathType.WATER && nearbyType == PathType.WATER) {
                    pathType2 = PathType.WATER
                }
            }
            if (type == PathType.OPEN && mob.getPathfindingMalus(pathType2) == 0.0f && sizeX <= 1) {
                PathType.OPEN
            } else {
                pathType2
            }
        }
    }

    fun findNearbyNodeTypes(
        pfContext: PathfindingContext,
        x: Int,
        y: Int,
        z: Int,
        sizeX: Int,
        sizeY: Int,
        sizeZ: Int,
        canOpenDoors: Boolean,
        canEnterOpenDoors: Boolean,
        nearbyTypes: EnumSet<PathType>,
        type: PathType,
        pos: BlockPos,
        returnedEarlyPathType: MutableList<PathType>
    ): PathType {
        var type = type
        for (i in 0 until sizeX) {
            for (j in 0 until sizeY) {
                for (k in 0 until sizeZ) {
                    val l = i + x
                    val m = j + y
                    val n = k + z
                    val currentType = getPathType(pfContext, l, m, n)
                    if (i == 0 && j == 0 && k == 0) {
                        if (currentType != null) {
                            type = currentType
                        }
                    }
                    nearbyTypes.add(currentType)
                    when (currentType) {
                        PathType.FENCE -> return type
                        PathType.LEAVES -> return type
                        PathType.DAMAGE_CAUTIOUS -> return type
                        PathType.UNPASSABLE_RAIL -> return type
                        PathType.DAMAGE_OTHER -> return type
                        else -> {}
                    }
                    if (mob.getPathfindingMalus(currentType) < 0) {
                        returnedEarlyPathType.add(currentType)
                        return currentType
                    }
                }
            }
        }
        return type
    }

    protected fun adjustNodeType(
        pfContext: PathfindingContext,
        canOpenDoors: Boolean,
        canEnterOpenDoors: Boolean,
        pos: BlockPos,
        type: PathType
    ): PathType {
        val blockState = pfContext.getCachedBlockState(pos)

        if (blockState.isAir && type != PathType.RAIL) {
            return PathType.OPEN
        }

        val block = blockState.block

        if (blockState.`is`(Blocks.CACTUS) || blockState.`is`(Blocks.SWEET_BERRY_BUSH)) {
            return PathType.DANGER_OTHER
        }

        if (isBurningBlock(blockState) && !this.canPathThroughFire && !(blockState.`is`(Blocks.MAGMA_BLOCK) && !isOnGround())) {
            return PathType.DANGER_FIRE
        }

        if (blockState.`is`(Blocks.WITHER_ROSE) || blockState.`is`(Blocks.POINTED_DRIPSTONE)) {
            return PathType.DAMAGE_CAUTIOUS
        }

        return if (type == PathType.DOOR_WOOD_CLOSED && canOpenDoors && canEnterOpenDoors) {
            PathType.WALKABLE_DOOR
        } else if (type == PathType.DOOR_OPEN && !canEnterOpenDoors) {
            PathType.BLOCKED
        } else if (type == PathType.RAIL && block !is BaseRailBlock && pfContext.getCachedBlockState(pos.below()).block !is BaseRailBlock) {
            PathType.UNPASSABLE_RAIL
        } else if (type == PathType.LEAVES) {
            if (canPathThroughLeaves() && blockState.block === CobblemonBlocks.SACCHARINE_LEAVES) {
                PathType.OPEN
            } else {
                PathType.BLOCKED
            }
        } else type
    }

    companion object {
        const val MAX_HITBOX_SIZE_FOR_WALKING = 1.6F
        const val MAX_HITBOX_FOR_VERTICAL_NEIGHBOURS = 2F
    }

    fun canWalk(): Boolean {
        return if (this.mob is OmniPathingEntity) {
            (this.mob as OmniPathingEntity).canWalk()
        } else {
            true
        }
    }

    fun canSwimInLava(): Boolean {
        return if (this.mob is OmniPathingEntity) {
            (this.mob as OmniPathingEntity).canSwimInLava()
        } else {
            false
        }
    }

    fun canSwimInWater(): Boolean {
        return if (this.mob is OmniPathingEntity) {
            (this.mob as OmniPathingEntity).canSwimInWater()
        } else {
            false
        }
    }

    fun canSwimUnderFluid(fluidState: FluidState): Boolean {
        return if (this.mob is OmniPathingEntity) {
            (this.mob as OmniPathingEntity).canSwimUnderFluid(fluidState)
        } else {
            false
        }
    }

    fun canPathThroughLeaves() : Boolean {
        return if (this.mob is OmniPathingEntity) {
            (this.mob as OmniPathingEntity).canPathThroughSaccLeaves()
        } else {
            false
        }
    }

    fun canWalkOnWater(): Boolean {
        return if (this.mob is OmniPathingEntity) {
            (this.mob as OmniPathingEntity).canWalkOnWater()
        } else {
            false
        }
    }

    fun canFly(): Boolean {
        return if (this.mob is OmniPathingEntity) {
            (this.mob as OmniPathingEntity).canFly()
        } else {
            false
        }
    }

    fun isOnGround(): Boolean {
        return if (this.mob is OmniPathingEntity) {
            (this.mob as OmniPathingEntity).entityOnGround()
        } else {
            false
        }
    }
}
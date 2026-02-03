/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.api.apricorn.Apricorn
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.block.*
import com.cobblemon.mod.common.block.campfirepot.CampfireBlock
import com.cobblemon.mod.common.block.LecternBlock
import com.cobblemon.mod.common.block.MintBlock.MintType
import com.cobblemon.mod.common.block.campfirepot.CampfirePotBlock
import com.cobblemon.mod.common.block.chest.GildedChestBlock
import com.cobblemon.mod.common.block.general.BaleBlock
import com.cobblemon.mod.common.block.grower.SaccharineTreeGrower
import com.cobblemon.mod.common.block.sign.CobblemonHangingSignBlock
import com.cobblemon.mod.common.block.sign.CobblemonSignBlock
import com.cobblemon.mod.common.block.sign.CobblemonWallHangingSignBlock
import com.cobblemon.mod.common.block.sign.CobblemonWallSignBlock
import com.cobblemon.mod.common.block.general.HorizontalRotationCarpetBlock
import com.cobblemon.mod.common.block.general.HorizontalRotationalBlock
import com.cobblemon.mod.common.mixin.invoker.*
import com.cobblemon.mod.common.platform.PlatformRegistry
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.valueproviders.UniformInt
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.block.state.properties.WoodType
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.shapes.Shapes

@Suppress("SameParameterValue", "HasPlatformType", "MemberVisibilityCanBePrivate", "unused")
object CobblemonBlocks : PlatformRegistry<Registry<Block>, ResourceKey<Registry<Block>>, Block>() {

    override val registry: Registry<Block> = BuiltInRegistries.BLOCK
    override val resourceKey: ResourceKey<Registry<Block>> = Registries.BLOCK

    val APRICORN_BLOCK_SET_TYPE = BlockSetType("apricorn")
    val APRICORN_WOOD_TYPE = WoodType.register(WoodType("apricorn", APRICORN_BLOCK_SET_TYPE))
    val SACCHARINE_BLOCK_SET_TYPE = BlockSetType("saccharine")
    val SACCHARINE_WOOD_TYPE = WoodType.register(WoodType("saccharine", SACCHARINE_BLOCK_SET_TYPE))

    // Evolution Ores
    @JvmField
    val DAWN_STONE_ORE = evolutionStoneOre("dawn_stone_ore", 7)
    @JvmField
    val DUSK_STONE_ORE = evolutionStoneOre("dusk_stone_ore")
    @JvmField
    val FIRE_STONE_ORE = evolutionStoneOre("fire_stone_ore", 10)
    @JvmField
    val NETHER_FIRE_STONE_ORE = this.create("nether_fire_stone_ore", DropExperienceBlock(UniformInt.of(1, 2), BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F).sound(SoundType.NETHER_ORE).lightLevel{10}))
    @JvmField
    val ICE_STONE_ORE = evolutionStoneOre("ice_stone_ore")
    @JvmField
    val LEAF_STONE_ORE = evolutionStoneOre("leaf_stone_ore")
    @JvmField
    val MOON_STONE_ORE = evolutionStoneOre("moon_stone_ore")
    @JvmField
    val DRIPSTONE_MOON_STONE_ORE = this.create("dripstone_moon_stone_ore", DropExperienceBlock(UniformInt.of(1, 2), BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F).sound(SoundType.DRIPSTONE_BLOCK)))
    @JvmField
    val SHINY_STONE_ORE = evolutionStoneOre("shiny_stone_ore", 12)
    @JvmField
    val SUN_STONE_ORE = evolutionStoneOre("sun_stone_ore", 2)
    @JvmField
    val TERRACOTTA_SUN_STONE_ORE = this.create("terracotta_sun_stone_ore", DropExperienceBlock(UniformInt.of(1, 2), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F).lightLevel{2}))
    @JvmField
    val THUNDER_STONE_ORE = evolutionStoneOre("thunder_stone_ore", 6)
    @JvmField
    val WATER_STONE_ORE = evolutionStoneOre("water_stone_ore")

    @JvmField
    val DEEPSLATE_DAWN_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_dawn_stone_ore", 7)
    @JvmField
    val DEEPSLATE_DUSK_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_dusk_stone_ore")
    @JvmField
    val DEEPSLATE_FIRE_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_fire_stone_ore", 10)
    @JvmField
    val DEEPSLATE_ICE_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_ice_stone_ore")
    @JvmField
    val DEEPSLATE_LEAF_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_leaf_stone_ore")
    @JvmField
    val DEEPSLATE_MOON_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_moon_stone_ore")
    @JvmField
    val DEEPSLATE_SHINY_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_shiny_stone_ore", 12)
    @JvmField
    val DEEPSLATE_SUN_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_sun_stone_ore", 2)
    @JvmField
    val DEEPSLATE_THUNDER_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_thunder_stone_ore", 6)
    @JvmField
    val DEEPSLATE_WATER_STONE_ORE = this.deepslateEvolutionStoneOre("deepslate_water_stone_ore")

    // Apricorns
    @JvmField
    val APRICORN_LOG = log("apricorn_log", arg2 = MapColor.COLOR_BROWN)
    @JvmField
    val STRIPPED_APRICORN_LOG = log("stripped_apricorn_log")
    @JvmField
    val APRICORN_WOOD = log("apricorn_wood")
    @JvmField
    val STRIPPED_APRICORN_WOOD = log("stripped_apricorn_wood")
    @JvmField
    val APRICORN_PLANKS = this.create("apricorn_planks",
        Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASS)
                .strength(2.0f, 3.0f).sound(SoundType.WOOD)
        )
    )
    @JvmField
    val APRICORN_LEAVES = leaves("apricorn_leaves")
    @JvmField
    val APRICORN_FENCE = this.create("apricorn_fence", FenceBlock(BlockBehaviour.Properties.of().mapColor(APRICORN_PLANKS.defaultMapColor()).instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f).sound(SoundType.WOOD)))
    @JvmField
    val APRICORN_FENCE_GATE = this.create("apricorn_fence_gate", FenceGateBlock(APRICORN_WOOD_TYPE, BlockBehaviour.Properties.of().mapColor(APRICORN_PLANKS.defaultMapColor()).strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava().forceSolidOn()))
    @JvmField
    val APRICORN_BUTTON = this.create("apricorn_button", BlocksInvoker.createWoodenButtonBlock(APRICORN_BLOCK_SET_TYPE))
    @JvmField
    val APRICORN_PRESSURE_PLATE = this.create("apricorn_pressure_plate", PressurePlateBlockInvoker.`cobblemon$create`(APRICORN_BLOCK_SET_TYPE, BlockBehaviour.Properties.of().mapColor(APRICORN_PLANKS.defaultMapColor()).noCollission().strength(0.5f).sound(SoundType.WOOD)))
    @JvmField
    val APRICORN_SIGN = this.create("apricorn_sign", CobblemonSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN), APRICORN_WOOD_TYPE))

    @JvmField //if you wonder why we don't copy of OAK_WALL_SIGN, then I can tell you its because it uses Properties#dropsLike to point to the SIGN loot table, but we cant use that in our setup due to a deeper rooted issue, so falling back to this and writing dedicated (duplicated) loot tables
    val APRICORN_WALL_SIGN = this.create(
        "apricorn_wall_sign",
        CobblemonWallSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN), APRICORN_WOOD_TYPE)
    )
    @JvmField
    val APRICORN_HANGING_SIGN = this.create(
        "apricorn_hanging_sign",
        CobblemonHangingSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN), APRICORN_WOOD_TYPE)
    )
    @JvmField
    val APRICORN_WALL_HANGING_SIGN = this.create("apricorn_wall_hanging_sign", CobblemonWallHangingSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN), APRICORN_WOOD_TYPE))
    @JvmField
    val APRICORN_SLAB = this.create("apricorn_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f).sound(SoundType.WOOD)))
    @JvmField
    val APRICORN_STAIRS = this.create("apricorn_stairs", StairsBlockInvoker.`cobblemon$create`(APRICORN_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(APRICORN_PLANKS)))
    @JvmField
    val APRICORN_DOOR = this.create("apricorn_door", DoorBlockInvoker.`cobblemon$create`(APRICORN_BLOCK_SET_TYPE, BlockBehaviour.Properties.of().mapColor(APRICORN_PLANKS.defaultMapColor()).instrument(NoteBlockInstrument.BASS).strength(3.0F).sound(SoundType.WOOD).noOcclusion()))
    @JvmField
    val APRICORN_TRAPDOOR = this.create("apricorn_trapdoor", TrapdoorBlockInvoker.`cobblemon$create`(APRICORN_BLOCK_SET_TYPE, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn { _, _, _, _ -> false }))

    private val PLANT_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY)
    @JvmField
    val BLACK_APRICORN_SAPLING = this.create("black_apricorn_sapling", ApricornSaplingBlock(PLANT_PROPERTIES, Apricorn.BLACK))
    @JvmField
    val BLUE_APRICORN_SAPLING = this.create("blue_apricorn_sapling", ApricornSaplingBlock(PLANT_PROPERTIES, Apricorn.BLUE))
    @JvmField
    val GREEN_APRICORN_SAPLING = this.create("green_apricorn_sapling", ApricornSaplingBlock(PLANT_PROPERTIES, Apricorn.GREEN))
    @JvmField
    val PINK_APRICORN_SAPLING = this.create("pink_apricorn_sapling", ApricornSaplingBlock(PLANT_PROPERTIES, Apricorn.PINK))
    @JvmField
    val RED_APRICORN_SAPLING = this.create("red_apricorn_sapling", ApricornSaplingBlock(PLANT_PROPERTIES, Apricorn.RED))
    @JvmField
    val WHITE_APRICORN_SAPLING = this.create("white_apricorn_sapling", ApricornSaplingBlock(PLANT_PROPERTIES, Apricorn.WHITE))
    @JvmField
    val YELLOW_APRICORN_SAPLING = this.create("yellow_apricorn_sapling", ApricornSaplingBlock(PLANT_PROPERTIES, Apricorn.YELLOW))

    // Saccharines
    @JvmField
    val SACCHARINE_LOG = this.create("saccharine_log", SaccharineLogBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()))
    @JvmField
    val SACCHARINE_LOG_SLATHERED = this.create("saccharine_log_slathered", SaccharineLogSlatheredBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava()))

    @JvmField
    val STRIPPED_SACCHARINE_LOG = this.create("stripped_saccharine_log", RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava())) //log("stripped_saccharine_log")
    @JvmField
    val SACCHARINE_WOOD = log("saccharine_wood")
    @JvmField
    val STRIPPED_SACCHARINE_WOOD = this.create("stripped_saccharine_wood", RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava())) //log("stripped_saccharine_log")
    @JvmField
    val SACCHARINE_PLANKS = this.create("saccharine_planks", Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f).sound(SoundType.WOOD)))
    @JvmField
    val SACCHARINE_LEAVES = this.create("saccharine_leaves", SaccharineLeafBlock(
        BlockBehaviour.Properties.of().mapColor(
            MapColor.PLANT
        ).strength(0.2f).randomTicks().sound(SoundType.GRASS).noOcclusion()
            .isValidSpawn { _: BlockState?, _: BlockGetter?, _: BlockPos?, entity: EntityType<*>? ->
                entity === EntityType.OCELOT || entity === EntityType.PARROT
            }.isSuffocating { _: BlockState?, _: BlockGetter?, _: BlockPos? ->
                false
            }.isViewBlocking { _: BlockState?, _: BlockGetter?, _: BlockPos? ->
                false
            }.ignitedByLava().pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor { _: BlockState?, _: BlockGetter?, _: BlockPos? ->
                false
            }))
    @JvmField
    val SACCHARINE_FENCE = this.create("saccharine_fence", FenceBlock(BlockBehaviour.Properties.of().mapColor(SACCHARINE_PLANKS.defaultMapColor()).instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f).sound(SoundType.WOOD)))
    @JvmField
    val SACCHARINE_FENCE_GATE = this.create("saccharine_fence_gate", FenceGateBlock(SACCHARINE_WOOD_TYPE, BlockBehaviour.Properties.of().mapColor(SACCHARINE_PLANKS.defaultMapColor()).instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava().forceSolidOn()))
    @JvmField
    val SACCHARINE_BUTTON = this.create("saccharine_button", BlocksInvoker.createWoodenButtonBlock(BlockSetType.OAK))
    @JvmField
    val SACCHARINE_PRESSURE_PLATE = this.create("saccharine_pressure_plate", PressurePlateBlockInvoker.`cobblemon$create`(SACCHARINE_BLOCK_SET_TYPE, BlockBehaviour.Properties.of().mapColor(SACCHARINE_PLANKS.defaultMapColor()).instrument(NoteBlockInstrument.BASS).noCollission().strength(0.5f).sound(SoundType.WOOD)))
    @JvmField
    val SACCHARINE_SIGN = this.create("saccharine_sign", CobblemonSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN), SACCHARINE_WOOD_TYPE))
    @JvmField
    val SACCHARINE_WALL_SIGN = this.create("saccharine_wall_sign", CobblemonWallSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SIGN), SACCHARINE_WOOD_TYPE))
    @JvmField
    val SACCHARINE_HANGING_SIGN = this.create("saccharine_hanging_sign", CobblemonHangingSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN), SACCHARINE_WOOD_TYPE))
    @JvmField
    val SACCHARINE_WALL_HANGING_SIGN = this.create("saccharine_wall_hanging_sign", CobblemonWallHangingSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN), SACCHARINE_WOOD_TYPE))
    @JvmField
    val SACCHARINE_SLAB = this.create("saccharine_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f).sound(SoundType.WOOD)))
    @JvmField
    val SACCHARINE_STAIRS = this.create("saccharine_stairs", StairsBlockInvoker.`cobblemon$create`(SACCHARINE_PLANKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(SACCHARINE_PLANKS)))
    @JvmField
    val SACCHARINE_DOOR = this.create("saccharine_door", DoorBlockInvoker.`cobblemon$create`(SACCHARINE_BLOCK_SET_TYPE, BlockBehaviour.Properties.of().mapColor(SACCHARINE_PLANKS.defaultMapColor()).instrument(NoteBlockInstrument.BASS).strength(3.0F).sound(SoundType.WOOD).noOcclusion()))
    @JvmField
    val SACCHARINE_TRAPDOOR = this.create("saccharine_trapdoor", TrapdoorBlockInvoker.`cobblemon$create`(SACCHARINE_BLOCK_SET_TYPE, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(3.0F).sound(SoundType.WOOD).noOcclusion()))
    @JvmField
    val SACCHARINE_SAPLING = this.create("saccharine_sapling", SaplingBlock(SaccharineTreeGrower(), PLANT_PROPERTIES))

    @JvmField
    val MEDICINAL_LEEK = this.create("medicinal_leek", MedicinalLeekBlock(BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).ignitedByLava().mapColor(MapColor.CRIMSON_NYLIUM).noCollission().randomTicks().instabreak().sound(CobblemonSounds.MEDICINAL_LEEK_SOUNDS)))

    // TODO after 1.7
//    @JvmField
//    val BUGWORT = this.create("bugwort", BugwortBlock(BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).ignitedByLava().mapColor(MapColor.COLOR_PURPLE).noCollission().randomTicks().instabreak().sound(CobblemonSounds.MEDICINAL_LEEK_SOUNDS)))
    @JvmField
    val ENERGY_ROOT = this.create("energy_root", EnergyRootBlock(BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).ignitedByLava().mapColor(MapColor.DIRT).noCollission().randomTicks().instabreak().sound(CobblemonSounds.ENERGY_ROOT_SOUNDS)))
    @JvmField
    val BIG_ROOT = this.create("big_root", BigRootBlock(BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).ignitedByLava().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(CobblemonSounds.BIG_ROOT_SOUNDS)))
    @JvmField
    val REVIVAL_HERB = this.create("revival_herb", RevivalHerbBlock(BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).mapColor(MapColor.PLANT).ignitedByLava().noCollission().instabreak().sound(CobblemonSounds.REVIVAL_HERB_SOUNDS)))

    @JvmField
    val POKE_CAKE = this.create("poke_cake", PokeSnackBlock(BlockBehaviour.Properties.of().forceSolidOn().strength(0.5F).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).lightLevel { if (it.getValue(BlockStateProperties.LIT)) 3 else 0 }, false))
    @JvmField
    val POKE_SNACK = this.create("poke_snack", PokeSnackBlock(BlockBehaviour.Properties.of().forceSolidOn().strength(0.5F).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).lightLevel { if (it.getValue(BlockStateProperties.LIT)) 3 else 0 }, true))

    @JvmField
    val TUMBLESTONE_CLUSTER = tumblestoneBlock("tumblestone_cluster", GrowableStoneBlock.STAGE_3, 7, 3, null)
    @JvmField
    val LARGE_BUDDING_TUMBLESTONE = tumblestoneBlock("large_budding_tumblestone", GrowableStoneBlock.STAGE_2, 5, 3, TUMBLESTONE_CLUSTER)
    @JvmField
    val MEDIUM_BUDDING_TUMBLESTONE = tumblestoneBlock("medium_budding_tumblestone", GrowableStoneBlock.STAGE_1, 4, 3, LARGE_BUDDING_TUMBLESTONE)
    @JvmField
    val SMALL_BUDDING_TUMBLESTONE = tumblestoneBlock("small_budding_tumblestone", GrowableStoneBlock.STAGE_0, 3, 4, MEDIUM_BUDDING_TUMBLESTONE)

    @JvmField
    val SKY_TUMBLESTONE_CLUSTER = tumblestoneBlock("sky_tumblestone_cluster", GrowableStoneBlock.STAGE_3, 7, 3, null)
    @JvmField
    val LARGE_BUDDING_SKY_TUMBLESTONE = tumblestoneBlock("large_budding_sky_tumblestone", GrowableStoneBlock.STAGE_2, 5, 3, SKY_TUMBLESTONE_CLUSTER)
    @JvmField
    val MEDIUM_BUDDING_SKY_TUMBLESTONE = tumblestoneBlock("medium_budding_sky_tumblestone", GrowableStoneBlock.STAGE_1, 4, 3, LARGE_BUDDING_SKY_TUMBLESTONE)
    @JvmField
    val SMALL_BUDDING_SKY_TUMBLESTONE = tumblestoneBlock("small_budding_sky_tumblestone", GrowableStoneBlock.STAGE_0, 3, 4, MEDIUM_BUDDING_SKY_TUMBLESTONE)

    @JvmField
    val BLACK_TUMBLESTONE_CLUSTER = tumblestoneBlock("black_tumblestone_cluster", GrowableStoneBlock.STAGE_3, 7, 3, null)
    @JvmField
    val LARGE_BUDDING_BLACK_TUMBLESTONE = tumblestoneBlock("large_budding_black_tumblestone", GrowableStoneBlock.STAGE_2, 5, 3, BLACK_TUMBLESTONE_CLUSTER)
    @JvmField
    val MEDIUM_BUDDING_BLACK_TUMBLESTONE = tumblestoneBlock("medium_budding_black_tumblestone", GrowableStoneBlock.STAGE_1, 4, 3, LARGE_BUDDING_BLACK_TUMBLESTONE)
    @JvmField
    val SMALL_BUDDING_BLACK_TUMBLESTONE = tumblestoneBlock("small_budding_black_tumblestone", GrowableStoneBlock.STAGE_0, 3, 4, MEDIUM_BUDDING_BLACK_TUMBLESTONE)

    @JvmField
    val TUMBLESTONE_BLOCK = this.create("tumblestone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SKY_TUMBLESTONE_BLOCK = this.create("sky_tumblestone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val BLACK_TUMBLESTONE_BLOCK = this.create("black_tumblestone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))

    @JvmField
    val POLISHED_TUMBLESTONE = this.create("polished_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val POLISHED_TUMBLESTONE_STAIRS = this.create("polished_tumblestone_stairs", StairsBlockInvoker.`cobblemon$create`(POLISHED_TUMBLESTONE.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(POLISHED_TUMBLESTONE)))
    @JvmField
    val POLISHED_TUMBLESTONE_SLAB = this.create("polished_tumblestone_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val POLISHED_TUMBLESTONE_WALL = this.create("polished_tumblestone_wall", WallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val CHISELED_POLISHED_TUMBLESTONE = this.create("chiseled_polished_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SMOOTH_TUMBLESTONE = this.create("smooth_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SMOOTH_TUMBLESTONE_STAIRS = this.create("smooth_tumblestone_stairs", StairsBlockInvoker.`cobblemon$create`(SMOOTH_TUMBLESTONE.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(SMOOTH_TUMBLESTONE)))
    @JvmField
    val SMOOTH_TUMBLESTONE_SLAB = this.create("smooth_tumblestone_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val TUMBLESTONE_BRICKS = this.create("tumblestone_bricks", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val TUMBLESTONE_BRICK_STAIRS = this.create("tumblestone_brick_stairs", StairsBlockInvoker.`cobblemon$create`(TUMBLESTONE_BRICKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(TUMBLESTONE_BRICKS)))
    @JvmField
    val TUMBLESTONE_BRICK_SLAB = this.create("tumblestone_brick_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val TUMBLESTONE_BRICK_WALL = this.create("tumblestone_brick_wall", WallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val CHISELED_TUMBLESTONE_BRICKS = this.create("chiseled_tumblestone_bricks", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))

    @JvmField
    val POLISHED_SKY_TUMBLESTONE = this.create("polished_sky_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val POLISHED_SKY_TUMBLESTONE_STAIRS = this.create("polished_sky_tumblestone_stairs", StairsBlockInvoker.`cobblemon$create`(POLISHED_SKY_TUMBLESTONE.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(POLISHED_SKY_TUMBLESTONE)))
    @JvmField
    val POLISHED_SKY_TUMBLESTONE_SLAB = this.create("polished_sky_tumblestone_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val POLISHED_SKY_TUMBLESTONE_WALL = this.create("polished_sky_tumblestone_wall", WallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val CHISELED_POLISHED_SKY_TUMBLESTONE = this.create("chiseled_polished_sky_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SMOOTH_SKY_TUMBLESTONE = this.create("smooth_sky_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SMOOTH_SKY_TUMBLESTONE_STAIRS = this.create("smooth_sky_tumblestone_stairs", StairsBlockInvoker.`cobblemon$create`(SMOOTH_SKY_TUMBLESTONE.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(SMOOTH_SKY_TUMBLESTONE)))
    @JvmField
    val SMOOTH_SKY_TUMBLESTONE_SLAB = this.create("smooth_sky_tumblestone_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SKY_TUMBLESTONE_BRICKS = this.create("sky_tumblestone_bricks", Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SKY_TUMBLESTONE_BRICK_STAIRS = this.create("sky_tumblestone_brick_stairs", StairsBlockInvoker.`cobblemon$create`(SKY_TUMBLESTONE_BRICKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(SKY_TUMBLESTONE_BRICKS)))
    @JvmField
    val SKY_TUMBLESTONE_BRICK_SLAB = this.create("sky_tumblestone_brick_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SKY_TUMBLESTONE_BRICK_WALL = this.create("sky_tumblestone_brick_wall", WallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val CHISELED_SKY_TUMBLESTONE_BRICKS = this.create("chiseled_sky_tumblestone_bricks", Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))

    @JvmField
    val POLISHED_BLACK_TUMBLESTONE = this.create("polished_black_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val POLISHED_BLACK_TUMBLESTONE_STAIRS = this.create("polished_black_tumblestone_stairs", StairsBlockInvoker.`cobblemon$create`(POLISHED_BLACK_TUMBLESTONE.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(POLISHED_BLACK_TUMBLESTONE)))
    @JvmField
    val POLISHED_BLACK_TUMBLESTONE_SLAB = this.create("polished_black_tumblestone_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val POLISHED_BLACK_TUMBLESTONE_WALL = this.create("polished_black_tumblestone_wall", WallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val CHISELED_POLISHED_BLACK_TUMBLESTONE = this.create("chiseled_polished_black_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SMOOTH_BLACK_TUMBLESTONE = this.create("smooth_black_tumblestone", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val SMOOTH_BLACK_TUMBLESTONE_STAIRS = this.create("smooth_black_tumblestone_stairs", StairsBlockInvoker.`cobblemon$create`(SMOOTH_BLACK_TUMBLESTONE.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(SMOOTH_BLACK_TUMBLESTONE)))
    @JvmField
    val SMOOTH_BLACK_TUMBLESTONE_SLAB = this.create("smooth_black_tumblestone_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val BLACK_TUMBLESTONE_BRICKS = this.create("black_tumblestone_bricks", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val BLACK_TUMBLESTONE_BRICK_STAIRS = this.create("black_tumblestone_brick_stairs", StairsBlockInvoker.`cobblemon$create`(BLACK_TUMBLESTONE_BRICKS.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(BLACK_TUMBLESTONE_BRICKS)))
    @JvmField
    val BLACK_TUMBLESTONE_BRICK_SLAB = this.create("black_tumblestone_brick_slab", SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val BLACK_TUMBLESTONE_BRICK_WALL = this.create("black_tumblestone_brick_wall", WallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))
    @JvmField
    val CHISELED_BLACK_TUMBLESTONE_BRICKS = this.create("chiseled_black_tumblestone_bricks", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(1.0F).sound(CobblemonSounds.TUMBLESTONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BASEDRUM)))

    @JvmField
    val FIRE_STONE_BLOCK = this.create("fire_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT).lightLevel{13}))
    @JvmField
    val WATER_STONE_BLOCK = this.create("water_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT)))
    @JvmField
    val THUNDER_STONE_BLOCK = this.create("thunder_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.GLOW_LICHEN).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT).lightLevel{8}))
    @JvmField
    val LEAF_STONE_BLOCK = this.create("leaf_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT)))
    @JvmField
    val ICE_STONE_BLOCK = this.create("ice_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT)))
    @JvmField
    val SUN_STONE_BLOCK = this.create("sun_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT).lightLevel{5}))
    @JvmField
    val MOON_STONE_BLOCK = this.create("moon_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT)))
    @JvmField
    val SHINY_STONE_BLOCK = this.create("shiny_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT).lightLevel{15}))
    @JvmField
    val DAWN_STONE_BLOCK = this.create("dawn_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT).lightLevel{15}))
    @JvmField
    val DUSK_STONE_BLOCK = this.create("dusk_stone_block", Block(BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_HYPHAE).strength(1.0F).sound(CobblemonSounds.EVOLUTION_STONE_BLOCK_SOUNDS).requiresCorrectToolForDrops().instrument(NoteBlockInstrument.BIT)))

    @JvmField
    val BLACK_APRICORN = apricornBlock("black_apricorn", Apricorn.BLACK)
    @JvmField
    val BLUE_APRICORN = apricornBlock("blue_apricorn", Apricorn.BLUE)
    @JvmField
    val GREEN_APRICORN = apricornBlock("green_apricorn", Apricorn.GREEN)
    @JvmField
    val PINK_APRICORN = apricornBlock("pink_apricorn", Apricorn.PINK)
    @JvmField
    val RED_APRICORN = apricornBlock("red_apricorn", Apricorn.RED)
    @JvmField
    val WHITE_APRICORN = apricornBlock("white_apricorn", Apricorn.WHITE)
    @JvmField
    val YELLOW_APRICORN = apricornBlock("yellow_apricorn", Apricorn.YELLOW)

    @JvmField
    val BLACK_CAMPFIRE_POT = create("campfire_pot_black",
        CampfirePotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(CobblemonSounds.CAMPFIRE_POT_SOUNDS)
                .strength(0.5F).pushReaction(PushReaction.BLOCK).noOcclusion()
        )
    )
    @JvmField
    val BLUE_CAMPFIRE_POT = create("campfire_pot_blue",
        CampfirePotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).sound(CobblemonSounds.CAMPFIRE_POT_SOUNDS)
                .strength(0.5F).pushReaction(PushReaction.BLOCK).noOcclusion()
        )
    )
    @JvmField
    val GREEN_CAMPFIRE_POT = create("campfire_pot_green",
        CampfirePotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).sound(CobblemonSounds.CAMPFIRE_POT_SOUNDS)
                .strength(0.5F).pushReaction(PushReaction.BLOCK).noOcclusion()
        )
    )
    @JvmField
    val PINK_CAMPFIRE_POT = create("campfire_pot_pink",
        CampfirePotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).sound(CobblemonSounds.CAMPFIRE_POT_SOUNDS)
                .strength(0.5F).pushReaction(PushReaction.BLOCK).noOcclusion()
        )
    )
    @JvmField
    val RED_CAMPFIRE_POT = create("campfire_pot_red",
        CampfirePotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).sound(CobblemonSounds.CAMPFIRE_POT_SOUNDS)
                .strength(0.5F).pushReaction(PushReaction.BLOCK).noOcclusion()
        )
    )
    @JvmField
    val WHITE_CAMPFIRE_POT = create("campfire_pot_white",
        CampfirePotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                .sound(CobblemonSounds.CAMPFIRE_POT_SOUNDS).strength(0.5F).pushReaction(PushReaction.BLOCK)
                .noOcclusion()
        )
    )
    @JvmField
    val YELLOW_CAMPFIRE_POT = create("campfire_pot_yellow",
        CampfirePotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).sound(CobblemonSounds.CAMPFIRE_POT_SOUNDS)
                .strength(0.5F).pushReaction(PushReaction.BLOCK).noOcclusion()
        )
    )

    @JvmField
    val CAMPFIRE = create("campfire", CampfireBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.BLOCK).mapColor(MapColor.PODZOL).strength(2.0F).lightLevel{14}, false))

    @JvmField
    val SOUL_CAMPFIRE = create("soul_campfire", CampfireBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.BLOCK).mapColor(MapColor.PODZOL).strength(2.0F).lightLevel{9}, true))

    @JvmField
    val RELIC_COIN_POUCH = create("relic_coin_pouch", CoinPouchBlock(BlockBehaviour.Properties.of().sound(CobblemonSounds.RELIC_COIN_POUCH_SOUNDS).pushReaction(PushReaction.DESTROY).noOcclusion(), true))
    @JvmField
    val RELIC_COIN_SACK = create("relic_coin_sack", CoinPouchBlock(BlockBehaviour.Properties.of().sound(CobblemonSounds.RELIC_COIN_SACK_SOUNDS).pushReaction(PushReaction.DESTROY).strength(0.4f), false))

    @JvmField
    val GILDED_CHEST = create("gilded_chest", GildedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion().sound(CobblemonSounds.GILDED_CHEST_SOUNDS), GildedChestBlock.Type.RED))
    @JvmField
    val BLUE_GILDED_CHEST = create("blue_gilded_chest", GildedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion().sound(CobblemonSounds.GILDED_CHEST_SOUNDS), GildedChestBlock.Type.BLUE))
    @JvmField
    val BLACK_GILDED_CHEST = create("black_gilded_chest", GildedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion().sound(CobblemonSounds.GILDED_CHEST_SOUNDS), GildedChestBlock.Type.BLACK))
    @JvmField
    val YELLOW_GILDED_CHEST = create("yellow_gilded_chest", GildedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion().sound(CobblemonSounds.GILDED_CHEST_SOUNDS), GildedChestBlock.Type.YELLOW))
    @JvmField
    val WHITE_GILDED_CHEST = create("white_gilded_chest", GildedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion().sound(CobblemonSounds.GILDED_CHEST_SOUNDS), GildedChestBlock.Type.WHITE))
    @JvmField
    val GREEN_GILDED_CHEST = create("green_gilded_chest", GildedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion().sound(CobblemonSounds.GILDED_CHEST_SOUNDS), GildedChestBlock.Type.GREEN))
    @JvmField
    val PINK_GILDED_CHEST = create("pink_gilded_chest", GildedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion().sound(CobblemonSounds.GILDED_CHEST_SOUNDS), GildedChestBlock.Type.PINK))
    @JvmField
    val GIMMIGHOUL_CHEST = create("gimmighoul_chest", GildedChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).noOcclusion().sound(CobblemonSounds.GILDED_CHEST_SOUNDS), GildedChestBlock.Type.FAKE))

    @JvmField
    val WHITE_PLAQUE = create("white_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val LIGHT_GRAY_PLAQUE = create("light_gray_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val GRAY_PLAQUE = create("gray_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val BLACK_PLAQUE = create("black_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val BROWN_PLAQUE = create("brown_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val RED_PLAQUE = create("red_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val ORANGE_PLAQUE = create("orange_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val YELLOW_PLAQUE = create("yellow_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val LIME_PLAQUE = create("lime_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val GREEN_PLAQUE = create("green_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val CYAN_PLAQUE = create("cyan_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val LIGHT_BLUE_PLAQUE = create("light_blue_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val BLUE_PLAQUE = create("blue_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val PURPLE_PLAQUE = create("purple_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val MAGENTA_PLAQUE = create("magenta_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))
    @JvmField
    val PINK_PLAQUE = create("pink_plaque", PlaqueBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).mapColor(MapColor.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).strength(5.0F)))

    @JvmField
    val CLEANSE_TAG = create("cleanse_tag", WallAttachedStackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).mapColor(MapColor.NONE).pushReaction(PushReaction.DESTROY).noOcclusion().noCollission(), WallAttachedStackableItemBlock.WallStackableItemBlockType.TAG))
    @JvmField
    val SPELL_TAG = create("spell_tag", WallAttachedStackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOD).mapColor(MapColor.NONE).pushReaction(PushReaction.DESTROY).noOcclusion().noCollission(), WallAttachedStackableItemBlock.WallStackableItemBlockType.TAG))

    @JvmField
    val BLUNDER_POLICY = create("blunder_policy", WallAttachedDirectionalShapeBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion().noCollission(), 14, 12, 1))
    @JvmField
    val WEAKNESS_POLICY = create("weakness_policy", WallAttachedDirectionalShapeBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion().noCollission(), 14, 12, 1))

    @JvmField
    val FULL_HEAL = create("full_heal", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.STATUS_RESTORE))
    @JvmField
    val ANTIDOTE = create("antidote", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.STATUS_RESTORE))
    @JvmField
    val AWAKENING = create("awakening", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.STATUS_RESTORE))
    @JvmField
    val BURN_HEAL = create("burn_heal", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.STATUS_RESTORE))
    @JvmField
    val ICE_HEAL = create("ice_heal", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.STATUS_RESTORE))
    @JvmField
    val PARALYZE_HEAL = create("paralyze_heal", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.STATUS_RESTORE))

    @JvmField
    val ETHER = create("ether", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.ETHER))
    @JvmField
    val MAX_ETHER = create("max_ether", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.ETHER))
    @JvmField
    val ELIXIR = create("elixir", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.ETHER))
    @JvmField
    val MAX_ELIXIR = create("max_elixir", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.ETHER))

    @JvmField
    val POTION = create("potion", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.POTION))
    @JvmField
    val SUPER_POTION = create("super_potion", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.POTION))
    @JvmField
    val HYPER_POTION = create("hyper_potion", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.POTION))
    @JvmField
    val MAX_POTION = create("max_potion", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.MAX_POTION))
    @JvmField
    val FULL_RESTORE = create("full_restore", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.MAX_POTION))

    @JvmField
    val DIRE_HIT = create("dire_hit", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.BATTLE_ITEM))
    @JvmField
    val GUARD_SPEC = create("guard_spec", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.BATTLE_ITEM))
    @JvmField
    val X_ACCURACY = create("x_${Stats.ACCURACY.identifier.path}", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.BATTLE_ITEM))
    @JvmField
    val X_ATTACK = create("x_${Stats.ATTACK.identifier.path}", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.BATTLE_ITEM))
    @JvmField
    val X_DEFENSE = create("x_${Stats.DEFENCE.identifier.path}", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.BATTLE_ITEM))
    @JvmField
    val X_SP_ATK = create("x_${Stats.SPECIAL_ATTACK.identifier.path}", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.BATTLE_ITEM))
    @JvmField
    val X_SP_DEF = create("x_${Stats.SPECIAL_DEFENCE.identifier.path}", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.BATTLE_ITEM))
    @JvmField
    val X_SPEED = create("x_${Stats.SPEED.identifier.path}", StackableItemBlock(BlockBehaviour.Properties.of().sound(SoundType.CANDLE).mapColor(MapColor.NONE).noOcclusion(), StackableItemBlock.StackableItemBlockType.BATTLE_ITEM))

    @JvmField
    val MONITOR = create("monitor", MonitorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F).lightLevel { if (it.getValue(MonitorBlock.SCREEN) != MonitorBlock.MonitorScreen.OFF) 13 else 0 }))
    @JvmField
    val FOSSIL_ANALYZER = create("fossil_analyzer", FossilAnalyzerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F).noOcclusion()))
    @JvmField
    val RESTORATION_TANK = create("restoration_tank", RestorationTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.GLASS).pushReaction(PushReaction.BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F).noOcclusion()))

    @JvmField
    val HEALING_MACHINE = create("healing_machine", HealingMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK).strength(2f).noOcclusion().lightLevel { if (it.getValue(HealingMachineBlock.CHARGE_LEVEL) >= HealingMachineBlock.MAX_CHARGE_LEVEL) 12 else 5 }))

    @JvmField
    val PC = create("pc", PCBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK).strength(2F).noOcclusion().lightLevel { if ((it.getValue(PCBlock.ON) as Boolean) && (it.getValue(PCBlock.PART) == PCBlock.PCPart.TOP)) 13 else 0 }))

    @JvmField
    val LECTERN = create("lectern", LecternBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(2.5F).ignitedByLava().lightLevel { if (it.getValue(LecternBlock.EMIT_LIGHT) as Boolean) 13 else 0 }))

    @JvmField
    val DISPLAY_CASE = create("display_case", DisplayCaseBlock(BlockBehaviour.Properties.of().sound(CobblemonSounds.DISPLAY_CASE_SOUNDS).noOcclusion().pushReaction(PushReaction.BLOCK).mapColor(MapColor.STONE).strength(0.3F).requiresCorrectToolForDrops()))

    // TODO after 1.7
//    @JvmField
//    val INCENSE_SWEET = create("incense_sweet", SweetIncenseBlock(BlockBehaviour.Properties.of().sound(CobblemonSounds.DISPLAY_CASE_SOUNDS).noOcclusion().pushReaction(PushReaction.BLOCK).mapColor(MapColor.STONE).strength(0.3f)))

    @JvmField
    val PASTURE = create("pasture", PastureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).sound(SoundType.WOOD).strength(2F).noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel { if ((it.getValue(PastureBlock.ON) as Boolean) && (it.getValue(PastureBlock.PART) == PastureBlock.PasturePart.TOP)) 13 else 0 }))

    val RED_MINT = create("red_mint", MintBlock(MintType.RED, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().randomTicks().instabreak().sound(CobblemonSounds.MINT_SOUNDS).pushReaction(PushReaction.DESTROY)))
    @JvmField
    val BLUE_MINT = create("blue_mint", MintBlock(MintType.BLUE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).noCollission().randomTicks().instabreak().sound(CobblemonSounds.MINT_SOUNDS).pushReaction(PushReaction.DESTROY)))
    @JvmField
    val CYAN_MINT = create("cyan_mint", MintBlock(MintType.CYAN, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollission().randomTicks().instabreak().sound(CobblemonSounds.MINT_SOUNDS).pushReaction(PushReaction.DESTROY)))
    @JvmField
    val PINK_MINT = create("pink_mint", MintBlock(MintType.PINK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollission().randomTicks().instabreak().sound(CobblemonSounds.MINT_SOUNDS).pushReaction(PushReaction.DESTROY)))
    @JvmField
    val GREEN_MINT = create("green_mint", MintBlock(MintType.GREEN, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).noCollission().randomTicks().instabreak().sound(CobblemonSounds.MINT_SOUNDS).pushReaction(PushReaction.DESTROY)))
    @JvmField
    val WHITE_MINT = create("white_mint", MintBlock(MintType.WHITE, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).noCollission().randomTicks().instabreak().sound(CobblemonSounds.MINT_SOUNDS).pushReaction(PushReaction.DESTROY)))

    @JvmField
    val VIVICHOKE_SEEDS = this.create("vivichoke_seeds", VivichokeBlock(BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).ignitedByLava().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(CobblemonSounds.VIVICHOKE_SOUNDS)))
    @JvmField
    val PEP_UP_FLOWER = this.create("pep_up_flower", FlowerBlock(MobEffects.LEVITATION, 10F, BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY)))
    @JvmField
    val POTTED_PEP_UP_FLOWER = this.create("potted_pep_up_flower", BlocksInvoker.createFlowerPotBlock(PEP_UP_FLOWER))
    @JvmField
    val HEARTY_GRAINS = this.create("hearty_grains", HeartyGrainsBlock(BlockBehaviour.Properties.of().pushReaction(PushReaction.DESTROY).ignitedByLava().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()))

    @JvmField
    val GALARICA_NUT_BUSH = this.create("galarica_nut_bush", NutBushBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY)))

    @JvmField
    val POTTED_RED_APRICORN_SAPLING = this.create("potted_red_apricorn_sapling", BlocksInvoker.createFlowerPotBlock(RED_APRICORN_SAPLING))
    @JvmField
    val POTTED_YELLOW_APRICORN_SAPLING = this.create("potted_yellow_apricorn_sapling", BlocksInvoker.createFlowerPotBlock(YELLOW_APRICORN_SAPLING))
    @JvmField
    val POTTED_GREEN_APRICORN_SAPLING = this.create("potted_green_apricorn_sapling", BlocksInvoker.createFlowerPotBlock(GREEN_APRICORN_SAPLING))
    @JvmField
    val POTTED_BLUE_APRICORN_SAPLING = this.create("potted_blue_apricorn_sapling", BlocksInvoker.createFlowerPotBlock(BLUE_APRICORN_SAPLING))
    @JvmField
    val POTTED_PINK_APRICORN_SAPLING = this.create("potted_pink_apricorn_sapling", BlocksInvoker.createFlowerPotBlock(PINK_APRICORN_SAPLING))
    @JvmField
    val POTTED_BLACK_APRICORN_SAPLING = this.create("potted_black_apricorn_sapling", BlocksInvoker.createFlowerPotBlock(BLACK_APRICORN_SAPLING))
    @JvmField
    val POTTED_WHITE_APRICORN_SAPLING = this.create("potted_white_apricorn_sapling", BlocksInvoker.createFlowerPotBlock(WHITE_APRICORN_SAPLING))
    @JvmField
    val POTTED_SACCHARINE_SAPLING = this.create("potted_saccharine_sapling", BlocksInvoker.createFlowerPotBlock(SACCHARINE_SAPLING))

    @JvmField
    val HEARTY_GRAIN_BALE = this.create("hearty_grain_bale", BaleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instrument(NoteBlockInstrument.BANJO).strength(0.5F).sound(CobblemonSounds.HEARTY_GRAIN_BALE_SOUNDS)))
    @JvmField
    val TATAMI_BLOCK = this.create("tatami_block", HorizontalRotationalBlock(BlockBehaviour.Properties.of().ignitedByLava().mapColor(MapColor.PLANT).strength(0.1F).sound(CobblemonSounds.TATAMI_BLOCK_SOUNDS)))
    @JvmField
    val TATAMI_MAT = this.create("tatami_mat", HorizontalRotationCarpetBlock(BlockBehaviour.Properties.of().ignitedByLava().mapColor(MapColor.PLANT).strength(0.1F).sound(CobblemonSounds.TATAMI_MAT_SOUNDS)))

    /**
     * Returns a map of all the blocks that can be stripped with an axe in the format of input - output.
     *
     * @return A map of all the blocks that can be stripped with an axe in the format of input - output.
     */
    fun strippedBlocks(): Map<Block, Block> = mapOf(
        APRICORN_WOOD to STRIPPED_APRICORN_WOOD,
        APRICORN_LOG to STRIPPED_APRICORN_LOG,
        SACCHARINE_WOOD to STRIPPED_SACCHARINE_WOOD,
        SACCHARINE_LOG to STRIPPED_SACCHARINE_LOG,
        SACCHARINE_LOG_SLATHERED to STRIPPED_SACCHARINE_LOG
    )

    private fun apricornBlock(name: String, apricorn: Apricorn): ApricornBlock = this.create(name, ApricornBlock(BlockBehaviour.Properties.of().mapColor(apricorn.mapColor()).randomTicks().strength(Blocks.OAK_LOG.defaultDestroyTime(), Blocks.OAK_LOG.explosionResistance).sound(SoundType.WOOD).noOcclusion(), apricorn))

    private fun tumblestoneBlock(name: String, stage: Int, height: Int, xzOffset: Int, nextStage: Block?) : Block {
        return this.create(name, TumblestoneBlock(BlockBehaviour.Properties.of()
            .pushReaction(PushReaction.DESTROY)
            .noOcclusion()
            .strength(1.5F)
            .sound(CobblemonSounds.TUMBLESTONE_SOUNDS),
            stage, height, xzOffset, nextStage))
    }

    private val berries = mutableMapOf<ResourceLocation, BerryBlock>()

    @JvmField
    val AGUAV_BERRY = this.berryBlock("aguav")
    @JvmField
    val APICOT_BERRY = this.berryBlock("apicot")
    @JvmField
    val ASPEAR_BERRY = this.berryBlock("aspear")
    @JvmField
    val BABIRI_BERRY = this.berryBlock("babiri")
    @JvmField
    val BELUE_BERRY = this.berryBlock("belue")
    @JvmField
    val BLUK_BERRY = this.berryBlock("bluk")
    @JvmField
    val CHARTI_BERRY = this.berryBlock("charti")
    @JvmField
    val CHERI_BERRY = this.berryBlock("cheri")
    @JvmField
    val CHESTO_BERRY = this.berryBlock("chesto")
    @JvmField
    val CHILAN_BERRY = this.berryBlock("chilan")
    @JvmField
    val CHOPLE_BERRY = this.berryBlock("chople")
    @JvmField
    val COBA_BERRY = this.berryBlock("coba")
    @JvmField
    val COLBUR_BERRY = this.berryBlock("colbur")
    @JvmField
    val CORNN_BERRY = this.berryBlock("cornn")
    @JvmField
    val CUSTAP_BERRY = this.berryBlock("custap")
    @JvmField
    val DURIN_BERRY = this.berryBlock("durin")
    @JvmField
    val EGGANT_BERRY = this.berryBlock("eggant")
    @JvmField
    val ENIGMA_BERRY = this.berryBlock("enigma")
    @JvmField
    val FIGY_BERRY = this.berryBlock("figy")
    @JvmField
    val GANLON_BERRY = this.berryBlock("ganlon")
    @JvmField
    val GREPA_BERRY = this.berryBlock("grepa")
    @JvmField
    val HABAN_BERRY = this.berryBlock("haban")
    @JvmField
    val HONDEW_BERRY = this.berryBlock("hondew")
    @JvmField
    val HOPO_BERRY = this.berryBlock("hopo")
    @JvmField
    val IAPAPA_BERRY = this.berryBlock("iapapa")
    @JvmField
    val JABOCA_BERRY = this.berryBlock("jaboca")
    @JvmField
    val KASIB_BERRY = this.berryBlock("kasib")
    @JvmField
    val KEBIA_BERRY = this.berryBlock("kebia")
    @JvmField
    val KEE_BERRY = this.berryBlock("kee")
    @JvmField
    val KELPSY_BERRY = this.berryBlock("kelpsy")
    @JvmField
    val LANSAT_BERRY = this.berryBlock("lansat")
    @JvmField
    val LEPPA_BERRY = this.berryBlock("leppa")
    @JvmField
    val LIECHI_BERRY = this.berryBlock("liechi")
    @JvmField
    val LUM_BERRY = this.berryBlock("lum")
    @JvmField
    val MAGO_BERRY = this.berryBlock("mago")
    @JvmField
    val MAGOST_BERRY = this.berryBlock("magost")
    @JvmField
    val MARANGA_BERRY = this.berryBlock("maranga")
    @JvmField
    val MICLE_BERRY = this.berryBlock("micle")
    @JvmField
    val NANAB_BERRY = this.berryBlock("nanab")
    @JvmField
    val NOMEL_BERRY = this.berryBlock("nomel")
    @JvmField
    val OCCA_BERRY = this.berryBlock("occa")
    @JvmField
    val ORAN_BERRY = this.berryBlock("oran")
    @JvmField
    val PAMTRE_BERRY = this.berryBlock("pamtre")
    @JvmField
    val PASSHO_BERRY = this.berryBlock("passho")
    @JvmField
    val PAYAPA_BERRY = this.berryBlock("payapa")
    @JvmField
    val PECHA_BERRY = this.berryBlock("pecha")
    @JvmField
    val PERSIM_BERRY = this.berryBlock("persim")
    @JvmField
    val PETAYA_BERRY = this.berryBlock("petaya")
    @JvmField
    val PINAP_BERRY = this.berryBlock("pinap")
    @JvmField
    val POMEG_BERRY = this.berryBlock("pomeg")
    @JvmField
    val QUALOT_BERRY = this.berryBlock("qualot")
    @JvmField
    val RABUTA_BERRY = this.berryBlock("rabuta")
    @JvmField
    val RAWST_BERRY = this.berryBlock("rawst")
    @JvmField
    val RAZZ_BERRY = this.berryBlock("razz")
    @JvmField
    val RINDO_BERRY = this.berryBlock("rindo")
    @JvmField
    val ROSELI_BERRY = this.berryBlock("roseli")
    @JvmField
    val ROWAP_BERRY = this.berryBlock("rowap")
    @JvmField
    val SALAC_BERRY = this.berryBlock("salac")
    @JvmField
    val SHUCA_BERRY = this.berryBlock("shuca")
    @JvmField
    val SITRUS_BERRY = this.berryBlock("sitrus")
    @JvmField
    val SPELON_BERRY = this.berryBlock("spelon")
    @JvmField
    val STARF_BERRY = this.berryBlock("starf")
    @JvmField
    val TAMATO_BERRY = this.berryBlock("tamato")
    @JvmField
    val TANGA_BERRY = this.berryBlock("tanga")
    @JvmField
    val TOUGA_BERRY = this.berryBlock("touga")
    @JvmField
    val WACAN_BERRY = this.berryBlock("wacan")
    @JvmField
    val WATMEL_BERRY = this.berryBlock("watmel")
    @JvmField
    val WEPEAR_BERRY = this.berryBlock("wepear")
    @JvmField
    val WIKI_BERRY = this.berryBlock("wiki")
    @JvmField
    val YACHE_BERRY = this.berryBlock("yache")
    //val BINDING_SOIL = this.create("binding_soil", BindingSoilBlock(BlockBehaviour.Properties.of()))

    init {
        /**
         * Makes all blocks in array flammable by adding them to FireBlock's flammableRegistry.
         * second value is burn chance and third value is spread chance
         */
        arrayOf(
            Triple(APRICORN_LOG, 5, 5),
            Triple(STRIPPED_APRICORN_LOG, 5, 5),
            Triple(APRICORN_WOOD, 5, 5),
            Triple(STRIPPED_APRICORN_WOOD, 5, 5),
            Triple(APRICORN_PLANKS, 5, 20),
            Triple(APRICORN_LEAVES, 30, 60),
            Triple(APRICORN_FENCE, 5, 20),
            Triple(APRICORN_FENCE_GATE, 5, 20),
            Triple(APRICORN_SLAB, 5, 20),
            Triple(APRICORN_STAIRS, 5, 20),
            Triple(SACCHARINE_LOG, 5, 5),
            Triple(SACCHARINE_LOG_SLATHERED, 5, 5),
            Triple(STRIPPED_SACCHARINE_LOG, 5, 5),
            Triple(SACCHARINE_WOOD, 5, 5),
            Triple(STRIPPED_SACCHARINE_WOOD, 5, 5),
            Triple(SACCHARINE_PLANKS, 5, 20),
            Triple(SACCHARINE_LEAVES, 30, 60),
            Triple(SACCHARINE_FENCE, 5, 20),
            Triple(SACCHARINE_FENCE_GATE, 5, 20),
            Triple(SACCHARINE_SLAB, 5, 20),
            Triple(SACCHARINE_STAIRS, 5, 20)

        ).onEach{ data -> setFlammable(data.first, data.second, data.third) }
    }

    fun berries() = this.berries.toMap()

    private fun berryBlock(name: String): BerryBlock {
        val identifier = cobblemonResource("${name}_berry")
        val block = this.create(identifier.path, BerryBlock(identifier, BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).dynamicShape().sound(CobblemonSounds.BERRY_BUSH_SOUNDS).strength(0.2F)))
        this.berries[identifier] = block
        return block
    }

    /**
     * Calls helper method from Vanilla
     */
    private fun log(name: String, arg: MapColor = MapColor.DIRT, arg2: MapColor = MapColor.DIRT): Block {
        val block = BlocksInvoker.createLogBlock(arg, arg2)
        return this.create(name, block)
    }

    /**
     * Method uses generic E in order to keep the block as the same return type.
     * If E is not a block then it will not be set as flammable.
     * Calls Vanilla implementation of registering a flammable block.
     * Mixins looks cursed but it is java's fault.
     */
    private fun <E> setFlammable(block: E, burnChance: Int, spreadChance: Int): E {
        if(block !is Block) return block

        var fireBlock: FireBlock =  Blocks.FIRE as FireBlock
        //Cursed Mixin stuff
        (fireBlock as FireBlockInvoker).registerNewFlammableBlock(block as Block, burnChance, spreadChance)
        return block
    }

    private fun evolutionStoneOre(name: String) = this.create(name, DropExperienceBlock(UniformInt.of(1, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)))
    private fun evolutionStoneOre(name: String, lightLevel: Int) = this.create(name, DropExperienceBlock(UniformInt.of(1, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).lightLevel { lightLevel }))

    private fun deepslateEvolutionStoneOre(name: String) = this.create(name, DropExperienceBlock(UniformInt.of(1, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)))
    private fun deepslateEvolutionStoneOre(name: String, lightLevel: Int) = this.create(name, DropExperienceBlock(UniformInt.of(1, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).lightLevel { lightLevel }))

    /**
     * Helper method for creating leaves
     * copied over from Vanilla
     */
    private fun leaves(name: String): Block {
        val block = BlocksInvoker.createLeavesBlock(SoundType.GRASS)
        return this.create(name, block)
    }
}
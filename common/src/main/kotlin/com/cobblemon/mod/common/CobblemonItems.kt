/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.cobblemon.mod.common.api.apricorn.Apricorn
import com.cobblemon.mod.common.api.item.ability.AbilityChanger
import com.cobblemon.mod.common.api.mulch.MulchVariant
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.api.text.blue
import com.cobblemon.mod.common.api.text.gray
import com.cobblemon.mod.common.api.types.tera.TeraType
import com.cobblemon.mod.common.api.types.tera.TeraTypes
import com.cobblemon.mod.common.block.BerryBlock
import com.cobblemon.mod.common.block.MintBlock
import com.cobblemon.mod.common.block.MintBlock.MintType
import com.cobblemon.mod.common.client.pokedex.PokedexType
import com.cobblemon.mod.common.block.campfirepot.CampfirePotColor
import com.cobblemon.mod.common.entity.boat.CobblemonBoatType
import com.cobblemon.mod.common.item.*
import com.cobblemon.mod.common.item.armor.CobblemonArmorTrims
import com.cobblemon.mod.common.item.battle.DireHitItem
import com.cobblemon.mod.common.item.battle.GuardSpecItem
import com.cobblemon.mod.common.item.battle.XStatItem
import com.cobblemon.mod.common.item.berry.BerryItem
import com.cobblemon.mod.common.item.berry.FriendshipRaisingBerryItem
import com.cobblemon.mod.common.item.berry.HealingBerryItem
import com.cobblemon.mod.common.item.berry.PPRestoringBerryItem
import com.cobblemon.mod.common.item.berry.PortionHealingBerryItem
import com.cobblemon.mod.common.item.berry.StatusCuringBerryItem
import com.cobblemon.mod.common.item.interactive.PonigiriItem
import com.cobblemon.mod.common.item.food.SinisterTeaItem
import com.cobblemon.mod.common.item.gimmicks.*
import com.cobblemon.mod.common.item.interactive.*
import com.cobblemon.mod.common.item.interactive.ability.AbilityChangeItem
import com.cobblemon.mod.common.item.pokebag.PokeBagItem
import com.cobblemon.mod.common.platform.PlatformRegistry
import com.cobblemon.mod.common.pokeball.PokeBall
import com.cobblemon.mod.common.pokemon.IVs
import com.cobblemon.mod.common.pokemon.helditem.CobblemonHeldItemManager
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.HangingSignItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.ItemNameBlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.SignItem
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

@Suppress("unused", "SameParameterValue")
object CobblemonItems : PlatformRegistry<Registry<Item>, ResourceKey<Registry<Item>>, Item>() {
    override val registry: Registry<Item> = BuiltInRegistries.ITEM
    override val resourceKey: ResourceKey<Registry<Item>> = Registries.ITEM

    @JvmField
    val NPC_EDITOR = create("npc_editor", CobblemonItem(Item.Properties().stacksTo(1)))

    @JvmField
    val pokeBalls = mutableListOf<PokeBallItem>()
    @JvmField
    val POKE_BALL = pokeBallItem(PokeBalls.POKE_BALL)
    @JvmField
    val CITRINE_BALL = pokeBallItem(PokeBalls.CITRINE_BALL)
    @JvmField
    val VERDANT_BALL = pokeBallItem(PokeBalls.VERDANT_BALL)
    @JvmField
    val AZURE_BALL = pokeBallItem(PokeBalls.AZURE_BALL)
    @JvmField
    val ROSEATE_BALL = pokeBallItem(PokeBalls.ROSEATE_BALL)
    @JvmField
    val SLATE_BALL = pokeBallItem(PokeBalls.SLATE_BALL)
    @JvmField
    val PREMIER_BALL = pokeBallItem(PokeBalls.PREMIER_BALL)
    @JvmField
    val GREAT_BALL = pokeBallItem(PokeBalls.GREAT_BALL)
    @JvmField
    val ULTRA_BALL = pokeBallItem(PokeBalls.ULTRA_BALL)
    @JvmField
    val SAFARI_BALL = pokeBallItem(PokeBalls.SAFARI_BALL)
    @JvmField
    val FAST_BALL = pokeBallItem(PokeBalls.FAST_BALL)
    @JvmField
    val LEVEL_BALL = pokeBallItem(PokeBalls.LEVEL_BALL)
    @JvmField
    val LURE_BALL = pokeBallItem(PokeBalls.LURE_BALL)
    @JvmField
    val HEAVY_BALL = pokeBallItem(PokeBalls.HEAVY_BALL)
    @JvmField
    val LOVE_BALL = pokeBallItem(PokeBalls.LOVE_BALL)
    @JvmField
    val FRIEND_BALL = pokeBallItem(PokeBalls.FRIEND_BALL)
    @JvmField
    val MOON_BALL = pokeBallItem(PokeBalls.MOON_BALL)
    @JvmField
    val SPORT_BALL = pokeBallItem(PokeBalls.SPORT_BALL)
    @JvmField
    val PARK_BALL = pokeBallItem(PokeBalls.PARK_BALL)
    @JvmField
    val NET_BALL = pokeBallItem(PokeBalls.NET_BALL)
    @JvmField
    val DIVE_BALL = pokeBallItem(PokeBalls.DIVE_BALL)
    @JvmField
    val NEST_BALL = pokeBallItem(PokeBalls.NEST_BALL)
    @JvmField
    val REPEAT_BALL = pokeBallItem(PokeBalls.REPEAT_BALL)
    @JvmField
    val TIMER_BALL = pokeBallItem(PokeBalls.TIMER_BALL)
    @JvmField
    val LUXURY_BALL = pokeBallItem(PokeBalls.LUXURY_BALL)
    @JvmField
    val DUSK_BALL = pokeBallItem(PokeBalls.DUSK_BALL)
    @JvmField
    val HEAL_BALL = pokeBallItem(PokeBalls.HEAL_BALL)
    @JvmField
    val QUICK_BALL = pokeBallItem(PokeBalls.QUICK_BALL)
    @JvmField
    val DREAM_BALL = pokeBallItem(PokeBalls.DREAM_BALL)
    @JvmField
    val BEAST_BALL = pokeBallItem(PokeBalls.BEAST_BALL)
    @JvmField
    val MASTER_BALL = pokeBallItem(PokeBalls.MASTER_BALL)
    @JvmField
    val CHERISH_BALL = pokeBallItem(PokeBalls.CHERISH_BALL)
    @JvmField
    val ANCIENT_POKE_BALL = pokeBallItem(PokeBalls.ANCIENT_POKE_BALL)
    @JvmField
    val ANCIENT_CITRINE_BALL = pokeBallItem(PokeBalls.ANCIENT_CITRINE_BALL)
    @JvmField
    val ANCIENT_VERDANT_BALL = pokeBallItem(PokeBalls.ANCIENT_VERDANT_BALL)
    @JvmField
    val ANCIENT_AZURE_BALL = pokeBallItem(PokeBalls.ANCIENT_AZURE_BALL)
    @JvmField
    val ANCIENT_ROSEATE_BALL = pokeBallItem(PokeBalls.ANCIENT_ROSEATE_BALL)
    @JvmField
    val ANCIENT_SLATE_BALL = pokeBallItem(PokeBalls.ANCIENT_SLATE_BALL)
    @JvmField
    val ANCIENT_IVORY_BALL = pokeBallItem(PokeBalls.ANCIENT_IVORY_BALL)
    @JvmField
    val ANCIENT_GREAT_BALL = pokeBallItem(PokeBalls.ANCIENT_GREAT_BALL)
    @JvmField
    val ANCIENT_ULTRA_BALL = pokeBallItem(PokeBalls.ANCIENT_ULTRA_BALL)
    @JvmField
    val ANCIENT_FEATHER_BALL = pokeBallItem(PokeBalls.ANCIENT_FEATHER_BALL)
    @JvmField
    val ANCIENT_WING_BALL = pokeBallItem(PokeBalls.ANCIENT_WING_BALL)
    @JvmField
    val ANCIENT_JET_BALL = pokeBallItem(PokeBalls.ANCIENT_JET_BALL)
    @JvmField
    val ANCIENT_HEAVY_BALL = pokeBallItem(PokeBalls.ANCIENT_HEAVY_BALL)
    @JvmField
    val ANCIENT_LEADEN_BALL = pokeBallItem(PokeBalls.ANCIENT_LEADEN_BALL)
    @JvmField
    val ANCIENT_GIGATON_BALL = pokeBallItem(PokeBalls.ANCIENT_GIGATON_BALL)
    @JvmField
    val ANCIENT_ORIGIN_BALL = pokeBallItem(PokeBalls.ANCIENT_ORIGIN_BALL)

    val pokedexes = mutableListOf<PokedexItem>()
    @JvmField
    val POKEDEX_RED = pokedexItem(PokedexType.RED)
    @JvmField
    val POKEDEX_YELLOW = pokedexItem(PokedexType.YELLOW)
    @JvmField
    val POKEDEX_GREEN = pokedexItem(PokedexType.GREEN)
    @JvmField
    val POKEDEX_BLUE = pokedexItem(PokedexType.BLUE)
    @JvmField
    val POKEDEX_PINK = pokedexItem(PokedexType.PINK)
    @JvmField
    val POKEDEX_BLACK = pokedexItem(PokedexType.BLACK)
    @JvmField
    val POKEDEX_WHITE = pokedexItem(PokedexType.WHITE)

    val campfire_pots = mutableListOf<CampfirePotItem>()
    @JvmField
    val CAMPFIRE_POT_RED = campfirePotItem(CobblemonBlocks.RED_CAMPFIRE_POT, CampfirePotColor.RED)
    @JvmField
    val CAMPFIRE_POT_YELLOW = campfirePotItem(CobblemonBlocks.YELLOW_CAMPFIRE_POT, CampfirePotColor.YELLOW)
    @JvmField
    val CAMPFIRE_POT_GREEN = campfirePotItem(CobblemonBlocks.GREEN_CAMPFIRE_POT, CampfirePotColor.GREEN)
    @JvmField
    val CAMPFIRE_POT_BLUE = campfirePotItem(CobblemonBlocks.BLUE_CAMPFIRE_POT, CampfirePotColor.BLUE)
    @JvmField
    val CAMPFIRE_POT_PINK = campfirePotItem(CobblemonBlocks.PINK_CAMPFIRE_POT, CampfirePotColor.PINK)
    @JvmField
    val CAMPFIRE_POT_BLACK = campfirePotItem(CobblemonBlocks.BLACK_CAMPFIRE_POT, CampfirePotColor.BLACK)
    @JvmField
    val CAMPFIRE_POT_WHITE = campfirePotItem(CobblemonBlocks.WHITE_CAMPFIRE_POT, CampfirePotColor.WHITE)

    @JvmField
    val HEARTY_GRAINS = compostableItem("hearty_grains", ItemNameBlockItem(CobblemonBlocks.HEARTY_GRAINS, Properties().rarity(Rarity.COMMON)))
    @JvmField
    val HEARTY_GRAIN_BALE = compostableBlockItem("hearty_grain_bale", CobblemonBlocks.HEARTY_GRAIN_BALE, 0.85f)

    @JvmField
    val TATAMI_BLOCK = blockItem("tatami_block", CobblemonBlocks.TATAMI_BLOCK)
    @JvmField
    val TATAMI_MAT = blockItem("tatami_mat", CobblemonBlocks.TATAMI_MAT)

    @JvmField
    val VIVICHOKE = compostableItem("vivichoke", CobblemonItem(Item.Properties()), 0.80f)

    @JvmField
    val VIVICHOKE_SEEDS = compostableItem("vivichoke_seeds", VivichokeItem(CobblemonBlocks.VIVICHOKE_SEEDS), 0.30f)

    @JvmField
    val RED_APRICORN = apricornItem("red", ApricornItem(CobblemonBlocks.RED_APRICORN))
    @JvmField
    val YELLOW_APRICORN = apricornItem("yellow", ApricornItem(CobblemonBlocks.YELLOW_APRICORN))
    @JvmField
    val GREEN_APRICORN = apricornItem("green", ApricornItem(CobblemonBlocks.GREEN_APRICORN))
    @JvmField
    val BLUE_APRICORN = apricornItem("blue", ApricornItem(CobblemonBlocks.BLUE_APRICORN))
    @JvmField
    val PINK_APRICORN = apricornItem("pink", ApricornItem(CobblemonBlocks.PINK_APRICORN))
    @JvmField
    val BLACK_APRICORN = apricornItem("black", ApricornItem(CobblemonBlocks.BLACK_APRICORN))
    @JvmField
    val WHITE_APRICORN = apricornItem("white", ApricornItem(CobblemonBlocks.WHITE_APRICORN))

    @JvmField
    val RED_APRICORN_SEED = apricornSeedItem("red", ApricornSeedItem(CobblemonBlocks.RED_APRICORN_SAPLING, CobblemonBlocks.RED_APRICORN))
    @JvmField
    val YELLOW_APRICORN_SEED = apricornSeedItem("yellow", ApricornSeedItem(CobblemonBlocks.YELLOW_APRICORN_SAPLING, CobblemonBlocks.YELLOW_APRICORN))
    @JvmField
    val GREEN_APRICORN_SEED = apricornSeedItem("green", ApricornSeedItem(CobblemonBlocks.GREEN_APRICORN_SAPLING, CobblemonBlocks.GREEN_APRICORN))
    @JvmField
    val BLUE_APRICORN_SEED = apricornSeedItem("blue", ApricornSeedItem(CobblemonBlocks.BLUE_APRICORN_SAPLING, CobblemonBlocks.BLUE_APRICORN))
    @JvmField
    val PINK_APRICORN_SEED = apricornSeedItem("pink", ApricornSeedItem(CobblemonBlocks.PINK_APRICORN_SAPLING, CobblemonBlocks.PINK_APRICORN))
    @JvmField
    val BLACK_APRICORN_SEED = apricornSeedItem("black", ApricornSeedItem(CobblemonBlocks.BLACK_APRICORN_SAPLING, CobblemonBlocks.BLACK_APRICORN))
    @JvmField
    val WHITE_APRICORN_SEED = apricornSeedItem("white", ApricornSeedItem(CobblemonBlocks.WHITE_APRICORN_SAPLING, CobblemonBlocks.WHITE_APRICORN))

    @JvmField
    val APRICORN_LOG = blockItem("apricorn_log", CobblemonBlocks.APRICORN_LOG)
    @JvmField
    val STRIPPED_APRICORN_LOG = blockItem("stripped_apricorn_log", CobblemonBlocks.STRIPPED_APRICORN_LOG)
    @JvmField
    val APRICORN_WOOD = blockItem("apricorn_wood", CobblemonBlocks.APRICORN_WOOD)
    @JvmField
    val STRIPPED_APRICORN_WOOD = blockItem("stripped_apricorn_wood", CobblemonBlocks.STRIPPED_APRICORN_WOOD)
    @JvmField
    val APRICORN_PLANKS = blockItem("apricorn_planks", CobblemonBlocks.APRICORN_PLANKS)
    @JvmField
    val APRICORN_LEAVES = compostableBlockItem("apricorn_leaves", CobblemonBlocks.APRICORN_LEAVES, 0.30f)
    @JvmField
    val APRICORN_BOAT = create("apricorn_boat", CobblemonBoatItem(CobblemonBoatType.APRICORN, false, Item.Properties().stacksTo(1)))
    @JvmField
    val APRICORN_CHEST_BOAT = create("apricorn_chest_boat", CobblemonBoatItem(CobblemonBoatType.APRICORN, true, Item.Properties().stacksTo(1)))

    @JvmField
    val APRICORN_DOOR = blockItem("apricorn_door", CobblemonBlocks.APRICORN_DOOR)
    @JvmField
    val APRICORN_TRAPDOOR = blockItem("apricorn_trapdoor", CobblemonBlocks.APRICORN_TRAPDOOR)
    @JvmField
    val APRICORN_FENCE = blockItem("apricorn_fence", CobblemonBlocks.APRICORN_FENCE)
    @JvmField
    val APRICORN_FENCE_GATE = blockItem("apricorn_fence_gate", CobblemonBlocks.APRICORN_FENCE_GATE)
    @JvmField
    val APRICORN_BUTTON = blockItem("apricorn_button", CobblemonBlocks.APRICORN_BUTTON)
    @JvmField
    val APRICORN_PRESSURE_PLATE = blockItem("apricorn_pressure_plate", CobblemonBlocks.APRICORN_PRESSURE_PLATE)
    @JvmField
    val APRICORN_SLAB = blockItem("apricorn_slab", CobblemonBlocks.APRICORN_SLAB)
    @JvmField
    val APRICORN_STAIRS = blockItem("apricorn_stairs", CobblemonBlocks.APRICORN_STAIRS)
    @JvmField
    val APRICORN_SIGN = this.create("apricorn_sign", SignItem(Item.Properties().stacksTo(16), CobblemonBlocks.APRICORN_SIGN, CobblemonBlocks.APRICORN_WALL_SIGN))
    @JvmField
    val APRICORN_HANGING_SIGN = this.create("apricorn_hanging_sign", HangingSignItem(CobblemonBlocks.APRICORN_HANGING_SIGN, CobblemonBlocks.APRICORN_WALL_HANGING_SIGN, Item.Properties().stacksTo(16)))
    @JvmField
    val GILDED_CHEST = this.create("gilded_chest", BlockItem(CobblemonBlocks.GILDED_CHEST, Item.Properties()))
    @JvmField
    val BLUE_GILDED_CHEST = this.create("blue_gilded_chest", BlockItem(CobblemonBlocks.BLUE_GILDED_CHEST, Item.Properties()))
    @JvmField
    val YELLOW_GILDED_CHEST = this.create("yellow_gilded_chest", BlockItem(CobblemonBlocks.YELLOW_GILDED_CHEST, Item.Properties()))
    @JvmField
    val PINK_GILDED_CHEST = this.create("pink_gilded_chest", BlockItem(CobblemonBlocks.PINK_GILDED_CHEST, Item.Properties()))
    @JvmField
    val BLACK_GILDED_CHEST = this.create("black_gilded_chest", BlockItem(CobblemonBlocks.BLACK_GILDED_CHEST, Item.Properties()))
    @JvmField
    val WHITE_GILDED_CHEST = this.create("white_gilded_chest", BlockItem(CobblemonBlocks.WHITE_GILDED_CHEST, Item.Properties()))
    @JvmField
    val GREEN_GILDED_CHEST = this.create("green_gilded_chest", BlockItem(CobblemonBlocks.GREEN_GILDED_CHEST, Item.Properties()))
    @JvmField
    val GIMMIGHOUL_CHEST = this.create("gimmighoul_chest", BlockItem(CobblemonBlocks.GIMMIGHOUL_CHEST, Item.Properties()))

    // Saccharines
    @JvmField
    val SACCHARINE_LOG = blockItem("saccharine_log", CobblemonBlocks.SACCHARINE_LOG)
    @JvmField
    val SACCHARINE_LOG_SLATHERED = blockItem("saccharine_log_slathered", CobblemonBlocks.SACCHARINE_LOG_SLATHERED)
    @JvmField
    val STRIPPED_SACCHARINE_LOG = blockItem("stripped_saccharine_log", CobblemonBlocks.STRIPPED_SACCHARINE_LOG)
    @JvmField
    val SACCHARINE_WOOD = blockItem("saccharine_wood", CobblemonBlocks.SACCHARINE_WOOD)
    @JvmField
    val STRIPPED_SACCHARINE_WOOD = blockItem("stripped_saccharine_wood", CobblemonBlocks.STRIPPED_SACCHARINE_WOOD)
    @JvmField
    val SACCHARINE_PLANKS = blockItem("saccharine_planks", CobblemonBlocks.SACCHARINE_PLANKS)
    @JvmField
    val SACCHARINE_LEAVES = compostableBlockItem("saccharine_leaves", CobblemonBlocks.SACCHARINE_LEAVES, 0.35f)
    @JvmField
    val SACCHARINE_BOAT = create("saccharine_boat", CobblemonBoatItem(CobblemonBoatType.SACCHARINE, false, Item.Properties().stacksTo(1)))
    @JvmField
    val SACCHARINE_CHEST_BOAT = create("saccharine_chest_boat", CobblemonBoatItem(CobblemonBoatType.SACCHARINE, true, Item.Properties().stacksTo(1)))

    @JvmField
    val SACCHARINE_DOOR = blockItem("saccharine_door", CobblemonBlocks.SACCHARINE_DOOR)
    @JvmField
    val SACCHARINE_TRAPDOOR = blockItem("saccharine_trapdoor", CobblemonBlocks.SACCHARINE_TRAPDOOR)
    @JvmField
    val SACCHARINE_FENCE = blockItem("saccharine_fence", CobblemonBlocks.SACCHARINE_FENCE)
    @JvmField
    val SACCHARINE_FENCE_GATE = blockItem("saccharine_fence_gate", CobblemonBlocks.SACCHARINE_FENCE_GATE)
    @JvmField
    val SACCHARINE_BUTTON = blockItem("saccharine_button", CobblemonBlocks.SACCHARINE_BUTTON)
    @JvmField
    val SACCHARINE_PRESSURE_PLATE = blockItem("saccharine_pressure_plate", CobblemonBlocks.SACCHARINE_PRESSURE_PLATE)
    @JvmField
    val SACCHARINE_SLAB = blockItem("saccharine_slab", CobblemonBlocks.SACCHARINE_SLAB)
    @JvmField
    val SACCHARINE_STAIRS = blockItem("saccharine_stairs", CobblemonBlocks.SACCHARINE_STAIRS)
    @JvmField
    val SACCHARINE_SIGN = this.create("saccharine_sign", SignItem(Item.Properties().stacksTo(16), CobblemonBlocks.SACCHARINE_SIGN, CobblemonBlocks.SACCHARINE_WALL_SIGN))
    @JvmField
    val SACCHARINE_HANGING_SIGN = this.create("saccharine_hanging_sign", HangingSignItem(CobblemonBlocks.SACCHARINE_HANGING_SIGN, CobblemonBlocks.SACCHARINE_WALL_HANGING_SIGN, Item.Properties().stacksTo(16)))
    @JvmField
    val SACCHARINE_SAPLING = compostableBlockItem("saccharine_sapling", CobblemonBlocks.SACCHARINE_SAPLING, 0.30f)

//    @JvmField
//    val BUGWORT = compostableBlockItem("bugwort", CobblemonBlocks.BUGWORT) // TODO after 1.7
    @JvmField
    val POKE_BAIT = noSettingsItem("poke_bait")

    @JvmField
    val POKE_CAKE = this.create("poke_cake", BlockItem(CobblemonBlocks.POKE_CAKE, Item.Properties().stacksTo(1)))
    @JvmField
    val POKE_SNACK = this.create("poke_snack", BlockItem(CobblemonBlocks.POKE_SNACK, Item.Properties().stacksTo(16)))

    val aprijuices = mutableListOf<AprijuiceItem>()
    @JvmField
    val APRIJUICE_RED = aprijuiceItem(Apricorn.RED)
    @JvmField
    val APRIJUICE_YELLOW = aprijuiceItem(Apricorn.YELLOW)
    @JvmField
    val APRIJUICE_GREEN = aprijuiceItem(Apricorn.GREEN)
    @JvmField
    val APRIJUICE_BLUE = aprijuiceItem(Apricorn.BLUE)
    @JvmField
    val APRIJUICE_PINK = aprijuiceItem(Apricorn.PINK)
    @JvmField
    val APRIJUICE_BLACK = aprijuiceItem(Apricorn.BLACK)
    @JvmField
    val APRIJUICE_WHITE = aprijuiceItem(Apricorn.WHITE)

    @JvmField
    val PONIGIRI = create("ponigiri", PonigiriItem())

    @JvmField
    val SINISTER_TEA = create("sinister_tea", SinisterTeaItem())

    // @JvmField
    // val POKE_PUFF = pokepuffItem("poke_puff")

    // FOODS
    @JvmField
    val SWEET_HEART = noSettingsItem("sweet_heart") // TODO make a SweetHeartItem class for breeding purposes

    @JvmField
    val TASTY_TAIL = create("tasty_tail", foodItem(3, 0.3f))

    @JvmField
    val PEWTER_CRUNCHIES = regionalFoodItem("pewter_crunchies", 64, 7, 0.3f, false)
    @JvmField
    val RAGE_CANDY_BAR = regionalFoodItem("rage_candy_bar", 64, 8, 0.2f, false)
    @JvmField
    val LAVA_COOKIE = regionalFoodItem("lava_cookie", 64, 2, 0.3f, false)
    @JvmField
    val OLD_GATEAU = regionalFoodItem("old_gateau", 64, 9, 0.1f, false)
    @JvmField
    val CASTELIACONE = regionalFoodItem("casteliacone", 64, 5, 0.3f, false)
    @JvmField
    val LUMIOSE_GALETTE = regionalFoodItem("lumiose_galette", 64, 4, 0.4f, false)
    @JvmField
    val BIG_MALASADA = regionalFoodItem("big_malasada", 64, 7, 0.3f, false)
    @JvmField
    val SMOKED_TAIL_CURRY = regionalFoodItem("smoked_tail_curry", 64, 10, 0.6f, false, ItemStack(Items.BOWL, 1))
    @JvmField
    val JUBILIFE_MUFFIN = regionalFoodItem("jubilife_muffin", 64, 7, 0.3f, false)
    @JvmField
    val OPEN_FACED_SANDWICH = regionalFoodItem("open_faced_sandwich", 64, 13, 0.5f, false)

    // todo we might need to wait on these for later? These impact battles and may be harder to do
    /*@JvmField
    val CHOICE_DUMPLING = noSettingsItem("choice_dumpling") // todo make a ChoiceDumpingItem class for battle purposes
    @JvmField
    val SWAP_SNACK = noSettingsItem("swap_snack") // todo make a SwapSnackItem class for battle purposes
    @JvmField
    val TWICE_SPICED_BEETROOT = noSettingsItem("twice_spiced_beetroot") // todo make a TwiceSpiceBeetrootItem class for battle purposes
*/
    @JvmField
    val POTATO_MOCHI = create("potato_mochi", CobblemonItem(Properties().stacksTo(64)
        .food(FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.4F)
            .build())))
    @JvmField
    val CANDIED_APPLE = create("candied_apple",  CobblemonItem(Properties().stacksTo(64)
        .food(FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.2F)
            .usingConvertsTo(Items.STICK)
            .build())))
    @JvmField
    val CANDIED_BERRY = create("candied_berry",  CobblemonItem(Properties().stacksTo(64)
        .food(FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.22F)
            .usingConvertsTo(Items.STICK)
            .build())))

    //@JvmField
    //val SCATTER_BANG = this.create("scatter_bang", ScatterBangItem(Item.Settings()))
    //@JvmField
    //val STICKY_GLOB = this.create("sticky_glob", StickyGlobItem(Item.Settings()))

    @JvmField
    val RESTORATION_TANK = blockItem("restoration_tank", CobblemonBlocks.RESTORATION_TANK)
    @JvmField
    val FOSSIL_ANALYZER = blockItem("fossil_analyzer", CobblemonBlocks.FOSSIL_ANALYZER)
    @JvmField
    val MONITOR = blockItem("monitor", CobblemonBlocks.MONITOR)
    @JvmField
    val HEALING_MACHINE = blockItem("healing_machine", CobblemonBlocks.HEALING_MACHINE, Rarity.UNCOMMON)
    @JvmField
    val PC = blockItem("pc", CobblemonBlocks.PC)
    @JvmField
    val PASTURE = blockItem("pasture", CobblemonBlocks.PASTURE)
    @JvmField
    val DISPLAY_CASE = blockItem("display_case", CobblemonBlocks.DISPLAY_CASE)
//    @JvmField
//    val INCENSE_SWEET = blockItem("incense_sweet", CobblemonBlocks.INCENSE_SWEET) // TODO after 1.7


    // Evolution items
    @JvmField val LINK_CABLE = create("link_cable", LinkCableItem())
    @JvmField val DRAGON_SCALE = noSettingsItem("dragon_scale")
    @JvmField val METAL_COAT = noSettingsItem("metal_coat")
    @JvmField val UPGRADE = noSettingsItem("upgrade")
    @JvmField val DUBIOUS_DISC = noSettingsItem("dubious_disc")
    @JvmField val DEEP_SEA_SCALE = noSettingsItem("deep_sea_scale")
    @JvmField val DEEP_SEA_TOOTH = noSettingsItem("deep_sea_tooth")
    @JvmField val ELECTIRIZER = noSettingsItem("electirizer")
    @JvmField val MAGMARIZER = noSettingsItem("magmarizer")
    @JvmField val OVAL_STONE = noSettingsItem("oval_stone")
    @JvmField val PROTECTOR = noSettingsItem("protector")
    @JvmField val REAPER_CLOTH = noSettingsItem("reaper_cloth")
    @JvmField val PRISM_SCALE = noSettingsItem("prism_scale")
    @JvmField val SACHET = noSettingsItem("sachet")
    @JvmField val WHIPPED_DREAM = create("whipped_dream", foodItem(8, 0.3f))
    @JvmField val STRAWBERRY_SWEET = create("strawberry_sweet", foodItem(6, 0.125f))
    @JvmField val LOVE_SWEET = create("love_sweet", foodItem(6, 0.125f))
    @JvmField val BERRY_SWEET = create("berry_sweet", foodItem(6, 0.125f))
    @JvmField val CLOVER_SWEET = create("clover_sweet", foodItem(6, 0.125f))
    @JvmField val FLOWER_SWEET = create("flower_sweet", foodItem(6, 0.125f))
    @JvmField val STAR_SWEET = create("star_sweet", foodItem(6, 0.125f))
    @JvmField val RIBBON_SWEET = create("ribbon_sweet", foodItem(6, 0.125f))
    @JvmField val CHIPPED_POT = noSettingsItem("chipped_pot")
    @JvmField val CRACKED_POT = noSettingsItem("cracked_pot")
    @JvmField val MASTERPIECE_TEACUP = noSettingsItem("masterpiece_teacup")
    @JvmField val UNREMARKABLE_TEACUP = noSettingsItem("unremarkable_teacup")
    @JvmField val SWEET_APPLE = compostableItem("sweet_apple", foodItem(4, 0.3f), 0.65f)
    @JvmField val TART_APPLE = compostableItem("tart_apple", foodItem(4, 0.3f), 0.65f)
    @JvmField val SYRUPY_APPLE = compostableItem("syrupy_apple", foodItem(4, 0.3f), 0.65f)
    @JvmField val GALARICA_CUFF = noSettingsItem("galarica_cuff")
    @JvmField val GALARICA_WREATH = noSettingsItem("galarica_wreath")
    @JvmField val BLACK_AUGURITE = noSettingsItem("black_augurite")
    @JvmField val PEAT_BLOCK = noSettingsItem("peat_block")
    @JvmField val RAZOR_CLAW = noSettingsItem("razor_claw")
    @JvmField val RAZOR_FANG = noSettingsItem("razor_fang")
    @JvmField val AUSPICIOUS_ARMOR = heldItem("auspicious_armor")
    @JvmField val MALICIOUS_ARMOR = heldItem("malicious_armor")
    @JvmField val SHELL_HELMET = heldItem("shell_helmet")
    @JvmField val METAL_ALLOY = noSettingsItem("metal_alloy")

    @JvmField val WHITE_PLAQUE = blockItem("white_plaque", CobblemonBlocks.WHITE_PLAQUE)
    @JvmField val LIGHT_GRAY_PLAQUE = blockItem("light_gray_plaque", CobblemonBlocks.LIGHT_GRAY_PLAQUE)
    @JvmField val GRAY_PLAQUE = blockItem("gray_plaque", CobblemonBlocks.GRAY_PLAQUE)
    @JvmField val BLACK_PLAQUE = blockItem("black_plaque", CobblemonBlocks.BLACK_PLAQUE)
    @JvmField val BROWN_PLAQUE = blockItem("brown_plaque", CobblemonBlocks.BROWN_PLAQUE)
    @JvmField val RED_PLAQUE = blockItem("red_plaque", CobblemonBlocks.RED_PLAQUE)
    @JvmField val ORANGE_PLAQUE = blockItem("orange_plaque", CobblemonBlocks.ORANGE_PLAQUE)
    @JvmField val YELLOW_PLAQUE = blockItem("yellow_plaque", CobblemonBlocks.YELLOW_PLAQUE)
    @JvmField val LIME_PLAQUE = blockItem("lime_plaque", CobblemonBlocks.LIME_PLAQUE)
    @JvmField val GREEN_PLAQUE = blockItem("green_plaque", CobblemonBlocks.GREEN_PLAQUE)
    @JvmField val CYAN_PLAQUE = blockItem("cyan_plaque", CobblemonBlocks.CYAN_PLAQUE)
    @JvmField val LIGHT_BLUE_PLAQUE = blockItem("light_blue_plaque", CobblemonBlocks.LIGHT_BLUE_PLAQUE)
    @JvmField val BLUE_PLAQUE = blockItem("blue_plaque", CobblemonBlocks.BLUE_PLAQUE)
    @JvmField val PURPLE_PLAQUE = blockItem("purple_plaque", CobblemonBlocks.PURPLE_PLAQUE)
    @JvmField val MAGENTA_PLAQUE = blockItem("magenta_plaque", CobblemonBlocks.MAGENTA_PLAQUE)
    @JvmField val PINK_PLAQUE = blockItem("pink_plaque", CobblemonBlocks.PINK_PLAQUE)

    private val berries = mutableMapOf<ResourceLocation, BerryItem>()
    // Plants
    @JvmField val CHERI_BERRY = berryItem("cheri", StatusCuringBerryItem(CobblemonBlocks.CHERI_BERRY, Statuses.PARALYSIS))
    @JvmField val CHESTO_BERRY = berryItem("chesto", StatusCuringBerryItem(CobblemonBlocks.CHESTO_BERRY, Statuses.SLEEP))
    @JvmField val PECHA_BERRY = berryItem("pecha", StatusCuringBerryItem(CobblemonBlocks.PECHA_BERRY, Statuses.POISON, Statuses.POISON_BADLY))
    @JvmField val RAWST_BERRY = berryItem("rawst", StatusCuringBerryItem(CobblemonBlocks.RAWST_BERRY, Statuses.BURN))
    @JvmField val ASPEAR_BERRY = berryItem("aspear", StatusCuringBerryItem(CobblemonBlocks.ASPEAR_BERRY, Statuses.FROZEN))
    @JvmField val LEPPA_BERRY = berryItem("leppa", PPRestoringBerryItem(CobblemonBlocks.LEPPA_BERRY) { CobblemonMechanics.berries.ppRestoreAmount })
    @JvmField val ORAN_BERRY = berryItem("oran", HealingBerryItem(CobblemonBlocks.ORAN_BERRY) { CobblemonMechanics.berries.oranRestoreAmount })
    @JvmField val PERSIM_BERRY = berryItem("persim", StatusCuringBerryItem(CobblemonBlocks.PERSIM_BERRY, Statuses.CONFUSE))
    @JvmField val LUM_BERRY = berryItem("lum", StatusCuringBerryItem(CobblemonBlocks.LUM_BERRY, *(Statuses.getPersistentStatuses().toTypedArray() + Statuses.CONFUSE)))
    @JvmField val SITRUS_BERRY = berryItem("sitrus", HealingBerryItem(CobblemonBlocks.SITRUS_BERRY) { CobblemonMechanics.berries.sitrusHealAmount })
    @JvmField val EGGANT_BERRY = berryItem("eggant", StatusCuringBerryItem(CobblemonBlocks.EGGANT_BERRY, Statuses.ATTRACT))
    @JvmField val FIGY_BERRY = berryItem("figy", PortionHealingBerryItem(CobblemonBlocks.FIGY_BERRY, true) { CobblemonMechanics.berries.portionHealRatio })
    @JvmField val WIKI_BERRY = berryItem("wiki", PortionHealingBerryItem(CobblemonBlocks.WIKI_BERRY, true) { CobblemonMechanics.berries.portionHealRatio })
    @JvmField val MAGO_BERRY = berryItem("mago", PortionHealingBerryItem(CobblemonBlocks.MAGO_BERRY, true) { CobblemonMechanics.berries.portionHealRatio })
    @JvmField val AGUAV_BERRY = berryItem("aguav", PortionHealingBerryItem(CobblemonBlocks.AGUAV_BERRY, true) { CobblemonMechanics.berries.portionHealRatio })
    @JvmField val IAPAPA_BERRY = berryItem("iapapa", PortionHealingBerryItem(CobblemonBlocks.IAPAPA_BERRY, true) { CobblemonMechanics.berries.portionHealRatio })
    @JvmField val RAZZ_BERRY = berryItem("razz", CobblemonBlocks.RAZZ_BERRY)
    @JvmField val BLUK_BERRY = berryItem("bluk", CobblemonBlocks.BLUK_BERRY)
    @JvmField val NANAB_BERRY = berryItem("nanab", CobblemonBlocks.NANAB_BERRY)
    @JvmField val WEPEAR_BERRY = berryItem("wepear", CobblemonBlocks.WEPEAR_BERRY)
    @JvmField val PINAP_BERRY = berryItem("pinap", CobblemonBlocks.PINAP_BERRY)
    @JvmField val POMEG_BERRY = berryItem("pomeg", FriendshipRaisingBerryItem(CobblemonBlocks.POMEG_BERRY, Stats.HP))
    @JvmField val KELPSY_BERRY = berryItem("kelpsy", FriendshipRaisingBerryItem(CobblemonBlocks.KELPSY_BERRY, Stats.ATTACK))
    @JvmField val QUALOT_BERRY = berryItem("qualot", FriendshipRaisingBerryItem(CobblemonBlocks.QUALOT_BERRY, Stats.DEFENCE))
    @JvmField val HONDEW_BERRY = berryItem("hondew", FriendshipRaisingBerryItem(CobblemonBlocks.HONDEW_BERRY, Stats.SPECIAL_ATTACK))
    @JvmField val GREPA_BERRY = berryItem("grepa", FriendshipRaisingBerryItem(CobblemonBlocks.GREPA_BERRY, Stats.SPECIAL_DEFENCE))
    @JvmField val TAMATO_BERRY = berryItem("tamato", FriendshipRaisingBerryItem(CobblemonBlocks.TAMATO_BERRY, Stats.SPEED))
    @JvmField val TOUGA_BERRY = berryItem("touga", CobblemonBlocks.TOUGA_BERRY)
    @JvmField val CORNN_BERRY = berryItem("cornn", CobblemonBlocks.CORNN_BERRY)
    @JvmField val MAGOST_BERRY = berryItem("magost", CobblemonBlocks.MAGOST_BERRY)
    @JvmField val RABUTA_BERRY = berryItem("rabuta", CobblemonBlocks.RABUTA_BERRY)
    @JvmField val NOMEL_BERRY = berryItem("nomel", CobblemonBlocks.NOMEL_BERRY)
    @JvmField val SPELON_BERRY = berryItem("spelon", CobblemonBlocks.SPELON_BERRY)
    @JvmField val PAMTRE_BERRY = berryItem("pamtre", CobblemonBlocks.PAMTRE_BERRY)
    @JvmField val WATMEL_BERRY = berryItem("watmel", CobblemonBlocks.WATMEL_BERRY)
    @JvmField val DURIN_BERRY = berryItem("durin", CobblemonBlocks.DURIN_BERRY)
    @JvmField val BELUE_BERRY = berryItem("belue", CobblemonBlocks.BELUE_BERRY)
    @JvmField val OCCA_BERRY = berryItem("occa", CobblemonBlocks.OCCA_BERRY)
    @JvmField val PASSHO_BERRY = berryItem("passho", CobblemonBlocks.PASSHO_BERRY)
    @JvmField val WACAN_BERRY = berryItem("wacan", CobblemonBlocks.WACAN_BERRY)
    @JvmField val RINDO_BERRY = berryItem("rindo", CobblemonBlocks.RINDO_BERRY)
    @JvmField val YACHE_BERRY = berryItem("yache", CobblemonBlocks.YACHE_BERRY)
    @JvmField val CHOPLE_BERRY = berryItem("chople", CobblemonBlocks.CHOPLE_BERRY)
    @JvmField val KEBIA_BERRY = berryItem("kebia", CobblemonBlocks.KEBIA_BERRY)
    @JvmField val SHUCA_BERRY = berryItem("shuca", CobblemonBlocks.SHUCA_BERRY)
    @JvmField val COBA_BERRY = berryItem("coba", CobblemonBlocks.COBA_BERRY)
    @JvmField val PAYAPA_BERRY = berryItem("payapa", CobblemonBlocks.PAYAPA_BERRY)
    @JvmField val TANGA_BERRY = berryItem("tanga", CobblemonBlocks.TANGA_BERRY)
    @JvmField val CHARTI_BERRY = berryItem("charti", CobblemonBlocks.CHARTI_BERRY)
    @JvmField val KASIB_BERRY = berryItem("kasib", CobblemonBlocks.KASIB_BERRY)
    @JvmField val HABAN_BERRY = berryItem("haban", CobblemonBlocks.HABAN_BERRY)
    @JvmField val COLBUR_BERRY = berryItem("colbur", CobblemonBlocks.COLBUR_BERRY)
    @JvmField val BABIRI_BERRY = berryItem("babiri", CobblemonBlocks.BABIRI_BERRY)
    @JvmField val CHILAN_BERRY = berryItem("chilan", CobblemonBlocks.CHILAN_BERRY)
    @JvmField val LIECHI_BERRY = berryItem("liechi", CobblemonBlocks.LIECHI_BERRY)
    @JvmField val GANLON_BERRY = berryItem("ganlon", CobblemonBlocks.GANLON_BERRY)
    @JvmField val SALAC_BERRY = berryItem("salac", CobblemonBlocks.SALAC_BERRY)
    @JvmField val PETAYA_BERRY = berryItem("petaya", CobblemonBlocks.PETAYA_BERRY)
    @JvmField val APICOT_BERRY = berryItem("apicot", CobblemonBlocks.APICOT_BERRY)
    @JvmField val LANSAT_BERRY = berryItem("lansat", CobblemonBlocks.LANSAT_BERRY)
    @JvmField val STARF_BERRY = berryItem("starf", CobblemonBlocks.STARF_BERRY)
    @JvmField val ENIGMA_BERRY = berryItem("enigma", CobblemonBlocks.ENIGMA_BERRY)
    @JvmField val MICLE_BERRY = berryItem("micle", CobblemonBlocks.MICLE_BERRY)
    @JvmField val CUSTAP_BERRY = berryItem("custap", CobblemonBlocks.CUSTAP_BERRY)
    @JvmField val JABOCA_BERRY = berryItem("jaboca", CobblemonBlocks.JABOCA_BERRY)
    @JvmField val ROWAP_BERRY = berryItem("rowap", CobblemonBlocks.ROWAP_BERRY)
    @JvmField val ROSELI_BERRY = berryItem("roseli", CobblemonBlocks.ROSELI_BERRY)
    @JvmField val KEE_BERRY = berryItem("kee", CobblemonBlocks.KEE_BERRY)
    @JvmField val MARANGA_BERRY = berryItem("maranga", CobblemonBlocks.MARANGA_BERRY)
    @JvmField val HOPO_BERRY = berryItem("hopo", PPRestoringBerryItem(CobblemonBlocks.HOPO_BERRY) { CobblemonMechanics.berries.ppRestoreAmount })

    @JvmField val BERRY_JUICE = this.create("berry_juice", BerryJuiceItem())

    @JvmField
    val GALARICA_NUTS = compostableItemNameBlockItem("galarica_nuts", CobblemonBlocks.GALARICA_NUT_BUSH)

    // Hyper Training Items
    val hyperTrainingItems = mutableListOf<HyperTrainingItem>()
    @JvmField
    val HEALTH_CANDY = hyperTrainingItem("health_candy", 1, setOf(Stats.HP), 0..IVs.MAX_VALUE)
    @JvmField
    val MIGHTY_CANDY = hyperTrainingItem("mighty_candy", 1, setOf(Stats.ATTACK), 0..IVs.MAX_VALUE)
    @JvmField
    val TOUGH_CANDY = hyperTrainingItem("tough_candy", 1, setOf(Stats.DEFENCE), 0..IVs.MAX_VALUE)
    @JvmField
    val SMART_CANDY = hyperTrainingItem("smart_candy", 1, setOf(Stats.SPECIAL_ATTACK), 0..IVs.MAX_VALUE)
    @JvmField
    val COURAGE_CANDY = hyperTrainingItem("courage_candy", 1, setOf(Stats.SPECIAL_DEFENCE), 0..IVs.MAX_VALUE)
    @JvmField
    val QUICK_CANDY = hyperTrainingItem("quick_candy", 1, setOf(Stats.SPEED), 0..IVs.MAX_VALUE)
    @JvmField
    val SICKLY_CANDY = hyperTrainingItem("sickly_candy", -1, setOf(Stats.HP), 0..IVs.MAX_VALUE)
    @JvmField
    val WEAK_CANDY = hyperTrainingItem("weak_candy", -1, setOf(Stats.ATTACK), 0..IVs.MAX_VALUE)
    @JvmField
    val BRITTLE_CANDY = hyperTrainingItem("brittle_candy", -1, setOf(Stats.DEFENCE), 0..IVs.MAX_VALUE)
    @JvmField
    val NUMB_CANDY = hyperTrainingItem("numb_candy", -1, setOf(Stats.SPECIAL_ATTACK), 0..IVs.MAX_VALUE)
    @JvmField
    val COWARD_CANDY = hyperTrainingItem("coward_candy", -1, setOf(Stats.SPECIAL_DEFENCE), 0..IVs.MAX_VALUE)
    @JvmField
    val SLOW_CANDY = hyperTrainingItem("slow_candy", -1, setOf(Stats.SPEED), 0..IVs.MAX_VALUE)

    // Medicine
    @JvmField
    val RARE_CANDY = candyItem("rare_candy", Rarity.RARE) { _, pokemon -> pokemon.getExperienceToNextLevel() }
    @JvmField
    val EXPERIENCE_CANDY_XS = candyItem("exp_candy_xs") { _, _ -> CandyItem.DEFAULT_XS_CANDY_YIELD }
    @JvmField
    val EXPERIENCE_CANDY_S = candyItem("exp_candy_s") { _, _ -> CandyItem.DEFAULT_S_CANDY_YIELD }
    @JvmField
    val EXPERIENCE_CANDY_M = candyItem("exp_candy_m") { _, _ -> CandyItem.DEFAULT_M_CANDY_YIELD }
    @JvmField
    val EXPERIENCE_CANDY_L = candyItem("exp_candy_l") { _, _ -> CandyItem.DEFAULT_L_CANDY_YIELD }
    @JvmField
    val EXPERIENCE_CANDY_XL = candyItem("exp_candy_xl") { _, _ -> CandyItem.DEFAULT_XL_CANDY_YIELD }
    @JvmField
    val CALCIUM = create("calcium", VitaminItem(Stats.SPECIAL_ATTACK))
    @JvmField
    val CARBOS = create("carbos", VitaminItem(Stats.SPEED))
    @JvmField
    val HP_UP = create("hp_up", VitaminItem(Stats.HP))
    @JvmField
    val IRON = create("iron", VitaminItem(Stats.DEFENCE))
    @JvmField
    val PROTEIN = create("protein", VitaminItem(Stats.ATTACK))
    @JvmField
    val ZINC = create("zinc", VitaminItem(Stats.SPECIAL_DEFENCE))
    @JvmField
    val HEALTH_MOCHI = create("health_mochi", MochiItem(Stats.HP))
    @JvmField
    val MUSCLE_MOCHI = create("muscle_mochi", MochiItem(Stats.ATTACK))
    @JvmField
    val RESIST_MOCHI = create("resist_mochi", MochiItem(Stats.DEFENCE))
    @JvmField
    val GENIUS_MOCHI = create("genius_mochi", MochiItem(Stats.SPECIAL_ATTACK))
    @JvmField
    val CLEVER_MOCHI = create("clever_mochi", MochiItem(Stats.SPECIAL_DEFENCE))
    @JvmField
    val SWIFT_MOCHI = create("swift_mochi", MochiItem(Stats.SPEED))
    @JvmField
    val FRESH_START_MOCHI = create("fresh_start_mochi", FreshStartMochiItem())
    @JvmField
    val GENIUS_FEATHER = create("genius_feather", FeatherItem(Stats.SPECIAL_ATTACK))
    @JvmField
    val SWIFT_FEATHER = create("swift_feather", FeatherItem(Stats.SPEED))
    @JvmField
    val HEALTH_FEATHER = create("health_feather", FeatherItem(Stats.HP))
    @JvmField
    val RESIST_FEATHER = create("resist_feather", FeatherItem(Stats.DEFENCE))
    @JvmField
    val MUSCLE_FEATHER = create("muscle_feather", FeatherItem(Stats.ATTACK))
    @JvmField
    val CLEVER_FEATHER = create("clever_feather", FeatherItem(Stats.SPECIAL_DEFENCE))
    @JvmField
    val MEDICINAL_LEEK = heldItem("medicinal_leek", MedicinalLeekItem(CobblemonBlocks.MEDICINAL_LEEK, Item.Properties().food(FoodProperties.Builder().fast().nutrition(1).saturationModifier(0.2f).build())), "leek")
    @JvmField
    val ROASTED_LEEK = compostableItem("roasted_leek", CobblemonItem(Item.Properties().food(FoodProperties.Builder().fast().nutrition(3).saturationModifier(0.2f).build())), 0.85f)
    @JvmField
    val VIVICHOKE_DIP = create("vivichoke_dip", object : CobblemonItem(Properties().stacksTo(1)
        .food(FoodProperties.Builder()
            .nutrition(10)
            .saturationModifier(0.6F)
            .effect(MobEffectInstance(MobEffects.ABSORPTION, 900, 0), 1F)
            .alwaysEdible()
            .usingConvertsTo(Items.BOWL)
            .build())) {
        override fun finishUsingItem(stack: ItemStack, world: Level, user: LivingEntity): ItemStack {
            user.removeAllEffects()
            return super.finishUsingItem(stack, world, user)
        }
    })
    @JvmField
    val ENERGY_ROOT = compostableItem("energy_root", EnergyRootItem(CobblemonBlocks.ENERGY_ROOT, Properties()), 0.65f)
    @JvmField
    val REVIVAL_HERB = compostableItem("revival_herb", RevivalHerbItem(CobblemonBlocks.REVIVAL_HERB))
    @JvmField
    val PEP_UP_FLOWER = compostableBlockItem("pep_up_flower", CobblemonBlocks.PEP_UP_FLOWER)
    @JvmField
    val MEDICINAL_BREW = noSettingsItem("medicinal_brew")
    @JvmField
    val REMEDY = compostableItem("remedy", RemedyItem(RemedyItem.NORMAL), 0.65f)
    @JvmField
    val FINE_REMEDY = compostableItem("fine_remedy", RemedyItem(RemedyItem.FINE), 0.85f)
    @JvmField
    val SUPERB_REMEDY = compostableItem("superb_remedy", RemedyItem(RemedyItem.SUPERB), 1f)
    @JvmField
    val MOOMOO_MILK = create("moomoo_milk", MoomooMilk(Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo((16))))

    @JvmField
    val POTION = create("potion", PotionItem(PotionType.POTION, CobblemonBlocks.POTION))
    @JvmField
    val SUPER_POTION = create("super_potion", PotionItem(PotionType.SUPER_POTION, CobblemonBlocks.SUPER_POTION))
    @JvmField
    val HYPER_POTION = create("hyper_potion", PotionItem(PotionType.HYPER_POTION, CobblemonBlocks.HYPER_POTION))
    @JvmField
    val MAX_POTION = create("max_potion", PotionItem(PotionType.MAX_POTION, CobblemonBlocks.MAX_POTION))
    @JvmField
    val FULL_RESTORE = create("full_restore", PotionItem(PotionType.FULL_RESTORE, CobblemonBlocks.FULL_RESTORE))

    @JvmField
    val HEAL_POWDER = create("heal_powder", HealPowderItem())
    @JvmField
    val LEEK_AND_POTATO_STEW = create("leek_and_potato_stew", CobblemonItem(Item.Properties().food(FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).usingConvertsTo(Items.BOWL).build()).stacksTo(1)))
    @JvmField
    val REVIVE = create("revive", ReviveItem(max = false))
    @JvmField
    val MAX_REVIVE = create("max_revive", ReviveItem(max = true))
    @JvmField
    val PP_UP = create("pp_up", PPUpItem(1))
    @JvmField
    val PP_MAX = create("pp_max", PPUpItem(3))

    @JvmField
    val RED_MINT_SEEDS = mintSeed("red", MintType.RED.getCropBlock())
    @JvmField
    val RED_MINT_LEAF = mintLeaf("red", MintLeafItem(MintType.RED))
    @JvmField
    val BLUE_MINT_SEEDS = mintSeed("blue", MintType.BLUE.getCropBlock())
    @JvmField
    val BLUE_MINT_LEAF = mintLeaf("blue", MintLeafItem(MintType.BLUE))
    @JvmField
    val CYAN_MINT_SEEDS = mintSeed("cyan", MintType.CYAN.getCropBlock())
    @JvmField
    val CYAN_MINT_LEAF = mintLeaf("cyan", MintLeafItem(MintType.CYAN))
    @JvmField
    val PINK_MINT_SEEDS = mintSeed("pink", MintType.PINK.getCropBlock())
    @JvmField
    val PINK_MINT_LEAF = mintLeaf("pink", MintLeafItem(MintType.PINK))
    @JvmField
    val GREEN_MINT_SEEDS = mintSeed("green", MintType.GREEN.getCropBlock())
    @JvmField
    val GREEN_MINT_LEAF = mintLeaf("green", MintLeafItem(MintType.GREEN))
    @JvmField
    val WHITE_MINT_SEEDS = mintSeed("white", MintType.WHITE.getCropBlock())
    @JvmField
    val WHITE_MINT_LEAF = mintLeaf("white", MintLeafItem(MintType.WHITE))

    val mints = mutableMapOf<String, MintItem>()

    @JvmField
    val LONELY_MINT = mintItem("lonely_mint", MintItem(Natures.LONELY))
    @JvmField
    val ADAMANT_MINT = mintItem("adamant_mint", MintItem(Natures.ADAMANT))
    @JvmField
    val NAUGHTY_MINT = mintItem("naughty_mint", MintItem(Natures.NAUGHTY))
    @JvmField
    val BRAVE_MINT = mintItem("brave_mint", MintItem(Natures.BRAVE))
    @JvmField
    val BOLD_MINT = mintItem("bold_mint", MintItem(Natures.BOLD))
    @JvmField
    val IMPISH_MINT = mintItem("impish_mint", MintItem(Natures.IMPISH))
    @JvmField
    val LAX_MINT = mintItem("lax_mint", MintItem(Natures.LAX))
    @JvmField
    val RELAXED_MINT = mintItem("relaxed_mint", MintItem(Natures.RELAXED))
    @JvmField
    val MODEST_MINT = mintItem("modest_mint", MintItem(Natures.MODEST))
    @JvmField
    val MILD_MINT = mintItem("mild_mint", MintItem(Natures.MILD))
    @JvmField
    val RASH_MINT = mintItem("rash_mint", MintItem(Natures.RASH))
    @JvmField
    val QUIET_MINT = mintItem("quiet_mint", MintItem(Natures.QUIET))
    @JvmField
    val CALM_MINT = mintItem("calm_mint", MintItem(Natures.CALM))
    @JvmField
    val GENTLE_MINT = mintItem("gentle_mint", MintItem(Natures.GENTLE))
    @JvmField
    val CAREFUL_MINT = mintItem("careful_mint", MintItem(Natures.CAREFUL))
    @JvmField
    val SASSY_MINT = mintItem("sassy_mint", MintItem(Natures.SASSY))
    @JvmField
    val TIMID_MINT = mintItem("timid_mint", MintItem(Natures.TIMID))
    @JvmField
    val HASTY_MINT = mintItem("hasty_mint", MintItem(Natures.HASTY))
    @JvmField
    val JOLLY_MINT = mintItem("jolly_mint", MintItem(Natures.JOLLY))
    @JvmField
    val NAIVE_MINT = mintItem("naive_mint", MintItem(Natures.NAIVE))
    @JvmField
    val SERIOUS_MINT = mintItem("serious_mint", MintItem(Natures.SERIOUS))

    @JvmField val X_ACCURACY = create("x_${Stats.ACCURACY.identifier.path}", XStatItem(Stats.ACCURACY, block = CobblemonBlocks.X_ACCURACY))
    @JvmField val X_ATTACK = create("x_${Stats.ATTACK.identifier.path}", XStatItem(Stats.ATTACK, block = CobblemonBlocks.X_ATTACK))
    @JvmField val X_DEFENSE = create("x_${Stats.DEFENCE.identifier.path}", XStatItem(Stats.DEFENCE, block = CobblemonBlocks.X_DEFENSE))
    @JvmField val X_SP_ATK = create("x_${Stats.SPECIAL_ATTACK.identifier.path}", XStatItem(Stats.SPECIAL_ATTACK, block = CobblemonBlocks.X_SP_ATK))
    @JvmField val X_SP_DEF = create("x_${Stats.SPECIAL_DEFENCE.identifier.path}", XStatItem(Stats.SPECIAL_DEFENCE, block = CobblemonBlocks.X_SP_DEF))
    @JvmField val X_SPEED = create("x_${Stats.SPEED.identifier.path}", XStatItem(Stats.SPEED, block = CobblemonBlocks.X_SPEED))

    @JvmField val DIRE_HIT = create("dire_hit", DireHitItem(CobblemonBlocks.DIRE_HIT))
    @JvmField val GUARD_SPEC = create("guard_spec", GuardSpecItem(CobblemonBlocks.GUARD_SPEC))

    @JvmField val FULL_HEAL = create("full_heal", StatusCureItem("item.cobblemon.full_heal", block = CobblemonBlocks.FULL_HEAL))
    @JvmField val ANTIDOTE = create("antidote", StatusCureItem("item.cobblemon.antidote", Statuses.POISON, Statuses.POISON_BADLY, block = CobblemonBlocks.ANTIDOTE))
    @JvmField val AWAKENING = create("awakening", StatusCureItem("item.cobblemon.awakening", Statuses.SLEEP, block = CobblemonBlocks.AWAKENING))
    @JvmField val BURN_HEAL = create("burn_heal", StatusCureItem("item.cobblemon.burn_heal", Statuses.BURN, block = CobblemonBlocks.BURN_HEAL))
    @JvmField val ICE_HEAL = create("ice_heal", StatusCureItem("item.cobblemon.ice_heal", Statuses.FROZEN, block = CobblemonBlocks.ICE_HEAL))
    @JvmField val PARALYZE_HEAL = create("paralyze_heal", StatusCureItem("item.cobblemon.paralyze_heal", Statuses.PARALYSIS, block = CobblemonBlocks.PARALYZE_HEAL))

    @JvmField val ETHER = create("ether", EtherItem(max = false, CobblemonBlocks.ETHER))
    @JvmField val MAX_ETHER = create("max_ether", EtherItem(max = true, CobblemonBlocks.MAX_ETHER))
    @JvmField val ELIXIR = create("elixir", ElixirItem(max = false, CobblemonBlocks.ELIXIR))
    @JvmField val MAX_ELIXIR = create("max_elixir", ElixirItem(max = true, CobblemonBlocks.MAX_ELIXIR))

    @JvmField
    val ABILITY_CAPSULE = this.create("ability_capsule", AbilityChangeItem(AbilityChanger.COMMON_ABILITY))
    @JvmField
    val ABILITY_PATCH = this.create("ability_patch", AbilityChangeItem(AbilityChanger.HIDDEN_ABILITY))

    /**
     * Evolution Ores and Stones
     */
    @JvmField
    val DAWN_STONE_ORE = blockItem("dawn_stone_ore", CobblemonBlocks.DAWN_STONE_ORE)
    @JvmField
    val DUSK_STONE_ORE = blockItem("dusk_stone_ore", CobblemonBlocks.DUSK_STONE_ORE)
    @JvmField
    val FIRE_STONE_ORE = blockItem("fire_stone_ore", CobblemonBlocks.FIRE_STONE_ORE)
    @JvmField
    val ICE_STONE_ORE = blockItem("ice_stone_ore", CobblemonBlocks.ICE_STONE_ORE)
    @JvmField
    val LEAF_STONE_ORE = blockItem("leaf_stone_ore", CobblemonBlocks.LEAF_STONE_ORE)
    @JvmField
    val MOON_STONE_ORE = blockItem("moon_stone_ore", CobblemonBlocks.MOON_STONE_ORE)
    @JvmField
    val SHINY_STONE_ORE = blockItem("shiny_stone_ore", CobblemonBlocks.SHINY_STONE_ORE)
    @JvmField
    val SUN_STONE_ORE = blockItem("sun_stone_ore", CobblemonBlocks.SUN_STONE_ORE)
    @JvmField
    val TERRACOTTA_SUN_STONE_ORE = blockItem("terracotta_sun_stone_ore", CobblemonBlocks.TERRACOTTA_SUN_STONE_ORE)
    @JvmField
    val THUNDER_STONE_ORE = blockItem("thunder_stone_ore", CobblemonBlocks.THUNDER_STONE_ORE)
    @JvmField
    val WATER_STONE_ORE = blockItem("water_stone_ore", CobblemonBlocks.WATER_STONE_ORE)
    @JvmField
    val DEEPSLATE_DAWN_STONE_ORE = blockItem("deepslate_dawn_stone_ore", CobblemonBlocks.DEEPSLATE_DAWN_STONE_ORE)
    @JvmField
    val DEEPSLATE_DUSK_STONE_ORE = blockItem("deepslate_dusk_stone_ore", CobblemonBlocks.DEEPSLATE_DUSK_STONE_ORE)
    @JvmField
    val DEEPSLATE_FIRE_STONE_ORE = blockItem("deepslate_fire_stone_ore", CobblemonBlocks.DEEPSLATE_FIRE_STONE_ORE)
    @JvmField
    val DEEPSLATE_ICE_STONE_ORE = blockItem("deepslate_ice_stone_ore", CobblemonBlocks.DEEPSLATE_ICE_STONE_ORE)
    @JvmField
    val DEEPSLATE_LEAF_STONE_ORE = blockItem("deepslate_leaf_stone_ore", CobblemonBlocks.DEEPSLATE_LEAF_STONE_ORE)
    @JvmField
    val DEEPSLATE_MOON_STONE_ORE = blockItem("deepslate_moon_stone_ore", CobblemonBlocks.DEEPSLATE_MOON_STONE_ORE)
    @JvmField
    val DEEPSLATE_SHINY_STONE_ORE = blockItem("deepslate_shiny_stone_ore", CobblemonBlocks.DEEPSLATE_SHINY_STONE_ORE)
    @JvmField
    val DEEPSLATE_SUN_STONE_ORE = blockItem("deepslate_sun_stone_ore", CobblemonBlocks.DEEPSLATE_SUN_STONE_ORE)
    @JvmField
    val DEEPSLATE_THUNDER_STONE_ORE = blockItem("deepslate_thunder_stone_ore", CobblemonBlocks.DEEPSLATE_THUNDER_STONE_ORE)
    @JvmField
    val DEEPSLATE_WATER_STONE_ORE = blockItem("deepslate_water_stone_ore", CobblemonBlocks.DEEPSLATE_WATER_STONE_ORE)
    @JvmField
    val DRIPSTONE_MOON_STONE_ORE = blockItem("dripstone_moon_stone_ore", CobblemonBlocks.DRIPSTONE_MOON_STONE_ORE)
    @JvmField
    val NETHER_FIRE_STONE_ORE = blockItem("nether_fire_stone_ore", CobblemonBlocks.NETHER_FIRE_STONE_ORE)

    @JvmField
    val DAWN_STONE = noSettingsItem("dawn_stone")
    @JvmField
    val DUSK_STONE = noSettingsItem("dusk_stone")
    @JvmField
    val FIRE_STONE = noSettingsItem("fire_stone")
    @JvmField
    val ICE_STONE = noSettingsItem("ice_stone")
    @JvmField
    val LEAF_STONE = noSettingsItem("leaf_stone")
    @JvmField
    val MOON_STONE = noSettingsItem("moon_stone")
    @JvmField
    val SHINY_STONE = noSettingsItem("shiny_stone")
    @JvmField
    val SUN_STONE = noSettingsItem("sun_stone")
    @JvmField
    val THUNDER_STONE = noSettingsItem("thunder_stone")
    @JvmField
    val WATER_STONE = noSettingsItem("water_stone")

    val wearables = mutableListOf<WearableItem>()
    //Wearable items (these items should have a corresponding 3D model)
    @JvmField val BLACK_GLASSES = wearableItem("black_glasses")
    @JvmField val CHOICE_BAND = wearableItem("choice_band")
    @JvmField val CHOICE_SPECS = wearableItem("choice_specs")
    @JvmField val EXP_SHARE = wearableItem("exp_share")
    @JvmField val FOCUS_BAND = wearableItem("focus_band")
    @JvmField val KINGS_ROCK = wearableItem("kings_rock")
    @JvmField val MUSCLE_BAND = wearableItem("muscle_band")
    @JvmField val ROCKY_HELMET = wearableItem("rocky_helmet")
    @JvmField val SAFETY_GOGGLES = wearableItem("safety_goggles")
    @JvmField val WISE_GLASSES = wearableItem("wise_glasses")

    // Held Items
    @JvmField
    val ABILITY_SHIELD = heldItem("ability_shield")
    @JvmField
    val ABSORB_BULB = heldItem("absorb_bulb")
    @JvmField
    val AIR_BALLOON = heldItem("air_balloon")
    @JvmField
    val ASSAULT_VEST = heldItem("assault_vest")
    @JvmField
    val BIG_ROOT = compostableBlockItem("big_root", CobblemonBlocks.BIG_ROOT, 0.30f)
    @JvmField
    val BINDING_BAND = heldItem("binding_band")
    @JvmField
    val BLACK_BELT = heldItem("black_belt")
    @JvmField
    val BLACK_SLUDGE = heldItem("black_sludge")
    @JvmField
    val BLUNDER_POLICY = itemNameBlockItem("blunder_policy", CobblemonBlocks.BLUNDER_POLICY)
    @JvmField
    val CELL_BATTERY = heldItem("cell_battery")
    @JvmField
    val CHARCOAL = heldItem("charcoal_stick", remappedName = "charcoal")
    @JvmField
    val CHOICE_SCARF = heldItem("choice_scarf")
    @JvmField
    val CLEANSE_TAG = itemNameBlockItem("cleanse_tag", CobblemonBlocks.CLEANSE_TAG)
    @JvmField
    val CLEAR_AMULET = heldItem("clear_amulet")
    @JvmField
    val COVERT_CLOAK = heldItem("covert_cloak")
    @JvmField
    val DESTINY_KNOT = heldItem("destiny_knot")
    @JvmField
    val DRAGON_FANG = heldItem("dragon_fang")
    @JvmField
    val EJECT_BUTTON = heldItem("eject_button")
    @JvmField
    val EJECT_PACK = heldItem("eject_pack")
    @JvmField
    val ELECTRIC_SEED = heldItem("electric_seed")
    @JvmField
    val EVERSTONE = heldItem("everstone")
    @JvmField
    val EVIOLITE = heldItem("eviolite")
    @JvmField
    val EXPERT_BELT = heldItem("expert_belt")
    @JvmField
    val FAIRY_FEATHER = heldItem("fairy_feather")
    @JvmField
    val FLAME_ORB = heldItem("flame_orb")
    @JvmField
    val FLOAT_STONE = heldItem("float_stone")
    @JvmField
    val FOCUS_SASH = heldItem("focus_sash")
    @JvmField
    val GRASSY_SEED = heldItem("grassy_seed")
    @JvmField
    val GRIP_CLAW = heldItem("grip_claw")
    @JvmField
    val HARD_STONE = heldItem("hard_stone")
    @JvmField
    val HEAVY_DUTY_BOOTS = heldItem("heavy_duty_boots")
    @JvmField
    val IRON_BALL = heldItem("iron_ball")
    @JvmField
    val LAGGING_TAIL = heldItem("lagging_tail")
    @JvmField
    val LEFTOVERS = compostableHeldItem("leftovers", null, 0.50f)
    @JvmField
    val LIFE_ORB = heldItem("life_orb")
    @JvmField
    val LIGHT_BALL = heldItem("light_ball")
    @JvmField
    val LIGHT_CLAY = heldItem("light_clay")
    @JvmField
    val LOADED_DICE = heldItem("loaded_dice")
    @JvmField
    val LUCKY_EGG = heldItem("lucky_egg")
    @JvmField
    val LUMINOUS_MOSS = heldItem("luminous_moss")
    @JvmField
    val MAGNET = heldItem("magnet")
    @JvmField
    val METRONOME = heldItem("metronome")
    @JvmField
    val MIRACLE_SEED = heldItem("miracle_seed")
    @JvmField
    val MYSTIC_WATER = heldItem("mystic_water")
    @JvmField
    val NEVER_MELT_ICE = heldItem("never_melt_ice")
    @JvmField
    val POISON_BARB = heldItem("poison_barb")
    @JvmField
    val POWER_ANKLET = heldItem("power_anklet")
    @JvmField
    val POWER_BAND = heldItem("power_band")
    @JvmField
    val POWER_BELT = heldItem("power_belt")
    @JvmField
    val POWER_BRACER = heldItem("power_bracer")
    @JvmField
    val POWER_LENS = heldItem("power_lens")
    @JvmField
    val POWER_WEIGHT = heldItem("power_weight")
    @JvmField
    val PSYCHIC_SEED = heldItem("psychic_seed")
    @JvmField
    val PROTECTIVE_PADS = heldItem("protective_pads")
    @JvmField
    val PUNCHING_GLOVE = heldItem("punching_glove")
    @JvmField
    val QUICK_CLAW = heldItem("quick_claw")
    @JvmField
    val RED_CARD = heldItem("red_card")
    @JvmField
    val RING_TARGET = heldItem("ring_target")
    @JvmField
    val ROOM_SERVICE = heldItem("room_service")
    @JvmField
    val SCOPE_LENS = heldItem("scope_lens")
    @JvmField
    val SHARP_BEAK = heldItem("sharp_beak")
    @JvmField
    val SHED_SHELL = heldItem("shed_shell")
    @JvmField
    val SHELL_BELL = heldItem("shell_bell")
    @JvmField
    val SILK_SCARF = heldItem("silk_scarf")
    @JvmField
    val SILVER_POWDER = heldItem("silver_powder")
    @JvmField
    val SOFT_SAND = heldItem("soft_sand")
    @JvmField
    val SPELL_TAG = itemNameBlockItem("spell_tag", CobblemonBlocks.SPELL_TAG)
    @JvmField
    val SMOKE_BALL = heldItem("smoke_ball")
    @JvmField
    val SOOTHE_BELL = heldItem("soothe_bell")
    @JvmField
    val STICKY_BARB = heldItem("sticky_barb")
    @JvmField
    val TERRAIN_EXTENDER = heldItem("terrain_extender")
    @JvmField
    val THROAT_SPRAY = heldItem("throat_spray")
    @JvmField
    val TOXIC_ORB = heldItem("toxic_orb")
    @JvmField
    val TWISTED_SPOON = heldItem("twisted_spoon")
    @JvmField
    val UTILITY_UMBRELLA = heldItem("utility_umbrella")
    @JvmField
    val WEAKNESS_POLICY = itemNameBlockItem("weakness_policy", CobblemonBlocks.WEAKNESS_POLICY)
    @JvmField
    val WIDE_LENS = heldItem("wide_lens")
    @JvmField
    val ZOOM_LENS = heldItem("zoom_lens")
    @JvmField
    val MENTAL_HERB = compostableHeldItem("mental_herb", null, 0.85F)
    @JvmField
    val MIRROR_HERB = compostableHeldItem("mirror_herb", null, 0.85F)
    @JvmField
    val MISTY_SEED = heldItem("misty_seed")
    @JvmField
    val POWER_HERB = compostableHeldItem("power_herb", null, 0.85F)
    @JvmField
    val WHITE_HERB = compostableHeldItem("white_herb", null, 0.85F)
    @JvmField
    val BRIGHT_POWDER = heldItem("bright_powder")
    @JvmField
    val METAL_POWDER = heldItem("metal_powder")
    @JvmField
    val QUICK_POWDER = heldItem("quick_powder")
    @JvmField
    val DAMP_ROCK = heldItem("damp_rock")
    @JvmField
    val HEAT_ROCK = heldItem("heat_rock")
    @JvmField
    val SMOOTH_ROCK = heldItem("smooth_rock")
    @JvmField
    val ICY_ROCK = heldItem("icy_rock")

    // Mulch
    @JvmField
    val MULCH_BASE = noSettingsItem("mulch_base")
    @JvmField
    val COARSE_MULCH = mulchItem("coarse_mulch", MulchVariant.COARSE)
    @JvmField
    val GROWTH_MULCH = mulchItem("growth_mulch", MulchVariant.GROWTH)
    @JvmField
    val HUMID_MULCH = mulchItem("humid_mulch", MulchVariant.HUMID)
    @JvmField
    val LOAMY_MULCH = mulchItem("loamy_mulch", MulchVariant.LOAMY)
    @JvmField
    val PEAT_MULCH = mulchItem("peat_mulch", MulchVariant.PEAT)
    @JvmField
    val RICH_MULCH = mulchItem("rich_mulch", MulchVariant.RICH)
    @JvmField
    val SANDY_MULCH = mulchItem("sandy_mulch", MulchVariant.SANDY)
    @JvmField
    val SURPRISE_MULCH = mulchItem("surprise_mulch", MulchVariant.SURPRISE)

    // Archaeology
    @JvmField
    val ARMOR_FOSSIL = itemWithRarity("armor_fossil", Rarity.UNCOMMON)
    @JvmField
    val FOSSILIZED_BIRD = itemWithRarity("fossilized_bird", Rarity.UNCOMMON)
    @JvmField
    val CLAW_FOSSIL = itemWithRarity("claw_fossil", Rarity.UNCOMMON)
    @JvmField
    val COVER_FOSSIL = itemWithRarity("cover_fossil", Rarity.UNCOMMON)
    @JvmField
    val FOSSILIZED_DINO = itemWithRarity("fossilized_dino", Rarity.UNCOMMON)
    @JvmField
    val DOME_FOSSIL = itemWithRarity("dome_fossil", Rarity.UNCOMMON)
    @JvmField
    val FOSSILIZED_DRAKE = itemWithRarity("fossilized_drake", Rarity.UNCOMMON)
    @JvmField
    val FOSSILIZED_FISH = itemWithRarity("fossilized_fish", Rarity.UNCOMMON)
    @JvmField
    val HELIX_FOSSIL = itemWithRarity("helix_fossil", Rarity.UNCOMMON)
    @JvmField
    val JAW_FOSSIL = itemWithRarity("jaw_fossil", Rarity.UNCOMMON)
    @JvmField
    val OLD_AMBER_FOSSIL = itemWithRarity("old_amber_fossil", Rarity.UNCOMMON)
    @JvmField
    val PLUME_FOSSIL = itemWithRarity("plume_fossil", Rarity.UNCOMMON)
    @JvmField
    val ROOT_FOSSIL = itemWithRarity("root_fossil", Rarity.UNCOMMON)
    @JvmField
    val SAIL_FOSSIL = itemWithRarity("sail_fossil", Rarity.UNCOMMON)
    @JvmField
    val SKULL_FOSSIL = itemWithRarity("skull_fossil", Rarity.UNCOMMON)

    @JvmField
    val BYGONE_SHERD = itemWithRarity("bygone_sherd", Rarity.UNCOMMON)
    @JvmField
    val CAPTURE_SHERD = itemWithRarity("capture_sherd",Rarity.UNCOMMON)
    @JvmField
    val DOME_SHERD = itemWithRarity("dome_sherd", Rarity.UNCOMMON)
    @JvmField
    val HELIX_SHERD = itemWithRarity("helix_sherd", Rarity.UNCOMMON)
    @JvmField
    val NOSTALGIC_SHERD = itemWithRarity("nostalgic_sherd",Rarity.UNCOMMON)
    @JvmField
    val SUSPICIOUS_SHERD = itemWithRarity("suspicious_sherd", Rarity.UNCOMMON)

    @JvmField
    val TUMBLESTONE = this.create("tumblestone", TumblestoneItem(Item.Properties(), CobblemonBlocks.SMALL_BUDDING_TUMBLESTONE))
    @JvmField
    val BLACK_TUMBLESTONE = this.create("black_tumblestone", TumblestoneItem(Item.Properties(), CobblemonBlocks.SMALL_BUDDING_BLACK_TUMBLESTONE))
    @JvmField
    val SKY_TUMBLESTONE = this.create("sky_tumblestone", TumblestoneItem(Item.Properties(), CobblemonBlocks.SMALL_BUDDING_SKY_TUMBLESTONE))

    @JvmField
    val SMALL_BUDDING_TUMBLESTONE = blockItem("small_budding_tumblestone", CobblemonBlocks.SMALL_BUDDING_TUMBLESTONE)
    @JvmField
    val MEDIUM_BUDDING_TUMBLESTONE = blockItem("medium_budding_tumblestone", CobblemonBlocks.MEDIUM_BUDDING_TUMBLESTONE)
    @JvmField
    val LARGE_BUDDING_TUMBLESTONE = blockItem("large_budding_tumblestone", CobblemonBlocks.LARGE_BUDDING_TUMBLESTONE)
    @JvmField
    val TUMBLESTONE_CLUSTER = blockItem("tumblestone_cluster", CobblemonBlocks.TUMBLESTONE_CLUSTER)

    @JvmField
    val SMALL_BUDDING_SKY_TUMBLESTONE = blockItem("small_budding_sky_tumblestone", CobblemonBlocks.SMALL_BUDDING_SKY_TUMBLESTONE)
    @JvmField
    val MEDIUM_BUDDING_SKY_TUMBLESTONE = blockItem("medium_budding_sky_tumblestone", CobblemonBlocks.MEDIUM_BUDDING_SKY_TUMBLESTONE)
    @JvmField
    val LARGE_BUDDING_SKY_TUMBLESTONE = blockItem("large_budding_sky_tumblestone", CobblemonBlocks.LARGE_BUDDING_SKY_TUMBLESTONE)
    @JvmField
    val SKY_TUMBLESTONE_CLUSTER = blockItem("sky_tumblestone_cluster", CobblemonBlocks.SKY_TUMBLESTONE_CLUSTER)

    @JvmField
    val SMALL_BUDDING_BLACK_TUMBLESTONE = blockItem("small_budding_black_tumblestone", CobblemonBlocks.SMALL_BUDDING_BLACK_TUMBLESTONE)
    @JvmField
    val MEDIUM_BUDDING_BLACK_TUMBLESTONE = blockItem("medium_budding_black_tumblestone", CobblemonBlocks.MEDIUM_BUDDING_BLACK_TUMBLESTONE)
    @JvmField
    val LARGE_BUDDING_BLACK_TUMBLESTONE = blockItem("large_budding_black_tumblestone", CobblemonBlocks.LARGE_BUDDING_BLACK_TUMBLESTONE)
    @JvmField
    val BLACK_TUMBLESTONE_CLUSTER = blockItem("black_tumblestone_cluster", CobblemonBlocks.BLACK_TUMBLESTONE_CLUSTER)

    @JvmField
    val TUMBLESTONE_BLOCK = blockItem("tumblestone_block", CobblemonBlocks.TUMBLESTONE_BLOCK)
    @JvmField
    val SKY_TUMBLESTONE_BLOCK = blockItem("sky_tumblestone_block", CobblemonBlocks.SKY_TUMBLESTONE_BLOCK)
    @JvmField
    val BLACK_TUMBLESTONE_BLOCK = blockItem("black_tumblestone_block", CobblemonBlocks.BLACK_TUMBLESTONE_BLOCK)

    @JvmField
    val POKEROD_SMITHING_TEMPLATE = create("pokerod_smithing_template", SmithingTemplateItem(
        Component.translatable("item.minecraft.fishing_rod").blue(),
        Component.translatable("item.cobblemon.smithing_template.pokerod.ingredients").blue(),
        Component.translatable("upgrade.cobblemon.pokerod").gray(),
        Component.translatable("item.cobblemon.smithing_template.pokerod.base_slot_description"),
        Component.translatable("item.cobblemon.smithing_template.pokerod.additions_slot_description"),
        listOf(cobblemonResource("item/empty_slot_fishing_rod")),
        listOf(cobblemonResource("item/empty_slot_pokeball"))
    ))

    @JvmField
    val POLISHED_TUMBLESTONE = blockItem("polished_tumblestone", CobblemonBlocks.POLISHED_TUMBLESTONE)
    @JvmField
    val POLISHED_TUMBLESTONE_STAIRS = blockItem("polished_tumblestone_stairs", CobblemonBlocks.POLISHED_TUMBLESTONE_STAIRS)
    @JvmField
    val POLISHED_TUMBLESTONE_SLAB = blockItem("polished_tumblestone_slab", CobblemonBlocks.POLISHED_TUMBLESTONE_SLAB)
    @JvmField
    val POLISHED_TUMBLESTONE_WALL = blockItem("polished_tumblestone_wall", CobblemonBlocks.POLISHED_TUMBLESTONE_WALL)
    @JvmField
    val CHISELED_POLISHED_TUMBLESTONE = blockItem("chiseled_polished_tumblestone", CobblemonBlocks.CHISELED_POLISHED_TUMBLESTONE)
    @JvmField
    val SMOOTH_TUMBLESTONE = blockItem("smooth_tumblestone", CobblemonBlocks.SMOOTH_TUMBLESTONE)
    @JvmField
    val SMOOTH_TUMBLESTONE_STAIRS = blockItem("smooth_tumblestone_stairs", CobblemonBlocks.SMOOTH_TUMBLESTONE_STAIRS)
    @JvmField
    val SMOOTH_TUMBLESTONE_SLAB = blockItem("smooth_tumblestone_slab", CobblemonBlocks.SMOOTH_TUMBLESTONE_SLAB)
    @JvmField
    val TUMBLESTONE_BRICKS = blockItem("tumblestone_bricks", CobblemonBlocks.TUMBLESTONE_BRICKS)
    @JvmField
    val TUMBLESTONE_BRICK_STAIRS = blockItem("tumblestone_brick_stairs", CobblemonBlocks.TUMBLESTONE_BRICK_STAIRS)
    @JvmField
    val TUMBLESTONE_BRICK_SLAB = blockItem("tumblestone_brick_slab", CobblemonBlocks.TUMBLESTONE_BRICK_SLAB)
    @JvmField
    val TUMBLESTONE_BRICK_WALL = blockItem("tumblestone_brick_wall", CobblemonBlocks.TUMBLESTONE_BRICK_WALL)
    @JvmField
    val CHISELED_TUMBLESTONE_BRICKS = blockItem("chiseled_tumblestone_bricks", CobblemonBlocks.CHISELED_TUMBLESTONE_BRICKS)
    @JvmField
    val POLISHED_SKY_TUMBLESTONE = blockItem("polished_sky_tumblestone", CobblemonBlocks.POLISHED_SKY_TUMBLESTONE)
    @JvmField
    val POLISHED_SKY_TUMBLESTONE_STAIRS = blockItem("polished_sky_tumblestone_stairs", CobblemonBlocks.POLISHED_SKY_TUMBLESTONE_STAIRS)
    @JvmField
    val POLISHED_SKY_TUMBLESTONE_SLAB = blockItem("polished_sky_tumblestone_slab", CobblemonBlocks.POLISHED_SKY_TUMBLESTONE_SLAB)
    @JvmField
    val POLISHED_SKY_TUMBLESTONE_WALL = blockItem("polished_sky_tumblestone_wall", CobblemonBlocks.POLISHED_SKY_TUMBLESTONE_WALL)
    @JvmField
    val CHISELED_POLISHED_SKY_TUMBLESTONE = blockItem("chiseled_polished_sky_tumblestone", CobblemonBlocks.CHISELED_POLISHED_SKY_TUMBLESTONE)
    @JvmField
    val SMOOTH_SKY_TUMBLESTONE = blockItem("smooth_sky_tumblestone", CobblemonBlocks.SMOOTH_SKY_TUMBLESTONE)
    @JvmField
    val SMOOTH_SKY_TUMBLESTONE_STAIRS = blockItem("smooth_sky_tumblestone_stairs", CobblemonBlocks.SMOOTH_SKY_TUMBLESTONE_STAIRS)
    @JvmField
    val SMOOTH_SKY_TUMBLESTONE_SLAB = blockItem("smooth_sky_tumblestone_slab", CobblemonBlocks.SMOOTH_SKY_TUMBLESTONE_SLAB)
    @JvmField
    val SKY_TUMBLESTONE_BRICKS = blockItem("sky_tumblestone_bricks", CobblemonBlocks.SKY_TUMBLESTONE_BRICKS)
    @JvmField
    val SKY_TUMBLESTONE_BRICK_STAIRS = blockItem("sky_tumblestone_brick_stairs", CobblemonBlocks.SKY_TUMBLESTONE_BRICK_STAIRS)
    @JvmField
    val SKY_TUMBLESTONE_BRICK_SLAB = blockItem("sky_tumblestone_brick_slab", CobblemonBlocks.SKY_TUMBLESTONE_BRICK_SLAB)
    @JvmField
    val SKY_TUMBLESTONE_BRICK_WALL = blockItem("sky_tumblestone_brick_wall", CobblemonBlocks.SKY_TUMBLESTONE_BRICK_WALL)
    @JvmField
    val CHISELED_SKY_TUMBLESTONE_BRICKS = blockItem("chiseled_sky_tumblestone_bricks", CobblemonBlocks.CHISELED_SKY_TUMBLESTONE_BRICKS)
    @JvmField
    val POLISHED_BLACK_TUMBLESTONE = blockItem("polished_black_tumblestone", CobblemonBlocks.POLISHED_BLACK_TUMBLESTONE)
    @JvmField
    val POLISHED_BLACK_TUMBLESTONE_STAIRS = blockItem("polished_black_tumblestone_stairs", CobblemonBlocks.POLISHED_BLACK_TUMBLESTONE_STAIRS)
    @JvmField
    val POLISHED_BLACK_TUMBLESTONE_SLAB = blockItem("polished_black_tumblestone_slab", CobblemonBlocks.POLISHED_BLACK_TUMBLESTONE_SLAB)
    @JvmField
    val POLISHED_BLACK_TUMBLESTONE_WALL = blockItem("polished_black_tumblestone_wall", CobblemonBlocks.POLISHED_BLACK_TUMBLESTONE_WALL)
    @JvmField
    val CHISELED_POLISHED_BLACK_TUMBLESTONE = blockItem("chiseled_polished_black_tumblestone", CobblemonBlocks.CHISELED_POLISHED_BLACK_TUMBLESTONE)
    @JvmField
    val SMOOTH_BLACK_TUMBLESTONE = blockItem("smooth_black_tumblestone", CobblemonBlocks.SMOOTH_BLACK_TUMBLESTONE)
    @JvmField
    val SMOOTH_BLACK_TUMBLESTONE_STAIRS = blockItem("smooth_black_tumblestone_stairs", CobblemonBlocks.SMOOTH_BLACK_TUMBLESTONE_STAIRS)
    @JvmField
    val SMOOTH_BLACK_TUMBLESTONE_SLAB = blockItem("smooth_black_tumblestone_slab", CobblemonBlocks.SMOOTH_BLACK_TUMBLESTONE_SLAB)
    @JvmField
    val BLACK_TUMBLESTONE_BRICKS = blockItem("black_tumblestone_bricks", CobblemonBlocks.BLACK_TUMBLESTONE_BRICKS)
    @JvmField
    val BLACK_TUMBLESTONE_BRICK_STAIRS = blockItem("black_tumblestone_brick_stairs", CobblemonBlocks.BLACK_TUMBLESTONE_BRICK_STAIRS)
    @JvmField
    val BLACK_TUMBLESTONE_BRICK_SLAB = blockItem("black_tumblestone_brick_slab", CobblemonBlocks.BLACK_TUMBLESTONE_BRICK_SLAB)
    @JvmField
    val BLACK_TUMBLESTONE_BRICK_WALL = blockItem("black_tumblestone_brick_wall", CobblemonBlocks.BLACK_TUMBLESTONE_BRICK_WALL)
    @JvmField
    val CHISELED_BLACK_TUMBLESTONE_BRICKS = blockItem("chiseled_black_tumblestone_bricks", CobblemonBlocks.CHISELED_BLACK_TUMBLESTONE_BRICKS)

    @JvmField
    val FIRE_STONE_BLOCK = blockItem("fire_stone_block", CobblemonBlocks.FIRE_STONE_BLOCK)
    @JvmField
    val WATER_STONE_BLOCK = blockItem("water_stone_block", CobblemonBlocks.WATER_STONE_BLOCK)
    @JvmField
    val THUNDER_STONE_BLOCK = blockItem("thunder_stone_block", CobblemonBlocks.THUNDER_STONE_BLOCK)
    @JvmField
    val LEAF_STONE_BLOCK = blockItem("leaf_stone_block", CobblemonBlocks.LEAF_STONE_BLOCK)
    @JvmField
    val ICE_STONE_BLOCK = blockItem("ice_stone_block", CobblemonBlocks.ICE_STONE_BLOCK)
    @JvmField
    val SUN_STONE_BLOCK = blockItem("sun_stone_block", CobblemonBlocks.SUN_STONE_BLOCK)
    @JvmField
    val MOON_STONE_BLOCK = blockItem("moon_stone_block", CobblemonBlocks.MOON_STONE_BLOCK)
    @JvmField
    val SHINY_STONE_BLOCK = blockItem("shiny_stone_block", CobblemonBlocks.SHINY_STONE_BLOCK)
    @JvmField
    val DAWN_STONE_BLOCK = blockItem("dawn_stone_block", CobblemonBlocks.DAWN_STONE_BLOCK)
    @JvmField
    val DUSK_STONE_BLOCK = blockItem("dusk_stone_block", CobblemonBlocks.DUSK_STONE_BLOCK)

    @JvmField
    val AUTOMATON_ARMOR_TRIM_SMITHING_TEMPLATE: SmithingTemplateItem = this.create(
        "automaton_armor_trim_smithing_template",
        SmithingTemplateItem.createArmorTrimTemplate(CobblemonArmorTrims.AUTOMATON)
    )

    val pokeRods = mutableListOf<PokerodItem>()
    @JvmField
    val POKE_ROD = pokerodItem(cobblemonResource("poke_rod"))
    @JvmField
    val CITRINE_ROD = pokerodItem(cobblemonResource("citrine_rod"))
    @JvmField
    val VERDANT_ROD = pokerodItem(cobblemonResource("verdant_rod"))
    @JvmField
    val AZURE_ROD = pokerodItem(cobblemonResource("azure_rod"))
    @JvmField
    val ROSEATE_ROD = pokerodItem(cobblemonResource("roseate_rod"))
    @JvmField
    val SLATE_ROD = pokerodItem(cobblemonResource("slate_rod"))
    @JvmField
    val PREMIER_ROD = pokerodItem(cobblemonResource("premier_rod"))
    @JvmField
    val GREAT_ROD = pokerodItem(cobblemonResource("great_rod"))
    @JvmField
    val ULTRA_ROD = pokerodItem(cobblemonResource("ultra_rod"))
    @JvmField
    val SAFARI_ROD = pokerodItem(cobblemonResource("safari_rod"))
    @JvmField
    val FAST_ROD = pokerodItem(cobblemonResource("fast_rod"))
    @JvmField
    val LEVEL_ROD = pokerodItem(cobblemonResource("level_rod"))
    @JvmField
    val LURE_ROD = pokerodItem(cobblemonResource("lure_rod"))
    @JvmField
    val HEAVY_ROD = pokerodItem(cobblemonResource("heavy_rod"))
    @JvmField
    val LOVE_ROD = pokerodItem(cobblemonResource("love_rod"))
    @JvmField
    val FRIEND_ROD = pokerodItem(cobblemonResource("friend_rod"))
    @JvmField
    val MOON_ROD = pokerodItem(cobblemonResource("moon_rod"))
    @JvmField
    val SPORT_ROD = pokerodItem(cobblemonResource("sport_rod"))
    @JvmField
    val PARK_ROD = pokerodItem(cobblemonResource("park_rod"))
    @JvmField
    val NET_ROD = pokerodItem(cobblemonResource("net_rod"))
    @JvmField
    val DIVE_ROD = pokerodItem(cobblemonResource("dive_rod"))
    @JvmField
    val NEST_ROD = pokerodItem(cobblemonResource("nest_rod"))
    @JvmField
    val REPEAT_ROD = pokerodItem(cobblemonResource("repeat_rod"))
    @JvmField
    val TIMER_ROD = pokerodItem(cobblemonResource("timer_rod"))
    @JvmField
    val LUXURY_ROD = pokerodItem(cobblemonResource("luxury_rod"))
    @JvmField
    val DUSK_ROD = pokerodItem(cobblemonResource("dusk_rod"))
    @JvmField
    val HEAL_ROD = pokerodItem(cobblemonResource("heal_rod"))
    @JvmField
    val QUICK_ROD = pokerodItem(cobblemonResource("quick_rod"))
    @JvmField
    val DREAM_ROD = pokerodItem(cobblemonResource("dream_rod"))
    @JvmField
    val BEAST_ROD = pokerodItem(cobblemonResource("beast_rod"), Rarity.RARE)
    @JvmField
    val MASTER_ROD = pokerodItem(cobblemonResource("master_rod"), Rarity.EPIC)
    @JvmField
    val CHERISH_ROD = pokerodItem(cobblemonResource("cherish_rod"), Rarity.EPIC)
    @JvmField
    val ANCIENT_POKE_ROD = pokerodItem(cobblemonResource("ancient_poke_rod"))
    @JvmField
    val ANCIENT_CITRINE_ROD = pokerodItem(cobblemonResource("ancient_citrine_rod"))
    @JvmField
    val ANCIENT_VERDANT_ROD = pokerodItem(cobblemonResource("ancient_verdant_rod"))
    @JvmField
    val ANCIENT_AZURE_ROD = pokerodItem(cobblemonResource("ancient_azure_rod"))
    @JvmField
    val ANCIENT_ROSEATE_ROD = pokerodItem(cobblemonResource("ancient_roseate_rod"))
    @JvmField
    val ANCIENT_SLATE_ROD = pokerodItem(cobblemonResource("ancient_slate_rod"))
    @JvmField
    val ANCIENT_IVORY_ROD = pokerodItem(cobblemonResource("ancient_ivory_rod"))
    @JvmField
    val ANCIENT_GREAT_ROD = pokerodItem(cobblemonResource("ancient_great_rod"))
    @JvmField
    val ANCIENT_ULTRA_ROD = pokerodItem(cobblemonResource("ancient_ultra_rod"))
    @JvmField
    val ANCIENT_FEATHER_ROD = pokerodItem(cobblemonResource("ancient_feather_rod"))
    @JvmField
    val ANCIENT_WING_ROD = pokerodItem(cobblemonResource("ancient_wing_rod"))
    @JvmField
    val ANCIENT_JET_ROD = pokerodItem(cobblemonResource("ancient_jet_rod"))
    @JvmField
    val ANCIENT_HEAVY_ROD = pokerodItem(cobblemonResource("ancient_heavy_rod"))
    @JvmField
    val ANCIENT_LEADEN_ROD = pokerodItem(cobblemonResource("ancient_leaden_rod"))
    @JvmField
    val ANCIENT_GIGATON_ROD = pokerodItem(cobblemonResource("ancient_gigaton_rod"))
    @JvmField
    val ANCIENT_ORIGIN_ROD = pokerodItem(cobblemonResource("ancient_origin_rod"), Rarity.EPIC)

    // Misc
    @JvmField
    val POKEMON_MODEL = this.create("pokemon_model", PokemonItem())
    @JvmField
    val RELIC_COIN = noSettingsItem("relic_coin")
    @JvmField
    val RELIC_COIN_POUCH = blockItem("relic_coin_pouch", CobblemonBlocks.RELIC_COIN_POUCH)
    @JvmField
    val RELIC_COIN_SACK = blockItem("relic_coin_sack", CobblemonBlocks.RELIC_COIN_SACK)

    // Type Gems
    @JvmField
    val NORMAL_GEM = noSettingsItem("normal_gem")
    @JvmField
    val FIRE_GEM = noSettingsItem("fire_gem")
    @JvmField
    val WATER_GEM = noSettingsItem("water_gem")
    @JvmField
    val GRASS_GEM = noSettingsItem("grass_gem")
    @JvmField
    val ELECTRIC_GEM = noSettingsItem("electric_gem")
    @JvmField
    val ICE_GEM = noSettingsItem("ice_gem")
    @JvmField
    val FIGHTING_GEM = noSettingsItem("fighting_gem")
    @JvmField
    val POISON_GEM = noSettingsItem("poison_gem")
    @JvmField
    val GROUND_GEM = noSettingsItem("ground_gem")
    @JvmField
    val FLYING_GEM = noSettingsItem("flying_gem")
    @JvmField
    val PSYCHIC_GEM = noSettingsItem("psychic_gem")
    @JvmField
    val BUG_GEM = noSettingsItem("bug_gem")
    @JvmField
    val ROCK_GEM = noSettingsItem("rock_gem")
    @JvmField
    val GHOST_GEM = noSettingsItem("ghost_gem")
    @JvmField
    val DRAGON_GEM = noSettingsItem("dragon_gem")
    @JvmField
    val DARK_GEM = noSettingsItem("dark_gem")
    @JvmField
    val STEEL_GEM = noSettingsItem("steel_gem")
    @JvmField
    val FAIRY_GEM = noSettingsItem("fairy_gem")
    //@JvmField
    //val BINDING_SOIL = blockItem("binding_soil", CobblemonBlocks.BINDING_SOIL)
    @JvmField
    val MEGA_BRACELET = create("mega_bracelet", MegaBraceletItem())
    @JvmField
    val TERA_ORB = create("tera_orb", TeraOrbItem())

    val TERA_SHARDS = mutableListOf<TeraShardItem>()

    @JvmField
    val TERA_SHARD_BUG = teraShard(TeraTypes.BUG)
    @JvmField
    val TERA_SHARD_DARK = teraShard(TeraTypes.DARK)
    @JvmField
    val TERA_SHARD_DRAGON = teraShard(TeraTypes.DRAGON)
    @JvmField
    val TERA_SHARD_ELECTRIC = teraShard(TeraTypes.ELECTRIC)
    @JvmField
    val TERA_SHARD_FAIRY = teraShard(TeraTypes.FAIRY)
    @JvmField
    val TERA_SHARD_FIGHTING = teraShard(TeraTypes.FIGHTING)
    @JvmField
    val TERA_SHARD_FIRE = teraShard(TeraTypes.FIRE)
    @JvmField
    val TERA_SHARD_FLYING = teraShard(TeraTypes.FLYING)
    @JvmField
    val TERA_SHARD_GHOST = teraShard(TeraTypes.GHOST)
    @JvmField
    val TERA_SHARD_GRASS = teraShard(TeraTypes.GRASS)
    @JvmField
    val TERA_SHARD_GROUND = teraShard(TeraTypes.GROUND)
    @JvmField
    val TERA_SHARD_ICE = teraShard(TeraTypes.ICE)
    @JvmField
    val TERA_SHARD_NORMAL = teraShard(TeraTypes.NORMAL)
    @JvmField
    val TERA_SHARD_POISON = teraShard(TeraTypes.POISON)
    @JvmField
    val TERA_SHARD_PSYCHIC = teraShard(TeraTypes.PSYCHIC)
    @JvmField
    val TERA_SHARD_ROCK = teraShard(TeraTypes.ROCK)
    @JvmField
    val TERA_SHARD_STEEL = teraShard(TeraTypes.STEEL)
    @JvmField
    val TERA_SHARD_STELLAR = teraShard(TeraTypes.STELLAR)
    @JvmField
    val TERA_SHARD_WATER = teraShard(TeraTypes.WATER)

    val MEGASTONES = mutableListOf<MegaStoneItem>()

    @JvmField
    val ABOMASITE = megaStone("abomasite", "abomasnow", "mega_evolution=mega")
    @JvmField
    val ABSOLITE = megaStone("absolite", "absol", "mega_evolution=mega")
    @JvmField
    val AERODACTYLITE = megaStone("aerodactylite", "aerodactyl", "mega_evolution=mega")
    @JvmField
    val AGGRONITE = megaStone("aggronite", "aggron", "mega_evolution=mega")
    @JvmField
    val ALAKAZITE = megaStone("alakazite", "alakazam", "mega_evolution=mega")
    @JvmField
    val ALTARIANITE = megaStone("altarianite", "altaria", "mega_evolution=mega")
    @JvmField
    val AMPHAROSITE = megaStone("ampharosite", "ampharos", "mega_evolution=mega")
    @JvmField
    val AUDINITE = megaStone("audinite", "audino", "mega_evolution=mega")
    @JvmField
    val BANETTITE = megaStone("banettite", "banette", "mega_evolution=mega")
    @JvmField
    val BEEDRILLITE = megaStone("beedrillite", "beedrill", "mega_evolution=mega")
    @JvmField
    val BLASTOISINITE = megaStone("blastoisinite", "blastoise", "mega_evolution=mega")
    @JvmField
    val BLAZIKENITE = megaStone("blazikenite", "blaziken", "mega_evolution=mega")
    @JvmField
    val CAMERUPTITE = megaStone("cameruptite", "camerupt", "mega_evolution=mega")
    @JvmField
    val CHARIZARDITE_X = megaStone("charizarditex", "charizard", "mega_evolution=mega_x")
    @JvmField
    val CHARIZARDITE_Y = megaStone("charizarditey", "charizard", "mega_evolution=mega_y")
    @JvmField
    val DIANCITE = megaStone("diancite", "diancie", "mega_evolution=mega")
    @JvmField
    val GALLADITE = megaStone("galladite", "gallade", "mega_evolution=mega")
    @JvmField
    val GARCHOMPITE = megaStone("garchompite", "garchomp", "mega_evolution=mega")
    @JvmField
    val GARDEVOIRITE = megaStone("gardevoirite", "gardevoir", "mega_evolution=mega")
    @JvmField
    val GENGARITE = megaStone("gengarite", "gengar", "mega_evolution=mega")
    @JvmField
    val GYARADOSITE = megaStone("gyaradosite", "gyarados", "mega_evolution=mega")
    @JvmField
    val HERACRONITE = megaStone("heracronite", "heracross", "mega_evolution=mega")
    @JvmField
    val HOUNDOOMINITE = megaStone("houndoominite", "houndoom", "mega_evolution=mega")
    @JvmField
    val KANGASKHANITE = megaStone("kangaskhanite", "kangaskan", "mega_evolution=mega")
    @JvmField
    val LATIASITE = megaStone("latiasite", "latias", "mega_evolution=mega")
    @JvmField
    val LATIOSITE = megaStone("latiosite", "latios", "mega_evolution=mega")
    @JvmField
    val LOPUNNITE = megaStone("lopunnite", "lopunny", "mega_evolution=mega")
    @JvmField
    val LUCARIONITE = megaStone("lucarionite", "lucario", "mega_evolution=mega")
    @JvmField
    val MANECTITE = megaStone("manectite", "manectric", "mega_evolution=mega")
    @JvmField
    val MAWILITE = megaStone("mawilite", "mawile", "mega_evolution=mega")
    @JvmField
    val MEDICHAMITE = megaStone("medichamite", "medicham", "mega_evolution=mega")
    @JvmField
    val METAGROSSITE = megaStone("metagrossite", "metagross", "mega_evolution=mega")
    @JvmField
    val MEWTWONITE_X = megaStone("mewtwonitex", "mewtwo", "mega_evolution=mega_x")
    @JvmField
    val MEWTWONITE_Y = megaStone("mewtwonitey", "mewtwo", "mega_evolution=mega_y")
    @JvmField
    val PIDGEOTITE = megaStone("pidgeotite", "pidgeot", "mega_evolution=mega")
    @JvmField
    val PINSIRITE = megaStone("pinsirite", "pinsir", "mega_evolution=mega")
    @JvmField
    val SABLENITE = megaStone("sablenite", "sableye", "mega_evolution=mega")
    @JvmField
    val SALAMENCITE = megaStone("salamencite", "salamence", "mega_evolution=mega")
    @JvmField
    val SCEPTILITE = megaStone("sceptilite", "sceptile", "mega_evolution=mega")
    @JvmField
    val SCIZORITE = megaStone("scizorite", "scizor", "mega_evolution=mega")
    @JvmField
    val SHARPEDONITE = megaStone("sharpedonite", "sharpedo", "mega_evolution=mega")
    @JvmField
    val SLOWBRONITE = megaStone("slowbronite", "slowbro", "mega_evolution=mega")
    @JvmField
    val STEELIXITE = megaStone("steelixite", "steelix", "mega_evolution=mega")
    @JvmField
    val SWAMPERTITE = megaStone("swampertite", "swampert", "mega_evolution=mega")
    @JvmField
    val TYRANITARITE = megaStone("tyranitarite", "tyranitar", "mega_evolution=mega")
    @JvmField
    val VENUSAURITE = megaStone("venusaurite", "venusaur", "mega_evolution=mega")

    val LEGENDARY_ITEMS = mutableListOf<LegendaryItem>()

    @JvmField
    val RARE_DNA = legendaryItem("rare_dna", 2)
    @JvmField
    val TIME_CORE = legendaryItem("time_core", 2)
    @JvmField
    val WISHING_STAR = legendaryItem("wishing_star", 2)
    @JvmField
    val METEORITE = legendaryItem("meteorite", 2)
    @JvmField
    val RARE_SEA_EGG = legendaryItem("rare_sea_egg", 2)
    @JvmField
    val NIGHTMARE_CORE = legendaryItem("nightmare_core", 2)
    @JvmField
    val GRACIDEA = legendaryItem("gracidea", 2)
    @JvmField
    val JEWEL_OF_LIFE = legendaryItem("jewel_of_life", 2)
    @JvmField
    val VICTORY_STAR = legendaryItem("victory_star", 2)
    @JvmField
    val RESOLUTE_SWORD = legendaryItem("resolute_sword", 2)
    @JvmField
    val RELIC_DISC = legendaryItem("relic_disc", 2)
    @JvmField
    val DISC_DRIVE = legendaryItem("disc_drive", 2)
    @JvmField
    val PINK_DIAMOND = legendaryItem("pink_diamond", 2)
    @JvmField
    val RING = legendaryItem("ring", 2)
    @JvmField
    val STEAM_ENGINE = legendaryItem("steam_engine", 2)
    @JvmField
    val SOUL_HEART = legendaryItem("soul_heart", 2)
    @JvmField
    val Z_SOUL = legendaryItem("z_soul", 2)
    @JvmField
    val MELTAN_NUT = legendaryItem("meltan_nut", 2)
    @JvmField
    val DADA_SCARF = legendaryItem("dada_scarf", 2)
    @JvmField
    val MYTHICAL_PECHA_BERRY = legendaryItem("mythical_pecha_berry", 2)
    @JvmField
    val GLACIAL_ORB = legendaryItem("glacial_orb", 2)
    @JvmField
    val STATIC_ORB = legendaryItem("static_orb", 2)
    @JvmField
    val FLARE_ORB = legendaryItem("flare_orb", 2)
    @JvmField
    val CLONING_CABLE = legendaryItem("cloning_cable", 2)
    @JvmField
    val SACRED_LIGHTNING = legendaryItem("sacred_lightning", 2)
    @JvmField
    val SACRED_FLAME = legendaryItem("sacred_flame", 2)
    @JvmField
    val SACRED_DROPLET = legendaryItem("sacred_droplet", 2)
    @JvmField
    val SILVER_WING = legendaryItem("silver_wing", 2)
    @JvmField
    val RAINBOW_WING = legendaryItem("rainbow_wing", 2)
    @JvmField
    val STEEL_ALLOY = legendaryItem("steel_alloy", 2)
    @JvmField
    val NEVER_MELT_ICICLE = legendaryItem("never_melt_icicle", 2)
    @JvmField
    val ANCIENT_INGOT = legendaryItem("ancient_ingot", 2)
    @JvmField
    val INFINITE_SOURCE = legendaryItem("infinite_source", 2)
    @JvmField
    val DRAGON_SKULL = legendaryItem("dragon_skull", 2)
    @JvmField
    val TITAN_TOTEM = legendaryItem("titan_totem", 2)
    @JvmField
    val BLUE_EON_TICKET = legendaryItem("blue_eon_ticket", 2)
    @JvmField
    val RED_EON_TICKET = legendaryItem("red_eon_ticket", 2)
    @JvmField
    val BLUE_ORB = legendaryItem("blue_orb", 2)
    @JvmField
    val RED_ORB = legendaryItem("red_orb", 2)
    @JvmField
    val JADE_ORB = legendaryItem("jade_orb", 2)
    @JvmField
    val RUBY_OF_WILLPOWER = legendaryItem("ruby_of_willpower", 2)
    @JvmField
    val RUBY_OF_EMOTION = legendaryItem("ruby_of_emotion", 2)
    @JvmField
    val RUBY_OF_KNOWLEDGE = legendaryItem("ruby_of_knowledge", 2)
    @JvmField
    val ADAMANT_ORB = legendaryItem("adamant_orb", 2)
    @JvmField
    val LUSTROUS_ORB = legendaryItem("lustrous_orb", 2)
    @JvmField
    val GRISEOUS_ORB = legendaryItem("griseous_orb", 2)
    @JvmField
    val MAGMA_CHUNK = legendaryItem("magma_chunk", 2)
    @JvmField
    val LUNAR_FEATHER = legendaryItem("lunar_feather", 2)
    @JvmField
    val COBALION_SWORD = legendaryItem("cobalion_sword", 2)
    @JvmField
    val VIRIZION_SWORD = legendaryItem("virizion_sword", 2)
    @JvmField
    val TERRAKION_SWORD = legendaryItem("terrakion_sword", 2)
    @JvmField
    val THUNDURUS_BOTTLE = legendaryItem("thundurus_bottle", 2)
    @JvmField
    val TORNADUS_BOTTLE = legendaryItem("tornadus_bottle", 2)
    @JvmField
    val LANDORUS_BOTTLE = legendaryItem("landorus_bottle", 2)
    @JvmField
    val ENAMORUS_BOTTLE = legendaryItem("enamorus_bottle", 2)
    @JvmField
    val LIGHT_STONE = legendaryItem("light_stone", 2)
    @JvmField
    val DARK_STONE = legendaryItem("dark_stone", 2)
    @JvmField
    val GRAY_STONE = legendaryItem("gray_stone", 2)
    @JvmField
    val TREE_OF_LIFE = legendaryItem("tree_of_life", 2)
    @JvmField
    val COCOON_OF_DESTRUCTION = legendaryItem("cocoon_of_destruction", 2)
    @JvmField
    val ZYGARDE_CUBE = legendaryItem("zygarde_cube", 2)
    @JvmField
    val BROKEN_MEMORY = legendaryItem("broken_memory", 2)
    @JvmField
    val ELECTRIC_TOTEM = legendaryItem("electric_totem", 2)
    @JvmField
    val PSYCHIC_TOTEM = legendaryItem("psychic_totem", 2)
    @JvmField
    val GRASS_TOTEM = legendaryItem("grass_totem", 2)
    @JvmField
    val WATER_TOTEM = legendaryItem("water_totem", 2)
    @JvmField
    val SOLAR_CORE = legendaryItem("solar_core", 2)
    @JvmField
    val LUNAR_CORE = legendaryItem("lunar_core", 2)
    @JvmField
    val ECLIPSE_CORE = legendaryItem("eclipse_core", 2)
    @JvmField
    val RUSTED_SWORD = legendaryItem("rusted_sword", 2)
    @JvmField
    val RUSTED_SHIELD = legendaryItem("rusted_shield", 2)
    @JvmField
    val DYNAMAX_CORE = legendaryItem("dynamax_core", 2)
    @JvmField
    val SCROLL_OF_DARKNESS = legendaryItem("scroll_of_darkness", 2)
    @JvmField
    val SCROLL_OF_WATERS = legendaryItem("scroll_of_waters", 2)
    @JvmField
    val MYSTICAL_BRANCH = legendaryItem("mystical_branch", 2)
    @JvmField
    val FROZEN_HOOF = legendaryItem("frozen_hoof", 2)
    @JvmField
    val GHOSTLY_HOOF = legendaryItem("ghostly_hoof", 2)
    @JvmField
    val KORAIDON_KEY = legendaryItem("koraidon_key", 2)
    @JvmField
    val MIRAIDON_KEY = legendaryItem("miraidon_key", 2)
    @JvmField
    val TOXIC_SCARF = legendaryItem("toxic_scarf", 2)
    @JvmField
    val TOXIC_HEADBAND = legendaryItem("toxic_headband", 2)
    @JvmField
    val TOXIC_RIBBON = legendaryItem("toxic_ribbon", 2)
    @JvmField
    val TEAL_MASK = maskItem("teal_mask")
    @JvmField
    val WELLSPRING_MASK = maskItem("wellspring_mask")
    @JvmField
    val HEARTHFLAME_MASK = maskItem("hearthflame_mask")
    @JvmField
    val CORNERSTONE_MASK = maskItem("cornerstone_mask")
    @JvmField
    val STELLAR_TERA_CORE = legendaryItem("stellar_tera_core", 2)
    @JvmField
    val PSYCHIC_ORB = legendaryItem("psychic_orb", 2)
    @JvmField
    val COMBAT_ORB = legendaryItem("combat_orb", 2)
    @JvmField
    val DARK_ORB = legendaryItem("dark_orb", 2)
    @JvmField
    val RUINOUS_SWORD = legendaryItem("ruinous_sword", 2)
    @JvmField
    val RUINOUS_BEADS = legendaryItem("ruinous_beads", 2)
    @JvmField
    val RUINOUS_TABLET = legendaryItem("ruinous_tablet", 2)
    @JvmField
    val RUINOUS_VESSEL = legendaryItem("ruinous_vessel", 2)
    @JvmField
    val ZERAORA_TUFT = legendaryItem("zeraora_tuft", 2)
    @JvmField
    val COSMIC_CORE = legendaryItem("cosmic_core", 2)
    @JvmField
    val COSMIC_FLUTE = legendaryItem("cosmic_flute", 2)
    @JvmField
    val ODD_SEA_EGG = legendaryItem("odd_sea_egg", 2)
    @JvmField
    val RKS_COMMUNICATOR = legendaryItem("rks_communicator", 2)
    @JvmField
    val SCROLL_OF_CHALLENGE = legendaryItem("scroll_of_challenge", 2)

    val COIN_ITEMS = mutableListOf<CoinItem>()

    @JvmField
    val BRONZE_COIN = coinItem("bronze_coin")
    @JvmField
    val SILVER_COIN = coinItem("silver_coin")
    @JvmField
    val GOLD_COIN = coinItem("gold_coin")

    val POKE_BAGS = mutableListOf<PokeBagItem>()

    @JvmField
    val SMALL_POKE_BAG = pokeBag("small_poke_bag", 3, Rarity.RARE)
    @JvmField
    val MEDIUM_POKE_BAG = pokeBag("medium_poke_bag", 4, Rarity.RARE)
    @JvmField
    val LARGE_POKE_BAG = pokeBag("large_poke_bag", 5, Rarity.RARE)
    @JvmField
    val HUGE_POKE_BAG = pokeBag("huge_poke_bag", 6, Rarity.EPIC)

    val TRANSFORMATION_ITEMS = mutableListOf<Item>()

    @JvmField
    val DRACO_PLATE = heldItem("draco_plate")
    @JvmField
    val DREAD_PLATE = heldItem("dread_plate")
    @JvmField
    val EARTH_PLATE = heldItem("earth_plate")
    @JvmField
    val FIST_PLATE = heldItem("fist_plate")
    @JvmField
    val FLAME_PLATE = heldItem("flame_plate")
    @JvmField
    val ICICLE_PLATE = heldItem("icicle_plate")
    @JvmField
    val INSECT_PLATE = heldItem("insect_plate")
    @JvmField
    val IRON_PLATE = heldItem("iron_plate")
    @JvmField
    val MEADOW_PLATE = heldItem("meadow_plate")
    @JvmField
    val MIND_PLATE = heldItem("mind_plate")
    @JvmField
    val PIXIE_PLATE = heldItem("pixie_plate")
    @JvmField
    val SKY_PLATE = heldItem("sky_plate")
    @JvmField
    val SPLASH_PLATE = heldItem("splash_plate")
    @JvmField
    val SPOOKY_PLATE = heldItem("spooky_plate")
    @JvmField
    val STONE_PLATE = heldItem("stone_plate")
    @JvmField
    val TOXIC_PLATE = heldItem("toxic_plate")
    @JvmField
    val ZAP_PLATE = heldItem("zap_plate")
    @JvmField
    val BUG_MEMORY = heldItem("bug_memory")
    @JvmField
    val DARK_MEMORY = heldItem("dark_memory")
    @JvmField
    val DRAGON_MEMORY = heldItem("dragon_memory")
    @JvmField
    val ELECTRIC_MEMORY = heldItem("electric_memory")
    @JvmField
    val FAIRY_MEMORY = heldItem("fairy_memory")
    @JvmField
    val FIGHTING_MEMORY = heldItem("fighting_memory")
    @JvmField
    val FIRE_MEMORY = heldItem("fire_memory")
    @JvmField
    val FLYING_MEMORY = heldItem("flying_memory")
    @JvmField
    val GHOST_MEMORY = heldItem("ghost_memory")
    @JvmField
    val GRASS_MEMORY = heldItem("grass_memory")
    @JvmField
    val GROUND_MEMORY = heldItem("ground_memory")
    @JvmField
    val ICE_MEMORY = heldItem("ice_memory")
    @JvmField
    val POISON_MEMORY = heldItem("poison_memory")
    @JvmField
    val PSYCHIC_MEMORY = heldItem("psychic_memory")
    @JvmField
    val ROCK_MEMORY = heldItem("rock_memory")
    @JvmField
    val STEEL_MEMORY = heldItem("steel_memory")
    @JvmField
    val WATER_MEMORY = heldItem("water_memory")
    @JvmField
    val BURN_DRIVE = heldItem("burn_drive")
    @JvmField
    val CHILL_DRIVE = heldItem("chill_drive")
    @JvmField
    val DOUSE_DRIVE = heldItem("douse_drive")
    @JvmField
    val SHOCK_DRIVE = heldItem("shock_drive")
    @JvmField
    val PRISON_BOTTLE = transformationItem("prison_bottle")
    @JvmField
    val BOOSTER_ENERGY = heldItem("booster_energy")
    @JvmField
    val HOOPA_RING = create("hoopa_ring", HoopaRingItem(Properties().stacksTo(16).rarity(Rarity.RARE)))

    private fun blockItem(name: String, block: Block, rarity: Rarity = Rarity.COMMON): BlockItem = this.create(name, BlockItem(block, Item.Properties().rarity(rarity)))

    private fun itemNameBlockItem(name: String, block: Block, rarity: Rarity = Rarity.COMMON): ItemNameBlockItem = this.create(name, ItemNameBlockItem(block, Item.Properties().rarity(rarity)))

    private fun noSettingsItem(name: String): CobblemonItem = this.create(name, CobblemonItem(Item.Properties()))

    private fun itemWithRarity(name: String, rarity: Rarity): CobblemonItem = this.create(name, CobblemonItem(Item.Properties().rarity(rarity)))
    
    fun berries() = this.berries.toMap()

    private fun mulchItem(name: String, mulchVariant: MulchVariant): MulchItem = this.create(name, MulchItem(mulchVariant))

    private fun pokeBallItem(pokeBall: PokeBall): PokeBallItem {
        val item = create(pokeBall.name.path, PokeBallItem(pokeBall))
        pokeBall.item = item
        pokeBalls.add(item)
        return item
    }
    fun teraShard(type: TeraType): TeraShardItem {
        val item = create("${type.name.lowercase()}_tera_shard", TeraShardItem(type))
        TERA_SHARDS.add(item)
        return item
    }

    fun megaStone(path: String, evolvablePokemon: String, aspect: String): MegaStoneItem {
        val item = create(path, MegaStoneItem(evolvablePokemon, aspect))
        CobblemonHeldItemManager.registerRemap(item, path)
        MEGASTONES.add(item)
        return item
    }

    fun maskItem(name: String): MaskItem {
        val item = create(name, MaskItem())
        LEGENDARY_ITEMS.add(item)
        return item
    }

    fun transformationItem(name: String): Item {
        val item = create(name, Item(Properties().stacksTo(1).rarity(Rarity.RARE)))
        TRANSFORMATION_ITEMS.add(item)
        return item
    }

    fun legendaryItem(name: String, durability: Int): LegendaryItem {
        val item = create(name, LegendaryItem(durability))
        LEGENDARY_ITEMS.add(item)
        return item
    }

    fun coinItem(name: String): CoinItem {
        val item = create(name, CoinItem())
        COIN_ITEMS.add(item)
        return item
    }

    fun pokeBag(path: String, size: Int, rarity: Rarity): PokeBagItem {
        val item = create(path, PokeBagItem(size, rarity))
        POKE_BAGS.add(item)
        return item
    }

    private fun candyItem(name: String, rarity: Rarity = Rarity.COMMON, calculator: CandyItem.Calculator): CandyItem  = this.create(name, CandyItem(rarity, calculator))

    private fun pokerodItem(pokeRodId: ResourceLocation, rarity: Rarity = Rarity.COMMON): PokerodItem {
        val settings = Item.Properties().stacksTo(1).durability(256).rarity(rarity)
        val item = create(pokeRodId.path, PokerodItem(pokeRodId, settings))
        pokeRods.add(item)
        return item
    }

    private fun pokedexItem(type: PokedexType): PokedexItem {
        val item = create("pokedex_${type.name.lowercase()}", PokedexItem(type))
        pokedexes.add(item)
        return item
    }

    private fun wearableItem(name: String, heldItemRemappedName: String? = null): CobblemonItem = create(
        name,
        WearableItem(name).also {
            wearables.add(it)
            if (heldItemRemappedName != null) {
                CobblemonHeldItemManager.registerRemap(it, heldItemRemappedName)
            }
        }
    )

    private fun campfirePotItem(block: Block, color: CampfirePotColor): CampfirePotItem {
        val item = create("campfire_pot_${color.suffix}", CampfirePotItem(block, color))
        campfire_pots.add(item)
        return item
    }

    private fun aprijuiceItem(type: Apricorn): AprijuiceItem {
        val item = create("aprijuice_${type.name.lowercase()}", AprijuiceItem(type))
        aprijuices.add(item)
        return item
    }

    private fun pokepuffItem(name: String): PokePuffItem {
        val item = create("poke_puff", PokePuffItem())
        return item
    }

    private fun regionalFoodItem(
        name: String,
        stacksTo: Int,
        nutrition: Int,
        saturationModifier: Float,
        alwaysEdible: Boolean = false,
        convertsToOnUse: ItemStack? = null
    ): RegionalFoodItem {
        val foodPropertiesBuilder = FoodProperties.Builder()
            .nutrition(nutrition)
            .saturationModifier(saturationModifier)

        if (alwaysEdible == true) {
            foodPropertiesBuilder.alwaysEdible()
        }

        if (convertsToOnUse != null && !convertsToOnUse.isEmpty) {
            foodPropertiesBuilder.usingConvertsTo(convertsToOnUse.item)
        }

        val properties = Item.Properties()
            .stacksTo(stacksTo)
            .food(foodPropertiesBuilder.build())

        return create(name, RegionalFoodItem(properties))
    }

    private fun heldItem(name: String, remappedName: String? = null): CobblemonItem = create(
        name,
        CobblemonItem(Item.Properties()).also {
            if (remappedName != null) {
                CobblemonHeldItemManager.registerRemap(it, remappedName)
            }
        }
    )

    private fun heldItem(name: String, item: Item, remappedName: String? = null) = create(
        name = name,
        entry = item.also {
            remappedName?.let { remappedName ->
                CobblemonHeldItemManager.registerRemap(it, remappedName)
            }
        }
    )

    private fun compostable(item: Item, increaseLevelChance: Float) = Cobblemon.implementation.registerCompostable(item, increaseLevelChance)

    private fun berryItem(name: String, berryBlock: BerryBlock): BerryItem {
        val finalName = "${name}_berry"
        val item = this.create(finalName, BerryItem(berryBlock))
        compostable(item, .65f)
        this.berries[cobblemonResource(finalName)] = item
        return item
    }

    private fun berryItem(name: String, berryItem: BerryItem): BerryItem {
        val finalName = "${name}_berry"
        val item = this.create(finalName, berryItem)
        compostable(item, .65f)
        this.berries[cobblemonResource(finalName)] = item
        return item
    }

    private fun mintItem(name: String, mintItem: MintItem): MintItem {
        val item = this.create(name, mintItem)
        mints[item.nature.displayName] = item
        compostable(item, .95f)
        return item
    }

    private fun hyperTrainingItem(name: String, increaseAmount: Int, targetStats: Set<Stat>, validRange: IntRange): HyperTrainingItem {
        val item = this.create(name, HyperTrainingItem(increaseAmount, targetStats, validRange))
        hyperTrainingItems.add(item)
        return item
    }

    private fun apricornItem(name: String, apricornItem: ApricornItem): ApricornItem {
        val finalName = "${name}_apricorn"
        val item = this.create(finalName, apricornItem)
        compostable(item, .65f)
        return item
    }

    private fun apricornSeedItem(name: String, apricornSeedItem: ApricornSeedItem): ApricornSeedItem {
        val finalName = "${name}_apricorn_seed"
        val item = this.create(finalName, apricornSeedItem)
        compostable(item, .30f)
        return item
    }


    private fun mintSeed(name: String, mintBlock: MintBlock): Item {
        val finalName = "${name}_mint_seeds"
        val item = this.blockItem(finalName, mintBlock)
        compostable(item, .30f)
        return item
    }

    private fun mintLeaf(name: String, mintLeafItem: MintLeafItem): MintLeafItem {
        val finalName = "${name}_mint_leaf"
        val item = this.create(finalName, mintLeafItem)
        compostable(item, .50f)
        return item
    }

    private fun compostableItem(name: String, item: Item? = null, increaseLevelChance: Float = .65f): Item {
        val createdItem = this.create(name, item ?: CobblemonItem(Item.Properties()))
        compostable(createdItem, increaseLevelChance)
        return createdItem
    }

    private fun compostableHeldItem(name: String, remappedName: String? = null, increaseLevelChance: Float = .65f): CobblemonItem {
        val createdItem = heldItem(name, remappedName)
        compostable(createdItem, increaseLevelChance)
        return createdItem
    }

    private fun compostableBlockItem(name: String, block: Block, increaseLevelChance: Float = .85f): Item {
        val createdItem = this.blockItem(name, block)
        compostable(createdItem, increaseLevelChance)
        return createdItem
    }

    private fun compostableItemNameBlockItem(name: String, block: Block, increaseLevelChance: Float = .30f): Item {
        val createdItem = this.itemNameBlockItem(name, block)
        compostable(createdItem, increaseLevelChance)
        return createdItem
    }

    private fun foodItem(nutrition: Int, saturationModifier: Float): Item {
        return CobblemonItem(Item.Properties().food(FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturationModifier).build()))
    }
}

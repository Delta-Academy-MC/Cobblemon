/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tags

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey

/**
 * A collection of the Cobblemon [TagKey]s related to the [Registries.ITEM].
 *
 * @author Licious
 * @since January 8th, 2023
 */
@Suppress("unused", "HasPlatformType")
object CobblemonItemTags {
    @JvmField val ABILITY_CHANGERS = create("ability_changers")
    @JvmField val ANCIENT_POKE_BALLS = create("ancient_poke_balls")
    @JvmField val APPLES = create("apples")
    @JvmField val APRICORN_LOGS = create("apricorn_logs")
    @JvmField val APRICORN_POKE_BALLS = create("apricorn_poke_balls")
    @JvmField val APRICORN_SPROUTS = create("apricorn_sprouts")
    @JvmField val APRICORNS = create("apricorns")
    @JvmField val APRIJUICES = create("aprijuices")
    /** This tag is only used for a Torterra aspect based easter egg evolution at the moment.
     * It simply includes the 'minecraft:azalea' and 'minecraft:flowering_azalea' items by default. */
    @JvmField val AZALEA_TREE = create("azalea_tree")
    @JvmField val BATTLE_ITEMS = create("battle_items")
    @JvmField val BERRIES = create("berries")
    @JvmField val BLACK_TUMBLESTONE_BRICKS = create("black_tumblestone_bricks")
    @JvmField val BLACK_TUMBLESTONES = create("black_tumblestones")
    @JvmField val BOATS = create("boats")
    /** This tag is used for Fossil Machine natural materials */
    @JvmField val COOKED_MEAT = create("cooked_meat")
    @JvmField val DAWN_STONE_ORES = create("dawn_stone_ores")
    @JvmField val POTTERY_SHERDS = create("decorated_pot_sherds")
    @JvmField val DEEP_SEAS = create("deep_seas")
    @JvmField val DISPLAY_CASE_ITEMS = create("display_case_items")
    @JvmField val DUSK_STONE_ORES = create("dusk_stone_ores")
    @JvmField val ETHERS = create("ethers")
    @JvmField val EVOLUTION_ITEMS = create("evolution_items")
    @JvmField val EVOLUTION_STONE_BLOCKS = create("evolution_stone_blocks")
    @JvmField val EVOLUTION_STONES = create("evolution_stones")
    @JvmField val EXPERIENCE_CANDIES = create("experience_candies")
    @JvmField val FEATHERS = create("feathers")
    @JvmField val FIRE_STONE_ORES = create("fire_stone_ores")
    @JvmField val FOSSIL_MACHINE_PARTS = create("fossil_machine_parts")
    @JvmField val FOSSILS = create("fossils")
    @JvmField val FULL_HEAL_INGREDIENTS = create("full_heal_ingredients")
    @JvmField val GILDED_CHESTS = create("gilded_chests")
    @JvmField val HANGING_SIGNS = create("hanging_signs")
    @JvmField val HERBS = create("herbs")
    @JvmField val ICE_STONE_ORES = create("ice_stone_ores")
    @JvmField val ITEM_BLOCKS = create("item_blocks")
    @JvmField val LEAF_STONE_ORES = create("leaf_stone_ores")
    @JvmField val MACHINES = create("machines")
    @JvmField val MINT_LEAF = create("mint_leaf")
    @JvmField val MINT_SEEDS = create("mint_seeds")
    @JvmField val MINTS = create("mints")
    @JvmField val MOCHIS = create("mochis")
    @JvmField val MOON_STONE_ORES = create("moon_stone_ores")
    @JvmField val MUTATED_BERRIES = create("mutated_berries")
    @JvmField val PLAQUES = create("plaques")
    @JvmField val PLANTS = create("plants")
    @JvmField val POKE_BALLS = create("poke_balls")
    @JvmField val POKE_RODS = create("poke_rods")
    @JvmField val POKEDEX = create("pokedex")
    @JvmField val POKEDEX_SCREEN = create("pokedex_screen")
    @JvmField val POKE_FOOD = create("poke_food")
    @JvmField val POTIONS = create("potions")
    @JvmField val PROTEIN_INGREDIENTS = create("protein_ingredients")
    /** See [COOKED_MEAT] */
    @JvmField val RAW_MEAT = create("raw_meat")
    @JvmField val RED_TUMBLESTONE_BRICKS = create("red_tumblestone_bricks")
    @JvmField val RED_TUMBLESTONES = create("red_tumblestones")
    @JvmField val REMEDIES = create("remedies")
    @JvmField val REMEDY_BERRIES = create("remedy_berries")
    @JvmField val RESTORES = create("restores")
    @JvmField val REVIVES = create("revives")
    @JvmField val SANDWICH_VEGGIE = create("sandwich_veggies")
    @JvmField val SEEDS = create("seeds")
    @JvmField val SHINY_STONE_ORES = create("shiny_stone_ores")
    @JvmField val SIGNS = create("signs")
    @JvmField val SKY_TUMBLESTONE_BRICKS = create("sky_tumblestone_bricks")
    @JvmField val SKY_TUMBLESTONES = create("sky_tumblestones")
    @JvmField val SUN_STONE_ORES = create("sun_stone_ores")
    @JvmField val SUPER_POTION_INGREDIENTS = create("super_potion_ingredients")
    @JvmField val FULL_HEAL_BOTTLES = create("full_heal_bottles")
    @JvmField val SWEETS = create("sweets")
    @JvmField val TEACUPS = create("teacups")
    @JvmField val TEAPOTS = create("teapots")
    @JvmField val THUNDER_STONE_ORES = create("thunder_stone_ores")
    @JvmField val TIER_1_POKE_BALL_MATERIALS = create("tier_1_poke_ball_materials")
    @JvmField val TIER_1_POKE_BALLS = create("tier_1_poke_balls")
    @JvmField val TIER_2_POKE_BALL_MATERIALS = create("tier_2_poke_ball_materials")
    @JvmField val TIER_2_POKE_BALLS = create("tier_2_poke_balls")
    @JvmField val TIER_3_POKE_BALL_MATERIALS = create("tier_3_poke_ball_materials")
    @JvmField val TIER_3_POKE_BALLS = create("tier_3_poke_balls")
    @JvmField val TIER_4_POKE_BALL_MATERIALS = create("tier_4_poke_ball_materials")
    @JvmField val TIER_4_POKE_BALLS = create("tier_4_poke_balls")
    @JvmField val TIER_5_POKE_BALLS = create("tier_5_poke_balls")
    @JvmField val TUMBLESTONE_BRICKS = create("tumblestone_bricks")
    @JvmField val TUMBLESTONES = create("tumblestones")
    @JvmField val TYPE_GEMS = create("type_gems")
    @JvmField val VITAMINS = create("vitamins")
    @JvmField val WATER_STONE_ORES = create("water_stone_ores")
    @JvmField val ZINC_INGREDIENTS = create("zinc_ingredients")

    // Held Item Tags
    @JvmField val CONSUMED_IN_NPC_BATTLE = create("held/consumed_in_npc_battle")
    @JvmField val CONSUMED_IN_PVP_BATTLE = create("held/consumed_in_pvp_battle")
    @JvmField val CONSUMED_IN_WILD_BATTLE = create("held/consumed_in_wild_battle")
    @JvmField val DESTINY_KNOT = create("held/destiny_knot")
    @JvmField val EVERSTONE = create("held/everstone")
    @JvmField val EXPERIENCE_SHARE = create("held/experience_share")
    @JvmField val IS_FRIENDSHIP_BOOSTER = create("is_friendship_booster")
    @JvmField val ANY_HELD_ITEM = create("held/is_held_item")
    @JvmField val BLACKLISTED_ITEMS_TO_HOLD = create("held/blacklisted_items_to_hold")
    @JvmField val WHITELISTED_ITEMS_TO_HOLD = create("held/whitelisted_items_to_hold")

    /** Tag that flags items as being able to "create" [CobblemonItems.LEFTOVERS]. */
    @JvmField val LEAVES_LEFTOVERS = create("held/leaves_leftovers")
    @JvmField val LUCKY_EGG = create("held/lucky_egg")
    @JvmField val POWER_ANKLET = create("held/power_anklet")
    @JvmField val POWER_BAND = create("held/power_band")
    @JvmField val POWER_BELT = create("held/power_belt")
    @JvmField val POWER_BRACER = create("held/power_bracer")
    @JvmField val POWER_LENS = create("held/power_lens")
    @JvmField val POWER_WEIGHT = create("held/power_weight")
    @JvmField val TERRAIN_SEEDS = create("held/terrain_seeds")
    //Held Item Visibility Tags
    @JvmField val WEARABLE_FACE_ITEMS = create("held/visibility/face")
    @JvmField val WEARABLE_HAT_ITEMS = create("held/visibility/hat")
    @JvmField val HIDDEN_ITEMS = create("held/visibility/hidden")

    //Empty Tag
    @JvmField val EMPTY = create("empty")


    private fun create(path: String) = TagKey.create(Registries.ITEM, cobblemonResource(path))

}
/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.tooltips

import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.CobblemonRecipeTypes
import com.cobblemon.mod.common.api.cooking.Flavour
import com.cobblemon.mod.common.api.cooking.Seasonings
import com.cobblemon.mod.common.api.fishing.SpawnBait
import com.cobblemon.mod.common.api.fishing.SpawnBaitEffects
import com.cobblemon.mod.common.api.fishing.SpawnBaitUtils
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.text.*
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.item.interactive.PokerodItem
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.util.lang
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffectUtil
import net.minecraft.world.item.ItemStack
import java.text.DecimalFormat

val cookingPotRecipeAbsorbHeader by lazy { lang("item_class.cooking_pot_recipe_absorbs").gray() }
val seasoningsHeader by lazy { lang("item_class.seasonings").gray() }
val flavorSeasoningHeader by lazy { lang("item_class.flavor_seasoning").blue() }
val flavorHeader by lazy { lang("seasoning_flavor_header").gray() }
val flavorInfoSubHeader by lazy { lang("seasoning_flavor_info_header").blue() }
val baitEffectSeasoningHeader by lazy { lang("item_class.bait_effect_seasoning").blue() }
val baitEffectHeader by lazy { lang("seasoning_bait_effect_header").gray() }
val baitEffectInfoSubHeader by lazy { lang("seasoning_bait_effect_info_header").blue() }
val foodSeasoningHeader by lazy { lang("item_class.food_seasoning").blue() }
val foodHeader by lazy { lang("seasoning_food_header").gray() }
val mobEffectSeasoningHeader by lazy { lang("item_class.mob_effect_seasoning").blue() }
val mobEffectHeader by lazy { lang("seasoning_mob_effect_header").gray() }
val rideBoostSeasoningHeader by lazy { lang("seasoning_ride_boosts_info_header").blue() }

private fun recipeUsesProcessor(stack: ItemStack, processorType: String): Boolean {
    val level = Minecraft.getInstance().level ?: return false
    val recipeManager = level.recipeManager

    val allCookingRecipes = recipeManager.getAllRecipesFor(CobblemonRecipeTypes.COOKING_POT_COOKING) +
            recipeManager.getAllRecipesFor(CobblemonRecipeTypes.COOKING_POT_SHAPELESS)

    return allCookingRecipes
        .any { recipe ->
            stack.item == recipe.value.result.item &&
                    recipe.value.seasoningProcessors.any { it.type == processorType }
        }
}

fun itemTakesSeasoningData(stack: ItemStack): Boolean {
    return recipeUsesFlavor(stack) || recipeUsesFood(stack) || recipeUsesMobEffect(stack) || recipeUsesBaitEffect(stack)
}

fun recipeUsesFlavor(stack: ItemStack): Boolean {
    return recipeUsesProcessor(stack, "flavour")
}

fun recipeUsesFood(stack: ItemStack): Boolean {
    return recipeUsesProcessor(stack, "food")
}

fun recipeUsesMobEffect(stack: ItemStack): Boolean {
    return recipeUsesProcessor(stack, "mob_effects")
}

fun recipeUsesBaitEffect(stack: ItemStack): Boolean {
    return recipeUsesProcessor(stack, "spawn_bait")
}

fun generateSeasoningAbsorbtionTooltip(accepted: List<Component>): Component {
    if (accepted.isEmpty()) return Component.empty()
    if (accepted.size == 1) return lang("item_class.cooking_pot_recipe_absorbs")
            .append(" ").append(accepted[0]).append(" ")
            .append(seasoningsHeader).gray()

    val result = Component.literal("")

    result.append(lang("item_class.cooking_pot_recipe_absorbs")).append(" ").gray()

    for ((index, component) in accepted.withIndex()) {
        when (index) {
            accepted.lastIndex -> {
                result.append("and ").append(component)
            }
            accepted.lastIndex - 1 -> {
                result.append(component).append(", ")
            }
            else -> {
                result.append(component).append(", ")
            }
        }
        if (index == accepted.lastIndex) {
            result.append(" ").append(seasoningsHeader).gray()
        }
    }

    return result
}

fun generateAdditionalFlavorTooltip(flavours: Map<Flavour, Int>): MutableList<Component> {
    val resultLines = mutableListOf<Component>()
    resultLines.add(flavorInfoSubHeader)

    val combinedFlavorsLine = Component.literal("")
    flavours.filter { it.key != Flavour.MILD }.forEach { (flavour, value) ->
        val flavourText = lang("seasoning_flavor.${flavour.name.lowercase()}").setStyle(Style.EMPTY.withColor(flavour.colour))
        if (combinedFlavorsLine.string.isNotEmpty()) {
            combinedFlavorsLine.append(" ")
        }

        combinedFlavorsLine.append(flavourText).append(" $value")
    }

    resultLines.add(combinedFlavorsLine)
    return resultLines
}

fun generateAdditionalBaitEffectTooltip(stack: ItemStack): MutableList<Component> {
    val resultLines = mutableListOf<Component>()

    val rawEffects = mutableListOf<SpawnBait.Effect>().apply {
        if (Seasonings.isSeasoning(stack)) {
            val seasoningEffects = Seasonings.getBaitEffectsFromItemStack(stack)
            if (seasoningEffects.isNotEmpty()) {
                addAll(seasoningEffects)
            }
        } else if (stack.item is PokerodItem) {
            addAll(SpawnBaitEffects.getEffectsFromRodItemStack(stack))
        } else {
            addAll(SpawnBaitEffects.getEffectsFromItemStack(stack))
        }
    }

    val baitEffects = SpawnBaitUtils.mergeEffects(rawEffects)

    if (baitEffects.isNotEmpty()) {
        resultLines.add(baitEffectInfoSubHeader)

        val formatter = DecimalFormat("0.##")

        val genders = mapOf(
                Gender.MALE to lang("gender.male"),
                Gender.FEMALE to lang("gender.female"),
                Gender.GENDERLESS to lang("gender.genderless")
        )

        for (effect in baitEffects) {
            val effectType = effect.type.path
            val effectSubcategory = effect.subcategory?.path
            val effectChance = effect.chance * 100
            var effectValue = when (effectType) {
                "bite_time" -> (effect.value * 100).toInt()
                "shiny_reroll" -> (effect.value + 1).toInt()
                else -> effect.value.toInt()
            }

            val subcategoryString: Component = if (effectSubcategory != null) {
                when (effectType) {
                    "nature", "ev", "iv" ->
                        com.cobblemon.mod.common.api.pokemon.stats.Stats.getStat(effectSubcategory).displayName

                    "gender_chance" ->
                        genders[Gender.valueOf(effectSubcategory.uppercase())]

                    "typing" ->
                        ElementalTypes.get(effectSubcategory)?.displayName

                    "egg_group" -> {
                        val eggGroup = EggGroup.fromIdentifier(effectSubcategory)
                        eggGroup?.let {
                            lang("egg_group.${it.name.lowercase()}")
                        } ?: Component.literal(effectSubcategory).gold()
                    }

                    else -> Component.empty()
                } ?: Component.literal("cursed").obfuscate()
            } else Component.literal("cursed").obfuscate()

            resultLines.add(lang(
                "fishing_bait_effects.$effectType.tooltip",
                Component.literal(formatter.format(effectChance)).yellow(),
                subcategoryString.copy().gold(),
                Component.literal(formatter.format(effectValue)).green()
            ))
        }
    }

    return resultLines
}

fun generateAdditionalRideBoostsTooltip(stack: ItemStack): MutableList<Component> {
    val boosts = stack.get(CobblemonItemComponents.RIDE_BOOST)?.boosts
        ?: return mutableListOf()

    val resultLines = mutableListOf<Component>()
    resultLines.add(rideBoostSeasoningHeader)

    for ((stat, value) in boosts.toSortedMap()) {
        val statName = stat.displayName.also { it.style = it.style.withColor(stat.flavour.colour) }
        val valueText = if (value < 0) {
            Component.literal("$value").red()
        } else {
            Component.literal("+$value").green()
        }

        resultLines.add(lang("seasoning_ride_boost_entry", statName, valueText))
    }

    return resultLines
}

private fun getRomanNumeral(number: Int): String {
    if (number <= 0) return number.toString()

    val numerals = listOf(
            1000 to "M",
            900 to "CM",
            500 to "D",
            400 to "CD",
            100 to "C",
            90 to "XC",
            50 to "L",
            40 to "XL",
            10 to "X",
            9 to "IX",
            5 to "V",
            4 to "IV",
            1 to "I"
    )

    var remaining = number
    val result = StringBuilder()

    for ((value, numeral) in numerals) {
        while (remaining >= value) {
            result.append(numeral)
            remaining -= value
        }
    }

    return result.toString()
}
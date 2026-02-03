/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex.widgets

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.riding.RidingProperties
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.HALF_OVERLAY_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.POKEMON_DESCRIPTION_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCALE
import com.cobblemon.mod.common.client.gui.pokedex.ScaledButton
import com.cobblemon.mod.common.client.gui.pokedex.renderTooltip
import com.cobblemon.mod.common.client.gui.summary.widgets.SoundlessWidget
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.drawScaledTextJustifiedRight
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.util.FastColor

class StatsWidget(val pX: Int, val pY: Int) : SoundlessWidget(
    pX,
    pY,
    HALF_OVERLAY_WIDTH,
    POKEMON_DESCRIPTION_HEIGHT,
    lang("ui.pokedex.pokemon_info")
) {

    companion object {
        const val STAT_TYPE_BASE = 0
        const val STAT_TYPE_RIDE = 1
        const val MAX_BAR_WIDTH = 75

        private val statTypes = listOf(STAT_TYPE_BASE, STAT_TYPE_RIDE)

        private val divider = cobblemonResource("textures/gui/pokedex/info_stat_divider.png")
        private val arrowLeft = cobblemonResource("textures/gui/pokedex/info_arrow_left.png")
        private val arrowRight = cobblemonResource("textures/gui/pokedex/info_arrow_right.png")
        private val subArrowLeft = cobblemonResource("textures/gui/pokedex/info_sub_arrow_left.png")
        private val subArrowRight = cobblemonResource("textures/gui/pokedex/info_sub_arrow_right.png")

        private val statLabels = arrayOf(
            lang("ui.stats.hp"),
            lang("ui.stats.atk"),
            lang("ui.stats.def"),
            lang("ui.stats.sp_atk"),
            lang("ui.stats.sp_def"),
            lang("ui.stats.speed")
        )

        private val stats = arrayOf(
            Stats.HP,
            Stats.ATTACK,
            Stats.DEFENCE,
            Stats.SPECIAL_ATTACK,
            Stats.SPECIAL_DEFENCE,
            Stats.SPEED
        )
    }

    val leftButton: ScaledButton = ScaledButton(
        pX + 2.5F,
        pY - 8F,
        7,
        10,
        arrowLeft,
        clickAction = { switchStatType(false) }
    )

    val rightButton: ScaledButton = ScaledButton(
        pX + 133F,
        pY - 8F,
        7,
        10,
        arrowRight,
        clickAction = { switchStatType(true) }
    )

    val leftSubButton: ScaledButton = ScaledButton(
        pX + 2.5F,
        pY + 18.5F,
        7,
        10,
        subArrowLeft,
        clickAction = { switchRideBehaviour(false) }
    )

    val rightSubButton: ScaledButton = ScaledButton(
        pX + 133F,
        pY + 18.5F,
        7,
        10,
        subArrowRight,
        clickAction = { switchRideBehaviour(true) }
    )

    var selectedRideBehavioursIndex: Int = 0

    var selectedStatTypeIndex: Int = STAT_TYPE_BASE
        set(value) {
            field = value

            val isRideStatVisible = value == STAT_TYPE_RIDE
            leftSubButton.visible = isRideStatVisible
            rightSubButton.visible = isRideStatVisible
        }

    var baseStats: Map<Stat, Int>? = null
    var rideProperties: RidingProperties? = null

    private fun switchStatType(nextIndex: Boolean) {
        val size = statTypes.size
        selectedStatTypeIndex = if (nextIndex) {
            (selectedStatTypeIndex + 1) % size
        } else {
            (selectedStatTypeIndex - 1 + size) % size
        }

        val showSubArrowButtons = selectedStatTypeIndex == STAT_TYPE_RIDE
        leftSubButton.visible = showSubArrowButtons
        rightSubButton.visible = showSubArrowButtons
    }

    private fun switchRideBehaviour(nextIndex: Boolean) {
        val size = rideProperties?.behaviours?.size ?: 0
        selectedRideBehavioursIndex = if (nextIndex) {
            (selectedRideBehavioursIndex + 1) % size
        } else {
            (selectedRideBehavioursIndex - 1 + size) % size
        }
    }

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        when (selectedStatTypeIndex) {
            STAT_TYPE_BASE -> {
                renderBaseStats(context)
            }
            STAT_TYPE_RIDE -> {
                renderRideStats(context, mouseX, mouseY, delta)
            }
        }
    }

    private fun renderBaseStats(context: GuiGraphics) {
        if (baseStats == null) return

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = Component.translatable("cobblemon.ui.pokedex.info.stats").bold(),
            x = pX + 9,
            y = pY - 10,
            shadow = true
        )

        val matrices = context.pose()

        blitk(matrixStack = matrices, texture = divider, x = pX + 51, y = pY + 4, height = 33.5, width = 1)

        for (i in statLabels.indices) {
            drawScaledText(
                context = context,
                text = statLabels[i].bold(),
                x = pX + 9,
                y = pY + 4 + (6 * i),
                colour = 0x606B6E,
                scale = SCALE
            )

            val statValue = baseStats!![stats[i]]

            if (statValue != null) {
                drawScaledTextJustifiedRight(
                    context = context,
                    text = statValue.toString().text(),
                    x = pX + 48,
                    y = pY + 4 + (6 * i),
                    colour = 0x3A96B6,
                    scale = SCALE
                )

                val (red, green, blue) = getStatValueRGB(statValue)
                blitk(
                    matrixStack = matrices,
                    texture = CobblemonResources.WHITE,
                    x = pX + 55,
                    y = pY + 4 + (6 * i),
                    height = 3.5F,
                    width = (statValue / 255F) * MAX_BAR_WIDTH,
                    red = red,
                    green = green,
                    blue = blue
                )
            }
        }
    }

    private fun renderRideStats(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        rideProperties?.behaviours?.let { behaviours ->
            val selectedBehaviour = behaviours.entries.toList()[selectedRideBehavioursIndex]
            val behaviourStats = selectedBehaviour.value.stats

            val matrices = context.pose()

            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = Component.translatable("cobblemon.ui.pokedex.info.stats_ride").bold(),
                x = pX + 9,
                y = pY - 10,
                shadow = true
            )

            rideProperties?.seats?.size?.let { seats ->
                drawScaledTextJustifiedRight(
                    context = context,
                    font = CobblemonResources.DEFAULT_LARGE,
                    text = Component.translatable("cobblemon.ui.pokedex.info.stats_ride.seats", seats).bold(),
                    x = pX + 130,
                    y = pY - 10,
                    shadow = true
                )
            }

            blitk(matrixStack = matrices, texture = divider, x = pX + 51, y = pY + 10, height = 27.5, width = 1)
            blitk(matrixStack = matrices, texture = divider, x = pX + 9, y = pY + 2, height = 6, width = 121)

            val rideStyle = selectedBehaviour.value.key.path.toString().split("/").last().lowercase()
            drawScaledText(
                context = context,
                text = lang("ui.ride_style.${selectedBehaviour.key.toString().lowercase()}.${rideStyle}")
                    .append(" | ").append(lang("ui.ride_style.${selectedBehaviour.key.toString().lowercase()}")).bold(),
                x = pX + 69.5,
                y = pY + 3,
                colour = 0x3A96B6,
                scale = SCALE,
                centered = true
            )

            RidingStat.entries.forEachIndexed { index, stat ->
                drawScaledText(
                    context = context,
                    text =  lang("ui.stats.ride.${stat.name.lowercase()}").bold(),
                    x = pX + 9,
                    y = pY + 10 + (6 * index),
                    colour = 0x606B6E,
                    scale = SCALE
                )

                behaviourStats[stat]?.let { range ->
                    val startPosY = pY + 10 + (6F * index)

                    val red = FastColor.ARGB32.red(stat.flavour.colour) / 255F
                    val green = FastColor.ARGB32.green(stat.flavour.colour) / 255F
                    val blue = FastColor.ARGB32.blue(stat.flavour.colour) / 255F

                    val barStart = pX + 55F
                    val baseStatWidth = (range.start / 100F) * MAX_BAR_WIDTH
                    val boostedStatWidth = ((range.endInclusive / 100F) * MAX_BAR_WIDTH) - baseStatWidth
                    blitk(
                        matrixStack = matrices,
                        texture = CobblemonResources.WHITE,
                        x = barStart,
                        y = startPosY,
                        height = 3.5F,
                        width = baseStatWidth,
                        red = red,
                        green = green,
                        blue = blue
                    )

                    val isBoostedStatHovered = mouseX.toFloat() in ((barStart + baseStatWidth)..(barStart + baseStatWidth + boostedStatWidth)) && mouseY.toFloat() in (startPosY..(startPosY + 3.5F))

                    drawScaledTextJustifiedRight(
                        context = context,
                        text = if (isBoostedStatHovered) range.endInclusive.toString().text().bold() else range.start.toString().text(),
                        x = pX + 48,
                        y = startPosY,
                        colour = 0x3A96B6,
                        scale = SCALE
                    )

                    // Boostable range
                    blitk(
                        matrixStack = matrices,
                        texture = CobblemonResources.WHITE,
                        x = barStart + baseStatWidth,
                        y = startPosY,
                        height = 3.5F,
                        width = boostedStatWidth,
                        red = red,
                        green = green,
                        blue = blue,
                        alpha = if (isBoostedStatHovered) 1F else 0.5F
                    )

                    if (isBoostedStatHovered) renderTooltip(
                        context,
                        lang("ui.stats.ride.boost_maximum").bold(),
                        mouseX,
                        mouseY,
                        delta,
                        -16
                    )
                }
            }
        } ?: run {
            // If ride properties null, reset index to base stats
            selectedStatTypeIndex = STAT_TYPE_BASE
        }
    }

    private fun getStatValueRGB(value: Int): Triple<Float, Float, Float> {
        when {
            value >= 150 -> return Triple(0.647F, 0.929F, 0.647F)
            value >= 120 -> return Triple(0.8F, 0.937F, 0.423F)
            value >= 100 -> return Triple(0.93F, 0.91F, 0.415F)
            value >= 80 -> return Triple(0.965F, 0.812F, 0.423F)
        }
        return Triple(0.964F, 0.64F, 0.502F)
    }
}

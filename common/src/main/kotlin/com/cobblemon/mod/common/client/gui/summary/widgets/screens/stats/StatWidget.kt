/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.summary.widgets.screens.stats

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures
import com.cobblemon.mod.common.api.pokemon.feature.SynchronizedSpeciesFeatureProvider
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.summary.SummaryButton
import com.cobblemon.mod.common.client.gui.summary.featurerenderers.BarSummarySpeciesFeatureRenderer
import com.cobblemon.mod.common.client.gui.summary.widgets.SoundlessWidget
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.stats.features.FriendshipFeatureRenderer
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.stats.features.FullnessFeatureRenderer
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.pokemon.EVs
import com.cobblemon.mod.common.pokemon.IVs
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.Mth.ceil
import net.minecraft.util.Mth.floor
import net.minecraft.world.phys.Vec2
import org.joml.Vector3f

class StatWidget(
    pX: Int, pY: Int,
    val pokemon: Pokemon,
    val tabIndex: Int = 0
): SoundlessWidget(pX, pY, WIDTH, HEIGHT, Component.literal("StatWidget")) {

    companion object {
        // Stat tab options
        private const val STATS = "stats"
        private const val IV = "ivs"
        private const val EV = "evs"
        private const val RIDE = "ride"
        private const val OTHER = "other"

        private const val OTHER_STAT_BARS_PER_PAGE = 4
        private const val STAT_TAB_WIDTH_6 = 22
        private const val STAT_TAB_WIDTH_5 = 24
        private const val MARKER_WIDTH = 8
        private const val WIDTH = 134
        private const val HEIGHT = 148
        private const val RIDE_ICON_SIZE = 32
        const val SCALE = 0.5F

        private const val WHITE = 0x00FFFFFF
        private const val GREY = 0x008F8F8F
        private const val BLUE = 0x00548BFB
        private const val RED = 0x00FB5454

        private val statsBaseResource = cobblemonResource("textures/gui/summary/summary_stats_chart_base.png")
        private val statsChartResource = cobblemonResource("textures/gui/summary/summary_stats_chart.png")
        private val statsChartPentagonResource = cobblemonResource("textures/gui/summary/summary_stats_chart_pentagon.png")
        private val statsChartPentagonHighlightResource = cobblemonResource("textures/gui/summary/summary_stats_chart_pentagon_highlight.png")
        private val statsOtherBaseResource = cobblemonResource("textures/gui/summary/summary_stats_other_base.png")
        private val statsOtherSidebarsResource = cobblemonResource("textures/gui/summary/summary_stats_other_sidebars.png")
        private val statsOtherSidebarArrowLeft = cobblemonResource("textures/gui/summary/summary_stats_other_sidebar_arrow_left.png")
        private val statsOtherSidebarArrowRight = cobblemonResource("textures/gui/summary/summary_stats_other_sidebar_arrow_right.png")
        private val tabMarkerResource = cobblemonResource("textures/gui/summary/summary_stats_tab_marker.png")
        private val statIncreaseResource = cobblemonResource("textures/gui/summary/summary_stats_icon_increase.png")
        private val statDecreaseResource = cobblemonResource("textures/gui/summary/summary_stats_icon_decrease.png")

        private val statLabels: Map<MutableComponent, Stats> = mapOf(
            lang("ui.stats.hp").bold() to Stats.HP,
            lang("ui.stats.atk").bold() to Stats.ATTACK,
            lang("ui.stats.def").bold() to Stats.DEFENCE,
            lang("ui.stats.speed").bold() to Stats.SPEED,
            lang("ui.stats.sp_def").bold() to Stats.SPECIAL_DEFENCE,
            lang("ui.stats.sp_atk").bold() to Stats.SPECIAL_ATTACK
        )

        private val hexagonVerticesOffset = listOf(
            Pair(67.0, 10.5), // 12 o'clock
            Pair(122.0, 42.5), // 2 o'clock
            Pair(122.0, 93.5), // 4 o'clock
            Pair(67.0, 124.5), // 6 o'clock
            Pair(12.0, 93.5), // 8 o'clock
            Pair(12.0, 42.5) // 10 o'clock
        )

        private val pentagonVerticesOffset = listOf(
            Pair(67.0, 10.5), // 12 o'clock
            Pair(123.0, 47.5), // 2 o'clock
            Pair(103.0, 112.5), // 5 o'clock
            Pair(31.0, 112.5), // 7 o'clock
            Pair(11.0, 47.5), // 10 o'clock
        )
    }

    private val statOptions = if (pokemon.form.riding.behaviours != null) listOf(STATS, IV, EV, RIDE, OTHER) else listOf(STATS, IV, EV, OTHER)

    private var _statTabIndex: Int = tabIndex
    var statTabIndex: Int
        get() = _statTabIndex.coerceIn(0, statOptions.size - 1)
        set(value) {
            _statTabIndex = value.coerceIn(0, statOptions.size - 1)
        }

    private var _rideBehaviourIndex: Int = 0
    var rideBehaviourIndex: Int
        get() = _rideBehaviourIndex.coerceIn(0, (pokemon.form.riding.behaviours?.size ?: 1) - 1)
        set(value) {
            _rideBehaviourIndex = value.coerceIn(0, (pokemon.form.riding.behaviours?.size ?: 1) - 1)
        }

    val universalFeatures = listOf(
        FriendshipFeatureRenderer(pokemon),
        FullnessFeatureRenderer(pokemon),
    )

    val renderableFeatures = SpeciesFeatures
        .getFeaturesFor(pokemon.species)
        .filterIsInstance<SynchronizedSpeciesFeatureProvider<*>>()
        .mapNotNull { it.getRenderer(pokemon) }

    var otherStatsPageIndex: Int = 0

    var otherStatLeftButton: SummaryButton? = null
    var otherStatRightButton: SummaryButton? = null

    init {
        if (renderableFeatures.size + universalFeatures.size > OTHER_STAT_BARS_PER_PAGE) {
            otherStatLeftButton = SummaryButton(
                x - 1F,
                y + 67F,
                14,
                14,
                resource = statsOtherSidebarArrowLeft,
                scale = SCALE,
                clickAction = { switchOtherStatsPage(false) }
            ).also { addWidget(it) }

            otherStatRightButton = SummaryButton(
                x + 128F,
                y + 67F,
                14,
                14,
                resource = statsOtherSidebarArrowRight,
                scale = SCALE,
                clickAction = { switchOtherStatsPage(true) }
            ).also { addWidget(it) }
        }
    }

    private fun switchOtherStatsPage(nextIndex: Boolean) {
        val pages = ceil((renderableFeatures.size + universalFeatures.size).toDouble() / OTHER_STAT_BARS_PER_PAGE.toDouble())
        otherStatsPageIndex = if (nextIndex) {
            (otherStatsPageIndex + 1) % pages
        } else {
            (otherStatsPageIndex - 1 + pages) % pages
        }
    }

    private fun drawTriangle(colour: Vector3f, v1: Vec2, v2: Vec2, v3: Vec2, opacity: Float = 0.6F) {
        CobblemonResources.WHITE.let { RenderSystem.setShaderTexture(0, it) }
        RenderSystem.setShaderColor(colour.x, colour.y, colour.z, opacity)
        val bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION)
        bufferBuilder.addVertex(v1.x, v1.y, 10F)
        bufferBuilder.addVertex(v2.x, v2.y, 10F)
        bufferBuilder.addVertex(v3.x, v3.y, 10F)
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow())
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F)
    }

    /**
     * Render a 5 or 6 sided polygon
     *
     * @param ratios list of ratios for how far each vertex is from the center, starting from top vertex going clockwise
     * @param colour the colour to render
     */
    private fun drawStatPolygon(ratios: List<Float>, colour: Vector3f) {
        val sides = ratios.size
        if (sides !in 5 .. 6) return

        val topVertexY = y + 22F
        val centerX = x + 67F
        val centerY = topVertexY + if (sides > 5) 48F else 49F

        val radius = centerY - topVertexY

        val startAngleDeg = -90.0
        val angleStepDeg = 360.0 / sides

        // Generate the angles for each vertex
        val anglesDeg = List(sides) { i -> startAngleDeg + i * angleStepDeg }
        val anglesRad = anglesDeg.map { Math.toRadians(it) }

        // Calculate maximum points on the circle
        val maxPoints = anglesRad.map { angle ->
            Vec2(
                centerX + radius * Math.cos(angle).toFloat(),
                centerY + radius * Math.sin(angle).toFloat()
            )
        }

        // Coerce values as 0.0 will prevent triangles from rendering
        val coercedRatios = ratios.map { it.coerceIn(5 / radius, 1F) }

        // Interpolate vertices based on ratios
        val vertices = maxPoints.mapIndexed { index, maxPoint ->
            Vec2(
                centerX + (maxPoint.x - centerX) * coercedRatios[index],
                centerY + (maxPoint.y - centerY) * coercedRatios[index]
            )
        }

        val centerPoint = Vec2(centerX, centerY)

        // Draw triangles between each vertex and the next clockwise starting from top vertex
        RenderSystem.disableDepthTest()
        for (i in vertices.indices) {
            val nextIndex = (i + 1) % vertices.size
            drawTriangle(colour, vertices[i], centerPoint, vertices[nextIndex])
        }
        RenderSystem.enableDepthTest()
    }

    override fun renderWidget(context: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTicks: Float) {
        val renderOtherStats = statOptions.get(statTabIndex) == OTHER
        val renderPentagonStats = statOptions.get(statTabIndex) == RIDE
        val matrices = context.pose()

        // Background
        blitk(
            matrixStack = matrices,
            texture = if (renderOtherStats)  statsOtherBaseResource else statsBaseResource,
            x= x,
            y = y,
            width = width,
            height = height
        )

        if (renderOtherStats) {
            val barOffsetY = 29
            val barPosX = x + 9F
            var drawY = y + 15F

            val featuresList: MutableList<Any> = renderableFeatures.filter {
                !pokemon.aspects.contains("hide-" + it.name) && (it.name != "blocks_traveled" || pokemon.hasBlocksTraveledRequirement())
            }.toMutableList()

            featuresList.addAll(0, universalFeatures.filter {
                !pokemon.aspects.contains("hide-" + it.name)
            })

            val pageFeatures = featuresList.subList(
                otherStatsPageIndex * OTHER_STAT_BARS_PER_PAGE,
                minOf((otherStatsPageIndex * OTHER_STAT_BARS_PER_PAGE) + OTHER_STAT_BARS_PER_PAGE, featuresList.size)
            )

            for (feature in pageFeatures) {
                if (feature is BarSummarySpeciesFeatureRenderer) {
                    val rendered = feature.render(
                        guiGraphics = context,
                        x = barPosX,
                        y = drawY,
                        pokemon = pokemon
                    )
                    if (rendered) drawY += barOffsetY
                }
            }
            if (featuresList.size > 4) {
                blitk(
                    matrixStack = matrices,
                    texture = statsOtherSidebarsResource,
                    x= x,
                    y = y + 13,
                    width = 134,
                    height = 115
                )
                otherStatLeftButton?.renderWidget(context, pMouseX, pMouseY, pPartialTicks)
                otherStatRightButton?.renderWidget(context, pMouseX, pMouseY, pPartialTicks)
            }
        } else {
            if (renderPentagonStats) {
                pokemon.form.riding.behaviours?.let {
                    val behaviours = it.entries.toList()
                    val selectedBehaviour = behaviours[rideBehaviourIndex]
                    val canSwitchStyle = behaviours.size > 1 && centerHovered(pMouseX, pMouseY)

                    blitk(
                        matrixStack = matrices,
                        texture = if (canSwitchStyle) statsChartPentagonHighlightResource else statsChartPentagonResource,
                        x= (x + 20.5) / SCALE,
                        y = (y + 22) / SCALE,
                        width = 186,
                        height = 176,
                        scale = SCALE
                    )

                    val pentagonColour = when (selectedBehaviour.key) {
                        RidingStyle.AIR -> {
                            if (canSwitchStyle)
                                Vector3f(70F/255F, 235F/225F, 195F/255F)
                            else Vector3f(40F/255F, 205F/255F, 165F/255F)
                        }
                        RidingStyle.LIQUID -> {
                            if (canSwitchStyle)
                                Vector3f(95F/255F, 165F/255F, 1F)
                            else Vector3f(65F/255F, 135F/255F, 1F)
                        }
                        else -> {
                            if (canSwitchStyle)
                                Vector3f(1F, 195F/255F, 30F/255F)
                            else Vector3f(1F, 165F/255F, 0F)
                        }
                    }

                    drawStatPolygon(
                        ratios = RidingStat.entries.map { pokemon.getRideStat(selectedBehaviour.key, it) / 100F },
                        colour = pentagonColour
                    )

                    val rideStyle = selectedBehaviour.value.key.path.toString().split("/").last().lowercase()
                    val iconResource = cobblemonResource("textures/gui/summary/icon_ride_${selectedBehaviour.key.toString().lowercase()}_${rideStyle}.png")
                    blitk(
                        matrixStack = matrices,
                        texture = iconResource,
                        x= (x + 59) / SCALE,
                        y = (y + 62.5) / SCALE,
                        width = RIDE_ICON_SIZE,
                        height = RIDE_ICON_SIZE,
                        scale = SCALE
                    )

                    // Stat Labels
                    renderPolygonLabels(
                        context = context,
                        labels = RidingStat.entries.toList().map { stat ->
                            if (statLabelsHovered(pentagonVerticesOffset, pMouseX, pMouseY))
                            "${floor(pokemon.getRideStat(selectedBehaviour.key, stat))}/${selectedBehaviour.value.stats[stat]?.endInclusive}".text()
                            else lang("ui.stats.ride.${stat.name.lowercase()}").bold()
                        },
                        verticesOffset = pentagonVerticesOffset
                    )

                    // Stat Values
                    renderPolygonLabels(
                        context = context,
                        labels = RidingStat.entries.toList().map { stat ->
                            if (statLabelsHovered(pentagonVerticesOffset, pMouseX, pMouseY))
                                "+${floor(pokemon.getRideBoost(stat) / pokemon.getMaxRideBoost(stat) * 100)}%".text()
                            else floor(pokemon.getRideStat(selectedBehaviour.key, stat)).toString().text()
                        },
                        verticesOffset = pentagonVerticesOffset,
                        offsetY = 5.5
                    )

                    if (pMouseX >= (x + 59) && pMouseY >= (y + 62.5) && pMouseX <= ((x + 59) + (RIDE_ICON_SIZE * SCALE)) && pMouseY <= ((y + 62.5) + (RIDE_ICON_SIZE * SCALE))) {
                        context.renderTooltip(
                            Minecraft.getInstance().font,
                            lang("ui.ride_style.${selectedBehaviour.key.toString().lowercase()}.${rideStyle}")
                                .append(" | ").append(lang("ui.ride_style.${selectedBehaviour.key.toString().lowercase()}")),
                            pMouseX,
                            pMouseY
                        )
                    }
                }

            } else {
                blitk(
                    matrixStack = matrices,
                    texture = statsChartResource,
                    x= (x + 25.5) / SCALE,
                    y = (y + 22) / SCALE,
                    width = 166,
                    height = 192,
                    scale = SCALE
                )

                when (statOptions.get(statTabIndex)) {
                    STATS -> drawStatPolygon(
                        listOf(
                            pokemon.maxHealth,
                            pokemon.attack,
                            pokemon.defence,
                            pokemon.speed,
                            pokemon.specialDefence,
                            pokemon.specialAttack
                        ).map { it / 400F },
                        colour = Vector3f(50F/255F, 215F/255F, 1F)
                    )
                    IV -> drawStatPolygon(
                        statLabels.values.map { pokemon.ivs.getEffectiveBattleIV(it) / 31F },
                        colour = Vector3f(216F/255, 100F/255, 1F)
                    )
                    EV -> drawStatPolygon(
                        statLabels.values.map { (pokemon.evs[it]?.toFloat() ?: 0F) / EVs.MAX_STAT_VALUE.toFloat() },
                        colour = Vector3f(1F, 1F, 100F/255F)
                    )
                }

                val labelsHovered = statLabelsHovered(hexagonVerticesOffset, pMouseX, pMouseY)

                val labels = statLabels.map { stat ->
                    if (labelsHovered) {
                        when (statOptions.get(statTabIndex)) {
                            IV -> {
                                "${
                                    if (pokemon.ivs.isHyperTrained(stat.value)) "${pokemon.ivs[stat.value]} (${pokemon.ivs.hyperTrainedIVs[stat.value]})"
                                    else pokemon.ivs[stat.value].toString()
                                }/${IVs.MAX_VALUE}".text()
                            }
                            EV -> "${pokemon.evs.getOrDefault(stat.value)}/${EVs.MAX_STAT_VALUE}".text()
                            else -> stat.key
                        }
                    } else { stat.key }
                }

                // Stat Labels
                renderPolygonLabels(
                    context = context,
                    labels = labels,
                    verticesOffset = hexagonVerticesOffset,
                    enableColour = true
                )

                // Stat Values
                renderPolygonLabels(
                    context = context,
                    labels = statValuesAsText(
                        statLabels.values.toList(),
                        labelsHovered
                    ),
                    verticesOffset = hexagonVerticesOffset,
                    offsetY = 5.5
                )

                // Nature-modified Stat Icons
                if (statOptions.get(statTabIndex) == STATS) {
                    val nature = pokemon.effectiveNature
                    renderModifiedStatIcon(matrices, nature.increasedStat, true)
                    renderModifiedStatIcon(matrices, nature.decreasedStat, false)
                }
            }
        }

        // Stat type select bar
        val hasRideBehaviour = pokemon.form.riding.behaviours != null
        val startPosX = x + if (hasRideBehaviour) 23 else 31
        val statTabWidth = if (hasRideBehaviour) STAT_TAB_WIDTH_6 else STAT_TAB_WIDTH_5

        statOptions.forEachIndexed { index, stat ->
            val statKey = if (stat == STATS) "" else ".$stat"
            drawScaledText(
                context = context,
                text = lang("ui.stats$statKey").bold(),
                x = startPosX + (statTabWidth * index),
                y = y + 143,
                scale = SCALE,
                colour = if (statOptions.get(statTabIndex) == stat) WHITE else GREY,
                centered = true
            )
        }

        blitk(
            matrixStack = context.pose(),
            texture = tabMarkerResource,
            x= (startPosX + (statTabIndex * statTabWidth) - (MARKER_WIDTH / 4)) / SCALE,
            y = (y + 140) / SCALE,
            width = MARKER_WIDTH,
            height = 4,
            scale = SCALE
        )
    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        val index = getTabIndexFromPos(pMouseX, pMouseY)
        // Only play sound here as the rest of the widget is meant to be silent
        if (index in 0 until statOptions.size && statTabIndex != index) {
            statTabIndex = index
            Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F))
        }

        if (statOptions.get(statTabIndex) == RIDE && ((pokemon.riding.behaviours?.size ?: 0) > 1) && centerHovered(pMouseX, pMouseY)) {
            rideBehaviourIndex = (rideBehaviourIndex + 1) % (pokemon.riding.behaviours?.size ?: 1)
            Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F))
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    private fun statValuesAsText(stats: List<Stat>, asPercent: Boolean): List<MutableComponent> {
        return stats.map { stat ->
            when (statOptions.get(statTabIndex)) {
                STATS -> (if (stat == Stats.HP) "${pokemon.currentHealth} / ${pokemon.maxHealth}" else pokemon.getStat(stat).toString()).text()
                IV -> {
                    (if (asPercent) {
                        "${floor((((if (pokemon.ivs.isHyperTrained(stat)) pokemon.ivs.hyperTrainedIVs[stat] else pokemon.ivs[stat]) ?: 0) / IVs.MAX_VALUE.toDouble()) * 100)}%"
                    } else {
                        if (pokemon.ivs.isHyperTrained(stat)) "${pokemon.ivs[stat]} (${pokemon.ivs.hyperTrainedIVs[stat]})"
                        else pokemon.ivs[stat].toString()
                    }).text()
                }
                EV -> {
                    (if (asPercent) "${floor((pokemon.evs.getOrDefault(stat) / EVs.MAX_STAT_VALUE.toDouble()) * 100)}%"
                    else pokemon.evs.getOrDefault(stat).toString()).text()
                }
                else -> "0".text()
            }
        }
    }

    private fun renderModifiedStatIcon(pPoseStack: PoseStack, stat: Stat?, increasedStat: Boolean) {
        if (stat != null) {
            var posX = x.toDouble()
            var posY = y.toDouble()

            when(stat) {
                Stats.HP -> { posX += 65; posY += 6 }
                Stats.SPECIAL_ATTACK -> { posX += 10; posY += 38 }
                Stats.ATTACK -> { posX += 120; posY += 38 }
                Stats.SPECIAL_DEFENCE -> { posX += 10; posY += 89 }
                Stats.DEFENCE -> { posX += 120; posY += 89 }
                Stats.SPEED -> { posX += 65; posY += 120 }
            }

            blitk(
                matrixStack = pPoseStack,
                texture = if (increasedStat) statIncreaseResource else statDecreaseResource,
                x= posX / SCALE,
                y = posY / SCALE,
                width = 8,
                height = 6,
                scale = SCALE,
            )
        }
    }

    private fun getModifiedStatColour(stat: Stat?, enableColour: Boolean): Int {
        if (statOptions.get(statTabIndex) == STATS && enableColour) {
            val nature = pokemon.effectiveNature

            if (nature.increasedStat == stat) return RED
            if (nature.decreasedStat == stat) return BLUE
        }
        return WHITE
    }

    private fun renderPolygonLabels(context: GuiGraphics, labels: List<MutableComponent>, verticesOffset: List<Pair<Double, Double>>, offsetY: Double = 0.0, enableColour: Boolean = false) {
        if (labels.size != verticesOffset.size) return

        labels.forEachIndexed { index, label ->
            drawScaledText(
                context = context,
                text = label,
                x = x + verticesOffset[index].first,
                y = y + verticesOffset[index].second + offsetY,
                scale = SCALE,
                colour = getModifiedStatColour(statLabels[label], enableColour),
                centered = true
            )
        }
    }

    private fun centerHovered(pMouseX: Number, pMouseY: Number): Boolean {
        val centerX = x + (WIDTH / 2)
        val centerY = y + (HEIGHT / 2)
        return (pMouseX.toInt() in (centerX - 30)..(centerX + 30)) && (pMouseY.toInt() in (centerY - 30)..(centerY + 30))
    }

    private fun statLabelsHovered(labelOffsets: List<Pair<Double, Double>>, mouseX: Int, mouseY: Int): Boolean {
        return labelOffsets.any { pos ->
            (mouseX.toDouble() in (x + pos.first - 12)..(x + pos.first + 12))
                && (mouseY.toDouble() in (y + pos.second - 1)..(y + pos.second + 10))
        }
    }

    private fun getTabIndexFromPos(mouseX: Double, mouseY: Double): Int {
        val hasRideBehaviour = pokemon.form.riding.behaviours != null
        val left = x + if (hasRideBehaviour) ((23 + 1) / 2).toFloat() else ((31 + 1) / 2).toFloat()
        val statTabWidth = if (hasRideBehaviour) STAT_TAB_WIDTH_6 else STAT_TAB_WIDTH_5

        val top = y + 140.0
        if (mouseX in left..(left + (statTabWidth * (statOptions.size + 1))) && mouseY in top..(top + 9.0)) {
            var startX = left
            var endX = left + statTabWidth
            for (index in 0 until statOptions.size) {
                if (mouseX in startX..endX) return index
                startX += statTabWidth
                endX += statTabWidth
            }
        }
        return -1
    }
}

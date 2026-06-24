/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.battle

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.gui.renderSprite
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.scheduling.Schedulable
import com.cobblemon.mod.common.api.scheduling.SchedulingTracker
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.green
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.api.types.tera.elemental.ElementalTypeTeraType
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon
import com.cobblemon.mod.common.client.battle.ClientBallDisplay
import com.cobblemon.mod.common.client.battle.ClientBattle
import com.cobblemon.mod.common.client.battle.ClientBattleActor
import com.cobblemon.mod.common.client.battle.ClientBattleInformationRepository
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import com.cobblemon.mod.common.net.messages.client.battle.BattlePokemonDTO
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleTeamInfoSelection
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane
import com.cobblemon.mod.common.client.keybind.boundKey
import com.cobblemon.mod.common.client.keybind.keybinds.PartySendBinding
import com.cobblemon.mod.common.client.render.SpriteType
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.drawScaledTextJustifiedRight
import com.cobblemon.mod.common.client.render.getDepletableRedGreen
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.wavefunction.sineFunction
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity
import com.cobblemon.mod.common.net.messages.client.battle.BattleInformationDTO
import com.cobblemon.mod.common.net.messages.client.battle.FieldEffect
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.pokemon.status.PersistentStatus
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.util.battleLang
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.toHex
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import org.joml.Vector3f
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.collections.get
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.text.get

class BattleOverlay : Gui(Minecraft.getInstance()), Schedulable {
    companion object {
        const val MAX_OPACITY = 1.0
        const val MIN_OPACITY = 0.5
        const val OPACITY_CHANGE_PER_SECOND = 0.1
        const val HORIZONTAL_INSET = 12
        const val VERTICAL_INSET = 22
        const val HORIZONTAL_SPACING = 4
        const val VERTICAL_SPACING = 40
        const val COMPACT_VERTICAL_SPACING = 32
        const val INFO_OFFSET_X = 7
        const val COMPACT_INFO_OFFSET_X = 6

        const val TILE_WIDTH = 140
        const val COMPACT_TILE_WIDTH = 128

        const val TILE_HEIGHT = 40
        const val COMPACT_TILE_HEIGHT = 28
        const val COMPACT_TILE_TEXTURE_HEIGHT = 56

        const val PORTRAIT_DIAMETER = 28
        const val COMPACT_PORTRAIT_DIAMETER = 19
        const val PORTRAIT_OFFSET_X = 5
        const val COMPACT_PORTRAIT_OFFSET_X = 4

        const val PORTRAIT_OFFSET_Y = 8
        const val COMPACT_PORTRAIT_OFFSET_Y = 7

        const val ROLE_CYCLE_SECONDS = 2.5

        const val SCALE = 0.5F

        private val PROMPT_TEXT_OPACITY_CURVE = sineFunction(period = 4F, verticalShift = 0.5F, amplitude = 0.5F)

        val battleInfoBase = cobblemonResource("textures/gui/battle/battle_info_base.png")
        val battleInfoBaseFlipped = cobblemonResource("textures/gui/battle/battle_info_base_flipped.png")
        val battleInfoBaseCompact = cobblemonResource("textures/gui/battle/battle_info_base_condensed.png")
        val battleInfoBaseFlippedCompact = cobblemonResource("textures/gui/battle/battle_info_base_flipped_condensed.png")
        val battleInfoRole = cobblemonResource("textures/gui/battle/battle_info_role.png")
        val battleInfoRoleFlipped = cobblemonResource("textures/gui/battle/battle_info_role_flipped.png")
        val battleInfoUnderlay = cobblemonResource("textures/gui/battle/battle_info_underlay.png")
        // Benched-team portrait column frames (same tile art the Info menu uses, scaled down).
        val partyColumnTile = cobblemonResource("textures/gui/battle/pokemon_tile.png")
        val partyColumnTileReversed = cobblemonResource("textures/gui/battle/pokemon_tile_reversed.png")
        val partyColumnTileDisabled = cobblemonResource("textures/gui/battle/pokemon_tile_disabled.png")
        val partyColumnTileDisabledReversed = cobblemonResource("textures/gui/battle/pokemon_tile_disabled_reversed.png")
        val caughtIndicator = cobblemonResource("textures/gui/battle/battle_owned_indicator.png")

        val partyPokeballIcon = cobblemonResource("textures/gui/battle/party_pokeball_icon.png")
        val turnCounter = cobblemonResource("textures/gui/battle/turn_counter.png")
        val expandedPokemonInfoLeft = cobblemonResource("textures/gui/battle/expanded_pokemon_info_left.png")
        val expandedPokemonInfoCompactLeft = cobblemonResource("textures/gui/battle/expanded_pokemon_info_compact_left.png")
        val expandedPokemonInfoRight = cobblemonResource("textures/gui/battle/expanded_pokemon_info_right.png")
        val expandedPokemonInfoCompactRight = cobblemonResource("textures/gui/battle/expanded_pokemon_info_compact_right.png")
        val questionMarkIcon = cobblemonResource("textures/gui/battle/question_mark.png")

        val healthBar = cobblemonResource("textures/gui/battle/health_bar.png")
        val healthBarFlipped = cobblemonResource("textures/gui/battle/health_bar_flipped.png")

        val effectTooltipBody = cobblemonResource("textures/gui/battle/effect_tooltip_body.png")
        val effectTooltipEdges = cobblemonResource("textures/gui/battle/effect_tooltip_edge.png")

        private val decimalFormat = DecimalFormat("0.00").also {
            it.roundingMode = RoundingMode.CEILING
        }
    }

    var opacity = MIN_OPACITY
    val opacityRatio: Double
        get() = (opacity - MIN_OPACITY) / (MAX_OPACITY - MIN_OPACITY)
    var passedSeconds = 0F

    var lastKnownBattle: UUID? = null
    lateinit var messagePane: BattleMessagePane
    var hidePortraits = false

    // Persistent per-Pokémon animation states for the benched-team portrait column (keyed by Pokémon
    // UUID) so those portraits keep animating across frames. Cleared when no battle is active.
    private val partyTileStates = mutableMapOf<UUID, FloatingState>()

    // Drag state for the benched-team portrait column. Each actor's column can be dragged as one group
    // to a custom offset; a click (press without drag) resets it back under the active Pokémon.
    // columnHitboxes holds the last-rendered column bounds [x, y, w, h] per actor for click hit-testing.
    private val columnOffsets = mutableMapOf<UUID, Pair<Float, Float>>()
    private val columnHitboxes = mutableMapOf<UUID, FloatArray>()
    private var draggedColumnActor: UUID? = null
    private var dragStartMouseX = 0.0
    private var dragStartMouseY = 0.0
    private var dragStartOffsetX = 0f
    private var dragStartOffsetY = 0f
    private var columnDragMoved = false
    private var lastColumnBattle: UUID? = null

    // While the BattleGUI screen is open the on-field portrait columns (and their hover panels) are queued
    // here during the HUD pass and re-drawn by the screen AFTER its own widgets, the switch menu's underlay
    // gradient and the chat — all of which draw later and would otherwise render on top of the columns.
    private val pendingColumns = mutableListOf<PendingColumn>()
    private class PendingColumn(
        val tileY: Float,
        val portraitStartX: Float,
        val portraitDiameter: Int,
        val reversed: Boolean,
        val actor: ClientBattleActor,
        val partialTicks: Float,
        val isCompact: Boolean
    )

    // The active Pokémon's hover panel is deferred alongside the columns (and drawn after them) so it lands
    // in the same pass and on top of the benched portraits, instead of behind them.
    private val pendingActivePanels = mutableListOf<PendingActivePanel>()
    private class PendingActivePanel(
        val x: Float,
        val y: Float,
        val reversed: Boolean,
        val pokemon: ClientBattlePokemon,
        val isCompact: Boolean
    )

    // In compact (doubles/triples) battles an actor owns multiple active tiles, so the benched column is
    // emitted once per actor (not per tile). This tracks which actors already have one queued this frame.
    private val columnDrawnActors = mutableSetOf<UUID>()
    override val schedulingTracker = SchedulingTracker()

    var mouseX: Int = 0
    var mouseY: Int = 0

    override fun render(context: GuiGraphics, tickCounter: DeltaTracker) {
        val tickDelta = tickCounter.realtimeDeltaTicks.takeIf { !Minecraft.getInstance()!!.isPaused } ?: 0F

        schedulingTracker.update(tickDelta / 20F)
        passedSeconds += tickDelta / 20
        if (passedSeconds > 100) {
            passedSeconds -= 100
        }
        val battle = CobblemonClient.battle
        if (battle == null) {
            mouseX = 0
            mouseY = 0
            partyTileStates.clear()
            columnOffsets.clear()
            columnHitboxes.clear()
            pendingColumns.clear()
            pendingActivePanels.clear()
            columnDrawnActors.clear()
            draggedColumnActor = null
            return
        }
        if (battle.minimised) {
            mouseX = 0
            mouseY = 0
        }

        // Reset the draggable team-portrait columns to their default positions whenever a new battle begins.
        if (lastColumnBattle != battle.battleId) {
            columnOffsets.clear()
            draggedColumnActor = null
            lastColumnBattle = battle.battleId
        }
        // Rebuilt fresh each frame; entries are queued below and drained by BattleGUI.renderDeferredColumns.
        pendingColumns.clear()
        pendingActivePanels.clear()
        columnDrawnActors.clear()

        val hoverInfo = getHoverInformation(tickDelta)

        opacity = if (battle.minimised) {
            java.lang.Double.max(opacity - tickDelta * OPACITY_CHANGE_PER_SECOND, MIN_OPACITY)
        } else {
            java.lang.Double.min(opacity + tickDelta * OPACITY_CHANGE_PER_SECOND, MAX_OPACITY)
        }
        val currentScreen = Minecraft.getInstance().screen
        val isCompact = battle.battleFormat.battleType.pokemonPerSide > 1
        val playerUUID = Minecraft.getInstance().player?.uuid ?: return
        val isOnSide1 = battle.side1.actors.any { it.uuid == playerUUID }
        val isOnSide2 = battle.side2.actors.any { it.uuid == playerUUID }
        val isSpectating = !isOnSide1 && !isOnSide2
        if (!hidePortraits && !BattleTeamInfoSelection.visible) {
            // We always want to keep player on left-hand side
            val side1 = if (isOnSide1) battle.side1 else battle.side2
            val side2 = if (side1 == battle.side1) battle.side2 else battle.side1

            // Command highlight for Double and Triple Battles
            val isBattleGUIActive = currentScreen is BattleGUI && currentScreen.getCurrentActionSelection() != null
            val selectedPNX = if((battle.battleFormat.battleType.slotsPerActor > 1 || battle.battleFormat.battleType.actorsPerSide > 1) && isBattleGUIActive) battle.getFirstUnansweredRequest()?.activePokemon?.getPNX() else null

            side1.activeClientBattlePokemon.forEachIndexed { index, activeClientBattlePokemon ->
                val rank = index
                val actor = activeClientBattlePokemon.actor
                if (hoverInfo != null && !hoverInfo.isReversed && hoverInfo.rank < rank) {
                    if (hoverInfo.hoveredPokeBall == null) {
                        return@forEachIndexed
                    }
                    else {
                        val unrevealed = ClientBattleInformationRepository.actors[actor.uuid]!![hoverInfo.hoveredPokeBall].activeBattlePokemonDTO == null
                        if (!unrevealed) {
                            return@forEachIndexed
                        }
                    }
                }
                drawTile(context, tickDelta, activeClientBattlePokemon, true, rank, PokedexEntryProgress.NONE, activeClientBattlePokemon.getPNX() == selectedPNX, false, isCompact)
            }
            side2.activeClientBattlePokemon.forEachIndexed { index, activeClientBattlePokemon ->
                val rank = side2.activeClientBattlePokemon.count() - index - 1
                val actor = activeClientBattlePokemon.actor
                if (hoverInfo != null && hoverInfo.isReversed && hoverInfo.rank < rank) {
                    if (hoverInfo.hoveredPokeBall == null) {
                        return@forEachIndexed
                    }
                    else {
                        val unrevealed = ClientBattleInformationRepository.actors[actor.uuid]!![hoverInfo.hoveredPokeBall].activeBattlePokemonDTO == null
                        if (!unrevealed) {
                            return@forEachIndexed
                        }
                    }
                }
                drawTile(context, tickDelta, activeClientBattlePokemon, false, rank, battle.knowledge, false, false, isCompact)
            }
        }

        if (Minecraft.getInstance().screen !is BattleGUI && battle.mustChoose) {
            val textOpacity = PROMPT_TEXT_OPACITY_CURVE(passedSeconds)
            drawScaledText(
                context = context,
                text = battleLang("ui.actions_label", PartySendBinding.boundKey().displayName),
                x = Minecraft.getInstance().window.guiScaledWidth / 2,
                y = Minecraft.getInstance().window.guiScaledHeight / 5,
                opacity = textOpacity,
                centered = true
            )
        }

        if (currentScreen == null || currentScreen is ChatScreen) {
            if (lastKnownBattle != battle.battleId) {
                lastKnownBattle = battle.battleId
                messagePane = BattleMessagePane(CobblemonClient.battle!!.messages)
            }
            messagePane.opacity = 0.3F
            messagePane.render(context, 0, 0, 0F)
        }

        if (!battle.isPvW) {
            drawNameplate(context, battle, isReversed = false, isSpectating = isSpectating)
            drawNameplate(context, battle, isReversed = true, isSpectating = isSpectating)

            val userUUID = Minecraft.getInstance().player?.uuid
            if (userUUID != null) {
                val shouldReverseSides = ClientBattleInformationRepository.actorActualSides[userUUID] == 2
                drawSidedEffects(context, battle, if (shouldReverseSides) 2 else 1, isReversed = false)
                drawSidedEffects(context, battle, if (shouldReverseSides) 1 else 2, isReversed = true)
            }
        }

        drawTurnCounter(context)
        drawPrimaryWeatherIcon(context)
        drawFieldIcons(context)
    }

    fun drawNameplate(context: GuiGraphics, battle: ClientBattle, isReversed: Boolean, isSpectating: Boolean) {
        /*
         * Currently the logic for cobblemon keeping the player on the left-hand side is confusing as shit.
         * Due to this logic, spectators always reverse the sides. What we're doing here is un-reversing it so nameplates
         * are always correct.
         *
         * I could probably clean this up, but this would probably make it more annoying to pull in changes down the
         * road while maintaining this mod.
         */
        val side = if (isSpectating) {
            if (isReversed) battle.side1 else battle.side2
        }
        else {
            if (isReversed) battle.side2 else battle.side1
        }
        val names = side.actors.joinToString(separator = " & ") { it.displayName.string }
        var x = HORIZONTAL_INSET - 7
        if (isReversed) {
            x = Minecraft.getInstance().window.guiScaledWidth - x - 128
        }
        blitk(
            matrixStack = context.pose(),
            texture = cobblemonResource("textures/gui/battle/name_plate.png"),
            y = 7,
            x = x,
            height = 8,
            width = 128,
            alpha = opacity
        )

        val maybeShortened = if (names.length > 18) names.take(18) + "..." else names

        if (isReversed) {
            drawScaledTextJustifiedRight(
                context = context,
                text = maybeShortened.text(),
                y = 9,
                x = x + 128 - 12,
                scale = SCALE,
                opacity = opacity,
                shadow = true
            )
        }
        else {
            drawScaledText(
                context = context,
                text = maybeShortened.text(),
                y = 9,
                x = x + 12,
                scale = SCALE,
                opacity = opacity,
                shadow = true
            )
        }
    }

    fun drawSidedEffects(context: GuiGraphics, battle: ClientBattle, side: Int, isReversed: Boolean) {
        val battleInfo = ClientBattleInformationRepository.battles[battle.battleId] ?: return
        val sidedEffects = if (side == 2) battleInfo.side2SidedEffects else battleInfo.side1SidedEffects
        val effectFrequencies = sidedEffects.groupBy { it.id }
        var x = HORIZONTAL_INSET - 7
        if (isReversed) {
            x = Minecraft.getInstance().window.guiScaledWidth - x - 128
        }
        context.pose().pushPose()
        context.pose().translate(0.0, 0.0, 100.0)

        effectFrequencies.keys.forEachIndexed { index, effect ->
            val effectX = (if (isReversed) x + 5 + index * 13 * 0.66 else x + 128 - 5 - 8 - index * 13 * 0.66)

            blitk(
                matrixStack = context.pose(),
                texture = cobblemonResource("textures/gui/battle/sided_effects/${effect}.png"),
                y = 7 / 0.66,
                x = effectX / 0.66,
                height = 12,
                width = 12,
                alpha = opacity,
                scale = 0.66f
            )

            val frequency = effectFrequencies[effect]!!.size
            if (frequency > 1) {
                drawScaledText(
                    context = context,
                    text = frequency.toString().text(),
                    y = 7 + 5.5,
                    x = effectX + 7,
                    shadow = true,
                    opacity = opacity,
                    scale = 0.6f,
                    centered = true
                )
            }

            if (mouseX >= effectX && mouseX <= effectX + 12 * 0.66 && mouseY >= 7 && mouseY <= 7 + 12 * 0.75) {
                val text = getFieldEffectText(effectFrequencies[effect]!!.first(), battleInfo, true)
                val mc = Minecraft.getInstance()
                val textWidth = (mc.font.width(text) + 4) * 0.66f
                val tooltipStartX = (mc.window.guiScaledWidth / 2) - (textWidth / 2)
                renderTooltip(context, text, tooltipStartX, 42f)
            }
        }
        context.pose().popPose()
    }

    fun drawTile(context: GuiGraphics, tickDelta: Float, activeBattlePokemon: ActiveClientBattlePokemon, left: Boolean, rank: Int, dexState: PokedexEntryProgress, hasCommand: Boolean = false, isHovered: Boolean = false, isCompact: Boolean = false) {
        val mc = Minecraft.getInstance()
        val battle = CobblemonClient.battle ?: return

        val battlePokemon = activeBattlePokemon.battlePokemon
        if (battlePokemon == null) {
            // No active Pokémon in this slot (e.g. a forced switch after fainting). There's no tile to
            // draw, but still show the benched-team column so the player can see their party while choosing
            // a switch. Deduped per actor so a fainted slot in doubles doesn't suppress/double the column.
            val actor = activeBattlePokemon.actor
            val reversed = !left
            if (!isCompact) {
                val baseX = if (left) HORIZONTAL_INSET.toFloat()
                    else (mc.window.guiScaledWidth - HORIZONTAL_INSET - TILE_WIDTH).toFloat()
                val portraitStartX = baseX + if (!reversed) PORTRAIT_OFFSET_X.toFloat()
                    else (TILE_WIDTH - PORTRAIT_DIAMETER - PORTRAIT_OFFSET_X).toFloat()
                queueOrDrawColumn(context, VERTICAL_INSET.toFloat(), portraitStartX, PORTRAIT_DIAMETER, reversed, actor, tickDelta, isCompact)
            } else if (actor.uuid !in columnDrawnActors) {
                columnDrawnActors.add(actor.uuid)
                val slotCount = battle.battleFormat.battleType.slotsPerActor
                var baseX = HORIZONTAL_INSET + (slotCount - rank - 1) * HORIZONTAL_SPACING.toFloat()
                if (!left) baseX = mc.window.guiScaledWidth - baseX - COMPACT_TILE_WIDTH
                val portraitStartX = baseX + if (!reversed) COMPACT_PORTRAIT_OFFSET_X.toFloat()
                    else (COMPACT_TILE_WIDTH - COMPACT_PORTRAIT_DIAMETER - COMPACT_PORTRAIT_OFFSET_X).toFloat()
                val activePerSide = battle.battleFormat.battleType.pokemonPerSide
                val stackBottomTileY = (VERTICAL_INSET + (activePerSide - 1) * COMPACT_VERTICAL_SPACING).toFloat()
                queueOrDrawColumn(context, stackBottomTileY, portraitStartX, COMPACT_PORTRAIT_DIAMETER, reversed, actor, tickDelta, isCompact)
            }
            return
        }

        val slotCount = battle.battleFormat.battleType.slotsPerActor
        val playerNumberOffset = (activeBattlePokemon.getActorShowdownId()[1].digitToInt() - 1) / 2 * 10

        var x = HORIZONTAL_INSET + (slotCount - rank - 1) * HORIZONTAL_SPACING.toFloat()
        val y = VERTICAL_INSET + rank * (if (isCompact) COMPACT_VERTICAL_SPACING else VERTICAL_SPACING) + (if (left) playerNumberOffset else (battle.battleFormat.battleType.actorsPerSide - 1) * 10 - playerNumberOffset)
        if (!left) {
            x = mc.window.guiScaledWidth - x - if(isCompact) COMPACT_TILE_WIDTH else TILE_WIDTH
        }
        // The active tile slides off/on screen as it faints or switches; the benched column should stay
        // put, so capture the resting x before the slide animation and anchor the column to it.
        val restX = x
        val invisibleX = if (left) {
            -(if(isCompact) COMPACT_TILE_WIDTH else TILE_WIDTH) - 1F
        } else {
            mc.window.guiScaledWidth.toFloat()
        }
        activeBattlePokemon.invisibleX = invisibleX
        activeBattlePokemon.xDisplacement = x
        activeBattlePokemon.animate(tickDelta)
        x = activeBattlePokemon.xDisplacement


        val hue = activeBattlePokemon.getHue()
        val r = ((hue shr 16) and 0b11111111) / 255F
        val g = ((hue shr 8) and 0b11111111) / 255F
        val b = (hue and 0b11111111) / 255F

        drawBattleTile(
            context = context,
            x = x,
            columnX = restX,
            y = y.toFloat(),
            partialTicks = tickDelta,
            reversed = !left,
            species = battlePokemon.species,
            level = battlePokemon.level,
            displayName = battlePokemon.displayName,
            gender = battlePokemon.gender,
            status = battlePokemon.status,
            state = battlePokemon.state,
            colour = Triple(r, g, b),
            opacity = opacity.toFloat(),
            ballState = activeBattlePokemon.ballCapturing,
            maxHealth = battlePokemon.maxHp.toInt(),
            health = battlePokemon.hpValue,
            isFlatHealth = battlePokemon.isHpFlat,
            isSelected = hasCommand,
            isHovered = isHovered,
            isCompact = isCompact,
            actorDisplayName = if (!battle.isPvW &&
                ((left && activeBattlePokemon.actor.activePokemon.firstOrNull { (it.battlePokemon?.hpValue ?: 0F) > 0F } == activeBattlePokemon)
                        || (!left && activeBattlePokemon.actor.activePokemon.lastOrNull { (it.battlePokemon?.hpValue ?: 0F) > 0F } == activeBattlePokemon))) activeBattlePokemon.actor.displayName
            else null,
            dexState = dexState,
            actor = activeBattlePokemon.actor,
            rank = rank,
            battlePokemon = battlePokemon,
        )
    }

    fun drawBattleTile(
        context: GuiGraphics,
        x: Float,
        columnX: Float,
        y: Float,
        partialTicks: Float,
        reversed: Boolean,
        species: Species,
        level: Int,
        displayName: MutableComponent,
        gender: Gender,
        status: PersistentStatus?,
        state: PosableState,
        colour: Triple<Float, Float, Float>?,
        opacity: Float,
        ballState: ClientBallDisplay? = null,
        maxHealth: Int,
        health: Float,
        isSelected: Boolean = false,
        isHovered: Boolean = false,
        isCompact: Boolean = false,
        actorDisplayName: MutableComponent? = null,
        isFlatHealth: Boolean,
        dexState: PokedexEntryProgress,
        actor: ClientBattleActor,
        rank: Int,
        battlePokemon: ClientBattlePokemon,
    ) {
        val tileWidth = if (isCompact) COMPACT_TILE_WIDTH else TILE_WIDTH
        val portraitOffsetX = if (isCompact) COMPACT_PORTRAIT_OFFSET_X else PORTRAIT_OFFSET_X
        val portraitOffsetY = if (isCompact) COMPACT_PORTRAIT_OFFSET_Y else PORTRAIT_OFFSET_Y
        val portraitDiameter = if (isCompact) COMPACT_PORTRAIT_DIAMETER else PORTRAIT_DIAMETER
        val infoOffsetX = if (isCompact) COMPACT_INFO_OFFSET_X else INFO_OFFSET_X
        val portraitStartX = x + if (!reversed) portraitOffsetX else { tileWidth - portraitDiameter - portraitOffsetX }
        // Same offset, but off the resting x so the benched column doesn't slide with the tile animation.
        val columnPortraitStartX = columnX + if (!reversed) portraitOffsetX else { tileWidth - portraitDiameter - portraitOffsetX }
        val portraitStartY = y + portraitOffsetY
        val matrixStack = context.pose()
        val isPortraitHovered = isPortraitHovered(portraitStartX, portraitStartY, portraitDiameter)
        val hoveredPartyPokeBall = getHoveredPartyPokeBall(x, y, actor, reversed, rank)
        val hoveredPartyPokemon = hoveredPartyPokeBall?.let {
            val actorTeam = ClientBattleInformationRepository.actors[actor.uuid] ?: return@let null
            if (actorTeam.size < it) return@let null
            val hoveredPokemonDTO = actorTeam[it].activeBattlePokemonDTO ?: return@let null
            val hoveredPokemon = ClientBattlePokemon(
                uuid = hoveredPokemonDTO.uuid,
                properties = hoveredPokemonDTO.properties,
                aspects = hoveredPokemonDTO.aspects,
                displayName = hoveredPokemonDTO.displayName,
                hpValue = hoveredPokemonDTO.hpValue,
                maxHp = hoveredPokemonDTO.maxHp,
                isHpFlat = hoveredPokemonDTO.isFlatHp,
                status = hoveredPokemonDTO.status,
                statChanges = hoveredPokemonDTO.statChanges,
            )
            hoveredPokemon.actor = actor
            hoveredPokemon
        }

        blitk(
            matrixStack = matrixStack,
            texture = battleInfoUnderlay,
            y = y + portraitOffsetY,
            x = portraitStartX,
            height = portraitDiameter,
            width = portraitDiameter,
            alpha = opacity
        )

        val statusToDisplay = hoveredPartyPokemon?.status ?: status
        if (statusToDisplay != null) {
            val statusWidth = if (isCompact) 40 else 40
            context.pose().pushPose()
            context.pose().translate(0.0, 0.0, 100.0)
            blitk(
                matrixStack = matrixStack,
                texture = cobblemonResource("textures/gui/battle/battle_status_" + statusToDisplay.showdownName + ".png"),
                x = x + if (reversed) 65 else (if (isCompact) 23 else 34),
                y = y + if (isCompact) 23 else 30,
                height = if (isCompact) 9 else 8,
                width = statusWidth,
                uOffset = if (reversed) 0 else (74 - statusWidth),
                vOffset = if (isCompact) 1 else 0,
                textureHeight = 7,
                textureWidth = 74,
                alpha = opacity
            )

            drawScaledText(
                context = context,
                font = if (isCompact) null else CobblemonResources.DEFAULT_LARGE,
                text = lang("ui.status." + statusToDisplay.showdownName).bold(),
                x = x + if (isCompact) (if (reversed) 87 else 30) else (if (reversed) 86 else 41),
                y = y + if (isCompact) 26 else 29,
                scale = if (isCompact) SCALE else 1F,
                opacity = opacity
            )
            context.pose().popPose()
        }

        // Second render the Pokémon through the scissors
        context.enableScissor(
            portraitStartX.toInt(),
            (y + portraitOffsetY).toInt(),
            (portraitStartX + portraitDiameter).toInt(),
            (y + portraitDiameter + portraitOffsetY).toInt(),
        )
        matrixStack.pushPose()
        matrixStack.translate(
            portraitStartX + portraitDiameter / 2.0,
            y.toDouble() + portraitOffsetY - if (isCompact) 15.0 else 5.0,
            0.0
        )

        if (ballState != null && ballState.currentPose != "shut")  {
            ballState.currentPose = "shut"
        }

        if (ballState != null && ballState.stateEmitter.get() == EmptyPokeBallEntity.CaptureState.SHAKE) {
            drawPokeBall(
                state = ballState,
                matrixStack = matrixStack,
                reversed = reversed,
                partialTicks = partialTicks
            )
        } else {
            val speciesToDisplay = hoveredPartyPokemon?.species ?: species
            val stateToDisplay = hoveredPartyPokemon?.state ?: state
            drawCustomPosablePortrait(
                identifier = speciesToDisplay.resourceIdentifier,
                matrixStack = matrixStack,
                scale = 18F * (ballState?.scale ?: 1F) * if (isCompact) 0.65F else 1.0f,
                contextScale = speciesToDisplay.getForm(stateToDisplay.currentAspects).baseScale,
                reversed = reversed,
                doQuirks = false,
                state = stateToDisplay,
                partialTicks = if (Cobblemon.config.animateBattleTiles) partialTicks else 0F
            )
        }
        matrixStack.popPose()
        context.disableScissor()

        // Third render the tile
        blitk(
            matrixStack = matrixStack,
            texture = if (isCompact) (if (reversed) battleInfoBaseFlippedCompact else battleInfoBaseCompact) else (if (reversed) battleInfoBaseFlipped else battleInfoBase),
            x = x,
            y = y,
            height = if (isCompact) COMPACT_TILE_HEIGHT else TILE_HEIGHT,
            width = tileWidth,
            textureHeight = if (isCompact) COMPACT_TILE_TEXTURE_HEIGHT else TILE_HEIGHT,
            vOffset = if (isHovered && isCompact) COMPACT_TILE_HEIGHT else 0,
            alpha = opacity,
        )

        val stage = floor((passedSeconds / ROLE_CYCLE_SECONDS % 1) * 5)
        if (colour != null && (!isSelected || stage != 4.0)) {
            val (r, g, b) = colour
            blitk(
                matrixStack = matrixStack,
                texture = if (reversed) battleInfoRoleFlipped else battleInfoRole,
                x = x + if (isCompact) (if (reversed) 93 else 8) else (if (reversed) 102 else 11),
                y = y + 1,
                height = 3,
                textureHeight = 12,
                width = 27,
                alpha = opacity,
                vOffset = if (isSelected) stage * 3 else 9,
                red = r,
                green = g,
                blue = b
            )

            if (dexState == PokedexEntryProgress.CAUGHT) {
                blitk(
                    matrixStack = matrixStack,
                    texture = caughtIndicator,
                    x = (x + 7) / SCALE,
                    y = (y + 9) / SCALE,
                    height = 10,
                    width = 10,
                    scale = SCALE,
                    alpha = opacity
                )
            }
        }

        // Draw labels
        val infoBoxX = x + if (!reversed) (portraitDiameter + portraitOffsetX + infoOffsetX) else infoOffsetX

        val displayNameToShow = hoveredPartyPokemon?.displayName ?: displayName
        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = displayNameToShow.bold(),
            x = infoBoxX + (if (dexState == PokedexEntryProgress.CAUGHT) 7 else 0),
            y = y + if (isCompact) 5 else 7,
            opacity = opacity,
            shadow = true
        )

        val genderToDisplay = hoveredPartyPokemon?.gender ?: gender
        if (genderToDisplay != Gender.GENDERLESS) {
            val isMale = genderToDisplay == Gender.MALE
            val textSymbol = if (isMale) "♂".text().bold() else "♀".text().bold()
            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = textSymbol,
                x = infoBoxX + 63,
                y = y + if (isCompact) 5 else 7,
                colour = if (isMale) 0x32CBFF else 0xFC5454,
                opacity = opacity,
                shadow = true
            )
        }

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = lang("ui.lv").bold(),
            x = infoBoxX + 69,
            y = y + if (isCompact) 5 else 7,
            opacity = opacity,
            shadow = true
        )

        val levelToDisplay = hoveredPartyPokemon?.level ?: level
        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = levelToDisplay.toString().text().bold(),
            x = infoBoxX + 82,
            y = y + if (isCompact) 5 else 7,
            opacity = opacity,
            shadow = true
        )

        val healthToDisplay = hoveredPartyPokemon?.hpValue ?: health
        val maxHealthToDisplay = hoveredPartyPokemon?.maxHp?.toInt() ?: maxHealth
        val isFlatHealthToDisplay = hoveredPartyPokemon?.isHpFlat ?: isFlatHealth
        val hpRatio = if (isFlatHealthToDisplay) healthToDisplay / maxHealthToDisplay else healthToDisplay
        val (healthRed, healthGreen) = getDepletableRedGreen(hpRatio)
        val fullWidth = 99
        val barWidth = hpRatio * fullWidth
        val barOffsetX = if (isCompact) (if (reversed) 1 else 3) else 2
        val barX = if (!reversed) infoBoxX - barOffsetX else infoBoxX - barOffsetX + (fullWidth - barWidth - 3)

        if (reversed) {
            blitk(
                matrixStack = matrixStack,
                texture = healthBarFlipped,
                x = barX,
                y = y + if (isCompact) 16 else 22,
                height = 6,
                width = min(4f, barWidth),
                textureWidth = 99,
                textureHeight = 6,
                red = healthRed * 0.8F,
                green = healthGreen * 0.8F,
                blue = 0.27F
            )
            blitk(
                matrixStack = matrixStack,
                texture = healthBarFlipped,
                x = barX + 4,
                y = y + if (isCompact) 16 else 22,
                height = 6,
                width = max(barWidth - 3.0, 0.0),
                textureWidth = 99,
                textureHeight = 6,
                uOffset = max(fullWidth - barWidth + 3.0, 2.0),
                red = healthRed * 0.8F,
                green = healthGreen * 0.8F,
                blue = 0.27F
            )
        }
        else {
            blitk(
                matrixStack = matrixStack,
                texture = healthBar,
                x = barX,
                y = y + if (isCompact) 16 else 22,
                height = 6,
                width = max(barWidth - 3.0, 0.0),
                textureWidth = 99,
                textureHeight = 6,
                red = healthRed * 0.8F,
                green = healthGreen * 0.8F,
                blue = 0.27F
            )
            blitk(
                matrixStack = matrixStack,
                texture = healthBar,
                x = barX + barWidth - 3,
                y = y + if (isCompact) 16 else 22,
                height = 6,
                width = min(3f, barWidth),
                textureWidth = 99,
                textureHeight = 6,
                uOffset = fullWidth - 3,
                red = healthRed * 0.8F,
                green = healthGreen * 0.8F,
                blue = 0.27F
            )
        }

        val text = if (isFlatHealthToDisplay) {
            "${healthToDisplay.toInt()}/$maxHealthToDisplay (${ceil(healthToDisplay / maxHealthToDisplay * 100)}%)"
        } else {
            "${ceil(healthToDisplay * 100)}%"
        }.text()

        drawScaledText(
            context = context,
            text = text,
            x = infoBoxX + (if (!reversed) 39.5 else 44.5),
            y = y + if(isCompact) 17 else 23,
            scale = 0.5F,
            opacity = opacity,
            centered = true,
            shadow = true
        )

        if (isCompact) {
            // Doubles/triples: the rest of an actor's team renders as the benched-portrait column singles
            // use, anchored below the whole active stack at the resting x. Replaces the poke-ball row.
            // Emitted once per actor (deduped) rather than per tile.
            if (actor.uuid !in columnDrawnActors) {
                columnDrawnActors.add(actor.uuid)
                val activePerSide = CobblemonClient.battle?.battleFormat?.battleType?.pokemonPerSide ?: 2
                val stackBottomTileY = (VERTICAL_INSET + (activePerSide - 1) * COMPACT_VERTICAL_SPACING).toFloat()
                queueOrDrawColumn(context, stackBottomTileY, columnPortraitStartX, portraitDiameter, reversed, actor, partialTicks, isCompact)
            }
        } else {
            // Benched portraits draw their own side panel on hover. Anchored to the resting x so they stay
            // put while the active tile slides out/in on faint or switch.
            queueOrDrawColumn(context, y, columnPortraitStartX, portraitDiameter, reversed, actor, partialTicks, isCompact)
        }

        // The active Pokémon's panel renders under it. Defer it so it layers above the benched portraits
        // (which are themselves deferred to the screen pass), instead of behind them.
        val panelPokemon = if (isPortraitHovered) battlePokemon else hoveredPartyPokemon
        if (panelPokemon != null) {
            if (Minecraft.getInstance().screen is BattleGUI) {
                pendingActivePanels.add(PendingActivePanel(portraitStartX, portraitStartY, reversed, panelPokemon, isCompact))
            } else {
                renderExtendedBattleInfo(context, portraitStartX, portraitStartY, reversed, panelPokemon, isCompact)
            }
        }
    }

    fun renderTooltip(context: GuiGraphics, text: MutableComponent, x: Float, y: Float) {
        val mc = Minecraft.getInstance()
        val textWidth = (mc.font.width(text) + 4) * 0.66f

        context.pose().pushPose()
        context.pose().translate(0.0, 0.0, 500.0)

        blitk(
            matrixStack = context.pose(),
            texture = effectTooltipEdges,
            x = x - 2,
            y = y,
            height = 11,
            width = 2,
            textureHeight = 11,
            textureWidth = 4,
            scale = 1f
        )

        blitk(
            matrixStack = context.pose(),
            texture = effectTooltipBody,
            x = x,
            y = y,
            height = 11,
            width = textWidth,
            textureHeight = 11,
            textureWidth = 5,
            scale = 1f,
            uOffset = 3f,
        )

        blitk(
            matrixStack = context.pose(),
            texture = effectTooltipEdges,
            x = x + textWidth,
            y = y,
            height = 11,
            width = 2,
            textureHeight = 11,
            textureWidth = 4,
            scale = 1f,
            uOffset = 2f
        )

        drawScaledText(
            context = context,
            text = text,
            x = x + 1.5,
            y = y + 3.5,
            scale = 0.66f,
            shadow = true,
            centered = false
        )

        context.pose().popPose()
    }

    @JvmOverloads
    fun drawCustomPosablePortrait(
        identifier: ResourceLocation,
        matrixStack: PoseStack,
        scale: Float = 13F,
        contextScale: Float = 1F,
        reversed: Boolean = false,
        state: PosableState,
        partialTicks: Float,
        limbSwing: Float = 0F,
        limbSwingAmount: Float = 0F,
        ageInTicks: Float = 0F,
        headYaw: Float = 0F,
        headPitch: Float = 0F,
        r: Float = 1F,
        g: Float = 1F,
        b: Float = 1F,
        a: Float = 1F,
        doQuirks: Boolean = true
    ) {
        RenderSystem.applyModelViewMatrix()
        matrixStack.pushPose()
        matrixStack.translate(0.0, PORTRAIT_DIAMETER.toDouble() + 2.0, 0.0)
        matrixStack.scale(scale, scale, -scale)
        matrixStack.translate(0.0, -PORTRAIT_DIAMETER / 18.0, 0.0)

        val sprite = VaryingModelRepository.getSprite(identifier, state, SpriteType.PORTRAIT);

        if (sprite == null) {
            val model = VaryingModelRepository.getPoser(identifier, state)
            state.currentModel = model
            val texture = VaryingModelRepository.getTexture(identifier, state)

            val context = RenderContext()
            model.context = context
            VaryingModelRepository.getTextureNoSubstitute(identifier, state).let { context.put(RenderContext.TEXTURE, it) }
            context.put(RenderContext.SCALE, contextScale)
            context.put(RenderContext.SPECIES, identifier)
            context.put(RenderContext.ASPECTS, state.currentAspects)
            context.put(RenderContext.POSABLE_STATE, state)
            context.put(RenderContext.DO_QUIRKS, doQuirks)

            val renderType = RenderType.entityCutout(texture)

            val quaternion1 = Axis.YP.rotationDegrees(-32F * if (reversed) -1F else 1F)
            val quaternion2 = Axis.XP.rotationDegrees(5F)

            val originalPose = state.currentPose
            state.setPoseToFirstSuitable(PoseType.PORTRAIT)
            state.updatePartialTicks(partialTicks)
            model.applyAnimations(null, state, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch)
            originalPose?.let { state.setPose(it) }

            matrixStack.translate(
                model.portraitTranslation.x * if (reversed) -1F else 1F,
                model.portraitTranslation.y + 1.5 * model.portraitScale,
                model.portraitTranslation.z - 4
            )
            matrixStack.scale(model.portraitScale, model.portraitScale, 1 / model.portraitScale)
            matrixStack.mulPose(quaternion1)
            matrixStack.mulPose(quaternion2)

            val light1 = Vector3f(0.2F, 1.0F, -1.0F)
            val light2 = Vector3f(0.1F, 0.0F, 8.0F)
            RenderSystem.setShaderLights(light1, light2)
            quaternion1.conjugate()

            val immediate = Minecraft.getInstance().renderBuffers().bufferSource()
            val buffer = immediate.getBuffer(renderType)
            val packedLight = LightTexture.pack(11, 7)

            val colour = toHex(r, g, b, a)
            model.withLayerContext(immediate, state, VaryingModelRepository.getLayers(identifier, state)) {
                model.render(context, matrixStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, colour)
                immediate.endBatch()
            }

            model.setDefault()

            Lighting.setupFor3DItems()
        } else {
            renderSprite(matrixStack, sprite)
        }

        matrixStack.popPose()
    }

    data class HoverInformation(
        val isReversed: Boolean,
        val rank: Int,
        val hoveredPokeBall: Int?
    )

    /**
     * We do this logic because for some reason it appears we cannot just render the tooltip over the portrait rendering
     * in the case of double/triple battles.
     *
     * The tile texture will always render on top on the left-hand side regardless of z-levels.
     * So what we do is check which (if any) tile is hovered, and prevent the render of any tiles under it while hovered..
     */
    private fun getHoverInformation(tickDelta: Float): HoverInformation? {
        val battle = CobblemonClient.battle ?: return null
        val slotCount = battle.battleFormat.battleType.slotsPerActor
        val isCompact = battle.battleFormat.battleType.pokemonPerSide > 1

        val portraitOffsetX = if (isCompact) COMPACT_PORTRAIT_OFFSET_X else PORTRAIT_OFFSET_X
        val portraitOffsetY = if (isCompact) COMPACT_PORTRAIT_OFFSET_Y else PORTRAIT_OFFSET_Y
        val portraitDiameter = if (isCompact) COMPACT_PORTRAIT_DIAMETER else PORTRAIT_DIAMETER

        val side1 = battle.side1
        for (i in 0 until side1.activeClientBattlePokemon.toList().size) {
            val activeBattlePokemon = side1.activeClientBattlePokemon.toList()[i]
            val playerNumberOffset = (activeBattlePokemon.getActorShowdownId()[1].digitToInt() - 1) / 2 * 10
            var x = HORIZONTAL_INSET + (slotCount - i - 1) * HORIZONTAL_SPACING.toFloat()
            val invisibleX = -(if(isCompact) COMPACT_TILE_WIDTH else TILE_WIDTH) - 1F
            activeBattlePokemon.invisibleX = invisibleX
            activeBattlePokemon.xDisplacement = x
            activeBattlePokemon.animate(tickDelta)
            x = activeBattlePokemon.xDisplacement
            val y = VERTICAL_INSET + i * (if (isCompact) COMPACT_VERTICAL_SPACING else VERTICAL_SPACING) + playerNumberOffset
            val portraitStartX = x + portraitOffsetX
            val portraitStartY = y + portraitOffsetY
            if (isPortraitHovered(portraitStartX, portraitStartY.toFloat(), portraitDiameter)) return HoverInformation(false, i, null)
            val hoveredPokemon = getHoveredPartyPokeBall(x, y.toFloat(), activeBattlePokemon.actor, false, i)
            if (hoveredPokemon != null) return HoverInformation(false, i, hoveredPokemon)
        }

        val mc = Minecraft.getInstance()
        val side2 = battle.side2
        for (i in 0 until side2.activeClientBattlePokemon.toList().size) {
            val activeBattlePokemon = side2.activeClientBattlePokemon.toList()[i]
            val playerNumberOffset = (activeBattlePokemon.getActorShowdownId()[1].digitToInt() - 1) / 2 * 10
            var x = mc.window.guiScaledWidth - (HORIZONTAL_INSET + (slotCount - i - 1) * HORIZONTAL_SPACING.toFloat()) - if(isCompact) COMPACT_TILE_WIDTH else TILE_WIDTH
            val invisibleX = mc.window.guiScaledWidth.toFloat()
            activeBattlePokemon.invisibleX = invisibleX
            activeBattlePokemon.xDisplacement = x
            activeBattlePokemon.animate(tickDelta)
            x = activeBattlePokemon.xDisplacement
            val y = VERTICAL_INSET + i * (if (isCompact) COMPACT_VERTICAL_SPACING else VERTICAL_SPACING) + playerNumberOffset
            val tileWidth = if (isCompact) COMPACT_TILE_WIDTH else TILE_WIDTH
            val portraitStartX = x + tileWidth - portraitDiameter - portraitOffsetX
            val portraitStartY = y + portraitOffsetY
            if (isPortraitHovered(portraitStartX, portraitStartY.toFloat(), portraitDiameter)) return HoverInformation(true, i, null)
            val hoveredPokemon = getHoveredPartyPokeBall(x, y.toFloat(), activeBattlePokemon.actor, true, i)
            if (hoveredPokemon != null) return HoverInformation(true, i, hoveredPokemon)
        }

        return null
    }

    private fun isPortraitHovered(portraitStartX: Float, portraitStartY: Float, portraitDiameter: Int): Boolean {
        return mouseX >= portraitStartX && mouseX <= portraitStartX + portraitDiameter && mouseY >= portraitStartY && mouseY <= portraitStartY + portraitDiameter
    }

    // Poke balls are replaced by the benched-portrait column, which does its own hover detection in
    // drawPartyColumn — so there is no poke ball left to hover.
    private fun getHoveredPartyPokeBall(x: Float, y: Float, actor: ClientBattleActor, reversed: Boolean, rank: Int): Int? = null

    fun renderExtendedBattleInfo(context: GuiGraphics, x: Float, y: Float, reversed: Boolean, pokemon: ClientBattlePokemon, compact: Boolean) {
        context.pose().pushPose()
        context.pose().translate(0.0, 0.0, 99.0)
        val startX = if (compact) {
            x - if (reversed) 113 else 5
        }
        else {
            x - if (reversed) 103 else 6
        }
        val startY = if (compact) y + 14 else y + 20
        val species = pokemon.species
        val form = species.getForm(pokemon.state.currentAspects)

        val actor = pokemon.actor.uuid
        val actorTeam = ClientBattleInformationRepository.actors[actor]
        val dto = actorTeam?.let { it.firstOrNull { dto -> pokemon.uuid == dto.uuid } }

        val expandedInfoTexture = when {
            reversed && compact -> expandedPokemonInfoCompactRight
            reversed && !compact -> expandedPokemonInfoRight
            !reversed && compact -> expandedPokemonInfoCompactLeft
            else -> expandedPokemonInfoLeft
        }

        blitk(
            matrixStack = context.pose(),
            texture = expandedInfoTexture,
            x = startX,
            y = startY,
            height = 123,
            width = 137,
            textureHeight = 123,
            textureWidth = 137,
            alpha = opacity
        )

        drawScaledText(
            context = context,
            text = "cobblemon.battle.ui.label.pokemon_form".asTranslated().bold(),
            x = startX + 36,
            y = startY + 17.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = true
        )

        drawScaledText(
            context = context,
            text = getFormText(species, form),
            x = startX + 36,
            y = startY + 24.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = true
        )

        drawScaledText(
            context = context,
            text = "cobblemon.battle.ui.label.buffs".asTranslated().bold(),
            x = startX + 36,
            y = startY + 34.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = true
        )

        drawScaledText(
            context = context,
            text = "cobblemon.stat.attack.name".asTranslated(),
            x = startX + 10,
            y = startY + 42.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = false
        )

        dto?.buffs?.get(Stats.ATTACK)?.let {
            drawScaledText(
                context = context,
                text = BattlePokemonInfoPanel.getBoostText(Stats.ATTACK, it),
                x = startX + 50,
                y = startY + 42.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.stat.defence.name".asTranslated(),
            x = startX + 10,
            y = startY + 50.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = false
        )

        dto?.buffs?.get(Stats.DEFENCE)?.let {
            drawScaledText(
                context = context,
                text = BattlePokemonInfoPanel.getBoostText(Stats.DEFENCE, it),
                x = startX + 50,
                y = startY + 50.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.battle.ui.stats.special_attack".asTranslated(),
            x = startX + 10,
            y = startY + 58.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = false
        )

        dto?.buffs?.get(Stats.SPECIAL_ATTACK)?.let {
            drawScaledText(
                context = context,
                text = BattlePokemonInfoPanel.getBoostText(Stats.SPECIAL_ATTACK, it),
                x = startX + 50,
                y = startY + 58.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.battle.ui.stats.special_defence".asTranslated(),
            x = startX + 10,
            y = startY + 66.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = false
        )

        dto?.buffs?.get(Stats.SPECIAL_DEFENCE)?.let {
            drawScaledText(
                context = context,
                text = BattlePokemonInfoPanel.getBoostText(Stats.SPECIAL_DEFENCE, it),
                x = startX + 50,
                y = startY + 66.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.stat.speed.name".asTranslated(),
            x = startX + 10,
            y = startY + 74.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = false
        )

        dto?.buffs?.get(Stats.SPEED)?.let {
            drawScaledText(
                context = context,
                text = BattlePokemonInfoPanel.getBoostText(Stats.SPEED, it),
                x = startX + 50,
                y = startY + 74.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.stat.accuracy.name".asTranslated(),
            x = startX + 10,
            y = startY + 82.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = false
        )

        dto?.buffs?.get(Stats.ACCURACY)?.let {
            drawScaledText(
                context = context,
                text = BattlePokemonInfoPanel.getBoostText(Stats.ACCURACY, it),
                x = startX + 50,
                y = startY + 82.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.stat.evasion.name".asTranslated(),
            x = startX + 10,
            y = startY + 90.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = false
        )

        dto?.buffs?.get(Stats.EVASION)?.let {
            drawScaledText(
                context = context,
                text = BattlePokemonInfoPanel.getBoostText(Stats.EVASION, it),
                x = startX + 50,
                y = startY + 90.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.battle.ui.label.ability".asTranslated().bold(),
            x = startX + 100,
            y = startY + 17.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = true
        )

        val ability = dto?.ability?.let { Abilities.get(it) }
        val abilityName = ability?.displayName?.text() ?: "?".text()

        drawScaledText(
            context = context,
            text = abilityName,
            x = startX + 100,
            y = startY + 24.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = true
        )

        drawScaledText(
            context = context,
            text = "cobblemon.battle.ui.label.moves".asTranslated().bold(),
            x = startX + 100,
            y = startY + 34.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = true
        )

        if (dto?.moves?.size != 4) {
            for (i in 0 until 4) {
                drawScaledText(
                    context = context,
                    text = "?".text(),
                    x = startX + 100,
                    y = startY + 42.5 + i * 8,
                    scale = SCALE,
                    shadow = true,
                    opacity = opacity,
                    centered = true
                )
            }
        }
        else {
            val questionMarkText = "?".text()
            dto.moves.forEachIndexed { index, moveDTO ->
                val move = moveDTO?.move ?: questionMarkText
                drawScaledText(
                    context = context,
                    text = move.string.text(),
                    x = if (move == questionMarkText) startX + 100 else startX + 74,
                    y = startY + 42.5 + index * 8,
                    scale = SCALE,
                    shadow = true,
                    opacity = opacity,
                    centered = move == questionMarkText
                )

                if (moveDTO != null) {
                    drawScaledTextJustifiedRight(
                        context = context,
                        text = moveDTO.timesUsed.toString().text(),
                        x = startX + 127.5,
                        y = startY + 42.5 + index * 8,
                        scale = SCALE,
                        shadow = true,
                        opacity = opacity,
                    )
                }
            }
        }

        if (dto?.speed != null) {
            val speed = dto.speed!!
            val speedBoost = dto.buffs.get(Stats.SPEED) ?: 1.0
            val speedAfterBoost = (speed * speedBoost).toInt()
            drawScaledText(
                context = context,
                text = "cobblemon.stat.speed.name".asTranslated(),
                x = startX + 100,
                y = startY + 76.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = true
            )

            drawScaledText(
                context = context,
                text = speedAfterBoost.toString().text(),
                x = startX + 100,
                y = startY + 83.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = true
            )
        }
        else {
            drawScaledText(
                context = context,
                text = "cobblemon.battle.ui.label.speed_tier".asTranslated(),
                x = startX + 100,
                y = startY + 76.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = true
            )

            val speedBoost = dto?.buffs?.get(Stats.SPEED) ?: 1.0
            val speedTier = getSpeedRange(form, pokemon.level, speedBoost)

            drawScaledText(
                context = context,
                text = speedTier,
                x = startX + 100,
                y = startY + 83.5,
                scale = SCALE,
                shadow = true,
                opacity = opacity,
                centered = true
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.battle.ui.label.held_item".asTranslated(),
            x = startX + 40,
            y = startY + 110.5,
            scale = SCALE,
            shadow = true,
            opacity = opacity,
            centered = true
        )

        if (dto != null && dto.terastallized != null) {
            val teraType = dto.terastallized
            if (teraType is ElementalTypeTeraType) {
                TypeIcon(
                    x = startX + 100.5,
                    y = startY + 99,
                    type = teraType.type,
                    small = false,
                    centeredX = true
                ).render(context)
            }
            else {
                TypeIcon(
                    x = startX + 100.5,
                    y = startY + 99,
                    small = false,
                    centeredX = true,
                    isStellar = true
                ).render(context)
            }
        }
        else {
            TypeIcon(
                x = startX + 100.5,
                y = startY + 99,
                type = form.primaryType,
                secondaryType = form.secondaryType,
                small = false,
                centeredX = true
            ).render(context)
        }

        if (form.secondaryType != null) {
            blitk(
                matrixStack = context.pose(),
                texture = cobblemonResource("textures/gui/battle/type_spacer_double.png"),
                x = (startX + 69.0) / 0.45,
                y = (startY + 102.5) / 0.45,
                height = 24,
                width = 140,
                textureHeight = 24,
                textureWidth = 140,
                alpha = opacity,
                scale = 0.45f
            )
        }
        else {
            blitk(
                matrixStack = context.pose(),
                texture = cobblemonResource("textures/gui/summary/type_spacer.png"),
                x = (startX + 70.5) / 0.45,
                y = (startY + 102.5) / 0.45,
                height = 24,
                width = 132,
                textureHeight = 24,
                textureWidth = 132,
                alpha = opacity,
                scale = 0.45f
            )
        }

        val heldItem = dto?.heldItem
        if (heldItem != null && !heldItem.isEmpty()) {
            context.pose().pushPose()
            context.pose().scale(0.95F, 0.95F, 0.95F)
            val itemX = (startX + 4) / 0.95
            context.renderItem(heldItem, itemX.toInt(), y.toInt() + if (compact) 122 else 129)
            context.pose().popPose()
        }
        else {
            blitk(
                matrixStack = context.pose(),
                texture = questionMarkIcon,
                x = startX + 6,
                y = startY + 103.5,
                height = 11,
                width = 10,
                alpha = opacity
            )
        }
        context.pose().popPose()
    }

    private fun getMultiplierText(multiplier: Double): MutableComponent {
        val rounded = decimalFormat.format(multiplier)
        val str = "x$rounded"
        return when {
            multiplier > 1.0 -> str.green()
            multiplier < 1.0 -> str.red()
            else -> str.text()
        }
    }

    private fun getSpeedRange(form: FormData, level: Int, speedBoost: Double): MutableComponent {
        val base = form.baseStats[Stats.SPEED] ?: 0
        val maxIV = 31
        val maxEV = 252
        val minNatureMod = 0.9
        val maxNatureMod = 1.1

        val minSpeed = getStat(base, level, 0, 0, minNatureMod) * speedBoost
        val maxSpeed = getStat(base, level, maxIV, maxEV, maxNatureMod) * speedBoost
        return "cobblemon.battle.ui.speed_tier".asTranslated(minSpeed.toInt(), maxSpeed.toInt())
    }

    private fun getStat(base: Int, level: Int, iv: Int, ev: Int, natureMod: Double): Int {
        return floor((floor(((2.0 * base + iv + floor(ev / 4.0)) * level) / 100) + 5) * natureMod).toInt()
    }

    private fun getFormText(species: Species, form: FormData): MutableComponent {
        val speciesName = species.name
        val formName = form.name
        return if (formName == "Normal") speciesName.text() else "$speciesName-$formName".text()
    }

    fun drawPartyPokeBalls(
        context: GuiGraphics,
        x: Float,
        y: Float,
        reversed: Boolean,
        actor: ClientBattleActor,
        rank: Int,
        isCompact: Boolean
    ) {
        val partySize = ClientBattleInformationRepository.actors[actor.uuid]?.size ?: 0
        if (partySize < 2) return
        val spacing = (if (isCompact) COMPACT_VERTICAL_SPACING else VERTICAL_SPACING) + 18
        for (i in 0 until partySize) {
            val fainted = ClientBattleInformationRepository.actors[actor.uuid]!![i].fainted
            val unrevealed = ClientBattleInformationRepository.actors[actor.uuid]!![i].activeBattlePokemonDTO == null
            val active = ClientBattleInformationRepository.actors[actor.uuid]!![i].uuid in actor.activePokemon.map { it.battlePokemon?.uuid }
            val vOffset = when {
                unrevealed -> 36
                fainted -> 18
                active -> 54
                else -> 0
            }
            val scalar = 0.4
            val offsetX = if (reversed) (x + 18 + i * (19 * scalar)) / scalar else (x + 94.725 - 18 + (5 * 19 * scalar) - i * (19 * scalar)) / scalar
            val offsetY = if (active) y + VERTICAL_INSET + rank * spacing else y + VERTICAL_INSET + 3 + rank * spacing
            blitk(
                matrixStack = context.pose(),
                texture = partyPokeballIcon,
                x = offsetX,
                y = offsetY,
                height = if (active) 21 else 18,
                width = 18,
                textureHeight = 76,
                textureWidth = 18,
                vOffset = vOffset,
                scale = scalar.toFloat(),
                alpha = opacity
            )
        }
    }

    /**
     * Draws the benched-team column now, unless the BattleGUI screen is open — in which case the screen
     * renders after the HUD (including chat) and the switch menu's underlay gradient, so we queue the
     * column and let [renderDeferredColumns] draw it on top once the screen has rendered everything else.
     */
    private fun queueOrDrawColumn(context: GuiGraphics, tileY: Float, portraitStartX: Float, portraitDiameter: Int, reversed: Boolean, actor: ClientBattleActor, partialTicks: Float, isCompact: Boolean) {
        if (Minecraft.getInstance().screen is BattleGUI) {
            pendingColumns.add(PendingColumn(tileY, portraitStartX, portraitDiameter, reversed, actor, partialTicks, isCompact))
        } else {
            drawPartyColumn(context, tileY, portraitStartX, portraitDiameter, reversed, actor, partialTicks, isCompact)
        }
    }

    /**
     * Drains the work queued during the HUD pass (benched columns, then the active Pokémon's hover panel),
     * drawing it above the screen's own widgets, the switch-menu underlay and the chat. Called by
     * [BattleGUI] at the end of its render, so this is the last thing painted. The active panel is drawn
     * after the columns so a hovered active Pokémon's info sits on top of the portraits, not behind them.
     */
    fun renderDeferredColumns(context: GuiGraphics) {
        if (pendingColumns.isEmpty() && pendingActivePanels.isEmpty()) return
        val queuedColumns = pendingColumns.toList()
        pendingColumns.clear()
        queuedColumns.forEach { drawPartyColumn(context, it.tileY, it.portraitStartX, it.portraitDiameter, it.reversed, it.actor, it.partialTicks, it.isCompact) }
        val queuedPanels = pendingActivePanels.toList()
        pendingActivePanels.clear()
        queuedPanels.forEach { renderExtendedBattleInfo(context, it.x, it.y, it.reversed, it.pokemon, it.isCompact) }
    }

    /**
     * Renders the rest of an actor's team as a vertical column of small portraits beneath the active tile.
     * Each portrait is smaller than the active so it reads as a benched Pokémon, faded when fainted.
     * Hovering one shows its info panel to the side. Knowledge gating is inherited from the DTO.
     */
    fun drawPartyColumn(
        context: GuiGraphics,
        tileY: Float,
        portraitStartX: Float,
        portraitDiameter: Int,
        reversed: Boolean,
        actor: ClientBattleActor,
        partialTicks: Float,
        isCompact: Boolean
    ) {
        val team = ClientBattleInformationRepository.actors[actor.uuid] ?: return
        val activeUuids = actor.activePokemon.mapNotNull { it.battlePokemon?.uuid }
        val benched = team.filter { it.uuid !in activeUuids }
        if (benched.isEmpty()) return

        // Each cell is the Info menu tile (41x39 frame with a 28 portrait) uniformly scaled down by `f`,
        // so the portraits read as smaller benched versions while keeping the same border and animation.
        val f = 0.65f
        val frameWidth = 41
        val frameHeight = 39
        val cellWidth = frameWidth * f
        val cellHeight = frameHeight * f
        val gap = 1f
        val tileHeight = if (isCompact) COMPACT_TILE_HEIGHT else TILE_HEIGHT
        // The whole column can be dragged as one group to a custom offset (default = under the active).
        val offset = columnOffsets[actor.uuid] ?: (0f to 0f)
        val cellX = portraitStartX + (portraitDiameter - cellWidth) / 2f + offset.first
        val startY = tileY + tileHeight + 3f + offset.second
        // Record the column bounds for click/drag hit-testing (used by BattleGUI mouse handling).
        columnHitboxes[actor.uuid] = floatArrayOf(cellX, startY, cellWidth, benched.size * cellHeight + (benched.size - 1) * gap)
        val matrixStack = context.pose()
        var hoveredPokemon: ClientBattlePokemon? = null
        var hoveredDto: BattlePokemonDTO? = null
        var hoveredCellY = 0f

        benched.forEachIndexed { index, dto ->
            val cellY = startY + index * (cellHeight + gap)
            val isFainted = dto.fainted
            val activeDto = dto.activeBattlePokemonDTO

            if (activeDto != null) {
                val clientPokemon = ClientBattlePokemon(
                    uuid = activeDto.uuid,
                    properties = activeDto.properties,
                    aspects = activeDto.aspects,
                    displayName = activeDto.displayName,
                    hpValue = activeDto.hpValue,
                    maxHp = activeDto.maxHp,
                    isHpFlat = activeDto.isFlatHp,
                    status = activeDto.status,
                    statChanges = activeDto.statChanges,
                )
                clientPokemon.actor = actor
                val species = clientPokemon.species
                // Persistent per-Pokémon state so the portrait keeps animating across frames.
                val state = partyTileStates.getOrPut(activeDto.uuid) { FloatingState() }
                state.currentAspects = activeDto.aspects

                val insetX = 5 + (if (reversed) 3 else 0)

                // 1. Portrait background (unscissored), in scaled tile space.
                matrixStack.pushPose()
                matrixStack.translate(cellX.toDouble(), cellY.toDouble(), 0.0)
                matrixStack.scale(f, f, f)
                blitk(
                    matrixStack = matrixStack,
                    texture = battleInfoUnderlay,
                    x = insetX,
                    y = 4,
                    width = 28,
                    height = 28,
                    alpha = opacity
                )
                matrixStack.popPose()

                // 2. Portrait, clipped to the (scaled) portrait window.
                context.enableScissor(
                    (cellX + insetX * f).toInt(),
                    (cellY + 5 * f).toInt(),
                    (cellX + (insetX + 28) * f).toInt(),
                    (cellY + (5 + 28) * f).toInt()
                )
                matrixStack.pushPose()
                matrixStack.translate(cellX.toDouble(), cellY.toDouble(), 0.0)
                matrixStack.scale(f, f, f)
                matrixStack.translate(insetX + 28 / 2.0, 0.0, 0.0)
                drawCustomPosablePortrait(
                    identifier = species.resourceIdentifier,
                    matrixStack = matrixStack,
                    scale = 18F,
                    contextScale = species.getForm(state.currentAspects).baseScale,
                    reversed = reversed,
                    state = state,
                    partialTicks = partialTicks
                )
                matrixStack.popPose()
                context.disableScissor()

                // 3. Border frame on top (disabled variant when fainted), in scaled tile space.
                val frame = when {
                    isFainted && reversed -> partyColumnTileDisabledReversed
                    isFainted -> partyColumnTileDisabled
                    reversed -> partyColumnTileReversed
                    else -> partyColumnTile
                }
                matrixStack.pushPose()
                matrixStack.translate(cellX.toDouble(), cellY.toDouble(), 0.0)
                matrixStack.scale(f, f, f)
                blitk(
                    matrixStack = matrixStack,
                    texture = frame,
                    x = 0,
                    y = 0,
                    width = frameWidth,
                    height = frameHeight,
                    alpha = opacity
                )
                matrixStack.popPose()

                // 4. Status condition badge in the bottom-left of the portrait. Reuses the same coloured
                //    status bar Cobblemon draws on the active Pokémon, shrunk to a corner pill with the
                //    3-letter abbreviation (BRN/PAR/SLP/...). Fainted mons are already greyed, so skip them.
                val status = activeDto.status
                if (status != null && !isFainted) {
                    val abbrev = status.showdownName
                    val badgeW = 17f
                    val badgeH = 7f
                    val badgeX = cellX + insetX * f
                    val badgeY = cellY + (4 + 28) * f - badgeH
                    matrixStack.pushPose()
                    matrixStack.translate(badgeX.toDouble(), badgeY.toDouble(), 50.0)
                    matrixStack.scale(badgeW / 74f, badgeH / 7f, 1f)
                    blitk(
                        matrixStack = matrixStack,
                        texture = cobblemonResource("textures/gui/battle/battle_status_$abbrev.png"),
                        x = 0,
                        y = 0,
                        width = 74,
                        height = 7,
                        textureWidth = 74,
                        textureHeight = 7,
                        alpha = opacity
                    )
                    matrixStack.popPose()
                    drawScaledText(
                        context = context,
                        text = abbrev.uppercase().text().bold(),
                        x = badgeX + badgeW / 2f,
                        y = badgeY + 1.5f,
                        scale = 0.5f,
                        shadow = true,
                        centered = true
                    )
                }

                if (mouseX >= cellX && mouseX <= cellX + cellWidth && mouseY >= cellY && mouseY <= cellY + cellHeight) {
                    hoveredPokemon = clientPokemon
                    hoveredDto = dto
                    hoveredCellY = cellY
                }
            }
            else {
                // Defensive fallback: species not transmitted (no team preview) — show an unrevealed ball.
                blitk(
                    matrixStack = matrixStack,
                    texture = partyPokeballIcon,
                    x = cellX,
                    y = cellY,
                    width = cellWidth,
                    height = cellHeight,
                    textureHeight = 76,
                    textureWidth = 18,
                    vOffset = 36,
                    alpha = opacity
                )
            }
        }

        val hp = hoveredPokemon
        val hd = hoveredDto
        // Don't pop the panel while this column is being dragged.
        if (hp != null && hd != null && draggedColumnActor != actor.uuid) {
            val maxY = (Minecraft.getInstance().window.guiScaledHeight - 116).toFloat()
            val panelY = hoveredCellY.coerceAtMost(maxY).coerceAtLeast(4f)
            BattlePokemonInfoPanel.render(context, cellX, panelY, cellWidth, reversed, hp, hd, showGap = false, showBuffs = false)
        }
    }

    /** Begins dragging a benched-team column if the press landed on one. Returns true if consumed. */
    fun columnMouseClicked(mx: Double, my: Double): Boolean {
        for ((uuid, box) in columnHitboxes) {
            if (mx >= box[0] && mx <= box[0] + box[2] && my >= box[1] && my <= box[1] + box[3]) {
                draggedColumnActor = uuid
                dragStartMouseX = mx
                dragStartMouseY = my
                val off = columnOffsets[uuid] ?: (0f to 0f)
                dragStartOffsetX = off.first
                dragStartOffsetY = off.second
                columnDragMoved = false
                return true
            }
        }
        return false
    }

    /** Moves the grabbed column group with the cursor. Returns true while a drag is in progress. */
    fun columnMouseDragged(mx: Double, my: Double): Boolean {
        val uuid = draggedColumnActor ?: return false
        columnDragMoved = true
        columnOffsets[uuid] = (dragStartOffsetX + (mx - dragStartMouseX).toFloat()) to (dragStartOffsetY + (my - dragStartMouseY).toFloat())
        return true
    }

    /** Ends a column drag. A click without movement resets that column back under the active Pokémon. */
    fun columnMouseReleased(): Boolean {
        val uuid = draggedColumnActor ?: return false
        if (!columnDragMoved) {
            columnOffsets.remove(uuid)
        }
        draggedColumnActor = null
        return true
    }

    fun drawFieldIcons(context: GuiGraphics) {
        val battle = CobblemonClient.battle ?: return
        val battleInfo = ClientBattleInformationRepository.battles[battle.battleId] ?: return

        val effects = mutableListOf<FieldEffect>()
        battleInfo.terrain?.let { effects.add(it) }
        battleInfo.room?.let { effects.add(it) }

        val mc = Minecraft.getInstance()
        val startX = (mc.window.guiScaledWidth / 2) - ((13 * 0.9 * effects.size) / 2)
        effects.forEachIndexed { index, effect ->
            blitk(
                matrixStack = context.pose(),
                texture = cobblemonResource("textures/gui/battle/effects/${effect.id}.png"),
                x = (startX + (13 * 0.9 * index)) / 0.9,
                y = 32 / 0.9,
                height = 12,
                width = 12,
                textureHeight = 12,
                textureWidth = 12,
                scale = 0.9f
            )

            if (mouseX >= (startX + (13 * 0.9 * index))
                && mouseX <= ((startX + (13 * 0.9 * index))) + 12 * 0.9
                && mouseY >= 31 / 0.9
                && mouseY <= (32 / 0.9) + 12 * 0.9)
            {
                val text = getFieldEffectText(effect, battleInfo, false)
                val textWidth = (mc.font.width(text) + 4) * 0.66f
                val tooltipStartX = (mc.window.guiScaledWidth / 2) - (textWidth / 2)

                renderTooltip(context, text, tooltipStartX, 44f)
            }
        }
    }

    private fun drawPrimaryWeatherIcon(context: GuiGraphics) {
        val battle = CobblemonClient.battle ?: return
        val battleInfo = ClientBattleInformationRepository.battles[battle.battleId] ?: return
        val weather = battleInfo.weather ?: return
        val supported = listOf("sunnyday", "sandstorm", "snow", "sunnyday")
        if (weather.id !in supported) return

        val mc = Minecraft.getInstance()
        val scale = 1.3f
        val startX = (mc.window.guiScaledWidth / 2) - ((32 * scale / 2))

        blitk(
            matrixStack = context.pose(),
            texture = cobblemonResource("textures/gui/battle/weather/${weather.id}.png"),
            x = startX / scale,
            y = 15 / scale,
            height = 12,
            width = 32,
            textureHeight = 12,
            textureWidth = 32,
            scale = scale
        )

        if (mouseX >= (startX + (13 * scale))
            && mouseX <= ((startX + (13 * scale))) + 32 * scale
            && mouseY >= 15 / scale
            && mouseY <= (15 / scale) + 13 * scale)
        {
            val text = getFieldEffectText(weather, battleInfo, false)
            val textWidth = (mc.font.width(text) + 4) * 0.66f
            val tooltipStartX = (mc.window.guiScaledWidth / 2) - (textWidth / 2)

            renderTooltip(context, text, tooltipStartX, 44f)
        }
    }

    private fun getFieldEffectText(effect: FieldEffect, battleInfo: BattleInformationDTO, sided: Boolean): MutableComponent {
        val translationKeyStart = if (sided) "cobblemon.battle.ui.sided_effect" else "cobblemon.battle.ui.effect"
        val turnsPassed = battleInfo.turn - effect.turnStarted
        return when (effect.id) {
            "primordialsea" -> "${translationKeyStart}.primordialsea".asTranslated()
            "desolateland" -> "${translationKeyStart}.desolateland".asTranslated()
            "deltastream" -> "$translationKeyStart.deltastream".asTranslated()
            "trickroom" -> "$translationKeyStart.trickroom".asTranslated((5 - turnsPassed).coerceAtLeast(1))
            "magicroom" -> "$translationKeyStart.magicroom".asTranslated((5 - turnsPassed).coerceAtLeast(1))
            "wonderroom" -> "$translationKeyStart.wonderroom".asTranslated((5 - turnsPassed).coerceAtLeast(1))
            "tailwind" -> "$translationKeyStart.tailwind".asTranslated((4 - turnsPassed).coerceAtLeast(1))
            "toxicspikes" -> "$translationKeyStart.toxicspikes".asTranslated()
            "spikes" -> "$translationKeyStart.spikes".asTranslated()
            "stealthrock" -> "$translationKeyStart.stealthrock".asTranslated()
            "stickyweb" -> "$translationKeyStart.stickyweb".asTranslated()
            else -> {
                if (turnsPassed < 5) {
                    "$translationKeyStart.${effect.id}.range".asTranslated(5 - turnsPassed, 8 - turnsPassed)
                }
                else {
                    // Past 5 turns means the rock (e.g. Damp Rock) is confirmed, so the 8-turn duration
                    // is now certain — count down from that. Clamp to 1 so a weather that lingers a turn
                    // past its 8th shows "1" rather than "0 turns remaining".
                    "$translationKeyStart.${effect.id}".asTranslated((8 - turnsPassed).coerceAtLeast(1))
                }
            }
        }
    }

    private fun drawTurnCounter(context: GuiGraphics) {
        val battle = CobblemonClient.battle ?: return
        val battleInfo = ClientBattleInformationRepository.battles[battle.battleId] ?: return

        val mc = Minecraft.getInstance()
        val x = (mc.window.guiScaledWidth / 2) - (103 / 2)

        blitk(
            matrixStack = context.pose(),
            texture = turnCounter,
            x = x,
            y = 4,
            height = 9,
            width = 103,
            textureHeight = 9,
            textureWidth = 103,
            alpha = opacity
        )

        val now = Instant.now()
        if (ClientBattleInformationRepository.mustChooseBy != null && !now.isAfter(ClientBattleInformationRepository.mustChooseBy)) {
            val totalSecondsRemaining = Duration.between(now, ClientBattleInformationRepository.mustChooseBy).seconds
            val minutesRemaining = totalSecondsRemaining / 60
            val secondsRemaining = String.format("%02d", totalSecondsRemaining % 60)
            val timer = "$minutesRemaining:$secondsRemaining"
            drawScaledText(
                context = context,
                text = "cobblemon.battle.ui.turn.timer".asTranslated(battleInfo.turn, timer).bold(),
                x = (mc.window.guiScaledWidth / 2),
                y = 6,
                scale = 0.75f,
                shadow = true,
                opacity = opacity,
                centered = true
            )
        }
        else {
            drawScaledText(
                context = context,
                text = "cobblemon.battle.ui.turn".asTranslated(battleInfo.turn).bold(),
                x = (mc.window.guiScaledWidth / 2),
                y = 6,
                scale = 0.75f,
                shadow = true,
                opacity = opacity,
                centered = true
            )
        }
    }

    private fun drawPokeBall(
        state: ClientBallDisplay,
        matrixStack: PoseStack,
        scale: Float = 5F,
        partialTicks: Float,
        reversed: Boolean = false
    ) {
        val context = RenderContext()
        val model = VaryingModelRepository.getPoser(state.pokeBall.name, state)
        val texture = VaryingModelRepository.getTexture(state.pokeBall.name, state)
        val renderType = RenderType.entityCutout(texture)//model.getLayer(texture)

        RenderSystem.applyModelViewMatrix()
        val quaternion1 = Axis.YP.rotationDegrees(-32F * if (reversed) -1F else 1F)
        val quaternion2 = Axis.XP.rotationDegrees(5F)

        state.currentModel = model
        state.setPoseToFirstSuitable(PoseType.PORTRAIT)
        state.updatePartialTicks(partialTicks)
        model.applyAnimations(null, state, 0F, 0F, 0F, 0F, 0F)

        matrixStack.scale(scale, scale, -scale)
        matrixStack.translate(0.0, 5.5, -4.0)
        matrixStack.pushPose()

        matrixStack.scale(scale * state.scale, scale * state.scale, 0.1F)

        matrixStack.mulPose(quaternion1)
        matrixStack.mulPose(quaternion2)

        val light1 = Vector3f(2.2F, 4.0F, -4.0F)
        val light2 = Vector3f(1.1F, -4.0F, 7.0F)
        RenderSystem.setShaderLights(light1, light2)
        quaternion1.conjugate()

        val immediate = Minecraft.getInstance().renderBuffers().bufferSource()
        val buffer = immediate.getBuffer(renderType)
        val packedLight = LightTexture.pack(11, 7)
        model.render(context, matrixStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)

        immediate.endBatch()

        matrixStack.popPose()

        Lighting.setupFor3DItems()
    }

    fun onLogout() {
        this.opacity = MIN_OPACITY
        this.passedSeconds = 0F
        this.lastKnownBattle = null
    }

}
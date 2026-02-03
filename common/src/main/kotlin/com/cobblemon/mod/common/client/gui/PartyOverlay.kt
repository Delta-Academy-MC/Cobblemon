/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.gui.drawPosablePortrait
import com.cobblemon.mod.common.api.text.darkGray
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.battle.BattleGUI
import com.cobblemon.mod.common.client.gui.toast.CobblemonToast
import com.cobblemon.mod.common.client.keybind.boundKey
import com.cobblemon.mod.common.client.keybind.keybinds.HidePartyBinding
import com.cobblemon.mod.common.client.keybind.keybinds.SummaryBinding
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.getDepletableRedGreen
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.renderScaledGuiItemIcon
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.toAssetPath
import java.util.UUID
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.toasts.AdvancementToast
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import kotlin.math.ceil
import kotlin.math.floor

class PartyOverlay : Gui(Minecraft.getInstance()) {

    companion object {
        private const val SLOT_HEIGHT = 30
        private const val SLOT_WIDTH = 62
        private const val SLOT_SPACING = 4
        private const val PORTRAIT_DIAMETER = 21
        private const val SCALE = 0.5F

        private val partySlot = cobblemonResource("textures/gui/party/party_slot.png")
        private val partySlotActive = cobblemonResource("textures/gui/party/party_slot_active.png")
        private val partySlotFainted = cobblemonResource("textures/gui/party/party_slot_fainted.png")
        private val partySlotFaintedActive = cobblemonResource("textures/gui/party/party_slot_fainted_active.png")
        private val partySlotCollapsed = cobblemonResource("textures/gui/party/party_slot_collapsed.png")
        private val genderIconMale = cobblemonResource("textures/gui/party/party_gender_male.png")
        private val genderIconFemale = cobblemonResource("textures/gui/party/party_gender_female.png")
        private val portraitBackground = cobblemonResource("textures/gui/party/party_slot_portrait_background.png")
        private val newMovesPopup = cobblemonResource("textures/gui/party/party_slot_notification_new_move.png")
        private val newEvoPopup = cobblemonResource("textures/gui/party/party_slot_notification_evolution.png")
        private val levelScroll = cobblemonResource("textures/gui/party/party_slot_portrait_level_up.png")

        private val screenExemptions: List<Class<out Screen>> = listOf(
            ChatScreen::class.java,
            BattleGUI::class.java
        )

        fun canRender(): Boolean {
            val minecraft = Minecraft.getInstance()
            // Hiding if a Screen is open and not exempt
            if (minecraft.screen != null) {
                if (!screenExemptions.contains(minecraft.screen?.javaClass as Class<out Screen>))
                    return false
            }
            if (minecraft.options.hideGui || minecraft.debugOverlay.showDebugScreen()) {
                return false
            }
            // Hiding if toggled via Keybind
            if (HidePartyBinding.shouldHide) {
                return false
            }
            return true
        }
    }

    private val stateAtIndex = hashMapOf<Int, Pair<UUID, FloatingState>>()

    val starterToast = CobblemonToast(
        Mth.createInsecureUUID(),
        CobblemonItems.POKE_BALL.defaultInstance,
        lang("ui.starter.choose_starter_title", SummaryBinding.boundKey().displayName).red(),
        lang("ui.starter.choose_starter_description", SummaryBinding.boundKey().displayName).darkGray(),
        AdvancementToast.BACKGROUND_SPRITE,
        -1F,
        0
    )

    private var attachedToast = false

    fun resetAttachedToast() {
        val minecraft = Minecraft.getInstance()
        minecraft.toasts.clear()
        starterToast.nextVisibility = Toast.Visibility.SHOW
        attachedToast = false
    }

    fun getPokemonState(index: Int, pokemonUUID: UUID): FloatingState {
        val state = this.stateAtIndex.getOrPut(index) { pokemonUUID to FloatingState() }
        if (state.first != pokemonUUID) {
            val newState = FloatingState()
            this.stateAtIndex[index] = pokemonUUID to newState
            return newState
        }
        return state.second
    }

    override fun render(context: GuiGraphics, tickCounter: DeltaTracker) {
        if (!canRender()) return

        val partialDeltaTicks = tickCounter.realtimeDeltaTicks
        val minecraft = Minecraft.getInstance()

        val panelX = 0
        val party = CobblemonClient.storage.party
        val matrices = context.pose()
        if (party.slots.none { it != null }) {
            if (CobblemonClient.clientPlayerData.promptStarter &&
                !CobblemonClient.clientPlayerData.starterLocked &&
                !CobblemonClient.clientPlayerData.starterSelected &&
                !CobblemonClient.checkedStarterScreen
            ) {
                if (!this.attachedToast) {
                    // Adding starter toast on start and connect of client to server.
                    minecraft.toasts.addToast(this.starterToast)
                    this.attachedToast = true
                }
            }
            return // Don't render the stuff below if no Pokémon in party
        }

        val totalHeight = party.slots.size * SLOT_HEIGHT
        val midY = minecraft.window.guiScaledHeight / 2
        val startY = (midY - totalHeight / 2) - ((SLOT_SPACING * 5) / 2)
        val portraitFrameOffsetX = 22
        val portraitFrameOffsetY = 2
        val selectedSlot = CobblemonClient.storage.selectedSlot

        party.forEachIndexed { index, pokemon ->
            if (pokemon != null) {
                val selectedOffsetX = if (selectedSlot == index) 6 else 0
                val indexOffsetY = (SLOT_HEIGHT + SLOT_SPACING) * index
                val y = startY + indexOffsetY + portraitFrameOffsetY

                blitk(
                    matrixStack = matrices,
                    texture = portraitBackground,
                    x = panelX + portraitFrameOffsetX + selectedOffsetX,
                    y = y,
                    height = PORTRAIT_DIAMETER,
                    width = PORTRAIT_DIAMETER
                )

                context.enableScissor(
                    panelX + portraitFrameOffsetX + selectedOffsetX,
                    y,
                    panelX + portraitFrameOffsetX + selectedOffsetX + PORTRAIT_DIAMETER,
                    y + PORTRAIT_DIAMETER
                )

                matrices.pushPose()
                matrices.translate(
                    panelX + portraitFrameOffsetX + selectedOffsetX + PORTRAIT_DIAMETER / 2.0 - 1.0,
                    y.toDouble() - 12,
                    0.0
                )

                val shouldAnimate = when (Cobblemon.config.partyPortraitAnimations) {
                    PokemonGUIAnimationStyle.NEVER_ANIMATE -> false
                    PokemonGUIAnimationStyle.ANIMATE_SELECTED -> selectedSlot == index
                    PokemonGUIAnimationStyle.ALWAYS_ANIMATE -> true
                }

                drawPosablePortrait(
                    identifier = pokemon.species.resourceIdentifier,
                    matrixStack = matrices,
                    doQuirks = false,
                    partialTicks = if (shouldAnimate) partialDeltaTicks else 0F,
                    contextScale = pokemon.form.baseScale,
                    state = getPokemonState(index = index, pokemonUUID = pokemon.uuid).also { it.currentAspects = pokemon.aspects }
                )

                matrices.popPose()
                context.disableScissor()
            }

            val selectedOffsetX = if (selectedSlot == index) 6 else 0
            val indexOffsetY = (SLOT_HEIGHT + SLOT_SPACING) * index
            val indexY = startY + indexOffsetY

            val slotTexture = if (pokemon != null)
                if (pokemon.isFainted())
                    if (selectedSlot == index) partySlotFaintedActive else partySlotFainted
                else
                    if (selectedSlot == index) partySlotActive else partySlot
            else partySlotCollapsed

            blitk(
                matrixStack = matrices,
                texture = slotTexture,
                x = panelX,
                y = indexY,
                height = SLOT_HEIGHT,
                width = SLOT_WIDTH
            )

            if (pokemon != null) {
                val expGainedData = PartyOverlayDataControl.getExpGainedData(pokemon.uuid)

                //Handling Level Up Scroll / For some reason draw text makes any future blitk render behind our model render, don't ask why
                var displayLevelScroll = false
                var levelScrollOffset = 0F
                var levelScrollPositionOffset = 0F
                var levelScrollSize = 17F
                if (expGainedData != null && expGainedData.oldLevel != null) {
                    val ticksWithDelta = (expGainedData.ticks + partialDeltaTicks) - (PartyOverlayDataControl.BAR_UPDATE_BEFORE_TIME + PartyOverlayDataControl.BAR_FLASH_TIME)
                    val transition = ticksWithDelta / PartyOverlayDataControl.LEVEL_UP_PORTRAIT_TIME
                    if ((0F..<1F).contains(transition)) {
                        displayLevelScroll = true
                        var step = ceil(transition * 14)
                        if (step <= 4) {
                            levelScrollSize = step * 4
                            levelScrollPositionOffset = 17 - (levelScrollSize)
                        } else if (step <= 10) {
                            step -= 4
                            levelScrollOffset = -1 + (step * 4)
                        } else {
                            step -= 10
                            levelScrollSize = 20 - (step * 4)
                            levelScrollOffset = 23 + (step * 4)
                        }
                    }
                }

                if (displayLevelScroll) {
                    blitk(
                        matrixStack = matrices,
                        texture = levelScroll,
                        x = (panelX + selectedOffsetX + portraitFrameOffsetX + 2),
                        y = (indexY + portraitFrameOffsetY + 2 + levelScrollPositionOffset),
                        height = levelScrollSize,
                        width = 17,
                        textureHeight = 43,
                        vOffset = levelScrollOffset
                    )
                }

                val stateIcon = pokemon.state.getIcon(pokemon)
                if (stateIcon != null) {
                    blitk(
                        matrixStack = matrices,
                        texture = stateIcon,
                        x = (panelX + selectedOffsetX + 8) / SCALE,
                        y = (indexY + portraitFrameOffsetY + 1) / SCALE,
                        height = 17,
                        width = 24,
                        scale = SCALE
                    )
                }

                drawScaledText(
                    context = context,
                    text = lang("ui.lv"),
                    x = panelX + selectedOffsetX + 6.5F,
                    y = indexY + 13.5,
                    scale = SCALE,
                    centered = true,
                    shadow = true
                )

                var level = pokemon.level
                if (expGainedData?.oldLevel != null) {
                    val ticksWithDelta = expGainedData.ticks + partialDeltaTicks
                    if (ticksWithDelta < PartyOverlayDataControl.BAR_UPDATE_BEFORE_TIME) {
                        level = expGainedData.oldLevel!!
                    }
                }

                drawScaledText(
                    context = context,
                    text = level.toString().text(),
                    x = panelX + selectedOffsetX + 6.5F,
                    y = indexY + 18,
                    scale = SCALE,
                    centered = true,
                    shadow = true
                )

                drawScaledText(
                    context = context,
                    text = pokemon.getDisplayName(),
                    x = panelX + selectedOffsetX + 2.5F,
                    y = indexY + 25,
                    scale = SCALE
                )

                if (pokemon.gender != Gender.GENDERLESS) {
                    blitk(
                        matrixStack = matrices,
                        texture = if (pokemon.gender == Gender.MALE) genderIconMale else genderIconFemale,
                        x = (panelX + selectedOffsetX + 40) / SCALE,
                        y = (indexY + 25)  / SCALE,
                        height = 7,
                        width = 5,
                        scale = SCALE
                    )
                }

                val hpRatio = pokemon.currentHealth / pokemon.maxHealth.toFloat()
                val barHeightMax = 18
                val hpBarWidth = 2
                val hpBarHeight = hpRatio * barHeightMax

                val (red, green) = getDepletableRedGreen(hpRatio)

                blitk(
                    matrixStack = matrices,
                    texture = CobblemonResources.WHITE,
                    x = panelX + selectedOffsetX + 46,
                    y = indexY + (barHeightMax - hpBarHeight) + 5,
                    width = hpBarWidth,
                    height = hpBarHeight,
                    textureHeight = hpBarHeight / hpRatio,
                    vOffset = barHeightMax - hpBarHeight,
                    red = red * 0.8F,
                    green = green * 0.8F,
                    blue = 0.27F
                )

                val expRatio: Float
                val expForThisLevel = pokemon.experience - if (pokemon.level == 1) 0 else pokemon.experienceGroup.getExperience(pokemon.level)
                val expToNextLevel = pokemon.experienceGroup.getExperience(pokemon.level + 1) - pokemon.experienceGroup.getExperience(pokemon.level)

                var expRed = 0.2
                var expGreen = 0.65
                var expBlue = 0.84

                if (expGainedData != null) {
                    val ticksWithDelta = expGainedData.ticks + partialDeltaTicks

                    if (expGainedData.oldLevel != null) {
                        //Handling Exp Bar
                        if (ticksWithDelta <= PartyOverlayDataControl.BAR_UPDATE_BEFORE_TIME) {
                            val expForLastLevel = (pokemon.experience - expGainedData.expGained) - if (expGainedData.oldLevel!! == 1) 0 else pokemon.experienceGroup.getExperience(expGainedData.oldLevel!!)
                            val expNeededForLastLevel = pokemon.experienceGroup.getExperience(expGainedData.oldLevel!! + 1) - pokemon.experienceGroup.getExperience(expGainedData.oldLevel!!)

                            val transition = (ticksWithDelta / PartyOverlayDataControl.BAR_UPDATE_BEFORE_TIME.toFloat()).coerceIn(0F..1F)
                            val transitionXP = expForLastLevel + ((expNeededForLastLevel - expForLastLevel) * transition)
                            expRatio = transitionXP / expNeededForLastLevel
                        } else if (ticksWithDelta <= PartyOverlayDataControl.BAR_UPDATE_BEFORE_TIME + PartyOverlayDataControl.BAR_FLASH_TIME) {
                            expRatio = 1F
                            expRed = 1.0
                            expGreen = 1.0
                            expBlue = 1.0
                        }
                        else {
                            val adjustedTicks = ticksWithDelta - (PartyOverlayDataControl.BAR_UPDATE_BEFORE_TIME + PartyOverlayDataControl.BAR_FLASH_TIME)
                            val transition = (adjustedTicks / PartyOverlayDataControl.BAR_UPDATE_BEFORE_TIME.toFloat()).coerceIn(0F..1F)
                            val transitionXP = expForThisLevel * transition
                            expRatio = transitionXP / expToNextLevel
                        }
                    } else {
                        //Handling Exp Bar
                        val transition = (ticksWithDelta / PartyOverlayDataControl.BAR_UPDATE_NO_LEVEL_TIME.toFloat()).coerceIn(0F..1F)
                        val transitionXP = expForThisLevel - (expGainedData.expGained * (1 - transition))
                        expRatio = transitionXP / expToNextLevel
                    }
                } else {
                    expRatio = expForThisLevel / expToNextLevel.toFloat()
                }

                val expBarWidth = 1
                val expBarHeight = expRatio * barHeightMax

                blitk(
                    matrixStack = matrices,
                    texture = CobblemonResources.WHITE,
                    x = panelX + selectedOffsetX + 49,
                    y = indexY + (barHeightMax - expBarHeight) + 5,
                    width = expBarWidth,
                    height = expBarHeight,
                    textureHeight = expBarHeight / expRatio,
                    vOffset = barHeightMax - expBarHeight,
                    red = expRed,
                    green = expGreen,
                    blue = expBlue
                )

                var ticksForMove = -1F
                var ticksForEvo = -1F

                //Handling Popups
                if (expGainedData != null) {
                    val ticksWithDelta = expGainedData.ticks + partialDeltaTicks
                    val ticks = ticksWithDelta - (PartyOverlayDataControl.BAR_UPDATE_BEFORE_TIME + PartyOverlayDataControl.BAR_FLASH_TIME)
                    if (expGainedData.countOfMovesLearned > 0) {
                        ticksForMove = ticks
                    }
                    if (expGainedData.countOfEvosUnlocked > 0) {
                        ticksForEvo = ticks
                    }
                } else {
                    val evoGainedData = PartyOverlayDataControl.getEvolutionData(pokemon.uuid)
                    if (evoGainedData != null) {
                        ticksForEvo = evoGainedData.ticks + partialDeltaTicks
                    }
                    val moveGainedData = PartyOverlayDataControl.getMovesData(pokemon.uuid)
                    if (moveGainedData != null) {
                        ticksForMove = moveGainedData.ticks + partialDeltaTicks
                    }
                }

                fun popupData(ticks: Float) : Pair<Float, Float> {
                    if (ticks > 0) {
                        if (ticks <= PartyOverlayDataControl.POPUP_TIME.fadeIn) {
                            val transition = ticks / PartyOverlayDataControl.POPUP_TIME.fadeIn
                            val step = (floor(transition * 3)) + 1
                            return (4 - step) to (step * 0.25F)
                        } else if (ticks <= PartyOverlayDataControl.POPUP_TIME.fadeIn + PartyOverlayDataControl.POPUP_TIME.hold) {
                            return 0F to 1F
                        } else {
                            val adjustedPopupTicks = ticks - (PartyOverlayDataControl.POPUP_TIME.fadeIn + PartyOverlayDataControl.POPUP_TIME.hold)
                            val transition = adjustedPopupTicks / PartyOverlayDataControl.POPUP_TIME.fadeIn
                            val step = (floor(transition * 3)) + 1
                            return (-step) to (1F - (step * 0.25F))
                        }
                    } else return 0F to 0F
                }

                val (movePopupYOffset, movePopupTransparency) = popupData(ticksForMove)
                val (evoPopupYOffset, evoPopupTransparency) = popupData(ticksForEvo)

                if (evoPopupTransparency > 0) {
                    blitk(
                        matrixStack = matrices,
                        texture = newEvoPopup,
                        x = (panelX + selectedOffsetX + 56.5F) / SCALE,
                        y = (indexY + 4 + evoPopupYOffset) / SCALE,
                        width = 37,
                        height = 20,
                        scale = SCALE,
                        alpha = evoPopupTransparency
                    )
                }
                if (movePopupTransparency > 0) {
                    blitk(
                        matrixStack = matrices,
                        texture = newMovesPopup,
                        x = (panelX + selectedOffsetX + 56.5F) / SCALE,
                        y = (indexY + 17 + movePopupYOffset) / SCALE,
                        width = 37,
                        height = 20,
                        scale = SCALE,
                        alpha = movePopupTransparency
                    )
                }

                val ballIcon = pokemon.caughtBall.name.toAssetPath("textures/gui/ball/", ".png")
                val ballHeight = 22
                blitk(
                    matrixStack = matrices,
                    texture = ballIcon,
                    x = (panelX + selectedOffsetX + 43.5) / SCALE,
                    y = (indexY + 22) / SCALE,
                    height = ballHeight,
                    width = 18,
                    vOffset = if (stateIcon != null) ballHeight else 0,
                    textureHeight = ballHeight * 2,
                    scale = SCALE
                )

                val status = pokemon.status?.status
                if (!pokemon.isFainted() && status != null) {
                    val statusName = status.showdownName
                    blitk(
                        matrixStack = matrices,
                        texture = cobblemonResource("textures/gui/party/status_$statusName.png"),
                        x = panelX + selectedOffsetX + 51,
                        y = indexY + 8,
                        height = 14,
                        width = 4
                    )
                }

                // Held Item
                val heldItem = pokemon.heldItemNoCopy()
                if (!heldItem.isEmpty) {
                    renderScaledGuiItemIcon(
                        itemStack = heldItem,
                        x = panelX + selectedOffsetX + 12.0,
                        y = indexY + 14.0,
                        scale = 0.5,
                        matrixStack = matrices,
                        zTranslation = 0.0F
                    )
                }
            }
        }
    }
}
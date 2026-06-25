/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.battle

import com.cobblemon.mod.common.battles.PassActionResponse
import com.cobblemon.mod.common.battles.ShowdownActionResponse
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.ClientBattleActor
import com.cobblemon.mod.common.client.battle.SingleActionRequest
import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleActionSelection
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleBackButton
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleGeneralActionSelection
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleSwitchPokemonSelection
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleTeamInfoSelection
import com.cobblemon.mod.common.client.gui.battle.subscreen.ForfeitConfirmationSelection
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane
import com.cobblemon.mod.common.client.gui.battle.widgets.BattleOptionTile
import com.cobblemon.mod.common.client.keybind.boundKey
import com.cobblemon.mod.common.client.keybind.keybinds.PartySendBinding
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.net.messages.server.battle.RemoveSpectatorPacket
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.util.battleLang
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen

class BattleGUI : Screen(battleLang("gui.title")), CobblemonRenderable {
    companion object {
        const val OPTION_VERTICAL_SPACING = 3
        const val OPTION_HORIZONTAL_SPACING = 3
        const val OPTION_ROOT_X = 12
        const val OPTION_VERTICAL_OFFSET = 85

        val fightResource = cobblemonResource("textures/gui/battle/battle_menu_fight.png")
        val bagResource = cobblemonResource("textures/gui/battle/battle_menu_bag.png")
        val switchResource = cobblemonResource("textures/gui/battle/battle_menu_switch.png")
        val runResource = cobblemonResource("textures/gui/battle/battle_menu_run.png")
        val forfeitResource = cobblemonResource("textures/gui/battle/battle_menu_forfeit.png")
    }

    lateinit var messagePane: BattleMessagePane
    var opacity = 0F
    val actor = CobblemonClient.battle?.side1?.actors?.find { it.uuid == Minecraft.getInstance().player?.uuid }
    val specBackButton = BattleBackButton(OPTION_ROOT_X - 3F, Minecraft.getInstance().window.guiScaledHeight - 22F)
    val teamInfoButton = BattleOptionTile(
        battleGUI = this,
        x = OPTION_ROOT_X,
        y = Minecraft.getInstance().window.guiScaledHeight - OPTION_VERTICAL_OFFSET + BattleOptionTile.OPTION_HEIGHT + OPTION_HORIZONTAL_SPACING,
        resource = BattleGeneralActionSelection.battleInfoSelection,
        text = "cobblemon.battle.ui.team_info.button".asTranslated(),
        onClick = {}
    )

    var queuedActions = mutableListOf<() -> Unit>()

    override fun init() {
        super.init()
        messagePane = BattleMessagePane(CobblemonClient.battle!!.messages)
        messagePane.opacity = CobblemonClient.battleOverlay.opacityRatio.toFloat().coerceAtLeast(0.3F)
        addRenderableWidget(messagePane)
    }

    fun changeActionSelection(newSelection: BattleActionSelection?) {
        val current = children().find { it is BattleActionSelection }
        queuedActions.add {
            current?.let(this::removeWidget)
            if (newSelection != null) {
                addRenderableWidget(newSelection)
            }
        }
    }

    fun getCurrentActionSelection() = children().filterIsInstance<BattleActionSelection>().firstOrNull()

    fun removeInvalidBattleActionSelection() {
        children().filterIsInstance<BattleActionSelection>().firstOrNull()?.let {
            children().remove(it)
        }
    }

    fun selectAction(request: SingleActionRequest, response: ShowdownActionResponse?) {
        val battle = CobblemonClient.battle ?: return
        if (request.response == null) {
            request.response = response
            changeActionSelection(null)
            battle.checkForFinishedChoosing()
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        opacity = CobblemonClient.battleOverlay.opacityRatio.toFloat()
        children().filterIsInstance<BattleMessagePane>().forEach { it.opacity = opacity.coerceAtLeast(0.3F) }

        queuedActions.forEach { it() }
        queuedActions.clear()

        val battle = CobblemonClient.battle
        if (battle == null) {
            onClose()
            return
        } else if (CobblemonClient.battleOverlay.opacityRatio <= 0.1 && CobblemonClient.battle?.minimised == true) {
            onClose()
            return
        }

        if (actor != null) {
            if (battle.mustChoose) {
                if (getCurrentActionSelection() == null) {
                    val unanswered = battle.getFirstUnansweredRequest()
                    if (unanswered != null) {
                        changeActionSelection(deriveRootActionSelection(actor, unanswered))
                    }
                }
            } else if (getCurrentActionSelection() != null) {
                changeActionSelection(null)
            }
        }

        if (battle.spectating && !BattleTeamInfoSelection.visible) {
            specBackButton.render(context, mouseX, mouseY, delta)
            teamInfoButton.render(context, mouseX, mouseY, delta)
        }

        val currentSelection = getCurrentActionSelection()
        if (currentSelection is ForfeitConfirmationSelection) {
            drawScaledText(
                context = context,
                text = battleLang("ui.forfeit_confirmation", PartySendBinding.boundKey().displayName),
                x = Minecraft.getInstance().window.guiScaledWidth / 2,
                y = (Minecraft.getInstance().window.guiScaledHeight / 5),
                opacity = 0.75F * opacity,
                centered = true
            )
        }


        queuedActions.forEach { it() }
        queuedActions.clear()
        CobblemonClient.battleOverlay.mouseX = mouseX
        CobblemonClient.battleOverlay.mouseY = mouseY
        // The benched-team portrait columns + active hover panels are queued during the HUD pass and drawn
        // here, last, so they sit above the switch-menu underlay, our own widgets and the chat.
        CobblemonClient.battleOverlay.renderDeferredColumns(context)
    }

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {

    }

    fun deriveRootActionSelection(actor: ClientBattleActor, request: SingleActionRequest): BattleActionSelection? {
        return if (request.forceSwitch) {
            BattleSwitchPokemonSelection(this, request)
        } else {
            // Known quirk of Showdown. It'll ask for actions on fainted slots
            // Also during a forced switch in doubles/triples it'll ask for actions on non-switching slots
            val pokemon = request.side?.pokemon?.firstOrNull { it.uuid == request.activePokemon.battlePokemon?.uuid }
            if (pokemon == null || pokemon.condition.contains("fnt") || pokemon.commanding || request.moveSet == null) {
                this.selectAction(request, PassActionResponse)
                null
            } else {
                BattleGeneralActionSelection(this, request)
            }
        }
    }

    override fun isPauseScreen() = false

    override fun onClose() {
        super.onClose()
        CobblemonClient.battle?.minimised = true
        BattleTeamInfoSelection.visible = false
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (CobblemonClient.battleOverlay.columnMouseDragged(mouseX, mouseY)) return true
        if (this::messagePane.isInitialized) messagePane.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (CobblemonClient.battleOverlay.columnMouseReleased()) return true
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (chr.toString().equals(PartySendBinding.boundKey().displayName.string, ignoreCase = true) && CobblemonClient.battleOverlay.opacity == BattleOverlay.MAX_OPACITY && PartySendBinding.canAction()) {
            return minimizeBattle()
        }
        return super.charTyped(chr, modifiers)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Grab a benched-team portrait column to drag it (left-click). Consumes the click if one was hit.
        if (button == 0 && CobblemonClient.battleOverlay.columnMouseClicked(mouseX, mouseY)) return true
        if (button == PartySendBinding.boundKey().value && CobblemonClient.battleOverlay.opacity == BattleOverlay.MAX_OPACITY && PartySendBinding.canAction()) {
            return minimizeBattle()
        }

        val battle = CobblemonClient.battle
        if (battle?.spectating == true && !BattleTeamInfoSelection.visible) {
            if (specBackButton.isHovered(mouseX, mouseY)) {
                RemoveSpectatorPacket(battle.battleId).sendToServer()
                CobblemonClient.endBattle()
            }
            if (teamInfoButton.isHovered(mouseX, mouseY)) {
                changeActionSelection(BattleTeamInfoSelection(this))
                BattleTeamInfoSelection.visible = true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun minimizeBattle(): Boolean {
        val battle = CobblemonClient.battle ?: return false
        battle.minimised = !battle.minimised
        PartySendBinding.actioned()
        return true
    }
}
/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.battle.subscreen

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.moves.categories.DamageCategories
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.gold
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.battles.*
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.battle.Effectiveness
import com.cobblemon.mod.common.client.battle.MoveEffectivenessCalculator
import com.cobblemon.mod.common.client.battle.SingleActionRequest
import com.cobblemon.mod.common.client.gui.MoveCategoryIcon
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.gui.battle.BattleGUI
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.moves.MovesWidget
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.drawScaledTextJustifiedRight
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.util.battleLang
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.toRGB
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth.floor
import java.math.RoundingMode
import java.text.DecimalFormat

class BattleMoveSelection(
    battleGUI: BattleGUI,
    val request: SingleActionRequest,
) : BattleActionSelection(
    battleGUI = battleGUI,
    x = 20,
    y = Minecraft.getInstance().window.guiScaledHeight - 84,
    width = 100,
    height = 100,
    battleLang("ui.select_move")
) {
    companion object {
        const val MOVE_WIDTH = 92
        const val MOVE_HEIGHT = 24
        const val MOVE_VERTICAL_SPACING = 5F
        const val MOVE_HORIZONTAL_SPACING = 13F

        val moveTexture = cobblemonResource("textures/gui/battle/battle_move.png")
        val moveOverlayTexture = cobblemonResource("textures/gui/battle/move_outline.png")
        val moveTooltipIcon = cobblemonResource("textures/gui/battle/move_tooltip_icon.png")
        val moveTooltip = cobblemonResource("textures/gui/battle/move_tooltip.png")
        val moveTooltipForStatus = cobblemonResource("textures/gui/battle/move_tooltip_status.png")

        val effectiveIcon = cobblemonResource("textures/gui/battle/effective_icon.png")
        val extremelyEffectiveIcon = cobblemonResource("textures/gui/battle/extremely_effective_icon.png")
        val immuneIcon = cobblemonResource("textures/gui/battle/immune_icon.png")
        val mostlyIneffectiveIcon = cobblemonResource("textures/gui/battle/mostly_ineffective_icon.png")
        val superEffectiveIcon = cobblemonResource("textures/gui/battle/super_effective_icon.png")
        val notVeryEffectiveIcon = cobblemonResource("textures/gui/battle/not_very_effective_icon.png")

        private val decimalFormat = DecimalFormat("#.##").also {
            it.roundingMode = RoundingMode.CEILING
        }
    }

    val moveSet = request.moveSet!!
    val baseTiles = moveSet.moves.mapIndexed { index, inBattleMove ->
        val isEven = index % 2 == 0
        val x = if (isEven) this.x.toFloat() else this.x + MOVE_HORIZONTAL_SPACING + MOVE_WIDTH
        val y = if (index > 1) this.y + MOVE_HEIGHT + MOVE_VERTICAL_SPACING else this.y.toFloat()
        // if already dynamaxed, base tiles are the gimmick tiles
        if (moveSet.hasActiveGimmick())
            DynamaxButton.DynamaxTile(this, inBattleMove, x, y)
        else
            MoveTile(this, inBattleMove, x, y)
    }
    var moveTiles = baseTiles

    val backButton = BattleBackButton(x - 11F, Minecraft.getInstance().window.guiScaledHeight - 22F)
    val gimmickButtons = moveSet.getGimmicks().filter { it !in moveSet.pendingGimmickUsedThisTurn }.mapIndexed { index, gimmick ->
        val initOff = BattleBackButton.WIDTH * 0.65F
        val xOff = initOff + BattleGimmickButton.SPACING * index
        BattleGimmickButton.create(gimmick, this, backButton.x + xOff, backButton.y)
    }

    val shiftButton = BattleShiftButton(x + 22.5F, Minecraft.getInstance().window.guiScaledHeight - 22F)

    open class MoveTile(
        val moveSelection: BattleMoveSelection,
        val move: InBattleMove,
        val x: Float,
        val y: Float,
    ) {
        var moveTemplate = Moves.getByNameOrDummy(move.id)
        val pokemon = moveSelection.request.activePokemon.actor.pokemon.firstOrNull { it.uuid == moveSelection.request.activePokemon.battlePokemon?.uuid }
        val elementalType = moveTemplate.getEffectiveElementalType(pokemon)
        var rgb = elementalType.hue.toRGB()

        open val targetList: List<Targetable>? get() = move.target.targetList(moveSelection.request.activePokemon)
        open val response: MoveActionResponse get() = MoveActionResponse(move.id, targetPnx)
        open val selectable: Boolean get() = !move.disabled

        val targetPnx: String? get() = targetList?.let { targets ->
            return@let when {
                targets.isEmpty() -> null
                targets.size == 1 -> targets[0].getPNX()
                else -> null    // TODO: multi-battles
            }
        }

        fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {

            val selectConditionOpacity = moveSelection.opacity * if (!selectable) 0.5F else 1F

            blitk(
                matrixStack = context.pose(),
                texture = moveTexture,
                x = x,
                y = y,
                width = MOVE_WIDTH,
                height = MOVE_HEIGHT,
                vOffset = if (selectable && isHovered(mouseX.toDouble(), mouseY.toDouble())) MOVE_HEIGHT else 0,
                textureHeight = MOVE_HEIGHT * 2,
                red = rgb.first,
                green = rgb.second,
                blue = rgb.third,
                alpha = selectConditionOpacity
            )

            blitk(
                matrixStack = context.pose(),
                texture = moveOverlayTexture,
                x = x,
                y = y,
                width = MOVE_WIDTH,
                height = MOVE_HEIGHT,
                alpha = moveSelection.opacity
            )

            // Type Icon
            TypeIcon(
                x = x - 9,
                y = y + 2,
                type = elementalType,
                opacity = moveSelection.opacity
            ).render(context)

            // Move Category
            MoveCategoryIcon(
                x = x + 41,
                y = y + 14.5,
                category = moveTemplate.damageCategory,
                opacity = moveSelection.opacity
            ).render(context)

            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = moveTemplate.displayName.bold(),
                x = x + 17,
                y = y + 2,
                opacity = selectConditionOpacity,
                shadow = true
            )

            var movePPText = Component.literal("${move.pp}/${move.maxpp}").bold()

            if (move.pp <= floor(move.maxpp / 2F)) {
                movePPText = if (move.pp == 0) movePPText.red() else movePPText.gold()
            }

            if (move.pp == 100 && move.maxpp == 100) {
                movePPText = "—/—".text().bold()
            }

            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = movePPText,
                x = x + 67,
                y = y + 14,
                opacity = moveSelection.opacity,
                centered = true
            )

            blitk(
                matrixStack = context.pose(),
                texture = moveTooltipIcon,
                x = x + 82,
                y = y + 14,
                width = 9,
                height = 9,
                textureHeight = 18,
                textureWidth = 9,
                alpha = moveSelection.opacity,
                vOffset = if (isTooltipHovered(mouseX.toDouble(), mouseY.toDouble())) 9 else 0
            )

            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = "i".text().bold(),
                x = x + 87.375,
                y = y + 14,
                opacity = moveSelection.opacity,
                centered = true
            )

            if (isTooltipHovered(mouseX.toDouble(), mouseY.toDouble())) {
                renderTooltip(context)
            }
        }

        fun renderTooltip(context: GuiGraphics) {
            val tooltipY = Minecraft.getInstance().window.guiScaledHeight - 84 - 59 - 4
            val tooltipX = 20

            val move = Moves.getByNameOrDummy(move.id)

            val effectiveness = getMoveEffectiveness(move)

            blitk(
                matrixStack = context.pose(),
                texture = if (effectiveness == null) moveTooltipForStatus else moveTooltip,
                x = tooltipX,
                y = tooltipY,
                width = 195,
                height = 56,
                alpha = moveSelection.opacity
            )

            drawScaledText(
                context = context,
                text = lang("ui.power"),
                x = tooltipX + 18,
                y = tooltipY + 11,
                opacity = moveSelection.opacity,
                centered = false,
                scale = 0.65f
            )

            blitk(
                matrixStack = context.pose(),
                texture = MovesWidget.movesPowerIconResource,
                x = (tooltipX + 8.5) / 0.66f,
                y = (tooltipY + 9.5) / 0.66f,
                width = 10,
                height = 10,
                alpha = moveSelection.opacity,
                scale = 0.66f
            )

            val powerStr = if (move.power <= 0) "—" else move.power.toInt().toString()

            drawScaledTextJustifiedRight(
                context = context,
                text = powerStr.text(),
                x = tooltipX + 73,
                y = tooltipY + 11,
                opacity = moveSelection.opacity,
                scale = 0.65f
            )

            blitk(
                matrixStack = context.pose(),
                texture = MovesWidget.movesAccuracyIconResource,
                x = (tooltipX + 8.5) / 0.66f,
                y = (tooltipY + 26) / 0.66f,
                width = 10,
                height = 10,
                alpha = moveSelection.opacity,
                scale = 0.66f
            )

            drawScaledText(
                context = context,
                text = lang("ui.accuracy"),
                x = tooltipX + 18,
                y = tooltipY + 27,
                opacity = moveSelection.opacity,
                centered = false,
                scale = 0.65f
            )

            val accuracyStr = format(move.accuracy).text()

            drawScaledTextJustifiedRight(
                context = context,
                text = accuracyStr,
                x = tooltipX + 73,
                y = tooltipY + 27,
                opacity = moveSelection.opacity,
                scale = 0.65f
            )

            blitk(
                matrixStack = context.pose(),
                texture = MovesWidget.movesEffectIconResource,
                x = (tooltipX + 8.5) / 0.66f,
                y = (tooltipY + 42.5) / 0.66f,
                width = 10,
                height = 10,
                alpha = moveSelection.opacity,
                scale = 0.66f
            )

            drawScaledText(
                context = context,
                text = lang("ui.effect"),
                x = tooltipX + 18,
                y = tooltipY + 43.5,
                opacity = moveSelection.opacity,
                centered = false,
                scale = 0.65f
            )

            val moveEffect = format(move.effectChances.firstOrNull() ?: 0.0).text()

            drawScaledTextJustifiedRight(
                context = context,
                text = moveEffect,
                x = tooltipX + 73,
                y = tooltipY + 43.5,
                opacity = moveSelection.opacity,
                scale = 0.65f
            )

            val moveDescription = move.description
            val lines = Minecraft.getInstance().font.splitter.splitLines(moveDescription, 200, moveDescription.style)
            val orderedLines = Language.getInstance().getVisualOrder(lines)
            val maxLines = if (effectiveness == null) 6 else 4
            orderedLines.take(maxLines).forEachIndexed { index, line ->
                drawScaledText(
                    context = context,
                    text = line,
                    x = tooltipX + 85,
                    y = tooltipY + 11 + (6 * index),
                    opacity = moveSelection.opacity,
                    scaleX = 0.5f,
                    scaleY = 0.5f,
                    centered = false
                )
            }

            if (effectiveness == null) return
            val effectivenessText = getEffectivenessText(effectiveness)
            val effectivenessIcon = getEffectivenessIcon(effectiveness)
            val effectivenessValue = getEffectivenessValue(effectiveness)

            blitk(
                matrixStack = context.pose(),
                texture = effectivenessIcon,
                x = (tooltipX + 84) / 0.5f,
                y = (tooltipY + 43) / 0.5f,
                width = 12,
                height = 12,
                alpha = moveSelection.opacity,
                scale = 0.5f
            )

            drawScaledText(
                context = context,
                text = effectivenessText,
                x = tooltipX + 94,
                y = tooltipY + 43.5,
                opacity = moveSelection.opacity,
                centered = false,
                scale = 0.65f
            )

            drawScaledText(
                context = context,
                text = effectivenessValue,
                x = tooltipX + 168,
                y = tooltipY + 43.5,
                opacity = moveSelection.opacity,
                centered = false,
                scale = 0.65f
            )
        }

        private fun getEffectivenessValue(effectiveness: Effectiveness): MutableComponent {
            return when (effectiveness) {
                Effectiveness.IMMUNE -> "0.00x".text()
                Effectiveness.MOSTLY_INEFFECTIVE -> "0.25x".text()
                Effectiveness.NOT_VERY_EFFECTIVE -> "0.50x".text()
                Effectiveness.EFFECTIVE -> "1.00x".text()
                Effectiveness.SUPER_EFFECTIVE -> "2.00x".text()
                Effectiveness.EXTREMELY_EFFECTIVE -> "4.00x".text()
            }
        }

        private fun getEffectivenessIcon(effectiveness: Effectiveness): ResourceLocation {
            return when (effectiveness) {
                Effectiveness.IMMUNE -> immuneIcon
                Effectiveness.MOSTLY_INEFFECTIVE -> mostlyIneffectiveIcon
                Effectiveness.NOT_VERY_EFFECTIVE -> notVeryEffectiveIcon
                Effectiveness.EFFECTIVE -> effectiveIcon
                Effectiveness.SUPER_EFFECTIVE -> superEffectiveIcon
                Effectiveness.EXTREMELY_EFFECTIVE -> extremelyEffectiveIcon
            }
        }

        private fun getEffectivenessText(effectiveness: Effectiveness): MutableComponent {
            return when (effectiveness) {
                Effectiveness.IMMUNE -> "cobblemon.battle.ui.effectiveness.has_no_effect".asTranslated()
                Effectiveness.MOSTLY_INEFFECTIVE -> "cobblemon.battle.ui.effectiveness.mostly_ineffective".asTranslated()
                Effectiveness.NOT_VERY_EFFECTIVE -> "cobblemon.battle.ui.effectiveness.not_very_effective".asTranslated()
                Effectiveness.EFFECTIVE -> "cobblemon.battle.ui.effectiveness.effective".asTranslated()
                Effectiveness.SUPER_EFFECTIVE -> "cobblemon.battle.ui.effectiveness.super_effective".asTranslated()
                Effectiveness.EXTREMELY_EFFECTIVE -> "cobblemon.battle.ui.effectiveness.extremely_effective".asTranslated()
            }
        }

        private fun getMoveEffectiveness(move: MoveTemplate): Effectiveness? {
            val battle = CobblemonClient.battle ?: return null
            val opponents = battle.side2.activeClientBattlePokemon.toList()
            if (opponents.size != 1) return null
            val opponent = battle.side2.activeClientBattlePokemon.first().battlePokemon ?: return null

            val aspects: Set<String> = opponent.state.currentAspects
            val opponentForm = opponent.species.getForm(aspects)
            if (move.damageCategory == DamageCategories.STATUS) return null
            return MoveEffectivenessCalculator.getMoveEffectiveness(move.elementalType, opponentForm.primaryType, opponentForm.secondaryType)
        }

        fun format(input: Double): String {
            if (input <= 0) return "—"
            return "${decimalFormat.format(input)}%"
        }

        fun isHovered(mouseX: Double, mouseY: Double): Boolean {
            return !isTooltipHovered(mouseX, mouseY) && mouseX >= x && mouseX <= x + MOVE_WIDTH && mouseY >= y && mouseY <= y + MOVE_HEIGHT
        }

        fun isTooltipHovered(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x + 82 && mouseX <= x + MOVE_WIDTH && mouseY >= y + 14 && mouseY <= y + MOVE_HEIGHT
        }

        fun onClick() {
            if (!selectable) return
            moveSelection.playDownSound(Minecraft.getInstance().soundManager)
            moveSelection.battleGUI.selectAction(moveSelection.request, response)
        }
    }

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        moveTiles.forEach {
            it.render(context, mouseX, mouseY, delta)
        }
        backButton.render(context, mouseX, mouseY, delta)
        gimmickButtons.forEach {
            it.render(context.pose(), mouseX, mouseY, delta)
        }
        if(this.request.activePokemon.getFormat().battleType.slotsPerActor == 3 && (request.activePokemon.getPNX()[2] == 'a' || request.activePokemon.getPNX()[2] == 'c')) {
            shiftButton.render(context, mouseX, mouseY, delta)
        }
    }

    override fun mousePrimaryClicked(mouseX: Double, mouseY: Double): Boolean {
        val move = moveTiles.find { it.isHovered(mouseX, mouseY) }
        val gimmick = gimmickButtons.find { it.isHovered(mouseX, mouseY) }
        if (move != null) {
            if(request.activePokemon.getFormat().battleType.pokemonPerSide == 1) {
                move.onClick()
            } else {
                battleGUI.changeActionSelection(
                    BattleTargetSelection(
                        battleGUI,
                        request,
                        move.move,
                        move.response.gimmickID,
                        move.move.gimmickMove
                    )
                )
                playDownSound(Minecraft.getInstance().soundManager)
            }
            return true
        } else if (backButton.isHovered(mouseX, mouseY)) {
            playDownSound(Minecraft.getInstance().soundManager)
            battleGUI.changeActionSelection(null)
        } else if (gimmick != null) {
            gimmickButtons.filter { it != gimmick }.forEach { it.toggled = false }
            moveTiles = if (gimmick.toggle()) gimmick.tiles else baseTiles
        } else if(shiftButton.isHovered(mouseX,mouseY)) {
            playDownSound(Minecraft.getInstance().soundManager)
            battleGUI.selectAction(request, ShiftActionResponse())
        }
        return false
    }

    override fun playDownSound(soundManager: SoundManager) {
        soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F))
    }
}
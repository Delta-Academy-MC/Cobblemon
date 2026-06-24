package com.cobblemon.mod.common.client.gui.battle

import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.green
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.drawScaledTextJustifiedRight
import com.cobblemon.mod.common.client.render.getDepletableRedGreen
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.net.messages.client.battle.BattlePokemonDTO
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor
import net.minecraft.network.chat.contents.TranslatableContents
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The "Info" style expanded stat panel for a single battle Pokémon (form, buffs/debuffs, ability,
 * moves, speed/speed-tier, types, held item). It renders to the *side* of a tile, with a connector
 * pointing back at it, and expands inwards (towards the screen centre) on both sides.
 *
 * Shared by the team Info screen (the team Info screen)
 * and the on-field benched-team portrait column ([BattleOverlay.drawPartyColumn]) so the two stay identical.
 *
 * Knowledge gating is inherited from [dto]: the team owner gets full data while opponents only carry
 * what has been revealed, so unrevealed ability/moves show "?" and speed falls back to a tier range.
 */
object BattlePokemonInfoPanel {
    private val expandedPokemonInfo = cobblemonResource("textures/gui/battle/expanded_pokemon_info_center.png")
    // Variant for non-active Pokémon: the left (Buffs/Debuffs) column is cleared for a Current HP gauge.
    private val expandedPokemonInfoInactive = cobblemonResource("textures/gui/battle/expanded_pokemon_info_center_inactive.png")
    // Same battle health-bar texture the active Pokémon's tile uses, for the Current HP gauge.
    private val healthBar = cobblemonResource("textures/gui/battle/health_bar.png")
    private val hoverGap = cobblemonResource("textures/gui/battle/hover_gap.png")
    private val hoverGapReversed = cobblemonResource("textures/gui/battle/hover_gap_reversed.png")

    private val multiplierFormat = DecimalFormat("0.##")

    fun render(
        context: GuiGraphics,
        tileX: Float,
        tileY: Float,
        tileWidth: Float,
        reversed: Boolean,
        pokemon: ClientBattlePokemon,
        dto: BattlePokemonDTO,
        showGap: Boolean = true,
        showBuffs: Boolean = true
    ) {
        context.pose().pushPose()
        context.pose().translate(0.0, 0.0, 300.0)
        // Panel sits just beside the tile (inwards) with the connector bridging the gap.
        val startX = if (reversed) tileX - 136 else tileX + tileWidth - 1
        val startY = tileY
        val species = pokemon.species
        val form = species.getForm(pokemon.state.currentAspects)

        // The connector is only shown for the Info screen tiles, not the on-field draggable column.
        if (showGap) {
            val gap = if (reversed) hoverGapReversed else hoverGap
            val xOffset = if (reversed) 136 else -7
            blitk(
                matrixStack = context.pose(),
                texture = gap,
                x = startX + xOffset,
                y = startY,
                height = 36,
                width = 8,
                textureHeight = 36,
                textureWidth = 8,
            )
        }

        blitk(
            matrixStack = context.pose(),
            texture = if (showBuffs) expandedPokemonInfo else expandedPokemonInfoInactive,
            x = startX,
            y = startY,
            height = 112,
            width = 137,
            textureHeight = 112,
            textureWidth = 137,
        )

        drawScaledText(
            context = context,
            text = "deltaclient.ui.battle.label.pokemon_form".asTranslated().bold(),
            x = startX + 36,
            y = startY + 6.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = true
        )

        drawScaledText(
            context = context,
            text = getFormText(species, form),
            x = startX + 36,
            y = startY + 13.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = true
        )

        if (showBuffs) {
        drawScaledText(
            context = context,
            text = "deltaclient.ui.battle.label.buffs".asTranslated().bold(),
            x = startX + 36,
            y = startY + 23.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = true
        )

        drawScaledText(
            context = context,
            text = "cobblemon.stat.attack.name".asTranslated().statColored(Stats.ATTACK),
            x = startX + 10,
            y = startY + 31.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = false
        )

        dto.buffs[Stats.ATTACK]?.let {
            drawScaledText(
                context = context,
                text = getBoostText(Stats.ATTACK, it),
                x = startX + 44,
                y = startY + 31.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.stat.defence.name".asTranslated().statColored(Stats.DEFENCE),
            x = startX + 10,
            y = startY + 39.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = false
        )

        dto.buffs[Stats.DEFENCE]?.let {
            drawScaledText(
                context = context,
                text = getBoostText(Stats.DEFENCE, it),
                x = startX + 44,
                y = startY + 39.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "deltaclient.ui.battle.stats.special_attack".asTranslated().statColored(Stats.SPECIAL_ATTACK),
            x = startX + 10,
            y = startY + 47.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = false
        )

        dto.buffs[Stats.SPECIAL_ATTACK]?.let {
            drawScaledText(
                context = context,
                text = getBoostText(Stats.SPECIAL_ATTACK, it),
                x = startX + 44,
                y = startY + 47.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "deltaclient.ui.battle.stats.special_defence".asTranslated().statColored(Stats.SPECIAL_DEFENCE),
            x = startX + 10,
            y = startY + 55.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = false
        )

        dto.buffs[Stats.SPECIAL_DEFENCE]?.let {
            drawScaledText(
                context = context,
                text = getBoostText(Stats.SPECIAL_DEFENCE, it),
                x = startX + 44,
                y = startY + 55.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.stat.speed.name".asTranslated().statColored(Stats.SPEED),
            x = startX + 10,
            y = startY + 63.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = false
        )

        dto.buffs[Stats.SPEED]?.let {
            drawScaledText(
                context = context,
                text = getBoostText(Stats.SPEED, it),
                x = startX + 44,
                y = startY + 63.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.stat.accuracy.name".asTranslated(),
            x = startX + 10,
            y = startY + 71.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = false
        )

        dto.buffs[Stats.ACCURACY]?.let {
            drawScaledText(
                context = context,
                text = getBoostText(Stats.ACCURACY, it),
                x = startX + 44,
                y = startY + 71.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = false
            )
        }

        drawScaledText(
            context = context,
            text = "cobblemon.stat.evasion.name".asTranslated(),
            x = startX + 10,
            y = startY + 79.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = false
        )

        dto.buffs[Stats.EVASION]?.let {
            drawScaledText(
                context = context,
                text = getBoostText(Stats.EVASION, it),
                x = startX + 44,
                y = startY + 79.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = false
            )
        }
        } else {
            // Non-active Pokémon have no stat stages; the inactive panel asset already clears the
            // Buffs/Debuffs column, so we just draw a "Current HP" gauge on that clean area.
            drawScaledText(
                context = context,
                text = "deltaclient.ui.battle.label.current_hp".asTranslated().bold(),
                x = startX + 36,
                y = startY + 23.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = true
            )

            // Owner sees flat HP (hp/maxHp); opponents carry a 0..1 ratio.
            val hpRatio = (if (pokemon.isHpFlat) pokemon.hpValue / pokemon.maxHp.toInt() else pokemon.hpValue)
                .coerceIn(0f, 1f)
            val (hpRed, hpGreen) = getDepletableRedGreen(hpRatio)

            // Same battle health-bar texture + tint the active Pokémon uses, sized to fill the asset's
            // bar slot beneath the "Current HP" label (slot ≈ x8..64, y30..36).
            val barX = startX + 8
            val barW = hpRatio * 57f
            blitk(
                matrixStack = context.pose(),
                texture = healthBar,
                x = barX,
                y = startY + 30,
                width = barW,
                height = 6,
                textureWidth = 99,
                textureHeight = 6,
                red = hpRed * 0.8f,
                green = hpGreen * 0.8f,
                blue = 0.27f
            )
            // The texture's right end is angled, so a left-clipped fill has a border on every side except
            // the right. Re-draw the texture's square left-edge column (texel 0) at the fill's right edge
            // to close the border into a full rectangle.
            if (barW >= 1f) {
                blitk(
                    matrixStack = context.pose(),
                    texture = healthBar,
                    x = barX + barW - 1f,
                    y = startY + 30,
                    width = 1f,
                    height = 6,
                    textureWidth = 99,
                    textureHeight = 6,
                    red = hpRed * 0.8f,
                    green = hpGreen * 0.8f,
                    blue = 0.27f
                )
            }

            // Percentage centred inside the bar — and exact values for your own Pokémon — exactly like
            // the active Pokémon's health bar does in battle.
            val hpText = if (pokemon.isHpFlat) {
                "${pokemon.hpValue.toInt()}/${pokemon.maxHp.toInt()} (${ceil(hpRatio * 100).toInt()}%)"
            } else {
                "${ceil(hpRatio * 100).toInt()}%"
            }
            drawScaledText(
                context = context,
                text = hpText.text(),
                x = startX + 36,
                y = startY + 31.0,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = true
            )
        }

        drawScaledText(
            context = context,
            text = "deltaclient.ui.battle.label.ability".asTranslated().bold(),
            x = startX + 100,
            y = startY + 6.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = true
        )

        val ability = dto.ability?.let { Abilities.get(it) }
        val abilityName = ability?.displayName?.text() ?: "?".text()

        drawScaledText(
            context = context,
            text = abilityName,
            x = startX + 100,
            y = startY + 13.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = true
        )

        drawScaledText(
            context = context,
            text = "deltaclient.ui.battle.label.moves".asTranslated().bold(),
            x = startX + 100,
            y = startY + 23.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = true
        )

        if (dto.moves.size != 4) {
            for (i in 0 until 4) {
                drawScaledText(
                    context = context,
                    text = "?".text(),
                    x = startX + 100,
                    y = startY + 31.5 + i * 8,
                    scale = BattleOverlay.SCALE,
                    shadow = true,
                    centered = true
                )
            }
        }
        else {
            val questionMarkText = "?".text()
            dto.moves.forEachIndexed { index, moveDTO ->
                val move = moveDTO?.move ?: questionMarkText
                val moveColor = moveDTO?.move?.let { typeColorFor(it) }
                drawScaledText(
                    context = context,
                    text = move.string.text().let { t -> if (moveColor != null) t.colored(moveColor) else t },
                    x = if (move == questionMarkText) startX + 100 else startX + 74,
                    y = startY + 31.5 + index * 8,
                    scale = BattleOverlay.SCALE,
                    shadow = true,
                    centered = move == questionMarkText
                )

                if (moveDTO != null) {
                    drawScaledTextJustifiedRight(
                        context = context,
                        text = moveDTO.timesUsed.toString().text(),
                        x = startX + 127.5,
                        y = startY + 31.5 + index * 8,
                        scale = BattleOverlay.SCALE,
                        shadow = true,
                    )
                }
            }
        }

        val dtoSpeed = dto.speed
        if (dtoSpeed != null) {
            val speedBoost = dto.buffs[Stats.SPEED] ?: 1.0
            val speedAfterBoost = (dtoSpeed * speedBoost).toInt()
            drawScaledText(
                context = context,
                text = "cobblemon.stat.speed.name".asTranslated(),
                x = startX + 100,
                y = startY + 65.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = true
            )

            drawScaledText(
                context = context,
                text = speedAfterBoost.toString().text(),
                x = startX + 100,
                y = startY + 72.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = true
            )
        }
        else {
            drawScaledText(
                context = context,
                text = "deltaclient.ui.battle.label.speed_tier".asTranslated(),
                x = startX + 100,
                y = startY + 65.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = true
            )

            val speedBoost = dto.buffs[Stats.SPEED] ?: 1.0
            val speedTier = getSpeedRange(form, pokemon.level, speedBoost)

            drawScaledText(
                context = context,
                text = speedTier,
                x = startX + 100,
                y = startY + 72.5,
                scale = BattleOverlay.SCALE,
                shadow = true,
                centered = true
            )
        }

        drawScaledText(
            context = context,
            text = "deltaclient.ui.battle.label.held_item".asTranslated(),
            x = startX + 40,
            y = startY + 99.5,
            scale = BattleOverlay.SCALE,
            shadow = true,
            centered = true
        )

        TypeIcon(
            x = startX + 100.5,
            y = startY + 88,
            type = form.primaryType,
            secondaryType = form.secondaryType,
            small = false,
            centeredX = true
        ).render(context)

        if (form.secondaryType != null) {
            blitk(
                matrixStack = context.pose(),
                texture = cobblemonResource("textures/gui/battle/type_spacer_double.png"),
                x = (startX + 69.0) / 0.45,
                y = (startY + 91.5) / 0.45,
                height = 24,
                width = 140,
                textureHeight = 24,
                textureWidth = 140,
                scale = 0.45f
            )
        }
        else {
            blitk(
                matrixStack = context.pose(),
                texture = cobblemonResource("textures/gui/summary/type_spacer.png"),
                x = (startX + 70.5) / 0.45,
                y = (startY + 91.5) / 0.45,
                height = 24,
                width = 132,
                textureHeight = 24,
                textureWidth = 132,
                scale = 0.45f
            )
        }

        val heldItem = dto.heldItem
        if (heldItem != null && !heldItem.isEmpty()) {
            context.pose().pushPose()
            context.pose().scale(0.95F, 0.95F, 0.95F)
            val itemX = (startX + 4) / 0.95
            val itemY = (startY + 91) / 0.95
            context.renderItem(heldItem, itemX.toInt(), itemY.toInt())
            context.pose().popPose()
        }
        else {
            blitk(
                matrixStack = context.pose(),
                texture = BattleOverlay.questionMarkIcon,
                x = startX + 6,
                y = startY + 92.5,
                height = 11,
                width = 10,
            )
        }
        context.pose().popPose()
    }

    private fun getSpeedRange(form: FormData, level: Int, speedBoost: Double): MutableComponent {
        val base = form.baseStats[Stats.SPEED] ?: 0
        val maxIV = 31
        val maxEV = 252
        val minNatureMod = 0.9
        val maxNatureMod = 1.1

        val minSpeed = getStat(base, level, 0, 0, minNatureMod) * speedBoost
        val maxSpeed = getStat(base, level, maxIV, maxEV, maxNatureMod) * speedBoost
        return "deltaclient.ui.battle.speed_tier".asTranslated(minSpeed.toInt(), maxSpeed.toInt())
    }

    private fun getStat(base: Int, level: Int, iv: Int, ev: Int, natureMod: Double): Int {
        return floor((floor(((2.0 * base + iv + floor(ev / 4.0)) * level) / 100) + 5) * natureMod).toInt()
    }

    internal fun getBoostText(stat: Stats, multiplier: Double): MutableComponent {
        val stage = stageFromMultiplier(stat, multiplier)
        val sign = if (stage > 0) "+" else ""
        val str = "$sign$stage (${multiplierFormat.format(multiplier)}x)"
        return when {
            multiplier > 1.0 -> str.green()
            multiplier < 1.0 -> str.red()
            else -> str.text()
        }
    }

    // Converts a stat-boost multiplier back to its stage. Accuracy/Evasion use a /3 base, others /2.
    private fun stageFromMultiplier(stat: Stats, multiplier: Double): Int {
        val base = if (stat == Stats.ACCURACY || stat == Stats.EVASION) 3.0 else 2.0
        return if (multiplier >= 1.0) Math.round(multiplier * base - base).toInt()
        else Math.round(base - base / multiplier).toInt()
    }

    // The type colour for a move from its "cobblemon.move.<name>" display name, or null if unknown.
    internal fun typeColorFor(move: Component): Int? {
        val key = (move.contents as? TranslatableContents)?.key ?: return null
        if (!key.startsWith("cobblemon.move.")) return null
        val type = Moves.getByName(key.removePrefix("cobblemon.move."))?.elementalType?.name ?: return null
        return typeColor(type)
    }

    private fun typeColor(type: String): Int = when (type.lowercase()) {
        "bug" -> 0xA8B820
        "dark" -> 0x705848
        "dragon" -> 0x7038F8
        "electric" -> 0xF8D030
        "fairy" -> 0xEE99AC
        "fighting" -> 0xC03028
        "fire" -> 0xF08030
        "flying" -> 0xA890F0
        "ghost" -> 0x705898
        "grass" -> 0x78C850
        "ground" -> 0xE0C068
        "ice" -> 0x98D8D8
        "normal" -> 0xA8A878
        "poison" -> 0xA040A0
        "psychic" -> 0xF85888
        "rock" -> 0xB8A038
        "steel" -> 0xB8B8D0
        "water" -> 0x6890F0
        else -> 0xFFFFFF
    }

    private fun getFormText(species: Species, form: FormData): MutableComponent {
        val speciesName = species.name
        val formName = form.name
        return if (formName == "Normal") speciesName.text() else "$speciesName-$formName".text()
    }
}


internal fun MutableComponent.colored(rgb: Int): MutableComponent = this.withStyle { it.withColor(TextColor.fromRgb(rgb)) }

// Buffs/Debuffs stat names are all white. (Per-stat colours can be re-enabled by returning them here.)
internal fun statColor(stat: Stats): Int? = null

internal fun MutableComponent.statColored(stat: Stats): MutableComponent {
    val rgb = statColor(stat) ?: return this
    return this.colored(rgb)
}

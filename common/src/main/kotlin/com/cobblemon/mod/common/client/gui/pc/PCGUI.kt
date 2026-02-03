/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pc

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.pokemon.PokemonSortMode
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.storage.pc.search.Search
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.italicise
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import com.cobblemon.mod.common.client.gui.ExitButton
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.gui.summary.Summary
import com.cobblemon.mod.common.client.gui.summary.Summary.Companion.iconCosmeticItemResource
import com.cobblemon.mod.common.client.gui.summary.Summary.Companion.iconHeldItemResource
import com.cobblemon.mod.common.client.gui.summary.SummaryButton
import com.cobblemon.mod.common.client.gui.summary.widgets.MarkingsWidget
import com.cobblemon.mod.common.client.gui.summary.widgets.ModelWidget
import com.cobblemon.mod.common.client.gui.summary.widgets.common.reformatNatureTextIfMinted
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.storage.ClientPC
import com.cobblemon.mod.common.client.storage.ClientParty
import com.cobblemon.mod.common.net.messages.server.storage.pc.MarkPCBoxWallpapersSeenPacket
import com.cobblemon.mod.common.net.messages.server.storage.pc.SortPCBoxPacket
import com.cobblemon.mod.common.net.messages.server.storage.pc.UnlinkPlayerFromPCPacket
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.isInventoryKeyPressed
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.toAssetPath
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import kotlin.isInitialized
import net.minecraft.resources.ResourceLocation

class PCGUI(
    val pc: ClientPC,
    val party: ClientParty,
    val configuration: PCGUIConfiguration,
    val openOnBox: Int = CobblemonClient.lastPcBoxViewed,
    val unseenWallpapers: MutableSet<ResourceLocation> = mutableSetOf()
) : Screen(Component.translatable("cobblemon.ui.pc.title")), CobblemonRenderable {

    companion object {
        const val BASE_WIDTH = 349
        const val BASE_HEIGHT = 205
        const val INFO_BOX_WIDTH = 63
        const val INFO_BOX_HEIGHT = 69
        const val RIGHT_PANEL_WIDTH = 82
        const val RIGHT_PANEL_HEIGHT = 169
        const val TYPE_SPACER_WIDTH = 126
        const val TYPE_SPACER_HEIGHT = 12
        const val PC_SPACER_WIDTH = 342
        const val PC_SPACER_HEIGHT = 14
        const val PORTRAIT_SIZE = 66
        const val SCALE = 0.5F

        const val STAT_INFO = 0
        const val STAT_IV = 1
        const val STAT_EV = 2

        private val statLabels = arrayOf(
            lang("ui.stats.hp"),
            lang("ui.stats.atk"),
            lang("ui.stats.def"),
            lang("ui.stats.sp_atk"),
            lang("ui.stats.sp_def"),
            lang("ui.stats.speed")
        )

        private val stats = arrayOf(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED)

        private val baseResource = cobblemonResource("textures/gui/pc/pc_base.png")
        private val portraitBackgroundResource = cobblemonResource("textures/gui/pc/portrait_background.png")
        private val infoBoxResource = cobblemonResource("textures/gui/pc/info_box.png")
        private val infoBoxStatResource = cobblemonResource("textures/gui/pc/info_box_stats.png")

        private val buttonOptionsResource = cobblemonResource("textures/gui/pc/pc_icon_options.png")
        private val buttonWallpaperResource = cobblemonResource("textures/gui/pc/pc_button_set_wallpaper.png")
        private val buttonInfoArrow = cobblemonResource("textures/gui/pc/info_arrow.png")

        private val topSpacerResource = cobblemonResource("textures/gui/pc/pc_spacer_top.png")
        private val bottomSpacerResource = cobblemonResource("textures/gui/pc/pc_spacer_bottom.png")
        private val rightSpacerResource = cobblemonResource("textures/gui/pc/pc_spacer_right.png")
        private val typeSpacerResource = cobblemonResource("textures/gui/pc/type_spacer.png")
        private val typeSpacerSingleResource = cobblemonResource("textures/gui/pc/type_spacer_single.png")
        private val typeSpacerDoubleResource = cobblemonResource("textures/gui/pc/type_spacer_double.png")
    }

    private lateinit var storageWidget: StorageWidget
    val storage: StorageWidget
        get() = storageWidget

    private lateinit var boxNameWidget: BoxNameWidget
    private lateinit var filterWidget: FilterWidget
    private lateinit var wallpaperWidget: WallpapersScrollingWidget
    private lateinit var markingsWidget: MarkingsWidget
    private var modelWidget: ModelWidget? = null
    internal var previewPokemon: Pokemon? = null
    var isPreviewInParty: Boolean? = null

    private var showCosmeticItem = false
    private var currentStatIndex = 0

    private val optionButtons: MutableList<IconButton> = mutableListOf()

    var search: Search = Search.DEFAULT
    var ticksElapsed = 0
    var selectPointerOffsetY = 0
    var selectPointerOffsetIncrement = false
    var displayOptions = false

    override fun renderBlurredBackground(delta: Float) {}

    override fun renderMenuBackground(context: GuiGraphics) {}

    override fun init() {
        val x = (width - BASE_WIDTH) / 2
        val y = (height - BASE_HEIGHT) / 2

        // Add Exit Button
        this.addRenderableWidget(
            ExitButton(pX = x + 320, pY = y + 186) {
                if (::wallpaperWidget.isInitialized && wallpaperWidget.visible) {
                    wallpaperWidget.visible = false
                    configuration.showParty = true
                    for (button in optionButtons) button.highlighted = false
                    playSound(CobblemonSounds.PC_CLICK)
                } else {
                    saveMarkings(isPreviewInParty ?: false)
                    configuration.exitFunction(this)
                }
            }
        )

        // Add Forward Button
        this.addRenderableWidget(
            NavigationButton(
                pX = x + 220,
                pY = y + 16,
                forward = true
            ) { storageWidget.box += 1 }
        )

        // Add Backwards Button
        this.addRenderableWidget(
            NavigationButton(
                pX = x + 117,
                pY = y + 16,
                forward = false
            ) { storageWidget.box -= 1 }
        )

        // Add Filter Widget
        this.filterWidget = FilterWidget(
            pX = x + 126,
            pY = y + 183,
            update = { search = Search.of(filterWidget.value) }
        )
        this.addRenderableWidget(filterWidget)

        // Add Storage
        this.storageWidget = StorageWidget(
            pX = x + 85,
            pY = y + 27,
            pcGui = this,
            pc = pc,
            party = party
        )
        this.storageWidget.box = if (openOnBox < pc.boxes.size) openOnBox else 0
        this.addRenderableWidget(storageWidget)

        // Add Box Name
        this.boxNameWidget = BoxNameWidget(
            pX = x + 126,
            pY = y + 12,
            pcGui = this,
            storageWidget = storageWidget
        )
        this.addRenderableWidget(boxNameWidget)

        // Initialise box options if not pasture
        if (storageWidget.pastureWidget == null) {
            optionButtons.clear()
            addRenderableWidget(
                IconButton(
                    pX = x + 218,
                    pY = y + 186,
                    buttonWidth = 16,
                    buttonHeight = 16,
                    resource = buttonOptionsResource,
                    label = "options"
                ) {
                    displayOptions = !displayOptions
                    if (!displayOptions && wallpaperWidget.visible) {
                        configuration.showParty = true
                        wallpaperWidget.visible = false
                    }
                    (it as IconButton).highlighted = displayOptions
                    storageWidget.setupStorageSlots()
                    for (button in optionButtons) {
                        button.visible = displayOptions
                        button.highlighted = false
                    }
                }.also { it.highlighted = displayOptions }
            )

            // Add Wallpaper Widget
            this.wallpaperWidget = WallpapersScrollingWidget(
                pX = x + 274,
                pY = y + 29,
                pcGui = this,
                storageWidget = storageWidget
            ).also {
                // Set default component visibility
                it.visible = false
                configuration.showParty = true
            }
            this.addRenderableWidget(wallpaperWidget)

            // Add Wallpaper Settings Button
            this.addRenderableWidget(IconButton(
                pX = x + 242,
                pY = y + 31,
                buttonWidth = 20,
                buttonHeight = 20,
                resource = buttonWallpaperResource,
                label = "open_wallpaper_settings"
            ) {
                val isVisible = wallpaperWidget.visible
                configuration.showParty = isVisible
                wallpaperWidget.visible = !isVisible
                (it as IconButton).highlighted = !isVisible
                if (!unseenWallpapers.isEmpty()) MarkPCBoxWallpapersSeenPacket(unseenWallpapers).sendToServer()
            }.also {
                it.visible = displayOptions
                optionButtons.add(it)
            })

            PokemonSortMode.entries.forEachIndexed { index, sortType ->
                val typeName = sortType.name.lowercase()
                this.addRenderableWidget(IconButton(
                    pX = x + 92 + (12 * index),
                    pY = y + 31,
                    buttonWidth = 20,
                    buttonHeight = 20,
                    resource = cobblemonResource("textures/gui/pc/pc_button_sort_${typeName}.png"),
                    altResource = cobblemonResource("textures/gui/pc/pc_button_sort_${typeName}_reverse.png"),
                    tooltipKey = "ui.sort.${typeName}",
                    label = "sort_${typeName}"
                ) {
                    storageWidget.resetSelected()
                    SortPCBoxPacket(pc.uuid, storageWidget.box, sortType, hasShiftDown()).sendToServer()
                }.also {
                    it.visible = displayOptions
                    optionButtons.add(it)
                })
            }
        }

        this.markingsWidget = MarkingsWidget(x + 29, y + 96.5, null, false)
        this.addRenderableWidget(markingsWidget)

        // Held/Cosmetic Item Button
        addRenderableWidget(
            SummaryButton(
                buttonX = x + 67F,
                buttonY = y + 107F,
                buttonWidth = 12,
                buttonHeight = 12,
                scale = 0.5F,
                resource = iconCosmeticItemResource,
                activeResource = iconHeldItemResource,
                clickAction = {
                    showCosmeticItem = !showCosmeticItem
                    (it as SummaryButton).buttonActive = showCosmeticItem
                }
            )
        )

        addRenderableWidget(
            IconButton(
                pX = x + 1,
                pY = y + 157,
                buttonWidth = 10,
                buttonHeight = 16,
                resource = buttonInfoArrow,
                label = "switch"
            ) {
                currentStatIndex = (currentStatIndex + 1) % 3
            }
        )

        this.setPreviewPokemon(null)

        super.init()
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val matrices = context.pose()
        renderBackground(context, mouseX, mouseY, delta)

        val x = (width - BASE_WIDTH) / 2
        val y = (height - BASE_HEIGHT) / 2

        // Render Portrait Background
        blitk(
            matrixStack = matrices,
            texture = portraitBackgroundResource,
            x = x + 6,
            y = y + 27,
            width = PORTRAIT_SIZE,
            height = PORTRAIT_SIZE
        )

        // Render Model Portrait
        if (search.passes(previewPokemon)) {
            modelWidget?.render(context, mouseX, mouseY, delta)
        }

        // Render Base Resource
        blitk(
            matrixStack = matrices,
            texture = baseResource,
            x = x, y = y,
            width = BASE_WIDTH,
            height = BASE_HEIGHT
        )

        // Render Info Box
        blitk(
            matrixStack = matrices,
            texture = if (currentStatIndex > 0) infoBoxStatResource else infoBoxResource,
            x = x + 9,
            y = y + 128,
            width = INFO_BOX_WIDTH,
            height = INFO_BOX_HEIGHT
        )

        val labelX = x + 9 + (INFO_BOX_WIDTH / 2)
        // Render Info Labels
        when (currentStatIndex) {
            STAT_INFO -> {
                drawScaledText(
                    context = context,
                    text = lang("ui.info.nature").bold(),
                    x = labelX,
                    y = y + 129.5,
                    centered = true,
                    scale = SCALE
                )

                drawScaledText(
                    context = context,
                    text = lang("ui.info.ability").bold(),
                    x = labelX,
                    y = y + 146.5,
                    centered = true,
                    scale = SCALE
                )

                drawScaledText(
                    context = context,
                    text = lang("ui.moves").bold(),
                    x = labelX,
                    y = y + 163.5,
                    centered = true,
                    scale = SCALE
                )
            }
            STAT_IV -> {
                drawScaledText(
                    context = context,
                    text = lang("ui.stats.ivs").bold(),
                    x = labelX,
                    y = y + 129.5,
                    centered = true,
                    scale = SCALE
                )
            }
            STAT_EV -> {
                drawScaledText(
                    context = context,
                    text = lang("ui.stats.evs").bold(),
                    x = labelX,
                    y = y + 129.5,
                    centered = true,
                    scale = SCALE
                )
            }
        }

        // Render Pokemon Info
        val pokemon = previewPokemon
        if (pokemon != null && search.passes(pokemon)) {
            // Status
            val status = pokemon.status?.status
            if (pokemon.isFainted() || status != null) {
                val statusName = if (pokemon.isFainted()) "fnt" else status?.showdownName
                blitk(
                    matrixStack = matrices,
                    texture = cobblemonResource("textures/gui/battle/battle_status_$statusName.png"),
                    x = x + 34,
                    y = y + 1,
                    height = 7,
                    width = 39,
                    uOffset = 35,
                    textureWidth = 74
                )

                blitk(
                    matrixStack = matrices,
                    texture = cobblemonResource("textures/gui/summary/status_trim.png"),
                    x = x + 34,
                    y = y + 2,
                    height = 6,
                    width = 3
                )

                drawScaledText(
                    context = context,
                    font = CobblemonResources.DEFAULT_LARGE,
                    text = lang("ui.status.$statusName").bold(),
                    x = x + 39,
                    y = y
                )
            }

            // Level
            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = lang("ui.lv").bold(),
                x = x + 6,
                y = y + 1.5,
                shadow = true
            )

            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = pokemon.level.toString().text().bold(),
                x = x + 19,
                y = y + 1.5,
                shadow = true
            )

            // Poké Ball
            val ballResource = pokemon.caughtBall.name.toAssetPath("textures/item/poke_balls/", ".png")
            blitk(
                matrixStack = matrices,
                texture = ballResource,
                x = (x + 3.5) / SCALE,
                y = (y + 12) / SCALE,
                width = 16,
                height = 16,
                scale = SCALE
            )

            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = pokemon.getDisplayName().bold(),
                x = x + 12,
                y = y + 11.5,
                shadow = true
            )

            if (pokemon.gender != Gender.GENDERLESS) {
                val isMale = pokemon.gender == Gender.MALE
                val textSymbol = if (isMale) "♂".text().bold() else "♀".text().bold()
                drawScaledText(
                    context = context,
                    font = CobblemonResources.DEFAULT_LARGE,
                    text = textSymbol,
                    x = x + 69, // 64 when tag icon is implemented
                    y = y + 11.5,
                    colour = if (isMale) 0x32CBFF else 0xFC5454,
                    shadow = true
                )
            }

            // Held/Cosmetic Item
            val displayedItem = if (showCosmeticItem) pokemon.cosmeticItem else pokemon.heldItemNoCopy()
            val itemX = x + 3
            val itemY = y + 98
            if (!displayedItem.isEmpty) {
                context.renderItem(displayedItem, itemX, itemY)
                context.renderItemDecorations(Minecraft.getInstance().font, displayedItem, itemX, itemY)
            }

            drawScaledText(
                context = context,
                text = lang("${if (showCosmeticItem) "cosmetic" else "held"}_item"),
                x = x + 24,
                y = y + 108.5,
                scale = SCALE
            )

            // Shiny Icon
            if (pokemon.shiny) {
                blitk(
                    matrixStack = matrices,
                    texture = Summary.iconShinyResource,
                    x = (x + 62.5) / SCALE,
                    y = (y + 28.5) / SCALE,
                    width = 16,
                    height = 16,
                    scale = SCALE
                )
            }

            blitk(
                matrixStack = matrices,
                texture = if (pokemon.secondaryType != null) typeSpacerDoubleResource else typeSpacerSingleResource,
                x = (x + 9) / SCALE,
                y = (y + 118.5) / SCALE,
                width = TYPE_SPACER_WIDTH,
                height = TYPE_SPACER_HEIGHT,
                scale = SCALE
            )

            TypeIcon(
                x = x + 40.5,
                y = y + 117,
                type = pokemon.primaryType,
                secondaryType = pokemon.secondaryType,
                doubleCenteredOffset = 5F,
                secondaryOffset = 10F,
                small = true,
                centeredX = true
            ).render(context)

            when (currentStatIndex) {
                STAT_INFO -> {
                    // Nature
                    val natureText = reformatNatureTextIfMinted(pokemon)
                    drawScaledText(
                        context = context,
                        text = natureText,
                        x = labelX,
                        y = y + 137,
                        centered = true,
                        shadow = true,
                        scale = SCALE,
                        pMouseX = mouseX,
                        pMouseY = mouseY
                    )

                    // Ability
                    drawScaledText(
                        context = context,
                        text = pokemon.ability.displayName.asTranslated(),
                        x = labelX,
                        y = y + 154,
                        centered = true,
                        shadow = true,
                        scale = SCALE
                    )

                    // Moves
                    val moveList = pokemon.moveSet.getMoves()
                        .take(4)
                        .map { it.displayName }
                        .plus(List(4 - pokemon.moveSet.getMoves().size) { "—".text() })

                    for (i in moveList.indices) {
                        drawScaledText(
                            context = context,
                            text = moveList[i],
                            x = labelX,
                            y = y + 170.5 + (7 * i),
                            centered = true,
                            shadow = true,
                            scale = SCALE
                        )
                    }
                }
                STAT_IV -> {
                    for (i in statLabels.indices) {
                        drawScaledText(
                            context = context,
                            text = statLabels[i],
                            x = x + 13,
                            y = y + 139 + (10 * i),
                            shadow = true,
                            scale = SCALE
                        )

                        val iv = pokemon.ivs.get(stats[i])
                        val effectiveIv = pokemon.ivs.getEffectiveBattleIV(stats[i])
                        drawScaledText(
                            context = context,
                            text = if (iv != effectiveIv) effectiveIv.toString().text().italicise() else iv.toString().text(),
                            x = x + 65,
                            y = y + 139 + (10 * i),
                            centered = true,
                            shadow = true,
                            scale = SCALE
                        )
                    }
                }
                STAT_EV -> {
                    for (i in statLabels.indices) {
                        drawScaledText(
                            context = context,
                            text = statLabels[i],
                            x = x + 13,
                            y = y + 139 + (10 * i),
                            shadow = true,
                            scale = SCALE
                        )

                        drawScaledText(
                            context = context,
                            text = pokemon.evs.get(stats[i]).toString().text(),
                            x = x + 65,
                            y = y + 139 + (10 * i),
                            centered = true,
                            shadow = true,
                            scale = SCALE
                        )
                    }
                }
            }

        } else {
            blitk(
                matrixStack = matrices,
                texture = typeSpacerResource,
                x = (x + 7) / SCALE,
                y = (y + 118.5) / SCALE,
                width = TYPE_SPACER_WIDTH,
                height = TYPE_SPACER_HEIGHT,
                scale = SCALE
            )
        }

        blitk(
            matrixStack = matrices,
            texture = topSpacerResource,
            x = (x + 86.5) / SCALE,
            y = (y + 13) / SCALE,
            width = PC_SPACER_WIDTH,
            height = PC_SPACER_HEIGHT,
            scale = SCALE
        )

        blitk(
            matrixStack = matrices,
            texture = bottomSpacerResource,
            x = (x + 86.5) / SCALE,
            y = (y + 189) / SCALE,
            width = PC_SPACER_WIDTH,
            height = PC_SPACER_HEIGHT,
            scale = SCALE
        )

        blitk(
            matrixStack = matrices,
            texture = rightSpacerResource,
            x = (x + 275.5) / SCALE,
            y = (y + 184) / SCALE,
            width = 64,
            height = 24,
            scale = SCALE
        )

        if (!optionButtons.isEmpty() && displayOptions) {
            for (button in optionButtons) button.showAlt = hasShiftDown()
        }

        super.render(context, mouseX, mouseY, delta)

        // Item Tooltip
        if (pokemon != null) {
            val displayedItem = if (showCosmeticItem) pokemon.cosmeticItem else pokemon.heldItemNoCopy()
            val itemX = x + 3
            val itemY = y + 98
            if (!displayedItem.isEmpty) {
                val itemHovered = mouseX.toFloat() in (itemX.toFloat()..(itemX.toFloat() + 16)) && mouseY.toFloat() in (itemY.toFloat()..(itemY.toFloat() + 16))
                if (itemHovered) context.renderTooltip(Minecraft.getInstance().font, displayedItem, mouseX, mouseY)
            }
        }
    }

    fun closeNormally(unlink: Boolean = true) {
        playSound(CobblemonSounds.PC_OFF)
        Minecraft.getInstance().setScreen(null)
        if (unlink) {
            UnlinkPlayerFromPCPacket().sendToServer()
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Trigger child component function as they will need to check the entire screen click area
        if (::boxNameWidget.isInitialized) boxNameWidget.mouseClicked(mouseX, mouseY, button)
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double, verticalAmount: Double): Boolean {
        storageWidget.pastureWidget?.let { pasture ->
            if (pasture.pastureScrollList.isHovered) pasture.pastureScrollList.mouseScrolled(mouseX, mouseY, amount, verticalAmount)
        }
        if (storageWidget.isHovered && mouseX < (storageWidget.x + StorageWidget.SCREEN_WIDTH)) this.storageWidget.box -= verticalAmount.toInt()
        if (::wallpaperWidget.isInitialized && wallpaperWidget.isHovered) wallpaperWidget.mouseScrolled(mouseX, mouseY, amount, verticalAmount)

        val infoBoxX = ((width - BASE_WIDTH) / 2) + 9
        val infoBoxY = ((height - BASE_HEIGHT) / 2) + 128

        if (mouseX in infoBoxX.toDouble()..(infoBoxX + INFO_BOX_WIDTH).toDouble()
            && mouseY in infoBoxY.toDouble()..(infoBoxY + INFO_BOX_HEIGHT).toDouble()
        ) {
            currentStatIndex = (currentStatIndex + verticalAmount.toInt() + 3) % 3
        }

        return super.mouseScrolled(mouseX, mouseY, amount, verticalAmount)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        storageWidget.pastureWidget?.let { pasture ->
            if (pasture.pastureScrollList.isHovered) pasture.pastureScrollList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val boxNameFocused = this::boxNameWidget.isInitialized && boxNameWidget.isFocused
        val filterFocused = this::filterWidget.isInitialized && filterWidget.isFocused

        if (isInventoryKeyPressed(minecraft, keyCode, scanCode) && !boxNameFocused && !filterFocused) {
            saveMarkings(isPreviewInParty ?: false)
            playSound(CobblemonSounds.PC_OFF)
            UnlinkPlayerFromPCPacket().sendToServer()
            Minecraft.getInstance().setScreen(null)
            return true
        }

        if (keyCode == InputConstants.KEY_ESCAPE) saveMarkings(isPreviewInParty ?: false)

        if (!filterFocused && !boxNameFocused) {
            when (keyCode) {
                InputConstants.KEY_ESCAPE -> {
                    playSound(CobblemonSounds.PC_OFF)
                    UnlinkPlayerFromPCPacket().sendToServer()
                    onClose()
                    return true
                }
                InputConstants.KEY_RIGHT -> {
                    playSound(CobblemonSounds.PC_CLICK)
                    this.storageWidget.box += 1
                    return true
                }
                InputConstants.KEY_LEFT -> {
                    playSound(CobblemonSounds.PC_CLICK)
                    this.storageWidget.box -= 1
                    return true
                }
            }
        } else if (keyCode == InputConstants.KEY_ESCAPE) {
            // Escape from text box
            if (::boxNameWidget.isInitialized) boxNameWidget.keyPressed(keyCode, scanCode, modifiers)
            this.focused = null
            return true
        }

        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    /**
     * Whether this Screen should pause the Game in SinglePlayer
     */
    override fun isPauseScreen() = false

    override fun tick() {
        ticksElapsed++

        // Calculate select pointer offset
        val delayFactor = 3
        if (ticksElapsed % (2 * delayFactor) == 0) selectPointerOffsetIncrement = !selectPointerOffsetIncrement
        if (ticksElapsed % delayFactor == 0) selectPointerOffsetY += if (selectPointerOffsetIncrement) 1 else -1
    }

    fun playSound(soundEvent: SoundEvent) {
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(soundEvent, 1.0F))
    }

    private fun saveMarkings(isParty: Boolean = false) {
        if (::markingsWidget.isInitialized) markingsWidget.saveMarkingsToPokemon(isParty)
    }

    fun setPreviewPokemon(pokemon: Pokemon?, isParty: Boolean = false) {
        if (pokemon != null) {
            saveMarkings(isParty)
            previewPokemon = pokemon

            val x = (width - BASE_WIDTH) / 2
            val y = (height - BASE_HEIGHT) / 2
            modelWidget = ModelWidget(
                pX = x + 6,
                pY = y + 27,
                pWidth = PORTRAIT_SIZE,
                pHeight = PORTRAIT_SIZE,
                pokemon = pokemon.asRenderablePokemon(),
                baseScale = 2F,
                rotationY = 325F,
                offsetY = -10.0
            )
            markingsWidget.setActivePokemon(previewPokemon)
        } else {
            previewPokemon = null
            modelWidget = null
            markingsWidget.setActivePokemon(null)
        }
    }
}
/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.keybind.keybinds

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonNetwork.sendToServer
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.ClientBattle
import com.cobblemon.mod.common.client.gui.battle.BattleGUI
import com.cobblemon.mod.common.client.keybind.CobblemonBlockingKeyBinding
import com.cobblemon.mod.common.client.keybind.KeybindCategories
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket
import com.cobblemon.mod.common.net.messages.server.RequestPlayerInteractionsPacket
import com.cobblemon.mod.common.net.messages.server.SendOutPokemonPacket
import com.cobblemon.mod.common.net.messages.server.riding.DismountPokemonPacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.isUsingPokedex
import com.cobblemon.mod.common.util.traceFirstEntityCollision
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import kotlin.math.pow

object PartySendBinding : CobblemonBlockingKeyBinding(
    "key.cobblemon.throwpartypokemon",
    InputConstants.Type.KEYSYM,
    InputConstants.KEY_R,
    KeybindCategories.COBBLEMON_CATEGORY
) {
    var canApplyChange = true
    var heldDownSeconds = 0F

    fun actioned() {
        canApplyChange = false
        wasDown = true
        heldDownSeconds = 0F
    }

    fun canAction() = canApplyChange && Minecraft.getInstance().player?.isUsingPokedex() == false

    override fun onTick() {
        if (wasDown) {
            if (heldDownSeconds < 100) {
                heldDownSeconds += Minecraft.getInstance().timer.getGameTimeDeltaPartialTick(false)
            }
        } else {
            heldDownSeconds = 0F
        }

        super.onTick()
    }

    override fun onRelease() {
        val canAction = canAction()
        wasDown = false
        canApplyChange = true
        if (!canAction) {
            return
        }
        val player = Minecraft.getInstance().player ?: return
        if (player.isSpectator) return

        val battle = CobblemonClient.battle
        if (battle != null) {
            toggleBattleScreen(battle)
            return
        }

        if (Minecraft.getInstance().screen != null) return

        val selectedPartyPokemon = if (CobblemonClient.storage.selectedSlot >= 0) {
            CobblemonClient.storage.party.get(CobblemonClient.storage.selectedSlot)
        } else {
            null
        }

        if (isRidingPokemon(player) && canAttemptDismount(player, selectedPartyPokemon)) {
            sendToServer(DismountPokemonPacket())
        } else if (selectedPartyPokemon != null && !isRidingSelectedPokemon(player, selectedPartyPokemon)){
            checkForTargetInteractions(player, selectedPartyPokemon)
        }
    }

    private fun toggleBattleScreen(battle: ClientBattle) {
        battle.minimised = !battle.minimised
        if (!battle.minimised && !Minecraft.getInstance().options.hideGui) {
            Minecraft.getInstance().setScreen(BattleGUI())
        }
    }

    private fun checkForTargetInteractions(player: LocalPlayer, selectedPartyPokemon: Pokemon) {
        val targetEntity = player.traceFirstEntityCollision(
            entityClass = LivingEntity::class.java,
            ignoreEntity = player,
            maxDistance = Cobblemon.config.battleSpectateMaxDistance,
            collideBlock = ClipContext.Fluid.NONE)
        if (canSendOutPokemon(player, targetEntity)) {
            sendToServer(SendOutPokemonPacket(CobblemonClient.storage.selectedSlot))
        } else {
            processEntityTarget(player, selectedPartyPokemon, targetEntity)
        }
    }

    private fun canSendOutPokemon(player: LocalPlayer, target: LivingEntity?): Boolean {
        if (isRidingPokemon(player, ignoreControlling = true)) return false
        return target == null || (target is PokemonEntity && target.ownerUUID == player.uuid)
    }

    private fun processEntityTarget(player: LocalPlayer, pokemon: Pokemon, entity: LivingEntity?) {
        if (entity == null) return
        if (!canProcessEntityTarget(player, entity)) return
        when (entity) {
            is Player -> {
                //This sends a packet to the server with the id of the player
                //The server sends a packet back that opens the player interaction menu with the proper options
                sendToServer(RequestPlayerInteractionsPacket(entity.uuid, entity.id, pokemon.uuid))
            }
            is PokemonEntity -> {
                if (!entity.canBattle(player) || entity.position().distanceToSqr(player.position()) > Cobblemon.config.battleWildMaxDistance.pow(2)) return
                    sendToServer(BattleChallengePacket(entity.id,  pokemon.uuid, BattleFormat.GEN_9_SINGLES))
                }
        }
    }

    private fun canProcessEntityTarget(player: LocalPlayer, target: LivingEntity): Boolean {
        return when (target) {
            is Player -> !isRidingPokemon(player)
            is PokemonEntity -> !isRidingPokemon(player, ignoreControlling = true)
            else -> true
        }
    }

    private fun canAttemptDismount(player: LocalPlayer, selectedPartyPokemon: Pokemon?): Boolean {
        val vehicle = player.vehicle as? PokemonEntity ?: return false

        if (player != vehicle.controllingPassenger) return true

        val isAirRide = vehicle.ridingController?.context?.style == RidingStyle.AIR
        val hasLandRide = vehicle.rideProp.behaviours?.get(RidingStyle.LAND) != null

        if (!Cobblemon.config.enableInFlightDismounting && isAirRide && hasLandRide) {
            return false
        }

        return vehicle.pokemon.uuid == selectedPartyPokemon?.uuid
    }

    private fun isRidingPokemon(player: LocalPlayer, ignoreControlling: Boolean = false): Boolean {
        if (!player.isPassenger) return false
        if (player.vehicle !is PokemonEntity) return false
        if (ignoreControlling && player.vehicle!!.controllingPassenger == player) return false
        return true
    }

    private fun isRidingSelectedPokemon(player: LocalPlayer, selectedPartyPokemon: Pokemon, ignoreControlling: Boolean = false): Boolean {
        if (!player.isPassenger) return false
        if (player.vehicle !is PokemonEntity) return false
        val vehicle = player.vehicle as PokemonEntity
        if (ignoreControlling && player.vehicle!!.controllingPassenger == player) return false
        return vehicle.pokemon.uuid == selectedPartyPokemon.uuid
    }

    override fun onPress() {
    }
}
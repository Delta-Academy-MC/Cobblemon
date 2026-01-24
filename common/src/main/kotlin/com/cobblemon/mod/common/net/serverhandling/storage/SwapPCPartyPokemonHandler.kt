/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.serverhandling.storage

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.storage.PCEvent
import com.cobblemon.mod.common.api.events.storage.SwapPCPartyPokemon
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.api.storage.pc.link.PCLinkManager
import com.cobblemon.mod.common.net.messages.client.storage.pc.ClosePCPacket
import com.cobblemon.mod.common.net.messages.server.storage.SwapPCPartyPokemonPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object SwapPCPartyPokemonHandler : ServerNetworkPacketHandler<SwapPCPartyPokemonPacket> {
    override fun handle(packet: SwapPCPartyPokemonPacket, server: MinecraftServer, player: ServerPlayer) {
        val event = SwapPCPartyPokemon(player)
        CobblemonEvents.SWAP_PC_PARTY_POKEMON.emit(event)
        if (event.isCanceled) {
            ClosePCPacket(null).sendToPlayer(player)
            return
        }

        val party = Cobblemon.storage.getParty(player)
        val pc = PCLinkManager.getPC(player) ?: return run { ClosePCPacket(null).sendToPlayer(player) }
        val partyPokemon = party[packet.partyPosition] ?: return
        val pcPokemon = pc[packet.pcPosition] ?: return

        CobblemonEvents.PC_EVENT.postThen(
            event = PCEvent(player),
            ifSucceeded = {
            },
            ifCanceled = {
                ClosePCPacket(null).sendToPlayer(player)
                return
            }
        )

        if (partyPokemon.uuid != packet.partyPokemonID || pcPokemon.uuid != packet.pcPokemonID) {
            return
        }
        party[packet.partyPosition] = pcPokemon
        pc[packet.pcPosition] = partyPokemon
    }
}
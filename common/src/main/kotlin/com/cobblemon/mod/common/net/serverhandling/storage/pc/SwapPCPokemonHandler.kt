/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.serverhandling.storage.pc

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.storage.PCEvent
import com.cobblemon.mod.common.api.events.storage.SwapPCPokemon
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.api.storage.pc.link.PCLinkManager
import com.cobblemon.mod.common.net.messages.client.storage.pc.ClosePCPacket
import com.cobblemon.mod.common.net.messages.server.storage.pc.SwapPCPokemonPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object SwapPCPokemonHandler : ServerNetworkPacketHandler<SwapPCPokemonPacket> {
    override fun handle(packet: SwapPCPokemonPacket, server: MinecraftServer, player: ServerPlayer) {
        val event = SwapPCPokemon(player)
        CobblemonEvents.SWAP_PC_POKEMON.emit(event)
        if (event.isCanceled) {
            ClosePCPacket(null).sendToPlayer(player)
            return
        }

        val pc = PCLinkManager.getPC(player) ?: return run { ClosePCPacket(null).sendToPlayer(player) }
        if (pc[packet.position1]?.uuid != packet.pokemon1ID || pc[packet.position2]?.uuid != packet.pokemon2ID) {
            return
        }
        CobblemonEvents.PC_EVENT.postThen(
            event = PCEvent(player),
            ifSucceeded = {
            },
            ifCanceled = {
                ClosePCPacket(null).sendToPlayer(player)
                return
            }
        )
        pc.swap(packet.position1, packet.position2)
    }
}
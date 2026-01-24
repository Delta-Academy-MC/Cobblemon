/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.serverhandling.storage.pc

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.storage.MovePCPokemon
import com.cobblemon.mod.common.api.events.storage.PCEvent
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.api.storage.pc.link.PCLinkManager
import com.cobblemon.mod.common.net.messages.client.storage.pc.ClosePCPacket
import com.cobblemon.mod.common.net.messages.server.storage.pc.MovePCPokemonPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object MovePCPokemonHandler : ServerNetworkPacketHandler<MovePCPokemonPacket> {
    override fun handle(packet: MovePCPokemonPacket, server: MinecraftServer, player: ServerPlayer) {
        val event = MovePCPokemon(player)
        CobblemonEvents.MOVE_PC_POKEMON.emit(event)
        if (event.isCanceled) {
            ClosePCPacket(null).sendToPlayer(player)
            return
        }

        val pc = PCLinkManager.getPC(player) ?: return run { ClosePCPacket(null).sendToPlayer(player) }
        val pokemon = pc[packet.oldPosition] ?: return
        if (pokemon.uuid != packet.pokemonID) {
            return
        }
        if (!pc.isValidPosition(packet.newPosition)) {
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
        val existingPokemon = pc[packet.newPosition]
        if (existingPokemon == null) {
            pc.move(pokemon, packet.newPosition)
        } else {
            // This should've been a swap if the slot is already taken. Seems like desync?
        }
    }
}
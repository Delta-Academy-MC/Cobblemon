/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.pokemon.update

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.client.pokemon.update.ClientboundUpdateRidingStatePacket
import net.minecraft.client.Minecraft

object ClientboundUpdateRidingStateHandler : ClientNetworkPacketHandler<ClientboundUpdateRidingStatePacket> {
    override fun handle(packet: ClientboundUpdateRidingStatePacket, client: Minecraft) {
        val player = client.player ?: return
        val entity = player.level().getEntity(packet.entity) ?: return
        if (entity !is PokemonEntity) return
        if (entity.controllingPassenger == player) return
        val buffer = packet.data ?: return
        if (entity.ridingController?.context?.settings?.key != packet.behaviour) {
            entity.ridingController?.changeBehaviour(packet.behaviour)
        }
        entity.ridingController?.context?.state?.decode(buffer)
    }
}

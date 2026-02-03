/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.riding

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.net.messages.client.orientation.ClientboundUpdateDriverInputPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer

object ClientboundUpdateDriverInputHandler : ClientNetworkPacketHandler<ClientboundUpdateDriverInputPacket> {
    override fun handle(packet: ClientboundUpdateDriverInputPacket, client: Minecraft) {
        val level = client.level ?: return
        val entity = level.getEntity(packet.entityId)
        if (entity is RemotePlayer) {
            entity.xxa = packet.driverInput.x
            entity.zza = packet.driverInput.z
            entity.jumping = packet.driverInput.y == 1.0f
            entity.isShiftKeyDown = packet.driverInput.y == -1.0f
        }
    }
}

/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.orientation

import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.duck.RidePassenger
import com.cobblemon.mod.common.net.messages.client.orientation.ClientboundUpdateOrientationPacket
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth

object ClientboundUpdateOrientationHandler : ClientNetworkPacketHandler<ClientboundUpdateOrientationPacket> {
    override fun handle(packet: ClientboundUpdateOrientationPacket, client: Minecraft) {
        val level = client.level ?: return
        val player = client.player ?: return
        val vehicle = level.getEntity(packet.entityId)
        val vehicleController = if (vehicle is OrientationControllable) vehicle.orientationController else return

        // If the client is also the driver of this entity then don't update it as the driver is the source
        // of truth on vehicle orientation updates.
        if (vehicle.controllingPassenger != player) {
            packet.active?.let { shouldUseCustomOrientation ->

                val playerIsPassenger = vehicle.passengers.contains(player)

                // If the player has just switched to riding a custom orientation ride then set their
                // ride x and y rots local to the vehicle rots(if the controller wasn't active and will now be active)
                // This ensures that on transition your camera stays in the same spot
                if (!vehicleController.isActive() && shouldUseCustomOrientation && playerIsPassenger) {
                    // Set local to the vehicle x and yrot
                    val playerRotater = player as RidePassenger?
                    if (playerRotater != null) {
                        playerRotater.`cobblemon$setRideXRot`(Mth.wrapDegrees(player.getXRot() - vehicle.getXRot()))
                        playerRotater.`cobblemon$setRideYRot`(Mth.wrapDegrees(player.getYRot() - vehicle.getYRot()))
                    }
                }

                vehicle.orientationController.active = shouldUseCustomOrientation
            }
            vehicle.orientationController.updateOrientation { _ -> packet.orientation }
        }
    }
}

/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.text.green
import com.cobblemon.mod.common.api.text.plus
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.net.messages.client.CalculateSeatPositionsPacket
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.world.phys.Vec3

object CalculateSeatPositionsHandler : ClientNetworkPacketHandler<CalculateSeatPositionsPacket> {
    private const val FORMAT = "%.2f"
    private const val SEAT_PREFIX = "seat_"
    private const val SIMULATION_PERIOD_TICKS = 15 * 20F

    class SeatRanges(
        var minX: Float = 9999F,
        var minY: Float = 9999F,
        var minZ: Float = 9999F,
        var maxX: Float = -9999F,
        var maxY: Float = -9999F,
        var maxZ: Float = -9999F
    )

    override fun handle(packet: CalculateSeatPositionsPacket, client: Minecraft) {
        val state = FloatingState()
        state.currentAspects = packet.aspects
        val model = VaryingModelRepository.getPoser(packet.speciesIdentifier, state)
        model.context = RenderContext()
        model.context.put(RenderContext.SPECIES, packet.speciesIdentifier)
        model.context.put(RenderContext.ASPECTS, packet.aspects)
        model.context.put(RenderContext.POSABLE_STATE, state)
        model.context.put(RenderContext.RENDER_STATE, RenderContext.RenderState.WORLD)


        state.setPoseToFirstSuitable(packet.poseType)

        val seatData = mutableMapOf<Int, SeatRanges>()

        var ticks = 0F

        // This will loop through the animation and find the extremes of the seat positions so we can later find the avg.
        // Most animations are going to loop in like 3 seconds but we can't know that, so just run through 15s to be safe.
        while (ticks < SIMULATION_PERIOD_TICKS) {
            state.updatePartialTicks(ticks)

            model.applyAnimations(null, state, 0F, 0F, 0F, 0F, 0F)
            model.updateLocators(null, state)

            for ((key, locatorState) in state.locatorStates.entries.filter { it.key.startsWith(SEAT_PREFIX) }.sortedBy { it.key }) {
                val index = key.replace(SEAT_PREFIX, "").toInt()
                val seatPosition = locatorState.transformPosition(Vec3.ZERO)
                val seatRanges = seatData.getOrPut(index, ::SeatRanges)
                if (seatPosition.x < seatRanges.minX) seatRanges.minX = seatPosition.x.toFloat()
                if (seatPosition.y < seatRanges.minY) seatRanges.minY = seatPosition.y.toFloat()
                if (seatPosition.z < seatRanges.minZ) seatRanges.minZ = seatPosition.z.toFloat()
                if (seatPosition.x > seatRanges.maxX) seatRanges.maxX = seatPosition.x.toFloat()
                if (seatPosition.y > seatRanges.maxY) seatRanges.maxY = seatPosition.y.toFloat()
                if (seatPosition.z > seatRanges.maxZ) seatRanges.maxZ = seatPosition.z.toFloat()
            }

            ticks++
        }

        val species = PokemonSpecies.getByIdentifier(packet.speciesIdentifier)!!
        val form = species.getForm(packet.aspects)
        val scale = form.baseScale

        var text = "Seats:".text()
        for ((index, seatRanges) in seatData) {
            val x = String.format(FORMAT, ((seatRanges.minX + seatRanges.maxX) / 2 * scale))
            val y = String.format(FORMAT, ((seatRanges.minY + seatRanges.maxY) / 2 * -1 * scale)) // Y is inverted between model and world
            val z = String.format(FORMAT, ((seatRanges.minZ + seatRanges.maxZ) / 2 * scale))

            val str = """
                {
              "x": $x,
              "y": $y,
              "z": $z
            }
            """.trimIndent()

            text.plus(" [$index]".green().also {
                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, str))
            })
        }
        Minecraft.getInstance().player!!.sendSystemMessage(text)
    }
}
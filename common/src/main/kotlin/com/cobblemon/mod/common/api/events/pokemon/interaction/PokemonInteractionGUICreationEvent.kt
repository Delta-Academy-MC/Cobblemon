/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.events.pokemon.interaction

import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelOption
import com.cobblemon.mod.common.client.gui.interact.wheel.Orientation
import com.google.common.collect.Multimap
import java.util.UUID

data class PokemonInteractionGUICreationEvent(
    val pokemonID: UUID,
    val mountShoulder: Boolean,
    val giveHeld: Boolean,
    val giveCosmetic: Boolean,
    val canRide: Boolean,
    val canTransform: Boolean,
    val options: Multimap<Orientation, InteractWheelOption>
) {
    fun addFillingOption(option: InteractWheelOption) {
        options.put(getNextFreeOrientation(), option)
    }

    fun addOption(orientation: Orientation, option: InteractWheelOption) {
        options.put(orientation, option)
    }

    private fun getNextFreeOrientation(): Orientation {
        var largest = Orientation.NORTH
        for (orientation in Orientation.entries) {
            if(!options.containsKey(orientation)) {
                return orientation
            } else {
                if(options.get(orientation).size < options.get(largest).size) {
                    largest = orientation
                }
            }
        }
        return largest
    }
}

/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.data

import com.cobblemon.mod.common.api.interaction.PokemonInteractionSet
import com.cobblemon.mod.common.api.interaction.PokemonInteractions
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.RegistryFriendlyByteBuf

class PokemonInteractionsSyncPacket(
    entries: Collection<PokemonInteractionSet>,
) : DataRegistrySyncPacket<PokemonInteractionSet, PokemonInteractionsSyncPacket>(entries) {
    override val id = ID

    override fun encodeEntry(buffer: RegistryFriendlyByteBuf, entry: PokemonInteractionSet) {
        entry.encode(buffer)
    }

    override fun decodeEntry(buffer: RegistryFriendlyByteBuf): PokemonInteractionSet {
        return PokemonInteractionSet.decode(buffer)
    }

    override fun synchronizeDecoded(entries: Collection<PokemonInteractionSet>) {
        PokemonInteractions.interactions.clear()
        PokemonInteractions.interactions.addAll(entries)
    }

    companion object {
        val ID = cobblemonResource("pokemon_interactions_sync")
        fun decode(buffer: RegistryFriendlyByteBuf): PokemonInteractionsSyncPacket = PokemonInteractionsSyncPacket(emptyList()).apply { decodeBuffer(buffer) }
    }
}
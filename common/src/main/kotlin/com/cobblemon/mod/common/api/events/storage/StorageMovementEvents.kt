/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.events.storage

import com.cobblemon.mod.common.api.events.Cancelable
import net.minecraft.server.level.ServerPlayer

data class MovePartyPokemon(val player: ServerPlayer): Cancelable()

data class MovePCPokemon(val player: ServerPlayer): Cancelable()

data class SwapPartyPokemon(val player: ServerPlayer): Cancelable()

data class SwapPCPokemon(val player: ServerPlayer): Cancelable()

data class SwapPCPartyPokemon(val player: ServerPlayer): Cancelable()
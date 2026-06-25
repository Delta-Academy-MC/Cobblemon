/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.timers

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.battles.ForfeitActionResponse
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import java.time.Instant

interface PlayerBattleTimer {
    val actor: PlayerBattleActor
    val battle: PokemonBattle

    fun startSelection()
    fun endSelection()
    fun tick()
    fun mustChooseBy(): Instant

    fun timeout() {
        actor.setActionResponses(listOf(ForfeitActionResponse()))
    }
}
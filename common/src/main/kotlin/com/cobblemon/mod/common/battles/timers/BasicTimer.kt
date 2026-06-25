/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.timers

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import java.time.Duration
import java.time.Instant

class BasicTimer(override val battle: PokemonBattle, override val actor: PlayerBattleActor, val duration: Duration) : PlayerBattleTimer {
    val endTime = Instant.now().plusSeconds(duration.toSeconds())

    override fun startSelection() {}
    override fun endSelection() {}
    override fun tick() {}

    override fun mustChooseBy(): Instant {
        return endTime
    }
}
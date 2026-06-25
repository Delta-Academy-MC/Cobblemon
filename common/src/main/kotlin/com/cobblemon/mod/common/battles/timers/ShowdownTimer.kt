/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.timers

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.battles.ForfeitActionResponse
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.util.getPlayer
import java.time.Duration
import java.time.Instant

class ShowdownTimer(override val battle: PokemonBattle, override val actor: PlayerBattleActor): PlayerBattleTimer {
    private lateinit var turnStart: Instant
    private var secondsRemaining = 210
    private var hasSelected = false

    init {
        checkForStreamerIncrease()
    }

    /**
     * For streamers, we introduce a permission that doubles this timer as a deterrence of stream sniping.
     * We also have a similar system for notifying streamers when the opponent has used a move (so they can choose last.)
     */
    fun checkForStreamerIncrease() {
        val player = actor.uuid.getPlayer() ?: return
        if (!Cobblemon.permissionValidator.hasPermission(player, "battle.mode.streamer", 4)) return
        secondsRemaining *= 2
    }

    override fun tick() {
        if (hasSelected) return
        val secondsSinceStart = Duration.between(turnStart, Instant.now()).seconds
        if (secondsRemaining - secondsSinceStart <= 0) timeout()
    }

    override fun startSelection() {
        turnStart = Instant.now()
        hasSelected = false
    }

    override fun endSelection() {
        hasSelected = true
        val secondsSinceStart = Duration.between(turnStart, Instant.now()).seconds.toInt()
        secondsRemaining = secondsRemaining - secondsSinceStart + 10
    }

    override fun mustChooseBy(): Instant {
        return Instant.now().plusSeconds(secondsRemaining.toLong())
    }
}
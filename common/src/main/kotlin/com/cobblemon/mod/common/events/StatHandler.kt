/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents.BATTLE_FLED
import com.cobblemon.mod.common.api.events.CobblemonEvents.BATTLE_STARTED_POST
import com.cobblemon.mod.common.api.events.CobblemonEvents.BATTLE_VICTORY
import com.cobblemon.mod.common.api.events.CobblemonEvents.COLLECT_EGG
import com.cobblemon.mod.common.api.events.CobblemonEvents.EVOLUTION_COMPLETE
import com.cobblemon.mod.common.api.events.CobblemonEvents.FOSSIL_REVIVED
import com.cobblemon.mod.common.api.events.CobblemonEvents.HATCH_EGG_POST
import com.cobblemon.mod.common.api.events.CobblemonEvents.LEVEL_UP_EVENT
import com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_CAPTURED
import com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_GAINED
import com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_RELEASED_EVENT_POST
import com.cobblemon.mod.common.api.events.CobblemonEvents.POKEROD_CAST_POST
import com.cobblemon.mod.common.api.events.CobblemonEvents.POKEROD_REEL
import com.cobblemon.mod.common.api.events.CobblemonEvents.RIDE_EVENT_POST
import com.cobblemon.mod.common.api.events.CobblemonEvents.TRADE_EVENT_POST
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.cobblemon.mod.common.api.events.fishing.PokerodCastEvent
import com.cobblemon.mod.common.api.events.fishing.PokerodReelEvent
import com.cobblemon.mod.common.api.events.pokemon.*
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent
import com.cobblemon.mod.common.api.events.storage.ReleasePokemonEvent
import com.cobblemon.mod.common.api.stats.CobblemonStats.getStat
import com.cobblemon.mod.common.util.getPlayer
import net.minecraft.server.level.ServerPlayer
import java.util.*

object StatHandler : EventHandler {
    override fun registerListeners() {
        POKEMON_CAPTURED.subscribe(Priority.NORMAL, ::onCapture)
        POKEMON_RELEASED_EVENT_POST.subscribe(Priority.NORMAL, ::onRelease)
        EVOLUTION_COMPLETE.subscribe(Priority.LOWEST, ::onEvolve)
        LEVEL_UP_EVENT.subscribe(Priority.NORMAL, ::onLevelUp)
        BATTLE_VICTORY.subscribe(Priority.NORMAL, ::onWinBattle)
        BATTLE_FLED.subscribe(Priority.NORMAL, ::onFleeBattle)
        BATTLE_STARTED_POST.subscribe(Priority.NORMAL, ::onBattleStart)
        POKEMON_GAINED.subscribe(Priority.NORMAL, ::onDexEntryGain)
        COLLECT_EGG.subscribe(Priority.NORMAL, ::onCollectEgg)
        HATCH_EGG_POST.subscribe(Priority.NORMAL, ::onHatchEgg)
        TRADE_EVENT_POST.subscribe(Priority.NORMAL, ::onTradeCompleted)
        FOSSIL_REVIVED.subscribe(Priority.NORMAL, ::onFossilRevived)
        RIDE_EVENT_POST.subscribe(Priority.NORMAL, ::onRide)
        POKEROD_CAST_POST.subscribe(Priority.NORMAL, ::onPokeRodCast)
        POKEROD_REEL.subscribe(Priority.NORMAL, ::onReelIn)
    }

    fun onCapture(event : PokemonCapturedEvent) {
        event.player.awardStat(getStat(Cobblemon.statistics.CAPTURED))
        if (event.pokemon.shiny) {
            event.player.awardStat(getStat(Cobblemon.statistics.SHINIES_CAPTURED))
        }
    }

    fun onRelease(event : ReleasePokemonEvent.Post) {
        event.player.awardStat(getStat(Cobblemon.statistics.RELEASED))
    }

    fun onEvolve(event : EvolutionCompleteEvent) {
        event.pokemon.getOwnerPlayer()?.awardStat(getStat(Cobblemon.statistics.EVOLVED))
    }

    fun onLevelUp(event : LevelUpEvent) {
        event.pokemon.getOwnerPlayer()?.awardStat(getStat(Cobblemon.statistics.LEVEL_UP))
    }

    fun onWinBattle(event : BattleVictoryEvent) {
        if (!event.wasWildCapture) {
            if (event.battle.isPvW) {
                event.winners
                    .flatMap { it.getPlayerUUIDs().mapNotNull(UUID::getPlayer) }
                    .forEach { player -> player.awardStat(getStat(Cobblemon.statistics.BATTLES_WON)) }
            }
        }
    }

    fun onFleeBattle(event: BattleFledEvent) {
        event.player.entity?.awardStat(getStat(Cobblemon.statistics.BATTLES_FLED))
    }

    fun onBattleStart(event : BattleStartedEvent) {
        event.battle.players.forEach { player ->
            player.awardStat(getStat(Cobblemon.statistics.BATTLES_TOTAL))
        }
    }

    fun onDexEntryGain(event : PokemonGainedEvent) {
        val player = event.pokemon.getOwnerPlayer()
        player?.awardStat(getStat(Cobblemon.statistics.DEX_ENTRIES))
    }

    fun onCollectEgg(event : CollectEggEvent) {
        event.player.awardStat(getStat(Cobblemon.statistics.EGGS_COLLECTED))
    }

    fun onHatchEgg(event : HatchEggEvent.Post) {
        event.player.awardStat(getStat(Cobblemon.statistics.EGGS_HATCHED))
    }

    fun onTradeCompleted(event : TradeEvent.Post) {
        event.tradeParticipant1Pokemon.getOwnerPlayer()?.awardStat(getStat(Cobblemon.statistics.TRADED))
        event.tradeParticipant2Pokemon.getOwnerPlayer()?.awardStat(getStat(Cobblemon.statistics.TRADED))
    }

    fun onFossilRevived(event : FossilRevivedEvent) {
        event.player?.awardStat(getStat(Cobblemon.statistics.FOSSILS_REVIVED))
    }

    fun onRide(event : RidePokemonEvent.Post) {
        event.player.awardStat(getStat(Cobblemon.statistics.TIMES_RIDDEN))
    }

    fun onPokeRodCast(event : PokerodCastEvent.Post) {
        if (event.bobber.owner is ServerPlayer) {
            (event.bobber.owner as ServerPlayer).awardStat(getStat(Cobblemon.statistics.ROD_CASTS))
        }
    }

    fun onReelIn(event : PokerodReelEvent) {
        event.player.awardStat(getStat(Cobblemon.statistics.REEL_INS))
    }
}

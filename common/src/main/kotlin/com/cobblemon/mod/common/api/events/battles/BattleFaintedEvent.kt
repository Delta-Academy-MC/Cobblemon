/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.events.battles

import com.bedrockk.molang.runtime.value.MoValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.api.battles.interpreter.BattleContext
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.util.asArrayValue

/**
 * Event fired when a [BattlePokemon] faints. Exposes the [BattlePokemon] that fainted and the [BattleContext]
 * of how it fainted.
 *
 * @author Segfault Guy
 * @since April 6th, 2023
 */
data class BattleFaintedEvent(
    override val battle: PokemonBattle,
    val killed: BattlePokemon,
    val context: BattleContext,
    val pnx: String
) : BattleEvent {
    val structContext = mutableMapOf<String, MoValue>(
        "battle" to battle.struct,
        "players" to battle.actors.filter { it.type == ActorType.PLAYER }.asArrayValue { it.struct },
        "npcs" to battle.actors.filter { it.type == ActorType.NPC }.asArrayValue { it.struct },
        "wild_pokemon" to battle.actors.filter { it.type == ActorType.WILD }.asArrayValue { it.struct },
        "pokemon" to killed.struct,
        "context" to context.getStruct(),
        "pnx" to StringValue(pnx)
    )
}
/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.events.battles.instruction

import com.bedrockk.molang.runtime.value.MoValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.events.battles.BattleEvent
import com.cobblemon.mod.common.battles.dispatch.InterpreterInstruction
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.util.asArrayValue

/**
 * Event that is fired when a Pokemon changes Forme in battle.
 * @param battle The battle where the change occurred.
 * @param pokemon The Pokemon that changed Forme.
 */
data class FormeChangeEvent(
    override val battle: PokemonBattle,
    val pokemon: BattlePokemon,
    val formeName: String,
    val pnx: String
) : BattleEvent {
    val context = mutableMapOf(
        "battle" to battle.struct,
        "players" to battle.actors.filter { it.type == ActorType.PLAYER }.asArrayValue { it.struct },
        "npcs" to battle.actors.filter { it.type == ActorType.NPC }.asArrayValue { it.struct },
        "wild_pokemon" to battle.actors.filter { it.type == ActorType.WILD }.asArrayValue { it.struct },
        "pokemon" to pokemon.struct,
        "forme" to StringValue(formeName),
        "pnx" to StringValue(pnx)
    )
}
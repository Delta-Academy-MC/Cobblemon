package com.cobblemon.mod.common.api.events.battles

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.events.Cancelable

data class FleeEvent(val battle: PokemonBattle): Cancelable()
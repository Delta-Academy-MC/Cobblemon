package com.cobblemon.mod.common.api.events.pokemon

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.server.level.ServerPlayer

data class TransformationValidationEvent(val pokemon: PokemonEntity, val player: ServerPlayer, var result: Boolean)
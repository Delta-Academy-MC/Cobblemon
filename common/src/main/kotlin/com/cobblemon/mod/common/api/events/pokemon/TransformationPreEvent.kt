package com.cobblemon.mod.common.api.events.pokemon

import com.cobblemon.mod.common.api.events.Cancelable
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.server.level.ServerPlayer

data class TransformationPreEvent(val pokemon: PokemonEntity, val player: ServerPlayer): Cancelable()
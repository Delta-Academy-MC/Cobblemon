package com.cobblemon.mod.common.api.events.pokemon.interaction

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.server.level.ServerPlayer

data class InteractWheelOpenEvent(
    val pokemon: PokemonEntity,
    val player: ServerPlayer,
    var canMountShoulder: Boolean,
    var canGiveHeldItem: Boolean,
    var canGiveCosmeticItem: Boolean,
    var canRide: Boolean,
    var canTransform: Boolean
)
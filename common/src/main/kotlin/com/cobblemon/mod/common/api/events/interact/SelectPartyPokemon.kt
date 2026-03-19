package com.cobblemon.mod.common.api.events.interact

import com.cobblemon.mod.common.api.events.Cancelable
import net.minecraft.server.level.ServerPlayer

data class SelectPartyPokemon(val player: ServerPlayer): Cancelable()
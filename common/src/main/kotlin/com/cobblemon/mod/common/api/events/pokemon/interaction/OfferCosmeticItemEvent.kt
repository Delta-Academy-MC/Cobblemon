package com.cobblemon.mod.common.api.events.pokemon.interaction

import com.cobblemon.mod.common.api.events.Cancelable
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

data class OfferCosmeticItemEvent(val pokemon: PokemonEntity, val player: Player, val stack: ItemStack) : Cancelable()
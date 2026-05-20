package com.cobblemon.mod.common.entity.pokemon

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

object TransformationHandler {

    fun canTransform(pokemon: PokemonEntity, player: ServerPlayer): Boolean {
        val item = player.getItemInHand(InteractionHand.MAIN_HAND).item
        return when (item) {
            CobblemonItems.GRACIDEA -> pokemon.pokemon.species.name == "Shaymin"
            else -> false
        }
    }

    fun transform(pokemon: PokemonEntity, player: ServerPlayer) {
        if (!canTransform(pokemon, player)) return

        val item = player.getItemInHand(InteractionHand.MAIN_HAND).item
        when (item) {
            CobblemonItems.GRACIDEA -> {
                if ("sky-forme" in pokemon.aspects) {
                    PokemonProperties.parse("gracidea_forme=land").apply(pokemon)
                }
                else {
                    PokemonProperties.parse("gracidea_forme=sky").apply(pokemon)
                }
            }
        }
    }

}
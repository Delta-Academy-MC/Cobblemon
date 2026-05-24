package com.cobblemon.mod.common.entity.pokemon

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.TransformationPreEvent
import com.cobblemon.mod.common.api.events.pokemon.TransformationValidationEvent
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

object TransformationHandler {

    fun canTransform(pokemon: PokemonEntity, player: ServerPlayer): Boolean {
        val item = player.getItemInHand(InteractionHand.MAIN_HAND).item
        val result = when (item) {
            CobblemonItems.GRACIDEA -> pokemon.pokemon.species.name == "Shaymin"
            else -> false
        }
        val event = TransformationValidationEvent(pokemon, player, result)
        CobblemonEvents.TRANSFORMATION_VALIDATION_EVENT.post(event)
        return event.result
    }

    fun transform(pokemon: PokemonEntity, player: ServerPlayer) {
        if (!canTransform(pokemon, player)) return

        val event = TransformationPreEvent(pokemon, player)
        CobblemonEvents.TRANSFORMATION_PRE_EVENT.post(event)
        if (event.isCanceled) return

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
            CobblemonItems.PRISON_BOTTLE -> {
                if ("unbound" in pokemon.aspects) {
                    PokemonProperties.parse("djinn_state=confined").apply(pokemon)
                }
                else {
                    PokemonProperties.parse("djinn_state=unbound").apply(pokemon)
                }
            }
        }
    }

}
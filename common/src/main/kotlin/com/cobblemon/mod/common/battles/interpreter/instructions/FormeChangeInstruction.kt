/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.interpreter.instructions

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.instruction.FormeChangeEvent
import com.cobblemon.mod.common.battles.ShowdownInterpreter
import com.cobblemon.mod.common.battles.dispatch.InterpreterInstruction
import com.cobblemon.mod.common.net.messages.client.battle.BattleSwitchPokemonPacket
import com.cobblemon.mod.common.util.battleLang

/**
 * Format: |-formechange|POKEMON|SPECIES|HP STATUS or |detailschange|POKEMON|DETAILS|HP STATUS
 *
 * POKEMON has temporarily (-formechange) or permanently (detailschange) changed form to SPECIES/DETAILS.
 * @author Segfault Guy
 * @since December 9th, 2024
 */
class FormeChangeInstruction(val message: BattleMessage): InterpreterInstruction {

    override fun invoke(battle: PokemonBattle) {
        val (pnx, _) = message.pnxAndUuid(0) ?: return
        val battlePokemon = message.battlePokemon(0, battle) ?: return
        val pokemonName = battlePokemon.getName()
        val details = message.argumentAt(1)?.split(',')?.get(0)?.lowercase() ?: return
        val speciesName = details.substringBefore('-')
        val formName = details.substringAfterLast('-').ifBlank { speciesName }
        val typeKey = if (message.id == "detailschange") "permanent" else "temporary"

        ShowdownInterpreter.broadcastOptionalAbility(battle, message.effect(), battlePokemon)

        battle.dispatchWaiting {
            battle.minorBattleActions[battlePokemon.uuid] = message
            CobblemonEvents.FORME_CHANGE.post(FormeChangeEvent(battle, battlePokemon, formName, pnx))

            if (formName.equals(battlePokemon.effectedPokemon.species.name, true)) {
                battlePokemon.sendUpdate()
                battlePokemon.clearBattleFeatures()
                battle.sendUpdate(BattleSwitchPokemonPacket(pnx, battlePokemon, true, battlePokemon.getIllusion()))
                battle.broadcastChatMessage(battleLang("formechangeend.$formName", pokemonName))
            } else {
                val form = battlePokemon.effectedPokemon.species.forms.find { it.name.equals(formName, true) }
                if (form != null) {
                    battlePokemon.clearBattleFeatures()
                    println("FORME CHANGE: ${form.aspects[0]} ALL ASPECTS: ${form.aspects}")
                    battlePokemon.setBattleFeature(form.aspects[0], true)
                }
                val lang = when(formName) {
                    "busted", "hero", "complete" -> return@dispatchWaiting
                    "school", "wishiwashi", "meteor", "minior" -> battleLang("formechange.$formName", pokemonName)
                    speciesName -> battleLang("formechange.default.temporary.end", pokemonName)
                    else -> battleLang("formechange.default.$typeKey", pokemonName, formName)
                }
                battle.broadcastChatMessage(lang)
            }
        }
    }
}
/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.interpreter.instructions

import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleHPEvent
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext
import com.cobblemon.mod.common.api.moves.animations.ActionEffects
import com.cobblemon.mod.common.api.moves.animations.UsersProvider
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.api.text.aqua
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.battles.ShowdownInterpreter
import com.cobblemon.mod.common.battles.dispatch.ActionEffectInstruction
import com.cobblemon.mod.common.battles.dispatch.GO
import com.cobblemon.mod.common.battles.dispatch.InstructionSet
import com.cobblemon.mod.common.battles.dispatch.UntilDispatch
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.net.messages.client.animation.PlayPosableAnimationPacket
import com.cobblemon.mod.common.net.messages.client.battle.BattleHealthChangePacket
import com.cobblemon.mod.common.pokemon.evolution.progress.DamageTakenEvolutionProgress
import com.cobblemon.mod.common.pokemon.evolution.progress.RecoilEvolutionProgress
import com.cobblemon.mod.common.pokemon.status.statuses.persistent.PoisonStatus
import com.cobblemon.mod.common.util.asIdentifierDefaultingNamespace
import com.cobblemon.mod.common.util.battleLang
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import java.util.concurrent.CompletableFuture
import net.minecraft.network.chat.Component

/**
 * Format: |-damage|POKEMON|HP STATUS
 *
 * POKEMON has taken damage and is now at HP STATUS
 * @author Hiroku
 * @since March 11th, 2022
 */
class DamageInstruction(
    val instructionSet: InstructionSet,
    val actor: BattleActor,
    val publicMessage: BattleMessage,
    val privateMessage: BattleMessage
) : ActionEffectInstruction {
    val expectedTarget = publicMessage.battlePokemon(0, actor.battle)
    override var future: CompletableFuture<*> = CompletableFuture.completedFuture(Unit)
    override var holds = mutableSetOf<String>()
    override val id = cobblemonResource("damage")

    override fun preActionEffect(battle: PokemonBattle) {
        val battlePokemon = publicMessage.battlePokemon(0, actor.battle) ?: return
        val recoiling = privateMessage.optionalArgument("from")?.equals("recoil", true) == true
        val lastCauser  = instructionSet.getMostRecentCauser(comparedTo = this)
        if (recoiling) {
            doRecoilEvoChecks(battlePokemon)
            if (lastCauser is MoveInstruction) {
                battle.dispatch {
                    //Is the recoil hold currently used anywhere?
                    UntilDispatch { "recoil" !in lastCauser.holds }
                }
            }
        }
        val newHealth = privateMessage.argumentAt(1)?.split(" ")?.get(0) ?: return
        CobblemonEvents.BATTLE_HP.post(BattleHPEvent(battle, battlePokemon, newHealth))
        val effect = privateMessage.effect()
        val source = privateMessage.battlePokemonFromOptional(battle)
        source?.let { ShowdownInterpreter.broadcastOptionalAbility(battle, effect, it) }
    }

    private fun doRecoilEvoChecks(battlePokemon: BattlePokemon) {
        battlePokemon.effectedPokemon.let { pokemon ->
            if (RecoilEvolutionProgress.supports(pokemon)) {
                val healthStr = privateMessage.argumentAt(1) ?: throw UnsupportedOperationException(
                    "Cant get recoil string"
                )
                val newHealth =
                    "([0-9]+).*".toRegex().find(healthStr)?.groups?.get(1)?.value?.toIntOrNull()
                        ?: throw UnsupportedOperationException("Cant get recoil string")
                val difference = pokemon.currentHealth - newHealth
                if (difference > 0) {
                    val progress = pokemon.evolutionProxy.current()
                        .progressFirstOrCreate({ it is RecoilEvolutionProgress }) { RecoilEvolutionProgress() }
                    progress.updateProgress(RecoilEvolutionProgress.Progress(progress.currentProgress().recoil + difference))
                }
            }
        }
    }

    override fun runActionEffect(battle: PokemonBattle, runtime: MoLangRuntime) {
        val effect = privateMessage.effect()
        val battlePokemon = publicMessage.battlePokemon(0, actor.battle) ?: return
        var status = effect?.id?.let { Statuses.getStatus(it) }
        battle.dispatch {
            val pokemon = privateMessage.battlePokemon(0, battle) ?: return@dispatch GO
            //Showdown doesnt tell us on damage if its poison or toxic so we gotta consult the entity
            if (status is PoisonStatus) {
                status = pokemon.effectedPokemon.status?.status ?: status
            }

            // Damage inflicted by an effect (not just a move) will potentially get its own action effect.
            // We don't play an effect at all if there's no effect on the damage because we anticipate that
            // the move's action effect will handle that. Status action effects are specially defined (we could
            // migrate this back to damage_{status} later tbh) but then it will try damage_{status} and failing
            // that then we should try to show something about the fact that some kind of effect damage is being applied.
            val actionEffect = status?.getActionEffect()
                ?: effect?.let {
                    ActionEffects.actionEffects["damage_${it.id}".asIdentifierDefaultingNamespace()]
                        ?: ActionEffects.actionEffects["generic_damage".asIdentifierDefaultingNamespace()]
                }
                ?: return@dispatch GO // will occur if the damage is from a move


            val providers = mutableListOf<Any>(battle)
            battlePokemon.effectedPokemon.entity?.let { UsersProvider(it) }?.let(providers::add)

            val context = ActionEffectContext(
                actionEffect = actionEffect,
                runtime = runtime,
                providers = providers,
                level = battle.players.firstOrNull()?.level()
            )
            this.future = actionEffect.run(context)
            holds = context.holds // Reference so future things can check on this action effect's holds
            future.thenApply { holds.clear() }
            return@dispatch GO
        }
    }

    override fun postActionEffect(battle: PokemonBattle) {
        val newHealth = privateMessage.argumentAt(1)?.split(" ")?.get(0) ?: return
        val battlePokemon = publicMessage.battlePokemon(0, actor.battle) ?: return
        var causedFaint = newHealth == "0"
        val effect = privateMessage.effect()
        val source = privateMessage.battlePokemonFromOptional(battle)
        val lastCauser  = instructionSet.getMostRecentCauser(comparedTo = this)
        battle.dispatch {
            val pokemonName = battlePokemon.getName()
            // Captured inside the dispatch (after any earlier hit of a multi-hit move has already been
            // applied) so the reported damage is this hit's amount, not the cumulative total.
            val previousHealth = battlePokemon.effectedPokemon.currentHealth
            val maxHp = battlePokemon.effectedPokemon.maxHealth
            val pokemonEntity = battlePokemon.entity
            //Play recoil animation if the pokemon recoiling isnt dead
            if (!causedFaint && pokemonEntity != null) {
                val pkt = PlayPosableAnimationPacket(pokemonEntity.id, setOf("recoil"), emptyList())
                pkt.sendToPlayersAround(
                    x = pokemonEntity.x,
                    y = pokemonEntity.y,
                    z = pokemonEntity.z,
                    worldKey = pokemonEntity.level().dimension(),
                    distance = 50.0
                )
            }

            val newHealthRatio: Float
            val remainingHealth = newHealth.split("/")[0].toInt()

            if (effect != null) {
                val lang = when (effect.id) {
                    "blacksludge", "stickybarb", "jabocaberry", "rowapberry" -> battleLang("damage.item", pokemonName, effect.typelessData)
                    "brn", "psn", "tox" -> {
                        val status = Statuses.getStatus(effect.id)?.name?.path ?: return@dispatch GO
                        lang("status.$status.hurt", pokemonName)
                    }
                    "aftermath", "innardsout" -> battleLang("damage.generic", pokemonName)
                    "chloroblast", "steelbeam" -> battleLang("damage.mindblown", pokemonName)
                    "jumpkick" -> battleLang("damage.highjumpkick", pokemonName)
                    else -> battleLang("damage.${effect.id}", pokemonName, source?.getName() ?: Component.literal("UNKOWN"))
                }
                battle.broadcastChatMessage(lang.red())
            }

            // Announce the percentage of the target's max HP that this hit removed.
            val damageDealt = (previousHealth - remainingHealth).coerceAtLeast(0)
            if (damageDealt > 0 && maxHp > 0) {
                val percent = Math.round(damageDealt.toFloat() / maxHp * 100f).coerceAtLeast(1)
                battle.broadcastChatMessage(battleLang("damage_dealt", battlePokemon.effectedPokemon.getDisplayName(), "${percent}%").aqua())
            }

            if (causedFaint) {
                newHealthRatio = 0F
                battle.dispatch {
                    battlePokemon.effectedPokemon.currentHealth = 0
                    battlePokemon.sendUpdate()
                    GO
                }
                causedFaint = true
            } else {
                val maxHealth = newHealth.split("/")[1].toInt()
                val difference = maxHealth - remainingHealth
                newHealthRatio = remainingHealth.toFloat() / maxHealth
                battle.dispatchToFront {
                    battlePokemon.effectedPokemon.currentHealth = remainingHealth
                    if (difference > 0) {
                        battlePokemon.effectedPokemon.let { pokemon ->
                            if (DamageTakenEvolutionProgress.supports(pokemon)) {
                                val progress = pokemon.evolutionProxy.current().progressFirstOrCreate({ it is DamageTakenEvolutionProgress }) { DamageTakenEvolutionProgress() }
                                progress.updateProgress(DamageTakenEvolutionProgress.Progress(progress.currentProgress().amount + difference))
                            }
                        }
                    }
                    battlePokemon.sendUpdate()
                    GO
                }
            }
            privateMessage.pnxAndUuid(0)?.let { (pnx, _) -> battle.sendSidedUpdate(actor, BattleHealthChangePacket(pnx, remainingHealth.toFloat()), BattleHealthChangePacket(pnx, newHealthRatio)) }

            battle.minorBattleActions[battlePokemon.uuid] = privateMessage
            if (lastCauser is MoveInstruction) {
                if (lastCauser.spreadTargets.size > 1) {
                    val subsequentInstructions = instructionSet.findInstructionsCausedBy(lastCauser)
                    // Only wait on the last target of a multitarget attack
                    if (subsequentInstructions.filterIsInstance<DamageInstruction>().last() != this) {
                        // TODO: Dragon darts?
                        return@dispatch GO
                    }
                }
            }

            // If they faint from this damage then don't bother waiting for the damage to be applied.
            if (lastCauser is MoveInstruction && lastCauser.actionEffect != null && !causedFaint) {
                UntilDispatch { lastCauser.future.isDone }
            } else if (causedFaint) {
                GO
            } else {
                UntilDispatch { "effects" !in holds }
            }
        }
    }

}
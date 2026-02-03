/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.pokemon

import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asStruct
import com.cobblemon.mod.common.api.moves.MoveSet
import com.cobblemon.mod.common.api.pokemon.feature.BattleFormFeature
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemProvider
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.types.tera.TeraType
import com.cobblemon.mod.common.battles.actor.MultiPokemonBattleActor
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor
import com.cobblemon.mod.common.battles.interpreter.ContextManager
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.client.battle.BattleInitializePacket
import com.cobblemon.mod.common.net.messages.client.battle.BattleUpdateTeamPokemonPacket
import com.cobblemon.mod.common.net.messages.client.battle.BattleActorInformationPacket
import com.cobblemon.mod.common.net.messages.client.battle.BattlePokemonDTO
import com.cobblemon.mod.common.net.messages.client.battle.MoveDTO
import com.cobblemon.mod.common.pokemon.IVs
import com.cobblemon.mod.common.pokemon.Nature
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.properties.BattleCloneProperty
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty
import com.cobblemon.mod.common.util.battleLang
import com.cobblemon.mod.common.util.getPlayer
import com.cobblemon.mod.common.util.server
import java.util.UUID
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.ItemStack
import java.util.function.Function

open class BattlePokemon(
    val originalPokemon: Pokemon,
    val effectedPokemon: Pokemon = originalPokemon,
    var postBattlePokemonOperations: MutableList<(BattlePokemon) -> Unit> = mutableListOf(),
    var postBattleEntityOperations: MutableList<(PokemonEntity) -> Unit> = mutableListOf()
) {
    lateinit var actor: BattleActor

    constructor(originalPokemon: Pokemon, effectedPokemon: Pokemon, postBattleEntityOperation: (PokemonEntity) -> Unit) : this(
        originalPokemon = originalPokemon,
        effectedPokemon = effectedPokemon,
        postBattlePokemonOperations = mutableListOf<(BattlePokemon) -> Unit>(),
        postBattleEntityOperations = mutableListOf(postBattleEntityOperation)
    )

    companion object {
        fun safeCopyOf(pokemon: Pokemon): BattlePokemon {
            //TOOD figure out a closer registry access (might have to break some method signatures for this (1.7?)
            val effectedPokemon = pokemon.clone(registryAccess = server()?.registryAccess() ?: throw IllegalStateException("No registry access available"))
            BattleCloneProperty.isBattleClone().apply(effectedPokemon)
            UncatchableProperty.uncatchable().apply(effectedPokemon)
            return BattlePokemon(
                originalPokemon = pokemon,
                effectedPokemon = effectedPokemon,
                postBattleEntityOperation = { it.recallWithAnimation() }
            )
        }

        fun playerOwned(pokemon: Pokemon): BattlePokemon = BattlePokemon(
            originalPokemon = pokemon,
            effectedPokemon = pokemon,
            postBattleEntityOperation = { entity ->
                entity.effects.wipe()
            }
        )
    }

    val struct = QueryStruct(
        hashMapOf(
            "pokemon" to Function { effectedPokemon.asStruct() },
            "original_pokemon" to Function {
                originalPokemon.asStruct()
            },
            "actor" to Function { actor.struct },
            "battle" to Function { actor.battle.struct },
            "uuid" to Function { StringValue(uuid.toString()) },
            "health" to Function { DoubleValue(health.toDouble()) },
            "max_health" to Function { DoubleValue(maxHealth.toDouble()) },
            "ivs" to Function { effectedPokemon.ivs.struct },
            "nature" to Function { StringValue(effectedPokemon.nature.name.toString()) },
            "moveset" to Function { effectedPokemon.moveSet.toStruct() }
        ))

    val uuid: UUID
        get() = effectedPokemon.uuid
    val health: Int
        get() = effectedPokemon.currentHealth
    val maxHealth: Int
        get() = effectedPokemon.maxHealth
    val ivs: IVs
        get() = effectedPokemon.ivs
    val nature: Nature
        get() = effectedPokemon.nature
    val moveSet: MoveSet
        get() = effectedPokemon.moveSet
    val statChanges = mutableMapOf<Stat, Int>()
    var gone = false
    // etc

    val entity: PokemonEntity?
        get() = effectedPokemon.entity

    var willBeSwitchedIn = false

    /** A set of all the BattlePokemon that they faced during the battle (for exp purposes) */
    val facedOpponents = mutableSetOf<BattlePokemon>()

    /**
     * The [HeldItemManager] backing this [BattlePokemon].
     */
    val heldItemManager: HeldItemManager by lazy { HeldItemProvider.provide(this) }

    val contextManager = ContextManager()

    val boosts = mutableMapOf<Stats, Int>()
    var revealed = false
    var revealedAbility: String? = null
    val revealedMoves: MutableList<MoveDTO?> = mutableListOf(null, null, null, null)
    var revealedHeldItem: ItemStack? = null
    var transformed: BattlePokemon? = null
    var terastallized: TeraType? = null

    open fun getName(): MutableComponent {
        val displayPokemon = getIllusion()?.effectedPokemon ?: effectedPokemon
        return if (actor is PokemonBattleActor || actor is MultiPokemonBattleActor) {
            displayPokemon.getDisplayName()
        } else {
            battleLang("owned_pokemon", actor.getName(), displayPokemon.getDisplayName())
        }
    }

    fun getBoostMultiplier(stat: Stats): Double {
        return when (boosts[stat]) {
            -6 -> 2.0 / 8.0
            -5 -> 2.0 / 7.0
            -4 -> 2.0 / 6.0
            -3 -> 2.0 / 5.0
            -2 -> 2.0 / 4.0
            -1 -> 2.0 / 3.0
            0 -> 2.0 / 2.0
            1 -> 3.0 / 2.0
            2 -> 4.0 / 2.0
            3 -> 5.0 / 2.0
            4 -> 6.0 / 2.0
            5 -> 7.0 / 2.0
            6 -> 8.0 / 2.0
            else -> 1.0
        }
    }

    fun toBattleDTO(ally: Boolean, forceReveal: Boolean = true): BattlePokemonDTO {
        val moves = this.revealedMoves.toMutableList()
        for (i in 0 until 4) {
            if (moves[i] == null) {
                val move = effectedPokemon.moveSet.getMovesWithNulls()[i]
                if (move == null) continue
                moves[i] = MoveDTO(move.displayName, 0)
            }
        }

        val boostMultipliers = boosts.mapValues { getBoostMultiplier(it.key) }.filter { it.value != 1.0 }

        val isTeamSheet = ::actor.isInitialized && actor.battle.format.isOpenTeamSheet
        if (ally || isTeamSheet) {
            val allyDto = BattleInitializePacket.ActiveBattlePokemonDTO.fromPokemon(this, true, getIllusion())
            return BattlePokemonDTO(
                this.effectedPokemon.uuid,
                this.effectedPokemon.isFainted(),
                this.effectedPokemon.ability.name,
                moves,
                this.effectedPokemon.heldItem,
                boostMultipliers,
                effectedPokemon.speed,
                terastallized = terastallized,
                allyDto
            )
        }
        else {
            val nonAllyDto = BattleInitializePacket.ActiveBattlePokemonDTO.fromPokemon(this, false, getIllusion())
            return BattlePokemonDTO(
                this.effectedPokemon.uuid,
                this.effectedPokemon.isFainted(),
                revealedAbility,
                revealedMoves,
                revealedHeldItem,
                boostMultipliers,
                speed = null,
                terastallized = terastallized,
                if (forceReveal || revealed) nonAllyDto else null
            )
        }
    }

    fun updateBattleActorInformation(uuids: List<UUID>) {
        uuids.forEach {
            if (actor.uuid == it || actor.battle.format.isOpenTeamSheet) {
                it.getPlayer()?.sendPacket(
                    BattleActorInformationPacket(actor.uuid, toBattleDTO(true))
                )
            }
            else {
                it.getPlayer()?.sendPacket(
                    BattleActorInformationPacket(actor.uuid, toBattleDTO(false))
                )
            }
        }
    }

    fun sendUpdate() {
        actor.sendUpdate(BattleUpdateTeamPokemonPacket(effectedPokemon))
        this.revealed = true
        val uuids = actor.battle.actors.map { it.uuid } + actor.battle.spectators
        updateBattleActorInformation(uuids)
    }

    fun isSentOut() = actor.battle.activePokemon.any { it.battlePokemon == this }

    fun canBeSentOut(): Boolean {
        val reviving = actor.request?.side?.pokemon?.any { it.reviving } == true
        println("${actor.getName().string} ${originalPokemon.species.name} canBeSentOut: reviving: $reviving isSentOut: ${isSentOut()} willBeSwitchedIn: $willBeSwitchedIn health: $health")

        if (reviving) {
            return !isSentOut() && !willBeSwitchedIn && health <= 0
        } else {
            return !isSentOut() && !willBeSwitchedIn && health > 0
        }
    }

    fun setBattleFeature(formName: String, enabled: Boolean) {
        this.effectedPokemon.getFeature<BattleFormFeature>(formName)?.let {
            it.enabled = enabled
            this.effectedPokemon.updateAspects()
        }
    }

    fun clearBattleFeatures() {
        var modified = false

        this.effectedPokemon.features.forEach {

            if (it is BattleFormFeature) {
                it.enabled = false
                modified = true
            }
        }

        if (modified) {
            this.effectedPokemon.updateAspects()
        }
    }

    fun getIllusion(): BattlePokemon? {
        if (!::actor.isInitialized) return null
        return this.actor.activePokemon.find { it.battlePokemon == this }?.illusion
    }
}
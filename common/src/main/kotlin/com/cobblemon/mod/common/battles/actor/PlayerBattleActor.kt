/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.actor

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonNetwork
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleChoiceRequestedEvent
import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.api.pokemon.experience.BattleExperienceSource
import com.cobblemon.mod.common.api.text.lightPurple
import com.cobblemon.mod.common.api.text.plus
import com.cobblemon.mod.common.api.text.red
import com.cobblemon.mod.common.api.text.yellow
import com.cobblemon.mod.common.battles.ShowdownActionResponse
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.battles.timers.PlayerBattleTimer
import com.cobblemon.mod.common.battles.timers.ShowdownTimer
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket
import com.cobblemon.mod.common.net.messages.client.battle.BattleMessagePacket
import com.cobblemon.mod.common.net.messages.client.battle.BattleMusicPacket
import com.cobblemon.mod.common.util.battleLang
import com.cobblemon.mod.common.util.getPlayer
import java.util.UUID
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.phys.Vec3

class PlayerBattleActor(
    uuid: UUID,
    pokemonList: List<BattlePokemon>,
) : BattleActor(uuid, pokemonList.toMutableList()), EntityBackedBattleActor<ServerPlayer> {

    override val initialPos: Vec3?
    override val entity: ServerPlayer?
        get() = this.uuid.getPlayer()
    init {
        initialPos = entity?.position();
    }

    /** The [ResourceLocation] to play to the player during a battle. Will start playing as soon as the battle starts. */
    var battleTheme: ResourceLocation? = null
        set(value) {
            if (this.isInitialized() && this.battle.started) this.sendUpdate(BattleMusicPacket(value))
            field = value
        }

    var timer: PlayerBattleTimer? = null

    override fun getName(): MutableComponent = this.entity?.name?.copy() ?: "Offline Player".red()
    override fun nameOwned(name: String): MutableComponent = battleLang("owned_pokemon", this.getName(), name)
    override val type = ActorType.PLAYER
    override fun getPlayerUUIDs() = setOf(uuid)
    override fun awardExperience(battlePokemon: BattlePokemon, experience: Int) {
        if (battle.isPvP && !Cobblemon.config.allowExperienceFromPvP) {
            return
        }

        val source = BattleExperienceSource(battle, battlePokemon.facedOpponents.toList())
        if (battlePokemon.effectedPokemon == battlePokemon.originalPokemon && experience > 0) {
            uuid.getPlayer()
                ?.let { battlePokemon.effectedPokemon.addExperienceWithPlayer(it, source, experience) }
                ?: run { battlePokemon.effectedPokemon.addExperience(source, experience) }
        }
    }

    override fun turn() {
        super.turn()
    }

    private fun hasStreamerBattleMode(player: ServerPlayer): Boolean {
        return Cobblemon.permissionValidator.hasPermission(player, "battle.mode.streamer", 4)
    }

    override fun setActionResponses(responses: List<ShowdownActionResponse>) {
        super.setActionResponses(responses)
        val player = uuid.getPlayer() ?: return
        battle.players.filter { it != uuid.getPlayer() && hasStreamerBattleMode(it) }
            .forEach {
                val message = "[Streamer Mode] ".lightPurple() + "${player.name.string} has ended their turn.".yellow()
                it.sendPacket(BattleMessagePacket(listOf(message)))
            }
    }

    override fun sendUpdate(packet: NetworkPacket<*>) {
        CobblemonNetwork.sendPacketToPlayers(getPlayerUUIDs().mapNotNull { it.getPlayer() }, packet)

        if (packet is BattleMakeChoicePacket) {
            val player = this.entity ?: return
            CobblemonEvents.BATTLE_CHOICE_REQUESTED.post(BattleChoiceRequestedEvent(player, packet))
        }
    }
}

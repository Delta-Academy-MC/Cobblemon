/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.interaction

import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.api.pokemon.requirement.Requirement
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.client.sound.UnvalidatedPlaySoundS2CPacket
import com.cobblemon.mod.common.pokemon.adapters.CobblemonRequirementAdapter
import com.cobblemon.mod.common.util.*
import com.google.gson.annotations.SerializedName
import net.minecraft.core.registries.Registries
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack

data class PokemonInteractionSet(
    val requirements: List<Requirement> = listOf(),
    val interactions: List<PokemonInteraction> = listOf(),
) {
    fun encode(buffer: FriendlyByteBuf) {
        buffer.writeCollection(requirements) { it, requirement ->
            CobblemonRequirementAdapter.encode(requirement, it)
        }
        buffer.writeCollection(interactions) { it, interaction ->
            interaction.encode(it)
        }
    }

    companion object {
        fun decode(buffer: FriendlyByteBuf): PokemonInteractionSet {
            val requirements = buffer.readList(CobblemonRequirementAdapter::decode)
            val interactions = buffer.readList(PokemonInteraction::decode)
            return PokemonInteractionSet(requirements, interactions)
        }
    }
}

data class PokemonInteraction(
    val grouping: ResourceLocation,
    val requirements: List<Requirement> = listOf(),
    val effects: List<InteractionEffect> = listOf(),
    val cooldown: ExpressionLike = "0".asExpressionLike()
) {
    fun encode(buffer: FriendlyByteBuf) {
        buffer.writeResourceLocation(grouping)
        buffer.writeCollection(requirements) { it, requirement ->
            CobblemonRequirementAdapter.encode(requirement, it)
        }
        buffer.writeExpressionLike(cooldown)
    }

    companion object {
        fun decode(buffer: FriendlyByteBuf): PokemonInteraction {
            val grouping = buffer.readResourceLocation()
            val requirements = buffer.readList { it ->
                CobblemonRequirementAdapter.decode(it)
            }

            // No need to sync these for now
            val effects = emptyList<InteractionEffect>()
            val cooldown = buffer.readExpressionLike()

            return PokemonInteraction(
                grouping,
                requirements,
                effects,
                cooldown,
            )
        }
    }
}

interface InteractionEffect {
    fun applyEffect(pokemon: PokemonEntity, player: ServerPlayer)
}

class DropItemEffect(val item: ResourceLocation, val amount: IntRange?) : InteractionEffect {
    override fun applyEffect(
        pokemon: PokemonEntity,
        player: ServerPlayer
    ) {
        val item = player.registryAccess().registryOrThrow(Registries.ITEM).get(item) ?: throw IllegalArgumentException(
            "Cannot load item with id: $item"
        )
        val stack = ItemStack(item)
        stack.count = amount?.randomOrNull() ?: 1
        if (stack.isEmpty)
            return
        val height: Double = pokemon.eyeY - 0.3
        val itemEntity = ItemEntity(pokemon.level(), pokemon.x, height, pokemon.z, stack)
        itemEntity.setPickUpDelay(40)
        itemEntity.setThrower(pokemon)
        pokemon.level().addFreshEntity(itemEntity)
    }

    companion object {
        val ID = "drop_item"
    }
}

class GiveItemEffect(val item: ResourceLocation, val amount: IntRange?) : InteractionEffect {
    override fun applyEffect(
        pokemon: PokemonEntity,
        player: ServerPlayer
    ) {
        val item = player.registryAccess().registryOrThrow(Registries.ITEM).get(item) ?: throw IllegalArgumentException(
            "Cannot load item with id: $item"
        )
        val stack = ItemStack(item)
        stack.count = amount?.randomOrNull() ?: 1
        if (stack.isEmpty)
            return
        player.giveOrDropItemStack(stack)
    }

    companion object {
        val ID = "give_item"
    }
}

class PlaySoundEffect(
    val sound: ResourceLocation,
    val soundSource: SoundSource?,
    @SerializedName("playAround") private val _playAround: Boolean?,
    @SerializedName("distance") private val _distance: Double?,
    @SerializedName("volume") private val _volume: Float?,
    @SerializedName("pitch") private val _pitch: Float?,
) : InteractionEffect {

    override fun applyEffect(
        pokemon: PokemonEntity,
        player: ServerPlayer
    ) {
        val packet = UnvalidatedPlaySoundS2CPacket(
            sound,
            soundSource ?: SoundSource.NEUTRAL,
            pokemon.x,
            pokemon.y,
            pokemon.z,
            volume,
            pitch
        )

        if (playAround) {
            packet.sendToPlayersAround(pokemon.x, pokemon.y, pokemon.z, distance, pokemon.level().dimension())
        } else {
            packet.sendToPlayer(player)
        }
    }

    val playAround: Boolean
        get() = _playAround != false

    val distance: Double
        get() = _distance ?: 64.0

    val volume: Float
        get() = _volume ?: 1.0F

    val pitch: Float
        get() = _pitch ?: 1.0F

    companion object {
        val ID = "play_sound"
    }
}

class ShrinkItemEffect(val amount: Int = 1) : InteractionEffect {
    override fun applyEffect(
        pokemon: PokemonEntity,
        player: ServerPlayer
    ) {
        if (player.getItemInHand(InteractionHand.MAIN_HAND).isDamageableItem)
            player.getItemInHand(InteractionHand.MAIN_HAND).hurtAndBreak(amount, player, EquipmentSlot.MAINHAND)
        else
            player.getItemInHand(InteractionHand.MAIN_HAND).consume(amount, player)
    }

    companion object {
        val ID = "shrink_item"
    }
}

class ScriptEffect(val script: ExpressionLike) : InteractionEffect {
    override fun applyEffect(
        pokemon: PokemonEntity,
        player: ServerPlayer
    ) {
        val runtime = MoLangRuntime().setup()
        runtime.withPlayerValue("player", player)
        runtime.withQueryValue("pokemon", pokemon.asMoLangValue())
        script.resolveDouble(runtime)
    }

    companion object {
        val ID = "script"
    }
}
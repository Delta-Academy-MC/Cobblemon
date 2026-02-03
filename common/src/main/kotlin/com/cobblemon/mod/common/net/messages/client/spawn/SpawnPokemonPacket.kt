/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.spawn

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.mark.Marks
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.entity.PlatformType
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readEnumConstant
import com.cobblemon.mod.common.util.readIdentifier
import com.cobblemon.mod.common.util.readString
import com.cobblemon.mod.common.util.readText
import com.cobblemon.mod.common.util.writeEnumConstant
import com.cobblemon.mod.common.util.writeIdentifier
import com.cobblemon.mod.common.util.writeString
import com.cobblemon.mod.common.util.writeText
import java.util.UUID
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

class SpawnPokemonPacket(
    var ownerId: UUID?,
    var pokemonUUID: UUID,
    var storageUUID: UUID?,
    var scaleModifier: Float,
    var speciesId: ResourceLocation,
    var gender: Gender,
    var shiny: Boolean,
    var formName: String,
    var aspects: Set<String>,
    var battleId: UUID?,
    var phasingTargetId: Int,
    var beamMode: Byte,
    var platform: PlatformType,
    var nickname: MutableComponent?,
    var mark: ResourceLocation?,
    var labelLevel: Int,
    var poseType: PoseType,
    var unbattlable: Boolean,
    var hideLabel: Boolean,
    var caughtBall: ResourceLocation,
    var spawnYaw: Float,
    var friendship: Int,
    var freezeFrame: Float,
    var tickSpawned: Int,
    var rideBoosts: Map<RidingStat, Float>,
    var rideStamina: Float,
    var silent: Boolean,
    vanillaSpawnPacket: ClientboundAddEntityPacket,
) : SpawnExtraDataEntityPacket<SpawnPokemonPacket, PokemonEntity>(vanillaSpawnPacket) {

    override val id: ResourceLocation = ID

    constructor(entity: PokemonEntity, vanillaSpawnPacket: ClientboundAddEntityPacket) : this(
        entity.ownerUUID,
        entity.pokemon.uuid,
        entity.pokemon.storeCoordinates.get()?.store?.uuid,
        entity.pokemon.scaleModifier,
        entity.exposedSpecies.resourceIdentifier,
        entity.pokemon.gender,
        entity.pokemon.shiny,
        entity.exposedForm.formOnlyShowdownId(),
        entity.exposedAspects,
        entity.battleId,
        entity.phasingTargetId,
        entity.beamMode.toByte(),
        entity.platform,
        entity.pokemon.nickname,
        entity.pokemon.activeMark?.identifier,
        if (Cobblemon.config.displayEntityLevelLabel) entity.entityData.get(PokemonEntity.LABEL_LEVEL) else -1,
        entity.entityData.get(PokemonEntity.POSE_TYPE),
        entity.entityData.get(PokemonEntity.UNBATTLEABLE),
        entity.entityData.get(PokemonEntity.HIDE_LABEL),
        entity.exposedBall.name,
        entity.entityData.get(PokemonEntity.SPAWN_DIRECTION),
        entity.entityData.get(PokemonEntity.FRIENDSHIP),
        entity.entityData.get(PokemonEntity.FREEZE_FRAME),
        entity.tickCount,
        entity.entityData.get(PokemonEntity.RIDE_BOOSTS),
        entity.entityData.get(PokemonEntity.RIDE_STAMINA),
        entity.isSilent,
        vanillaSpawnPacket
    )

    override fun encodeEntityData(buffer: RegistryFriendlyByteBuf) {
        buffer.writeNullable(ownerId) { _, v -> buffer.writeUUID(v) }
        buffer.writeUUID(this.pokemonUUID)
        buffer.writeNullable(this.storageUUID) { pb, value -> pb.writeUUID(value) }
        buffer.writeFloat(this.scaleModifier)
        buffer.writeIdentifier(this.speciesId)
        buffer.writeEnumConstant(this.gender)
        buffer.writeBoolean(this.shiny)
        buffer.writeString(this.formName)
        buffer.writeCollection(this.aspects) { pb, value -> pb.writeString(value) }
        buffer.writeNullable(this.battleId) { pb, value -> pb.writeUUID(value) }
        buffer.writeInt(this.phasingTargetId)
        buffer.writeByte(this.beamMode.toInt())
        buffer.writeEnumConstant(this.platform)
        buffer.writeNullable(this.nickname) { _, v -> buffer.writeText(v) }
        buffer.writeNullable(this.mark) { _, v -> buffer.writeResourceLocation(v) }
        buffer.writeInt(this.labelLevel)
        buffer.writeEnumConstant(this.poseType)
        buffer.writeBoolean(this.unbattlable)
        buffer.writeBoolean(this.hideLabel)
        buffer.writeIdentifier(this.caughtBall)
        buffer.writeFloat(this.spawnYaw)
        buffer.writeInt(this.friendship)
        buffer.writeFloat(this.freezeFrame)
        buffer.writeInt(this.tickSpawned)
        buffer.writeMap(
            rideBoosts,
            { _, stat -> buffer.writeEnumConstant(stat) },
            { _, value -> buffer.writeFloat(value) }
        )
        buffer.writeFloat(rideStamina)
        buffer.writeBoolean(silent)
    }

    override fun applyData(entity: PokemonEntity, level: ClientLevel) {
        entity.ownerUUID = ownerId

        // Reuse the stored Pokémon instance to avoid creating a duplicate,
        // preventing client/entity desync issues.
        this@SpawnPokemonPacket.storageUUID?.let {
            val storedPokemon = CobblemonClient.storage.locatePokemon(it, this@SpawnPokemonPacket.pokemonUUID)
            if (storedPokemon != null) {
                entity.pokemon = storedPokemon
            }
        }

        entity.pokemon.apply {
            uuid = this@SpawnPokemonPacket.pokemonUUID
            scaleModifier = this@SpawnPokemonPacket.scaleModifier
            species =
                this@SpawnPokemonPacket.speciesId.let { PokemonSpecies.getByIdentifier(it) ?: PokemonSpecies.random() }
            gender = this@SpawnPokemonPacket.gender
            shiny = this@SpawnPokemonPacket.shiny
            form =
                this@SpawnPokemonPacket.formName.let { formName -> species.forms.find { it.formOnlyShowdownId() == formName } }
                    ?: species.standardForm
            forcedAspects = this@SpawnPokemonPacket.aspects
            nickname = this@SpawnPokemonPacket.nickname
            this@SpawnPokemonPacket.mark?.let { activeMark = Marks.getByIdentifier(it) }
            PokeBalls.getPokeBall(this@SpawnPokemonPacket.caughtBall)?.let { caughtBall = it }
        }
        entity.phasingTargetId = this.phasingTargetId
        entity.beamMode = this.beamMode.toInt()
        entity.platform = this.platform
        entity.battleId = this.battleId
        entity.entityData.set(PokemonEntity.LABEL_LEVEL, labelLevel)
        entity.entityData.set(PokemonEntity.SPECIES, entity.pokemon.species.resourceIdentifier.toString())
        entity.entityData.set(PokemonEntity.ASPECTS, aspects)
        entity.entityData.set(PokemonEntity.POSE_TYPE, poseType)
        entity.entityData.set(PokemonEntity.UNBATTLEABLE, unbattlable)
        entity.entityData.set(PokemonEntity.HIDE_LABEL, hideLabel)
        entity.entityData.set(PokemonEntity.SPAWN_DIRECTION, spawnYaw)
        entity.entityData.set(PokemonEntity.FRIENDSHIP, friendship)
        entity.entityData.set(PokemonEntity.FREEZE_FRAME, freezeFrame)
        entity.entityData.set(PokemonEntity.RIDE_BOOSTS, rideBoosts)
        entity.entityData.set(PokemonEntity.RIDE_STAMINA, rideStamina)
        entity.entityData.set(PokemonEntity.SCALE_MODIFIER, scaleModifier)
        entity.isSilent = silent

        entity.tickSpawned = this.tickSpawned
        entity.delegate.updateAge(this.tickSpawned)
    }

    override fun checkType(entity: Entity): Boolean = entity is PokemonEntity

    companion object {
        val ID = cobblemonResource("spawn_pokemon_entity")
        fun decode(buffer: RegistryFriendlyByteBuf): SpawnPokemonPacket {
            val ownerId = buffer.readNullable { buffer.readUUID() }
            val pokemonUUID = buffer.readUUID()
            val storageUUID = buffer.readNullable { buffer.readUUID() }
            val scaleModifier = buffer.readFloat()
            val speciesId = buffer.readIdentifier()
            val gender = buffer.readEnumConstant(Gender::class.java)
            val shiny = buffer.readBoolean()
            val formName = buffer.readString()
            val aspects = buffer.readList { it.readString() }.toSet()
            val battleId = buffer.readNullable { buffer.readUUID() }
            val phasingTargetId = buffer.readInt()
            val beamModeEmitter = buffer.readByte()
            val platform = buffer.readEnumConstant(PlatformType::class.java)
            val nickname = buffer.readNullable { buffer.readText().copy() }
            val mark = buffer.readNullable { buffer.readResourceLocation() }
            val labelLevel = buffer.readInt()
            val poseType = buffer.readEnumConstant(PoseType::class.java)
            val unbattlable = buffer.readBoolean()
            val hideLabel = buffer.readBoolean()
            val caughtBall = buffer.readIdentifier()
            val spawnAngle = buffer.readFloat()
            val friendship = buffer.readInt()
            val freezeFrame = buffer.readFloat()
            val tickSpawned = buffer.readInt()
            val rideBoosts = buffer.readMap(
                { buffer.readEnumConstant(RidingStat::class.java) },
                { buffer.readFloat() }
            )
            val rideStamina = buffer.readFloat()
            val silent = buffer.readBoolean()
            val vanillaPacket = decodeVanillaPacket(buffer)

            return SpawnPokemonPacket(
                ownerId,
                pokemonUUID,
                storageUUID,
                scaleModifier,
                speciesId,
                gender,
                shiny,
                formName,
                aspects,
                battleId,
                phasingTargetId,
                beamModeEmitter,
                platform,
                nickname,
                mark,
                labelLevel,
                poseType,
                unbattlable,
                hideLabel,
                caughtBall,
                spawnAngle,
                friendship,
                freezeFrame,
                tickSpawned,
                rideBoosts,
                rideStamina,
                silent,
                vanillaPacket
            )
        }
    }

}
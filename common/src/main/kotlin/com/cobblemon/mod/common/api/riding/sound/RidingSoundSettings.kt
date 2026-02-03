/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.sound

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.api.net.Encodable
import com.cobblemon.mod.common.util.asExpression
import com.cobblemon.mod.common.util.readExpression
import com.cobblemon.mod.common.util.writeExpression
import com.google.gson.annotations.SerializedName
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation

/**
 * Class to store data for looping sounds played during riding.
 *
 * @author Jackowes
 * @since April 26th, 2025
 */
data class RideSoundSettings(
    val soundLocation: ResourceLocation,
    @SerializedName("volumeExpr") private val _volumeExpr: Expression? = "1.0".asExpression(),
    @SerializedName("pitchExpr") private val _pitchExpr: Expression? = "1.0".asExpression(),
    val playForPassengers: Boolean = true,
    val playForNonPassengers: Boolean = false,
    val submerged: Boolean = false,
    val muffleEnabled: Boolean = false,
    @SerializedName("attenuationModel") private val _attenuationModel: RideAttenuationModel? = RideAttenuationModel.NONE
): Encodable {

    //this is needed because GSON does NOT support default constructor parameters and will just happily throw nulls all over the place
    val volumeExpr: Expression
        get() = _volumeExpr ?: "1.0".asExpression()

    val pitchExpr: Expression
        get() = _pitchExpr ?: "1.0".asExpression()

    val attenuationModel: RideAttenuationModel
        get() = _attenuationModel ?: RideAttenuationModel.NONE

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeResourceLocation(soundLocation)
        buffer.writeExpression(volumeExpr)
        buffer.writeExpression(pitchExpr)
        buffer.writeBoolean(playForPassengers)
        buffer.writeBoolean(playForNonPassengers)
        buffer.writeBoolean(submerged)
        buffer.writeBoolean(muffleEnabled)
        buffer.writeEnum(attenuationModel)
    }

    companion object {
        fun decode(buffer: RegistryFriendlyByteBuf): RideSoundSettings {

            val soundLocation = buffer.readResourceLocation()
            val volumeExpr = buffer.readExpression()
            val pitchExpr = buffer.readExpression()
            val playForPassengers = buffer.readBoolean()
            val playForNonPassengers = buffer.readBoolean()
            val submerged = buffer.readBoolean()
            val muffleEnabled = buffer.readBoolean()
            val attenuationModel = buffer.readEnum(RideAttenuationModel::class.java)

            return RideSoundSettings(
                soundLocation,
                volumeExpr,
                pitchExpr,
                playForPassengers,
                playForNonPassengers,
                submerged,
                muffleEnabled,
                attenuationModel
            )
        }
    }
}

class RideSoundSettingsList(var sounds: List<RideSoundSettings> = mutableListOf()) {

    fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeCollection(sounds) { _, sound -> sound.encode(buffer) }
    }

    companion object {
        fun decode(buffer: RegistryFriendlyByteBuf): RideSoundSettingsList {
            val list = RideSoundSettingsList()
            list.sounds = buffer.readList { RideSoundSettings.decode(buffer) }.toMutableList()
            return list
        }
    }
}

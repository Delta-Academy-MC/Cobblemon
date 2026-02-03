/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.persisted

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.Cobblemon.LOGGER
import com.cobblemon.mod.common.config.CobblemonConfig
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.file.Paths
import kotlin.io.path.exists
import net.minecraft.client.CameraType
import net.minecraft.resources.ResourceLocation

object ClientPersistedData {

    val gson = CobblemonConfig.GSON

    val snapshotsPath = Paths.get("cobblemon").resolve("snapshots.json")
    val ridingPerspectivesPath = Paths.get("cobblemon").resolve("riding_perspectives.json")

    var ridingPerspectives: RidingPerspectiveData = readRidingPerspectivesData() ?: RidingPerspectiveData()

    fun readSnapshotsData() : SnapshotAcknowledgementData? {
        if (snapshotsPath.exists()) {
            BufferedReader(InputStreamReader(FileInputStream(snapshotsPath.toFile()))).use { reader ->
                try {
                    return gson.fromJson(reader, SnapshotAcknowledgementData::class.java)
                } catch (exception: Exception) {
                    LOGGER.error("Failed to load snapshots.json... Resetting File")
                    val data = SnapshotAcknowledgementData(Cobblemon.VERSION, false)
                    writeSnapshotsData(data)
                    return data
                }
            }
        }

        return null
    }

    fun writeSnapshotsData(data: SnapshotAcknowledgementData) {
        snapshotsPath.toFile().parentFile.mkdirs()
        BufferedWriter(FileWriter(snapshotsPath.toFile())).use { writer -> gson.toJson(data, writer) }
    }

    fun readRidingPerspectivesData() : RidingPerspectiveData? {
        if (ridingPerspectivesPath.exists()) {
            BufferedReader(InputStreamReader(FileInputStream(ridingPerspectivesPath.toFile()))).use { reader ->
                try {
                    return gson.fromJson(reader, RidingPerspectiveData::class.java)
                } catch (exception: Exception) {
                    LOGGER.error("Failed to load riding_perspectives.json... Resetting File")
                    val data = RidingPerspectiveData()
                    saveRidingPerspectivesData(data)
                    return data
                }
            }
        }

        return null
    }

    fun saveRidingPerspectivesData(data: RidingPerspectiveData) {
        ridingPerspectivesPath.toFile().parentFile.mkdirs()
        BufferedWriter(FileWriter(ridingPerspectivesPath.toFile())).use { writer -> gson.toJson(data, writer) }
    }

    data class SnapshotAcknowledgementData(val version: String, val dontShowAgain: Boolean)
    data class RidingPerspectiveData(
        val perspectives: MutableMap<ResourceLocation, CameraType> = mutableMapOf()
    ) {
        fun updatePerspective(key: ResourceLocation, cameraType: CameraType) {
            perspectives[key] = cameraType
            saveRidingPerspectivesData(this)
        }
    }
}
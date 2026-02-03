/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util.adapters

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.Environment
import com.cobblemon.mod.common.util.server
import com.google.gson.JsonElement
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import net.minecraft.resources.RegistryOps

/**
 * Utility for obtaining RegistryOps with the correct client/server registry access.
 * Used when manually encoding/decoding registry-backed values (e.g., items, predicates).
 */
object RegistryOpsProvider {
    fun getOpsWithDefaultFallback(): DynamicOps<JsonElement> {
        if (Cobblemon.implementation.environment() == Environment.CLIENT) {
            val minecraft = Minecraft.getInstance()
            if (minecraft != null && minecraft.level != null)
                return RegistryOps.create(JsonOps.INSTANCE, minecraft.level!!.registryAccess())
        }


        if (Cobblemon.implementation.environment() == Environment.SERVER) {
            val server = server()
            if (server != null)
                return RegistryOps.create(JsonOps.INSTANCE, server.registryAccess())
        }

        return JsonOps.INSTANCE
    }
}
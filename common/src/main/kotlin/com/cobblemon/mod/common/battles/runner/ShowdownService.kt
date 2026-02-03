/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.runner

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService
import com.cobblemon.mod.common.battles.runner.socket.SocketShowdownService
import com.google.gson.JsonArray
import java.util.UUID

/**
 * Mediator service for communicating between the Cobblemon Minecraft mod and Cobblemon showdown service.
 *
 * All outgoing calls to showdown should be done through this layer to decouple the mod from a specific implementation.
 * To get an instance of the service, use the singleton provided via `ShowdownService.get()`
 *
 * @since February 27th, 2023
 * @author landonjw
 */
interface ShowdownService {
    fun openConnection()
    fun closeConnection()
    fun startBattle(battle: PokemonBattle, messages: Array<String>)
    fun endBattle(battle: PokemonBattle)
    fun send(battleId: UUID, messages: Array<String>)
    fun getRegistryData(type: String): JsonArray
    fun sendRegistryData(data: Map<String, String>, type: String)
    fun sendRegistryEntry(data: String, type: String)
    fun resetRegistryData(type: String)
    fun resetAllRegistries()
    fun indicateSpeciesInitialized() {}

    companion object {
        val service: ShowdownService by lazy { GraalShowdownService() }
    }
}
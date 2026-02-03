/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.runner.socket

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.battles.ShowdownInterpreter
import com.cobblemon.mod.common.battles.runner.ShowdownService
import com.google.gson.Gson
import com.google.gson.JsonArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.UUID
import kotlin.text.replace

/**
 * Mediator service for communicating between the Cobblemon Minecraft mod and Cobblemon showdown service via
 * a socket client.
 *
 * This is primarily used for debugging purposes, but could be extended in the future to provide
 * a means of connecting to a remote Showdown server. This does not provide any fault handling in the
 * event that the server goes down.
 *
 * When messages are sent to showdown, this will await a response from showdown.
 * The protocol for messages sent from showdown is <length of characters in payload><payload>,
 * and a payload length of 0 indicates that there is no response.
 *
 * @see {@code sim/cobbled-debug-server.ts} within cobblemon-showdown repository
 * @since February 27, 2023
 * @author landonjw
 */
class SocketShowdownService(val host: String = "localhost", val port: Int = 18468, val localPort: Int = 0) : ShowdownService {

    private lateinit var socket: Socket
    private lateinit var writer: OutputStreamWriter
    private lateinit var reader: BufferedReader
    val gson = Gson()

    override fun openConnection() {
        socket = Socket(InetAddress.getLocalHost(), port, InetAddress.getLocalHost(), localPort)
        writer = socket.getOutputStream().writer(charset = Charset.forName("ascii"))
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    }

    override fun closeConnection() {
        socket.close()
    }

    override fun startBattle(battle: PokemonBattle, messages: Array<String>) {
        writer.write(">startbattle ${battle.battleId}\n")
        acknowledge { Cobblemon.LOGGER.error("Failed to start battle!") }
        send(battle.battleId, messages)
    }

    override fun endBattle(battle: PokemonBattle) {
        writer.write(">endbattle ${battle.battleId}\n")
        acknowledge { Cobblemon.LOGGER.error("Failed to end battle ${battle.battleId}!") }
    }

    override fun send(battleId: UUID, messages: Array<String>) {
        for (message in messages) {
            writer.write("$battleId~$message\n")
            writer.flush()
            readBattleInput().forEach { interpretMessage(battleId, it) }
        }
    }

    private fun read(reader: BufferedReader, size: Int): String {
        val buffer = CharArray(size)
        while (true) {
            if(reader.read(buffer) == 0) continue
            return String(buffer)
        }
    }

    private fun readMessage(): String {
        val payloadSize = read(reader, 8).toInt()
        val payload = read(reader, payloadSize)
        return payload
    }

    private fun readBattleInput(): List<String> {
        val lines = mutableListOf<String>()
        val numLines = read(reader, 8).toInt()
        if (numLines != 0) {
            for (i in 0 until numLines) {
                lines.add(readMessage())
            }
        }
        return lines
    }

    private fun interpretMessage(battleId: UUID, message: String) {
        ShowdownInterpreter.interpretMessage(battleId, message)
    }

    override fun sendRegistryData(data: Map<String, String>, type: String) {
        data.forEach { (key, value) -> sendRegistryEntry(key, value, type) }
        // The code for sending bulk data is commented out below, because:
        // 1) while debugging it's useful to have individual entries, and socket is only used for debug atm
        // 2) the species JSONs are way too big to be handled as one line on the JS side (maybe both sides?)
        // this can eventually be remedied if socket is intended to be used for any actual servers/etc.
        // alternatively, you could get around this limitation by just having ONLY PokemonSpecies send individually,
        // but it's likely that with enough custom data for moves/abilities/etc. you'd run into the same issue

        /*val payload = data.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            val newV = v.replace(Regex("[\r\n]+"), " ")
            "\"$k\": $newV"
        }
        writer.write(">receiveData $type payload")
        acknowledge { Cobblemon.LOGGER.error("Failed to send $type data to Showdown: $data") }*/
    }

    fun sendRegistryEntry(key: String, data: String, type: String) {
        val payload = data.replace(Regex("[\r\n]+"), " ")
        writer.write(">receiveEntry $type $key $payload")
        acknowledge { Cobblemon.LOGGER.error("Failed to send $type data to Showdown: $payload") }
    }

    override fun sendRegistryEntry(data: String, type: String) {
        val payload = data.replace(Regex("[\r\n]+"), " ")
        writer.write(">receiveEntry $type $payload")
        acknowledge { Cobblemon.LOGGER.error("Failed to send $type data to Showdown: $payload") }
    }

    override fun getRegistryData(type: String): JsonArray {
        writer.write(">getData $type")
        writer.flush()
        val response = readMessage()
        return gson.fromJson(response, JsonArray::class.java)
    }

    override fun resetRegistryData(type: String) {
        writer.write(">resetData $type")
        acknowledge()
    }

    override fun resetAllRegistries() {
        writer.write(">resetAll")
        acknowledge()
    }

    fun acknowledge(ifFails: () -> Unit = {}) {
        writer.flush()
        val ack = CharArray(3)
        reader.read(ack)
        if (String(ack) != "ACK") {
            ifFails()
        }
    }

    override fun indicateSpeciesInitialized() {
        writer.write(">afterSpeciesInit")
        acknowledge()
    }

}
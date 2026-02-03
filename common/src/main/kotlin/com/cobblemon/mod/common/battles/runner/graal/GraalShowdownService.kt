/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.battles.runner.graal

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.battles.ShowdownInterpreter
import com.cobblemon.mod.common.battles.runner.ShowdownService
import com.google.gson.Gson
import com.google.gson.JsonArray
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.AccessMode
import java.nio.file.DirectoryStream
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileAttribute
import java.util.UUID
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.HostAccess.Export
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.FileSystem
import kotlin.text.replace

/**
 * Mediator service for communicating between the Cobblemon Minecraft mod and Cobblemon showdown service via
 * GraalVM. This directly invokes JavaScript functions provided within the showdown service.
 *
 * @see {@code cobbled-exports/cobbled-index.js} within cobbledmon-showdown repository
 * @see <a href="https://www.graalvm.org/">
 * @since February 27, 2023
 * @author Hiroku, landonjw
 */
class GraalShowdownService : ShowdownService {

    @Transient
    lateinit var context: Context
    @Transient
    lateinit var sendBattleMessageFunction: Value
    @Transient
    val unbundler = GraalShowdownUnbundler()
    @Transient
    val gson = Gson()

    override fun openConnection() {
        unbundler.attemptUnbundle()
        createContext()
        boot()
    }

    private fun createContext() {
        val wd = Paths.get("./showdown")
        val access = HostAccess.newBuilder(HostAccess.EXPLICIT)
            .allowIterableAccess(true)
            .allowArrayAccess(true)
            .allowListAccess(true)
            .allowMapAccess(true)
            .build()
        context = Context.newBuilder("js")
            .allowIO(true)
            .fileSystem(object : FileSystem
                {
                    val default = FileSystem.newDefaultFileSystem()
                    override fun parsePath(uri: URI) = default.parsePath(uri)
                    override fun parsePath(path: String) = default.parsePath(path)
                    override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) = default.createDirectory(dir, *attrs)
                    override fun delete(path: Path) = default.delete(path)
                    override fun newByteChannel(path: Path, options: MutableSet<out OpenOption>, vararg attrs: FileAttribute<*>) = default.newByteChannel(path, options, *attrs)
                    override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>) = default.newDirectoryStream(dir, filter)
                    override fun toAbsolutePath(path: Path) = default.toAbsolutePath(path)
                    override fun toRealPath(path: Path, vararg linkOptions: LinkOption) = default.toRealPath(path, *linkOptions)
                    override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption) = default.readAttributes(path, attributes, *options)
                    override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) {
                        if (!path.toRealPath(LinkOption.NOFOLLOW_LINKS).startsWith(wd.toRealPath(LinkOption.NOFOLLOW_LINKS))) {
                            Cobblemon.LOGGER.error("Hacked JS files in datapacks or some weird file system setup that Hiroku failed to anticipate.")
                            throw IOException("Someone has put hacked JS files into datapacks because file access is being attempted outside of controlled folders.")
                        }
                    }
                }
            )
            .allowExperimentalOptions(true)
            .allowPolyglotAccess(PolyglotAccess.ALL)
            .allowHostAccess(access)
            .allowCreateThread(true)
            .logHandler(GraalLogger)
            .option("engine.WarnInterpreterOnly", "false")
            .option("js.commonjs-require", "true")
            .option("js.commonjs-require-cwd", "showdown")
            .option(
                "js.commonjs-core-modules-replacements",
                "buffer:buffer/,crypto:crypto-browserify,path:path-browserify"
            )
            .allowHostClassLoading(true)
            .allowNativeAccess(true)
            .allowCreateProcess(true)
            .build()

        context.eval("js", """
            globalThis.process = {
                cwd: function() {
                    return '';
                }
            }
        """.trimIndent())
    }

    override fun closeConnection() {
        context.close()
    }

    private fun boot() {
        context.eval("js", File("showdown/index.js").readText())
        sendBattleMessageFunction = context.getBindings("js").getMember("sendBattleMessage")
    }

    override fun startBattle(battle: PokemonBattle, messages: Array<String>) {
        val startBattleFunction = context.getBindings("js").getMember("startBattle")
        startBattleFunction.execute(this, battle.battleId.toString(), messages)
    }

    override fun endBattle(battle: PokemonBattle) {
        val endBattleFunction = context.getBindings("js").getMember("endBattle")
        endBattleFunction.execute(battle.battleId.toString())
    }

    override fun send(battleId: UUID, messages: Array<String>) {
        sendToShowdown(battleId, messages)
    }

    override fun getRegistryData(type: String): JsonArray {
        val func = context.getBindings("js").getMember("getData")
        val result = func.execute(type).asString()
        return gson.fromJson(result, JsonArray::class.java)
    }

    override fun sendRegistryData(data: Map<String, String>, type: String) {
        val payload = data.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            val newV = v.replace(Regex("[\r\n]+"), " ")
            "\"$k\": $newV"
        }
        val func = this.context.getBindings("js").getMember("receiveData")
        func.execute(payload, type)
    }

    override fun sendRegistryEntry(data: String, type: String) {
        val payload = data.replace(Regex("[\r\n]+"), " ")
        val func = this.context.getBindings("js").getMember("receiveData")
        func.execute(payload, type)
    }

    override fun resetRegistryData(type: String) {
        this.context.getBindings("js").getMember("resetData").execute(type)
    }

    override fun resetAllRegistries() {
        this.context.getBindings("js").getMember("resetAll").execute()
    }

    override fun indicateSpeciesInitialized() {
        val func = this.context.getBindings("js").getMember("afterSpeciesInit")
        func.execute()
    }

    private fun sendToShowdown(battleId: UUID, messages: Array<String>) {
        sendBattleMessageFunction.execute(battleId.toString(), messages)
    }

    @Export
    fun sendFromShowdown(battleId: String, message: String) {
        ShowdownInterpreter.interpretMessage(UUID.fromString(battleId), message)
    }

    @Export
    fun log(message: String) {
        Cobblemon.LOGGER.info(message)
    }
}
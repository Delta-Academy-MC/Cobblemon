/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.adapter.flatfile

import com.cobblemon.mod.common.Cobblemon.LOGGER
import com.cobblemon.mod.common.api.storage.PokemonStore
import com.cobblemon.mod.common.api.storage.StorePosition
import com.cobblemon.mod.common.api.storage.adapter.CobblemonAdapterParent
import java.io.File
import java.util.UUID
import net.minecraft.core.RegistryAccess

/**
 * A subset of [FileStoreAdapter] that make predictable use of files based on the implementation of [rootFolder],
 * [useNestedFolders], [folderPerClass], and [fileExtension].
 *
 * @property rootFolder The root folder for these storages, such as "data/mystores/"
 * @property useNestedFolders Whether the stores in a folder should be nested within folders named after the first
 * two characters of the UUID. This makes it easier to access files on an FTP connection by drastically reducing the
 * number of stores in a single folder. For example, a store with UUID 380df991-f603-344c-a090-369bad2a924a would be
 * located under the root folder in {rootFolder}/38/{fileName}.
 * @property folderPerClass Whether different types of store will be saved in different folders beneath the root. If this
 * is false, stores will save with a suffix denoting what class of store they are so that they can be differentiated.
 * @property fileExtension The file extension, such as json or dat, that will be appended to the file.
 * @param S The serialized form of a storage. This is what will be constructed synchronously, while the saving
 *             may occur asynchronously.
 *
 * @author Hiroku
 * @since November 30th, 2021
 */
abstract class OneToOneFileStoreAdapter<S>(
    private val rootFolder: String,
    private val useNestedFolders: Boolean,
    private val folderPerClass: Boolean,
    private val fileExtension: String
) : FileStoreAdapter<S>, CobblemonAdapterParent<S>() {
    abstract fun save(file: File, serialized: S)
    abstract fun <E, T : PokemonStore<E>> load(file: File, storeClass: Class<out T>, uuid: UUID, registryAccess: RegistryAccess): T?

    fun getFile(storeClass: Class<out PokemonStore<*>>, uuid: UUID): File {
        val className = storeClass.simpleName.lowercase()
        val subfolder1 = if (folderPerClass) "$className/" else ""
        val subfolder2 = if (useNestedFolders) "${uuid.toString().substring(0, 2)}/" else ""
        val folder = if (!rootFolder.endsWith("/")) "$rootFolder/" else rootFolder
        val fileName = if (folderPerClass) "$uuid.$fileExtension" else "$uuid-$className.$fileExtension"
        val file = File(folder + subfolder1 + subfolder2, fileName)
        file.parentFile.mkdirs()
        return file
    }

    override fun save(storeClass: Class<out PokemonStore<*>>, uuid: UUID, serialized: S) {
        val file = getFile(storeClass, uuid)
        val tempFile = File(file.absolutePath + ".temp")
        val oldFile = File(file.absolutePath + ".old")
        tempFile.createNewFile()
        save(tempFile, serialized)
        if (file.exists()) {
            file.copyTo(oldFile, overwrite = true)
        }
        tempFile.copyTo(file, overwrite = true)
        tempFile.delete()
    }

    override fun <E : StorePosition, T : PokemonStore<E>> provide(storeClass: Class<T>, uuid: UUID, registryAccess: RegistryAccess): T? {
        val file = getFile(storeClass, uuid)
        val oldFile = File(file.absolutePath + ".old")

        return if (file.exists() && file.length() > 0L) {
            load(file, storeClass, uuid, registryAccess)
                ?: let {
                    file.delete()
                    if (oldFile.exists() && oldFile.length() > 0L) {
                        var result = load(oldFile, storeClass, uuid, registryAccess) ?: let {
                            LOGGER.error("Pokémon save file for ${storeClass.simpleName} ($uuid) was corrupted. A fresh file will be created.")
                            var result = storeClass.getConstructor(UUID::class.java).newInstance(uuid)
                            save(storeClass, uuid, serialize(result, registryAccess))
                            oldFile.delete()
                            return result
                        }
                        LOGGER.warn("Loading old Pokémon save file for ${storeClass.simpleName} ($uuid) due to corruption of current file.")
                        oldFile.copyTo(file, overwrite = true)
                        result
                    }
                    else {
                        var result = storeClass.getConstructor(UUID::class.java).newInstance(uuid)
                        save(storeClass, uuid, serialize(result, registryAccess))
                        oldFile.delete()
                        result
                    }
                }
        } else {
            file.delete()
            if (oldFile.exists() && oldFile.length() > 0L) {
                var result = load(oldFile, storeClass, uuid, registryAccess) ?: let {
                    oldFile.delete()
                    return null
                }
                oldFile.copyTo(file, overwrite = true)
                result
            }
            null
        }
    }
}
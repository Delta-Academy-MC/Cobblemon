/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.pc

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.CobblemonUnlockableWallpapers
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.storage.WallpaperUnlockedEvent
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.api.storage.BottomlessStore
import com.cobblemon.mod.common.api.storage.PokemonStore
import com.cobblemon.mod.common.api.storage.StoreCoordinates
import com.cobblemon.mod.common.api.text.add
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.api.toast.Toast
import com.cobblemon.mod.common.net.messages.client.storage.RemoveClientPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.SwapClientPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.InitializePCPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.MoveClientPCPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.SetPCPokemonPacket
import com.cobblemon.mod.common.net.messages.client.storage.pc.wallpaper.UnlockPCBoxWallpaperPacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.DataKeys
import com.cobblemon.mod.common.util.asTranslated
import com.cobblemon.mod.common.util.getPlayer
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.toJsonArray
import com.google.gson.JsonObject
import java.util.UUID
import net.minecraft.core.RegistryAccess
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack

/**
 * The store used for PCs. It is divided into some number of [PCBox]es, and can
 * direct resize overflow into a [BottomlessStore].
 *
 * The overflow is reserved only for when the number of PC boxes changes and there
 * is existing data that no longer fits. Using it for handling [getFirstAvailablePosition]
 * overflow would be a bad idea because it would make it easy to create an explosively large
 * store by a bad actor.
 *
 * @author Hiroku
 * @since April 26th, 2022
 */
open class PCStore(
    final override val uuid: UUID,
    val name: MutableComponent
) : PokemonStore<PCPosition>() {
    constructor(uuid: UUID): this(uuid, lang("your_pc"))

    val boxes = mutableListOf<PCBox>()
    protected var lockedSize = false
    val backupStore = BottomlessStore(UUID(0L, 0L))
    val observingUUIDs = mutableSetOf(uuid)
    val unlockedWallpapers = mutableSetOf<ResourceLocation>()
    val unseenWallpapers = mutableSetOf<ResourceLocation>()

    override fun iterator() = boxes.flatMap { it.toList() }.iterator()
    override fun getObservingPlayers() = observingUUIDs.mapNotNull { it.getPlayer() }

    val struct = asMoLangValue()

    fun addObserver(player: ServerPlayer) {
        observingUUIDs.add(player.uuid)
        sendTo(player)
    }
    fun removeObserver(playerID: UUID) {
        observingUUIDs.remove(playerID)
    }

    val pcChangeObservable = SimpleObservable<Unit>()

    override fun getFirstAvailablePosition(): PCPosition? {
        boxes.forEach { it.getFirstAvailablePosition()?.let { return it } }
        return null
    }

    override fun isValidPosition(position: PCPosition): Boolean {
        return position.box in (0 until boxes.size) && position.slot in (0 until POKEMON_PER_BOX)
    }

    override fun sendTo(player: ServerPlayer) {
        InitializePCPacket(this).sendToPlayer(player)
        boxes.forEach { it.sendTo(player) }
    }

    override fun initialize() {
        boxes.forEach { it.initialize() }
        backupStore.initialize()
    }

    fun relocateEvictedBoxPokemon(pokemon: Pokemon) {
        val space = getFirstAvailablePosition()
        if (space != null) {
            this[space] = pokemon
        } else {
            backupStore.add(pokemon)
        }
    }

    fun resize(newSize: Int, lockNewSize: Boolean = false, overflowHandler: (Pokemon) -> Unit = ::relocateEvictedBoxPokemon) {
        if (newSize <= 0) {
            throw java.lang.IllegalArgumentException("Invalid box count: Must be greater than zero.")
        }

        this.lockedSize = lockNewSize
        if (boxes.size > newSize) {
            // reduce
            val slicedBoxes = boxes.slice(newSize until boxes.size)
            boxes.removeAll(slicedBoxes)
            slicedBoxes.flatMap { it.asIterable() }.forEach(overflowHandler)
        } else {
            // expand
            while (boxes.size < newSize) {
                this.boxes.add(PCBox(this))
            }

            tryRestoreBackedUpPokemon()
        }
        pcChangeObservable.emit(Unit)
    }

    fun removeListOfBoxes(boxList: List<PCBox>,lockNewSize: Boolean = false, overflowHandler: (Pokemon) -> Unit = ::relocateEvictedBoxPokemon){
        this.lockedSize = lockNewSize
        boxes.removeAll(boxList)
        boxList.flatMap { it.asIterable() }.forEach(overflowHandler)
        pcChangeObservable.emit(Unit)
    }

    fun tryRestoreBackedUpPokemon() {
        var newPosition = getFirstAvailablePosition()
        val backedUpPokemon = backupStore.pokemon.toMutableList()
        while (newPosition != null && backedUpPokemon.isNotEmpty()) {
            this[newPosition] = backedUpPokemon.removeAt(0)
            newPosition = getFirstAvailablePosition()
        }
    }

    override fun saveToNBT(nbt: CompoundTag, registryAccess: RegistryAccess): CompoundTag {
        nbt.putShort(DataKeys.STORE_BOX_COUNT, boxes.size.toShort())
        nbt.putBoolean(DataKeys.STORE_BOX_COUNT_LOCKED, lockedSize)
        boxes.forEachIndexed { index, box ->
            nbt.put(DataKeys.STORE_BOX + index, box.saveToNBT(CompoundTag(), registryAccess))
        }
        nbt.put(DataKeys.STORE_BACKUP, backupStore.saveToNBT(CompoundTag(), registryAccess))
        nbt.put(DataKeys.STORE_UNLOCKED_WALLPAPERS, ListTag().also { it.addAll(unlockedWallpapers.map { StringTag.valueOf(it.toString()) }) })
        nbt.put(DataKeys.STORE_UNSEEN_WALLPAPERS, ListTag().also { it.addAll(unseenWallpapers.map { StringTag.valueOf(it.toString()) }) })
        return nbt
    }

    override fun loadFromNBT(nbt: CompoundTag, registryAccess: RegistryAccess): PokemonStore<PCPosition> {
        val boxCountStored = nbt.getShort(DataKeys.STORE_BOX_COUNT)
        for (boxNumber in 0 until boxCountStored) {
            boxes.add(PCBox(this).loadFromNBT(nbt.getCompound(DataKeys.STORE_BOX + boxNumber), registryAccess))
        }
        lockedSize = nbt.getBoolean(DataKeys.STORE_BOX_COUNT_LOCKED)
        if (!lockedSize && boxes.size != Cobblemon.config.defaultBoxCount) {
            resize(Cobblemon.config.defaultBoxCount, lockNewSize = false)
        } else {
            tryRestoreBackedUpPokemon()
        }

        removeDuplicates()

        if (nbt.contains(DataKeys.STORE_UNLOCKED_WALLPAPERS)) {
            unlockedWallpapers.addAll(nbt.getList(DataKeys.STORE_UNLOCKED_WALLPAPERS, Tag.TAG_STRING.toInt()).map { ResourceLocation.parse(it.asString) })
        }
        if (nbt.contains(DataKeys.STORE_UNSEEN_WALLPAPERS)) {
            unseenWallpapers.addAll(nbt.getList(DataKeys.STORE_UNSEEN_WALLPAPERS, Tag.TAG_STRING.toInt()).map { ResourceLocation.parse(it.asString) })
        }
        return this
    }

    fun removeDuplicates() {
        val knownUUIDs = mutableListOf<UUID>()
        for (box in boxes) {
            for (i in 0 until POKEMON_PER_BOX) {
                val pokemon = box[i] ?: continue
                if (pokemon.uuid !in knownUUIDs) {
                    knownUUIDs.add(pokemon.uuid)
                } else {
                    box[i] = null
                    pcChangeObservable.emit(Unit)
                }
            }
        }
    }

    override fun saveToJSON(json: JsonObject, registryAccess: RegistryAccess): JsonObject {
        json.addProperty(DataKeys.STORE_BOX_COUNT, boxes.size.toShort())
        json.addProperty(DataKeys.STORE_BOX_COUNT_LOCKED, lockedSize)
        boxes.forEachIndexed { index, box ->
            json.add(DataKeys.STORE_BOX + index, box.saveToJSON(JsonObject(), registryAccess))
        }
        json.add(DataKeys.STORE_BACKUP, backupStore.saveToJSON(JsonObject(), registryAccess))
        json.add(DataKeys.STORE_UNLOCKED_WALLPAPERS, unlockedWallpapers.map(ResourceLocation::toString).toJsonArray())
        json.add(DataKeys.STORE_UNSEEN_WALLPAPERS, unseenWallpapers.map(ResourceLocation::toString).toJsonArray())
        return json
    }

    override fun loadFromJSON(json: JsonObject, registryAccess: RegistryAccess): PokemonStore<PCPosition> {
        val boxCountStored = json.get(DataKeys.STORE_BOX_COUNT).asShort
        for (boxNumber in 0 until boxCountStored) {
            boxes.add(PCBox(this).loadFromJSON(json.getAsJsonObject(DataKeys.STORE_BOX + boxNumber), registryAccess))
        }
        lockedSize = json.get(DataKeys.STORE_BOX_COUNT_LOCKED).asBoolean
        if (!lockedSize && boxes.size != Cobblemon.config.defaultBoxCount) {
            resize(newSize = Cobblemon.config.defaultBoxCount, lockNewSize = false)
        } else {
            tryRestoreBackedUpPokemon()
        }

        removeDuplicates()

        if (json.has(DataKeys.STORE_UNLOCKED_WALLPAPERS)) {
            unlockedWallpapers.addAll(json.getAsJsonArray(DataKeys.STORE_UNLOCKED_WALLPAPERS).map { ResourceLocation.parse(it.asString) })
        }
        if (json.has(DataKeys.STORE_UNSEEN_WALLPAPERS)) {
            unseenWallpapers.addAll(json.getAsJsonArray(DataKeys.STORE_UNSEEN_WALLPAPERS).map { ResourceLocation.parse(it.asString) })
        }

        return this
    }

    override operator fun set(position: PCPosition, pokemon: Pokemon) {
        super.set(position, pokemon)
        val copy = Pokemon()
        copy.copyFrom(pokemon)
        sendPacketToObservers(SetPCPokemonPacket(uuid, position) { copy })
    }

    override fun remove(pokemon: Pokemon): Boolean {
        return if (super.remove(pokemon)) {
            sendPacketToObservers(RemoveClientPokemonPacket(this, pokemon.uuid))
            true
        } else {
            false
        }
    }

    override fun swap(position1: PCPosition, position2: PCPosition) {
        val pokemon1 = get(position1)
        val pokemon2 = get(position2)
        super.swap(position1, position2)
        if (pokemon1 != null && pokemon2 != null) {
            sendPacketToObservers(SwapClientPokemonPacket(this, pokemon1.uuid, pokemon2.uuid))
        } else if (pokemon1 != null || pokemon2 != null) {
            val newPosition = if (pokemon1 == null) position1 else position2
            val pokemon = pokemon1 ?: pokemon2!!
            sendPacketToObservers(MoveClientPCPokemonPacket(uuid, pokemon.uuid, newPosition))
        }
    }

    override fun loadPositionFromNBT(nbt: CompoundTag): StoreCoordinates<PCPosition> {
        return StoreCoordinates(this, PCPosition(nbt.getShort(DataKeys.STORE_BOX).toInt(), nbt.getByte(DataKeys.STORE_SLOT).toInt()))
    }

    override fun savePositionToNBT(position: PCPosition, nbt: CompoundTag) {
        nbt.putShort(DataKeys.STORE_BOX, position.box.toShort())
        nbt.putByte(DataKeys.STORE_SLOT, position.slot.toByte())
    }

    override fun getAnyChangeObservable() = pcChangeObservable

    override fun setAtPosition(position: PCPosition, pokemon: Pokemon?) {
        if (position.box !in 0 until boxes.size) {
            throw IllegalArgumentException("Invalid box number ${position.box}. Should be between 0 and ${boxes.size}")
        }
        boxes[position.box][position.slot] = pokemon
    }

    override operator fun get(position: PCPosition): Pokemon? {
        return if (position.box !in 0 until boxes.size) {
            null
        } else {
            boxes[position.box][position.slot]
        }
    }

    override fun onPokemonChanged(pokemon: Pokemon) {
        pcChangeObservable.emit(Unit)
    }

    fun clearPC() {
        boxes.forEach { box ->
            box.getNonEmptySlots().forEach{
                remove(PCPosition(box.boxNumber, it.key))
            }
        }
    }

    fun markWallpapersSeen(textures: Set<ResourceLocation>) {
        // This first step cleans out any that were unseen but were removed from the datapack side before they were seen.
        val currentlyUnseen = unseenWallpapers.mapNotNull { CobblemonUnlockableWallpapers.unlockableWallpapers[it] }

        val remainingUnseen = currentlyUnseen.filter { it.texture !in textures }
        this.unseenWallpapers.clear()
        this.unseenWallpapers.addAll(remainingUnseen.map { it.id })
        pcChangeObservable.emit(Unit)
    }

    fun unlockWallpaper(wallpaper: ResourceLocation, playSound: Boolean = true): Boolean {
        val unlockableWallpaper = CobblemonUnlockableWallpapers.unlockableWallpapers[wallpaper]
        var succeeded = false
        if (unlockableWallpaper != null && unlockableWallpaper.enabled) {
            val event = WallpaperUnlockedEvent(pc = this, wallpaper = unlockableWallpaper, shouldNotify = playSound)
            CobblemonEvents.WALLPAPER_UNLOCKED_EVENT.postThen(
                event = event,
                ifSucceeded = {
                    if (unlockedWallpapers.add(wallpaper)) {
                        unseenWallpapers.add(unlockableWallpaper.texture)
                        pcChangeObservable.emit(Unit)
                        getObservingPlayers().forEach { player ->
                            player.sendPacket(UnlockPCBoxWallpaperPacket(unlockableWallpaper.texture))
                            if (event.shouldNotify) {
                                val toast = Toast(
                                    title = lang("wallpaper_unlocked"),
                                    description = unlockableWallpaper.displayName?.let { "\"".text().add(it.asTranslated()).add("\"") } ?: lang("unknown_wallpaper") ,
                                    icon = ItemStack(CobblemonItems.PC)
                                )
                                toast.addListeners(player)
                                toast.expireAfter(5F)
                                player.playNotifySound(CobblemonSounds.PC_WALLPAPER_UNLOCK, SoundSource.MASTER, 1F, 1F)
                            }
                        }
                        succeeded = true
                    }
                }
            )
        }

        return succeeded
    }
}
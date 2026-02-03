/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric

import com.cobblemon.mod.common.*
import com.cobblemon.mod.common.advancement.CobblemonCriteria
import com.cobblemon.mod.common.advancement.predicate.CobblemonEntitySubPredicates
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.CobblemonMobEffects
import com.cobblemon.mod.common.api.net.serializers.*
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.item.gimmicks.MegaBraceletItem
import com.cobblemon.mod.common.item.gimmicks.TeraOrbItem
import com.cobblemon.mod.common.item.group.CobblemonItemGroups
import com.cobblemon.mod.common.item.pokebag.PokeBagItem
import com.cobblemon.mod.common.item.pokebag.inventory.PokeBagMenuType
import com.cobblemon.mod.common.loot.LootInjector
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.platform.events.*
import com.cobblemon.mod.common.sherds.CobblemonSherds
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.didSleep
import com.cobblemon.mod.common.world.CobblemonStructures
import com.cobblemon.mod.common.world.feature.CobblemonFeatures
import com.cobblemon.mod.common.world.placementmodifier.CobblemonPlacementModifierTypes
import com.cobblemon.mod.common.world.predicate.CobblemonBlockPredicates
import com.cobblemon.mod.common.world.structureprocessors.CobblemonProcessorTypes
import com.cobblemon.mod.common.world.structureprocessors.CobblemonStructureProcessorListOverrides
import com.cobblemon.mod.fabric.net.CobblemonFabricNetworkManager
import com.cobblemon.mod.fabric.permission.FabricPermissionValidator
import com.cobblemon.mod.fabric.terastallization.TerastallizationEventHandler
import com.mojang.brigadier.arguments.ArgumentType
import dev.emi.trinkets.api.TrinketsApi
import dev.emi.trinkets.api.event.TrinketDropCallback
import dev.emi.trinkets.api.event.TrinketEquipCallback
import dev.emi.trinkets.api.event.TrinketUnequipCallback
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.loot.v2.LootTableEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.trade.TradeOfferHelper
import net.fabricmc.fabric.api.`object`.builder.v1.world.poi.PointOfInterestHelper
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.stats.Stats
import net.minecraft.tags.TagKey
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.reflect.KClass

object CobblemonFabric : CobblemonImplementation {

    override val modAPI = ModAPI.FABRIC

    private var server: MinecraftServer? = null

    override val networkManager = CobblemonFabricNetworkManager

    fun initialize() {
        Cobblemon.preInitialize(this)

        Cobblemon.statistics.registerStats()
        Cobblemon.statistics.stats.forEach { entry ->
            val cobblemonStat = entry.value
            Registry.register(BuiltInRegistries.CUSTOM_STAT, cobblemonStat.resourceLocation, cobblemonStat.resourceLocation)
            Stats.CUSTOM.get(cobblemonStat.resourceLocation, cobblemonStat.formatter)
        }

        Cobblemon.initialize()
        networkManager.registerMessages()
        networkManager.registerServerHandlers()

        //This has to be registered elsewhere on forge so we cant do it in common
        CobblemonSherds.registerSherds()
        CobblemonBlockPredicates.touch()
        CobblemonPlacementModifierTypes.touch()
        CobblemonProcessorTypes.touch()
        CobblemonActivities.activities.forEach { Registry.register(BuiltInRegistries.ACTIVITY, cobblemonResource(it.name), it) }
        CobblemonSensors.sensors.forEach { (key, sensorType) -> Registry.register(BuiltInRegistries.SENSOR_TYPE, cobblemonResource(key), sensorType) }
        CobblemonMemories.memories.forEach { (key, memoryModuleType) -> Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, cobblemonResource(key), memoryModuleType) }

        EntitySleepEvents.STOP_SLEEPING.register { playerEntity, _ ->
            if (playerEntity !is ServerPlayer) {
                return@register
            }

            playerEntity.didSleep()
        }

        // This doesn't work (yet) because Fabric does not let us make non-potion ingredients/inputs/outputs.
        // Throws an exception about the ingredient not being a potion. Maybe one day?
//        FabricBrewingRecipeRegistryBuilder.BUILD.register { builder ->
//            BrewingRecipes.recipes.forEach { (input, ingredient, output) ->
//                if (input is CobblemonPotionIngredient) {
//                    // This doesn't respect the specific potion types that are considered inputs. This is less than perfect
//                    // but will at least let JEI/EMI etc fill in the ingredients.
//                    builder.addContainerRecipe(Items.POTION, ingredient, output)
//                } else {
//                    input as CobblemonItemIngredient
//                    builder.addContainerRecipe(input.item, ingredient, output)
//                }
//            }
//        }

        Cobblemon.builtinPacks
            .filter { it.neededMods.all(Cobblemon.implementation::isModInstalled) }
            .forEach {
                val mod = FabricLoader.getInstance().getModContainer(Cobblemon.MODID).get()
                val resourcePackActivationType = when (it.activationBehaviour) {
                    ResourcePackActivationBehaviour.NORMAL -> ResourcePackActivationType.NORMAL
                    ResourcePackActivationBehaviour.DEFAULT_ENABLED -> ResourcePackActivationType.DEFAULT_ENABLED
                    ResourcePackActivationBehaviour.ALWAYS_ENABLED -> ResourcePackActivationType.ALWAYS_ENABLED
                }
                val id = cobblemonResource(it.id)
                val subPath = "${ if (it.packType == PackType.CLIENT_RESOURCES) "resourcepacks" else "datapacks" }/${id.path}"
                ResourceManagerHelperImpl.registerBuiltinResourcePack(id, subPath, mod, it.displayName, resourcePackActivationType)
            }

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register { player, isLogin ->
            if (isLogin) {
                Cobblemon.dataProvider.sync(player)
            }
        }
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
            PlatformEvents.SERVER_STARTING.post(ServerEvent.Starting(server))
            CobblemonStructures.registerJigsaws(server)
            CobblemonStructureProcessorListOverrides.register(server)
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server -> PlatformEvents.SERVER_STARTED.post(ServerEvent.Started(server)) }
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            server.playerList.players.forEach { player -> PlatformEvents.SERVER_PLAYER_LOGOUT.post(ServerPlayerEvent.Logout(player)) }
            PlatformEvents.SERVER_STOPPING.post(ServerEvent.Stopping(server))
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server -> PlatformEvents.SERVER_STOPPED.post(ServerEvent.Stopped(server)) }
        ServerTickEvents.START_SERVER_TICK.register { server -> PlatformEvents.SERVER_TICK_PRE.post(ServerTickEvent.Pre(server)) }
        ServerTickEvents.END_SERVER_TICK.register { server -> PlatformEvents.SERVER_TICK_POST.post(ServerTickEvent.Post(server)) }
        ServerPlayConnectionEvents.JOIN.register { handler, _, server -> server.executeIfPossible { PlatformEvents.SERVER_PLAYER_LOGIN.post(ServerPlayerEvent.Login(handler.player)) } }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            if (!server.isStopped) server.executeIfPossible { PlatformEvents.SERVER_PLAYER_LOGOUT.post(ServerPlayerEvent.Logout(handler.player)) }
        }
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, _, _ ->
            if (entity is ServerPlayer) {
                PlatformEvents.PLAYER_DEATH.postThen(
                    event = ServerPlayerEvent.Death(entity),
                    ifSucceeded = {},
                    ifCanceled = { return@register false }
                )
            }
            return@register true
        }

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register { player, origin, destination ->
            PlatformEvents.CHANGE_DIMENSION.post(ChangeDimensionEvent(player, origin, destination))
        }


        UseBlockCallback.EVENT.register { player, _, hand, hitResult ->
            val serverPlayer = player as? ServerPlayer ?: return@register InteractionResult.PASS
            PlatformEvents.RIGHT_CLICK_BLOCK.postThen(
                event = ServerPlayerEvent.RightClickBlock(serverPlayer, hitResult.blockPos, hand, hitResult.direction),
                ifSucceeded = {},
                ifCanceled = { return@register InteractionResult.FAIL }
            )
            return@register InteractionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, _, hand, entity, _ ->
            val item = player.getItemInHand(hand)
            val serverPlayer = player as? ServerPlayer ?: return@register InteractionResult.PASS

            PlatformEvents.RIGHT_CLICK_ENTITY.postThen(
                event = ServerPlayerEvent.RightClickEntity(serverPlayer, item, hand, entity),
                ifSucceeded = {},
                ifCanceled = { return@register InteractionResult.FAIL }
            )

            return@register InteractionResult.PASS
        }

        LootTableEvents.MODIFY.register { id, tableBuilder, source ->
            LootInjector.attemptInjection(id.location(), tableBuilder::withPool)
        }

        CommandRegistrationCallback.EVENT.register(CobblemonCommands::register)

        registerTrinketEvents()
        TerastallizationEventHandler.initialise()
    }

    override fun isModInstalled(id: String) = FabricLoader.getInstance().isModLoaded(id)

    override fun environment(): Environment {
        return when(FabricLoader.getInstance().environmentType) {
            EnvType.CLIENT -> Environment.CLIENT
            EnvType.SERVER -> Environment.SERVER
            else -> throw IllegalStateException("Fabric implementation cannot resolve environment yet")
        }
    }

    override fun registerPermissionValidator() {
        if (this.isModInstalled("fabric-permissions-api-v0")) {
            Cobblemon.permissionValidator = FabricPermissionValidator()
        }
    }

    override fun registerSoundEvents() {
        CobblemonSounds.register { identifier, sound -> Registry.register(CobblemonSounds.registry, identifier, sound) }
    }

    override fun registerDataComponents() {
        CobblemonItemComponents.register { identifier, component -> Registry.register(CobblemonItemComponents.registry, identifier, component) }
    }

    override fun registerEntityDataSerializers() {
        EntityDataSerializers.registerSerializer(Vec3DataSerializer)
        EntityDataSerializers.registerSerializer(StringSetDataSerializer)
        EntityDataSerializers.registerSerializer(PoseTypeDataSerializer)
        EntityDataSerializers.registerSerializer(PlatformTypeDataSerializer)
        EntityDataSerializers.registerSerializer(IdentifierDataSerializer)
        EntityDataSerializers.registerSerializer(UUIDSetDataSerializer)
        EntityDataSerializers.registerSerializer(NPCPlayerTextureSerializer)
        EntityDataSerializers.registerSerializer(RideBoostsDataSerializer)
    }

    fun registerTrinketEvents() {
        TrinketEquipCallback.EVENT.register { stack, _, entity ->
            if (entity is ServerPlayer) {
                if (stack.item is MegaBraceletItem) {
                    val playerData = Cobblemon.playerDataManager.getGenericData(entity)
                    val keyItems = playerData.keyItems
                    if (!keyItems.contains(cobblemonResource("key_stone"))) {
                        keyItems.add(cobblemonResource("key_stone"))
                    }
                    Cobblemon.playerDataManager.saveSingle(playerData, PlayerInstancedDataStoreTypes.GENERAL)
                }
                if (stack.item is TeraOrbItem) {
                    val playerData = Cobblemon.playerDataManager.getGenericData(entity)
                    val keyItems = playerData.keyItems
                    if (!keyItems.contains(cobblemonResource("tera_orb"))) {
                        keyItems.add(cobblemonResource("tera_orb"))
                    }
                    Cobblemon.playerDataManager.saveSingle(playerData, PlayerInstancedDataStoreTypes.GENERAL)
                }
            }
        }

        TrinketUnequipCallback.EVENT.register { stack, _, entity ->
            if (entity is ServerPlayer) {
                if (stack.item is MegaBraceletItem) {
                    val playerData = Cobblemon.playerDataManager.getGenericData(entity)
                    val keyItems = playerData.keyItems
                    if (keyItems.contains(cobblemonResource("key_stone"))) {
                        keyItems.remove(cobblemonResource("key_stone"))
                    }
                    Cobblemon.playerDataManager.saveSingle(playerData, PlayerInstancedDataStoreTypes.GENERAL)
                }
                if (stack.item is TeraOrbItem) {
                    val playerData = Cobblemon.playerDataManager.getGenericData(entity)
                    val keyItems = playerData.keyItems
                    if (keyItems.contains(cobblemonResource("tera_orb"))) {
                        keyItems.remove(cobblemonResource("tera_orb"))
                    }
                    Cobblemon.playerDataManager.saveSingle(playerData, PlayerInstancedDataStoreTypes.GENERAL)
                }
            }
        }

        CobblemonEvents.POKE_BAG_CONTAINER_VALID_CHECK.subscribe { event ->
            val trinketComponent = TrinketsApi.getTrinketComponent(event.player).orElse(null)
            if (trinketComponent != null) {
                event.isValid = trinketComponent.allEquipped.any { it.b == event.backpack }
            }
        }

        CobblemonEvents.POKE_BAG_OPEN_REQUEST.subscribe { event ->
            TrinketsApi.getTrinketComponent(event.player).ifPresent { trinket ->
                trinket.allEquipped.forEach { entry ->
                    val stack = entry.b
                    val item = entry.b.item
                    if (item is PokeBagItem) {
                        item.open(event.player, stack)
                    }
                }
            }
        }
    }

    override fun registerItems() {
        CobblemonItems.register { identifier, item -> Registry.register(CobblemonItems.registry, identifier, item) }
        CobblemonItemGroups.register { provider ->
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, provider.key, FabricItemGroup.builder()
                .title(provider.displayName)
                .icon(provider.displayIconProvider)
                .displayItems(provider.entryCollector)
                .build())
        }

        CobblemonItemGroups.injectorKeys().forEach { key ->
            ItemGroupEvents.modifyEntriesEvent(key).register { content ->
                val fabricInjector = FabricItemGroupInjector(content)
                CobblemonItemGroups.inject(key, fabricInjector)
            }
        }
    }

    override fun registerBlocks() {
        CobblemonBlocks.register { identifier, item -> Registry.register(CobblemonBlocks.registry, identifier, item) }
        CobblemonBlocks.strippedBlocks().forEach(StrippableBlockRegistry::register)
    }

    override fun registerEntityTypes() {
        CobblemonEntities.register { identifier, type -> Registry.register(CobblemonEntities.registry, identifier, type) }
    }

    override fun registerEntityAttributes() {
        CobblemonEntities.registerAttributes { entityType, builder -> FabricDefaultAttributeRegistry.register(entityType, builder) }
    }

    override fun registerBlockEntityTypes() {
        CobblemonBlockEntities.register { identifier, type -> Registry.register(CobblemonBlockEntities.registry, identifier, type) }
    }

    override fun registerPoiTypes() {
        CobblemonPoiTypes.register { identifier, type -> PointOfInterestHelper.register(identifier, type.maxTickets, type.validRange, type.matchingStates) }
    }

    override fun registerVillagers() {
        CobblemonVillagerProfessions.register { identifier, profession -> Registry.register(CobblemonVillagerProfessions.registry, identifier, profession) }

        CobblemonTradeOffers.tradeOffersForAll().forEach { tradeOffer -> TradeOfferHelper.registerVillagerOffers(tradeOffer.profession, tradeOffer.requiredLevel) { factories -> factories.addAll(tradeOffer.tradeOffers) } }
        // 1 = common trades, 2 = rare, it has no concept of levels
        CobblemonTradeOffers.resolveWanderingTradeOffers().forEach { tradeOffer -> TradeOfferHelper.registerWanderingTraderOffers(if (tradeOffer.isRareTrade) 2 else 1) { factories -> factories.addAll(tradeOffer.tradeOffers) } }
    }

    override fun registerRecipeSerializers() {
        CobblemonRecipeSerializers.register { identifier, factory -> Registry.register(CobblemonRecipeSerializers.registry, identifier, factory) }
    }

    override fun registerRecipeTypes() {
        CobblemonRecipeTypes.register { identifier, factory -> Registry.register(CobblemonRecipeTypes.registry, identifier, factory) }
    }


    override fun registerWorldGenFeatures() {
        CobblemonFeatures.register { identifier, feature -> Registry.register(CobblemonFeatures.registry, identifier, feature) }
    }

    override fun registerParticles() {
        CobblemonParticles.register { identifier, particleType -> Registry.register(CobblemonParticles.registry, identifier, particleType) }
    }

    override fun registerMenu() {
        CobblemonMenuType.register { identifier, factory -> Registry.register(CobblemonMenuType.registry, identifier, factory) }
        PokeBagMenuType.initialise()
    }

    override fun addFeatureToWorldGen(feature: ResourceKey<PlacedFeature>, step: GenerationStep.Decoration, validTag: TagKey<Biome>?) {
        val predicate: (BiomeSelectionContext) -> Boolean = { context -> validTag == null || context.hasTag(validTag) }
        BiomeModifications.addFeature(predicate, step, feature)
    }

    override fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>> registerCommandArgument(identifier: ResourceLocation, argumentClass: KClass<A>, serializer: ArgumentTypeInfo<A, T>) {
        ArgumentTypeRegistry.registerArgumentType(identifier, argumentClass.java, serializer)
    }

    override fun <T : GameRules.Value<T>> registerGameRule(name: String, category: GameRules.Category, type: GameRules.Type<T>): GameRules.Key<T> = GameRuleRegistry.register(name, category, type)

    override fun registerCriteria() {
        CobblemonCriteria.register { id, obj ->
            Registry.register(CobblemonCriteria.registry, id, obj)
        }
    }

    override fun registerEntitySubPredicates() {
        CobblemonEntitySubPredicates.register { resourceLocation, mapCodec ->
            Registry.register(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE, resourceLocation, mapCodec)
        }
    }

    override fun registerResourceReloader(identifier: ResourceLocation, reloader: PreparableReloadListener, type: PackType, dependencies: Collection<ResourceLocation>) {
        ResourceManagerHelper.get(type).registerReloadListener(CobblemonReloadListener(identifier, reloader, dependencies))
    }

    override fun server(): MinecraftServer? = if (this.environment() == Environment.CLIENT) Minecraft.getInstance().singleplayerServer else this.server

    override fun registerCompostable(item: ItemLike, chance: Float) {
        CompostingChanceRegistry.INSTANCE.add(item, chance)
    }

    override fun registerMobEffects() {
        CobblemonMobEffects.register { identifier, effect ->
            Registry.register(CobblemonMobEffects.registry, identifier, effect)
        }
    }

    private class CobblemonReloadListener(private val identifier: ResourceLocation, private val reloader: PreparableReloadListener, private val dependencies: Collection<ResourceLocation>) : IdentifiableResourceReloadListener {

        override fun reload(synchronizer: PreparableReloadListener.PreparationBarrier, manager: ResourceManager, prepareProfiler: ProfilerFiller, applyProfiler: ProfilerFiller, prepareExecutor: Executor, applyExecutor: Executor): CompletableFuture<Void> = this.reloader.reload(synchronizer, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor)

        override fun getFabricId(): ResourceLocation = this.identifier

        override fun getName(): String = this.reloader.name

        override fun getFabricDependencies(): MutableCollection<ResourceLocation> = this.dependencies.toMutableList()
    }

    private class FabricItemGroupInjector(private val fabricItemGroupEntries: FabricItemGroupEntries) : CobblemonItemGroups.Injector {
        override fun putFirst(item: ItemLike) {
            this.fabricItemGroupEntries.prepend(item)
        }

        override fun putBefore(item: ItemLike, target: ItemLike) {
            this.fabricItemGroupEntries.addBefore(target, item)
        }

        override fun putAfter(item: ItemLike, target: ItemLike) {
            this.fabricItemGroupEntries.addAfter(target, item)
        }

        override fun putLast(item: ItemLike) {
            this.fabricItemGroupEntries.accept(item)
        }
    }
}

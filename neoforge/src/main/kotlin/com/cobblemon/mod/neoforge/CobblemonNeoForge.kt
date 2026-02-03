/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.neoforge

import com.cobblemon.mod.common.*
import com.cobblemon.mod.common.CobblemonRecipeCategories.COOKING_POT_COMPLEX_DISHES
import com.cobblemon.mod.common.CobblemonRecipeCategories.COOKING_POT_FOODS
import com.cobblemon.mod.common.CobblemonRecipeCategories.COOKING_POT_MEDICINES
import com.cobblemon.mod.common.CobblemonRecipeCategories.COOKING_POT_MISC
import com.cobblemon.mod.common.CobblemonRecipeCategories.COOKING_POT_SEARCH
import com.cobblemon.mod.common.advancement.CobblemonCriteria
import com.cobblemon.mod.common.advancement.predicate.CobblemonEntitySubPredicates
import com.cobblemon.mod.common.CobblemonMobEffects
import com.cobblemon.mod.common.api.net.serializers.*
import com.cobblemon.mod.common.item.group.CobblemonItemGroups
import com.cobblemon.mod.common.loot.LootInjector
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.sherds.CobblemonSherds
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.didSleep
import com.cobblemon.mod.common.world.CobblemonStructures
import com.cobblemon.mod.common.world.feature.CobblemonFeatures
import com.cobblemon.mod.common.world.placementmodifier.CobblemonPlacementModifierTypes
import com.cobblemon.mod.common.world.predicate.CobblemonBlockPredicates
import com.cobblemon.mod.common.world.structureprocessors.CobblemonProcessorTypes
import com.cobblemon.mod.common.world.structureprocessors.CobblemonStructureProcessorListOverrides
import com.cobblemon.mod.neoforge.client.CobblemonNeoForgeClient
import com.cobblemon.mod.neoforge.event.NeoForgePlatformEventHandler
import com.cobblemon.mod.neoforge.net.CobblemonNeoForgeNetworkManager
import com.cobblemon.mod.neoforge.permission.ForgePermissionValidator
import com.cobblemon.mod.neoforge.worldgen.CobblemonBiomeModifiers
import com.mojang.brigadier.arguments.ArgumentType
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.PathPackResources
import net.minecraft.server.packs.repository.BuiltInPackSource
import net.minecraft.server.packs.repository.KnownPack
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.Pack.Position
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.stats.Stats
import net.minecraft.tags.TagKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTab.TabVisibility
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.EventPriority
import net.neoforged.fml.InterModComms
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent
import net.neoforged.neoforge.common.ItemAbilities
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.NeoForgeMod
import net.neoforged.neoforge.event.*
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent
import net.neoforged.neoforge.event.village.VillagerTradesEvent
import net.neoforged.neoforge.event.village.WandererTradesEvent
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import java.util.*
import kotlin.reflect.KClass

@Mod(Cobblemon.MODID)
class CobblemonNeoForge : CobblemonImplementation {
    override val modAPI = ModAPI.NEOFORGE
    private val hasBeenSynced = hashSetOf<UUID>()

    private val commandArgumentTypes = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Cobblemon.MODID)
    private val reloadableResources = arrayListOf<PreparableReloadListener>()
    private val queuedWork = arrayListOf<() -> Unit>()

    override val networkManager = CobblemonNeoForgeNetworkManager

    init {
        with(MOD_BUS) {
            this@CobblemonNeoForge.commandArgumentTypes.register(this)
            addListener(this@CobblemonNeoForge::initialize)
            Cobblemon.preInitialize(this@CobblemonNeoForge)
            addListener(CobblemonBiomeModifiers::register)
            addListener(this@CobblemonNeoForge::on)
            addListener(networkManager::registerMessages)
            addListener(EventPriority.HIGH, ::onBuildContents)
            addListener(::onAddPackFindersEvent)
            addListener(::loadRecipeCategory)
        }
        with(NeoForge.EVENT_BUS) {
            addListener(::onDataPackSync)
            addListener(::onLogin)
            addListener(::onLogout)
            addListener(::wakeUp)
            addListener(::handleBlockStripping)
            addListener(::registerCommands)
            addListener(::onReload)
            addListener(::addCobblemonStructures)
            addListener(::onVillagerTradesRegistry)
            addListener(::onWanderingTraderRegistry)
            addListener(::onLootTableLoad)
        }
        NeoForgePlatformEventHandler.register()
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CobblemonNeoForgeClient.init()
        }
    }

    fun addCobblemonStructures(event: ServerAboutToStartEvent) {
        CobblemonStructures.registerJigsaws(event.server)
        CobblemonStructureProcessorListOverrides.register(event.server)
    }

    fun wakeUp(event: PlayerWakeUpEvent) {
        val playerEntity = event.entity as? ServerPlayer ?: return
        playerEntity.didSleep()
    }

    fun initialize(event: FMLCommonSetupEvent) {
        Cobblemon.LOGGER.info("Initializing...")
        event.enqueueWork {
            this.queuedWork.forEach { it.invoke() }
            this.attemptModCompat()
        }
        Cobblemon.initialize()
    }

    // This event gets fired before init, so we need to put resource packs in EARLY
    fun onAddPackFindersEvent(event: AddPackFindersEvent) {
        val modFile = ModList.get().getModContainerById(Cobblemon.MODID).get().modInfo
        Cobblemon.builtinPacks
            .filter { it.neededMods.all(Cobblemon.implementation::isModInstalled) }
            .filter { it.packType == event.packType }
            .forEach {
                val subPath = if (it.packType == PackType.CLIENT_RESOURCES) "resourcepacks" else "datapacks"
                var packLocation = cobblemonResource("$subPath/${it.id}")
                var resourcePath = modFile.owningFile.file.findResource(packLocation.path)

                var version = modFile.version

                var pack = Pack.readMetaAndCreate(PackLocationInfo("mod/$packLocation", it.displayName, PackSource.BUILT_IN, Optional.of(KnownPack("neoforge", "mod/$packLocation", version.toString()))),
                    BuiltInPackSource.fromName { PathPackResources(it, resourcePath) },
                    it.packType,
                    PackSelectionConfig(it.activationBehaviour == ResourcePackActivationBehaviour.ALWAYS_ENABLED, Position.TOP, false)
                )

                if (pack == null) {
                    Cobblemon.LOGGER.error("Failed to register built-in pack ${it.id}. If you are in dev you can ignore this")
                    return@forEach
                }

                event.addRepositorySource { it.accept(pack) }
            }
    }

    fun on(event: RegisterEvent) {

        event.register(Registries.BLOCK_PREDICATE_TYPE) {
            CobblemonBlockPredicates.touch()
        }
        event.register(Registries.PLACEMENT_MODIFIER_TYPE) {
            CobblemonPlacementModifierTypes.touch()
        }
        event.register(Registries.DECORATED_POT_PATTERN) {
            CobblemonSherds.registerSherds()
        }

        event.register(Registries.STRUCTURE_PROCESSOR) {
            CobblemonProcessorTypes.touch()
        }

        event.register(Registries.ACTIVITY) { registry ->
            CobblemonActivities.activities.forEach {
                registry.register(cobblemonResource(it.name), it)
            }
        }

        event.register(Registries.SENSOR_TYPE) { registry ->
            CobblemonSensors.sensors.forEach { (key, sensorType) ->
                registry.register(cobblemonResource(key), sensorType)
            }
        }

        event.register(Registries.MEMORY_MODULE_TYPE) { registry ->
            CobblemonMemories.memories.forEach { (key, memoryModuleType) ->
                registry.register(cobblemonResource(key), memoryModuleType)
            }
        }

        event.register(Registries.CUSTOM_STAT) { registry ->
            Cobblemon.statistics.registerStats()
            Cobblemon.statistics.stats.forEach { entry ->
                val cobblemonStat = entry.value
                registry.register(cobblemonStat.resourceLocation, cobblemonStat.resourceLocation)
                Stats.CUSTOM.get(cobblemonStat.resourceLocation, cobblemonStat.formatter)
            }
        }
    }

    fun onDataPackSync(event: OnDatapackSyncEvent) {
        Cobblemon.dataProvider.sync(event.player ?: return)
    }

    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        this.hasBeenSynced.add(event.entity.uuid)
    }

    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        this.hasBeenSynced.remove(event.entity.uuid)
    }

    override fun isModInstalled(id: String) = ModList.get().isLoaded(id)

    override fun environment(): Environment {
        return if (FMLEnvironment.dist.isClient) Environment.CLIENT else Environment.SERVER
    }

    override fun registerPermissionValidator() {
        Cobblemon.permissionValidator = ForgePermissionValidator
    }

    override fun registerSoundEvents() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonSounds.resourceKey) { helper ->
                CobblemonSounds.register { identifier, sounds -> helper.register(identifier, sounds) }
            }
        }
    }

    override fun registerDataComponents() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonItemComponents.resourceKey) { helper ->
                CobblemonItemComponents.register { identifier, ComponentType ->  helper.register(identifier, ComponentType)}
            }
        }
    }

    override fun registerEntityDataSerializers() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS) { helper ->
                helper.register(Vec3DataSerializer.ID, Vec3DataSerializer)
                helper.register(StringSetDataSerializer.ID, StringSetDataSerializer)
                helper.register(PoseTypeDataSerializer.ID, PoseTypeDataSerializer)
                helper.register(RideBoostsDataSerializer.ID, RideBoostsDataSerializer)
                helper.register(PlatformTypeDataSerializer.ID, PlatformTypeDataSerializer)
                helper.register(IdentifierDataSerializer.ID, IdentifierDataSerializer)
                helper.register(UUIDSetDataSerializer.ID, UUIDSetDataSerializer)
                helper.register(NPCPlayerTextureSerializer.ID, NPCPlayerTextureSerializer)
            }
        }
    }

    override fun registerBlocks() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonBlocks.resourceKey) { helper ->
                CobblemonBlocks.register { identifier, block -> helper.register(identifier, block) }
            }
        }
    }

    override fun registerParticles() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonParticles.resourceKey) { helper ->
                CobblemonParticles.register { identifier, particleType -> helper.register(identifier, particleType) }
            }
        }
    }

    override fun registerMenu() {
        with(MOD_BUS) {
            addListener<RegisterEvent> { event ->
                event.register(CobblemonMenuType.resourceKey) { helper ->
                    CobblemonMenuType.register { identifier, item -> helper.register(identifier, item) }
                }
            }
        }
    }

    private fun handleBlockStripping(e: BlockEvent.BlockToolModificationEvent) {
        if (e.itemAbility == ItemAbilities.AXE_STRIP) {
            val start = e.state.block
            val result = CobblemonBlocks.strippedBlocks()[start] ?: return
            e.setFinalState(result.withPropertiesOf(e.state))
        }
    }

    override fun registerRecipeTypes() {
        with(MOD_BUS) {
            addListener<RegisterEvent> { event ->
                event.register(CobblemonRecipeTypes.resourceKey) { helper ->
                    CobblemonRecipeTypes.register { identifier, item -> helper.register(identifier, item) }
                }
            }
        }
    }

    override fun registerItems() {
        with(MOD_BUS) {
            addListener<RegisterEvent> { event ->
                event.register(CobblemonItems.resourceKey) { helper ->
                    CobblemonItems.register { identifier, item -> helper.register(identifier, item) }
                }
            }
            addListener<RegisterEvent> { event ->
                event.register(Registries.CREATIVE_MODE_TAB) { helper ->
                    CobblemonItemGroups.register { holder ->
                        val itemGroup = CreativeModeTab.builder()
                            .title(holder.displayName)
                            .icon(holder.displayIconProvider)
                            .displayItems(holder.entryCollector)
                            .build()
                        helper.register(holder.key, itemGroup)
                        itemGroup
                    }
                }
            }
        }
    }

    override fun registerEntityTypes() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonEntities.resourceKey) { helper ->
                CobblemonEntities.register { identifier, type -> helper.register(identifier, type) }
            }
        }
    }

    override fun registerEntityAttributes() {
        MOD_BUS.addListener<EntityAttributeCreationEvent> { event ->
            CobblemonEntities.registerAttributes { entityType, builder ->
                builder.add(NeoForgeMod.NAMETAG_DISTANCE)
                    .add(NeoForgeMod.SWIM_SPEED)
                    //.add(ForgeMod.ENTITY_GRAVITY)
                event.put(entityType, builder.build())
            }
        }
    }

    override fun registerBlockEntityTypes() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonBlockEntities.resourceKey) { helper ->
                CobblemonBlockEntities.register { identifier, type -> helper.register(identifier, type) }
            }
        }
    }

    override fun registerPoiTypes() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonPoiTypes.resourceKey) { helper ->
                CobblemonPoiTypes.register { identifier, type -> helper.register(identifier, type) }
            }
        }
    }

    override fun registerVillagers() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonVillagerProfessions.resourceKey) { helper ->
                CobblemonVillagerProfessions.register { identifier, profession -> helper.register(identifier, profession) }
            }
        }
    }

    override fun registerRecipeSerializers() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonRecipeSerializers.resourceKey) { helper ->
                CobblemonRecipeSerializers.register { identifier, feature -> helper.register(identifier, feature) }
            }
        }
    }

    override fun registerWorldGenFeatures() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonFeatures.resourceKey) { helper ->
                CobblemonFeatures.register { identifier, feature -> helper.register(identifier, feature) }
            }
        }
    }

    override fun addFeatureToWorldGen(feature: ResourceKey<PlacedFeature>, step: GenerationStep.Decoration, validTag: TagKey<Biome>?) {
        CobblemonBiomeModifiers.add(feature, step, validTag)
    }

    override fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>> registerCommandArgument(identifier: ResourceLocation, argumentClass: KClass<A>, serializer: ArgumentTypeInfo<A, T>) {

        //This is technically a supplier not a function (it is unused), but we need to explicitly say whether its a supplier or a function
        //Idk how to explicitly say its a supplier, so lets just make it a function by specifying a param
        this.commandArgumentTypes.register(identifier.path) { it ->
            ArgumentTypeInfos.registerByClass(argumentClass.java, serializer)
        }
    }

    private fun registerCommands(e: RegisterCommandsEvent) {
        CobblemonCommands.register(e.dispatcher, e.buildContext, e.commandSelection)
    }

    override fun <T : GameRules.Value<T>> registerGameRule(name: String, category: GameRules.Category, type: GameRules.Type<T>): GameRules.Key<T> = GameRules.register(name, category, type)


    override fun registerCriteria() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonCriteria.resourceKey) { helper ->
                CobblemonCriteria.register { identifier, criteria -> helper.register(identifier, criteria) }
            }
        }
    }

    override fun registerEntitySubPredicates() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonEntitySubPredicates.resourceKey) { helper ->
                CobblemonEntitySubPredicates.register { identifier, criteria -> helper.register(identifier, criteria) }
            }
        }
    }

    override fun registerResourceReloader(identifier: ResourceLocation, reloader: PreparableReloadListener, type: PackType, dependencies: Collection<ResourceLocation>) {
        if (type == PackType.SERVER_DATA) {
            this.reloadableResources += reloader
        }
        else {
            CobblemonNeoForgeClient.registerResourceReloader(reloader)
        }
    }

    private fun onReload(e: AddReloadListenerEvent) {
        this.reloadableResources.forEach(e::addListener)
    }

    override fun server(): MinecraftServer? = ServerLifecycleHooks.getCurrentServer()

    override fun registerCompostable(item: ItemLike, chance: Float) {
        // NeoForge uses data-driven files to determine compostable, vanilla code is ignored
        // eventually we probaly want to datagen the output file maybe?
        // check neoforged/resources/data/neoforge/data_maps/item/compostables.json for all considered entries
        // you can easily update this by running the game, putting a breakpoint anywhere that gets triggered in world and then run that in the debugger
        // ComposterBlock.COMPOSTABLES.entries.filter { it.key.toString().contains("cobblemon") }.sortedBy { it.key.toString() }.map { "\"${it.key}\": {\"chance\": ${it.value}}"}.joinToString(",")
        // returns one single json setup that you can paste in the compostables.json values block and done
    }

    override fun registerMobEffects() {
        MOD_BUS.addListener<RegisterEvent> { event ->
            event.register(CobblemonMobEffects.resourceKey) { helper ->
                CobblemonMobEffects.register { identifier, effect ->
                    helper.register(identifier, effect)
                }
            }
        }
    }

    private fun onVillagerTradesRegistry(e: VillagerTradesEvent) {
        CobblemonTradeOffers.tradeOffersFor(e.type).forEach { tradeOffer ->
            // Will never be null between 1 n 5
            e.trades[tradeOffer.requiredLevel]?.addAll(tradeOffer.tradeOffers)
        }
    }

    private fun onWanderingTraderRegistry(e: WandererTradesEvent) {
        CobblemonTradeOffers.resolveWanderingTradeOffers().forEach { tradeOffer ->
            if (tradeOffer.isRareTrade) e.rareTrades.addAll(tradeOffer.tradeOffers) else e.genericTrades.addAll(tradeOffer.tradeOffers)
        }
    }

    private fun loadRecipeCategory(e: RegisterRecipeBookCategoriesEvent) {
        e.registerBookCategories(CobblemonRecipeBookTypes.COOKING_POT, listOf(
            COOKING_POT_SEARCH.toVanillaCategory(),
            COOKING_POT_FOODS.toVanillaCategory(),
            COOKING_POT_MISC.toVanillaCategory(),
            COOKING_POT_MEDICINES.toVanillaCategory(),
            COOKING_POT_COMPLEX_DISHES.toVanillaCategory()
        ))
        CobblemonRecipeCategories.customAggregateCategories.forEach { bookType ->
            e.registerAggregateCategory(bookType.key, bookType.value)
        }
    }

    private fun onLootTableLoad(e: LootTableLoadEvent) {
        LootInjector.attemptInjection(e.name) { builder -> e.table.addPool(builder.build()) }
    }

    private fun onBuildContents(e: BuildCreativeModeTabContentsEvent) {
        val forgeInject = ForgeItemGroupInject(e)
        CobblemonItemGroups.inject(e.tabKey, forgeInject)
    }

    private fun attemptModCompat() {
        // CarryOn has a tag key for this but for some reason Forge version just doesn't work instead we do this :)
        // See https://github.com/Tschipp/CarryOn/wiki/IMC-support-for-Modders
        if (this.isModInstalled("carryon")) {
            InterModComms.sendTo("carryon", "blacklistEntity") { CobblemonEntities.POKEMON_KEY.toString() }
            InterModComms.sendTo("carryon", "blacklistEntity") { CobblemonEntities.EMPTY_POKEBALL_KEY.toString() }
        }
    }

    private class ForgeItemGroupInject(private val entries: BuildCreativeModeTabContentsEvent) : CobblemonItemGroups.Injector {

        override fun putFirst(item: ItemLike) {
            this.entries.insertFirst(ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS)
        }

        override fun putBefore(item: ItemLike, target: ItemLike) {
            this.entries.insertBefore(
                ItemStack(target),
                ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS
            )
        }

        override fun putAfter(item: ItemLike, target: ItemLike) {
            this.entries.insertAfter(
                ItemStack(target),
                ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS)
        }

        override fun putLast(item: ItemLike) {
            this.entries.accept(ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS)
        }
    }
}

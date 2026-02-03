/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.config

import com.cobblemon.mod.common.api.drop.ItemDropMethod
import com.cobblemon.mod.common.api.pokeball.catching.calculators.CaptureCalculator
import com.cobblemon.mod.common.api.pokemon.status.Statuses
import com.cobblemon.mod.common.client.gui.PokemonGUIAnimationStyle
import com.cobblemon.mod.common.config.CobblemonConfigField.CobblemonConfigSide.CLIENT
import com.cobblemon.mod.common.config.CobblemonConfigField.CobblemonConfigSide.SERVER
import com.cobblemon.mod.common.config.constraint.IntConstraint
import com.cobblemon.mod.common.pokeball.catching.calculators.CobblemonCaptureCalculator
import com.cobblemon.mod.common.util.adapters.CaptureCalculatorAdapter
import com.cobblemon.mod.common.util.adapters.IdentifierAdapter
import com.cobblemon.mod.common.util.adapters.IntRangeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import net.minecraft.resources.ResourceLocation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class CobblemonConfig {
    companion object {
        val GSON = GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeAdapter(IntRange::class.java, IntRangeAdapter)
            .registerTypeAdapter(ItemDropMethod::class.java, ItemDropMethod.adapter)
            .registerTypeAdapter(CaptureCalculator::class.java, CaptureCalculatorAdapter)
            .registerTypeAdapter(ResourceLocation::class.java, IdentifierAdapter)
            .create()
    }

    var lastSavedVersion: String = "0.0.1"

    @CobblemonConfigField(Category.Pokemon, lang = "max_pokemon_level", SERVER)
    @IntConstraint(min = 1, max = 1000)
    var maxPokemonLevel = 100

    @CobblemonConfigField(Category.Pokemon, lang = "max_pokemon_friendship", SERVER)
    @IntConstraint(min = 0, max = 1000)
    var maxPokemonFriendship = 255

    @CobblemonConfigField(Category.Pokemon, lang = "announce_drop_items", SERVER)
    var announceDropItems = true
    @CobblemonConfigField(Category.Pokemon, lang = "default_drop_item_method", SERVER)
    var defaultDropItemMethod = ItemDropMethod.ON_ENTITY
    @CobblemonConfigField(Category.Pokemon, lang = "drops_after_death_animation", SERVER)
    var dropAfterDeathAnimation = false

    @CobblemonConfigField(Category.Pokemon, lang = "ambient_pokemon_cry_ticks", SERVER)
    @LastChangedVersion("1.4.0")
    var ambientPokemonCryTicks = 1080

    @CobblemonConfigField(Category.Storage, lang = "default_key_items", SERVER)
    var defaultKeyItems = mutableSetOf<ResourceLocation>()

    @CobblemonConfigField(Category.Storage, lang = "default_box_count", SERVER)
    @IntConstraint(min = 1, max = 1000)
    @LastChangedVersion("1.7.0")
    var defaultBoxCount = 40
    @CobblemonConfigField(Category.Storage, lang = "pokemon_save_interval_seconds", SERVER)
    @IntConstraint(min = 1, max = 120)
    var pokemonSaveIntervalSeconds = 30

    @CobblemonConfigField(Category.Storage, lang = "storage_format", SERVER)
    var storageFormat = "nbt"

    @CobblemonConfigField(Category.Storage, lang = "prevent_complete_party_deposit", SERVER)
    var preventCompletePartyDeposit = false

    @CobblemonConfigField(Category.Storage, lang = "mongo_db_connection_string", SERVER)
    var mongoDBConnectionString = "mongodb://localhost:27017"
    @CobblemonConfigField(Category.Storage, lang = "mongo_db_database_name", SERVER)
    var mongoDBDatabaseName = "cobblemon"

    @CobblemonConfigField(Category.Spawning, lang = "max_vertical_correction_blocks", SERVER)
    @IntConstraint(min = 1, max = 200)
    var maxVerticalCorrectionBlocks = 64

    @CobblemonConfigField(Category.Spawning, lang = "minimum_level_range_max", SERVER)
    @IntConstraint(min = 1, max = 1000)
    var minimumLevelRangeMax = 10

    @CobblemonConfigField(Category.Spawning, lang = "enable_spawning", SERVER)
    var enableSpawning = true

    @CobblemonConfigField(Category.Spawning, lang = "minimum_distance_between_entities", SERVER)
    var minimumDistanceBetweenEntities = 8.0

    @CobblemonConfigField(Category.Spawning, lang = "max_nearby_blocks_horizontal_range", SERVER)
    var maxNearbyBlocksHorizontalRange = 4

    @CobblemonConfigField(Category.Spawning, lang = "max_nearby_blocks_vertical_range", SERVER)
    var maxNearbyBlocksVerticalRange = 2

    @CobblemonConfigField(Category.Spawning, lang = "max_vertical_space", SERVER)
    var maxVerticalSpace = 8

    @CobblemonConfigField(Category.Spawning, lang = "spawning_zone_diameter", SERVER)
    @SerializedName("spawningZoneDiameter", alternate = ["worldSliceDiameter"])
    var spawningZoneDiameter = 8

    @CobblemonConfigField(Category.Spawning, lang = "spawning_zone_height", SERVER)
    @SerializedName("spawningZoneHeight", alternate = ["worldSliceHeight"])
    var spawningZoneHeight = 16

    @CobblemonConfigField(Category.Spawning, lang = "ticks_between_spawn_attempts", SERVER)
    var ticksBetweenSpawnAttempts = 20F

    @CobblemonConfigField(Category.Spawning, lang = "minimum_spawning_zone_distance_from_player", SERVER)
    @SerializedName("minimumSpawningZoneDistanceFromPlayer", alternate = ["minimumSliceDistanceFromPlayer"])
    var minimumSpawningZoneDistanceFromPlayer = 16F

    @CobblemonConfigField(Category.Spawning, lang = "maximum_spawning_zone_distance_from_player", SERVER)
    @SerializedName("maximumSpawningZoneDistanceFromPlayer", alternate = ["maximumSliceDistanceFromPlayer"])
    var maximumSpawningZoneDistanceFromPlayer = 16 * 4F

    @CobblemonConfigField(Category.Spawning, lang = "maximum_spawns_per_pass", SERVER)
    var maximumSpawnsPerPass = 8

    @CobblemonConfigField(Category.Spawning, lang = "export_spawn_config", SERVER)
    var exportSpawnConfig = false

    @CobblemonConfigField(Category.Spawning, lang = "save_pokemon_to_world", SERVER)
    var savePokemonToWorld = true

    @CobblemonConfigField(Category.Starter, lang = "export_starter_config", SERVER)
    var exportStarterConfig = false

    @CobblemonConfigField(Category.Battles, lang = "auto_update_showdown", SERVER)
    var autoUpdateShowdown = true

    @CobblemonConfigField(Category.Battles, lang = "default_flee_distance", SERVER)
    var defaultFleeDistance = 16F * 2

    @CobblemonConfigField(Category.Battles, lang = "allow_experience_from_pvp", SERVER)
    var allowExperienceFromPvP = true

    @CobblemonConfigField(Category.Battles, lang = "experience_share_multiplier", SERVER)
    var experienceShareMultiplier = .5

    @CobblemonConfigField(Category.Battles, lang = "lucky_egg_multiplier", SERVER)
    var luckyEggMultiplier = 1.5

    @CobblemonConfigField(Category.Battles, lang = "allow_spectating", SERVER)
    var allowSpectating = true

    @CobblemonConfigField(Category.Pokemon, lang = "experience_multiplier", SERVER)
    var experienceMultiplier = 2F

    @CobblemonConfigField(Category.Spawning, lang = "pokemon_per_chunk", SERVER)
    var pokemonPerChunk = 1F

    @CobblemonConfigField(Category.Spawning, lang = "poke_snack_pokemon_per_chunk", SERVER)
    var pokeSnackPokemonPerChunk = 2F

    @CobblemonConfigField(Category.PassiveStatus, lang = "passive_statuses", SERVER)
    var passiveStatuses = mutableMapOf(
        Statuses.POISON.configEntry(),
        Statuses.POISON_BADLY.configEntry(),
        Statuses.PARALYSIS.configEntry(),
        Statuses.FROZEN.configEntry(),
        Statuses.SLEEP.configEntry(),
        Statuses.BURN.configEntry()
    )

    @CobblemonConfigField(Category.Healing, lang = "infinite_healer_charge", SERVER)
    var infiniteHealerCharge = false

    @CobblemonConfigField(Category.Healing, lang = "max_healer_charge", SERVER)
    var maxHealerCharge = 6.0f

    @CobblemonConfigField(Category.Healing, lang = "seconds_to_charge_healing_machine", SERVER)
    var secondsToChargeHealingMachine = 900.0

    @CobblemonConfigField(Category.Healing, lang = "default_faint_timer", SERVER)
    var defaultFaintTimer = 300

    @CobblemonConfigField(Category.Healing, lang = "faint_awaken_health_percent", SERVER)
    var faintAwakenHealthPercent = 0.2f

    @CobblemonConfigField(Category.Healing, lang = "heal_percent", SERVER)
    var healPercent = 0.05

    @CobblemonConfigField(Category.Healing, lang = "heal_timer", SERVER)
    var healTimer = 60

    @CobblemonConfigField(Category.Spawning, lang = "base_apricorn_tree_generation_chance", SERVER)
    var baseApricornTreeGenerationChance = 0.1F

    @CobblemonConfigField(Category.Pokemon, lang = "display_entity_level_label", CLIENT)
    var displayEntityLevelLabel = true

    @CobblemonConfigField(Category.Pokemon, lang = "display_entity_name_label", CLIENT)
    var displayEntityNameLabel = true

    @CobblemonConfigField(Category.Pokemon, lang = "display_name_for_unknown_pokemon", CLIENT)
    var displayNameForUnknownPokemon = false

    @CobblemonConfigField(Category.Pokemon, lang = "display_entity_labels_when_crouching_only", CLIENT)
    var displayEntityLabelsWhenCrouchingOnly = false

    @CobblemonConfigField(Category.Spawning, lang = "shiny_rate", SERVER)
    var shinyRate = 8192F

    @CobblemonConfigField(Category.Pokemon, lang = "shiny_notice_particles_distance", CLIENT)
    var shinyNoticeParticlesDistance = 24F

    @CobblemonConfigField(Category.Pokemon, lang = "capture_calculator", SERVER)
    var captureCalculator: CaptureCalculator = CobblemonCaptureCalculator

    @CobblemonConfigField(Category.Pokemon, lang = "player_damage_pokemon", SERVER)
    var playerDamagePokemon = true

    @CobblemonConfigField(Category.World, lang = "apple_leftovers_chance", SERVER)
    var appleLeftoversChance = 0.025

    @CobblemonConfigField(Category.World, lang = "max_roots_in_area", SERVER)
    @LastChangedVersion("1.7.0")
    var maxRootsInArea = 9

    @CobblemonConfigField(Category.World, lang = "big_root_propagation_chance", SERVER)
    @LastChangedVersion("1.7.0")
    var bigRootPropagationChance = 0.5

    @CobblemonConfigField(Category.World, lang = "energy_root_chance", SERVER)
    var energyRootChance = 0.25

    @CobblemonConfigField(Category.Pokemon, lang = "max_dynamax_level", SERVER)
    @IntConstraint(min = 0, max = 10)
    var maxDynamaxLevel = 10

    @CobblemonConfigField(Category.Spawning, lang = "tera_type_rate", SERVER)
    var teraTypeRate = 20F

    @CobblemonConfigField(Category.World, lang = "default_pastured_pokemon_limit", SERVER)
    var defaultPasturedPokemonLimit = 16

    @CobblemonConfigField(Category.World, lang = "pasture_block_update_ticks", SERVER)
    var pastureBlockUpdateTicks = 40

    @CobblemonConfigField(Category.World, lang = "pasture_max_wander_distance", SERVER)
    var pastureMaxWanderDistance = 64

    @CobblemonConfigField(Category.World, lang = "pasture_max_per_chunk", SERVER)
    var pastureMaxPerChunk = 4F

    @CobblemonConfigField(Category.World, lang = "max_inserted_fossil_items", SERVER)
    var maxInsertedFossilItems = 2

    @CobblemonConfigField(Category.Battles, lang = "walking_in_battle_animations", CLIENT)
    var walkingInBattleAnimations = false

    @CobblemonConfigField(Category.Battles, lang = "battle_wild_max_distance", SERVER)
    var battleWildMaxDistance = 12F

    @CobblemonConfigField(Category.World, lang = "trade_max_distance", SERVER)
    var tradeMaxDistance = 12F

    @CobblemonConfigField(Category.Battles, lang = "battle_pvp_max_distance", SERVER)
    @SerializedName("battlePvPMaxDistance", alternate = ["BattlePvPMaxDistance"])
    var battlePvPMaxDistance = 32F

    @CobblemonConfigField(Category.Battles, lang = "battle_spectate_max_distance", SERVER)
    var battleSpectateMaxDistance = 64F

    @CobblemonConfigField(Category.Pokedex, lang = "max_pokedex_scanning_detection_range", SERVER)
    var maxPokedexScanningDetectionRange = 10.0

    @CobblemonConfigField(Category.Pokedex, lang = "hide_unimplemented_pokemon_in_the_pokedex", CLIENT)
    var hideUnimplementedPokemonInThePokedex = false

    @CobblemonConfigField(Category.Interface, lang = "summary_pokemon_follow_cursor", CLIENT)
    var summaryPokemonFollowCursor = true

    @CobblemonConfigField(Category.Interface, lang = "party_portrait_animations", CLIENT)
    var partyPortraitAnimations = PokemonGUIAnimationStyle.NEVER_ANIMATE

    @CobblemonConfigField(Category.Interface, lang = "pc_profile_animations", CLIENT)
    var pcProfileAnimations = PokemonGUIAnimationStyle.ANIMATE_SELECTED

    @CobblemonConfigField(Category.Interface, lang = "summary_profile_animations", CLIENT)
    var summaryProfileAnimations = PokemonGUIAnimationStyle.ANIMATE_SELECTED

    @CobblemonConfigField(Category.Interface, lang = "animate_battle_tiles", CLIENT)
    var animateBattleTiles = false

    // Disabled as non-tenable for 1.7 until all the posers catch up with this property.
    // @CobblemonConfigField(Category.Riding, lang = "third_person_view_bobbing", CLIENT)
    var thirdPersonViewBobbing = true

    @CobblemonConfigField(Category.Riding, lang = "invert_roll", CLIENT)
    var invertRoll = false

    @CobblemonConfigField(Category.Riding, lang = "invert_pitch", CLIENT)
    var invertPitch = false

    @CobblemonConfigField(Category.Riding, lang = "invert_yaw", CLIENT)
    var invertYaw = false

    @CobblemonConfigField(Category.Riding, lang = "x_axis_sensitivity", CLIENT)
    var xAxisSensitivity = 1.0

    @CobblemonConfigField(Category.Riding, lang = "y_axis_sensitivity", CLIENT)
    var yAxisSensitivity = 1.0

    @CobblemonConfigField(Category.Riding, lang = "swap_x_and_y_axes", CLIENT)
    var swapXAndYAxes = false

    @CobblemonConfigField(Category.Riding, lang = "automatic_righting_delay", CLIENT)
    var rightingDelay = -1.0

    @CobblemonConfigField(Category.Riding, lang = "disable_roll", CLIENT)
    var disableRoll = false

    @CobblemonConfigField(Category.Riding, lang = "display_controls_duration_seconds", CLIENT)
    var displayControlSeconds = 0

    @CobblemonConfigField(Category.Riding, lang = "infinite_ride_stamina", SERVER)
    var infiniteRideStamina = false

    @CobblemonConfigField(Category.Riding, lang = "remember_riding_camera", CLIENT)
    var rememberRidingCamera = false

    @CobblemonConfigField(Category.Riding, lang = "enable_in_flight_dismounting", CLIENT)
    var enableInFlightDismounting = false

    @CobblemonConfigField(Category.Debug, lang = "enable_debug_keys", CLIENT)
    var enableDebugKeys = false

    @CobblemonConfigField(Category.Spawning, lang = "despawner_near_distance", SERVER)
    var despawnerNearDistance = 32f

    @CobblemonConfigField(Category.Spawning, lang = "despawner_far_distance", SERVER)
    var despawnerFarDistance = 96f

    @CobblemonConfigField(Category.Spawning, lang = "despawner_min_age_ticks", SERVER)
    var despawnerMinAgeTicks = 600

    @CobblemonConfigField(Category.Spawning, lang = "despawner_max_age_ticks", SERVER)
    var despawnerMaxAgeTicks = 3600

    fun clone(): CobblemonConfig {
        val newConfig = CobblemonConfig()
        CobblemonConfig::class.memberProperties.forEach { property ->
            if (property is kotlin.reflect.KMutableProperty<*>) {
                property.isAccessible = true
                val value = property.getter.call(this)
                if (value != null) {
                    property.setter.call(newConfig, value)
                }
            }
        }
        return newConfig
    }
}
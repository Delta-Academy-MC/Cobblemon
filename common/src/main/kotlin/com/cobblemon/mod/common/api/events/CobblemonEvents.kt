/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.events

import com.cobblemon.mod.common.api.events.battles.*
import com.cobblemon.mod.common.api.events.battles.instruction.FormeChangeEvent
import com.cobblemon.mod.common.api.events.battles.instruction.MegaEvolutionEvent
import com.cobblemon.mod.common.api.events.battles.instruction.TerastallizationEvent
import com.cobblemon.mod.common.api.events.battles.instruction.ZMoveUsedEvent
import com.cobblemon.mod.common.api.events.berry.BerryHarvestEvent
import com.cobblemon.mod.common.api.events.berry.BerryMutationOfferEvent
import com.cobblemon.mod.common.api.events.berry.BerryMutationResultEvent
import com.cobblemon.mod.common.api.events.berry.BerryYieldCalculationEvent
import com.cobblemon.mod.common.api.events.cooking.PokeSnackSpawnPokemonEvent
import com.cobblemon.mod.common.api.events.drops.LootDroppedEvent
import com.cobblemon.mod.common.api.events.entity.*
import com.cobblemon.mod.common.api.events.farming.ApricornHarvestEvent
import com.cobblemon.mod.common.api.events.fishing.*
import com.cobblemon.mod.common.api.events.interact.SelectPartyPokemon
import com.cobblemon.mod.common.api.events.item.LeftoversCreatedEvent
import com.cobblemon.mod.common.api.events.pokebag.PokeBagContainerValidCheckEvent
import com.cobblemon.mod.common.api.events.pokebag.PokeBagOpenRequestEvent
import com.cobblemon.mod.common.api.events.pokeball.PokeBallCaptureCalculatedEvent
import com.cobblemon.mod.common.api.events.pokeball.PokemonCatchRateEvent
import com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitBlockEvent
import com.cobblemon.mod.common.api.events.pokeball.ThrownPokeballHitEvent
import com.cobblemon.mod.common.api.events.pokedex.scanning.PokemonScannedEvent
import com.cobblemon.mod.common.api.events.pokemon.*
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionAcceptedEvent
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionDisplayEvent
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionTestedEvent
import com.cobblemon.mod.common.api.events.pokemon.healing.PokemonHealedEvent
import com.cobblemon.mod.common.api.events.pokemon.interaction.ExperienceCandyUseEvent
import com.cobblemon.mod.common.api.events.pokemon.interaction.PokemonInteractionGUICreationEvent
import com.cobblemon.mod.common.api.events.starter.StarterChosenEvent
import com.cobblemon.mod.common.api.events.storage.*
import com.cobblemon.mod.common.api.events.trade.TradeEvent
import com.cobblemon.mod.common.api.events.world.BigRootPropagatedEvent
import com.cobblemon.mod.common.api.reactive.CancelableObservable
import com.cobblemon.mod.common.api.reactive.EventObservable
import com.cobblemon.mod.common.api.reactive.Observable.Companion.filter
import com.cobblemon.mod.common.api.reactive.Observable.Companion.map
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.api.riding.events.SelectDriverEvent
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.server.level.ServerPlayer

@Suppress("unused")
object CobblemonEvents {

    @JvmField
    val POKEMON_PROPERTY_INITIALISED = SimpleObservable<Unit>()

    @JvmField
    val COBBLEMON_INITIALISED = SimpleObservable<Unit>()

    @JvmField
    val DATA_SYNCHRONIZED = SimpleObservable<ServerPlayer>()

    @JvmField
    val SHOULDER_MOUNT = CancelableObservable<ShoulderMountEvent>()

    @JvmField
    val FRIENDSHIP_UPDATED = EventObservable<FriendshipUpdatedEvent>()

    @JvmField
    val FULLNESS_UPDATED = EventObservable<FullnessUpdatedEvent>()

    @JvmField
    val POKEMON_FAINTED = EventObservable<PokemonFaintedEvent>()

    @JvmField
    val EVOLUTION_ACCEPTED = CancelableObservable<EvolutionAcceptedEvent>()

    @JvmField
    val EVOLUTION_DISPLAY = EventObservable<EvolutionDisplayEvent>()

    @JvmField
    val EVOLUTION_TESTED = EventObservable<EvolutionTestedEvent>()

    @JvmField
    val EVOLUTION_COMPLETE = EventObservable<EvolutionCompleteEvent>()

    @JvmField
    val POKEMON_NICKNAMED = CancelableObservable<PokemonNicknamedEvent>()

    @JvmField
    val THROWN_POKEBALL_HIT = CancelableObservable<ThrownPokeballHitEvent>()

    @JvmField
    val THROWN_POKEBALL_HIT_BLOCK = CancelableObservable<ThrownPokeballHitBlockEvent>()

    @JvmField
    val POKEMON_CATCH_RATE = EventObservable<PokemonCatchRateEvent>()

    @JvmField
    val POKE_BALL_CAPTURE_CALCULATED = EventObservable<PokeBallCaptureCalculatedEvent>()

    @JvmField
    val POKEMON_CAPTURED = EventObservable<PokemonCapturedEvent>()

    @JvmField
    val POKEMON_CAPTURE_PRE = CancelableObservable<PokemonCapturePreEvent>()

    @JvmField
    val BATTLE_USE_MOVE = EventObservable<BattleUseMoveEvent>()

    @JvmField
    val BATTLE_PICK_MOVE = EventObservable<BattlePickMoveEvent>()

    @JvmField
    val BATTLE_SWITCH = EventObservable<BattleSwitchEvent>()

    @JvmField
    val BATTLE_TERA = EventObservable<BattleTerastallizeEvent>()

    @JvmField
    val BATTLE_HP = EventObservable<BattleHPEvent>()

    @JvmField
    val POKEMON_HEALTH_CHANGE = EventObservable<PokemonHealthChangeEvent>()

    @JvmField
    val GIMMIGHOUL_FAKE = EventObservable<GimmighoulEvent>()

    @JvmField
    val NPC_EVENT = EventObservable<NPCEvent>()

    @JvmField
    val NPC_DEFINE_BATTLE_ACTOR = EventObservable<NPCDefineBattleActorEvent>()

    @JvmField
    val PC_EVENT = CancelableObservable<PCEvent>()

    @JvmField
    val TRADE_EVENT = CancelableObservable<TradeEvent>()

    @JvmField
    val BATTLE_START = CancelableObservable<BattleStartEvent>()

    @JvmField
    val FOSSIL_REVIVED = EventObservable<FossilRevivedEvent>()

    @JvmField
    val BATTLE_STARTED_PRE = CancelableObservable<BattleStartedEvent.Pre>()

    @JvmField
    val BATTLE_STARTED_POST = EventObservable<BattleStartedEvent.Post>()

    @JvmField
    val BATTLE_FLED = EventObservable<BattleFledEvent>()

    @JvmField
    val BATTLE_FLEEING = EventObservable<FleeEvent>()

    @JvmField
    val BATTLE_VICTORY = EventObservable<BattleVictoryEvent>()

    @JvmField
    val BATTLE_FAINTED = EventObservable<BattleFaintedEvent>()

    // instructions
    @JvmField
    val MEGA_EVOLUTION = EventObservable<MegaEvolutionEvent>()

    @JvmField
    val TERASTALLIZATION = EventObservable<TerastallizationEvent>()

    @JvmField
    val ZPOWER_USED = EventObservable<ZMoveUsedEvent>()

    @JvmField
    val FORME_CHANGE = EventObservable<FormeChangeEvent>()

    @JvmField
    val POKEMON_SENT_PRE = CancelableObservable<PokemonSentEvent.Pre>()

    @JvmField
    val POKEMON_SENT_POST = EventObservable<PokemonSentEvent.Post>()

    @JvmField
    val POKEMON_RECALL_PRE = CancelableObservable<PokemonRecallEvent.Pre>()

    @JvmField
    val POKEMON_RECALL_POST = EventObservable<PokemonRecallEvent.Post>()

    @JvmField
    val TRADE_EVENT_PRE = CancelableObservable<TradeEvent.Pre>()

    @JvmField
    val TRADE_EVENT_POST = EventObservable<TradeEvent.Post>()

    @JvmField
    val RIDE_EVENT_PRE = CancelableObservable<RidePokemonEvent.Pre>()

    @JvmField
    val RIDE_EVENT_APPLY_STAMINA = EventObservable<RidePokemonEvent.ApplyStamina>()

    @JvmField
    val RIDE_EVENT_POST = EventObservable<RidePokemonEvent.Post>()

    @JvmField
    val LEVEL_UP_EVENT = EventObservable<LevelUpEvent>()

    @JvmField
    val POKEMON_HEALED = CancelableObservable<PokemonHealedEvent>()

    @JvmField
            /** CLIENT ONLY! */
    val POKEMON_INTERACTION_GUI_CREATION = EventObservable<PokemonInteractionGUICreationEvent>()

    @JvmField
    val POKEMON_ENTITY_SAVE = EventObservable<PokemonEntitySaveEvent>()

    @JvmField
    val POKEMON_ENTITY_LOAD = CancelableObservable<PokemonEntityLoadEvent>()

    @JvmField
    val POKEMON_ENTITY_SAVE_TO_WORLD = CancelableObservable<PokemonEntitySaveToWorldEvent>()

    @JvmField
    val ENTITY_SPAWN = CancelableObservable<SpawnEvent<*>>()

    @JvmField
    val SHINY_CHANCE_CALCULATION = EventObservable<ShinyChanceCalculationEvent>()

    @JvmField
    val POKEMON_ENTITY_SPAWN = ENTITY_SPAWN
        .pipe(
            filter { it.entity is PokemonEntity },
            map {
                @Suppress("UNCHECKED_CAST")
                it as SpawnEvent<PokemonEntity>
            }
        )

    @JvmField
    val EXPERIENCE_GAINED_EVENT_PRE = CancelableObservable<ExperienceGainedEvent.Pre>()

    @JvmField
    val EXPERIENCE_GAINED_EVENT_POST = EventObservable<ExperienceGainedEvent.Post>()

    @JvmField
    val EXPERIENCE_CANDY_USE_PRE = CancelableObservable<ExperienceCandyUseEvent.Pre>()

    @JvmField
    val EXPERIENCE_CANDY_USE_POST = EventObservable<ExperienceCandyUseEvent.Post>()

    @JvmField
    val EV_GAINED_EVENT_PRE = CancelableObservable<EvGainedEvent.Pre>()

    @JvmField
    val EV_GAINED_EVENT_POST = EventObservable<EvGainedEvent.Post>()

    @JvmField
    val HYPER_TRAINED_IV_PRE = CancelableObservable<HyperTrainedIvEvent.Pre>()

    @JvmField
    val HYPER_TRAINED_IV_POST = EventObservable<HyperTrainedIvEvent.Post>()

    @JvmField
    val POKEMON_RELEASED_EVENT_PRE = CancelableObservable<ReleasePokemonEvent.Pre>()

    @JvmField
    val POKEMON_RELEASED_EVENT_POST = EventObservable<ReleasePokemonEvent.Post>()

    @JvmField
    val RENAME_PC_BOX_EVENT_PRE = CancelableObservable<RenamePCBoxEvent.Pre>()

    @JvmField
    val RENAME_PC_BOX_EVENT_POST = EventObservable<RenamePCBoxEvent.Post>()

    @JvmField
    val WALLPAPER_COLLECTION_EVENT = EventObservable<WallpaperCollectionEvent>()

    @JvmField
    val WALLPAPER_UNLOCKED_EVENT = CancelableObservable<WallpaperUnlockedEvent>()

    @JvmField
    val CHANGE_PC_BOX_WALLPAPER_EVENT_PRE = CancelableObservable<ChangePCBoxWallpaperEvent.Pre>()

    @JvmField
    val CHANGE_PC_BOX_WALLPAPER_EVENT_POST = EventObservable<ChangePCBoxWallpaperEvent.Post>()

    @JvmField
    val LOOT_DROPPED = CancelableObservable<LootDroppedEvent>()

    @JvmField
    val STARTER_CHOSEN = CancelableObservable<StarterChosenEvent>()

    @JvmField
    val POKEMON_SCANNED = EventObservable<PokemonScannedEvent>()

    @JvmField
    val APRICORN_HARVESTED = EventObservable<ApricornHarvestEvent>()

    // Berries
    @JvmField
    val BERRY_HARVEST = EventObservable<BerryHarvestEvent>()

    @JvmField
    val BERRY_MUTATION_OFFER = EventObservable<BerryMutationOfferEvent>()

    @JvmField
    val BERRY_MUTATION_RESULT = EventObservable<BerryMutationResultEvent>()

    @JvmField
    val BERRY_YIELD = EventObservable<BerryYieldCalculationEvent>()

    @JvmField
    val LEFTOVERS_CREATED = CancelableObservable<LeftoversCreatedEvent>()

    @JvmField
    val BIG_ROOT_PROPAGATED = CancelableObservable<BigRootPropagatedEvent>()

    @JvmField
    val HELD_ITEM_PRE = CancelableObservable<HeldItemEvent.Pre>()

    @JvmField
    val HELD_ITEM_POST = EventObservable<HeldItemEvent.Post>()

    @JvmField
    val COSMETIC_ITEM_PRE = CancelableObservable<HeldItemEvent.Pre>()

    @JvmField
    val COSMETIC_ITEM_POST = EventObservable<HeldItemEvent.Post>()

    @JvmField
    val POKEMON_GAINED = EventObservable<PokemonGainedEvent>()

    @JvmField
    val POKEMON_SEEN = CancelableObservable<PokemonSeenEvent>()

    @JvmField
    val POKEMON_ASPECTS_CHANGED = EventObservable<PokemonAspectsChangedEvent>()

    @JvmField
    val POKEDEX_DATA_CHANGED_PRE = CancelableObservable<PokedexDataChangedEvent.Pre>()

    @JvmField
    val POKEDEX_DATA_CHANGED_POST = EventObservable<PokedexDataChangedEvent.Post>()

    // Fishing
    @JvmField
    val BAIT_SET = CancelableObservable<BaitSetEvent>()

    @JvmField
    val BAIT_SET_PRE = CancelableObservable<BaitSetEvent>()

    @JvmField
    val BAIT_CONSUMED = CancelableObservable<BaitConsumedEvent>()

    @JvmField
    val POKEROD_CAST_PRE = CancelableObservable<PokerodCastEvent.Pre>()

    @JvmField
    val POKEROD_CAST_POST = EventObservable<PokerodCastEvent.Post>()

    @JvmField
    val POKEROD_REEL = CancelableObservable<PokerodReelEvent>()

    @JvmField
    val BOBBER_SPAWN_POKEMON_PRE = CancelableObservable<BobberSpawnPokemonEvent.Pre>()

    @JvmField
    val BOBBER_SPAWN_POKEMON_MODIFY = EventObservable<BobberSpawnPokemonEvent.Modify>()

    @JvmField
    val BOBBER_SPAWN_POKEMON_POST = EventObservable<BobberSpawnPokemonEvent.Post>()

    @JvmField
    val POKE_SNACK_SPAWN_POKEMON_PRE = CancelableObservable<PokeSnackSpawnPokemonEvent.Pre>()

    @JvmField
    val POKE_SNACK_SPAWN_POKEMON_POST = EventObservable<PokeSnackSpawnPokemonEvent.Post>()

    @JvmField
    val BAIT_EFFECT_REGISTRATION = EventObservable<BaitEffectFunctionRegistryEvent>()

    @JvmField
    val SPAWN_BUCKET_CHOSEN = EventObservable<SpawnBucketChosenEvent>()

    // Baits and Lures
    @JvmField
    val BAIT_SPAWN_POKEMON_MODIFY = EventObservable<BaitSpawnPokemonEvent.Modify>()


    @JvmField
    val COLLECT_EGG = CancelableObservable<CollectEggEvent>()

    @JvmField
    val HATCH_EGG_PRE = CancelableObservable<HatchEggEvent.Pre>()

    @JvmField
    val HATCH_EGG_POST = EventObservable<HatchEggEvent.Post>()

    // -------------------------------------------------------------------------------------
    //
    // Riding
    //
    // -------------------------------------------------------------------------------------
    @JvmField
    val SELECT_DRIVER = EventObservable<SelectDriverEvent>()

    @JvmField
    val BATTLE_CHOICE_REQUESTED = EventObservable<BattleChoiceRequestedEvent>()

    @JvmField
    val BATTLE_CHOICE_MADE = EventObservable<BattleChoiceMadeEvent>()

    @JvmField
    val POKE_BAG_CONTAINER_VALID_CHECK = SimpleObservable<PokeBagContainerValidCheckEvent>()

    @JvmField
    val POKE_BAG_OPEN_REQUEST = SimpleObservable<PokeBagOpenRequestEvent>()

    @JvmField
    val MOVE_PARTY_POKEMON = EventObservable<MovePartyPokemon>()

    @JvmField
    val MOVE_PC_POKEMON = EventObservable<MovePCPokemon>()

    @JvmField
    val SWAP_PARTY_POKEMON = EventObservable<SwapPartyPokemon>()

    @JvmField
    val SWAP_PC_POKEMON = EventObservable<SwapPCPokemon>()

    @JvmField
    val SWAP_PC_PARTY_POKEMON = EventObservable<SwapPCPartyPokemon>()

    @JvmField
    val SELECT_PARTY_POKEMON = EventObservable<SelectPartyPokemon>()

    @JvmField
    val MOVESET_CHANGED = EventObservable<MovesetChangedEvent>()

    @JvmField
    val TRANSFORMATION_PRE_EVENT = EventObservable<TransformationPreEvent>()

    @JvmField
    val TRANSFORMATION_VALIDATION_EVENT = EventObservable<TransformationValidationEvent>()
}
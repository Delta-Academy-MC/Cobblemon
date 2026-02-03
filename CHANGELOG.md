# Changelog
## [1.7.3 (January 31st, 2026)](#1-7-3)

### Fixes
- Fixed game crashing when removing national pokedex using datapacks

### Changes
- Renamed `chargeGainedPerTick` config to `secondsToChargeHealingMachine`.
- Made Blocks of Gold count as Big Nuggets when held by a Pokémon (for Fling functionality)
- Substantially optimised spawning checks mainly by front-loading biome filtering.

### Fixes
- Fixed Particles sometimes facing the wrong direction (looking at you, Swords Dance)
- Fixed PCs always opening at box 2 instead of box 1.
- Fixed not being able to do complex item requirements aside from just NBT with evolution conditions, requirements and fossil items.
- Fixed the usage `hiddenability` in `pokegive` or other spawn commands resulting in a forced hidden ability
- Fixed the consumption of friendship berries (or EV berries) not making any noise
- Fixed Instantly breaking and replacing a fossil analyzer with any block entity crashing the game in a complete multi-structure
- Fixed players getting disconnected when sidemods update a Pokémon's teratype
- Fixed fling not using Item Names for minecraft held items that substitute Pokémon items
- Fixed evolutions sometimes preventing players from logging in to servers
- Fixed rendering of shoulder-mounted Pokémon desyncing between clients
- Fixed apricorn chest boats forgetting their inventories when being unloaded
- Fixed moves not updating correctly between form changes resulting in illegal movesets
- Fixed crash sometimes occurring with the "Oritech" mod
- Fixed crashes due to an incorrect Java version handing out an obscure crash.
- Fixed some berries being able to rarely get too many berries and cause a crash.
- Fixed status curing berries not playing the berry eating sound, same for healing berries used mid-battle.
- Fixed owned Pokémon sometimes being un-interactable after the player relogs fast
- Fixed field name in evolution requirements for Spewpa Pokeball.
- Fixed LevelUpCriterion logic to correctly check that the Pokémon is a preEvo.
- Fixed `hide_additional_tooltip` vanilla flag not properly hiding tooltips on pokerod and bait items
- Removed a number of scenarios in which a Pokémon battle may send out a Pokémon into collision geometry.
- Fixed NPCs using Pokémon outside of their pool when a Pokémon name had a typo.
- Fixed an issue with datapacked species features not being applied properly when relogging.
- Fixed Pokémon marked as silent still playing shiny sounds and effects.
- Fixed an issue with newer versions of Fabric API where underground Pokémon were spawning in The End.
- Fixed spawning not working well when you're at high points surrounded by lower altitude spawning areas, such as flying.
- Fixed some Pokémon having erroneous tutor moves if another move included a valid tutor move as a substring.
- Fixed certain Pokémon with forms not having appropriate stock Pokédex entries.
- Fixed issue with Pokédex Scanner that caused the open/close overlay to have the wrong opacity values
- Fixed dragon's breath not being usable on the restoration tank when it should be
- Fixed Moon Stones not interacting properly with dripstone blocks.
- Fixed some effects like particles from fishing rods appearing for players in the same coordinates in another world.
- Fixed an issue with Sketch where the Pokémon using Sketch would not properly learn moves with special characters in their name (e.g. King's Shield, Baby-Doll Eyes, etc.)
- Fixed wild Pokémon sometimes spawning with incorrect friendship values
- Fixed typo while saving/loading NPCEntity causes data loss
- Fixed an issue where catching a Pokémon while it was leashed to a fence would not update the fence.
- Fixed the `dimensions` spawning condition using the incorrect ResourceLocation, causing it to not function properly.
- Fix issue where locator X axis was not aligned with blockbench.
- Fix issue where particle effects that play on frame 1 on sendout would never play (Gastly)
- The Wiglett line will now sink in water again.
- Fixed the Sprigatito line's portraits being zoomed in too far.
- Fixed Floragato's battle cry animation from breaking.
- Fixed the block texture name for the Pep-Up Flower being inaccurate
- Fixed issue where the restoration tank would not accept valid items from a hopper.
- Fixed energy root not being shearable
- Fixed energy root always spreading into more energy roots instead of by chance (same as big root)
- Fixed issue where Pokémon spawned by the "spawnallpokemon" command potentially receiving a raft.
- Fixed logspam on NeoForge when adorn is not installed
- Fixed Cobblemon crashing if it tries to load a bedrock model not meant for cobblemon (example: Qlipoth Awakening)
- Fixed Berries (and thus mulches) not being plantable on Farmers delight rich soil farmland
- Fixed wild Pokémon vanishing when third party mods try to tame them the "vanilla" way
- Fixed Pokémon not being able to path over skulk veins, pressure plates, fence gates, signs, lanterns, chains, and many other short blocks.
- Fixed some cases in which Pokémon could not path over fence posts situations.
- Fixed flyers not being able to do vertical takeoff if surrounded by blocks.
- Fixed swimming Pokémon attempting to swim up through solid blocks.
- Fixed Pokémon surface swimming diving downward a block for the duration of the swim.
- Improved flyers avoiding getting stuck on fence posts.
- Fixed air balloon battle text not correctly displaying the Pokémon or item name
- Fixed an issue where items retrieved from a Display case would disappear if a player's inventory is full
- Fixed Pokédex Scanner not respecting the "Invert Mouse" option
- Fixed recoil eyes on Shellder & Cloyster.
- Fixed Shroomish spinning at lightning speed when fainting (no longer allowed to let it rip.)
- Fixed Pokédex Scanner not respecting the "Invert Mouse" option.
- Fixed a crash due to a ConcurrentModificationException that could occur during world generation.
- Fixed Quaxly's animations showing hidden wing sets
- Fixed Moon Ball moon phase logic to actually work correctly
- Fixed `/pokedex printcalculations` to now show the correct percentage completed of the Pokedex
- Fixed mod incompatibility with the `Raised` mod
- Fixed a vulnerability that could cause party and PC rollbacks under specific circumstances.
- Fixed a rare edge case where sorting your PC could be rolled back later.
- Fixed BotanyPots built-in integration
- Fixed shading of berries on berry trees.
- Fixed static text cursor for nickname input in summary.
- Fixed Cobblemon brewing recipes placing result into all slots, and not just slots with correct ingredients inside the brewing stand.
- Fixed an issue where hoppers and brewing stands were not recognizing Cobblemon brewing recipes.
- Fixed global species features... not working. Since they were created. Oops.
- Fixed Pokémon with alternate forms being created with an incorrect 'forced' tag on their ability.
- Fixed busted abilities and moves in Pokémon data due to removed datapacks etc. causing storage corruption. It now just rerolls their ability / uses Tackle.
- Fixed singular Pokémon corruption causing entire storage corruption. Storages will now skip corrupted Pokémon and print an error to console.
- Fixed species additions not being able to properly mark a species as implemented.
- Fixed Pokémon item models not showing a glint when enchanted.
- Fixed the missing Terracotta Sun Stone Ore smelting and blasting recipes
- Improved experience gain from smelting evolution stone ores to better match other ore types
- Fixed some specific bag items not being dropped when used in battle.
- The Corphish line will now sink in water.
- Fixed the "use all berry bait" achievement not being progressed
- Fixed bobber hook and berry sprouts texture sizes causing mipmap issues.
- Fixed Ally Switch not updating party positions. 
- Fixed head locator not taking into account scale for positioning.
- Fixed Pokémon step counts not being saved when recalling them to a Pokéball or reentering the world.
- Fixed NPC pokémon not being linked to their NPC, which previously caused NPC pokémon to be catchable.
- Fixed an uncommon error caused by scanning a pokémon on a player's shoulder.
- Fixed Alcremie and Eevee clipping into the player head when shoulder mounted
- Fixed a case where spawning could fail and log warnings when nothing wrong was happening.
- Fixed incorrect camera pivot on Bird, Jet, and Dolphin mounts, leading to some disorienting riding.
- Fixed players suffocating while on vanilla mounts.
- Fixed a graphics crash that could occur on some machines when campfire pots were nearby.
- Fixed ride controls overlay being displayed to passengers. The passengers probably don't need to know about those.
- Fixed mount jumping so that it doesn't switch back to ground animations.
- Fixed ridden Pokémon land collision and some issues around stepping up blocks. Land mounts are smoother overall now.
- Fixed Furfrou's pink and magenta trims being swapped. Only a man could make that mistake.
- Fixed friendship being reset to the default value when evolving a Pokémon.
- Fixed Combees not depositing honey upon leaving a hive if they entered it with nectar.
- Fixed pastured Combees not moving during the night or in rain.
- Fixed a case in which a Combee might try to path to a flower that no longer exists.
- Fixed Combees attempting to enter a full hive.
- Fixed a crash involving Combees and hives. 500 bee bugs. Bugged bees? We can be bees. Mark, this is good news.
- Fixed a crash related to NPC navigation.
- Fixed a crash when opening a PC box with certain wallpapers while using *VulkanMod*.
- Fixed a crash when you place a block in the way of hearty grains 2nd block.
- Fixed passengers hearing the shiny noise of a ridden shiny Pokémon.
- Ponigiri can no longer be eaten at full hunger.
- Fixed Poké Snack spawning sounds not coming from the block.
- Medicinal Brew's Campfire Pot recipe now correctly displays that it can be made using an empty glass bottle or a filled bottle.
- Fixed glass bottle not being returned when slathering honey on a saccharine leaf block.
- Fixed saccharine hanging signs sometimes dropping their oak wood counterpart when being broken.
- Fixed a bunch of incompatibilities with NeoForge mods.
- Fixed a bug where wind charges hitting pasture blocks would recall all Pokémon in it.
- Fixed the Home on the Range advancement triggering when opening the Pokémon interaction wheel.
- Fixed recipes that use concrete not working on NeoForge.
- Fixed Boltund's model.
- Fixed Cyclizar's textures.
- Fixed mounted Pokémon not playing sounds.
- Fixed players failing to join servers randomly.
- Fixed some Pokémon having no stamina for flying.
- Fixed `/pctake` not respecting the actual size of the PC.

### Developer
- Poké Ball GUI textures now respect the ball's namespace, enabling proper texture loading for custom balls from other mods.

- Pokemon now have a fireImmune attribute in their behaviour that can be set to true to ignore all fire damage (lava, magma blocks, etc.)
  `JSON
  {
    "behaviour": {
      "fireImmune": true
    }
  }
  `
- The IVs class has now been extended to include Hyper Trained values.
- Added `Pokemon#hyperTrainIV()` and `IVs#setHyperTrainedIV(Stat, Int)`.
- Added `HyperTrainedIvEvent.Pre` and `HyperTrainedIvEvent.Post`.
- Added `Pokemon#validateMoveSet()` to validate an existing Pokemon's moveset, clearing illegal moves.
- Added a `hoverText` option to PartySelectCallback, to display a tooltip on hovering over a Pokémon in the selection screen.
- `PokemonEntity` instances spawned into the world now appropriately finalize the spawn for mod compatibility.
- Added PokedexManager.obtain as a replacement for .catch which is not a friendly function name in Java.
- Added `Pokemon#hyperTrainIV()` and `IVs#setHyperTrainedIV(Stat, Int)`
- `ElementalType` now implements `ShowdownIdentifiable` to ensure the communcation with showdown stays consistent (also in regards to TeraTypes)
- Pokemon no longer have a change observable
  

### Changes
- When using the `cobblemon` or `generation_9` capture calculators a critical capture with a single shake will always play for successful captures when you've already registered the Pokémon as caught in your Pokédex.
- Pokémon can now have a behaviour changing the value of a species feature on lightning hit: 
  ```JSON
  { 
    "behaviour": {
      "lightningHit": {
        "rotateFeatures": [
          {
            "key": "mooshtank",
            "chain": ["red", "brown"]
          }
        ]
      }
    }
  }
  ```
  This will change the value of `mooshtank` from `red` to `brown` and vice versa when a lightning strikes this Pokémon. Only chains of which a value is the current value will be considered, so multiple entries for the same key with different chains can be added.
- Removed Npc interface from NPCEntity, the interface is unused and in vanilla is only implemented by VillagerEntity as a means to disable villagers with the `spawn-npcs` server property.
- Added new `Observable#subscribe` methods that take Java Consumers to make usage in Java a little cleaner.
- Annotated a bunch of Kotlin methods and fields for cleaner Java names.
- Added `PartyStore#isEmpty()`.
- Added `ElementalTypes#getRandomType()`.
- Added `IVs#getEffectiveBattleTotal()`.
- Added `IVs.MAX_TOTAL` constant.
- Added `PokemonStats#total()`.
- Add `NatureAdapter` for serializing and deserializing Natures using Gson.
- Added `Pokemon.getBaseRideStat(RidingStat)` for getting the base value of the given stat.
- Fixed `Species#create` using the species name instead of identifier, which had led to certain mismatches generating random pokémon.

### MoLang & Datapacks
- The following usages for item predicates can now use item conditions like advancements do, you can learn about them in the [Minecraft wiki](https://minecraft.wiki/w/Advancement_definition#minecraft:filled_bucket)
  - The `requiredContext` for an item interaction evolution
  - The `itemCondition` for a held item evolution requirement
  - The `fossils` for a fossil entry
- Added MoLang flows for `poke_ball_capture_calculated`, `evolution_tested`, `evolution_accepted`, `evolution_completed`
- Added `interpolate` boolean property to animated textures to allow gradual colour changes between frames.
- Fixed species additions not being capable of changing implemented status.
- Added support for action effects that are triggered by `|-activate|` Showdown instructions. `activate_{effect_id}` is the syntax.
- Added MoLang functions for rendering items `render_item(item_id, locator_name)` and `clear_items()`.
- Fixed location spawn filter components causing crashes
- Added `pokemon` as an available MoLang function for the `battleActor` functions.
- Added `spawn_pokemon` as an available MoLang function for the `worldHolder` functions.
- Added `attempt_wild_battle` as an available MoLang function.
- Added `pokemon` as an available Molang function for the `battleActor` functions.
- Fixed `heldItem` property inside spawn files not working and causing crashes
- Fixed `spawn_bedrock_particles` MoLang causing crashes when used in a server environment
- The following move sources are now valid for the `moves` array in species data:
  - `legacy:{move}`
  - `special:{move}`
- The Pokédex form lang key definition now follows `cobblemon.ui.pokedex.info.form.{species}-{formname}` instead of `cobblemon.ui.pokedex.info.form.{formname}`.
- Added `play_sound_on_server` as an available Molang function for the `worldHolder` & `player` functions.
- Added `run_molang_after` as an available Molang function for the `entity` functions when schedulable.
- Added an optional parameter for `run_molang` to schedule the function.
- Added `labels` & `has_label` as available Molang functions for the `speciesFunctions`
- Added datapack-defined starter categories via `data/<namespace>/starters/*.json`, with built-in fallback and `useConfigStarters` merge option.
- The format of the `remedies.json` file has changed to allow for individual friendshipDrop amounts per remedy
- Fixed `entity.find_nearby_block` causing crashes when attempting to use a block tag
- Spawn Filters can now access `v.spawn.class` to get the identifier of an NPC class for when trying to influence NPC spawns
- Added Molang functions for Party and PC: `set_pokemon`
- Added Molang functions for Pokémon: `pokeball`, `held_item`, `remove_held_item`, `hyper_train_iv`, `validate_moveset`, `initialize_moveset`, and `add_exp`.
- Added Molang functions for Pokémon: `aspects`, `form_aspects`, `unlearn_move`, `teach_learnable_moves`, `cosmetic_item`, and `remove_cosmetic_item`.
- Added Molang functions for Pokémon: `ability`, `set_iv`, `set_ev`, `teach_move`, and `can_learn_move`.
- Added Molang function `q.delete_variable(<struct>, <variable_name>)` to delete a value from a variable structure in MoLang data.
- Added Molang function `q.delete_variables(<struct>)` to delete all values from a variable structure in MoLang data.
- Adds Flows for `STARTER_CHOSEN`, `SHOULDER_MOUNTED`, `EV_GAINED`, `POKEMON_RELEASED`, `POKEMON_NICKNAMED`, `HELD_ITEM`, and `TRADE_COMPLETED` events
- Adds Flows for `POKEMON_HEALED`, `POKEMON_SCANNED`, `BERRY_HARVEST`, `LOOT_DROPPED`, `POKEMON_SEEN`, `COLLECT_EGG`, `HATCH_EGG`, and `EXPERIENCE_GAINED`.
- Adds Flows for `POKEMON_CATCH_RATE`, `BAIT_SET`, `BAIT_SET_PRE`, `BAIT_CONSUMED`, `POKEROD_CAST_PRE`, `POKEROD_CAST_POST`, `POKEROD_REEL`, and `BOBBER_SPAWN_POKEMON_PRE`.
- Adds Flows for `POKEMON_ASPECTS_CHANGED`, `FRIENDSHIP_UPDATED`, `CHANGE_PC_BOX_WALLPAPER_EVENT_PRE`, `CHANGE_PC_BOX_WALLPAPER_EVENT_POST`, and `FULLNESS_UPDATED`.
- Added Callback for `SERVER_STOPPING`
- MoLang triggered battles may now set the battle format, whether to clone the player's party, set level, or heal prior.
- Added Molang function for Player: `inventory`
- Adds Flows for `STARTER_CHOSEN`, `EV_GAINED`, `POKEMON_RELEASED`, `POKEMON_NICKNAMED`, `HELD_ITEM`, and `TRADE_COMPLETED` events
- Adds Pokemon functions for `pokeball`, `held_item`, `remove_held_item`, `add_aspects`, and `remove_aspects`
- Added `pokemon.hyper_train_iv` as an available Molang function.
- Added `prepare_{effect}` and `damage_{effect}` action effect hooks in battles for more battle particle effects.
- Added `q.has_argument(<argument_name>, [argument_value])` MoLang function to several battle-related action effect contexts.
- Added `q.has_argument_at(<index>, [argument_value])` MoLang function to several battle-related action effect contexts.
- Added `q.hit_count` MoLang function to move action effect contexts.
- Added `is_included`, `to_lower`, `to_upper`, and `string_length` as available Molang functions.
- Fixed a crash that would occur during battles if the opponent wild Pokémon species comes from a namespace other than cobblemon
- Fixed `clientActions` inside Dialogue pages being executed twice
- Added `q.split_string(<text>, <delimiter/comma>)` Outputs an array with the divided text.
- Added `q.spawn_npc(<x>, <y>, <z>, <npc>)` Spawns an NPC at the given coordinates and returns the NPC.
- Added `q.player.seen_credits` Returns whether the player has seen the end credits.
- Added `q.player.is_in_dialogue` Returns whether the player is currently in a dialogue.
- Added `q.player.active_dialogue` Returns the active dialogue of the player, or null if none.
- Added `q.player.is_spectator` Returns whether the player is in spectator mode.
- Added `q.player.is_adventure` Returns whether the player is in adventure mode.
- Added `q.player.is_creative` Returns whether the player is in creative mode.
- Added `q.player.is_survival` Returns whether the player is in survival mode.
- Added `q.player.set_battle_theme(<sound resource>)` Sets the player's battle theme to the given theme.
- Added `q.player.battle_music(<sound_resource>, <volume>, <pitch>)` Plays the given battle music for the player.
- Added `q.player.stop_battle_music()` Stops any currently playing battle music for the player. (With a fade out).
- Added `q.player.riding_pokemon` Returns the Pokémon the player is currently riding.
- Added `q.entity.is_player` Returns whether the entity is a player.
- Added `q.entity.is_pokemon` Returns whether the entity is a Pokémon.
- Added `q.entity.is_npc` Returns whether the entity is an NPC.
- Added `q.entity.is_mob` Returns whether the entity is a vanilla mob.
- Added `q.entity.is_animal` Returns whether the entity is an animal.
- Added `q.entity.is_hostile` Returns whether the entity is a hostile mob.
- Added `q.entity.is_baby` Returns whether the entity is a baby.
- Added `q.entity.is_adult` Returns whether the entity is an adult.
- Added `q.entity.is_tamable` Returns whether the entity is tamable.
- Added `q.entity.is_tamed` Returns whether the entity is tamed.
- Added `q.entity.add_effect(<effect>, <duration>, <amplifier>, <ambient> <show_particles>)` Adds a status effect to the entity.
- Added `q.entity.remove_effect(<effect>)` Removes a status effect from the entity.
- Added `q.entity.has_effect(<effect>)` Returns whether the entity has the given status effect.
- Added `q.entity.is_looking_at(<entity>)` Returns whether the entity is looking at the given entity.
- Added `q.entity.tags` Returns an array of the entity's tags.
- Added `q.entity.add_tag(<tag>)` Adds the given tag to the entity.
- Added `q.entity.remove_tag(<tag>)` Removes the given tag from the entity.
- Added `q.entity.has_tag(<tag>)` Returns whether the entity has the given tag.
- Added `q.entity.make_intangible(<true/false>)` Makes the entity intangible.
- Added `q.pokemon.force_evolve(<index>)` Forces the Pokémon at the given index.
- Added `q.pokemon.can_evolve` Returns whether the Pokémon at the given index can evolve.
- Added `q.pokemon.is_busy` Returns whether the Pokémon is busy.
- Added `q.pokemon.is_rideable` Returns whether the Pokémon is rideable.
- Fixed `background` field and added `textColor` field for dialogues.
- Added support to reload some data registries:
    - MoLang scripts
    - Callbacks
    - Spawn Detail Presets
    - Spawn Pools
    - Spawn Rules
    - Cosmetic Items
    - Dialogues
    - Fossils
    - Natural Materials
    - Action Effects
    - Mechanics
    - Unlockable Wallpapers
    - Starter Data
- Fixed `cobblemon:reel_in_pokemon` criteria not working when used together with a `baitId`
  - Also changed the default from `cobblemon:empty_bait` to `any`
  - The previous default is still available by using the above as baitId
- Added support for species-specific move action effects, using the format `{move_id}_{species}.json`.
- Added `look_at_entity_types` variable for look_at_entities to specify what entity type or entity tag to look at.
- Added `Looks At Players` behaviour preset that only sets the look target to players.
- Added various functions to `q.file` for JSON file handling in MoLang, strictly for `./config` and `./data` folders that have `/molang/` in the path:
  - `q.file.save(<path>, <struct>)` Saves to the given file path with the given variable struct.
  - `q.file.load(<path>)` Loads a variable struct from the given path, or gets it from the cache if it's already been loaded.
  - `q.file.exists(<path>)` Returns 1 if the given file path exists.
  - `q.file.clear(<path>)` Clears the given file from the cache.
- Using `q.run_script` will now allow additional arguments which will be put into `c.arg_1`, `c.arg_2`, etc.
### Molang & Datapacks
- Added a `chance` requirement type for Pokémon interactions.

### Localization
- Updated translations for:
  - French
  - Canadian French
  - Korean
  - Portuguese
  - Simplified Chinese
  - Spanish
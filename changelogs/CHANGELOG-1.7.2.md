# Changelog
## [1.7.2 (January 25th, 2026)](#1-7-2)

### Additions
- Added Saccharine wood furniture for Adorn.
- Added the Partner Mark, which party Pokémon have a very small chance to earn as the player walks, provided they have enough friendship.
- Added decorative block models for existing items: Potion, Super Potion, Hyper Potion, Max Potion, Full Restore, Antidote, Awakening, Burn Heal, Ice Heal, Paralyze Heal, Full Heal, Ether, Elixir, Max Ether, Max Elixir, X Accuracy, X Attack, X Defence, X Special Attack, X Special Defence, X Speed, Dire Hit, Guard Spec, Weakness Policy, Blunder Policy, Cleanse Tag, and Spell Tag.
- Added decorative sign blocks called Plaques.
- Added ride style labels to ride stats in Pokédex, as well as ride style icons in the summary GUI.
- Added more seasonings for sinister tea:
  - Mental Herb - Mental Restoration I for 10 seconds, which staves off Insomnia.
  - White Herb - Removes negative effects.
  - Milk, Moomoo Milk - Removes effects.
- Added dispenser support for:
  - Applying a Honey Bottle to Saccharine Logs.
  - Washing honey off of Saccharine Slathered Logs.
  - Applying and removing honey on Saccharine Leaves.
- Added riding statistics: `Distance by Pokémon on Land`, `Distance by Pokémon in Air` and `Distance by Pokémon in Liquid`.

### New rideable Pokémon
- Honchkrow 
- Mantine

### Pokémon Added

#### Gen 5
- Throh
- Sawk

#### Gen 6
- Litleo
- Pyroar
- Spritzee
- Aromatisse
- Swirlix
- Slurpuff

### Model updates for the following Pokémon
- Falinks
- Cottonee
- Whimsicott
- Ferroseed
- Ferrothorn
- Chingling
- Chimecho

### Animation updates for the following Pokémon
- Mantine (ride animations)
- Flygon (ride jump animation)
- Honchkrow (ride animations)
- Croagunk
- Toxicroak

### Changes
- Added a new config option, `Enable In-Flight Dismounting` (default: off), which lets you dismount while riding a Pokémon in the air. This is the textbook definition of caveat emptor.
- Ride sounds have been separated into stereo for passengers and mono for other players. Riding will now sound more spacious.
- Battle AI now uses a smarter threshold for switching, reducing unnecessary switches.
- AI will always use the most damaging move when at low HP and when it is not switching, improving endgame decision-making.
- Items given from interacting with Saccharine Leaves are placed in the active hotbar slot if possible.
- Added `label` as an alternate key to the `tag` property.

### Fixes
- Fixed crashing when viewing another player in spectator mode. Some said this was reason to release this update a month ago. They were probably right.
- Fixed crashing when sending a Pokémon out after editing its form.
- Fixed all Pokémon being saved to chunks and never despawning even when they should be.
- Fixed Poké Snacks crashing if there was no available spawn.
- Fixed Poké Snack / Bait effects so that EV Yield, Type, and Egg Group filters now all apply together when weighting spawns. And you thought you were going crazy.
- Fixed Furfrou not being trimmable on NeoForge specifically.
- Fixed honey from Saccharine Leaves being harvestable at less than the max age.
- Fixed possible error on world generation when Combee are placed in naturally generated bee nests.
- Fixed Galarian Weezing crashing the world if it decides to blink the wrong way. The same could happen to you!
- Fixed Galarian Mr. Mime incorrectly being rideable. Some would argue that regular Mr. Mime shouldn't be rideable, but that would make them wrong.
- Fixed missing apricorn textures for Adorn blocks.
- Fixed Apricorn block model rotation for counters/sinks/cupboards/drawers to match that of most recent version of Adorn.
- Fixed item interaction sometimes not working properly when playing on servers.
- Fixed Mint Leaves not being usable for filling the Resurrection Machine.
- Fixed Super Potion recipes incorrectly using Hondew Berry instead of Aguav Berry. They look similar, to be fair.
- Fixed Wishiwashi not schooling properly.
- Fixed battle item duplication issue. How did it work? Wouldn't you like to know, weather boy.
- Fixed the ordering of Aprijuice's riding stat boosts so they always have the same order.
- Fixed the movesets of certain Pokémon being incorrectly sanitized upon reload, causing some forms to keep losing moves.
- Fixed selected leading Pokémon in battle not being used when levels are set.
- Fixed the bottom half of pasture blocks having an incorrect hit-box when facing north or south.
- Fixed gimmicks not being usable in double and triple battles.
- Fixed gimmick buttons showing when already used in the same turn in double and triple battles.
- Fixed mochi items consuming two items at a time.
- Fixed Fresh Start Mochi's EV reset not syncing health changes properly.
- Fixed Pokémon spawn influences not being specific to regional forms.
- Fixed memory leak from battles not being cleaned up after ending.
- Fixed orientation not getting reset after crashing while flying and getting back on the Pokémon.
- Fixed shiny Pokémon not respecting the `silent` flag on spawn.
- Fixed 'Plain' prefix not being applied to all low-quality Aprijuice.
- Fixed Pokémon interactions not playing sound effects when the volume was not specified.
- Fixed Pokémon interaction cooldowns taking longer than intended when the Pokémon was in the player’s party.
- Fixed Pokémon interactions occasionally crashing the game when spamming an interaction with a 0 cooldown.
- Fixed Pokémon fullness decreasing more than intended when the Pokémon was pastured.
- Fixed Scraggy line, Milcery line, and Morpeko spawning on unnatural blocks. 
- Fixed invulnerability-bypassed damage being resisted by invulnerable NPC entities.
- Fixed Fabric network packets not being handled immediately, fixing incompatibilities with Supplementaries, Prometheus, Sleep Tight, and others.
- Fixed killer not being set early enough on a wild Pokémon entity from `WinInstruction`.
- Fixed requirements such as `biomeCondition` crashing the game when used to define Pokémon interactions.
- Fixed an issue that prevented registering custom dispenser behaviours (no longer overrides vanilla behaviour).
- Fixed an issue navigation that prevented Combee from pathing into and out of Saccharine Leaves.
- Added Saccharine Boats and Saccharine Boat with Chests to relevant boat tags.
- Fixed Soothe Bell not being properly tagged as a held item.
- Fixed `/calculateseatpositions` expecting a locator format that even we don't use. It needs an underscore after "seat".
- Fixed the `run_script` Molang function to not fail if the environment's context is null.
- Fixed the Molang functions `date_local_time`, `date_of`, `date_is_after` to use the correct date format.

### Developer
- Changed the `owner` parameter in the `OwnerQueryRequirement` interface from `ServerPlayer` to `Player`. This method is now also called on the client to verify whether a Pokémon interaction succeeded, so make sure to update your implementations to handle both server and client contexts.
- Added the `pnx` to the `BattleFaintedEvent` and `FormeChangeEvent`.
- Added register methods for custom instructions to `ShowdownInterpreter`.
- Changed callback operations in `BattlePokemon` to allow multiple callbacks and be mutable.
- Added `display_name`, `description`, and `max_pp` functions to the `MoveTemplate` struct.
- Changed the implementations of `AIBattleActor` to be open classes to allow extension.
- Changed the resource location of Poké Ball icons in the GUI to use the balls namespace instead of Cobblemon's, allowing mods to properly use their own namespace for these textures.

### Molang & Datapacks
- Ride sounds can now be set as exclusive to passengers.
- Ride sounds no longer play when submerged unless specified with a new setting.
- Exception handling has been added to `run_molang`, resolving some crashes caused by malformed MoLang expressions.
- Added the `create_itemstack`, `has_inventory_space`, `set_inventory_slot`, and `give_item` MoLang functions for item and inventory utility.
- Added the `get_move_from_id` MoLang function for general move info queries without requiring a Moveset object to access.
- Added support for optional message variants in battle activate instructions for more context-specific battle text.
  - Example: `this.add('-activate', pokemon, 'ability: example', '[msg]message1');` will parse to lang key `cobblemon.battle.activate.example.message1`.

### Localization
- Updated translations for:
  - French
  - Canadian French
  - Simplified Chinese
  - Ukrainian
  - Brazilian Portuguese
  - Spanish
  - Korean
  - Japanese

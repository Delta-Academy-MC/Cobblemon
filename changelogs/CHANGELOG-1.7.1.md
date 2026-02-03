# Changelog
## [1.7.1 (November 29th, 2025)](#1-7-1)

### Additions
- Added trades for Saccharine Saplings, Hearty Grains, Chipped Pot, and Masterpiece Teacup to the Wandering Trader.
- Added brewing recipe for Throat Spray.
- Added vanilla sprinting's field of view change to Pokémon land mounts.
- Added a customizable keybind for the riding freelook button.
- Added double tap to sprint on land mounts.

### Changes
- Made Jet pitch turning 1.5x faster across all Pokémon.
- Party Pokémon now attack only mobs that are attacking their owner, instead of randomly targeting unrelated mobs and getting themselves hurt.
- Reduced the cost of the Vivichoke Seed trade with the Wandering Trader.

### Fixes
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

### Developer

### Molang & Datapacks
- Removed automatic generation of PC wallpaper screen glow if no asset is provided; it is now optional.
    - This is because there's no practical way to do this without blowing up things like *VulkanMod*.

### Particles

### Localization
- Updated translations for:
    - French
    - Japanese
    - Korean
    - Brazilian Portuguese
    - Simplified Chinese
    - Traditional Chinese
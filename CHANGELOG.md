# Dandelion 0.1.1-dev – Changelog

## New Features
- **Editable Message System** – All server messages are now fully configurable via external files.
- **Custom Blocks Support** – Now supports custom blocks using the same as *MCGalaxy* JSON definition files.
- **`/blocks` Command** – Manage and list custom blocks directly in-game.
- **JsonParser** – Added for improved JSON parsing capabilities.
- **Improved Disconnection Handling** – Better handling of unexpected exceptions during player connections.
- **Better CPE Compatibility** – Improved fallback behavior when clients do not support certain CPE extensions.

## CPE Extensions
### Previously Supported
- EnvColors
- EnvWeatherType
- EnvMapAspect
- CustomBlocks

### Newly Added in 0.1.1-dev
- ClickDistance
- BlockPermissions
- HackControl
- HeldBlock
- SetHotbar
- SetSpawnPoint
- MessageTypes
- InstantMOTD
- LongerMessages
- BlockDefinitions
- BlockDefinitionsExt (version 2)

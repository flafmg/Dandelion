# Dandelion 0.1.3-dev – Changelog

## New Features
- **ClassicWorld** format support added
- **particles** system added
- **player info** updated to use a db instead of a yaml
- **Level permission** added dandelion.level.build.<level> and dandelion.level.join.<level> permission (defaults to true, needs to be explicitly revoked)
- **plugin message** you can receive trough events and send using Plugin.sendMessage(channel, message, [target]) (if target is null it will be sent to all online players) <- to be improved
- new aliases added for perms command to make it less verbose

### Dandelion Level Format v4
- updated to use extendedBlocks

## fixes
- dlvl now supports extended blocks that
- cp437 typo and bad implementation
- level transfer not fully removing player from level before sending new data
- chat replacing % with & when it doesnt represent a color code
- texthotkey not being added on the supported cpe list

## BREAKING
- packages changed from dandelion.classic to dandelion.server
- command permissions now specify dandelion.command

## CPE Extensions

### Newly Added
- **CustomParticles** - Support for custom particle effects
- **CP437** - although it was added previously the implementation was lacking
- **PluginMessage** - added message event and system
- **TextHotKey** - the implementation was previously added but i forgot to add the actual entry, fixed
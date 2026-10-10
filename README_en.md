# halfmasa

**English** | [中文](https://github.com/halfkite/halfmasa/blob/main/README.md)

[![License](https://img.shields.io/github/license/halfkite/halfmasa)](https://choosealicense.com/licenses/mit/)
[![Modrinth](https://img.shields.io/modrinth/dt/9ZHJ1Ue9?color=00AF5C&label=Modrinth%20downloads&logo=modrinth)](https://modrinth.com/project/9ZHJ1Ue9)
[![CurseForge](https://img.shields.io/curseforge/dt/1661919?logo=curseforge&label=CurseForge%20downloads&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/halfmasa)
[![MC Versions](https://cf.way2muchnoise.eu/versions/For%20MC_1661919_all.svg)](https://www.curseforge.com/minecraft/mc-mods/halfmasa)
[![GitHub](https://img.shields.io/github/downloads/halfkite/halfmasa/total?color=161616&label=GitHub%20downloads&logo=github)](https://github.com/halfkite/halfmasa/releases)

halfmasa is a Minecraft Fabric client-side utility mod that brings together waypoint management, schematic refills, Void Trading, creative-mode tools, and interface improvements. Most optional features are disabled by default; see the configuration documentation for their defaults.

Supports the Fabric Loader and Minecraft versions `1.21.1`–`26.3`.

## Documentation

- [中文功能介绍](https://github.com/halfkite/halfmasa/blob/main/docs/features_cn.md)
- [English feature and configuration guide](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md)

Press `X + H` to open the halfmasa configuration screen, or open it through Mod Menu. See the feature guide for all features and hotkey settings.

## Dependencies and Integrations

| Name | Type | Description |
|---|---|---|
| [Fabric Loader](https://fabricmc.net/use/installer/) | Required | Use `0.17.3+` for Minecraft 1.21.x and `0.18.4+` for Minecraft 26.x. |
| [MaLiLib](https://modrinth.com/mod/malilib) | Required | Install the MaLiLib version matching your Minecraft version. |
| [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) / [World Map](https://modrinth.com/mod/xaeros-world-map) | Optional integration | Provides waypoint binding and map features. |
| [Mod Menu](https://github.com/TerraformersMC/ModMenu) | Optional integration | Adds an entry for opening the halfmasa configuration screen from the mod list. |
| [Litematica](https://link.mcmod.cn/target/aHR0cHM6Ly9naXRodWIuY29tL3Nha3VyYS1yeW9rby9saXRlbWF0aWNh) | Feature-specific | Easy Place refills require a compatible version of Litematica. |
| [Carpet-FGA-Addition](https://github.com/halfkite/Carpet-FGA-Addition) | Optional server component | Schematic and printer refills require a compatible FGA inventory API and the required server permissions. |
| [QuickShulker](https://github.com/MoRanpcy/quickshulker) | Feature-specific | Void Trading material preparation can retrieve emeralds from carried QuickShulker boxes; this requires compatible server support. |
| [Conflux Map](https://github.com/Conflux-Union/conflux-map) | Optional integration | Adds waypoint lists, temporary waypoints, and teleport-related extensions. |

The halfmasa mod itself is client-side; ordinary client features do not require halfmasa on the server. Features that depend on a server inventory API or Void Trading extension require the corresponding server component. See the [compatibility and configuration guide](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md) for details.

## Downloads

- [GitHub Releases](https://github.com/halfkite/halfmasa/releases)
- [Modrinth](https://modrinth.com/project/9ZHJ1Ue9)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/halfmasa)

Download the JAR for your Minecraft version and place it in that game instance's `mods` folder.

## Features

### Waypoints and Maps

- Bind singleplayer saves to Xaero waypoint directories so existing waypoints remain available after a save is renamed, moved, or restored from a backup.
- Switch between multiple singleplayer save directories; import or export waypoint sets across dimensions with deduplication, undo, and redo.
- Extend Conflux Map with local and shared waypoint lists, temporary waypoints that expire when you leave the world, close-map-after-teleport behavior, and a configurable teleport height for destinations with unknown elevation.

### Schematic Refills and Void Trading

- Refill materials for Litematica Easy Place and compatible printer integrations. Configure whether to retrieve items from fake-player inventories, the refill amount, and silent withdrawals.
- Minecraft 26.3 adds [allowlists and blocklists for schematic saving, deletion, and pasting](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md#schematic-features). Filter saved content, world blocks allowed for deletion, and pasted content by block ID; configure each in the Other Mod Extensions tab.
- Automatically open villager trading screens and buy offers by trade-row number or output-item allowlist. The screen can close or acquired items can be dropped after trading.
- Void Trading can identify fake players in the same boat or minecart and restore them after the trading screen closes. Material preparation supports uncrafting emerald blocks and retrieving emeralds from QuickShulker.
- Automatic refills require a server inventory API; Void Trading material preparation requires the corresponding server extension. See the [schematic refill and feature configuration guide](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md#schematic-features) for compatibility details.

### Creative Mode and Interface Improvements

- Fill shulker boxes, chests, offhand containers, and bundles; organize creative search history, expandable creative inventory entries, and saved hotbars.
- Adds a Trial creative tab with trial spawners for every vanilla mob (normal, ominous, and cooldown states) and trial vaults.
- Provides JEI/REI recipe and usage search history, inventory movement, fast scrolling, draggable lists, and a configurable hotkey radial menu.
- Includes boat camera and held-item display options, screenshot clipboard copying, elytra time information, Chinese-English text spacing, an in-game IME, and other client-side interface helpers.

## License

The project is released under the [MIT License](https://github.com/halfkite/halfmasa/blob/main/LICENSE). Third-party attributions and license details are listed in [THIRD_PARTY_NOTICES.md](https://github.com/halfkite/halfmasa/blob/main/src/main/resources/META-INF/halfmasa/THIRD_PARTY_NOTICES.md).

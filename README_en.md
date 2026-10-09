# halfmasa

**English** | [中文](https://github.com/halfkite/halfmasa/blob/main/README.md)

[![License](https://img.shields.io/github/license/halfkite/halfmasa)](https://choosealicense.com/licenses/mit/)
[![Modrinth](https://img.shields.io/modrinth/dt/9ZHJ1Ue9?color=00AF5C&label=Modrinth%20downloads&logo=modrinth)](https://modrinth.com/project/9ZHJ1Ue9)
[![CurseForge](https://img.shields.io/curseforge/dt/1661919?logo=curseforge&label=CurseForge%20downloads&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/halfmasa)
[![MC Versions](https://cf.way2muchnoise.eu/versions/For%20MC_1661919_all.svg)](https://www.curseforge.com/minecraft/mc-mods/halfmasa)
[![GitHub](https://img.shields.io/github/downloads/halfkite/halfmasa/total?color=161616&label=GitHub%20downloads&logo=github)](https://github.com/halfkite/halfmasa/releases)

halfmasa is a Minecraft Fabric client utility mod for Xaero and MaLiLib users. It brings together waypoint management, schematic refills, Void Trading, creative-mode tools, and interface improvements. Most optional features are disabled by default; see the configuration documentation for individual defaults.

## Dependencies

| Name | Type | Description |
|---|---|---|
| [Fabric Loader](https://fabricmc.net/use/installer/) | Required | Use `0.17.3+` for Minecraft 1.21.x and `0.18.4+` for Minecraft 26.x. |
| [MaLiLib](https://modrinth.com/mod/malilib) | Required | Install the MaLiLib version matching your Minecraft version. |
| [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) / [World Map](https://modrinth.com/mod/xaeros-world-map) | Optional integration | Provides waypoint binding and map features. |
| [Mod Menu](https://github.com/TerraformersMC/ModMenu) | Optional integration | Adds an entry for opening the halfmasa configuration screen from the mod list. |
| [Litematica](https://link.mcmod.cn/target/aHR0cHM6Ly9naXRodWIuY29tL3Nha3VyYS1yeW9rby9saXRlbWF0aWNh), Fabric API | Feature-specific | Easy Place refills require a compatible Litematica version and Fabric API. |
| [Carpet-FGA-Addition](https://github.com/halfkite/Carpet-FGA-Addition) | Optional server component | Easy Place and printer refills require a compatible FGA inventory API and the required server permissions. |
| [QuickShulker](https://github.com/MoRanpcy/quickshulker), Void Trading server extension | Feature-specific | Void Trading material preparation can retrieve emeralds from carried QuickShulker boxes and requires compatible server support. |
| [Conflux Map](https://github.com/Conflux-Union/conflux-map) | Optional integration | Adds waypoint-list, temporary waypoint, and teleport-related extensions. |

The halfmasa mod itself is client-side; ordinary client features do not require halfmasa on the server. Features that use a server inventory API or the Void Trading extension require the corresponding server component. See the [feature and configuration guide](docs/features_en.md) for details.

## Supported Versions

| Minecraft | halfmasa | Minimum Fabric Loader |
|---|---|---|
| `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.8`, `1.21.10`, `1.21.11` | `1.6.0` | `0.17.3` |
| `26.1.2`, `26.2`, `26.3` | `1.6.0` | `0.18.4` |

## Downloads

- [GitHub Releases](https://github.com/halfkite/halfmasa/releases)
- [Modrinth](https://modrinth.com/project/9ZHJ1Ue9)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/halfmasa)

Download the JAR for your Minecraft version and place it in that game instance's `mods` folder. For a custom game directory, use the absolute path configured in your launcher profile.

## Features

### Waypoints and Maps

- Bind singleplayer worlds to Xaero waypoint roots so renamed, moved, or restored worlds can keep using their existing waypoints.
- Switch between multiple singleplayer saves directories; import or export multi-dimension waypoint bundles with deduplication, undo, and redo.
- Extend Conflux Map with combined local and shared waypoint lists, temporary waypoints cleared when rejoining a world, close-map-after-teleport behavior, and a configurable teleport height for locations with unknown elevation.

### Schematic Refills and Void Trading

- Minecraft 26.3 adds independent [schematic save, deletion, and paste block filters](docs/litematica-block-filters_en.md) in the Other Mod Extensions tab, using block ID whitelists or blacklists.

- Refill materials for Litematica Easy Place and compatible printer integrations. Configure fake-player stock access, refill amounts, and silent withdrawals.
- Automatically open villager trade screens and buy offers through the local player's trading interface by row number or output-item whitelist. The screen can close or the acquired items can be dropped after trading.
- Void Trading can identify fake players aboard the same boat or minecart and restore them after the trade screen closes. Material preparation can uncraft emerald blocks and retrieve emeralds from QuickShulker boxes.
- Automatic refills require a server inventory API; Void Trading material preparation requires its server extension. See the [refill compatibility guide](docs/litematica-auto-refill.md) and [feature guide](docs/features_en.md) for details.

### Creative Tools and Interface Improvements

- Fill shulker boxes, chests, offhand containers, and bundles; manage creative search history, expandable creative entries, and saved hotbars.
- Adds a "Trial" creative tab that gathers trial spawners for every vanilla mob (normal / ominous / cooldown) plus trial vaults.
- Use JEI/REI recipe and usage histories, inventory movement, fast scrolling, draggable lists, and a configurable hotkey radial menu.
- Includes boat camera and held-item options, screenshot clipboard copying, elytra time information, Chinese-English display spacing, an in-game IME, and other client-side interface helpers.

## Documentation

- [English features and configuration](docs/features_en.md)
- [中文功能与配置说明](docs/features.md)
- [Litematica and printer refill compatibility](docs/litematica-auto-refill.md)
- [Version compatibility notes](docs/version_compatibility.md)
- [Modrinth English description](docs/modrinth_en.md)
- [Build and release process](docs/releasing.md)
- [Web GPT project instructions and review workflow (Chinese)](docs/web-gpt-project-instructions.md)

Press `X + H` to open the halfmasa configuration screen, or open it through Mod Menu. See the feature guide for all options and hotkeys.

## Development Build

```powershell
.\gradlew.bat buildAllVersions
```

## License

The project is released under the [MIT License](LICENSE). Third-party attributions and license details are listed in [THIRD_PARTY_NOTICES.md](src/main/resources/META-INF/halfmasa/THIRD_PARTY_NOTICES.md).

# halfmasa

halfmasa is a client-side Minecraft Fabric utility mod for Xaero, MaLiLib, creative tools, inventory screens, and JEI/REI lookup history.

It combines practical client features in one MaLiLib-style configuration screen. The server does not need to install halfmasa.

## Highlights

- Bind singleplayer worlds to Xaero Minimap and Xaero World Map roots. The binding follows renamed, moved, and restored worlds.
- Switch between multiple singleplayer saves directories from the world-selection screen. The button can be moved by holding it for one second and dragging.
- Import and export multi-dimension Xaero waypoint bundles as `XWB2:` clipboard text or files, with legacy `XWB1:` support, deduplication, undo, and redo.
- Fill shulker boxes, chests, offhand containers, and bundles in creative mode.
- Keep separate JEI and REI recipe and usage lookup histories with a configurable four-corner overlay.
- Preview filled maps in hotbar, inventory, and container slots.
- Drag resource-pack and server lists, with independent drag modes and optional arrow hiding.
- Configure fast interface scrolling, reach-around bridging assistance, saved-hotbar improvements, boat camera and held-item rendering, inventory movement, and cooldown-based auto attack.
- Add smooth Night Vision fading, elytra flight-time information, screenshot clipboard copying, clickable chat sending, server icon caching, server ping recovery, toast suppression, and a Windows in-game IME.
- Optionally add Chinese-English spacing to translations, signs, and editable or written books with independent controls and a toggle hotkey.
- Customize the keybind radial menu, HUD-like overlays, configuration folding, and per-category scroll positions.

Unless a feature explicitly says otherwise, it is disabled by default.

## Compatibility

The project currently provides builds for Minecraft `1.21.1`, `1.21.4`, `1.21.8`, `1.21.10`, `1.21.11`, `26.1.2`, and `26.2`.

Required dependencies:

- Fabric Loader `0.17.3` or newer for Minecraft 1.21.x; `0.18.4` or newer for Minecraft 26.x
- MaLiLib matching the Minecraft version

Optional integrations include Xaero's Minimap, Xaero's World Map, Mod Menu, Carpet, Tweakeroo, REI, and JEI.

## Installation

Download the JAR for your Minecraft version and place it in the absolute `mods` directory of the game instance.

Windows default instance:

```text
C:\Users\<username>\AppData\Roaming\.minecraft\mods\
```

Linux default instance:

```text
/home/<username>/.minecraft/mods/
```

macOS default instance:

```text
/Users/<username>/Library/Application Support/minecraft/mods/
```

For a launcher profile with a custom game directory, replace the default directory above with that profile's absolute game directory.

## Configuration and storage paths

Open the configuration screen with `X + H`, or use Mod Menu.

Windows default instance paths:

```text
C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\halfmasa.json
C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\search-history\creative.json
C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\search-history\jei.json
C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\search-history\rei.json
C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\keybind-pie\bindings.json
C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\server-icons\
```

Singleplayer world binding data is stored inside the world directory:

```text
C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\config\halfmasa\xaero-world-binding.json
```

The old legacy file, when present, is read and migrated automatically:

```text
C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\.halfmasa-xaero-binding.json
```

If a custom saves directory is active, replace the `.minecraft\saves` portion with the configured absolute saves directory. For example:

```text
D:\MinecraftData\saves\<world>\config\halfmasa\xaero-world-binding.json
```

## Documentation

- [English features and configuration](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md)
- [Chinese features and configuration](https://github.com/halfkite/halfmasa/blob/main/docs/features.md)
- [Third-party notices](https://github.com/halfkite/halfmasa/blob/main/src/main/resources/META-INF/halfmasa/THIRD_PARTY_NOTICES.md)

## License

halfmasa is released under the [MIT License](https://github.com/halfkite/halfmasa/blob/main/LICENSE). Third-party notices and bundled licenses are included in the source tree and in the mod JAR under `META-INF/halfmasa/THIRD_PARTY_NOTICES.md`.

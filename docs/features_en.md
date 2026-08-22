# halfmasa Features and Configuration

> Documentation version: `1.1.1-beta.1`

Press `X + H` to open the configuration screen, or use Mod Menu. Unless noted otherwise, features are disabled by default. An empty hotkey means that no key is bound by default.

## Xaero and Waypoint Tools

| Config or action | Default | Description |
|---|---|---|
| `enableWorldBinding` | `false` | Stores the selected Xaero Minimap and World Map root IDs in `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\config\halfmasa\xaero-world-binding.json` so renamed, moved, or restored worlds can keep using the same waypoint and map data. The legacy `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\.halfmasa-xaero-binding.json` is migrated automatically. |
| `customSavesPaths` | empty list | Custom saves path list. Absolute paths and paths relative to the game directory are supported. Click the current path in the top-left of the singleplayer world selection screen to switch immediately; the vanilla `saves` directory is always available. |
| `keepWorldSelectionOnEmpty` | `false` | When enabled, clicking Singleplayer with no worlds in the current saves path stays on world selection instead of opening world creation automatically. |
| `copyWaypointBundle` | unbound | Compresses normal waypoints and sets from the current Xaero world into an `XWB1:` string and copies it to the clipboard. |
| `importWaypointBundle` | unbound | Imports a complete `XWB1:` bundle from the clipboard into the current Xaero world. |
| `shareCurrentWaypointSet` / `shareAllWaypoints` | unbound | Sends the current set or all normal waypoints through chat using Xaero's native share format. |
| `dedupeCurrentWaypointSet` / `dedupeAllWaypoints` | unbound | Removes later waypoints with matching coordinates and names within each set. |
| `waypointChatInterval` | `10` ticks | Controls the delay between queued waypoint chat messages. |

Temporary, server-provided, and third-party dynamic waypoints are excluded from bundles. World Map data is handled only when Xaero's World Map is installed.

## Creative Tools

| Config or action | Default | Description |
|---|---|---|
| `enableGiveFullInventory` | `false` | Enables creative container filling. |
| `giveFullInventory` | `G` | Fills a shulker box, chest, offhand container, or bundle from the held item according to the current main-hand and offhand combination. |
| `bundleFill` | `1` | Number of insertion attempts when the offhand target is a bundle. |
| `fillSafety` | `true` | Prevents unsafe container and shulker-box nesting. |
| `itemSearchHistory` | `false` | Records items obtained after creative searches and displays an independent history area above the results. |
| `itemSearchHistoryRows` | `3` | Maximum creative-history rows, from `1` to `9`. |
| `itemSearchHistoryDuringSearch` | `false` | Keeps history visible while search text is present. |
| `condensedCreative` | `false` | Groups enchanted books, potions, tipped arrows, and many block variants into expandable creative entries. |

## JEI/REI Lookup History (Extensions)

| Config or action | Default | Description |
|---|---|---|
| `itemManagerRecipeHistory` | `false` | Keeps separate JEI and REI histories for recipe lookups, usage lookups, and successfully obtained items. |
| `itemManagerRecipeHistoryRows` | `3` | Number of history-grid rows, from `1` to `9`. |
| `itemManagerRecipeHistoryPosition` | `bottom_right` | Anchors history to any screen corner while the native entry and favorites areas make room. |
| `cycleItemManagerRecipeHistoryPosition` | unbound | Cycles through all four corners, works while an inventory is open, and displays the new position. |

The panel initializes on the first item-manager screen without requiring a recipe search. On the default Windows instance, history files are stored separately under `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\search-history\`.

## Client and UI Features

| Config | Default | Description |
|---|---|---|
| `screenshotToClipboard` | `false` | Copies the complete image to the system clipboard whenever F2 saves a screenshot. |
| `elytraTimeTooltip` | `false` | Adds estimated remaining flight time to elytra tooltips. |
| `reportElytraTime` | unbound | Reports the equipped elytra's estimated remaining time in chat. |
| `nightVisionFade` | `true` | Enables smooth Night Vision fading; disabling it restores vanilla's final 10-second flicker. |
| `nightVisionFadeSeconds` | `5` | Sets the smooth fade duration; range `0-60`, where `0` disables early fading. |
| `boatView360` / `boatItemView` | `false` | Removes the local boat-camera rotation limit and keeps first-person held items visible while rowing. |
| `inventoryMove` | `false` | Allows movement, jumping, and sneaking while vanilla inventory or container screens are open. |
| `fastWorldLoadingScreen` / `fastResourcePackLoadingScreen` | `false` | Reduces avoidable waiting in world and resource-pack loading screens. |
| `betterSavedHotbars` | `false` | Extends creative saved-hotbar behavior. |
| `cooldownAutoAttack` | `false` | Attacks the targeted entity while attack is held and the vanilla cooldown is ready. |
| `draggableLists` | `false` | Adds drag reordering to resource-pack and server lists, with optional arrow hiding. |
| `fastScrolling` | `false` | Accelerates only the current screen's scroll events, including MaLiLib config screens, and leaves in-game hotbar scrolling unchanged; expand it to configure both modes. |
| `fastScrollingPrimaryEnabled` / `fastScrollingPrimaryHotkey` / `fastScrollingPrimaryMultiplier` | `true` / `Left Ctrl` / `2` | Mode one can be independently enabled, rebound, and configured from `1–32`. |
| `fastScrollingSecondaryEnabled` / `fastScrollingSecondaryHotkey` / `fastScrollingSecondaryMultiplier` | `true` / `Left Ctrl + Left Shift` / `6` | Mode two can be independently enabled, rebound, and configured from `1–32`; it wins when both modes match. |
| `bridgingAssist` | `false` / unbound | Adds Bedrock-style reach-around placement when the crosshair misses; expand it to configure distance, crouching, axes, delay, view source, snapping, slab assistance, torch filtering, crosshair, and outline. |
| `skipResourcePackCompatibilityCheck` | `false` | Treats added resource packs as compatible and skips version mismatch confirmation. |
| `disablePausedItemTrajectoryPrediction` | `false` | Stops client-side dropped-item trajectory prediction while Carpet or vanilla ticks are frozen. |
| `keepModMenuScroll` | `false` | Remembers separate scroll positions for Mod Menu and every MaLiLib configuration category. |

## Input, Maps, and Utilities

| Config | Default | Description |
|---|---|---|
| `keybindPieMenu` | `false` | Provides a customizable radial selector for conflicting or related keys, including colors, animation, scale, and cancel-zone settings. |
| `clickAndSend` | `false` | Sends clickable non-command text as ordinary chat. |
| `cjkLatinSpacing` | `false` / unbound | Adds display spacing between Chinese text and adjacent Latin words or numbers; expand it to independently control translations, signs, and editable or written books without changing stored sign or book text. |
| `cjkLatinSpacingTranslations` / `cjkLatinSpacingSigns` / `cjkLatinSpacingBooks` | `true` | Independently controls display spacing for translations, sign text, and book pages. |
| `mapInSlot` | `false` | Renders filled-map previews in hotbar, inventory, and container slots while preserving count and decoration layers. |
| `serverIconCache` | `true` | Caches server icons and matches them by name, address, or both with a configurable limit; clearing requires confirmation. |
| `toastKiller` | `false` | Clears current toasts and rejects new ones while enabled. |
| `serverPingerFix` | `false` | Expands the server-refresh executor and clears stale queued work. |
| `contingameIme` | `false` | Windows JNI in-game IME with composition text, candidate overlay, temporary mode, and persistent mode. |

On the default Windows instance, keybind-pie data is stored in `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\keybind-pie\bindings.json`; the server-icon cache is stored under `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\server-icons\`. Persistent data associated with multiplayer servers is always stored under the absolute game-directory `config\halfmasa\` path and is never written into a singleplayer save.

## Disabled Advanced Features

Fluid-render suppression and entity-render aggregation are placed in the Disabled Features category. They can substantially alter rendering or compatibility and should be enabled only after reviewing their settings and impact.

## Configuration Files

On the default Windows instance, the main configuration file is `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\halfmasa.json`. Legacy root-level configuration is migrated into `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\legacy\`. Feature data uses temporary files and atomic replacement where applicable to avoid incomplete JSON during normal saves. Replace the default `.minecraft` directory with the absolute game directory configured by your launcher profile when necessary.

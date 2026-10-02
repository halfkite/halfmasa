# halfmasa Features and Configuration

Easy Place and printer refill appear under Extensions, each with foldable fake-stock permission, quantity (default32;0 means half a stack) and silent options. Printer adapters select the Hana coordinator or shared InventoryUtils method signatures. Exact quantities require updated server FGA. [Compatibility and acceptance](litematica-auto-refill.md)

> Documentation version: `1.6.0`

Press `X + H` to open the configuration screen, or use Mod Menu. Unless noted otherwise, features are disabled by default. An empty hotkey means that no key is bound by default.

## Xaero and Waypoint Tools

| Config or action | Default | Description |
|---|---|---|
| `enableWorldBinding` | `false` | Stores the selected Xaero Minimap and World Map root IDs in `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\config\halfmasa\xaero-world-binding.json` so renamed, moved, or restored worlds can keep using the same waypoint and map data. The legacy `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\.halfmasa-xaero-binding.json` is migrated automatically. |
| `customSavesPaths` | empty list | Custom saves path list. Absolute paths and paths relative to the game directory are supported. Click the current path in the top-left of the singleplayer world selection screen to switch immediately; the vanilla `saves` directory is always available. |
| `keepWorldSelectionOnEmpty` | `false` | When enabled, clicking Singleplayer with no worlds in the current saves path stays on world selection instead of opening world creation automatically. |
| `importWaypointBundle` | action button | Automatically imports `XWB1:`/`XWB2:` clipboard text or the first copied text file. Legacy XWB1 data targets the current dimension; XWB2 preserves dimensions and sets. |
| `exportAllDimensions` | action buttons | Exports every dimension and set in the current Xaero root container as `XWB2:` text or a UTF-8 `.txt` file, including extra dimensions created by VC and similar mods rather than only the overworld, Nether, and End. Each dimension also stores its formal dimension ID, Xaero equivalent dimension ID, complete container nodes, full world path, local world key, Xaero dimension name, and custom name. |
| `exportCurrentDimension` | action buttons | Exports every set in the current dimension as text or a file. |
| `exportCurrentWaypointSet` | action buttons | Exports the current set in the current dimension as text or a file. |
| `dedupeWaypoints` | action buttons | Merge Current processes the current set; Merge All processes every set in this dimension independently, removing later waypoints with matching coordinates and names. |
| `waypointHistory` | action buttons | Undoes or redoes the latest import and deduplication operations in the current game session, up to 5 steps; cleared when the Xaero world changes. |

File exports default to `halfmasa-xaero-yyyyMMdd-HHmm.txt` and also copy the same content to the clipboard after saving. Temporary, server-provided, and third-party dynamic waypoints are excluded from bundles. Import and deduplication operations keep complete Xaero waypoint snapshots for undo; exports are not added to the operation history. XWB2 imports match dimensions in order by formal dimension ID, Xaero equivalent dimension ID, complete container nodes, and local world key, preventing VC dimensions that share the `waypoints` world node from being merged.

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

`litematicaRefillSilent` is an optional, hotkey-configurable Easy Place refill mode, off by default. It requires FGA's `silent_take` channel and withdraws directly from verified offline inventories without spawning fake players. Already-online sources remain online. Unsupported servers suspend refill with a message.

| Config | Default | Description |
|---|---|---|
| `screenshotToClipboard` | `false` | Copies the complete image to the system clipboard whenever F2 saves a screenshot. |
| `elytraTimeTooltip` | `false` | Adds estimated remaining flight time to elytra tooltips. |
| `reportElytraTime` | unbound | Reports the equipped elytra's estimated remaining time in chat. |
| `nightVisionFade` | `true` | Enables smooth Night Vision fading; disabling it restores vanilla's final 10-second flicker. |
| `nightVisionFadeSeconds` | `5` | Sets the smooth fade duration; range `0-60`, where `0` disables early fading. |
| `boatView360` / `boatItemView` | `false` | Removes the local boat-camera rotation limit and keeps first-person held items visible while rowing. |
| `confluxMapExtensions` | `false` | Requires Conflux Map for the matching game version and extension API. Sets waypoint-list defaults, closes the map after teleporting, uses `confluxMapUnknownHeight` (default `128`) for targets with unknown height, can show local and shared waypoint lists together side by side or stacked, and adds a hotkey that creates multiple numbered local waypoints cleared when rejoining that world. |
| `inventoryMove` | `false` | Allows movement, jumping, and sneaking while vanilla inventory or container screens are open. |
| `fastWorldLoadingScreen` / `fastResourcePackLoadingScreen` | `false` | Reduces avoidable waiting in world and resource-pack loading screens. |
| `betterSavedHotbars` | `false` | Enhances creative saved hotbars with individual insertion or replacement, middle-click deletion, and retained scrolling; the legacy root `hotbar.nbt` is copied once to `config/halfmasa/better-saved-hotbars/hotbar.nbt`. |
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

## Litematica Easy Place and Printer Refill (All Supported Client Versions)

`litematicaAutoRefill` defaults to false and supports a custom hotkey. With the feature enabled, first take the configured amount (default 32; 0 means half a stack) directly from the plain Chinese-name fake player (requires the optional FGA direct_take channel), falling back to categorized queries when the source is missing, insufficient or invalid. Request external supplies only when the required easy-place item is absent from the player's inventory, hotbar, offhand and carried shulker boxes. A single matching item suppresses external refill. Recheck local supplies while waiting for queries and before taking; read one layer of shulker contents without extracting or modifying items. Wait for vanilla inventory synchronization before retrying the original schematic target. Both easy-place implementations are supported. The client requires Litematica and Fabric API; the server requires the FGA inventory API v1 and stock/inventoryTake permissions. Unsupported servers, denied permissions, missing stock and a full inventory produce a message and stop the request. An unresolved take suspends further transfers until reconnecting to prevent duplicate debits.

## Configuration Files

On the default Windows instance, the main configuration file is `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\halfmasa.json`. Legacy root-level configuration is migrated into `C:\Users\<username>\AppData\Roaming\.minecraft\config\halfmasa\legacy\`. Feature data uses temporary files and atomic replacement where applicable to avoid incomplete JSON during normal saves. Replace the default `.minecraft` directory with the absolute game directory configured by your launcher profile when necessary.

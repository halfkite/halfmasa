# halfmasa Features and Configuration

Easy Place and printer refill appear under Other Mod Extensions, each with foldable fake-stock permission, quantity (default32;0 means half a stack) and silent options. Printer adapters select the Hana coordinator or shared InventoryUtils method signatures. Exact quantities require updated server FGA. [Compatibility and acceptance](litematica-auto-refill.md)

> Documentation version: `1.6.0`

Minecraft 26.3 adds independent [schematic save, deletion, and paste block filters](litematica-block-filters_en.md) under Other Mod Extensions, matching block registry IDs.

Press `X + H` to open the configuration screen, or use Mod Menu. Unless noted otherwise, features are disabled by default. An empty hotkey means that no key is bound by default.

## Other Mod Extensions: Xaero Waypoints and Conflux Map

| Config or action | Default | Description |
|---|---|---|
| `enableWorldBinding` | `false` | Named “Xaero Waypoints and Singleplayer Save Binding” in the config. Stores the selected Xaero Minimap and World Map root IDs in `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\config\halfmasa\xaero-world-binding.json` so renamed, moved, or restored worlds can keep using the same waypoint and map data. The legacy `C:\Users\<username>\AppData\Roaming\.minecraft\saves\<world>\.halfmasa-xaero-binding.json` is migrated automatically. |
| `importWaypointBundle` | action button | Automatically imports `XWB1:`/`XWB2:` clipboard text or the first copied text file. Legacy XWB1 data targets the current dimension; XWB2 preserves dimensions and sets. |
| `exportAllDimensions` | action buttons | Exports every dimension and set in the current Xaero root container as `XWB2:` text or a UTF-8 `.txt` file, including extra dimensions created by VC and similar mods rather than only the overworld, Nether, and End. Each dimension also stores its formal dimension ID, Xaero equivalent dimension ID, complete container nodes, full world path, local world key, Xaero dimension name, and custom name. |
| `exportCurrentDimension` | action buttons | Exports every set in the current dimension as text or a file. |
| `exportCurrentWaypointSet` | action buttons | Exports the current set in the current dimension as text or a file. |
| `dedupeWaypoints` | action buttons | Merge Current processes the current set; Merge All processes every set in this dimension independently, removing later waypoints with matching coordinates and names. |
| `waypointHistory` | action buttons | Undoes or redoes the latest import and deduplication operations in the current game session, up to 5 steps; cleared when the Xaero world changes. |
| `confluxMapExtensions` | `false` | Requires Conflux Map for the matching game version and extension API. Sets waypoint-list defaults, closes the map after teleporting, uses `confluxMapUnknownHeight` (default `128`) for targets with unknown height, can show local and shared waypoint lists side by side or stacked, and adds a hotkey for multiple temporary local waypoints cleared when rejoining that world. |

File exports default to `halfmasa-xaero-yyyyMMdd-HHmm.txt` and also copy the same content to the clipboard after saving. Temporary, server-provided, and third-party dynamic waypoints are excluded from bundles. Import and deduplication operations keep complete Xaero waypoint snapshots for undo; exports are not added to the operation history. XWB2 imports match dimensions in order by formal dimension ID, Xaero equivalent dimension ID, complete container nodes, and local world key, preventing VC dimensions that share the `waypoints` world node from being merged.

## Singleplayer Save Paths

| Config | Default | Description |
|---|---|---|
| `customSavesPaths` | empty list | Custom saves path list. Absolute paths and paths relative to the game directory are supported. Click the current path in the top-left of the singleplayer world selection screen to switch immediately; the vanilla `saves` directory is always available. |
| `keepWorldSelectionOnEmpty` | `false` | When enabled, clicking Singleplayer with no worlds in the current saves path stays on world selection instead of opening world creation automatically. |

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
| `trialCreativeTab` | `false` | Adds a "Trial" tab at the end of the creative inventory with every trial spawner configuration and both trial vaults (see below). |

### Trial Tab

With `trialCreativeTab` enabled, a client-only Trial tab appears. On 26.3 with Fabric's creative-tab API, it follows normal registration, content rebuilding and pagination; toggle changes invalidate the content cache. Without that optional API, a Trial button beside the creative inventory opens its contents without affecting vanilla tabs.

- **Trial spawners**: all 14 vanilla trial chamber configurations, each with three entries — normal, ominous, and a cooldown variant — for 42 entries total.

- Each entry is a vanilla trial spawner carrying block entity data, so it places in the preset state; the cooldown variants set `cooldown_ends_at` to a future time and start in cooldown.
- **Trial vaults**: one normal and one ominous entry, 2 entries total.

On 26.3, spawner, trial spawner and vault items use the vanilla textures for their actual block state. Spawner items also preview the contained mob, including baby and size data. Tooltips show cage type, mobs, current state and trial cooldown status. This also supports natural cages copied with data and requires no custom name. Creative Ctrl + middle-click preserves the source's normal/ominous and state properties; plain middle-click omits mob data. Custom configurations unavailable to the client are reported as unknown.

GUI icons use the vanilla cage materials for the matching state, without colored rims or tinting. Configured normal/ominous cages in the trial tab use the vanilla waiting-for-players state; cooldown cages use the cooldown state, and naturally copied cages retain their source block state. Contained mobs stand upright facing the viewer in front of the cage; slimes keep their native size, while other mobs are enlarged by 20%. The foreground transform keeps the mob centered so the icon does not clip the model. Held and dropped items retain the native perspective.

The covered vanilla configs are `trial_chamber/breeze`, `melee/{husk,spider,zombie}`, `ranged/{poison_skeleton,skeleton,stray}`, `slow_ranged/{poison_skeleton,skeleton,stray}`, and `small_melee/{baby_zombie,cave_spider,silverfish,slime}`.

> Note: 1.21.1 predates the trial spawner config registry, so that version uses an equivalent inline config object instead.

## JEI/REI Lookup History (Other Mod Extensions)

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
| `keepConfigSearchPosition` (26.3) | `true` | Sub-option of configuration position memory. With the main switch enabled, remember text/key searches, search bar visibility, and scroll position separately for each MaLiLib config category during this session. |
| `keepConfigSelectedTab` (26.3) | `true` | With the main switch enabled, reopen halfmasa on the last selected tab. Limited to All, Recommended, Creative Utilities, Ported Features, Other Mod Extensions, and Disabled. |

On 26.3, expand the configuration position memory option to edit these settings. Enable the main switch, search and scroll in one tab, then switch away and back or close and reopen to check restoration. Disabling search/position memory clears that state; disabling selected-tab memory makes the normal config entry open All again. Restarting the game clears all interface memory.

## Void Trading (Extension)

Void Trading is disabled by default. When enabled, opening a trade screen for a villager in a boat or minecart can take configured fake-player names—or players detected on that vehicle—offline. Closing the screen restores them with `rejoin` by default; the alternative waits for `spawn` to appear in the player list before sending `mount`. Automatic detection can mistake real riders for fake players, so use it only when no real player is aboard.

After the villager disappears or all configured fake players go offline, automatic trading buys offers through the local player's open villager screen; keep the screen open while it runs. Enter one-based offer row numbers such as `1,2,3`; each offer is purchased until its input materials run out. Alternatively, enable the output-item whitelist to select offers by item ID. After trading, the screen can close automatically and the items obtained in that run can be dropped. Auto-open has a separate cancel hotkey, defaulting to `Esc`.

Material preparation can uncraft emerald blocks and use the QuickShulker API to retrieve emeralds or emerald blocks from carried boxes. It requires Fabric API on the client and server plus a compatible Void Trading server extension; QuickShulker retrieval also requires QuickShulker on the server.

## Input, Maps, and Utilities

On Minecraft 26.3, the Better Key Settings browser remembers its list position, each mod group's collapsed state, for the current game session; the 104/122-key layout loads from its saved sub-option. Keyboard rows use consistent boundaries. Folding a group keeps the list near its current position, and bound keys have green backgrounds and unbound keys have translucent gray backgrounds; blue markers identify vanilla bindings, orange markers identify MaLiLib bindings, and purple markers identify combinations along the right edge; the original light red single-key and dark red combination conflict indicators remain in an adjacent column. The 122-key view follows a PC/5250-style layout with F13–F24 and six terminal combination keys. Conflict Key Selection has a Conflict Selection Layout option to choose a wheel or a list with one action per row; scroll long lists and release the trigger key to execute the hovered action. Both layouts include conflicting vanilla keys, MaLiLib (Masa) hotkeys, and customized key combinations. Combination bindings conflict only when their complete key sets match; pressing C while the X selector is open switches it to matching X+C bindings.

On 26.3, Conflict Key Selection settings use names applicable to both layouts. Selection Interface Opacity is the sole background opacity control: 0 is transparent and 255 is opaque, including the list frame and scrollbar. Label inset, margin, label shadow, background blur, and the blending switch have been removed. Both layouts share base, selected, highlight, and per-binding colors, alternating brightness and its amount, size, selected-option expansion, opening animation, and background darkening. List size also scales row height, labels, and the visible row count; rendering and hit detection use the same geometry. Light backgrounds use dark text automatically. Circle detail and the center cancel zone apply only to wheel mode; list mode cancels outside rows and in the gaps between them. Existing JSON field names, colors, and opacity values remain compatible.

On 26.3, left, right, and middle mouse buttons do not automatically open the conflict selector unless a keyboard key is also held. The original actions handle attacking, placing, and tool selection conditions. Mouse combinations with keyboard keys and mouse side buttons keep the existing conflict rules.

The 26.3 binding list has Reset, Trigger in key order, and Trigger key options column headings, with Yes/No controls in the latter two columns. Standalone Shift, Ctrl, Alt, and primary mouse buttons default to no key options; complete modifier chords remain eligible, and explicit per-binding choices override these defaults. Hardware Fn normally emits no separate game key event. Disabling key options does not disable the original hotkey. Binding edits and toggles stay in a local draft until Save and exit writes the vanilla and MaLiLib configurations. Exit without saving requires confirmation only when there are changes; cancelling or pressing Escape in the confirmation returns to editing. An unchanged draft closes directly. Keyboard filters show exact matches, or bindings with the fewest additional keys, before the remaining category groups. Entering or changing worlds resets selector state and removes missed releases while preserving keys still physically held. Header buttons have translucent backgrounds, categories expand directly below their button, and tooltips render above the entire screen with wrapping and screen bounds.

On 26.3, Better Key Settings is enabled by default with a shortcut to open its browser; the old Open Keymap Browser shortcut migrates to this main option. Its children are an Open key binding screen trigger/shortcut, Key binding input, Virtual keyboard layout (104/122 keys), and Background transparency (0–100%; default 31% preserves the original appearance). The old Allow direct key rebind, Confirm on release, individual conflict editor, and reload conflict configuration entries are removed. Virtual input is the default: click a binding in the list, then click virtual keys to select or deselect them. The ordered preview uses arrows; the left-side Confirm button applies it. The confirmation-shortcut feature is removed, and Enter can be bound normally. Physical input previews keyboard and mouse presses without confirming on release. Escape clears the preview, while Backspace is bindable. Header controls retain input-mode and layout switches. Both virtual keyboards share the layout preference, which is persisted to the sub-option on Save and exit. Except for the category selector, controls use text-sized widths and wrap in order. Save and discard controls are in the header, freeing bottom space for the list; the keyboard and list follow the wrapped header. Binding, mode and layout changes remain local until Save and exit, which also confirms the current preview. Discarding changes asks for confirmation; cancelling or pressing Escape in that confirmation resumes editing.

Ignored Selection Keys displays key names with a Virtual Keys button opening a separate editor. Click a key to ignore it, and click again to undo. The editor shows keyboard and mouse binding sources with 104/122-key layouts. Reset restores WASD, both Shift/Ctrl/Alt keys and the three primary mouse buttons. The input interface has no separate Fn key code. Changes stay in a draft until Save and exit; discarding a changed draft requires confirmation, Escape returns from the confirmation, and unchanged or reverted drafts close directly. Inverted filtering displays an allow list instead. Both virtual keyboards show all bound keys in green, with blue markers for vanilla, orange markers for MaLiLib, and purple markers for combinations. All applicable markers appear together along the right edge using the previous rectangular conflict-marker style. The original light red single-key and dark red combination conflict indicators remain in an adjacent column and can appear together. Ignored-for-conflict-selection keys use a gray dot and keep their original green or translucent gray background. All six markers are 3x3 in fixed slots in two columns and three rows; absent statuses leave empty slots. Legend dots use the same size, unbound keys use translucent gray, and the current browser filter has a gold border. Numeric storage remains compatible; 26.3 expands previous default lists to include modifier and mouse keys while preserving other custom lists.

| Config | Default | Description |
|---|---|---|
| `keybindPieMenu` | `false` | Conflict Key Selection provides wheel or list layouts for conflicting keys, with shared colors, opacity, animation, size, and background darkening. |
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

26.3 adds `classicPauseMenu` (disabled by default): classic main rows for return, advancements/stats, mods, options/world options, and disconnect. Additional icons and server buttons move to the right and wrap into columns on smaller screens; actions, disabled states and existing tooltips are preserved.

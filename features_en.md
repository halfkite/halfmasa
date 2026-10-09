[中文](https://github.com/halfkite/halfmasa/blob/ds1/features_cn.md) | [English](https://github.com/halfkite/halfmasa/blob/ds1/features_en.md)

# halfmasa Features and Configuration

> Documentation version `1.6.0` | Open the configuration screen with `X + H` (or from Mod Menu) | Disabled by default unless noted; an empty hotkey means unbound

## Schematic Features

| Name | Type | Default | Description |
|---|---|---|---|
| Easy Place automatic refill<br>`litematicaAutoRefill` | Boolean with hotkey | unbound, `false` | When Easy Place runs out of an item, take it from a fake player (32 by default; 0 means half a stack), then query categorized inventory by item ID. Recheck local materials before taking items. Requires Litematica, Fabric API, and FGA (inventory API v1). Missing channel/permission, out of stock, or a full inventory displays a notice and stops; an uncertain take pauses refilling until reconnect. [Acceptance steps](https://github.com/halfkite/halfmasa/blob/ds1/docs/litematica-auto-refill.md) |
| Allow schematic refill from fake-player stock<br>`litematicaRefillAllowFake` | Boolean | `false` | Allow Easy Place refill to take items from fake-player stock. |
| Schematic refill amount<br>`litematicaRefillAmount` | Integer | `32` | Amount to take per request; 0 means half a stack. Range: 0–64. |
| Silent schematic refill<br>`litematicaRefillSilent` | Boolean with hotkey | unbound, `false` | Take items directly from offline stock without summoning or logging out a fake player. Requires the FGA `silent_take` interface. |
| Printer automatic refill<br>`printerAutoRefill` | Boolean | `false` | Refill the printer through a Hana coordinator or InventoryUtils integration. Includes fake-stock permission, amount, and silent sub-options, and can be collapsed. Specified amounts require an updated FGA server. |
| Allow printer refill from fake-player stock<br>`printerRefillAllowFake` | Boolean | `false` | Allow printer refill to take items from fake-player stock. |
| Printer refill amount<br>`printerRefillAmount` | Integer | `32` | Amount to take per printer request; 0 means half a stack. |
| Silent printer refill<br>`printerRefillSilent` | Boolean | `false` | Take printer materials directly from offline stock. |
| Schematic save block filter<br>`litematicaSaveFilter` | Boolean | `false` | Filter blocks by block ID when saving a schematic (26.3); expand to configure a whitelist or blacklist. |
| Schematic save block whitelist<br>`litematicaSaveWhitelist` | String list | `[]` | Block IDs allowed by the schematic save whitelist (26.3). |
| Schematic save block blacklist<br>`litematicaSaveBlacklist` | String list | `[]` | Block IDs excluded by the schematic save blacklist (26.3). |
| Schematic deletion block filter<br>`litematicaDeleteFilter` | Boolean | `false` | Filter blocks by block ID when deleting from a schematic (26.3). |
| Schematic deletion block whitelist<br>`litematicaDeleteWhitelist` | String list | `[]` | Block IDs allowed by the schematic deletion whitelist (26.3). |
| Schematic deletion block blacklist<br>`litematicaDeleteBlacklist` | String list | `[]` | Block IDs excluded by the schematic deletion blacklist (26.3). |
| Schematic paste block filter<br>`litematicaPasteFilter` | Boolean | `false` | Filter blocks by block ID when pasting a schematic (26.3). [Compatibility and acceptance](https://github.com/halfkite/halfmasa/blob/ds1/docs/litematica-block-filters.md) |
| Schematic paste block whitelist<br>`litematicaPasteWhitelist` | String list | `[]` | Block IDs allowed by the schematic paste whitelist (26.3). |
| Schematic paste block blacklist<br>`litematicaPasteBlacklist` | String list | `[]` | Block IDs excluded by the schematic paste blacklist (26.3). |

## Xaero Waypoints and Conflux Map

| Name | Type | Default | Description |
|---|---|---|---|
| Bind Xaero waypoints to a singleplayer save<br>`enableWorldBinding` | Boolean | `false` | Stores the Xaero root directory ID in `saves\<world>\config\halfmasa\xaero-world-binding.json`. The save can keep using the same waypoints after being renamed, moved, or restored from a backup; the legacy `.halfmasa-xaero-binding.json` file is migrated automatically. |
| Import waypoint bundle<br>`importWaypointBundle` | Action button | — | Recognizes `XWB1:`/`XWB2:` clipboard text or a text file. XWB1 imports into the current dimension; XWB2 preserves dimensions and sets. Imports match by dimension ID, container node, and world key in sequence to avoid merging VC dimensions. |
| Share all sets in all dimensions<br>`exportAllDimensions` | Action button | — | Exports all dimensions and sets as `XWB2:` text or `.txt`, including extra dimensions from VC and other mods. The default filename is `halfmasa-xaero-yyyyMMdd-HHmm.txt`; the content is also copied to the clipboard. |
| Share all sets in the current dimension<br>`exportCurrentDimension` | Action button | — | Exports every set in the current dimension. |
| Share the current waypoint set<br>`exportCurrentWaypointSet` | Action button | — | Exports the current set in the current dimension. |
| Deduplicate waypoints<br>`dedupeWaypoints` | Action button | — | Deduplicates by coordinates and name, keeping the earlier waypoint. Choose the current set or all sets in the current dimension. |
| Waypoint operation history<br>`waypointHistory` | Action button | — | Undo or redo recent imports and deduplication, up to 5 steps. History is cleared when the Xaero world changes; exports are not included. |
| Conflux Map extensions<br>`confluxMapExtensions` | Boolean | `false` | Conflux Map extensions: default to showing all dimensions or public waypoints, close the map after teleporting, show local and shared waypoints together, and create temporary local waypoints with a hotkey (cleared on re-entry). Requires the matching game version and extension API. |
| Teleport height for unknown elevation<br>`confluxMapUnknownHeight` | Integer | `128` | Y coordinate used when the target elevation is unknown. Range: -64–320. |

> Temporary, server-provided, and third-party dynamic waypoints are excluded from waypoint bundles.

## JEI/REI Recipe History

| Name | Type | Default | Description |
|---|---|---|---|
| JEI/REI recipe history<br>`itemManagerRecipeHistory` | Boolean | `false` | Records recipe searches, uses, and successfully obtained items separately for JEI and REI. Initialized when the item manager is first opened. History is stored in `config\halfmasa\search-history\`, in separate files for JEI and REI. |
| Recipe history rows<br>`itemManagerRecipeHistoryRows` | Integer | `3` | Number of rows in the history grid. Range: 1–9. |
| Recipe history position<br>`itemManagerRecipeHistoryPosition` | Option list | `bottom_right` | Position the history at the bottom-right, top-right, top-left, or bottom-left; the native item list and favorites area move out of the way. |
| Cycle recipe history position<br>`cycleItemManagerRecipeHistoryPosition` | Hotkey | unbound | Cycle through the four corners. Works with the inventory open and shows a position hint. |

## Creative Mode Tools

| Name | Type | Default | Description |
|---|---|---|---|
| Enable G-key container fill<br>`enableGiveFullInventory` | Boolean | `false` | Enable creative-mode container filling. |
| Fill a container once<br>`giveFullInventory` | Hotkey | `G` | Use the item in the main hand to fill a shulker box, chest, offhand container, or bundle. The result depends on the main hand, offhand, and container type. |
| Bundle fill attempts<br>`bundleFill` | Integer | `1` | Number of insertion attempts when a bundle is in the offhand. Range: 1–64. |
| Container nesting safety<br>`fillSafety` | Boolean | `true` | Prevent unsafe nesting of containers and shulker boxes. |
| Creative search history<br>`itemSearchHistory` | Boolean | `false` | Record items obtained from creative search and show a separate history row above the results. |
| Search history rows<br>`itemSearchHistoryRows` | Integer | `3` | Maximum number of rows in creative search history. Range: 1–9. |
| Show history while searching<br>`itemSearchHistoryDuringSearch` | Boolean | `false` | Keep the history visible while search text is entered. When disabled, it appears only after the search box is cleared. |
| Condensed creative items<br>`condensedCreative` | Boolean | `false` | Combine enchanted books, potions, tipped arrows, and block variants into expandable entries. |
| Trial tab<br>`trialCreativeTab` | Boolean | `false` | Add a “Trial” tab at the end of the creative inventory, visible only on this modded client. On 26.3, it registers normally when the Fabric Creative Tab API is installed; otherwise, open it with the button on the right. Includes 42 trial spawners (14 configurations × normal/ominous/cooldown) and 2 vaults. Items carry block-entity data. Spawner items use vanilla textures and show their contained entity; tooltips describe spawner type, entity, state, and cooldown. Ctrl+middle-click preserves state properties. Models face the player (slimes retain their size; other mobs are enlarged by 20%) and are centered in the foreground. Configurations: `trial_chamber/breeze`, `melee/{husk,spider,zombie}`, `ranged/{poison_skeleton,skeleton,stray}`, `slow_ranged/{poison_skeleton,skeleton,stray}`, `small_melee/{baby_zombie,cave_spider,silverfish,slime}`. Version 1.21.1 uses an equivalent inline configuration object. |

## Client and Interface Features

| Name | Type | Default | Description |
|---|---|---|---|
| Copy screenshots to clipboard<br>`screenshotToClipboard` | Boolean | `false` | Copy the screenshot to the system clipboard when pressing F2. |
| Elytra remaining-time tooltip<br>`elytraTimeTooltip` | Boolean | `false` | Show remaining flight time in the elytra tooltip. |
| Report elytra time in chat<br>`reportElytraTime` | Hotkey | unbound | Report the estimated remaining time for the equipped elytra in chat. |
| Smooth night-vision fade<br>`nightVisionFade` | Boolean | `true` | Fade out night vision smoothly; when disabled, restore the vanilla flicker in its final 10 seconds. |
| Night-vision fade duration (seconds)<br>`nightVisionFadeSeconds` | Integer | `5` | Fade duration; 0 means no early fade. Range: 0–60. |
| 360° boat view<br>`boatView360` | Boolean | `false` | Remove the boat camera rotation limit. |
| Show held items while rowing<br>`boatItemView` | Boolean | `false` | Keep held items visible in first person while rowing. |
| Move while in inventory<br>`inventoryMove` | Boolean | `false` | Continue moving, jumping, and sneaking while a vanilla inventory or container screen is open. |
| Close world-loading screen early<br>`fastWorldLoadingScreen` | Boolean | `false` | Reduce the additional wait on the world-loading screen. |
| Close resource-pack loading overlay early<br>`fastResourcePackLoadingScreen` | Boolean | `false` | Reduce the extra wait on the resource-pack loading screen. |
| Enhanced saved creative hotbars<br>`betterSavedHotbars` | Boolean | `false` | Drag or replace individual items, delete with middle-click, and preserve scroll position. The legacy `hotbar.nbt` is copied to `config\halfmasa\better-saved-hotbars\` once. |
| Auto-attack when cooldown is ready<br>`cooldownAutoAttack` | Boolean | `false` | While the attack key is held, attack the crosshair target when the vanilla cooldown completes. |
| Draggable lists<br>`draggableLists` | Boolean | `false` | Drag resource-pack and server-list entries; optionally hide the vanilla move arrows. |
| Fast interface scrolling<br>`fastScrolling` | Boolean | `false` | Speed up scrolling in the current screen, including MaLiLib configuration screens, without affecting the hotbar. Expand to configure two modes. |
| Enable fast-scroll mode 1<br>`fastScrollingPrimaryEnabled` | Boolean | `true` | Toggle mode 1 independently. |
| Fast-scroll mode 1 key<br>`fastScrollingPrimaryHotkey` | Hotkey | `Left Ctrl` | Hold to use the mode 1 multiplier. |
| Fast-scroll mode 1 multiplier<br>`fastScrollingPrimaryMultiplier` | Integer | `2` | Scroll multiplier for mode 1. Range: 1–32. |
| Enable fast-scroll mode 2<br>`fastScrollingSecondaryEnabled` | Boolean | `true` | Toggle mode 2 independently. Mode 2 takes priority if both modes match. |
| Fast-scroll mode 2 key<br>`fastScrollingSecondaryHotkey` | Hotkey | `Left Ctrl+Left Shift` | Hold to use the mode 2 multiplier. |
| Fast-scroll mode 2 multiplier<br>`fastScrollingSecondaryMultiplier` | Integer | `6` | Scroll multiplier for mode 2. Range: 1–32. |
| Bridging assist<br>`bridgingAssist` | Boolean with hotkey | unbound, `false` | Provide Bedrock-style wraparound placement when the crosshair is not targeting a block. Expand to set distance, crouching, axes, delay, line of sight, snapping, slab assist, torch filtering, crosshair, and outline. |
| Skip resource-pack compatibility check<br>`skipResourcePackCompatibilityCheck` | Boolean | `false` | Treat added resource packs as compatible and skip the version-mismatch confirmation. |
| Disable paused item-trajectory prediction<br>`disablePausedItemTrajectoryPrediction` | Boolean | `false` | Stop client-side dropped-item trajectory prediction while Carpet or vanilla `/tick freeze` pauses ticks. |
| Keep configuration-screen position<br>`keepModMenuScroll` | Boolean | `false` | Remember scroll positions for Mod Menu and each MaLiLib configuration category. |
| Keep search content and position<br>`keepConfigSearchPosition` | Boolean | `true` | When the main option is enabled, restore search text, key search, search-bar state, and list position per MaLiLib category. Session-only (26.3). |
| Keep selected tab<br>`keepConfigSelectedTab` | Boolean | `true` | Restore the previously selected tab when reopening configuration (26.3). |
| Classic pause menu<br>`classicPauseMenu` | Boolean | `false` | Restore the classic pause-menu button arrangement; move extra buttons to the right and use columns in small windows (26.3). |

## Void Trading

| Name | Type | Default | Description |
|---|---|---|---|
| Void trading<br>`voidTrading` | Boolean | `false` | Main switch. When opening a villager trade screen while in a boat or minecart, log out fake players selected by name or detected on the same vehicle. When the screen closes, the default recovery is `rejoin`; alternatively wait for `spawn` and then `mount`. **Caution:** auto-detection may treat a real player on the vehicle as a fake player; use it only when no real player is present. |
| Void-trading fake-player names<br>`voidTradingFakePlayerNames` | String list | `[]` | Names of fake players to log out. |
| Auto-detect vehicle fake players<br>`voidTradingAutoDetectFakePlayers` | Boolean | `false` | Automatically identify fake players on the same vehicle. |
| Fake-player recovery mode<br>`voidTradingRecoveryMode` | Option list | `rejoin` | Recovery method after closing the trade screen: `rejoin` or wait for `spawn`, then `mount`. |
| Automatically buy villager trades<br>`voidTradingAutoTrade` | Boolean | `false` | After the villager disappears or all fake players log out, the local player buys items in the open trade screen. Keep the screen open. |
| Automatically open villager screen<br>`voidTradingAutoOpen` | Boolean | `false` | Automatically open the villager trade screen. |
| Cancel auto-open hotkey<br>`voidTradingAutoOpenCancel` | Hotkey | `Esc` | Stop automatically opening the villager screen. |
| Trade-slot indices<br>`voidTradingTradeIndices` | String | empty | Enter slot indices, such as `1,2,3`. Buy each item until materials run out. |
| Trade specified items only<br>`voidTradingTradeSpecifiedItems` | Boolean | `false` | Enable the item whitelist and filter by output item ID. |
| Specified trade-item whitelist<br>`voidTradingTradeItems` | String list | `[]` | Filter trades by output item ID. |
| Close screen after trading<br>`voidTradingAutoClose` | Boolean | `false` | Close the screen after trading. |
| Drop items after trading<br>`voidTradingDropTradeItems` | Boolean | `false` | Drop items obtained during this trade. |
| Automatically uncraft emerald blocks<br>`voidTradingAutoUncraftEmeraldBlocks` | Boolean | `false` | Convert emerald blocks into emeralds before trading. |
| Take trade materials from QuickShulker<br>`voidTradingQuickShulker` | Boolean | `false` | Use QuickShulker API to take emeralds/emerald blocks from carried shulker boxes. Requires Fabric API on the client and server and a compatible Void Trading server extension; QuickShulker must also be installed on the server when this option is used. |

## Input and Key Settings

### Better Key Settings (26.3; enabled by default)

| Name | Type | Default | Description |
|---|---|---|---|
| Better Key Settings<br>`keymapSettingsGroup` | Boolean with hotkey | enabled, unbound | Main switch and hotkey to open the screen. The old “Open keymap browser” hotkey migrates here. |
| Open key-binding screen<br>`openKeymapBrowser` | Action button/hotkey | — | Open the key-binding browser. |
| Binding method<br>`keymapBindingMode` | Option list | virtual keyboard | Virtual mode: click a listed key, then select keys on the keyboard; arrows connect the preview, and the “Confirm” button applies it. Mechanical mode: press keys to preview; releasing does not confirm; Esc clears the preview; Backspace can be bound normally. |
| Virtual keyboard layout<br>`keymapKeyboardLayout` | Option list | `104-key` | Choose a 104- or 122-key layout. The 122-key layout adds F13–F24 and six terminal key combinations. |
| Key-setting background transparency<br>`keymapBackgroundTransparency` | Integer | `31` | Transparency from 0–100%; 31% preserves the original background appearance. |

**Browser behavior:** Remembers list position and collapsed mod categories; aligns keyboard rows; bound keys have green backgrounds and unbound keys translucent gray. Markers appear on the right: blue for vanilla, orange for MaLiLib, purple for key combinations; conflict markers appear in an adjacent column (light red for single-key conflicts, dark red for combination conflicts).

**List controls:** The right-side columns are “Reset,” “Trigger in key order,” and “Trigger key options”; the latter two display only yes/no. Standalone Shift/Ctrl/Alt and left/middle/right mouse buttons do not trigger options by default, but full combinations with modifiers can; Fn is handled by the keyboard hardware. Disabling a trigger option does not disable the original hotkey. Changes remain staged until “Save and Exit”; discarding changes requires confirmation.

**Mouse behavior:** Without a keyboard key held, left/right/middle mouse buttons do not automatically trigger conflict selection. Mouse combinations with keyboard keys and side buttons keep the original conflict rules.

### Conflict Key Selection

| Name | Type | Default | Description |
|---|---|---|---|
| Conflict key selection<br>`keybindPieMenu` | Boolean | `false` | Show a wheel or list to choose between actions that share a key; supports colors, transparency, animation, scale, and dimming the background. |
| Conflict selection layout<br>`keybindSelectionLayout` | Option list | wheel | Choose wheel or list mode. Each list entry occupies a row, can be scrolled, and is triggered when the binding key is released. Combinations conflict only with an exact matching combination; pressing C while the X selector is open switches to X+C. |
| Selection repeat cooldown<br>`keybindPieRepeatCooldown` | Integer | — | Tick interval between repeated triggers while the selected action is held. |
| Selection cooldown<br>`keybindPieSelectionCooldown` | Integer | — | Cooldown for selecting an action. |
| Attack selection workaround<br>`keybindPieAttackWorkaround` | Boolean | — | Clear the client attack delay when selecting an attack action. |
| Ignored selection keys<br>`keybindPieIgnoredKeys` | String | — | Comma-separated GLFW key codes ignored by the selector. |
| Invert ignored-key rule<br>`keybindPieInvertIgnoredKeys` | Boolean | `false` | Treat the list as allowed keys instead of ignored keys. |
| Wheel circle detail<br>`keybindPieCircleVertices` | Integer | — | Number of vertices used to draw the wheel circle. |
| Dim background behind selector<br>`keybindPieDarkenBackground` | Boolean | — | Dim the game view behind the wheel. |
| Expand selected option<br>`keybindPieExpansion` | Decimal | — | Expansion multiplier for the selected sector. |
| Selector size<br>`keybindPieScale` | Decimal | — | Wheel scale; in list mode this also changes row height, text size, and visible row count. |
| Center cancel area (wheel mode)<br>`keybindPieCancelZone` | Decimal | — | Center area that cancels selection on release. In list mode, pointing outside a row or between rows cancels. |
| Selector base color<br>`keybindPieMenuColor` | Color | — | Base sector color. |
| Selected option color<br>`keybindPieSelectedColor` | Color | — | Color of the selected sector. |
| Selector highlight color<br>`keybindPieHighlightColor` | Color | — | Highlight label color. |
| Brighten alternating options<br>`keybindPieAlternateLighten` | Decimal | — | Brightness added to alternating sectors. |
| Selector transparency<br>`keybindPieAlpha` | Integer | — | Sector transparency from 0–255; the border and scrollbar follow it. |
| Alternate light and dark sectors<br>`keybindPieGradation` | Boolean | — | Alternate the brightness of adjacent sectors. |
| Selector opening animation<br>`keybindPieAnimate` | Boolean | — | Play an opening animation. |
| Open per-key conflict settings<br>`openKeybindPieEditor` | Action button | — | Edit the name, category visibility, and sector color for each vanilla key ID. |
| Reload conflict-key settings<br>`reloadKeybindPieData` | Action button | — | Reload `config/halfmasa/keybind-pie/bindings.json`. |

> In 26.3, label inset, margins, text shadow, background blur, and color blending were removed. Light colors automatically use dark text; JSON field names remain compatible.

### Other Input and Utility Features

| Name | Type | Default | Description |
|---|---|---|---|
| Click and send<br>`clickAndSend` | Boolean | `false` | Send clickable non-slash command text as ordinary chat. |
| CJK-Latin spacing<br>`cjkLatinSpacing` | Boolean with hotkey | unbound, `false` | Add display spaces between Chinese text and adjacent Latin words/numbers. Separate controls exist for translated text, signs, and books; stored sign/book text is unchanged. |
| Spacing in translated text<br>`cjkLatinSpacingTranslations` | Boolean | `true` | Control display spacing in translated text. |
| Spacing on signs<br>`cjkLatinSpacingSigns` | Boolean | `true` | Control display spacing in sign text. |
| Spacing in books<br>`cjkLatinSpacingBooks` | Boolean | `true` | Control display spacing on book pages. |
| Preview maps in inventory slots<br>`mapInSlot` | Boolean | `false` | Preview filled maps in hotbar/inventory/container slots while retaining the count and decorations. |
| Server icon cache<br>`serverIconCache` | Boolean | `true` | Cache server icons; match by name, address, or both; configure the cache limit. Clearing requires confirmation. |
| Suppress toast notifications<br>`toastKiller` | Boolean | `false` | Clear existing toasts and reject new ones while enabled. |
| Server ping refresh fix<br>`serverPingerFix` | Boolean | `false` | Expand the server-ping thread pool and clear stale queued tasks. |
| In-game IME<br>`contingameIme` | Boolean | `false` | Windows JNI input method with composition text, candidate UI, temporary mode, and persistent mode. |

## Singleplayer Save Paths

| Name | Type | Default | Description |
|---|---|---|---|
| Custom save paths<br>`customSavesPaths` | String list | `[]` | Custom save directories can be absolute or relative to the game directory. Click the current path in the top-left of world selection to switch immediately; vanilla `saves` remains available. |
| Keep world selection when empty<br>`keepWorldSelectionOnEmpty` | Boolean | `false` | If the selected directory contains no worlds, clicking “Singleplayer” still opens world selection instead of jumping to world creation. |

## Entity Aggregation Rendering

| Name | Type | Default | Description |
|---|---|---|---|
| Disable fluid rendering<br>`disableFluidRendering` | Boolean | `false` | Disable fluid rendering; may significantly change the appearance. |
| Disable non-source fluid rendering<br>`disableNonSourceFluidRendering` | Boolean | `false` | Disable rendering of non-source fluids only. |
| Aggregate rendering for similar entities<br>`entityRenderAggregation` | Boolean | `false` | Aggregate rendering for similar entities; may significantly affect appearance or compatibility. |
| Merge dropped-item rendering<br>`itemRenderAggregation` | Boolean | `false` | Merge dropped-item rendering. |
| Count only; hide entity models<br>`entityAggregationCountOnly` | Boolean | `false` | Show counts without rendering aggregate models. |
| Entity aggregation radius<br>`entityAggregationRadius` | Decimal | — | Radius used for entity aggregation. |
| Entity aggregation threshold<br>`entityAggregationThreshold` | Integer | — | Entity-count threshold that triggers aggregation. |
| Entity aggregation scan interval<br>`entityAggregationScanInterval` | Integer | — | Tick interval between aggregation scans. |
| Entity aggregation label position<br>`entityAggregationLabelPosition` | Option list | — | Position of the aggregate label. |
| Entity aggregation list mode<br>`entityAggregationListMode` | Option list | — | Whitelist, blacklist, or disabled. |
| Entity aggregation whitelist<br>`entityAggregationWhitelist` | String list | `[]` | Entity types included in aggregation. |
| Entity aggregation blacklist<br>`entityAggregationBlacklist` | String list | `[]` | Entity types excluded from aggregation. |

> These features are in the “Disabled Features” category and are not included on the recommended page. Enable them only after understanding their effects.

## Configuration Files and Data Paths

Main configuration: `config\halfmasa\halfmasa.json`

| Data | Path relative to `.minecraft` |
|---|---|
| Main configuration | `config\halfmasa\halfmasa.json` |
| Keybind wheel | `config\halfmasa\keybind-pie\bindings.json` |
| Server icon cache | `config\halfmasa\server-icons\` |
| JEI/REI recipe history | `config\halfmasa\search-history\` |
| Xaero world binding | `saves\<world>\config\halfmasa\xaero-world-binding.json` |
| Enhanced saved hotbars | `config\halfmasa\better-saved-hotbars\hotbar.nbt` |

Persistent data related to multiplayer servers is stored under `config\halfmasa\`, not in singleplayer save directories.

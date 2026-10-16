[中文](https://github.com/halfkite/halfmasa/blob/main/docs/features_cn.md) | [English](https://github.com/halfkite/halfmasa/blob/main/docs/features_en.md)

# halfmasa Features and Configuration

Version 1.7.0 backports range feeding, Auto Eat, Better Boss Bar, invisible block markers, animation controls, custom entity rendering filters and the combined Conflux Map list to every project target. Vanilla preedit display settings apply to 26.1+; older targets retain the Windows JNI input method.

Conflux Map 0.1.11-beta.1 requires Fabric Loader 0.19.0+ on 26.x; integration checks use 0.19.5. The standalone HalfMasa minimum remains defined by its mod metadata.

> Documentation version `1.7.0` | Open the configuration screen with `X + H` (or from Mod Menu) | Disabled by default unless noted; an empty hotkey means unbound

## Invisible Block Markers

Disabled by default in client configuration, with an optional hotkey. Its three children, Show Barriers, Show Light Blocks and Show Structure Voids, are enabled by default.

Shows vanilla markers without holding special items or requiring creative mode. Uses vanilla random sampling, particle textures, particle settings and occlusion. Light blocks retain their level-specific vanilla icons without additional number overlays. Visibility fills in through sampling; existing markers expire naturally after disabling the feature or removing a block, with the vanilla 80-tick lifetime of about four seconds at normal speed.

Type switches control added markers while retaining vanilla held-item behavior. A sample that already produced a vanilla marker receives no duplicate. Uses client-received block data and requires no server extension.

## Auto Eat

Independent client feature, disabled by default, with a master hotkey and expandable settings. Starts at 14/20 hunger and continues to a 20/20 target by default. The food-source row has a cycling option, an independent hotkey and a shared reset: Hotbar → Inventory → QuickShulker.

All sources share the filter. Default whitelist: golden carrot, cooked porkchop, bread. Default blacklist: golden apple, enchanted golden apple. Both lists are preserved when switching filters. Smart prefers food that does not overshoot the target, then nutrition and saturation; left-to-right and highest nutrition are also available. Items need valid vanilla food data and positive nutrition. Minecraft 1.21–1.21.1 uses legacy food properties; newer targets also require CONSUMABLE.

QuickShulker mode searches hotbar, inventory, carried shulker boxes, then the ender chest when its item is carried. Primary target: MoRanpcy QuickShulker's 26.3 opening protocol, with matching QuickShulker and Fabric API on both sides. Falls back to inventory without support; no Void Trading server extension is required. No nested-container search. Container screens may appear briefly; no open request is sent with full storage.

Normal container requests use server synchronization rather than local item prediction. Manual input always takes priority; inactive in creative and spectator modes. Conflicting restore slots are left untouched and reported. A remote timeout disables remote retrieval for that connection; rejoin to retry. Empty containers are not repeatedly opened; changing the source, filter or carried container permits a new search.

Bowls, bottles and other use remainders are preserved. Stacked food that needs additional remainder space is skipped when that space is unavailable. Death and leaving the world cancel the flow and clear its state.

With slot restoration enabled, unused food retrieved through QuickShulker is returned to its original container slot before temporary swaps and the selected hotbar slot are restored. Only items withdrawn by this operation are returned. Manual changes to the container or slot stop restoration and retain the items without overwriting or dropping them.

Silent eating is disabled by default. Singleplayer supports it directly. Multiplayer requires a game-version-matched FGA build implementing this protocol, Carpet and Fabric API on the server, with `/carpet silentAutoEat true`; FGA is optional on the client. The server validates the actual stack and invokes vanilla consumption, preserving hunger, saturation, food effects and use remainders without use animations, eating sounds or food particles. Unsupported servers and disabled rules fall back to ordinary eating with a notification. An unresolved request pauses consumption to prevent duplicate transactions.

## Better Boss Bar

The master switch is disabled by default, with an optional hotkey. Its children are Merge Similar Boss Bars (on), Show Boss Health (off), and Compact Boss Bars (off). Each child has an independent, initially unbound hotkey that toggles that child; rendering still requires the master switch. Rows contain a toggle, hotkey and reset; reset restores the default toggle and clears the binding. The merge child expands into the settings below. Previous merge activation, hotkeys and custom group references migrate to the master switch.

| Setting | Default | Description |
|---|---|---|
| Matching mode | Name and style | Matches visible text, text styling, bar colors and overlays; excludes hidden hover, click and insertion metadata; name-only mode uses the first bar's style |
| Merge different health levels | Off | Matches exact server target progress rather than animated progress |
| Merged health progress | Average | Average, minimum or maximum vanilla animated progress |
| Show boss count | On | Appends `×N` to merged names; single entries keep their names |
| Maximum visible bars | `0` | An extra limit while enabled; `0` adds no limit and preserves vanilla height limits |

For `80%, 80%, 40%`, the default produces two bars: `×2` at 80% and a single bar at 40%. Allowing different health gives approximately 66.7% in average mode. Groups are recalculated each frame without changing original events, boss music, sky darkening or fog. Matching uses boss-bar properties, not entity types; custom server bars can also match. Invalid progress stays independent and does not affect merged progress.

Health uses reliable entity associations or server data, such as `240/300`. Groups show totals only when every member has reliable values: two Withers can display `480/600`. Loaded Withers can be identified by entity UUID in hover text when their health ratio agrees with the bar. The integrated server or this FGA build can identify vanilla Wither and Ender Dragon bars by boss UUID. Other bars fall back to percentages without guessed maximum health.

Compact mode uses thin bars with the name, count and health on one row. Long names are shortened while retaining the health suffix. Disabling the master switch restores vanilla rendering.

## Item Despawn Timer

Disabled by default in the client configuration, with an independent optional hotkey and expandable settings. It works with dropped-item aggregation and entity highlighting disabled.

| Setting | Default | Description |
|---|---|---|
| `itemDespawnTimer` | `false` | Independent timer switch |
| `itemDespawnSource` | Auto | Auto / Servux / client estimate |
| `itemDespawnFormat` | Minutes:seconds | Minutes:seconds / seconds / game ticks |
| `itemDespawnThroughWalls` | `true` | Show labels behind blocks |
| `itemDespawnMaxLabels` | `64` | Prefer the nearest 8–256 labels |
| `itemDespawnSyncInterval` | `20` ticks | Minimum server Age request interval, range 10–200; at most 4 requests submitted per tick |
| `itemDespawnDistance` | `64` blocks | Range 8–128, limited to loaded client entities |

Auto reads Age on the local server thread in singleplayer. In multiplayer it reuses MiniHUD's Servux queue and server data, or uses its own Servux client when MiniHUD is absent. Versions up to 1.21.10 use v1 vanilla NBT replies; 1.21.11 and 26.x use v2 data, supporting both uncompressed named NBT and length-prefixed GZIP envelopes. Older MiniHUD is integrated through its server-reply handler, since its cache can also contain client-generated Age. The network path needs Fabric networking modules. Disabled MiniHUD synchronization or denied server queries produce estimates in Auto and `--:--` in Servux mode.

`≈` marks estimates from first observation or stale samples; `∞` requires a fresh sample confirming persistence. Aggregated labels use the shortest predicted member lifetime and mark incomplete data as uncertain. Seconds assume 20 TPS and the vanilla 6000-tick lifetime. Custom rules, low TPS and actual item merges can change the prediction. World changes, entity unloading and disabling the feature clear tracking.

Available for every project target: 1.21–1.21.1, 1.21.2–1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3. Explicit saved toggle values are preserved; new and reset configurations default to disabled.

Ordinary Servux entity replies are implemented. Splitter packets 10/11 are currently reported as unsupported and never interpreted as Age. Large component data works when the upstream server can send an ordinary reply; missing samples follow the selected fallback policy.

## Schematic Features

| Name | Type | Default | Description |
|---|---|---|---|
| Easy Place automatic refill<br>`litematicaAutoRefill` | Boolean with hotkey | unbound, `false` | When Easy Place runs out of an item, take it from a fake player (32 by default; 0 means half a stack), then query categorized inventory by item ID. Recheck local materials before taking items. Requires Litematica, Fabric API, and FGA (inventory API v1). Missing channel/permission, out of stock, or a full inventory displays a notice and stops; an uncertain take pauses refilling until reconnect. See the compatibility and configuration details in this section. |
| Allow schematic refill from fake-player stock<br>`litematicaRefillAllowFake` | Boolean | `false` | Allow Easy Place refill to take items from fake-player stock. |
| Schematic refill amount<br>`litematicaRefillAmount` | Integer | `32` | Amount to take per request; 0 means half a stack. Range: 0–64. |
| Silent schematic refill<br>`litematicaRefillSilent` | Boolean with hotkey | unbound, `false` | Take items directly from offline stock without summoning or logging out a fake player. Requires the FGA `silent_take` interface. |
| Printer automatic refill<br>`printerAutoRefill` | Boolean | `false` | Refill the printer through a Hana coordinator or InventoryUtils integration. Includes fake-stock permission, amount, and silent sub-options, and can be collapsed. Specified amounts require an updated FGA server. |
| Allow printer refill from fake-player stock<br>`printerRefillAllowFake` | Boolean | `false` | Allow printer refill to take items from fake-player stock. |
| Printer refill amount<br>`printerRefillAmount` | Integer | `32` | Amount to take per printer request; 0 means half a stack. |
| Silent printer refill<br>`printerRefillSilent` | Boolean | `false` | Take printer materials directly from offline stock. |
| Schematic save block filter<br>`litematicaSaveFilter` | Boolean | `false` | Filter blocks by block ID when saving a schematic; expand to configure a whitelist or blacklist. |
| Schematic save block whitelist<br>`litematicaSaveWhitelist` | String list | `[]` | Block IDs allowed by the schematic save whitelist. |
| Schematic save block blacklist<br>`litematicaSaveBlacklist` | String list | `[]` | Block IDs excluded by the schematic save blacklist. |
| Schematic deletion block filter<br>`litematicaDeleteFilter` | Boolean | `false` | Filter blocks by block ID when deleting from a schematic. |
| Schematic deletion block whitelist<br>`litematicaDeleteWhitelist` | String list | `[]` | Block IDs allowed by the schematic deletion whitelist. |
| Schematic deletion block blacklist<br>`litematicaDeleteBlacklist` | String list | `[]` | Block IDs excluded by the schematic deletion blacklist. |
| Schematic paste block filter<br>`litematicaPasteFilter` | Boolean | `false` | Filter blocks by block ID when pasting a schematic; see the configuration details in this section. |
| Schematic paste block whitelist<br>`litematicaPasteWhitelist` | String list | `[]` | Block IDs allowed by the schematic paste whitelist. |
| Schematic paste block blacklist<br>`litematicaPasteBlacklist` | String list | `[]` | Block IDs excluded by the schematic paste blacklist. |

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
| Condensed creative items<br>`condensedCreative` | Boolean | `false` | Combine enchanted books, potions, tipped arrows, and block variants into expandable entries. Inventory hover tooltips omit source attribution. |
| Grouping mode (26.3)<br>`condensedCreativeGrouping` | Option list | `default` | Existing categories / Material first. Material first groups block forms of the same material, limited to entries in the current tab or search results. |
| Keep entries expanded (26.3)<br>`condensedCreativeKeepExpanded` | Boolean | `true` | Remember expanded entries per tab and grouping mode when reopening the inventory during this game session; not written to disk and cleared on restart. |
| Entry background (26.3)<br>`condensedCreativeBackground` | Boolean | `false` | Translucent group backgrounds; existing preferences are preserved. |
| Rotating group preview (26.3)<br>`condensedCreativeRotatingPreview` | Boolean | `false` | Cycle collapsed icons once per second. Disabled by default; existing preferences are preserved. |
| Trial tab<br>`trialCreativeTab` | Boolean | `false` | Add a “Trial” tab at the end of the creative inventory containing 42 trial spawners (14 configurations × 3 states: normal/ominous/cooldown, with block-entity data) and 2 Trial Vaults. |

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
| Keep screen position (26.3)<br>`keepScreenPosition` | Hotkeyed boolean | `false` | Remember Mod Menu scrolling; children control configuration search/scroll, selected category and creative inventory state. Earlier versions retain `keepModMenuScroll`. |
| Keep search content and position<br>`keepConfigSearchPosition` | Boolean | `true` | When the main option is enabled, restore search text, key search, search-bar state, and list position per MaLiLib category. Session-only. |
| Keep selected tab<br>`keepConfigSelectedTab` | Boolean | `true` | Restore the previously selected tab when reopening configuration. |
| Keep creative screen position (26.3)<br>`keepCreativeScreenPosition` | Hotkeyed boolean | `false` | Restore the creative tab, page, search and separate scrolling for each tab while the master is enabled. Session only. |
| Classic pause menu<br>`classicPauseMenu` | Boolean | `false` | Restore the classic pause-menu button arrangement; move extra buttons to the right and use columns in small windows. |

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

### Better Key Settings (enabled by default)

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
| In-game IME<br>`contingameIme` | Boolean | `false` | Windows JNI on older versions; custom display options for vanilla IME on 26.1+. Disabling the master switch preserves vanilla input and UI and releases the legacy JNI hook. |

## Singleplayer Save Paths

| Name | Type | Default | Description |
|---|---|---|---|
| Custom save paths<br>`customSavesPaths` | String list | `[]` | Custom save directories can be absolute or relative to the game directory. Click the current path in the top-left of world selection to switch immediately; vanilla `saves` remains available. |
| Keep world selection when empty<br>`keepWorldSelectionOnEmpty` | Boolean | `false` | If the selected directory contains no worlds, clicking “Singleplayer” still opens world selection instead of jumping to world creation. |

## Entity and Block Highlights

| Name | Type | Default | Description |
|---|---|---|---|
| Entity and block highlights<br>`entityGlowHighlight` | Boolean with hotkey | `false` | Outline entities and dropped items, plus configured blocks through walls; client side only. |
| Entity highlight mode<br>`entityGlowMode` | Option list | All | Highlight all entities and dropped items, or only enabled rules; blocks always follow their rules. |
| Switch entity highlight mode<br>`entityGlowModeHotkey` | Hotkey | Unbound | Switch between all and individual modes without enabling the main switch. |
| Highlight all color<br>`entityGlowAllColor` | Color | White | Outline color in all mode. |
| Highlight rule editor<br>`entityGlowRuleEditor` | Action | — | Set colors, enabled states, and hotkeys by entity, item, or block ID; changes apply after saving. |

## Entity Aggregation Rendering

Mob and dropped-item aggregation have separate expandable settings, refresh schedules, filters and label positions. Merge count means the minimum number of entities; previous settings are converted to preserve their trigger count.

| Name | Type | Default | Description |
|---|---|---|---|
| Disable fluid rendering<br>`disableFluidRendering` | Boolean | `false` | Disable fluid rendering; may significantly change the appearance. |
| Disable non-source fluid rendering<br>`disableNonSourceFluidRendering` | Boolean | `false` | Disable rendering of non-source fluids only. |
| Aggregate rendering for similar entities<br>`entityRenderAggregation` | Boolean | `false` | Aggregate rendering for similar entities; may significantly affect appearance or compatibility. |
| Separate ages and sizes<br>`entityAggregationSeparateSizes` | Boolean | `true` | Count babies, adults, and different sizes separately and label each group. |
| Count only; hide entity models<br>`entityAggregationCountOnly` | Boolean | `false` | Hide grouped mob models; dropped items are unaffected. |
| Entity aggregation radius<br>`entityAggregationRadius` | Decimal | `1` | Maximum neighbor distance for chained mob aggregation. |
| Entity merge count<br>`entityAggregationThreshold` | Integer | `11` | Minimum matching mob count required to merge. |
| Entity aggregation scan interval<br>`entityAggregationScanInterval` | Integer | `10` | Tick interval between scans; 10 ticks is about half a second. |
| Entity aggregation label position<br>`entityAggregationLabelPosition` | Option list | Top | Above or beside the mob. |
| Entity aggregation list mode<br>`entityAggregationListMode` | Option list | — | Whitelist, blacklist, or disabled. |
| Entity aggregation whitelist<br>`entityAggregationWhitelist` | String list | `[]` | Entity types included in aggregation. |
| Entity aggregation blacklist<br>`entityAggregationBlacklist` | String list | `[]` | Entity types excluded from aggregation. |

| Independent dropped-item settings | Type | Default | Description |
|---|---|---|---|
| Merge dropped-item rendering<br>`itemRenderAggregation` | Boolean | `true` | Render one model per stack; merge nearby item entities with matching items and components, showing total item quantity. |
| Dropped item merge count<br>`itemAggregationThreshold` | Integer | `2` | Minimum number of dropped stack entities required to merge. |
| Dropped item merge radius<br>`itemAggregationRadius` | Decimal | `1` | Maximum neighbor distance for chained item aggregation. |
| Dropped item list mode<br>`itemAggregationListMode` | Option list | No list | Whitelist handles listed items only; blacklist excludes listed items. |
| Dropped item whitelist<br>`itemAggregationWhitelist` | String list | `[]` | Item IDs, such as `minecraft:stone`; an empty whitelist handles no items. |
| Dropped item blacklist<br>`itemAggregationBlacklist` | String list | `[]` | Item IDs excluded from aggregation. |
| Dropped item label position<br>`itemAggregationLabelPosition` | Option list | Top | Above or beside the item. |
| Dropped item refresh interval<br>`itemAggregationScanInterval` | Integer | `10` | Independent refresh interval in client ticks. |

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

## Hold-to-show bindings, boss bar size and animation controls

Better Boss Bars supports 50%–200% scaling shared by normal and compact layouts. Boss bars and invisible blocks have independent hold-only switches (disabled by default) and passive display bindings excluded from conflict selection. Releasing the binding hides markers immediately. Disabling the master restores vanilla behavior.

Related boolean child options support independent hotkeys, including invisible block types, advanced boss merge toggles and auto-eat boolean settings. Remove Attack and Use Cooldown Animations and all its children default to disabled. Only local visuals change; vanilla attacks, mining, item use, cooldowns and third-person animations remain intact.

## Conflux Map combined waypoint list

Enable Conflux Map Extensions and Show Both Local and Shared Waypoints to use one scroll area for both sources. Server waypoints come first by default; change the source order with the top button or child configuration. Preserve native distance ordering within each source.

Conflux Map extension toggles default to off, preserving saved user choices. Compact Waypoints and its independent dimension and distance options default to off and support hotkeys. Compact cards show names and native icons; hovering opens full details and an interactive native action panel with the actions available for that source: selection, visibility, sharing, editing, delete confirmation, locate and teleport. Disabling compact preserves the original native list, selection and actions. Entries fill left to right before wrapping, including personal entries immediately after server entries. Filtering, deletion and synchronization rebuild a contiguous layout and clamp scrolling.

Editing, teleporting and permission checks use the native Conflux Map actions. Resolve targets by source and waypoint ID at click time so a deleted card cannot redirect its action to a neighboring entry. Dropdowns render above the list and receive input first. Disabling the combined view restores the native screen. The bridge targets the Conflux Map 0.1.11-beta.1 UI contract and requires its matching game-version build. An incompatible UI contract restores the native list.

## Range feeding

Range Feeding defaults to off. Enable it to request one batch with Feed Nearby Animals Once, using current main-hand food and stopping when the batch is complete. Hold to Enable Range Feeding temporarily runs continuous batches even with the master switch off, without changing its saved value. Releasing clears only the hold queue. All related keys default to unbound and use MaLiLib binding buttons.

Do Not Feed Babies defaults to on and has an independent toggle key. It checks `Animal.isBaby()` in every list mode and does not affect manual right-click feeding. Before each interaction, recheck baby protection, current lists, food, entity validity, line of sight and distance. Defaults to 3 blocks from the player's eyes to the target hitbox, also capped by actual entity interaction reach. Lists match registered entity type IDs with an optional `minecraft:` namespace; invalid IDs never match a default entity, and an empty whitelist feeds nothing.

Feeding Interval accepts 1–200 ticks and defaults to 4 ticks (about 5 interactions per second at 20 TPS). Lower values are faster. Single, hold, rescans and repeated baby feeding share the configured interval; live changes take effect immediately. Changing baby protection refreshes active queues without repeating processed targets in a single batch. Screens, focus loss, death, disconnects and dimension changes cancel tasks and require a fresh press. Manual attack/use and ongoing item use pause automatic interactions.

## Custom entity rendering disable

The Disabled category gains an off-by-default, collapsible toggle with its binding on the same row. Cycle blacklist, whitelist and no-list modes with the button or independent hotkey. No list hides all unprotected entities; whitelist hides only listed entities (empty hides none); blacklist hides entities outside the list and keeps listed entities visible (the default empty blacklist hides all unprotected entities). Hover the List Mode label or button for the full explanation. Cached rules match registered entity type IDs, accept an omitted minecraft namespace and never fall back to a default entity for invalid IDs.

Hold to Show Entities defaults to off and must be enabled before its temporary display key works. Its passive key is unbound and excluded from conflict selection. Holding bypasses only this feature. Releasing, opening a screen, losing window focus or leaving the world ends the bypass. Protect the local player and camera entity. Existing aggregation and external rendering rules remain independent; collisions, interactions and server simulation are preserved.


## MiniHUD container preview optimization

MiniHUD Container Preview Optimization (26.3) defaults to enabled under Other Mod Extensions, with no additional hotkey. It caches data preparation for container, shulker box, bundle and ender chest previews. Following Jade's separation of updates and drawing, held world-container previews validate contents per client tick and reuse preparation within that tick; new data, source replacement, block-state changes and target changes can refresh earlier. Different slot-count requests share content validation. The preview screen and its independent layout are reused. Native target checks, data requests and live drawing still run every frame, preserving complete slots, counts, colors and controls. Generic grids use independent display-stack snapshots validated on ticks and data updates, with immediate count changes, avoiding per-frame copies; special layouts and hover tooltips keep their existing protection. Crafter disabled slots can update immediately. Disconnects, resource reloads and disabling clear caches; disabling restores the original path. Mixins do not apply without MiniHUD. Maps and enchantment effects remain live; frame render states are not cached. Jade is not required.


## MiniHUD independent preview

MiniHUD Independent Preview (26.3) defaults to off under Other Mod Extensions, with an unbound toggle hotkey. Enabling it keeps world-container previews available with the original MiniHUD preview key while its main rendering toggle is off. MiniHUD preview controls, background colors and target detection remain in use. When the main toggle is on, MiniHUD handles the preview once through its native path; when off, only the world-container preview is added and info lines remain disabled. Item, bundle, ender chest and map tooltip previews retain their settings. This toggle is independent of preview optimization and does nothing without MiniHUD.

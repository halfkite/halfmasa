# Schematic Save, Deletion, and Paste Block Filters

This new feature currently targets Minecraft 26.3 and requires a compatible Litematica installation.

Open halfmasa's **Other Mod Extensions** tab and enable the independent schematic save, deletion, or paste filter. Expand each group to edit its mode and lists. All three switches support custom hotkeys and are disabled by default, with separate modes and lists.

| Mode | Behavior | Empty list |
|---|---|---|
| Blacklist (default) | Exclude listed blocks | Allow all blocks |
| Whitelist | Allow only listed blocks | Allow no blocks |

Each list is saved separately. Only the selected mode's list is used. Enter one block registry ID per row, such as `minecraft:stone`, `minecraft:chest`, or `modnamespace:block_name`. Missing namespaces default to `minecraft`. Item names, wildcards, tags, and block state conditions are not supported.

## Saving from the World

Filtering applies while creating a schematic from world blocks, for both whole-region and chunk-based reads. Excluded blocks become air, without block entity data or scheduled block/fluid ticks. Region positions and dimensions are preserved. World blocks remain untouched. Entity saving follows Litematica's own settings.

For example, a whitelist containing only `minecraft:stone` preserves stone and saves other blocks as air. Native ignore-block settings still apply.

This does not refilter already loaded schematic files, format conversions, or simple Save As operations. Create a new schematic from the world to apply the filter.

## Deleting World Blocks

Placement and area deletion check the **actual world block ID**. Different states share the same ID, so all chest orientations match `minecraft:chest`. Native placement deletion modes and layer ranges still apply.

Both direct singleplayer deletion and multiplayer command deletion are filtered. Area deletion queues commands only for permitted contiguous blocks, retaining Litematica's command queue and required permissions. Complex filters over large regions may generate more commands. Protected chests and their contents are preserved. Entity deletion follows Litematica's existing setting independently of block filtering.

## Pasting Schematics

Filter by the **source schematic's block IDs**. Supports direct singleplayer pasting and multiplayer command pasting, including bulk `/fill`, WorldEdit, and block entity data handling. Allowed blocks still follow Litematica's replacement mode, layer range, and NBT settings.

Excluded blocks are skipped, preserving destination blocks and container contents. The schematic file and rendered preview remain unchanged. This switch does not control Easy Place or printer placement. Direct pasting also skips scheduled block/fluid ticks for excluded source blocks. Entity pasting follows Litematica's own settings.

For example, whitelist only `minecraft:stone` to paste stone and preserve other positions. Blacklist `minecraft:air` to prevent schematic air from clearing world blocks; add `minecraft:cave_air` and `minecraft:void_air` separately if present in your schematic.

## In-Game Checks

1. Place stone, dirt, and a chest containing items. Whitelist only `minecraft:stone` for saving, create a new schematic, and confirm that only stone remains while dimensions are unchanged.
2. Blacklist `minecraft:chest` for deletion. Try placement and area deletion, confirming that the chest and contents remain while other eligible blocks are removed.
3. Switch to an empty whitelist and confirm no blocks are deleted. Disable the filter and confirm native Litematica behavior is restored.
4. Whitelist only `minecraft:stone` for pasting. Paste a schematic containing stone, air, and chests onto existing dirt and stocked chests. Confirm only stone is pasted; excluded schematic chests neither overwrite destination chests nor change their contents. Check direct pasting and both `/setblock` and `/fill` command modes.

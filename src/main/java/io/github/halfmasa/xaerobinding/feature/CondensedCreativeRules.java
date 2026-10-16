package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
//#if MC > 1.21.10
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.WoodType;

import io.github.halfmasa.xaerobinding.config.CondensedCreativeGrouping;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Classifies only the blocks present in a creative tab; it never manufactures group members. */
final class CondensedCreativeRules
{
    private static final Set<String> TABS = Set.of(
            "building_blocks", "colored_blocks", "natural_blocks", "functional_blocks", "redstone_blocks", "search");
    private static final List<String> COLORS = List.of(
            "light_gray", "light_blue", "white", "gray", "black", "brown", "red", "orange", "yellow",
            "lime", "green", "cyan", "blue", "purple", "magenta", "pink");
    // WoodType is populated by the game, so new vanilla wood sets do not require another name whitelist.
    private static final List<String> WOODS = WoodType.values().map(WoodType::name)
            .filter(name -> !name.contains(":") || name.startsWith("minecraft:"))
            .map(name -> name.replace("minecraft:", ""))
            .sorted(Comparator.comparingInt(String::length).reversed()).toList();
    private static final Set<String> WOOD_FORMS = Set.of(
            "log", "wood", "stem", "hyphae", "planks", "stairs", "slab", "fence", "fence_gate", "door",
            "trapdoor", "pressure_plate", "button", "sign", "hanging_sign", "shelf", "leaves", "sapling",
            "propagule", "block", "mosaic", "mosaic_stairs", "mosaic_slab");
    private static final Set<String> COLORED_FORMS = Set.of(
            "wool", "wool_stairs", "wool_slab", "carpet", "bed", "banner", "candle", "stained_glass",
            "stained_glass_pane", "terracotta", "glazed_terracotta", "concrete", "concrete_stairs",
            "concrete_slab", "concrete_powder", "shulker_box", "cushion");
    // Specific compound materials must precede their generic words (sandstone/stone, nether brick/brick).
    private static final List<String> MATERIALS = List.of(
            "red_sandstone", "sandstone", "end_stone", "nether_brick", "cobblestone", "blackstone",
            "deepslate", "prismarine", "granite", "diorite", "andesite", "basalt", "quartz", "tuff",
            "mud", "resin", "sulfur", "cinnabar", "copper", "iron", "gold", "netherite", "diamond",
            "emerald", "lapis", "redstone", "coal", "amethyst", "brick", "stone");

    private CondensedCreativeRules() {}

    //#if MC > 1.21.10
    private static Identifier identifier(String path) { return Identifier.withDefaultNamespace(path); }
    //#else
    //$$ private static ResourceLocation identifier(String path) { return ResourceLocation.withDefaultNamespace(path); }
    //#endif

    static String blockGroup(ItemStack stack, String path, String tab)
    {
        if (!TABS.contains(tab)) return null;
        if (Configs.CONDENSED_CREATIVE_GROUPING.getOptionListValue() == CondensedCreativeGrouping.MATERIAL)
        {
            String material = materialGroup(path);
            if (material != null) return material;
        }
        return switch (tab)
        {
            case "building_blocks" -> materialGroup(path);
            case "colored_blocks" -> coloredGroup(path);
            case "natural_blocks" -> naturalGroup(stack, path);
            case "functional_blocks" -> functionalGroup(stack, path);
            case "redstone_blocks" -> redstoneGroup(stack, path);
            case "search" -> searchGroup(stack, path);
            default -> null;
        };
    }

    static String itemGroup(String path, String tab)
    {
        // Cushions are placed as entities; bundles and harnesses are also ordinary items, not BlockItems.
        if (!Set.of("colored_blocks", "functional_blocks", "tools_and_utilities", "search").contains(tab)) return null;
        String family = withoutColor(path);
        return Set.of("cushion", "bundle", "harness").contains(family) ? "colored:" + family : null;
    }

    static Component materialTitle(String key, List<ItemStack> children)
    {
        String family = key.substring(key.indexOf(':') + 1);
        String base = key.startsWith("wood:") ? family + "_planks" : switch (family)
        {
            case "iron", "gold", "netherite", "diamond", "emerald", "lapis", "redstone", "coal", "amethyst", "copper" -> family + "_block";
            case "nether_brick" -> "nether_bricks";
            case "wool" -> "white_wool";
            case "concrete" -> "white_concrete";
            case "resin" -> "resin_bricks";
            default -> family;
        };
        //#if MC > 1.21.10
        var item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(base));
        //#elseif MC > 1.21.1
        //$$ var item = BuiltInRegistries.ITEM.getValue(ResourceLocation.withDefaultNamespace(base));
        //#else
        //$$ var item = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(base));
        //#endif
        Component name = item != null && item != Items.AIR ? item.getDefaultInstance().getHoverName() : children.getFirst().getHoverName();
        return Component.translatable("halfmasa.feature.condensed_creative.material_group", name);
    }

    private static String materialGroup(String path)
    {
        // Keep hazardous lookalikes in their own group, rather than mixing them into ordinary stone.
        if (path.startsWith("infested_")) return "type:infested_blocks";
        String normalized = withoutColor(path.startsWith("stripped_") ? path.substring(9) : path);
        for (String wood : WOODS)
        {
            if (normalized.startsWith(wood + "_") && WOOD_FORMS.contains(normalized.substring(wood.length() + 1)))
                return "wood:" + wood;
        }
        if (normalized.equals("bamboo")) return "wood:bamboo";
        if (Set.of("wool", "wool_stairs", "wool_slab", "carpet").contains(normalized)) return "material:wool";
        if (Set.of("concrete", "concrete_stairs", "concrete_slab", "concrete_powder").contains(normalized)) return "material:concrete";
        if (normalized.equals("terracotta") || normalized.equals("glazed_terracotta")) return "material:terracotta";
        if (Set.of("glass", "glass_pane", "stained_glass", "stained_glass_pane", "tinted_glass").contains(normalized)) return "material:glass";
        if (path.endsWith("_ore"))
        {
            String ore = path.substring(0, path.length() - 4);
            if (ore.startsWith("deepslate_")) ore = ore.substring(10);
            if (ore.startsWith("nether_")) ore = ore.substring(7);
            if (MATERIALS.contains(ore)) return "material:" + ore;
        }
        for (String material : MATERIALS)
        {
            if (hasPart(path, material) || hasPart(path, material + "s"))
                return "material:" + switch (material)
                {
                    case "cobblestone" -> "stone";
                    case "brick" -> "bricks";
                    default -> material;
                };
        }
        return null;
    }

    private static String coloredGroup(String path)
    {
        String family = withoutColor(path);
        return COLORED_FORMS.contains(family) ? "colored:" + family : null;
    }

    private static String naturalGroup(ItemStack stack, String path)
    {
        if (tag(stack, "ores") || path.endsWith("_ore") || path.equals("ancient_debris") || path.startsWith("raw_") && path.endsWith("_block"))
            return "type:ores";
        if (tag(stack, "saplings")) return "type:saplings";
        if (tag(stack, "leaves")) return "type:leaves";
        if (tag(stack, "logs")) return path.startsWith("stripped_") ? "type:stripped_logs" : "type:logs";
        if (tag(stack, "flowers")) return "type:flowers";
        if (tag(stack, "coral_blocks") || hasPart(path, "coral_block")) return "type:coral_blocks";
        if (tag(stack, "corals") || hasPart(path, "coral") || hasPart(path, "coral_fan")) return "type:corals";
        if (tag(stack, "mushrooms") || path.endsWith("_fungus") || path.endsWith("_mushroom_block")) return "type:mushrooms";
        return materialGroup(path);
    }

    private static String functionalGroup(ItemStack stack, String path)
    {
        String colored = coloredGroup(path);
        if (colored != null) return colored;
        if (tag(stack, "wooden_shelves")) return "type:shelves";
        if (tag(stack, "ceiling_hanging_signs") || path.endsWith("_hanging_sign")) return "type:hanging_signs";
        if (tag(stack, "standing_signs") || path.endsWith("_sign")) return "type:signs";
        if (path.startsWith("infested_")) return "type:infested_blocks";
        if (hasPart(path, "copper_bulb")) return "type:copper_bulbs";
        if (hasPart(path, "golem_statue")) return "type:golem_statues";
        if (hasPart(path, "lightning_rod")) return "type:lightning_rods";
        if (tag(stack, "anvil")) return "type:anvils";
        if (tag(stack, "skulls")) return "type:skulls";
        if (path.equals("chest") || path.endsWith("_chest")) return "type:chests";
        if (path.equals("chain") || path.endsWith("_chain")) return "type:chains";
        if (path.equals("lantern") || path.endsWith("_lantern") && !path.equals("sea_lantern") && !path.equals("jack_o_lantern"))
            return "type:lanterns";
        if (path.endsWith("_bars")) return "type:bars";
        return null;
    }

    private static String redstoneGroup(ItemStack stack, String path)
    {
        if (tag(stack, "buttons")) return "type:buttons";
        if (tag(stack, "pressure_plates")) return "type:pressure_plates";
        if (tag(stack, "trapdoors")) return "type:trapdoors";
        if (tag(stack, "doors")) return "type:doors";
        if (tag(stack, "fence_gates")) return "type:fence_gates";
        if (tag(stack, "rails")) return "type:rails";
        return functionalGroup(stack, path);
    }

    private static String searchGroup(ItemStack stack, String path)
    {
        String group = coloredGroup(path);
        if (group != null) return group;
        group = functionalGroup(stack, path);
        if (group != null) return group;
        group = redstoneGroup(stack, path);
        if (group != null) return group;
        return naturalGroup(stack, path);
    }

    private static boolean tag(ItemStack stack, String name)
    {
        return stack.getItem() instanceof BlockItem item && item.getBlock().builtInRegistryHolder()
                .is(TagKey.create(Registries.BLOCK, identifier(name)));
    }

    private static String withoutColor(String path)
    {
        for (String color : COLORS) if (path.startsWith(color + "_")) return path.substring(color.length() + 1);
        return path;
    }

    private static boolean hasPart(String path, String part)
    {
        return path.equals(part) || path.startsWith(part + "_") || path.endsWith("_" + part) || path.contains("_" + part + "_");
    }
}
//#endif

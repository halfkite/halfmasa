package io.github.halfmasa.xaerobinding.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import io.github.halfmasa.xaerobinding.config.Configs;

public final class CondensedCreativeManager
{
    private static final Map<CreativeModeInventoryScreen, State> STATES = new WeakHashMap<>();
    private static final Set<String> COLORS = Set.of(
            "white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
            "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink");
    private static final Set<String> WOODS = Set.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
            "bamboo", "crimson", "warped");
    private static final Set<String> MATERIALS = Set.of(
            "stone", "granite", "diorite", "andesite", "tuff", "mud", "sandstone",
            "red_sandstone", "prismarine", "deepslate", "nether_brick", "blackstone",
            "end_stone", "quartz", "copper");

    private CondensedCreativeManager() {}

    public static boolean isApplied(CreativeModeInventoryScreen screen)
    {
        return STATES.containsKey(screen);
    }

    public static void rebuild(CreativeModeInventoryScreen screen, CreativeModeTab tab, NonNullList<ItemStack> items)
    {
        if (!Configs.CONDENSED_CREATIVE.getBooleanValue())
        {
            STATES.remove(screen);
            return;
        }

        State oldState = STATES.get(screen);
        Set<String> expanded = oldState != null ? new HashSet<>(oldState.expanded) : new HashSet<>();
        State state = new State(copy(items), expanded, tabId(tab));
        STATES.put(screen, state);
        apply(state, items);
    }

    public static boolean ensureApplied(CreativeModeInventoryScreen screen, CreativeModeTab tab, NonNullList<ItemStack> items)
    {
        State state = STATES.get(screen);
        if (Configs.CONDENSED_CREATIVE.getBooleanValue())
        {
            if (state == null || !state.tabId.equals(tabId(tab)))
            {
                rebuild(screen, tab, items);
                return true;
            }
            return false;
        }

        if (state != null)
        {
            STATES.remove(screen);
            replace(items, state.source);
            return true;
        }
        return false;
    }

    public static boolean toggle(CreativeModeInventoryScreen screen, ItemStack stack, NonNullList<ItemStack> items)
    {
        State state = STATES.get(screen);
        if (state == null) return false;

        for (Group group : state.groups.values())
        {
            if (ItemStack.isSameItemSameComponents(group.parent, stack))
            {
                if (!state.expanded.add(group.key)) state.expanded.remove(group.key);
                apply(state, items);
                return true;
            }
        }
        return false;
    }

    public static boolean rotatePreview(CreativeModeInventoryScreen screen, NonNullList<ItemStack> items)
    {
        if (!Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.getBooleanValue()) return false;
        State state = STATES.get(screen);
        if (state == null || state.groups.isEmpty()) return false;

        long step = System.currentTimeMillis() / 1000L;
        if (state.lastPreviewStep == step) return false;
        state.lastPreviewStep = step;
        apply(state, items);
        return true;
    }

    public static boolean isParent(CreativeModeInventoryScreen screen, ItemStack stack)
    {
        State state = STATES.get(screen);
        return state != null && state.groups.values().stream()
                .anyMatch(group -> ItemStack.isSameItemSameComponents(group.parent, stack));
    }

    public static Component getParentHint(CreativeModeInventoryScreen screen, ItemStack stack)
    {
        State state = STATES.get(screen);
        if (state == null) return null;
        for (Group group : state.groups.values())
        {
            if (ItemStack.isSameItemSameComponents(group.parent, stack))
            {
                String key = state.expanded.contains(group.key)
                        ? "halfmasa.feature.condensed_creative.collapse"
                        : "halfmasa.feature.condensed_creative.expand";
                return Component.translatable(key, group.children.size());
            }
        }
        return null;
    }

    public static Visual getVisual(CreativeModeInventoryScreen screen, ItemStack stack)
    {
        State state = STATES.get(screen);
        if (state == null) return null;
        for (Group group : state.groups.values())
        {
            boolean expanded = state.expanded.contains(group.key);
            if (ItemStack.isSameItemSameComponents(group.parent, stack))
                return new Visual(group.key, true, expanded);
            if (expanded && group.children.stream().anyMatch(child -> ItemStack.isSameItemSameComponents(child, stack)))
                return new Visual(group.key, false, true);
        }
        return null;
    }

    private static void apply(State state, NonNullList<ItemStack> target)
    {
        state.groups.clear();
        target.clear();
        Set<String> inserted = new HashSet<>();
        Map<String, List<ItemStack>> grouped = collectGroups(state.source, state.tabId);

        for (ItemStack stack : state.source)
        {
            String key = groupKey(stack, state.tabId);
            List<ItemStack> children = key != null ? grouped.get(key) : null;
            if (key == null || children == null || children.size() < 2)
            {
                target.add(stack.copy());
                continue;
            }
            if (!inserted.add(key)) continue;

            int previewIndex = Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.getBooleanValue()
                    ? (int) ((System.currentTimeMillis() / 1000L) % children.size())
                    : 0;
            ItemStack parent = children.get(previewIndex).copy();
            Component title = groupTitle(key, children).copy().append(" (" + children.size() + ")");
            parent.set(DataComponents.CUSTOM_NAME, title);
            Group group = new Group(key, children, parent);
            state.groups.put(key, group);
            target.add(parent);
            if (state.expanded.contains(key)) children.forEach(child -> target.add(child.copy()));
        }
    }

    private static Map<String, List<ItemStack>> collectGroups(List<ItemStack> source, String tabId)
    {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : source)
        {
            String key = groupKey(stack, tabId);
            if (key != null) groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(stack.copy());
        }
        return groups;
    }

    private static String groupKey(ItemStack stack, String tabId)
    {
        //#if MC >= 1.21.11
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        //#else
        //$$ ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        //#endif
        if (!"minecraft".equals(id.getNamespace())) return null;
        String path = id.getPath();

        return switch (tabId)
        {
            case "building_blocks" -> buildingGroup(path);
            case "colored_blocks" -> coloredGroup(path);
            case "natural_blocks" -> naturalGroup(path);
            case "functional_blocks" -> functionalGroup(stack, path);
            case "redstone_blocks" -> redstoneGroup(path);
            case "spawn_eggs" -> spawnEggGroup(stack);
            case "tools_and_utilities" -> toolsGroup(stack, path);
            case "combat" -> combatGroup(stack);
            case "food_and_drinks" -> foodGroup(stack);
            case "ingredients" -> ingredientGroup(stack, path);
            case "search" -> searchGroup(stack, path);
            default -> null;
        };
    }

    private static Component groupTitle(String key, List<ItemStack> children)
    {
        if (key.startsWith("wood:") || key.startsWith("material:")) return children.getFirst().getHoverName().copy();
        return Component.translatable("halfmasa.feature.condensed_creative.group." + key.substring(key.indexOf(':') + 1));
    }

    private static String buildingGroup(String path)
    {
        String wood = matchingWood(path);
        if (wood != null) return "wood:" + wood;
        String material = matchingMaterial(path);
        return material != null ? "material:" + material : null;
    }

    private static String coloredGroup(String path)
    {
        String family = stripPrefix(path, COLORS);
        return family != null && isColoredFamily(family) ? "colored:" + family : null;
    }

    private static String naturalGroup(String path)
    {
        if (path.endsWith("_ore")) return "type:ores";
        if (path.endsWith("_sapling")) return "type:saplings";
        if (path.endsWith("_leaves")) return "type:leaves";
        if ((path.endsWith("_log") || path.endsWith("_stem")) && !path.startsWith("stripped_")) return "type:logs";
        if (path.contains("coral")) return "type:corals";
        if (isFlower(path)) return "type:flowers";
        return null;
    }

    private static String functionalGroup(ItemStack stack, String path)
    {
        String colored = coloredGroup(path);
        if (colored != null && (colored.endsWith("candle") || colored.endsWith("bed") ||
                colored.endsWith("banner") || colored.endsWith("shulker_box"))) return colored;
        if (path.endsWith("_sign") || path.endsWith("_hanging_sign")) return "type:signs";
        if (path.startsWith("infested_")) return "type:infested_blocks";
        if (path.contains("copper_bulb")) return "type:copper_bulbs";
        if (stack.is(Items.PAINTING)) return "variants:paintings";
        return null;
    }

    private static String redstoneGroup(String path)
    {
        if (path.endsWith("_button")) return "type:buttons";
        if (path.endsWith("_pressure_plate")) return "type:pressure_plates";
        if (path.endsWith("_trapdoor")) return "type:trapdoors";
        if (path.endsWith("_door")) return "type:doors";
        if (path.endsWith("_fence_gate")) return "type:fence_gates";
        return null;
    }

    private static String spawnEggGroup(ItemStack stack)
    {
        if (!(stack.getItem() instanceof SpawnEggItem egg)) return null;
        //#if MC >= 1.21.10
        MobCategory category = egg.getType(stack).getCategory();
        //#elseif MC >= 1.21.4
        //$$ Minecraft client = Minecraft.getInstance();
        //$$ if (client.level == null) return null;
        //$$ MobCategory category = egg.getType(client.level.registryAccess(), stack).getCategory();
        //#else
        //$$ MobCategory category = egg.getType(stack).getCategory();
        //#endif
        if (category == MobCategory.MONSTER) return "type:monster_spawn_eggs";
        if (category == MobCategory.MISC) return "type:misc_spawn_eggs";
        return "type:creature_spawn_eggs";
    }

    private static String toolsGroup(ItemStack stack, String path)
    {
        if (path.startsWith("music_disc_")) return "type:music_discs";
        if (path.endsWith("_boat") || path.endsWith("_raft") || path.endsWith("_chest_boat") || path.endsWith("_chest_raft"))
            return "type:boats";
        if (stack.is(Items.GOAT_HORN)) return "variants:goat_horns";
        return null;
    }

    private static String combatGroup(ItemStack stack)
    {
        return Configs.CONDENSED_CREATIVE_TIPPED_ARROWS.getBooleanValue() && stack.is(Items.TIPPED_ARROW)
                ? "variants:tipped_arrows" : null;
    }

    private static String foodGroup(ItemStack stack)
    {
        if (Configs.CONDENSED_CREATIVE_POTIONS.getBooleanValue())
        {
            if (stack.is(Items.POTION)) return "variants:potions";
            if (stack.is(Items.SPLASH_POTION)) return "variants:splash_potions";
            if (stack.is(Items.LINGERING_POTION)) return "variants:lingering_potions";
        }
        return stack.is(Items.SUSPICIOUS_STEW) ? "variants:suspicious_stews" : null;
    }

    private static String ingredientGroup(ItemStack stack, String path)
    {
        if (Configs.CONDENSED_CREATIVE_ENCHANTED_BOOKS.getBooleanValue() && stack.is(Items.ENCHANTED_BOOK))
            return "variants:enchanted_books";
        if (path.endsWith("_dye")) return "type:dyes";
        if (path.endsWith("_pottery_sherd")) return "type:pottery_sherds";
        if (path.endsWith("_smithing_template")) return "type:smithing_templates";
        return null;
    }

    private static String searchGroup(ItemStack stack, String path)
    {
        String result = ingredientGroup(stack, path);
        if (result != null) return result;
        result = foodGroup(stack);
        if (result != null) return result;
        result = combatGroup(stack);
        if (result != null) return result;
        result = functionalGroup(stack, path);
        if (result != null) return result;
        result = coloredGroup(path);
        if (result != null) return result;
        result = redstoneGroup(path);
        if (result != null) return result;
        result = naturalGroup(path);
        if (result != null) return result;
        result = toolsGroup(stack, path);
        if (result != null) return result;
        result = spawnEggGroup(stack);
        return result != null ? result : buildingGroup(path);
    }

    private static String stripPrefix(String path, Set<String> prefixes)
    {
        String prefix = matchingPrefix(path, prefixes);
        return prefix != null ? path.substring(prefix.length() + 1) : null;
    }

    private static String matchingPrefix(String path, Set<String> prefixes)
    {
        return prefixes.stream()
                .filter(prefix -> path.startsWith(prefix + "_"))
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .findFirst().orElse(null);
    }

    private static String matchingMaterial(String path)
    {
        return MATERIALS.stream()
                .filter(material -> path.equals(material) || path.startsWith(material + "_") ||
                        path.endsWith("_" + material) || path.contains("_" + material + "_"))
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .findFirst().orElse(null);
    }

    private static String matchingWood(String path)
    {
        String normalized = path.startsWith("stripped_") ? path.substring("stripped_".length()) : path;
        String wood = matchingPrefix(normalized, WOODS);
        return wood != null && isWoodFamily(normalized.substring(wood.length() + 1)) ? wood : null;
    }

    private static boolean isFlower(String path)
    {
        return Set.of("dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip",
                "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower",
                "lily_of_the_valley", "torchflower", "sunflower", "lilac", "rose_bush", "peony").contains(path);
    }

    private static String tabId(CreativeModeTab tab)
    {
        //#if MC >= 1.21.11
        Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        //#else
        //$$ ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        //#endif
        return id != null ? id.getPath() : "unknown";
    }

    private static boolean isColoredFamily(String suffix)
    {
        return suffix.equals("wool") || suffix.equals("carpet") || suffix.equals("bed") ||
                suffix.equals("banner") || suffix.equals("candle") || suffix.equals("stained_glass") ||
                suffix.equals("stained_glass_pane") || suffix.equals("terracotta") ||
                suffix.equals("glazed_terracotta") || suffix.equals("concrete") ||
                suffix.equals("concrete_powder") || suffix.equals("shulker_box");
    }

    private static boolean isWoodFamily(String suffix)
    {
        return suffix.equals("log") || suffix.equals("wood") || suffix.equals("stem") || suffix.equals("hyphae") ||
                suffix.equals("planks") || suffix.equals("stairs") || suffix.equals("slab") ||
                suffix.equals("fence") || suffix.equals("fence_gate") || suffix.equals("door") ||
                suffix.equals("trapdoor") || suffix.equals("pressure_plate") || suffix.equals("button") ||
                suffix.equals("sign") || suffix.equals("hanging_sign") || suffix.equals("boat") ||
                suffix.equals("chest_boat") || suffix.equals("raft") || suffix.equals("chest_raft");
    }

    private static List<ItemStack> copy(List<ItemStack> stacks)
    {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private static void replace(NonNullList<ItemStack> target, List<ItemStack> source)
    {
        target.clear();
        source.forEach(stack -> target.add(stack.copy()));
    }

    private record Group(String key, List<ItemStack> children, ItemStack parent) {}

    public record Visual(String key, boolean parent, boolean expanded) {}

    private static final class State
    {
        private final List<ItemStack> source;
        private final Set<String> expanded;
        private final String tabId;
        private final Map<String, Group> groups = new LinkedHashMap<>();
        private long lastPreviewStep = Long.MIN_VALUE;

        private State(List<ItemStack> source, Set<String> expanded, String tabId)
        {
            this.source = source;
            this.expanded = expanded;
            this.tabId = tabId;
        }
    }
}

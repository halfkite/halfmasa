package io.github.halfmasa.xaerobinding.feature.minihud;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
//#if MC > 1.21.10
import fi.dy.masa.malilib.util.nbt.NbtInventory;
//#endif
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Render-thread-only data snapshots. Drawing, layout and network requests remain upstream. */
public final class PreviewCache implements IClientTickHandler
{
    private static final PreviewCache INSTANCE = new PreviewCache();
    // Hovering between a few slots should stay warm without retaining an inventory of previews.
    private static final int CAPACITY = 4;
    private static final List<Items> ITEMS = new ArrayList<>();
    private static final List<Inventory> INVENTORIES = new ArrayList<>();
    private static final List<DataValue<?>> DATA = new ArrayList<>();
    private static final List<Serialized> SERIALIZED = new ArrayList<>();
    private static final List<BlockData> BLOCK_DATA = new ArrayList<>();
    private static final List<BlockTag> BLOCK_TAGS = new ArrayList<>();
    private static final List<WorldData> WORLD_DATA = new ArrayList<>();
    private static final Map<ItemStack, DrawItem> DRAW_ITEMS = new WeakHashMap<>();
    private static int depth;
    private static int worldDepth;
    private static ItemStack drawingStack;
    private static Object world;
    private static long tick;
    private static volatile long revision;

    private record Items(Object component, int slots, boolean bundle, NonNullList<ItemStack> value) {}
    private record Inventory(NonNullList<ItemStack> source, Container value) {}
    private record DataValue<T>(Object kind, Object registry, BaseData snapshot, T value) {}
    private record Serialized(Object registry, List<ItemStack> snapshot, ListData value) {}
    private record BlockData(Object component, CompoundTag snapshot, CompoundData value) {}
    private record BlockTag(Object component, CompoundTag snapshot, CompoundTag value) {}
    private record WorldInventory(int slots) {}
    private record DrawItem(ItemStack display, long tick, long revision) {}
    private static final class WorldData
    {
        final Object registry;
        CompoundData source;
        CompoundData snapshot;
        long tick;
        long revision;
        final Map<Integer, Container> inventories = new HashMap<>();
        WorldData(Object registry, CompoundData source)
        {
            this.registry = registry;
            this.source = source;
            this.snapshot = source.copy();
        }
    }

    private PreviewCache() {}
    public static PreviewCache getInstance() { return INSTANCE; }
    public static long tick() { return tick; }
    public static long revision() { return revision; }
    public static void receivedData() { revision++; }
    public static boolean active() { return depth > 0 && Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.getBooleanValue(); }
    public static void enter() { depth++; }
    public static void exit() { depth--; }
    public static boolean worldActive() { return worldDepth > 0 && active(); }
    public static void enterWorld() { enter(); worldDepth++; }
    public static void exitWorld() { worldDepth--; exit(); }
    public static boolean isDrawingStack(ItemStack stack) { return drawingStack == stack; }
    public static ItemStack swapDrawingStack(ItemStack stack)
    {
        ItemStack previous = drawingStack;
        drawingStack = stack;
        return previous;
    }

    public static void clear()
    {
        ITEMS.clear();
        INVENTORIES.clear();
        DATA.clear();
        SERIALIZED.clear();
        BLOCK_DATA.clear();
        BLOCK_TAGS.clear();
        WORLD_DATA.clear();
        DRAW_ITEMS.clear();
        //#if MC > 1.21.10
        HeldWorldPreview.clear();
        //#else
        //$$ LegacyHeldWorldPreview.clear();
        //$$ LegacyNbtCache.clear();
        //#endif
        revision++;
    }

    @Override public void onClientTick(Minecraft client)
    {
        tick++;
        if (world != client.level || !Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.getBooleanValue())
        {
            clear();
            world = client.level;
        }
    }

    private static <T> T remember(List<T> cache, T entry)
    {
        if (cache.size() == CAPACITY) cache.removeFirst();
        cache.add(entry);
        return entry;
    }

    public static NonNullList<ItemStack> items(ItemStack stack, int slots, boolean bundle,
                                               Supplier<NonNullList<ItemStack>> original)
    {
        if (!active()) return original.get();
        Object component = bundle ? stack.get(DataComponents.BUNDLE_CONTENTS) : stack.get(DataComponents.CONTAINER);
        if (component == null) return original.get();
        for (Items entry : ITEMS)
            if (entry.component == component && entry.slots == slots && entry.bundle == bundle) return entry.value;
        // Use the upstream complete result, including empty slots and its version-specific size rules.
        return remember(ITEMS, new Items(component, slots, bundle, original.get())).value;
    }

    public static Container inventory(NonNullList<ItemStack> items, Supplier<Container> original)
    {
        if (!active()) return original.get();
        for (Inventory entry : INVENTORIES) if (entry.source == items) return entry.value;
        return remember(INVENTORIES, new Inventory(items, original.get())).value;
    }

    public static CompoundData blockData(ItemStack stack, Supplier<CompoundData> original)
    {
        if (!active()) return original.get();
        var component = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = component == null ? null : component.getUnsafe();
        for (BlockData entry : BLOCK_DATA)
            if (entry.component == component && java.util.Objects.equals(entry.snapshot, tag)) return entry.value;
        return remember(BLOCK_DATA, new BlockData(component, tag == null ? null : tag.copy(), original.get())).value;
    }

    public static CompoundTag blockTag(ItemStack stack, Supplier<CompoundTag> original)
    {
        if (!active()) return original.get();
        var component = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = component == null ? null : component.getUnsafe();
        for (BlockTag entry : BLOCK_TAGS)
            if (entry.component == component && java.util.Objects.equals(entry.snapshot, tag)) return entry.value;
        return remember(BLOCK_TAGS, new BlockTag(component, tag == null ? null : tag.copy(), original.get())).value;
    }

    /** Compare mutable data structurally, including added/removed compound keys and empty compounds. */
    public static boolean sameData(BaseData a, BaseData b)
    {
        if (a == b) return true;
        if (a == null || b == null || a.getType() != b.getType()) return false;
        if (a instanceof CompoundData ac && b instanceof CompoundData bc)
        {
            if (!ac.getKeys().equals(bc.getKeys())) return false;
            for (String key : ac.getKeys())
                if (!sameData(ac.getData(key).orElse(null), bc.getData(key).orElse(null))) return false;
            return true;
        }
        if (a instanceof ListData al && b instanceof ListData bl)
        {
            if (al.size() != bl.size()) return false;
            for (int i = 0; i < al.size(); i++) if (!sameData(al.get(i), bl.get(i))) return false;
            return true;
        }
        return a.equals(b);
    }

    @SuppressWarnings("unchecked")
    public static <T> T data(Object kind, Object registry, BaseData source, Supplier<T> original)
    {
        if (!active()) return original.get();
        for (DataValue<?> entry : DATA)
            if (entry.kind.equals(kind) && entry.registry == registry && sameData(entry.snapshot, source)) return (T) entry.value;
        T value = original.get();
        remember(DATA, new DataValue<>(kind, registry, source.copy(), value));
        return value;
    }

    /** World previews decode the same mutable server snapshot through several upstream callers. */
    public static Container worldInventory(CompoundData source, int slots, RegistryAccess registry,
                                            Supplier<Container> original)
    {
        if (!worldActive()) return data(new WorldInventory(slots), registry, source, original);
        long currentRevision = revision;
        WorldData found = null;
        for (WorldData entry : WORLD_DATA)
        {
            if (entry.registry == registry && entry.source == source)
            {
                found = entry;
                if ((entry.tick != tick || entry.revision != currentRevision) && !sameData(entry.snapshot, source))
                {
                    entry.snapshot = source.copy();
                    entry.inventories.clear();
                }
                break;
            }
        }
        if (found == null)
        {
            for (WorldData entry : WORLD_DATA)
                if (entry.registry == registry && sameData(entry.snapshot, source)) { found = entry; break; }
        }
        if (found == null) found = remember(WORLD_DATA, new WorldData(registry, source));
        found.source = source;
        found.tick = tick;
        found.revision = currentRevision;
        // One content comparison per source/update, shared by different slot-count requests.
        if (!found.inventories.containsKey(slots)) found.inventories.put(slots, original.get());
        return found.inventories.get(slots);
    }

    /** Renderer-owned copies persist across frames; live inventory stacks are never passed to drawing. */
    public static ItemStack drawCopy(ItemStack source, Supplier<ItemStack> original)
    {
        if (!worldActive() || source.isEmpty()) return original.get();
        DrawItem entry = DRAW_ITEMS.get(source);
        long currentRevision = revision;
        if (entry != null && entry.display.getItem() == source.getItem() && entry.display.getCount() == source.getCount())
        {
            if (entry.tick == tick && entry.revision == currentRevision) return entry.display;
            if (ItemStack.matches(entry.display, source))
            {
                DRAW_ITEMS.put(source, new DrawItem(entry.display, tick, currentRevision));
                return entry.display;
            }
        }
        ItemStack display = original.get();
        DRAW_ITEMS.put(source, new DrawItem(display, tick, currentRevision));
        return display;
    }

    /** Ender previews often wrap the same live inventory in a new NbtInventory on every frame. */
    //#if MC > 1.21.10
    public static ListData serialize(NbtInventory inventory, Object registry, Supplier<ListData> original)
    {
        if (!active()) return original.get();
        var current = inventory.toVanillaList(Math.max(0, inventory.size()));
        for (Serialized entry : SERIALIZED)
        {
            if (entry.registry != registry || entry.snapshot.size() != current.size()) continue;
            boolean same = true;
            for (int i = 0; i < current.size(); i++)
                if (!ItemStack.matches(entry.snapshot.get(i), current.get(i))) { same = false; break; }
            if (same) return entry.value;
        }
        ListData value = original.get();
        var snapshot = current.stream().map(ItemStack::copy).toList();
        return remember(SERIALIZED, new Serialized(registry, snapshot, value)).value;
    }
    //#endif
}
//#endif

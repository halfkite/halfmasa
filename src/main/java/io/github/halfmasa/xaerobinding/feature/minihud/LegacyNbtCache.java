package io.github.halfmasa.xaerobinding.feature.minihud;

//#if MC >= 1.21.1 && MC < 1.21.11
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
//#if MC > 1.21.5
import fi.dy.masa.malilib.util.nbt.NbtInventory;
//#endif
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

/** Bounded snapshots for MiniHUD versions that still use vanilla NBT. */
public final class LegacyNbtCache
{
    private static final int CAPACITY = 4;
    private static final List<Value<?>> VALUES = new ArrayList<>();
    private static final List<Serialized> SERIALIZED = new ArrayList<>();
    private record Value<T>(Object kind, Object registry, CompoundTag snapshot, T value) {}
    private record Serialized(Object registry, List<ItemStack> snapshot, ListTag value) {}
    private LegacyNbtCache() {}
    public static void clear() { VALUES.clear(); SERIALIZED.clear(); }

    @SuppressWarnings("unchecked")
    public static <T> T data(Object kind, Object registry, CompoundTag source, Supplier<T> original)
    {
        if (!PreviewCache.active()) return original.get();
        for (Value<?> entry : VALUES)
            if (Objects.equals(entry.kind, kind) && entry.registry == registry && Objects.equals(entry.snapshot, source))
                return (T) entry.value;
        T value = original.get();
        if (VALUES.size() == CAPACITY) VALUES.removeFirst();
        VALUES.add(new Value<>(kind, registry, source == null ? null : source.copy(), value));
        return value;
    }

    //#if MC > 1.21.5
    public static ListTag serialize(NbtInventory inventory, Object registry, Supplier<ListTag> original)
    {
        if (!PreviewCache.active()) return original.get();
        var current = inventory.toVanillaList(Math.max(0, inventory.size()));
        for (Serialized entry : SERIALIZED)
        {
            if (entry.registry != registry || entry.snapshot.size() != current.size()) continue;
            boolean same = true;
            for (int i = 0; i < current.size(); i++)
                if (!ItemStack.matches(entry.snapshot.get(i), current.get(i))) { same = false; break; }
            if (same) return entry.value;
        }
        ListTag value = original.get();
        if (SERIALIZED.size() == CAPACITY) SERIALIZED.removeFirst();
        SERIALIZED.add(new Serialized(registry, current.stream().map(ItemStack::copy).toList(), value));
        return value;
    }
    //#endif
}
//#endif

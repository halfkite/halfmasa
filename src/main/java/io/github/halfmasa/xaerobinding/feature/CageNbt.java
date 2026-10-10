package io.github.halfmasa.xaerobinding.feature;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Preserve typed/default reads across the NBT API change in 1.21.5. */
public final class CageNbt
{
    private CageNbt() {}
    public static CompoundTag compound(CompoundTag tag, String key)
    {
        //#if MC >= 1.21.5
        return tag.getCompoundOrEmpty(key);
        //#else
        //$$ return tag.getCompound(key);
        //#endif
    }
    public static CompoundTag compound(ListTag tag, int index)
    {
        //#if MC >= 1.21.5
        return tag.getCompoundOrEmpty(index);
        //#else
        //$$ return tag.getCompound(index);
        //#endif
    }
    public static ListTag list(CompoundTag tag, String key)
    {
        //#if MC >= 1.21.5
        return tag.getListOrEmpty(key);
        //#else
        //$$ return tag.getList(key, Tag.TAG_COMPOUND);
        //#endif
    }
    public static String string(CompoundTag tag, String key, String fallback)
    {
        //#if MC >= 1.21.5
        return tag.getStringOr(key, fallback);
        //#else
        //$$ return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
        //#endif
    }
    public static long longValue(CompoundTag tag, String key, long fallback)
    {
        //#if MC >= 1.21.5
        return tag.getLongOr(key, fallback);
        //#else
        //$$ return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback;
        //#endif
    }
    public static int integer(CompoundTag tag, String key, int fallback)
    {
        //#if MC >= 1.21.5
        return tag.getIntOr(key, fallback);
        //#else
        //$$ return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
        //#endif
    }
    public static boolean bool(CompoundTag tag, String key, boolean fallback)
    {
        //#if MC >= 1.21.5
        return tag.getBooleanOr(key, fallback);
        //#else
        //$$ return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
        //#endif
    }
}

//#if MC >= 26.3
package io.github.halfmasa.xaerobinding.feature;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.SchematicBlockFilterMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

public final class LitematicaBlockFilters
{
    private static final Cache SAVE = new Cache();
    private static final Cache DELETE = new Cache();
    private static final Cache PASTE = new Cache();

    private LitematicaBlockFilters() {}

    public static boolean allowsSave(BlockState state)
    {
        if (!Configs.LITEMATICA_SAVE_FILTER.getBooleanValue()) return true;
        boolean whitelist = Configs.LITEMATICA_SAVE_FILTER_MODE.getOptionListValue() == SchematicBlockFilterMode.WHITELIST;
        return SAVE.allows(state, whitelist, (whitelist ? Configs.LITEMATICA_SAVE_WHITELIST : Configs.LITEMATICA_SAVE_BLACKLIST).getStrings());
    }

    public static boolean allowsDelete(BlockState state)
    {
        if (!Configs.LITEMATICA_DELETE_FILTER.getBooleanValue()) return true;
        boolean whitelist = Configs.LITEMATICA_DELETE_FILTER_MODE.getOptionListValue() == SchematicBlockFilterMode.WHITELIST;
        return DELETE.allows(state, whitelist, (whitelist ? Configs.LITEMATICA_DELETE_WHITELIST : Configs.LITEMATICA_DELETE_BLACKLIST).getStrings());
    }

    public static boolean isFilteredAreaDelete(Object task)
    {
        return Configs.LITEMATICA_DELETE_FILTER.getBooleanValue()
                && task.getClass().getName().equals("fi.dy.masa.litematica.scheduler.tasks.TaskDeleteArea");
    }

    public static boolean allowsPaste(BlockState state)
    {
        if (!Configs.LITEMATICA_PASTE_FILTER.getBooleanValue()) return true;
        boolean whitelist = Configs.LITEMATICA_PASTE_FILTER_MODE.getOptionListValue() == SchematicBlockFilterMode.WHITELIST;
        return PASTE.allows(state, whitelist, (whitelist ? Configs.LITEMATICA_PASTE_WHITELIST : Configs.LITEMATICA_PASTE_BLACKLIST).getStrings());
    }

    private static final class Cache
    {
        private List<String> entries;
        private boolean whitelist;
        private BlockIdFilter filter;
        private final Map<Block, Boolean> decisions = new IdentityHashMap<>();

        synchronized boolean allows(BlockState state, boolean whitelist, List<String> entries)
        {
            if (filter == null || this.whitelist != whitelist || !entries.equals(this.entries))
            {
                // MaLiLib exposes immutable string lists; retain the reference for cheap unchanged checks.
                this.entries = entries;
                this.whitelist = whitelist;
                this.filter = new BlockIdFilter(entries, whitelist);
                decisions.clear();
            }
            Block block = state.getBlock();
            Boolean allowed = decisions.get(block);
            if (allowed == null)
            {
                allowed = filter.allows(BuiltInRegistries.BLOCK.getKey(block).toString());
                decisions.put(block, allowed);
            }
            return allowed;
        }
    }
}
//#endif

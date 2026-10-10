//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exact registry IDs only; an empty whitelist permits no blocks. */
public final class BlockIdFilter
{
    private final Set<String> ids;
    private final boolean whitelist;

    public BlockIdFilter(List<String> entries, boolean whitelist)
    {
        this.whitelist = whitelist;
        Set<String> parsed = new HashSet<>();
        for (String entry : entries)
        {
            String id = entry.trim();
            if (!id.contains(":")) id = "minecraft:" + id;
            if (id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) parsed.add(id);
        }
        this.ids = Set.copyOf(parsed);
    }

    public boolean allows(String blockId)
    {
        return ids.contains(blockId) == whitelist;
    }
}
//#endif

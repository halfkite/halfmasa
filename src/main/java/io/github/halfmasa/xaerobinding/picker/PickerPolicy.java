//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.picker;

import java.util.*;
import fi.dy.masa.malilib.config.IConfigStringList;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.fabricmc.loader.api.FabricLoader;
import static io.github.halfmasa.xaerobinding.picker.RegistryIndex.Kind.*;

/** Explicit schema, never infer from arbitrary user text or an empty list. */
public final class PickerPolicy {
    private PickerPolicy() {}

    private static final boolean OTHER_PICKER = FabricLoader.getInstance().getAllMods().stream()
            .map(mod -> mod.getMetadata().getId().replace("-", "").replace("_", ""))
            .anyMatch("helpfultweake"::equals);

    public static boolean enabled()
    {
        return Configs.BETTER_REGISTRY_PICKER.getBooleanValue() && !OTHER_PICKER;
    }
    // Register only known ID-list configs. Extend this table after checking the upstream list grammar.
    public static Set<RegistryIndex.Kind> kinds(String modId, IConfigStringList config) {
        String n=config.getName().toLowerCase(Locale.ROOT);
        if (modId.equals("halfmasa")) {
            if (Set.of("litematicasavewhitelist", "litematicasaveblacklist", "litematicadeletewhitelist",
                    "litematicadeleteblacklist", "litematicapastewhitelist", "litematicapasteblacklist",
                    "blockplacementpreviewwhitelist", "blockplacementpreviewblacklist", "blockrenderblacklist", "blockrenderwhitelist").contains(n)) return Set.of(BLOCK);
            if(Set.of("entityrenderdisableblacklist","entityrenderdisablewhitelist","entityaggregationwhitelist","entityaggregationblacklist","rangefeedingwhitelist","rangefeedingblacklist","entityentries").contains(n)) return Set.of(ENTITY);
            if(Set.of("itemaggregationwhitelist","itemaggregationblacklist","autoeatwhitelist","autoeatblacklist").contains(n)) return Set.of(ITEM);
        }
        // Tweakeroo 26.3 Configs.Lists: verified registered names.
        if(modId.equals("tweakeroo")) {
            if(Set.of("blocktypebreakrestrictionblacklist","blocktypebreakrestrictionwhitelist","fastrightclickblockblacklist","fastrightclickblockwhitelist").contains(n)) return Set.of(BLOCK);
            if(Set.of("entitytypeattackrestrictionblacklist","entitytypeattackrestrictionwhitelist").contains(n)) return Set.of(ENTITY);
            if(Set.of("fastplacementitemblacklist","fastplacementitemwhitelist","fastrightclickblacklist","fastrightclickwhitelist","handrestockblacklist","handrestockwhitelist","unstackingitems").contains(n)) return Set.of(ITEM);
        }
        return Set.of();
    }
    public static Map<String,String> registryCandidates(Collection<String> values) {
        if(values.isEmpty()) return Map.of();
        var result=new LinkedHashMap<String,String>();
        // Explicit namespace and valid registry ID required; do not replace non-registry selectors.
        for(String s:values) {
            if(s==null || !s.contains(":") || RegistryIndex.lookup(s).isEmpty()) return Map.of();
            var id=net.minecraft.resources.Identifier.tryParse(s);
            if(id==null || !id.toString().equals(s)) return Map.of();
            result.put(id.toString(),s);
        }
        return result;
    }
}
//#endif

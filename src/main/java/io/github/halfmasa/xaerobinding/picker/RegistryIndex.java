//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.picker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/** Client-thread cache. No registry traversal while rendering the config list. */
public final class RegistryIndex {
    public enum Kind { ITEM, BLOCK, ENTITY, FLUID }
    public record Entry(Kind kind, String id, String name, String searchName, Item iconItem)
    {
        public ItemStack icon() { return previewStack(iconItem); }
    }
    private record Preview(ItemStack stack, boolean componentsBound) {}
    private static final Map<Item, Preview> PREVIEWS = new IdentityHashMap<>();
    private static List<Entry> entries;
    private static Map<String,List<Entry>> byId;
    private static Language language;
    private RegistryIndex() {}
    private static String lower(String s) { return s.toLowerCase(Locale.ROOT); }
    //#if MC >= 1.21.11
    private static Entry entry(Kind kind, Identifier id, String name, Item icon) {
    //#else
    //$$ private static Entry entry(Kind kind, ResourceLocation id, String name, Item icon) {
    //#endif
        return new Entry(kind,id.toString(),name,lower(name),icon);
    }
    public static void invalidate() { entries=null; byId=null; PREVIEWS.clear(); }
    public static List<Entry> entries() {
        if(entries != null && language == Language.getInstance()) return entries;
        language=Language.getInstance();
        var result = new ArrayList<Entry>();
        for (var item : BuiltInRegistries.ITEM) {
            var id=BuiltInRegistries.ITEM.getKey(item);
            if(id==null || item==Items.AIR) continue;
            String name = translatedName(item.getDescriptionId(), id);
            result.add(entry(Kind.ITEM, id, name, item));
        }
        for(var block:BuiltInRegistries.BLOCK) {
            var id=BuiltInRegistries.BLOCK.getKey(block);
            if(id==null) continue;
            var icon = block.asItem() == Items.AIR ? Items.BARRIER : block.asItem();
            result.add(entry(Kind.BLOCK,id,block.getName().getString(),icon));
        }
        for(var type:BuiltInRegistries.ENTITY_TYPE) {
            var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if(id==null) continue;
            var icon=entityIcon(id);
            result.add(entry(Kind.ENTITY,id,translated("entity",id),icon));
        }
        for(var fluid:BuiltInRegistries.FLUID) {
            var id=BuiltInRegistries.FLUID.getKey(fluid);
            if(id==null) continue;
            var bucket=iconFor(id.withPath(id.getPath().replaceFirst("^flowing_", "")+"_bucket"));
            if (bucket == null) bucket = Items.BUCKET;
            result.add(entry(Kind.FLUID,id,translated("fluid",id),bucket));
        }
        result.sort(Comparator.comparing(Entry::kind).thenComparing(Entry::id));
        var map=new HashMap<String,List<Entry>>();
        for(var e:result) map.computeIfAbsent(e.id(),k->new ArrayList<>()).add(e);
        byId=map; entries=List.copyOf(result); return entries;
    }
    //#if MC >= 1.21.11
    private static String translated(String prefix, Identifier id) {
    //#else
    //$$ private static String translated(String prefix, ResourceLocation id) {
    //#endif
        return translatedName(prefix + "." + id.getNamespace() + "." + id.getPath().replace('/', '.'), id);
    }

    //#if MC >= 1.21.11
    private static String translatedName(String key, Identifier id)
    //#else
    //$$ private static String translatedName(String key, ResourceLocation id)
    //#endif
    {
        return Language.getInstance().getOrDefault(key, id.getPath());
    }

    //#if MC >= 1.21.11
    private static Item iconFor(Identifier id)
    //#else
    //$$ private static Item iconFor(ResourceLocation id)
    //#endif
    {
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    //#if MC >= 1.21.11
    private static Item entityIcon(Identifier id) {
    //#else
    //$$ private static Item entityIcon(ResourceLocation id) {
    //#endif
        var egg=iconFor(id.withPath(id.getPath()+"_spawn_egg"));
        if (egg != null) return egg;
        var same=iconFor(id);
        if (same != null) return same;
        return switch(id.toString()) {
            case "minecraft:player" -> Items.PLAYER_HEAD;
            case "minecraft:item" -> Items.DIAMOND;
            case "minecraft:experience_orb" -> Items.EXPERIENCE_BOTTLE;
            default -> Items.NAME_TAG;
        };
    }

    private static ItemStack previewStack(Item item)
    {
        //#if MC >= 26.1
        var holder = item.builtInRegistryHolder();
        boolean bound = holder.areComponentsBound();
        var previous = PREVIEWS.get(item);
        if (previous != null && previous.componentsBound() == bound) return previous.stack();

        // 26.3 can open configuration before the server has bound built-in item components.
        var iconHolder = bound ? holder : Holder.direct(item, DataComponentMap.builder()
                .set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(item))
                .set(DataComponents.ITEM_NAME, Component.translatable(item.getDescriptionId()))
                .build());
        var stack = new ItemStack(iconHolder);
        PREVIEWS.put(item, new Preview(stack, bound));
        return stack;
        //#else
        //$$ return PREVIEWS.computeIfAbsent(item, value -> new Preview(new ItemStack(value), true)).stack();
        //#endif
    }
    public static List<Entry> lookup(String text) {
        entries();
        //#if MC >= 1.21.11
        var id=Identifier.tryParse(text.trim());
        //#else
        //$$ var id=ResourceLocation.tryParse(text.trim());
        //#endif
        return id==null?List.of():byId.getOrDefault(id.toString(),List.of());
    }
    public static Entry first(String id, Set<Kind> kinds) {
        return lookup(id).stream().filter(e->kinds.contains(e.kind())).findFirst().orElse(null);
    }
    public static List<Entry> search(Set<Kind> kinds, String text, Predicate<Entry> allowed) {
        String query=lower(text.trim());
        record Ranked(Entry entry,int rank) {}
        var matches=new ArrayList<Ranked>();
        for(var e:entries()) {
            if(!kinds.contains(e.kind()) || !allowed.test(e)) continue;
            int rank=rank(e,query);
            if(rank>=0) matches.add(new Ranked(e,rank));
        }
        matches.sort(Comparator.comparing((Ranked r)->r.entry().kind()).thenComparingInt(Ranked::rank).thenComparing(r->r.entry().id()));
        return matches.stream().map(Ranked::entry).toList();
    }
    private static int rank(Entry e, String q) {
        if(q.isEmpty()) return 0;
        if(q.startsWith("@")) return e.id().substring(0,e.id().indexOf(':')).startsWith(q.substring(1))?0:-1;
        String path=e.id().substring(e.id().indexOf(':')+1);
        if(e.id().equals(q)||path.equals(q)||e.searchName().equals(q)) return 0;
        if(e.id().startsWith(q)||path.startsWith(q)||e.searchName().startsWith(q)) return 1;
        if(e.id().contains(q)||e.searchName().contains(q)) return 2;
        if(PinyinCompat.contains(e.name(),q)) return 3;
        return -1;
    }
}
//#endif

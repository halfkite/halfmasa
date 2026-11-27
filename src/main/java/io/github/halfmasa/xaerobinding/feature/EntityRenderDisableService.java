package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
//#if MC >= 1.21.1
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.EntityRenderDisableMode;

/** Immutable type rules are replaced only when configuration changes, never per entity/frame. */
public final class EntityRenderDisableService
{
    private record Rules(Set<EntityType<?>> whitelist, Set<EntityType<?>> blacklist) {}
    private static volatile Rules rules = new Rules(Set.of(), Set.of());
    private EntityRenderDisableService() {}

    public static void reload()
    {
        var invalid = new java.util.LinkedHashSet<String>();
        rules = new Rules(normalize(Configs.ENTITY_RENDER_DISABLE_WHITELIST.getStrings(), invalid),
                normalize(Configs.ENTITY_RENDER_DISABLE_BLACKLIST.getStrings(), invalid));
        if (!invalid.isEmpty()) XaeroWorldBinding.LOGGER.warn("Ignoring invalid or unregistered entity rendering IDs: {}", invalid);
    }
    private static Set<EntityType<?>> normalize(List<String> entries, Set<String> invalid)
    {
        var types = new HashSet<EntityType<?>>();
        for (String entry : entries)
        {
            if (entry == null || entry.isBlank()) continue;
            String input = entry.trim().toLowerCase(Locale.ROOT);
            if (!input.contains(":")) input = "minecraft:" + input;
            //#if MC >= 1.21.1
            Identifier id = Identifier.tryParse(input);
            //#else
            //$$ ResourceLocation id = ResourceLocation.tryParse(input);
            //#endif
            if (id == null) { invalid.add(entry); continue; }
            // getOptional avoids the registry's default entity for an unknown ID.
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (type.isPresent()) types.add(type.get()); else invalid.add(entry);
        }
        return Set.copyOf(types);
    }
    public static boolean isFiltering()
    {
        if (!Configs.CUSTOM_ENTITY_RENDER_DISABLE.getBooleanValue() || Minecraft.getInstance().level == null
                || HoldToShow.entityRenderingRestored()) return false;
        //#if MC >= 1.21.1
        return Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue() != EntityRenderDisableMode.BLACKLIST || !rules.blacklist.isEmpty();
        //#else
        //$$ return Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue() != EntityRenderDisableMode.WHITELIST || !rules.whitelist.isEmpty();
        //#endif
    }
    public static boolean shouldHide(Entity entity)
    {
        if (entity == null || !isFiltering()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player || entity == mc.getCameraEntity()) return false;
        return switch ((EntityRenderDisableMode)Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue())
        {
            //#if MC >= 1.21.1
            case BLACKLIST -> rules.blacklist.contains(entity.getType());
            case WHITELIST -> !rules.whitelist.contains(entity.getType());
            //#else
            //$$ case BLACKLIST -> !rules.blacklist.contains(entity.getType());
            //$$ case WHITELIST -> rules.whitelist.contains(entity.getType());
            //#endif
            case NONE -> true;
        };
    }
}
//#endif

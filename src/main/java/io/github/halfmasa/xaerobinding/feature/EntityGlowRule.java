package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.Locale;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import io.github.halfmasa.xaerobinding.config.Configs;

//#if MC >= 1.21.11
public record EntityGlowRule(TargetType type, Identifier id, int color, boolean enabled, String hotkey)
//#else
//$$ public record EntityGlowRule(TargetType type, ResourceLocation id, int color, boolean enabled, String hotkey)
//#endif
{
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    //#if MC >= 1.21.11
    public EntityGlowRule(TargetType type, Identifier id, int color)
    //#else
    //$$ public EntityGlowRule(TargetType type, ResourceLocation id, int color)
    //#endif
    {
        this(type, id, color, true, "");
    }
    public enum TargetType
    {
        ENTITY("entity"), ITEM("item"), BLOCK("block");

        private final String serialized;

        TargetType(String serialized) { this.serialized = serialized; }
        public String serialized() { return this.serialized; }
        public TargetType next() { return this == ENTITY ? ITEM : this == ITEM ? BLOCK : ENTITY; }
    }

    //#if MC >= 1.21.11
    public static Identifier normalizeId(String input)
    {
        return input == null || input.isBlank()
                ? null : Identifier.tryParse(input.trim().toLowerCase(Locale.ROOT));
    }
    //#else
    //$$ public static ResourceLocation normalizeId(String input)
    //$$ {
    //$$     return input == null || input.isBlank()
    //$$             ? null : ResourceLocation.tryParse(input.trim().toLowerCase(Locale.ROOT));
    //$$ }
    //#endif

    public static EntityGlowRule parse(String encoded)
    {
        if (encoded == null) return null;
        String[] fields = encoded.split("\\|", -1);
        if (fields.length != 2 && fields.length != 3 && fields.length != 5) return null;
        TargetType type = switch (fields[0])
        {
            case "entity" -> TargetType.ENTITY;
            case "item" -> TargetType.ITEM;
            case "block" -> TargetType.BLOCK;
            default -> null;
        };
        //#if MC >= 1.21.11
        Identifier id = normalizeId(fields[1]);
        //#else
        //$$ ResourceLocation id = normalizeId(fields[1]);
        //#endif
        Integer color = fields.length == 2 ? DEFAULT_COLOR : parseColor(fields[2]);
        if (type == null || id == null || color == null) return null;
        if (fields.length != 5) return new EntityGlowRule(type, id, color);
        if (!fields[3].equals("true") && !fields[3].equals("false")) return null;
        try
        {
            String hotkey = new String(Base64.getUrlDecoder().decode(fields[4]), StandardCharsets.UTF_8);
            return new EntityGlowRule(type, id, color, Boolean.parseBoolean(fields[3]), hotkey);
        }
        catch (IllegalArgumentException exception)
        {
            return null;
        }
    }

    public static Integer parseColor(String input)
    {
        if (input == null) return null;
        String value = input.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.matches("[0-9a-fA-F]{6}")) return 0xFF000000 | Integer.parseInt(value, 16);
        if (value.matches("[0-9a-fA-F]{8}")) return (int) Long.parseLong(value, 16);
        return null;
    }

    public String key() { return this.type.serialized() + "|" + this.id; }
    public String encode()
    {
        String encodedHotkey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(this.hotkey.getBytes(StandardCharsets.UTF_8));
        return this.key() + "|" + String.format(Locale.ROOT, "%08X", this.color)
                + "|" + this.enabled + "|" + encodedHotkey;
    }

    /** The old block list is read until the unified editor saves it into the primary list. */
    public static List<EntityGlowRule> loadAll()
    {
        Map<String, EntityGlowRule> merged = new LinkedHashMap<>();
        for (String encoded : Configs.ENTITY_GLOW_RULES.getStrings())
        {
            EntityGlowRule rule = parse(encoded);
            if (rule != null) merged.putIfAbsent(rule.key(), rule);
        }
        for (String encoded : Configs.BLOCK_GLOW_RULES.getStrings())
        {
            EntityGlowRule rule = parse(encoded);
            if (rule != null) merged.putIfAbsent(rule.key(), rule);
        }
        return new ArrayList<>(merged.values());
    }
}
//#endif

package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.Locale;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import fi.dy.masa.malilib.config.options.ConfigStringList;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import io.github.halfmasa.xaerobinding.config.Configs;

//#if MC >= 1.21.11
public record EntityGlowRule(TargetType type, Identifier id, int color, boolean enabled, String hotkey, int distance)
//#else
//$$ public record EntityGlowRule(TargetType type, ResourceLocation id, int color, boolean enabled, String hotkey, int distance)
//#endif
{
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;
    public static final int INHERIT_DISTANCE = 0;
    public static final int DEFAULT_BLOCK_DISTANCE = 16;
    public static final int MAX_BLOCK_DISTANCE = 128;
    //#if MC >= 1.21.11
    public EntityGlowRule(TargetType type, Identifier id, int color)
    //#else
    //$$ public EntityGlowRule(TargetType type, ResourceLocation id, int color)
    //#endif
    {
        this(type, id, color, true, "", INHERIT_DISTANCE);
    }

    public int effectiveDistance(int globalDistance)
    {
        return this.distance == INHERIT_DISTANCE ? globalDistance : this.distance;
    }

    public EntityGlowRule withEnabled(boolean enabled)
    {
        return new EntityGlowRule(this.type, this.id, this.color, enabled, this.hotkey, this.distance);
    }
    public enum TargetType
    {
        ENTITY("entity"), ITEM("item"), BLOCK("block");

        private final String serialized;

        TargetType(String serialized) { this.serialized = serialized; }
        public String serialized() { return this.serialized; }
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
        if (fields.length != 2 && fields.length != 3 && fields.length != 5 && fields.length != 6) return null;
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
        if (fields.length < 5) return new EntityGlowRule(type, id, color);
        if (!fields[3].equals("true") && !fields[3].equals("false")) return null;
        Integer distance = INHERIT_DISTANCE;
        if (fields.length == 6 && type == TargetType.BLOCK) distance = parseBlockDistance(fields[5]);
        try
        {
            String hotkey = new String(Base64.getUrlDecoder().decode(fields[4]), StandardCharsets.UTF_8);
            return new EntityGlowRule(type, id, color, Boolean.parseBoolean(fields[3]), hotkey,
                    distance == null ? INHERIT_DISTANCE : distance);
        }
        catch (IllegalArgumentException exception)
        {
            return null;
        }
    }

    public static Integer parseBlockDistance(String input)
    {
        String value = input.trim();
        if (value.isEmpty()) return INHERIT_DISTANCE;
        if (!value.matches("[0-9]{1,3}")) return null;
        int distance = Integer.parseInt(value);
        return distance >= 1 && distance <= MAX_BLOCK_DISTANCE ? distance : null;
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
        String encoded = this.key() + "|" + String.format(Locale.ROOT, "%08X", this.color)
                + "|" + this.enabled + "|" + encodedHotkey;
        return this.type == TargetType.BLOCK && this.distance != INHERIT_DISTANCE
                ? encoded + "|" + this.distance : encoded;
    }

    public static ConfigStringList configFor(boolean blocks)
    {
        return blocks ? Configs.BLOCK_GLOW_RULES : Configs.ENTITY_GLOW_RULES;
    }

    public static List<EntityGlowRule> loadRules(boolean blocks)
    {
        return configFor(blocks).getStrings().stream()
                .map(EntityGlowRule::parse)
                .filter(Objects::nonNull)
                .filter(rule -> (rule.type() == TargetType.BLOCK) == blocks)
                .toList();
    }

    public static List<EntityGlowRule> loadAll()
    {
        List<EntityGlowRule> rules = new ArrayList<>(loadRules(false));
        rules.addAll(loadRules(true));
        return rules;
    }

    public static void migrateSplitRules()
    {
        List<String> entityRules = new ArrayList<>();
        List<String> blockRules = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // The unified list took precedence when both legacy lists contained the same rule.
        for (boolean fromBlockList : new boolean[] { false, true })
        {
            for (String encoded : configFor(fromBlockList).getStrings())
            {
                EntityGlowRule rule = parse(encoded);
                if (rule == null)
                {
                    (fromBlockList ? blockRules : entityRules).add(encoded);
                }
                else if (seen.add(rule.key()))
                {
                    (rule.type() == TargetType.BLOCK ? blockRules : entityRules).add(encoded);
                }
            }
        }
        Configs.ENTITY_GLOW_RULES.setStrings(entityRules);
        Configs.BLOCK_GLOW_RULES.setStrings(blockRules);
    }
}
//#endif

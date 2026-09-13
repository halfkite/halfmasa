package io.github.halfmasa.xaerobinding.feature;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fi.dy.masa.malilib.config.IConfigBase;

import io.github.halfmasa.xaerobinding.config.ConfigGroupHeader;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Resolves editable config entries without making Tweakeroo a hard dependency. */
public final class CustomConfigSources
{
    public static final String HALF_MASA = CustomConfigGroupStore.HALF_MASA_SOURCE;
    public static final String TWEAKEROO = CustomConfigGroupStore.TWEAKEROO_SOURCE;

    private static final String[] TWEAKEROO_CATEGORIES = {
            "Generic", "Fixes", "Lists", "Disable", "Internal"};

    private CustomConfigSources()
    {
    }

    public static List<Candidate> getCandidates(String source)
    {
        if (TWEAKEROO.equals(source))
        {
            return getTweakerooCandidates();
        }

        List<Candidate> result = new ArrayList<>();
        for (IConfigBase config : Configs.getCustomGroupCandidates())
        {
            result.add(new Candidate(HALF_MASA, config.getName(), config));
        }
        return result;
    }

    public static boolean isTweakerooAvailable()
    {
        try
        {
            Class.forName("fi.dy.masa.tweakeroo.config.Configs", false,
                    CustomConfigSources.class.getClassLoader());
            return true;
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    private static List<Candidate> getTweakerooCandidates()
    {
        Map<String, Candidate> result = new LinkedHashMap<>();
        for (String category : TWEAKEROO_CATEGORIES)
        {
            String className = "fi.dy.masa.tweakeroo.config.Configs$" + category;
            try
            {
                Class<?> categoryClass = Class.forName(className);
                Field options = categoryClass.getField("OPTIONS");
                if (!Modifier.isStatic(options.getModifiers()))
                {
                    continue;
                }
                Object value = options.get(null);
                if (!(value instanceof Iterable<?> iterable))
                {
                    continue;
                }
                for (Object entry : iterable)
                {
                    if (entry instanceof IConfigBase config && !(config instanceof ConfigGroupHeader) &&
                            !config.getName().endsWith("Expanded"))
                    {
                        result.putIfAbsent(CustomConfigGroupStore.reference(TWEAKEROO, config.getName()),
                                new Candidate(TWEAKEROO, config.getName(), config));
                    }
                }
            }
            catch (Throwable ignored)
            {
                // Tweakeroo is optional and its category layout can change between versions.
            }
        }
        // Feature toggles and generic hotkeys live outside Configs$*.OPTIONS.
        addCandidatesFromField(result,
                "fi.dy.masa.tweakeroo.config.FeatureToggle", "VALUES");
        addCandidatesFromField(result,
                "fi.dy.masa.tweakeroo.config.Hotkeys", "HOTKEY_LIST");
        return new ArrayList<>(result.values());
    }

    private static void addCandidatesFromField(
            Map<String, Candidate> result, String className, String fieldName)
    {
        try
        {
            Class<?> owner = Class.forName(className);
            Field field = owner.getField(fieldName);
            if (!Modifier.isStatic(field.getModifiers()))
            {
                return;
            }
            Object value = field.get(null);
            if (!(value instanceof Iterable<?> iterable))
            {
                return;
            }
            for (Object entry : iterable)
            {
                if (entry instanceof IConfigBase config &&
                        !(config instanceof ConfigGroupHeader) &&
                        !config.getName().endsWith("Expanded"))
                {
                    result.putIfAbsent(CustomConfigGroupStore.reference(TWEAKEROO, config.getName()),
                            new Candidate(TWEAKEROO, config.getName(), config));
                }
            }
        }
        catch (Throwable ignored)
        {
            // Optional Tweakeroo classes may not exist or may change between versions.
        }
    }

    public record Candidate(String source, String name, IConfigBase config)
    {
        public String reference()
        {
            return CustomConfigGroupStore.reference(this.source, this.name);
        }
    }
}

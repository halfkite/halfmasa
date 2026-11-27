package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import java.util.Map;
//#if MC >= 1.21.1
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Shadow;
//#endif

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.Resource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.CjkLatinSpacing;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin
{
    //#if MC >= 1.21.1
    @Shadow private static void appendFrom(String locale, List<Resource> resources, Map<String, String> translations)
    {
        throw new AssertionError();
    }

    @WrapOperation(method = "loadFrom", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/packs/resources/ResourceManager;getNamespaces()Ljava/util/Set;"))
    private static Set<String> halfmasa$loadChineseFallback(ResourceManager manager, Operation<Set<String>> original,
            @Local(argsOnly = true) List<String> languageStack, @Local(ordinal = 0) String locale,
            @Local Map<String, String> translations)
    {
        Set<String> namespaces = original.call(manager);
        if (!Configs.CHINESE_MOD_LANGUAGE_FALLBACK.getBooleanValue() || languageStack.isEmpty()
                || !locale.equals(languageStack.getLast())) return namespaces;

        String fallback = switch (locale)
        {
            case "zh_cn" -> "zh_tw";
            case "zh_tw" -> "zh_cn";
            default -> null;
        };
        if (fallback == null) return namespaces;

        Map<String, String> defaults = new HashMap<>();
        for (String namespace : new TreeSet<>(namespaces))
        {
            if (namespace.equals("minecraft") || !manager.getResourceStack(
                    halfmasa$languageResource(namespace, locale)).isEmpty()) continue;
            List<Resource> resources = manager.getResourceStack(
                    halfmasa$languageResource(namespace, fallback));
            if (resources.isEmpty()) continue;
            Map<String, String> entries = new HashMap<>();
            appendFrom(fallback, resources, entries);
            entries.forEach(defaults::putIfAbsent);
        }
        // The selected locale loads next, preserving native translations across all namespaces.
        translations.putAll(defaults);
        return namespaces;
    }

    //#if MC >= 1.21.11
    private static Identifier halfmasa$languageResource(String namespace, String locale)
    {
        return Identifier.fromNamespaceAndPath(namespace, "lang/" + locale + ".json");
    }
    //#else
    //$$ private static ResourceLocation halfmasa$languageResource(String namespace, String locale)
    //$$ {
    //$$     return ResourceLocation.fromNamespaceAndPath(namespace, "lang/" + locale + ".json");
    //$$ }
    //#endif
    //#endif

    @Inject(method = "appendFrom", at = @At("RETURN"))
    private static void halfmasa_addCjkLatinSpacing(
            String languageCode, List<Resource> resources, Map<String, String> translations, CallbackInfo ci)
    {
        if (Configs.CJK_LATIN_SPACING.getBooleanValue() &&
                Configs.CJK_LATIN_SPACING_TRANSLATIONS.getBooleanValue())
        {
            translations.replaceAll((key, value) -> CjkLatinSpacing.apply(value));
        }
    }
}

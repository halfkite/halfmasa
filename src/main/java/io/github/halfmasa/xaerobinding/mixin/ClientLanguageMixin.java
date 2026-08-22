package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import java.util.Map;

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

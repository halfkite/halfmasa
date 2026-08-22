package io.github.halfmasa.xaerobinding.mixin;

import java.util.function.Function;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.CjkLatinComponentSpacing;

@Mixin(SignText.class)
public abstract class SignTextMixin
{
    @Shadow @Nullable private FormattedCharSequence[] renderMessages;
    @Unique private boolean halfmasa$spacingEnabled;

    @Inject(method = "getRenderMessages", at = @At("HEAD"))
    private void halfmasa_refreshSpacingCache(
            boolean filtered,
            Function<Component, FormattedCharSequence> formatter,
            CallbackInfoReturnable<FormattedCharSequence[]> cir)
    {
        boolean enabled = Configs.CJK_LATIN_SPACING.getBooleanValue() &&
                Configs.CJK_LATIN_SPACING_SIGNS.getBooleanValue();
        if (enabled != this.halfmasa$spacingEnabled)
        {
            this.halfmasa$spacingEnabled = enabled;
            this.renderMessages = null;
        }
    }

    @ModifyVariable(method = "getRenderMessages", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Function<Component, FormattedCharSequence> halfmasa_spaceSignText(
            Function<Component, FormattedCharSequence> formatter)
    {
        if (!Configs.CJK_LATIN_SPACING.getBooleanValue() ||
                !Configs.CJK_LATIN_SPACING_SIGNS.getBooleanValue())
        {
            return formatter;
        }
        return component -> formatter.apply(CjkLatinComponentSpacing.apply(component));
    }
}

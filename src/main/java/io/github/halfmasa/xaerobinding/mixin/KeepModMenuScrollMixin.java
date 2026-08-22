package io.github.halfmasa.xaerobinding.mixin;

import com.terraformersmc.modmenu.gui.ModsScreen;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.ConfigScrollMemory;

@Mixin(value = ModsScreen.class, remap = false)
public abstract class KeepModMenuScrollMixin
{
    @Shadow(remap = false) private ModListWidget modList;

    @Inject(method = {"removed", "method_25419"}, at = @At("HEAD"), require = 0, remap = false)
    private void halfmasa_saveModMenuScroll(CallbackInfo ci)
    {
        if (Configs.KEEP_MOD_MENU_SCROLL.getBooleanValue() && this.modList != null)
        {
            //#if MC >= 1.21.4
            ConfigScrollMemory.saveModMenu(this.modList.scrollAmount());
            //#else
            //$$ ConfigScrollMemory.saveModMenu(this.modList.getScrollAmount());
            //#endif
        }
    }

    @Inject(method = {"init", "method_25426"}, at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa_restoreModMenuScroll(CallbackInfo ci)
    {
        if (Configs.KEEP_MOD_MENU_SCROLL.getBooleanValue() && this.modList != null)
        {
            this.modList.setScrollAmount(ConfigScrollMemory.restoreModMenu());
        }
    }
}

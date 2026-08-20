package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
//#if MC >= 26.2
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.EditBox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.feature.ImeService;

@Mixin(EditBox.class)
public abstract class ImeEditBoxMixin
{
    @Shadow private int displayPos;
    @Shadow private boolean bordered;
    @Shadow public abstract String getValue();
    @Shadow public abstract int getCursorPosition();

    @Inject(method = "setFocused", at = @At("TAIL"))
    private void halfmasa_trackImeFocus(boolean focused, CallbackInfo ci)
    {
        ImeService.getInstance().onEditFocus((EditBox) (Object) this, focused);
    }

    //#if MC >= 26.2
    @Inject(method = "extractWidgetRenderState", at = @At("TAIL"))
    private void halfmasa_updateImeCaret(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "renderWidget", at = @At("TAIL"))
    //$$ private void halfmasa_updateImeCaret(
    //$$         GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    //#endif
    {
        EditBox edit = (EditBox) (Object) this;
        if (!edit.isFocused())
        {
            return;
        }
        String value = this.getValue();
        int start = Math.max(0, Math.min(this.displayPos, value.length()));
        int end = Math.max(start, Math.min(this.getCursorPosition(), value.length()));
        int padding = this.bordered ? 4 : 0;
        int x = edit.getX() + padding + Minecraft.getInstance().font.width(value.substring(start, end));
        int y = edit.getY() + (edit.getHeight() - Minecraft.getInstance().font.lineHeight) / 2;
        ImeService.getInstance().updateCaret(edit, x, y);
    }
}

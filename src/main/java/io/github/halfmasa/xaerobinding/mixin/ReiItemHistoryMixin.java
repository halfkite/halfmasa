package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.gui.GuiGraphics;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.objectweb.asm.Opcodes;

import io.github.halfmasa.xaerobinding.feature.ItemManagerHistoryOverlay;

@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.widget.entrylist.EntryListWidget", remap = false)
public abstract class ReiItemHistoryMixin
{
    @Dynamic
    @Inject(method = {"render", "method_25394"}, at = @At("HEAD"), require = 0, remap = false)
    private void halfmasa_synchronizeReiHistoryLayout(
            GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.synchronizeReiLayout(this);
    }

    @Dynamic
    @Inject(
            method = "updateEntriesPosition",
            at = @At(
                    value = "FIELD",
                    target = "Lme/shedaniel/rei/impl/client/gui/widget/entrylist/EntryListWidget;innerBounds:Lme/shedaniel/math/Rectangle;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER),
            require = 0,
            remap = false)
    private void halfmasa_reserveReiHistoryRows(CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.reserveReiArea(this);
    }

    @Dynamic
    @Inject(method = {"render", "method_25394"}, at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa_renderReiHistory(
            GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        ItemManagerHistoryOverlay.renderRei(this, graphics, mouseX, mouseY);
    }

    @Dynamic
    @Inject(method = {"mouseClicked", "method_25402"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void halfmasa_clickReiHistory(
            double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir)
    {
        if (ItemManagerHistoryOverlay.handleReiClick(this, mouseX, mouseY, button))
        {
            cir.setReturnValue(true);
        }
    }
}

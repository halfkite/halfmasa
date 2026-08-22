package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 1.21.4
import net.minecraft.client.renderer.state.MapRenderState;
//#endif
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
//#if MC >= 1.21.8
import org.joml.Matrix3x2fStack;
//#endif

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

//#if MC >= 26.1
@Mixin(GuiGraphicsExtractor.class)
//#else
//$$ @Mixin(GuiGraphics.class)
//#endif
public abstract class MapInSlotMixin
{
    @Shadow @Final private Minecraft minecraft;

    //#if MC >= 1.21.8
    @Shadow public abstract Matrix3x2fStack pose();
    //#if MC >= 26.1
    @Shadow public abstract void map(MapRenderState state);
    //#else
    //$$ @Shadow public abstract void submitMapRenderState(MapRenderState state);
    //#endif
    //#else
    //$$ @Shadow public abstract PoseStack pose();
//#endif
    //#if MC >= 1.21.4
    @Unique private final MapRenderState halfmasa_mapState = new MapRenderState();
    //#endif

    //#if MC >= 26.1
    @Inject(
            method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"))
    //#else
    //$$ @Inject(
    //$$         method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
    //$$         at = @At("HEAD"))
    //#endif
    private void halfmasa_renderMapInSlot(
            Font font, ItemStack stack, int x, int y, String countText, CallbackInfo ci)
    {
        if (!Configs.MAP_IN_SLOT.getBooleanValue() || !stack.is(Items.FILLED_MAP) || this.minecraft.level == null)
        {
            return;
        }
        if ((MinecraftClientCompat.getScreen(this.minecraft) == null && !Configs.MAP_IN_HOTBAR.getBooleanValue()) ||
            (MinecraftClientCompat.getScreen(this.minecraft) != null && !Configs.MAP_IN_INVENTORY.getBooleanValue()))
        {
            return;
        }

        var mapId = stack.get(DataComponents.MAP_ID);
        var savedData = MapItem.getSavedData(mapId, this.minecraft.level);
        if (mapId == null || savedData == null)
        {
            return;
        }

        //#if MC >= 26.1
        this.pose().pushMatrix();
        this.pose().translate(x, y);
        this.pose().scale(0.125F, 0.125F);
        this.minecraft.getMapRenderer().extractRenderState(mapId, savedData, this.halfmasa_mapState);
        this.map(this.halfmasa_mapState);
        this.pose().popMatrix();
        //#elseif MC >= 1.21.8
        //$$ this.pose().pushMatrix();
        //$$ this.pose().translate(x, y);
        //$$ this.pose().scale(0.125F, 0.125F);
        //$$ this.minecraft.getMapRenderer().extractRenderState(mapId, savedData, this.halfmasa_mapState);
        //$$ this.submitMapRenderState(this.halfmasa_mapState);
        //$$ this.pose().popMatrix();
        //#elseif MC >= 1.21.4
        //$$ this.pose().pushPose();
        //$$ this.pose().translate(x, y, 200.0F);
        //$$ this.pose().scale(0.125F, 0.125F, 0.125F);
        //$$ this.minecraft.getMapRenderer().extractRenderState(mapId, savedData, this.halfmasa_mapState);
        //$$ this.minecraft.getMapRenderer().render(
        //$$         this.halfmasa_mapState, this.pose(), this.minecraft.renderBuffers().bufferSource(), false, 0xF000F0);
        //$$ this.pose().popPose();
        //#else
        //$$ this.pose().pushPose();
        //$$ this.pose().translate(x, y, 200.0F);
        //$$ this.pose().scale(0.125F, 0.125F, 0.125F);
        //$$ this.minecraft.gameRenderer.getMapRenderer().render(
        //$$         this.pose(), this.minecraft.renderBuffers().bufferSource(), mapId, savedData, false, 0xF000F0);
        //$$ this.pose().popPose();
        //#endif
    }
}

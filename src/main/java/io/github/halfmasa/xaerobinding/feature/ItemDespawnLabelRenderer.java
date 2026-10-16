package io.github.halfmasa.xaerobinding.feature;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import io.github.halfmasa.xaerobinding.config.Configs;
//#if MC < 1.21.10
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraft.client.gui.Font;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#endif

public final class ItemDespawnLabelRenderer
{
    private ItemDespawnLabelRenderer() {}

    public static ItemDespawnLabelState.Label snapshot(ItemEntity item)
    {
        var text = ItemDespawnTimerService.getInstance().labelFor(item);
        return text == null ? null : new ItemDespawnLabelState.Label(text,
                new Vec3(0, item.getBbHeight() + 0.9D, 0), Configs.ITEM_DESPAWN_THROUGH_WALLS.getBooleanValue());
    }

    //#if MC < 1.21.10
    //$$ public static void render(ItemDespawnLabelState.Label label, PoseStack pose, MultiBufferSource buffers, int light)
    //$$ {
    //$$     if (label == null || !Configs.ITEM_DESPAWN_TIMER.getBooleanValue()) return;
    //$$     var client = Minecraft.getInstance();
    //$$     pose.pushPose();
    //$$     pose.translate(label.attachment().x, label.attachment().y + 0.5D, label.attachment().z);
    //$$     pose.mulPose(client.getEntityRenderDispatcher().cameraOrientation());
    //$$     pose.scale(0.025F, -0.025F, 0.025F);
    //$$     client.font.drawInBatch(label.text(), -client.font.width(label.text()) / 2.0F, 0, 0xFFFFFFFF, false,
    //$$             pose.last().pose(), buffers, label.throughWalls() ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
    //$$             0x40000000, light);
    //$$     pose.popPose();
    //$$ }
    //#endif
}

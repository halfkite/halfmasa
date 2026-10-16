package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.SwingVisualTracker;
//#if MC >= 1.21.10
import net.minecraft.client.renderer.SubmitNodeCollector;
//#else
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#endif
//#if MC >= 26.3
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
//#else
//$$ import net.minecraft.client.renderer.ItemInHandRenderer;
//$$ import net.minecraft.client.player.AbstractClientPlayer;
//#endif
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

//#if MC >= 26.3
@Mixin(FirstPersonHandsAndItemsRenderer.class)
//#else
//$$ @Mixin(ItemInHandRenderer.class)
//#endif
public abstract class RemoveSwingAnimationMixin
{
    //#if MC >= 26.3
    @WrapMethod(method = "submitArmWithItem")
    private void halfmasa$visualProgress(PlayerRenderState player, FirstPersonHandsAndItemsRenderState items,
            float partialTick, float pitch, InteractionHand hand, float swingProgress, ItemStack stack,
            float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector, int light, Operation<Void> original)
    {
        if (SwingVisualTracker.hide(hand)) swingProgress = 0;
        if (Configs.REMOVE_COOLDOWN_ANIMATIONS.getBooleanValue() && Configs.REMOVE_EQUIP_ANIMATION.getBooleanValue())
            inverseArmHeight = 0;
        original.call(player, items, partialTick, pitch, hand, swingProgress, stack, inverseArmHeight, pose, collector, light);
    }
    //#else
    //#if MC >= 26.2
    //$$ @WrapMethod(method = "submitArmWithItem")
    //#else
    //$$ @WrapMethod(method = "renderArmWithItem")
    //#endif
    //$$ private void halfmasa$visualProgress(AbstractClientPlayer player, float partialTick, float pitch,
    //$$         InteractionHand hand, float swingProgress, ItemStack stack, float inverseArmHeight,
    //#if MC >= 1.21.10
    //$$         PoseStack pose, SubmitNodeCollector collector, int light, Operation<Void> original)
    //#else
    //$$         PoseStack pose, MultiBufferSource collector, int light, Operation<Void> original)
    //#endif
    //$$ {
    //$$     if (SwingVisualTracker.hide(hand)) swingProgress = 0;
    //$$     if (Configs.REMOVE_COOLDOWN_ANIMATIONS.getBooleanValue() && Configs.REMOVE_EQUIP_ANIMATION.getBooleanValue())
    //$$         inverseArmHeight = 0;
    //$$     original.call(player, partialTick, pitch, hand, swingProgress, stack, inverseArmHeight, pose, collector, light);
    //$$ }
    //#endif
}
//#endif

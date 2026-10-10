package io.github.halfmasa.xaerobinding.mixin;

//#if MC < 1.21.4
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.halfmasa.xaerobinding.feature.BakedSpawnerPreview;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class BakedSpawnerItemMixin
{
    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void halfmasa$stateModel(ItemStack stack, Level level, LivingEntity entity, int seed,
                                    CallbackInfoReturnable<BakedModel> cir)
    {
        if (BakedSpawnerPreview.supports(stack)) cir.setReturnValue(BakedSpawnerPreview.cageModel(stack));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void halfmasa$standingOccupant(ItemStack stack, ItemDisplayContext context, boolean leftHand,
            PoseStack poses, MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci)
    {
        BakedSpawnerPreview.render(stack, context, poses, buffers, light, model);
    }
}
//#endif

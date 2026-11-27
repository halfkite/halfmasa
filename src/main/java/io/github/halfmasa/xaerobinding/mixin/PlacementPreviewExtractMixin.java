package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
//#if MC >= 26.2
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
//#else
//$$ import net.minecraft.client.renderer.LevelRenderer;
//#endif
import io.github.halfmasa.xaerobinding.feature.placementpreview.PlacementPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 26.2
@Mixin(LevelExtractor.class)
//#else
//$$ @Mixin(LevelRenderer.class)
//#endif
public abstract class PlacementPreviewExtractMixin {
    //#if MC >= 26.2
    @Shadow @Final private SimpleGizmoCollector mainThreadGizmos;

    @Inject(method = "extractGizmos", at = @At("HEAD"))
    //#elseif MC >= 26.1
    //$$ @Inject(method = "extractLevel", at = @At("TAIL"))
    //#else
    //$$ @Inject(method = "renderLevel", at = @At("HEAD"))
    //#endif
    private void halfmasa$extract(CallbackInfo ci)
    {
        //#if MC >= 26.2
        try (var ignored = Gizmos.withCollector(this.mainThreadGizmos))
        //#elseif MC >= 1.21.11
        //$$ try (var ignored = ((LevelRenderer) (Object) this).collectPerFrameGizmos())
        //#endif
        {
            PlacementPreview.extract();
        }
    }
}
//#endif

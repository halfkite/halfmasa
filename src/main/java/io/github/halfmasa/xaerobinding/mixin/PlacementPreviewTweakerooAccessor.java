package io.github.halfmasa.xaerobinding.mixin;
//#if MC >= 1.21.1
//#if MC >= 1.21.5
import fi.dy.masa.malilib.util.position.PositionUtils;
//#else
//$$ import fi.dy.masa.malilib.util.PositionUtils;
//#endif
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Pseudo
@Mixin(targets="fi.dy.masa.tweakeroo.tweaks.PlacementTweaks", remap=false)
public interface PlacementPreviewTweakerooAccessor {
    @Accessor("firstWasRotation") static boolean halfmasa$firstRotation() { throw new AssertionError(); }
    @Accessor("firstWasOffset") static boolean halfmasa$firstOffset() { throw new AssertionError(); }
    @Invoker("getRotatedFacing") static Direction halfmasa$rotate(Direction face, Direction playerFacing, PositionUtils.HitPart part) { throw new AssertionError(); }
}
//#endif

package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LerpingBossEvent.class)
public interface LerpingBossEventAccessor
{
    @Accessor("targetPercent")
    float halfmasa$getTargetPercent();
}
//#endif

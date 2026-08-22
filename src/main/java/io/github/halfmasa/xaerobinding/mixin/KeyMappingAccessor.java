package io.github.halfmasa.xaerobinding.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor
{
    @Accessor("key")
    InputConstants.Key halfmasa$getBoundKey();

    @Accessor("isDown")
    void halfmasa$setDownDirect(boolean down);

    @Accessor("clickCount")
    void halfmasa$setClickCount(int count);
}

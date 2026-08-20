package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftInputAccessor
{
    @Accessor("missTime")
    void halfmasa$setMissTime(int value);
}

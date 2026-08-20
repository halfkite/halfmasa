package io.github.halfmasa.xaerobinding.mixin;

import java.nio.file.Path;

import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.halfmasa.xaerobinding.feature.CustomSavesPath;

@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceMixin
{
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Path halfmasa$replaceDefaultSavesPath(Path original)
    {
        return CustomSavesPath.replaceDefault(original);
    }
}

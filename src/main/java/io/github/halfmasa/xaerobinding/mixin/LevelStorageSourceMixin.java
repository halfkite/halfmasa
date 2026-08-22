package io.github.halfmasa.xaerobinding.mixin;

import java.nio.file.Path;

import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import io.github.halfmasa.xaerobinding.feature.CustomSavesPath;
import io.github.halfmasa.xaerobinding.feature.LevelStorageSourceAccess;

@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceMixin implements LevelStorageSourceAccess
{
    @Mutable
    @Shadow @Final
    private Path baseDir;

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Path halfmasa$replaceDefaultSavesPath(Path original)
    {
        return CustomSavesPath.replaceDefault(original);
    }

    @Unique
    @Override
    public void halfmasa$setBaseDir(Path path)
    {
        this.baseDir = path;
    }
}

package io.github.halfmasa.xaerobinding.mixin;

import java.nio.file.Path;

import net.minecraft.client.HotbarManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.feature.BetterSavedHotbarStorage;

@Mixin(HotbarManager.class)
public abstract class HotbarManagerMixin
{
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"))
    private Path halfmasa_useDedicatedStorage(Path gameDirectory, String fileName)
    {
        return BetterSavedHotbarStorage.prepare(gameDirectory, fileName);
    }
}

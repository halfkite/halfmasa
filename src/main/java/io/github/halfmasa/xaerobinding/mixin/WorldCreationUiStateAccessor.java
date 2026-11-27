package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import java.nio.file.Path;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldCreationUiState.class)
public interface WorldCreationUiStateAccessor
{
    @Mutable @Accessor("savesFolder")
    void halfmasa$setSavesFolder(Path path);
}
//#endif

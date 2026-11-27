package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.WorldListEntryStorage;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryStorageMixin implements WorldListEntryStorage
{
    //#if MC >= 1.21.10
    @Shadow @Final private WorldSelectionList list;
    //#else
    //$$ @Shadow @Final private LevelSummary summary;
    //#endif
    @Unique private LevelStorageSource halfmasa$storage;

    @Override public LevelStorageSource halfmasa$getStorage() { return this.halfmasa$storage; }
    @Override public void halfmasa$setStorage(LevelStorageSource storage) { this.halfmasa$storage = storage; }

    @Override public LevelSummary halfmasa$getSummary()
    {
        //#if MC >= 1.21.10
        return ((WorldSelectionList.WorldListEntry) (Object) this).getLevelSummary();
        //#else
        //$$ return this.summary;
        //#endif
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/screens/FaviconTexture;forWorld(Lnet/minecraft/client/renderer/texture/TextureManager;Ljava/lang/String;)Lnet/minecraft/client/gui/screens/FaviconTexture;"), index = 1)
    private String halfmasa$uniqueIcon(String id)
    {
        boolean managed = Configs.BETTER_WORLD_SAVES.getBooleanValue();
        //#if MC >= 1.21.10
        managed &= this.list.getScreen() instanceof SelectWorldScreen;
        //#endif
        return managed ? id + "/" + UUID.randomUUID() : id;
    }

    @Redirect(method = {"doDeleteWorld", "editWorld", "recreateWorld"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/Minecraft;getLevelSource()Lnet/minecraft/world/level/storage/LevelStorageSource;"))
    private LevelStorageSource halfmasa$operationStorage(Minecraft client)
    {
        return this.halfmasa$storage == null ? client.getLevelSource() : this.halfmasa$storage;
    }

    @Redirect(method = {"joinWorld", "recreateWorld"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/Minecraft;createWorldOpenFlows()Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;"))
    private WorldOpenFlows halfmasa$openFlows(Minecraft client)
    {
        return this.halfmasa$storage == null ? client.createWorldOpenFlows() : new WorldOpenFlows(client, this.halfmasa$storage);
    }
}
//#endif

package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

public interface WorldListEntryStorage
{
    LevelStorageSource halfmasa$getStorage();
    void halfmasa$setStorage(LevelStorageSource storage);
    LevelSummary halfmasa$getSummary();

    static LevelSummary summary(WorldSelectionList.WorldListEntry row)
    {
        return ((WorldListEntryStorage) (Object) row).halfmasa$getSummary();
    }
}
//#endif

package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.CustomSavesPath;
import io.github.halfmasa.xaerobinding.feature.NewWorldSave;
import io.github.halfmasa.xaerobinding.feature.WorldCreationListAccess;
import io.github.halfmasa.xaerobinding.feature.WorldFolders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
//#if MC >= 26.1
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelDataAndDimensions.WorldDataAndGenSettings;
//#else
//$$ import net.minecraft.world.level.storage.WorldData;
//#endif
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen
{
    @Unique private static final ThreadLocal<Screen> halfmasa$origin = new ThreadLocal<>();
    @Shadow @Final private WorldCreationUiState uiState;
    @Shadow private Path tempDataPackDir;
    @Unique private Screen halfmasa$parent;
    @Unique private Path halfmasa$destination;
    @Unique private boolean halfmasa$keepDraft;

    protected CreateWorldScreenMixin(Component title) { super(title); }

    //#if MC >= 1.21.10
    @WrapMethod(method = "openFresh(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;)V")
    private static void halfmasa$captureOrigin(Minecraft client, Runnable onClose, Operation<Void> original)
    //#else
    //$$ @WrapMethod(method = "openFresh(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V")
    //$$ private static void halfmasa$captureOrigin(Minecraft client, Screen onClose, Operation<Void> original)
    //#endif
    {
        if (!Configs.BETTER_WORLD_SAVES.getBooleanValue())
        {
            original.call(client, onClose);
            return;
        }
        Screen previous = halfmasa$origin.get();
        halfmasa$origin.set(MinecraftClientCompat.getScreen(client));
        try { original.call(client, onClose); }
        finally
        {
            if (previous == null) halfmasa$origin.remove();
            else halfmasa$origin.set(previous);
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void halfmasa$initialPath(CallbackInfo ci)
    {
        this.halfmasa$parent = halfmasa$origin.get();
        if (this.halfmasa$parent == null) return;
        Path selected = CustomSavesPath.getCurrentPath();
        if (Configs.WORLD_SAVE_CREATE_CHOOSE_PATH.getBooleanValue() && this.halfmasa$parent instanceof SelectWorldScreen list)
        {
            var worldList = ((SelectWorldScreenAccessor) list).halfmasa$getWorldList();
            if (worldList != null) selected = ((WorldCreationListAccess) worldList).halfmasa$creationRoot();
        }
        if (!halfmasa$available(selected)) selected = CustomSavesPath.getCurrentPath();
        this.halfmasa$destination = selected;
        if (Configs.WORLD_SAVE_CREATE_CHOOSE_PATH.getBooleanValue() && halfmasa$available(selected))
            halfmasa$updatePreview(selected);
    }

    @WrapOperation(method = "init", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement halfmasa$addSavePathControl(HeaderAndFooterLayout layout, LayoutElement buttons, Operation<LayoutElement> original)
    {
        if (this.halfmasa$destination == null || !Configs.WORLD_SAVE_CREATE_CHOOSE_PATH.getBooleanValue())
            return original.call(layout, buttons);
        var choices = CustomSavesPath.getOptions().stream().filter(option -> option.available() && !option.invalid()).toList();
        if (choices.isEmpty()) return original.call(layout, buttons);
        var current = choices.stream().filter(option -> option.path().equals(this.halfmasa$destination))
                .findFirst().orElse(choices.getFirst());
        java.util.function.Function<CustomSavesPath.PathOption, Component> label = option -> {
            var name = option.path().getFileName();
            return option.defaultPath() ? Component.translatable("halfmasa.world_folders.default")
                    : Component.literal(this.font.plainSubstrByWidth(name == null ? option.path().toString() : name.toString(), 132));
        };
        //#if MC >= 1.21.11
        var builder = CycleButton.builder(label, current);
        //#else
        //$$ var builder = CycleButton.builder(label).withInitialValue(current);
        //#endif
        var selector = builder.withValues(choices).withTooltip(option -> Tooltip.create(Component.literal(option.path().toString())))
                .create(0, 0, Math.min(270, this.width - 16), 20, Component.translatable("halfmasa.world_folders.create_path"),
                        (widget, option) -> halfmasa$updatePreview(option.path()));
        var footer = LinearLayout.vertical().spacing(4);
        footer.defaultCellSetting().alignHorizontallyCenter();
        footer.addChild(selector);
        footer.addChild(buttons);
        layout.setFooterHeight(HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT + 24);
        original.call(layout, footer);
        return buttons;
    }

    @Unique private static boolean halfmasa$available(Path path)
    {
        return Files.isDirectory(path) && CustomSavesPath.getOptions().stream()
                .anyMatch(option -> option.available() && path.equals(option.path()));
    }

    @Unique private void halfmasa$updatePreview(Path path)
    {
        var access = (WorldCreationUiStateAccessor) this.uiState;
        try
        {
            access.halfmasa$setSavesFolder(path);
            this.uiState.setName(this.uiState.getName());
            this.halfmasa$destination = path;
        }
        catch (RuntimeException failure)
        {
            access.halfmasa$setSavesFolder(this.halfmasa$destination);
            XaeroWorldBinding.LOGGER.warn("Cannot use {} as a world creation destination", path, failure);
            SystemToast.onWorldAccessFailure(this.minecraft, path.toString());
        }
    }

    //#if MC >= 1.21.3
    @Inject(method = "createNewWorld", at = @At("HEAD"), cancellable = true)
    //#if MC >= 26.1
    private void halfmasa$createInDestination(LayeredRegistryAccess<RegistryLayer> registries, WorldDataAndGenSettings settings,
            Optional<GameRules> rules, CallbackInfoReturnable<Boolean> ci)
    //#else
    //$$ private void halfmasa$createInDestination(LayeredRegistryAccess<RegistryLayer> registries, WorldData settings,
    //$$         CallbackInfoReturnable<Boolean> ci)
    //#endif
    {
        if (this.halfmasa$destination == null) return;
        boolean choosePath = Configs.WORLD_SAVE_CREATE_CHOOSE_PATH.getBooleanValue();
        boolean saveOnly = Configs.WORLD_SAVE_CREATE_WITHOUT_JOIN.getBooleanValue();
        if (!choosePath && !saveOnly) return;
        Path destination = choosePath ? this.halfmasa$destination : CustomSavesPath.getCurrentPath();
        LevelStorageAccess access;
        try
        {
            if (!halfmasa$available(destination)) throw new IOException("Save location is no longer available");
            var storage = CustomSavesPath.createWorldListSource(destination, this.minecraft.getLevelSource());
            access = NewWorldSave.prepare(storage, this.uiState.getTargetFolder(), this.tempDataPackDir);
            //#if MC >= 26.1
            if (saveOnly) NewWorldSave.saveForLater(access, registries, settings, rules);
            //#else
            //$$ if (saveOnly) NewWorldSave.saveForLater(access, registries, settings);
            //#endif
        }
        catch (IOException | RuntimeException failure)
        {
            this.halfmasa$keepDraft = true;
            XaeroWorldBinding.LOGGER.warn("Could not create world in {}", destination, failure);
            MinecraftClientCompat.setScreen(this.minecraft, this);
            SystemToast.onWorldAccessFailure(this.minecraft, destination.toString());
            ci.setReturnValue(true);
            return;
        }
        if (saveOnly) this.halfmasa$returnToList(destination);
        //#if MC >= 26.1
        else new WorldOpenFlows(this.minecraft, access.parent()).createLevelFromExistingSettings(access,
                this.uiState.getSettings().dataPackResources(), registries, settings, rules);
        //#else
        //$$ else new WorldOpenFlows(this.minecraft, access.parent()).createLevelFromExistingSettings(access,
        //$$         this.uiState.getSettings().dataPackResources(), registries, settings);
        //#endif
        ci.setReturnValue(true);
    }

    @WrapOperation(method = "createWorldAndCleanup", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;removeTempDataPackDir()V"))
    private void halfmasa$preservePacksOnFailure(CreateWorldScreen screen, Operation<Void> original)
    {
        if (!this.halfmasa$keepDraft) original.call(screen);
        this.halfmasa$keepDraft = false;
    }
    //#else
    //$$ @Shadow private void removeTempDataPackDir() { throw new AssertionError(); }
    //$$
    //$$ @WrapOperation(method = "createNewWorld", at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;createNewWorldDirectory()Ljava/util/Optional;"))
    //$$ private Optional<LevelStorageAccess> halfmasa$prepareLegacy(CreateWorldScreen screen,
    //$$         Operation<Optional<LevelStorageAccess>> original)
    //$$ {
    //$$     if (this.halfmasa$destination == null) return original.call(screen);
    //$$     boolean choosePath = Configs.WORLD_SAVE_CREATE_CHOOSE_PATH.getBooleanValue();
    //$$     if (!choosePath && !Configs.WORLD_SAVE_CREATE_WITHOUT_JOIN.getBooleanValue()) return original.call(screen);
    //$$     Path destination = choosePath ? this.halfmasa$destination : CustomSavesPath.getCurrentPath();
    //$$     try {
    //$$         if (!halfmasa$available(destination)) throw new IOException("Save location is no longer available");
    //$$         return Optional.of(NewWorldSave.prepare(CustomSavesPath.createWorldListSource(destination,
    //$$                 this.minecraft.getLevelSource()), this.uiState.getTargetFolder(), this.tempDataPackDir));
    //$$     } catch (IOException | RuntimeException failure) {
    //$$         halfmasa$showLegacyFailure(destination, failure);
    //$$         return Optional.empty();
    //$$     }
    //$$ }
    //$$
    //$$ @WrapOperation(method = "createNewWorld", at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;removeTempDataPackDir()V"))
    //$$ private void halfmasa$keepLegacyDraft(CreateWorldScreen screen, Operation<Void> original)
    //$$ {
    //$$     if (!halfmasa$saveLegacyWithoutJoin()) original.call(screen);
    //$$ }
    //$$
    //$$ @WrapOperation(method = "createNewWorld", at = @At(value = "INVOKE", target =
    //$$         "Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;createLevelFromExistingSettings(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/ReloadableServerResources;Lnet/minecraft/core/LayeredRegistryAccess;Lnet/minecraft/world/level/storage/WorldData;)V"))
    //$$ private void halfmasa$chooseLegacyJoin(WorldOpenFlows flows, LevelStorageAccess access,
    //$$         net.minecraft.server.ReloadableServerResources resources, LayeredRegistryAccess<RegistryLayer> registries,
    //$$         WorldData settings, Operation<Void> original)
    //$$ {
    //$$     if (!halfmasa$saveLegacyWithoutJoin()) {
    //$$         original.call(flows, access, resources, registries, settings);
    //$$         return;
    //$$     }
    //$$     Path destination = access.getLevelPath(net.minecraft.world.level.storage.LevelResource.ROOT).getParent();
    //$$     try {
    //$$         NewWorldSave.saveForLater(access, registries, settings);
    //$$     } catch (IOException | RuntimeException failure) {
    //$$         halfmasa$showLegacyFailure(destination, failure);
    //$$         return;
    //$$     }
    //$$     removeTempDataPackDir();
    //$$     halfmasa$returnToList(destination);
    //$$ }
    //$$
    //$$ @Unique private boolean halfmasa$saveLegacyWithoutJoin()
    //$$ {
    //$$     return this.halfmasa$destination != null && Configs.WORLD_SAVE_CREATE_WITHOUT_JOIN.getBooleanValue();
    //$$ }
    //$$
    //$$ @Unique private void halfmasa$showLegacyFailure(Path destination, Exception failure)
    //$$ {
    //$$     XaeroWorldBinding.LOGGER.warn("Could not create world in {}", destination, failure);
    //$$     MinecraftClientCompat.setScreen(this.minecraft, this);
    //$$     SystemToast.onWorldAccessFailure(this.minecraft, destination.toString());
    //$$ }
    //#endif

    @Unique private void halfmasa$returnToList(Path path)
    {
        WorldFolders.getInstance().expandRoot(path);
        var listScreen = this.halfmasa$parent instanceof SelectWorldScreen previous
                ? previous : new SelectWorldScreen(this.halfmasa$parent);
        MinecraftClientCompat.setScreen(this.minecraft, listScreen);
        var access = (SelectWorldScreenAccessor) listScreen;
        access.halfmasa$getSearchBox().setValue("");
        ((WorldCreationListAccess) access.halfmasa$getWorldList()).halfmasa$showCreatedWorld(path);
    }
}
//#endif

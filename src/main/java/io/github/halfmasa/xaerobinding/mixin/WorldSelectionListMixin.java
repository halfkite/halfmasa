package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.halfmasa.xaerobinding.config.Configs;
//#if MC >= 1.21.1
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import com.mojang.blaze3d.platform.InputConstants;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.halfmasa.xaerobinding.feature.CustomSavesPath;
import io.github.halfmasa.xaerobinding.feature.WorldFolders;
import io.github.halfmasa.xaerobinding.feature.WorldListEntryStorage;
import io.github.halfmasa.xaerobinding.feature.WorldCreationListAccess;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.gui.PlayListStar;
import io.github.halfmasa.xaerobinding.gui.WorldTreeFolderRow;
//#endif

@Mixin(WorldSelectionList.class)
//#if MC >= 1.21.1
public abstract class WorldSelectionListMixin extends ObjectSelectionList<WorldSelectionList.Entry> implements WorldCreationListAccess
//#else
//$$ public abstract class WorldSelectionListMixin
//#endif
{
    //#if MC >= 1.21.1
    //#if MC >= 1.21.10
    @Shadow @Final private Screen screen;
    //#else
    //$$ @Shadow @Final private SelectWorldScreen screen;
    //#endif
    @Shadow private String filter;
    @Shadow private List<LevelSummary> currentlyDisplayedLevels;
    @Shadow private boolean filterAccepts(String query, LevelSummary summary) { throw new AssertionError(); }
    @Shadow private void notifyListUpdated() { throw new AssertionError(); }
    @Shadow public abstract void reloadWorldList();
    @Unique private final Map<Path, LevelStorageSource> halfmasa$storage = new HashMap<>();
    @Unique private final Map<Path, CompletableFuture<List<LevelSummary>>> halfmasa$scans = new HashMap<>();
    @Unique private boolean halfmasa$scanChanged;
    @Unique private Component halfmasa$dropMessage;
    @Unique private long halfmasa$dropMessageUntil;
    @Unique private Path halfmasa$lastFolderRoot;
    @Unique private Path halfmasa$shownCreationRoot;
    @Unique private WorldSelectionList.WorldListEntry halfmasa$pressedWorld;
    @Unique private double halfmasa$pressX, halfmasa$pressY;
    //#if MC >= 1.21.10
    @Unique private MouseButtonEvent halfmasa$press;
    @Unique private boolean halfmasa$doubleClick;
    //#endif
    @Unique private boolean halfmasa$dragging;

    protected WorldSelectionListMixin(Minecraft client, int width, int height, int top, int entryHeight)
    {
        super(client, width, height, top, entryHeight);
    }

    @Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
    private void halfmasa$starGutter(CallbackInfoReturnable<Integer> ci)
    {
        if (this.halfmasa$managed() && Configs.WORLD_SAVE_FAVORITES.getBooleanValue())
            ci.setReturnValue(Math.min(ci.getReturnValue(), Math.max(1, getWidth() - PlayListStar.GUTTER * 2)));
    }

    @Unique private boolean halfmasa$managed()
    {
        //#if MC >= 1.21.10
        return this.screen instanceof SelectWorldScreen && Configs.BETTER_WORLD_SAVES.getBooleanValue();
        //#else
        //$$ return Configs.BETTER_WORLD_SAVES.getBooleanValue();
        //#endif
    }

    @Unique private boolean halfmasa$treeEnabled()
    {
        return this.halfmasa$managed() && Configs.WORLD_SAVE_FOLDERS.getBooleanValue();
    }

    @Inject(method = "fillLevels", at = @At("HEAD"), cancellable = true)
    private void halfmasa$organize(String query, List<LevelSummary> levels, CallbackInfo ci)
    {
        if (!this.halfmasa$managed()) return;
        ci.cancel();
        halfmasa$rebuild(query, levels, null);
        notifyListUpdated();
    }

    @Inject(method = "handleNewLevels", at = @At("TAIL"))
    private void halfmasa$showEmptyTree(List<LevelSummary> levels, CallbackInfo ci)
    {
        if ((this.halfmasa$treeEnabled() || this.halfmasa$shownCreationRoot != null) && levels != null && levels.isEmpty())
            halfmasa$rebuild(this.filter, levels, null);
    }

    @Inject(method = "reloadWorldList", at = @At("HEAD"))
    private void halfmasa$invalidateSources(CallbackInfo ci)
    {
        if (this.halfmasa$managed()) this.halfmasa$scans.clear();
    }

    @Override public Path halfmasa$creationRoot()
    {
        var selected = halfmasa$key(getSelected());
        if (selected != null && selected.path() != null) return selected.path();
        if (this.halfmasa$shownCreationRoot != null) return this.halfmasa$shownCreationRoot;
        return this.halfmasa$lastFolderRoot == null ? CustomSavesPath.getCurrentPath() : this.halfmasa$lastFolderRoot;
    }

    @Override public void halfmasa$showCreatedWorld(Path path)
    {
        if (this.halfmasa$treeEnabled()) WorldFolders.getInstance().expandRoot(path);
        else this.halfmasa$shownCreationRoot = path;
        reloadWorldList();
    }

    @Unique private List<LevelSummary> halfmasa$levels(Path path, List<LevelSummary> currentLevels)
    {
        if (path.equals(CustomSavesPath.getCurrentPath())) return currentLevels;

        var scan = this.halfmasa$scans.get(path);
        if (scan != null) return scan.getNow(List.of());

        LevelStorageSource storage = halfmasa$source(path);
        var loading = CompletableFuture.supplyAsync(() -> {
            try { return storage.findLevelCandidates(); }
            catch (LevelStorageException exception) { throw new java.util.concurrent.CompletionException(exception); }
        }, Util.ioPool()).thenCompose(storage::loadLevelSummaries).exceptionally(failure -> {
            XaeroWorldBinding.LOGGER.warn("Unable to list worlds in {}", path, failure);
            return List.of();
        });
        this.halfmasa$scans.put(path, loading);
        loading.thenRun(() -> this.minecraft.execute(() -> {
            if (this.halfmasa$scans.get(path) == loading) this.halfmasa$scanChanged = true;
        }));
        return loading.getNow(List.of());
    }

    @Unique private LevelStorageSource halfmasa$source(Path path)
    {
        return this.halfmasa$storage.computeIfAbsent(path,
                root -> CustomSavesPath.createWorldListSource(root, this.minecraft.getLevelSource()));
    }

    @Unique private void halfmasa$rebuild(String query, List<LevelSummary> levels, WorldFolders.RowKey moved)
    {
        Path current = CustomSavesPath.getCurrentPath();
        var rows = new ArrayList<WorldSelectionList.Entry>();
        if (this.halfmasa$treeEnabled())
        {
            var store = WorldFolders.getInstance();
            for (var option : CustomSavesPath.getOptions())
            {
                Path path = option.path();
                boolean available = option.available() && !option.invalid();
                boolean expanded = available && store.rootExpanded(path);
                if (query.isBlank())
                {
                    String name = option.defaultPath() ? Component.translatable("halfmasa.world_folders.default").getString()
                            : option.invalid() || path.getFileName() == null
                                    ? Component.translatable("halfmasa.world_folders.root").getString() : path.getFileName().toString();
                    String note = option.invalid() ? Component.translatable("halfmasa.custom_saves.invalid").getString() : store.note(path);
                    String location = option.invalid() ? option.configuredValue() : path.toString();
                    rows.add(new WorldTreeFolderRow(path, null, Component.literal(name), halfmasa$details(note, location),
                            expanded, available ? () -> {
                                this.halfmasa$lastFolderRoot = path;
                                store.toggleRoot(path);
                                halfmasa$refreshView(null);
                            } : null));
                    if (!expanded) continue;
                    var worlds = halfmasa$worlds(path, query, halfmasa$levels(path, levels));
                    worlds.stream().filter(world -> store.folderOf(path, WorldListEntryStorage.summary(world).getLevelId()) == null)
                            .forEach(rows::add);
                    for (var folder : store.folders(path))
                    {
                        boolean open = store.isExpanded(folder.id());
                        rows.add(new WorldTreeFolderRow(path, folder.id(), Component.literal(folder.name()),
                                halfmasa$details(folder.note(), location + " › " + folder.name()), open, () -> {
                                    this.halfmasa$lastFolderRoot = path;
                                    store.toggleFolder(folder.id());
                                    halfmasa$refreshView(null);
                                }));
                        if (open) worlds.stream().filter(world -> folder.id().equals(store.folderOf(path,
                                WorldListEntryStorage.summary(world).getLevelId()))).forEach(rows::add);
                    }
                    worlds.stream().filter(world -> !rows.contains(world)).forEach(WorldSelectionList.WorldListEntry::close);
                }
                else if (available) rows.addAll(halfmasa$worlds(path, query, halfmasa$levels(path, levels)));
            }
        }
        else
        {
            Path path = this.halfmasa$shownCreationRoot == null ? current : this.halfmasa$shownCreationRoot;
            rows.addAll(halfmasa$worlds(path, query, halfmasa$levels(path, levels)));
        }
        this.halfmasa$replace(rows, moved);
    }

    @Unique private List<WorldSelectionList.WorldListEntry> halfmasa$worlds(Path path, String query, List<LevelSummary> levels)
    {
        var store = WorldFolders.getInstance();
        boolean favorites = Configs.WORLD_SAVE_FAVORITES.getBooleanValue();
        var order = new HashMap<String, Integer>();
        for (String world : store.order(path)) order.put(world, order.size());
        var list = (WorldSelectionList) (Object) this;
        return levels.stream().filter(level -> filterAccepts(query, level))
                .sorted(Comparator.comparing((LevelSummary level) -> favorites && !store.isFavorite(path, level.getLevelId()))
                        .thenComparingInt(level -> order.getOrDefault(level.getLevelId(), Integer.MAX_VALUE)))
                .map(level -> {
                    //#if MC >= 1.21.11 || MC < 1.21.10
                    var row = list.new WorldListEntry(list, level);
                    //#else
                    //$$ var row = new WorldSelectionList.WorldListEntry(list, level);
                    //#endif
                    ((WorldListEntryStorage) (Object) row).halfmasa$setStorage(halfmasa$source(path));
                    return row;
                }).toList();
    }

    @Unique private static WorldFolders.RowKey halfmasa$key(WorldSelectionList.Entry row)
    {
        if (row instanceof WorldTreeFolderRow folder) return new WorldFolders.RowKey(folder.root(), folder.folder(), true);
        if (row instanceof WorldSelectionList.WorldListEntry world)
        {
            var storage = ((WorldListEntryStorage) (Object) world).halfmasa$getStorage();
            if (storage != null) return new WorldFolders.RowKey(storage.getBaseDir(), WorldListEntryStorage.summary(world).getLevelId(), false);
        }
        return null;
    }

    @Unique private static String halfmasa$details(String note, String location)
    {
        if (!Configs.WORLD_SAVE_SHOW_PATHS.getBooleanValue()) return note;
        return note.isBlank() ? location : note + " · " + location;
    }

    @Unique private void halfmasa$refreshView(WorldFolders.RowKey moved)
    {
        if (this.currentlyDisplayedLevels != null) halfmasa$rebuild(this.filter, this.currentlyDisplayedLevels, moved);
    }

    @Unique private double halfmasa$scroll()
    {
        //#if MC >= 1.21.4
        return scrollAmount();
        //#else
        //$$ return getScrollAmount();
        //#endif
    }

    @Unique private void halfmasa$replace(List<WorldSelectionList.Entry> rows, WorldFolders.RowKey moved)
    {
        double scroll = halfmasa$scroll();
        var previousSelection = getSelected();
        var selected = halfmasa$key(previousSelection);
        // Keep the first surviving visible row as an anchor. The row being dragged can move offscreen.
        var positioned = new HashMap<WorldFolders.RowKey, WorldSelectionList.Entry>();
        for (var row : rows) positioned.put(halfmasa$key(row), row);
        WorldSelectionList.Entry anchor = null;
        int anchorY = 0;
        for (var oldRow : children())
        {
            var key = halfmasa$key(oldRow);
            if (key == null || key.equals(moved) || halfmasa$rowBottom(oldRow) <= getY()
                    || halfmasa$rowTop(oldRow) >= getBottom()) continue;
            anchor = positioned.get(key);
            if (anchor == null) continue;
            anchorY = halfmasa$rowTop(oldRow);
            break;
        }
        replaceEntries(rows);
        if (previousSelection != null) setSelected(selected == null ? null : positioned.get(selected));
        setScrollAmount(scroll);
        if (anchor != null) setScrollAmount(halfmasa$scroll() + halfmasa$rowTop(anchor) - anchorY);
    }

    //#if MC >= 26.1
    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void halfmasa$refreshScannedRoots(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "renderWidget", at = @At("HEAD"))
    //$$ private void halfmasa$refreshScannedRoots(GuiGraphics graphics, int x, int y, float delta, CallbackInfo ci)
    //#endif
    {
        if (!this.halfmasa$managed() || !this.halfmasa$scanChanged || this.halfmasa$pressedWorld != null) return;
        this.halfmasa$scanChanged = false;
        halfmasa$refreshView(null);
    }

    //#if MC >= 26.1
    @Inject(method = "extractWidgetRenderState", at = @At("TAIL"))
    private void halfmasa$drawWorldControls(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "renderWidget", at = @At("TAIL"))
    //$$ private void halfmasa$drawWorldControls(GuiGraphics graphics, int x, int y, float delta, CallbackInfo ci)
    //#endif
    {
        if (!this.halfmasa$managed()) return;
        if (this.halfmasa$dropMessage != null && Util.getMillis() < this.halfmasa$dropMessageUntil)
        {
            int center = getX() + getWidth() / 2;
            int halfWidth = this.minecraft.font.width(this.halfmasa$dropMessage) / 2 + 6;
            int baseline = getBottom() - 14;
            graphics.fill(center - halfWidth, baseline - 3, center + halfWidth, baseline + 11, 0xE0201919);
            //#if MC >= 26.1
            graphics.centeredText(this.minecraft.font, this.halfmasa$dropMessage, center, baseline, 0xFFFFB6AC);
            //#else
            //$$ graphics.drawCenteredString(this.minecraft.font, this.halfmasa$dropMessage, center, baseline, 0xFFFFB6AC);
            //#endif
        }
        if (Configs.WORLD_SAVE_FAVORITES.getBooleanValue())
        {
            var store = WorldFolders.getInstance();
            graphics.enableScissor(getX(), getY(), getRight(), getBottom());
            for (var entry : children())
            {
                if (entry instanceof WorldSelectionList.WorldListEntry world && halfmasa$rowContentY(world) >= getY()
                        && halfmasa$rowContentY(world) < getBottom())
                {
                    var key = halfmasa$key(world);
                    if (key != null) PlayListStar.draw(graphics, halfmasa$rowLeft(world), halfmasa$rowContentY(world),
                            store.isFavorite(key.path(), key.id()), x, y);
                }
            }
            graphics.disableScissor();
        }
        if (this.halfmasa$dragging)
        {
            var target = isMouseOver(x, y) ? getEntryAtPosition(x, y) : null;
            var targetKey = halfmasa$key(target);
            var moved = halfmasa$key(this.halfmasa$pressedWorld);
            if (targetKey != null && moved != null && moved.path().equals(targetKey.path()) && !moved.equals(targetKey))
            {
                int line = !targetKey.folder() && halfmasa$dropAfter(target, y)
                        ? halfmasa$rowBottom(target) - 2 : halfmasa$rowTop(target);
                graphics.fill(halfmasa$rowLeft(target), line, halfmasa$rowRight(target), line + 2, 0xFFFFD080);
            }
            //#if MC >= 26.1
            graphics.text(this.minecraft.font, "→ " + WorldListEntryStorage.summary(this.halfmasa$pressedWorld).getLevelName(),
            //#else
            //$$ graphics.drawString(this.minecraft.font, "→ " + WorldListEntryStorage.summary(this.halfmasa$pressedWorld).getLevelName(),
            //#endif
                    Math.max(0, Math.min(x + 10, getRight() - 110)), Math.min(y + 6, getBottom() - 10), 0xFFFFD080);
        }
    }

    //#if MC >= 1.21.10
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (!halfmasa$mouseDown(event.x(), event.y(), event.button())) return super.mouseClicked(event, doubleClick);
        this.halfmasa$press = event;
        this.halfmasa$doubleClick = doubleClick;
        return true;
    }
    //#else
    //$$ @Override public boolean mouseClicked(double x, double y, int button) {
    //$$     return halfmasa$mouseDown(x, y, button) || super.mouseClicked(x, y, button);
    //$$ }
    //#endif

    @Unique private boolean halfmasa$mouseDown(double x, double y, int button)
    {
        if (!this.halfmasa$managed() || button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (Configs.WORLD_SAVE_FAVORITES.getBooleanValue() && x < getRowLeft() && isMouseOver(x, y))
        {
            var row = getEntryAtPosition(getRowLeft() + 2, y);
            if (row instanceof WorldSelectionList.WorldListEntry world && PlayListStar.hits(halfmasa$rowLeft(row), halfmasa$rowContentY(row), x, y))
            {
                var key = halfmasa$key(world);
                if (key != null && WorldFolders.getInstance().toggleFavorite(key.path(), key.id()))
                    halfmasa$refreshView(key);
                return true;
            }
        }
        var row = getEntryAtPosition(x, y);
        if (Configs.WORLD_SAVE_DRAG.getBooleanValue() && this.filter.isBlank() && isMouseOver(x, y)
                && row instanceof WorldSelectionList.WorldListEntry world && x >= halfmasa$rowContentX(world) + 32)
        {
            this.halfmasa$pressedWorld = world;
            this.halfmasa$pressX = x;
            this.halfmasa$pressY = y;
            setSelected(world);
            return true;
        }
        return false;
    }

    //#if MC >= 1.21.10
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
    {
        if (this.halfmasa$pressedWorld == null || event.button() != InputConstants.MOUSE_BUTTON_LEFT)
            return super.mouseDragged(event, dx, dy);
        return halfmasa$drag(event.x(), event.y());
    }
    //#else
    //$$ @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
    //$$     if (this.halfmasa$pressedWorld == null || button != InputConstants.MOUSE_BUTTON_LEFT)
    //$$         return super.mouseDragged(x, y, button, dx, dy);
    //$$     return halfmasa$drag(x, y);
    //$$ }
    //#endif

    @Unique private boolean halfmasa$drag(double x, double y)
    {
        this.halfmasa$dragging |= Math.hypot(x - this.halfmasa$pressX, y - this.halfmasa$pressY) >= 5;
        if (this.halfmasa$dragging)
        {
            if (y < getY() + 12) setScrollAmount(halfmasa$scroll() - 8);
            else if (y > getBottom() - 12) setScrollAmount(halfmasa$scroll() + 8);
        }
        return true;
    }

    //#if MC >= 1.21.10
    @Override public boolean mouseReleased(MouseButtonEvent event)
    {
        if (this.halfmasa$pressedWorld == null || event.button() != InputConstants.MOUSE_BUTTON_LEFT)
            return super.mouseReleased(event);
        if (halfmasa$release(event.x(), event.y())) {
            super.mouseClicked(this.halfmasa$press, this.halfmasa$doubleClick);
            super.mouseReleased(event);
        }
        return true;
    }
    //#else
    //$$ @Override public boolean mouseReleased(double x, double y, int button) {
    //$$     if (this.halfmasa$pressedWorld == null || button != InputConstants.MOUSE_BUTTON_LEFT)
    //$$         return super.mouseReleased(x, y, button);
    //$$     if (halfmasa$release(x, y)) {
    //$$         super.mouseClicked(this.halfmasa$pressX, this.halfmasa$pressY, button);
    //$$         super.mouseReleased(x, y, button);
    //$$     }
    //$$     return true;
    //$$ }
    //#endif

    @Unique private boolean halfmasa$release(double x, double y)
    {
        var world = this.halfmasa$pressedWorld;
        boolean dragged = this.halfmasa$dragging;
        this.halfmasa$pressedWorld = null;
        this.halfmasa$dragging = false;
        var target = getEntryAtPosition(x, y);
        if (dragged)
        {
            if (isMouseOver(x, y)) this.halfmasa$dropWorld(world, target, y);
        }
        return !dragged && target == world;
    }

    //#if MC >= 1.21.10
    @Unique private int halfmasa$rowLeft(WorldSelectionList.Entry row) { return row.getX(); }
    @Unique private int halfmasa$rowRight(WorldSelectionList.Entry row) { return row.getX() + row.getWidth(); }
    @Unique private int halfmasa$rowTop(WorldSelectionList.Entry row) { return row.getY(); }
    @Unique private int halfmasa$rowBottom(WorldSelectionList.Entry row) { return row.getY() + row.getHeight(); }
    @Unique private int halfmasa$rowContentX(WorldSelectionList.Entry row) { return row.getContentX(); }
    @Unique private int halfmasa$rowContentY(WorldSelectionList.Entry row) { return row.getContentY(); }
    //#else
    //$$ @Unique private int halfmasa$rowLeft(WorldSelectionList.Entry row) { return getRowLeft(); }
    //$$ @Unique private int halfmasa$rowRight(WorldSelectionList.Entry row) { return getRowLeft() + getRowWidth(); }
    //$$ @Unique private int halfmasa$rowTop(WorldSelectionList.Entry row) { return getRowTop(children().indexOf(row)); }
    //$$ @Unique private int halfmasa$rowBottom(WorldSelectionList.Entry row) { return getRowBottom(children().indexOf(row)); }
    //$$ @Unique private int halfmasa$rowContentX(WorldSelectionList.Entry row) { return halfmasa$rowLeft(row); }
    //$$ @Unique private int halfmasa$rowContentY(WorldSelectionList.Entry row) { return halfmasa$rowTop(row); }
    //#endif

    @Unique private boolean halfmasa$dropAfter(WorldSelectionList.Entry target, double y)
    {
        int top = halfmasa$rowTop(target);
        return y >= top + (halfmasa$rowBottom(target) - top) / 2.0;
    }

    @Unique private void halfmasa$dropWorld(WorldSelectionList.WorldListEntry world, WorldSelectionList.Entry target, double y)
    {
        var moved = halfmasa$key(world);
        var destination = halfmasa$key(target);
        if (moved == null || destination == null || moved.equals(destination) || this.currentlyDisplayedLevels == null) return;
        var path = moved.path();
        if (!path.equals(destination.path()))
        {
            if (destination.path() != null)
            {
                this.halfmasa$dropMessage = Component.translatable("halfmasa.world_folders.cross_path_drop");
                this.halfmasa$dropMessageUntil = Util.getMillis() + 2600;
            }
            return;
        }
        var store = WorldFolders.getInstance();
        String folder;
        String before = null;
        if (destination.folder())
        {
            folder = destination.id();
        }
        else
        {
            boolean grouped = halfmasa$treeEnabled();
            before = destination.id();
            folder = grouped ? store.folderOf(path, before) : store.folderOf(path, moved.id());
            if (halfmasa$dropAfter(target, y))
            {
                String targetFolder = folder;
                before = children().stream().skip(children().indexOf(target) + 1L)
                        .map(entry -> halfmasa$key(entry))
                        .filter(key -> key != null && !key.folder() && path.equals(key.path()))
                        .map(WorldFolders.RowKey::id)
                        .filter(id -> !grouped || Objects.equals(targetFolder, store.folderOf(path, id)))
                        .findFirst().orElse(null);
            }
        }
        if (store.move(path, moved.id(), folder, before,
                halfmasa$levels(path, this.currentlyDisplayedLevels).stream().map(LevelSummary::getLevelId).toList()))
            halfmasa$refreshView(moved);
    }
    //#endif
    //#if MC >= 1.21.10
    @Redirect(
            //#if MC >= 1.21.1
            method = "handleNewLevels",
            //#else
            //$$ method = "loadLevels",
            //#endif
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openFresh(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;)V"),
            require = 0)
    private void halfmasa$keepWorldSelectionOnEmpty(Minecraft minecraft, Runnable callback)
    //#else
    //$$ @Redirect(
    //$$         method = "loadLevels",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openFresh(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V"),
    //$$         require = 0)
    //$$ private void halfmasa$keepWorldSelectionOnEmpty(Minecraft minecraft, Screen callback)
    //#endif
    {
        //#if MC >= 1.21.1
        boolean keepOpen = this.halfmasa$managed() && (this.halfmasa$shownCreationRoot != null || Configs.KEEP_WORLD_SELECTION_ON_EMPTY.getBooleanValue()
                || this.halfmasa$treeEnabled() && CustomSavesPath.getOptions().stream()
                        .anyMatch(option -> option.available() && !option.invalid()
                                && !option.path().equals(CustomSavesPath.getCurrentPath())));
        //#else
        //$$ boolean keepOpen = Configs.KEEP_WORLD_SELECTION_ON_EMPTY.getBooleanValue();
        //#endif
        if (!keepOpen)
        {
            CreateWorldScreen.openFresh(minecraft, callback);
        }
    }
}

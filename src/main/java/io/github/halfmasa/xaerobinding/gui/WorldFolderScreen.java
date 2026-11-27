package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiPredicate;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.WorldFolders;
import io.github.halfmasa.xaerobinding.mixin.SelectWorldScreenAccessor;

public final class WorldFolderScreen extends Screen
{
    private final SelectWorldScreen parent;
    private final Path root;
    private final WorldFolders store = WorldFolders.getInstance();
    private final String world;
    private int page;
    private boolean error;

    public WorldFolderScreen(SelectWorldScreen parent, Path root, String world)
    {
        super(Component.translatable("halfmasa.world_folders.title"));
        this.parent = parent;
        this.root = root;
        this.world = world;
    }

    @Override protected void init()
    {
        int rowWidth = Math.min(width - 16, 380);
        int x = (width - rowWidth) / 2;
        int perPage = Math.max(1, (height - 108) / 24);
        List<WorldFolders.Folder> folders = this.store.folders(this.root);
        int pages = Math.max(1, (folders.size() + perPage - 1) / perPage);
        this.page = Math.min(this.page, pages - 1);
        addFolderRow(null, x, 33, rowWidth);
        for (int i = this.page * perPage; i < Math.min(folders.size(), (this.page + 1) * perPage); i++)
        {
            addFolderRow(folders.get(i), x, 57 + (i - this.page * perPage) * 24, rowWidth);
        }
        button(Component.translatable("halfmasa.world_folders.new"), x, height - 46, rowWidth / 3 - 2,
                () -> editFolder(null));
        button(Component.translatable("halfmasa.multiplayer.page", this.page + 1, pages),
                x + rowWidth / 3, height - 46, rowWidth / 3 - 2,
                () -> { this.page = (this.page + 1) % pages; rebuildWidgets(); }).active = pages > 1;
        button(CommonComponents.GUI_BACK, x + 2 * rowWidth / 3, height - 46, rowWidth / 3, this::onClose);
    }

    private void addFolderRow(WorldFolders.Folder folder, int x, int y, int width)
    {
        String id = folder == null ? null : folder.id();
        Component name = folder == null ? Component.translatable("halfmasa.world_folders.ungrouped")
                : Component.literal(folder.name());
        String detail = folder == null ? this.store.note(this.root) : folder.note();
        if (Configs.WORLD_SAVE_SHOW_PATHS.getBooleanValue())
        {
            String location = this.root.toString() + (folder == null ? "" : " › " + folder.name());
            detail = detail.isBlank() ? location : detail + " · " + location;
        }
        String label = detail.isBlank() ? name.getString() : name.getString() + "   " + detail;
        int openWidth = width - 128;
        Button open = button(Component.literal(font.plainSubstrByWidth(label, openWidth - 10)),
                x, y, openWidth, () -> open(id));
        open.setTooltip(Tooltip.create(Component.literal(label)));
        Button move = button(Component.translatable("halfmasa.world_folders.assign"), x + openWidth + 2,
                y, 62, () -> {
                    if (this.store.assign(this.root, this.world, id)) open(id);
                    else this.error = true;
                });
        move.active = this.world != null;
        Button edit = button(Component.literal("✎"), x + openWidth + 66, y, 28,
                () -> { if (folder == null) editRoot(this, this.root); else editFolder(folder); });
        edit.setTooltip(Tooltip.create(Component.translatable("halfmasa.world_folders.edit")));
        if (folder != null)
        {
            Button remove = button(Component.literal("−"), x + openWidth + 96, y, 32, () ->
                    MinecraftClientCompat.setScreen(this.minecraft, new ConfirmScreen(approved -> {
                        if (approved && !this.store.removeFolder(this.root, folder.id())) this.error = true;
                        MinecraftClientCompat.setScreen(this.minecraft, this);
                    }, Component.translatable("halfmasa.world_folders.delete"),
                            Component.translatable("halfmasa.world_folders.delete_explanation", folder.name()))));
            remove.setTooltip(Tooltip.create(Component.translatable("halfmasa.world_folders.delete")));
        }
    }

    private Button button(Component name, int x, int y, int width, Runnable action)
    {
        return addRenderableWidget(Button.builder(name, button -> action.run()).bounds(x, y, width, 20).build());
    }

    private void open(String folder)
    {
        this.store.select(this.root, folder);
        onClose();
    }

    private void editFolder(WorldFolders.Folder folder)
    {
        MinecraftClientCompat.setScreen(this.minecraft, new DetailsScreen(this,
                folder == null ? "" : folder.name(), folder == null ? "" : folder.note(),
                (name, note) -> this.store.saveFolder(this.root, folder == null ? null : folder.id(), name, note)));
    }

    public static void editRoot(Screen parent, Path path)
    {
        var store = WorldFolders.getInstance();
        MinecraftClientCompat.setScreen(net.minecraft.client.Minecraft.getInstance(),
                new DetailsScreen(parent, null, store.note(path), (name, note) -> store.setNote(path, note)));
    }

    @Override public void onClose()
    {
        var list = ((SelectWorldScreenAccessor) this.parent).halfmasa$getWorldList();
        WorldFolders.refresh(list);
        MinecraftClientCompat.setScreen(this.minecraft, this.parent);
    }

    //#if MC >= 26.1
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    //#else
    //$$ @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //#endif
    {
        //#if MC >= 26.1
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        //#else
        //$$ super.render(graphics, mouseX, mouseY, partialTick);
        //#endif
        //#if MC >= 26.1
        graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
        //#else
        //$$ graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
        //#endif
        //#if MC >= 26.1
        if (this.error) graphics.centeredText(font, Component.translatable("halfmasa.world_folders.save_failed"),
        //#else
        //$$ if (this.error) graphics.drawCenteredString(font, Component.translatable("halfmasa.world_folders.save_failed"),
        //#endif
                width / 2, height - 16, 0xFFFF6666);
    }

    private static final class DetailsScreen extends Screen
    {
        private final Screen parent;
        private final BiPredicate<String, String> save;
        private String name;
        private String note;
        private boolean error;

        private DetailsScreen(Screen parent, String name, String note, BiPredicate<String, String> save)
        {
            super(Component.translatable("halfmasa.world_folders.edit"));
            this.parent = parent;
            this.name = name;
            this.note = note;
            this.save = save;
        }

        @Override protected void init()
        {
            int w = Math.min(300, width - 16), x = (width - w) / 2;
            Button done = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
                if (this.save.test(this.name, this.note)) onClose();
                else this.error = true;
            }).bounds(x, 112, w / 2 - 2, 20).build());
            done.active = this.name == null || !this.name.isBlank();
            if (this.name != null)
            {
                var field = field("halfmasa.world_folders.name", x, 43, w, this.name, value -> {
                    this.name = value;
                    done.active = !value.isBlank();
                });
                setInitialFocus(field);
            }
            var noteBox = field("halfmasa.world_folders.note", x, 78, w, this.note, value -> this.note = value);
            if (this.name == null) setInitialFocus(noteBox);
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                    .bounds(x + w / 2 + 2, 112, w / 2 - 2, 20).build());
        }

        private EditBox field(String key, int x, int y, int width, String value, java.util.function.Consumer<String> onChange)
        {
            var label = Component.translatable(key);
            var input = addRenderableWidget(new EditBox(font, x, y, width, 20, label));
            input.setMaxLength(128);
            input.setHint(label);
            input.setValue(value);
            input.setResponder(onChange);
            return input;
        }

        //#if MC >= 26.1
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
        //#else
        //$$ @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        //#endif
        {
            //#if MC >= 26.1
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            //#else
            //$$ super.render(graphics, mouseX, mouseY, partialTick);
            //#endif
            //#if MC >= 26.1
            graphics.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
            //#else
            //$$ graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFFFF);
            //#endif
            //#if MC >= 26.1
            if (this.error) graphics.centeredText(font, Component.translatable("halfmasa.world_folders.save_failed"),
            //#else
            //$$ if (this.error) graphics.drawCenteredString(font, Component.translatable("halfmasa.world_folders.save_failed"),
            //#endif
                    width / 2, 141, 0xFFFF6666);
        }

        @Override public void onClose() { MinecraftClientCompat.setScreen(this.minecraft, this.parent); }
    }
}
//#endif

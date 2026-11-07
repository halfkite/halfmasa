package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

import java.util.*;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;

/** Keyboard-accessible context menu and paginated move/relink dialogs. */
public final class ServerFolderScreen extends Screen {
    private final Screen parentScreen;
    private final String entryId;
    private final String createParent;
    private final ServerFolderManager manager = ServerFolderManager.getInstance();
    private int page;
    private String mode = "edit";
    private EditBox name;
    public ServerFolderScreen(Screen parent, String entryId, String createParent) {
        super(Component.translatable(entryId == null ? "halfmasa.multiplayer.new_folder" : "halfmasa.multiplayer.manage"));
        parentScreen = parent; this.entryId = entryId; this.createParent = createParent;
    }
    private void button(String key, int y, Runnable action) { button(Component.translatable(key), y, action); }
    private void button(Component label, int y, Runnable action) {
        addRenderableWidget(Button.builder(label, b -> action.run()).bounds(width / 2 - 140, y, 280, 20).build());
    }
    @Override protected void init() {
        int y = 48;
        if (mode.equals("move") || mode.equals("relink")) {
            List<String> choices = new ArrayList<>();
            if (mode.equals("move")) {
                choices.add(null);
                manager.folders().stream().filter(f -> !f.id.equals(entryId) && !manager.isDescendant(f.id, entryId))
                        .sorted(Comparator.comparing(f -> manager.path(f.id) + "/" + f.name)).forEach(f -> choices.add(f.id));
            } else manager.unresolved().forEach(a -> choices.add(a.entryId));
            int perPage = Math.max(1, (height - 125) / 24);
            int pages = Math.max(1, (choices.size() + perPage - 1) / perPage); page = Math.min(page, pages - 1);
            for (int i = page * perPage; i < Math.min(choices.size(), (page + 1) * perPage); i++) {
                String id = choices.get(i);
                String label = id == null ? "/" : mode.equals("move") ? manager.path(id) + "/" + manager.folder(id).name
                        : manager.path(id) + "/" + manager.assignment(id).lastName;
                button(Component.literal(font.plainSubstrByWidth(label, 260)), y, () -> {
                    if (mode.equals("move")) manager.move(entryId, id, null); else manager.relink(entryId, id);
                    onClose();
                }); y += 24;
            }
            final int totalPages = pages;
            button(Component.translatable("halfmasa.multiplayer.page", page + 1, pages), height - 69,
                    () -> { page = (page + 1) % totalPages; rebuildWidgets(); });
            button("gui.back", height - 44, () -> { mode = "edit"; page = 0; rebuildWidgets(); });
            return;
        }
        ServerFolder folder = manager.folder(entryId);
        if (entryId == null || folder != null) {
            name = addRenderableWidget(new EditBox(font, width / 2 - 140, y, 280, 20, Component.translatable("halfmasa.multiplayer.folder_name")));
            name.setMaxLength(128); name.setValue(folder == null ? "" : folder.name); setInitialFocus(name); y += 26;
            Button save = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
                if (folder == null) manager.create(name.getValue(), createParent); else manager.rename(entryId, name.getValue()); onClose();
            }).bounds(width / 2 - 140, y, 280, 20).build());
            save.active = !name.getValue().isBlank(); name.setResponder(value -> save.active = !value.isBlank()); y += 26;
        }
        if (entryId != null) {
            button("halfmasa.multiplayer.move", y, () -> { mode = "move"; rebuildWidgets(); }); y += 26;
            if (folder != null) {
                button("halfmasa.multiplayer.new_folder", y, () -> minecraft.setScreenAndShow(new ServerFolderScreen(this, null, entryId))); y += 26;
                button("halfmasa.multiplayer.delete_folder", y, () -> minecraft.setScreenAndShow(new ConfirmScreen(confirmed -> {
                    if (confirmed) manager.delete(entryId); minecraft.setScreenAndShow(parentScreen);
                }, Component.translatable("halfmasa.multiplayer.delete_folder"), Component.translatable("halfmasa.multiplayer.delete_explanation"))));
            } else if (!manager.unresolved().isEmpty()) {
                button("halfmasa.multiplayer.relink", y, () -> { mode = "relink"; rebuildWidgets(); });
            }
        }
        button("gui.cancel", height - 40, this::onClose);
    }
    //#if MC >= 26.1
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    //#else
    //$$ @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
    //#endif
        //#if MC >= 26.1
        super.extractRenderState(g, mouseX, mouseY, delta);
        //#else
        //$$ super.render(g, mouseX, mouseY, delta);
        //#endif
        //#if MC >= 26.1
        g.centeredText(font, title, width / 2, 20, 0xffffffff);
        //#else
        //$$ g.drawCenteredString(font, title, width / 2, 20, 0xffffffff);
        //#endif
        //#if MC >= 26.1
        if (!manager.error().isEmpty()) g.centeredText(font, Component.translatable(manager.error()), width / 2, height - 59, 0xffff5555);
        //#else
        //$$ if (!manager.error().isEmpty()) g.drawCenteredString(font, Component.translatable(manager.error()), width / 2, height - 59, 0xffff5555);
        //#endif
    }
    @Override public void onClose() { minecraft.setScreenAndShow(parentScreen); }
}

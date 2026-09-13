package io.github.halfmasa.xaerobinding.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.CustomConfigGroupStore;
import io.github.halfmasa.xaerobinding.feature.CustomConfigSources;
import io.github.halfmasa.xaerobinding.feature.ConfigScrollMemory;

/** Editor for user-defined groups in the halfmasa configuration screen. */
public final class CustomConfigGroupScreen extends GuiBase
{
    private static final int LIST_TOP = 66;
    private static final int ROW_HEIGHT = 24;
    private static final int BOTTOM_MARGIN = 30;
    private static final int HANDLE_X = 10;
    private static final int HANDLE_WIDTH = 18;

    private final Screen parent;
    private final CustomConfigGroupStore store = CustomConfigGroupStore.getInstance();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, String> configLabels = new HashMap<>();
    private String sourceFilter = CustomConfigSources.HALF_MASA;
    private GuiTextFieldGeneric newGroupName;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private String draggingGroupId;
    private String draggingChildGroupId;
    private String draggingChildName;

    public CustomConfigGroupScreen(Screen parent)
    {
        this.parent = parent;
        this.sourceFilter = ConfigScrollMemory.restoreCustomGroupSource();
        this.scrollOffset = ConfigScrollMemory.restoreCustomGroupEditor(this.sourceFilter);
        this.setTitle(StringUtils.translate("halfmasa.gui.custom_groups.title"));
    }

    public static void openGroupPicker(
            Minecraft minecraft, Screen parent, String source, String configName)
    {
        if (parent != null)
        {
            MinecraftClientCompat.setScreen(minecraft,
                    new GroupTargetChoiceScreen(parent, source, configName));
        }
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearElements();
        this.rows.clear();
        this.configLabels.clear();
        ensureDefaultGroups(this.store, this.sourceFilter);
        for (CustomConfigSources.Candidate candidate : CustomConfigSources.getCandidates(this.sourceFilter))
        {
            this.configLabels.put(candidate.reference(), candidate.config().getConfigGuiDisplayName());
        }
        this.rebuildRows();

        this.newGroupName = new GuiTextFieldGeneric(10, 28,
                Math.min(240, Math.max(120, this.getScreenWidth() - 140)), 20, this.mc.font);
        this.newGroupName.setMaxLengthWrapper(128);
        this.addTextField(this.newGroupName, field -> true);
        this.addSourceButton(360, CustomConfigSources.HALF_MASA, "halfmasa.gui.custom_groups.source_halfmasa");
        ButtonGeneric tweakerooSource = this.addSourceButton(450, CustomConfigSources.TWEAKEROO,
                "halfmasa.gui.custom_groups.source_tweakeroo");
        tweakerooSource.setEnabled(CustomConfigSources.isTweakerooAvailable());
        this.addButton(new ButtonGeneric(256, 28, 92, 20,
                StringUtils.translate("halfmasa.gui.custom_groups.create")),
                (button, mouseButton) -> this.createGroup());

        int listBottom = this.getScreenHeight() - BOTTOM_MARGIN;
        for (int index = 0; index < this.rows.size(); index++)
        {
            Row row = this.rows.get(index);
            int y = LIST_TOP + index * ROW_HEIGHT - this.scrollOffset;
            if (y + ROW_HEIGHT < LIST_TOP || y > listBottom)
            {
                continue;
            }
            if (row.isGroup())
            {
                this.addGroupControls(row.groupId(), y);
            }
            else
            {
                this.addChildControls(row.groupId(), row.childName(), y);
            }
        }

        this.addButton(new ButtonGeneric(10, this.getScreenHeight() - 26, 82, 20,
                StringUtils.translate("halfmasa.gui.custom_groups.done")),
                (button, mouseButton) -> this.onClose());
    }

    private ButtonGeneric addSourceButton(int x, String source, String translationKey)
    {
        ButtonGeneric button = new ButtonGeneric(x, 28, 84, 20, StringUtils.translate(translationKey));
        button.setEnabled(!source.equals(this.sourceFilter));
        this.addButton(button, (clicked, mouseButton) -> {
            ConfigScrollMemory.saveCustomGroupEditor(this.sourceFilter, this.scrollOffset);
            this.sourceFilter = source;
            this.scrollOffset = ConfigScrollMemory.restoreCustomGroupEditor(source);
            this.initGui();
        });
        return button;
    }

    private static void ensureDefaultGroups(CustomConfigGroupStore store, String source)
    {
        if (CustomConfigSources.TWEAKEROO.equals(source))
        {
            if (CustomConfigSources.isTweakerooAvailable())
            {
                store.ensureDefaults(source, TweakerooConfigExpansionProvider.getBuiltInGroupTemplates());
            }
            return;
        }
        store.ensureDefaults(source, HalfMasaConfigExpansionProvider.getBuiltInGroupTemplates());
    }

    private void addGroupControls(String groupId, int y)
    {
        int right = this.getScreenWidth() - 10;
        int downX = right - 18;
        int upX = downX - 22;
        int minusX = upX - 22;
        int plusX = minusX - 22;
        int mainX = Math.max(220, plusX - 164);
        int nameWidth = Math.max(100, mainX - 38);

        CustomConfigGroupStore.Group group = this.findGroup(groupId);
        GuiTextFieldGeneric name = new GuiTextFieldGeneric(32, y + 2, nameWidth, 20, this.mc.font);
        name.setTextWrapper(group == null ? "" : group.name);
        name.setMaxLengthWrapper(128);
        this.addTextField(name, field -> {
            String value = field.getTextWrapper().trim();
            if (!value.isEmpty())
            {
                this.store.rename(groupId, value);
            }
            return true;
        });

        String mainName = group == null || group.mainConfig == null || group.mainConfig.isBlank()
                ? StringUtils.translate("halfmasa.gui.custom_groups.no_main")
                : this.configLabel(CustomConfigGroupStore.reference(group.mainSource, group.mainConfig));
        this.addButton(new ButtonGeneric(mainX, y + 2, 164, 20,
                this.trimButtonText(StringUtils.translate("halfmasa.gui.custom_groups.main", mainName), 164),
                StringUtils.translate("halfmasa.gui.custom_groups.choose_main")),
                (button, mouseButton) -> this.openChoice(groupId, true));
        this.addSmallButton(plusX, y + 2, "+", "halfmasa.gui.custom_groups.add_child",
                () -> this.openChoice(groupId, false));
        boolean hasMain = group != null && group.mainConfig != null && !group.mainConfig.isBlank();
        this.addSmallButton(minusX, y + 2, "-", hasMain
                        ? "halfmasa.gui.custom_groups.demote_main"
                        : "halfmasa.gui.custom_groups.delete_group",
                () -> {
                    if (hasMain)
                    {
                        this.demoteMain(groupId);
                    }
                    else
                    {
                        this.confirmDelete(groupId);
                    }
                });
        this.addSmallButton(upX, y + 2, "\u2191", "halfmasa.gui.custom_groups.move_up",
                () -> this.moveGroup(groupId, -1));
        this.addSmallButton(downX, y + 2, "\u2193", "halfmasa.gui.custom_groups.move_down",
                () -> this.moveGroup(groupId, 1));
    }

    private void addChildControls(String groupId, String childName, int y)
    {
        int right = this.getScreenWidth() - 10;
        int downX = right - 18;
        int upX = downX - 22;
        int minusX = upX - 22;
        this.addSmallButton(minusX, y + 2, "-", "halfmasa.gui.custom_groups.remove_child",
                () -> this.removeChild(groupId, childName));
        this.addSmallButton(upX, y + 2, "\u2191", "halfmasa.gui.custom_groups.move_up",
                () -> this.moveChild(groupId, childName, -1));
        this.addSmallButton(downX, y + 2, "\u2193", "halfmasa.gui.custom_groups.move_down",
                () -> this.moveChild(groupId, childName, 1));
    }

    private void addSmallButton(int x, int y, String label, String tooltipKey, Runnable action)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, 18, 20, label,
                StringUtils.translate(tooltipKey));
        this.addButton(button, (clicked, mouseButton) -> action.run());
    }

    private void createGroup()
    {
        String name = this.newGroupName == null ? "" : this.newGroupName.getTextWrapper();
        if (this.store.create(this.sourceFilter, name) == null)
        {
            return;
        }
        this.newGroupName.setTextWrapper("");
        this.scrollOffset = Integer.MAX_VALUE;
        this.initGui();
    }

    private void openChoice(String groupId, boolean main)
    {
        MinecraftClientCompat.setScreen(this.mc, new CustomConfigChoiceScreen(this, groupId, main));
    }

    private void confirmDelete(String groupId)
    {
        CustomConfigGroupStore.Group group = this.findGroup(groupId);
        if (group == null)
        {
            return;
        }
        MinecraftClientCompat.setScreen(this.mc, new ConfirmScreen(confirmed -> {
            if (confirmed)
            {
                this.store.delete(groupId);
            }
            MinecraftClientCompat.setScreen(this.mc, this);
        }, Component.translatable("halfmasa.gui.custom_groups.delete_title"),
                Component.translatable("halfmasa.gui.custom_groups.delete_confirm", group.name)));
    }

    private void moveGroup(String groupId, int delta)
    {
        List<CustomConfigGroupStore.Group> groups = this.store.getGroups();
        int source = groupIndex(groups, groupId);
        if (source >= 0)
        {
            this.store.moveGroup(groupId, source + delta);
            this.initGui();
        }
    }

    private void removeChild(String groupId, String childName)
    {
        this.store.removeChild(groupId, childName);
        this.initGui();
    }

    private void demoteMain(String groupId)
    {
        if (this.store.demoteMain(groupId))
        {
            this.initGui();
        }
    }

    private void moveChild(String groupId, String childName, int delta)
    {
        CustomConfigGroupStore.Group group = this.findGroup(groupId);
        if (group == null)
        {
            return;
        }
        int source = group.children.indexOf(childName);
        if (source >= 0)
        {
            this.store.moveChild(groupId, childName, source + delta);
            this.initGui();
        }
    }

    private void rebuildRows()
    {
        for (CustomConfigGroupStore.Group group : this.store.getGroups())
        {
            if (!this.sourceFilter.equals(group.source))
            {
                continue;
            }
            this.rows.add(new Row(group.id, null));
            for (String child : group.children)
            {
                this.rows.add(new Row(group.id, child));
            }
        }
        this.scrollOffset = this.clampScroll(this.scrollOffset);
    }

    private boolean isInsideList(double y)
    {
        return y >= LIST_TOP && y < this.getScreenHeight() - BOTTOM_MARGIN;
    }

    private int rowAt(double y)
    {
        if (!this.isInsideList(y) || this.rows.isEmpty())
        {
            return -1;
        }
        int index = (int) ((y - LIST_TOP + this.scrollOffset) / ROW_HEIGHT);
        return index >= 0 && index < this.rows.size() ? index : -1;
    }

    private void handleDrag(double mouseY)
    {
        int targetRow = this.rowAt(mouseY);
        if (targetRow < 0)
        {
            return;
        }
        Row target = this.rows.get(targetRow);
        if (this.draggingGroupId != null)
        {
            List<CustomConfigGroupStore.Group> groups = this.store.getGroups();
            int source = groupIndex(groups, this.draggingGroupId);
            int destination = groupIndex(groups, target.groupId());
            if (source >= 0 && destination >= 0 && source != destination)
            {
                this.store.moveGroup(this.draggingGroupId, destination);
                this.initGui();
            }
            return;
        }
        if (this.draggingChildGroupId == null || this.draggingChildName == null)
        {
            return;
        }

        CustomConfigGroupStore.Group sourceGroup = this.findGroup(this.draggingChildGroupId);
        CustomConfigGroupStore.Group targetGroup = this.findGroup(target.groupId());
        if (sourceGroup == null || targetGroup == null)
        {
            return;
        }
        int targetIndex = target.childName() == null
                ? targetGroup.children.size() : targetGroup.children.indexOf(target.childName());
        if (targetIndex < 0)
        {
            targetIndex = targetGroup.children.size();
        }
        if (targetGroup.id.equals(sourceGroup.id))
        {
            int sourceIndex = sourceGroup.children.indexOf(this.draggingChildName);
            if (sourceIndex >= 0 && sourceIndex < targetIndex)
            {
                targetIndex--;
            }
            if (sourceIndex >= 0 && sourceIndex != targetIndex)
            {
                this.store.moveChild(sourceGroup.id, this.draggingChildName, targetIndex);
                this.initGui();
            }
        }
        else if (this.store.moveChildToGroup(sourceGroup.id,
                CustomConfigGroupStore.nameOf(this.draggingChildName),
                CustomConfigGroupStore.sourceOf(this.draggingChildName),
                targetGroup.id, targetIndex))
        {
            this.initGui();
        }
    }

    private void handleScrollbar(double mouseY)
    {
        int trackTop = LIST_TOP;
        int trackBottom = this.getScreenHeight() - BOTTOM_MARGIN;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        int totalHeight = this.rows.size() * ROW_HEIGHT;
        int thumbHeight = Math.max(16, trackHeight * trackHeight / Math.max(trackHeight, totalHeight));
        int available = Math.max(1, trackHeight - thumbHeight);
        int position = (int) mouseY - trackTop - thumbHeight / 2;
        this.scrollOffset = Math.max(0, Math.min(this.maxScroll(),
                position * this.maxScroll() / available));
        this.initGui();
    }

    private int maxScroll()
    {
        return Math.max(0, this.rows.size() * ROW_HEIGHT -
                (this.getScreenHeight() - BOTTOM_MARGIN - LIST_TOP));
    }

    private int clampScroll(int value)
    {
        return Math.max(0, Math.min(this.maxScroll(), value));
    }

    private CustomConfigGroupStore.Group findGroup(String id)
    {
        return this.store.getGroups().stream()
                .filter(group -> group.id.equals(id)).findFirst().orElse(null);
    }

    private static int groupIndex(List<CustomConfigGroupStore.Group> groups, String groupId)
    {
        for (int index = 0; index < groups.size(); index++)
        {
            if (groups.get(index).id.equals(groupId))
            {
                return index;
            }
        }
        return -1;
    }

    private String configLabel(String reference)
    {
        String label = this.configLabels.get(reference);
        return label == null || label.isBlank() ? CustomConfigGroupStore.nameOf(reference) : label;
    }

    private String trimButtonText(String text, int width)
    {
        return this.mc.font.plainSubstrByWidth(text, Math.max(20, width - 8));
    }

    //#if MC >= 1.21.11
    @Override
    protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    //#else
    //$$ @Override
    //$$ protected void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //#endif
    {
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.custom_groups.new_name"), 10, 18,
                0xFFE0E0E0);
        this.drawString(graphics, StringUtils.translate("halfmasa.gui.custom_groups.drag_hint"), 10, 53,
                0xFFB0B0B0);

        for (int index = 0; index < this.rows.size(); index++)
        {
            Row row = this.rows.get(index);
            int y = LIST_TOP + index * ROW_HEIGHT - this.scrollOffset;
            if (y + ROW_HEIGHT < LIST_TOP || y > this.getScreenHeight() - BOTTOM_MARGIN)
            {
                continue;
            }
            int handleColor = row.isGroup() ? 0xFFFFFFFF : 0xFFB0B0B0;
            graphics.fill(HANDLE_X, y + 6, HANDLE_X + HANDLE_WIDTH, y + 8, handleColor);
            graphics.fill(HANDLE_X, y + 11, HANDLE_X + HANDLE_WIDTH, y + 13, handleColor);
            graphics.fill(HANDLE_X, y + 16, HANDLE_X + HANDLE_WIDTH, y + 18, handleColor);
            if (!row.isGroup())
            {
                String label = "\u21b3 " + this.configLabel(row.childName());
                int maxWidth = Math.max(80, this.getScreenWidth() - 120);
                this.drawString(graphics, this.mc.font.plainSubstrByWidth(label, maxWidth), 36, y + 7,
                        0xFFE0E0E0);
            }
        }

        int trackTop = LIST_TOP;
        int trackBottom = this.getScreenHeight() - BOTTOM_MARGIN;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        int totalHeight = this.rows.size() * ROW_HEIGHT;
        int thumbHeight = Math.max(16, trackHeight * trackHeight / Math.max(trackHeight, totalHeight));
        int thumbY = trackTop;
        if (this.maxScroll() > 0)
        {
            thumbY += (trackHeight - thumbHeight) * this.scrollOffset / this.maxScroll();
        }
        graphics.fill(this.getScreenWidth() - 6, trackTop, this.getScreenWidth() - 3, trackBottom,
                0x66303030);
        graphics.fill(this.getScreenWidth() - 7, thumbY, this.getScreenWidth() - 2, thumbY + thumbHeight,
                0xFFD0D0D0);
    }

    //#if MC >= 1.21.10
    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (event.button() == 0)
        {
            if (event.x() >= this.getScreenWidth() - 12 && this.isInsideList(event.y()))
            {
                this.draggingScrollbar = true;
                this.handleScrollbar(event.y());
                return true;
            }
            int rowIndex = this.rowAt(event.y());
            if (rowIndex >= 0 && event.x() >= HANDLE_X && event.x() < HANDLE_X + HANDLE_WIDTH)
            {
                Row row = this.rows.get(rowIndex);
                if (row.isGroup())
                {
                    this.draggingGroupId = row.groupId();
                }
                else
                {
                    this.draggingChildGroupId = row.groupId();
                    this.draggingChildName = row.childName();
                }
                return true;
            }
        }
        return super.onMouseClicked(event, doubleClick);
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event)
    {
        this.draggingScrollbar = false;
        this.draggingGroupId = null;
        this.draggingChildGroupId = null;
        this.draggingChildName = null;
        return super.onMouseReleased(event);
    }
    //#else
    //$$ @Override
    //$$ public boolean mouseClicked(double mouseX, double mouseY, int button)
    //$$ {
    //$$     if (button == 0)
    //$$     {
    //$$         if (mouseX >= this.getScreenWidth() - 12 && this.isInsideList(mouseY))
    //$$         { this.draggingScrollbar = true; this.handleScrollbar(mouseY); return true; }
    //$$         int rowIndex = this.rowAt(mouseY);
    //$$         if (rowIndex >= 0 && mouseX >= HANDLE_X && mouseX < HANDLE_X + HANDLE_WIDTH)
    //$$         {
    //$$             Row row = this.rows.get(rowIndex);
    //$$             if (row.isGroup()) this.draggingGroupId = row.groupId();
    //$$             else { this.draggingChildGroupId = row.groupId(); this.draggingChildName = row.childName(); }
    //$$             return true;
    //$$         }
    //$$     }
    //$$     return super.mouseClicked(mouseX, mouseY, button);
    //$$ }
    //#endif

    //#if MC >= 1.21.11
    @Override
    public boolean onMouseDragged(MouseButtonEvent event, double deltaX, double deltaY)
    {
        if (this.draggingScrollbar)
        {
            this.handleScrollbar(event.y());
            return true;
        }
        if (this.draggingGroupId != null || this.draggingChildGroupId != null)
        {
            this.handleDrag(event.y());
            return true;
        }
        return super.onMouseDragged(event, deltaX, deltaY);
    }
    //#elseif MC >= 1.21.10
    //$$ @Override
    //$$ public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY)
    //$$ {
    //$$     if (this.draggingScrollbar)
    //$$     {
    //$$         this.handleScrollbar(event.y());
    //$$         return true;
    //$$     }
    //$$     if (this.draggingGroupId != null || this.draggingChildGroupId != null)
    //$$     {
    //$$         this.handleDrag(event.y());
    //$$         return true;
    //$$     }
    //$$     return super.mouseDragged(event, deltaX, deltaY);
    //$$ }
    //#else
    //$$ @Override
    //$$ public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    //$$ {
    //$$     if (this.draggingScrollbar)
    //$$     {
    //$$         this.handleScrollbar(mouseY);
    //$$         return true;
    //$$     }
    //$$     if (this.draggingGroupId != null || this.draggingChildGroupId != null)
    //$$     {
    //$$         this.handleDrag(mouseY);
    //$$         return true;
    //$$     }
    //$$     return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    //$$ }
    //#endif

    //#if MC >= 1.21.10
    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
    //#else
    //$$ @Override
    //$$ public boolean onMouseScrolled(int mouseX, int mouseY, double deltaX, double deltaY)
    //#endif
    {
        if (this.isInsideList(mouseY))
        {
            this.scrollOffset = this.clampScroll(this.scrollOffset - (int) Math.signum(deltaY) * ROW_HEIGHT * 3);
            this.initGui();
            return true;
        }
        return super.onMouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void onClose()
    {
        ConfigScrollMemory.saveCustomGroupEditor(this.sourceFilter, this.scrollOffset);
        MinecraftClientCompat.setScreen(this.mc, this.parent);
    }

    private record Row(String groupId, String childName)
    {
        private boolean isGroup()
        {
            return this.childName == null;
        }
    }

    /** Small picker used by both the main-config and child-config buttons. */
    private static final class CustomConfigChoiceScreen extends GuiBase
    {
        private final CustomConfigGroupScreen parent;
        private final String groupId;
        private final boolean main;
        private final CustomConfigGroupStore store = CustomConfigGroupStore.getInstance();
        private final List<CustomConfigSources.Candidate> choices = new ArrayList<>();
        private final String source;
        private String search = "";
        private int scroll;

        private CustomConfigChoiceScreen(CustomConfigGroupScreen parent, String groupId, boolean main)
        {
            this.parent = parent;
            this.groupId = groupId;
            this.main = main;
            CustomConfigGroupStore.Group group = parent.findGroup(groupId);
            this.source = group == null ? CustomConfigSources.HALF_MASA : group.source;
            this.scroll = ConfigScrollMemory.restoreCustomGroupChoice(this.source);
            this.setTitle(StringUtils.translate(main
                    ? "halfmasa.gui.custom_groups.choose_main"
                    : "halfmasa.gui.custom_groups.choose_child"));
        }

        @Override
        public void initGui()
        {
            super.initGui();
            this.clearElements();
            GuiTextFieldGeneric field = new GuiTextFieldGeneric(10, 28,
                    Math.min(360, this.getScreenWidth() - 20), 20, this.mc.font);
            field.setTextWrapper(this.search);
            field.setMaxLengthWrapper(128);
            this.addTextField(field, input -> {
                this.search = input.getTextWrapper();
                this.scroll = 0;
                this.initGui();
                return true;
            });

            this.choices.clear();
            String query = this.search.trim().toLowerCase(Locale.ROOT);
            for (CustomConfigSources.Candidate candidate : CustomConfigSources.getCandidates(this.source))
            {
                IConfigBase config = candidate.config();
                String display = config.getConfigGuiDisplayName();
                if (query.isEmpty() || candidate.name().toLowerCase(Locale.ROOT).contains(query) ||
                        display.toLowerCase(Locale.ROOT).contains(query))
                {
                    this.choices.add(candidate);
                }
            }

            int top = 58;
            int bottom = this.getScreenHeight() - 30;
            int visible = Math.max(1, (bottom - top) / 22);
            int maxScroll = Math.max(0, this.choices.size() - visible);
            this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
            for (int index = 0; index < visible && this.scroll + index < this.choices.size(); index++)
            {
                CustomConfigSources.Candidate candidate = this.choices.get(this.scroll + index);
                IConfigBase config = candidate.config();
                String label = config.getConfigGuiDisplayName() + " | " + candidate.name();
                this.addButton(new ButtonGeneric(10, top + index * 22,
                        this.getScreenWidth() - 20, 20,
                        this.mc.font.plainSubstrByWidth(label, this.getScreenWidth() - 32)),
                        (button, mouseButton) -> this.select(candidate));
            }
            this.addButton(new ButtonGeneric(10, this.getScreenHeight() - 26, 82, 20,
                    StringUtils.translate("halfmasa.gui.custom_groups.done")),
                    (button, mouseButton) -> this.onClose());
        }

        private void select(CustomConfigSources.Candidate candidate)
        {
            ConfigScrollMemory.saveCustomGroupChoice(this.source, this.scroll);
            boolean changed = this.main
                    ? this.store.setMain(this.groupId, candidate.source(), candidate.name())
                    : this.store.addChild(this.groupId, candidate.source(), candidate.name());
            if (changed)
            {
                this.parent.initGui();
                MinecraftClientCompat.setScreen(this.mc, this.parent);
            }
        }

        //#if MC >= 1.21.10
        @Override
        public boolean onMouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
        //#else
        //$$ @Override
        //$$ public boolean onMouseScrolled(int mouseX, int mouseY, double deltaX, double deltaY)
        //#endif
        {
            if (mouseY >= 58 && mouseY < this.getScreenHeight() - 30)
            {
                this.scroll -= (int) Math.signum(deltaY) * 3;
                this.initGui();
                return true;
            }
            return super.onMouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }

        @Override
        public void onClose()
        {
            ConfigScrollMemory.saveCustomGroupChoice(this.source, this.scroll);
            MinecraftClientCompat.setScreen(this.mc, this.parent);
        }
    }

    /** Selects or creates a custom group for the plus button in a config row. */
    private static final class GroupTargetChoiceScreen extends GuiBase
    {
        private final Screen parent;
        private final String source;
        private final String configName;
        private final CustomConfigGroupStore store = CustomConfigGroupStore.getInstance();
        private final List<CustomConfigGroupStore.Group> choices = new ArrayList<>();
        private GuiTextFieldGeneric newGroupName;
        private int scroll;

        private GroupTargetChoiceScreen(Screen parent, String source, String configName)
        {
            this.parent = parent;
            this.source = source;
            this.configName = configName;
            this.scroll = ConfigScrollMemory.restoreCustomGroupTarget(this.source);
            this.setTitle(StringUtils.translate("halfmasa.gui.custom_groups.select_group"));
        }

        @Override
        public void initGui()
        {
            super.initGui();
            this.clearElements();
            ensureDefaultGroups(this.store, this.source);
            int createButtonWidth = 96;
            int createMainButtonWidth = 190;
            int nameWidth = Math.max(80, Math.min(320,
                    this.getScreenWidth() - 10 - 4 - createButtonWidth - 4 - createMainButtonWidth));
            this.newGroupName = new GuiTextFieldGeneric(10, 28, nameWidth, 20, this.mc.font);
            this.newGroupName.setMaxLengthWrapper(128);
            this.addTextField(this.newGroupName, field -> true);
            this.addButton(new ButtonGeneric(10 + nameWidth + 4, 28, createButtonWidth, 20,
                    StringUtils.translate("halfmasa.gui.custom_groups.create")),
                    (button, mouseButton) -> this.createEmpty());
            this.addButton(new ButtonGeneric(10 + nameWidth + 4 + createButtonWidth + 4, 28,
                    createMainButtonWidth, 20,
                    StringUtils.translate("halfmasa.gui.custom_groups.create_and_add")),
                    (button, mouseButton) -> this.createAndAdd());
            this.choices.clear();
            for (CustomConfigGroupStore.Group group : this.store.getGroups())
            {
                if (this.source.equals(group.source))
                {
                    this.choices.add(group);
                }
            }

            int top = 58;
            int bottom = this.getScreenHeight() - 30;
            int visible = Math.max(1, (bottom - top) / 22);
            int maxScroll = Math.max(0, this.choices.size() - visible);
            this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
            for (int index = 0; index < visible && this.scroll + index < this.choices.size(); index++)
            {
                CustomConfigGroupStore.Group group = this.choices.get(this.scroll + index);
                String label = group.name;
                if (group.mainConfig != null && !group.mainConfig.isBlank())
                {
                    label += " | " + CustomConfigGroupStore.nameOf(group.mainConfig);
                }
                this.addButton(new ButtonGeneric(10, top + index * 22,
                        this.getScreenWidth() - 20,
                        20,
                        this.mc.font.plainSubstrByWidth(label, this.getScreenWidth() - 32)),
                        (button, mouseButton) -> this.select(group));
            }
            if (this.choices.isEmpty())
            {
                this.addButton(new ButtonGeneric(10, top, this.getScreenWidth() - 20, 20,
                        StringUtils.translate("halfmasa.gui.custom_groups.no_groups")),
                        (button, mouseButton) -> {});
            }
            this.addButton(new ButtonGeneric(10, this.getScreenHeight() - 26, 82, 20,
                    StringUtils.translate("halfmasa.gui.custom_groups.done")),
                    (button, mouseButton) -> this.onClose());
        }

        private void select(CustomConfigGroupStore.Group group)
        {
            ConfigScrollMemory.saveCustomGroupTarget(this.source, this.scroll);
            if (this.store.addChild(group.id, this.source, this.configName) &&
                    this.parent instanceof GuiConfigsBase configScreen)
            {
                configScreen.initGui();
            }
            MinecraftClientCompat.setScreen(this.mc, this.parent);
        }

        private void createEmpty()
        {
            String name = this.newGroupName == null ? "" : this.newGroupName.getTextWrapper().trim();
            if (name.isEmpty() || this.store.create(this.source, name) == null)
            {
                return;
            }
            this.newGroupName.setTextWrapper("");
            this.initGui();
        }

        private void createAndAdd()
        {
            String name = this.newGroupName == null ? "" : this.newGroupName.getTextWrapper().trim();
            if (name.isEmpty())
            {
                return;
            }
            CustomConfigGroupStore.Group group = this.store.create(this.source, name);
            if (group == null || !this.store.setMain(group.id, this.source, this.configName))
            {
                return;
            }
            if (this.parent instanceof GuiConfigsBase configScreen)
            {
                configScreen.initGui();
            }
            MinecraftClientCompat.setScreen(this.mc, this.parent);
        }

        //#if MC >= 1.21.11
        @Override
        protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
        //#else
        //$$ @Override
        //$$ protected void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        //#endif
        {
            this.drawString(graphics, StringUtils.translate("halfmasa.gui.custom_groups.new_name"), 10, 18,
                    0xFFE0E0E0);
        }

        //#if MC >= 1.21.10
        @Override
        public boolean onMouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
        //#else
        //$$ @Override
        //$$ public boolean onMouseScrolled(int mouseX, int mouseY, double deltaX, double deltaY)
        //#endif
        {
            if (mouseY >= 58 && mouseY < this.getScreenHeight() - 30)
            {
                this.scroll -= (int) Math.signum(deltaY) * 3;
                this.initGui();
                return true;
            }
            return super.onMouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }

        @Override
        public void onClose()
        {
            ConfigScrollMemory.saveCustomGroupTarget(this.source, this.scroll);
            MinecraftClientCompat.setScreen(this.mc, this.parent);
        }
    }
}

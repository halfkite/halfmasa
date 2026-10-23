package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ConfluxMapWaypointOrder;
import io.github.halfmasa.xaerobinding.feature.ConfluxWaypointScreenBridge;
import io.github.halfmasa.xaerobinding.feature.WaypointGridLayout;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Native rows and actions remain authoritative; only compact mode replaces row geometry. */
@Pseudo
@Mixin(targets = "cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen", remap = false)
public abstract class ConfluxMapCombinedWaypointListMixin extends Screen
{
    @Unique private ConfluxWaypointScreenBridge halfmasa$bridge;
    @Unique private List<ConfluxWaypointScreenBridge.Row> halfmasa$rows = List.of();
    @Unique private WaypointGridLayout halfmasa$layout;
    @Unique private Object halfmasa$previousTab;
    @Unique private boolean halfmasa$active, halfmasa$failed, halfmasa$compact, halfmasa$dimension, halfmasa$distance;
    @Unique private ConfluxMapWaypointOrder halfmasa$order;
    @Unique private int halfmasa$scroll;
    @Unique private int halfmasa$settingsToken;
    @Unique private String halfmasa$query = "";
    @Unique private boolean halfmasa$warned;
    @Unique private ConfluxWaypointScreenBridge.Row halfmasa$hover;
    @Unique private ConfluxWaypointScreenBridge.Row halfmasa$restoreHover;
    @Unique private WaypointGridLayout.Cell halfmasa$anchor, halfmasa$panel;
    @Unique private List<AbstractWidget> halfmasa$actions = List.of();

    //#if MC >= 1.21.1
    @Unique private List<AbstractWidget> halfmasa$checks = List.of();
    @Unique private boolean halfmasa$merged, halfmasa$memoryRestored;
    @Unique private String halfmasa$memoryWorld;
    @Unique private int halfmasa$viewToken;

    @Unique private void halfmasa$restoreMemory() throws ReflectiveOperationException
    {
        if (halfmasa$memoryRestored || !io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.enabled()) return;
        if (halfmasa$bridge == null) halfmasa$bridge = new ConfluxWaypointScreenBridge(this);
        String world = halfmasa$bridge.worldKey();
        if (world == null) return;
        halfmasa$memoryWorld = world; halfmasa$memoryRestored = true;
        var state = io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.get(world);
        if (state == null) return;
        Object requested = halfmasa$bridge.restore(state);
        halfmasa$previousTab = Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue() ? requested : null;
        if (halfmasa$previousTab != null) halfmasa$bridge.useLocalTab();
        halfmasa$scroll = state.compactScroll(); halfmasa$query = halfmasa$bridge.query();
        halfmasa$viewToken = halfmasa$bridge.viewToken();
        halfmasa$bridge.rebuild();
    }

    @Inject(method = {"init", "method_25426"}, at = @At("TAIL"), require = 1, remap = false)
    private void halfmasa$restoreLastList(CallbackInfo ci)
    {
        try { halfmasa$restoreMemory(); }
        catch (ReflectiveOperationException error) { halfmasa$contractFailure(error); }
    }

    @Override
    public void removed()
    {
        try
        {
            if (halfmasa$bridge != null && halfmasa$memoryWorld != null && halfmasa$memoryWorld.equals(halfmasa$bridge.worldKey()))
                io.github.halfmasa.xaerobinding.feature.ConfluxWaypointListMemory.save(halfmasa$memoryWorld,
                        halfmasa$bridge.snapshot(halfmasa$previousTab, halfmasa$scroll));
        }
        catch (ReflectiveOperationException error) { halfmasa$contractFailure(error); }
        super.removed();
    }


    @Unique private void halfmasa$rebuildChecks() throws ReflectiveOperationException
    {
        for (var check : halfmasa$checks)
            ((ScreenAccessor)this).halfmasa$removeWidget(check);
        var checks = new ArrayList<AbstractWidget>();
        if (halfmasa$compact && halfmasa$layout != null)
            for (var cell : halfmasa$layout.cells())
            {
                var row = halfmasa$rows.get(cell.index());
                if (row.local() == null || cell.width() < 48) continue;
                var check = halfmasa$bridge.button(Component.literal(halfmasa$bridge.selected(row) ? "✓" : ""), button -> {
                    try { halfmasa$bridge.toggleSelection(row); }
                    catch (ReflectiveOperationException error) { halfmasa$contractFailure(error); }
                }, cell.x() + cell.width() - 23, cell.y() + (cell.height() - 20) / 2, 20);
                check.active = halfmasa$bridge.editAccess(row).available();
                check.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("halfmasa.conflux_map.select")));
                this.addRenderableWidget(check);
                ((ScreenAccessor)this).halfmasa$getRenderables().remove(check);
                checks.add(check);
            }
        halfmasa$checks = checks;
    }
    //#endif

    protected ConfluxMapCombinedWaypointListMixin(Component title) { super(title); }
    @Unique private boolean halfmasa$enabled()
    { return !halfmasa$failed && Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue()
            && (Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue()
                //#if MC >= 1.21.1
                || Configs.CONFLUX_MAP_WAYPOINT_COMPACT.getBooleanValue()
                //#endif
                ); }
    @Unique private boolean halfmasa$serverFirst()
    { return Configs.CONFLUX_MAP_WAYPOINT_ORDER.getOptionListValue() == ConfluxMapWaypointOrder.SERVER_FIRST; }

    @Inject(method = "rebuild", at = @At("HEAD"), require = 1, remap = false)
    private void halfmasa$prepareContract(CallbackInfo ci)
    {
        //#if MC >= 1.21.1
        halfmasa$checks = List.of();
        if (halfmasa$bridge != null) halfmasa$bridge.clearPublicationButtons();
        //#endif
        // The native rebuild clears children. Forget popup widgets before that happens.
        halfmasa$restoreHover = halfmasa$hover;
        halfmasa$actions = List.of(); halfmasa$hover = null; halfmasa$panel = null;
        try
        {
            if (halfmasa$enabled())
            {
                if (halfmasa$bridge == null) halfmasa$bridge = new ConfluxWaypointScreenBridge(this);
                //#if MC >= 1.21.1
                if (Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue())
                {
                //#endif
                    if (halfmasa$previousTab == null) halfmasa$previousTab = halfmasa$bridge.tab();
                    halfmasa$bridge.useLocalTab();
                //#if MC >= 1.21.1
                }
                else if (halfmasa$previousTab != null)
                { halfmasa$bridge.tab(halfmasa$previousTab); halfmasa$previousTab = null; }
                //#endif
            }
            else if (halfmasa$previousTab != null)
            { halfmasa$bridge.tab(halfmasa$previousTab); halfmasa$previousTab = null; }
        }
        catch (ReflectiveOperationException | LinkageError exception) { halfmasa$contractFailure(exception); }
    }

    @Inject(method = "buildRows", at = @At("RETURN"), cancellable = true, require = 1, remap = false)
    private void halfmasa$combineNativeRows(CallbackInfoReturnable<List<Object>> cir)
    {
        if (!halfmasa$enabled() || !Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue() || halfmasa$bridge == null || halfmasa$bridge.collecting()) return;
        try { cir.setReturnValue(halfmasa$bridge.combinedNativeRows(halfmasa$serverFirst())); }
        catch (ReflectiveOperationException | LinkageError exception) { halfmasa$contractFailure(exception); }
    }

    @Redirect(method = "rebuild", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"), require = 1, remap = false)
    private void halfmasa$sortSources(List<Object> rows, Comparator<Object> nativeSort)
    {
        if (!halfmasa$enabled() || !Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue() || halfmasa$bridge == null) { rows.sort(nativeSort); return; }
        rows.sort(Comparator.comparingInt((Object row) -> halfmasa$bridge.sourceRank(row, halfmasa$serverFirst())).thenComparing(nativeSort));
    }

    @Inject(method = "rebuild", at = @At("TAIL"), require = 1, remap = false)
    private void halfmasa$rebuildCombined(CallbackInfo ci)
    {
        halfmasa$active = halfmasa$enabled();
        if (!halfmasa$active) { halfmasa$rows = List.of(); halfmasa$layout = null; return; }
        try
        {
            //#if MC >= 1.21.1
            halfmasa$merged = Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue();
            int viewToken = halfmasa$bridge.viewToken();
            if (viewToken != halfmasa$viewToken) halfmasa$scroll = 0;
            halfmasa$viewToken = viewToken;
            //#endif
            halfmasa$compact = Configs.CONFLUX_MAP_WAYPOINT_COMPACT.getBooleanValue();
            halfmasa$dimension = Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.getBooleanValue();
            halfmasa$distance = Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.getBooleanValue();
            halfmasa$order = (ConfluxMapWaypointOrder)Configs.CONFLUX_MAP_WAYPOINT_ORDER.getOptionListValue();
            String query = halfmasa$bridge.query();
            if (!query.equals(halfmasa$query)) halfmasa$scroll = 0;
            halfmasa$query = query;
            halfmasa$rows = halfmasa$bridge.rows(halfmasa$serverFirst());
            halfmasa$settingsToken = halfmasa$bridge.settingsToken();
            halfmasa$relayout();
            int top = halfmasa$bridge.listTop();
            //#if MC >= 1.21.1
            var tabs = new ArrayList<AbstractWidget>();
            //#endif
            for (var child : this.children()) if (child instanceof AbstractWidget widget)
            {
                //#if MC >= 1.21.1
                if (!halfmasa$merged && widget.getY() == 28) { tabs.add(widget); continue; }
                //#endif
                if (widget.getY() == 28 || halfmasa$compact && widget.getY() >= top && widget.getY() < this.height - 34)
                { widget.visible = false; widget.active = false; }
            }
            int total = Math.min(halfmasa$compact ? 440 : 284, Math.max(1, this.width - 32));
            int part = total / (halfmasa$compact ? 4 : 2), x = (this.width - total) / 2;
            //#if MC >= 1.21.1
            if (!halfmasa$merged)
            {
                total = Math.min(880, Math.max(1, this.width - 32));
                part = total / (tabs.size() + (halfmasa$compact ? 3 : 1)); x = (this.width - total) / 2;
                for (var tab : tabs)
                { tab.setX(x); tab.setWidth(Math.max(1, part - 4)); x += part; }
                x -= part; // The layout toggle below is placed at x + part.
            }
            else
            //#endif
            this.addRenderableWidget(halfmasa$bridge.button(Component.literal(halfmasa$order.getDisplayName()), button -> {
                Configs.CONFLUX_MAP_WAYPOINT_ORDER.setOptionListValue(halfmasa$order.cycle(true));
                halfmasa$scroll = 0; new Configs().save(); halfmasa$refresh();
            }, x, 28, part - 4));
            this.addRenderableWidget(halfmasa$bridge.button(Component.translatable(halfmasa$compact
                    ? "halfmasa.conflux_map.compact_on" : "halfmasa.conflux_map.compact_off"), button -> {
                Configs.CONFLUX_MAP_WAYPOINT_COMPACT.toggleBooleanValue();
                halfmasa$scroll = 0; new Configs().save(); halfmasa$refresh();
            }, x + part, 28, part - 4));
            if (halfmasa$compact)
            {
                this.addRenderableWidget(halfmasa$bridge.button(halfmasa$checkLabel("dimension", halfmasa$dimension), button -> {
                    Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.toggleBooleanValue(); new Configs().save(); halfmasa$refresh();
                }, x + part * 2, 28, part - 4));
                this.addRenderableWidget(halfmasa$bridge.button(halfmasa$checkLabel("distance", halfmasa$distance), button -> {
                    Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.toggleBooleanValue(); new Configs().save(); halfmasa$refresh();
                }, x + part * 3, 28, part - 4));
                if (halfmasa$restoreHover != null)
                    for (var cell : halfmasa$layout.cells())
                        if (halfmasa$rows.get(cell.index()).sameIdentity(halfmasa$restoreHover))
                        { halfmasa$openPanel(cell); break; }
            }
            //#if MC >= 1.21.1
            halfmasa$rebuildChecks();
            //#endif
            halfmasa$restoreHover = null;
        }
        catch (ReflectiveOperationException | LinkageError exception) { halfmasa$contractFailure(exception); }
    }
    @Unique private Component halfmasa$checkLabel(String key, boolean checked)
    { return Component.translatable("halfmasa.conflux_map.show_" + key, checked ? "[✓]" : "[ ]"); }

    @Unique private void halfmasa$relayout() throws ReflectiveOperationException
    {
        int left = Math.max(16, (this.width - 880) / 2), top = halfmasa$bridge.listTop();
        halfmasa$layout = WaypointGridLayout.create(halfmasa$rows.size(), left, top,
                this.width - left * 2 - 8, this.height - 34 - top,
                halfmasa$compact, this.font.lineHeight * (halfmasa$compact && (halfmasa$dimension || halfmasa$distance) ? 2 : 1), halfmasa$scroll);
        halfmasa$scroll = halfmasa$layout.scroll();
    }
    @Inject(method = {"tick", "method_25393"}, at = @At("TAIL"), require = 1, remap = false)
    private void halfmasa$settingsChanged(CallbackInfo ci)
    {
        try {
        //#if MC >= 1.21.1
        halfmasa$restoreMemory();
        if (halfmasa$bridge != null) halfmasa$bridge.refreshPublicationButtons();
        //#endif
        if (halfmasa$active != halfmasa$enabled() || halfmasa$active &&
                (
                 //#if MC >= 1.21.1
                 halfmasa$merged != Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue() ||
                 //#endif
                 halfmasa$compact != Configs.CONFLUX_MAP_WAYPOINT_COMPACT.getBooleanValue() ||
                 halfmasa$dimension != Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.getBooleanValue() ||
                 halfmasa$distance != Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.getBooleanValue() ||
                 halfmasa$order != Configs.CONFLUX_MAP_WAYPOINT_ORDER.getOptionListValue() ||
                 halfmasa$settingsToken != halfmasa$bridge.settingsToken())) halfmasa$refresh();
        } catch (ReflectiveOperationException exception) { halfmasa$contractFailure(exception); }
    }
    @Unique private void halfmasa$refresh()
    {
        try
        {
            if (halfmasa$bridge == null)
            { if (!halfmasa$enabled()) return; halfmasa$bridge = new ConfluxWaypointScreenBridge(this); }
            halfmasa$bridge.rebuild();
        }
        catch (ReflectiveOperationException | LinkageError exception) { halfmasa$contractFailure(exception); }
    }

    @Inject(method = "renderContents", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void halfmasa$renderCompact(@Coerce Object draw, int mx, int my, float partialTick, CallbackInfo ci)
    {
        if (!halfmasa$active || !halfmasa$enabled() || !halfmasa$compact || halfmasa$layout == null) return;
        try
        {
            halfmasa$bridge.background(draw, mx, my, partialTick);
            String title = this.getTitle().getString();
            halfmasa$bridge.text(draw, title, (this.width - this.font.width(title)) / 2F, 10, 0xFFFFFFFF);
            halfmasa$updateHover(mx, my);
            for (var cell : halfmasa$layout.cells())
            {
                var row = halfmasa$rows.get(cell.index());
                boolean hovered = halfmasa$hover != null && halfmasa$hover.sameIdentity(row);
                // Use the native list's translucent row palette.
                halfmasa$bridge.fill(draw, cell.x(), cell.y() + 1, cell.x() + cell.width(), cell.y() + cell.height() - 1,
                        hovered ? 0x55333333 : 0x33202020);
                halfmasa$bridge.marker(draw, row, cell.x() + 11F, cell.y() + 11F, hovered);
                int available = Math.max(0, cell.width() - 28
                        //#if MC >= 1.21.1
                        - (row.local() != null && cell.width() >= 48 ? 24 : 0)
                        //#endif
                        );
                halfmasa$bridge.text(draw, this.font.plainSubstrByWidth(row.name(), available), cell.x() + 23F, cell.y() + 5F, 0xFFFFFFFF);
                if (halfmasa$dimension || halfmasa$distance)
                {
                    String info = (halfmasa$dimension ? halfmasa$bridge.dimensionText(row) : "") +
                            (halfmasa$dimension && halfmasa$distance ? " · " : "") + (halfmasa$distance ? halfmasa$bridge.distanceText(row) : "");
                    halfmasa$bridge.text(draw, this.font.plainSubstrByWidth(info, available), cell.x() + 23F, cell.y() + 16F, 0xFFAAAAAA);
                }
            }
            //#if MC >= 1.21.1
            halfmasa$bridge.renderActions(draw, halfmasa$checks, mx, my, partialTick);
            //#endif
            if (halfmasa$rows.isEmpty()) halfmasa$bridge.text(draw, Component.translatable("halfmasa.conflux_map.dual_waypoints.empty").getString(),
                    halfmasa$layout.left() + 6F, halfmasa$layout.top() + 6F, 0xFFAAAAAA);
            if (halfmasa$layout.maxScroll() > 0 && halfmasa$layout.height() > 0)
            {
                int x = halfmasa$layout.left() + halfmasa$layout.width() + 3, height = halfmasa$layout.height();
                int total = halfmasa$layout.maxScroll() + halfmasa$layout.visibleRows();
                int thumb = Math.min(height, Math.max(8, height * halfmasa$layout.visibleRows() / Math.max(1, total)));
                int y = halfmasa$layout.top() + (height - thumb) * halfmasa$scroll / halfmasa$layout.maxScroll();
                halfmasa$bridge.fill(draw, x, halfmasa$layout.top(), x + 4, halfmasa$layout.top() + height, 0x33202020);
                halfmasa$bridge.fill(draw, x, y, x + 4, y + thumb, 0xFFAAAAAA);
            }
            ci.cancel();
        }
        catch (ReflectiveOperationException | LinkageError exception) { halfmasa$contractFailure(exception); }
    }

    @Inject(method = "renderAfterWidgets", at = @At("HEAD"), require = 1, remap = false)
    private void halfmasa$popupAboveWidgets(@Coerce Object draw, int mx, int my, float tick, CallbackInfo ci)
    {
        if (!halfmasa$active || !halfmasa$enabled() || !halfmasa$compact || halfmasa$panel == null) return;
        try
        { halfmasa$renderPanel(draw); halfmasa$bridge.renderActions(draw, halfmasa$actions, mx, my, tick); }
        catch (ReflectiveOperationException | LinkageError exception) { halfmasa$contractFailure(exception); }
    }

    @Unique private void halfmasa$closePanel() throws ReflectiveOperationException
    {
        halfmasa$bridge.removeActions(halfmasa$actions); halfmasa$actions = List.of();
        halfmasa$hover = null; halfmasa$panel = null; halfmasa$anchor = null;
    }
    @Unique private void halfmasa$updateHover(int mx, int my) throws ReflectiveOperationException
    {
        if (halfmasa$bridge.dropdownOpen()) { halfmasa$closePanel(); return; }
        // Keep the same popup while moving from its card into the action area.
        if (halfmasa$panel != null && (halfmasa$panel.contains(mx, my) || halfmasa$anchor.contains(mx, my))) return;
        var cell = halfmasa$layout.at(mx, my);
        halfmasa$closePanel();
        if (cell == null || this.width < 64) return;
        halfmasa$openPanel(cell);
    }
    @Unique private void halfmasa$openPanel(WaypointGridLayout.Cell cell) throws ReflectiveOperationException
    {
        halfmasa$anchor = cell; halfmasa$hover = halfmasa$rows.get(cell.index());
        halfmasa$actions = halfmasa$bridge.actionWidgets(halfmasa$hover);
        int panelWidth = Math.min(360, Math.max(1, this.width - 32));
        int x = Math.max(16, Math.min(cell.x(), this.width - 16 - panelWidth));
        int contentY = this.font.lineHeight * 4 + 12, buttonX = x + 6, buttonY = contentY;
        for (var widget : halfmasa$actions)
        {
            int w = Math.min(widget.getWidth(), Math.max(1, panelWidth - 12));
            if (buttonX > x + 6 && buttonX + w > x + panelWidth - 6) { buttonX = x + 6; buttonY += 24; }
            widget.setWidth(w); widget.setX(buttonX); widget.setY(buttonY);
            buttonX += w + 4;
        }
        int panelHeight = buttonY + 26;
        int y = cell.y() + cell.height();
        if (y + panelHeight > this.height - 34) y = cell.y() - panelHeight;
        y = Math.max(0, Math.min(y, Math.max(0, this.height - panelHeight)));
        halfmasa$panel = new WaypointGridLayout.Cell(-1, x, y, panelWidth, panelHeight);
        for (var widget : halfmasa$actions) widget.setY(widget.getY() + y);
    }
    @Unique private void halfmasa$renderPanel(Object draw) throws ReflectiveOperationException
    {
        var p = halfmasa$panel;
        halfmasa$bridge.fill(draw, p.x(), p.y(), p.x() + p.width(), p.y() + p.height(), 0xF0181818);
        halfmasa$bridge.fill(draw, p.x(), p.y(), p.x() + p.width(), p.y() + 1, 0xFF8A8A8A);
        halfmasa$bridge.fill(draw, p.x(), p.y() + p.height() - 1, p.x() + p.width(), p.y() + p.height(), 0xFF8A8A8A);
        halfmasa$bridge.fill(draw, p.x(), p.y(), p.x() + 1, p.y() + p.height(), 0xFF8A8A8A);
        halfmasa$bridge.fill(draw, p.x() + p.width() - 1, p.y(), p.x() + p.width(), p.y() + p.height(), 0xFF8A8A8A);
        String[] lines = { halfmasa$hover.name(), halfmasa$bridge.secondaryText(halfmasa$hover),
                halfmasa$bridge.dimensionText(halfmasa$hover) + " · " + halfmasa$bridge.distanceText(halfmasa$hover),
                halfmasa$bridge.coordinates(halfmasa$hover) };
        for (int i = 0; i < lines.length; i++) halfmasa$bridge.text(draw,
                this.font.plainSubstrByWidth(lines[i], p.width() - 12), p.x() + 6F, p.y() + 6F + i * this.font.lineHeight, i == 0 ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    @Inject(method = {"mouseClicked", "method_25402"}, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    //#if MC >= 1.21.10
    private void halfmasa$compactClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir)
    //#else
    //$$ private void halfmasa$compactClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir)
    //#endif
    {
        //#if MC >= 1.21.10
        double mouseX = event.x(), mouseY = event.y();
        //#endif
        if (!halfmasa$active || !halfmasa$enabled() || !halfmasa$compact || halfmasa$layout == null) return;
        try
        {
            if (halfmasa$bridge.dropdownOpen()) return;
            halfmasa$updateHover((int)mouseX, (int)mouseY);
            if (halfmasa$panel != null && halfmasa$panel.contains(mouseX, mouseY))
            {
                boolean exists = halfmasa$bridge.rows(halfmasa$serverFirst()).stream().anyMatch(row -> row.sameIdentity(halfmasa$hover));
                if (!exists) { halfmasa$closePanel(); cir.setReturnValue(true); return; }
                // The popup can cover header controls on narrow screens: give its native widgets priority.
                for (var widget : halfmasa$actions)
                    //#if MC >= 1.21.10
                    if (widget.mouseClicked(event, doubleClick))
                    //#else
                    //$$ if (widget.mouseClicked(mouseX, mouseY, button))
                    //#endif
                    { this.setFocused(widget); this.setDragging(true); break; }
                cir.setReturnValue(true);
                return;
            }
            //#if MC >= 1.21.1
            // Popup actions have priority; card checkboxes never trigger another row action.
            for (var check : List.copyOf(halfmasa$checks))
                //#if MC >= 1.21.10
                if (check.mouseClicked(event, doubleClick))
                //#else
                //$$ if (check.mouseClicked(mouseX, mouseY, button))
                //#endif
            { this.setFocused(check); this.setDragging(true); cir.setReturnValue(true); return; }
            //#endif
            if (halfmasa$layout.contains(mouseX, mouseY)) cir.setReturnValue(true);
        }
        catch (ReflectiveOperationException | LinkageError exception) { halfmasa$contractFailure(exception); }
    }
    @Inject(method = {"mouseScrolled", "method_25401"}, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void halfmasa$compactScroll(double mx, double my, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir)
    {
        if (!halfmasa$active || !halfmasa$enabled() || !halfmasa$compact || halfmasa$layout == null || vertical == 0) return;
        try
        {
            if (halfmasa$bridge.dropdownOpen() || !halfmasa$layout.contains(mx, my)) return;
            halfmasa$closePanel(); halfmasa$scroll -= (int)Math.signum(vertical); halfmasa$relayout();
            //#if MC >= 1.21.1
            halfmasa$rebuildChecks();
            //#endif
            cir.setReturnValue(true);
        }
        catch (ReflectiveOperationException exception) { halfmasa$contractFailure(exception); }
    }
    @Unique private void halfmasa$contractFailure(Throwable failure)
    {
        halfmasa$failed = true;
        if (!halfmasa$warned)
        { halfmasa$warned = true; XaeroWorldBinding.LOGGER.warn("Conflux Map waypoint UI contract changed; restoring its native list and actions", failure); }
    }
}
//#endif

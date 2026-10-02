package io.github.halfmasa.xaerobinding.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ConfluxMapWaypointListLayout;

/** Adds a read-only dual-pane view over Conflux Map's local and shared waypoint data. */
@Pseudo
@Mixin(targets = "cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen", remap = false)
public abstract class ConfluxMapDualWaypointListMixin
{
    @Unique private static final int halfmasa$LIST_TOP = 118;
    @Unique private static final int halfmasa$LIST_BOTTOM_MARGIN = 38;
    @Unique private static boolean halfmasa$warnedContract;

    @Unique private boolean halfmasa$dualViewWasEnabled;
    @Unique private int halfmasa$localScrollOffset;
    @Unique private int halfmasa$sharedScrollOffset;
    @Unique private int halfmasa$localMaxScroll;
    @Unique private int halfmasa$sharedMaxScroll;

    @Inject(method = "rebuild", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa$prepareDualView(CallbackInfo ci)
    {
        this.halfmasa$dualViewWasEnabled = this.halfmasa$isDualViewEnabled();
        if (!this.halfmasa$dualViewWasEnabled)
        {
            return;
        }

        try
        {
            int screenHeight = ((Screen) (Object) this).height;
            for (Object child : ((Screen) (Object) this).children())
            {
                if (child instanceof AbstractWidget widget &&
                        widget.getY() >= halfmasa$LIST_TOP && widget.getY() < screenHeight - halfmasa$LIST_BOTTOM_MARGIN)
                {
                    widget.visible = false;
                    widget.active = false;
                }
            }
        }
        catch (RuntimeException exception)
        {
            this.halfmasa$warn(exception);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa$refreshAfterSettingChange(CallbackInfo ci)
    {
        boolean enabled = this.halfmasa$isDualViewEnabled();
        if (enabled != this.halfmasa$dualViewWasEnabled)
        {
            try
            {
                Method rebuild = this.getClass().getDeclaredMethod("rebuild");
                rebuild.setAccessible(true);
                rebuild.invoke(this);
            }
            catch (ReflectiveOperationException | RuntimeException exception)
            {
                this.halfmasa$warn(exception);
            }
        }
    }

    @Inject(method = "renderAfterWidgets", at = @At("TAIL"), require = 0, remap = false)
    private void halfmasa$renderDualView(@Coerce Object guiDraw, int mouseX, int mouseY, float partialTick,
                                         CallbackInfo ci)
    {
        if (!this.halfmasa$isDualViewEnabled())
        {
            return;
        }

        try
        {
            this.halfmasa$drawPanes(guiDraw);
        }
        catch (ReflectiveOperationException | RuntimeException | LinkageError exception)
        {
            this.halfmasa$warn(exception);
        }
    }

//#if MC >= 1.21.10
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void halfmasa$captureDualPaneClick(MouseButtonEvent event, boolean doubleClick,
                                                CallbackInfoReturnable<Boolean> cir)
    {
        if (!this.halfmasa$isDualViewEnabled())
        {
            return;
        }

        Screen screen = (Screen) (Object) this;
        if (event.y() >= halfmasa$LIST_TOP && event.y() < screen.height - halfmasa$LIST_BOTTOM_MARGIN &&
                event.x() >= 12 && event.x() < screen.width - 12)
        {
            // The dual panes are a read-only combined view. Do not let clicks hit
            // the native row action widgets hidden beneath these panes.
            cir.setReturnValue(true);
        }
    }

//#else
//$$     @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
//$$     private void halfmasa$captureDualPaneClick(double mouseX, double mouseY, int button,
//$$                                                 CallbackInfoReturnable<Boolean> cir)
//$$     {
//$$         if (!this.halfmasa$isDualViewEnabled())
//$$         {
//$$             return;
//$$         }

//$$         Screen screen = (Screen) (Object) this;
//$$         if (mouseY >= halfmasa$LIST_TOP && mouseY < screen.height - halfmasa$LIST_BOTTOM_MARGIN &&
//$$                 mouseX >= 12 && mouseX < screen.width - 12)
//$$         {
//$$             // The dual panes are a read-only combined view. Do not let clicks hit
//$$             // the native row action widgets hidden beneath these panes.
//$$             cir.setReturnValue(true);
//$$         }
//$$     }

//#endif

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void halfmasa$scrollDualPane(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
                                         CallbackInfoReturnable<Boolean> cir)
    {
        if (!this.halfmasa$isDualViewEnabled() || verticalAmount == 0.0D)
        {
            return;
        }

        Screen screen = (Screen) (Object) this;
        if (mouseY < halfmasa$LIST_TOP || mouseY >= screen.height - halfmasa$LIST_BOTTOM_MARGIN ||
                mouseX < 12 || mouseX >= screen.width - 12)
        {
            return;
        }

        boolean local = this.halfmasa$isSideBySide()
                ? mouseX < screen.width / 2.0D
                : mouseY < (halfmasa$LIST_TOP + screen.height - halfmasa$LIST_BOTTOM_MARGIN) / 2.0D;
        int step = (int) Math.signum(verticalAmount);
        if (local)
        {
            this.halfmasa$localScrollOffset = this.halfmasa$clamp(
                    this.halfmasa$localScrollOffset - step, 0, this.halfmasa$localMaxScroll);
        }
        else
        {
            this.halfmasa$sharedScrollOffset = this.halfmasa$clamp(
                    this.halfmasa$sharedScrollOffset - step, 0, this.halfmasa$sharedMaxScroll);
        }
        cir.setReturnValue(true);
    }

    @Unique
    private void halfmasa$drawPanes(Object guiDraw) throws ReflectiveOperationException
    {
        Screen screen = (Screen) (Object) this;
        int width = screen.width;
        int height = screen.height;
        int bottom = Math.max(halfmasa$LIST_TOP + 48, height - halfmasa$LIST_BOTTOM_MARGIN);
        boolean sideBySide = this.halfmasa$isSideBySide();
        Object font = Screen.class.getMethod("getFont").invoke(screen);
        Method fill = guiDraw.getClass().getMethod("fill", int.class, int.class, int.class, int.class, int.class);
        Method drawText = guiDraw.getClass().getMethod(
                "drawTextWithShadow", Font.class, String.class, float.class, float.class, int.class);
        Method trimText = font.getClass().getMethod("plainSubstrByWidth", String.class, int.class);

        List<?> localRows;
        List<?> sharedRows;
        try
        {
            List<?>[] rows = this.halfmasa$loadBothRowLists();
            localRows = rows[0];
            sharedRows = rows[1];
        }
        catch (ReflectiveOperationException | RuntimeException exception)
        {
            this.halfmasa$drawErrorPanes(guiDraw, fill, drawText, font, trimText, width, bottom, sideBySide);
            this.halfmasa$warn(exception);
            return;
        }

        Pane localPane;
        Pane sharedPane;
        if (sideBySide)
        {
            int margin = 12;
            int gap = 10;
            int paneWidth = Math.max(120, (width - margin * 2 - gap) / 2);
            localPane = new Pane(margin, halfmasa$LIST_TOP, paneWidth, bottom - halfmasa$LIST_TOP);
            sharedPane = new Pane(margin + paneWidth + gap, halfmasa$LIST_TOP,
                    Math.max(120, width - margin - (margin + paneWidth + gap)), bottom - halfmasa$LIST_TOP);
        }
        else
        {
            int gap = 8;
            int paneHeight = Math.max(32, (bottom - halfmasa$LIST_TOP - gap) / 2);
            localPane = new Pane(12, halfmasa$LIST_TOP, width - 24, paneHeight);
            sharedPane = new Pane(12, halfmasa$LIST_TOP + paneHeight + gap, width - 24,
                    bottom - (halfmasa$LIST_TOP + paneHeight + gap));
        }

        this.halfmasa$localMaxScroll = this.halfmasa$drawPane(
                guiDraw, fill, drawText, trimText, font, localPane, localRows,
                "halfmasa.conflux_map.local_waypoints", this.halfmasa$localScrollOffset, sideBySide);
        this.halfmasa$sharedMaxScroll = this.halfmasa$drawPane(
                guiDraw, fill, drawText, trimText, font, sharedPane, sharedRows,
                "halfmasa.conflux_map.shared_waypoints", this.halfmasa$sharedScrollOffset, sideBySide);
        this.halfmasa$localScrollOffset = this.halfmasa$clamp(
                this.halfmasa$localScrollOffset, 0, this.halfmasa$localMaxScroll);
        this.halfmasa$sharedScrollOffset = this.halfmasa$clamp(
                this.halfmasa$sharedScrollOffset, 0, this.halfmasa$sharedMaxScroll);
    }

    @Unique
    private int halfmasa$drawPane(Object guiDraw, Method fill, Method drawText, Method trimText, Object font,
                                  Pane pane, List<?> rows, String titleKey, int scrollOffset,
                                  boolean sideBySide) throws ReflectiveOperationException
    {
        fill.invoke(guiDraw, pane.x(), pane.y(), pane.x() + pane.width(), pane.y() + pane.height(), 0xFF14171D);
        fill.invoke(guiDraw, pane.x(), pane.y(), pane.x() + pane.width(), pane.y() + 23, 0xFF2A3440);

        String title = net.minecraft.network.chat.Component.translatable(titleKey).getString();
        int rowHeight = sideBySide ? 30 : 28;
        int firstRowY = pane.y() + 27;
        int visibleRows = Math.max(0, (pane.y() + pane.height() - firstRowY - 4) / rowHeight);
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        int start = this.halfmasa$clamp(scrollOffset, 0, maxScroll);
        int end = Math.min(rows.size(), start + visibleRows);
        title += "  (" + rows.size() + ")";
        this.halfmasa$drawText(guiDraw, drawText, trimText, font, title,
                pane.x() + 8, pane.y() + 7, pane.width() - 16, 0xFFFFFFFF);

        Method nameMethod = rows.isEmpty() ? null : this.halfmasa$rowMethod(rows.get(0), "name");
        Method secondaryMethod = rows.isEmpty() ? null : this.halfmasa$rowMethod(rows.get(0), "secondaryText");
        Method dimensionMethod = rows.isEmpty() ? null : this.halfmasa$rowMethod(rows.get(0), "dimensionText");
        Method distanceMethod = rows.isEmpty() ? null : this.halfmasa$rowMethod(rows.get(0), "distance");
        for (int index = start; index < end; index++)
        {
            Object row = rows.get(index);
            int y = firstRowY + (index - start) * rowHeight;
            if (((index - start) & 1) == 0)
            {
                fill.invoke(guiDraw, pane.x() + 2, y - 1, pane.x() + pane.width() - 2,
                        y + rowHeight - 1, 0x66343B45);
            }

            String name = String.valueOf(nameMethod.invoke(row));
            String secondary = String.valueOf(secondaryMethod.invoke(row));
            String dimension = String.valueOf(dimensionMethod.invoke(row));
            double distance = ((Number) distanceMethod.invoke(row)).doubleValue();
            String detail = dimension;
            if (!secondary.isBlank())
            {
                detail = detail.isBlank() ? secondary : detail + " · " + secondary;
            }
            if (Double.isFinite(distance))
            {
                detail = detail.isBlank() ? Math.round(distance) + "m" : detail + " · " + Math.round(distance) + "m";
            }

            int textWidth = pane.width() - 18;
            this.halfmasa$drawText(guiDraw, drawText, trimText, font, name,
                    pane.x() + 8, y + 2, textWidth, 0xFFFFFFFF);
            if (rowHeight >= 28 && !detail.isBlank())
            {
                this.halfmasa$drawText(guiDraw, drawText, trimText, font, detail,
                        pane.x() + 8, y + 15, textWidth, 0xFFB8C2CC);
            }
        }

        if (rows.isEmpty())
        {
            String empty = net.minecraft.network.chat.Component.translatable(
                    "halfmasa.conflux_map.dual_waypoints.empty").getString();
            this.halfmasa$drawText(guiDraw, drawText, trimText, font, empty,
                    pane.x() + 8, firstRowY + 6, pane.width() - 16, 0xFFB8C2CC);
        }
        else if (maxScroll > 0)
        {
            String scrollHint = net.minecraft.network.chat.Component.translatable(
                    "halfmasa.conflux_map.dual_waypoints.scroll").getString();
            String page = (start + 1) + "-" + end + "/" + rows.size() + "  ·  " + scrollHint;
            this.halfmasa$drawText(guiDraw, drawText, trimText, font, page,
                    pane.x() + 8, pane.y() + pane.height() - 13, pane.width() - 16, 0xFF8F9AA5);
        }

        return maxScroll;
    }

    @Unique
    private void halfmasa$drawErrorPanes(Object guiDraw, Method fill, Method drawText, Object font,
                                         Method trimText, int width, int bottom, boolean sideBySide)
            throws ReflectiveOperationException
    {
        String titleLocal = net.minecraft.network.chat.Component.translatable(
                "halfmasa.conflux_map.local_waypoints").getString();
        String titleShared = net.minecraft.network.chat.Component.translatable(
                "halfmasa.conflux_map.shared_waypoints").getString();
        String error = net.minecraft.network.chat.Component.translatable(
                "halfmasa.conflux_map.dual_waypoints.unavailable").getString();

        if (sideBySide)
        {
            int paneWidth = (width - 34) / 2;
            this.halfmasa$drawErrorPane(guiDraw, fill, drawText, trimText, font,
                    new Pane(12, halfmasa$LIST_TOP, paneWidth, bottom - halfmasa$LIST_TOP), titleLocal, error);
            this.halfmasa$drawErrorPane(guiDraw, fill, drawText, trimText, font,
                    new Pane(22 + paneWidth, halfmasa$LIST_TOP, width - 34 - paneWidth,
                            bottom - halfmasa$LIST_TOP), titleShared, error);
        }
        else
        {
            int paneHeight = (bottom - halfmasa$LIST_TOP - 8) / 2;
            this.halfmasa$drawErrorPane(guiDraw, fill, drawText, trimText, font,
                    new Pane(12, halfmasa$LIST_TOP, width - 24, paneHeight), titleLocal, error);
            this.halfmasa$drawErrorPane(guiDraw, fill, drawText, trimText, font,
                    new Pane(12, halfmasa$LIST_TOP + paneHeight + 8, width - 24,
                            bottom - (halfmasa$LIST_TOP + paneHeight + 8)), titleShared, error);
        }
    }

    @Unique
    private void halfmasa$drawErrorPane(Object guiDraw, Method fill, Method drawText, Method trimText, Object font,
                                        Pane pane, String title, String error) throws ReflectiveOperationException
    {
        fill.invoke(guiDraw, pane.x(), pane.y(), pane.x() + pane.width(), pane.y() + pane.height(), 0xFF14171D);
        fill.invoke(guiDraw, pane.x(), pane.y(), pane.x() + pane.width(), pane.y() + 23, 0xFF2A3440);
        this.halfmasa$drawText(guiDraw, drawText, trimText, font, title,
                pane.x() + 8, pane.y() + 7, pane.width() - 16, 0xFFFFFFFF);
        this.halfmasa$drawText(guiDraw, drawText, trimText, font, error,
                pane.x() + 8, pane.y() + 34, pane.width() - 16, 0xFFFF9090);
    }

    @Unique
    private void halfmasa$drawText(Object guiDraw, Method drawText, Method trimText, Object font,
                                   String text, int x, int y, int maxWidth, int color)
            throws ReflectiveOperationException
    {
        String trimmed = (String) trimText.invoke(font, text, Math.max(0, maxWidth));
        drawText.invoke(guiDraw, font, trimmed, (float) x, (float) y, color);
    }

    @Unique
    private List<?>[] halfmasa$loadBothRowLists() throws ReflectiveOperationException
    {
        Field tabField = this.halfmasa$field("tab");
        Object previousTab = tabField.get(this);
        Object gameBridge = this.halfmasa$field("gameBridge").get(this);
        Object session = gameBridge.getClass().getMethod("session").invoke(gameBridge);
        Object dimension = session.getClass().getMethod("dimension").invoke(session);
        Object playerView = ((Optional<?>) gameBridge.getClass().getMethod("player").invoke(gameBridge))
                .orElse(null);
        double x = playerView == null ? 0.0D : ((Number) playerView.getClass().getMethod("x").invoke(playerView)).doubleValue();
        double y = playerView == null ? 0.0D : ((Number) playerView.getClass().getMethod("y").invoke(playerView)).doubleValue();
        double z = playerView == null ? 0.0D : ((Number) playerView.getClass().getMethod("z").invoke(playerView)).doubleValue();
        Method buildRows = this.getClass().getDeclaredMethod(
                "buildRows", dimension.getClass(), double.class, double.class, double.class);
        buildRows.setAccessible(true);

        try
        {
            Class<?> tabClass = previousTab.getClass();
            tabField.set(this, tabClass.getField("LOCAL").get(null));
            List<?> localRows = (List<?>) buildRows.invoke(this, dimension, x, y, z);
            tabField.set(this, tabClass.getField("PUBLIC").get(null));
            List<?> sharedRows = (List<?>) buildRows.invoke(this, dimension, x, y, z);

            String search = "";
            Object searchField = this.halfmasa$field("searchField").get(this);
            if (searchField != null)
            {
                search = String.valueOf(searchField.getClass().getMethod("getValue").invoke(searchField));
            }
            if (!search.isBlank())
            {
                String query = search.toLowerCase(Locale.ROOT);
                localRows = this.halfmasa$filterRows(localRows, query);
                sharedRows = this.halfmasa$filterRows(sharedRows, query);
            }

            this.halfmasa$sortRowsByDistance(localRows);
            this.halfmasa$sortRowsByDistance(sharedRows);
            return new List<?>[]{localRows, sharedRows};
        }
        finally
        {
            tabField.set(this, previousTab);
        }
    }

    @Unique
    private List<?> halfmasa$filterRows(List<?> rows, String query) throws ReflectiveOperationException
    {
        List<Object> filtered = new ArrayList<>();
        if (rows.isEmpty())
        {
            return filtered;
        }

        Method matchesSearch = this.getClass().getDeclaredMethod(
                "matchesSearch", rows.get(0).getClass(), String.class);
        matchesSearch.setAccessible(true);
        for (Object row : rows)
        {
            if (Boolean.TRUE.equals(matchesSearch.invoke(null, row, query)))
            {
                filtered.add(row);
            }
        }
        return filtered;
    }

    @Unique
    private void halfmasa$sortRowsByDistance(List<?> rows) throws ReflectiveOperationException
    {
        if (rows.size() < 2)
        {
            return;
        }
        Method distance = this.halfmasa$rowMethod(rows.get(0), "distance");
        @SuppressWarnings("unchecked")
        List<Object> mutableRows = (List<Object>) rows;
        mutableRows.sort(Comparator.comparingDouble(row -> {
            try
            {
                return ((Number) distance.invoke(row)).doubleValue();
            }
            catch (ReflectiveOperationException exception)
            {
                return Double.POSITIVE_INFINITY;
            }
        }));
    }

    @Unique
    private Method halfmasa$rowMethod(Object row, String name) throws ReflectiveOperationException
    {
        Method method = row.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method;
    }

    @Unique
    private Field halfmasa$field(String name) throws ReflectiveOperationException
    {
        for (Class<?> type = this.getClass(); type != null; type = type.getSuperclass())
        {
            try
            {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            }
            catch (NoSuchFieldException ignored)
            {
                // Continue through the screen hierarchy.
            }
        }
        throw new NoSuchFieldException(name);
    }

    @Unique
    private boolean halfmasa$isDualViewEnabled()
    {
        return Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue() &&
                Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue();
    }

    @Unique
    private boolean halfmasa$isSideBySide()
    {
        return Configs.CONFLUX_MAP_WAYPOINT_LIST_LAYOUT.getOptionListValue() ==
                ConfluxMapWaypointListLayout.SIDE_BY_SIDE;
    }

    @Unique
    private int halfmasa$clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    @Unique
    private void halfmasa$warn(Throwable exception)
    {
        if (!halfmasa$warnedContract)
        {
            halfmasa$warnedContract = true;
            XaeroWorldBinding.LOGGER.warn(
                    "Conflux Map changed its waypoint-list internals; the simultaneous local/shared view is unavailable",
                    exception);
        }
    }

    @Unique
    private record Pane(int x, int y, int width, int height)
    {
    }
}

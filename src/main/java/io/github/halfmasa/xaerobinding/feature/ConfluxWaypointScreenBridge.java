package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Version-specific optional adapter. Storage, icons and permissions stay owned by Conflux Map. */
public final class ConfluxWaypointScreenBridge
{
    public record Row(Object nativeRow, UUID id, String source, String scope, String name,
                      String detail, Object local, Object shared, Object renderEntry)
    {
        public boolean sameIdentity(Row other)
        { return id.equals(other.id) && source.equals(other.source) && scope.equals(other.scope); }
    }
    public record Access(boolean available, Component explanation) {}
    private final Screen screen;
    private final Field tab, search, dimensionDropdown, setDropdown, dimensionFilter, totalCount;
    private final Object localTab, publicTab, gameBridge, service, sharedService;
    private final Method buildRows, matchesSearch, localCurrent, teleport, teleportAccess, openEdit, openSharedEdit;
    private final Method local, shared, sibling, id, name, secondary, dimension, distance, renderEntry;
    private final Method markerDraw, fill, text, background, tooltip;
    private final Object noVerticalRelation;
    private boolean collecting;
    //#if MC >= 1.21.1
    private final java.util.Map<Button, Row> publicationButtons = new java.util.IdentityHashMap<>();

    public void clearPublicationButtons() { publicationButtons.clear(); }

    public boolean selected(Row row) throws ReflectiveOperationException
    { return ((java.util.Set<?>)field(screen.getClass(), "selectedWaypointIds").get(screen)).contains(row.id); }

    public void toggleSelection(Row target) throws ReflectiveOperationException
    {
        for (Row row : rows(false)) if (row.sameIdentity(target) && row.local != null)
        {
            Object store = localCurrent.invoke(service);
            if (store != null && (boolean)method(store.getClass(), "persistenceWritable").invoke(store))
                named(screen.getClass(), "toggleSelected").invoke(screen, store, row.id);
            return;
        }
    }

    private record Publication(Access access, String labelKey) {}

    private Publication publicationState(Row row) throws ReflectiveOperationException
    {
        for (String[] state : new String[][] {
                {"isLocationShared", "halfmasa.conflux_map.published"},
                {"isCreatePending", "halfmasa.conflux_map.publishing"} })
            if ((boolean)method(sharedService.getClass(), state[0], row.local.getClass()).invoke(sharedService, row.local))
                return new Publication(new Access(false, Component.translatable(state[1])), state[1]);
        boolean allowed = (boolean)method(sharedService.getClass(), "canCreate").invoke(sharedService);
        Object reason = method(sharedService.getClass(), "createDisabledReasonKey").invoke(sharedService);
        return new Publication(new Access(allowed, Component.translatable(allowed
                ? "halfmasa.conflux_map.publish_hint" : String.valueOf(reason))), "halfmasa.conflux_map.publish");
    }

    private Access publishAccess(Row row) throws ReflectiveOperationException
    { return publicationState(row).access; }

    public void refreshPublicationButtons() throws ReflectiveOperationException
    {
        for (var entry : publicationButtons.entrySet())
        {
            Publication state = publicationState(entry.getValue());
            Button button = entry.getKey();
            button.active = state.access.available;
            button.setMessage(Component.translatable(state.labelKey));
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(state.access.explanation));
        }
    }

    private void publish(Row target) throws ReflectiveOperationException
    {
        if (!io.github.halfmasa.xaerobinding.config.Configs.CONFLUX_MAP_EXTENSIONS.getBooleanValue()
                || !io.github.halfmasa.xaerobinding.config.Configs.CONFLUX_MAP_QUICK_PUBLISH.getBooleanValue()) return;
        for (Row row : rows(false)) if (row.sameIdentity(target) && row.local != null)
        {
            if (publishAccess(row).available)
                method(sharedService.getClass(), "create", row.local.getClass()).invoke(sharedService, row.local);
            refreshPublicationButtons();
            return;
        }
    }
    //#endif

    public boolean collecting() { return collecting; }

    public List<Object> combinedNativeRows(boolean serverFirst) throws ReflectiveOperationException
    { return new ArrayList<>(rows(serverFirst).stream().map(Row::nativeRow).toList()); }

    public int sourceRank(Object row, boolean serverFirst)
    {
        try { return (shared.invoke(row) != null) == serverFirst ? 0 : 1; }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot identify native waypoint source", exception); }
    }

    public Button button(Component label, Button.OnPress press, int x, int y, int width) throws ReflectiveOperationException
    {
        Class<?> type = Class.forName("cn.net.rms.confluxmap.mc.ui.widget.ConfluxTextButton", false, screen.getClass().getClassLoader());
        return (Button)type.getConstructor(int.class, int.class, int.class, int.class, Component.class, Button.OnPress.class)
                .newInstance(x, y, Math.max(1, width), 20, label, press);
    }

    /** Native widget instances retain theme, callbacks, delete confirmation and access rules. */
    public List<AbstractWidget> actionWidgets(Row row) throws ReflectiveOperationException
    {
        var before = new java.util.HashSet<>(screen.children());
        named(screen.getClass(), "addRowWidgets").invoke(screen, row.nativeRow, localCurrent.invoke(service));
        var widgets = new ArrayList<>(screen.children().stream().filter(child -> !before.contains(child) && child instanceof AbstractWidget)
                .map(child -> (AbstractWidget)child).toList());
        //#if MC >= 1.21.1
        if (row.local != null && io.github.halfmasa.xaerobinding.config.Configs.CONFLUX_MAP_QUICK_PUBLISH.getBooleanValue())
        {
            Button publish = button(Component.translatable("halfmasa.conflux_map.publish"), ignored -> {
                try { publish(row); }
                catch (ReflectiveOperationException error)
                {
                    io.github.halfmasa.xaerobinding.XaeroWorldBinding.LOGGER.warn("Cannot publish Conflux waypoint copy", error);
                    ignored.active = false;
                    ignored.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("halfmasa.conflux_map.publish_unavailable")));
                }
            }, 0, 0, 108);
            ((io.github.halfmasa.xaerobinding.mixin.ScreenAccessor)screen).halfmasa$addRenderableWidget(publish);
            publicationButtons.put(publish, row); widgets.add(publish);
            refreshPublicationButtons();
        }
        //#endif
        // Popup widgets are drawn above the header while retaining native input/narration.
        ((io.github.halfmasa.xaerobinding.mixin.ScreenAccessor)screen).halfmasa$getRenderables().removeAll(widgets);
        return widgets;
    }

    public void renderActions(Object draw, List<AbstractWidget> widgets, int mx, int my, float tick) throws ReflectiveOperationException
    {
        //#if MC >= 26.0
        var context = (net.minecraft.client.gui.GuiGraphicsExtractor)method(draw.getClass(), "context").invoke(draw);
        //#else
        //$$ var context = (net.minecraft.client.gui.GuiGraphics)method(draw.getClass(), "context").invoke(draw);
        //#endif
        //#if MC >= 26.0
        for (var widget : widgets) widget.extractRenderState(context, mx, my, tick);
        //#else
        //$$ for (var widget : widgets) widget.render(context, mx, my, tick);
        //#endif
    }

    public void removeActions(List<AbstractWidget> widgets) throws ReflectiveOperationException
    {
        var icons = (java.util.Set<?>)field(screen.getClass(), "iconActions").get(screen);
        icons.removeAll(widgets);
        //#if MC >= 1.21.1
        for (var widget : widgets) publicationButtons.remove(widget);
        //#endif
        for (var widget : widgets)
        {
            if (screen.getFocused() == widget) screen.setFocused(null);
            ((io.github.halfmasa.xaerobinding.mixin.ScreenAccessor)screen).halfmasa$removeWidget(widget);
        }
    }

    public String dimensionText(Row row) throws ReflectiveOperationException
    { return String.valueOf(dimension.invoke(row.nativeRow)); }
    public String distanceText(Row row) throws ReflectiveOperationException
    { return String.valueOf(named(screen.getClass(), "formatDistance").invoke(null, distance.invoke(row.nativeRow))); }
    public String secondaryText(Row row) throws ReflectiveOperationException
    { return String.valueOf(secondary.invoke(row.nativeRow)); }
    public String coordinates(Row row) throws ReflectiveOperationException
    {
        Object entry = row.renderEntry;
        return Math.round(coordinate(entry, "x")) + ", " + Math.round(coordinate(entry, "y")) + ", " + Math.round(coordinate(entry, "z"));
    }

    public int settingsToken() throws ReflectiveOperationException
    {
        Object config = field(screen.getClass(), "config").get(screen);
        return java.util.Objects.hash(field(config.getClass(), "localWaypointsVisible").get(config),
                field(config.getClass(), "crossWorldWaypointsVisible").get(config),
                field(config.getClass(), "teleportCommand").get(config),
                //#if MC >= 1.21.1
                io.github.halfmasa.xaerobinding.config.Configs.CONFLUX_MAP_QUICK_PUBLISH.getBooleanValue(),
                //#endif
                net.minecraft.client.Minecraft.getInstance().options.languageCode);
    }

    public ConfluxWaypointScreenBridge(Screen screen) throws ReflectiveOperationException
    {
        this.screen = screen;
        Class<?> type = screen.getClass();
        tab = field(type, "tab"); search = field(type, "searchField");
        dimensionDropdown = field(type, "dimensionDropdownOpen"); setDropdown = field(type, "openSetDropdown");
        dimensionFilter = field(type, "dimensionFilter"); totalCount = field(type, "totalRowCount");
        localTab = tab.getType().getField("LOCAL").get(null); publicTab = tab.getType().getField("PUBLIC").get(null);
        gameBridge = field(type, "gameBridge").get(screen);
        service = field(type, "waypointService").get(screen);
        sharedService = field(type, "sharedWaypoints").get(screen);
        localCurrent = method(service.getClass(), "current");
        buildRows = named(type, "buildRows"); matchesSearch = named(type, "matchesSearch");
        teleport = named(type, "teleport"); teleportAccess = named(type, "teleportAccess");
        openEdit = named(type, "openEdit"); openSharedEdit = named(type, "openSharedEdit");
        Class<?> row = teleport.getParameterTypes()[0];
        local = method(row, "local"); shared = method(row, "shared"); sibling = method(row, "sibling");
        id = method(row, "id"); name = method(row, "name"); secondary = method(row, "secondaryText");
        dimension = method(row, "dimensionText"); distance = method(row, "distance"); renderEntry = method(row, "renderEntry");
        ClassLoader loader = type.getClassLoader();
        Class<?> draw = Class.forName("cn.net.rms.confluxmap.mc.ui.GuiDraw", false, loader);
        Class<?> relation = Class.forName("cn.net.rms.confluxmap.core.waypoint.WaypointVerticalRelation", false, loader);
        noVerticalRelation = relation.getField("NONE").get(null);
        markerDraw = named(Class.forName("cn.net.rms.confluxmap.mc.ui.WaypointMarkerRenderer", false, loader), "draw");
        fill = draw.getMethod("fill", int.class, int.class, int.class, int.class, int.class);
        text = draw.getMethod("drawTextWithShadow", Font.class, String.class, float.class, float.class, int.class);
        background = draw.getMethod("renderBackground", Screen.class, int.class, int.class, float.class);
        tooltip = draw.getMethod("drawTooltip", Screen.class, Font.class, Component.class, int.class, int.class);
    }

    //#if MC >= 1.21.1
    public int viewToken() throws ReflectiveOperationException
    { return java.util.Objects.hash(tab(), dimensionFilter.get(screen), field(screen.getClass(), "selectedSetFilter").get(screen)); }

    public String worldKey() throws ReflectiveOperationException
    {
        Object store = localCurrent.invoke(service);
        if (store == null) return null;
        Object world = method(store.getClass(), "world").invoke(store);
        if (!(boolean)method(world.getClass(), "isPresent").invoke(world)) return null;
        return method(world.getClass(), "serverId").invoke(world) + "\n"
                + method(world.getClass(), "worldId").invoke(world);
    }

    public ConfluxWaypointListMemory.State snapshot(Object preferredTab, int compactScroll) throws ReflectiveOperationException
    {
        Object filter = dimensionFilter.get(screen);
        Object dim = method(filter.getClass(), "dimension").invoke(filter);
        return new ConfluxWaypointListMemory.State(String.valueOf(preferredTab == null ? tab() : preferredTab),
                String.valueOf(method(filter.getClass(), "mode").invoke(filter)), dim == null ? null : dim.toString(),
                (String)field(screen.getClass(), "selectedSetFilter").get(screen), query(),
                field(screen.getClass(), "scrollOffset").getInt(screen), compactScroll);
    }

    public Object restore(ConfluxWaypointListMemory.State state) throws ReflectiveOperationException
    {
        Object requested = "PUBLIC".equals(state.tab()) ? publicTab : localTab;
        Object availability = method(sharedService.getClass(), "availability").invoke(sharedService);
        if (requested == publicTab && !(boolean)method(availability.getClass(), "enabled").invoke(availability)) requested = localTab;
        tab(requested);
        Object filter = method(dimensionFilter.getType(), "current").invoke(null);
        if ("ALL".equals(state.dimensionMode())) filter = method(dimensionFilter.getType(), "all").invoke(null);
        else if ("ONLY".equals(state.dimensionMode()))
        {
            Object session = method(gameBridge.getClass(), "session").invoke(gameBridge);
            Object dim = method(session.getClass(), "dimension").invoke(session);
            for (Object known : (List<?>)named(screen.getClass(), "knownDimensions").invoke(screen, dim))
                if (known.toString().equals(state.dimension()))
                    filter = method(dimensionFilter.getType(), "only", known.getClass()).invoke(null, known);
        }
        dimensionFilter.set(screen, filter);
        field(screen.getClass(), "selectedSetFilter").set(screen, state.set());
        var box = (net.minecraft.client.gui.components.EditBox)search.get(screen);
        box.setValue(state.search());
        field(screen.getClass(), "observedSearch").set(screen, box.getValue());
        field(screen.getClass(), "scrollOffset").setInt(screen, state.nativeScroll());
        return requested;
    }
    //#endif

    public Object tab() throws IllegalAccessException { return tab.get(screen); }
    public void tab(Object value) throws IllegalAccessException { tab.set(screen, value); }
    public void useLocalTab() throws IllegalAccessException { tab(localTab); }
    public String query() throws ReflectiveOperationException
    {
        Object box = search.get(screen);
        return box == null ? "" : ((net.minecraft.client.gui.components.EditBox)box).getValue();
    }
    public boolean dropdownOpen() throws IllegalAccessException
    { return dimensionDropdown.getBoolean(screen) || setDropdown.get(screen) != null; }
    public void rebuild() throws ReflectiveOperationException { method(screen.getClass(), "rebuild").invoke(screen); }
    public int listTop() throws ReflectiveOperationException { return (int)method(screen.getClass(), "listTop").invoke(screen); }
    public Component countLabel(int count) throws ReflectiveOperationException
    {
        totalCount.setInt(screen, count);
        Object session = method(gameBridge.getClass(), "session").invoke(gameBridge);
        Object dim = method(session.getClass(), "dimension").invoke(session);
        String label = String.valueOf(named(screen.getClass(), "dimensionFilterLabel").invoke(screen, dimensionFilter.get(screen), dim));
        return Component.translatable("confluxmap.screen.waypoints.dimension_filter", label, count).copy().append(" ▾");
    }

    public List<Row> rows(boolean serverFirst) throws ReflectiveOperationException
    {
        Object session = method(gameBridge.getClass(), "session").invoke(gameBridge);
        Object currentDimension = method(session.getClass(), "dimension").invoke(session);
        Object player = ((Optional<?>)method(gameBridge.getClass(), "player").invoke(gameBridge)).orElse(null);
        double x = coordinate(player, "x"), y = coordinate(player, "y"), z = coordinate(player, "z");
        String query = query().toLowerCase(java.util.Locale.ROOT);
        Object previous = tab();
        boolean wasCollecting = collecting;
        collecting = true;
        List<Row> personal, server;
        try
        {
            //#if MC >= 1.21.1
            if (!io.github.halfmasa.xaerobinding.config.Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.getBooleanValue())
                return List.copyOf(readRows(currentDimension, x, y, z, query));
            //#endif
            tab(localTab); personal = readRows(currentDimension, x, y, z, query);
            tab(publicTab); server = readRows(currentDimension, x, y, z, query);
        }
        finally { collecting = wasCollecting; tab(previous); }
        List<Row> ordered = new ArrayList<>(personal.size() + server.size());
        ordered.addAll(serverFirst ? server : personal); ordered.addAll(serverFirst ? personal : server);
        return List.copyOf(ordered);
    }

    private List<Row> readRows(Object dim, double x, double y, double z, String query) throws ReflectiveOperationException
    {
        List<Object> nativeRows = new ArrayList<>((List<?>)buildRows.invoke(screen, dim, x, y, z));
        List<Row> result = new ArrayList<>();
        // Propagate contract errors rather than silently dropping or misordering entries.
        var distances = new java.util.IdentityHashMap<Object, Double>();
        for (Object row : nativeRows) distances.put(row, ((Number)distance.invoke(row)).doubleValue());
        nativeRows.sort(Comparator.comparingDouble(row -> distances.get(row)));
        for (Object row : nativeRows)
        {
            if (!query.isBlank() && !Boolean.TRUE.equals(matchesSearch.invoke(null, row, query))) continue;
            Object owned = local.invoke(row), published = shared.invoke(row), guest = sibling.invoke(row);
            String source = published != null ? "server" : guest != null ? "sibling" : "personal";
            String scope = guest == null ? "" : String.valueOf(secondary.invoke(row));
            double blocks = distances.get(row);
            String detail = String.valueOf(dimension.invoke(row)) + " · " + secondary.invoke(row);
            detail += " · " + String.valueOf(named(screen.getClass(), "formatDistance").invoke(null, blocks));
            result.add(new Row(row, (UUID)id.invoke(row), source, scope, String.valueOf(name.invoke(row)),
                    detail, owned, published, renderEntry.invoke(row)));
        }
        return result;
    }

    public Access editAccess(Row row) throws ReflectiveOperationException
    {
        if (row.shared != null)
        {
            boolean allowed = (boolean)method(sharedService.getClass(), "canUpdate", row.shared.getClass()).invoke(sharedService, row.shared);
            String reason = String.valueOf(method(sharedService.getClass(), "updateDisabledReasonKey", row.shared.getClass()).invoke(sharedService, row.shared));
            return new Access(allowed, Component.translatable(allowed ? "confluxmap.screen.waypoints.edit" : reason));
        }
        Object store = localCurrent.invoke(service);
        boolean allowed = row.local != null && store != null && (boolean)method(store.getClass(), "persistenceWritable").invoke(store);
        return new Access(allowed, Component.translatable(allowed ? "confluxmap.screen.waypoints.edit" : "confluxmap.screen.waypoints.read_only"));
    }

    public Access teleportAccess(Row row) throws ReflectiveOperationException
    {
        Object access = teleportAccess.invoke(screen, row.nativeRow);
        boolean available = (boolean)method(access.getClass(), "available").invoke(access);
        String key = available ? "confluxmap.screen.waypoints.teleport"
                : (String)method(access.getClass(), "reasonKey").invoke(access);
        return new Access(available, Component.translatable(key));
    }

    public void act(Row target, boolean edit, boolean serverFirst) throws ReflectiveOperationException
    {
        // Resolve by identity at click time: synchronization or sorting cannot redirect a click.
        for (Row latest : rows(serverFirst)) if (latest.sameIdentity(target))
        {
            if (edit)
            {
                if (!editAccess(latest).available) return;
                if (latest.shared != null) openSharedEdit.invoke(screen, latest.shared);
                else openEdit.invoke(screen, localCurrent.invoke(service), latest.local);
            }
            else if (teleportAccess(latest).available) teleport.invoke(screen, latest.nativeRow);
            return;
        }
    }

    public void background(Object draw, int mx, int my, float tick) throws ReflectiveOperationException
    { background.invoke(draw, screen, mx, my, tick); }
    public void fill(Object draw, int x, int y, int right, int bottom, int color) throws ReflectiveOperationException
    { fill.invoke(draw, x, y, right, bottom, color); }
    public void text(Object draw, String value, float x, float y, int color) throws ReflectiveOperationException
    { text.invoke(draw, net.minecraft.client.Minecraft.getInstance().font, value, x, y, color); }
    public void marker(Object draw, Row row, float x, float y, boolean hovered) throws ReflectiveOperationException
    { markerDraw.invoke(null, draw, net.minecraft.client.Minecraft.getInstance().font, row.renderEntry, x, y, 6F, 1F, hovered, noVerticalRelation); }
    public void tooltip(Object draw, Component value, int mx, int my) throws ReflectiveOperationException
    { tooltip.invoke(draw, screen, net.minecraft.client.Minecraft.getInstance().font, value, mx, my); }

    private static double coordinate(Object player, String name) throws ReflectiveOperationException
    { return player == null ? 0 : ((Number)method(player.getClass(), name).invoke(player)).doubleValue(); }
    private static Method method(Class<?> type, String name, Class<?>... args) throws NoSuchMethodException
    {
        // Public APIs can be inherited from interfaces (GameBridge.player is a default method).
        try { var method = type.getMethod(name, args); method.setAccessible(true); return method; }
        catch (NoSuchMethodException missing) { /* Continue with private screen/row methods. */ }
        for (Class<?> c = type; c != null; c = c.getSuperclass())
        {
            try { var method = c.getDeclaredMethod(name, args); method.setAccessible(true); return method; }
            catch (NoSuchMethodException missing) { /* Look through the native screen hierarchy. */ }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }
    private static Method named(Class<?> type, String name) throws NoSuchMethodException
    {
        for (var method : type.getDeclaredMethods()) if (method.getName().equals(name)) { method.setAccessible(true); return method; }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }
    private static Field field(Class<?> type, String name) throws NoSuchFieldException
    {
        var field = type.getDeclaredField(name); field.setAccessible(true); return field;
    }
}
//#endif

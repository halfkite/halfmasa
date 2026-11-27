package io.github.halfmasa.xaerobinding.feature.renderfilter;

//#if MC >= 1.21.1
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigStringList;
import io.github.halfmasa.xaerobinding.config.EntityRenderDisableMode;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.compat.RenderPipelineCompatibility;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.BlockRenderListMode;
import io.github.halfmasa.xaerobinding.favorites.MasaFavoritesScreen;
import io.github.halfmasa.xaerobinding.feature.HoldToShow;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.*;
import static io.github.halfmasa.xaerobinding.feature.renderfilter.RenderFilterRules.*;

/** All edits happen on client thread; chunk workers read only published immutable snapshots. */
public final class RenderFilterController implements IClientTickHandler {
    public static final RenderFilterController INSTANCE = new RenderFilterController();
    private record LegacyData(List<Group> blocks, List<Group> entities, String entitySelected) {}
    private static final Gson GSON = new Gson();
    private Snapshot<Block> blockRules = Snapshot.empty();
    private BlockRenderListMode appliedBlockMode;
    private List<String> appliedBlockEntries = List.of();
    private volatile Snapshot<Block> effectiveBlocks = Snapshot.empty();
    private volatile boolean hideBlockEntities, hideOutline;
    private boolean refreshPending, blockMaster, blockHold;
    private RenderFilterController() {}

    //#if MC >= 1.21.11
    public static Identifier id(String raw) {
    //#else
    //$$ public static ResourceLocation id(String raw) {
    //#endif
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        //#if MC >= 1.21.11
        return Identifier.tryParse(s.contains(":") ? s : "minecraft:" + s);
        //#else
        //$$ return ResourceLocation.tryParse(s.contains(":") ? s : "minecraft:" + s);
        //#endif
    }
    //#if MC >= 1.21.11
    private static <T> Optional<T> resolve(String raw, java.util.function.Function<Identifier,Optional<T>> resolver) {
    //#else
    //$$ private static <T> Optional<T> resolve(String raw, java.util.function.Function<ResourceLocation,Optional<T>> resolver) {
    //#endif
        //#if MC >= 1.21.11
        Identifier id = id(raw); return id == null ? Optional.empty() : resolver.apply(id);
        //#else
        //$$ ResourceLocation id = id(raw); return id == null ? Optional.empty() : resolver.apply(id);
        //#endif
    }
    public static void migrateLists(JsonObject old)
    {
        LegacyData legacy = readLegacyGroups();
        if (!old.has(Configs.BLOCK_RENDER_BLACKLIST.getName()) && !old.has(Configs.BLOCK_RENDER_WHITELIST.getName()))
        {
            if (old.get("blockEntries") instanceof com.google.gson.JsonArray entries)
            {
                ConfigStringList target = blockList((BlockRenderListMode) Configs.BLOCK_RENDER_LIST_MODE.getOptionListValue());
                // The shared entries still belong to a list when temporarily in No List mode.
                (target == null ? Configs.BLOCK_RENDER_BLACKLIST : target).setValueFromJsonElement(entries);
            }
            else if (!old.has(Configs.BLOCK_RENDER_LIST_MODE.getName()) && legacy != null
                    && legacy.blocks() != null && !legacy.blocks().isEmpty())
            {
                //#if MC >= 1.21.11
                Snapshot<Identifier> rules = oldRules(legacy.blocks());
                //#else
                //$$ Snapshot<ResourceLocation> rules = oldRules(legacy.blocks());
                //#endif
                var mode = hidesAll(legacy.blocks()) ? BlockRenderListMode.NONE
                        : rules.inverse() ? BlockRenderListMode.WHITELIST : BlockRenderListMode.BLACKLIST;
                Configs.BLOCK_RENDER_LIST_MODE.setOptionListValue(mode);
                ConfigStringList target = blockList(mode);
                if (target != null) target.setStrings(normalized(rules));
            }
        }

        if (legacy != null && legacy.entities() != null && !legacy.entities().isEmpty())
        {
            List<Group> groups = legacy.entities();
            //#if MC >= 1.21.11
            Snapshot<Identifier> rules = oldRules(groups);
            //#else
            //$$ Snapshot<ResourceLocation> rules = oldRules(groups);
            //#endif
            var mode = hidesAll(groups) ? EntityRenderDisableMode.NONE
                    : rules.inverse() ? EntityRenderDisableMode.WHITELIST : EntityRenderDisableMode.BLACKLIST;
            Group selected = groups.stream().filter(group -> group.id().equals(legacy.entitySelected()))
                    .findFirst().orElse(groups.getFirst());
            Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(mode);
            Configs.ENTITY_RENDER_DISABLE_BLACKLIST.setStrings(mode == EntityRenderDisableMode.BLACKLIST
                    ? normalized(rules) : selected.whitelist());
            Configs.ENTITY_RENDER_DISABLE_WHITELIST.setStrings(mode == EntityRenderDisableMode.WHITELIST
                    ? normalized(rules) : selected.blacklist());
        }
        else
        {
            // Pre-group configs used “whitelist” to hide the listed types.
            var oldMode = (EntityRenderDisableMode) Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue();
            if (old.has(Configs.ENTITY_RENDER_DISABLE_MODE.getName())
                    || old.has(Configs.ENTITY_RENDER_DISABLE_BLACKLIST.getName())
                    || old.has(Configs.ENTITY_RENDER_DISABLE_WHITELIST.getName()))
            {
                var previousBlacklist = List.copyOf(Configs.ENTITY_RENDER_DISABLE_BLACKLIST.getStrings());
                Configs.ENTITY_RENDER_DISABLE_BLACKLIST.setStrings(Configs.ENTITY_RENDER_DISABLE_WHITELIST.getStrings());
                Configs.ENTITY_RENDER_DISABLE_WHITELIST.setStrings(previousBlacklist);
                Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(switch (oldMode)
                {
                    case BLACKLIST -> EntityRenderDisableMode.WHITELIST;
                    case WHITELIST -> EntityRenderDisableMode.BLACKLIST;
                    case NONE -> EntityRenderDisableMode.NONE;
                });
            }
        }
    }

    private static LegacyData readLegacyGroups()
    {
        var stored = Configs.RENDER_FILTER_DATA.getStrings();
        if (stored.isEmpty()) return null;
        try
        {
            return GSON.fromJson(stored.getFirst(), LegacyData.class);
        }
        catch (com.google.gson.JsonParseException invalid)
        {
            XaeroWorldBinding.LOGGER.warn("Could not read legacy render filter groups", invalid);
            return null;
        }
    }

    //#if MC >= 1.21.11
    private static Snapshot<Identifier> oldRules(List<Group> groups)
    //#else
    //$$ private static Snapshot<ResourceLocation> oldRules(List<Group> groups)
    //#endif
    {
        return compile(groups, value -> Optional.ofNullable(id(value)));
    }

    private static boolean hidesAll(List<Group> groups)
    {
        return groups.stream().anyMatch(group -> group.enabled() && group.mode() == Mode.NONE);
    }

    //#if MC >= 1.21.11
    private static List<String> normalized(Snapshot<Identifier> rules)
    //#else
    //$$ private static List<String> normalized(Snapshot<ResourceLocation> rules)
    //#endif
    {
        //#if MC >= 1.21.11
        return rules.values().stream().map(Identifier::toString).sorted().toList();
        //#else
        //$$ return rules.values().stream().map(ResourceLocation::toString).sorted().toList();
        //#endif
    }

    private static ConfigStringList blockList(BlockRenderListMode mode)
    {
        return switch (mode)
        {
            case BLACKLIST -> Configs.BLOCK_RENDER_BLACKLIST;
            case WHITELIST -> Configs.BLOCK_RENDER_WHITELIST;
            case NONE -> null;
        };
    }

    private boolean updateBlockList(Minecraft client) {
        var mode = (BlockRenderListMode) Configs.BLOCK_RENDER_LIST_MODE.getOptionListValue();
        var list = blockList(mode);
        List<String> entries = list == null ? List.of() : list.getStrings();
        boolean changedMode = appliedBlockMode != null && appliedBlockMode != mode;
        if (mode == appliedBlockMode && appliedBlockEntries.equals(entries)) return false;
        //#if MC >= 26.2
        boolean overlayOpen = client.gui.overlay() != null;
        //#else
        //$$ boolean overlayOpen = client.getOverlay() != null;
        //#endif
        if (appliedBlockMode != null && (isHalfMasaConfigOpen(MinecraftClientCompat.getScreen(client)) || overlayOpen)) return false;

        boolean changedEntries = appliedBlockMode != null && !appliedBlockEntries.equals(entries);
        appliedBlockMode = mode;
        appliedBlockEntries = List.copyOf(entries);
        Set<Block> blocks = new HashSet<>();
        if (mode != BlockRenderListMode.NONE) {
            for (String entry : entries) resolve(entry, BuiltInRegistries.BLOCK::getOptional).ifPresent(blocks::add);
        }
        blockRules = new Snapshot<>(mode != BlockRenderListMode.BLACKLIST, blocks);
        return changedMode || changedEntries;
    }

    private static boolean isHalfMasaConfigOpen(Screen screen) {
        while (screen instanceof GuiBase gui) {
            if (screen instanceof HalfMasaConfigScreen || screen instanceof MasaFavoritesScreen) return true;
            screen = gui.getParent();
        }
        return false;
    }
    private void publishBlockRules() {
        Snapshot<Block> next=blockMaster && !blockHold ? blockRules : Snapshot.empty();
        boolean be=Configs.BLOCK_RENDER_HIDE_BLOCK_ENTITIES.getBooleanValue();
        boolean outline=Configs.BLOCK_RENDER_HIDE_OUTLINE.getBooleanValue();
        if (!next.equals(effectiveBlocks)) { effectiveBlocks=next; refreshPending=true; }
        hideBlockEntities=be; hideOutline=outline;
    }
    public boolean blockFiltering() { return effectiveBlocks.active(); }
    public boolean hideBlock(BlockState state) { return state != null && !state.isAir() && effectiveBlocks.hides(state.getBlock()); }
    public boolean hideBlockEntity(BlockState state) { return hideBlockEntities && hideBlock(state); }
    public boolean hideOutline(BlockState state) { return hideOutline && hideBlock(state); }
    @Override public void onClientTick(Minecraft mc) {
        if (!Configs.isConfigLoaded()) return;
        blockMaster = RenderPipelineCompatibility.supportsChunkRendering()
                && Configs.CUSTOM_BLOCK_RENDER_DISABLE.getBooleanValue();
        var key=Configs.BLOCK_RENDER_HOLD_SHOW_HOTKEY.getKeybind();
        boolean held=false;
        if (Configs.BLOCK_RENDER_HOLD_SHOW.getBooleanValue() && mc.level!=null && mc.player!=null && mc.player.isAlive() && mc.isWindowActive() && MinecraftClientCompat.getScreen(mc)==null && !key.getKeys().isEmpty()) {
            key.updateIsPressed(); held=key.isKeybindHeld();
        }
        blockHold = held;
        boolean reloadPacks = updateBlockList(mc);
        publishBlockRules();
        if (reloadPacks) {
            refreshPending = false;
            mc.reloadResourcePacks();
        } else if (refreshPending) {
            refreshPending = false;
            if (mc.level != null) MinecraftClientCompat.reloadLevelRenderer(mc);
        }
    }
    public void register()
    {
        bind(Configs.BLOCK_RENDER_GROUP_ADD, () -> pickBlock(true));
        bind(Configs.BLOCK_RENDER_GROUP_REMOVE, () -> pickBlock(false));
        bind(Configs.ENTITY_RENDER_GROUP_ADD, () -> pickEntity(true));
        bind(Configs.ENTITY_RENDER_GROUP_REMOVE, () -> pickEntity(false));
    }

    private static void bind(ConfigHotkey key, java.util.function.BooleanSupplier action)
    {
        key.getKeybind().setCallback((event, binding) -> event == KeyAction.PRESS && action.getAsBoolean());
    }

    private boolean pickBlock(boolean add)
    {
        var client = Minecraft.getInstance();
        var list = blockList((BlockRenderListMode) Configs.BLOCK_RENDER_LIST_MODE.getOptionListValue());
        if (client.level == null || client.player == null || list == null
                || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return false;
        var block = client.level.getBlockState(hit.getBlockPos()).getBlock();
        return updateList(list, BuiltInRegistries.BLOCK.getKey(block), add);
    }

    private boolean pickEntity(boolean add)
    {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !(client.hitResult instanceof EntityHitResult hit)) return false;
        var list = switch ((EntityRenderDisableMode) Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue())
        {
            case BLACKLIST -> Configs.ENTITY_RENDER_DISABLE_BLACKLIST;
            case WHITELIST -> Configs.ENTITY_RENDER_DISABLE_WHITELIST;
            case NONE -> null;
        };
        return list != null && updateList(list, BuiltInRegistries.ENTITY_TYPE.getKey(hit.getEntity().getType()), add);
    }

    //#if MC >= 1.21.11
    private static boolean updateList(ConfigStringList list, Identifier target, boolean add)
    //#else
    //$$ private static boolean updateList(ConfigStringList list, ResourceLocation target, boolean add)
    //#endif
    {
        if (target == null) return false;
        var entries = new ArrayList<>(list.getStrings());
        if (add)
        {
            if (entries.stream().anyMatch(entry -> target.equals(id(entry)))) return false;
            entries.add(target.toString());
        }
        else if (!entries.removeIf(entry -> target.equals(id(entry)))) return false;
        list.setStrings(entries);
        new Configs().save();
        message((add ? "+ " : "- ") + target);
        return true;
    }
    private static void message(String s) { Minecraft mc=Minecraft.getInstance(); if(mc.player!=null)MinecraftClientCompat.setOverlayMessage(mc,Component.literal(s),false); }
}
//#endif

package io.github.halfmasa.xaerobinding.feature;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.world.item.Item;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

/** One bounded refill transaction per connection, followed by a guarded native easy-place retry. */
public final class LitematicaMaterialRefill implements IClientTickHandler
{
    private enum Phase { DIRECT_READY, DIRECT_WAIT, QUERY_READY, QUERY_WAIT, TAKE_READY, TAKE_WAIT, SYNC_WAIT }
    private static final LitematicaMaterialRefill INSTANCE = new LitematicaMaterialRefill();
    private final Set<String> failedItems = new HashSet<>();
    private final Map<String,Long> printerFailures = new LinkedHashMap<>();
    private long lastPrinterDemand;
    private ClientPacketListener connection;
    private Pending pending;
    private Pending queued;
    private String lastDemandItemId;
    private IKeybind easyPlaceActivation;
    private boolean activationLookupFailed;
    private long nextId, lastPacket, lastQuery, lastTake;
    private boolean disabled, uncertainTake, probing, retrying, matched, attempted;

    private static final class Pending
    {
        ItemStack material;
        final String itemId;
        BlockPos pos;
        final Level world;
        final boolean rewritten;
        final boolean silent, printer, customAmount;
        final int amount;
        BlockState state;
        final long started = now();
        Phase phase = Phase.QUERY_READY;
        String token;
        int cursor, retries, backoffs;
        boolean cancelQuery;
        long id, deadline, readyAt, tokenExpires;
        Pending(ItemStack material, BlockPos pos, Level world, BlockState state, boolean rewritten, boolean printer)
        {
            this.material = material.copy();
            this.itemId = BuiltInRegistries.ITEM.getKey(material.getItem()).toString();
            this.pos = pos.immutable();
            this.world = world;
            this.rewritten = rewritten;
            this.printer = printer;
            this.silent = (printer ? Configs.PRINTER_REFILL_SILENT : Configs.LITEMATICA_REFILL_SILENT).getBooleanValue();
            this.amount = (printer ? Configs.PRINTER_REFILL_AMOUNT : Configs.LITEMATICA_REFILL_AMOUNT).getIntegerValue();
            this.customAmount = LitematicaRefillNetwork.supportsAmount();
            this.state = state;
            if (customAmount || silent || LitematicaRefillNetwork.supportsDirectTake()) phase = Phase.DIRECT_READY;
        }
    }

    private LitematicaMaterialRefill() {}
    public static LitematicaMaterialRefill getInstance() { return INSTANCE; }
    private static long now() { return System.nanoTime() / 1_000_000; }

    public void onToggle()
    {
        if (Configs.PRINTER_AUTO_REFILL.getBooleanValue() &&
                !io.github.halfmasa.xaerobinding.mixin.XaeroMixinPlugin.supportsPrinterRefill()) message("printer_unsupported");
        disabled = false;
        failedItems.clear();
        printerFailures.clear();
        queued = null;
        // An already submitted take must be resolved before another transaction can start.
        if (pending != null && pending.phase == Phase.QUERY_WAIT) pending.cancelQuery = true;
        else if (pending != null && !taking(pending.phase) && pending.phase != Phase.SYNC_WAIT)
            pending = null;
    }

    /** Called after native pick-block; retries must reach this point before being marked attempted. */
    public boolean afterPick(ItemStack material, BlockPos pos, Minecraft client, BlockState state, boolean rewritten)
    {
        observeConnection(client.getConnection());
        if (probing || retrying)
        {
            matched = pending != null && pending.pos.equals(pos) && pending.material.is(material.getItem());
            attempted = matched;
            return probing || !matched; // A retry can never place at a different schematic target.
        }
        return onMaterialNeeded(material, pos, client, state, rewritten);
    }

    /** Observe the material before native placement checks can reject an attempt for an empty hand. */
    public boolean onMaterialNeeded(ItemStack material, BlockPos pos, Minecraft client, BlockState state, boolean rewritten)
    {
        return requestMaterial(material, pos, client, state, rewritten, false);
    }

    /** Printer callbacks only record actual missing material; native printing resumes after inventory sync. */
    public boolean onPrinterMaterialNeeded(List<Item> acceptedItems, Item preferred)
    {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || acceptedItems == null || acceptedItems.isEmpty()) return false;
        for (Item item : acceptedItems) if (item != null && hasLocalSupply(client, new ItemStack(item))) return false;
        Item item = preferred != null && acceptedItems.contains(preferred) ? preferred : acceptedItems.get(0);
        if (item == null) return false;
        lastPrinterDemand = now();
        BlockPos pos = client.player.blockPosition();
        return requestMaterial(new ItemStack(item), pos, client, client.level.getBlockState(pos), false, true);
    }

    private boolean requestMaterial(ItemStack material, BlockPos pos, Minecraft client, BlockState state, boolean rewritten, boolean printer)
    {
        if (probing || retrying) return false;
        observeConnection(client.getConnection());
        if (!enabled(printer) || client.player == null || client.level == null ||
                client.getConnection() == null || MinecraftClientCompat.getScreen(client) != null ||
                client.player.isCreative() || material.isEmpty() ||
                (!printer && client.level.getBlockState(pos) == state)) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(material.getItem()).toString();
        if (!printer && !itemId.equals(lastDemandItemId))
        {
            // A missing material must not permanently blacklist the next building operation.
            failedItems.clear();
            lastDemandItemId = itemId;
        }
        if (hasLocalSupply(client, material))
        {
            failedItems.remove(itemId);
            return false;
        }
        if (disabled || uncertainTake || (printer ? now() < printerFailures.getOrDefault(itemId, 0L) : failedItems.contains(itemId))) return false;
        // Keep optional Fabric API classes out of the missing-dependency path.
        if (!FabricLoader.getInstance().isModLoaded("fabric-api"))
        {
            disabled = true;
            message("unsupported");
            return false;
        }
        int amount = (printer ? Configs.PRINTER_REFILL_AMOUNT : Configs.LITEMATICA_REFILL_AMOUNT).getIntegerValue();
        if (!LitematicaRefillNetwork.supportsAmount() && amount != 0 && amount != Math.max(1, Math.min(64, material.getMaxStackSize()) / 2))
        {
            disabled = true;
            message("amount_unsupported");
            return false;
        }
        if ((printer ? Configs.PRINTER_REFILL_SILENT : Configs.LITEMATICA_REFILL_SILENT).getBooleanValue() &&
                (!FabricLoader.getInstance().isModLoaded("fabric-api") || (!LitematicaRefillNetwork.supportsSilentTake() && !LitematicaRefillNetwork.supportsAmount())))
        {
            disabled = true;
            message("silent_unsupported");
            return false;
        }
        if (pending != null)
        {
            // A scan can demand many different materials in one tick. Complete the current printer
            // demand (including direct-name fallback) before admitting another scanned material.
            if (printer && pending.printer && !pending.itemId.equals(itemId)) return false;
            if (pending.itemId.equals(itemId) && pending.world == client.level && pending.rewritten == rewritten && pending.printer == printer)
            {
                pending.pos = pos.immutable();
                pending.state = state;
                pending.material = material.copy();
                queued = null;
            }
            else if (pending.phase == Phase.QUERY_WAIT || taking(pending.phase) || pending.phase == Phase.SYNC_WAIT)
            {
                // Resolve the in-flight request before starting a new ID, especially after a submitted take.
                boolean changed = queued == null || !queued.itemId.equals(itemId);
                queued = new Pending(material, pos, client.level, state, rewritten, printer);
                if (changed) message("waiting_previous", material.getHoverName());
            }
            else
            {
                queued = new Pending(material, pos, client.level, state, rewritten, printer);
                finishPending();
            }
            return true;
        }
        if (!FabricLoader.getInstance().isModLoaded("fabric-api") || !LitematicaRefillNetwork.isSupported())
        {
            disabled = true;
            message("unsupported");
            return false;
        }
        pending = new Pending(material, pos, client.level, state, rewritten, printer);
        announce(pending);
        return true;
    }

    /** Captured before native cache/cooldown and inventory operations, so probes have no pick-block side effects. */
    public void onSchematicTarget(BlockState state, BlockPos pos)
    {
        if (!probing && !retrying) return;
        Minecraft client = Minecraft.getInstance();
        matched = pending != null && pending.pos.equals(pos) && pending.state == state &&
                client.level == pending.world && client.level.getBlockState(pos) != state;
    }

    public boolean cancelTargetAttempt()
    {
        return probing || (retrying && !matched);
    }

    /** Query/debit only when the item is absent from both loose inventory and carried shulker boxes. */
    private static boolean hasLocalSupply(Minecraft client, ItemStack required)
    {
        if (client.player == null || required.isEmpty()) return false;
        var inventory = client.player.getInventory();
        // Check loose stacks first; a single item is enough to suppress an external refill.
        for (int slot = 0; slot < 36; slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(required.getItem())) return true;
        }
        ItemStack offhand = client.player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.is(required.getItem())) return true;
        for (int slot = 0; slot < 36; slot++)
        {
            if (shulkerContains(inventory.getItem(slot), required)) return true;
        }
        return shulkerContains(offhand, required);
    }

    private static boolean shulkerContains(ItemStack stack, ItemStack required)
    {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem) ||
                !(blockItem.getBlock() instanceof ShulkerBoxBlock)) return false;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        // Inspect only this box's contents; do not recursively scan other containers or mutate stacks.
        //#if MC >= 26.0
        return contents != null && contents.nonEmptyItemCopyStream()
                .anyMatch(inner -> inner.is(required.getItem()));
        //#else
        //$$ if (contents == null) return false;
        //$$ for (ItemStack inner : contents.nonEmptyItems())
        //$$     if (inner.is(required.getItem())) return true;
        //$$ return false;
        //#endif
    }

    /** Actual usable inventory synchronization stays separate from the shulker availability check. */
    private static boolean hasMaterial(Minecraft client, ItemStack required)
    {
        if (client.player == null) return false;
        int count = 0;
        // Use Litematica's item-only matching; components on building materials need not match the schematic.
        var inventory = client.player.getInventory();
        for (int slot = 0; slot < 36; slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(required.getItem())) count += stack.getCount();
        }
        ItemStack offhand = client.player.getOffhandItem();
        if (offhand.is(required.getItem())) count += offhand.getCount();
        return count >= Math.max(1, required.getCount());
    }

    private void observeConnection(ClientPacketListener current)
    {
        if (connection == current) return;
        connection = current;
        pending = null;
        queued = null;
        lastDemandItemId = null;
        nextId = lastPacket = lastQuery = lastTake = 0;
        disabled = uncertainTake = false;
        failedItems.clear();
        printerFailures.clear();
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        observeConnection(client.getConnection());
        // Holding one failed demand stays quiet; releasing both custom easy-place input and use
        // allows the next deliberate attempt to query again without toggling the feature.
        if (!failedItems.isEmpty() && !client.options.keyUse.isDown() && !isEasyPlaceActivationHeld())
            failedItems.clear();
        Pending p = pending;
        if (p == null || connection == null || client.player == null) return;
        long time = now();
        boolean enabled = enabled(p.printer) && (!p.printer || PrinterRefillBridge.isPrinting(time - lastPrinterDemand));
        boolean valid = enabled && client.level == p.world && MinecraftClientCompat.getScreen(client) == null && !client.player.isCreative();
        boolean awaitingTake = taking(p.phase) || p.phase == Phase.SYNC_WAIT;
        if (!valid && p.phase == Phase.QUERY_WAIT) p.cancelQuery = true;
        else if (!valid && !awaitingTake) { finishPending(); return; }
        // Also stop pending queries before retry/pagination/take if local supplies have appeared.
        if (!awaitingTake && hasLocalSupply(client, p.material))
        {
            if (p.phase == Phase.QUERY_WAIT) p.cancelQuery = true;
            else { finishPending(); return; }
        }
        if (time - p.started > 60_000)
        {
            fail("timeout", taking(p.phase));
            return;
        }
        if (p.phase == Phase.SYNC_WAIT)
        {
            if (hasMaterial(client, p.material))
            {
                if (!valid || queued != null || p.printer) { finishPending(); return; }
                if (time < p.readyAt) return;
                boolean attempted = invokeEasyPlace(client, false);
                if (attempted || !matched) finishPending();
                else if (time >= p.deadline) fail("timeout", false);
                else p.readyAt = time + 250; // Let native swap cooldown expire before the one placement retry.
            }
            else if (time >= p.deadline) fail("sync_timeout", true);
            return;
        }
        if (p.phase == Phase.QUERY_WAIT || taking(p.phase))
        {
            if (time >= p.deadline && time - lastPacket >= 1000)
            {
                if (p.retries++ >= 1) { fail("timeout", taking(p.phase)); return; }
                // Retransmit the SAME request ID and parameters, including a possibly committed take.
                send(p, taking(p.phase), time);
            }
            return;
        }
        if (time < p.readyAt || time - lastPacket < 300) return;
        boolean direct = p.phase == Phase.DIRECT_READY;
        boolean take = direct || p.phase == Phase.TAKE_READY;
        if (time - (take ? lastTake : lastQuery) < 1100) return;
        // The native cache/cooldown can reject a probe before reaching material detection.
        // Refill follows the captured demand; native placement checks apply after inventory sync.
        if (take && !p.printer && client.level.getBlockState(p.pos) == p.state) { finishPending(); return; }
        if (take && hasLocalSupply(client, p.material)) { finishPending(); return; }
        if (take && !direct && time >= p.tokenExpires)
        {
            p.phase = Phase.QUERY_READY;
            p.cursor = 0;
            return;
        }
        p.id = ++nextId;
        p.retries = 0;
        p.phase = direct ? Phase.DIRECT_WAIT : take ? Phase.TAKE_WAIT : Phase.QUERY_WAIT;
        send(p, take, time);
    }

    private void send(Pending p, boolean take, long time)
    {
        try
        {
            if (take)
            {
                if (p.customAmount) LitematicaRefillNetwork.takeAmount(p.id, p.phase == Phase.DIRECT_WAIT ? "" : p.token, p.itemId, p.amount, p.silent);
                else if (p.silent) LitematicaRefillNetwork.silentTake(p.id, p.phase == Phase.DIRECT_WAIT ? "" : p.token, p.itemId);
                else if (p.phase == Phase.DIRECT_WAIT) LitematicaRefillNetwork.directTake(p.id, p.itemId);
                else LitematicaRefillNetwork.take(p.id, p.token, p.itemId);
                lastTake = time;
            }
            else
            {
                if (p.customAmount) LitematicaRefillNetwork.queryAmount(p.id, p.itemId, p.cursor, p.amount);
                else LitematicaRefillNetwork.query(p.id, p.itemId, p.cursor);
                lastQuery = time;
            }
            lastPacket = time;
            p.deadline = time + (take ? 15_000 : 3_000);
        }
        catch (RuntimeException e)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to send Litematica material refill request", e);
            fail("network_error", take);
        }
    }

    public void onReply(ClientPacketListener source, long id, String json)
    {
        Pending p = pending;
        if (source != connection || p == null || p.id != id ||
                (p.phase != Phase.QUERY_WAIT && !taking(p.phase))) return;
        boolean direct = p.phase == Phase.DIRECT_WAIT;
        boolean take = taking(p.phase);
        try
        {
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            if (data.get("version").getAsInt() != 1) { disabled = true; fail("UNSUPPORTED_VERSION", take); return; }
            String status = data.get("status").getAsString();
            long time = now();
            // These terminal responses guarantee that the direct attempt did not debit stock.
            if (direct && Set.of("NOT_FOUND", "NOT_ENOUGH", "INVALID_TARGET", "INVALID_ITEM",
                    "UNVERIFIED_TARGET", "PROTECTED_TARGET").contains(status))
            {
                if (queued != null) { finishPending(); return; }
                p.phase = Phase.QUERY_READY;
                p.cursor = p.retries = p.backoffs = 0;
                p.token = null;
                p.readyAt = time + 300;
                message("fallback", p.material.getHoverName(),
                        Component.translatableWithFallback("halfmasa.message.litematica_refill.status." + status, status));
                return;
            }
            if (!take && (p.cancelQuery || queued != null) && !status.equals("REQUEST_PENDING") &&
                    !status.equals("PERMISSION_DENIED") && !status.equals("DISABLED") &&
                    !status.equals("UNSUPPORTED_VERSION"))
            {
                // The old query is now resolved. Do not take its token after demand has changed.
                finishPending();
                return;
            }
            switch (status)
            {
                case "OK" -> {
                    if (!p.itemId.equals(data.get("itemId").getAsString()) ||
                            !(take ? "take" : "material").equals(data.get("kind").getAsString()))
                        throw new IllegalArgumentException("Mismatched reply kind/item");
                    if (take)
                    {
                        int moved = data.get("moved").getAsInt();
                        if (moved <= 0 || moved > 2304 || (p.amount > 0 && moved != p.amount))
                            throw new IllegalArgumentException("Unexpected transfer quantity");
                        p.phase = Phase.SYNC_WAIT;
                        p.deadline = time + 5_000;
                        message("received", data.get("moved").getAsInt(), p.material.getHoverName());
                    }
                    else
                    {
                        if (p.amount > 0 && data.get("takeCount").getAsInt() != p.amount)
                            throw new IllegalArgumentException("Unexpected query quantity");
                        p.token = data.get("token").getAsString();
                        int expires = data.get("expiresInMs").getAsInt();
                        if (p.token.isEmpty() || p.token.length() > 64 || expires <= 0)
                            throw new IllegalArgumentException("Invalid material token");
                        p.tokenExpires = time + Math.min(30_000, expires);
                        p.phase = Phase.TAKE_READY;
                    }
                }
                case "SEARCH_CONTINUE" -> {
                    int cursor = data.get("nextCursor").getAsInt();
                    if (take || cursor <= p.cursor) throw new IllegalArgumentException("Invalid material cursor");
                    p.cursor = cursor;
                    p.phase = Phase.QUERY_READY;
                    p.readyAt = time + 1100;
                }
                case "REQUEST_PENDING" -> p.deadline = time + (take ? 15_000 : 3_000);
                case "RATE_LIMITED", "SERVER_BUSY", "TARGET_BUSY", "SNAPSHOT_EXPIRED", "STALE_SNAPSHOT" -> {
                    if (++p.backoffs > 3) { fail(status, false); return; }
                    p.cursor = 0;
                    p.phase = direct ? Phase.DIRECT_READY : Phase.QUERY_READY;
                    p.readyAt = time + p.backoffs * 1500L;
                }
                case "PERMISSION_DENIED", "DISABLED", "UNSUPPORTED_VERSION" -> {
                    disabled = true;
                    fail(status, false);
                }
                default -> fail(status, take && (status.equals("TRANSFER_FAILED") ||
                        status.equals("REPLAY_EXPIRED") || status.equals("REQUEST_ID_REUSED")));
            }
        }
        catch (RuntimeException e)
        {
            XaeroWorldBinding.LOGGER.warn("Invalid FGA material refill reply", e);
            disabled = true;
            fail("invalid_reply", take);
        }
    }

    /** Native ray tracing and checks are retained; the mixin cancels a probe or a changed-target retry. */
    private boolean invokeEasyPlace(Minecraft client, boolean probe)
    {
        Pending p = pending;
        matched = false;
        attempted = false;
        probing = probe;
        retrying = !probe;
        try
        {
            Class<?> generic = Class.forName("fi.dy.masa.litematica.config.Configs$Generic");
            if (!((ConfigBoolean) generic.getField("EASY_PLACE_MODE").get(null)).getBooleanValue()) return false;
            boolean rewritten = ((ConfigBoolean) generic.getField("EASY_PLACE_POST_REWRITE").get(null)).getBooleanValue();
            if (rewritten != p.rewritten) return false;
            Object mode = Class.forName("fi.dy.masa.litematica.data.DataManager").getMethod("getToolMode").invoke(null);
            if (((Enum<?>) mode).name().equals("REBUILD")) return false;
            if (rewritten)
                Class.forName("fi.dy.masa.litematica.util.EasyPlaceUtils").getMethod("handleEasyPlaceWithMessage").invoke(null);
            else
                Class.forName("fi.dy.masa.litematica.util.WorldUtils").getMethod("handleEasyPlace", Minecraft.class).invoke(null, client);
            return probe ? matched : attempted;
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            XaeroWorldBinding.LOGGER.warn("Litematica refill could not retry easy place", e);
            disabled = true;
            message("integration_error");
            return false;
        }
        finally { probing = retrying = false; }
    }

    private void fail(String status, boolean uncertain)
    {
        if (pending == null) return;
        if (pending.printer)
        {
            if (printerFailures.size() >= 256) printerFailures.remove(printerFailures.keySet().iterator().next());
            printerFailures.put(pending.itemId, now() + 10_000);
        }
        else failedItems.add(pending.itemId);
        uncertainTake |= uncertain;
        String key = "halfmasa.message.litematica_refill.status." + status;
        message(uncertain ? "uncertain" : disabled ? "suspended" : "failed", pending.material.getHoverName(),
                Component.translatableWithFallback(key, status));
        finishPending();
    }

    private void finishPending()
    {
        pending = disabled || uncertainTake ? null : queued;
        queued = null;
        if (pending != null) announce(pending);
    }

    private static boolean enabled(boolean printer)
    {
        return printer ? Configs.PRINTER_AUTO_REFILL.getBooleanValue() && Configs.PRINTER_REFILL_ALLOW_FAKE.getBooleanValue()
                : Configs.LITEMATICA_AUTO_REFILL.getBooleanValue() && Configs.LITEMATICA_REFILL_ALLOW_FAKE.getBooleanValue();
    }

    private static boolean taking(Phase phase)
    {
        return phase == Phase.DIRECT_WAIT || phase == Phase.TAKE_WAIT;
    }

    private static void announce(Pending p)
    {
        message(p.phase == Phase.DIRECT_READY ? (p.silent ? "silent" : "direct") : "searching", p.material.getHoverName());
    }

    private boolean isEasyPlaceActivationHeld()
    {
        if (activationLookupFailed) return true;
        try
        {
            if (easyPlaceActivation == null)
                easyPlaceActivation = ((ConfigHotkey) Class.forName("fi.dy.masa.litematica.config.Hotkeys")
                        .getField("EASY_PLACE_ACTIVATION").get(null)).getKeybind();
            return easyPlaceActivation.isKeybindHeld();
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            activationLookupFailed = true;
            XaeroWorldBinding.LOGGER.warn("Cannot read Litematica activation key for refill retry", e);
            return true;
        }
    }

    private static void message(String key, Object... args)
    {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null)
            //#if MC >= 26.1
            client.player.sendSystemMessage(Component.translatable("halfmasa.message.litematica_refill." + key, args));
            //#else
            //$$ client.player.displayClientMessage(Component.translatable("halfmasa.message.litematica_refill." + key, args), false);
            //#endif
    }
}

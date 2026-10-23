package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.AutoEatSourceMode;
import io.github.halfmasa.xaerobinding.config.Configs;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
//#if MC >= 1.21.5
import net.minecraft.network.HashedStack;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
//#if MC >= 26.0
import net.minecraft.world.inventory.ContainerInput;
//#else
//$$ import net.minecraft.world.inventory.ClickType;
//#endif
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** One server-authoritative inventory operation at a time; never picks up a cursor stack. */
public final class AutoEatService implements IClientTickHandler
{
    private static int selectedSlot(net.minecraft.world.entity.player.Inventory inventory)
    {
        //#if MC >= 1.21.5
        return inventory.getSelectedSlot();
        //#else
        //$$ return inventory.selected;
        //#endif
    }

    private static void selectSlot(net.minecraft.world.entity.player.Inventory inventory, int slot)
    {
        //#if MC >= 1.21.5
        inventory.setSelectedSlot(slot);
        //#else
        //$$ inventory.selected = slot;
        //#endif
    }
    private enum State { IDLE, WAIT_SWAP, EATING,
        //#if MC >= 1.21.1
        WAIT_SILENT_USE,
        //#endif
        WAIT_SILENT, WAIT_OPEN, WAIT_TAKE, WAIT_RESTORE,
        WAIT_RETURN_SWAP, WAIT_RETURN_OPEN, WAIT_RETURN }
    private record Candidate(int index, ItemStack stack, boolean ender) {}
    private record ContainerScan(ItemStack stack, java.util.List<ItemStack> inventory) {}
    private static final int STORAGE_SLOTS = 36;
    private static final int HOTBAR_SLOTS = 9;
    private static final int RESPONSE_TIMEOUT_TICKS = 20 * 10;
    private static final int COMBAT_QUIET_TICKS = 20;
    private static final int IDLE_RESCAN_TICKS = 20;
    private static final AutoEatService INSTANCE = new AutoEatService();
    private final AutoEatInventorySync sync = AutoEatInventorySync.getInstance();
    private final ArrayDeque<Candidate> candidates = new ArrayDeque<>();
    private final Map<Integer, ContainerScan> emptyContainers = new HashMap<>();
    private LocalPlayer actor;
    private ClientLevel level;
    private State state = State.IDLE;
    private AutoEatSourceMode source = AutoEatSourceMode.HOTBAR;
    private long ticks, deadline, requestRevision, nextScan, combatUntil;
    private boolean feeding, ownsUse, remoteSuppressed, restoreSelection = true;
    private int selectedBefore = -1, activeSlot = -1, takeSlot = -1, takeCount, inventoryCount;
    private ItemStack eatingFood = ItemStack.EMPTY;
    private ItemStack takingFood = ItemStack.EMPTY;
    private Candidate opening;
    private AbstractContainerMenu ownedMenu;
    private PendingRestore pendingRestore;
    private RemoteWithdrawal pendingReturn;
    private long silentRequest;
    //#if MC >= 1.21.1
    private long silentUseReadyAt;
    //#endif
    private String silentResult;
    private boolean silentSuppressed, silentWarned;
    private java.util.List<ItemStack> beforeTake;

    private static final class RemoteWithdrawal
    {
        final Candidate origin;
        final int menuSlot, inventorySlot, taken;
        final ItemStack food;
        int consumed, hotbar = -1;
        ItemStack returning;
        RemoteWithdrawal(Candidate origin, int menuSlot, int inventorySlot, ItemStack food)
        { this.origin = origin; this.menuSlot = menuSlot; this.inventorySlot = inventorySlot; this.food = food.copy(); this.taken = food.getCount(); }
    }

    private static final class PendingRestore
    {
        final int source, hotbar;
        final int selected;
        final ItemStack food, original;
        final long swapRevision;
        boolean swapped, warned;
        ItemStack remainder;
        PendingRestore(int source, int hotbar, int selected, ItemStack food, ItemStack original, long revision)
        {
            this.source = source; this.hotbar = hotbar; this.selected = selected; this.food = food.copy();
            this.original = original.copy(); this.swapRevision = revision;
        }
    }

    private AutoEatService() {}
    public static AutoEatService getInstance() { return INSTANCE; }
    public void onWorldExit() { clearSession(); this.sync.clear(); HalfMasaSupportClient.clear(); BossBarGrouper.clear(); }

    public boolean ownsUse(LocalPlayer player)
    {
        return this.ownsUse && this.state == State.EATING && this.actor == player
                && Configs.AUTO_EAT.getBooleanValue() && sourceMode() == this.source
                && !Minecraft.getInstance().options.keyAttack.isDown()
                && !Minecraft.getInstance().options.keyUse.isDown()
                && selectedSlot(player.getInventory()) == this.activeSlot
                && ItemStack.isSameItemSameComponents(player.getUseItem(), this.eatingFood);
    }

    public void onAttack() { this.combatUntil = this.ticks + COMBAT_QUIET_TICKS; }

    /** Player/other-mod menu input hands the entire menu back to its user. */
    public void onManualContainerInput()
    {
        this.ownedMenu = null;
        cancel(Minecraft.getInstance(), true);
    }

    public void onSettingsChanged()
    {
        if (this.state != State.WAIT_SILENT) { this.silentSuppressed = false; this.silentWarned = false; }
        this.emptyContainers.clear(); this.nextScan = this.ticks;
        cancel(Minecraft.getInstance(), false);
    }

    @Override public void onClientTick(Minecraft client)
    {
        this.ticks++;
        if (this.actor != client.player || this.level != client.level)
        {
            clearSession(); this.actor = client.player; this.level = client.level; this.source = sourceMode();
        }
        if (this.actor == null || this.level == null || client.gameMode == null) return;
        if (!this.actor.isAlive() || this.actor.isCreative() || this.actor.isSpectator())
        {
            cancel(client, true); this.pendingRestore = null; this.pendingReturn = null; this.state = State.IDLE; return;
        }
        if (sourceMode() != this.source) { cancel(client, false); this.source = sourceMode(); this.emptyContainers.clear(); }
        if (!Configs.AUTO_EAT.getBooleanValue() && !returning()) cancel(client, false);

        if (this.pendingReturn != null && this.state != State.EATING && this.state != State.WAIT_SILENT
                //#if MC >= 1.21.1
                && this.state != State.WAIT_SILENT_USE
                //#endif
                && (returning() || this.pendingRestore == null))
        {
            if (handleReturn(client)) return;
        }
        if (this.pendingRestore != null && !this.ownsUse && this.state != State.EATING && this.state != State.WAIT_SILENT
                //#if MC >= 1.21.1
                && this.state != State.WAIT_SILENT_USE
                //#endif
                )
        {
            if (this.state == State.WAIT_SWAP && (MinecraftClientCompat.getScreen(client) != null
                    || selectedSlot(this.actor.getInventory()) != this.pendingRestore.selected)) cancel(client, true);
            handleRestore(client);
            if (this.pendingRestore != null) return;
        }
        //#if MC >= 1.21.1
        // A sent consume request must settle even after the feature is disabled.
        if (!Configs.AUTO_EAT.getBooleanValue() && this.state != State.WAIT_SILENT) return;
        //#else
        //$$ if (!Configs.AUTO_EAT.getBooleanValue()) return;
        //#endif

        boolean combat = Configs.AUTO_EAT_PAUSE_COMBAT.getBooleanValue()
                && (this.actor.hurtTime > 0 || client.options.keyAttack.isDown() || this.ticks < this.combatUntil);
        boolean manual = client.options.keyAttack.isDown() || client.options.keyUse.isDown();
        boolean foreignMenu = this.state != State.WAIT_OPEN && this.actor.containerMenu != this.actor.inventoryMenu
                && this.actor.containerMenu != this.ownedMenu;
        if (manual || combat || foreignMenu)
        {
            if (foreignMenu) this.ownedMenu = null;
            cancel(client, manual || foreignMenu);
            //#if MC >= 1.21.1
            if (this.state != State.WAIT_SILENT) return;
            //#else
            //$$ return;
            //#endif
        }

        //#if MC >= 1.21.1
        if (this.state == State.WAIT_SILENT_USE)
        {
            // Nothing has been sent yet: interrupted preparation can be cancelled safely.
            ItemStack food = this.actor.getInventory().getItem(this.activeSlot);
            if (MinecraftClientCompat.getScreen(client) != null || this.actor.isUsingItem()
                    || !ItemStack.matches(food, this.eatingFood) || !AutoEatFoodRules.isAllowed(food)
                    || this.actor.getFoodData().getFoodLevel() >= targetHunger() || !silentAvailable(client))
            {
                cancel(client, false); return;
            }
            if (this.level.getGameTime() >= this.silentUseReadyAt) requestSilentMeal();
            return;
        }
        //#endif
        if (this.state == State.WAIT_OPEN || this.state == State.WAIT_TAKE)
        {
            handleContainer(client); return;
        }
        if (this.state == State.WAIT_SILENT)
        {
            if (this.silentResult == null) this.silentResult = HalfMasaSupportClient.takeResult(this.silentRequest);
            if (this.silentResult != null && !"OK".equals(this.silentResult))
            {
                if (!java.util.Set.of("STALE", "NOT_NEEDED", "BUSY", "COOLDOWN", "NO_SPACE", "RATE_LIMITED").contains(this.silentResult))
                { this.silentSuppressed = true; warnSilent(client); }
                this.state = State.IDLE; this.nextScan = this.ticks + IDLE_RESCAN_TICKS;
                //#if MC >= 1.21.1
                if (!this.feeding && this.pendingRestore == null && this.pendingReturn == null) finishSelection();
                //#endif
                return;
            }
            ItemStack remaining = this.actor.getInventory().getItem(this.activeSlot);
            if ("OK".equals(this.silentResult) && consumedConfirmed(remaining)) mealFinished(remaining);
            else if (this.ticks >= this.deadline)
            {
                // An uncertain consume cannot be retried with a new nonce or followed by normal use.
                //#if MC >= 1.21.1
                if (!this.silentSuppressed)
                    MinecraftClientCompat.setOverlayMessage(client, Component.translatable("halfmasa.auto_eat.silent_uncertain"), false);
                this.silentSuppressed = true; this.feeding = false;
                //#else
                //$$ this.silentSuppressed = true; this.feeding = false;
                //$$ MinecraftClientCompat.setOverlayMessage(client, Component.translatable("halfmasa.auto_eat.silent_uncertain"), false);
                //#endif
            }
            return;
        }
        if (this.state == State.EATING)
        {
            if (MinecraftClientCompat.getScreen(client) != null || selectedSlot(this.actor.getInventory()) != this.activeSlot
                    || this.actor.isUsingItem() && !ItemStack.isSameItemSameComponents(this.actor.getUseItem(), this.eatingFood))
            {
                cancel(client, true); return;
            }
            if (!AutoEatFoodRules.isAllowed(this.eatingFood)) { cancel(client, false); return; }
            if (!this.actor.isUsingItem())
            {
                // A hunger update or authoritative consumed-stack update is required before another meal.
                ItemStack remaining = this.actor.getInventory().getItem(this.activeSlot);
                if (consumedConfirmed(remaining)) mealFinished(remaining);
                else if (this.ticks >= this.deadline) cancel(client, false);
            }
            return;
        }

        if (MinecraftClientCompat.getScreen(client) != null || this.actor.containerMenu != this.actor.inventoryMenu
                || this.actor.isUsingItem() || !this.actor.inventoryMenu.getCarried().isEmpty() || this.ticks < this.nextScan) return;
        int hunger = this.actor.getFoodData().getFoodLevel();
        if (hunger >= targetHunger()) { this.feeding = false; finishSelection(); return; }
        int start = Math.min(Configs.AUTO_EAT_START_HUNGER.getIntegerValue(), targetHunger() - 1);
        if (!this.feeding && hunger > start) return;
        if (!this.feeding)
        {
            this.feeding = true; this.selectedBefore = selectedSlot(this.actor.getInventory()); this.restoreSelection = true;
        }
        int slot = findFood(0, HOTBAR_SLOTS);
        if (slot >= 0) { beginEating(client, slot); return; }
        if (this.source != AutoEatSourceMode.HOTBAR)
        {
            slot = findFood(HOTBAR_SLOTS, STORAGE_SLOTS);
            if (slot >= 0) { if (silentAvailable(client)) beginEating(client, slot); else beginSwap(client, slot); return; }
        }
        if (this.source == AutoEatSourceMode.QUICK_SHULKER && !this.remoteSuppressed && hasEmptyStorage()
                && AutoEatQuickShulkerBridge.isAvailable())
        {
            prepareCandidates(); if (openNext(client)) return;
        }
        this.feeding = false; finishSelection(); this.nextScan = this.ticks + IDLE_RESCAN_TICKS;
    }

    private static AutoEatSourceMode sourceMode() { return (AutoEatSourceMode) Configs.AUTO_EAT_SOURCE.getOptionListValue(); }
    private static int targetHunger() { return Configs.AUTO_EAT_TARGET_HUNGER.getIntegerValue(); }

    private int findFood(int from, int to)
    {
        int chosen = -1; ItemStack best = ItemStack.EMPTY;
        for (int index = from; index < to; index++)
        {
            ItemStack stack = this.actor.getInventory().getItem(index);
            if (AutoEatFoodRules.isAllowed(stack) && canKeepUseRemainder(stack, false) && AutoEatFoodRules.isBetter(stack, best,
                    targetHunger() - this.actor.getFoodData().getFoodLevel())) { chosen = index; best = stack; }
        }
        return chosen;
    }

    private void beginSwap(Minecraft client, int sourceSlot)
    { beginSwap(client, sourceSlot, false); }

    private void beginSwap(Minecraft client, int sourceSlot, boolean forReturn)
    {
        int menuSlot = playerMenuSlot(sourceSlot);
        if (menuSlot < 0) { this.feeding = false; finishSelection(); return; }
        int hotbar = selectedSlot(this.actor.getInventory());
        if (forReturn) for (int i = 0; i < HOTBAR_SLOTS; i++) if (i != hotbar) { hotbar = i; break; }
        for (int index = 0; index < HOTBAR_SLOTS; index++) if (this.actor.getInventory().getItem(index).isEmpty()) { hotbar = index; break; }
        this.activeSlot = hotbar;
        this.pendingRestore = new PendingRestore(sourceSlot, hotbar, selectedSlot(this.actor.getInventory()), this.actor.getInventory().getItem(sourceSlot),
                this.actor.getInventory().getItem(hotbar), this.sync.revision());
        this.state = State.WAIT_SWAP; this.deadline = this.ticks + RESPONSE_TIMEOUT_TICKS;
        //#if MC >= 26.0
        if (!sendInput(client, this.actor.inventoryMenu, menuSlot, hotbar, ContainerInput.SWAP))
        //#else
        //$$ if (!sendInput(client, this.actor.inventoryMenu, menuSlot, hotbar, ClickType.SWAP))
        //#endif
        { this.pendingRestore = null; this.state = State.IDLE; this.feeding = false; finishSelection(); }
    }

    private int playerMenuSlot(int inventoryIndex)
    {
        for (var slot : this.actor.inventoryMenu.slots)
            if (slot.container == this.actor.getInventory() && slot.getContainerSlot() == inventoryIndex) return slot.index;
        return -1;
    }

    private void beginEating(Minecraft client, int slot)
    {
        if (!canKeepUseRemainder(this.actor.getInventory().getItem(slot), false)) { cancel(client, false); return; }
        this.activeSlot = slot; this.eatingFood = this.actor.getInventory().getItem(slot).copy();
        if (silentAvailable(client))
        {
            //#if MC >= 1.21.1
            // Silence removes sound and hand use, not the food's vanilla consumption time.
            this.silentUseReadyAt = this.level.getGameTime() + Math.max(0, this.eatingFood.getUseDuration(this.actor));
            this.state = State.WAIT_SILENT_USE;
            if (this.level.getGameTime() >= this.silentUseReadyAt) requestSilentMeal();
            //#else
            //$$ this.requestRevision = this.sync.revision(); this.deadline = this.ticks + RESPONSE_TIMEOUT_TICKS;
            //$$ this.state = State.WAIT_SILENT; this.silentResult = null;
            //$$ this.silentRequest = HalfMasaSupportClient.eat(slot,this.eatingFood,targetHunger(),Configs.AUTO_EAT_PAUSE_COMBAT.getBooleanValue());
            //#endif
            return;
        }
        selectSlot(this.actor.getInventory(), slot); this.requestRevision = this.sync.revision();
        this.deadline = this.ticks + this.eatingFood.getUseDuration(this.actor) + RESPONSE_TIMEOUT_TICKS;
        this.state = State.EATING; this.ownsUse = true;
        client.gameMode.useItem(this.actor, InteractionHand.MAIN_HAND);
    }

    //#if MC >= 1.21.1
    private void requestSilentMeal()
    {
        this.requestRevision = this.sync.revision(); this.deadline = this.ticks + RESPONSE_TIMEOUT_TICKS;
        this.state = State.WAIT_SILENT; this.silentResult = null;
        this.silentRequest = HalfMasaSupportClient.eat(this.activeSlot, this.eatingFood,
                targetHunger(), Configs.AUTO_EAT_PAUSE_COMBAT.getBooleanValue());
    }
    //#endif

    private boolean consumedConfirmed(ItemStack remaining)
    {
        return this.sync.confirmed(this.activeSlot, remaining, this.requestRevision)
                && (remaining.isEmpty() || ItemStack.isSameItemSameComponents(remaining, this.eatingFood)
                        && remaining.getCount() < this.eatingFood.getCount() || isUseRemainder(remaining, this.eatingFood));
    }

    private void mealFinished(ItemStack remaining)
    {
        if (this.pendingReturn != null && (this.activeSlot == this.pendingReturn.inventorySlot
                || this.pendingRestore != null && this.pendingRestore.source == this.pendingReturn.inventorySlot
                && this.activeSlot == this.pendingRestore.hotbar))
            this.pendingReturn.consumed += ItemStack.isSameItemSameComponents(remaining, this.eatingFood)
                    ? this.eatingFood.getCount() - remaining.getCount() : this.eatingFood.getCount();
        this.ownsUse = false; this.state = State.IDLE;
        if (this.actor.getFoodData().getFoodLevel() >= targetHunger()) this.feeding = false;
        if (this.pendingRestore == null && this.pendingReturn == null && !this.feeding) finishSelection();
    }

    private boolean silentAvailable(Minecraft client)
    {
        if (!Configs.AUTO_EAT_SILENT.getBooleanValue() || this.silentSuppressed) return false;
        if (HalfMasaSupportClient.canEat()) return true;
        warnSilent(client); return false;
    }

    private void warnSilent(Minecraft client)
    {
        if (!this.silentWarned) { MinecraftClientCompat.setOverlayMessage(client, Component.translatable("halfmasa.auto_eat.silent_unsupported"), false); this.silentWarned = true; }
    }

    /** Empty changedSlots means no local prediction; ordinary server menu validation still applies. */
    //#if MC >= 26.0
    private static boolean sendInput(Minecraft client, AbstractContainerMenu menu, int slot, int button, ContainerInput input)
    //#else
    //$$ private static boolean sendInput(Minecraft client, AbstractContainerMenu menu, int slot, int button, ClickType input)
    //#endif
    {
        if (client.getConnection() == null || slot < 0 || slot >= menu.slots.size() || client.player.containerMenu != menu
                || !menu.getCarried().isEmpty()) return false;
        client.getConnection().send(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(),
                //#if MC >= 1.21.5
                (short) slot, (byte) button, input, new Int2ObjectOpenHashMap<>(), HashedStack.EMPTY));
                //#else
                //$$ slot, button, input, ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()));
                //#endif
        return true;
    }

    private void handleRestore(Minecraft client)
    {
        PendingRestore restore = this.pendingRestore;
        if (!restore.swapped)
        {
            if (this.sync.confirmed(restore.hotbar, restore.food, restore.swapRevision)
                    && this.sync.confirmed(restore.source, restore.original, restore.swapRevision))
            {
                restore.swapped = true;
                if (this.state == State.WAIT_SWAP && this.feeding && Configs.AUTO_EAT.getBooleanValue()
                        && safeInventory(client) && !client.options.keyAttack.isDown() && !client.options.keyUse.isDown()
                        && (!Configs.AUTO_EAT_PAUSE_COMBAT.getBooleanValue() || this.actor.hurtTime == 0 && this.ticks >= this.combatUntil))
                { beginEating(client, restore.hotbar); return; }
                this.state = State.IDLE;
            }
            else if (this.sync.confirmed(restore.source, restore.food, restore.swapRevision)
                    && this.sync.confirmed(restore.hotbar, restore.original, restore.swapRevision))
            { this.pendingRestore = null; this.state = State.IDLE; this.feeding = false; finishSelection(); return; }
            else
            {
                if (this.ticks >= this.deadline)
                {
                    cancel(client, false);
                    if (!restore.warned) { MinecraftClientCompat.setOverlayMessage(client, Component.translatable("halfmasa.auto_eat.sync_wait"), false); restore.warned = true; }
                }
                return;
            }
        }
        if (this.state == State.WAIT_RESTORE)
        {
            if (this.sync.confirmed(restore.hotbar, restore.original, this.requestRevision)
                    && this.sync.confirmed(restore.source, restore.remainder, this.requestRevision))
            {
                this.pendingRestore = null; this.state = State.IDLE;
                if (!this.feeding) finishSelection(); return;
            }
            if (this.ticks >= this.deadline)
            {
                this.feeding = false;
                if (!restore.warned) { MinecraftClientCompat.setOverlayMessage(client, Component.translatable("halfmasa.auto_eat.sync_wait"), false); restore.warned = true; }
            }
            return;
        }
        if (!safeInventory(client) || this.actor.isUsingItem() || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) return;
        ItemStack sourceStack = this.actor.getInventory().getItem(restore.source);
        ItemStack hotbarStack = this.actor.getInventory().getItem(restore.hotbar);
        boolean foodMatches = hotbarStack.isEmpty() || ItemStack.isSameItemSameComponents(hotbarStack, restore.food)
                && hotbarStack.getCount() <= restore.food.getCount() || isUseRemainder(hotbarStack, restore.food);
        if (!ItemStack.matches(sourceStack, restore.original) || !foodMatches)
        {
            if (!restore.warned) { MinecraftClientCompat.setOverlayMessage(client, Component.translatable("halfmasa.auto_eat.restore_conflict"), false); restore.warned = true; }
            return;
        }
        if (hotbarStack.isEmpty() && sourceStack.isEmpty())
        { this.pendingRestore = null; this.state = State.IDLE; if (!this.feeding) finishSelection(); return; }
        restore.remainder = hotbarStack.copy(); this.requestRevision = this.sync.revision();
        this.deadline = this.ticks + RESPONSE_TIMEOUT_TICKS; this.state = State.WAIT_RESTORE;
        //#if MC >= 26.0
        if (!sendInput(client, this.actor.inventoryMenu, playerMenuSlot(restore.source), restore.hotbar, ContainerInput.SWAP)) this.state = State.IDLE;
        //#else
        //$$ if (!sendInput(client, this.actor.inventoryMenu, playerMenuSlot(restore.source), restore.hotbar, ClickType.SWAP)) this.state = State.IDLE;
        //#endif
    }

    private boolean safeInventory(Minecraft client)
    {
        return this.actor.containerMenu == this.actor.inventoryMenu && this.actor.inventoryMenu.getCarried().isEmpty()
                && MinecraftClientCompat.getScreen(client) == null;
    }

    private void prepareCandidates()
    {
        this.candidates.clear();
        for (boolean ender : new boolean[] {false, true})
            for (int index = 0; index < STORAGE_SLOTS; index++)
            {
                ItemStack stack = this.actor.getInventory().getItem(index);
                ContainerScan scan = this.emptyContainers.get(index);
                if (scan != null && ItemStack.matches(stack, scan.stack)
                        && (scan.inventory == null || inventoryMatches(scan.inventory))) continue;
                boolean shulker = stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock;
                if (ender ? stack.is(Items.ENDER_CHEST) : shulker)
                {
                    if (!ender)
                    {
                        var contents = stack.get(DataComponents.CONTAINER);
                    //#if MC >= 26.0
                    if (contents == null || contents.nonEmptyItemCopyStream().noneMatch(food ->
                    //#else
                    //$$ if (contents == null || contents.nonEmptyStream().noneMatch(food ->
                    //#endif
                                AutoEatFoodRules.isAllowed(food) && capacity(food) > 0 && canKeepUseRemainder(food, true))) continue;
                    }
                    this.candidates.add(new Candidate(index, stack.copy(), ender));
                }
            }
    }

    private boolean openNext(Minecraft client)
    {
        if (!safeInventory(client)) return false;
        while (!this.candidates.isEmpty())
        {
            Candidate candidate = this.candidates.removeFirst();
            if (!ItemStack.matches(this.actor.getInventory().getItem(candidate.index), candidate.stack)) continue;
            this.opening = candidate; this.requestRevision = this.sync.revision();
            if (AutoEatQuickShulkerBridge.open(client, candidate.index))
            { this.state = State.WAIT_OPEN; this.deadline = this.ticks + RESPONSE_TIMEOUT_TICKS; return true; }
        }
        return false;
    }

    private void handleContainer(Minecraft client)
    {
        AbstractContainerMenu menu = this.actor.containerMenu;
        if (this.state == State.WAIT_OPEN && menu == this.actor.inventoryMenu && MinecraftClientCompat.getScreen(client) != null)
        { this.remoteSuppressed = true; cancel(client, true); return; }
        if (this.ticks >= this.deadline)
        {
            this.remoteSuppressed = true;
            MinecraftClientCompat.setOverlayMessage(client, Component.translatable("halfmasa.auto_eat.remote_timeout"), false);
            cancel(client, false); return;
        }
        if (this.state == State.WAIT_OPEN)
        {
            if (menu == this.actor.inventoryMenu) return;
            boolean expected = this.opening.ender ? menu instanceof ChestMenu : menu instanceof ShulkerBoxMenu;
            if (!expected) { this.ownedMenu = null; cancel(client, true); this.remoteSuppressed = true; return; }
            if (!this.sync.fullMenuAfter(menu, this.requestRevision)) return;
            this.ownedMenu = menu;
            int chosen = -1; ItemStack best = ItemStack.EMPTY; boolean hasAllowedFood = false;
            for (var slot : menu.slots)
                if (slot.container != this.actor.getInventory() && slot.mayPickup(this.actor)
                        && AutoEatFoodRules.isAllowed(slot.getItem()) && slot.getItem().getCount() <= slot.getItem().getMaxStackSize())
                {
                    hasAllowedFood = true;
                    if (capacity(slot.getItem()) > 0 && canKeepUseRemainder(slot.getItem(), true)
                            && AutoEatFoodRules.isBetter(slot.getItem(), best, targetHunger() - this.actor.getFoodData().getFoodLevel()))
                    { chosen = slot.index; best = slot.getItem(); }
                }
            if (chosen < 0)
            {
                // Capacity blocks are retried only after the inventory changes, rather than treated as empty forever.
                this.emptyContainers.put(this.opening.index, new ContainerScan(this.actor.getInventory().getItem(this.opening.index).copy(),
                        hasAllowedFood ? inventorySnapshot() : null));
                closeOwned(client); this.state = State.IDLE;
                if (!openNext(client)) { this.feeding = false; finishSelection(); this.nextScan = this.ticks + IDLE_RESCAN_TICKS; }
                return;
            }
            this.takeSlot = chosen; this.takeCount = best.getCount(); this.takingFood = best.copy();
            this.inventoryCount = this.sync.count(best); this.requestRevision = this.sync.revision();
            this.beforeTake = inventorySnapshot();
            this.state = State.WAIT_TAKE; this.deadline = this.ticks + RESPONSE_TIMEOUT_TICKS;
            //#if MC >= 26.0
            sendInput(client, menu, chosen, 0, ContainerInput.QUICK_MOVE);
            //#else
            //$$ sendInput(client, menu, chosen, 0, ClickType.QUICK_MOVE);
            //#endif
        }
        else
        {
            if (menu != this.ownedMenu) { this.ownedMenu = null; cancel(client, true); return; }
            if (this.sync.changedMenuSlot(menu, this.takeSlot, this.takeCount, this.requestRevision)
                    && this.sync.count(this.takingFood) > this.inventoryCount)
            {
                if (Configs.AUTO_EAT_RESTORE_SLOT.getBooleanValue()) rememberWithdrawal(menu);
                closeOwned(client); this.state = State.IDLE; this.candidates.clear();
            }
        }
    }

    private void rememberWithdrawal(AbstractContainerMenu menu)
    {
        int destination = -1, gained = 0;
        for (int i = 0; i < STORAGE_SLOTS; i++)
        {
            ItemStack current = this.actor.getInventory().getItem(i), previous = this.beforeTake.get(i);
            int oldCount = ItemStack.isSameItemSameComponents(previous, this.takingFood) ? previous.getCount() : 0;
            if (ItemStack.isSameItemSameComponents(current, this.takingFood) && current.getCount() > oldCount)
            {
                if (!previous.isEmpty() || destination >= 0) { returnConflict(Minecraft.getInstance()); return; }
                destination = i; gained = current.getCount();
            }
        }
        if (destination < 0 || gained != this.takeCount) { returnConflict(Minecraft.getInstance()); return; }
        ItemStack container = this.actor.getInventory().getItem(this.opening.index).copy();
        if (!this.opening.ender)
        {
            var contents = new java.util.ArrayList<ItemStack>();
            for (var slot : menu.slots) if (slot.container != this.actor.getInventory()) contents.add(slot.getItem().copy());
            container.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(contents));
        }
        this.pendingReturn = new RemoteWithdrawal(new Candidate(this.opening.index,container,this.opening.ender),this.takeSlot,destination,this.takingFood);
    }

    private boolean returning()
    { return this.state == State.WAIT_RETURN_SWAP || this.state == State.WAIT_RETURN_OPEN || this.state == State.WAIT_RETURN; }

    private void returnConflict(Minecraft client)
    {
        MinecraftClientCompat.setOverlayMessage(client, Component.translatable("halfmasa.auto_eat.return_conflict"), false);
        this.pendingReturn = null; closeOwned(client); this.state = State.IDLE;
    }

    private boolean handleReturn(Minecraft client)
    {
        RemoteWithdrawal restore = this.pendingReturn;
        if (client.options.keyAttack.isDown() || client.options.keyUse.isDown() || this.actor.isUsingItem()) return true;
        if (this.state == State.WAIT_RETURN_SWAP)
        {
            var local = this.pendingRestore;
            if (!safeInventory(client)) { returnConflict(client); return true; }
            if (!this.sync.confirmed(local.hotbar,local.food,local.swapRevision)
                    || !this.sync.confirmed(local.source,local.original,local.swapRevision))
            { if (this.ticks >= this.deadline) returnConflict(client); return true; }
            local.swapped = true; restore.hotbar = local.hotbar;
            openReturn(client,restore); return true;
        }
        if (this.state == State.WAIT_RETURN_OPEN)
        {
            var menu = this.actor.containerMenu;
            if (menu == this.actor.inventoryMenu) {
                if (MinecraftClientCompat.getScreen(client) != null || this.ticks >= this.deadline) returnConflict(client);
                return true;
            }
            boolean expected = restore.origin.ender ? menu instanceof ChestMenu : menu instanceof ShulkerBoxMenu;
            if (!expected) { this.ownedMenu = null; returnConflict(client); return true; }
            if (!this.sync.fullMenuAfter(menu,this.requestRevision)) { if (this.ticks >= this.deadline) returnConflict(client); return true; }
            this.ownedMenu = menu;
            if (restore.menuSlot < 0 || restore.menuSlot >= menu.slots.size()
                    || !menu.getSlot(restore.menuSlot).getItem().isEmpty()
                    || !menu.getSlot(restore.menuSlot).mayPlace(restore.returning)
                    || !ItemStack.matches(this.actor.getInventory().getItem(restore.hotbar),restore.returning))
            { returnConflict(client); return true; }
            this.requestRevision = this.sync.revision(); this.deadline = this.ticks + RESPONSE_TIMEOUT_TICKS;
            this.state = State.WAIT_RETURN;
            //#if MC >= 26.0
            if (!sendInput(client,menu,restore.menuSlot,restore.hotbar,ContainerInput.SWAP)) returnConflict(client);
            //#else
            //$$ if (!sendInput(client,menu,restore.menuSlot,restore.hotbar,ClickType.SWAP)) returnConflict(client);
            //#endif
            return true;
        }
        if (this.state == State.WAIT_RETURN)
        {
            if (this.actor.containerMenu != this.ownedMenu) { this.ownedMenu = null; returnConflict(client); return true; }
            if (this.sync.confirmed(restore.hotbar,ItemStack.EMPTY,this.requestRevision)
                    && this.sync.confirmedMenuSlot(this.ownedMenu,restore.menuSlot,restore.returning,this.requestRevision))
            { this.pendingReturn = null; closeOwned(client); this.state = State.IDLE; if (this.pendingRestore == null && !this.feeding) finishSelection(); }
            else if (this.ticks >= this.deadline) returnConflict(client);
            return true;
        }
        ItemStack current = this.actor.getInventory().getItem(restore.inventorySlot);
        if (current.isEmpty() && restore.consumed == restore.taken) { this.pendingReturn = null; if (!this.feeding) finishSelection(); return false; }
        if (this.feeding && AutoEatFoodRules.isAllowed(current)) return false;
        if (!Configs.AUTO_EAT_RESTORE_SLOT.getBooleanValue()) { this.pendingReturn = null; finishSelection(); return false; }
        if (!safeInventory(client) || Configs.AUTO_EAT_PAUSE_COMBAT.getBooleanValue()
                && (this.actor.hurtTime > 0 || this.ticks < this.combatUntil)) return true;
        boolean remaining = ItemStack.isSameItemSameComponents(current,restore.food) && current.getCount() == restore.taken - restore.consumed;
        boolean remainder = restore.consumed == restore.taken && isUseRemainder(current,restore.food);
        if ((!remaining && !remainder) || !ItemStack.matches(this.actor.getInventory().getItem(restore.origin.index),restore.origin.stack))
        { returnConflict(client); return true; }
        restore.returning = current.copy();
        if (restore.inventorySlot < HOTBAR_SLOTS) { restore.hotbar = restore.inventorySlot; openReturn(client,restore); }
        else { beginSwap(client,restore.inventorySlot,true); if (this.pendingRestore != null) this.state = State.WAIT_RETURN_SWAP; else returnConflict(client); }
        return true;
    }

    private void openReturn(Minecraft client, RemoteWithdrawal restore)
    {
        if (!safeInventory(client) || !ItemStack.matches(this.actor.getInventory().getItem(restore.origin.index),restore.origin.stack)
                || !AutoEatQuickShulkerBridge.open(client,restore.origin.index)) { returnConflict(client); return; }
        this.requestRevision = this.sync.revision(); this.deadline = this.ticks + RESPONSE_TIMEOUT_TICKS; this.state = State.WAIT_RETURN_OPEN;
    }

    private int capacity(ItemStack food)
    {
        int capacity = 0;
        for (int index = 0; index < STORAGE_SLOTS; index++)
        {
            ItemStack current = this.actor.getInventory().getItem(index);
            if (current.isEmpty()) capacity += food.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(current, food)) capacity += Math.max(0, current.getMaxStackSize() - current.getCount());
        }
        return capacity;
    }

    private java.util.List<ItemStack> inventorySnapshot()
    {
        var items = new java.util.ArrayList<ItemStack>(STORAGE_SLOTS);
        for (int index = 0; index < STORAGE_SLOTS; index++) items.add(this.actor.getInventory().getItem(index).copy());
        return items;
    }

    private boolean inventoryMatches(java.util.List<ItemStack> snapshot)
    {
        for (int index = 0; index < STORAGE_SLOTS; index++)
            if (!ItemStack.matches(snapshot.get(index), this.actor.getInventory().getItem(index))) return false;
        return true;
    }

    private static ItemStack useRemainder(ItemStack food)
    {
        return AutoEatFoodRules.useRemainder(food);
    }

    private static boolean isUseRemainder(ItemStack stack, ItemStack food)
    {
        return !stack.isEmpty() && ItemStack.matches(stack, useRemainder(food));
    }

    /** A stacked drink inserts its bottle separately; reserve space before consumption or remote transfer. */
    private boolean canKeepUseRemainder(ItemStack food, boolean incoming)
    {
        ItemStack remainder = useRemainder(food);
        if (food.getCount() <= 1 || remainder.isEmpty()) return true;
        if (!incoming) return capacity(remainder) >= remainder.getCount();
        int emptySlots = 0, foodMergeCapacity = 0, remainderMergeCapacity = 0;
        for (int index = 0; index < STORAGE_SLOTS; index++)
        {
            ItemStack current = this.actor.getInventory().getItem(index);
            if (current.isEmpty()) emptySlots++;
            else
            {
                int free = Math.max(0, current.getMaxStackSize() - current.getCount());
                if (ItemStack.isSameItemSameComponents(current, food)) foodMergeCapacity += free;
                if (ItemStack.isSameItemSameComponents(current, remainder)) remainderMergeCapacity += free;
            }
        }
        int newFoodCount = Math.min(Math.max(0, food.getCount() - foodMergeCapacity), emptySlots * food.getMaxStackSize());
        int usedEmptySlots = (newFoodCount + food.getMaxStackSize() - 1) / food.getMaxStackSize();
        return remainderMergeCapacity + (emptySlots - usedEmptySlots) * remainder.getMaxStackSize() >= remainder.getCount();
    }

    private boolean hasEmptyStorage()
    {
        for (int index = 0; index < STORAGE_SLOTS; index++) if (this.actor.getInventory().getItem(index).isEmpty()) return true;
        return false;
    }

    private void closeOwned(Minecraft client)
    {
        if (this.ownedMenu != null && this.actor != null && this.actor.containerMenu == this.ownedMenu && this.ownedMenu.getCarried().isEmpty())
        {
            this.actor.closeContainer(); MinecraftClientCompat.setScreen(client, null);
        }
        this.ownedMenu = null;
    }

    private void cancel(Minecraft client, boolean manual)
    {
        if (manual) this.restoreSelection = false;
        if (this.ownsUse && this.actor != null && client.gameMode != null && this.actor == client.player
                && this.actor.isUsingItem() && ItemStack.isSameItemSameComponents(this.actor.getUseItem(), this.eatingFood))
            client.gameMode.releaseUsingItem(this.actor);
        this.ownsUse = false; this.feeding = false; this.candidates.clear(); closeOwned(client);
        if (manual && returning()) this.pendingReturn = null;
        if (this.state != State.WAIT_RESTORE && this.state != State.WAIT_SILENT && (manual || !returning())) this.state = State.IDLE;
        if (this.pendingRestore == null) finishSelection();
        this.nextScan = this.ticks + IDLE_RESCAN_TICKS;
    }

    private void finishSelection()
    {
        //#if MC >= 1.21.1
        // Cancellation stops new meals, but cannot undo a server request already sent.
        // Keep its slot and expected stack until the reply and inventory sync settle it.
        if (this.state == State.WAIT_SILENT) return;
        //#endif
        if (this.actor != null && this.selectedBefore >= 0 && this.restoreSelection && Configs.AUTO_EAT_RESTORE_SLOT.getBooleanValue()
                && selectedSlot(this.actor.getInventory()) == this.activeSlot)
            selectSlot(this.actor.getInventory(), this.selectedBefore);
        this.selectedBefore = -1; this.activeSlot = -1;
    }

    private void clearSession()
    {
        this.actor = null; this.level = null; this.state = State.IDLE; this.pendingRestore = null;
        this.pendingReturn = null; this.silentResult = null; this.silentSuppressed = false; this.silentWarned = false;
        this.ownedMenu = null; this.opening = null; this.candidates.clear(); this.emptyContainers.clear();
        this.feeding = false; this.ownsUse = false; this.remoteSuppressed = false;
        this.selectedBefore = -1; this.activeSlot = -1; this.nextScan = 0; this.combatUntil = 0;
    }
}
//#endif

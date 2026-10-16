package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Records only server packets, independently of local menu prediction. */
public final class AutoEatInventorySync
{
    private record Snapshot(long revision, ItemStack stack) {}
    private static final AutoEatInventorySync INSTANCE = new AutoEatInventorySync();
    private final Map<Integer, Snapshot> inventory = new HashMap<>();
    private final Map<Integer, Snapshot> menuSlots = new HashMap<>();
    private LocalPlayer player;
    private ClientLevel level;
    private AbstractContainerMenu menu;
    private long revision;
    private long fullRevision;
    private AutoEatInventorySync() {}
    public static AutoEatInventorySync getInstance() { return INSTANCE; }
    public long revision() { return this.revision; }
    public void clear()
    {
        this.inventory.clear(); this.menuSlots.clear(); this.menu = null; this.fullRevision = 0;
        this.player = null; this.level = null;
    }

    private void bind(Minecraft client)
    {
        if (this.player != client.player || this.level != client.level)
        {
            this.inventory.clear(); this.menuSlots.clear(); this.menu = null; this.fullRevision = 0;
            this.player = client.player; this.level = client.level;
        }
    }

    public void playerSlot(int index, ItemStack stack)
    {
        bind(Minecraft.getInstance());
        this.inventory.put(index, new Snapshot(++this.revision, stack.copy()));
    }

    public void menuSlot(int containerId, int index, ItemStack stack)
    {
        Minecraft client = Minecraft.getInstance(); bind(client);
        if (this.player == null) return;
        //#if MC < 1.21.3
        //$$ if (containerId == -2) { playerSlot(index, stack); return; }
        //#endif
        AbstractContainerMenu target = containerId == this.player.inventoryMenu.containerId
                ? this.player.inventoryMenu : this.player.containerMenu;
        if (target.containerId != containerId || index < 0 || index >= target.slots.size()) return;
        long updated = ++this.revision;
        var slot = target.getSlot(index);
        if (slot.container == this.player.getInventory())
            this.inventory.put(slot.getContainerSlot(), new Snapshot(updated, stack.copy()));
        if (target == this.player.containerMenu)
        {
            if (this.menu != target) { this.menuSlots.clear(); this.fullRevision = 0; this.menu = target; }
            this.menuSlots.put(index, new Snapshot(updated, stack.copy()));
        }
    }

    public void fullMenu(int containerId, java.util.List<ItemStack> items)
    {
        for (int i = 0; i < items.size(); i++) menuSlot(containerId, i, items.get(i));
        if (this.menu != null && this.menu.containerId == containerId) this.fullRevision = ++this.revision;
    }

    public boolean fullMenuAfter(AbstractContainerMenu target, long after)
    {
        return this.menu == target && this.fullRevision > after;
    }

    public boolean confirmed(int inventoryIndex, ItemStack expected, long after)
    {
        Snapshot actual = this.inventory.get(inventoryIndex);
        return actual != null && actual.revision > after && ItemStack.matches(actual.stack, expected);
    }

    public boolean changedMenuSlot(AbstractContainerMenu target, int index, int previousCount, long after)
    {
        Snapshot actual = this.menuSlots.get(index);
        return this.menu == target && actual != null && actual.revision > after && actual.stack.getCount() < previousCount;
    }

    public boolean confirmedMenuSlot(AbstractContainerMenu target, int index, ItemStack expected, long after)
    {
        Snapshot actual = this.menuSlots.get(index);
        return this.menu == target && actual != null && actual.revision > after && ItemStack.matches(actual.stack, expected);
    }

    public int count(ItemStack food)
    {
        int total = 0;
        for (int index = 0; index < 36; index++)
        {
            Snapshot actual = this.inventory.get(index);
            if (actual != null && ItemStack.isSameItemSameComponents(actual.stack, food)) total += actual.stack.getCount();
        }
        return total;
    }
}
//#endif

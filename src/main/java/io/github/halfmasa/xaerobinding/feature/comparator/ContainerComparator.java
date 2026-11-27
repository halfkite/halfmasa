package io.github.halfmasa.xaerobinding.feature.comparator;

//#if MC >= 1.21.1
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Read-only estimate from the client menu, never from player inventory. */
public final class ContainerComparator
{
    private ContainerComparator() {}

    public record Result(int signal, int maximum, int occupied, int slots,
                         double fullness, int disabled, int effective, int nextItems) {}

    public static Result calculate(AbstractContainerMenu menu, Component title)
    {
        // Ender chests use the same menu as regular chests. Standard titles
        // can be excluded; arbitrary server-supplied titles cannot identify a block.
        Component ender = Component.translatable("container.enderchest");
        if (title.equals(ender) || title.getString().equals(ender.getString())) return null;

        if (menu instanceof CrafterMenu crafter)
        {
            if (menu.slots.size() < 9) return null;
            int occupied = 0, disabled = 0, effective = 0;
            for (int i = 0; i < 9; i++)
            {
                boolean hasItem = !crafter.getSlot(i).getItem().isEmpty();
                boolean off = crafter.isSlotDisabled(i);
                if (hasItem) occupied++;
                if (off) disabled++;
                if (hasItem || off) effective++;
            }
            return new Result(effective, 9, occupied, 9, occupied / 9.0,
                    disabled, effective, -1);
        }

        int size;
        if (menu instanceof ChestMenu chest) size = chest.getRowCount() * 9;
        else if (menu instanceof ShulkerBoxMenu) size = 27;
        else if (menu instanceof HopperMenu) size = 5;
        else if (menu instanceof DispenserMenu) size = 9;
        else if (menu instanceof AbstractFurnaceMenu) size = 3;
        else if (menu instanceof BrewingStandMenu) size = 5;
        else return null;

        if (size <= 0 || menu.slots.size() < size) return null;
        Container container = menu.getSlot(0).container;
        if (container.getContainerSize() != size) return null;
        // Check layout before reading the backing container; reject altered layouts.
        for (int i = 0; i < size; i++)
        {
            Slot slot = menu.getSlot(i);
            if (slot.container != container || slot.getContainerSlot() != i) return null;
        }

        int signal = AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        int occupied = 0;
        double sum = 0;
        for (int i = 0; i < size; i++)
        {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty())
            {
                occupied++;
                int max = container.getMaxStackSize(stack);
                if (max > 0) sum += (double) stack.getCount() / max;
            }
        }
        double fullness = sum / size;
        // Theoretical 64-stack equivalents, not an inventory insertion simulation.
        int next = signal >= 15 ? -1 : signal == 0 ? 1 :
                Math.max(1, (int) Math.ceil((signal * size / 14.0 - sum) * 64.0 - 1e-9));
        return new Result(signal, 15, occupied, size, fullness, 0, occupied, next);
    }
}
//#else
//$$ public final class ContainerComparator { private ContainerComparator() {} }
//#endif

package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 26.3
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Preserves source block properties after vanilla has returned the authorized pick-block item. */
public final class SpawnerPickCapture implements IClientTickHandler
{
    private static final SpawnerPickCapture INSTANCE = new SpawnerPickCapture();
    private static final long REPLY_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(10);
    private Pending pending;
    private int replySlot = -1;
    private record Pending(ClientLevel level, Item item, BlockItemStateProperties properties,
                           CompoundTag sourceMob, long expiresAt) {}
    private SpawnerPickCapture() {}
    public static SpawnerPickCapture getInstance() { return INSTANCE; }

    public void capture(BlockPos position, boolean includeData)
    {
        this.pending = null;
        this.replySlot = -1;
        Minecraft client = Minecraft.getInstance();
        if (!includeData || client.level == null || client.player == null || !client.player.isCreative()) return;
        var state = client.level.getBlockState(position);
        Item item = state.is(Blocks.TRIAL_SPAWNER) ? Items.TRIAL_SPAWNER : state.is(Blocks.VAULT) ? Items.VAULT : null;
        if (item == null) return;
        var blockEntity = client.level.getBlockEntity(position);
        CompoundTag data = blockEntity != null ? blockEntity.getUpdateTag(client.level.registryAccess()) : new CompoundTag();
        this.pending = new Pending(client.level, item, propertiesFor(state),
                data.getCompoundOrEmpty("spawn_data").getCompoundOrEmpty("entity").copy(),
                System.nanoTime() + REPLY_TIMEOUT_NANOS);
    }

    static BlockItemStateProperties propertiesFor(BlockState state)
    {
        var properties = new HashMap<String, String>();
        state.getValues().forEach(value -> {
            var property = value.property();
            if (property.getName().equals("ominous") || property.getName().equals("trial_spawner_state") ||
                    property.getName().equals("vault_state") || property.getName().equals("facing"))
                properties.put(property.getName(), value.valueName());
        });
        return new BlockItemStateProperties(Map.copyOf(properties));
    }

    public void acknowledge(int slot) { if (this.pending != null) this.replySlot = slot; }

    @Override
    public void onClientTick(Minecraft client)
    {
        SpawnerItemModel.clearIfWorldChanged(client.level);
        if (this.pending == null) return;
        if (client.level != this.pending.level() || client.player == null || client.gameMode == null ||
                !client.player.isCreative() || System.nanoTime() > this.pending.expiresAt())
        {
            this.pending = null;
            return;
        }
        if (this.replySlot < 0 || this.replySlot >= 9 || client.player.getInventory().getSelectedSlot() != this.replySlot) return;
        ItemStack received = client.player.getInventory().getItem(this.replySlot);
        ItemStack copy = restoreCopiedItem(received, this.pending.item(), this.pending.properties(), this.pending.sourceMob());
        if (copy.isEmpty()) return;
        this.pending = null;
        if (ItemStack.isSameItemSameComponents(copy, received)) return;
        client.player.getInventory().setItem(this.replySlot, copy);
        // Creative inventory updates persist the captured state when moving or placing the item.
        client.gameMode.handleCreativeModeItemAdd(copy, 36 + this.replySlot);
    }

    static ItemStack restoreCopiedItem(ItemStack received, Item expected,
                                      BlockItemStateProperties properties, CompoundTag sourceMob)
    {
        var entityData = received.get(DataComponents.BLOCK_ENTITY_DATA);
        if (!received.is(expected) || entityData == null) return ItemStack.EMPTY;
        CompoundTag mob = entityData.copyTagWithoutId().getCompoundOrEmpty("spawn_data").getCompoundOrEmpty("entity");
        if (!sourceMob.isEmpty() && !sourceMob.equals(mob)) return ItemStack.EMPTY;
        ItemStack copy = received.copy();
        copy.set(DataComponents.BLOCK_STATE, properties);
        return copy;
    }
}
//#endif

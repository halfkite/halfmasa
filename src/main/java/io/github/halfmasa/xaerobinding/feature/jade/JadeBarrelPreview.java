//#if MC >= 26.1
package io.github.halfmasa.xaerobinding.feature.jade;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.JadeBarrelStorageAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import snownee.jade.addon.universal.ItemCollector;
import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.JadeIds;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.impl.BlockAccessorImpl;
import snownee.jade.impl.ObjectDataCenter;
import snownee.jade.network.RequestBlockPacket;
import snownee.jade.util.ClientProxy;

/** Client-only compensation for servers whose Jade provider returns individual TIS barrel halves. */
public final class JadeBarrelPreview
{
    private static final String REQUEST_ID = "halfmasa:jade_barrel_request";
    private static final int HALF_SLOTS = 27;
    // A lost request must not block the other half, or keep an old preview alive indefinitely.
    private static final long REQUEST_TIMEOUT_MS = 5_000;
    private static final long MIN_SAMPLE_LIFETIME_MS = 2_000;
    private static final Request[] REQUESTS = new Request[2];
    private static final Sample[] SAMPLES = new Sample[2];
    private static Object connection;
    private static Level level;
    private static Pair pair;
    private static long sequence;
    private static int nextHalf;
    private static boolean nativeCombined;
    private static boolean sorted, details;
    private static List<ViewGroup<ItemStack>> combined;
    private static Field tisRule;
    private static boolean checkedTis;
    private static boolean unavailableTis;

    private record Pair(BlockPos first, BlockPos second, Direction facing,
            BarrelBlockEntity firstEntity, BarrelBlockEntity secondEntity) {}
    private record Request(long id, int half, Pair pair, BlockAccessor accessor, long sent) {}
    private record Sample(List<ItemStack> items, long time) {}

    private JadeBarrelPreview() {}

    /** Called at Jade's own tick boundary, including disconnects and disabled overlays. */
    public static void tick()
    {
        Minecraft client = Minecraft.getInstance();
        if (connection != client.getConnection() || level != client.level)
        {
            reset();
            connection = client.getConnection();
            level = client.level;
        }
        if (!Configs.JADE_LARGE_BARREL.getBooleanValue() || level == null || client.player == null)
            reset();
        else if (pair != null && !validPair(pair))
            reset();
    }

    private static void reset()
    {
        pair = null;
        nextHalf = 0;
        nativeCombined = false;
        combined = null;
        for (int i = 0; i < 2; i++) { REQUESTS[i] = null; SAMPLES[i] = null; }
    }

    private static boolean tisEnabled()
    {
        if (!checkedTis)
        {
            checkedTis = true;
            if (FabricLoader.getInstance().isModLoaded("carpet-tis-addition"))
            {
                try { tisRule = Class.forName("carpettisaddition.CarpetTISAdditionSettings").getField("largeBarrel"); }
                catch (ReflectiveOperationException exception) { unavailableTis = true; }
            }
        }
        if (unavailableTis) return false;
        if (tisRule == null) return true; // Explicit opt-in also supports a client without Carpet/TIS.
        try { return tisRule.getBoolean(null); }
        catch (IllegalAccessException exception) { return false; }
    }

    private static Pair identify(Accessor<?> accessor)
    {
        if (!(accessor instanceof BlockAccessor block) || !accessor.isServerConnected()
                || accessor.getLevel() != level || !tisEnabled()
                || !(block.getBlockEntity() instanceof BarrelBlockEntity)) return null;
        BlockState state = block.getBlockState();
        if (!(state.getBlock() instanceof BarrelBlock)) return null;
        Direction facing = state.getValue(BarrelBlock.FACING);
        BlockPos pos = block.getPosition();
        BlockPos other = pos.relative(facing.getOpposite());
        if (!level.hasChunkAt(other)) return null;
        BlockState otherState = level.getBlockState(other);
        if (!(otherState.getBlock() instanceof BarrelBlock)
                || otherState.getValue(BarrelBlock.FACING) != facing.getOpposite()
                || !(level.getBlockEntity(other) instanceof BarrelBlockEntity)) return null;
        return facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                ? new Pair(pos.immutable(), other.immutable(), facing,
                        (BarrelBlockEntity) block.getBlockEntity(), (BarrelBlockEntity) level.getBlockEntity(other))
                : new Pair(other.immutable(), pos.immutable(), facing.getOpposite(),
                        (BarrelBlockEntity) level.getBlockEntity(other), (BarrelBlockEntity) block.getBlockEntity());
    }

    private static boolean validPair(Pair value)
    {
        if (!tisEnabled() || !level.hasChunkAt(value.first) || !level.hasChunkAt(value.second)) return false;
        BlockState first = level.getBlockState(value.first), second = level.getBlockState(value.second);
        return first.getBlock() instanceof BarrelBlock && second.getBlock() instanceof BarrelBlock
                && level.getBlockEntity(value.first) == value.firstEntity
                && level.getBlockEntity(value.second) == value.secondEntity
                && first.getValue(BarrelBlock.FACING) == value.facing
                && second.getValue(BarrelBlock.FACING) == value.facing.getOpposite();
    }

    private static Pair bind(Accessor<?> accessor)
    {
        tick();
        if (!Configs.JADE_LARGE_BARREL.getBooleanValue()) return null;
        Pair target = identify(accessor);
        if (!Objects.equals(pair, target))
        {
            reset();
            pair = target;
            if (target != null) nextHalf = ((BlockAccessor) accessor).getPosition().equals(target.first) ? 0 : 1;
        }
        boolean sort = accessor.getServerData().getBooleanOr("SortItems", false);
        if (sorted != sort || details != accessor.showDetails())
        {
            combined = null;
            SAMPLES[0] = SAMPLES[1] = null;
            REQUESTS[0] = REQUESTS[1] = null;
            sorted = sort;
            details = accessor.showDetails();
        }
        return target;
    }

    /** Replace one native request with one request; never increase Jade's packet rate. */
    public static boolean request(BlockAccessor accessor, List<IServerDataProvider<BlockAccessor>> providers)
    {
        Pair target = bind(accessor);
        if (target == null || nativeCombined || !providers.contains(ItemStorageProvider.BLOCK)) return false;
        long now = Util.getMillis();
        int half = nextHalf;
        if (REQUESTS[half] != null && now - REQUESTS[half].sent < REQUEST_TIMEOUT_MS)
            half = 1 - half;
        if (REQUESTS[half] != null && now - REQUESTS[half].sent < REQUEST_TIMEOUT_MS) return true;
        BlockPos position = half == 0 ? target.first : target.second;
        boolean primary = position.equals(accessor.getPosition());
        CompoundTag data = primary ? accessor.getServerData().copy() : new CompoundTag();
        long id = ++sequence;
        data.putLong(REQUEST_ID, id);
        data.putBoolean("SortItems", sorted);
        data.putInt("MaxItemCount", accessor.getServerData().getIntOr("MaxItemCount", ItemCollector.MAX_SIZE));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(position), accessor.getSide(), position, false);
        REQUESTS[half] = new Request(id, half, target, accessor, now);
        nextHalf = 1 - half;
        List<IServerDataProvider<BlockAccessor>> requestedProviders = primary ? providers : List.of(ItemStorageProvider.BLOCK);
        ClientProxy.sendPacket(new RequestBlockPacket(
                new BlockAccessorImpl.SyncData(details, hit, accessor.getServersideRep(), data), requestedProviders));
        return true;
    }

    /** Consume auxiliary replies before Jade can replace the current target's payload. */
    public static boolean receive(CompoundTag data)
    {
        if (!data.contains(REQUEST_ID)) return false;
        tick();
        long id = data.getLongOr(REQUEST_ID, -1);
        Request request = null;
        for (Request candidate : REQUESTS) if (candidate != null && candidate.id == id) request = candidate;
        if (request == null || !Objects.equals(pair, request.pair) || !validPair(request.pair)
                || Util.getMillis() - request.sent > REQUEST_TIMEOUT_MS) return true;
        REQUESTS[request.half] = null;
        BlockPos pos = request.half == 0 ? pair.first : pair.second;
        if (data.getIntOr("x", Integer.MIN_VALUE) != pos.getX()
                || data.getIntOr("y", Integer.MIN_VALUE) != pos.getY()
                || data.getIntOr("z", Integer.MIN_VALUE) != pos.getZ()
                || !"minecraft:barrel".equals(data.getStringOr("BlockId", ""))) return true;
        // Only use Jade's default, ungrouped container data. Respect locks, loot and provider failures.
        var raw = data.get(JadeIds.UNIVERSAL_ITEM_STORAGE.toString());
        var decoded = raw == null ? null : request.accessor.decodeFromNbt(JadeBarrelStorageAccessor.halfmasa$codec(), raw).orElse(null);
        if (data.getBooleanOr("Locked", false) || data.getBooleanOr("Loot", false) || decoded == null
                || !decoded.getKey().equals(JadeIds.UNIVERSAL_ITEM_STORAGE_DEFAULT) || decoded.getValue().size() != 1
                || decoded.getValue().getFirst().id != null
                || decoded.getValue().getFirst().getExtraData().contains("Collecting"))
        {
            SAMPLES[request.half] = null;
            combined = null;
        }
        else
        {
            List<ItemStack> items = decoded.getValue().getFirst().views;
            if (items.size() > HALF_SLOTS)
            {
                nativeCombined = true;
                SAMPLES[0] = SAMPLES[1] = null;
            }
            else SAMPLES[request.half] = new Sample(items.stream().map(JadeBarrelPreview::copyDisplayStack).toList(), Util.getMillis());
            combined = null;
        }
        // The current target's normal metadata still belongs to Jade, even if the player switched halves.
        var current = snownee.jade.JadeClient.tickHandler().state;
        return current == null || !(current.accessor() instanceof BlockAccessor block) || !block.getPosition().equals(pos);
    }

    private static ItemStack copyDisplayStack(ItemStack source)
    {
        ItemStack copy = source.copy();
        CustomData custom = copy.get(DataComponents.CUSTOM_DATA);
        // Jade uses this wire-only field for totals above 99 and leaves it on decoded stacks.
        // Remove it before comparing components so unequal half-totals still merge.
        if (copy.getCount() >= 99 && custom != null && custom.copyTag().contains("__JadeCount"))
        {
            CompoundTag tag = custom.copyTag();
            tag.remove("__JadeCount");
            if (tag.isEmpty()) copy.remove(DataComponents.CUSTOM_DATA);
            else copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return copy;
    }

    public static List<ViewGroup<ItemStack>> groups(Accessor<?> accessor, List<ViewGroup<ItemStack>> original)
    {
        if (bind(accessor) == null || nativeCombined || SAMPLES[0] == null || SAMPLES[1] == null) return original;
        long now = Util.getMillis();
        long lifetime = Math.max(MIN_SAMPLE_LIFETIME_MS, (long) ObjectDataCenter.rateLimiter * 4);
        if (now - SAMPLES[0].time > lifetime || now - SAMPLES[1].time > lifetime) return original;
        if (combined == null)
            combined = List.of(new ViewGroup<>(merge(SAMPLES[0].items, SAMPLES[1].items, sorted)));
        return combined;
    }

    /** Match the entire component patch: named, enchanted and filled items stay distinct. */
    public static List<ItemStack> merge(List<ItemStack> first, List<ItemStack> second, boolean sort)
    {
        var counts = new LinkedHashMap<ItemCollector.ItemDefinition, Long>();
        for (var half : List.of(first, second)) for (ItemStack stack : half)
        {
            if (stack.isEmpty()) continue;
            var key = new ItemCollector.ItemDefinition(stack.getItem(), stack.getComponentsPatch());
            counts.merge(key, (long) stack.getCount(), Long::sum);
        }
        var result = new ArrayList<ItemStack>();
        counts.forEach((key, count) -> result.add(key.toStack((int) Math.min(Integer.MAX_VALUE, count))));
        if (sort) result.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
        return List.copyOf(result);
    }
}
//#endif

//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.feature;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import io.github.halfmasa.xaerobinding.compat.LitematicaKelpSelection;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All world access and block interactions stay on the client thread.
 * Feature inspiration: lpctools. Feature source/reference: Helpful-Tweake.
 */
public final class AutoKelpClearing implements IClientTickHandler {
    private static final AutoKelpClearing INSTANCE = new AutoKelpClearing();
    private static final int SCAN_BUDGET = 256;
    private static final int RETRY_TICKS = 20;
    private static final int IDLE_TICKS = 5;
    private static final double MAX_RANGE = 8.0;
    private static final boolean HAS_LITEMATICA = FabricLoader.getInstance().isModLoaded("litematica");
    private static final List<BlockPos> OFFSETS = createOffsets();

    private ClientLevel level;
    private LocalPlayer actor;
    private BlockPos origin;
    private int cursor;
    private long tick;
    // Tracked even while disabled; reset() must not clear this edge state.
    private boolean guiWasOpen;
    private long nextScan;
    private double lastRange = -1;
    private boolean lastRoot;
    private boolean lastRestricted;
    private List<LitematicaKelpSelection.Bounds> lastBoxes = List.of();
    private final Map<BlockPos, Long> retry = new HashMap<>();

    private AutoKelpClearing() {}
    public static AutoKelpClearing getInstance() { return INSTANCE; }

    private static List<BlockPos> createOffsets() {
        int radius = (int) Math.ceil(MAX_RANGE) + 1;
        var list = new ArrayList<BlockPos>();
        for (int x = -radius; x <= radius; x++)
            for (int y = -radius; y <= radius; y++)
                for (int z = -radius; z <= radius; z++) {
                    // Distance from the eye's grid cell to this block's grid cell.
                    int dx = Math.max(0, Math.abs(x) - 1);
                    int dy = Math.max(0, Math.abs(y) - 1);
                    int dz = Math.max(0, Math.abs(z) - 1);
                    if (dx*dx + dy*dy + dz*dz <= MAX_RANGE*MAX_RANGE)
                        list.add(new BlockPos(x, y, z));
                }
        list.sort(Comparator.comparingInt(p -> p.getX()*p.getX() + p.getY()*p.getY() + p.getZ()*p.getZ()));
        return List.copyOf(list);
    }

    private void reset() {
        level = null;
        actor = null;
        origin = null;
        cursor = 0;
        nextScan = 0;
        lastRange = -1;
        lastBoxes = List.of();
        retry.clear();
    }

    private static boolean isKelp(BlockState state) {
        return state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }

    private static boolean loaded(ClientLevel world, BlockPos p) {
        return !world.isOutsideBuildHeight(p) && world.hasChunkAt(p);
    }

    private static boolean inBoxes(BlockPos p, List<LitematicaKelpSelection.Bounds> boxes) {
        for (var box : boxes) if (box.contains(p)) return true;
        return false;
    }

    private static boolean root(ClientLevel world, BlockPos pos) {
        BlockPos below = pos.below();
        return world.isOutsideBuildHeight(below) || (loaded(world, below) && !isKelp(world.getBlockState(below)));
    }

    /** Fail closed: every block potentially removed by the kelp cascade must be selected. */
    private static boolean cascadeInside(ClientLevel world, BlockPos pos, List<LitematicaKelpSelection.Bounds> boxes) {
        for (BlockPos p = pos; !world.isOutsideBuildHeight(p); p = p.above()) {
            if (!loaded(world, p)) return false;
            if (!isKelp(world.getBlockState(p))) return true;
            if (!inBoxes(p, boxes)) return false;
        }
        return true;
    }

    private static double distanceSqToBlock(Vec3 eye, BlockPos p) {
        double x = Math.max(0, Math.max(p.getX() - eye.x, eye.x - p.getX() - 1));
        double y = Math.max(0, Math.max(p.getY() - eye.y, eye.y - p.getY() - 1));
        double z = Math.max(0, Math.max(p.getZ() - eye.z, eye.z - p.getZ() - 1));
        return x*x + y*y + z*z;
    }

    private static BlockHitResult findHit(ClientLevel world, LocalPlayer player, BlockPos pos, double range) {
        Vec3 eye = player.getEyePosition();
        // Aim at center of the outline; water itself never occludes the ray.
        var hit = world.clip(new ClipContext(eye, Vec3.atCenterOf(pos),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) return null;
        return eye.distanceToSqr(hit.getLocation()) <= range*range ? hit : null;
    }

    private static double range(LocalPlayer player) {
        double vanilla = player.blockInteractionRange();
        double wanted = Configs.AUTO_KELP_CUSTOM_REACH_ENABLED.getBooleanValue()
            ? Configs.AUTO_KELP_CUSTOM_REACH.getDoubleValue() : vanilla;
        // Independent reach is an upper bound, never a global reach modifier.
        return Math.max(0, Math.min(MAX_RANGE, Math.min(wanted, vanilla)));
    }

    @Override public void onClientTick(Minecraft client) {
        tick++;
        boolean guiOpen = MinecraftClientCompat.getScreen(client) != null;
        boolean openedGui = guiOpen && !guiWasOpen;
        guiWasOpen = guiOpen;
        if (openedGui && client.level != null && client.player != null
                && Configs.AUTO_KELP_CLEARING.getBooleanValue()
                && Configs.AUTO_KELP_DISABLE_ON_GUI.getBooleanValue()) {
            Configs.AUTO_KELP_CLEARING.setBooleanValue(false);
        }
        if (!Configs.AUTO_KELP_CLEARING.getBooleanValue() || client.level == null
                || client.player == null || client.gameMode == null || !client.player.isAlive()
                || client.player.isSpectator() || !client.player.getAbilities().mayBuild
                || guiOpen || !client.isWindowActive()) {
            reset();
            return;
        }
        if (level != client.level || actor != client.player) {
            reset();
            level = client.level;
            actor = client.player;
        }
        if (client.options.keyAttack.isDown() || client.options.keyUse.isDown()
                || actor.isUsingItem() || client.gameMode.isDestroying()) return;

        double reach = range(actor);
        if (!Double.isFinite(reach) || reach <= 0) return;
        boolean rootOnly = Configs.AUTO_KELP_ROOT_ONLY.getBooleanValue();
        boolean restricted = Configs.AUTO_KELP_SELECTION_ONLY.getBooleanValue();
        List<LitematicaKelpSelection.Bounds> boxes = List.of();
        if (restricted) {
            if (!HAS_LITEMATICA) { reset(); return; }
            try { boxes = LitematicaKelpSelection.snapshot(); }
            catch (LinkageError | RuntimeException unavailable) { reset(); return; }
            if (boxes.isEmpty()) { reset(); return; }
        }
        Vec3 eye = actor.getEyePosition();
        BlockPos now = BlockPos.containing(eye);
        if (!now.equals(origin) || Double.compare(reach, lastRange) != 0
                || rootOnly != lastRoot || restricted != lastRestricted || !boxes.equals(lastBoxes)) {
            origin = now;
            cursor = 0;
            nextScan = 0;
            lastRange = reach;
            lastRoot = rootOnly;
            lastRestricted = restricted;
            lastBoxes = boxes;
        }
        retry.entrySet().removeIf(e -> e.getValue() <= tick);
        if (tick < nextScan) return;

        int limit = Configs.AUTO_KELP_BLOCKS_PER_TICK.getIntegerValue();
        int attempts = 0;
        for (int checked = 0; checked < SCAN_BUDGET && cursor < OFFSETS.size() && attempts < limit; checked++) {
            BlockPos offset = OFFSETS.get(cursor++);
            double bound = reach + Math.sqrt(3.0);
            if (offset.getX()*offset.getX() + offset.getY()*offset.getY() + offset.getZ()*offset.getZ() > bound*bound) {
                cursor = OFFSETS.size();
                break;
            }
            BlockPos pos = origin.offset(offset);
            if (retry.containsKey(pos) || distanceSqToBlock(eye, pos) > reach*reach
                    || !loaded(level, pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!isKelp(state) || (rootOnly && !root(level, pos))) continue;
            if (restricted && (!inBoxes(pos, boxes) || !cascadeInside(level, pos, boxes))) continue;
            // Kelp is instant-break in vanilla. Do not start a slow mining session when a mod changes it.
            if (!actor.isCreative() && state.getDestroyProgress(actor, level, pos) < 1.0F) continue;
            BlockHitResult hit = findHit(level, actor, pos, reach);
            if (hit == null) continue;
            retry.put(pos, tick + RETRY_TICKS);
            attempts++;
            if (client.gameMode.startDestroyBlock(pos, hit.getDirection())) {
                //#if MC >= 26.3
                actor.swing(InteractionHand.MAIN_HAND, actor.getMainHandItem().getAttackAnimation(), false);
                //#else
                //$$ actor.swing(InteractionHand.MAIN_HAND);
                //#endif
            }
            if (client.gameMode.isDestroying()) break;
        }
        if (cursor >= OFFSETS.size()) {
            cursor = 0;
            nextScan = tick + IDLE_TICKS;
        }
    }
}
//#endif

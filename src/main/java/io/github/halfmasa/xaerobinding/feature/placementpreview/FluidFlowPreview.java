package io.github.halfmasa.xaerobinding.feature.placementpreview;
//#if MC >= 1.21.1
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
//#if MC >= 1.21.11
import net.minecraft.world.attribute.EnvironmentAttributes;
//#endif
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//#if MC >= 1.21.11
import net.minecraft.world.level.gamerules.GameRules;
//#else
//$$ import net.minecraft.world.level.GameRules;
//#endif
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.PlacementPreviewFluidInvoker;
import io.github.halfmasa.xaerobinding.mixin.PlacementPreviewItemInvoker;
import io.github.halfmasa.xaerobinding.mixin.PlacementPreviewCauldronInvoker;

/** Compares two bounded flow estimates over the same client block snapshot. */
public final class FluidFlowPreview {
    private FluidFlowPreview() {}
    private static final Direction[] SIDES={Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};
    private static final int MAX_CELLS=1024, MAX_WORK=100000;
    private record Cell(FluidState fluid, boolean contained) {}
    private record Key(ClientLevel world,BlockPos pos,int radius,int depth,boolean contained, Kind kind,
                       int dropOff, int slopeDistance, boolean sourceConversion) {}
    private record Prediction(Map<BlockPos,Cell> cells,boolean truncated) {}
    private static Key key;
    private static Prediction cached;
    private static long updated;
    static void clear(){key=null;cached=null;}

    /** True if bucket is selected, even when no valid placement is predicted. */
    static boolean renderIfHeld(Minecraft mc){
        var player=mc.player;
        var stack=player.getMainHandItem();
        if(stack.isEmpty()&&Configs.PLACEMENT_PREVIEW_OFFHAND.getBooleanValue())stack=player.getOffhandItem();
        Kind kind=stack.is(Items.WATER_BUCKET)?Kind.WATER:stack.is(Items.LAVA_BUCKET)?Kind.LAVA:null;
        if(kind==null){clear();return false;}
        if(!Configs.PLACEMENT_PREVIEW_FLUID.getBooleanValue()||!kind.enabled()){clear();return true;}
        var world=mc.level;
        // Cauldrons consume the block interaction before BucketItem.use runs.
        if (!player.isSecondaryUseActive() && mc.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK && valid(world, blockHit.getBlockPos())
                && renderCauldron(world, blockHit.getBlockPos(), kind)) return true;
        var hit=PlacementPreviewItemInvoker.halfmasa$bucketRay(world,player,ClipContext.Fluid.NONE);
        if(hit.getType()!=HitResult.Type.BLOCK){clear();return true;}
        var clicked=hit.getBlockPos();
        if(!valid(world,clicked)){clear();return true;}
        var state=world.getBlockState(clicked);
        var adjacent = clicked.relative(hit.getDirection());
        var source = kind == Kind.WATER && state.getBlock() instanceof LiquidBlockContainer
                && !player.isShiftKeyDown() ? clicked : adjacent;
        if (!source.equals(adjacent) && !canEmptyAt(player, world, source, kind)) source = adjacent;
        if(!valid(world,source)){clear();return true;}
        var target=world.getBlockState(source);
        boolean contained = !target.canBeReplaced(kind.fluid()) && target.getBlock() instanceof LiquidBlockContainer
                && canEmptyAt(player, world, source, kind);
        FluidState oldFluid = target.getFluidState();
        boolean sameSource = oldFluid.isSource() && kind.fluid().isSame(oldFluid.getType());
        //#if MC >= 1.21.11
        boolean evaporates = world.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, source);
        //#else
        //$$ boolean evaporates = world.dimensionType().ultraWarm();
        //#endif
        if((kind==Kind.WATER&&evaporates)
                || (!sameSource && !canEmptyAt(player, world, source, kind))){
            clear();if(Configs.PLACEMENT_PREVIEW_INVALID.getBooleanValue())PlacementPreviewBoxes.draw(new AABB(source).inflate(.002), 0xffff4545);return true;
        }
        if (sameSource) {
            clear();
            PlacementPreviewBoxes.draw(new AABB(source).inflate(.002), 0xff000000 | kind.color(true, false));
            return true;
        }
        // Non-waterlogged special containers have side effects outside this estimator.
        if(contained&&(kind!=Kind.WATER||!target.hasProperty(BlockStateProperties.WATERLOGGED))){clear();return true;}
        var access=(PlacementPreviewFluidInvoker)(Object)kind.fluid();
        int dropOff=access.halfmasa$dropOff(world), slopeDistance=access.halfmasa$slopeDistance(world);
        // Prevent pathological custom rules from causing unbounded searches.
        if(dropOff<=0||dropOff>8||slopeDistance<0||slopeDistance>16){clear();return true;}
        var server = mc.getSingleplayerServer();
        var serverLevel = server == null ? null : server.getLevel(world.dimension());
        //#if MC >= 1.21.11
        boolean sourceConversion = serverLevel == null ? kind == Kind.WATER : serverLevel.getGameRules().get(
                kind == Kind.WATER ? GameRules.WATER_SOURCE_CONVERSION : GameRules.LAVA_SOURCE_CONVERSION);
        //#else
        //$$ boolean sourceConversion = serverLevel == null ? kind == Kind.WATER : serverLevel.getGameRules().getBoolean(
        //$$         kind == Kind.WATER ? GameRules.RULE_WATER_SOURCE_CONVERSION : GameRules.RULE_LAVA_SOURCE_CONVERSION);
        //#endif
        var next=new Key(world,source.immutable(),kind.radius(),kind.depth(),contained,kind,dropOff,slopeDistance,sourceConversion);
        long now=System.nanoTime();
        if(!next.equals(key)||cached==null||now-updated>=250_000_000L){
            cached=predict(next);key=next;updated=now;
        }
        int alpha=Math.round(kind.opacity()*2.55f);
        for(var entry:cached.cells().entrySet()){
            var p=entry.getKey();var c=entry.getValue();
            boolean falling = c.fluid().getValue(FlowingFluid.FALLING);
            boolean sameAbove = cached.cells().containsKey(p.above())
                    || kind.fluid().isSame(world.getFluidState(p.above()).getType());
            double height = falling || sameAbove ? 1 : c.fluid().getOwnHeight();
            int color=kind.color(c.fluid().isSource(),falling);
            // Waterlogged geometry is indicated by outline, not a solid filled cube.
            int fill=c.contained()?0:(alpha<<24)|color;
            PlacementPreviewBoxes.draw(new AABB(p.getX()+.003,p.getY()+.003,p.getZ()+.003,p.getX()+.997,p.getY()+height,p.getZ()+.997),
                    0xff000000|color, fill);
        }
        if(cached.truncated())PlacementPreviewBoxes.draw(new AABB(source).inflate(.01), 0xffffbb44);
        return true;
    }

    private static boolean renderCauldron(ClientLevel world, BlockPos pos, Kind kind) {
        BlockState current = world.getBlockState(pos);
        if (!current.is(Blocks.CAULDRON) && !current.is(Blocks.WATER_CAULDRON)
                && !current.is(Blocks.LAVA_CAULDRON) && !current.is(Blocks.POWDER_SNOW_CAULDRON)) return false;
        clear();
        if (world.getFluidState(pos.above()).is(FluidTags.WATER)) {
            if (Configs.PLACEMENT_PREVIEW_INVALID.getBooleanValue()) {
                PlacementPreviewBoxes.draw(new AABB(pos).inflate(.002), 0xffff4545);
            }
            return true;
        }
        BlockState filled = kind == Kind.WATER
                ? Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
                : Blocks.LAVA_CAULDRON.defaultBlockState();
        double surface = ((PlacementPreviewCauldronInvoker) filled.getBlock()).halfmasa$contentHeight(filled);
        double inset = 2.0 / 16.0 + .002;
        double y = pos.getY() + surface + .002;
        int rgb = kind.color(true, false);
        int fill = current == filled ? 0 : Math.round(kind.opacity() * 2.55f) << 24 | rgb;
        PlacementPreviewBoxes.draw(new AABB(pos.getX() + inset, y, pos.getZ() + inset,
                        pos.getX() + 1 - inset, y + .002, pos.getZ() + 1 - inset),
                0xff000000 | rgb, fill);
        return true;
    }
    private static boolean canEmptyAt(net.minecraft.world.entity.player.Player player, ClientLevel world,
                                      BlockPos pos, Kind kind) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.canBeReplaced(kind.fluid())
                || state.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, world, pos, state, kind.fluid());
    }

    private static Prediction predict(Key key) {
        Map<BlockPos, BlockState> snapshot = new HashMap<>();
        Simulation before = new Simulation(key, snapshot);
        before.run(false);
        Simulation after = new Simulation(key, snapshot);
        after.run(true);
        Map<BlockPos, Cell> changes = new LinkedHashMap<>();
        after.cells.forEach((pos, cell) -> {
            if (pos.equals(key.pos()) || !cell.equals(before.cell(pos))) changes.put(pos, cell);
        });
        return new Prediction(Collections.unmodifiableMap(changes), after.truncated);
    }
    private enum Kind {
        WATER, LAVA;
        FlowingFluid fluid(){return this==WATER?Fluids.WATER:Fluids.LAVA;}
        BlockState block(){return (this==WATER?Blocks.WATER:Blocks.LAVA).defaultBlockState();}
        boolean enabled(){return (this==WATER?Configs.PLACEMENT_PREVIEW_WATER:Configs.PLACEMENT_PREVIEW_LAVA).getBooleanValue();}
        int radius(){return (this==WATER?Configs.PLACEMENT_PREVIEW_WATER_RADIUS:Configs.PLACEMENT_PREVIEW_LAVA_RADIUS).getIntegerValue();}
        int depth(){return (this==WATER?Configs.PLACEMENT_PREVIEW_WATER_DEPTH:Configs.PLACEMENT_PREVIEW_LAVA_DEPTH).getIntegerValue();}
        int opacity(){return (this==WATER?Configs.PLACEMENT_PREVIEW_WATER_OPACITY:Configs.PLACEMENT_PREVIEW_LAVA_OPACITY).getIntegerValue();}
        int color(boolean source,boolean falling){return this==WATER?(source?0x2196ff:falling?0x38a7de:0x64caff):(source?0xff591e:falling?0xff7d29:0xffa040);}
    }
    private static boolean valid(ClientLevel world,BlockPos p){return !world.isOutsideBuildHeight(p)&&world.hasChunkAt(p)&&world.getWorldBorder().isWithinBounds(p);}
    private static final class Simulation {
        final Key key;
        final Map<BlockPos, BlockState> states;
        final Map<BlockPos, Cell> cells = new LinkedHashMap<>();
        final Queue<BlockPos> queue = new ArrayDeque<>();
        final Set<BlockPos> pending = new HashSet<>();
        int work;
        boolean truncated;

        Simulation(Key key, Map<BlockPos, BlockState> states) {
            this.key = key;
            this.states = states;
        }

        BlockState actual(BlockPos pos) {
            return states.computeIfAbsent(pos, key.world()::getBlockState);
        }

        boolean inBounds(BlockPos pos) {
            return Math.abs(pos.getX() - key.pos().getX()) <= key.radius()
                    && Math.abs(pos.getZ() - key.pos().getZ()) <= key.radius()
                    && key.pos().getY() - pos.getY() <= key.depth() && pos.getY() <= key.pos().getY();
        }

        Cell cell(BlockPos pos) {
            Cell predicted = cells.get(pos);
            if (predicted != null || !valid(key.world(), pos)) return predicted;
            var state = actual(pos);
            var fluid = state.getFluidState();
            return sameKind(fluid) ? new Cell(fluid, state.getBlock() instanceof LiquidBlockContainer) : null;
        }

        boolean sameKind(FluidState fluid) {
            return key.kind().fluid().isSame(fluid.getType());
        }

        BlockState blockAt(BlockPos pos) {
            Cell changed = cells.get(pos);
            if (changed == null) return actual(pos);
            return changed.contained() ? actual(pos).setValue(BlockStateProperties.WATERLOGGED, true)
                    : changed.fluid().createLegacyBlock();
        }

        boolean wallOpen(BlockPos from, Direction direction, BlockState fromState, BlockState toState) {
            //#if MC >= 1.21.3
            return PlacementPreviewFluidInvoker.halfmasa$pass(direction, key.world(), from, fromState,
            //#else
            //$$ return ((PlacementPreviewFluidInvoker) key.kind().fluid()).halfmasa$pass(direction, key.world(), from, fromState,
            //#endif
                    from.relative(direction), toState);
        }

        boolean pass(BlockPos from, Direction direction) {
            if (++work > MAX_WORK) { truncated = true; return false; }
            BlockPos to = from.relative(direction);
            if (key.world().isOutsideBuildHeight(to) || !key.world().getWorldBorder().isWithinBounds(to)) return false;
            if (!key.world().hasChunkAt(to)) { truncated = true; return false; }
            BlockState target = blockAt(to);
            BlockState current = blockAt(from);
            if (current.getFluidState().isEmpty() && !(current.getBlock() instanceof LiquidBlockContainer)) {
                current = key.kind().block();
            }
            if (!wallOpen(from, direction, current, target)) return false;
            FluidState other = target.getFluidState();
            if (!other.isEmpty() && !sameKind(other)) { truncated = true; return false; }
            if (other.isSource()) return false;
            //#if MC >= 1.21.3
            if (!PlacementPreviewFluidInvoker.halfmasa$hold(key.world(), to, target, key.kind().fluid())) return false;
            //#else
            //$$ if (!((PlacementPreviewFluidInvoker) key.kind().fluid()).halfmasa$hold(key.world(), to, target, key.kind().fluid())) return false;
            //#endif
            if (target.getBlock() instanceof LiquidBlockContainer container
                    && (key.kind() != Kind.WATER || !target.hasProperty(BlockStateProperties.WATERLOGGED)
                    || !container.canPlaceLiquid(null, key.world(), to, target, key.kind().fluid()))) return false;
            if (!inBounds(to)) { truncated = true; return false; }
            return true;
        }

        void enqueue(BlockPos pos) {
            if (inBounds(pos) && pending.add(pos)) queue.add(pos);
        }

        void offer(BlockPos pos, FluidState fluid, boolean contained) {
            Cell old = cell(pos);
            if (old != null && (old.fluid().isSource() || !fluid.isSource()
                    && old.fluid().getAmount() >= fluid.getAmount()
                    && (!fluid.getValue(FlowingFluid.FALLING) || old.fluid().getValue(FlowingFluid.FALLING)))) return;
            if (!cells.containsKey(pos) && cells.size() >= MAX_CELLS) { truncated = true; return; }
            cells.put(pos, new Cell(fluid, contained));
            enqueue(pos);
        }

        FluidState incoming(BlockPos pos) {
            FlowingFluid type = key.kind().fluid();
            BlockState target = blockAt(pos);
            int sources = 0;
            int strongest = 0;
            for (var direction : SIDES) {
                var from = pos.relative(direction);
                Cell neighbor = cell(from);
                if (neighbor == null || !wallOpen(pos, direction, target, blockAt(from))) continue;
                if (neighbor.fluid().isSource()) sources++;
                strongest = Math.max(strongest, neighbor.fluid().getAmount());
            }
            if (key.sourceConversion() && sources >= 2) {
                var below = pos.below();
                Cell floor = cell(below);
                if (valid(key.world(), below) && (blockAt(below).isSolid() || floor != null && floor.fluid().isSource())) {
                    return type.getSource(false);
                }
            }
            var above = pos.above();
            if (cell(above) != null && wallOpen(pos, Direction.UP, target, blockAt(above))) return type.getFlowing(8, true);
            int amount = strongest - key.dropOff();
            return amount > 0 ? type.getFlowing(amount, false) : Fluids.EMPTY.defaultFluidState();
        }

        void spread(BlockPos pos) {
            Cell cell = cell(pos);
            if (cell == null) return;
            FluidState current = cell.fluid();
            if (pass(pos, Direction.DOWN)) {
                BlockPos below = pos.below();
                fill(below, incoming(below));
                int sources = 0;
                for (Direction side : SIDES) {
                    Cell neighbor = cell(pos.relative(side));
                    if (neighbor != null && neighbor.fluid().isSource()) sources++;
                }
                if (sources < 3) return;
            } else if (!current.isSource()) {
                Cell below = cell(pos.below());
                if (below != null && wallOpen(pos, Direction.DOWN, blockAt(pos), blockAt(pos.below()))) return;
            }
            int amount = current.getValue(FlowingFluid.FALLING) ? 7 : current.getAmount() - key.dropOff();
            if (amount <= 0) return;
            int best = Integer.MAX_VALUE;
            var scores = new EnumMap<Direction, Integer>(Direction.class);
            for (var direction : SIDES) {
                if (!pass(pos, direction)) continue;
                int score = slope(pos.relative(direction), direction.getOpposite());
                scores.put(direction, score);
                best = Math.min(best, score);
            }
            for (var score : scores.entrySet()) {
                if (score.getValue() == best) {
                    var next = pos.relative(score.getKey());
                    fill(next, incoming(next));
                }
            }
        }

        void fill(BlockPos pos, FluidState fluid) {
            if (fluid.isEmpty()) return;
            boolean contained = actual(pos).getBlock() instanceof LiquidBlockContainer;
            offer(pos, contained ? key.kind().fluid().getSource(false) : fluid, contained);
        }
        // Shortest horizontal route to a drop, using the current fluid slope-search limit.
        int slope(BlockPos start,Direction cameFrom){
            record Step(BlockPos p,int distance){};
            var q=new ArrayDeque<Step>();var seen=new HashSet<BlockPos>();seen.add(start);seen.add(start.relative(cameFrom));q.add(new Step(start,0));
            while(!q.isEmpty()&&work<MAX_WORK){var s=q.removeFirst();if(pass(s.p(),Direction.DOWN))return s.distance();if(s.distance()>=key.slopeDistance())continue;
                for(var d:SIDES){var n=s.p().relative(d);if(!seen.contains(n)&&pass(s.p(),d)){seen.add(n);q.addLast(new Step(n,s.distance()+1));}}}
            return Integer.MAX_VALUE;
        }
        void run(boolean place) {
            if (place) offer(key.pos(), key.kind().fluid().getSource(false), key.contained());
            else if (cell(key.pos()) != null) enqueue(key.pos());
            for (Direction direction : Direction.values()) {
                var neighbor = key.pos().relative(direction);
                if (cell(neighbor) != null) enqueue(neighbor);
            }
            while (!queue.isEmpty() && work < MAX_WORK) {
                var pos = queue.remove();
                pending.remove(pos);
                spread(pos);
            }
            truncated |= !queue.isEmpty();
        }
    }
}
//#endif

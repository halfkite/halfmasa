"""Same 27 visible items: original MiniHUD, optimized MiniHUD, and actual Jade 26.3.5."""
import json
import run_minihud_jade_preview_smoke as profile
import run_minihud_world_preview_smoke as world
import run_minihud_independent_smoke as independent
import run_minihud_preview_smoke as harness
import minihud_held_boundary_checks as boundaries

SOURCE = profile.SOURCE.replace('120', '600').replace('240', '1800').replace('>=30', '>=120')
SOURCE = SOURCE.replace('if(frame==1800){metric("optimized");', 'if(frame==1200)metric("optimized");if(frame==1800){metric("jade");')
SOURCE = SOURCE.replace('metric("jade");tests(ctx);', 'metric("jade");snownee.jade.Jade.config().general().setDisplayTooltip(false);tests(ctx);')
SOURCE = SOURCE.replace('if(frame==600)metric("original");', 'if(frame==600)metric("original");')
SOURCE = SOURCE.replace('Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(frame>=600);', 'Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(frame>=600);')
SOURCE = SOURCE.replace('public static int worldCalls,', 'public static boolean drawing;public static int drawnItems;public static long frameStart;public static int worldCalls,')
SOURCE = SOURCE.replace('List<Long> nanos=new ArrayList<>(),bytes=new ArrayList<>();', 'List<Long> nanos=new ArrayList<>(),bytes=new ArrayList<>(),frameTimes=new ArrayList<>();boolean sampleFrame;long jadeTick=Long.MIN_VALUE;')
SOURCE = SOURCE.replace('check(worldCalls-startCalls==600,', 'check(worldCalls-startCalls==(name.equals("jade")?0:600),')
SOURCE = SOURCE.replace('" gc_collections="+(gc()-startGc)', '" full_frame_p50_ns="+median(frameTimes)+" gc_collections="+(gc()-startGc)')
SOURCE = SOURCE.replace('nanos.clear();bytes.clear();', 'nanos.clear();bytes.clear();frameTimes.clear();')
SOURCE = SOURCE.replace('void metric(String name)', 'long median(List<Long> values){Collections.sort(values);return values.isEmpty()?0:values.get(values.size()/2);}void metric(String name)')
SOURCE = SOURCE.replace('long elapsed=System.nanoTime()-time,allocated=', 'long elapsed=System.nanoTime()-time,allocated=')
SOURCE = SOURCE.replace('time=System.nanoTime();call(ctx);', 'time=System.nanoTime();measuredCall(ctx);')
SOURCE = SOURCE.replace('if(frame%600>=120){nanos.add(elapsed);bytes.add(allocated);}', 'sampleFrame=frame%600>=120;if(sampleFrame){nanos.add(elapsed);bytes.add(allocated);check(drawnItems==27,"same 27 visible inventory items mode="+(frame<600?"original":frame<1200?"optimized":"jade"));}')
SOURCE = SOURCE.replace('void tests(GuiContext ctx)', '''void measuredCall(GuiContext ctx)throws Exception{
   drawnItems=0;drawing=true;try{if(frame<1200){snownee.jade.Jade.config().general().setDisplayTooltip(false);call(ctx);}else{MAIN.setBooleanValue(false);var jade=snownee.jade.JadeClient.tickHandler();snownee.jade.Jade.config().general().setDisplayTooltip(true);long tick=io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.tick();if(jadeTick!=tick||jade.rootElement==null){jade.tickClient();jadeTick=tick;}snownee.jade.overlay.OverlayRenderer.renderOverlay478757(ctx,net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks());if(frame==1320)System.out.println("PREVIEW_JADE_VISIBLE items="+drawnItems+" root="+(jade.rootElement!=null)+" normal_limit="+snownee.jade.Jade.config().plugin().getInt(snownee.jade.api.JadeIds.UNIVERSAL_ITEM_STORAGE_NORMAL_AMOUNT));}}finally{drawing=false;}
  }
  void tests(GuiContext ctx)''')
SOURCE = SOURCE.replace('var ctx=GuiContext.fromGuiGraphics(gui);MinecraftClientCompat', 'sampleFrame=false;var ctx=GuiContext.fromGuiGraphics(gui);MinecraftClientCompat')
SOURCE = SOURCE.replace('KEY.setValueFromString("LEFT_SHIFT,F9");', '''snownee.jade.Jade.config().general().setDisplayTooltip(false);snownee.jade.Jade.config().plugin().set(snownee.jade.api.JadeIds.UNIVERSAL_ITEM_STORAGE_NORMAL_AMOUNT,54);snownee.jade.Jade.config().plugin().set(snownee.jade.api.JadeIds.UNIVERSAL_ITEM_STORAGE_DETAILED_AMOUNT,54);KEY.setValueFromString("LEFT_SHIFT,F9");''')
# Unique names preserve all 25 shulker icons when Jade merges equivalent items.
SOURCE = SOURCE.replace('box.setItem(slot,stack);', 'stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Box "+slot));box.setItem(slot,stack);')
SOURCE = SOURCE.replace('public static void frame(Minecraft mc)', '''public static void beginFrame(){frameStart=System.nanoTime();}public static void endFrame(Minecraft mc){if(MinecraftClientCompat.getScreen(mc) instanceof MatrixScreen screen&&screen.sampleFrame)screen.frameTimes.add(System.nanoTime()-frameStart);}
 public static void frame(Minecraft mc)''')
SOURCE = SOURCE.replace('System.out.println("PREVIEW_INDEPENDENT_CHECK "+why);', 'if(!why.startsWith("world contents preserved frame=")&&!why.startsWith("same 27 visible inventory items"))System.out.println("PREVIEW_INDEPENDENT_CHECK "+why);')
SOURCE = boundaries.extend(SOURCE)

ITEM_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiGraphicsExtractor.class) public class VisibleItemProbeMixin {
 @Inject(method="item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",at=@At("HEAD")) private void item(LivingEntity actor,Level level,ItemStack stack,int x,int y,int seed,CallbackInfo ci){if(PreviewSmoke.drawing&&(stack.is(Items.SHULKER_BOX)||stack.is(Items.DIAMOND)||stack.is(Items.EMERALD)))PreviewSmoke.drawnItems++;}
}
'''
FRAME_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class TimingProbeMixin {
 @Inject(method="runTick",at=@At("HEAD")) private void start(CallbackInfo ci){PreviewSmoke.beginFrame();}
 @Inject(method="runTick",at=@At("TAIL")) private void end(CallbackInfo ci){PreviewSmoke.endFrame((Minecraft)(Object)this);}
}
'''


def main():
    jade = harness.ROOT / 'build/compat-test-mods/Jade-mc26.3-Fabric-26.3.5.jar'
    assert jade.exists(), jade
    harness.EXTRA_CLASSPATH = [jade]
    harness.EXTRA_MODS = [jade]
    harness.EXTRA_SOURCES = {'VisibleItemProbeMixin': ITEM_PROBE, 'TimingProbeMixin': FRAME_PROBE, 'HeldGridProbeMixin': boundaries.GRID_PROBE}
    world.SOURCE = SOURCE
    independent.WORLD_PROBE = profile.WORLD_PROBE
    independent.MAP_PROBE = profile.COMPARE_PROBE
    original_command = harness.startup.production_command
    def command(args, game, mod):
        options = game / 'options.txt'
        options.write_text(options.read_text().replace('maxFps:30', 'maxFps:240'))
        return original_command(args, game, mod)
    harness.startup.production_command = command
    folder = harness.ROOT / 'build/minihud-preview-tests'
    before = set(folder.iterdir())
    result = world.main()
    for directory in set(folder.iterdir()) - before:
        report = directory / 'report.json'
        if report.exists():
            data = json.loads(report.read_text(encoding='utf-8'))
            data['test_kind'] = 'Actual Jade 26.3.5 comparison: equal 27 icons, 600 frames per mode, 240 FPS test cap'
            data['jade'] = str(jade)
            data['measurement'] = 'Callback CPU and allocation plus full runTick duration, including 240 FPS pacing; 120 warmup frames per mode'
            report.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    return result


if __name__ == '__main__':
    raise SystemExit(main())

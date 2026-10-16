"""Render real MiniHUD previews in an isolated 26.3 client and compare switch-off/on work."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import zipfile
import sys
sys.stdout.reconfigure(encoding="utf-8")
import run_startup_smoke as startup
import run_entity_render_disable_smoke as entity_smoke
import port_smoke_compat as port_compat

ROOT = Path(__file__).resolve().parents[2]
EXTRA_SOURCES = {}
EXTRA_CLASSPATH = []
EXTRA_MODS = []
SOURCE = r'''
package io.github.halfmasa.previewsmoke;
import java.util.*;
import java.lang.management.ManagementFactory;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.block.entity.BlockEntityType;
import fi.dy.masa.malilib.render.*;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.data.tag.*;
import fi.dy.masa.malilib.util.nbt.*;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.data.EntityDataManager;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
public class PreviewSmoke implements ClientModInitializer {
 public static int stored,bundles,inventory,dataItems,checks,mapExtracts;public static byte mapColor;
 public static Container drawn;public static Set<Integer> disabled;public static InventoryOverlayType type;
 private static PreviewSmoke instance;private int stage;private PreviewScreen screen;
 public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);System.out.println("PREVIEW_CHECK "+why);}
 static void enabled(boolean value){Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(value);}
 static ItemStack container(Item item,int size){var stack=new ItemStack(item);var list=NonNullList.withSize(size,ItemStack.EMPTY);for(int i=0;i<size;i++)if(i%3!=1)list.set(i,new ItemStack(i%2==0?Items.DIAMOND:Items.EMERALD,i%64+1));stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(list));return stack;}
 static void render(GuiContext ctx,ItemStack stack){drawn=null;RenderHandler.getInstance().onRenderTooltipLast(ctx,stack,180,290);}
 static List<ItemStack> snapshot(Container c){var result=new ArrayList<ItemStack>();for(int i=0;i<c.getContainerSize();i++)result.add(c.getItem(i).copy());return result;}
 static void same(List<ItemStack> a,Container b,String why){check(b!=null&&a.size()==b.getContainerSize(),why+" size expected="+a.size()+" actual="+(b==null?-1:b.getContainerSize()));for(int i=0;i<a.size();i++)if(!ItemStack.matches(a.get(i),b.getItem(i)))throw new AssertionError(why+" slot="+i);check(true,why+" all slots and holes");}
 static void enderItem(Minecraft mc,Item item,int count)throws Exception{
  mc.getSingleplayerServer().submit(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().getEnderChestInventory().setItem(0,new ItemStack(item,count))).get(5,java.util.concurrent.TimeUnit.SECONDS);
  var local=mc.player.getEnderChestInventory();local.setItem(0,new ItemStack(item,count));
  try(var ni=NbtInventory.fromInventory(local)){var data=new CompoundData();data.put(NbtKeys.ENDER_ITEMS,ni.toDataList(mc.level.registryAccess()));EntityDataManager.getInstance().handleEntityData(mc.player.getId(),data);}
 }
 private void tick(Minecraft mc){try{
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
   check(Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.getBooleanValue(),"new option defaults enabled");check(Configs.EXTENSIONS.contains(Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION),"option appears in Extensions");
   enabled(false);new Configs().save();enabled(true);new Configs().load();check(!Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.getBooleanValue(),"saved disabled choice survives reload");enabled(true);
   fi.dy.masa.minihud.config.Configs.Generic.MAIN_RENDERING_TOGGLE.setBooleanValue(false);fi.dy.masa.minihud.config.Configs.Generic.SHULKER_BOX_PREVIEW.setBooleanValue(true);fi.dy.masa.minihud.config.Configs.Generic.SHULKER_DISPLAY_REQUIRE_SHIFT.setBooleanValue(false);fi.dy.masa.minihud.config.Configs.Generic.BUNDLE_PREVIEW.setBooleanValue(true);fi.dy.masa.minihud.config.Configs.Generic.BUNDLE_DISPLAY_REQUIRE_SHIFT.setBooleanValue(false);fi.dy.masa.minihud.config.Configs.Generic.SHULKER_DISPLAY_ENDER_CHEST.setBooleanValue(true);Configs.MAP_IN_SLOT.setBooleanValue(false);
   stage=1;mc.createWorldOpenFlows().createFreshLevel("preview-fixture",new LevelSettings("Preview Fixture",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;
  }
  if(stage==1&&mc.level!=null&&mc.player!=null&&mc.level.getGameTime()>40){stage=2;screen=new PreviewScreen();MinecraftClientCompat.setScreen(mc,screen);}
  if(stage==2&&screen.done){stage=3;System.out.println("PREVIEW_WORLD_PASS checks="+checks);}
  else if(stage==3){stage=4;net.minecraft.client.Screenshot.grab(mc,false);}
  else if(stage==4){stage=5;mc.stop();}
 }catch(Throwable e){throw new IllegalStateException("Preview smoke stage="+stage,e);}}
 static class PreviewScreen extends Screen {
  int frames;boolean done;ItemStack heavy=container(Items.SHULKER_BOX,81),finalPreview=container(Items.SHULKER_BOX,27);long beforeCalls,gcStart;List<Long> nanos=new ArrayList<>(),bytes=new ArrayList<>();
  final com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
  PreviewScreen(){super(Component.literal("MiniHUD preview integration"));}
  public boolean isPauseScreen(){return false;}
  public void extractRenderState(GuiGraphicsExtractor gui,int x,int y,float delta){try{
   GuiContext ctx=GuiContext.fromGuiGraphics(gui);if(done){render(ctx,finalPreview);return;}
   if(frames==0){enabled(false);beforeCalls=stored;gcStart=gcCount();}
   if(frames==100){metric("original");check(stored-beforeCalls==100,"disabled switch uses original parsing on every frame");enabled(true);beforeCalls=stored;gcStart=gcCount();}
   if(frames==200){metric("optimized");check(stored-beforeCalls==1,"MiniHUD hover reuses one complete snapshot across 100 frames");tests(ctx);done=true;render(ctx,container(Items.SHULKER_BOX,27));return;}
   long tid=Thread.currentThread().threadId(),a=bean.getThreadAllocatedBytes(tid),t=System.nanoTime();render(ctx,heavy);long elapsed=System.nanoTime()-t,allocated=bean.getThreadAllocatedBytes(tid)-a;
   if(frames%100==0)System.out.println("PREVIEW_COLD mode="+(frames==0?"original":"optimized")+" extract_cpu_ns="+elapsed+" allocation_bytes="+allocated);
   if(frames%100>=30){nanos.add(elapsed);bytes.add(allocated);}frames++;
  }catch(Throwable e){throw new IllegalStateException("Preview rendering frame="+frames,e);}}
  long gcCount(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();}
  void metric(String name){Collections.sort(nanos);Collections.sort(bytes);System.out.println("PREVIEW_METRIC mode="+name+" samples="+nanos.size()+" extract_cpu_p50_ns="+nanos.get(nanos.size()/2)+" extract_cpu_p99_ns="+nanos.get(nanos.size()-1)+" gc_collections="+(gcCount()-gcStart)+" allocation_p50_bytes="+bytes.get(bytes.size()/2));nanos.clear();bytes.clear();}
  void tests(GuiContext ctx)throws Exception {
   var mc=Minecraft.getInstance();
   for(Item item:List.of(Items.SHULKER_BOX,Items.FURNACE,Items.BREWING_STAND,Items.CRAFTER)){
    int size=item==Items.FURNACE?3:item==Items.BREWING_STAND?5:item==Items.CRAFTER?9:256;var stack=container(item,size);
    if(item==Items.CRAFTER){var tag=new CompoundTag();tag.putIntArray("disabled_slots",new int[]{1,4});stack.set(DataComponents.BLOCK_ENTITY_DATA,TypedEntityData.of(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft","crafter")),tag));}
    enabled(false);render(ctx,stack);var expected=snapshot(drawn);var expectedType=type;var expectedLocked=Set.copyOf(disabled);
    enabled(true);render(ctx,stack);same(expected,drawn,"original layout "+item);check(type==expectedType&&disabled.equals(expectedLocked),"special layout and crafter disabled slots retained "+item);
    int old=stored;render(ctx,stack);check(stored==old,"unchanged component cached "+item);
    var replacement=NonNullList.withSize(size,ItemStack.EMPTY);replacement.set(size-1,new ItemStack(Items.GOLD_INGOT,7));stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(replacement));render(ctx,stack);check(drawn.getItem(size-1).is(Items.GOLD_INGOT),"component replacement updates last slot immediately "+item);
    if(item==Items.CRAFTER){stack.get(DataComponents.BLOCK_ENTITY_DATA).getUnsafe().putIntArray("disabled_slots",new int[]{2});render(ctx,stack);check(disabled.equals(Set.of(2)),"in-place block data mutation invalidates crafter snapshot");}
   }
   var bundle=new ItemStack(Items.BUNDLE);bundle.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(new ItemStackTemplate(Items.DIAMOND,3),new ItemStackTemplate(Items.EMERALD,2))));
   enabled(false);render(ctx,bundle);var expected=snapshot(drawn);enabled(true);render(ctx,bundle);same(expected,drawn,"bundle original ordering");int count=bundles;render(ctx,bundle);check(bundles==count,"bundle snapshot reused");bundle.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(List.of(new ItemStackTemplate(Items.GOLD_INGOT,4))));render(ctx,bundle);check(drawn.getItem(0).is(Items.GOLD_INGOT),"bundle replacement updates immediately");
   fi.dy.masa.minihud.config.Configs.Generic.SHULKER_BOX_PREVIEW.setBooleanValue(false);render(ctx,heavy);check(drawn==null,"MiniHUD original preview switch still controls visibility");fi.dy.masa.minihud.config.Configs.Generic.SHULKER_BOX_PREVIEW.setBooleanValue(true);
   PreviewCache.clear();render(ctx,heavy);int old=stored;RenderUtils.renderShulkerBoxPreview(ctx,heavy,180,290,true);check(stored==old+1,"another mod calling MaLiLib directly remains outside optimization scope");check(!PreviewCache.active(),"render scope restored after callback");
   var local=mc.player.getEnderChestInventory();enderItem(mc,Items.DIAMOND,12);var ender=new ItemStack(Items.ENDER_CHEST);enabled(false);render(ctx,ender);check(drawn!=null,"original ender fallback available");var enderExpected=snapshot(drawn);enabled(true);render(ctx,ender);same(enderExpected,drawn,"ender fallback contents");int parsed=dataItems;render(ctx,ender);check(dataItems==parsed,"ender serialized content is parsed once");enderItem(mc,Items.EMERALD,9);render(ctx,ender);check(drawn.getItem(0).is(Items.EMERALD),"ender live inventory change updates immediately");
   // Mutable server data must invalidate even if its identity never changes.
   var source=new CompoundData();source.put("EnderItems",new ListData());int[] builds={0};PreviewCache.enter();try{PreviewCache.data("fixture",null,source,()->++builds[0]);PreviewCache.data("fixture",null,source,()->++builds[0]);source.putInt("added",1);PreviewCache.data("fixture",null,source,()->++builds[0]);check(builds[0]==2,"added mutable compound keys invalidate cache");source.remove("added");PreviewCache.data("fixture",null,source,()->++builds[0]);check(builds[0]==2,"empty compounds compare correctly and removed keys restore matching snapshot");}finally{PreviewCache.exit();}
   int[] refresh={0};var inv=new SimpleContainer(27);inv.setItem(0,new ItemStack(Items.DIAMOND));var context=new InventoryOverlayContext(InventoryOverlayType.GENERIC,inv,null,null,null,(data,world)->{refresh[0]++;return data;});
   var overlay=new InventoryOverlayScreen("minihud",context);overlay.init(900,600);var gui=new GuiGraphicsExtractor(mc,new net.minecraft.client.renderer.state.gui.GuiRenderState(),0,0);
   enabled(false);for(int i=0;i<40;i++)overlay.extractRenderState(gui,0,0,0);check(refresh[0]==10,"disabled world preview retains four-frame upstream cadence");enabled(true);refresh[0]=0;for(int i=0;i<40;i++)overlay.extractRenderState(gui,0,0,0);check(refresh[0]==1,"world preview refresh deduplicated within game tick");
   long revision=PreviewCache.revision();EntityDataManager.getInstance().handleEntityData(-12345,new CompoundData());check(PreviewCache.revision()>revision,"MiniHUD data reply invalidates refresh gate");for(int i=0;i<4;i++)overlay.extractRenderState(gui,0,0,0);check(refresh[0]==2,"new server data bypasses same-tick refresh deduplication");PreviewCache.getInstance().onClientTick(mc);for(int i=0;i<4;i++)overlay.extractRenderState(gui,0,0,0);check(refresh[0]==3,"next game tick permits refresh");
   var other=new InventoryOverlayScreen("fixture-other-mod",context);other.init(900,600);refresh[0]=0;for(int i=0;i<40;i++)other.extractRenderState(gui,0,0,0);check(refresh[0]==10,"other mods world preview cadence unchanged");
   fi.dy.masa.minihud.config.Configs.Generic.MAP_PREVIEW.setBooleanValue(true);fi.dy.masa.minihud.config.Configs.Generic.MAP_PREVIEW_REQUIRE_SHIFT.setBooleanValue(false);var mapId=new net.minecraft.world.level.saveddata.maps.MapId(876543);var mapData=net.minecraft.world.level.saveddata.maps.MapItemSavedData.createFresh(0,0,(byte)0,true,false,mc.level.dimension());mc.level.overrideMapData(mapId,mapData);var map=new ItemStack(Items.FILLED_MAP);map.set(DataComponents.MAP_ID,mapId);enabled(false);int maps=mapExtracts;render(ctx,map);check(mapExtracts==maps+1,"original map preview extracts once");enabled(true);maps=mapExtracts;mapData.setColor(0,0,(byte)34);render(ctx,map);check(mapExtracts==maps+1&&mapColor==34,"optimized mode keeps live map updates without duplicate extraction");
   enabled(false);render(ctx,heavy);old=stored;render(ctx,heavy);check(stored==old+1,"runtime disabling restores original path immediately");enabled(true);render(ctx,heavy);old=stored;PreviewCache.clear();render(ctx,heavy);check(stored==old+1,"lifecycle invalidation rebuilds rather than retaining old preview");
  }
 }
}
'''
PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import fi.dy.masa.malilib.util.InventoryUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=InventoryUtils.class,remap=false) public class ProbeMixin {
 @Inject(method="getStoredItems(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/core/NonNullList;",at=@At("HEAD")) private static void stored(CallbackInfoReturnable<?> ci){PreviewSmoke.stored++;}
 @Inject(method="getBundleItems(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/core/NonNullList;",at=@At("HEAD")) private static void bundle(CallbackInfoReturnable<?> ci){PreviewSmoke.bundles++;}
 @Inject(method="getAsInventory",at=@At("HEAD")) private static void inventory(CallbackInfoReturnable<?> ci){PreviewSmoke.inventory++;}
 @Inject(method="getDataItems(Lfi/dy/masa/malilib/util/data/tag/CompoundData;ILnet/minecraft/core/RegistryAccess;)Lnet/minecraft/core/NonNullList;",at=@At("HEAD")) private static void data(CallbackInfoReturnable<?> ci){PreviewSmoke.dataItems++;}
}
'''
DRAW_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import java.util.Set;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import fi.dy.masa.malilib.render.*;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=InventoryOverlay.class,remap=false) public class DrawProbeMixin {
 @Inject(method="renderInventoryStacks(Lfi/dy/masa/malilib/render/GuiContext;Lfi/dy/masa/malilib/render/InventoryOverlayType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;DD)V",at=@At("HEAD")) private static void drawn(GuiContext ctx,InventoryOverlayType type,Container inv,int x,int y,int row,int start,int max,Set<Integer> disabled,double mx,double my,CallbackInfo ci){PreviewSmoke.drawn=inv;PreviewSmoke.disabled=disabled;PreviewSmoke.type=type;}
}
'''


MAP_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.level.saveddata.maps.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MapRenderer.class) public class MapProbeMixin {
 @Inject(method="extractRenderState",at=@At("HEAD")) private void map(MapId id,MapItemSavedData data,MapRenderState state,CallbackInfo ci){PreviewSmoke.mapExtracts++;PreviewSmoke.mapColor=data.colors[0];}
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', required=True, type=Path)
    parser.add_argument('--minihud', type=Path, default=ROOT / 'build/compat-test-mods/minihud-fabric-26.3-0.41.1.jar')
    parser.add_argument('--timeout', type=int, default=300)
    parser.add_argument('--malilib', type=Path)
    parser.add_argument('--with-api', action='store_true')
    parser.add_argument('--version', default='26.3')
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    if args.java_home is None:
        args.java_home = Path('C:/Program Files/Java/jdk-25.0.3') if args.version.startswith('26.') else ROOT / 'build/toolchains/jdk-21.0.12.1+1'
    args.artifact = args.artifact.resolve()
    directory = ROOT / 'build/minihud-preview-tests' / datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    classes = directory / 'classes'
    classes.mkdir(parents=True)
    frame = entity_smoke.FRAME.replace('entitydisablesmoke', 'previewsmoke').replace('EntityDisableSmoke', 'PreviewSmoke')
    clipboard = entity_smoke.CLIPBOARD.replace('entitydisablesmoke', 'previewsmoke')
    native_cp = port_compat.classpath(args.version, args.artifact)
    named_minihud = port_compat.named_mod(args.version, args.java_home, args.minihud, native_cp)
    cp = os.pathsep.join(path for path in [str(named_minihud), str(args.malilib) if args.malilib else '', *map(str, EXTRA_CLASSPATH), native_cp] if path)
    sources = []
    for name, source in [('PreviewSmoke', SOURCE), ('FrameMixin', frame), ('ClipboardMixin', clipboard), ('ProbeMixin', PROBE), ('DrawProbeMixin', DRAW_PROBE), ('MapProbeMixin', MAP_PROBE), *EXTRA_SOURCES.items()]:
        path = directory / (name + '.java')
        path.write_text(port_compat.source(source, args.version, cp), encoding='utf-8')
        sources.append(str(path))
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Dfile.encoding=UTF-8', '-J-Duser.language=en', '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        diagnostic = result.stderr.decode('utf-8', errors='replace')
        (directory / 'compile.log').write_text(diagnostic, encoding='utf-8')
        print(diagnostic)
        return 1
    mod = directory / 'preview-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_preview_smoke', 'version': '1.0', 'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.previewsmoke.PreviewSmoke']}, 'mixins': ['preview-smoke.mixins.json']}))
        jar.writestr('preview-smoke.mixins.json', json.dumps({'required': True, 'package': 'io.github.halfmasa.previewsmoke.mixin', 'compatibilityLevel': 'JAVA_21' if args.version.startswith('1.21.') else 'JAVA_25', 'client': ['FrameMixin', 'ClipboardMixin', 'ProbeMixin', 'DrawProbeMixin', 'MapProbeMixin', *EXTRA_SOURCES], 'injectors': {'defaultRequire': 1}}))
    mod = port_compat.remap_fixture(args.version, args.java_home, mod, cp, directory)
    game = directory / 'game'
    (game / 'mods').mkdir(parents=True)
    (game / 'config').mkdir()
    (game / 'config/malilib.json').write_text('{}\n')
    (game / 'options.txt').write_text('onboardAccessibility:false\nenableVsync:false\nmaxFps:30\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:zh_cn\ntutorialStep:none\n')
    shutil.copy2(args.minihud, game / 'mods' / args.minihud.name)
    for extra_mod in EXTRA_MODS:
        shutil.copy2(extra_mod, game / 'mods' / extra_mod.name)
    if args.with_api:
        import re
        api_version = dict(re.findall(r"'([\d.]+)': '([^']+)'", (ROOT / 'common.gradle').read_text(encoding='utf-8')))[args.version]
        api = next(p for p in (Path.home() / f'.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/{api_version}').glob('*/*.jar') if not p.name.endswith(('-sources.jar', '-javadoc.jar')))
        shutil.copy2(api, game / 'mods' / api.name)
    command = startup.production_command(args, game, mod)
    if args.malilib:
        for installed in (game / 'mods').glob('*.jar'):
            with zipfile.ZipFile(installed) as jar:
                metadata = json.loads(jar.read('fabric.mod.json'))
            if metadata.get('id') == 'malilib':
                assert installed.resolve().parent == (game / 'mods').resolve()
                installed.write_bytes(args.malilib.read_bytes())
    command[1:1] = ['-Djava.awt.headless=true', '-Dsun.java2d.d3d=false', '-XX:ActiveProcessorCount=2', f'-XX:ErrorFile={directory / "jvm-crash.log"}']
    command[command.index('--width') + 1] = '1120'
    command[command.index('--height') + 1] = '720'
    log = directory / 'launcher.log'
    started = time.monotonic()
    timed_out = False
    with log.open('w', encoding='utf-8') as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            timed_out = True
            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
            code = process.wait(timeout=30)
    text = log.read_text(encoding='utf-8', errors='replace')
    passed = code == 0 and not timed_out and 'PREVIEW_WORLD_PASS' in text
    report = {'result': 'PASS' if passed else 'FAIL', 'game_version': args.version, 'artifact': str(args.artifact), 'minihud': str(args.minihud), 'malilib_override': str(args.malilib) if args.malilib else None, 'full_fabric_api': args.with_api, 'exit_code': code, 'timed_out': timed_out, 'duration_seconds': round(time.monotonic() - started, 1), 'log': str(log), 'checks_and_metrics': [line for line in text.splitlines() if 'PREVIEW_' in line], 'measurement': 'CPU render extraction and render-thread allocations; not end-to-end FPS', 'screenshots': [str(p) for p in (game / 'screenshots').glob('*.png')]}
    report_path = directory / 'report.json'
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('MINIHUD PREVIEW:', report['result'])
    print(report_path)
    if not passed:
        print('\n'.join(text.splitlines()[-75:]))
    return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())

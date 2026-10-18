"""Cross-version real-client regression for cancelled silent meals and Conflux 0.1.10+.

Conflux is optional: omit --conflux to test just the meal lifecycle without the mod.
"""
import argparse
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import zipfile
import port_smoke_compat as compat
import run_startup_smoke as startup

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''package io.github.halfmasa.compatfixsmoke;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.item.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.compat.*;
public class CompatFixSmoke implements ClientModInitializer {
 public static boolean hold, replay;
 public static int uses;
 public static final Queue<Runnable> slots=new ConcurrentLinkedQueue<>(), replies=new ConcurrentLinkedQueue<>();
 private static CompatFixSmoke instance;
 private static final AutoEatService service=AutoEatService.getInstance();
 private int stage,checks,frames;
 private volatile boolean ready;
 private long request;
 private Object map;
 public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);System.out.println("COMPAT_FIX_CHECK "+why);}
 private static Field field(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 private static Object get(Object obj,String name)throws Exception{return field(obj.getClass(),name).get(obj);}
 private static void set(Object obj,String name,Object value)throws Exception{field(obj.getClass(),name).set(obj,value);}
 private static String state()throws Exception{return get(service,"state").toString();}
 private static Method method(Class<?> type,String name)throws Exception{var m=Arrays.stream(type.getDeclaredMethods()).filter(v->v.getName().equals(name)).findFirst().orElseThrow();m.setAccessible(true);return m;}
 private void next(int value){stage=value;frames=0;System.out.println("COMPAT_FIX_STAGE "+stage);}
 private static void flush(Queue<Runnable> queue){replay=true;try{for(Runnable r;(r=queue.poll())!=null;)r.run();}finally{replay=false;}}
 private void tick(Minecraft mc){try{
  frames++;
  if(!hold){flush(replies);flush(slots);}
  if(stage==0){
   if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
   check(!Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.getBooleanValue(),"MiniHUD preview optimization defaults off");
   check(Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.getKeybind().getStringValue().isEmpty(),"optimization hotkey defaults unbound");
   next(1);
   mc.createWorldOpenFlows().createFreshLevel("compat-fixture",new LevelSettings("Compatibility Fixture",GameType.SURVIVAL,
    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
    new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;
  }
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  if(stage==1){
   if(mc.level.getGameTime()<30)return;
   MinecraftClientCompat.setScreen(mc,null);Configs.AUTO_EAT.setBooleanValue(false);
   Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);Configs.AUTO_EAT_SILENT.setBooleanValue(true);
   mc.player.getInventory().setSelectedSlot(3);mc.player.setNoGravity(true);
   mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    p.setNoGravity(true);p.getFoodData().setFoodLevel(14);p.getFoodData().setSaturation(0);
    var inv=p.getInventory();for(int i=0;i<inv.getContainerSize();i++)inv.setItem(i,ItemStack.EMPTY);
    inv.setSelectedSlot(3);inv.setItem(3,new ItemStack(Items.IRON_PICKAXE));inv.setItem(10,new ItemStack(Items.BREAD,8));
    p.inventoryMenu.broadcastChanges();ready=true;
   });next(2);return;
  }
  if(stage==2){
   if(!ready||mc.player.getFoodData().getFoodLevel()!=14||mc.player.getInventory().getItem(10).getCount()!=8)return;
   hold=true;Configs.AUTO_EAT.setBooleanValue(true);next(3);return;
  }
  if(stage==3){
   if(!state().equals("WAIT_SILENT"))return;
   check((Integer)get(service,"activeSlot")==10,"silent request tracks inventory slot");request=(Long)get(service,"silentRequest");
   service.onSettingsChanged();service.onClientTick(mc);
   check(state().equals("WAIT_SILENT")&&(Integer)get(service,"activeSlot")==10,"settings cancellation preserves context");
   mc.options.keyAttack.setDown(true);service.onClientTick(mc);mc.options.keyAttack.setDown(false);
   check((Integer)get(service,"activeSlot")==10,"manual cancellation preserves context");
   Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.HOTBAR);service.onClientTick(mc);
   check((Integer)get(service,"activeSlot")==10,"source switch preserves pending slot");
   Configs.AUTO_EAT.setBooleanValue(false);service.onClientTick(mc);set(service,"deadline",0L);service.onClientTick(mc);service.onClientTick(mc);
   check(state().equals("WAIT_SILENT")&&(Boolean)get(service,"silentSuppressed"),"disabled request reaches uncertain timeout");
   check((Long)get(service,"silentRequest")==request&&uses==0,"no duplicate request or ordinary use");next(4);return;
  }
  if(stage==4){
   flush(replies);service.onClientTick(mc);
   if(get(service,"silentResult")==null)return;
   check("OK".equals(get(service,"silentResult")),"disabled feature drains reply");
   check(state().equals("WAIT_SILENT")&&(Integer)get(service,"activeSlot")==10,"reply alone cannot settle missing inventory sync");
   hold=false;next(5);return;
  }
  if(stage==5){
   if(!state().equals("IDLE"))return;
   check((Integer)get(service,"activeSlot")==-1,"slot cleared after settlement");
   check(mc.player.getInventory().getItem(10).is(Items.BREAD)&&mc.player.getInventory().getItem(10).getCount()==7,"exactly one meal consumed");
   check(mc.player.getInventory().getSelectedSlot()==3&&uses==0,"manual selection preserved without use packet");
   if(!FabricLoader.getInstance().isModLoaded("confluxmap")){finish(mc);return;}
   check(ConfluxMapCapabilities.legacyTeleport(),"official Conflux dual lookup contract detected");
   check(!ConfluxMapCapabilities.event("teleportLandingResolve"),"release fixture uses legacy resolver");
   Configs.CONFLUX_MAP_EXTENSIONS.setBooleanValue(true);Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(true);
   var list=Class.forName("cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen").getConstructor().newInstance();
   MinecraftClientCompat.setScreen(mc,(Screen)list);
   check((Boolean)get(list,"halfmasa$active"),"combined native list adapter active");next(6);return;
  }
  if(stage==6){
   if(frames<5)return;
   Configs.CONFLUX_MAP_UNKNOWN_HEIGHT.setIntegerValue(128);Configs.CONFLUX_MAP_CLOSE_AFTER_TELEPORT.setBooleanValue(true);
   var type=Class.forName("cn.net.rms.confluxmap.mc.ui.screen.FullscreenMapScreen");
   var ctor=Arrays.stream(type.getConstructors()).filter(c->c.getParameterCount()==1).findFirst().orElseThrow();
   map=ctor.newInstance(new Object[]{null});MinecraftClientCompat.setScreen(mc,(Screen)map);
   var targetType=Class.forName("cn.net.rms.confluxmap.mc.ui.screen.FullscreenMapLocationMenu$Target");
   var lookup=Class.forName("cn.net.rms.confluxmap.core.store.ColumnStore$SurfaceLookup");
   var targetCtor=targetType.getDeclaredConstructor(int.class,lookup,int.class);targetCtor.setAccessible(true);
   var adapter=new ConfluxMapLegacyTeleport(type);
   var unknown=targetCtor.newInstance(50000,lookup.getField("UNKNOWN").get(null),50000);
   check(!adapter.needsFallback(map,unknown),"live unknown column retains native ground discovery");
   var target=targetCtor.newInstance(50000,lookup.getConstructor(boolean.class,OptionalInt.class).newInstance(true,OptionalInt.empty()),50000);
   check(adapter.needsFallback(map,target),"void column can use fallback Y");
   method(type,"locationMenuButtonSpecs").invoke(map,target,null,null,false);
   var action=Class.forName("cn.net.rms.confluxmap.mc.ui.screen.FullscreenMapLocationMenu$Action");
   Object teleport=Arrays.stream(action.getEnumConstants()).filter(a->a.toString().equals("TELEPORT")).findFirst().orElseThrow();
   method(type,"runLocationAction").invoke(map,teleport,target,null,null,map);
   check(MinecraftClientCompat.getScreen(mc)==null,"legacy map closes after teleport");
   check(get(get(map,"groundTeleport"),"pending")==null,"fallback leaves no deferred correction");next(7);return;
  }
  if(stage==7){
   if(Math.abs(mc.player.getX()-50000.5)>.1||Math.abs(mc.player.getZ()-50000.5)>.1)return;
   check(Math.abs(mc.player.getY()-128)<.1,"real server confirms configured landing Y");finish(mc);
  }
 }catch(Throwable error){throw new IllegalStateException("Compatibility fixes smoke stage="+stage,error);}}
 private void finish(Minecraft mc)throws Exception{
  check(!Configs.AUTO_EAT.getBooleanValue(),"feature remains disabled after settlement");
  service.onWorldExit();check(state().equals("IDLE")&&get(service,"actor")==null&&(Integer)get(service,"activeSlot")==-1,"world cleanup clears state and slot together");
  System.out.println("COMPAT_FIX_PASS checks="+checks);stage=99;mc.stop();
 }
}
'''
FRAME = '''package io.github.halfmasa.compatfixsmoke.mixin;
import io.github.halfmasa.compatfixsmoke.CompatFixSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class FrameMixin {@Inject(method="runTick",at=@At("TAIL")) private void frame(CallbackInfo ci){CompatFixSmoke.frame((Minecraft)(Object)this);}}
'''
SLOTS = '''package io.github.halfmasa.compatfixsmoke.mixin;
import io.github.halfmasa.compatfixsmoke.CompatFixSmoke;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class) public class SlotDelayMixin {
 @Inject(method="handleContainerSetSlot",at=@At("HEAD"),cancellable=true) private void slot(ClientboundContainerSetSlotPacket p,CallbackInfo ci){if(CompatFixSmoke.hold&&!CompatFixSmoke.replay){CompatFixSmoke.slots.add(()->((ClientPacketListener)(Object)this).handleContainerSetSlot(p));ci.cancel();}}
 @Inject(method="handleContainerContent",at=@At("HEAD"),cancellable=true) private void content(ClientboundContainerSetContentPacket p,CallbackInfo ci){if(CompatFixSmoke.hold&&!CompatFixSmoke.replay){CompatFixSmoke.slots.add(()->((ClientPacketListener)(Object)this).handleContainerContent(p));ci.cancel();}}
 PLAYER_SLOT_HANDLER
}
'''
PLAYER_SLOT = '''@Inject(method="handleSetPlayerInventory",at=@At("HEAD"),cancellable=true) private void inventory(ClientboundSetPlayerInventoryPacket p,CallbackInfo ci){if(CompatFixSmoke.hold&&!CompatFixSmoke.replay){CompatFixSmoke.slots.add(()->((ClientPacketListener)(Object)this).handleSetPlayerInventory(p));ci.cancel();}}'''
REPLY = '''package io.github.halfmasa.compatfixsmoke.mixin;
import io.github.halfmasa.compatfixsmoke.CompatFixSmoke;
import io.github.halfmasa.xaerobinding.feature.HalfMasaSupportClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=HalfMasaSupportClient.class,remap=false) public class ReplyDelayMixin {
 @Inject(method="result",at=@At("HEAD"),cancellable=true,remap=false) private static void result(long id,String status,CallbackInfo ci){if(CompatFixSmoke.hold&&!CompatFixSmoke.replay){CompatFixSmoke.replies.add(()->HalfMasaSupportClient.result(id,status));ci.cancel();}}
}
'''
PROBE = '''package io.github.halfmasa.compatfixsmoke.mixin;
import io.github.halfmasa.compatfixsmoke.CompatFixSmoke;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientCommonPacketListenerImpl.class) public class UseProbeMixin {@Inject(method="send",at=@At("HEAD")) private void send(Packet<?> p,CallbackInfo ci){if(p instanceof net.minecraft.network.protocol.game.ServerboundUseItemPacket)CompatFixSmoke.uses++;}}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', required=True)
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--conflux', type=Path)
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Java/jdk-25.0.3'))
    parser.add_argument('--timeout', type=int, default=480)
    parser.add_argument('--jvm-arg', action='append', default=[])
    parser.add_argument('--loader-version', help='Loader override for optional mods with a newer minimum')
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    directory = ROOT / 'build/compat-fixes-tests' / (args.version + '-' + datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
    classes = directory / 'classes'
    classes.mkdir(parents=True)
    cp = compat.classpath(args.version, args.artifact)
    sources = []
    slots = SLOTS.replace('PLAYER_SLOT_HANDLER', '' if args.version == '1.21.1' else PLAYER_SLOT)
    for name, source in [('CompatFixSmoke', SOURCE), ('FrameMixin', FRAME), ('SlotDelayMixin', slots), ('ReplyDelayMixin', REPLY), ('UseProbeMixin', PROBE)]:
        source = compat.source(source, args.version, cp).replace('Difficulty.PEACEFUL', 'Difficulty.NORMAL')
        if args.version.startswith('1.21.') and tuple(map(int, args.version.split('.'))) < (1, 21, 5):
            source = source.replace('.setSelectedSlot(3)', '.selected=3').replace('.getSelectedSlot()', '.selected')
        path = directory / (name + '.java')
        path.write_text(source, encoding='utf-8')
        sources.append(str(path))
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Duser.language=en', '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        diagnostic = result.stderr.decode('utf-8', errors='replace')
        (directory / 'compile.log').write_text(diagnostic, encoding='utf-8')
        print(diagnostic)
        return 1
    mod = directory / 'compat-fixes-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_compat_fixes_smoke', 'version': '1.0', 'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.compatfixsmoke.CompatFixSmoke']}, 'mixins': ['compat-fixes.mixins.json']}))
        jar.writestr('compat-fixes.mixins.json', json.dumps({'required': True, 'package': 'io.github.halfmasa.compatfixsmoke.mixin', 'compatibilityLevel': 'JAVA_21' if args.version.startswith('1.21.') else 'JAVA_25', 'client': ['FrameMixin', 'SlotDelayMixin', 'ReplyDelayMixin', 'UseProbeMixin'], 'injectors': {'defaultRequire': 1}}))
    mod = compat.remap_fixture(args.version, args.java_home, mod, cp, directory)
    game = directory / 'game'
    (game / 'mods').mkdir(parents=True)
    (game / 'config').mkdir()
    (game / 'config/malilib.json').write_text('{}\n', encoding='utf-8')
    (game / 'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nnarrator:0\nlang:en_us\nguiScale:1\nenableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\n', encoding='utf-8')
    api = next(p for p in (Path.home() / f'.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/{compat.api_version(args.version)}').glob('*/*.jar') if not p.name.endswith(('-sources.jar', '-javadoc.jar')))
    shutil.copy2(api, game / 'mods' / api.name)
    if args.conflux:
        shutil.copy2(args.conflux, game / 'mods' / args.conflux.name)
        (game / 'config/confluxmap').mkdir()
        (game / 'config/confluxmap/config.json').write_text(json.dumps({'predictionEnabled': False, 'updateCheckEnabled': False, 'surveyReminderDismissed': True, 'minimapEnabled': False}), encoding='utf-8')
    command = startup.production_command(args, game, mod)
    command[1:1] = args.jvm_arg
    log_path = directory / 'launcher.log'
    start = time.monotonic()
    timed_out = False
    with log_path.open('w', encoding='utf-8') as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            timed_out = True
            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
            code = process.wait(timeout=30)
    log = log_path.read_text(encoding='utf-8', errors='replace')
    passed = code == 0 and not timed_out and 'COMPAT_FIX_PASS' in log
    report = {'version': args.version, 'result': 'PASS' if passed else 'FAIL', 'artifact': str(args.artifact), 'sha256': hashlib.sha256(args.artifact.read_bytes()).hexdigest(), 'conflux': str(args.conflux.resolve()) if args.conflux else None, 'jvm_args': args.jvm_arg, 'duration_seconds': round(time.monotonic() - start, 1), 'exit_code': code, 'timed_out': timed_out, 'log': str(log_path), 'checks': [line for line in log.splitlines() if 'COMPAT_FIX_' in line]}
    (directory / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('COMPAT FIXES:', report['result'], directory / 'report.json', flush=True)
    if not passed:
        print('\n'.join(log.splitlines()[-35:]), flush=True)
    return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())

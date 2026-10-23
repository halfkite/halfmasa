"""Real-client checks for silent meal duration and 26.3 temporary waypoint coordinates.

Uses the compatibility runner's isolated client, packet probes and report format.
Run against the original artifact first; timing failures are collected so waypoint
checks still complete. No files in an existing game instance are changed.
"""
import argparse
import json
from pathlib import Path
import shutil
import sys
import zipfile
import run_compat_fixes_smoke as runner


runner.SOURCE = r'''package io.github.halfmasa.compatfixsmoke;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.item.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
public class CompatFixSmoke implements ClientModInitializer {
 public static boolean hold,replay;
 public static int uses;
 public static final Queue<Runnable> slots=new ConcurrentLinkedQueue<>(),replies=new ConcurrentLinkedQueue<>();
 private static CompatFixSmoke instance;
 private static final AutoEatService service=AutoEatService.getInstance();
 private int stage,checks,failures,point,requests;
 private volatile boolean ready;
 private long since,beginTick;
 private long pendingRequest;
 private int duration;
 private final double[] heights={-60.0,-32.625,128.375,192.0};
 private double expectedX,expectedZ;
 public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private void check(boolean ok,String why){checks++;System.out.println("COMPAT_FIX_"+(ok?"CHECK ":"FAIL ")+why);if(!ok)failures++;}
 private static Object get(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 private static Object invoke(Object o,String name)throws Exception{var m=o.getClass().getMethod(name);m.setAccessible(true);return m.invoke(o);}
 private static String state()throws Exception{return get(service,"state").toString();}
 private static void set(Object o,String name,Object value)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
 private static void flush(Queue<Runnable> queue){replay=true;try{for(Runnable r;(r=queue.poll())!=null;)r.run();}finally{replay=false;}}
 private void next(Minecraft mc,int s){stage=s;since=mc.level==null?0:mc.level.getGameTime();System.out.println("COMPAT_FIX_STAGE "+s);}
 public static void begin(Minecraft mc,int slot){if(instance!=null){instance.beginTick=mc.level.getGameTime();instance.duration=mc.player.getInventory().getItem(slot).getUseDuration(mc.player);}}
 public static void request(){if(instance!=null){var t=instance;long elapsed=Minecraft.getInstance().level.getGameTime()-t.beginTick;t.requests++;t.check(elapsed>=t.duration,"silent request waits food duration: elapsed="+elapsed+" required="+t.duration);}}
 private static Object waypointApi()throws Exception{
  var api=Class.forName("cn.net.rms.confluxmap.api.ConfluxMapApi");
  var root=((Optional<?>)api.getMethod("instance").invoke(null)).orElse(null);
  return root==null?null:api.getMethod("waypoints").invoke(root);
 }
 private static List<?> points()throws Exception{
  var api=waypointApi();return api==null?List.of():(List<?>)Class.forName("cn.net.rms.confluxmap.api.WaypointApi").getMethod("list").invoke(api);
 }
 private void place(Minecraft mc){
  if(point>0)mc.player.setPos(123.25+point,heights[point],-45.75-point);
  expectedX=mc.player.getX();expectedZ=mc.player.getZ();
  System.out.println("COMPAT_FIX_POSITION before_key x="+mc.player.getX()+" y="+mc.player.getY()+" z="+mc.player.getZ());
  var key=Configs.CONFLUX_MAP_TEMPORARY_WAYPOINT.getKeybind();key.setValueFromString("KP_PLUS");
  fi.dy.masa.malilib.event.InputEventHandler.getKeybindManager().updateUsedKeys();
  int code=key.getKeys().getFirst();var event=new net.minecraft.client.input.KeyEvent(code,code,0);
  var input=(fi.dy.masa.malilib.event.InputEventHandler)fi.dy.masa.malilib.event.InputEventHandler.getInputManager();
  // 0.30.2 moved state preprocessing to the native input hook; 0.30.1 also does it internally.
  fi.dy.masa.malilib.hotkeys.KeybindMulti.onKeyInputPre(event,1);
  check(input.onKeyInput(event,1,mc),"KP_PLUS press handled through MaLiLib input");
  fi.dy.masa.malilib.hotkeys.KeybindMulti.onKeyInputPre(event,0);
  input.onKeyInput(event,0,mc);
  // Change position before the queued API operation: the press-time snapshot must win.
  mc.player.setPos(140,heights[point]+7,40);
  next(mc,3);
 }
 private void food(Minecraft mc,Item item,int count,int target){
  Configs.AUTO_EAT.setBooleanValue(false);Configs.AUTO_EAT_TARGET_HUNGER.setIntegerValue(target);ready=false;
  mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
   p.setNoGravity(true);p.getFoodData().setFoodLevel(14);p.getFoodData().setSaturation(0);
   var inv=p.getInventory();for(int i=0;i<inv.getContainerSize();i++)inv.setItem(i,ItemStack.EMPTY);
   inv.setSelectedSlot(3);inv.setItem(3,new ItemStack(Items.IRON_PICKAXE));inv.setItem(10,new ItemStack(item,count));
   p.inventoryMenu.broadcastChanges();ready=true;});
 }
 private void tick(Minecraft mc){try{
  if(stage==0){
   var screen=MinecraftClientCompat.getScreen(mc);
   boolean readyScreen=screen instanceof TitleScreen||screen!=null&&screen.getClass().getSimpleName().equals("McdrSetupScreen");
   if(!readyScreen||mc.gui.overlay()!=null)return;
   Configs.AUTO_EAT.setBooleanValue(false);next(mc,1);
   mc.createWorldOpenFlows().createFreshLevel("food-waypoint-fixture",new LevelSettings("Food and Waypoint Fixture",GameType.SURVIVAL,
    new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),true,WorldDataConfiguration.DEFAULT),
    new WorldOptions(123L,false,false),r->r.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
      .getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),MinecraftClientCompat.getScreen(mc));return;
  }
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  if(mc.level.getGameTime()-since>800)throw new AssertionError("stage timed out "+stage);
  if(stage==1){
   if(mc.level.getGameTime()<35||!mc.player.onGround())return;
   if(Boolean.getBoolean("halfmasa.foodwaypoint.foodOnly")){
    MinecraftClientCompat.setScreen(mc,null);mc.player.setNoGravity(true);
    mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setNoGravity(true));
    Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);Configs.AUTO_EAT_SILENT.setBooleanValue(true);
    Configs.AUTO_EAT_WHITELIST.setStrings(List.of("minecraft:bread","minecraft:dried_kelp"));
    food(mc,Items.BREAD,4,20);next(mc,10);return;
   }
   if(waypointApi()==null)return;
   check(mc.player.getY()==-60.0,"default superflat standing height is -60");
   check(mc.level.getBlockState(mc.player.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK),"default superflat grass directly under player");
   MinecraftClientCompat.setScreen(mc,null);mc.player.setNoGravity(true);
   mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setNoGravity(true));
   Configs.CONFLUX_MAP_EXTENSIONS.setBooleanValue(true);place(mc);return;
  }
  if(stage==3){
   Object saved=null;
   for(Object wp:points())if(String.valueOf(invoke(wp,"name")).endsWith(" "+(point+1)+"\u2063"))saved=wp;
   if(saved==null)return;
   double x=((Number)invoke(saved,"x")).doubleValue(),y=((Number)invoke(saved,"y")).doubleValue(),z=((Number)invoke(saved,"z")).doubleValue();
   System.out.println("COMPAT_FIX_POSITION saved x="+x+" y="+y+" z="+z);
   check(x==expectedX&&y==heights[point]&&z==expectedZ,"API preserves press-time XYZ for height "+heights[point]);
   var app=Class.forName("cn.net.rms.confluxmap.ConfluxMapClient").getMethod("get").invoke(null);
   var store=invoke(invoke(app,"waypointService"),"current");
   for(Object wp:(List<?>)invoke(store,"list"))if(wp.getClass().getField("id").get(wp).equals(invoke(saved,"id"))){
    check(wp.getClass().getField("y").getDouble(wp)==heights[point],"native waypoint store preserves exact Y");
    var type=Class.forName("cn.net.rms.confluxmap.mc.ui.screen.WaypointEditScreen");
    var edit=(Screen)type.getMethod("forEdit",Screen.class,wp.getClass()).invoke(null,null,wp);
    MinecraftClientCompat.setScreen(mc,edit);
    String text=((net.minecraft.client.gui.components.EditBox)get(edit,"yField")).getValue();
    check(Double.parseDouble(text)==heights[point],"native edit field displays exact Y: "+text);
    MinecraftClientCompat.setScreen(mc,null);
   }
   if(++point<heights.length){place(mc);return;}
   Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);Configs.AUTO_EAT_SILENT.setBooleanValue(true);
   Configs.AUTO_EAT_WHITELIST.setStrings(List.of("minecraft:bread","minecraft:dried_kelp"));
   food(mc,Items.BREAD,4,20);next(mc,10);return;
  }
  if(stage==10||stage==20||stage==30||stage==40){
   if(!ready||mc.player.getFoodData().getFoodLevel()!=14||mc.player.getInventory().getItem(10).getCount()!=4)return;
   mc.player.getInventory().setSelectedSlot(3);
   if(stage==40)hold=true;
   Configs.AUTO_EAT.setBooleanValue(true);next(mc,stage+1);return;
  }
  if(stage==11||stage==21){
   if(mc.player.isUsingItem())throw new AssertionError("silent meal started item use");
   if(mc.player.getFoodData().getFoodLevel()<(stage==11?20:15)||!state().equals("IDLE"))return;
   check(true,"silent meal does not start item use");
   Configs.AUTO_EAT.setBooleanValue(false);
   check(mc.player.getInventory().getSelectedSlot()==3,"silent meal preserves selected tool");
   if(stage==11){food(mc,Items.DRIED_KELP,4,15);next(mc,20);return;}
   if(Boolean.getBoolean("halfmasa.foodwaypoint.baseline")){finish(mc);return;}
   food(mc,Items.BREAD,4,20);next(mc,30);return;
  }
  if(stage==31){
   if(!state().equals("WAIT_SILENT_USE"))return;
   Configs.AUTO_EAT.setBooleanValue(false);service.onSettingsChanged();uses=requests;next(mc,32);return;
  }
  if(stage==32){
   if(mc.level.getGameTime()-since<45)return;
   check(requests==uses,"cancel before duration sends no consume request");
   check(mc.player.getInventory().getItem(10).getCount()==4&&mc.player.getFoodData().getFoodLevel()==14,"cancelled wait consumes no food");
   check(state().equals("IDLE"),"cancelled wait leaves no active task");
   food(mc,Items.BREAD,4,20);next(mc,40);return;
  }
  if(stage==41){
   if(!state().equals("WAIT_SILENT"))return;
   pendingRequest=(Long)get(service,"silentRequest");uses=requests;
   check((Integer)get(service,"activeSlot")==10,"sent request owns valid food slot");
   service.onSettingsChanged();service.onClientTick(mc);
   check(state().equals("WAIT_SILENT")&&(Integer)get(service,"activeSlot")==10,"settings interruption preserves sent request");
   mc.options.keyAttack.setDown(true);service.onClientTick(mc);mc.options.keyAttack.setDown(false);
   check((Integer)get(service,"activeSlot")==10,"manual interruption preserves sent request");
   Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.HOTBAR);service.onClientTick(mc);
   check((Integer)get(service,"activeSlot")==10,"source change preserves sent request");
   Configs.AUTO_EAT.setBooleanValue(false);service.onClientTick(mc);set(service,"deadline",0L);service.onClientTick(mc);
   check(state().equals("WAIT_SILENT")&&(Boolean)get(service,"silentSuppressed"),"disabled sent request remains pending on timeout");
   check((Long)get(service,"silentRequest")==pendingRequest&&requests==uses,"interrupted request is not duplicated");next(mc,42);return;
  }
  if(stage==42){
   flush(replies);service.onClientTick(mc);if(get(service,"silentResult")==null)return;
   check("OK".equals(get(service,"silentResult"))&&state().equals("WAIT_SILENT"),"reply alone waits for inventory confirmation");
   hold=false;flush(slots);next(mc,43);return;
  }
  if(stage==43){
   if(!state().equals("IDLE")||mc.player.getFoodData().getFoodLevel()!=19)return;
   check(mc.player.getInventory().getItem(10).getCount()==3&&mc.player.getFoodData().getFoodLevel()==19,"interrupted sent request settles one meal");
   check((Integer)get(service,"activeSlot")==-1&&mc.player.getInventory().getSelectedSlot()==3,"settlement clears slot and preserves selection");
   service.onWorldExit();check(state().equals("IDLE")&&get(service,"actor")==null,"world cleanup clears prepared and sent state");finish(mc);
  }
 }catch(Throwable e){throw new IllegalStateException("Food/waypoint smoke stage="+stage,e);}}
 private void finish(Minecraft mc){
  System.out.println("COMPAT_FIX_RESULT checks="+checks+" failures="+failures);
  if(failures>0)throw new AssertionError("Food/waypoint regressions: "+failures);
  System.out.println("COMPAT_FIX_PASS checks="+checks);stage=99;mc.stop();
 }
}
'''

runner.PROBE = r'''package io.github.halfmasa.compatfixsmoke.mixin;
import io.github.halfmasa.compatfixsmoke.CompatFixSmoke;
import io.github.halfmasa.xaerobinding.feature.AutoEatService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AutoEatService.class,remap=false) public class UseProbeMixin {
 @Inject(method="beginEating",at=@At("HEAD"),remap=false) private void begin(Minecraft mc,int slot,CallbackInfo ci){CompatFixSmoke.begin(mc,slot);}
}
'''

runner.REPLY = r'''package io.github.halfmasa.compatfixsmoke.mixin;
import io.github.halfmasa.compatfixsmoke.CompatFixSmoke;
import io.github.halfmasa.xaerobinding.feature.HalfMasaSupportClient;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=HalfMasaSupportClient.class,remap=false) public class ReplyDelayMixin {
 @Inject(method="eat",at=@At("HEAD"),remap=false) private static void request(int slot,ItemStack food,int target,boolean combat,CallbackInfoReturnable<Long> ci){CompatFixSmoke.request();}
 @Inject(method="result",at=@At("HEAD"),cancellable=true,remap=false) private static void result(long id,String status,CallbackInfo ci){if(CompatFixSmoke.hold&&!CompatFixSmoke.replay){CompatFixSmoke.replies.add(()->HalfMasaSupportClient.result(id,status));ci.cancel();}}
}
'''

if __name__ == '__main__':
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument('--mods-dir', type=Path, help='Optional installed mod set, copied into the isolated test directory')
    parser.add_argument('--exclude-mod', action='append', default=[], help='Explicitly exclude an incompatible mod ID from that set')
    extra, remaining = parser.parse_known_args()
    sys.argv = [sys.argv[0], *remaining]
    if extra.mods_dir:
        production = runner.startup.production_command

        def with_mod_set(args, game, mod):
            command = production(args, game, mod)
            added = []
            for jar in sorted(extra.mods_dir.glob('*.jar')):
                with zipfile.ZipFile(jar) as archive:
                    if 'fabric.mod.json' not in archive.namelist():
                        continue
                    metadata = json.loads(archive.read('fabric.mod.json'), strict=False)
                if metadata['id'] in {'halfmasa', 'confluxmap', *extra.exclude_mod}:
                    continue
                if metadata['id'] in {'malilib', 'fabric-api'}:
                    for previous in (game / 'mods').glob('*.jar'):
                        with zipfile.ZipFile(previous) as archive:
                            old = json.loads(archive.read('fabric.mod.json'), strict=False)
                        if old['id'] == metadata['id']:
                            previous.unlink()
                shutil.copy2(jar, game / 'mods' / jar.name)
                added.append({'id': metadata['id'], 'version': metadata['version']})
            (game.parent / 'additional-mods.json').write_text(json.dumps(added, indent=2), encoding='utf-8')
            (game.parent / 'excluded-mods.json').write_text(json.dumps(extra.exclude_mod), encoding='utf-8')
            print('ADDITIONAL MODS:', len(added), flush=True)
            return command

        runner.startup.production_command = with_mod_set
    raise SystemExit(runner.main())

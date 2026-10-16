"""Create an isolated vanilla test world and exercise the installed timer and render Mixins."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import zipfile

import run_startup_smoke as startup

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''
package io.github.halfmasa.despawnsmoke;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import fi.dy.masa.malilib.interfaces.IDataSyncer;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import net.minecraft.core.UUIDUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.feature.*;
public class DespawnSmoke implements ClientModInitializer {
 private int stage;
 private int waited;
 private volatile List<Integer> ids;
 private static void check(boolean v,String reason){if(!v)throw new AssertionError(reason);}
 @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
 private void tick(Minecraft mc){
  try{
   if(stage==0){
    if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
    Configs.ITEM_RENDER_AGGREGATION.setBooleanValue(false);Configs.ENTITY_GLOW_HIGHLIGHT.setBooleanValue(false);
    Configs.ITEM_DESPAWN_TIMER.setBooleanValue(true);Configs.ITEM_DESPAWN_SOURCE.setOptionListValue(ItemDespawnDataSource.ESTIMATE);
    stage=1;
    mc.createWorldOpenFlows().createFreshLevel("despawn-fixture",new LevelSettings("Despawn Fixture",GameType.CREATIVE,
      new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
      new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));
    return;
   }
   if(stage==1){
    if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
    if(++waited<60)return;waited=0;
    var ready=ItemDespawnServuxClient.class.getDeclaredField("ready");ready.setAccessible(true);
    check(ready.getBoolean(null)!=FabricLoader.getInstance().isModLoaded("minihud"),"incorrect Servux channel ownership");
    MinecraftClientCompat.setScreen(mc,null);mc.player.setYRot(180);mc.player.setXRot(20);
    var server=mc.getSingleplayerServer();stage=2;
    server.execute(()->{
     var player=server.getPlayerList().getPlayers().getFirst();var level=player.level();
     var finite=new ItemEntity(level,player.getX()-1,player.getY()+0.3,player.getZ()-4,new ItemStack(Items.DIAMOND),0,0,0);
     var infinite=new ItemEntity(level,player.getX(),player.getY()+0.3,player.getZ()-4,new ItemStack(Items.EMERALD),0,0,0);
     var extended=new ItemEntity(level,player.getX()+1,player.getY()+0.3,player.getZ()-4,new ItemStack(Items.GOLD_INGOT),0,0,0);
     infinite.setUnlimitedLifetime();extended.setExtendedLifetime();
     for(var item:List.of(finite,infinite,extended)){item.setNeverPickUp();item.setNoGravity(true);level.addFreshEntity(item);}
     ids=List.of(finite.getId(),infinite.getId(),extended.getId());
    });return;
   }
   if(ids==null||mc.level==null||mc.player==null)return;
   var items=ids.stream().map(id->mc.level.getEntity(id)).toList();
   if(items.stream().anyMatch(e->!(e instanceof ItemEntity)))return;
   var service=ItemDespawnTimerService.getInstance();var finite=(ItemEntity)items.get(0);
   double dx=finite.getX()-mc.player.getX(),dy=finite.getY()-mc.player.getEyeY(),dz=finite.getZ()-mc.player.getZ();
   mc.player.setYRot((float)Math.toDegrees(Math.atan2(dz,dx))-90);
   mc.player.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
   if(++waited<40)return;waited=0;
   if(stage==2){
    check(service.labelFor(finite)!=null&&service.labelFor(finite).getString().startsWith("≈"),"independent estimate missing");
    Configs.ITEM_DESPAWN_SOURCE.setOptionListValue(ItemDespawnDataSource.AUTO);stage=3;return;
   }
   if(stage==3){
    check(service.labelFor(finite)!=null&&!service.labelFor(finite).getString().startsWith("≈")&&!service.labelFor(finite).getString().equals("--:--"),"local server Age missing");
    check(service.labelFor((ItemEntity)items.get(1)).getString().equals("∞"),"persistent sample missing");
    check(service.labelFor((ItemEntity)items.get(2)).getString().startsWith("09:"),"extended lifetime missing");
    var state=mc.getEntityRenderDispatcher().extractEntity(finite,0);
    check(((ItemDespawnLabelState)state).halfmasa$getDespawnLabel()!=null,"render extraction dropped timer");
    check(((ItemDespawnLabelState)state).halfmasa$getDespawnLabel().throughWalls(),"through-wall snapshot missing");
    Configs.ITEM_DESPAWN_THROUGH_WALLS.setBooleanValue(false);
    state=mc.getEntityRenderDispatcher().extractEntity(finite,0);
    check(!((ItemDespawnLabelState)state).halfmasa$getDespawnLabel().throughWalls(),"through-wall toggle did not update snapshot");
    Configs.ITEM_DESPAWN_THROUGH_WALLS.setBooleanValue(true);
    System.out.println("DESPAWN_CAMERA player="+mc.player.position()+" item="+finite.position()+" camera="+mc.gameRenderer.mainCamera().position()+" forward="+mc.gameRenderer.mainCamera().forwardVector());
    net.minecraft.client.Screenshot.grab(mc,false);
    Configs.ITEM_DESPAWN_SOURCE.setOptionListValue(ItemDespawnDataSource.SERVUX);stage=4;return;
   }
   if(stage==4){
    check(service.labelFor(finite).getString().equals("--:--"),"strict mode consumed local data without Servux");
    if(FabricLoader.getInstance().isModLoaded("minihud")){
     var generic=Class.forName("fi.dy.masa.minihud.config.Configs$Generic");
     Object sync=generic.getField("ENTITY_DATA_SYNC").get(null);sync.getClass().getMethod("setBooleanValue",boolean.class).invoke(sync,true);
     var managerClass=Class.forName("fi.dy.masa.minihud.data.EntityDataManager");
     var manager=(IDataSyncer)managerClass.getMethod("getInstance").invoke(null);
     managerClass.getMethod("setIsServuxServer").invoke(manager);
     manager.getCache().addToCache(finite.getId(),finite,new CompoundData().putShort("Age",(short)5900).putCodec("UUID",UUIDUtil.CODEC,finite.getUUID()));
     var sample=ItemDespawnDataBridge.pollMiniHud(finite);
     check(sample!=null&&sample.age()==5900,"MiniHUD public cache adapter failed");
     service.onClientTick(mc);
     check(service.labelFor(finite).getString().equals("00:05"),"timer did not consume MiniHUD cached Age");
     System.out.println("DESPAWN_MINIHUD_CACHE_PASS");
    }
    Configs.ITEM_DESPAWN_TIMER.setBooleanValue(false);check(service.labelFor(finite)==null,"disabled timer remains visible");
    Configs.ITEM_DESPAWN_TIMER.setBooleanValue(true);Configs.ITEM_DESPAWN_SOURCE.setOptionListValue(ItemDespawnDataSource.AUTO);stage=5;return;
   }
   if(stage==5){
    check(service.labelFor(finite)!=null&&!service.labelFor(finite).getString().startsWith("≈"),"timer did not recover after toggling");
    System.out.println("DESPAWN_WORLD_PASS estimate=true local_age=true persistent=true extended=true render_snapshot=true through_wall_toggle=true strict_source=true disable_reenable=true");
    stage=6;mc.stop();
   }
  }catch(Throwable failure){throw new IllegalStateException("Item despawn world smoke failed stage="+stage,failure);}
 }
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--extra-mod", action="append", type=Path, default=[])
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    args.version = "26.3"
    directory = ROOT / "build/despawn-world-tests" / datetime.now(timezone(timedelta(hours=8))).strftime("%Y%m%d-%H%M%S")
    directory.mkdir(parents=True)
    source = directory / "DespawnSmoke.java"
    source.write_text(SOURCE, encoding="utf-8")
    classes = directory / "classes"
    classes.mkdir()
    cp = str(ROOT / "versions/26.3/build/classes/java/main") + os.pathsep + (ROOT / "build/port-classpath-26.3.txt").read_text(encoding="utf-8")
    subprocess.run([str(args.java_home / "bin/javac.exe"), "-proc:none", "-encoding", "UTF-8", "-cp", cp, "-d", str(classes), str(source)], check=True)
    mod = directory / "despawn-smoke.jar"
    with zipfile.ZipFile(mod, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_despawn_smoke", "version": "1.0", "environment": "client", "entrypoints": {"client": ["io.github.halfmasa.despawnsmoke.DespawnSmoke"]}}))
    game = directory / "game"
    (game / "mods").mkdir(parents=True)
    (game / "options.txt").write_text("enableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\n", encoding="utf-8")
    api = next((Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.161.0+26.3").glob("*/*.jar"))
    shutil.copy2(api, game / "mods" / api.name)
    for extra in args.extra_mod:
        shutil.copy2(extra, game / "mods" / extra.name)
    command = startup.production_command(args, game, mod)
    log_path = directory / "launcher.log"
    started = time.monotonic()
    timed_out = False
    with log_path.open("w", encoding="utf-8") as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            timed_out = True
            subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], capture_output=True, check=False)
            code = process.wait(timeout=30)
    log = log_path.read_text(encoding="utf-8", errors="replace")
    passed = code == 0 and not timed_out and "DESPAWN_WORLD_PASS" in log
    report = {"result": "PASS" if passed else "FAIL", "artifact": str(args.artifact), "extra_mods": [str(path.resolve()) for path in args.extra_mod], "minihud_cache_pass": "DESPAWN_MINIHUD_CACHE_PASS" in log, "duration_seconds": round(time.monotonic() - started, 1), "exit_code": code, "timed_out": timed_out, "log": str(log_path), "screenshots": [str(path) for path in (game / "screenshots").glob("*.png")]}
    path = directory / "report.json"
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"DESPAWN WORLD: {report['result']}", flush=True)
    print(path, flush=True)
    if not passed:
        print("\n".join(log.splitlines()[-80:]), flush=True)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

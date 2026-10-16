"""Verify the installable FGA/halfmasa jars over an isolated dedicated-server connection."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import zipfile
import run_startup_smoke as startup

ROOT = Path(__file__).resolve().parents[2]
SERVER = r'''
package io.github.halfmasa.supportsmoke;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
public class ServerFixture implements DedicatedServerModInitializer {
 private boolean ready;
 private static int reset(ServerPlayer p){
  var inv=p.getInventory();for(int i=0;i<inv.getContainerSize();i++)inv.setItem(i,ItemStack.EMPTY);
  inv.setItem(3,new ItemStack(Items.IRON_PICKAXE));inv.setItem(10,new ItemStack(Items.BREAD,8));inv.setSelectedSlot(3);
  p.setHealth(20);p.getFoodData().setFoodLevel(14);p.getFoodData().setSaturation(0);p.inventoryMenu.broadcastChanges();return 1;
 }
 @Override public void onInitializeServer(){
  ServerLifecycleEvents.SERVER_STARTED.register(server->server.getCommands().getDispatcher().register(
   Commands.literal("halfmasa_support_fixture").then(Commands.literal("reset").executes(context->reset(server.getPlayerList().getPlayers().getFirst())))));
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(ready||server.getPlayerList().getPlayers().isEmpty())return;ready=true;
   var p=server.getPlayerList().getPlayers().getFirst();reset(p);
   var boss=net.minecraft.world.entity.EntityTypes.WITHER.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
   boss.setPos(p.getX()+4,p.getY()+4,p.getZ());boss.setNoAi(true);boss.setNoGravity(true);boss.setHealth(240);p.level().addFreshEntity(boss);
   ((carpet.fga.mixin.WitherBossEventAccessor)boss).fga$bossEvent().addPlayer(p);
   System.out.println("SUPPORT_SERVER_FIXTURE_READY");
  });
 }
}
'''
CLIENT = r'''
package io.github.halfmasa.supportsmoke;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.item.Items;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
public class ClientFixture implements ClientModInitializer {
 private static ClientFixture instance;
 private int stage,checks;private long started;private boolean ordinaryUse,unexpectedSilentUse;private UUID bossId;
 @Override public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private void check(boolean condition,String name){checks++;if(!condition)throw new AssertionError(name);System.out.println("SUPPORT_CHECK "+name);}
 private static boolean idle()throws Exception{
  var service=AutoEatService.getInstance();for(String name:List.of("pendingRestore","pendingReturn")){var f=AutoEatService.class.getDeclaredField(name);f.setAccessible(true);if(f.get(service)!=null)return false;}
  var f=AutoEatService.class.getDeclaredField("state");f.setAccessible(true);return f.get(service).toString().equals("IDLE");
 }
 private void tick(Minecraft mc){try{
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
   String address=System.getProperty("halfmasa.smoke.address");stage=1;ConnectScreen.startConnecting(MinecraftClientCompat.getScreen(mc),mc,ServerAddress.parseString(address),new ServerData("Support fixture",address,ServerData.Type.OTHER),false,null);return;}
  if(mc.player==null||mc.level==null)return;
  if(started==0)started=mc.level.getGameTime();if(mc.level.getGameTime()-started>1400)throw new AssertionError("remote phase timeout "+stage);
  switch(stage){
   case 1->{if(mc.player.getFoodData().getFoodLevel()!=14||mc.player.getInventory().getItem(10).getCount()!=8)return;
    mc.player.getInventory().setSelectedSlot(3);
    check(mc.getSingleplayerServer()==null,"actual dedicated connection");check(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("carpet-fga-addition"),"client has no FGA installed");
    check(HalfMasaSupportNetwork.canEat()&&HalfMasaSupportNetwork.canHealth(),"server advertises both protocol channels");
    Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);Configs.AUTO_EAT_SILENT.setBooleanValue(true);Configs.AUTO_EAT.setBooleanValue(true);stage=2;}
   case 2->{ordinaryUse|=mc.player.isUsingItem();if(mc.player.getFoodData().getFoodLevel()!=20||!idle())return;
    check(ordinaryUse&&mc.player.getInventory().getItem(10).getCount()==6,"disabled FGA rule falls back to ordinary consumption");
    check(mc.player.getInventory().getSelectedSlot()==3&&mc.player.getInventory().getItem(3).is(Items.IRON_PICKAXE),"fallback restores original tool and selection");
    Configs.AUTO_EAT.setBooleanValue(false);System.out.println("SUPPORT_ENABLE_RULE");stage=3;}
   case 3->{if(mc.player.getFoodData().getFoodLevel()!=14||mc.player.getInventory().getItem(10).getCount()!=8)return;
    Configs.AUTO_EAT_SILENT.setBooleanValue(false);Configs.AUTO_EAT_SILENT.setBooleanValue(true);Configs.AUTO_EAT.setBooleanValue(true);stage=4;}
   case 4->{unexpectedSilentUse|=mc.player.isUsingItem();if(mc.player.getFoodData().getFoodLevel()!=20||!idle())return;
    check(!unexpectedSilentUse&&mc.player.getInventory().getItem(10).getCount()==6,"remote silent consumption finishes two server-confirmed meals without use animation");
    check(mc.player.getInventory().getSelectedSlot()==3&&mc.player.getInventory().getItem(3).is(Items.IRON_PICKAXE),"remote silent consumption retains selection and tool");
    Configs.AUTO_EAT.setBooleanValue(false);var overlay=mc.gui.hud.getBossOverlay();var f=overlay.getClass().getDeclaredField("events");f.setAccessible(true);
    @SuppressWarnings("unchecked") var events=(Map<UUID,LerpingBossEvent>)f.get(overlay);if(events.isEmpty())throw new AssertionError("missing real Wither boss event");bossId=events.keySet().iterator().next();
    HalfMasaSupportNetwork.queryHealth(new HalfMasaSupportPayloads.HealthQuery(bossId));stage=5;}
   case 5->{var health=HalfMasaSupportClient.health(bossId);if(health==null)return;
    check(health.valid()&&health.current()==240&&health.maximum()==300,"dedicated FGA returns real Wither health 240/300");
    Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_SHOW_HEALTH.setBooleanValue(true);Configs.BOSS_BAR_COMPACT.setBooleanValue(true);
    System.out.println("SUPPORT_REMOTE_PASS checks="+checks);stage=6;mc.stop();}
  }
 }catch(Throwable failure){throw new IllegalStateException("Dedicated support smoke failed stage="+stage,failure);}}
}
'''
MIXIN = r'''package io.github.halfmasa.supportsmoke.mixin;
import io.github.halfmasa.supportsmoke.ClientFixture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class FrameMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void support$frame(CallbackInfo ci){ClientFixture.frame((Minecraft)(Object)this);}
}'''


def fixture(directory, args):
    sources = []
    for name, text in [("ServerFixture", SERVER), ("ClientFixture", CLIENT), ("FrameMixin", MIXIN)]:
        path = directory / (name + ".java")
        path.write_text(text, encoding="utf-8")
        sources.append(str(path))
    classes = directory / "classes"
    classes.mkdir()
    cp = os.pathsep.join([str(args.artifact), str(args.fga), (ROOT / "build/port-classpath-26.3.txt").read_text().strip()])
    subprocess.run([str(args.java_home / "bin/javac.exe"), "-proc:none", "-encoding", "UTF-8", "-cp", cp, "-d", str(classes), *sources], check=True)
    mod = directory / "support-smoke.jar"
    with zipfile.ZipFile(mod, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_support_smoke", "version": "1.0", "environment": "*", "entrypoints": {"server": ["io.github.halfmasa.supportsmoke.ServerFixture"], "client": ["io.github.halfmasa.supportsmoke.ClientFixture"]}, "mixins": ["support-smoke.mixins.json"]}))
        jar.writestr("support-smoke.mixins.json", json.dumps({"required": True, "package": "io.github.halfmasa.supportsmoke.mixin", "compatibilityLevel": "JAVA_25", "client": ["FrameMixin"], "injectors": {"defaultRequire": 1}}))
    return mod


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--fga", type=Path, required=True)
    parser.add_argument("--carpet", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--jvm-arg", action="append", default=[])
    parser.add_argument("--timeout", type=int, default=600)
    args = parser.parse_args()
    args.version = "26.3"
    directory = ROOT / "build/support-tests" / datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    directory.mkdir(parents=True)
    mod = fixture(directory, args)
    server_game, client_game = directory / "server", directory / "client"
    for game in (server_game, client_game):
        (game / "mods").mkdir(parents=True)
    api = next((Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.161.0+26.3").glob("*/*.jar"))
    for game in (server_game, client_game):
        shutil.copy2(api, game / "mods" / api.name)
    for artifact in (args.fga, args.carpet):
        shutil.copy2(artifact, server_game / "mods" / artifact.name)
    # The client deliberately receives neither FGA nor Carpet.
    with socket.socket() as reserved:
        reserved.bind(("127.0.0.1", 0))
        port = reserved.getsockname()[1]
    (server_game / "eula.txt").write_text("eula=true\n")
    (server_game / "server.properties").write_text(f"online-mode=false\nwhite-list=false\nenforce-secure-profile=false\nserver-ip=127.0.0.1\nserver-port={port}\nlevel-name=fixture\nview-distance=3\nsimulation-distance=3\ndifficulty=normal\ngamemode=survival\nspawn-protection=0\nlevel-type=minecraft:flat\ngenerator-settings={{\"layers\":[{{\"block\":\"minecraft:bedrock\",\"height\":1}}],\"biome\":\"minecraft:plains\"}}\n")
    (client_game / "options.txt").write_text("onboardAccessibility:false\nenableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:3\npauseOnLostFocus:false\nsoundCategory_master:0.0\ntutorialStep:none\n")
    (client_game / "config").mkdir()
    (client_game / "config/malilib.json").write_text("{}\n")
    client_command = startup.production_command(args, client_game, mod)
    client_command[1:1] = [*args.jvm_arg, f"-Dhalfmasa.smoke.address=127.0.0.1:{port}"]
    cp = client_command[client_command.index("-cp") + 1].split(os.pathsep)
    cp[0] = str(Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-extracted_server.jar")
    server_command = [str(args.java_home / "bin/java.exe"), "-Xmx2G", "--enable-native-access=ALL-UNNAMED", f"-Dfabric.addMods={mod}", "-cp", os.pathsep.join(cp), "net.fabricmc.loader.impl.launch.knot.KnotServer", "nogui"]
    server_log, client_log = directory / "server.log", directory / "client.log"
    started = time.monotonic()
    server = client = None
    reason = ""
    try:
        with server_log.open("w", encoding="utf-8") as sout, client_log.open("w", encoding="utf-8") as cout:
            server = subprocess.Popen(server_command, cwd=server_game, stdin=subprocess.PIPE, stdout=sout, stderr=subprocess.STDOUT, text=True, creationflags=subprocess.CREATE_NO_WINDOW)
            while "Done (" not in server_log.read_text(encoding="utf-8", errors="replace"):
                if server.poll() is not None or time.monotonic() - started > 240:
                    raise RuntimeError("dedicated server failed to start")
                time.sleep(1)
            client = subprocess.Popen(client_command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=cout, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
            enabled = False
            while client.poll() is None:
                if server.poll() is not None or time.monotonic() - started > args.timeout:
                    raise RuntimeError("remote smoke timed out or server exited")
                if not enabled and "SUPPORT_ENABLE_RULE" in client_log.read_text(encoding="utf-8", errors="replace"):
                    server.stdin.write("carpet silentAutoEat true\nhalfmasa_support_fixture reset\n")
                    server.stdin.flush()
                    enabled = True
                time.sleep(1)
            server.stdin.write("stop\n")
            server.stdin.flush()
            server.wait(timeout=60)
            if client.returncode != 0 or server.returncode != 0 or "SUPPORT_REMOTE_PASS" not in client_log.read_text(encoding="utf-8", errors="replace"):
                raise RuntimeError("remote smoke assertion failed")
    except Exception as error:
        reason = str(error)
    finally:
        for process in (client, server):
            if process is not None and process.poll() is None:
                subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], capture_output=True, check=False)
                process.wait(timeout=30)
    log = client_log.read_text(encoding="utf-8", errors="replace") if client_log.exists() else ""
    report = {"result": "FAIL" if reason else "PASS", "reason": reason, "version": "26.3", "artifact": str(args.artifact), "fga": str(args.fga), "carpet": str(args.carpet), "client_fga_installed": False, "jvm_args": args.jvm_arg, "duration_seconds": round(time.monotonic() - started, 1), "checks": [line for line in log.splitlines() if "SUPPORT_" in line], "server_log": str(server_log), "client_log": str(client_log)}
    (directory / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("DEDICATED SUPPORT:", report["result"], flush=True)
    print(directory / "report.json", flush=True)
    if reason:
        for path in (server_log, client_log):
            if path.exists():
                print(path, "\n", "\n".join(path.read_text(encoding="utf-8", errors="replace").splitlines()[-32:]))
    return 1 if reason else 0


if __name__ == "__main__":
    raise SystemExit(main())

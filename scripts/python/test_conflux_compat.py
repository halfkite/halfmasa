"""Check production adapters against pinned upstream API sources and real 0.1.10+ jars.

Run with CONFLUX_TEST_JAR pointing to an official 26.3 Conflux Map jar.
The API contract is pinned to upstream bfcac532 (unreleased at test authoring).
This checks adapter contracts; run_conflux_grid_smoke.py provides a real client check.
"""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]
COMMIT = "bfcac5326994a1088402a7205d665f87c9949a83"
API_FILES = "ActionApi ApiStatus ConfluxMapApi ConfluxMapEvents ConfluxMapPlugin Event EventRegistration MapDataApi MarkerApi SharedWaypointApi WaypointApi".split()
STUBS = {
    "net/minecraft/world/level/chunk/status/ChunkStatus.java": "package net.minecraft.world.level.chunk.status;public class ChunkStatus {public static final ChunkStatus FULL=new ChunkStatus();}",
    "io/github/halfmasa/xaerobinding/config/Configs.java": """package io.github.halfmasa.xaerobinding.config;
public class Configs {public static class V {public boolean flag=true; public int n=128; public boolean getBooleanValue(){return flag;} public int getIntegerValue(){return n;}}
public static final V CONFLUX_MAP_EXTENSIONS=new V(), CONFLUX_MAP_UNKNOWN_HEIGHT=new V(), CONFLUX_MAP_ALL_DIMENSIONS=new V(),CONFLUX_MAP_PUBLIC_WAYPOINTS=new V(),CONFLUX_MAP_CLOSE_AFTER_TELEPORT=new V();} """,
    "net/fabricmc/loader/api/FabricLoader.java": "package net.fabricmc.loader.api; public class FabricLoader {public static boolean installed=true;public static FabricLoader getInstance(){return new FabricLoader();} public boolean isModLoaded(String id){return installed;}}",
    "fi/dy/masa/malilib/interfaces/IClientTickHandler.java": "package fi.dy.masa.malilib.interfaces; public interface IClientTickHandler {void onClientTick(net.minecraft.client.Minecraft client);}",
    "io/github/halfmasa/xaerobinding/XaeroWorldBinding.java": "package io.github.halfmasa.xaerobinding;public class XaeroWorldBinding {public static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger(\"ConfluxTest\");}",
    "net/minecraft/client/Minecraft.java": """package net.minecraft.client;
public class Minecraft {public static final Minecraft INSTANCE=new Minecraft();public Level level=new Level();public Object player=new Object();public static Minecraft getInstance(){return INSTANCE;}
public static class Level {public boolean loaded;public Key dimension(){return new Key();}public boolean hasChunk(int x,int z){return true;}public Chunks getChunkSource(){return new Chunks(this);}}public static class Chunks {private final Level level;public Chunks(Level level){this.level=level;}public Object getChunk(int x,int z,net.minecraft.world.level.chunk.status.ChunkStatus status,boolean create){if(create)throw new AssertionError("Must not load chunks");return level.loaded?new Object():null;}}public static class Key {public String identifier(){return "minecraft:overworld";}}} """,
}
HARNESS = r'''
import java.util.*;
import java.lang.reflect.*;
import cn.net.rms.confluxmap.api.*;
import cn.net.rms.confluxmap.core.store.ColumnStore;
import cn.net.rms.confluxmap.core.model.DimensionId;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.ConfluxMapCapabilities;
import net.minecraft.client.Minecraft;
public class ConfluxCompatCheck {
 static int checks,closed;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static final Map<String,Event<?>> events=new HashMap<>();
 @SuppressWarnings("unchecked") static <T> Event<T> event(String name){return (Event<T>)events.computeIfAbsent(name,k->new Event<>());}
 @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type,java.lang.reflect.InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);}
 static ConfluxMapApi api(){
  var eventApi=proxy(ConfluxMapEvents.class,(p,m,a)->event(m.getName()));
  var actions=proxy(ActionApi.class,(p,m,a)->{if(m.getName().equals("closeFullscreenMap"))closed++;return null;});
  return proxy(ConfluxMapApi.class,(p,m,a)->switch(m.getName()){case "events"->eventApi;case "actions"->actions;case "version"->"0.1.12-contract";default->null;});
 }
 static void tick(){ConfluxMapApiCompat.getInstance().onClientTick(Minecraft.getInstance());}
 static void apiChecks()throws Exception{
  check(ConfluxMapCapabilities.event("teleportLandingResolve"),"public resolver detected without loading target screen");
  var plugin=new io.github.halfmasa.xaerobinding.mixin.XaeroMixinPlugin();
  check(!plugin.shouldApplyMixin("", "ConfluxMapSurfaceTeleportMixin"),"public resolver excludes legacy surface mixin");
  check(!plugin.shouldApplyMixin("", "ConfluxMapTeleportScreenMixin"),"obsolete OptionalInt mixin excluded");
  check(!plugin.shouldApplyMixin("", "ConfluxMapWaypointListMixin"),"public opening event excludes private defaults mixin");
  ConfluxMapApi.install(api());tick();tick();
  var listenerField=Event.class.getDeclaredField("listeners");listenerField.setAccessible(true);
  check(((List<?>)listenerField.get(event("teleportLandingResolve"))).size()==1,"repeated ticks register once");
  var resolve=new ConfluxMapEvents.TeleportLandingResolve(0,0,"minecraft:overworld",true);
  ConfluxCompatCheck.<ConfluxMapEvents.TeleportLandingResolve>event("teleportLandingResolve").fire(resolve);
  check(resolve.landingY().orElseThrow()==128,"void resolver supplies configured landing Y");
  Configs.CONFLUX_MAP_UNKNOWN_HEIGHT.n=-64;
  resolve=new ConfluxMapEvents.TeleportLandingResolve(0,0,"minecraft:the_nether",false);
  ConfluxCompatCheck.<ConfluxMapEvents.TeleportLandingResolve>event("teleportLandingResolve").fire(resolve);
  check(resolve.landingY().orElseThrow()==-64,"offline resolver reads changed negative Y without re-registering");
  var opening=new ConfluxMapEvents.WaypointListOpening(true,true);
  ConfluxCompatCheck.<ConfluxMapEvents.WaypointListOpening>event("waypointListOpening").fire(opening);
  check(opening.requestedAllDimensions()&&opening.requestedTab()==ConfluxMapEvents.WaypointListTab.PUBLIC,"hotkey requests both configured defaults");
  opening=new ConfluxMapEvents.WaypointListOpening(true,false);
  ConfluxCompatCheck.<ConfluxMapEvents.WaypointListOpening>event("waypointListOpening").fire(opening);
  check(opening.requestedTab()==null,"unavailable shared catalog does not request PUBLIC");
  opening=new ConfluxMapEvents.WaypointListOpening(false,true);
  ConfluxCompatCheck.<ConfluxMapEvents.WaypointListOpening>event("waypointListOpening").fire(opening);
  check(!opening.requestedAllDimensions()&&opening.requestedTab()==null,"other screen opens preserve upstream defaults");
  var action=new ConfluxMapEvents.LocationActionEvent(ConfluxMapEvents.LocationAction.TELEPORT,0,0,"minecraft:overworld",false);
  ConfluxCompatCheck.<ConfluxMapEvents.LocationActionEvent>event("locationActionPerformed").fire(action);
  check(closed==1,"teleport action closes map once");
  Configs.CONFLUX_MAP_CLOSE_AFTER_TELEPORT.flag=false;
  ConfluxCompatCheck.<ConfluxMapEvents.LocationActionEvent>event("locationActionPerformed").fire(action);
  check(closed==1,"close option changes immediately");
  Configs.CONFLUX_MAP_EXTENSIONS.flag=false;
  resolve=new ConfluxMapEvents.TeleportLandingResolve(0,0,"minecraft:overworld",true);
  ConfluxCompatCheck.<ConfluxMapEvents.TeleportLandingResolve>event("teleportLandingResolve").fire(resolve);
  check(resolve.landingY().isEmpty(),"disable takes effect before next tick");tick();
  check(!event("teleportLandingResolve").hasListeners(),"disabled extension unregisters listeners");
  Configs.CONFLUX_MAP_EXTENSIONS.flag=true;tick();
  check(((List<?>)listenerField.get(event("teleportLandingResolve"))).size()==1,"reenable installs one listener");
  ConfluxMapApi.uninstall();tick();check(!event("teleportLandingResolve").hasListeners(),"API shutdown unregisters old listeners");
  ConfluxMapApi.install(api());tick();check(event("teleportLandingResolve").hasListeners(),"new API instance reconnects cleanly");
 }
 public static class Session {public DimensionId dimension(){return DimensionId.OVERWORLD;}public Object world(){return this;}}
 public static class Service {public int calls;public double x,y,z;public void teleportExact(double x,double y,double z,DimensionId dimension,Object world){calls++;this.x=x;this.y=y;this.z=z;}}
 public static class Screen {public boolean live=true;public ColumnStore.SurfaceLookup saved=ColumnStore.SurfaceLookup.UNKNOWN;private final Service groundTeleport=new Service();
  boolean viewingLiveSession(){return live;} ColumnStore.SurfaceLookup capturedGroundAt(int x,int z){return saved;} Session viewSession(){return new Session();}}
 static Object target(ColumnStore.SurfaceLookup ground)throws Exception{
  var type=Class.forName("cn.net.rms.confluxmap.mc.ui.screen.FullscreenMapLocationMenu$Target");
  var ctor=type.getDeclaredConstructor(int.class,ColumnStore.SurfaceLookup.class,int.class);ctor.setAccessible(true);return ctor.newInstance(-10000,ground,10000);
 }
 static void legacyChecks()throws Exception{
  check(ConfluxMapCapabilities.legacyTeleport(),"official jar has dual lookup teleport contract");
  var screen=new Screen();var adapter=new ConfluxMapLegacyTeleport(Screen.class);
  var unknown=target(ColumnStore.SurfaceLookup.UNKNOWN);
  check(!adapter.needsFallback(screen,unknown),"live unknown target retains two stage teleport");
  screen.live=false;check(adapter.needsFallback(screen,unknown),"offline unknown target accepts fallback");
  Configs.CONFLUX_MAP_UNKNOWN_HEIGHT.n=-64;
  check(Minecraft.getInstance().level.hasChunk(0,0)&&!Minecraft.getInstance().level.loaded,"client boundary reproduces hasChunk true for unloaded chunks");
  check(adapter.teleport(screen,unknown)&&screen.groundTeleport.calls==1,"offline fallback sends exactly one command");
  check(screen.groundTeleport.x==-9999.5&&screen.groundTeleport.y==-64&&screen.groundTeleport.z==10000.5,"negative coordinates centered and exact landing Y preserved");
  Minecraft.getInstance().level.loaded=true;
  check(!adapter.teleport(screen,unknown)&&screen.groundTeleport.calls==1,"loaded actual terrain is authoritative");Minecraft.getInstance().level.loaded=false;
  screen.saved=new ColumnStore.SurfaceLookup(true,OptionalInt.of(70));
  check(!adapter.needsFallback(screen,unknown),"captured terrain is authoritative");
  screen.saved=new ColumnStore.SurfaceLookup(true,OptionalInt.empty());screen.live=true;
  check(adapter.needsFallback(screen,target(new ColumnStore.SurfaceLookup(true,OptionalInt.of(200)))),"captured void takes priority over predicted surface");
  check(adapter.teleport(screen,unknown)&&screen.groundTeleport.calls==2,"void uses configured fallback once");
  screen.saved=ColumnStore.SurfaceLookup.UNKNOWN;
  check(adapter.needsFallback(screen,target(new ColumnStore.SurfaceLookup(true,OptionalInt.empty()))),"predicted void can resolve fallback");
  Configs.CONFLUX_MAP_EXTENSIONS.flag=false;check(!adapter.teleport(screen,unknown),"disabled extension never intercepts");
  Configs.CONFLUX_MAP_EXTENSIONS.flag=true;check(!adapter.teleport(screen,null),"missing target preserved");
  Minecraft.getInstance().level=null;check(!adapter.teleport(screen,unknown),"world exit cannot send fallback command");
 }
 public static void main(String[] args)throws Exception{if(args[0].equals("api"))apiChecks();else legacyChecks();System.out.println("PASS "+args[0]+" checks="+checks);}
}
'''


class ConfluxCompatibilityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.mod = Path(os.environ.get("CONFLUX_TEST_JAR", "D:/我的世界/.minecraft/versions/26.3-Fabric 0.19.5/mods/confluxmap-26.3-0.1.11.jar"))
        if not cls.mod.is_file():
            raise unittest.SkipTest("Set CONFLUX_TEST_JAR to an official 26.3 Conflux Map 0.1.10+ jar")
        cls.temp = tempfile.TemporaryDirectory()
        cls.folder = Path(cls.temp.name)
        cls.java = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-25.0.3")) / "bin"
        api_root = ROOT / "build/conflux-issue6/upstream-api/cn/net/rms/confluxmap/api"
        api_root.mkdir(parents=True, exist_ok=True)
        for name in API_FILES:
            path = api_root / f"{name}.java"
            if not path.exists():
                url = f"https://raw.githubusercontent.com/Conflux-Union/conflux-map/{COMMIT}/api/src/main/java/cn/net/rms/confluxmap/api/{name}.java"
                path.write_bytes(urllib.request.urlopen(url, timeout=30).read())
        files = []
        for name, source in STUBS.items():
            path = cls.folder / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding="utf-8")
            files.append(path)
        harness = cls.folder / "ConfluxCompatCheck.java"
        harness.write_text(HARNESS, encoding="utf-8")
        files.append(harness)
        with zipfile.ZipFile(cls.mod) as jar:
            for name in jar.namelist():
                if name.startswith("META-INF/jars/") and name.endswith(".jar"):
                    (cls.folder / Path(name).name).write_bytes(jar.read(name))
        jars = [str(cls.mod), *(str(p) for p in cls.folder.glob("*.jar"))]
        jars.append((ROOT / "build/port-classpath-26.3.txt").read_text(encoding="utf-8").strip())
        cls.cp = os.pathsep.join([str(cls.folder), *jars])
        source_root = ROOT / "src/main/java/io/github/halfmasa/xaerobinding"
        files += [source_root / p for p in ["compat/ConfluxMapCapabilities.java", "feature/ConfluxMapApiCompat.java", "feature/ConfluxMapLegacyTeleport.java", "mixin/XaeroMixinPlugin.java"]]
        files += list(api_root.glob("*.java"))
        subprocess.run([str(cls.java / "javac.exe"), "-proc:none", "-encoding", "UTF-8", "-cp", cls.cp, "-d", str(cls.folder), *(str(p) for p in files)], check=True, capture_output=True)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def scenario(self, name):
        result = subprocess.run([str(self.java / "java.exe"), "-cp", self.cp, "ConfluxCompatCheck", name], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("PASS", result.stdout)
        print(result.stdout.strip())

    def test_public_events_lifecycle_and_live_configuration(self):
        self.scenario("api")

    def test_legacy_lookup_priority_and_exact_landing(self):
        self.scenario("legacy")


if __name__ == "__main__":
    unittest.main()

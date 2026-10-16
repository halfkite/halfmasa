"""Launch an isolated dev client, confirm the title screen, and stop it automatically."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import zipfile
import hashlib
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
SMOKE_SOURCE = r'''
package io.github.halfmasa.smoke;
import java.lang.reflect.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"net.minecraft.client.Minecraft", "net.minecraft.class_310"})
public abstract class ClientSmokeMixin {
 @Unique private long halfmasaSmoke$titleAt;
 @Unique private boolean halfmasaSmoke$done;
 @Inject(method={"runTick", "method_1523"}, at=@At("TAIL"))
 private void halfmasaSmoke$check(CallbackInfo ci) {
  if(halfmasaSmoke$done) return;
  try {
   Class<?> compat=Class.forName("io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat");
   Object screen=compat.getMethod("getScreen",getClass()).invoke(null,this);
   if(screen==null||!(screen.getClass().getName().endsWith("TitleScreen")||screen.getClass().getName().equals("net.minecraft.class_442"))) return;
   Object overlay;
   try {overlay=getClass().getMethod("getOverlay").invoke(this);}
   catch(NoSuchMethodException oldBackend) {
    try {overlay=getClass().getMethod("method_18506").invoke(this);}
    catch(NoSuchMethodException modernBackend) {Object gui=getClass().getField("gui").get(this);overlay=gui.getClass().getMethod("overlay").invoke(gui);}
   }
   if(overlay!=null) return;
   if(halfmasaSmoke$titleAt==0) {
    halfmasaSmoke$titleAt=System.nanoTime();
    Class<?> configs=Class.forName("io.github.halfmasa.xaerobinding.config.Configs");
    // The title screen may never instantiate an EditBox or vanilla preedit
    // overlay. Load these classes explicitly to validate their Mixin targets.
    try {Class.forName("net.minecraft.client.gui.components.EditBox");}
    catch(ClassNotFoundException production) {Class.forName("net.minecraft.class_342");}
    try {Class.forName("net.minecraft.client.gui.screens.ChatScreen");}
    catch(ClassNotFoundException production) {Class.forName("net.minecraft.class_408");}
    try {
     configs.getField("IME_RENDER_STYLE");
     Class.forName("net.minecraft.client.gui.components.IMEPreeditOverlay");
    } catch(NoSuchFieldException legacyWithoutPreedit) {}
    System.out.println("HALFMASA_SMOKE INPUT_MIXINS_PASS");
    Class<?> configBase=Class.forName("fi.dy.masa.malilib.config.IConfigBase");
    for(String name:new String[]{"RANGE_FEEDING","AUTO_EAT","BETTER_BOSS_BAR","INVISIBLE_BLOCKS","CUSTOM_ENTITY_RENDER_DISABLE","REMOVE_COOLDOWN_ANIMATIONS","REMOVE_ATTACK_SWING","REMOVE_INTERACT_SWING","REMOVE_ITEM_COOLDOWN_OVERLAY","REMOVE_EQUIP_ANIMATION"}) {
     Object option=configs.getField(name).get(null);
     if((boolean)option.getClass().getMethod("getBooleanValue").invoke(option)) throw new AssertionError("unsafe default: "+name);
    }
    Object feeding=configs.getField("RANGE_FEEDING").get(null);
    Object protection=configs.getField("RANGE_FEEDING_SKIP_BABIES").get(null);
    if(!(boolean)protection.getClass().getMethod("getBooleanValue").invoke(protection)) throw new AssertionError("baby protection must default on");
    Object interval=configs.getField("RANGE_FEEDING_INTERVAL").get(null);
    if((int)interval.getClass().getMethod("getIntegerValue").invoke(interval)!=4) throw new AssertionError("feeding interval default");
    for(String name:new String[]{"RANGE_FEEDING_SKIP_BABIES","RANGE_FEEDING_HOLD","RANGE_FEEDING_ONCE","RANGE_FEEDING_RANGE","RANGE_FEEDING_INTERVAL","RANGE_FEEDING_LIST_MODE","RANGE_FEEDING_BLACKLIST","RANGE_FEEDING_WHITELIST"}) {
     Object child=configs.getField(name).get(null);
     if(configs.getMethod("getExpansionParent",configBase).invoke(null,child)!=feeding) throw new AssertionError("missing feeding child: "+name);
    }
    for(String name:new String[]{"RangeFeedingService","AutoEatService","BossBarGrouper","EntityRenderDisableService","MarkerBlock","SwingVisualTracker","ConfluxWaypointScreenBridge"}) Class.forName("io.github.halfmasa.xaerobinding.feature."+name);
    Field supportReady=Class.forName("io.github.halfmasa.xaerobinding.feature.HalfMasaSupportNetwork").getDeclaredField("ready");supportReady.setAccessible(true);
    if(!supportReady.getBoolean(null)) throw new AssertionError("support protocol registration failed");
    Class<?> hotkeyType=Class.forName("fi.dy.masa.malilib.hotkeys.IHotkey");
    Method passive=Class.forName("io.github.halfmasa.xaerobinding.feature.HoldToShow").getMethod("isPassive",hotkeyType);
    for(String name:new String[]{"BOSS_BAR_HOLD_KEY","INVISIBLE_HOLD_KEY","ENTITY_RENDER_HOLD_SHOW_HOTKEY"}) {
     Object binding=configs.getField(name).get(null);
     if(!(boolean)passive.invoke(null,binding)) throw new AssertionError("held binding enters conflict wheel: "+name);
     Object key=binding.getClass().getMethod("getKeybind").invoke(binding);
     if(!((java.util.List<?>)key.getClass().getMethod("getKeys").invoke(key)).isEmpty()) throw new AssertionError("new hold binding is not unbound: "+name);
    }
    org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
    System.out.println("HALFMASA_SMOKE PORTED_FEATURES_PASS");
    if(java.util.Arrays.stream(configs.getFields()).anyMatch(field -> field.getName().equals("ITEM_DESPAWN_TIMER"))) {
     Class<?> renderer;
     try {renderer=Class.forName("net.minecraft.client.renderer.entity.ItemEntityRenderer");}
     catch(ClassNotFoundException production) {renderer=halfmasaSmoke$itemRendererClass(this);}
     Method extract=java.util.Arrays.stream(renderer.getDeclaredMethods()).filter(method -> method.getName().contains("halfmasa$extractTimer")).findFirst().orElse(null);
     if(extract!=null) {
      Object state=extract.getParameterTypes()[1].getConstructor().newInstance();
      Class<?> labelState=Class.forName("io.github.halfmasa.xaerobinding.feature.ItemDespawnLabelState");
      if(!labelState.isInstance(state)) throw new AssertionError("despawn render-state Mixin was not applied");
     } else if(java.util.Arrays.stream(renderer.getDeclaredMethods()).noneMatch(method -> method.getName().contains("halfmasa$renderTimer"))) throw new AssertionError("legacy despawn renderer Mixin was not applied");
     Object timer=configs.getField("ITEM_DESPAWN_TIMER").get(null);
     if((boolean)timer.getClass().getMethod("getBooleanValue").invoke(timer)) throw new AssertionError("despawn timer should default to disabled");
     Object configHandler=configs.getConstructor().newInstance();
     timer.getClass().getMethod("setBooleanValue",boolean.class).invoke(timer,true);
     configs.getMethod("save").invoke(configHandler);
     timer.getClass().getMethod("setBooleanValue",boolean.class).invoke(timer,false);
     configs.getMethod("load").invoke(configHandler);
     if(!(boolean)timer.getClass().getMethod("getBooleanValue").invoke(timer)) throw new AssertionError("saved enabled timer preference was overwritten");
     timer.getClass().getMethod("resetToDefault").invoke(timer);
     if((boolean)timer.getClass().getMethod("getBooleanValue").invoke(timer)) throw new AssertionError("timer reset default is not disabled");
     configs.getMethod("save").invoke(configHandler);
     Method timerParent=configs.getMethod("getExpansionParent",configBase);
     for(String suffix:new String[]{"SOURCE","FORMAT","THROUGH_WALLS","MAX_LABELS","SYNC_INTERVAL","DISTANCE"})
      if(timerParent.invoke(null,configs.getField("ITEM_DESPAWN_"+suffix).get(null))!=timer) throw new AssertionError("wrong despawn child parent: "+suffix);
     if(configs.getMethod("getExpansionConfig",configBase).invoke(null,timer)==null) throw new AssertionError("despawn settings cannot expand");
     Object loader=Class.forName("net.fabricmc.loader.api.FabricLoader").getMethod("getInstance").invoke(null);
     boolean mini=(boolean)loader.getClass().getMethod("isModLoaded",String.class).invoke(loader,"minihud");
     Field ready=Class.forName("io.github.halfmasa.xaerobinding.feature.ItemDespawnServuxClient").getDeclaredField("ready");ready.setAccessible(true);
     if(ready.getBoolean(null)==mini) throw new AssertionError("incorrect standalone Servux channel ownership");
     if(mini) {
      try {Class.forName("fi.dy.masa.minihud.data.EntityDataManager");}
      catch(ClassNotFoundException legacy) {
       Class<?> manager=Class.forName("fi.dy.masa.minihud.data.EntitiesDataManager");
       if(java.util.Arrays.stream(manager.getDeclaredMethods()).noneMatch(method -> method.getName().contains("halfmasa$serverItemAge"))) throw new AssertionError("legacy MiniHUD server-reply hook missing");
      }
     }
     System.out.println("HALFMASA_SMOKE DESPAWN_MIXINS_AND_CONFIG_PASS");
    }
    Object item=configs.getField("ITEM_RENDER_AGGREGATION").get(null);
    Object mob=configs.getField("ENTITY_RENDER_AGGREGATION").get(null);
    Method parent=configs.getMethod("getExpansionParent",configBase);
    if(parent.invoke(null,item)!=null||parent.invoke(null,mob)!=null) throw new AssertionError("aggregation parents are still nested");
    if(configs.getMethod("getExpansionConfig",configBase).invoke(null,item)==null) throw new AssertionError("item settings cannot expand");
    for(String suffix:new String[]{"THRESHOLD","RADIUS","LIST_MODE","WHITELIST","BLACKLIST","LABEL_POSITION","SCAN_INTERVAL"})
     if(parent.invoke(null,configs.getField("ITEM_AGGREGATION_"+suffix).get(null))!=item) throw new AssertionError("wrong item child parent: "+suffix);
    Class<?> ime=Class.forName("io.github.halfmasa.xaerobinding.feature.Ime261Compat");
    if((boolean)ime.getMethod("cancelVanillaPreeditRender").invoke(null)||(boolean)ime.getMethod("shouldRenderOverlayLayer").invoke(null)) throw new AssertionError("disabled IME changes vanilla UI");
    Object master=configs.getField("CONTINGAME_IME").get(null);
    Object service=Class.forName("io.github.halfmasa.xaerobinding.feature.ImeService").getMethod("getInstance").invoke(null);
    Object bridge=Class.forName("city.windmill.ingameime.client.jni.ExternalBaseIME").getMethod("getInstance").invoke(null);
    if((boolean)bridge.getClass().getMethod("isInitialized").invoke(bridge)) throw new AssertionError("disabled IME initialized its native bridge");
    master.getClass().getMethod("setBooleanValue",boolean.class).invoke(master,true);
    service.getClass().getMethod("onClientTick",getClass()).invoke(service,this);
    boolean installed=(boolean)bridge.getClass().getMethod("isInitialized").invoke(bridge);
    master.getClass().getMethod("setBooleanValue",boolean.class).invoke(master,false);
    service.getClass().getMethod("onClientTick",getClass()).invoke(service,this);
    if((boolean)bridge.getClass().getMethod("isInitialized").invoke(bridge)) throw new AssertionError("IME bridge remains installed after disabling");
    System.out.println("HALFMASA_SMOKE IME_TOGGLE native_installed="+installed);
    System.out.println("HALFMASA_SMOKE CONFIG_AND_IME_PASS");
    System.out.println("HALFMASA_SMOKE TITLE_READY");
   }
   if(System.nanoTime()-halfmasaSmoke$titleAt>=2_000_000_000L) {
    halfmasaSmoke$done=true;
    System.out.println("HALFMASA_SMOKE PASS");
    try {getClass().getMethod("stop").invoke(this);}
    catch(NoSuchMethodException production) {getClass().getMethod("method_1592").invoke(this);}
   }
  } catch(Throwable failure) {throw new IllegalStateException("HalfMasa startup smoke failed",failure);}
 }
 @Unique private static Class<?> halfmasaSmoke$itemRendererClass(Object client)throws Exception {
  Object dispatcher;
  try {dispatcher=client.getClass().getMethod("getEntityRenderDispatcher").invoke(client);}
  catch(NoSuchMethodException production) {dispatcher=client.getClass().getMethod("method_1561").invoke(client);}
  for(Field field:dispatcher.getClass().getDeclaredFields()) {
   if(!java.util.Map.class.isAssignableFrom(field.getType())) continue;
   field.setAccessible(true);Object map=field.get(dispatcher);
   if(map instanceof java.util.Map<?,?> renderers) for(Object candidate:renderers.values()) {
    if(candidate!=null&&java.util.Arrays.stream(candidate.getClass().getDeclaredMethods()).anyMatch(method -> method.getName().contains("halfmasa$extractTimer")||method.getName().contains("halfmasa$renderTimer"))) return candidate.getClass();
   }
  }
  throw new AssertionError("item renderer was not registered");
 }
}
'''


CLIPBOARD_SOURCE = r'''
package io.github.halfmasa.smoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="io.github.halfmasa.xaerobinding.feature.ScreenshotClipboard",remap=false)
public abstract class ClipboardSmokeMixin {
 @Inject(method="initialize",at=@At("HEAD"),cancellable=true)
 private static void halfmasaSmoke$skipDesktopClipboard(CallbackInfo ci){ci.cancel();}
}
'''


def prepare_mod(java_home, disable_clipboard_init=False):
    directory = ROOT / "build/startup-harness"
    source = directory / "src/io/github/halfmasa/smoke/ClientSmokeMixin.java"
    classes = directory / "classes"
    source.parent.mkdir(parents=True, exist_ok=True)
    classes.mkdir(parents=True, exist_ok=True)
    source.write_text(SMOKE_SOURCE, encoding="utf-8")
    sources = [str(source)]
    if disable_clipboard_init:
        clipboard_source = source.with_name("ClipboardSmokeMixin.java")
        clipboard_source.write_text(CLIPBOARD_SOURCE, encoding="utf-8")
        sources.append(str(clipboard_source))
    mixins = [path for path in (Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc/sponge-mixin").glob("*/*/*.jar")
              if not path.name.endswith(("-sources.jar", "-javadoc.jar"))]
    if not mixins:
        raise RuntimeError("Cached Mixin library not found")
    mixin = max(mixins, key=lambda path: path.stat().st_mtime)
    subprocess.run([str(java_home / "bin/javac.exe"), "--release", "21", "-proc:none", "-encoding", "UTF-8",
                    "-classpath", str(mixin), "-d", str(classes), *sources], check=True)
    artifact = directory / "startup-smoke.jar"
    with zipfile.ZipFile(artifact, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_startup_smoke",
                     "version": "1.0.0", "name": "HalfMasa Startup Smoke", "environment": "client",
                     "mixins": ["startup-smoke.mixins.json"]}))
        jar.writestr("startup-smoke.mixins.json", json.dumps({"required": True,
                     "package": "io.github.halfmasa.smoke", "compatibilityLevel": "JAVA_21",
                     "client": ["ClientSmokeMixin"] + (["ClipboardSmokeMixin"] if disable_clipboard_init else []), "injectors": {"defaultRequire": 1}}))
    return artifact


def allowed(rules):
    if not rules:
        return True
    result = False
    for rule in rules:
        if "features" in rule:
            continue
        system = rule.get("os", {})
        if system.get("name") not in (None, "windows") or system.get("arch") in ("x86", "aarch64", "arm64"):
            continue
        result = rule["action"] == "allow"
    return result


def resolve_library(library):
    coordinate = library["name"].split(":")
    group, artifact, version = coordinate[:3]
    suffix = "-" + coordinate[3] if len(coordinate) > 3 else ""
    filename = f"{artifact}-{version}{suffix}.jar"
    files = Path.home() / f".gradle/caches/modules-2/files-2.1/{group}/{artifact}/{version}"
    matches = list(files.glob("*/" + filename))
    if matches:
        return matches[0]
    download = library.get("downloads", {}).get("artifact", {})
    relative = download.get("path", f"{group.replace('.', '/')}/{artifact}/{version}/{filename}")
    installed = Path(r"D:\我的世界\.minecraft\libraries") / relative
    if installed.is_file():
        return installed
    destination = (ROOT / "build/startup-libraries" / relative).resolve()
    if not destination.is_relative_to((ROOT / "build/startup-libraries").resolve()):
        raise RuntimeError("Invalid library path")
    if not destination.is_file():
        destination.parent.mkdir(parents=True, exist_ok=True)
        url = download.get("url") or library.get("url", "https://libraries.minecraft.net/") + relative
        data = urllib.request.urlopen(url, timeout=45).read()
        if download.get("sha1") and hashlib.sha1(data).hexdigest() != download["sha1"]:
            raise RuntimeError(f"Library hash mismatch: {filename}")
        destination.write_bytes(data)
    return destination


def production_command(args, game, smoke_mod):
    mc_cache = Path.home() / f".gradle/caches/fabric-loom/{args.version}"
    info = json.loads((mc_cache / "mojang_minecraft_info.json").read_text(encoding="utf-8"))
    loader = getattr(args, "loader_version", None) or ("0.19.5" if args.version == "26.3" else "0.18.4" if args.version.startswith("26.") else "0.17.3")
    profile_cache = ROOT / f"build/startup-libraries/loader-{args.version}-{loader}.json"
    if not profile_cache.is_file():
        profile_cache.parent.mkdir(parents=True, exist_ok=True)
        url = f"https://meta.fabricmc.net/v2/versions/loader/{args.version}/{loader}/profile/json"
        profile_cache.write_bytes(urllib.request.urlopen(url, timeout=45).read())
    profile = json.loads(profile_cache.read_text(encoding="utf-8"))
    libraries_by_module = {}
    for library in profile["libraries"] + info["libraries"]:
        if not allowed(library.get("rules")):
            continue
        parts = library["name"].split(":")
        module = (parts[0], parts[1], parts[3] if len(parts) > 3 else "")
        # Fabric's profile overrides vanilla versions of shared libraries such
        # as ASM. Loading both copies is rejected by the production launcher.
        libraries_by_module.setdefault(module, library)
    libraries = list(libraries_by_module.values())
    resolved = [resolve_library(library) for library in libraries]
    classpath = list(dict.fromkeys([mc_cache / "minecraft-client.jar", *resolved]))
    natives = game / "natives"
    natives.mkdir()
    for path in resolved:
        with zipfile.ZipFile(path) as library:
            for entry in library.infolist():
                if entry.filename.lower().endswith(".dll"):
                    (natives / Path(entry.filename).name).write_bytes(library.read(entry))
    asset_id = info["assetIndex"]["id"]
    assets = next((path for path in [Path.home() / ".gradle/caches/fabric-loom/assets", Path(r"D:\我的世界\.minecraft\assets")]
                   if (path / f"indexes/{asset_id}.json").is_file()), None)
    if assets is None:
        raise RuntimeError(f"Cached asset index {asset_id} not found")
    properties = {}
    for line in (ROOT / f"versions/{args.version}/gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            properties[key] = value
    malilib_id = properties["malilib_modrinth_version_id"]
    malilib = next(path for path in (Path.home() / f".gradle/caches/modules-2/files-2.1/maven.modrinth/GcWjdA9I/{malilib_id}").glob("*/*.jar")
                   if not path.name.endswith(("-sources.jar", "-javadoc.jar")))
    shutil.copy2(malilib, game / "mods" / malilib.name)
    shutil.copy2(args.artifact, game / "mods" / args.artifact.name)
    jvm = ["-Xmx2G", "--enable-native-access=ALL-UNNAMED", f"-Djava.library.path={natives}",
           f"-Dorg.lwjgl.librarypath={natives}", f"-Dfabric.addMods={smoke_mod}", "-Dfabric.log.disableAnsi=true"]
    offline_name = "HalfMasaSmoke"
    uuid = bytearray(hashlib.md5(("OfflinePlayer:" + offline_name).encode()).digest())
    uuid[6] = uuid[6] & 15 | 48
    uuid[8] = uuid[8] & 63 | 128
    return [str(args.java_home / "bin/java.exe"), *jvm, "-cp", os.pathsep.join(map(str, classpath)), profile["mainClass"],
            "--version", args.version, "--gameDir", str(game), "--assetsDir", str(assets), "--assetIndex", asset_id,
            "--username", offline_name, "--uuid", uuid.hex(), "--accessToken", "0", "--userType", "legacy",
            "--versionType", "release", "--width", "480", "--height", "270"]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--loader-version", help="Optional loader override for integration dependencies with a newer minimum")
    parser.add_argument("--java-home", required=True, type=Path)
    parser.add_argument("--timeout", type=int, default=240)
    parser.add_argument("--artifact", type=Path, help="Launch this distributable jar in production mode")
    parser.add_argument("--extra-mod", action="append", type=Path, default=[], help="Optional mod jar for an isolated compatibility launch")
    parser.add_argument("--disable-clipboard-init", action="store_true", help="Isolate feature checks from unrelated AWT clipboard startup")
    parser.add_argument("--jvm-arg", action="append", default=[])
    args = parser.parse_args()
    if args.artifact:
        args.artifact = args.artifact.resolve()
        if not args.artifact.is_file():
            parser.error(f"Artifact not found: {args.artifact}")
    versions = json.loads((ROOT / "settings.json").read_text(encoding="utf-8"))["versions"]
    if args.version not in versions:
        parser.error("Unsupported game version")
    smoke_mod = prepare_mod(args.java_home, args.disable_clipboard_init)
    now = datetime.now(timezone(timedelta(hours=8)))
    directory = ROOT / "build/startup-tests" / args.version / now.strftime("%Y%m%d-%H%M%S")
    game = directory / "game"
    config_dir = game / "config/halfmasa"
    config_dir.mkdir(parents=True, exist_ok=False)
    (game / "config/malilib.json").write_text("{}\n", encoding="utf-8")
    api_versions = dict(re.findall(r"'([\d.]+)': '([^']+)'", (ROOT / "common.gradle").read_text(encoding="utf-8")))
    api_version = api_versions[args.version]
    api_jars = [path for path in (Path.home() / f".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/{api_version}").glob("*/*.jar")
                if not path.name.endswith(("-sources.jar", "-javadoc.jar"))]
    if len(api_jars) != 1:
        raise RuntimeError(f"Expected one cached Fabric API jar for {args.version}, found {api_jars}")
    (game / "mods").mkdir()
    for extra in args.extra_mod:
        shutil.copy2(extra, game / "mods" / extra.name)
    shutil.copy2(api_jars[0], game / "mods" / api_jars[0].name)
    (game / "options.txt").write_text("onboardAccessibility:false\nfullscreen:false\nmaxFps:60\nrenderDistance:2\nsoundCategory_master:0.0\n", encoding="utf-8")
    # Exercise migration and custom IME settings with the master switch disabled.
    (config_dir / "halfmasa.json").write_text(json.dumps({"ConfigVersion": 46,
        "Ported": {"contingameIme": {"enabled": False, "hotkey": {"keys": ""}},
                   "imeRenderStyle": "mod", "imeHideVanillaPreedit": True, "imeStatusIndicator": True},
        "Disabled": {"entityAggregationThreshold": 10, "entityAggregationRadius": 2.0,
                     "entityAggregationScanInterval": 7, "entityAggregationLabelPosition": "side",
                     "entityAggregationListMode": "blacklist", "entityAggregationBlacklist": ["minecraft:item"]}}, indent=2), encoding="utf-8")
    env = os.environ.copy()
    env["JAVA_HOME"] = str(args.java_home)
    log_path = directory / "launcher.log"
    command = [str(ROOT / "gradlew.bat"), f":{args.version}:runClient", "--no-daemon", "--console=plain",
               "--init-script", str(ROOT / "scripts/gradle/startup-smoke.init.gradle"),
               f"-Pstartup_smoke_version={args.version}", f"-Pstartup_smoke_run_dir={game}",
               f"-Pstartup_smoke_mod={smoke_mod}"]
    if args.artifact:
        command = production_command(args, game, smoke_mod)
        command[1:1] = args.jvm_arg
    start = time.monotonic()
    timed_out = False
    with log_path.open("w", encoding="utf-8") as output:
        process = subprocess.Popen(command, cwd=ROOT, env=env, stdin=subprocess.DEVNULL,
                                   stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            timed_out = True
            subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], capture_output=True, check=False)
            code = process.wait(timeout=30)
    log = log_path.read_text(encoding="utf-8", errors="replace")
    title_ready = "HALFMASA_SMOKE TITLE_READY" in log
    marker_pass = "HALFMASA_SMOKE PASS" in log
    patterns = r"Mixin apply.*failed|InvalidInjectionException|InjectionError|NoClassDefFoundError|Exception in thread|HalfMasa startup smoke failed"
    errors = [line for line in log.splitlines() if re.search(patterns, line)]
    error_lines = [line for line in log.splitlines() if "/ERROR]" in line]
    native_toggle_pass = (f"HALFMASA_SMOKE IME_TOGGLE native_installed={str(args.version.startswith('1.21.')).lower()}" in log)
    migrated = json.loads((config_dir / "halfmasa.json").read_text(encoding="utf-8"))
    values = migrated.get("Disabled", {})
    migration_pass = (migrated.get("ConfigVersion", 0) >= 47 and values.get("entityAggregationThreshold") == 11
                      and values.get("itemAggregationRadius") == 2.0 and values.get("itemAggregationScanInterval") == 7
                      and values.get("itemAggregationLabelPosition") == "side"
                      and values.get("itemAggregationListMode") == "whitelist")
    input_mixins_pass = "HALFMASA_SMOKE INPUT_MIXINS_PASS" in log
    despawn_pass = "HALFMASA_SMOKE DESPAWN_MIXINS_AND_CONFIG_PASS" in log
    ported_features_pass = "HALFMASA_SMOKE PORTED_FEATURES_PASS" in log
    passed = code == 0 and marker_pass and not timed_out and not errors and migration_pass and native_toggle_pass and input_mixins_pass and despawn_pass and ported_features_pass
    report = {"game_version": args.version, "started_at": now.isoformat(timespec="seconds"),
              "launch_mode": "production" if args.artifact else "development", "artifact": str(args.artifact) if args.artifact else None,
              "extra_mods": [str(path.resolve()) for path in args.extra_mod],
              "duration_seconds": round(time.monotonic() - start, 1), "result": "PASS" if passed else "FAIL",
              "exit_code": code, "timed_out": timed_out, "title_screen_ready": title_ready,
              "config_and_ime_pass": "HALFMASA_SMOKE CONFIG_AND_IME_PASS" in log,
              "input_mixins_pass": input_mixins_pass,
              "ported_features_pass": ported_features_pass,
              "clipboard_initialization_disabled": args.disable_clipboard_init,
              "despawn_mixins_and_config_pass": despawn_pass,
              "migration_pass": migration_pass, "native_toggle_pass": native_toggle_pass,
              "fatal_errors": errors, "non_fatal_error_lines": error_lines, "launcher_log": str(log_path),
              "game_log": str(game / "logs/latest.log"), "java_home": str(args.java_home)}
    report_path = directory / "report.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"STARTUP {args.version}: {report['result']} ({report['duration_seconds']}s)", flush=True)
    print(report_path, flush=True)
    if not passed:
        print("\n".join(log.splitlines()[-100:]), flush=True)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

"""Check real configuration-screen lifecycles and resource/chunk rebuilds on each target."""
import argparse
from datetime import datetime
import json
from pathlib import Path
import subprocess
import sys
import time
import zipfile

import run_entity_render_disable_smoke as fixtures
import run_startup_smoke as startup
import port_smoke_compat as port_compat

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path('C:/Program Files/Java/jdk-25.0.3')
SOURCE = r'''package io.github.halfmasa.configreloadsmoke;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.*;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
public class ConfigReloadSmoke implements ClientModInitializer {
 private static ConfigReloadSmoke instance;
 public static int reloads;
 public static int chunks;
 private int stage=-1,checks,frames;
 private static final boolean DIAGNOSTIC=DIAGNOSTIC_VALUE;
 @Override public void onInitializeClient(){instance=this;}
 public static void reload(){reloads++;new Exception("CONFIG_RELOAD_CALL "+reloads).printStackTrace(System.out);}
 public static void chunks(){chunks++;new Exception("CONFIG_RELOAD_CHUNKS "+chunks).printStackTrace(System.out);}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private static Field field(Class<?> c,String n)throws Exception{for(;c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(n);}
 private static Method method(Class<?> c,String n)throws Exception{for(;c!=null;c=c.getSuperclass())try{var m=c.getDeclaredMethod(n);m.setAccessible(true);return m;}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(n);}
 private void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);System.out.println("CONFIG_RELOAD_CHECK "+why);}
 private HalfMasaConfigScreen open(){var ui=new HalfMasaConfigScreen();GuiBase.openGui(ui);return ui;}
 private void close(Minecraft mc){MinecraftClientCompat.setScreen(mc,new TitleScreen());}
 private WidgetListConfigOptions list(HalfMasaConfigScreen ui)throws Exception{return (WidgetListConfigOptions)method(ui.getClass(),"getListWidget").invoke(ui);}
 @SuppressWarnings("unchecked") private void layout(Minecraft mc,HalfMasaConfigScreen ui)throws Exception{
  if(DIAGNOSTIC)return;
  var widgets=(List<WidgetBase>)field(ui.getClass(),"widgets").get(ui);WidgetLabel hint=null;
  for(var widget:widgets)if(widget instanceof WidgetLabel label&&((List<String>)field(label.getClass(),"labels").get(label)).contains("Click [+] to expand sub-settings"))hint=label;
  check(hint!=null,"expansion hint is a visible top-level label");check(hint.getY()>10&&hint.getY()+hint.getHeight()<42,"hint has its own line above tabs");check(field(list(ui).getClass(),"posY").getInt(list(ui))>=68,"configuration rows remain below hint and tabs");
 }
 private void tick(Minecraft mc){try{
  if(mc.gui.overlay()!=null)return;
  if(stage==-1){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen))return;stage=0;mc.createWorldOpenFlows().createFreshLevel("config-reload-fixture",new LevelSettings("Config Reload Fixture",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;}
  if(mc.level==null||mc.player==null||mc.level.getGameTime()<40)return;
  if(++frames<8)return;frames=0;
  if(stage==0){reloads=0;chunks=0;if(!DIAGNOSTIC){var manager=mc.getLanguageManager();var language=manager.getSelected();manager.setSelected("zh_cn");manager.onResourceManagerReload(mc.getResourceManager());var text=Component.translatable("halfmasa.config.client.name.bossBarScale");check(text.getString().equals("Boss 栏大小"),"persisted spacing enabled on first language load");Configs.CJK_LATIN_SPACING.setBooleanValue(false);check(text.getString().equals("Boss栏大小"),"first toggle after startup removes persisted spacing");Configs.CJK_LATIN_SPACING.setBooleanValue(true);check(text.getString().equals("Boss 栏大小"),"first toggle back restores spacing");manager.setSelected(language);manager.onResourceManagerReload(mc.getResourceManager());}layout(mc,open());stage++;return;}
  if(stage==1){net.minecraft.client.Screenshot.grab(mc,false);stage=10;return;}
  if(stage==10){close(mc);System.out.println("CONFIG_RELOAD_OBSERVE unchangedClose="+reloads+" chunks="+chunks);if(!DIAGNOSTIC)check(reloads==0&&chunks==0,"opening and closing unchanged configuration causes no resource or chunk reload");stage=2;return;}
  if(stage==2){var ui=open();Configs.BOAT_VIEW_360.toggleBooleanValue();list(ui).markConfigsModified();close(mc);System.out.println("CONFIG_RELOAD_OBSERVE unrelatedToggleClose="+reloads+" chunks="+chunks);if(!DIAGNOSTIC)check(reloads==0&&chunks==0,"unrelated toggle and save cause no resource or chunk reload");stage++;return;}
  if(stage==3){
   for(boolean master:List.of(false,true))for(boolean translations:List.of(false,true)){
    Configs.CJK_LATIN_SPACING.setBooleanValue(master);Configs.CJK_LATIN_SPACING_TRANSLATIONS.setBooleanValue(translations);
    var ui=open();list(ui).markConfigsModified();close(mc);
    if(!DIAGNOSTIC)check(reloads==0,"spacing state "+master+"/"+translations+" saves without full reload");
   }
   if(!DIAGNOSTIC){check(chunks==0,"spacing updates and saves cause no chunk rebuilds");Configs.DISABLE_FLUID_RENDERING.onValueChanged();Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.onValueChanged();check(chunks==0,"duplicate fluid callbacks do not rebuild chunks");
    Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.toggleBooleanValue();check(chunks==0,"child toggle cannot affect rendering while all fluids are hidden");Configs.DISABLE_FLUID_RENDERING.setBooleanValue(false);check(chunks==1,"actual fluid visibility change rebuilds chunks exactly once");
    var ui=open();list(ui).markConfigsModified();close(mc);check(chunks==1,"saving changed fluid visibility does not rebuild again");Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.setBooleanValue(true);check(chunks==2,"actual flowing-fluid visibility change rebuilds exactly once");
    new Configs().save();var path=Configs.getHalfMasaDirectory().resolve("halfmasa.json");var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();var disabled=json.getAsJsonObject("Disabled");disabled.getAsJsonObject("disableFluidRendering").addProperty("enabled",true);disabled.getAsJsonObject("disableNonSourceFluidRendering").addProperty("enabled",false);java.nio.file.Files.writeString(path,json.toString());new Configs().load();
    check(chunks==3&&Configs.DISABLE_FLUID_RENDERING.getBooleanValue()&&!Configs.DISABLE_NON_SOURCE_FLUID_RENDERING.getBooleanValue(),"complete file reload applies final fluid state with one rebuild");new Configs().load();check(chunks==3,"unchanged file reload never rebuilds chunks");}
   stage++;return;
  }
  if(stage==4){
   var manager=mc.getLanguageManager();var selected=manager.getSelected();manager.setSelected("zh_cn");manager.onResourceManagerReload(mc.getResourceManager());
   var retained=Component.translatable("halfmasa.config.client.name.bossBarScale");
   Configs.CJK_LATIN_SPACING_TRANSLATIONS.setBooleanValue(true);Configs.CJK_LATIN_SPACING.setBooleanValue(false);var plain=retained.getString();
   Configs.CJK_LATIN_SPACING.setBooleanValue(true);var spaced=retained.getString();
   if(!DIAGNOSTIC){check(plain.equals("Boss栏大小")&&spaced.equals("Boss 栏大小"),"existing translatable component updates when spacing is enabled");Configs.CJK_LATIN_SPACING.setBooleanValue(false);check(plain.equals(retained.getString()),"spacing removal restores original translation");
    var language=Language.getInstance();Configs.CJK_LATIN_SPACING_TRANSLATIONS.toggleBooleanValue();check(Language.getInstance()==language,"inactive child changes do not refresh language");
    Configs.CJK_LATIN_SPACING.onValueChanged();check(Language.getInstance()==language,"redundant callbacks do not refresh language");check(reloads==0,"entire toggle and close regression performs zero full reloads");check(chunks==3,"language changes do not rebuild chunk meshes");}
   manager.setSelected(selected);manager.onResourceManagerReload(mc.getResourceManager());
   System.out.println("CONFIG_RELOAD_PASS checks="+checks+" fullReloads="+reloads+" diagnostic="+DIAGNOSTIC);stage++;mc.stop();
  }
 }catch(Throwable error){throw new IllegalStateException("Config reload smoke stage="+stage,error);}}
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--version', default='26.3')
    parser.add_argument('--diagnostic', action='store_true')
    parser.add_argument('--timeout', type=int, default=150)
    args = parser.parse_args()
    args.java_home = ROOT / 'build/toolchains/jdk-21.0.12.1+1' if args.version.startswith('1.21.') else JAVA
    args.artifact = args.artifact.resolve()
    directory = ROOT / 'build/config-reload-tests' / datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    classes = directory / 'classes'; classes.mkdir(parents=True)
    cp = port_compat.classpath(args.version, args.artifact)
    frame = fixtures.FRAME.replace('entitydisablesmoke', 'configreloadsmoke').replace('EntityDisableSmoke', 'ConfigReloadSmoke')
    frame = frame.replace('@Mixin(Minecraft.class) public class FrameMixin {', '''@Mixin(Minecraft.class) public class FrameMixin {
 @Inject(method="reloadResourcePacks(ZLnet/minecraft/client/GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;",at=@At("HEAD"))
 private void reload(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<?> ci){ConfigReloadSmoke.reload();}
''')
    clipboard = fixtures.CLIPBOARD.replace('entitydisablesmoke', 'configreloadsmoke')
    chunks = r'''package io.github.halfmasa.configreloadsmoke.mixin;
import io.github.halfmasa.configreloadsmoke.ConfigReloadSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(net.minecraft.client.renderer.extract.LevelExtractor.class) public class ChunkProbeMixin {
 @Inject(method="allChanged",at=@At("HEAD")) private void changed(CallbackInfo ci){ConfigReloadSmoke.chunks();}
}'''
    if args.version not in ('26.3', '26.2'):
        chunks = chunks.replace('net.minecraft.client.renderer.extract.LevelExtractor', 'net.minecraft.client.renderer.LevelRenderer')
    if args.version not in ('26.3', '26.2'):
        frame = frame.replace('Lnet/minecraft/client/GameLoadCookie;', 'Lnet/minecraft/client/Minecraft$GameLoadCookie;')
    sources = []
    for name, source in [('ConfigReloadSmoke', SOURCE.replace('DIAGNOSTIC_VALUE', str(args.diagnostic).lower())), ('FrameMixin', frame), ('ClipboardMixin', clipboard), ('ChunkProbeMixin', chunks)]:
        path = directory / (name + '.java'); path.write_text(port_compat.source(source, args.version, cp), encoding='utf-8'); sources.append(str(path))
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Duser.language=en', '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        print(result.stderr.decode('utf-8', errors='replace')); return 1
    mod = directory / 'config-reload-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'): jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_config_reload_smoke', 'version': '1.0', 'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.configreloadsmoke.ConfigReloadSmoke']}, 'mixins': ['config-reload-smoke.mixins.json']}))
        jar.writestr('config-reload-smoke.mixins.json', json.dumps({'required': True, 'package': 'io.github.halfmasa.configreloadsmoke.mixin', 'compatibilityLevel': 'JAVA_21' if args.version.startswith('1.21.') else 'JAVA_25', 'client': ['FrameMixin', 'ClipboardMixin', 'ChunkProbeMixin'], 'injectors': {'defaultRequire': 1}}))
    mod = port_compat.remap_fixture(args.version, args.java_home, mod, cp, directory)
    game = directory / 'game'; config = game / 'config/halfmasa'; config.mkdir(parents=True); (game / 'mods').mkdir()
    (game / 'config/malilib.json').write_text('{}\n')
    # A persisted enabled value exercises MaLiLib's last-value/dirty callbacks when the GUI closes.
    (config / 'halfmasa.json').write_text(json.dumps({'ConfigVersion': 55, 'Ported': {'cjkLatinSpacing': {'enabled': True, 'hotkey': {'keys': ''}}, 'cjkLatinSpacingTranslations': True}, 'Disabled': {'disableFluidRendering': {'enabled': True, 'hotkey': {'keys': ''}}, 'disableNonSourceFluidRendering': {'enabled': True, 'hotkey': {'keys': ''}}}}))
    (game / 'options.txt').write_text('onboardAccessibility:false\nenableVsync:false\nmaxFps:30\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:en_us\ntutorialStep:none\n')
    command = startup.production_command(args, game, mod)
    command[1:1] = ['-Djava.awt.headless=true', '-XX:ActiveProcessorCount=2']
    command[command.index('--width')+1] = '1120'; command[command.index('--height')+1] = '720'
    log = directory / 'launcher.log'; start = time.monotonic(); timed_out = False
    with log.open('w', encoding='utf-8') as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try: code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            timed_out = True; subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True); code = process.wait(timeout=30)
    text = log.read_text(encoding='utf-8', errors='replace')
    report = {'result': 'PASS' if code == 0 and not timed_out and 'CONFIG_RELOAD_PASS' in text else 'FAIL', 'game_version': args.version, 'diagnostic': args.diagnostic, 'artifact': str(args.artifact), 'exit_code': code, 'timed_out': timed_out, 'duration_seconds': round(time.monotonic() - start, 1), 'checks': [line for line in text.splitlines() if 'CONFIG_RELOAD_' in line], 'log': str(log), 'screenshots': [str(p) for p in (game / 'screenshots').glob('*.png')]}
    path = directory / 'report.json'; path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8'); print(report['result'], path)
    if report['result'] != 'PASS': print('\n'.join(text.splitlines()[-45:]))
    return 0 if report['result'] == 'PASS' else 1


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    raise SystemExit(main())

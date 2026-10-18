"""Exercise actual 1.21.1 book editing with and without optional Caxton."""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import time
import zipfile

import port_smoke_compat as compat
import run_entity_render_disable_smoke as fixtures
import run_startup_smoke as startup
import smoke_remap

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''package io.github.halfmasa.caxtonbooksmoke;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.feature.BookEditDisplayCacheAccess;
import io.github.halfmasa.xaerobinding.feature.CjkLatinSpacing;
public class BookSmoke implements ClientModInitializer {
 private static BookSmoke instance;
 private int stage=-1,frames,checks,test;
 private BookEditScreen screen;
 private ItemStack book;
 private final boolean caxton=FabricLoader.getInstance().isModLoaded("caxton");
 private static final String[] TEXTS={"", "中文abc123测试", "中文abc\n第二行XYZ", "汉\uD83D\uDE00字ABC", "中文abcdef0123456789 ".repeat(12).stripTrailing()};
 public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private static Field field(Class<?> c,String n)throws Exception{var f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 private static Object get(Object o,String n)throws Exception{return field(o.getClass(),n).get(o);}
 private static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception{var m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(o,args);}
 private static Object call(Object o,String n)throws Exception{return call(o,n,new Class<?>[0]);}
 private void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);System.out.println("BOOK_CHECK "+why);}
 private TextFieldHelper edit()throws Exception{return (TextFieldHelper)get(screen,"pageEdit");}
 private String text()throws Exception{return (String)call(screen,"getCurrentPageText");}
 private Object cache()throws Exception{return call(screen,"getDisplayCache");}
 private void clear()throws Exception{call(screen,"clearDisplayCache");}
 private void setText(String value)throws Exception{call(screen,"setCurrentPageText",new Class<?>[]{String.class},value);edit().setCursorToEnd();clear();}
 private void tick(Minecraft mc){try{
  if(mc.getOverlay()!=null)return;
  if(stage==-1){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen))return;
   // Force the same early class transformation as mods that load book screens at startup.
   Class.forName(BookEditScreen.class.getName());
   System.out.println("BOOK_CLASS_LOADED caxton="+caxton);
   stage=0;mc.createWorldOpenFlows().createFreshLevel("book-compat-fixture",new LevelSettings("Book Compatibility",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),MinecraftClientCompat.getScreen(mc));return;
  }
  if(mc.level==null||mc.player==null||mc.level.getGameTime()<40)return;
  if(++frames<3)return;frames=0;
  if(stage==0){book=new ItemStack(Items.WRITABLE_BOOK);mc.player.setItemInHand(InteractionHand.MAIN_HAND,book);screen=new BookEditScreen(mc.player,book,InteractionHand.MAIN_HAND);MinecraftClientCompat.setScreen(mc,screen);
   check(Arrays.stream(screen.getClass().getDeclaredMethods()).anyMatch(m->m.getName().contains("prepareBookPage"))!=caxton,"spacing screen mixin follows layout ownership");
   check((cache() instanceof BookEditDisplayCacheAccess)!=caxton,"spacing cache mapping follows same ownership");
   if(caxton)check(Class.forName("xyz.flirora.caxton.layout.gui.BookEditScreenPageContentExt").isInstance(cache()),"Caxton page layout is active");stage=1;return;}
  if(stage==1){if(test>=TEXTS.length*4){stage=3;return;}
   Configs.CJK_LATIN_SPACING.setBooleanValue((test/TEXTS.length&1)!=0);
   Configs.CJK_LATIN_SPACING_BOOKS.setBooleanValue((test/TEXTS.length&2)!=0);
   setText(TEXTS[test%TEXTS.length]);stage=2;return;}
  if(stage==2){String original=TEXTS[test%TEXTS.length];boolean spaced=!caxton&&Configs.CJK_LATIN_SPACING.getBooleanValue()&&Configs.CJK_LATIN_SPACING_BOOKS.getBooleanValue();Object layout=cache();
   check(get(layout,"fullText").equals(spaced?CjkLatinSpacing.apply(original):original),"layout text case "+test);
   check(text().equals(original),"display never changes stored page case "+test);
   for(int index:new int[]{0,Math.min(1,original.length()),original.length()}){edit().setCursorPos(index);clear();layout=cache();Object cursor=get(layout,"cursor");var constructor=cursor.getClass().getDeclaredConstructor(int.class,int.class);constructor.setAccessible(true);
    // Hit-test the drawn text with its actual font metrics and line starts;
    // Caxton's cached cursor geometry is a separate implementation detail.
    String drawn=(String)get(layout,"fullText");int displayIndex=spaced?CjkLatinSpacing.applyWithMapping(original).toSpacedIndex(index):index;int[] starts=(int[])get(layout,"lineStarts");int line=0;while(line+1<starts.length&&starts[line+1]<=displayIndex)line++;
    Object click=constructor.newInstance(mc.font.width(drawn.substring(starts[line],displayIndex))+1,line*mc.font.lineHeight);int actual=(int)call(layout,"getIndexAtPosition",new Class<?>[]{net.minecraft.client.gui.Font.class,cursor.getClass()},mc.font,click);check(actual==index,"text hit-test returns source index "+index+" case "+test+" actual="+actual);}
   if(original.length()>2){edit().setSelectionRange(0,2);clear();check(java.lang.reflect.Array.getLength(get(cache(),"selection"))>0,"selection has visible geometry case "+test);screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE,0,0);check(text().equals(original.substring(2)),"selection deletion uses source indices case "+test);}
   test++;stage=1;return;}
  if(stage==3){setText("");for(char c:"中文ABC".toCharArray())screen.charTyped(c,0);check(text().equals("中文ABC"),"native typing preserves source text");
   screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT,0,0);check(edit().getCursorPos()==4,"left arrow moves by source character");screen.charTyped('Z',0);check(text().equals("中文ABZC"),"typing at cursor inserts at source position");
   setText("中文ABC\n第二行DEF");edit().setCursorToStart();screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN,0,0);check(edit().getCursorPos()==6,"down arrow moves to next source line");screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_HOME,0,0);check(edit().getCursorPos()==6,"home keeps source line start");screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_END,0,0);check(edit().getCursorPos()==12,"end reaches source line end");
   setText("第一页ABC");call(screen,"pageForward");setText("第二页XYZ");call(screen,"pageBack");check(text().equals("第一页ABC"),"page navigation preserves first page");call(screen,"pageForward");check(text().equals("第二页XYZ"),"page navigation preserves second page");call(screen,"updateLocalCopy");var content=book.get(DataComponents.WRITABLE_BOOK_CONTENT);check(content!=null&&content.pages().size()==2&&content.pages().get(0).raw().equals("第一页ABC")&&content.pages().get(1).raw().equals("第二页XYZ"),"saved book stores original text without display spaces");
   Configs.CJK_LATIN_SPACING.setBooleanValue(false);check(CjkLatinSpacing.apply("中文ABC").equals("中文 ABC"),"unrelated spacing implementation remains available");stage=4;return;
  }
  if(stage==4){net.minecraft.client.Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();check(true,"all enabled mixin targets transform successfully");System.out.println("BOOK_PASS checks="+checks+" caxton="+caxton);mc.stop();stage=5;}
 }catch(Throwable error){throw new IllegalStateException("Book compatibility stage="+stage+" case="+test,error);}}
}'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', required=True, type=Path)
    parser.add_argument('--version', default='1.21.1', choices=['1.21.1','1.21.3','1.21.4','1.21.5'])
    parser.add_argument('--caxton', type=Path)
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    args.loader_version = '0.19.5'
    args.java_home = ROOT / 'build/toolchains/jdk-21.0.12.1+1'
    args.artifact = args.artifact.resolve()
    directory = ROOT / 'build/caxton-book-tests' / datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    classes = directory / 'classes'
    classes.mkdir(parents=True)
    cp = compat.classpath(args.version, args.artifact)
    mapping = smoke_remap.mappings(args.version, cp)
    # Only reflection strings need this adjustment; TinyRemapper handles normal calls.
    owners = {'net/minecraft/client/gui/screens/inventory/BookEditScreen',
              'net/minecraft/client/gui/screens/inventory/BookEditScreen$DisplayCache',
              'net/minecraft/client/gui/screens/inventory/BookEditScreen$Pos2i'}
    source = SOURCE.replace('new GameRules()', 'new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())') if args.version != '1.21.1' else SOURCE
    if args.version != '1.21.1':
        source = source.replace('access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()', 'WorldPresets::createFlatWorldDimensions')
        source = source.replace('new BookEditScreen(mc.player,book,InteractionHand.MAIN_HAND)', 'new BookEditScreen(mc.player,book,InteractionHand.MAIN_HAND,book.get(DataComponents.WRITABLE_BOOK_CONTENT))')
    owner = None
    for line in mapping.read_text(encoding='utf-8').splitlines():
        cols = line.split('\t')
        if cols[0] == 'c':
            owner = cols[-1]
        elif owner in owners and len(cols) > 5 and cols[1] in ('m', 'f'):
            source = source.replace('"' + cols[-1] + '"', '"' + cols[-2] + '"')
    sources = []
    for name, body in [('BookSmoke', source),
                       ('FrameMixin', fixtures.FRAME.replace('entitydisablesmoke', 'caxtonbooksmoke').replace('EntityDisableSmoke', 'BookSmoke')),
                       ('ClipboardMixin', fixtures.CLIPBOARD.replace('entitydisablesmoke', 'caxtonbooksmoke'))]:
        path = directory / (name + '.java')
        path.write_text(body, encoding='utf-8')
        sources.append(str(path))
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Duser.language=en', '-J-Dfile.encoding=UTF-8', '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        diagnostic = result.stderr.decode('utf-8', errors='replace')
        (directory / 'compile.log').write_text(diagnostic, encoding='utf-8')
        print(diagnostic)
        return 1
    mod = directory / 'book-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_book_smoke', 'version': '1.0', 'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.caxtonbooksmoke.BookSmoke']}, 'mixins': ['book-smoke.mixins.json']}))
        jar.writestr('book-smoke.mixins.json', json.dumps({'required': True, 'package': 'io.github.halfmasa.caxtonbooksmoke.mixin', 'compatibilityLevel': 'JAVA_21', 'client': ['FrameMixin', 'ClipboardMixin'], 'injectors': {'defaultRequire': 1}}))
    mod = compat.remap_fixture(args.version, args.java_home, mod, cp, directory)
    game = directory / 'game'
    (game / 'config/halfmasa').mkdir(parents=True)
    (game / 'mods').mkdir()
    (game / 'config/malilib.json').write_text('{}\n')
    (game / 'options.txt').write_text('onboardAccessibility:false\nenableVsync:false\nmaxFps:30\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\ntutorialStep:none\n')
    if args.caxton:
        shutil.copy2(args.caxton, game / 'mods' / args.caxton.name)
    api = next(p for p in (Path.home() / f'.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/{compat.api_version(args.version)}').glob('*/*.jar') if not p.name.endswith(('-sources.jar', '-javadoc.jar')))
    shutil.copy2(api, game / 'mods' / api.name)
    command = startup.production_command(args, game, mod)
    command[1:1] = ['-Djava.awt.headless=true', '-XX:ActiveProcessorCount=2']
    command[command.index('--width')+1] = '1120'
    command[command.index('--height')+1] = '720'
    log = directory / 'launcher.log'
    start = time.monotonic()
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
    report = {'result': 'PASS' if code == 0 and not timed_out and 'BOOK_PASS' in text else 'FAIL', 'game_version': args.version, 'loader_version': args.loader_version, 'artifact': str(args.artifact), 'artifact_sha256': hashlib.sha256(args.artifact.read_bytes()).hexdigest(), 'caxton': str(args.caxton.resolve()) if args.caxton else None, 'caxton_sha512': hashlib.sha512(args.caxton.read_bytes()).hexdigest() if args.caxton else None, 'exit_code': code, 'timed_out': timed_out, 'duration_seconds': round(time.monotonic()-start, 1), 'checks': [line for line in text.splitlines() if 'BOOK_CHECK' in line or 'BOOK_PASS' in line], 'log': str(log), 'screenshots': [str(p) for p in (game / 'screenshots').glob('*.png')]}
    path = directory / 'report.json'
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(report['result'], path)
    if report['result'] != 'PASS':
        print('\n'.join(text.splitlines()[-45:]))
    return 0 if report['result'] == 'PASS' else 1


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    raise SystemExit(main())

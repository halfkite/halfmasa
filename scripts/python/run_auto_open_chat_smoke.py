"""Exercise native chat input, screen lifecycle and config persistence on each target."""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time
import zipfile

import run_startup_smoke as startup
import port_smoke_compat as port_compat
import smoke_remap

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''package io.github.halfmasa.autochatsmoke;
import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;
import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.AutoOpenChat;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLMouse;
public class AutoChatSmoke implements ClientModInitializer {
 static AutoChatSmoke instance;
 public static final List<String> sent=new ArrayList<>();
 int stage,checks;long since;
 Screen other;ChatScreen chat;
 public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;System.out.println("AUTO_CHAT_CHECK "+why);}
 static Field field(Class<?> c,String name)throws Exception{for(;c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
 EditBox input()throws Exception{return (EditBox)field(ChatScreen.class,"input").get(chat);}
 void key(int code){chat.keyPressed(new KeyEvent(code,0,0));}
 void send(String text)throws Exception{input().setValue(text);key(InputConstants.KEY_RETURN);}
 void go(int value){stage=value;since=net.minecraft.util.Util.getMillis();}
 boolean waitChat(Minecraft mc){if(MinecraftClientCompat.getScreen(mc) instanceof ChatScreen c){chat=c;return true;}return false;}
 void clear(Minecraft mc){MinecraftClientCompat.setScreen(mc,null);}
 void move(Minecraft mc,double x,double y){
  SDLMouse.SDL_WarpMouseInWindow(mc.getWindow().handle(),(float)x,(float)y);
  mc.mouseHandler.setIgnoreFirstMove();mc.mouseHandler.onMove(mc.getWindow().handle(),x,y,0,0);
 }
 void pointer(Minecraft mc,double x,double y,String why){
  check(Math.abs(mc.mouseHandler.xpos()-x)<2&&Math.abs(mc.mouseHandler.ypos()-y)<2,
   why+" actual="+mc.mouseHandler.xpos()+","+mc.mouseHandler.ypos()+" expected="+x+","+y);
 }
 void centered(Minecraft mc,String why){pointer(mc,mc.getWindow().getScreenWidth()/2.0,mc.getWindow().getScreenHeight()/2.0,why);}
 void tick(Minecraft mc){try{
  long elapsed=net.minecraft.util.Util.getMillis()-since;
  if(stage!=0&&elapsed>15000)throw new AssertionError("stage timeout "+stage+" screen="+MinecraftClientCompat.getScreen(mc));
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
   check(!Configs.AUTO_OPEN_CHAT.getBooleanValue()&&Configs.AUTO_OPEN_CHAT_STOP_MESSAGES.getStrings().isEmpty(),"master defaults off and stop list defaults empty");
   check(Configs.AUTO_OPEN_CHAT.getKeybind().getStringValue().isEmpty(),"hotkey defaults unbound");
   check(Configs.AUTO_OPEN_CHAT_RESTORE_POINTER.getBooleanValue(),"pointer restoration defaults enabled");
   Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);
   check(Configs.getExpansionParent(Configs.AUTO_OPEN_CHAT_STOP_MESSAGES)==Configs.AUTO_OPEN_CHAT,"stop list belongs to master expansion");
   check(Configs.getExpansionParent(Configs.AUTO_OPEN_CHAT_RESTORE_POINTER)==Configs.AUTO_OPEN_CHAT,"pointer toggle belongs to master expansion");
   check(!Configs.getClientView().contains(Configs.AUTO_OPEN_CHAT_RESTORE_POINTER),"collapsed pointer child is hidden");
   check(!Configs.getClientView().contains(Configs.AUTO_OPEN_CHAT_STOP_MESSAGES),"collapsed child is hidden");
   Configs.AUTO_OPEN_CHAT_EXPANDED.setBooleanValue(true);
   check(Configs.getClientView().contains(Configs.AUTO_OPEN_CHAT_STOP_MESSAGES),"expanded child is visible");
   check(Configs.getClientView().contains(Configs.AUTO_OPEN_CHAT_RESTORE_POINTER),"expanded pointer child is visible");
   Configs.AUTO_OPEN_CHAT.setBooleanValue(true);AutoOpenChat.getInstance().onClientTick(mc);
   check(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen,"main menu is never replaced");
   Configs.AUTO_OPEN_CHAT.setBooleanValue(false);go(1);
   mc.createWorldOpenFlows().createFreshLevel("auto-chat-fixture",new LevelSettings("Auto Chat Fixture",GameType.CREATIVE,
    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
    new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;
  }
  if((mc.player==null||mc.level==null)&&stage!=25)return;
  if(stage==1){if(mc.level.getGameTime()<30)return;clear(mc);go(2);return;}
  if(stage==2){if(elapsed<300)return;
   check(MinecraftClientCompat.getScreen(mc)==null,"disabled feature leaves gameplay alone");
   other=new PauseScreen(false);MinecraftClientCompat.setScreen(mc,other);Configs.AUTO_OPEN_CHAT.setBooleanValue(true);go(3);return;}
  if(stage==3){if(elapsed<300)return;check(MinecraftClientCompat.getScreen(mc)==other,"existing UI is not replaced");
   check(sent.isEmpty(),"opening chat never sends messages");clear(mc);go(4);return;}
  if(stage==4){if(!waitChat(mc))return;check(true,"registered client tick opens native chat after UI closes");
   Configs.AUTO_OPEN_CHAT_STOP_MESSAGES.setStrings(List.of("quit","Stop","/say chat-stop"," ",""));
   check(mc.getWindow().isFocused(),"game window has focus for real pointer checks");
   move(mc,70,80);send("ordinary message");go(5);return;}
  if(stage==5){if(!waitChat(mc)||elapsed<150)return;
   check(Configs.AUTO_OPEN_CHAT.getBooleanValue()&&sent.contains("ordinary message"),"ordinary message is sent and chat reopens");
   pointer(mc,70,80,"pointer restored after ordinary submission");
   move(mc,120,90);go(50);return;}
  if(stage==50){if(elapsed<200)return;pointer(mc,120,90,"later user movement is not overwritten");send("QUIT");go(6);return;}
  if(stage==6){if(!waitChat(mc)||elapsed<150)return;check(Configs.AUTO_OPEN_CHAT.getBooleanValue(),"stop matching is case sensitive");
   pointer(mc,120,90,"next submission remembers the latest pointer");send("please quit");go(7);return;}
  if(stage==7){if(!waitChat(mc)||elapsed<150)return;check(Configs.AUTO_OPEN_CHAT.getBooleanValue(),"substring does not stop automatic chat");
   int before=sent.size();send("   ");check(sent.size()==before,"blank submission is not sent");go(8);return;}
  if(stage==8){if(!waitChat(mc)||elapsed<150)return;check(Configs.AUTO_OPEN_CHAT.getBooleanValue(),"blank stop entries never match");send("  quit   ");go(9);return;}
  if(stage==9){if(elapsed<300)return;
   check(!Configs.AUTO_OPEN_CHAT.getBooleanValue()&&MinecraftClientCompat.getScreen(mc)==null,"normalized matching message stops reopening");
   check(sent.contains("quit"),"matching stop message still uses native send");
   var json=JsonParser.parseString(Files.readString(Configs.getHalfMasaDirectory().resolve("halfmasa.json"))).getAsJsonObject();
   check(!json.getAsJsonObject("Client").getAsJsonObject("autoOpenChat").get("enabled").getAsBoolean(),"stop persists disabled master to disk");
   check(json.getAsJsonObject("Client").get("autoOpenChatRestorePointer").getAsBoolean(),"pointer child is persisted enabled");
   Configs.AUTO_OPEN_CHAT.setBooleanValue(true);go(10);return;}
  if(stage==10){if(!waitChat(mc))return;check(true,"manual re-enable starts a new run");input().setValue("/gam");go(11);return;}
  if(stage==11){if(elapsed<300)return;
   var suggestions=(CommandSuggestions)field(ChatScreen.class,"commandSuggestions").get(chat);
   suggestions.showSuggestions(false);if(field(CommandSuggestions.class,"suggestions").get(suggestions)==null)return;
   key(InputConstants.KEY_ESCAPE);
   check(MinecraftClientCompat.getScreen(mc)==chat&&Configs.AUTO_OPEN_CHAT.getBooleanValue(),"Esc dismissing real command suggestions does not stop");
   check(field(CommandSuggestions.class,"suggestions").get(suggestions)==null,"first Esc only closes suggestions");
   key(InputConstants.KEY_ESCAPE);go(12);return;}
  if(stage==12){if(elapsed<300)return;check(!Configs.AUTO_OPEN_CHAT.getBooleanValue()&&MinecraftClientCompat.getScreen(mc)==null,"Esc actually closing chat stops reopening");
   Configs.AUTO_OPEN_CHAT.setBooleanValue(true);go(13);return;}
  if(stage==13){if(!waitChat(mc))return;
   move(mc,90,75);other=new PauseScreen(false);MinecraftClientCompat.setScreen(mc,other);move(mc,200,120);go(14);return;}
  if(stage==14){if(elapsed<300)return;
   check(Configs.AUTO_OPEN_CHAT.getBooleanValue()&&MinecraftClientCompat.getScreen(mc)==other,"interrupted chat waits without disabling");
   pointer(mc,200,120,"other screen pointer is not changed");clear(mc);go(15);return;}
  if(stage==15){if(!waitChat(mc))return;check(true,"automatic chat resumes after interrupting UI closes");
   pointer(mc,90,75,"interrupted chat restores its own pointer");send("/say chat-stop");go(16);return;}
  if(stage==16){if(elapsed<300)return;
   check(!Configs.AUTO_OPEN_CHAT.getBooleanValue()&&MinecraftClientCompat.getScreen(mc)==null,"matching slash command stops reopening");
   check(sent.contains("/say chat-stop"),"matching command is sent through native command path");
   check(mc.gui.hud.getChat().getRecentChat().contains("quit"),"native chat history is preserved");
   Configs.AUTO_OPEN_CHAT_RESTORE_POINTER.setBooleanValue(false);Configs.AUTO_OPEN_CHAT.setBooleanValue(true);go(17);return;}
  if(stage==17){if(!waitChat(mc))return;move(mc,70,80);send("pointer disabled");go(18);return;}
  if(stage==18){if(!waitChat(mc)||elapsed<150)return;centered(mc,"disabled pointer child preserves vanilla centering");
   Configs.AUTO_OPEN_CHAT_RESTORE_POINTER.setBooleanValue(true);move(mc,110,80);
   other=new PauseScreen(false);MinecraftClientCompat.setScreen(mc,other);
   Configs.AUTO_OPEN_CHAT_RESTORE_POINTER.setBooleanValue(false);Configs.AUTO_OPEN_CHAT_RESTORE_POINTER.setBooleanValue(true);
   clear(mc);go(19);return;}
  if(stage==19){if(!waitChat(mc)||elapsed<150)return;centered(mc,"toggling pointer child clears the old position");
   move(mc,80,85);send("quit");go(20);return;}
  if(stage==20){if(elapsed<150)return;check(!Configs.AUTO_OPEN_CHAT.getBooleanValue(),"matching message stops pointer session");
   Configs.AUTO_OPEN_CHAT.setBooleanValue(true);go(21);return;}
  if(stage==21){if(!waitChat(mc))return;centered(mc,"new run does not reuse old pointer");
   move(mc,mc.getWindow().getScreenWidth()-10,mc.getWindow().getScreenHeight()-10);
   other=new PauseScreen(false);MinecraftClientCompat.setScreen(mc,other);mc.getWindow().setWindowed(400,240);go(22);return;}
  if(stage==22){if(elapsed<300||mc.getWindow().getScreenWidth()>400||mc.getWindow().getScreenHeight()>240)return;
   clear(mc);go(23);return;}
  if(stage==23){if(!waitChat(mc)||elapsed<150)return;
   pointer(mc,mc.getWindow().getScreenWidth()-1,mc.getWindow().getScreenHeight()-1,"window shrink clamps pointer to current bounds");
   move(mc,70,65);other=new PauseScreen(false);MinecraftClientCompat.setScreen(mc,other);
   Configs.AUTO_OPEN_CHAT.setBooleanValue(false);Configs.AUTO_OPEN_CHAT.setBooleanValue(true);clear(mc);go(24);return;}
  if(stage==24){if(!waitChat(mc)||elapsed<150)return;centered(mc,"master off and on clears interrupted pointer");
   move(mc,90,70);other=new PauseScreen(false);MinecraftClientCompat.setScreen(mc,other);
   go(25);mc.schedule(()->{mc.level.disconnect(net.minecraft.network.chat.Component.literal("HalfMasa fixture complete"));mc.disconnectWithSavingScreen();});return;}
  if(stage==25){if(mc.level!=null||mc.getSingleplayerServer()!=null||elapsed<300)return;
   check(field(AutoOpenChat.class,"pointer").get(AutoOpenChat.getInstance())==null,"real world disconnect clears saved pointer");
   check(!(MinecraftClientCompat.getScreen(mc) instanceof ChatScreen),"disconnect does not reopen chat");
   System.out.println("AUTO_CHAT_PASS checks="+checks+" submissions="+sent);go(99);mc.stop();return;}
 }catch(Throwable failure){throw new IllegalStateException("Auto chat smoke stage="+stage,failure);}}
}
'''
FRAME = r'''package io.github.halfmasa.autochatsmoke.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.autochatsmoke.AutoChatSmoke;
@Mixin(Minecraft.class) public class FrameMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void frame(boolean regularFrame,CallbackInfo ci){if(regularFrame)AutoChatSmoke.frame((Minecraft)(Object)this);}
}
'''
SENDS = r'''package io.github.halfmasa.autochatsmoke.mixin;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.autochatsmoke.AutoChatSmoke;
@Mixin(ClientPacketListener.class) public class SendMixin {
 @Inject(method="sendChat",at=@At("TAIL")) private void chat(String text,CallbackInfo ci){AutoChatSmoke.sent.add(text);}
 @Inject(method="sendCommand",at=@At("TAIL")) private void command(String text,CallbackInfo ci){AutoChatSmoke.sent.add("/"+text);}
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--version', default='26.3')
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Java/jdk-25.0.3'))
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    directory = ROOT / 'build/auto-chat-tests' / (args.version + '-' + datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
    classes = directory / 'classes'
    classes.mkdir(parents=True)
    cp = port_compat.classpath(args.version, args.artifact)
    sources = []
    for name, source in [('AutoChatSmoke', SOURCE), ('FrameMixin', FRAME), ('SendMixin', SENDS)]:
        source = port_compat.source(source, args.version, cp)
        if args.version != '26.3':
            if args.version == '26.1.2' or args.version.startswith('1.21.'):
                source = source.replace('mc.gui.hud.getChat()', 'mc.gui.getChat()')
            source = source.replace('import org.lwjgl.sdl.SDLMouse;', 'import org.lwjgl.glfw.GLFW;')
            start = source.find(' void move(Minecraft mc,double x,double y){')
            if start >= 0:
                end = source.index('\n void pointer(', start)
                source = source[:start] + ''' void move(Minecraft mc,double x,double y)throws Exception{
  GLFW.glfwSetCursorPos(mc.getWindow().handle(),x,y);
  field(net.minecraft.client.MouseHandler.class,"xpos").setDouble(mc.mouseHandler,x);
  field(net.minecraft.client.MouseHandler.class,"ypos").setDouble(mc.mouseHandler,y);
 }''' + source[end:]
            source = source.replace('mc.getWindow().isFocused()', 'mc.isWindowActive()')
            source = source.replace('mc.getWindow().setWindowed(400,240)', 'GLFW.glfwSetWindowSize(mc.getWindow().handle(),400,240)')
            if args.version.startswith('1.21.'):
                if tuple(map(int,args.version.split('.'))) < (1,21,10):
                    source = source.replace('import net.minecraft.client.input.KeyEvent;', '')
                    source = source.replace('chat.keyPressed(new KeyEvent(code,0,0))', 'chat.keyPressed(code,0,0)')
                    source = source.replace('mc.getWindow().handle()', 'mc.getWindow().getWindow()')
                if tuple(map(int,args.version.split('.'))) < (1,21,11):
                    source = source.replace('net.minecraft.util.Util', 'net.minecraft.Util')
                if args.version == '1.21.1':
                    source = source.replace('mc.schedule(', 'mc.tell(')
                if tuple(map(int,args.version.split('.'))) <= (1,21,5):
                    source = source.replace('mc.disconnectWithSavingScreen()', 'mc.disconnect()')
                    source = source.replace('mc.level.disconnect(net.minecraft.network.chat.Component.literal("HalfMasa fixture complete"))', 'mc.level.disconnect()')
                mapping = smoke_remap.mappings(args.version, cp)
                for owner, names in [('net/minecraft/client/gui/screens/ChatScreen',['input','commandSuggestions']),
                                     ('net/minecraft/client/gui/components/CommandSuggestions',['suggestions']),
                                     ('net/minecraft/client/MouseHandler',['xpos','ypos'])]:
                    for field_name in names:
                        mapped = smoke_remap.field_name(mapping, owner, field_name)
                        source = source.replace('"'+field_name+'"','"'+mapped+'"')
        path = directory / (name + '.java')
        path.write_text(source, encoding='utf-8')
        sources.append(str(path))
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-proc:none', '-encoding', 'UTF-8', '-cp', cp,
                             '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    mod = directory / 'auto-chat-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_auto_chat_smoke', 'version': '1.0',
            'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.autochatsmoke.AutoChatSmoke']},
            'mixins': ['auto-chat-smoke.mixins.json']}))
        jar.writestr('auto-chat-smoke.mixins.json', json.dumps({'required': True,
            'package': 'io.github.halfmasa.autochatsmoke.mixin', 'compatibilityLevel': 'JAVA_21' if args.version.startswith('1.21.') else 'JAVA_25',
            'client': ['FrameMixin', 'SendMixin'], 'injectors': {'defaultRequire': 1}}))
    mod = port_compat.remap_fixture(args.version, args.java_home, mod, cp, directory)
    game = directory / 'game'
    (game / 'mods').mkdir(parents=True)
    (game / 'config').mkdir()
    api_version = port_compat.api_version(args.version)
    api = next(p for p in (Path.home()/('.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/'+api_version)).glob('*/*.jar') if not p.name.endswith('-sources.jar'))
    shutil.copy2(api, game / 'mods' / api.name)
    (game / 'config/malilib.json').write_text('{}\n', encoding='utf-8')
    (game / 'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nnarrator:0\nlang:en_us\n'
        'guiScale:1\nenableVsync:false\nmaxFps:60\nrenderDistance:2\nsimulationDistance:5\n'
        'pauseOnLostFocus:false\nsoundCategory_master:0.0\n', encoding='utf-8')
    command = startup.production_command(args, game, mod)
    command[1:1] = ['-Djava.awt.headless=true', '-Dsun.java2d.d3d=false']
    log_path = directory / 'launcher.log'
    start = time.monotonic()
    with log_path.open('w', encoding='utf-8') as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output,
                                   stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        (directory / 'launcher.pid').write_text(str(process.pid), encoding='utf-8')
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
            code = process.wait(timeout=30)
    log = log_path.read_text(encoding='utf-8', errors='replace')
    passed = code == 0 and 'AUTO_CHAT_PASS' in log
    report = {'result': 'PASS' if passed else 'FAIL', 'version': args.version,
        'artifact': str(args.artifact), 'sha256': hashlib.sha256(args.artifact.read_bytes()).hexdigest(),
        'duration_seconds': round(time.monotonic() - start, 1), 'exit_code': code, 'log': str(log_path),
        'checks': [line for line in log.splitlines() if 'AUTO_CHAT_' in line]}
    report_path = directory / 'report.json'
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('AUTO CHAT:', report['result'], report_path, flush=True)
    if not passed:
        print('\n'.join(line for line in log.splitlines() if 'Caused by:' in line or 'AUTO_CHAT_' in line), flush=True)
    return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())

"""Exercise production IME gates, rendering and queued commits with UI boundary stubs."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
HARNESS = r'''
import java.lang.reflect.*;
import java.util.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.config.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
public class ImeCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void set(String name,Object value)throws Exception {var f=Ime261Compat.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
 static Object get(String name)throws Exception {var f=Ime261Compat.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
 public static void main(String[] args)throws Exception {
  var client=Minecraft.getInstance();var service=ImeService.getInstance();var graphics=new GuiGraphicsExtractor();
  Configs.IME_RENDER_STYLE.option=ImeStyle.MOD;Configs.IME_HIDE_VANILLA_PREEDIT.flag=true;Configs.IME_STATUS_INDICATOR.flag=true;
  if(args[0].equals("legacy_release")){
   var bridge=city.windmill.ingameime.client.jni.ExternalBaseIME.getInstance();bridge.installed=true;
   service.onClientTick(client);
   check(!bridge.isInitialized()&&bridge.releaseCount==1,"disabled feature leaves the legacy native hook installed");
   service.onClientTick(client);check(bridge.releaseCount==1,"inactive bridge is released twice");return;
  }
  if(args[0].equals("disabled")){
   service.onEditFocus(new EditBox(),true);set("typing",true);set("conversionMode",1);
   ImeOverlay.setComposition("stale",2);ImeOverlay.setCandidates(new String[]{"candidate"});ImeOverlay.setAlphaMode(false);
   check(!Ime261Compat.isModStyle()&&!Ime261Compat.shouldRenderOverlayLayer(),"disabled master still selects custom UI");
   check(!Ime261Compat.cancelVanillaPreeditRender(),"disabled master suppresses vanilla preedit");
   check(Ime261Compat.getBadgeText()==null,"disabled master displays a status badge");
   ImeOverlay.clear();Ime261Compat.onPreedit("ignored",1,null,0);
   check(!ImeOverlay.isComposing(),"disabled master captures preedit");
   ImeOverlay.setComposition("stale",2);ImeOverlay.render(graphics);
   check(graphics.operations==0,"disabled master draws over the input field");
   service.commit("、text");client.flush();check(client.keyboardHandler.characters.isEmpty(),"disabled master injects characters");
   service.onClientTick(client);
   check(!ImeOverlay.isComposing()&&!Ime261Compat.shouldRenderOverlayLayer(),"disabled tick retains composition or focus UI");
   check(service.hasFocusTarget(),"disabled IME changes the keybind wheel's typing context");
   service.onGenericEditFocus(new Object(),true);service.onClientTick(client);
   check(service.hasFocusTarget(),"disabled IME loses book/sign typing focus");
   service.onScreenChanged();check(!service.hasFocusTarget(),"typing focus survives a screen change");
   check(get("getForegroundWindow")==null,"disabled master initializes system IME polling");
  }else{
   Configs.CONTINGAME_IME.flag=true;service.onEditFocus(new EditBox(),true);set("lastPoll",System.currentTimeMillis());set("conversionMode",1);
   service.onClientTick(client);Ime261Compat.onPreedit("active",1,List.of("candidate"),0);ImeOverlay.render(graphics);
   check(graphics.operations>0&&Ime261Compat.cancelVanillaPreeditRender(),"enabled custom style stopped working");
   service.commit("queued");Configs.CONTINGAME_IME.flag=false;client.flush();
   check(client.keyboardHandler.characters.isEmpty(),"queued callback commits after the feature is disabled");
   graphics.operations=0;ImeOverlay.render(graphics);
   check(graphics.operations==0&&!Ime261Compat.cancelVanillaPreeditRender()&&Ime261Compat.getBadgeText()==null,"disable does not immediately restore vanilla rendering");
   service.onClientTick(client);check(!ImeOverlay.isComposing(),"disable leaves active composition behind");
  }
 }
}
'''

STUBS = {
    "io/github/halfmasa/xaerobinding/config/Configs.java": '''package io.github.halfmasa.xaerobinding.config;import java.util.*;public class Configs {
 public static class Value {public boolean flag;public ImeStyle option=ImeStyle.VANILLA;public boolean getBooleanValue(){return flag;}public ImeStyle getOptionListValue(){return option;}public List<String> getStrings(){return List.of("、");}}
 public static final Value CONTINGAME_IME=new Value(),IME_RENDER_STYLE=new Value(),IME_HIDE_VANILLA_PREEDIT=new Value(),IME_STATUS_INDICATOR=new Value(),IME_AUTO_REPLACE_SLASH=new Value(),IME_SLASH_CHARACTERS=new Value(),IME_DISABLE_IN_COMMAND_MODE=new Value();}''',
    "io/github/halfmasa/xaerobinding/config/ImeStyle.java": 'package io.github.halfmasa.xaerobinding.config;public enum ImeStyle {VANILLA,MOD;public String getStringValue(){return name();}}',
    "io/github/halfmasa/xaerobinding/XaeroWorldBinding.java": 'package io.github.halfmasa.xaerobinding;public class XaeroWorldBinding {public static final Log LOGGER=new Log();public static class Log {public void info(String text,Object... values){}}}',
    "fi/dy/masa/malilib/interfaces/IClientTickHandler.java": 'package fi.dy.masa.malilib.interfaces;import net.minecraft.client.Minecraft;public interface IClientTickHandler {void onClientTick(Minecraft client);}',
    "city/windmill/ingameime/client/jni/ExternalBaseIME.java": 'package city.windmill.ingameime.client.jni;public class ExternalBaseIME {private static final ExternalBaseIME INSTANCE=new ExternalBaseIME();public boolean installed;public int releaseCount;public static ExternalBaseIME getInstance(){return INSTANCE;}public boolean initialize(){return false;}public boolean isInitialized(){return installed;}public void uninitialize(){if(installed){installed=false;releaseCount++;}}public void setState(boolean active){}public void setFullScreen(boolean full){}}',
    "io/github/halfmasa/xaerobinding/compat/MinecraftClientCompat.java": 'package io.github.halfmasa.xaerobinding.compat;import net.minecraft.client.Minecraft;public class MinecraftClientCompat {public static Object getScreen(Minecraft client){return client.screen;}}',
    "io/github/halfmasa/xaerobinding/compat/InputCompat.java": 'package io.github.halfmasa.xaerobinding.compat;import net.minecraft.client.Minecraft;public class InputCompat {public static boolean isFullscreen(Minecraft client){return false;}}',
    "io/github/halfmasa/xaerobinding/mixin/KeyboardHandlerAccessor.java": 'package io.github.halfmasa.xaerobinding.mixin;import net.minecraft.client.input.CharacterEvent;public interface KeyboardHandlerAccessor {void halfmasa$charTyped(long window,CharacterEvent event);}',
    "net/minecraft/client/input/CharacterEvent.java": 'package net.minecraft.client.input;public record CharacterEvent(int codePoint) {}',
    "net/minecraft/client/gui/components/EditBox.java": 'package net.minecraft.client.gui.components;public class EditBox {public boolean isFocused(){return true;}public int getCursorPosition(){return 0;}public String getValue(){return "";}}',
    "net/minecraft/client/gui/screens/ChatScreen.java": 'package net.minecraft.client.gui.screens;public class ChatScreen {}',
    "net/minecraft/client/gui/GuiGraphicsExtractor.java": 'package net.minecraft.client.gui;import net.minecraft.client.Minecraft;public class GuiGraphicsExtractor {public int operations;public void fill(int x,int y,int right,int bottom,int color){operations++;}public void text(Minecraft.Font font,String value,int x,int y,int color,boolean shadow){operations++;}}',
    "net/minecraft/client/Minecraft.java": '''package net.minecraft.client;import java.util.*;import io.github.halfmasa.xaerobinding.mixin.KeyboardHandlerAccessor;import net.minecraft.client.input.CharacterEvent;public class Minecraft {
 private static final Minecraft INSTANCE=new Minecraft();public static Minecraft getInstance(){return INSTANCE;}public Object screen;public final Font font=new Font();public final Keyboard keyboardHandler=new Keyboard();public final Mouse mouseHandler=new Mouse();private final List<Runnable> queued=new ArrayList<>();public void execute(Runnable action){queued.add(action);}public void flush(){var actions=List.copyOf(queued);queued.clear();actions.forEach(Runnable::run);}public Window getWindow(){return new Window();}
 public static class Font {public int lineHeight=9;public int width(String value){return value.length()*6;}}
 public static class Window {public int getGuiScaledWidth(){return 320;}public int getGuiScaledHeight(){return 180;}public double getGuiScale(){return 1;}public long handle(){return 1;}}
 public static class Mouse {public double xpos(){return 0;}public double ypos(){return 0;}}
 public static class Keyboard implements KeyboardHandlerAccessor {public final List<Integer> characters=new ArrayList<>();public void halfmasa$charTyped(long window,CharacterEvent event){characters.add(event.codePoint());}}
}''',
}


class ImeDisabledTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="halfmasa-ime-")
        cls.addClassCleanup(cls.temp.cleanup)
        work = Path(cls.temp.name)
        java_home = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-25.0.3"))
        cls.java = java_home / "bin/java.exe"
        cls.classpath = str(work)
        sources = []
        for name, code in STUBS.items():
            path = work / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(code, encoding="utf-8")
            sources.append(path)
        feature = ROOT / "src/main/java/io/github/halfmasa/xaerobinding/feature"
        sources.extend(feature / name for name in ["ImeService.java", "ImeOverlay.java", "Ime261Compat.java"])
        harness = work / "ImeCheck.java"
        harness.write_text(HARNESS, encoding="utf-8")
        sources.append(harness)
        result = subprocess.run([str(java_home / "bin/javac.exe"), "-encoding", "UTF-8", "-d", str(work), *map(str, sources)], capture_output=True, text=True)
        if result.returncode:
            raise AssertionError(result.stdout + result.stderr)

    def scenario(self, name):
        result = subprocess.run([str(self.java), "-classpath", self.classpath, "ImeCheck", name], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_disabled_feature_leaves_vanilla_ui_and_input_untouched(self):
        self.scenario("disabled")

    def test_disable_immediately_blocks_ui_and_queued_commits(self):
        self.scenario("toggle")

    def test_disabling_releases_the_legacy_native_hook_once(self):
        self.scenario("legacy_release")


if __name__ == "__main__":
    unittest.main()

"""Exercise configuration migration and real creative inventory lifecycle on 26.3."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import zipfile
import run_startup_smoke as startup
import run_entity_render_disable_smoke as entity_smoke

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''package io.github.halfmasa.screenmemorysmoke;
import java.util.*;
import java.nio.file.*;
import java.lang.reflect.*;
import com.google.gson.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import fi.dy.masa.malilib.hotkeys.*;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
import io.github.halfmasa.xaerobinding.mixin.ConfigSearchBarAccessor;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.widgets.*;
public class ScreenMemorySmoke implements ClientModInitializer {
 private static final boolean PAGES=WITH_PAGES;
 private static ScreenMemorySmoke instance;
 private int stage,checks;private long since;
 @Override public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private static Field field(Class<?> c,String n)throws Exception{for(;c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(n);}
 private static Method method(Class<?> c,String n,Class<?>...p)throws Exception{for(;c!=null;c=c.getSuperclass())try{var m=c.getDeclaredMethod(n,p);m.setAccessible(true);return m;}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(n);}
 private void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);System.out.println("SCREEN_MEMORY_CHECK "+why);}
 private void close(Minecraft mc){MinecraftClientCompat.setScreen(mc,null);}
 private CreativeModeInventoryScreen open(Minecraft mc){var s=new CreativeModeInventoryScreen(mc.player,mc.level.enabledFeatures(),true);MinecraftClientCompat.setScreen(mc,s);return s;}
 private CreativeModeTab selected()throws Exception{return (CreativeModeTab)field(CreativeModeInventoryScreen.class,"selectedTab").get(null);}
 private float scroll(CreativeModeInventoryScreen s)throws Exception{return field(s.getClass(),"scrollOffs").getFloat(s);}
 private void scroll(CreativeModeInventoryScreen s,float v)throws Exception{field(s.getClass(),"scrollOffs").setFloat(s,v);s.getMenu().scrollTo(v);}
 private EditBox search(CreativeModeInventoryScreen s)throws Exception{return (EditBox)field(s.getClass(),"searchBox").get(s);}
 private void query(CreativeModeInventoryScreen s,String text)throws Exception{search(s).setValue(text);method(s.getClass(),"refreshSearchResults").invoke(s);}
 private void select(CreativeModeInventoryScreen s,CreativeModeTab tab)throws Exception{method(s.getClass(),"selectTab",CreativeModeTab.class).invoke(s,tab);}
 private void same(float a,float b,String why){check(Math.abs(a-b)<.001f,why+" actual="+a+" expected="+b);}
 private Path config(){return Configs.getHalfMasaDirectory().resolve("halfmasa.json");}
 private JsonObject read()throws Exception{return JsonParser.parseString(Files.readString(config())).getAsJsonObject();}
 private void write(JsonObject root)throws Exception{Files.writeString(config(),root.toString());}
 private void configuration()throws Exception{
  check(Configs.KEEP_SCREEN_POSITION==Configs.KEEP_MOD_MENU_SCROLL,"old Java constant aliases the renamed master");
  check(Configs.KEEP_SCREEN_POSITION.getBooleanValue()&&Configs.KEEP_SCREEN_POSITION.getKeybind().getStringValue().equals("F7"),"startup migration retains enabled master and hotkey");
  check(!Configs.KEEP_CONFIG_SEARCH_POSITION.getBooleanValue()&&Configs.KEEP_CONFIG_SELECTED_TAB.getBooleanValue()&&Configs.KEEP_CONFIG_POSITION_EXPANDED.getBooleanValue(),"startup migration preserves children and expansion values");
  check(!Configs.KEEP_CREATIVE_SCREEN_POSITION.getBooleanValue(),"new creative child defaults off during migration");check(Configs.KEEP_CONFIG_SEARCH_POSITION.getKeybind().getStringValue().equals("F5")&&Configs.KEEP_CONFIG_SELECTED_TAB.getKeybind().getStringValue().equals("F6"),"old child shortcuts survive migration");
  var groups=CustomConfigGroupStore.getInstance().getGroups();check(groups.stream().anyMatch(g->g.id.equals("fixture-main")&&g.mainConfig.equals("keepScreenPosition")),"custom group master reference migrated");check(groups.stream().anyMatch(g->g.id.equals("fixture-child")&&g.children.contains("halfmasa::keepScreenPosition")),"custom group child reference migrated");
  var root=read();check(root.get("ConfigVersion").getAsInt()==55&&!root.getAsJsonObject("Ported").has("keepModMenuScroll"),"migration writes schema 55 and removes old key");
  Configs.KEEP_SCREEN_POSITION.getKeybind().setSettings(KeybindSettings.create(KeybindSettings.Context.ANY,KeyAction.BOTH,true,false,false,false));new Configs().save();root=read();var ported=root.getAsJsonObject("Ported");var expected=ported.get("keepScreenPosition").deepCopy();ported.remove("keepScreenPosition");ported.add("keepModMenuScroll",expected.deepCopy());write(root);Configs.KEEP_SCREEN_POSITION.setBooleanValue(false);Configs.KEEP_SCREEN_POSITION.getKeybind().resetToDefault();new Configs().load();check(read().getAsJsonObject("Ported").get("keepScreenPosition").equals(expected),"whole hotkey JSON including nondefault settings survives migration at current schema");
  root=read();ported=root.getAsJsonObject("Ported");var legacy=expected.deepCopy().getAsJsonObject();legacy.addProperty("enabled",false);legacy.getAsJsonObject("hotkey").addProperty("keys","F9");ported.add("keepModMenuScroll",legacy);write(root);new Configs().load();check(read().getAsJsonObject("Ported").get("keepScreenPosition").equals(expected),"new key wins when both old and new keys exist");new Configs().load();check(read().getAsJsonObject("Ported").get("keepScreenPosition").equals(expected),"migration is idempotent");root=read();ported=root.getAsJsonObject("Ported");var disabled=expected.deepCopy().getAsJsonObject();disabled.addProperty("enabled",false);ported.remove("keepScreenPosition");ported.add("keepModMenuScroll",disabled);write(root);new Configs().load();check(!Configs.KEEP_SCREEN_POSITION.getBooleanValue()&&read().getAsJsonObject("Ported").get("keepScreenPosition").equals(disabled),"disabled legacy master and its hotkey settings are preserved");
  var children=Configs.getExpansionChildren(Configs.KEEP_SCREEN_POSITION);check(children.equals(List.of(Configs.KEEP_CONFIG_SEARCH_POSITION,Configs.KEEP_CONFIG_SELECTED_TAB,Configs.KEEP_CREATIVE_SCREEN_POSITION)),"three children keep original order and add creative last");
  check(Configs.HOTKEYS.contains(Configs.KEEP_CREATIVE_SCREEN_POSITION)&&Configs.PORTED.stream().filter(c->c==Configs.KEEP_SCREEN_POSITION).count()==1&&Configs.PORTED.contains(Configs.KEEP_CREATIVE_SCREEN_POSITION),"creative option registered for persistence and hotkeys without duplicate master");
  Configs.KEEP_SCREEN_POSITION.resetToDefault();Configs.KEEP_CONFIG_SEARCH_POSITION.resetToDefault();Configs.KEEP_CONFIG_SELECTED_TAB.resetToDefault();Configs.KEEP_CREATIVE_SCREEN_POSITION.resetToDefault();check(!Configs.KEEP_SCREEN_POSITION.getBooleanValue()&&Configs.KEEP_CONFIG_SEARCH_POSITION.getBooleanValue()&&Configs.KEEP_CONFIG_SELECTED_TAB.getBooleanValue()&&!Configs.KEEP_CREATIVE_SCREEN_POSITION.getBooleanValue(),"fresh defaults are off/on/on/off");var toggle=((KeybindMulti)Configs.KEEP_CREATIVE_SCREEN_POSITION.getKeybind()).getCallback();check(toggle!=null&&toggle.onKeyAction(KeyAction.PRESS,Configs.KEEP_CREATIVE_SCREEN_POSITION.getKeybind())&&Configs.KEEP_CREATIVE_SCREEN_POSITION.getBooleanValue()&&!Configs.KEEP_SCREEN_POSITION.getBooleanValue(),"creative child hotkey toggles independently of master");Configs.KEEP_CREATIVE_SCREEN_POSITION.resetToDefault();
  Configs.KEEP_SCREEN_POSITION.setBooleanValue(true);ConfigScrollMemory.saveModMenu(123);same((float)ConfigScrollMemory.restoreModMenu(),123,"original Mod Menu memory uses renamed master");ConfigScrollMemory.saveSelectedTab("CLIENT");check(ConfigScrollMemory.restoreSelectedTab().equals("CLIENT"),"original selected category memory retained");Configs.KEEP_CONFIG_SELECTED_TAB.setBooleanValue(false);check(ConfigScrollMemory.restoreSelectedTab().equals("ALL"),"selected category child still gates memory");Configs.KEEP_SCREEN_POSITION.setBooleanValue(false);check(ConfigScrollMemory.restoreModMenu()==0&&!Configs.KEEP_CONFIG_SELECTED_TAB.getBooleanValue()&&Configs.KEEP_CONFIG_SEARCH_POSITION.getBooleanValue(),"master disables memory without changing child values");Configs.KEEP_CONFIG_SELECTED_TAB.setBooleanValue(true);
 }
 private void browsing(Minecraft mc)throws Exception{
  var s=open(mc);var a=CreativeModeTabs.getDefaultTab();var b=CreativeModeTabs.allTabs().stream().filter(t->t!=a&&t!=CreativeModeTabs.searchTab()&&t.shouldDisplay()&&t.getDisplayItems().size()>54).findFirst().orElseThrow();
  Configs.KEEP_SCREEN_POSITION.setBooleanValue(true);Configs.KEEP_CREATIVE_SCREEN_POSITION.setBooleanValue(false);select(s,a);scroll(s,.6f);close(mc);s=open(mc);same(scroll(s),0,"creative child off preserves vanilla scroll reset");
  Configs.KEEP_CREATIVE_SCREEN_POSITION.setBooleanValue(true);select(s,a);scroll(s,.65f);select(s,b);scroll(s,.25f);select(s,a);same(scroll(s),.65f,"first category retains its own scroll");select(s,b);same(scroll(s),.25f,"second category retains separate scroll");close(mc);s=open(mc);check(selected()==b,"reopening restores selected category");same(scroll(s),.25f,"reopening restores category scroll");
  select(s,CreativeModeTabs.searchTab());query(s,"#minecraft:");check(s.getMenu().items.size()>54,"search fixture has enough results to scroll");scroll(s,.72f);String first=BuiltInRegistries.ITEM.getKey(s.getMenu().getSlot(0).getItem().getItem()).toString();close(mc);s=open(mc);check(selected()==CreativeModeTabs.searchTab()&&search(s).getValue().equals("#minecraft:"),"reopening restores search category and query");same(scroll(s),.72f,"search scroll restored after final result rebuild");check(BuiltInRegistries.ITEM.getKey(s.getMenu().getSlot(0).getItem().getItem()).toString().equals(first),"visible item row matches restored result offset");select(s,a);select(s,CreativeModeTabs.searchTab());check(search(s).getValue().equals("#minecraft:"),"switching categories restores search text");same(scroll(s),.72f,"switching categories restores search scroll");s.resize(s.width,s.height);check(search(s).getValue().equals("#minecraft:"),"resize retains search text");same(scroll(s),.72f,"resize retains scroll after native resize finishes");
  Configs.CONDENSED_CREATIVE.setBooleanValue(true);close(mc);s=open(mc);same(scroll(s),.72f,"condensed creative rebuild preserves remembered scroll");Configs.ITEM_SEARCH_HISTORY.setBooleanValue(true);s.containerTick();close(mc);s=open(mc);same(scroll(s),.72f,"search history and condensed entries compose with restoration");Configs.CONDENSED_CREATIVE.setBooleanValue(false);Configs.ITEM_SEARCH_HISTORY.setBooleanValue(false);close(mc);
  CreativeScreenMemory.saveClose(CreativeModeTabs.searchTab(),.9f,"bedrock",0);s=open(mc);check(search(s).getValue().equals("bedrock")&&!s.getMenu().canScroll(),"small search result fixture has no scrollbar");same(scroll(s),0,"shortened results clamp stale scroll to zero");
  CreativeScreenMemory.saveTab(a,Float.NaN,null);same(CreativeScreenMemory.restoreScroll(a),0,"NaN scroll sanitized");CreativeScreenMemory.saveTab(a,Float.POSITIVE_INFINITY,null);same(CreativeScreenMemory.restoreScroll(a),0,"infinite scroll sanitized");CreativeScreenMemory.saveTab(a,-1,null);same(CreativeScreenMemory.restoreScroll(a),0,"negative scroll clamped");CreativeScreenMemory.saveTab(a,2,null);same(CreativeScreenMemory.restoreScroll(a),1,"oversized scroll clamped");CreativeScreenMemory.saveTab(null,.5f,null);check(CreativeScreenMemory.restoreTab()!=null,"null tab save is ignored");
  close(mc);Configs.KEEP_CREATIVE_SCREEN_POSITION.setBooleanValue(false);check(CreativeScreenMemory.restoreTab()==null&&CreativeScreenMemory.restoreSearch().isEmpty(),"disabling creative child clears session state");Configs.KEEP_CREATIVE_SCREEN_POSITION.setBooleanValue(true);check(CreativeScreenMemory.restoreTab()==null,"reenabling child does not restore stale state");
  if(PAGES)pages(mc);
  s=open(mc);select(s,a);scroll(s,.4f);close(mc);Configs.KEEP_SCREEN_POSITION.setBooleanValue(false);check(!CreativeScreenMemory.enabled()&&CreativeScreenMemory.restoreTab()==null,"master off clears creative session state");check(Configs.KEEP_CREATIVE_SCREEN_POSITION.getBooleanValue(),"master does not change creative child value");s=open(mc);same(scroll(s),0,"master off does not restore creative scroll");close(mc);Configs.KEEP_SCREEN_POSITION.setBooleanValue(true);
  var missing=CreativeModeTab.builder(CreativeModeTab.Row.TOP,0).title(net.minecraft.network.chat.Component.literal("removed fixture")).icon(()->new ItemStack(Items.STONE)).build();CreativeScreenMemory.saveClose(missing,.5f,"",999);check(CreativeScreenMemory.restoreTab()==null,"unregistered remembered category is rejected");
  Configs.KEEP_CREATIVE_SCREEN_POSITION.setBooleanValue(false);new Configs().save();check(!Files.readString(config()).contains("#minecraft:"),"browsing state never serialized to configuration");
 }
 private void pages(Minecraft mc)throws Exception{
  var s=open(mc);var pages=(net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen)s;check(pages.getPageCount()>1,"extra mod categories create multiple actual Fabric pages");var tab=Fixture.TABS.getLast();pages.switchToPage(pages.getPage(tab));pages.setSelectedTab(tab);int page=pages.getCurrentPage();check(page>0&&selected()==tab,"mod category selected on a later page");close(mc);s=open(mc);pages=(net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen)s;check(selected()==tab&&pages.getCurrentPage()==page,"reopening restores mod category and page");
  pages.setSelectedTab(CreativeModeTabs.searchTab());query(s,"#minecraft:");scroll(s,.33f);int searchPage=pages.getCurrentPage();close(mc);s=open(mc);pages=(net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen)s;check(selected()==CreativeModeTabs.searchTab()&&pages.getCurrentPage()==searchPage,"search category retains its original page");same(scroll(s),.33f,"search results retain scrolling on a later page");close(mc);
  Configs.TRIAL_CREATIVE_TAB.setBooleanValue(true);s=open(mc);pages=(net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen)s;var trial=TrialCreativeTab.get();check(trial.shouldDisplay(),"trial category enabled and visible");pages.switchToPage(pages.getPage(trial));pages.setSelectedTab(trial);close(mc);s=open(mc);check(selected()==trial,"trial category restores through Fabric page bridge");close(mc);Configs.TRIAL_CREATIVE_TAB.setBooleanValue(false);s=open(mc);s.containerTick();check(selected()!=trial&&CreativeModeTabs.tabs().contains(selected()),"hidden trial category falls back to a valid native category");close(mc);
 }
 private WidgetListConfigOptions configList(HalfMasaConfigScreen ui)throws Exception{return (WidgetListConfigOptions)method(ui.getClass(),"getListWidget").invoke(ui);}
 private String configQuery(WidgetListConfigOptions list){return ((ConfigSearchBarAccessor)list.getSearchBarWidget()).halfmasa$getSearchBox().getValueWrapper();}
 private void configQuery(WidgetListConfigOptions list,String text){((ConfigSearchBarAccessor)list.getSearchBarWidget()).halfmasa$getSearchBox().setValueWrapper(text);list.getSearchBarWidget().setSearchOpen(true);list.refreshEntries();}
 @SuppressWarnings("unchecked") private void configUi(Minecraft mc)throws Exception{
  Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);Configs.KEEP_SCREEN_POSITION.setBooleanValue(true);Configs.KEEP_CONFIG_SEARCH_POSITION.setBooleanValue(true);Configs.KEEP_CONFIG_SELECTED_TAB.setBooleanValue(true);Configs.KEEP_CONFIG_POSITION_EXPANDED.setBooleanValue(true);Configs.KEEP_SCREEN_POSITION.getKeybind().setValueFromString("F7");ConfigScrollMemory.clearConfigState();
  var ui=new HalfMasaConfigScreen();var tab=field(ui.getClass(),"tab");tab.set(ui,Enum.valueOf((Class)tab.getType(),"PORTED_FEATURES"));GuiBase.openGui(ui);var list=configList(ui);var entries=list.getCurrentEntries();check(entries.stream().anyMatch(e->e.getConfig()==Configs.KEEP_SCREEN_POSITION)&&Configs.KEEP_CONFIG_POSITION_CHILDREN.stream().allMatch(c->entries.stream().anyMatch(e->e.getConfig()==c)),"actual configuration screen exposes master and all three children");
  configQuery(list,"Keep");close(mc);ui=new HalfMasaConfigScreen();GuiBase.openGui(ui);list=configList(ui);check(tab.get(ui).toString().equals("PORTED_FEATURES")&&configQuery(list).equals("Keep")&&list.getSearchBarWidget().isSearchOpen(),"renamed master preserves actual category and text-search UI state");
  var filter=((WidgetSearchBarConfigs)list.getSearchBarWidget()).getKeybind();filter.clearKeys();Configs.KEEP_SCREEN_POSITION.getKeybind().getKeys().forEach(filter::addKey);var expected=List.copyOf(filter.getKeys());list.refreshEntries();close(mc);ui=new HalfMasaConfigScreen();GuiBase.openGui(ui);list=configList(ui);check(((WidgetSearchBarConfigs)list.getSearchBarWidget()).getKeybind().getKeys().equals(expected),"actual MaLiLib key-search filter survives reopening");
  ((WidgetSearchBarConfigs)list.getSearchBarWidget()).getKeybind().clearKeys();configQuery(list,"");int offset=Math.min(12,list.getScrollbar().getMaxValue());check(offset>0,"configuration list fixture can scroll");list.getScrollbar().setValue(offset);close(mc);ui=new HalfMasaConfigScreen();GuiBase.openGui(ui);list=configList(ui);check(list.getScrollbar().getValue()==offset,"actual configuration list restores scroll position");configQuery(list,"");int parentIndex=0;while(list.getCurrentEntries().get(parentIndex).getConfig()!=Configs.KEEP_SCREEN_POSITION)parentIndex++;list.getScrollbar().setValue(Math.min(parentIndex,list.getScrollbar().getMaxValue()));
 }
 @SuppressWarnings("unchecked") private void layout(Minecraft mc)throws Exception{
  var list=configList((HalfMasaConfigScreen)MinecraftClientCompat.getScreen(mc));var widgets=(List<WidgetConfigOption>)field(list.getClass(),"listWidgets").get(list);Integer valueX=null;int count=0;
  for(var config:List.of(Configs.KEEP_SCREEN_POSITION,Configs.KEEP_CONFIG_SEARCH_POSITION,Configs.KEEP_CONFIG_SELECTED_TAB,Configs.KEEP_CREATIVE_SCREEN_POSITION)){
   var row=widgets.stream().filter(w->w.getEntry().getConfig()==config).findFirst().orElseThrow();var children=(List<WidgetBase>)field(row.getClass(),"subWidgets").get(row);var value=children.stream().filter(w->w.getClass().getSimpleName().equals("ConfigButtonBoolean")).findFirst().orElseThrow();var key=children.stream().filter(w->w instanceof fi.dy.masa.malilib.gui.button.ConfigButtonKeybind).findFirst().orElseThrow();check(value.getY()==key.getY()&&value.getX()+value.getWidth()<=key.getX(),"boolean and independent hotkey share aligned row: "+config.getName());if(valueX==null)valueX=value.getX();check(value.getX()==valueX,"child controls align with master value column: "+config.getName());count++;
  }check(count==4,"native GUI renders renamed parent and three independent child controls");net.minecraft.client.Screenshot.grab(mc,false);
 }
 private void tick(Minecraft mc){try{
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;configuration();stage=1;mc.createWorldOpenFlows().createFreshLevel("screen-memory-fixture",new LevelSettings("Screen Memory Fixture",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;}
  if(mc.level==null||mc.player==null)return;if(stage==1){if(mc.level.getGameTime()<40)return;close(mc);since=mc.level.getGameTime();stage=2;return;}
  if(stage==2){browsing(mc);configUi(mc);since=mc.level.getGameTime();stage=3;return;}
  if(stage==3&&mc.level.getGameTime()-since>=10){layout(mc);System.out.println("SCREEN_MEMORY_WORLD_PASS checks="+checks+" pages="+PAGES);stage=4;mc.stop();}
 }catch(Throwable error){throw new IllegalStateException("Screen memory smoke stage="+stage,error);}}
}
'''
FIXTURE = r'''package io.github.halfmasa.screenmemorysmoke;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
public class Fixture implements ModInitializer {
 public static final List<CreativeModeTab> TABS=new ArrayList<>();
 public void onInitialize(){for(int i=0;i<24;i++){var tab=FabricCreativeModeTab.builder().title(Component.literal("Memory fixture "+i)).icon(()->new ItemStack(Items.STONE)).build();Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,Identifier.fromNamespaceAndPath("screen_memory_fixture","tab_"+i),tab);CreativeModeTabEvents.modifyOutputEvent(BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElseThrow()).register(output->output.accept(new ItemStack(Items.STONE)));TABS.add(tab);}}
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Java/jdk-25.0.3'))
    parser.add_argument('--without-api', action='store_true')
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    args.version = '26.3'
    directory = ROOT / 'build/screen-position-tests' / datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    classes = directory / 'classes'; classes.mkdir(parents=True)
    sources = []
    frame = entity_smoke.FRAME.replace('entitydisablesmoke', 'screenmemorysmoke').replace('EntityDisableSmoke', 'ScreenMemorySmoke')
    clipboard = entity_smoke.CLIPBOARD.replace('entitydisablesmoke', 'screenmemorysmoke')
    for name, text in [('ScreenMemorySmoke', SOURCE.replace('WITH_PAGES', str(not args.without_api).lower())), ('Fixture', FIXTURE), ('FrameMixin', frame), ('ClipboardMixin', clipboard)]:
        path = directory / (name + '.java'); path.write_text(text, encoding='utf-8'); sources.append(str(path))
    cp = str(args.artifact) + os.pathsep + (ROOT / 'build/port-classpath-26.3.txt').read_text().strip()
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Dfile.encoding=UTF-8', '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        diagnostic = result.stderr.decode('utf-8', errors='replace'); (directory / 'compile.log').write_text(diagnostic, encoding='utf-8'); print(diagnostic); return 1
    mod = directory / 'screen-memory-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'): jar.write(path, path.relative_to(classes).as_posix())
        entries = {'client': ['io.github.halfmasa.screenmemorysmoke.ScreenMemorySmoke']}
        if not args.without_api: entries['main'] = ['io.github.halfmasa.screenmemorysmoke.Fixture']
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_screen_memory_smoke', 'version': '1.0', 'environment': '*', 'entrypoints': entries, 'mixins': ['screen-memory-smoke.mixins.json']}))
        jar.writestr('screen-memory-smoke.mixins.json', json.dumps({'required': True, 'package': 'io.github.halfmasa.screenmemorysmoke.mixin', 'compatibilityLevel': 'JAVA_25', 'client': ['FrameMixin', 'ClipboardMixin'], 'injectors': {'defaultRequire': 1}}))
    game = directory / 'game'; config = game / 'config/halfmasa'; config.mkdir(parents=True); (game / 'mods').mkdir()
    (game / 'config/malilib.json').write_text('{}\n')
    (config / 'halfmasa.json').write_text(json.dumps({'ConfigVersion': 54, 'Ported': {'keepModMenuScroll': {'enabled': True, 'hotkey': {'keys': 'F7'}}, 'keepConfigSearchPosition': {'enabled': False, 'hotkey': {'keys': 'F5'}}, 'keepConfigSelectedTab': {'enabled': True, 'hotkey': {'keys': 'F6'}}, 'keepConfigPositionExpanded': True}}), encoding='utf-8')
    groups = [{'id': 'fixture-main', 'source': 'halfmasa', 'name': 'master migration', 'mainSource': 'halfmasa', 'mainConfig': 'keepModMenuScroll', 'children': ['halfmasa::keepConfigSelectedTab'], 'expanded': True}, {'id': 'fixture-child', 'source': 'halfmasa', 'name': 'child migration', 'mainSource': 'halfmasa', 'mainConfig': 'betterSavedHotbars', 'children': ['halfmasa::keepModMenuScroll'], 'expanded': False}]
    (config / 'custom-config-groups.json').write_text(json.dumps({'groups': groups}), encoding='utf-8')
    (game / 'options.txt').write_text('onboardAccessibility:false\nenableVsync:false\nmaxFps:30\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:en_us\ntutorialStep:none\n')
    if not args.without_api:
        api = next(path for path in (Path.home() / '.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.161.0+26.3').glob('*/*.jar') if not path.name.endswith(('-sources.jar','-javadoc.jar'))); shutil.copy2(api, game / 'mods' / api.name)
    command = startup.production_command(args, game, mod)
    command[1:1] = ['-Djava.awt.headless=true', '-Dsun.java2d.d3d=false', '-XX:ActiveProcessorCount=4', f'-XX:ErrorFile={directory / "jvm-crash.log"}']
    command[command.index('--width')+1] = '1120'; command[command.index('--height')+1] = '720'
    log = directory / 'launcher.log'; started = time.monotonic(); timed_out = False
    with log.open('w', encoding='utf-8') as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try: code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            timed_out = True; subprocess.run(['taskkill','/PID',str(process.pid),'/T','/F'],capture_output=True); code = process.wait(timeout=30)
    text = log.read_text(encoding='utf-8', errors='replace'); passed = code == 0 and not timed_out and 'SCREEN_MEMORY_WORLD_PASS' in text
    report = {'result': 'PASS' if passed else 'FAIL', 'game_version': args.version, 'artifact': str(args.artifact), 'fabric_api_installed': not args.without_api, 'world_graphics_enabled': True, 'exit_code': code, 'timed_out': timed_out, 'duration_seconds': round(time.monotonic()-started,1), 'log': str(log), 'checks': [line for line in text.splitlines() if 'SCREEN_MEMORY_' in line], 'clipboard_initialization_disabled': True, 'screenshots': [str(p) for p in (game / 'screenshots').glob('*.png')]}
    report_path = directory / 'report.json'; report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8'); print('SCREEN POSITION:',report['result']); print(report_path)
    if not passed: print('\n'.join(text.splitlines()[-65:]))
    return 0 if passed else 1

if __name__ == '__main__':
    raise SystemExit(main())

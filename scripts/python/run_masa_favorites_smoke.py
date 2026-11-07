"""Exercise the 26.3 favorites UI and persistence in an isolated production client."""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time
import zipfile

import port_smoke_compat as compat
import run_startup_smoke as startup

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''package io.github.halfmasa.favoritessmoke;
import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.*;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.button.*;
import fi.dy.masa.malilib.gui.widgets.*;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.favorites.*;
import io.github.halfmasa.xaerobinding.gui.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.mixin.GuiListBaseAccessor;
public class FavoritesSmoke implements ClientModInitializer {
 static final boolean BASE_ONLY=Boolean.getBoolean("favorites.baseOnly");
 static int stage,frames,wait; static boolean done;
 static MasaFavoritesScreen screen;
 static List<MasaConfigCatalog.Entry> chosen;
 static TitleScreen title;
 static GuiConfigsBase livePositionScreen;
 static class KeylessBoolean extends ConfigBoolean implements IHotkeyTogglable {
  KeylessBoolean(){super("fixtureKeylessBoolean",false);}
  public fi.dy.masa.malilib.hotkeys.IKeybind getKeybind(){return null;}
 }
 static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);System.out.println("FAVORITES_CHECK "+name);}
 public void onInitializeClient(){}
 static Field field(Class<?> c,String name)throws NoSuchFieldException{while(c!=null){try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(name);}
 static WidgetListConfigOptions list(GuiConfigsBase s){return (WidgetListConfigOptions)((GuiListBaseAccessor)(Object)s).halfmasa$getListWidget();}
 static List<WidgetConfigOption> rows(GuiConfigsBase s)throws Exception{return (List<WidgetConfigOption>)field(WidgetListBase.class,"listWidgets").get(list(s));}
 static List<WidgetBase> children(WidgetConfigOption row)throws Exception{return (List<WidgetBase>)field(WidgetContainer.class,"subWidgets").get(row);}
 static String buttonText(WidgetBase button){try{return (String)field(ButtonBase.class,"displayString").get(button);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
 static void click(WidgetBase widget){check(widget.onMouseClicked(new MouseButtonEvent(widget.getX()+widget.getWidth()/2.0,widget.getY()+widget.getHeight()/2.0,new MouseButtonInfo(fi.dy.masa.malilib.util.input.ScanCodes.OFFSET_MOUSE_LEFT,0)),false),"widget click handled");}
 static MasaFavoriteButton star(WidgetConfigOption row)throws Exception{return (MasaFavoriteButton)children(row).stream().filter(w->w instanceof MasaFavoriteButton).findFirst().orElseThrow();}
 static void layout(GuiConfigsBase gui,boolean left)throws Exception{
  for(var row:rows(gui)){
   if(row.getEntry().getConfig()==null || row.getEntry().getConfig() instanceof io.github.halfmasa.xaerobinding.config.ConfigGroupHeader)continue;
   MasaFavoriteButton favorite=star(row);
   check(!field(ButtonGeneric.class,"renderDefaultBackground").getBoolean(favorite),"favorite star has no button background");
   check(children(row).stream().filter(w->w instanceof MasaFavoriteButton).count()==1,"one star per config row");
   check(favorite.getX()>=row.getX()&&favorite.getX()+favorite.getWidth()<=row.getX()+row.getWidth(),"star inside clickable row");
   check(left?favorite.getX()==row.getX():favorite.getX()>row.getX(),"star side matches preference");
   if(!left){String resetText=fi.dy.masa.malilib.util.StringUtils.translate("malilib.gui.button.reset.caps");
    var reset=children(row).stream().filter(w->w instanceof ButtonBase&&resetText.equals(buttonText(w))).max(java.util.Comparator.comparingInt(w->w.getX()+w.getWidth()));
    check(reset.isPresent()&&favorite.getX()==reset.get().getX()+reset.get().getWidth()+2,"right star immediately follows reset button");}
   for(var child:children(row))if(child instanceof ButtonBase && child!=favorite)
    check(child.getX()+child.getWidth()<=favorite.getX() || child.getX()>=favorite.getX()+favorite.getWidth(),"star does not overlap action/reset/group buttons");
   var buttons=children(row).stream().filter(w->w instanceof ButtonBase).toList();
   for(int i=0;i<buttons.size();i++)for(int j=i+1;j<buttons.size();j++){var a=buttons.get(i);var b=buttons.get(j);
    check(a.getX()+a.getWidth()<=b.getX() || b.getX()+b.getWidth()<=a.getX(),"config action/reset/group buttons do not overlap");}
  }
 }
 static void screenshot(Minecraft mc){net.minecraft.client.Screenshot.grab(mc,false);}
 static String value(MasaConfigCatalog.Entry e){var state=new com.google.gson.JsonObject();state.add("value",e.identity().getAsJsonElement());if(e.editable() instanceof fi.dy.masa.malilib.hotkeys.IHotkey hotkey&&hotkey.getKeybind()!=null)state.add("hotkey",hotkey.getKeybind().getAsJsonElement());return state.toString();}
 public static void frame(Minecraft mc){
  if(done)return;frames++;if(frames>1800){System.out.println("FAVORITES_FAIL timeout stage="+stage);done=true;mc.stop();return;}
  try{
   if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
    title=(TitleScreen)MinecraftClientCompat.getScreen(mc);
    if(Boolean.getBoolean("favorites.lpctoolsLayout")){
     Configs.MASA_FAVORITES_POSITION.setOptionListValue(MasaFavoritePosition.RIGHT);
     Object page=Class.forName("lpctools.LPCTools").getField("page").get(null);
     livePositionScreen=(GuiConfigsBase)page.getClass().getMethod("get").invoke(page);
     MinecraftClientCompat.setScreen(mc,livePositionScreen);
     var select=livePositionScreen.getClass().getDeclaredMethod("select",int.class);select.setAccessible(true);select.invoke(livePositionScreen,4);
     stage=16;wait=0;return;
    }
    MasaConfigCatalog.discover();
    for(String mod:List.of("itemscroller","tweakermore","lpctools","magiclib"))if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(mod))
     check(MasaConfigCatalog.entries().stream().anyMatch(e->e.modId().equals(mod)),"global catalog covers installed "+mod+" before original screen opens");
    if(Boolean.getBoolean("favorites.gadgetFixture"))check(MasaGadgetCompat.pages().stream().anyMatch(m->m.modId().equals("masa_gadget_mod")),"Gadget Mod Menu page merged before original screen opens");
    boolean restart=Boolean.getBoolean("favorites.restart");
    for(String mod:BASE_ONLY?List.of("halfmasa"):List.of("halfmasa","minihud","litematica","tweakeroo"))check(MasaConfigCatalog.entries().stream().anyMatch(e->e.modId().equals(mod)),"discover "+mod+" before opening any config screen");
    if(BASE_ONLY)check(MasaConfigCatalog.entries().stream().noneMatch(e->List.of("minihud","litematica","tweakeroo").contains(e.modId())),"missing optional mods do not create invalid entries");
    if(restart){
     var expected=com.google.gson.JsonParser.parseString(Files.readString(Path.of("build/favorites-expected.json"))).getAsJsonObject();
     for(var pair:expected.entrySet()){var entry=MasaConfigCatalog.find(pair.getKey());check(entry!=null&&value(entry).equals(pair.getValue().getAsString()),"original mod value survived restart: "+entry.modId());}
     screen=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,screen);check(screen.getConfigs().size()==expected.size(),"favorites restored before original screens visited");
     if(Files.exists(Path.of("build/favorite-page-expected.txt")))check(MasaFavoritesStore.get().contains(Files.readString(Path.of("build/favorite-page-expected.txt"))),"switcher page favorite survives process restart");stage=90;wait=0;return;
    }
    for(String key:MasaFavoritesStore.get().snapshot())MasaFavoritesStore.get().setFavorite(key,false);
    check(Configs.MASA_FAVORITES_POSITION.getOptionListValue()==MasaFavoritePosition.LEFT,"legacy boolean position migrates to left option");Configs.MASA_FAVORITES_POSITION.setOptionListValue(MasaFavoritePosition.RIGHT);
    check(Configs.MASA_FAVORITES.getBooleanValue()&&Configs.MASA_FAVORITES_SHOW_STARS.getBooleanValue(),"independent favorites master and side stars default enabled");
    for(var child:Configs.getExpansionChildren(Configs.MASA_FAVORITES))check(Configs.getExpansionParent(child)==Configs.MASA_FAVORITES,"favorite option belongs to independent master");
    check(Configs.getExpansionParent(Configs.OPEN_MASA_FAVORITES)==Configs.MASA_FAVORITES&&Configs.getExpansionParent(Configs.MASA_FAVORITES_LEFT)==Configs.MASA_FAVORITES,"favorites are not children of config grouping");
    Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);Configs.CONFIG_GROUPING_EXPANDED.setBooleanValue(false);Configs.MASA_FAVORITES_EXPANDED.setBooleanValue(false);
    check(Configs.getPortedView().contains(Configs.MASA_FAVORITES)&&!Configs.getPortedView().contains(Configs.OPEN_MASA_FAVORITES),"favorites master remains top-level while config grouping is collapsed");
    Configs.MASA_FAVORITES_EXPANDED.setBooleanValue(true);check(Configs.getPortedView().contains(Configs.OPEN_MASA_FAVORITES),"favorites expand independently of config grouping");Configs.MASA_FAVORITES_EXPANDED.setBooleanValue(false);
    check(Configs.getExpansionParent(Configs.MASA_BETTER_SWITCHER)==Configs.MASA_FAVORITES&&Configs.getExpansionParent(Configs.MASA_SWITCHER_FAVORITES)==Configs.MASA_BETTER_SWITCHER,"switcher features have nested favorites hierarchy");
    var groups=io.github.halfmasa.xaerobinding.feature.CustomConfigGroupStore.getInstance();groups.ensureDefaults("halfmasa",HalfMasaConfigExpansionProvider.getBuiltInGroupTemplates());
    var oldGroup=groups.getGroups().stream().filter(g->g.id.equals("builtin_halfmasa_configGroupingGroup")).findFirst().orElseThrow();
    check(oldGroup.children.stream().noneMatch(ref->ref.contains("MasaFavorites")||ref.contains("masaFavorites")),"existing built-in grouping releases favorites during migration");
    check(groups.getGroups().stream().anyMatch(g->g.id.equals("builtin_halfmasa_masaFavorites")&&g.mainConfig.equals("masaFavorites")),"existing custom grouping receives independent favorites group");
    check(groups.getGroups().size()==3&&groups.getGroups().stream().anyMatch(g->g.id.equals("user-keep")&&g.name.equals("User layout")),"migration preserves user groups and deleted unrelated defaults");
    Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(true);Configs.MASA_FAVORITES_EXPANDED.setBooleanValue(true);Configs.MASA_BETTER_SWITCHER_EXPANDED.setBooleanValue(false);
    var customProvider=new HalfMasaConfigExpansionProvider("halfmasa");var customView=customProvider.prepareEntries(GuiConfigsBase.ConfigOptionWrapper.createFor(Configs.ALL),false);
    check(customView.stream().noneMatch(w->w.getConfig()==Configs.MASA_SWITCHER_FAVORITES),"custom grouping keeps switcher child collapsed");Configs.MASA_BETTER_SWITCHER_EXPANDED.setBooleanValue(true);
    check(customProvider.prepareEntries(GuiConfigsBase.ConfigOptionWrapper.createFor(Configs.ALL),false).stream().anyMatch(w->w.getConfig()==Configs.MASA_SWITCHER_FAVORITES),"custom grouping expands nested switcher favorite option");
    Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);Configs.MASA_FAVORITES_EXPANDED.setBooleanValue(false);Configs.MASA_BETTER_SWITCHER_EXPANDED.setBooleanValue(false);
    Configs.MASA_FAVORITES.setBooleanValue(false);check(!MasaFavoriteButton.isShown(new HalfMasaConfigScreen())&&!Configs.OPEN_MASA_FAVORITES.trigger(),"disabled master disables stars and open action");Configs.MASA_FAVORITES.setBooleanValue(true);
    Configs.MASA_FAVORITES_SHOW_STARS.setBooleanValue(false);check(!MasaFavoriteButton.isShown(new HalfMasaConfigScreen())&&MasaFavoriteButton.isShown(new MasaFavoritesScreen()),"side star visibility is independent of favorites page");Configs.MASA_FAVORITES_SHOW_STARS.setBooleanValue(true);
    var keyless=new KeylessBoolean();var keylessEntry=MasaConfigCatalog.observe("halfmasa",keyless);
    check(keylessEntry.editable()==keyless,"null keybind config keeps original boolean control instead of invalid hotkey wrapper");
    check(MasaConfigCatalog.observe("halfmasa",new ConfigTypeWrapper(ConfigType.BOOLEAN,keyless)).key().equals(keylessEntry.key()),"type wrapper resolves to existing original object");
    var wrappedInteger=new ConfigInteger("fixtureWrappedInteger",7);var wrappedEntry=MasaConfigCatalog.observe("halfmasa",new ConfigTypeWrapper(ConfigType.INTEGER,wrappedInteger));
    check(wrappedEntry.identity()==wrappedInteger&&wrappedEntry.editable().getType()==ConfigType.INTEGER,"generic wrapper does not misclassify numeric configs as booleans");
    MasaFavoritesStore.get().setFavorite(keylessEntry.key(),true);var keylessPage=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,keylessPage);
    var keylessRow=rows(keylessPage).get(0);click(children(keylessRow).stream().filter(w->w instanceof ConfigButtonBoolean).findFirst().orElseThrow());
    check(keyless.getBooleanValue()&&keylessPage.session().changedMods().contains("halfmasa"),"null keybind boolean can be displayed, edited and tracked");MinecraftClientCompat.setScreen(mc,title);MasaFavoritesStore.get().setFavorite(keylessEntry.key(),false);
    var box=MasaConfigCatalog.observe("halfmasa",Configs.ENABLE_GIVE_FULL_INVENTORY);MasaFavoritesStore.get().setFavorite(box.key(),true);
    Configs.GIVE_FULL_INVENTORY_EXPANDED.setBooleanValue(true);var boxPage=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,boxPage);
    check(list(boxPage).getCurrentEntries().size()==1,"favorite parent starts collapsed even when original page is expanded");
    click(children(rows(boxPage).get(0)).stream().filter(w->w instanceof ButtonBase&&"[+]".equals(buttonText(w))).findFirst().orElseThrow());
    check(list(boxPage).getCurrentEntries().stream().anyMatch(w->w.getConfig()==Configs.FILL_SAFETY),"expanding favorite parent exposes unstarred child config");
    check(Configs.GIVE_FULL_INVENTORY_EXPANDED.getBooleanValue(),"favorites fold state does not alter original page state");
    var safetyRow=rows(boxPage).stream().filter(r->r.getEntry().getConfig()==Configs.FILL_SAFETY).findFirst().orElseThrow();boolean safetyBefore=Configs.FILL_SAFETY.getBooleanValue();
    click(children(safetyRow).stream().filter(w->w instanceof ConfigButtonBoolean).findFirst().orElseThrow());check(Configs.FILL_SAFETY.getBooleanValue()!=safetyBefore,"favorite child edits original config object");
    check(boxPage.session().changedMods().contains("halfmasa"),"unstarred child modification is tracked for owner save");
    var safetyEntry=MasaConfigCatalog.observe("halfmasa",Configs.FILL_SAFETY);MasaFavoritesStore.get().setFavorite(safetyEntry.key(),true);
    MinecraftClientCompat.setScreen(mc,title);new Configs().load();check(Configs.FILL_SAFETY.getBooleanValue()!=safetyBefore,"unstarred child edit survives owner reload");
    boxPage=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,boxPage);check(list(boxPage).getCurrentEntries().size()==1,"separately favorited child stays folded under favorite parent");
    click(children(rows(boxPage).get(0)).stream().filter(w->w instanceof ButtonBase&&"[+]".equals(buttonText(w))).findFirst().orElseThrow());
    check(list(boxPage).getCurrentEntries().stream().filter(w->w.getConfig()==Configs.FILL_SAFETY).count()==1,"expanded child is not duplicated by separate favorite membership");
    click(star(rows(boxPage).get(0)));check(list(boxPage).getCurrentEntries().size()>1,"cancelled favorite parent retains expanded snapshot for this visit");
    MinecraftClientCompat.setScreen(mc,title);MasaFavoritesStore.get().setFavorite(safetyEntry.key(),false);Configs.GIVE_FULL_INVENTORY_EXPANDED.setBooleanValue(false);
    check(new MasaFavoritesScreen().getConfigs().isEmpty(),"empty favorites screen");
    var probeA=new ConfigBoolean("globalSearchProbeA",false);var probeB=new ConfigBoolean("globalSearchProbeB",false);
    var entryA=MasaConfigCatalog.observe("halfmasa",probeA);var entryB=MasaConfigCatalog.observe("halfmasa",probeB);
    MasaFavoritesStore.get().setFavorite(entryB.key(),true);
    var globalPage=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,globalPage);
    check(globalPage.getTitle().getString().contains("halfmasa"),"favorites title credits halfmasa");
    check(fi.dy.masa.malilib.util.StringUtils.translate("halfmasa.gui.favorites.source").contains("halfmasa"),"favorites source label credits halfmasa");
    check(list(globalPage).getCurrentEntries().size()==1,"global search starts with favorites only");
    var globalSearch=list(globalPage).getSearchBarWidget();globalSearch.setSearchOpen(true);
    var globalBox=(GuiTextFieldGeneric)field(WidgetSearchBar.class,"searchBox").get(globalSearch);globalBox.setValueWrapper("GLOBALSEARCHPROBE");list(globalPage).refreshEntries();
    check(list(globalPage).getCurrentEntries().size()==2,"search finds unstarred original configs case-insensitively");
    check(list(globalPage).getCurrentEntries().get(0).getConfig()==probeB,"global search ranks favorite before non-favorite");
    var probeRow=rows(globalPage).stream().filter(r->r.getEntry().getConfig()==probeA).findFirst().orElseThrow();
    click(children(probeRow).stream().filter(w->w instanceof ConfigButtonBoolean).findFirst().orElseThrow());check(probeA.getBooleanValue(),"global search edits original object");
    check(globalPage.session().changedMods().contains("halfmasa"),"non-favorite search edit is tracked for owner save");
    click(star(rows(globalPage).get(0)));check(list(globalPage).getCurrentEntries().get(0).getConfig()==probeB,"unfavorite does not jump or remove current search row");
    globalBox.setValueWrapper("no_global_matches");list(globalPage).refreshEntries();
    check(list(globalPage).getCurrentEntries().isEmpty(),"nonmatching search is empty");
    globalBox.setValueWrapper("globalSearchProbe");list(globalPage).refreshEntries();
    check(list(globalPage).getCurrentEntries().get(0).getConfig()==probeA,"new query rebuild uses current favorite state");
    Configs.MASA_FAVORITES_GLOBAL_SEARCH.setBooleanValue(false);list(globalPage).refreshEntries();
    check(list(globalPage).getCurrentEntries().size()==1,"disabling global search immediately restores snapshot-only filtering");
    Configs.MASA_FAVORITES_GLOBAL_SEARCH.setBooleanValue(true);globalBox.setValueWrapper("halfmasa");list(globalPage).refreshEntries();
    check(list(globalPage).getCurrentEntries().size()>10,"global search accepts mod identifier");
    globalBox.setValueWrapper("");list(globalPage).refreshEntries();
    check(list(globalPage).getCurrentEntries().size()==1,"clearing query restores visit snapshot including unfavorited row");
    MinecraftClientCompat.setScreen(mc,title);check(new MasaFavoritesScreen().getConfigs().isEmpty(),"reopening removes cancelled search favorite");
    Path testDir=Configs.getHalfMasaDirectory().resolve("favorites-fixture");Files.createDirectories(testDir);
    Path file=testDir.resolve("valid.json");MasaFavoritesStore store=new MasaFavoritesStore(file);
    check(store.snapshot().isEmpty(),"missing file starts empty");check(store.setFavorite("missing_mod:config",true),"unknown mod key saves");
    check(new MasaFavoritesStore(file).contains("missing_mod:config"),"unknown mod record survives reload");
    check(store.setFavorite("missing_mod:config",false)&&new MasaFavoritesStore(file).snapshot().isEmpty(),"remove persists immediately");
    Path bad=testDir.resolve("bad.json");Files.writeString(bad,"{broken");MasaFavoritesStore invalid=new MasaFavoritesStore(bad);
    check(!invalid.setFavorite("test:key",true)&&Files.readString(bad).equals("{broken"),"corrupt file preserved on attempted change");
    Path future=testDir.resolve("future.json");Files.writeString(future,"{\"version\":99,\"favorites\":[]}");
    check(!new MasaFavoritesStore(future).setFavorite("test:key",true),"future schema is not overwritten");
    Path denied=testDir.resolve("directory.json");MasaFavoritesStore failed=new MasaFavoritesStore(denied);failed.snapshot();Files.createDirectories(denied);
    check(!failed.setFavorite("test:key",true)&&!failed.contains("test:key"),"failed write does not change membership");
    chosen=new ArrayList<>();
    chosen.add(MasaConfigCatalog.entries().stream().filter(e->e.identity()==Configs.MASA_FAVORITES_PIN).findFirst().orElseThrow());
    chosen.add(MasaConfigCatalog.entries().stream().filter(e->e.identity()==Configs.OPEN_MASA_FAVORITES).findFirst().orElseThrow());
    if(!BASE_ONLY)for(String mod:List.of("minihud","litematica","tweakeroo"))chosen.add(MasaConfigCatalog.entries().stream().filter(e->e.modId().equals(mod)&&e.identity() instanceof IConfigBoolean&&!(e.identity() instanceof IConfigLockedList)).filter(e->!mod.equals("tweakeroo")||e.identity().getName().equals("tweakToolSwitch")).findFirst().orElseThrow());
    chosen.add(MasaConfigCatalog.entries().stream().filter(e->e.modId().equals("halfmasa")&&e.identity() instanceof ConfigInteger).findFirst().orElseThrow());
    for(var entry:chosen)check(MasaFavoritesStore.get().setFavorite(entry.key(),true),"favorite "+entry.modId()+"/"+entry.identity().getName());
    screen=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,screen);check(screen.getConfigs().size()==chosen.size(),"all favorites in snapshot");
    check(screen.getConfigs().get(0).getConfig()==chosen.get(0).editable(),"favorites row uses catalog object identity");stage=1;wait=0;return;
   }
   if(++wait<8)return;wait=0;
   if(stage==1){layout(screen,false);screenshot(mc);
    var row=rows(screen).get(0);var favorite=star(row);
    favorite.onMouseClicked(new MouseButtonEvent(favorite.getX()+5,favorite.getY()+5,new MouseButtonInfo(fi.dy.masa.malilib.util.input.ScanCodes.OFFSET_MOUSE_RIGHT,0)),false);
    check(MasaFavoritesStore.get().contains(chosen.get(0).key()),"right click does not toggle membership");click(favorite);
    check(!MasaFavoritesStore.get().contains(chosen.get(0).key()),"cancel persisted");
    check(screen.getConfigs().size()==chosen.size(),"cancel keeps snapshot row");
    var search=list(screen).getSearchBarWidget();search.setSearchOpen(true);
    var searchBox=(GuiTextFieldGeneric)field(WidgetSearchBar.class,"searchBox").get(search);searchBox.setValueWrapper(chosen.get(0).identity().getName());list(screen).refreshEntries();
    check(list(screen).getCurrentEntries().size()==1,"cancelled row remains searchable during visit");searchBox.setValueWrapper("");search.setSearchOpen(false);list(screen).refreshEntries();
    check(list(screen).getCurrentEntries().size()==chosen.size(),"clearing filter restores unchanged snapshot");
    var visit=screen;MinecraftClientCompat.setScreen(mc,new TitleScreen());MinecraftClientCompat.setScreen(mc,visit);
    check(screen.getConfigs().size()==chosen.size(),"return from another screen preserves current visit");
    screen.resize(640,360);check(screen.getConfigs().size()==chosen.size(),"resize does not refresh membership");stage=2;return;}
   if(stage==2){layout(screen,false);click(star(rows(screen).get(0)));
    check(MasaFavoritesStore.get().contains(chosen.get(0).key()),"empty star restores membership");
    MasaFavoritesStore.get().setFavorite(chosen.get(0).key(),false);
    MinecraftClientCompat.setScreen(mc,title);screen=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,screen);
    check(screen.getConfigs().size()==chosen.size()-1,"new visit excludes cancelled favorite");
    MasaFavoritesStore.get().setFavorite(chosen.get(0).key(),true);
    MinecraftClientCompat.setScreen(mc,title);screen=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,screen);stage=3;return;}
   if(stage==3){
    var hotkeyRow=rows(screen).stream().filter(r->r.getEntry().getConfig()==Configs.OPEN_MASA_FAVORITES).findFirst().orElseThrow();
    var keyButton=children(hotkeyRow).stream().filter(w->w instanceof ConfigButtonKeybind).findFirst().orElseThrow();click(keyButton);
    screen.onKeyTyped(new KeyEvent(fi.dy.masa.malilib.util.input.ScanCodes.SCAN_F8,fi.dy.masa.malilib.util.input.ScanCodes.SCAN_F8,0));
    check(!Configs.OPEN_MASA_FAVORITES.getKeybind().getKeys().isEmpty(),"native hotkey control updates original binding");
    screen.setActiveKeybindButton(null);
    for(var entry:chosen)if(entry.identity() instanceof IConfigBoolean b){var row=rows(screen).stream().filter(r->r.getEntry().getConfig()==entry.editable()).findFirst().orElseThrow();boolean before=b.getBooleanValue();
     ButtonBase toggle=(ButtonBase)children(row).stream().filter(w->w instanceof ConfigButtonBoolean).findFirst().orElseThrow();click(toggle);check(b.getBooleanValue()!=before,"native control changes original "+entry.modId()+" object");}
    var numeric=chosen.get(chosen.size()-1);var row=rows(screen).stream().filter(r->r.getEntry().getConfig()==numeric.editable()).findFirst().orElseThrow();
    var config=(ConfigInteger)numeric.identity();if(config.shouldUseSlider())config.toggleUseSlider();screen.initGui();
    row=rows(screen).stream().filter(r->r.getEntry().getConfig()==numeric.editable()).findFirst().orElseThrow();
    Object text=field(WidgetConfigOptionBase.class,"textField").get(row);var input=((fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper<?>)text).textField();
    int next=config.getIntegerValue()==config.getMinIntegerValue()?config.getMaxIntegerValue():config.getMinIntegerValue();input.setValueWrapper(Integer.toString(next));
    check(screen.session().changedMods().containsAll(chosen.stream().map(MasaConfigCatalog.Entry::modId).toList()),"session tracks all changed owners");
    MinecraftClientCompat.setScreen(mc,title);check(config.getIntegerValue()==next,"closing commits pending text edit");
    check(new MasaFavoritesScreen().session().changedMods().isEmpty(),"new session starts clean");
    var expected=new com.google.gson.JsonObject();for(var entry:chosen)expected.addProperty(entry.key(),value(entry));Files.writeString(Path.of("build/favorites-expected.json"),expected.toString());
    Configs.MASA_FAVORITES_POSITION.setOptionListValue(MasaFavoritePosition.LEFT);screen=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,screen);stage=4;return;}
   if(stage==4){layout(screen,true);screenshot(mc);
    Object dropdown=field(GuiConfigsBase.class,"modSwitchWidget").get(screen);var mods=(List<fi.dy.masa.malilib.util.data.ModInfo>)field(WidgetDropDownList.class,"entries").get(dropdown);
    int index=java.util.stream.IntStream.range(0,mods.size()).filter(i->mods.get(i).modId().equals("halfmasa")).findFirst().orElseThrow();
    var select=dropdown.getClass().getDeclaredMethod("setSelectedEntry",int.class);select.setAccessible(true);select.invoke(dropdown,index);
    check(MinecraftClientCompat.getScreen(mc) instanceof HalfMasaConfigScreen,"native switcher opens original halfmasa screen");MinecraftClientCompat.setScreen(mc,screen);
    screen.resize(480,270);stage=5;return;}
   if(stage==5){layout(screen,true);MinecraftClientCompat.setScreen(mc,title);
    if(BASE_ONLY){Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(true);MinecraftClientCompat.setScreen(mc,new HalfMasaConfigScreen());stage=81;return;}
    GuiConfigsBase original=(GuiConfigsBase)Class.forName("fi.dy.masa.minihud.gui.GuiConfigs").getConstructor().newInstance();MinecraftClientCompat.setScreen(mc,original);stage=6;return;}
   if(stage>=6&&stage<=8){GuiConfigsBase original=(GuiConfigsBase)MinecraftClientCompat.getScreen(mc);layout(original,true);
    if(stage==6){Class<?> owner=original.getClass();var tab=owner.getField("tab");Class<?> tabs=Class.forName("fi.dy.masa.minihud.gui.GuiConfigs$ConfigGuiTab");
     for(Object category:tabs.getEnumConstants()){if(category.toString().equals("SHAPES"))continue;tab.set(null,category);var page=(GuiConfigsBase)owner.getConstructor().newInstance();MinecraftClientCompat.setScreen(mc,page);
      for(var option:page.getConfigs())if(option.getConfig() instanceof fi.dy.masa.malilib.hotkeys.IHotkey hotkey&&hotkey.getKeybind()==null)System.out.println("FAVORITES_NULL_KEYBIND "+option.getConfig().getClass().getName()+"/"+option.getConfig().getName());
      list(page).refreshEntries();check(true,"MiniHUD category opens and refreshes: "+category);
      if(category.toString().equals("INFO_LINES")){
       var info=page.getConfigs().stream().map(w->w.getConfig()).filter(c->c instanceof ConfigTypeWrapper).findFirst().orElseThrow();var first=MasaConfigCatalog.observe("minihud",info);
       list(page).refreshEntries();var recreated=page.getConfigs().stream().map(w->w.getConfig()).filter(c->c!=null&&c.getName().equals(info.getName())).findFirst().orElseThrow();
       check(info!=recreated&&MasaConfigCatalog.observe("minihud",recreated).key().equals(first.key()),"MiniHUD information-row wrappers retain stable identity after refresh");
       MasaFavoritesStore.get().setFavorite(first.key(),true);var infoPage=new MasaFavoritesScreen();MinecraftClientCompat.setScreen(mc,infoPage);
       check(infoPage.session().snapshot().stream().anyMatch(e->e.identity()==first.identity()),"MiniHUD information favorite uses original config");
       var infoRow=rows(infoPage).stream().filter(r->r.getEntry().getConfig()==first.editable()).findFirst().orElseThrow();var raw=(IConfigBoolean)first.identity();boolean before=raw.getBooleanValue();
       click(children(infoRow).stream().filter(w->w instanceof ConfigButtonBoolean).findFirst().orElseThrow());check(raw.getBooleanValue()!=before,"MiniHUD information favorite edits original boolean");
       MinecraftClientCompat.setScreen(mc,title);MasaFavoritesStore.get().setFavorite(first.key(),false);
      }
     }
     tab.set(null,java.lang.Enum.valueOf((Class)tabs,"GENERIC"));original=(GuiConfigsBase)MinecraftClientCompat.getScreen(mc);}
    check(field(GuiConfigsBase.class,"modSwitchWidget").get(original)!=null,"native mod switcher available: "+original.getModId());
    if(stage<8){String next=stage==6?"litematica":"tweakeroo";MinecraftClientCompat.setScreen(mc,(GuiConfigsBase)Class.forName("fi.dy.masa."+next+".gui.GuiConfigs").getConstructor().newInstance());stage++;return;}
    Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(true);MinecraftClientCompat.setScreen(mc,new HalfMasaConfigScreen());stage=81;return;}
   if(stage==81){layout((GuiConfigsBase)MinecraftClientCompat.getScreen(mc),true);
    Configs.MASA_FAVORITES_POSITION.setOptionListValue(MasaFavoritePosition.RIGHT);MinecraftClientCompat.setScreen(mc,new HalfMasaConfigScreen());stage=9;return;}
   if(stage==9){layout((GuiConfigsBase)MinecraftClientCompat.getScreen(mc),false);screenshot(mc);
    if(!BASE_ONLY)check(MasaConfigCatalog.observe("tweakeroo",chosen.get(4).editable()).key().equals(chosen.get(4).key()),"wrapper maps back to stable original key");
    Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);Configs.MASA_FAVORITES_PIN.setBooleanValue(true);
    var entries=List.of(new GuiConfigsBase.ConfigOptionWrapper(Configs.OPEN_TOOLS),new GuiConfigsBase.ConfigOptionWrapper(chosen.get(2).editable()));
    var sorted=MasaFavoritesOrder.sort(entries,"halfmasa",null);check(sorted.get(0).getConfig()==chosen.get(2).editable(),"stable partition pins favorite");
    var parent=new ConfigBoolean("fixtureParent",false);var child=new ConfigBoolean("fixtureChild",false);var tail=new ConfigBoolean("fixtureTail",false);
    var childEntry=MasaConfigCatalog.observe("fixture",child);MasaFavoritesStore.get().setFavorite(childEntry.key(),true);
    var grouped=MasaFavoritesOrder.sort(List.of(new GuiConfigsBase.ConfigOptionWrapper(tail),new GuiConfigsBase.ConfigOptionWrapper(parent),new GuiConfigsBase.ConfigOptionWrapper(child)),"fixture",new ConfigExpansionProvider(){public IConfigBase getExpansionParent(IConfigBase config){return config==child?parent:null;}public List<IConfigBase> getExpansionChildren(IConfigBase config){return config==parent?List.of(child):List.of();}});
    check(grouped.get(0).getConfig()==parent&&grouped.get(1).getConfig()==child&&grouped.get(2).getConfig()==tail,"favorite child moves with parent as a stable group");MasaFavoritesStore.get().setFavorite(childEntry.key(),false);
    MinecraftClientCompat.setScreen(mc,title);new Configs().save();
    var finalExpected=new com.google.gson.JsonObject();for(var entry:chosen)finalExpected.addProperty(entry.key(),value(entry));Files.writeString(Path.of("build/favorites-expected.json"),finalExpected.toString());
    Configs.CONFIG_GROUPING_EXPANDED.setBooleanValue(false);Configs.MASA_FAVORITES_EXPANDED.setBooleanValue(true);
    MinecraftClientCompat.setScreen(mc,new HalfMasaConfigScreen());
    var independent=(GuiConfigsBase)MinecraftClientCompat.getScreen(mc);var search=list(independent).getSearchBarWidget();search.setSearchOpen(true);
    ((GuiTextFieldGeneric)field(WidgetSearchBar.class,"searchBox").get(search)).setValueWrapper("masaFavorites");list(independent).refreshEntries();
    check(list(independent).getCurrentEntries().size()==7,"independent feature exposes master plus six settings with switcher child collapsed");
    stage=10;return;}
   if(stage==10){livePositionScreen=(GuiConfigsBase)MinecraftClientCompat.getScreen(mc);layout(livePositionScreen,false);screenshot(mc);
    var switcherRow=rows(livePositionScreen).stream().filter(r->r.getEntry().getConfig()==Configs.MASA_BETTER_SWITCHER).findFirst().orElseThrow();click(children(switcherRow).stream().filter(w->w instanceof ButtonBase&&"[+]".equals(buttonText(w))).findFirst().orElseThrow());
    check(list(livePositionScreen).getCurrentEntries().stream().anyMatch(w->w.getConfig()==Configs.MASA_SWITCHER_FAVORITES),"nested switcher child stays searchable by favorites ancestor");
    var row=rows(livePositionScreen).stream().filter(r->r.getEntry().getConfig()==Configs.MASA_FAVORITES_POSITION).findFirst().orElseThrow();click(children(row).stream().filter(w->w instanceof ConfigButtonOptionList).findFirst().orElseThrow());stage=11;return;}
   if(stage==11){check(MinecraftClientCompat.getScreen(mc)==livePositionScreen,"position change keeps current screen instance");layout(livePositionScreen,true);screenshot(mc);
    check(list(livePositionScreen).getSearchBarWidget().getFilter().equals("masafavorites"),"live position change preserves search");
    var row=rows(livePositionScreen).stream().filter(r->r.getEntry().getConfig()==Configs.MASA_FAVORITES_POSITION).findFirst().orElseThrow();click(children(row).stream().filter(w->w instanceof ConfigButtonOptionList).findFirst().orElseThrow());stage=12;return;}
   if(stage==12){check(MinecraftClientCompat.getScreen(mc)==livePositionScreen,"right position applies without reopening");layout(livePositionScreen,false);screenshot(mc);
    var dropdown=(WidgetDropDownList<fi.dy.masa.malilib.util.data.ModInfo>)field(GuiConfigsBase.class,"modSwitchWidget").get(livePositionScreen);check(dropdown instanceof MasaConfigSwitcher,"better switcher replaces only native dropdown widget");
    click(dropdown);var mods=(List<fi.dy.masa.malilib.util.data.ModInfo>)field(WidgetDropDownList.class,"filteredEntries").get(dropdown);
    int row=java.util.stream.IntStream.range(0,mods.size()).filter(i->mods.get(i).modId().equals("halfmasa")).findFirst().orElseThrow();
    dropdown.onMouseClicked(new MouseButtonEvent(dropdown.getX()+8,dropdown.getY()+dropdown.getHeight()+1+row*dropdown.getHeight()+dropdown.getHeight()/2.0,new MouseButtonInfo(fi.dy.masa.malilib.util.input.ScanCodes.OFFSET_MOUSE_LEFT,0)),false);
    check(MinecraftClientCompat.getScreen(mc)==livePositionScreen&&field(WidgetDropDownList.class,"isOpen").getBoolean(dropdown),"page star pins without switching screen or closing menu");
    check(mods.get(0).modId().equals("halfmasa")&&MasaFavoritesStore.get().contains("screen:halfmasa"),"favorite page immediately moves to top");
    Files.writeString(Path.of("build/favorite-page-expected.txt"),"screen:halfmasa");screenshot(mc);click(dropdown);
    Configs.MASA_SWITCHER_FAVORITES.setBooleanValue(false);stage=13;return;}
   if(stage==13){check(!MasaConfigSwitcher.canFavorite(),"page favorite toggle disables stars immediately");check(MasaFavoritesStore.get().contains("screen:halfmasa"),"disabling menu stars preserves favorites");
    Configs.MASA_BETTER_SWITCHER.setBooleanValue(false);stage=14;return;}
   if(stage==14){
    if(Boolean.getBoolean("favorites.gadgetFixture")){
     Object gadget=field(GuiConfigsBase.class,"masa_gadget$masaModGuiList").get(livePositionScreen);
     check(gadget!=null&&((io.github.halfmasa.xaerobinding.mixin.MasaFavoritesGuiWidgetsAccessor)livePositionScreen).halfmasa$getWidgets().contains(gadget),"disabling enhancement restores Gadget real MagicLib dropdown");
    }
    check(!(field(GuiConfigsBase.class,"modSwitchWidget").get(livePositionScreen) instanceof MasaConfigSwitcher),"disabling better switcher restores native widget");
    Configs.MASA_BETTER_SWITCHER.setBooleanValue(true);Configs.MASA_SWITCHER_FAVORITES.setBooleanValue(true);stage=15;return;}
   if(stage==15){
    if(Boolean.getBoolean("favorites.gadgetFixture"))check(field(GuiConfigsBase.class,"masa_gadget$masaModGuiList").get(livePositionScreen)==null,"enhancement detaches Gadget widget and late-render reference");
    check(field(GuiConfigsBase.class,"modSwitchWidget").get(livePositionScreen) instanceof MasaConfigSwitcher,"better switcher re-enables without reopening");
    var mods=new ArrayList<fi.dy.masa.malilib.util.data.ModInfo>();for(int i=0;i<40;i++)mods.add(new fi.dy.masa.malilib.util.data.ModInfo("fixture_page"+i,"Fixture extended configuration page "+i,HalfMasaConfigScreen::new));
    var large=new MasaConfigSwitcher(mc.getWindow().getGuiScaledWidth()-25,6,130,18,mods,mods.get(0));
    int capacity=field(WidgetDropDownList.class,"maxVisibleEntries").getInt(large);int expectedCapacity=Math.min(40,Math.max(1,(mc.getWindow().getGuiScaledHeight()-6-18-6)/18));
    check(capacity==expectedCapacity&&capacity>10,"dropdown uses available height beyond native ten-row cap");
    check(large.getY()+field(WidgetDropDownList.class,"totalHeight").getInt(large)<=mc.getWindow().getGuiScaledHeight(),"expanded dropdown remains inside screen");
    check(large.getX()>=0&&large.getX()+large.getWidth()<=mc.getWindow().getGuiScaledWidth(),"long mod names fit available screen width");
    click(large);var search=(fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper<?>)field(WidgetDropDownList.class,"searchBar").get(large);search.textField().setValueWrapper("configuration page 23");
    var refresh=WidgetDropDownList.class.getDeclaredMethod("updateFilteredEntries");refresh.setAccessible(true);refresh.invoke(large);
    large.onMouseClicked(new MouseButtonEvent(large.getX()+8,large.getY()+large.getHeight()+10,new MouseButtonInfo(fi.dy.masa.malilib.util.input.ScanCodes.OFFSET_MOUSE_LEFT,0)),false);
    check(search.textField().getValueWrapper().equals("configuration page 23")&&MasaFavoritesStore.get().contains("screen:fixture_page23"),"native menu search survives favorite toggle");
    MasaFavoritesStore.get().setFavorite("screen:fixture_page23",false);
    if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("magiclib")){
     check(MasaConfigCatalog.entries().stream().anyMatch(e->e.modId().equals("magiclib")&&e.identity().getName().equals("testConfigBoolean")),"MagicLib registry indexes inactive debug categories");
     livePositionScreen=(GuiConfigsBase)Class.forName("top.hendrixshen.magiclib.game.malilib.ConfigGui").getConstructor().newInstance();MinecraftClientCompat.setScreen(mc,livePositionScreen);stage=20;return;
    }
    MinecraftClientCompat.setScreen(mc,title);stage=90;return;}
   if(stage==20){
    var widget=field(GuiConfigsBase.class,"modSwitchWidget").get(livePositionScreen);
    check(MasaGadgetCompat.pages().stream().filter(m->m.modId().equals(livePositionScreen.getModId())).count()==1,"MagicLib nested entrypoint uses screen identity without duplicate pages");
    check(widget instanceof MasaConfigSwitcher,"MagicLib config page uses enhanced page switcher");
    check(((io.github.halfmasa.xaerobinding.mixin.MasaFavoritesGuiWidgetsAccessor)livePositionScreen).halfmasa$getWidgets().contains(widget),"MagicLib enhanced switcher remains visible after subclass initialization");
    layout(livePositionScreen,false);screenshot(mc);click((WidgetBase)widget);stage=21;return;}
   if(stage==21){screenshot(mc);MinecraftClientCompat.setScreen(mc,title);stage=90;return;}
   if(stage==16){screenshot(mc);
    var xs=new HashSet<Integer>();int noReset=0,total=0;
    String resetText=fi.dy.masa.malilib.util.StringUtils.translate("malilib.gui.button.reset.caps");
    for(int offset=0;offset<list(livePositionScreen).getCurrentEntries().size();offset++){
     list(livePositionScreen).getScrollbar().setValue(offset);list(livePositionScreen).refreshEntries();
     for(var row:rows(livePositionScreen)){
      var favorite=children(row).stream().filter(w->w instanceof MasaFavoriteButton).findFirst();if(favorite.isEmpty())continue;
      var reset=children(row).stream().filter(w->w instanceof ButtonBase&&resetText.equals(buttonText(w))).findFirst();
      System.out.println("LPCTOOLS_LAYOUT "+row.getEntry().getConfig().getName()+" star="+favorite.get().getX()+" reset="+(reset.isPresent()?reset.get().getX()+":"+reset.get().getWidth():"none")+" valueEnd="+field(row.getClass(),"halfmasa$valueEnd").getInt(row));
      for(var child:children(row))if(child instanceof ButtonBase&&child!=favorite.get())check(child.getX()+child.getWidth()<=favorite.get().getX()||child.getX()>=favorite.get().getX()+favorite.get().getWidth(),"LPCTools star never overlaps controls");
      xs.add(favorite.get().getX());total++;if(reset.isEmpty())noReset++;
      if(reset.isPresent())check(favorite.get().getX()==reset.get().getX()+reset.get().getWidth()+2,"LPCTools star immediately follows actual reset");
     }
    }
    check(noReset>0&&total>10,"LPCTools covers ordinary, action and reset-less rows across scroll");
    check(xs.size()==1,"LPCTools all right stars share one column including reset-less rows");
    Configs.MASA_FAVORITES_POSITION.setOptionListValue(MasaFavoritePosition.LEFT);stage=17;wait=0;return;}
   if(stage==17){for(var row:rows(livePositionScreen))for(var child:children(row))if(child instanceof MasaFavoriteButton)check(child.getX()==row.getX(),"LPCTools left star applies without reopening");
    Configs.MASA_FAVORITES_POSITION.setOptionListValue(MasaFavoritePosition.RIGHT);stage=18;wait=0;return;}
   if(stage==18){list(livePositionScreen).getScrollbar().setValue(6);list(livePositionScreen).refreshEntries();stage=19;wait=0;return;}
   if(stage==19){screenshot(mc);stage=90;return;}
   if(stage==90){org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();System.out.println("FAVORITES_PASS restart="+Boolean.getBoolean("favorites.restart"));done=true;mc.stop();}
  }catch(Throwable error){error.printStackTrace();System.out.println("FAVORITES_FAIL stage="+stage);done=true;mc.stop();}
 }
}'''
FRAME = r'''package io.github.halfmasa.favoritessmoke.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.favoritessmoke.FavoritesSmoke;
@Mixin(Minecraft.class) public class FrameMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void frame(boolean regularFrame,CallbackInfo ci){if(regularFrame)FavoritesSmoke.frame((Minecraft)(Object)this);}
}'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--version', default='26.3')
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Java/jdk-25.0.3'))
    parser.add_argument('--extra-mod', type=Path, action='append', default=[])
    parser.add_argument('--gadget-fixture', action='store_true')
    parser.add_argument('--lpctools-layout', action='store_true')
    parser.add_argument('--language', default='en_us')
    parser.add_argument('--base-only', action='store_true', help='Test halfmasa without optional Masa mods')
    parser.add_argument('--malilib', type=Path, help='Use the MaLiLib version required by optional test mods')
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()

    args.artifact = args.artifact.resolve()
    directory = ROOT / 'build/masa-favorites-tests' / datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    classes = directory / 'classes'
    classes.mkdir(parents=True)
    cp = compat.classpath(args.version, args.artifact)
    sources = []
    for name, source in [('FavoritesSmoke', SOURCE), ('FrameMixin', FRAME)]:
        file = directory / (name + '.java')
        file.write_text(compat.source(source, args.version, cp).replace('fi.dy.masa.malilib.util.input.ScanCodes.OFFSET_MOUSE_LEFT', 'com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT').replace('fi.dy.masa.malilib.util.input.ScanCodes.OFFSET_MOUSE_RIGHT', 'com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT').replace('fi.dy.masa.malilib.util.input.ScanCodes.SCAN_F8', 'org.lwjgl.glfw.GLFW.GLFW_KEY_F8') if args.version != '26.3' else source, encoding='utf-8')
        sources.append(str(file))
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-proc:none', '-encoding', 'UTF-8', '-cp', cp,
                             '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    mod = directory / 'favorites-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for file in classes.rglob('*.class'):
            jar.write(file, file.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_favorites_smoke', 'version': '1.0',
            'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.favoritessmoke.FavoritesSmoke']},
            'mixins': ['favorites-smoke.mixins.json']}))
        jar.writestr('favorites-smoke.mixins.json', json.dumps({'required': True,
            'package': 'io.github.halfmasa.favoritessmoke.mixin', 'compatibilityLevel': 'JAVA_21' if args.version.startswith('1.21.') else 'JAVA_25',
            'client': ['FrameMixin'], 'injectors': {'defaultRequire': 1}}))
    mod = compat.remap_fixture(args.version, args.java_home, mod, cp, directory)
    game = directory / 'game'
    (game / 'mods').mkdir(parents=True)
    (game / 'config').mkdir()
    (game / 'build').mkdir()
    api_version = compat.api_version(args.version)
    api = next(p for p in (Path('C:/Users/28656') / ('.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/' + api_version)).glob('*/*.jar') if not p.name.endswith('-sources.jar'))
    shutil.copy2(api, game / 'mods' / api.name)
    for extra in args.extra_mod:
        shutil.copy2(extra, game / 'mods' / extra.name)
    if args.gadget_fixture:
        import masa_gadget_fixture
        modmenu = next(p for p in args.extra_mod if p.name.startswith('modmenu-')).resolve()
        magic = next(p for p in args.extra_mod if p.name.startswith('MagicLib-'))
        with zipfile.ZipFile(magic) as bundle:
            name = next(n for n in bundle.namelist() if n.startswith('META-INF/jars/magiclib-malilib-extra-'))
            magic_extra = directory / 'magiclib-extra.jar'
            magic_extra.write_bytes(bundle.read(name))
        fixture = masa_gadget_fixture.build(directory, args.java_home, cp, modmenu, magic_extra)
        shutil.copy2(fixture, game / 'mods' / fixture.name)
    (game / 'config/malilib.json').write_text('{}\n', encoding='utf-8')
    half_config = game / 'config/halfmasa'
    half_config.mkdir(exist_ok=True)
    (half_config / 'halfmasa.json').write_text(json.dumps({'ConfigVersion': 55, 'Ported': {'masaFavoritesStarLeft': True}}), encoding='utf-8')
    (half_config / 'custom-config-groups.json').write_text(json.dumps({
        'initializedSources': ['halfmasa'], 'initializedDefaultVersions': ['halfmasa::v3'],
        'groups': [
            {'id': 'builtin_halfmasa_configGroupingGroup', 'source': 'halfmasa', 'name': 'Config grouping',
             'mainSource': 'halfmasa', 'mainConfig': 'tweakerooCollapsibleConfig', 'expanded': False,
             'children': ['halfmasa::customConfigGroups', 'halfmasa::openCustomConfigGroups',
                          'halfmasa::openMasaFavorites', 'halfmasa::masaFavoritesStarLeft', 'halfmasa::masaFavoritesPin']},
            {'id': 'user-keep', 'source': 'halfmasa', 'name': 'User layout',
             'mainSource': 'halfmasa', 'mainConfig': 'nightVisionFade',
             'children': ['halfmasa::nightVisionFadeSeconds'], 'expanded': False}
        ]}), encoding='utf-8')
    (game / 'options.txt').write_text(f'onboardAccessibility:false\ntutorialStep:none\nnarrator:0\nlang:{args.language}\n'
        'guiScale:1\nenableVsync:false\nmaxFps:60\nrenderDistance:2\nsimulationDistance:5\n'
        'pauseOnLostFocus:false\nsoundCategory_master:0.0\n', encoding='utf-8')
    reports = []
    for restart in [False, True]:
        # production_command creates natives each time; retain the first resolved command for restart.
        if not restart:
            command = startup.production_command(args, game, mod)
            if args.malilib:
                for installed in (game / 'mods').glob('*.jar'):
                    with zipfile.ZipFile(installed) as candidate:
                        metadata = json.loads(candidate.read('fabric.mod.json'))
                    if metadata.get('id') == 'malilib':
                        installed.unlink()
                shutil.copy2(args.malilib, game / 'mods' / args.malilib.name)
            command[1:1] = ['-Djava.awt.headless=true', '-Dsun.java2d.d3d=false', '-XX:ActiveProcessorCount=2', '-XX:TieredStopAtLevel=1']
        run_command = command[:]
        if args.gadget_fixture:
            run_command.insert(1, '-Dfavorites.gadgetFixture=true')
        if args.lpctools_layout:
            run_command.insert(1, '-Dfavorites.lpctoolsLayout=true')
        if args.base_only:
            run_command.insert(1, '-Dfavorites.baseOnly=true')
        if restart:
            run_command.insert(1, '-Dfavorites.restart=true')
        log_path = directory / ('restart.log' if restart else 'launcher.log')
        start = time.monotonic()
        with log_path.open('w', encoding='utf-8') as output:
            process = subprocess.Popen(run_command, cwd=game, stdin=subprocess.DEVNULL, stdout=output,
                                       stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
            try:
                code = process.wait(timeout=args.timeout)
            except subprocess.TimeoutExpired:
                subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
                code = process.wait(timeout=30)
        log = log_path.read_text(encoding='utf-8', errors='replace')
        passed = code == 0 and 'FAVORITES_PASS' in log and 'FAVORITES_FAIL' not in log
        reports.append({'result': 'PASS' if passed else 'FAIL', 'restart': restart, 'exit_code': code,
            'duration_seconds': round(time.monotonic() - start, 1), 'log': str(log_path),
            'checks': [line for line in log.splitlines() if 'FAVORITES_' in line]})
        if not passed:
            print('\n'.join(log.splitlines()[-70:]), flush=True)
            break
    report = {'version': args.version, 'result': 'PASS' if len(reports) == 2 and all(r['result'] == 'PASS' for r in reports) else 'FAIL',
        'artifact': str(args.artifact), 'sha256': hashlib.sha256(args.artifact.read_bytes()).hexdigest(),
        'extra_mods': [str(p) for p in args.extra_mod], 'runs': reports}
    report_path = directory / 'report.json'
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('FAVORITES:', report['result'], report_path, flush=True)
    return 0 if report['result'] == 'PASS' else 1


if __name__ == '__main__':
    raise SystemExit(main())

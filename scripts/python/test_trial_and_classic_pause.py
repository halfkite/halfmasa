"""Run production menu geometry and trial registration/cache logic in a focused Java harness."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
HARNESS = r"""
import java.util.*;
import java.lang.reflect.*;
import io.github.halfmasa.xaerobinding.gui.ClassicPauseLayout;
import io.github.halfmasa.xaerobinding.gui.ClassicPauseLayout.*;
import io.github.halfmasa.xaerobinding.feature.TrialCreativeTab;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
public class TrialPauseCheck {
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
 static void cache(boolean expected) throws Exception {
  var cls=Class.forName("io.github.halfmasa.xaerobinding.mixin.TrialCreativeTabMixin");
  var field=cls.getDeclaredField("CACHED_PARAMETERS");field.setAccessible(true);
  var marker=new CreativeModeTab.ItemDisplayParameters();field.set(null,marker);
  var method=cls.getDeclaredMethod("halfmasa_invalidateTrialContents",Class.forName("org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable"));method.setAccessible(true);method.invoke(null,new Object[]{null});
  check(expected?field.get(null)==null:field.get(null)==marker,"cache invalidation does not follow actual toggle changes");
 }
 static boolean overlap(Rect a,Rect b){return a.x()<b.x()+b.width()&&b.x()<a.x()+a.width()&&a.y()<b.y()+b.height()&&b.y()<a.y()+a.height();}
 static class PageScreen extends CreativeModeInventoryScreen implements FabricCreativeModeInventoryScreen {
  int page=1;CreativeModeTab selected;
  public PageScreen(CreativeModeTab selected){this.selected=selected;}
  public CreativeModeTab getSelectedTab(){return selected;}
  public int getPage(CreativeModeTab tab){return tab==TrialCreativeTab.get()?1:0;}
  public int getCurrentPage(){return page;}
  public boolean switchToPage(int wanted){page=wanted;selected=CreativeModeTabs.getDefaultTab();return true;}
  public boolean setSelectedTab(CreativeModeTab wanted){if(getPage(wanted)!=page)return false;selected=wanted;return true;}
 }
 public static void main(String[] args) throws Exception {switch(args[0]){
  case "classic" -> {
   var extras=Collections.nCopies(5,new Size(20,20));
   var l=ClassicPauseLayout.arrange(854,480,true,extras);
   check(l.main().size()==7 && l.extras().size()==5,"classic menu loses a main or extra button");
   Rect back=l.main().get(Role.RETURN),mods=l.main().get(Role.MODS),options=l.main().get(Role.OPTIONS),world=l.main().get(Role.WORLD_OPTIONS),exit=l.main().get(Role.EXIT);
   check(back.width()==204&&mods.width()==204&&exit.width()==204,"full-width classic rows lost");
   check(mods.y()==back.y()+48&&options.y()==mods.y()+24&&exit.y()==options.y()+24,"classic row order changed");
   check(options.y()==world.y()&&world.x()>options.x(),"world options not paired with options");
   for(Rect r:l.extras())check(r.x()>=back.x()+back.width()+4,"extra button not on the right");
  }
  case "title" -> {
   var l=ClassicPauseLayout.arrangeTitle(854,480,true,Collections.nCopies(5,new Size(20,20)));
   var back=l.main().get(Role.RETURN);
   check(back.y()==168,"title controls overlap logo area");
   for(var r:l.extras())check(r.x()>=back.x()+back.width()+4,"title extra controls not on the right");
  }
  case "resize" -> {
   for(int w:new int[]{240,320,426,640,854,1280})for(int h:new int[]{180,240,480})for(int n:new int[]{0,1,5,12,20}){
    var sizes=new ArrayList<Size>();for(int i=0;i<n;i++)sizes.add(new Size(i%3==0?100:20,i%5==0?24:20));
    var l=ClassicPauseLayout.arrange(w,h,true,sizes);
    var all=new ArrayList<Rect>(l.main().values());all.addAll(l.extras());
    check(l.extras().size()==n,"resizing loses extra controls");
    for(Rect r:all)check(r.width()>0&&r.height()>0&&r.x()>=0&&r.y()>=0&&r.x()+r.width()<=w&&r.y()+r.height()<=h,"control escapes the window at "+w+"x"+h+" with "+n+" extras");
    for(int i=0;i<all.size();i++)for(int j=i+1;j<all.size();j++)check(!overlap(all.get(i),all.get(j)),"controls overlap at "+w+"x"+h+" with "+n+" extras");
   }
  }
  case "without_modmenu" -> {
   var l=ClassicPauseLayout.arrange(854,480,false,List.of(new Size(20,20)));
   check(!l.main().containsKey(Role.MODS)&&l.main().size()==6,"missing Mod Menu leaves a fake Mods row");
   check(l.main().get(Role.OPTIONS).y()==l.main().get(Role.RETURN).y()+48,"missing Mods row leaves a blank gap");
  }
  case "registered" -> {
   FabricLoader.available=true;TrialCreativeTab.initialize();
   check(TrialCreativeTab.isRegistered()&&BuiltInRegistries.CREATIVE_MODE_TAB.values.size()==1,"trial tab not registered before freeze");
   check(BuiltInRegistries.CREATIVE_MODE_TAB.values.containsKey("halfmasa:trial"),"unstable registry identity");
   BuiltInRegistries.CREATIVE_MODE_TAB.frozen=true;
   var tab=TrialCreativeTab.get();tab.buildContents(new CreativeModeTab.ItemDisplayParameters());
   check(!tab.shouldDisplay()&&tab.getDisplayItems().isEmpty(),"disabled tab still displays");cache(false);
   Configs.TRIAL_CREATIVE_TAB.value=true;cache(true);cache(false);
   tab.buildContents(new CreativeModeTab.ItemDisplayParameters());
   check(tab.shouldDisplay()&&tab.getDisplayItems().size()==44,"registered generator does not populate visible contents");
   Configs.TRIAL_CREATIVE_TAB.value=false;cache(true);
   tab.buildContents(new CreativeModeTab.ItemDisplayParameters());
   check(!tab.shouldDisplay()&&tab.getDisplayItems().isEmpty(),"disabling leaves a stale visible tab");
   check(BuiltInRegistries.CREATIVE_MODE_TAB.values.size()==1&&TrialCreativeTab.get()==tab,"toggle registers again after freeze");
  }
  case "selection" -> {
   FabricLoader.available=true;TrialCreativeTab.initialize();
   var tab=TrialCreativeTab.get();var screen=new PageScreen(tab);
   TrialCreativeTab.synchronizeSelection(screen);
   check(screen.page==0&&screen.selected==CreativeModeTabs.getDefaultTab(),"hidden trial tab leaves selection trapped on its empty page");
   Configs.TRIAL_CREATIVE_TAB.value=true;tab.buildContents(new CreativeModeTab.ItemDisplayParameters());
   screen.selected=tab;screen.page=0;TrialCreativeTab.synchronizeSelection(screen);
   check(screen.page==1&&screen.selected==tab,"page synchronization loses a visible selected tab");
  }
  case "fallback" -> {
   FabricLoader.available=false;TrialCreativeTab.initialize();Configs.TRIAL_CREATIVE_TAB.value=true;
   check(!TrialCreativeTab.isRegistered()&&BuiltInRegistries.CREATIVE_MODE_TAB.values.isEmpty(),"no-API path touches the frozen registry");
   var cls=Class.forName("io.github.halfmasa.xaerobinding.mixin.TrialCreativeTabMixin");
   var method=cls.getDeclaredMethod("halfmasa$withTrialTab",List.class);method.setAccessible(true);
   List<CreativeModeTab> normal=List.of(CreativeModeTab.builder(CreativeModeTab.Row.TOP,0).build());
   check(method.invoke(null,normal)==normal,"synthetic tab is still appended and corrupts pagination");
   var items=new ArrayList<ItemStack>();TrialCreativeTab.fill(items);check(items.size()==44,"fallback cannot supply trial items");
   check(TrialCreativeTab.isTrialTab(TrialCreativeTab.get())&&!TrialCreativeTab.isTrialTab(null),"fallback identity is unstable");
  }
 }}
}
"""

class TrialAndClassicPauseTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.temp=tempfile.TemporaryDirectory(prefix="halfmasa-trial-pause-");cls.addClassCleanup(cls.temp.cleanup);cls.work=Path(cls.temp.name)
  java_home=Path(os.environ.get("JAVA_HOME","C:/Program Files/Java/jdk-25.0.3"));cls.java=str(java_home/"bin/java.exe");javac=str(java_home/"bin/javac.exe")
  stubs={
   "net/fabricmc/loader/api/FabricLoader.java":"package net.fabricmc.loader.api; public class FabricLoader {public static boolean available;public static FabricLoader getInstance(){return new FabricLoader();}public boolean isModLoaded(String id){return available;}}",
   "io/github/halfmasa/xaerobinding/config/Configs.java":"package io.github.halfmasa.xaerobinding.config; public class Configs {public static final Toggle TRIAL_CREATIVE_TAB=new Toggle();public static class Toggle {public boolean value;public boolean getBooleanValue(){return value;}}}",
   "io/github/halfmasa/xaerobinding/feature/TrialCreativeItems.java":"package io.github.halfmasa.xaerobinding.feature; import java.util.*;import net.minecraft.world.item.ItemStack;public class TrialCreativeItems {public static List<ItemStack> getItems(){var list=new ArrayList<ItemStack>();for(int i=0;i<44;i++)list.add(new ItemStack(i));return list;}}",
   "io/github/halfmasa/xaerobinding/feature/CondensedCreativeManager.java":"package io.github.halfmasa.xaerobinding.feature;public class CondensedCreativeManager {}",
   "net/minecraft/world/item/ItemStack.java":"package net.minecraft.world.item;public class ItemStack {public static final ItemStack EMPTY=new ItemStack(-1);private final Object item;public ItemStack(Object item){this.item=item;}public ItemStack copy(){return new ItemStack(item);}}",
   "net/minecraft/world/item/Items.java":"package net.minecraft.world.item;public class Items {public static final Object TRIAL_SPAWNER=new Object();}",
   "net/minecraft/network/chat/Component.java":"package net.minecraft.network.chat;public class Component {public static Component translatable(String key){return new Component();}}",
   "net/minecraft/resources/Identifier.java":"package net.minecraft.resources;public record Identifier(String value) {public static Identifier fromNamespaceAndPath(String ns,String key){return new Identifier(ns+\":\"+key);}}",
   "net/minecraft/core/Registry.java":"package net.minecraft.core;import java.util.*;import net.minecraft.resources.Identifier;public class Registry<T> {public boolean frozen;public final Map<String,T> values=new LinkedHashMap<>();public java.util.Optional<String> getResourceKey(T value){return values.entrySet().stream().filter(e->e.getValue()==value).map(e->e.getKey()).findFirst();}public static <T>T register(Registry<T> registry,Identifier id,T value){if(registry.frozen)throw new IllegalStateException(\"late registration\");if(registry.values.putIfAbsent(id.value(),value)!=null)throw new IllegalStateException(\"duplicate\");return value;}}",
   "net/minecraft/core/registries/BuiltInRegistries.java":"package net.minecraft.core.registries;import net.minecraft.core.Registry;import net.minecraft.world.item.CreativeModeTab;public class BuiltInRegistries {public static final Registry<CreativeModeTab> CREATIVE_MODE_TAB=new Registry<>();}",
   "net/minecraft/world/item/CreativeModeTab.java":"package net.minecraft.world.item;import java.util.*;import java.util.function.Supplier;import net.minecraft.network.chat.Component;public class CreativeModeTab {public enum Row {TOP,BOTTOM}public static class ItemDisplayParameters {}public interface Output {void accept(ItemStack stack);}public interface DisplayItemsGenerator {void accept(ItemDisplayParameters p,Output out);}private DisplayItemsGenerator generator=(p,o)->{};private final List<ItemStack> items=new ArrayList<>();public static Builder builder(Row row,int col){return new Builder();}public Collection<ItemStack> getDisplayItems(){return items;}public boolean shouldDisplay(){return !items.isEmpty();}public void buildContents(ItemDisplayParameters p){items.clear();generator.accept(p,items::add);net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.apply(items);}public static class Builder {private final CreativeModeTab tab=new CreativeModeTab();public Builder title(Component c){return this;}public Builder icon(Supplier<ItemStack> icon){return this;}public Builder displayItems(DisplayItemsGenerator generator){tab.generator=generator;return this;}public CreativeModeTab build(){return tab;}}}",
   "net/minecraft/world/item/CreativeModeTabs.java":"package net.minecraft.world.item;public class CreativeModeTabs {private static final CreativeModeTab DEFAULT=CreativeModeTab.builder(CreativeModeTab.Row.TOP,0).displayItems((p,o)->o.accept(new ItemStack(0))).build();static {DEFAULT.buildContents(new CreativeModeTab.ItemDisplayParameters());}public static CreativeModeTab getDefaultTab(){return DEFAULT;}}",
   "net/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen.java":"package net.minecraft.client.gui.screens.inventory;public class CreativeModeInventoryScreen {}",
   "net/fabricmc/fabric/api/client/creativetab/v1/FabricCreativeModeInventoryScreen.java":"package net.fabricmc.fabric.api.client.creativetab.v1;import net.minecraft.world.item.CreativeModeTab;public interface FabricCreativeModeInventoryScreen {CreativeModeTab getSelectedTab();int getPage(CreativeModeTab tab);int getCurrentPage();boolean switchToPage(int page);boolean setSelectedTab(CreativeModeTab tab);}",
   "net/fabricmc/fabric/api/creativetab/v1/FabricCreativeModeTab.java":"package net.fabricmc.fabric.api.creativetab.v1;import net.minecraft.world.item.CreativeModeTab;public class FabricCreativeModeTab {public static CreativeModeTab.Builder builder(){return CreativeModeTab.builder(CreativeModeTab.Row.TOP,0);}}",
   "net/fabricmc/fabric/api/creativetab/v1/CreativeModeTabEvents.java":"package net.fabricmc.fabric.api.creativetab.v1;import java.util.*;import java.util.function.Consumer;import net.minecraft.world.item.ItemStack;public class CreativeModeTabEvents {public static Consumer<FabricCreativeModeTabOutput> callback;public static Event modifyOutputEvent(String key){return new Event();}public static class Event {public void register(Consumer<FabricCreativeModeTabOutput> consumer){callback=consumer;}}public static void apply(List<ItemStack> items){if(callback!=null)callback.accept(new FabricCreativeModeTabOutput(items));}}",
   "net/fabricmc/fabric/api/creativetab/v1/FabricCreativeModeTabOutput.java":"package net.fabricmc.fabric.api.creativetab.v1;import java.util.*;import net.minecraft.world.item.ItemStack;public class FabricCreativeModeTabOutput {private final List<ItemStack> items;public FabricCreativeModeTabOutput(List<ItemStack> items){this.items=items;}public void accept(ItemStack stack){items.add(stack);}}",
   "org/spongepowered/asm/mixin/Mixin.java":"package org.spongepowered.asm.mixin;public @interface Mixin {Class<?>[] value();}",
   "org/spongepowered/asm/mixin/Shadow.java":"package org.spongepowered.asm.mixin;public @interface Shadow {}",
   "org/spongepowered/asm/mixin/Unique.java":"package org.spongepowered.asm.mixin;public @interface Unique {}",
   "org/spongepowered/asm/mixin/injection/At.java":"package org.spongepowered.asm.mixin.injection;public @interface At {String value();}",
   "org/spongepowered/asm/mixin/injection/Inject.java":"package org.spongepowered.asm.mixin.injection;public @interface Inject {String[] method();At at();boolean cancellable() default false;}",
   "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable.java":"package org.spongepowered.asm.mixin.injection.callback;public class CallbackInfoReturnable<T> {public T getReturnValue(){return null;}public void setReturnValue(T value){}}",
  }
  sources=[]
  for name,text in stubs.items():
   p=cls.work/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding="utf-8");sources.append(p)
  for name in ["gui/ClassicPauseLayout.java","feature/TrialCreativeTab.java","feature/TrialCreativeTabRegistration.java","mixin/TrialCreativeTabMixin.java"]:sources.append(ROOT/"src/main/java/io/github/halfmasa/xaerobinding"/name)
  harness=cls.work/"TrialPauseCheck.java";harness.write_text(HARNESS,encoding="utf-8");sources.append(harness)
  proc=subprocess.run([javac,"-encoding","UTF-8","-d",str(cls.work),*map(str,sources)],capture_output=True,text=True)
  if proc.returncode:raise AssertionError(proc.stdout+proc.stderr)
 def scenario(self,name):
  proc=subprocess.run([self.java,"-classpath",str(self.work),"TrialPauseCheck",name],capture_output=True,text=True);self.assertEqual(proc.returncode,0,proc.stdout+proc.stderr)
 def test_title_rows_clear_logo_and_extras_stay_right(self):self.scenario("title")
 def test_classic_rows_and_all_extra_buttons_on_right(self):self.scenario("classic")
 def test_small_windows_and_multiple_columns_never_overlap(self):self.scenario("resize")
 def test_without_modmenu_no_empty_middle_row(self):self.scenario("without_modmenu")
 def test_registered_trial_toggle_rebuilds_visibility_without_late_registration(self):self.scenario("registered")
 def test_hidden_trial_selection_returns_to_a_visible_page(self):self.scenario("selection")
 def test_without_api_keeps_tab_lists_and_provides_local_contents(self):self.scenario("fallback")

if __name__=="__main__":unittest.main()

"""Install a 26.3 artifact into an isolated survival client and verify real inventory transactions."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import zipfile
import run_startup_smoke as startup

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''
package io.github.halfmasa.eatsmoke;
import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.widgets.*;
import fi.dy.masa.malilib.gui.button.*;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.hotkeys.*;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.gui.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
public class AutoEatSmoke implements ClientModInitializer {
 public static boolean holdPackets,dropQuickRequests;
 public static int openMenus,quickRequests;
 public static int useRequests,foodSounds;
 public static boolean observeSilent;
 public static final Queue<Runnable> delayed=new ConcurrentLinkedQueue<>();
 public static boolean replay;
 private static AutoEatSmoke instance;
 private int stage,assertions;
 private long started;
 private boolean screenShot,compat;
 private ItemStack rpcExpected;
 private int rpcBeforeUses;
 private volatile UUID rpcBossId;
 private UUID unknownBossId;
 private volatile net.minecraft.world.entity.boss.wither.WitherBoss rpcBoss;
 private static final AutoEatService service=AutoEatService.getInstance();
 private volatile boolean fixtureReady;
 @Override public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.run(mc);}
 private void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 private static Field field(Class<?> type,String name)throws Exception{for(Class<?> c=type;c!=null;c=c.getSuperclass()){try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}}throw new NoSuchFieldException(name);}
 @SuppressWarnings("unchecked") private static List<ButtonBase> buttons(WidgetBase row)throws Exception{return ((List<WidgetBase>)field(row.getClass(),"subWidgets").get(row)).stream().filter(w->w instanceof ButtonBase).map(w->(ButtonBase)w).toList();}
 private static void click(ButtonBase button){button.onMouseClicked(new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new MouseButtonInfo(0,0)),false);}
 private void sourceLayout(HalfMasaConfigScreen screen)throws Exception{
  var list=new ActionConfigListWidget(10,52,700,240,240,0,false,screen);
  for(int width:new int[]{140,240,380}){
   var row=new ActionConfigOptionWidget(10,80,700,22,180,width,GuiConfigsBase.ConfigOptionWrapper.createFor(List.of(Configs.AUTO_EAT_SOURCE)).getFirst(),0,screen,list,list,HalfMasaConfigExpansionProvider.INSTANCE);
   var all=buttons(row);var mode=all.stream().min(Comparator.comparingInt(ButtonBase::getX)).orElseThrow();var key=(ConfigButtonKeybind)all.stream().filter(b->b instanceof ConfigButtonKeybind).findFirst().orElseThrow();var reset=all.stream().max(Comparator.comparingInt(ButtonBase::getX)).orElseThrow();
   check(mode.getWidth()==60&&key.getX()==mode.getX()+mode.getWidth()+2&&key.getX()+key.getWidth()==mode.getX()+width&&reset.getX()==mode.getX()+width+2,"source controls use native aligned columns width="+width);
   Configs.AUTO_EAT_SOURCE.resetToDefault();for(var expected:List.of(AutoEatSourceMode.INVENTORY,AutoEatSourceMode.QUICK_SHULKER,AutoEatSourceMode.HOTBAR)){click(mode);check(Configs.AUTO_EAT_SOURCE.getOptionListValue()==expected,"source button cycles");}
   Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);Configs.AUTO_EAT_SOURCE_HOTKEY.setValueFromString("F8");click(reset);check(Configs.AUTO_EAT_SOURCE.getOptionListValue()==AutoEatSourceMode.HOTBAR&&Configs.AUTO_EAT_SOURCE_HOTKEY.getStringValue().isEmpty(),"source reset restores mode and hotkey");
  }
  Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);
 }
 private static Object value(String name)throws Exception{Field field=AutoEatService.class.getDeclaredField(name);field.setAccessible(true);return field.get(service);}
 private static String state()throws Exception{return value("state").toString();}
 private long age(Minecraft mc){return mc.level.getGameTime()-started;}
 private void next(Minecraft mc,int stage){this.stage=stage;started=mc.level.getGameTime();System.out.println("AUTO_EAT_STAGE "+stage+" state="+safeState()+" hunger="+mc.player.getFoodData().getFoodLevel());}
 private static String safeState(){try{return state();}catch(Exception e){return "unknown";}}
 private static int foodCount(Minecraft mc){int count=0;for(int i=0;i<36;i++){var stack=mc.player.getInventory().getItem(i);if(stack.is(Items.BREAD))count+=stack.getCount();}return count;}
 private static ItemStack box(Item item,int bread){var stack=new ItemStack(Items.SHULKER_BOX);stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(item,bread))));return stack;}
 private void fixture(Minecraft mc,String kind,int hunger){
  Configs.AUTO_EAT.setBooleanValue(false);fixtureReady=false;mc.player.getInventory().setSelectedSlot(3);
  mc.getSingleplayerServer().execute(()->{
   var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var inv=p.getInventory();
   for(int i=0;i<inv.getContainerSize();i++)inv.setItem(i,ItemStack.EMPTY);
   p.getEnderChestInventory().clearContent();p.getFoodData().setFoodLevel(hunger);p.getFoodData().setSaturation(0);p.setHealth(20);inv.setSelectedSlot(3);
   inv.setItem(3,new ItemStack(Items.IRON_PICKAXE));
   switch(kind){
    case "hotbar" -> inv.setItem(0,new ItemStack(Items.BREAD,6));
    case "inventory" -> {
     for(int i=0;i<36;i++)inv.setItem(i,new ItemStack(Items.COBBLESTONE));
     inv.setItem(3,new ItemStack(Items.IRON_PICKAXE));inv.setItem(10,new ItemStack(Items.BREAD,8));
    }
    case "stew", "honey" -> {
     for(int i=0;i<36;i++)inv.setItem(i,new ItemStack(Items.COBBLESTONE,64));
     inv.setItem(3,new ItemStack(Items.IRON_PICKAXE));inv.setItem(10,kind.equals("stew")?new ItemStack(Items.MUSHROOM_STEW):new ItemStack(Items.HONEY_BOTTLE,8));
    }
    case "full" -> {
     for(int i=0;i<36;i++)inv.setItem(i,new ItemStack(Items.COBBLESTONE,64));
     inv.setItem(3,new ItemStack(Items.IRON_PICKAXE));inv.setItem(10,box(Items.BREAD,4));inv.setItem(11,new ItemStack(Items.ENDER_CHEST));
     p.getEnderChestInventory().setItem(0,new ItemStack(Items.BREAD,6));
    }
    case "shulker" -> {inv.setItem(10,box(Items.BREAD,4));inv.setItem(11,new ItemStack(Items.ENDER_CHEST));p.getEnderChestInventory().setItem(0,new ItemStack(Items.BREAD,6));}
    case "ender" -> {inv.setItem(10,box(Items.GOLDEN_APPLE,4));inv.setItem(11,new ItemStack(Items.ENDER_CHEST));p.getEnderChestInventory().setItem(0,new ItemStack(Items.BREAD,6));}
    case "nested" -> {inv.setItem(11,new ItemStack(Items.ENDER_CHEST));p.getEnderChestInventory().setItem(0,box(Items.BREAD,6));}
    case "remote-honey" -> {for(int i=0;i<36;i++)inv.setItem(i,new ItemStack(Items.COBBLESTONE,64));inv.setItem(3,new ItemStack(Items.IRON_PICKAXE));inv.setItem(11,new ItemStack(Items.ENDER_CHEST));inv.setItem(15,ItemStack.EMPTY);p.getEnderChestInventory().setItem(0,new ItemStack(Items.HONEY_BOTTLE,8));}
    case "priority" -> {inv.setItem(0,new ItemStack(Items.BREAD));inv.setItem(9,new ItemStack(Items.BREAD));inv.setItem(10,box(Items.BREAD,1));inv.setItem(11,new ItemStack(Items.ENDER_CHEST));p.getEnderChestInventory().setItem(0,new ItemStack(Items.BREAD));}
   }
   p.inventoryMenu.broadcastChanges();fixtureReady=true;
  });
 }
 private boolean synced(Minecraft mc,int hunger,Item item,int index){return fixtureReady&&mc.player.getFoodData().getFoodLevel()==hunger&&mc.player.getInventory().getItem(index).is(item)&&stateIdle();}
 private static boolean stateIdle(){try{return state().equals("IDLE")&&value("pendingRestore")==null&&value("pendingReturn")==null;}catch(Exception e){return false;}}
 private boolean finished(Minecraft mc){return mc.player.getFoodData().getFoodLevel()==20&&stateIdle()&&!mc.player.isUsingItem();}
 private void rules()throws Exception{
  check(!Configs.AUTO_EAT.getBooleanValue(),"default disabled");
  check(Configs.AUTO_EAT_SOURCE.getOptionListValue()==AutoEatSourceMode.HOTBAR,"default source");
  check(Configs.AUTO_EAT_START_HUNGER.getIntegerValue()==14&&Configs.AUTO_EAT_TARGET_HUNGER.getIntegerValue()==20,"default thresholds");
  check(Configs.AUTO_EAT_WHITELIST.getStrings().equals(List.of("minecraft:golden_carrot","minecraft:cooked_porkchop","minecraft:bread")),"default whitelist");
  check(Configs.AUTO_EAT_BLACKLIST.getStrings().equals(List.of("minecraft:golden_apple","minecraft:enchanted_golden_apple")),"default blacklist");
  check(AutoEatFoodRules.isAllowed(new ItemStack(Items.BREAD))&&!AutoEatFoodRules.isAllowed(new ItemStack(Items.GOLDEN_APPLE)),"whitelist selection");
  Configs.AUTO_EAT_FILTER_MODE.setOptionListValue(AutoEatFilterMode.BLACKLIST);
  check(!AutoEatFoodRules.isAllowed(new ItemStack(Items.GOLDEN_APPLE))&&!AutoEatFoodRules.isAllowed(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)),"blacklist selection");
  check(!AutoEatFoodRules.isAllowed(ItemStack.EMPTY)&&!AutoEatFoodRules.isAllowed(new ItemStack(Items.COBBLESTONE)),"empty and nonfood");
  check(AutoEatFoodRules.normalizeId(" BREAD ").equals("minecraft:bread")&&AutoEatFoodRules.normalizeId("bad id").isEmpty(),"ID normalization");
  Configs.AUTO_EAT_FILTER_MODE.resetToDefault();
  var bread=new ItemStack(Items.BREAD);var pork=new ItemStack(Items.COOKED_PORKCHOP);
  check(AutoEatFoodRules.isBetter(bread,pork,6)&&!AutoEatFoodRules.isBetter(pork,bread,6),"smart avoids overshoot");
  Configs.AUTO_EAT_PRIORITY.setOptionListValue(AutoEatFoodPriority.HIGH_NUTRITION);check(AutoEatFoodRules.isBetter(pork,bread,6),"high nutrition");
  Configs.AUTO_EAT_PRIORITY.setOptionListValue(AutoEatFoodPriority.LEFT_TO_RIGHT);check(!AutoEatFoodRules.isBetter(pork,bread,6),"left-to-right stable");Configs.AUTO_EAT_PRIORITY.resetToDefault();
  var key=Configs.AUTO_EAT_SOURCE_HOTKEY.getKeybind();var callback=((KeybindMulti)key).getCallback();
  for(var expected:List.of(AutoEatSourceMode.INVENTORY,AutoEatSourceMode.QUICK_SHULKER,AutoEatSourceMode.HOTBAR)){callback.onKeyAction(KeyAction.PRESS,key);check(Configs.AUTO_EAT_SOURCE.getOptionListValue()==expected,"source hotkey cycle");}
  check(Configs.HOTKEYS.contains(Configs.AUTO_EAT)&&Configs.HOTKEYS.contains(Configs.AUTO_EAT_SOURCE_HOTKEY),"hotkeys registered");
  check(HalfMasaConfigExpansionProvider.INSTANCE.getInlineCompanion(Configs.AUTO_EAT_SOURCE)==Configs.AUTO_EAT_SOURCE_HOTKEY,"source inline companion");
  Configs.AUTO_EAT_EXPANDED.setBooleanValue(false);check(!Configs.getClientView().contains(Configs.AUTO_EAT_SOURCE),"collapsed config");
  Configs.AUTO_EAT_EXPANDED.setBooleanValue(true);check(Configs.getClientView().contains(Configs.AUTO_EAT_SOURCE)&&Configs.getAllView().contains(Configs.AUTO_EAT_SOURCE),"expanded config");
  Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);Configs.AUTO_EAT_SOURCE_HOTKEY.setValueFromString("F8");new Configs().save();
  Configs.AUTO_EAT_SOURCE.resetToDefault();Configs.AUTO_EAT_SOURCE_HOTKEY.resetToDefault();new Configs().load();
  check(Configs.AUTO_EAT_SOURCE.getOptionListValue()==AutoEatSourceMode.QUICK_SHULKER&&Configs.AUTO_EAT_SOURCE_HOTKEY.getStringValue().equals("F8"),"source and hotkey persistence");
  Configs.AUTO_EAT_SOURCE.resetToDefault();Configs.AUTO_EAT_SOURCE_HOTKEY.resetToDefault();
  System.out.println("AUTO_EAT_RULES_PASS assertions="+assertions);
 }
 private void run(Minecraft mc){try{
  replay=true;try{if(!holdPackets)for(Runnable task;(task=delayed.poll())!=null;)task.run();}finally{replay=false;}
  if(stage==0){
   if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
   stage=1;System.out.println("AUTO_EAT_CREATING_WORLD");
   mc.createWorldOpenFlows().createFreshLevel("eat-fixture",new LevelSettings("Auto Eat Fixture",GameType.SURVIVAL,
    new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),true,WorldDataConfiguration.DEFAULT),
    new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;
  }
  if(stage==33){
   if(mc.player!=null||mc.level!=null)return;
   check(value("actor")==null&&value("pendingRestore")==null&&state().equals("IDLE"),"world exit clears all food state");
   System.out.println("AUTO_EAT_WORLD_PASS assertions="+assertions+" quickshulker="+compat+" menus="+openMenus+" requests="+quickRequests);stage=34;mc.stop();return;
  }
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  if(stage==1){if(mc.level.getGameTime()<50)return;rules();MinecraftClientCompat.setScreen(mc,null);compat=AutoEatQuickShulkerBridge.isAvailable();fixture(mc,"hotbar",15);next(mc,2);return;}
  if(age(mc)>500)throw new AssertionError("stage timeout "+stage+" state="+state()+" hunger="+mc.player.getFoodData().getFoodLevel()+" food="+foodCount(mc));
  switch(stage){
   case 2 -> {if(!synced(mc,15,Items.BREAD,0))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,3);}
   case 3 -> {if(age(mc)<40)return;check(mc.player.getFoodData().getFoodLevel()==15&&foodCount(mc)==6&&!mc.player.isUsingItem(),"above threshold does not eat");mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().getFoodData().setFoodLevel(14));next(mc,4);}
   case 4 -> {
    if(mc.player.isUsingItem()&&!screenShot){net.minecraft.client.Screenshot.grab(mc,false);screenShot=true;}
    if(!finished(mc))return;check(foodCount(mc)==4,"continuous hotbar eating consumed exactly two breads");check(mc.player.getInventory().getSelectedSlot()==3,"hotbar selection restored");fixture(mc,"inventory",14);next(mc,5);
   }
   case 5 -> {if(!synced(mc,14,Items.BREAD,10))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,6);}
   case 6 -> {if(age(mc)<60)return;check(foodCount(mc)==8&&mc.player.getFoodData().getFoodLevel()==14,"hotbar mode ignores inventory");holdPackets=true;Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);next(mc,7);}
   case 7 -> {if(!state().equals("WAIT_SWAP"))return;next(mc,8);}
   case 8 -> {if(age(mc)<30)return;check(!mc.player.isUsingItem()&&mc.player.getInventory().getItem(3).is(Items.IRON_PICKAXE)&&mc.player.getInventory().getItem(10).getCount()==8,"no prediction or use before delayed server confirmation");check(delayed.size()>=2,"delayed authoritative slot packets captured");holdPackets=false;next(mc,9);}
   case 9 -> {if(!finished(mc))return;check(mc.player.getInventory().getItem(3).is(Items.IRON_PICKAXE)&&mc.player.getInventory().getItem(10).is(Items.BREAD)&&mc.player.getInventory().getItem(10).getCount()==6,"inventory swap and remainder restored");check(mc.player.getInventory().getSelectedSlot()==3,"inventory selection restored");fixture(mc,"hotbar",14);Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.HOTBAR);next(mc,10);}
   case 10 -> {if(!synced(mc,14,Items.BREAD,0))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,11);}
   case 11 -> {if(!state().equals("EATING")||!mc.player.isUsingItem())return;mc.options.keyAttack.setDown(true);next(mc,12);}
   case 12 -> {if(age(mc)<5)return;check(!service.ownsUse(mc.player)&&foodCount(mc)==6,"manual attack interrupts automatic use");mc.options.keyAttack.setDown(false);fixture(mc,"stew",14);Configs.AUTO_EAT_FILTER_MODE.setOptionListValue(AutoEatFilterMode.BLACKLIST);Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);next(mc,40);}
   case 40 -> {if(!synced(mc,14,Items.MUSHROOM_STEW,10))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,41);}
   case 41 -> {if(!finished(mc))return;check(mc.player.getInventory().getItem(3).is(Items.IRON_PICKAXE)&&mc.player.getInventory().getItem(10).is(Items.BOWL),"single soup replaces food with bowl and restores tool in full inventory");fixture(mc,"honey",14);next(mc,42);}
   case 42 -> {if(!synced(mc,14,Items.HONEY_BOTTLE,10))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,43);}
   case 43 -> {
    if(age(mc)<60)return;check(mc.player.getFoodData().getFoodLevel()==14&&mc.player.getInventory().getItem(10).getCount()==8&&!mc.player.isUsingItem(),"full inventory refuses stacked drink before dropping bottles");
    check(mc.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,mc.player.getBoundingBox().inflate(8)).isEmpty(),"no dropped use remainder");
    fixtureReady=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.getInventory().setItem(15,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();fixtureReady=true;});next(mc,44);
   }
   case 44 -> {if(!finished(mc))return;int bottles=0;for(int i=0;i<36;i++)if(mc.player.getInventory().getItem(i).is(Items.GLASS_BOTTLE))bottles+=mc.player.getInventory().getItem(i).getCount();check(mc.player.getInventory().getItem(3).is(Items.IRON_PICKAXE)&&mc.player.getInventory().getItem(10).getCount()==7&&bottles==1,"drink resumes when space exists and keeps bottle and original tool");Configs.AUTO_EAT_FILTER_MODE.resetToDefault();fixture(mc,"full",14);Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);next(mc,13);}
   case 13 -> {if(!synced(mc,14,Items.SHULKER_BOX,10))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,14);}
   case 14 -> {if(age(mc)<60)return;check(openMenus==0&&mc.player.containerMenu==mc.player.inventoryMenu&&foodCount(mc)==0,"full inventory does not open containers");
    if(!compat){fixture(mc,"inventory",14);next(mc,15);}else{fixture(mc,"shulker",14);next(mc,17);}}
   case 15 -> {if(!synced(mc,14,Items.BREAD,10))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,16);}
   case 16 -> {if(!finished(mc))return;check(foodCount(mc)==6&&Configs.AUTO_EAT_SOURCE.getOptionListValue()==AutoEatSourceMode.QUICK_SHULKER,"missing QuickShulker falls back without changing chosen mode");fixture(mc,"inventory",14);Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);Configs.AUTO_EAT_SILENT.setBooleanValue(true);next(mc,70);}
   case 17 -> {if(!synced(mc,14,Items.SHULKER_BOX,10))return;Configs.AUTO_EAT_SILENT.setBooleanValue(true);rpcBeforeUses=useRequests;foodSounds=0;observeSilent=true;Configs.AUTO_EAT.setBooleanValue(true);next(mc,18);}
   case 18 -> {check(!mc.player.isUsingItem(),"silent QuickShulker meal never starts item use");if(!finished(mc))return;check(foodCount(mc)==0&&openMenus==2,"shulker remainder returned before ender chest");
    check(mc.player.getInventory().getItem(10).get(DataComponents.CONTAINER).nonEmptyItemCopyStream().filter(s->s.is(Items.BREAD)).mapToInt(ItemStack::getCount).sum()==2,"remaining bread restored to original shulker slot");check(mc.player.getInventory().getSelectedSlot()==3,"remote meal restores selection");check(useRequests==rpcBeforeUses&&foodSounds==0,"silent QuickShulker take and return emits no use or food sounds");observeSilent=false;Configs.AUTO_EAT_SILENT.setBooleanValue(false);fixture(mc,"ender",14);next(mc,19);}
   case 19 -> {if(!synced(mc,14,Items.ENDER_CHEST,11))return;Configs.AUTO_EAT_SOURCE.resetToDefault();Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);Configs.AUTO_EAT.setBooleanValue(true);next(mc,20);}
   case 20 -> {if(!finished(mc))return;check(foodCount(mc)==0&&openMenus==4,"ender remainder returned after filtered shulker");
    check(mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().getEnderChestInventory().getItem(0).getCount()==4,"ender remaining bread returned to original slot");fixture(mc,"nested",14);next(mc,21);}
   case 21 -> {if(!synced(mc,14,Items.ENDER_CHEST,11))return;Configs.AUTO_EAT_SOURCE.resetToDefault();Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);Configs.AUTO_EAT.setBooleanValue(true);next(mc,22);}
   case 22 -> {if(age(mc)<80)return;check(foodCount(mc)==0&&mc.player.getFoodData().getFoodLevel()==14&&openMenus==5,"no nested container search or repeated empty open");fixture(mc,"priority",1);next(mc,23);}
   case 23 -> {if(!synced(mc,1,Items.BREAD,0))return;Configs.AUTO_EAT_SOURCE.resetToDefault();Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);Configs.AUTO_EAT.setBooleanValue(true);next(mc,24);}
   case 24 -> {if(!finished(mc))return;check(foodCount(mc)==0&&openMenus==7,"all four source tiers supply one bread");check(mc.player.getInventory().getSelectedSlot()==3,"selection restored across multiple source tiers");fixture(mc,"remote-honey",14);Configs.AUTO_EAT_FILTER_MODE.setOptionListValue(AutoEatFilterMode.BLACKLIST);next(mc,60);}
   case 60 -> {if(!synced(mc,14,Items.ENDER_CHEST,11))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,61);}
   case 61 -> {
    if(age(mc)<100)return;check(openMenus==8&&mc.player.getFoodData().getFoodLevel()==14&&mc.player.getInventory().getItem(15).isEmpty(),"remote drink reserves remainder space and does not reopen blocked container");
    mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.getInventory().setItem(16,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});next(mc,62);
   }
   case 62 -> {
    if(!finished(mc))return;int honey=0,bottles=0;for(int i=0;i<36;i++){var s=mc.player.getInventory().getItem(i);if(s.is(Items.HONEY_BOTTLE))honey+=s.getCount();if(s.is(Items.GLASS_BOTTLE))bottles+=s.getCount();}
    check(openMenus==10&&honey==0&&bottles==1&&mc.player.getInventory().getItem(3).is(Items.IRON_PICKAXE),"blocked remote food retries after space changes and preserves items");
    check(mc.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,mc.player.getBoundingBox().inflate(8)).isEmpty(),"remote drink drops no items");
    Configs.AUTO_EAT_FILTER_MODE.resetToDefault();fixture(mc,"inventory",14);Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);Configs.AUTO_EAT_SILENT.setBooleanValue(true);next(mc,70);
   }
   case 70 -> {if(!synced(mc,14,Items.BREAD,10))return;rpcBeforeUses=useRequests;foodSounds=0;observeSilent=true;Configs.AUTO_EAT.setBooleanValue(true);next(mc,71);}
   case 71 -> {
    check(!mc.player.isUsingItem()&&mc.player.getInventory().getSelectedSlot()==3,"silent inventory meal has no use or selection change");
    if(!finished(mc))return;check(foodCount(mc)==6&&mc.player.getInventory().getItem(3).is(Items.IRON_PICKAXE),"silent inventory consumes food and retains tool");
    check(useRequests==rpcBeforeUses&&foodSounds==0,"silent consumption emits no use requests or food sounds: use="+useRequests+" before="+rpcBeforeUses+" sounds="+foodSounds);observeSilent=false;
    Configs.AUTO_EAT_SILENT.setBooleanValue(false);
    if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("carpet-fga-addition")){fixture(mc,"inventory",14);next(mc,80);return;}
    if(!compat){next(mc,30);return;}fixture(mc,"shulker",14);Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);dropQuickRequests=true;next(mc,25);
   }
   case 80 -> {
    if(!synced(mc,14,Items.BREAD,10))return;check(HalfMasaSupportNetwork.canEat()&&HalfMasaSupportNetwork.canHealth(),"FGA advertised both support channels");
    rpcExpected=mc.player.getInventory().getItem(10).copy();rpcBeforeUses=useRequests;foodSounds=0;observeSilent=true;
    HalfMasaSupportNetwork.eat(new HalfMasaSupportPayloads.Eat(1,8000001L,10,rpcExpected,20,true));next(mc,81);
   }
   case 81 -> {
    String status=HalfMasaSupportClient.takeResult(8000001L);if(status==null)return;
    check(status.equals("DISABLED")&&foodCount(mc)==8&&mc.player.getFoodData().getFoodLevel()==14,"FGA rule defaults disabled and consumes nothing");
    fixtureReady=false;mc.getSingleplayerServer().execute(()->{mc.getSingleplayerServer().getCommands().performPrefixedCommand(mc.getSingleplayerServer().createCommandSourceStack(),"carpet silentAutoEat true");fixtureReady=true;});next(mc,82);
   }
   case 82 -> {if(!fixtureReady)return;HalfMasaSupportNetwork.eat(new HalfMasaSupportPayloads.Eat(1,8000002L,10,rpcExpected,20,true));next(mc,83);}
   case 83 -> {
    String status=HalfMasaSupportClient.takeResult(8000002L);if(status!=null)check(status.equals("OK"),"FGA silent consume accepts enabled rule");
    if(mc.player.getFoodData().getFoodLevel()!=19||foodCount(mc)!=7)return;
    check(!mc.player.isUsingItem()&&mc.player.getInventory().getSelectedSlot()==3&&useRequests==rpcBeforeUses&&foodSounds==0,"FGA consumption has no animation, selected-slot change, use request or eating sound");
    HalfMasaSupportNetwork.eat(new HalfMasaSupportPayloads.Eat(1,8000002L,10,rpcExpected,20,true));next(mc,84);
   }
   case 84 -> {
    String status=HalfMasaSupportClient.takeResult(8000002L);if(status==null)return;check(status.equals("OK")&&foodCount(mc)==7&&mc.player.getFoodData().getFoodLevel()==19,"duplicate FGA nonce returns cached result without eating twice");
    HalfMasaSupportNetwork.eat(new HalfMasaSupportPayloads.Eat(1,8000002L,10,mc.player.getInventory().getItem(10).copy(),20,true));next(mc,85);
   }
   case 85 -> {
    String status=HalfMasaSupportClient.takeResult(8000002L);if(status==null)return;check(status.equals("REQUEST_ID_REUSED")&&foodCount(mc)==7,"reused nonce with different stack rejected");
    HalfMasaSupportNetwork.eat(new HalfMasaSupportPayloads.Eat(1,8000003L,10,rpcExpected,20,true));next(mc,86);
   }
   case 86 -> {
    String status=HalfMasaSupportClient.takeResult(8000003L);if(status==null)return;check(status.equals("STALE")&&foodCount(mc)==7,"stale expected stack cannot consume");
    fixtureReady=false;mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();if(Math.abs(p.getFoodData().getSaturationLevel()-6.0F)>0.001F)throw new AssertionError("vanilla saturation not preserved");}finally{fixtureReady=true;}});next(mc,87);
   }
   case 87 -> {
    if(!fixtureReady)return;HalfMasaSupportNetwork.eat(new HalfMasaSupportPayloads.Eat(1,7999999L,10,mc.player.getInventory().getItem(10).copy(),20,true));next(mc,88);
   }
   case 88 -> {
    String status=HalfMasaSupportClient.takeResult(7999999L);if(status==null)return;check(status.equals("EXPIRED_REQUEST")&&foodCount(mc)==7,"old unseen nonce cannot consume again");observeSilent=false;
    fixtureReady=false;mc.getSingleplayerServer().execute(()->{
     var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
     rpcBoss=net.minecraft.world.entity.EntityTypes.WITHER.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
     rpcBoss.setPos(p.getX()+4,p.getY()+4,p.getZ());rpcBoss.setNoAi(true);rpcBoss.setNoGravity(true);rpcBoss.setHealth(240);
     p.level().addFreshEntity(rpcBoss);var event=((io.github.halfmasa.xaerobinding.mixin.WitherBossEventAccessor)rpcBoss).halfmasa$bossEvent();event.addPlayer(p);rpcBossId=event.getId();fixtureReady=true;
    });next(mc,89);
   }
   case 89 -> {
    if(!fixtureReady)return;HalfMasaSupportNetwork.queryHealth(new HalfMasaSupportPayloads.HealthQuery(rpcBossId));next(mc,90);
   }
   case 90 -> {
    var health=HalfMasaSupportClient.health(rpcBossId);if(health==null)return;check(health.valid()&&health.current()==240&&health.maximum()==300,"FGA health wire reply identifies actual Wither by boss UUID");
    unknownBossId=UUID.randomUUID();HalfMasaSupportNetwork.queryHealth(new HalfMasaSupportPayloads.HealthQuery(unknownBossId));next(mc,91);
   }
   case 91 -> {
    var health=HalfMasaSupportClient.health(unknownBossId);if(health==null)return;check(!health.valid()&&health.maximum()==0,"unknown boss UUID supplies no invented health");
    mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();((io.github.halfmasa.xaerobinding.mixin.WitherBossEventAccessor)rpcBoss).halfmasa$bossEvent().removePlayer(p);rpcBoss.discard();});
    if(!compat){next(mc,30);return;}fixture(mc,"shulker",14);Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);dropQuickRequests=true;next(mc,25);
   }
   case 25 -> {if(!synced(mc,14,Items.SHULKER_BOX,10))return;Configs.AUTO_EAT_SOURCE.resetToDefault();Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.QUICK_SHULKER);Configs.AUTO_EAT.setBooleanValue(true);next(mc,26);}
   case 26 -> {if(!(boolean)value("remoteSuppressed"))return;check(mc.player.containerMenu==mc.player.inventoryMenu&&foodCount(mc)==0,"unresponsive open stops remote attempts");fixture(mc,"inventory",14);next(mc,27);}
   case 27 -> {if(!synced(mc,14,Items.BREAD,10))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,28);}
   case 28 -> {if(!finished(mc))return;check(foodCount(mc)==6&&openMenus==10,"timeout retains ordinary inventory eating");next(mc,30);}
   case 30 -> {
    Configs.AUTO_EAT.setBooleanValue(false);Configs.AUTO_EAT_EXPANDED.setBooleanValue(true);ConfigScrollMemory.saveSelectedTab("ALL");
    GuiBase.openGui(new HalfMasaConfigScreen());var screen=MinecraftClientCompat.getScreen(mc);sourceLayout((HalfMasaConfigScreen)screen);
    Method getList=GuiListBase.class.getDeclaredMethod("getListWidget");getList.setAccessible(true);var list=(WidgetListConfigOptions)getList.invoke(screen);var search=list.getSearchBarWidget();search.setSearchOpen(true);
    Field box=WidgetSearchBar.class.getDeclaredField("searchBox");box.setAccessible(true);((GuiTextFieldGeneric)box.get(search)).setValue("autoEat");list.refreshEntries();next(mc,31);
   }
   case 31 -> {if(age(mc)<10)return;net.minecraft.client.Screenshot.grab(mc,false);next(mc,32);}
   case 32 -> {if(age(mc)<10)return;MinecraftClientCompat.setScreen(mc,null);fixture(mc,"inventory",14);Configs.AUTO_EAT_SOURCE.setOptionListValue(AutoEatSourceMode.INVENTORY);next(mc,50);}
   case 50 -> {if(!synced(mc,14,Items.BREAD,10))return;Configs.AUTO_EAT.setBooleanValue(true);next(mc,51);}
   case 51 -> {if(!mc.player.isUsingItem()||!state().equals("EATING"))return;mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setHealth(0));next(mc,52);}
   case 52 -> {if(mc.player.isAlive()||!stateIdle())return;check(!service.ownsUse(mc.player)&&value("pendingRestore")==null,"death cancels owned use and pending restore");stage=33;mc.disconnectWithSavingScreen();}
  }
 }catch(Throwable failure){throw new IllegalStateException("Auto Eat smoke failed stage="+stage+" state="+safeState(),failure);}}
}
'''

FRAME = '''package io.github.halfmasa.eatsmoke.mixin;
import io.github.halfmasa.eatsmoke.AutoEatSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class AutoEatFrameMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void eatSmoke$frame(CallbackInfo ci){AutoEatSmoke.frame((Minecraft)(Object)this);}
}'''

DELAY = '''package io.github.halfmasa.eatsmoke.mixin;
import io.github.halfmasa.eatsmoke.AutoEatSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ClientPacketListener.class,priority=2000) public class AutoEatDelayMixin {
 @Inject(method="handleSoundEvent",at=@At("TAIL")) private void eatSmoke$sound(ClientboundSoundPacket p,CallbackInfo ci){if(AutoEatSmoke.observeSilent){var id=net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(p.getSound().value()).toString();if(id.endsWith(".eat")||id.endsWith(".drink")||id.endsWith(".burp")){AutoEatSmoke.foodSounds++;System.out.println("AUTO_EAT_SILENT_SOUND "+id);}}}
 @Inject(method="handleSoundEntityEvent",at=@At("TAIL")) private void eatSmoke$entitySound(ClientboundSoundEntityPacket p,CallbackInfo ci){if(AutoEatSmoke.observeSilent){var id=net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(p.getSound().value()).toString();if(id.endsWith(".eat")||id.endsWith(".drink")||id.endsWith(".burp"))AutoEatSmoke.foodSounds++;}}
 private boolean eatSmoke$hold(){return Minecraft.getInstance().isSameThread()&&AutoEatSmoke.holdPackets&&!AutoEatSmoke.replay;}
 @Inject(method="handleContainerSetSlot",at=@At("HEAD"),cancellable=true) private void eatSmoke$slot(ClientboundContainerSetSlotPacket p,CallbackInfo ci){if(eatSmoke$hold()){AutoEatSmoke.delayed.add(()->((ClientPacketListener)(Object)this).handleContainerSetSlot(p));ci.cancel();}}
 @Inject(method="handleContainerContent",at=@At("HEAD"),cancellable=true) private void eatSmoke$content(ClientboundContainerSetContentPacket p,CallbackInfo ci){if(eatSmoke$hold()){AutoEatSmoke.delayed.add(()->((ClientPacketListener)(Object)this).handleContainerContent(p));ci.cancel();}}
 @Inject(method="handleSetPlayerInventory",at=@At("HEAD"),cancellable=true) private void eatSmoke$inventory(ClientboundSetPlayerInventoryPacket p,CallbackInfo ci){if(eatSmoke$hold()){AutoEatSmoke.delayed.add(()->((ClientPacketListener)(Object)this).handleSetPlayerInventory(p));ci.cancel();}}
 @Inject(method="handleOpenScreen",at=@At("TAIL")) private void eatSmoke$open(ClientboundOpenScreenPacket p,CallbackInfo ci){AutoEatSmoke.openMenus++;}
}'''

PROBE = '''package io.github.halfmasa.eatsmoke.mixin;
import io.github.halfmasa.eatsmoke.AutoEatSmoke;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientCommonPacketListenerImpl.class) public class AutoEatProbeMixin {
 @Inject(method="send",at=@At("HEAD"),cancellable=true) private void eatSmoke$send(Packet<?> packet,CallbackInfo ci){
  if(packet instanceof net.minecraft.network.protocol.game.ServerboundUseItemPacket)AutoEatSmoke.useRequests++;
  if(packet instanceof ServerboundCustomPayloadPacket p && p.payload().type().id().toString().equals("quickshulker:open_shulker_packet")){
   AutoEatSmoke.quickRequests++;if(AutoEatSmoke.dropQuickRequests)ci.cancel();
  }
 }
}'''


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--timeout", type=int, default=240)
    parser.add_argument("--extra-mod", action="append", type=Path, default=[])
    parser.add_argument("--jvm-arg", action="append", default=[], help="Additional JVM option for the isolated test client")
    parser.add_argument("--language", default="en_us", help="Test client language")
    parser.add_argument("--without-api", action="store_true", help="Verify the standalone integrated-server implementation without Fabric API")
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    if not args.artifact.is_file(): parser.error(f"Artifact not found: {args.artifact}")
    args.version = "26.3"
    directory = ROOT / "build/auto-eat-tests" / datetime.now(timezone(timedelta(hours=8))).strftime("%Y%m%d-%H%M%S-%f")
    directory.mkdir(parents=True)
    sources = []
    for name, content in [("AutoEatSmoke", SOURCE), ("AutoEatFrameMixin", FRAME), ("AutoEatDelayMixin", DELAY), ("AutoEatProbeMixin", PROBE)]:
        path = directory / (name + ".java")
        path.write_text(content, encoding="utf-8")
        sources.append(path)
    classes = directory / "classes"
    classes.mkdir()
    cp = str(args.artifact) + os.pathsep + (ROOT / "build/port-classpath-26.3.txt").read_text(encoding="utf-8").strip()
    result = subprocess.run([str(args.java_home / "bin/javac.exe"), "-proc:none", "-encoding", "UTF-8", "-cp", cp, "-d", str(classes), *map(str, sources)], capture_output=True)
    if result.returncode:
        print(result.stderr.decode("utf-8", errors="replace"), flush=True)
        return result.returncode
    mod = directory / "auto-eat-smoke.jar"
    with zipfile.ZipFile(mod, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_auto_eat_smoke", "version": "1.0", "environment": "client", "entrypoints": {"client": ["io.github.halfmasa.eatsmoke.AutoEatSmoke"]}, "mixins": ["auto-eat-smoke.mixins.json"]}))
        jar.writestr("auto-eat-smoke.mixins.json", json.dumps({"required": True, "package": "io.github.halfmasa.eatsmoke.mixin", "compatibilityLevel": "JAVA_25", "client": ["AutoEatFrameMixin", "AutoEatDelayMixin", "AutoEatProbeMixin"], "injectors": {"defaultRequire": 1}}))
    game = directory / "game"
    (game / "mods").mkdir(parents=True)
    (game / "config").mkdir()
    (game / "config/malilib.json").write_text("{}\n", encoding="utf-8")
    (game / "options.txt").write_text("onboardAccessibility:false\ntutorialStep:none\nnarrator:0\nlang:" + args.language + "\nguiScale:1\nenableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\n", encoding="utf-8")
    api = next((Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.161.0+26.3").glob("*/*.jar"))
    if not args.without_api: shutil.copy2(api, game / "mods" / api.name)
    for extra in args.extra_mod:
        shutil.copy2(extra, game / "mods" / extra.name)
    command = startup.production_command(args, game, mod)
    command[command.index("--width") + 1] = "960"
    command[command.index("--height") + 1] = "540"
    command[1:1] = args.jvm_arg + ["-XX:ErrorFile=" + str(directory / "jvm-crash.log")]
    log_path = directory / "launcher.log"
    started = time.monotonic()
    timed_out = False
    with log_path.open("w", encoding="utf-8") as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            timed_out = True
            subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], capture_output=True, check=False)
            code = process.wait(timeout=30)
    log = log_path.read_text(encoding="utf-8", errors="replace")
    passed = code == 0 and not timed_out and "AUTO_EAT_WORLD_PASS" in log
    report = {"result": "PASS" if passed else "FAIL", "artifact": str(args.artifact), "extra_mods": [str(path.resolve()) for path in args.extra_mod], "jvm_args": args.jvm_arg, "duration_seconds": round(time.monotonic() - started, 1), "exit_code": code, "timed_out": timed_out, "log": str(log_path), "checks": [line for line in log.splitlines() if "AUTO_EAT_" in line], "screenshots": [str(path) for path in (game / "screenshots").glob("*.png")]}
    path = directory / "report.json"
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"AUTO EAT: {report['result']}\n{path}", flush=True)
    if not passed:
        print("\n".join(log.splitlines()[-45:]), flush=True)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

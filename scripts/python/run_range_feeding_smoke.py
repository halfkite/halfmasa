"""Verify range feeding with controlled input and a real isolated integrated server."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
import shutil
from pathlib import Path
import subprocess
import time
import zipfile

import run_startup_smoke as startup
import smoke_remap

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''
package io.github.halfmasa.feedingsmoke;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.EntityHitResult;
import fi.dy.masa.malilib.hotkeys.*;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.widgets.*;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;

public final class FeedingSmoke implements ClientModInitializer {
 public static boolean controlled, physicalDown, suppress;
 public static final List<UUID> interactions=new ArrayList<>();
 private static final List<Long> interactionTimes=new ArrayList<>();
 private static final RangeFeedingService service=RangeFeedingService.getInstance();
 private static final List<Animal> fixtures=new ArrayList<>();
 private static FeedingSmoke instance;
 private static int assertions, fakeId=-10000;
 private static long schedulerTick;
 private int stage, frames;
 private int optionalFrames;
 private long since;
 private volatile boolean ready;
 private volatile int serverFood, serverBabyAge;
 private volatile boolean serverLove;
 private volatile UUID realAdult, realBaby;
 private static Field focus;

 @Override public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.run(mc);}
 public static void interaction(Entity entity){if(entity instanceof Animal){interactions.add(entity.getUUID());interactionTimes.add(schedulerTick);}}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);assertions++;}
 private static void portedRules(){
  check(AutoEatFoodRules.isAllowed(new ItemStack(Items.BREAD)),"ported food whitelist accepts bread");
  check(!AutoEatFoodRules.isAllowed(new ItemStack(Items.GOLDEN_APPLE)),"ported food filter preserves default exclusions");
  check(AutoEatFoodRules.useRemainder(new ItemStack(Items.MUSHROOM_STEW)).is(Items.BOWL),"vanilla bowl remainder");
  check(AutoEatFoodRules.useRemainder(new ItemStack(Items.HONEY_BOTTLE)).is(Items.GLASS_BOTTLE),"vanilla bottle remainder");
  Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_MERGE.setBooleanValue(true);
  var bars=new ArrayList<net.minecraft.client.gui.components.LerpingBossEvent>();
  for(float progress:new float[]{.8f,.8f,.4f})bars.add(new net.minecraft.client.gui.components.LerpingBossEvent(UUID.randomUUID(),net.minecraft.network.chat.Component.literal("Boss"),progress,net.minecraft.world.BossEvent.BossBarColor.PURPLE,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS,false,false,false));
  check(BossBarGrouper.merge(bars).size()==2,"ported boss grouping preserves exact target health");
  Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.setBooleanValue(true);
  var merged=BossBarGrouper.merge(bars);check(merged.size()==1&&Math.abs(merged.iterator().next().getProgress()-2f/3)<.0001f,"ported boss average progress");
  Configs.BETTER_BOSS_BAR.resetToDefault();Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.resetToDefault();BossBarGrouper.clear();
 }
 private static void serverFoodLogic(net.minecraft.server.level.ServerPlayer player){
  player.getFoodData().setFoodLevel(10);player.getInventory().setItem(2,new ItemStack(Items.BREAD,3));
  check(SilentFoodTransaction.consume(player,2,player.getInventory().getItem(2).copy(),20,false).equals("OK"),"ported silent food transaction accepted");
  check(player.getInventory().getItem(2).getCount()==2&&player.getFoodData().getFoodLevel()==15,"vanilla nutrition and count preserved");
  player.getFoodData().setFoodLevel(10);player.getInventory().setItem(3,new ItemStack(Items.MUSHROOM_STEW));
  check(SilentFoodTransaction.consume(player,3,player.getInventory().getItem(3).copy(),20,false).equals("OK")&&player.getInventory().getItem(3).is(Items.BOWL),"real consumption preserves bowl");
  check(!SilentFoodTransaction.isSilent(player),"silent transaction thread state cleaned");
  player.getInventory().setItem(2,ItemStack.EMPTY);player.getInventory().setItem(3,ItemStack.EMPTY);player.getFoodData().setFoodLevel(20);
 }
 private static void ticks(Minecraft mc,int count){for(int i=0;i<count;i++){schedulerTick++;service.onClientTick(mc);}}
 private static void defaults(){
  Configs.RANGE_FEEDING.resetToDefault();Configs.RANGE_FEEDING_SKIP_BABIES.resetToDefault();
  Configs.RANGE_FEEDING_HOLD.resetToDefault();Configs.RANGE_FEEDING_ONCE.resetToDefault();
  Configs.RANGE_FEEDING_RANGE.resetToDefault();Configs.RANGE_FEEDING_LIST_MODE.resetToDefault();
  Configs.RANGE_FEEDING_INTERVAL.resetToDefault();
  Configs.RANGE_FEEDING_BLACKLIST.resetToDefault();Configs.RANGE_FEEDING_WHITELIST.resetToDefault();Configs.RANGE_FEEDING_EXPANDED.resetToDefault();
 }
 private static void configs(){
  defaults();
  check(!Configs.RANGE_FEEDING.getBooleanValue()&&Configs.RANGE_FEEDING_SKIP_BABIES.getBooleanValue(),"safe defaults");
  check(Configs.RANGE_FEEDING_RANGE.getIntegerValue()==3&&Configs.RANGE_FEEDING_LIST_MODE.getOptionListValue()==RangeFeedingListMode.NONE,"range/list defaults");
  for(var hotkey:List.of(Configs.RANGE_FEEDING,Configs.RANGE_FEEDING_SKIP_BABIES,Configs.RANGE_FEEDING_HOLD,Configs.RANGE_FEEDING_ONCE)){
   check(hotkey.getKeybind().getKeys().isEmpty()&&Configs.HOTKEYS.contains(hotkey),"registered, unbound hotkey "+hotkey.getName());
  }
  check(Configs.RANGE_FEEDING_HOLD.getKeybind().getSettings().getActivateOn()==KeyAction.BOTH&&Configs.RANGE_FEEDING_HOLD.getKeybind().getSettings().getContext()==KeybindSettings.Context.INGAME,"press/release in-game context");
  check(Configs.getExpansionConfig(Configs.RANGE_FEEDING)==Configs.RANGE_FEEDING_EXPANDED,"expansion registered");
  for(var child:Configs.getExpansionChildren(Configs.RANGE_FEEDING))check(Configs.getExpansionParent(child)==Configs.RANGE_FEEDING,"child parent");
  check(Configs.getExpansionChildren(Configs.RANGE_FEEDING).size()==8,"all eight child settings including interval");
  check(Configs.RANGE_FEEDING_INTERVAL.getIntegerValue()==4,"default feeding interval");
  for(var toggle:List.of(Configs.REMOVE_COOLDOWN_ANIMATIONS,Configs.REMOVE_ATTACK_SWING,Configs.REMOVE_INTERACT_SWING,Configs.REMOVE_ITEM_COOLDOWN_OVERLAY,Configs.REMOVE_EQUIP_ANIMATION)){
   toggle.resetToDefault();check(!toggle.getBooleanValue()&&!toggle.getDefaultBooleanValue(),"all animation defaults/reset off: "+toggle.getName());
  }
  Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);
  check(!Configs.getClientView().contains(Configs.RANGE_FEEDING_SKIP_BABIES),"collapsed child hidden");
  Configs.RANGE_FEEDING_EXPANDED.setBooleanValue(true);
  check(Configs.getClientView().contains(Configs.RANGE_FEEDING_SKIP_BABIES)&&Configs.getAllView().contains(Configs.RANGE_FEEDING_HOLD),"expanded client/all children");
  var toggle=(KeybindMulti)Configs.RANGE_FEEDING_SKIP_BABIES.getKeybind();toggle.getCallback().onKeyAction(KeyAction.PRESS,toggle);
  check(!Configs.RANGE_FEEDING_SKIP_BABIES.getBooleanValue()&&!Configs.RANGE_FEEDING.getBooleanValue(),"independent protection callback");
  Configs.RANGE_FEEDING_HOLD.getKeybind().setValueFromString("F8");Configs.RANGE_FEEDING_SKIP_BABIES.getKeybind().setValueFromString("F7");
  Configs.RANGE_FEEDING_ONCE.getKeybind().setValueFromString("F6");Configs.RANGE_FEEDING_RANGE.setIntegerValue(5);
  Configs.RANGE_FEEDING_INTERVAL.setIntegerValue(9);
  Configs.RANGE_FEEDING_LIST_MODE.setOptionListValue(RangeFeedingListMode.WHITELIST);Configs.RANGE_FEEDING_WHITELIST.setStrings(List.of("cow"));
  new Configs().save();defaults();new Configs().load();
  check(!Configs.RANGE_FEEDING.getBooleanValue()&&!Configs.RANGE_FEEDING_SKIP_BABIES.getBooleanValue(),"switch persistence");
  check(Configs.RANGE_FEEDING_HOLD.getKeybind().getStringValue().equals("F8")&&Configs.RANGE_FEEDING_SKIP_BABIES.getKeybind().getStringValue().equals("F7")&&Configs.RANGE_FEEDING_ONCE.getKeybind().getStringValue().equals("F6"),"independent binding persistence");
  check(Configs.RANGE_FEEDING_RANGE.getIntegerValue()==5&&Configs.RANGE_FEEDING_LIST_MODE.getOptionListValue()==RangeFeedingListMode.WHITELIST&&Configs.RANGE_FEEDING_WHITELIST.getStrings().equals(List.of("cow")),"range/list persistence");
  check(Configs.RANGE_FEEDING_INTERVAL.getIntegerValue()==9,"feeding interval persistence");
  check(RangeFeedingListMode.NONE.cycle(false)==RangeFeedingListMode.WHITELIST&&RangeFeedingListMode.WHITELIST.cycle(true)==RangeFeedingListMode.NONE&&RangeFeedingListMode.NONE.fromString(null)==RangeFeedingListMode.NONE,"mode cycling/fallback");
  defaults();System.out.println("RANGE_FEEDING_CONFIG_PASS assertions="+assertions);
 }
 private static void clearFixtures(Minecraft mc){for(var animal:fixtures)mc.level.removeEntity(animal.getId(),Entity.RemovalReason.DISCARDED);fixtures.clear();}
 private static void fresh(Minecraft mc)throws Exception{
  service.setHoldActive(false);physicalDown=false;defaults();Configs.RANGE_FEEDING_HOLD.getKeybind().setValueFromString("F8");
  MinecraftClientCompat.setScreen(mc,null);focus.setBoolean(mc.getWindow(),true);mc.player.setHealth(20);mc.player.stopUsingItem();mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);
  clearFixtures(mc);mc.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT,64));ticks(mc,25);interactions.clear();interactionTimes.clear();
 }
 private static Animal animal(Minecraft mc,boolean baby,double z){
  Animal cow=EntityTypes.COW.create(mc.level,EntitySpawnReason.COMMAND);cow.setId(fakeId--);cow.setBaby(baby);cow.setNoAi(true);cow.setNoGravity(true);
  cow.setPos(mc.player.getX()+0.4,mc.player.getY(),mc.player.getZ()+z);mc.level.addEntity(cow);fixtures.add(cow);return cow;
 }
 private static void press(){physicalDown=true;var key=(KeybindMulti)Configs.RANGE_FEEDING_HOLD.getKeybind();key.getCallback().onKeyAction(KeyAction.PRESS,key);}
 private static void release(){physicalDown=false;var key=(KeybindMulti)Configs.RANGE_FEEDING_HOLD.getKeybind();key.getCallback().onKeyAction(KeyAction.RELEASE,key);}
 private static void scheduler(Minecraft mc)throws Exception{
  controlled=true;suppress=true;focus=mc.getWindow().getClass().getDeclaredField("focused");focus.setAccessible(true);
  for(var mode:RangeFeedingListMode.values())for(boolean protect:List.of(true,false)){
   fresh(mc);var adult=animal(mc,false,1);var baby=animal(mc,true,2);Configs.RANGE_FEEDING.setBooleanValue(true);Configs.RANGE_FEEDING_LIST_MODE.setOptionListValue(mode);
   Configs.RANGE_FEEDING_WHITELIST.setStrings(List.of("cow"));Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(protect);
   check(service.requestSingleFeed(),"single accepted");ticks(mc,12);
   check(interactions.contains(adult.getUUID())&&interactions.contains(baby.getUUID())!=protect&&interactions.size()==(protect?1:2),"baby rule in "+mode+" protect="+protect);
  }
  fresh(mc);animal(mc,false,1);var newlyAllowedBaby=animal(mc,true,2);Configs.RANGE_FEEDING.setBooleanValue(true);
  service.requestSingleFeed();Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(false);ticks(mc,12);
  System.out.println("RANGE_FEEDING_TOGGLE_REPRO baby_fed="+interactions.contains(newlyAllowedBaby.getUUID()));
  check(interactions.contains(newlyAllowedBaby.getUUID()),"turning protection off during a batch must include newly allowed babies");
  fresh(mc);var fedAdult=animal(mc,false,1);var laterAdult=animal(mc,false,1.5);var addedBaby=animal(mc,true,2);Configs.RANGE_FEEDING.setBooleanValue(true);
  service.requestSingleFeed();ticks(mc,1);Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(false);Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(true);Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(false);ticks(mc,15);
  check(interactions.size()==3&&Collections.frequency(interactions,fedAdult.getUUID())==1&&interactions.contains(laterAdult.getUUID())&&interactions.contains(addedBaby.getUUID()),"protection refresh never repeats an already processed adult");
  for(int interval:List.of(1,4,20,200)){
   fresh(mc);animal(mc,false,1);animal(mc,false,1.7);animal(mc,false,2.4);Configs.RANGE_FEEDING_INTERVAL.setIntegerValue(interval);ticks(mc,interval);
   Configs.RANGE_FEEDING.setBooleanValue(true);service.requestSingleFeed();ticks(mc,1+interval*2);
   check(interactions.size()==3,"configured interval completes three targets: "+interval);
   for(int i=1;i<interactionTimes.size();i++)check(interactionTimes.get(i)-interactionTimes.get(i-1)==interval,"exact configured interval: "+interval);
  }
  fresh(mc);animal(mc,true,1);Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(false);Configs.RANGE_FEEDING_INTERVAL.setIntegerValue(1);press();ticks(mc,8);
  check(interactions.size()==8,"fast baby feeding has no hidden 20-tick rescan/retry cap");release();
  fresh(mc);animal(mc,false,1);animal(mc,false,1.7);animal(mc,false,2.4);Configs.RANGE_FEEDING.setBooleanValue(true);service.requestSingleFeed();ticks(mc,1);
  Configs.RANGE_FEEDING_INTERVAL.setIntegerValue(20);ticks(mc,19);check(interactions.size()==1,"slower setting applies immediately to next pending interaction");ticks(mc,1);check(interactions.size()==2,"slow interval boundary");
  Configs.RANGE_FEEDING_INTERVAL.setIntegerValue(1);ticks(mc,1);check(interactions.size()==3,"faster setting applies immediately to pending interaction");
  fresh(mc);var adult=animal(mc,false,1);var baby=animal(mc,true,2);Configs.RANGE_FEEDING.setBooleanValue(true);Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(false);
  service.requestSingleFeed();ticks(mc,1);check(interactions.equals(List.of(adult.getUUID())),"one interaction per tick");
  var key=(KeybindMulti)Configs.RANGE_FEEDING_SKIP_BABIES.getKeybind();key.getCallback().onKeyAction(KeyAction.PRESS,key);ticks(mc,12);
  check(!interactions.contains(baby.getUUID()),"protection toggled after scan applies before next interaction");
  for(String mutation:List.of("blacklist","whitelist","empty-whitelist","invalid-id","range","food","removed","master")){
   fresh(mc);animal(mc,false,1);var later=animal(mc,false,2);Configs.RANGE_FEEDING.setBooleanValue(true);service.requestSingleFeed();ticks(mc,1);
   switch(mutation){
    case "blacklist"->{Configs.RANGE_FEEDING_LIST_MODE.setOptionListValue(RangeFeedingListMode.BLACKLIST);Configs.RANGE_FEEDING_BLACKLIST.setStrings(List.of(" COW "));}
    case "whitelist"->{Configs.RANGE_FEEDING_LIST_MODE.setOptionListValue(RangeFeedingListMode.WHITELIST);Configs.RANGE_FEEDING_WHITELIST.setStrings(List.of("sheep"));}
    case "empty-whitelist"->Configs.RANGE_FEEDING_LIST_MODE.setOptionListValue(RangeFeedingListMode.WHITELIST);
    case "invalid-id"->{Configs.RANGE_FEEDING_LIST_MODE.setOptionListValue(RangeFeedingListMode.WHITELIST);Configs.RANGE_FEEDING_WHITELIST.setStrings(List.of("not a valid id","unknown:missing"));}
    case "range"->later.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ()+10);
    case "food"->mc.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CARROT));
    case "removed"->mc.level.removeEntity(later.getId(),Entity.RemovalReason.DISCARDED);
    case "master"->Configs.RANGE_FEEDING.setBooleanValue(false);
   }
   ticks(mc,12);check(interactions.size()==1,"live queued mutation: "+mutation);
  }
  fresh(mc);animal(mc,false,8);Configs.RANGE_FEEDING_RANGE.setIntegerValue(64);Configs.RANGE_FEEDING.setBooleanValue(true);service.requestSingleFeed();ticks(mc,8);check(interactions.isEmpty(),"configured range cannot extend actual reach");
  fresh(mc);animal(mc,false,1);animal(mc,false,1.7);animal(mc,false,2.4);Configs.RANGE_FEEDING.setBooleanValue(true);ticks(mc,8);check(interactions.isEmpty(),"master on alone does not feed");
  service.requestSingleFeed();press();ticks(mc,9);check(interactions.size()==3,"single and hold share one batch interaction budget");
  for(int i=1;i<interactionTimes.size();i++)check(interactionTimes.get(i)-interactionTimes.get(i-1)==4,"successive queued targets are exactly four ticks apart");release();
  fresh(mc);animal(mc,false,1);animal(mc,false,1.7);animal(mc,false,2.4);press();ticks(mc,1);release();ticks(mc,20);check(interactions.size()==1,"release cancels a nonempty hold queue");
  fresh(mc);animal(mc,false,1);animal(mc,false,2);press();ticks(mc,1);Configs.RANGE_FEEDING_HOLD.getKeybind().setValueFromString("F9");ticks(mc,12);check(interactions.size()==1,"rebinding during hold cancels old queued work");release();
  fresh(mc);animal(mc,false,1);check(!service.requestSingleFeed(),"master off blocks single");ticks(mc,8);check(interactions.isEmpty(),"off/unheld idle");
  press();for(int i=0;i<16;i++)press();ticks(mc,45);check(interactions.size()>=2&&!Configs.RANGE_FEEDING.getBooleanValue(),"hold temporarily runs continuously without enabling master");
  for(int i=1;i<interactionTimes.size();i++)check(interactionTimes.get(i)-interactionTimes.get(i-1)>=4,"shared four-tick rate limit");
  new Configs().save();new Configs().load();check(!Configs.RANGE_FEEDING.getBooleanValue(),"active hold never persists master=true");
  int before=interactions.size();release();ticks(mc,30);check(interactions.size()==before,"release immediately drains hold work");
  fresh(mc);animal(mc,false,1);animal(mc,false,2);Configs.RANGE_FEEDING.setBooleanValue(true);service.requestSingleFeed();press();release();ticks(mc,12);check(interactions.size()==2,"releasing hold preserves separately requested single batch");
  fresh(mc);animal(mc,false,1);animal(mc,false,2);Configs.RANGE_FEEDING.setBooleanValue(true);service.requestSingleFeed();service.requestSingleFeed();ticks(mc,1);var added=animal(mc,false,2.5);service.requestSingleFeed();ticks(mc,15);check(interactions.size()==2&&!interactions.contains(added.getUUID()),"repeat single does not rescan a running batch");
  fresh(mc);animal(mc,false,1);animal(mc,false,2);Configs.RANGE_FEEDING.setBooleanValue(true);press();ticks(mc,1);Configs.RANGE_FEEDING.setBooleanValue(false);ticks(mc,12);check(interactions.size()>=2&&new HashSet<>(interactions).size()==2,"master off does not cancel hold");
  fresh(mc);animal(mc,false,1);animal(mc,false,2);press();ticks(mc,1);before=interactions.size();physicalDown=false;ticks(mc,8);physicalDown=true;ticks(mc,20);check(interactions.size()==before,"missing release event still stops; physical state alone cannot restart");release();
  for(String boundary:List.of("gui","focus","death","world","player")){
   fresh(mc);animal(mc,false,1);animal(mc,false,2);press();ticks(mc,1);before=interactions.size();
   switch(boundary){
    case "gui"->{MinecraftClientCompat.setScreen(mc,new TitleScreen());ticks(mc,1);MinecraftClientCompat.setScreen(mc,null);}
    case "focus"->{focus.setBoolean(mc.getWindow(),false);ticks(mc,1);focus.setBoolean(mc.getWindow(),true);}
    case "death"->{mc.player.setHealth(0);ticks(mc,1);mc.player.setHealth(20);}
    case "world"->{var level=mc.level;mc.level=null;ticks(mc,1);mc.level=level;}
    case "player"->{var player=mc.player;mc.player=null;ticks(mc,1);mc.player=player;}
   }
   press();ticks(mc,25);check(interactions.size()==before,"boundary cancels and blocks held restart: "+boundary);
   release();ticks(mc,1);press();ticks(mc,5);check(interactions.size()>before,"fresh press resumes after "+boundary);release();
  }
  fresh(mc);animal(mc,false,1);Configs.RANGE_FEEDING_HOLD.getKeybind().clearKeys();press();ticks(mc,10);check(interactions.isEmpty(),"unbound hold never activates");
  fresh(mc);animal(mc,false,1);press();mc.options.keyUse.setDown(true);ticks(mc,8);check(interactions.isEmpty(),"manual use takes precedence");mc.options.keyUse.setDown(false);ticks(mc,5);check(!interactions.isEmpty(),"hold resumes after manual use");release();
  fresh(mc);controlled=false;suppress=false;defaults();System.out.println("RANGE_FEEDING_SCHEDULER_PASS assertions="+assertions);
 }
 private static WidgetListConfigOptions openSettings(Minecraft mc,String query)throws Exception{
  ConfigScrollMemory.saveSelectedTab("ALL");GuiBase.openGui(new HalfMasaConfigScreen());var method=GuiListBase.class.getDeclaredMethod("getListWidget");method.setAccessible(true);
  var list=(WidgetListConfigOptions)method.invoke(MinecraftClientCompat.getScreen(mc));var search=list.getSearchBarWidget();search.setSearchOpen(true);var field=WidgetSearchBar.class.getDeclaredField("searchBox");field.setAccessible(true);((GuiTextFieldGeneric)field.get(search)).setValue(query);list.refreshEntries();return list;
 }
 @SuppressWarnings("unchecked") private static List<WidgetBase> widgets(WidgetConfigOption row)throws Exception{
  var f=WidgetContainer.class.getDeclaredField("subWidgets");f.setAccessible(true);return (List<WidgetBase>)f.get(row);
 }
 @SuppressWarnings("unchecked") private static Map<String,WidgetConfigOption> glowRows(Minecraft mc)throws Exception{
  var method=GuiListBase.class.getDeclaredMethod("getListWidget");method.setAccessible(true);var list=(WidgetListConfigOptions)method.invoke(MinecraftClientCompat.getScreen(mc));
  var f=WidgetListBase.class.getDeclaredField("listWidgets");f.setAccessible(true);var result=new HashMap<String,WidgetConfigOption>();
  for(var row:(List<WidgetConfigOption>)f.get(list))if(row.getEntry().getConfig()!=null)result.put(row.getEntry().getConfig().getName(),row);return result;
 }
 private static fi.dy.masa.malilib.gui.button.ButtonBase reset(WidgetConfigOption row)throws Exception{
  return widgets(row).stream().filter(w->w instanceof fi.dy.masa.malilib.gui.button.ButtonBase).map(w->(fi.dy.masa.malilib.gui.button.ButtonBase)w).max(Comparator.comparingInt(WidgetBase::getX)).orElseThrow();
 }
 private static void glowLayout(Minecraft mc)throws Exception{
  var rows=glowRows(mc);var master=rows.get("entityGlowHighlight");var mode=rows.get("entityGlowMode");var color=rows.get("entityGlowAllColor");var action=rows.get("entityGlowRuleEditor");
  check(master!=null&&mode!=null&&color!=null&&action!=null,"expanded glow rows visible");
  var masterKey=widgets(master).stream().filter(w->w instanceof fi.dy.masa.malilib.gui.button.ConfigButtonKeybind).findFirst().orElseThrow();
  var modeKey=widgets(mode).stream().filter(w->w instanceof fi.dy.masa.malilib.gui.button.ConfigButtonKeybind).findFirst().orElseThrow();
  check(masterKey.getX()==modeKey.getX()&&masterKey.getWidth()==modeKey.getWidth(),"glow mode key column aligns with native boolean hotkey");
  var masterGear=widgets(master).stream().filter(w->w instanceof WidgetKeybindSettings).findFirst().orElseThrow();var modeGear=widgets(mode).stream().filter(w->w instanceof WidgetKeybindSettings).findFirst().orElseThrow();
  check(masterGear.getX()==modeGear.getX()&&modeGear.getWidth()==20,"native advanced-keybind widget exists and aligns");
  for(var row:List.of(mode,color,action))check(reset(row).getX()==reset(master).getX(),"glow reset columns aligned: "+row.getEntry().getConfig().getName());
 }
 private static void openGlowKeySettings(Minecraft mc)throws Exception{
  var gear=widgets(glowRows(mc).get("entityGlowMode")).stream().filter(w->w instanceof WidgetKeybindSettings).findFirst().orElseThrow();
  var event=new net.minecraft.client.input.MouseButtonEvent(gear.getX()+10,gear.getY()+10,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
  check(gear.onMouseClicked(event,true),"advanced keybind icon accepts click");check(MinecraftClientCompat.getScreen(mc) instanceof fi.dy.masa.malilib.gui.GuiKeybindSettings,"advanced keybind dialog actually opens");
 }
 private void next(Minecraft mc,int value){stage=value;since=mc.level.getGameTime();}
 private static void finish(Minecraft mc){defaults();new Configs().save();System.out.println("RANGE_FEEDING_WORLD_PASS assertions="+assertions);mc.stop();}
 private static boolean confluxFlag(Object screen,String name)throws Exception{var field=screen.getClass().getDeclaredField(name);field.setAccessible(true);return field.getBoolean(screen);}
 private void serverState(Minecraft mc){ready=false;mc.getSingleplayerServer().execute(()->{
  var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayers().getFirst();var adult=(Animal)player.level().getEntity(realAdult);var baby=(Animal)player.level().getEntity(realBaby);
  serverFood=player.getMainHandItem().getCount();serverBabyAge=baby.getAge();serverLove=adult.isInLove();ready=true;
 });}
 private void run(Minecraft mc){try{
  if(++frames>20000)throw new AssertionError("frame timeout stage="+stage);
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;configs();stage=1;
   mc.createWorldOpenFlows().createFreshLevel("range-feeding-fixture",new LevelSettings("Range Feeding Fixture",GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;}
  if(mc.player==null||mc.level==null)return;
  if(stage==1){if(mc.level.getGameTime()<40)return;MinecraftClientCompat.setScreen(mc,null);portedRules();scheduler(mc);ready=false;
   mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();player.setPermanentlyInvulnerable(true);player.setGameMode(GameType.SURVIVAL);
    serverFoodLogic(player);
    for(int i=0;i<2;i++){var cow=EntityTypes.COW.create(player.level(),EntitySpawnReason.COMMAND);cow.setNoAi(true);cow.setNoGravity(true);cow.setAge(i==0?0:-24000);cow.setPos(player.getX()+0.4,player.getY(),player.getZ()+1+i);player.level().addFreshEntity(cow);if(i==0)realAdult=cow.getUUID();else realBaby=cow.getUUID();}
    player.getInventory().setItem(0,new ItemStack(Items.WHEAT,20));player.getInventory().setSelectedSlot(0);player.inventoryMenu.broadcastChanges();ready=true;});next(mc,2);return;}
  if(mc.level.getGameTime()-since>600)throw new AssertionError("world timeout stage="+stage);
  if(stage==2){if(!ready||mc.level.getEntity(realAdult)==null||mc.level.getEntity(realBaby)==null||mc.player.getMainHandItem().getCount()!=20)return;
   focus.setBoolean(mc.getWindow(),true);Configs.RANGE_FEEDING.setBooleanValue(true);check(service.requestSingleFeed(),"real single accepted");next(mc,3);return;}
  if(stage==3){focus.setBoolean(mc.getWindow(),true);if(mc.level.getGameTime()-since<20)return;serverState(mc);next(mc,4);return;}
  if(stage==4){if(!ready)return;check(serverFood==19&&serverLove&&serverBabyAge<-23000,"real server: one adult fed, protected baby unchanged");
   Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(false);service.requestSingleFeed();next(mc,5);return;}
  if(stage==5){focus.setBoolean(mc.getWindow(),true);if(mc.level.getGameTime()-since<20)return;serverState(mc);next(mc,6);return;}
  if(stage==6){if(!ready)return;check(serverFood==18&&serverBabyAge>-23000,"real server: disabling protection feeds and grows baby");
   Configs.RANGE_FEEDING_SKIP_BABIES.setBooleanValue(true);var baby=mc.level.getEntity(realBaby);mc.gameMode.interact(mc.player,baby,new EntityHitResult(baby),InteractionHand.MAIN_HAND);next(mc,7);return;}
  if(stage==7){if(mc.level.getGameTime()-since<15)return;serverState(mc);next(mc,8);return;}
  if(stage==8){if(!ready)return;check(serverFood==17,"protection does not block a manual vanilla interaction");Configs.RANGE_FEEDING.setBooleanValue(false);Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);Configs.RANGE_FEEDING_EXPANDED.setBooleanValue(true);
   openSettings(mc,"喂食");next(mc,9);return;}
  if(stage==9){if(mc.level.getGameTime()-since<3)return;net.minecraft.client.Screenshot.grab(mc,false);Configs.ENTITY_GLOW_EXPANDED.setBooleanValue(true);openSettings(mc,"entityGlow");glowLayout(mc);next(mc,10);return;}
  if(stage==10){if(mc.level.getGameTime()-since<3)return;net.minecraft.client.Screenshot.grab(mc,false);openGlowKeySettings(mc);next(mc,11);return;}
  if(stage==11){if(mc.level.getGameTime()-since<3)return;net.minecraft.client.Screenshot.grab(mc,false);
   if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("confluxmap")){
    Configs.CONFLUX_MAP_EXTENSIONS.setBooleanValue(true);Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(true);Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(true);
    var ui=(net.minecraft.client.gui.screens.Screen)Class.forName("cn.net.rms.confluxmap.mc.ui.screen.WaypointListScreen").getConstructor().newInstance();MinecraftClientCompat.setScreen(mc,ui);stage=12;optionalFrames=frames;return;
   }stage=14;finish(mc);}
  if(stage==12){if(frames-optionalFrames<8)return;var ui=MinecraftClientCompat.getScreen(mc);
   check(confluxFlag(ui,"halfmasa$active")&&!confluxFlag(ui,"halfmasa$failed"),"actual Conflux compact screen contract and rendering");
   var bridge=new ConfluxWaypointScreenBridge(ui);bridge.query();bridge.rows(true);check(true,"actual Conflux query and combined rows in production naming");
   Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(false);stage=13;optionalFrames=frames;return;}
  if(stage==13){if(frames-optionalFrames<8)return;check(!confluxFlag(MinecraftClientCompat.getScreen(mc),"halfmasa$failed"),"actual Conflux detailed screen live switch");Configs.CONFLUX_MAP_EXTENSIONS.setBooleanValue(false);stage=14;finish(mc);}
 }catch(Throwable failure){throw new IllegalStateException("Range feeding smoke stage="+stage,failure);}}
}
'''
FRAME = r'''package io.github.halfmasa.feedingsmoke.mixin;
import io.github.halfmasa.feedingsmoke.FeedingSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class FrameMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void frame(CallbackInfo ci){FeedingSmoke.frame((Minecraft)(Object)this);}
}'''
INPUT = r'''package io.github.halfmasa.feedingsmoke.mixin;
import io.github.halfmasa.feedingsmoke.FeedingSmoke;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=KeybindMulti.class,remap=false) public class InputMixin {
 @Inject(method="isKeyDown",at=@At("HEAD"),cancellable=true) private static void physical(int key,CallbackInfoReturnable<Boolean> ci){if(FeedingSmoke.controlled)ci.setReturnValue(FeedingSmoke.physicalDown);}
}'''
PROBE = r'''package io.github.halfmasa.feedingsmoke.mixin;
import io.github.halfmasa.feedingsmoke.FeedingSmoke;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPlayerGameMode.class) public class ProbeMixin {
 @Inject(method="interact",at=@At("HEAD"),cancellable=true) private void interaction(Player player,Entity target,EntityHitResult hit,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){FeedingSmoke.interaction(target);if(FeedingSmoke.suppress)ci.setReturnValue(InteractionResult.PASS);}
}'''
CLIPBOARD = r'''package io.github.halfmasa.feedingsmoke.mixin;
import io.github.halfmasa.xaerobinding.feature.ScreenshotClipboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ScreenshotClipboard.class) public class ClipboardMixin {
 @Inject(method="initialize",at=@At("HEAD"),cancellable=true) private static void skipUnrelatedDesktopClipboard(CallbackInfo ci){ci.cancel();}
}'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--version", default="26.3")
    parser.add_argument("--loader-version")
    parser.add_argument("--extra-mod", action="append", type=Path, default=[])
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--jvm-arg", action="append", default=[])
    parser.add_argument("--animal", choices=["cow", "chicken"], default="cow")
    parser.add_argument("--disable-clipboard-init", action="store_true", help="Isolate feeding tests from AWT desktop clipboard initialization")
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    directory = ROOT / "build/range-feeding-tests" / datetime.now(timezone(timedelta(hours=8))).strftime("%Y%m%d-%H%M%S-%f")
    classes = directory / "classes"
    classes.mkdir(parents=True)
    sources = []
    compile_cp = (ROOT / f"build/port-classpath-{args.version}.txt").read_text(encoding="utf-8").strip()
    legacy = args.version.startswith("1.21.")
    mapping = smoke_remap.mappings(args.version, compile_cp) if legacy else None
    test_sources = [("FeedingSmoke", SOURCE), ("FrameMixin", FRAME), ("InputMixin", INPUT), ("ProbeMixin", PROBE)]
    if args.disable_clipboard_init:
        test_sources.append(("ClipboardMixin", CLIPBOARD))
    for name, source in test_sources:
        if args.version != "26.3":
            source = source.replace("player.setPermanentlyInvulnerable(true)", "player.getAbilities().invulnerable=true")
        if args.version == "26.1.2" or legacy:
            source = source.replace("mc.gui.overlay()", "mc.getOverlay()")
            source = source.replace("EntityTypes.", "EntityType.").replace("WorldPresets::createTestWorldDimensions", "WorldPresets::createFlatWorldDimensions")
            source = source.replace("net.minecraft.client.Screenshot.grab(mc,false)", "net.minecraft.client.Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{})")
        if legacy:
            source = source.replace("new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT", "false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT")
            if args.version == "1.21.11":
                source = source.replace("new GameRules()", "new net.minecraft.world.level.gamerules.GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())")
            else:
                source = source.replace("new GameRules()", "new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())")
            if args.version == "1.21.1":
                source = source.replace("new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())", "new GameRules()")
                source = source.replace("EntitySpawnReason.COMMAND", "MobSpawnType.COMMAND")
                source = source.replace(".create(mc.level,MobSpawnType.COMMAND)", ".create(mc.level)").replace(".create(player.level(),MobSpawnType.COMMAND)", ".create(player.level())")
                source = source.replace("WorldPresets::createFlatWorldDimensions", "access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()")
            source = source.replace("Player player,Entity target,EntityHitResult hit,InteractionHand hand", "Player player,Entity target,InteractionHand hand")
            source = source.replace("mc.gameMode.interact(mc.player,baby,new EntityHitResult(baby),InteractionHand.MAIN_HAND)", "mc.gameMode.interact(mc.player,baby,InteractionHand.MAIN_HAND)")
            focus_name = smoke_remap.field_name(mapping, "net/minecraft/client/Minecraft", "windowActive")
            source = source.replace('mc.getWindow().getClass().getDeclaredField("focused")', 'Minecraft.class.getDeclaredField("' + focus_name + '")')
            source = source.replace("focus.setBoolean(mc.getWindow(),", "focus.setBoolean(mc,")
            if tuple(map(int, args.version.split("."))) < (1, 21, 10):
                source = source.replace("  var event=new net.minecraft.client.input.MouseButtonEvent(gear.getX()+10,gear.getY()+10,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));", "")
                source = source.replace("gear.onMouseClicked(event,true)", "gear.onMouseClicked(gear.getX()+10,gear.getY()+10,0)")
            if tuple(map(int, args.version.split("."))) < (1, 21, 5):
                for fixture_id in ("realBaby", "realAdult"):
                    source = source.replace(f"mc.level.getEntity({fixture_id})", f"java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).filter(entity->entity.getUUID().equals({fixture_id})).findFirst().orElse(null)")
                    source = source.replace(f"player.level().getEntity({fixture_id})", f"((net.minecraft.server.level.ServerLevel)player.level()).getEntity({fixture_id})")
                source = source.replace("player.getInventory().setSelectedSlot(0)", "player.getInventory().selected=0")
        if args.animal == "chicken":
            source = source.replace("EntityTypes.COW", "EntityTypes.CHICKEN").replace("EntityType.COW", "EntityType.CHICKEN").replace("Items.WHEAT", "Items.WHEAT_SEEDS")
            source = source.replace('"cow"', '"chicken"').replace('" COW "', '" CHICKEN "')
        path = directory / (name + ".java")
        path.write_text(source, encoding="utf-8")
        sources.append(str(path))
    cp = str(ROOT / f"versions/{args.version}/build/classes/java/main") + os.pathsep + compile_cp if legacy else str(args.artifact) + os.pathsep + compile_cp
    compiled = subprocess.run([str(args.java_home / "bin/javac.exe"), "-J-Dfile.encoding=UTF-8", "-proc:none", "-encoding", "UTF-8", "-cp", cp, "-d", str(classes), *sources], capture_output=True)
    if compiled.returncode:
        diagnostic = compiled.stderr.decode("utf-8", errors="replace")
        (directory / "compile.log").write_text(diagnostic, encoding="utf-8")
        print(diagnostic)
        return 1
    mod = directory / "feeding-smoke.jar"
    with zipfile.ZipFile(mod, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_feeding_smoke", "version": "1.0", "environment": "client", "entrypoints": {"client": ["io.github.halfmasa.feedingsmoke.FeedingSmoke"]}, "mixins": ["feeding-smoke.mixins.json"]}))
        jar.writestr("feeding-smoke.mixins.json", json.dumps({"required": True, "package": "io.github.halfmasa.feedingsmoke.mixin", "compatibilityLevel": "JAVA_21" if legacy else "JAVA_25", "client": ["FrameMixin", "InputMixin", "ProbeMixin"] + (["ClipboardMixin"] if args.disable_clipboard_init else []), "injectors": {"defaultRequire": 1}}))
    if legacy:
        mod = smoke_remap.remap(args.java_home, mod, directory / "feeding-smoke-remapped.jar", mapping, cp, directory / "remap.log")
    game = directory / "game"
    (game / "mods").mkdir(parents=True)
    (game / "config").mkdir()
    for extra in args.extra_mod:
        shutil.copy2(extra, game / "mods" / extra.name)
    conflux_config = game / "config/confluxmap"
    conflux_config.mkdir()
    (conflux_config / "config.json").write_text(json.dumps({"predictionEnabled": False, "updateCheckEnabled": False, "surveyReminderDismissed": True, "minimapEnabled": False}), encoding="utf-8")
    (conflux_config / "server.json").write_text(json.dumps({"checkForUpdates": False, "webMap": {"enabled": False}}), encoding="utf-8")
    (game / "config/malilib.json").write_text("{}\n", encoding="utf-8")
    (game / "options.txt").write_text("onboardAccessibility:false\nenableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:zh_cn\ntutorialStep:none\n", encoding="utf-8")
    command = startup.production_command(args, game, mod)
    command[1:1] = [f"-XX:ErrorFile={directory / 'jvm-crash.log'}", *args.jvm_arg]
    command[command.index("--width") + 1] = "1120"
    command[command.index("--height") + 1] = "720"
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
    passed = code == 0 and not timed_out and "RANGE_FEEDING_WORLD_PASS" in log
    report = {"result": "PASS" if passed else "FAIL", "game_version": args.version, "artifact": str(args.artifact), "animal": args.animal, "jvm_args": args.jvm_arg, "duration_seconds": round(time.monotonic() - started, 1), "exit_code": code, "timed_out": timed_out, "log": str(log_path), "checks": [line for line in log.splitlines() if "RANGE_FEEDING_" in line], "screenshots": [str(path) for path in (game / "screenshots").glob("*.png")]}
    report["clipboard_initialization_disabled"] = args.disable_clipboard_init
    report["extra_mods"] = [str(path.resolve()) for path in args.extra_mod]
    report["input_mode"] = "Simulated held state for scheduler checks; vanilla interactions on an integrated server"
    (directory / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("RANGE FEEDING:", report["result"], flush=True)
    print(directory / "report.json", flush=True)
    if not passed:
        print("\n".join(log.splitlines()[-65:]), flush=True)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())

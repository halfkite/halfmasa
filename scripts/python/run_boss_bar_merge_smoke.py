"""Exercise the installed 26.3 grouper, config and render Mixins in an isolated client/world."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import zipfile

import run_startup_smoke as startup

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''
package io.github.halfmasa.bosssmoke;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.state.gui.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.feature.BossBarGrouper;
import io.github.halfmasa.xaerobinding.feature.BossBarHealth;
import io.github.halfmasa.xaerobinding.feature.CustomConfigGroupStore;
import io.github.halfmasa.xaerobinding.mixin.WitherBossEventAccessor;
import io.github.halfmasa.xaerobinding.feature.ConfigScrollMemory;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import io.github.halfmasa.xaerobinding.mixin.LerpingBossEventAccessor;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

public class BossSmoke implements ClientModInitializer {
 private static BossSmoke instance;
 private int stage,waited;
 private final List<UUID> realBossIds=new ArrayList<>();
 private volatile boolean realReady;
 private static int assertions;
 private static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
 private static void close(float value,float expected,String reason){check(Math.abs(value-expected)<0.00001F,reason+": "+value);}
 private static LerpingBossEvent bar(String name,float progress){return bar(Component.literal(name),progress,BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);}
 private static LerpingBossEvent bar(Component name,float progress,BossEvent.BossBarColor color,BossEvent.BossBarOverlay overlay){return new LerpingBossEvent(UUID.randomUUID(),name,progress,color,overlay,false,false,false);}
 private static List<LerpingBossEvent> merge(Collection<LerpingBossEvent> bars){return List.copyOf(BossBarGrouper.merge(bars));}
 @SuppressWarnings("unchecked") private static Map<UUID,LerpingBossEvent> events(BossHealthOverlay overlay)throws Exception{
  Field field=BossHealthOverlay.class.getDeclaredField("events");field.setAccessible(true);return (Map<UUID,LerpingBossEvent>)field.get(overlay);
 }
 private static void populate(BossHealthOverlay overlay,List<LerpingBossEvent> bars)throws Exception{
  overlay.reset();for(var bar:bars)events(overlay).put(bar.getId(),bar);
 }
 private static List<String> render(Minecraft mc,BossHealthOverlay overlay)throws Exception{
  var state=new GuiRenderState();overlay.extractRenderState(new GuiGraphicsExtractor(mc,state,0,0));
  List<String> texts=new ArrayList<>();Field field=GuiTextRenderState.class.getDeclaredField("text");field.setAccessible(true);
  state.forEachText(text->{try{
   var sequence=(net.minecraft.util.FormattedCharSequence)field.get(text);StringBuilder string=new StringBuilder();
   sequence.accept((index,style,codePoint)->{string.appendCodePoint(codePoint);return true;});texts.add(string.toString());
  }catch(IllegalAccessException e){throw new AssertionError(e);}});return texts;
 }
 private static void scaleAndHold(Minecraft mc,BossHealthOverlay overlay)throws Exception{
  for(boolean compact:new boolean[]{false,true}){
   Configs.BOSS_BAR_COMPACT.setBooleanValue(compact);
   for(int percent:new int[]{50,100,200}){
    Configs.BOSS_BAR_SCALE.setIntegerValue(percent);var state=new GuiRenderState();var graphics=new GuiGraphicsExtractor(mc,state,0,0);
    var before=new org.joml.Matrix3x2f(graphics.pose());overlay.extractRenderState(graphics);
    check(new org.joml.Matrix3x2f(graphics.pose()).equals(before),"matrix restored in normal/compact scale "+compact+"/"+percent);
    var texts=new ArrayList<GuiTextRenderState>();state.forEachText(texts::add);check(!texts.isEmpty(),"scaled labels still extracted");
    float expected=Math.min(percent/100F,(graphics.guiWidth()-16)/186F);close(texts.getFirst().pose.m00(),expected,"actual text scale");
    var center=texts.getFirst().pose.transformPosition(graphics.guiWidth()/2F,0,new org.joml.Vector2f());close(center.x,graphics.guiWidth()/2F,"scaling stays centered");
   }
  }
  Configs.BOSS_BAR_HOLD_ONLY.setBooleanValue(true);Configs.BOSS_BAR_HOLD_KEY.getKeybind().clearKeys();
  check(render(mc,overlay).isEmpty(),"unbound hold-only hides actual normal/compact rendering");
  check(overlay.shouldPlayMusic()||!events(overlay).isEmpty(),"hidden Boss source events retained");
  Configs.BETTER_BOSS_BAR.setBooleanValue(false);check(!render(mc,overlay).isEmpty(),"master off restores native visibility even in hold mode");
  Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_HOLD_ONLY.resetToDefault();Configs.BOSS_BAR_HOLD_KEY.resetToDefault();Configs.BOSS_BAR_SCALE.resetToDefault();Configs.BOSS_BAR_COMPACT.setBooleanValue(false);
 }
 private static void defaults(){
  Configs.BOSS_BAR_SCALE.resetToDefault();Configs.BOSS_BAR_HOLD_ONLY.resetToDefault();Configs.BOSS_BAR_HOLD_KEY.resetToDefault();
  Configs.BETTER_BOSS_BAR.resetToDefault();Configs.BETTER_BOSS_BAR_EXPANDED.resetToDefault();Configs.BOSS_BAR_SHOW_HEALTH.resetToDefault();Configs.BOSS_BAR_COMPACT.resetToDefault();
  Configs.BOSS_BAR_MERGE.resetToDefault();Configs.BOSS_BAR_MERGE_MATCH_MODE.resetToDefault();
  Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.resetToDefault();Configs.BOSS_BAR_MERGE_PROGRESS_MODE.resetToDefault();
  Configs.BOSS_BAR_MERGE_SHOW_COUNT.resetToDefault();Configs.BOSS_BAR_MERGE_MAX_BARS.resetToDefault();Configs.BOSS_BAR_MERGE_EXPANDED.resetToDefault();
 }
 private static void tests(Minecraft mc)throws Exception{
  defaults();List<LerpingBossEvent> empty=new ArrayList<>();
  check(BossBarGrouper.merge(empty)==empty,"disabled empty identity");
  var a=bar("Wither",0.8F);var b=bar("Wither",0.8F);var c=bar("Wither",0.4F);var bars=new ArrayList<>(List.of(a,b,c));
  check(BossBarGrouper.merge(bars)==bars,"disabled must preserve original collection");
  Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_MERGE.setBooleanValue(true);
  check(BossBarGrouper.merge(empty)==empty,"enabled empty identity");
  var single=List.of(a);check(BossBarGrouper.merge(single)==single,"single identity");
  check(((LerpingBossEventAccessor)(Object)a).halfmasa$getTargetPercent()==0.8F,"Accessor target");
  var result=merge(bars);check(result.size()==2&&result.get(0).getName().getString().equals("Wither ×2")&&result.get(1)==c,"strict grouping");
  close(result.get(0).getProgress(),0.8F,"strict progress");
  check(a.getName().getString().equals("Wither")&&bars.size()==3,"no source mutation");
  Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.setBooleanValue(true);
  for(var mode:BossBarMergeProgressMode.values()){
   Configs.BOSS_BAR_MERGE_PROGRESS_MODE.setOptionListValue(mode);result=merge(bars);
   check(result.size()==1&&result.get(0).getName().getString().equals("Wither ×3"),"different health grouping");
   close(result.get(0).getProgress(),mode==BossBarMergeProgressMode.AVERAGE?2.0F/3:mode==BossBarMergeProgressMode.MIN?0.4F:0.8F,"progress mode "+mode);
  }
  defaults();Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_MERGE.setBooleanValue(true);
  // Freeze interpolation at two different displayed values, with identical targets.
  Field progress=BossEvent.class.getDeclaredField("progress");progress.setAccessible(true);
  Field setTime=LerpingBossEvent.class.getDeclaredField("setTime");setTime.setAccessible(true);
  progress.setFloat(a,1.0F);progress.setFloat(b,0.6F);setTime.setLong(a,Util.getMillis()+60000);setTime.setLong(b,Util.getMillis()+60000);
  result=merge(List.of(a,b));check(result.size()==1,"same targets different animation must merge");close(result.getFirst().getProgress(),0.8F,"animated average");
  Configs.BOSS_BAR_MERGE_PROGRESS_MODE.setOptionListValue(BossBarMergeProgressMode.MIN);close(merge(List.of(a,b)).getFirst().getProgress(),0.6F,"animated minimum");
  Configs.BOSS_BAR_MERGE_PROGRESS_MODE.setOptionListValue(BossBarMergeProgressMode.MAX);close(merge(List.of(a,b)).getFirst().getProgress(),1.0F,"animated maximum");
  b.setProgress(0.4F);check(merge(List.of(a,b)).size()==2,"target update must split next frame");
  var colored=bar(Component.literal("Wither"),0.8F,BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
  var segmented=bar(Component.literal("Wither"),0.8F,BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.NOTCHED_10);
  check(merge(List.of(a,colored,segmented)).size()==3,"default color and overlay matching");
  Configs.BOSS_BAR_MERGE_MATCH_MODE.setOptionListValue(BossBarMergeMatchMode.NAME);
  result=merge(List.of(colored,a,segmented));check(result.size()==1&&result.getFirst().getColor()==BossEvent.BossBarColor.RED,"name-only uses first style");
  var styled=bar(Component.literal("Wither").withStyle(ChatFormatting.RED),0.8F,BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
  check(merge(List.of(a,styled)).size()==2,"full Component formatting is matched");
  var uuidNameA=Component.literal("Wither").withStyle(style->style.withInsertion(UUID.randomUUID().toString()));
  var uuidNameB=Component.literal("Wither").withStyle(style->style.withInsertion(UUID.randomUUID().toString()));
  check(!uuidNameA.equals(uuidNameB)&&merge(List.of(bar(uuidNameA,0.8F,BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS),bar(uuidNameB,0.8F,BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS))).size()==1,"nonvisual UUID metadata ignored");
  Configs.BOSS_BAR_MERGE_SHOW_COUNT.setBooleanValue(false);result=merge(List.of(colored,a));
  check(result.getFirst().getName().equals(colored.getName())&&colored.getName().getString().equals("Wither"),"count off and immutable title");
  defaults();Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_MERGE.setBooleanValue(true);
  var first=bar("First",0.8F);var second=bar("Second",0.8F);var third=bar("Third",0.8F);
  result=merge(List.of(first,second,bar("First",0.8F),third));
  check(result.stream().map(e->e.getName().getString()).toList().equals(List.of("First ×2","Second","Third")),"first-occurrence order");
  Configs.BOSS_BAR_MERGE_MAX_BARS.setIntegerValue(2);check(merge(List.of(first,second,third)).size()==2,"limit with no groups merged");
  Configs.BOSS_BAR_MERGE.toggleBooleanValue();check(BossBarGrouper.merge(bars)==bars,"toggle restores originals even with limit");
  Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_MERGE.setBooleanValue(true);Configs.BOSS_BAR_MERGE_MAX_BARS.setIntegerValue(0);
  for(boolean allow:List.of(false,true)){
   Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.setBooleanValue(allow);
   var invalid=new ArrayList<LerpingBossEvent>(List.of(bar("Invalid",0.5F),bar("Invalid",0.5F)));
   for(float value:new float[]{Float.NaN,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,-0.1F,1.1F})invalid.add(bar("Invalid",value));
   result=merge(invalid);check(result.size()==7,"invalid progress independent, allow="+allow);close(result.getFirst().getProgress(),0.5F,"invalid must not poison aggregate");
  }
  Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.setBooleanValue(false);
  check(merge(List.of(bar("Zero",0.0F),bar("Zero",-0.0F))).size()==1,"signed zero equality");
  var poisoned=bar("Wither",0.8F);progress.setFloat(poisoned,Float.NaN);setTime.setLong(poisoned,Util.getMillis()+60000);
  check(merge(List.of(a,poisoned)).size()==2,"invalid animated progress independent");
  var dynamic=new ArrayList<>(List.of(first,bar("First",0.8F)));check(merge(dynamic).size()==1,"dynamic initial group");
  dynamic.removeLast();check(merge(dynamic).getFirst()==first,"removal leaves original single");
  dynamic.clear();check(merge(dynamic).isEmpty(),"no cross-frame residue");
  check(merge(List.of(bar("First",0.8F))).getFirst().getName().getString().equals("First"),"new events have no old count");
  check(Configs.HOTKEYS.contains(Configs.BETTER_BOSS_BAR),"registered hotkey");
  var bossToggles=List.of(Configs.BOSS_BAR_MERGE,Configs.BOSS_BAR_SHOW_HEALTH,Configs.BOSS_BAR_COMPACT);
  boolean master=Configs.BETTER_BOSS_BAR.getBooleanValue();Configs.BETTER_BOSS_BAR.setBooleanValue(false);
  for(var toggle:bossToggles){
   check(Configs.HOTKEYS.contains(toggle)&&toggle.getKeybind().getStringValue().isEmpty(),"independent child hotkey registered and initially unbound: "+toggle.getName());
   var before=bossToggles.stream().map(t->t.getBooleanValue()).toList();var callback=((KeybindMulti)toggle.getKeybind()).getCallback();
   check(callback.onKeyAction(KeyAction.PRESS,toggle.getKeybind())&&toggle.getBooleanValue()!=before.get(bossToggles.indexOf(toggle)),"native hotkey toggles its child: "+toggle.getName());
   check(!Configs.BETTER_BOSS_BAR.getBooleanValue()&&bossToggles.stream().filter(t->t!=toggle).allMatch(t->t.getBooleanValue()==before.get(bossToggles.indexOf(t))),"child hotkey leaves master and other children unchanged");
   callback.onKeyAction(KeyAction.PRESS,toggle.getKeybind());
   toggle.getKeybind().setValueFromString("F6");toggle.setBooleanValue(!toggle.getDefaultBooleanValue());toggle.resetToDefault();
   check(toggle.getKeybind().getStringValue().isEmpty()&&toggle.getBooleanValue()==toggle.getDefaultBooleanValue(),"child reset restores switch and clears binding");
  }
  Configs.BETTER_BOSS_BAR.setBooleanValue(master);
  check(Configs.getExpansionConfig(Configs.BOSS_BAR_MERGE)==Configs.BOSS_BAR_MERGE_EXPANDED,"expansion parent");
  check(Configs.getExpansionChildren(Configs.BOSS_BAR_MERGE).size()==5,"five child settings");
  Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);Configs.BOSS_BAR_MERGE_EXPANDED.setBooleanValue(false);
  check(!Configs.getClientView().contains(Configs.BOSS_BAR_MERGE_MATCH_MODE),"collapsed children hidden");
  Configs.BETTER_BOSS_BAR_EXPANDED.setBooleanValue(true);Configs.BOSS_BAR_MERGE_EXPANDED.setBooleanValue(true);
  check(Configs.getClientView().contains(Configs.BOSS_BAR_MERGE_MATCH_MODE)&&Configs.getAllView().contains(Configs.BOSS_BAR_MERGE_MATCH_MODE),"expanded children visible in client/all views");
  Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(true);
  check(Configs.getClientView().contains(Configs.BOSS_BAR_MERGE_MATCH_MODE)&&!Configs.getClientView().contains(Configs.BOSS_BAR_MERGE_EXPANDED),"custom grouping candidates");
  Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);
  Configs.BOSS_BAR_MERGE_MATCH_MODE.setOptionListValue(BossBarMergeMatchMode.NAME);
  Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.setBooleanValue(true);Configs.BOSS_BAR_MERGE_PROGRESS_MODE.setOptionListValue(BossBarMergeProgressMode.MIN);
  Configs.BOSS_BAR_MERGE_SHOW_COUNT.setBooleanValue(false);Configs.BOSS_BAR_MERGE_MAX_BARS.setIntegerValue(2);
  Configs.BOSS_BAR_MERGE.getKeybind().setValueFromString("F5");Configs.BOSS_BAR_SHOW_HEALTH.getKeybind().setValueFromString("F6");Configs.BOSS_BAR_COMPACT.getKeybind().setValueFromString("F7");
  Configs.BETTER_BOSS_BAR.getKeybind().setValueFromString("F8");new Configs().save();defaults();Configs.BETTER_BOSS_BAR.getKeybind().setValueFromString("");new Configs().load();
  check(Configs.BOSS_BAR_MERGE.getKeybind().getStringValue().equals("F5")&&Configs.BOSS_BAR_SHOW_HEALTH.getKeybind().getStringValue().equals("F6")&&Configs.BOSS_BAR_COMPACT.getKeybind().getStringValue().equals("F7"),"all three independent hotkeys persist across reload");
  check(Configs.BOSS_BAR_MERGE.getBooleanValue()&&Configs.BOSS_BAR_MERGE_EXPANDED.getBooleanValue()&&Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.getBooleanValue(),"boolean persistence");
  check(Configs.BOSS_BAR_MERGE_MATCH_MODE.getOptionListValue()==BossBarMergeMatchMode.NAME&&Configs.BOSS_BAR_MERGE_PROGRESS_MODE.getOptionListValue()==BossBarMergeProgressMode.MIN,"enum persistence");
  check(!Configs.BOSS_BAR_MERGE_SHOW_COUNT.getBooleanValue()&&Configs.BOSS_BAR_MERGE_MAX_BARS.getIntegerValue()==2&&Configs.BETTER_BOSS_BAR.getKeybind().getStringValue().equals("F8"),"count limit hotkey persistence");
  check(BossBarMergeMatchMode.NAME.fromString("bad")==BossBarMergeMatchMode.NAME_STYLE&&BossBarMergeProgressMode.MAX.fromString(null)==BossBarMergeProgressMode.AVERAGE,"unknown enum fallback");
  check(BossBarMergeProgressMode.AVERAGE.cycle(false)==BossBarMergeProgressMode.MAX&&BossBarMergeProgressMode.MAX.cycle(true)==BossBarMergeProgressMode.AVERAGE,"enum cycling");
  defaults();Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_MERGE.setBooleanValue(true);
  var overlay=new BossHealthOverlay(mc);var calm=bar("Wither",0.8F);var effects=bar("Wither",0.8F);effects.setDarkenScreen(true).setPlayBossMusic(true).setCreateWorldFog(true);
  populate(overlay,List.of(calm,effects));
  check(render(mc,overlay).equals(List.of("Wither ×2")),"actual render Mixin emits one merged label");
  check(events(overlay).size()==2&&overlay.shouldPlayMusic()&&overlay.shouldDarkenScreen()&&overlay.shouldCreateWorldFog(),"original event flags remain active");
  Configs.BOSS_BAR_MERGE.setBooleanValue(false);check(render(mc,overlay).equals(List.of("Wither","Wither")),"disabled actual render restores bars");
  Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_MERGE.setBooleanValue(true);Configs.BOSS_BAR_MERGE_MAX_BARS.setIntegerValue(1);
  populate(overlay,List.of(first,second,third));check(render(mc,overlay).equals(List.of("First")),"actual render extra limit");
  Configs.BOSS_BAR_MERGE_MAX_BARS.setIntegerValue(0);
  List<LerpingBossEvent> many=new ArrayList<>();for(int i=0;i<30;i++)many.add(bar("Boss "+i,1));populate(overlay,many);
  int y=12,height=mc.getWindow().getGuiScaledHeight(),expected=0;do{expected++;y+=19;}while(y<height/3&&expected<many.size());
  check(render(mc,overlay).size()==expected,"vanilla height limit preserved");
  overlay.reset();check(render(mc,overlay).isEmpty()&&events(overlay).isEmpty(),"world reset clears source and render view");
  Configs.BOSS_BAR_SHOW_HEALTH.setBooleanValue(true);populate(overlay,List.of(calm));
  check(render(mc,overlay).equals(List.of("Wither  80%")),"unknown health falls back to progress percent");
  Configs.BOSS_BAR_COMPACT.setBooleanValue(true);check(render(mc,overlay).equals(List.of("Wither  80%")),"compact extraction retains health text");
  Configs.BETTER_BOSS_BAR.setBooleanValue(false);check(render(mc,overlay).equals(List.of("Wither")),"master disables all rendering changes");
  check(Configs.getExpansionChildren(Configs.BETTER_BOSS_BAR).containsAll(List.of(Configs.BOSS_BAR_MERGE,Configs.BOSS_BAR_SHOW_HEALTH,Configs.BOSS_BAR_COMPACT,Configs.BOSS_BAR_SCALE,Configs.BOSS_BAR_HOLD_ONLY,Configs.BOSS_BAR_HOLD_KEY)),"Boss children include scale and independent hold settings");
  Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BETTER_BOSS_BAR.getKeybind().setValueFromString("F8");new Configs().save();
  var path=Configs.getHalfMasaDirectory().resolve("halfmasa.json");var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();var client=json.getAsJsonObject("Client");
  client.add("bossBarMerge",client.remove("betterBossBar"));client.remove("betterBossBarExpanded");json.addProperty("ConfigVersion",48);java.nio.file.Files.writeString(path,json.toString());
  var group=CustomConfigGroupStore.getInstance().create("Migration Test");CustomConfigGroupStore.getInstance().setMain(group.id,"bossBarMerge");
  defaults();new Configs().load();
  check(Configs.BETTER_BOSS_BAR.getBooleanValue()&&Configs.BOSS_BAR_MERGE.getBooleanValue()&&Configs.BETTER_BOSS_BAR.getKeybind().getStringValue().equals("F8"),"legacy master and hotkey migration");
  check(CustomConfigGroupStore.getInstance().getGroups().stream().anyMatch(g->g.id.equals(group.id)&&g.mainConfig.equals("betterBossBar")),"custom group master reference migration");
  new Configs().save();json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();client=json.getAsJsonObject("Client");
  client.addProperty("bossBarMerge",false);client.addProperty("bossBarShowHealth",true);client.addProperty("bossBarCompact",true);java.nio.file.Files.writeString(path,json.toString());defaults();new Configs().load();
  check(!Configs.BOSS_BAR_MERGE.getBooleanValue()&&Configs.BOSS_BAR_SHOW_HEALTH.getBooleanValue()&&Configs.BOSS_BAR_COMPACT.getBooleanValue(),"previous plain boolean child configurations preserve all switch states");
  check(bossToggles.stream().allMatch(t->t.getKeybind().getStringValue().isEmpty()),"legacy child switches begin without bindings");
  defaults();new Configs().save();System.out.println("BOSS_MERGE_ASSERTIONS_PASS count="+assertions);
 }
 @Override public void onInitializeClient(){instance=this;}
 private static Map<UUID,LerpingBossEvent> eventsUnchecked(BossHealthOverlay overlay){try{return events(overlay);}catch(Exception e){throw new IllegalStateException(e);}}
 public static void onFrame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private void tick(Minecraft mc){try{
  if(stage==0){
   if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null){
    if(++waited==300)System.out.println("BOSS_MERGE_WAIT screen="+MinecraftClientCompat.getScreen(mc));return;
   }
   waited=0;
   tests(mc);stage=1;
   mc.createWorldOpenFlows().createFreshLevel("boss-fixture",new LevelSettings("Boss Fixture",GameType.CREATIVE,
    new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),true,WorldDataConfiguration.DEFAULT),
    new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;
  }
  if(mc.player==null||mc.level==null)return;
  if(++waited<40)return;waited=0;var overlay=mc.gui.hud.getBossOverlay();
  if(stage==1){
   MinecraftClientCompat.setScreen(mc,null);Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_MERGE.setBooleanValue(true);
   populate(overlay,List.of(bar("Wither",0.8F),bar("Wither",0.8F),bar("Wither",0.4F),bar(Component.literal("Wither"),0.8F,BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS),bar("Ender Dragon",0.6F)));
   stage=2;return;
  }
  if(stage==2){check(render(mc,overlay).size()==4,"world strict render");net.minecraft.client.Screenshot.grab(mc,false);Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.setBooleanValue(true);stage=3;return;}
  if(stage==3){check(render(mc,overlay).size()==3,"world mixed-health render");net.minecraft.client.Screenshot.grab(mc,false);Configs.BOSS_BAR_MERGE.toggleBooleanValue();stage=4;return;}
  if(stage==4){check(render(mc,overlay).size()==5,"world toggle restores originals");net.minecraft.client.Screenshot.grab(mc,false);overlay.reset();stage=5;return;}
  if(stage==5){
   check(render(mc,overlay).isEmpty(),"world clear has no residue");defaults();Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_SHOW_HEALTH.setBooleanValue(true);
   mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=player.level();
    for(int i=0;i<2;i++){var wither=net.minecraft.world.entity.EntityTypes.WITHER.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);wither.setNoAi(true);wither.setNoGravity(true);wither.setHealth(240);wither.setPos(player.getX()+i*3,player.getY()+2,player.getZ()+5);level.addFreshEntity(wither);
     var event=((WitherBossEventAccessor)wither).halfmasa$bossEvent();event.setProgress(0.8F);event.addPlayer(player);realBossIds.add(event.getId());}realReady=true;});stage=6;return;
  }
  if(stage==6){
   if(!realReady||realBossIds.stream().anyMatch(id->!eventsUnchecked(overlay).containsKey(id)))return;
   var originals=realBossIds.stream().map(id->eventsUnchecked(overlay).get(id)).toList();var grouped=merge(originals);
   String value=BossBarHealth.value(grouped.getFirst());if(!value.equals("480/600")){waited=0;return;}
   check(!originals.get(0).getName().equals(originals.get(1).getName()),"real Withers carry different UUID metadata");check(grouped.size()==1,"real Wither names merge");
   scaleAndHold(mc,overlay);
   check(value.equals("480/600"),"real merged health total from verified entity/server data");net.minecraft.client.Screenshot.grab(mc,false);Configs.BOSS_BAR_COMPACT.setBooleanValue(true);stage=7;return;
  }
  if(stage==7){check(render(mc,overlay).stream().anyMatch(s->s.contains("480/600")),"compact actual Wither health labels");net.minecraft.client.Screenshot.grab(mc,false);
   Configs.BETTER_BOSS_BAR_EXPANDED.setBooleanValue(true);Configs.BOSS_BAR_MERGE_EXPANDED.setBooleanValue(true);ConfigScrollMemory.saveSelectedTab("ALL");GuiBase.openGui(new HalfMasaConfigScreen());
   var getList=GuiListBase.class.getDeclaredMethod("getListWidget");getList.setAccessible(true);var list=(WidgetListConfigOptions)getList.invoke(MinecraftClientCompat.getScreen(mc));var search=list.getSearchBarWidget();search.setSearchOpen(true);
   var box=WidgetSearchBar.class.getDeclaredField("searchBox");box.setAccessible(true);((GuiTextFieldGeneric)box.get(search)).setValue("boss");list.refreshEntries();stage=8;return;}
  if(stage==8){net.minecraft.client.Screenshot.grab(mc,false);defaults();new Configs().save();System.out.println("BOSS_MERGE_WORLD_PASS assertions="+assertions);stage=9;mc.stop();}
 }catch(Throwable failure){throw new IllegalStateException("Boss bar merge smoke failed stage="+stage,failure);}}
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--jvm-arg", action="append", default=[])
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--extra-mod", action="append", type=Path, default=[])
    parser.add_argument("--malilib", type=Path, help="Use this MaLiLib jar only in the isolated test client")
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    args.version = "26.3"
    directory = ROOT / "build/boss-bar-merge-tests" / datetime.now(timezone(timedelta(hours=8))).strftime("%Y%m%d-%H%M%S-%f")
    directory.mkdir(parents=True)
    source = directory / "BossSmoke.java"
    source.write_text(SOURCE, encoding="utf-8")
    mixin_source = directory / "BossSmokeMixin.java"
    mixin_source.write_text('''package io.github.halfmasa.bosssmoke.mixin;
import io.github.halfmasa.bosssmoke.BossSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class BossSmokeMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void bossSmoke$frame(CallbackInfo ci){BossSmoke.onFrame((Minecraft)(Object)this);}
}''', encoding="utf-8")
    classes = directory / "classes"
    classes.mkdir()
    cp = str(args.artifact) + os.pathsep + (ROOT / "build/port-classpath-26.3.txt").read_text(encoding="utf-8").strip()
    subprocess.run([str(args.java_home / "bin/javac.exe"), "-proc:none", "-encoding", "UTF-8", "-cp", cp, "-d", str(classes), str(source), str(mixin_source)], check=True)
    mod = directory / "boss-smoke.jar"
    with zipfile.ZipFile(mod, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_boss_smoke", "version": "1.0", "environment": "client", "entrypoints": {"client": ["io.github.halfmasa.bosssmoke.BossSmoke"]}, "mixins": ["boss-smoke.mixins.json"]}))
        jar.writestr("boss-smoke.mixins.json", json.dumps({"required": True, "package": "io.github.halfmasa.bosssmoke.mixin", "compatibilityLevel": "JAVA_25", "client": ["BossSmokeMixin"], "injectors": {"defaultRequire": 1}}))
    game = directory / "game"
    (game / "mods").mkdir(parents=True)
    (game / "options.txt").write_text("onboardAccessibility:false\nenableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:zh_cn\ntutorialStep:none\n", encoding="utf-8")
    (game / "config").mkdir()
    (game / "config/malilib.json").write_text("{}\n", encoding="utf-8")
    api = next((Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.161.0+26.3").glob("*/*.jar"))
    shutil.copy2(api, game / "mods" / api.name)
    for extra in args.extra_mod:
        shutil.copy2(extra, game / "mods" / extra.name)
    command = startup.production_command(args, game, mod)
    command[command.index("--width") + 1] = "960"
    command[command.index("--height") + 1] = "540"
    command[1:1] = args.jvm_arg
    if args.malilib:
        properties = dict(line.split("=", 1) for line in (ROOT / "versions/26.3/gradle.properties").read_text().splitlines() if "=" in line)
        (game / "mods" / ("GcWjdA9I-" + properties["malilib_modrinth_version_id"] + ".jar")).unlink()
        shutil.copy2(args.malilib, game / "mods" / args.malilib.name)
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
    passed = code == 0 and not timed_out and "BOSS_MERGE_WORLD_PASS" in log
    report = {"result": "PASS" if passed else "FAIL", "artifact": str(args.artifact), "extra_mods": [str(path.resolve()) for path in args.extra_mod], "jvm_args": args.jvm_arg, "malilib_override": str(args.malilib.resolve()) if args.malilib else None, "duration_seconds": round(time.monotonic() - started, 1), "exit_code": code, "timed_out": timed_out, "log": str(log_path), "checks": [line for line in log.splitlines() if "BOSS_MERGE_" in line], "screenshots": [str(path) for path in (game / "screenshots").glob("*.png")]}
    path = directory / "report.json"
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"BOSS MERGE: {report['result']}", flush=True)
    print(path, flush=True)
    if not passed:
        print("\n".join(log.splitlines()[-65:]), flush=True)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

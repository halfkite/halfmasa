"""Exercise native entity rendering filters, cached mod entity types and same-row controls in an isolated 26.3 client."""
import argparse
from datetime import datetime
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import time
import zipfile
import run_startup_smoke as startup
import smoke_remap

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''package io.github.halfmasa.entitydisablesmoke;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.widgets.*;
import fi.dy.masa.malilib.hotkeys.*;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.gui.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
public class EntityDisableSmoke implements ClientModInitializer {
 private static EntityDisableSmoke instance;
 public static boolean probe;
 public static final Set<UUID> extracted=new HashSet<>();
 private final List<UUID> ids=new ArrayList<>();
 private volatile boolean ready,serverChecked;
 private volatile float serverHealth;
 private int stage,checks;private long since;
 private Entity cow,zombie,item,custom,secondCow;
 private final List<Integer> savedKeys=new ArrayList<>();
 @Override public void onInitializeClient(){instance=this;net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(Fixture.CUSTOM,net.minecraft.client.renderer.entity.CowRenderer::new);}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 public static void rendered(Entity entity){if(probe)extracted.add(entity.getUUID());}
 private static Field field(Class<?> type,String name)throws Exception{for(Class<?> t=type;t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
 private static Method method(Class<?> type,String name,Class<?>...args)throws Exception{for(Class<?> t=type;t!=null;t=t.getSuperclass())try{var m=t.getDeclaredMethod(name,args);m.setAccessible(true);return m;}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);}
 private void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);System.out.println("ENTITY_DISABLE_CHECK "+why);}
 private void next(Minecraft mc,int value){stage=value;since=mc.level.getGameTime();System.out.println("ENTITY_DISABLE_STAGE "+stage);}
 private void defaults(){Configs.CUSTOM_ENTITY_RENDER_DISABLE.resetToDefault();Configs.ENTITY_RENDER_DISABLE_MODE.resetToDefault();Configs.ENTITY_RENDER_DISABLE_BLACKLIST.resetToDefault();Configs.ENTITY_RENDER_DISABLE_WHITELIST.resetToDefault();Configs.ENTITY_RENDER_HOLD_SHOW.resetToDefault();Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.resetToDefault();Configs.ENTITY_RENDER_DISABLE_EXPANDED.resetToDefault();Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY.resetToDefault();Configs.ENTITY_RENDER_AGGREGATION.setBooleanValue(false);Configs.ITEM_RENDER_AGGREGATION.setBooleanValue(false);EntityRenderAggregation.getInstance().clear();}
 private void configs()throws Exception{
  defaults();check(!Configs.CUSTOM_ENTITY_RENDER_DISABLE.getBooleanValue()&&Configs.CUSTOM_ENTITY_RENDER_DISABLE.getKeybind().getKeys().isEmpty()&&!Configs.ENTITY_RENDER_HOLD_SHOW.getBooleanValue(),"disabled unbound master and hold switch");
  check(Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue()==EntityRenderDisableMode.BLACKLIST&&Configs.ENTITY_RENDER_DISABLE_BLACKLIST.getStrings().isEmpty()&&Configs.ENTITY_RENDER_DISABLE_WHITELIST.getStrings().isEmpty(),"default blacklist and empty lists");
  var children=List.of(Configs.ENTITY_RENDER_DISABLE_MODE,Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY,Configs.ENTITY_RENDER_DISABLE_BLACKLIST,Configs.ENTITY_RENDER_DISABLE_WHITELIST,Configs.ENTITY_RENDER_HOLD_SHOW,Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY);
  check(Configs.getExpansionChildren(Configs.CUSTOM_ENTITY_RENDER_DISABLE).containsAll(children)&&Configs.getExpansionConfig(Configs.CUSTOM_ENTITY_RENDER_DISABLE)==Configs.ENTITY_RENDER_DISABLE_EXPANDED,"folding parent and all children");
  Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);check(children.stream().noneMatch(c->Configs.getDisabledView().contains(c)||Configs.getAllView().contains(c)),"collapsed children hidden in disabled and all views");Configs.ENTITY_RENDER_DISABLE_EXPANDED.setBooleanValue(true);check(children.stream().allMatch(c->Configs.getDisabledView().contains(c)&&Configs.getAllView().contains(c)),"expanded children visible in both views");
  var cb=((KeybindMulti)Configs.CUSTOM_ENTITY_RENDER_DISABLE.getKeybind()).getCallback();check(cb!=null&&cb.onKeyAction(KeyAction.PRESS,Configs.CUSTOM_ENTITY_RENDER_DISABLE.getKeybind())&&Configs.CUSTOM_ENTITY_RENDER_DISABLE.getBooleanValue(),"native same-row master callback toggles");
  var mode=((KeybindMulti)Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY.getKeybind()).getCallback();for(var expected:List.of(EntityRenderDisableMode.WHITELIST,EntityRenderDisableMode.NONE,EntityRenderDisableMode.BLACKLIST)){mode.onKeyAction(KeyAction.PRESS,Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY.getKeybind());check(Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue()==expected,"mode hotkey cycles "+expected);}check(!mode.onKeyAction(KeyAction.RELEASE,Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY.getKeybind())&&Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue()==EntityRenderDisableMode.BLACKLIST,"release does not double-cycle");
  check(EntityRenderDisableMode.BLACKLIST.cycle(false)==EntityRenderDisableMode.NONE&&EntityRenderDisableMode.NONE.fromString(null)==EntityRenderDisableMode.BLACKLIST,"reverse cycle and malformed mode fallback");
  var provider=new HalfMasaConfigExpansionProvider(CustomConfigGroupStore.HALF_MASA_SOURCE);check(provider.getInlineCompanion(Configs.ENTITY_RENDER_DISABLE_MODE)==Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY&&provider.isInlineCompanion(Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY),"mode selector and binding share one row");
  check(Configs.HOTKEYS.containsAll(List.of(Configs.CUSTOM_ENTITY_RENDER_DISABLE,Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY,Configs.ENTITY_RENDER_HOLD_SHOW,Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY)),"all action and passive keys registered");
  check(HoldToShow.isPassive(Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY)&&((KeybindMulti)Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getKeybind()).getCallback()==null,"hold key passive without toggle callback");defaults();
 }
 private boolean dispatch(Minecraft mc,Entity e){var f=new Frustum(new org.joml.Matrix4f(),new org.joml.Matrix4f().scaling(.001f));f.prepare(mc.player.getX(),mc.player.getY(),mc.player.getZ());return mc.getEntityRenderDispatcher().shouldRender(e,f,mc.player.getX(),mc.player.getY(),mc.player.getZ(),0);}
 private List<Entity> filtered(List<Entity> entities){var result=new ArrayList<Entity>();EntityRenderAggregation.getInstance().filterForRendering(entities).forEach(result::add);return result;}
 private void rules(Minecraft mc)throws Exception{
  Configs.CUSTOM_ENTITY_RENDER_DISABLE.setBooleanValue(true);var sample=List.of(cow,zombie,item,custom);
  var same=EntityRenderAggregation.getInstance().filterForRendering(sample);check(filtered(sample).isEmpty()&&sample.stream().allMatch(EntityRenderDisableService::shouldHide),"empty blacklist hides all unprotected entities");
  Configs.ENTITY_RENDER_DISABLE_BLACKLIST.setStrings(List.of(" zombie ","MINECRAFT:ZOMBIE","invalid id!", "minecraft:not_registered", ""));check(filtered(sample).equals(List.of(zombie)),"normalized blacklist applies with aggregation disabled and ignores invalid IDs");check(dispatch(mc,zombie)&&!dispatch(mc,cow),"native dispatcher keeps blacklisted type and hides outside types");
  var extractor=new LevelExtractor(mc,new LevelRenderState(),mc.levelRenderer);var frustum=new Frustum(new org.joml.Matrix4f(),new org.joml.Matrix4f().scaling(.001f));frustum.prepare(mc.player.getX(),mc.player.getY(),mc.player.getZ());check(!extractor.isEntityVisible(cow,frustum,mc.player.getX(),mc.player.getY(),mc.player.getZ(),0,0),"actual LevelExtractor boundary rejects hidden entity");
  Configs.ENTITY_RENDER_DISABLE_BLACKLIST.setStrings(List.of("item"));check(filtered(sample).equals(List.of(item)),"live list callback replaces cache immediately");
  Configs.ENTITY_RENDER_DISABLE_BLACKLIST.setStrings(List.of("entity_disable_fixture:custom_cow"));check(!EntityRenderDisableService.shouldHide(custom)&&EntityRenderDisableService.shouldHide(cow),"registered mod entity ID matches independently of vanilla cow");
  Configs.ENTITY_RENDER_DISABLE_BLACKLIST.setStrings(List.of("zombie"));Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(EntityRenderDisableMode.WHITELIST);Configs.ENTITY_RENDER_DISABLE_WHITELIST.setStrings(List.of("cow"));check(filtered(sample).equals(List.of(zombie,item,custom)),"whitelist hides only listed entity type");
  Configs.ENTITY_RENDER_DISABLE_WHITELIST.setStrings(List.of());check(EntityRenderAggregation.getInstance().filterForRendering(sample)==sample&&sample.stream().noneMatch(EntityRenderDisableService::shouldHide),"empty whitelist skips filtering and hides none");Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(EntityRenderDisableMode.NONE);check(sample.stream().allMatch(EntityRenderDisableService::shouldHide),"no-list mode hides all types");check(!EntityRenderDisableService.shouldHide(mc.player),"local player protected for third person");
  var original=mc.getCameraEntity();mc.setCameraEntity(cow);check(!EntityRenderDisableService.shouldHide(cow)&&EntityRenderDisableService.shouldHide(zombie),"spectator camera entity protected");mc.setCameraEntity(original);
  check(!EntityRenderDisableService.shouldHide(null),"null entity safe");Configs.CUSTOM_ENTITY_RENDER_DISABLE.setBooleanValue(false);check(filtered(sample)==sample||filtered(sample).equals(sample),"master off restores entities immediately");Configs.CUSTOM_ENTITY_RENDER_DISABLE.setBooleanValue(true);
 }
 @SuppressWarnings("unchecked") private void holding(Minecraft mc)throws Exception{
  var keys=(List<Integer>)field(KeybindMulti.class,"PRESSED_KEYS").get(null);savedKeys.addAll(keys);keys.clear();boolean focused=mc.isWindowActive();var focus=field(mc.getWindow().getClass(),"focused");focus.setBoolean(mc.getWindow(),true);
  Configs.ENTITY_RENDER_HOLD_SHOW.setBooleanValue(true);Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getKeybind().setValueFromString("F8");check(EntityRenderDisableService.shouldHide(cow),"unpressed key keeps hide rule");keys.addAll(Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getKeybind().getKeys());check(!EntityRenderDisableService.shouldHide(cow)&&Configs.CUSTOM_ENTITY_RENDER_DISABLE.getBooleanValue(),"held key bypasses hiding without changing master");keys.clear();check(EntityRenderDisableService.shouldHide(cow),"release restores hiding immediately");keys.addAll(Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getKeybind().getKeys());focus.setBoolean(mc.getWindow(),false);check(EntityRenderDisableService.shouldHide(cow),"focus loss ends temporary display");focus.setBoolean(mc.getWindow(),true);MinecraftClientCompat.setScreen(mc,new TitleScreen());check(EntityRenderDisableService.shouldHide(cow),"GUI cannot activate held display");MinecraftClientCompat.setScreen(mc,null);
  keys.clear();Configs.ENTITY_RENDER_HOLD_SHOW.setBooleanValue(false);var binding=Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getKeybind();fi.dy.masa.malilib.event.InputEventHandler.getKeybindManager().updateUsedKeys();var input=(fi.dy.masa.malilib.event.InputEventHandler)fi.dy.masa.malilib.event.InputEventHandler.getInputManager();int holdCode=binding.getKeys().getFirst();var holdEvent=new net.minecraft.client.input.KeyEvent(holdCode,0,0);input.onKeyInput(holdEvent,com.mojang.blaze3d.platform.InputConstants.PRESS,mc);check(EntityRenderDisableService.shouldHide(cow),"bound temporary key requires enabled hold switch");Configs.ENTITY_RENDER_HOLD_SHOW.setBooleanValue(true);check(!EntityRenderDisableService.shouldHide(cow),"enabling hold switch while key is held restores rendering immediately");var moveEvent=new net.minecraft.client.input.KeyEvent(io.github.halfmasa.xaerobinding.compat.InputCompat.layoutKeyCode(87),0,0);input.onKeyInput(moveEvent,com.mojang.blaze3d.platform.InputConstants.PRESS,mc);check(!EntityRenderDisableService.shouldHide(cow),"native MaLiLib key press works with movement held");input.onKeyInput(holdEvent,com.mojang.blaze3d.platform.InputConstants.REPEAT,mc);check(!EntityRenderDisableService.shouldHide(cow),"native repeat leaves hold active");input.onKeyInput(holdEvent,com.mojang.blaze3d.platform.InputConstants.RELEASE,mc);check(EntityRenderDisableService.shouldHide(cow)&&Configs.CUSTOM_ENTITY_RENDER_DISABLE.getBooleanValue(),"native release restores hiding without changing master");input.onKeyInput(moveEvent,com.mojang.blaze3d.platform.InputConstants.RELEASE,mc);
  var ordinary=Configs.CUSTOM_ENTITY_RENDER_DISABLE.getKeybind();ordinary.setValueFromString("F8");fi.dy.masa.malilib.event.InputEventHandler.getKeybindManager().updateUsedKeys();var manager=KeybindPieManager.getInstance();var set=(Set<Integer>)field(manager.getClass(),"pressedInputKeys").get(manager);var prior=Set.copyOf(set);set.clear();int code=ordinary.getKeys().getFirst();set.add(code);var order=(List<Integer>)field(manager.getClass(),"pressedInputOrder").get(manager);var oldOrder=List.copyOf(order);order.clear();order.add(code);var candidates=(List<KeybindPieManager.PieAction>)method(manager.getClass(),"wheelCandidates",com.mojang.blaze3d.platform.InputConstants.Key.class,net.minecraft.client.gui.screens.Screen.class).invoke(manager,io.github.halfmasa.xaerobinding.compat.InputCompat.keyboardKey(code),null);check(candidates.stream().noneMatch(a->a.hotkey()==Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY)&&candidates.stream().anyMatch(a->a.hotkey()==Configs.CUSTOM_ENTITY_RENDER_DISABLE),"actual conflict selection excludes only passive key");set.clear();set.addAll(prior);order.clear();order.addAll(oldOrder);
  var browser=new KeymapBrowserScreen();method(browser.getClass(),"rebuildEntries").invoke(browser);var entries=(List<KeymapBrowserScreen.BrowserEntry>)field(browser.getClass(),"allEntries").get(browser);for(var entry:entries)if(entry.hotkey()==Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY)check(entry.getClass().getMethod("conflictType").invoke(entry).toString().equals("NONE"),"browser never marks passive key conflicting");
  if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("tweakeroo")){
   var foreign=(fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed)Class.forName("fi.dy.masa.tweakeroo.config.Configs$Disable").getField("DISABLE_ENTITY_RENDERING").get(null);boolean original=foreign.getBooleanValue();keys.clear();keys.addAll(Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getKeybind().getKeys());foreign.setBooleanValue(true);check(!EntityRenderDisableService.shouldHide(cow)&&!dispatch(mc,cow),"held bypass does not undo actual Tweakeroo global rendering disable");foreign.setBooleanValue(original);
  }
  keys.clear();keys.addAll(savedKeys);focus.setBoolean(mc.getWindow(),focused);Configs.ENTITY_RENDER_HOLD_SHOW.setBooleanValue(false);
 }
 @SuppressWarnings("unchecked") private void layout(Minecraft mc)throws Exception{
  var ui=(HalfMasaConfigScreen)MinecraftClientCompat.getScreen(mc);var list=(WidgetListConfigOptions)method(GuiListBase.class,"getListWidget").invoke(ui);
  var widgets=(List<WidgetConfigOption>)field(list.getClass(),"listWidgets").get(list);
  var main=widgets.stream().filter(w->w.getEntry().getConfig()==Configs.CUSTOM_ENTITY_RENDER_DISABLE).findFirst().orElseThrow();
  var mainWidgets=(List<WidgetBase>)field(main.getClass(),"subWidgets").get(main);var toggle=mainWidgets.stream().filter(w->w.getClass().getSimpleName().equals("ConfigButtonBoolean")).findFirst().orElseThrow();var binding=mainWidgets.stream().filter(w->w instanceof fi.dy.masa.malilib.gui.button.ConfigButtonKeybind).findFirst().orElseThrow();
  check(toggle.getY()==binding.getY()&&toggle.getX()+toggle.getWidth()<=binding.getX(),"actual master toggle and binding aligned without overlap");
  var mode=widgets.stream().filter(w->w.getEntry().getConfig()==Configs.ENTITY_RENDER_DISABLE_MODE).findFirst().orElseThrow();var modeWidgets=(List<WidgetBase>)field(mode.getClass(),"subWidgets").get(mode);var selector=modeWidgets.stream().filter(w->w instanceof fi.dy.masa.malilib.gui.button.ConfigButtonOptionList).findFirst().orElseThrow();var modeKey=modeWidgets.stream().filter(w->w instanceof fi.dy.masa.malilib.gui.button.ConfigButtonKeybind).findFirst().orElseThrow();check(selector.getY()==modeKey.getY()&&selector.getX()+selector.getWidth()<=modeKey.getX(),"actual mode button and shortcut aligned without overlap");for(var value:EntityRenderDisableMode.values())check(mc.font.width(value.getDisplayName())<=selector.getWidth()-4,"mode label fits native value column: "+value);
  var comment=Configs.ENTITY_RENDER_DISABLE_MODE.getComment();check(comment.contains("无名单：全部禁用渲染")&&comment.contains("白名单模式：仅禁用渲染白名单内实体")&&comment.contains("黑名单模式：名单外的实体禁用渲染"),"localized mode tooltip explains all three requested semantics");check(modeWidgets.stream().filter(w->w instanceof WidgetHoverInfo).map(w->String.join("",((WidgetHoverInfo)w).getLines())).anyMatch(t->t.contains("无名单：全部禁用渲染")),"actual mode label has hover explanation");var hover=(List<String>)field(selector.getClass(),"hoverStrings").get(selector);check(String.join("",hover).contains("空黑名单禁用全部"),"actual mode button has empty-list hover explanation");check(Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getComment().contains("必须先开启"),"temporary key tooltip states required enable switch");
  var entries=(List<GuiConfigsBase.ConfigOptionWrapper>)list.getCurrentEntries();check(entries.stream().noneMatch(e->e.getConfig()==Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY),"inline shortcut does not create a duplicate row");
  var old=Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue();mode.onMouseClicked(new net.minecraft.client.input.MouseButtonEvent(selector.getX()+selector.getWidth()/2.0,selector.getY()+selector.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);check(Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue()!=old,"native mode selector click cycles value");Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(old);
  var arrow=mainWidgets.stream().filter(w->w instanceof fi.dy.masa.malilib.gui.button.ButtonBase).min(Comparator.comparingInt(WidgetBase::getX)).orElseThrow();main.onMouseClicked(new net.minecraft.client.input.MouseButtonEvent(arrow.getX()+arrow.getWidth()/2.0,arrow.getY()+arrow.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);check(!Configs.ENTITY_RENDER_DISABLE_EXPANDED.getBooleanValue()&&!Configs.getDisabledView().contains(Configs.ENTITY_RENDER_DISABLE_MODE),"actual folding arrow collapses all children");Configs.ENTITY_RENDER_DISABLE_EXPANDED.setBooleanValue(true);method(ui.getClass(),"refreshExpandedConfigs").invoke(ui);
 }
 private void tick(Minecraft mc){try{
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;configs();stage=1;mc.createWorldOpenFlows().createFreshLevel("entity-disable-fixture",new LevelSettings("Entity Render Disable Fixture",GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;}
  if(mc.player==null||mc.level==null)return;
  if(stage==1){if(mc.level.getGameTime()<40)return;MinecraftClientCompat.setScreen(mc,null);ids.clear();ready=false;mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.SURVIVAL);player.setPermanentlyInvulnerable(true);for(var type:List.of(EntityTypes.COW,EntityTypes.ZOMBIE,EntityTypes.ITEM,Fixture.CUSTOM,EntityTypes.COW)){Entity e=type.create(player.level(),EntitySpawnReason.COMMAND);if(e==null)throw new AssertionError("fixture entity creation");e.setPos(player.getX()+ids.size()*.5,player.getY(),player.getZ()+4);e.setNoGravity(true);e.setPermanentlyInvulnerable(true);if(e instanceof Mob mob)mob.setNoAi(true);if(e instanceof ItemEntity drop)drop.setItem(new ItemStack(Items.DIAMOND,12));if(e instanceof LivingEntity living)living.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));player.level().addFreshEntity(e);ids.add(e.getUUID());}player.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));player.inventoryMenu.broadcastChanges();ready=true;});next(mc,2);return;}
  if(mc.level.getGameTime()-since>900)throw new AssertionError("stage timeout "+stage);
  switch(stage){
   case 2->{if(!ready)return;var map=new HashMap<UUID,Entity>();mc.level.entitiesForRendering().forEach(e->map.put(e.getUUID(),e));if(ids.stream().anyMatch(id->!map.containsKey(id)))return;cow=map.get(ids.get(0));zombie=map.get(ids.get(1));item=map.get(ids.get(2));custom=map.get(ids.get(3));secondCow=map.get(ids.get(4));rules(mc);holding(mc);Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(EntityRenderDisableMode.WHITELIST);Configs.ENTITY_RENDER_DISABLE_WHITELIST.setStrings(List.of("zombie","item"));mc.player.setYRot(0);mc.player.setXRot(0);extracted.clear();probe=true;next(mc,3);}
   case 3->{if(mc.level.getGameTime()-since<15)return;check(!extracted.contains(zombie.getUUID())&&!extracted.contains(item.getUUID())&&(extracted.contains(cow.getUUID())||extracted.contains(custom.getUUID())),"actual world render extraction hides zombie and dropped item while drawing allowed cows");net.minecraft.client.Screenshot.grab(mc,false);probe=false;Configs.ENTITY_RENDER_DISABLE_WHITELIST.setStrings(List.of("cow"));Configs.ENTITY_RENDER_AGGREGATION.setBooleanValue(true);Configs.ENTITY_AGGREGATION_RADIUS.setDoubleValue(64);Configs.ENTITY_AGGREGATION_THRESHOLD.setIntegerValue(2);Configs.ENTITY_AGGREGATION_SCAN_INTERVAL.setIntegerValue(1);EntityRenderAggregation.getInstance().onClientTick(mc);next(mc,4);}
   case 4->{var agg=EntityRenderAggregation.getInstance();agg.onClientTick(mc);if(!agg.shouldHide(cow)&&!agg.shouldHide(secondCow))return;Entity hidden=agg.shouldHide(cow)?cow:secondCow;Configs.ENTITY_RENDER_HOLD_SHOW.setBooleanValue(true);Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getKeybind().setValueFromString("F8");var keys=(List<Integer>)field(KeybindMulti.class,"PRESSED_KEYS").get(null);var saved=List.copyOf(keys);keys.clear();keys.addAll(Configs.ENTITY_RENDER_HOLD_SHOW_HOTKEY.getKeybind().getKeys());var focus=field(mc.getWindow().getClass(),"focused");boolean old=mc.isWindowActive();focus.setBoolean(mc.getWindow(),true);check(!EntityRenderDisableService.shouldHide(hidden)&&!filtered(List.of(cow,secondCow)).contains(hidden),"held bypass preserves independent aggregation hiding");keys.clear();keys.addAll(saved);focus.setBoolean(mc.getWindow(),old);Configs.ENTITY_RENDER_AGGREGATION.setBooleanValue(false);agg.clear();Configs.ENTITY_RENDER_HOLD_SHOW.setBooleanValue(false);Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(EntityRenderDisableMode.NONE);
    mc.hitResult=new net.minecraft.world.phys.EntityHitResult(cow);field(Minecraft.class,"missTime").setInt(mc,0);ready=false;mc.getSingleplayerServer().execute(()->{var entity=(LivingEntity)mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().level().getEntity(cow.getUUID());entity.setPermanentlyInvulnerable(false);entity.setHealth(20);ready=true;});next(mc,5);}
   case 5->{if(!ready||mc.level.getGameTime()-since<5)return;check(EntityRenderDisableService.shouldHide(cow)&&cow.isAlive()&&cow.isPickable(),"hidden entity retains native alive/pickable state");mc.hitResult=new net.minecraft.world.phys.EntityHitResult(cow);method(Minecraft.class,"startAttack").invoke(mc);serverChecked=false;next(mc,6);}
   case 6->{if(mc.level.getGameTime()-since<10)return;if(!serverChecked){mc.getSingleplayerServer().execute(()->{var e=(LivingEntity)mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().level().getEntity(cow.getUUID());serverHealth=e.getHealth();serverChecked=true;});return;}check(serverHealth<20,"hidden cow receives actual server-confirmed attack damage");
    Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(EntityRenderDisableMode.WHITELIST);Configs.ENTITY_RENDER_DISABLE_WHITELIST.setStrings(List.of("entity_disable_fixture:custom_cow"));Configs.ENTITY_RENDER_DISABLE_BLACKLIST.setStrings(List.of("zombie"));Configs.CUSTOM_ENTITY_RENDER_DISABLE.getKeybind().setValueFromString("F7");Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY.getKeybind().setValueFromString("F6");Configs.ENTITY_RENDER_HOLD_SHOW.setBooleanValue(true);new Configs().save();defaults();new Configs().load();check(Configs.CUSTOM_ENTITY_RENDER_DISABLE.getBooleanValue()&&Configs.ENTITY_RENDER_DISABLE_MODE.getOptionListValue()==EntityRenderDisableMode.WHITELIST&&Configs.ENTITY_RENDER_HOLD_SHOW.getBooleanValue()&&Configs.CUSTOM_ENTITY_RENDER_DISABLE.getKeybind().getStringValue().equals("F7")&&Configs.ENTITY_RENDER_DISABLE_MODE_HOTKEY.getKeybind().getStringValue().equals("F6")&&!EntityRenderDisableService.shouldHide(cow)&&EntityRenderDisableService.shouldHide(custom),"saved config and reload reconstruct cached mod entity rules and bindings");
    Configs.ENTITY_RENDER_DISABLE_MODE.setOptionListValue(EntityRenderDisableMode.BLACKLIST);Configs.ENTITY_RENDER_HOLD_SHOW.setBooleanValue(false);Configs.ENTITY_RENDER_DISABLE_EXPANDED.setBooleanValue(true);var ui=new HalfMasaConfigScreen();var tabField=field(ui.getClass(),"tab");tabField.set(ui,Enum.valueOf((Class)tabField.getType(),"DISABLED"));GuiBase.openGui(ui);next(mc,7);}
   case 7->{if(mc.level.getGameTime()-since<10)return;layout(mc);net.minecraft.client.Screenshot.grab(mc,false);MinecraftClientCompat.setScreen(mc,null);next(mc,8);}
   case 8->{if(mc.level.getGameTime()-since<20)return;defaults();new Configs().save();System.out.println("ENTITY_DISABLE_WORLD_PASS checks="+checks);stage=9;mc.stop();}
  }
 }catch(Throwable failure){throw new IllegalStateException("Entity rendering smoke stage="+stage,failure);}}
}
'''
FIXTURE = r'''package io.github.halfmasa.entitydisablesmoke;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
public class Fixture implements ModInitializer {
 public static EntityType<Cow> CUSTOM;
 @Override public void onInitialize(){var id=Identifier.fromNamespaceAndPath("entity_disable_fixture","custom_cow");CUSTOM=Registry.register(BuiltInRegistries.ENTITY_TYPE,id,EntityType.Builder.of(Cow::new,MobCategory.CREATURE).sized(.9f,1.4f).build(ResourceKey.create(Registries.ENTITY_TYPE,id)));FabricDefaultAttributeRegistry.register(CUSTOM,Cow.createAttributes());}
}
'''
FRAME = r'''package io.github.halfmasa.entitydisablesmoke.mixin;
import io.github.halfmasa.entitydisablesmoke.EntityDisableSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class FrameMixin { @Inject(method="runTick",at=@At("TAIL")) private void frame(CallbackInfo ci){EntityDisableSmoke.frame((Minecraft)(Object)this);} }
'''
CLIPBOARD = r'''package io.github.halfmasa.entitydisablesmoke.mixin;
import io.github.halfmasa.xaerobinding.feature.ScreenshotClipboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ScreenshotClipboard.class) public class ClipboardMixin { @Inject(method="initialize",at=@At("HEAD"),cancellable=true) private static void skip(CallbackInfo ci){ci.cancel();} }
'''
PROBE = r'''package io.github.halfmasa.entitydisablesmoke.mixin;
import io.github.halfmasa.entitydisablesmoke.EntityDisableSmoke;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EntityRenderDispatcher.class) public class ProbeMixin { @Inject(method="extractEntity",at=@At("RETURN")) private <E extends Entity> void extracted(E entity,float partial,CallbackInfoReturnable<EntityRenderState> ci){EntityDisableSmoke.rendered(entity);} }
'''

def port_source(source, version, mapping):
    if version == "26.3":
        return source
    legacy = version.startswith("1.21.")
    source = source.replace(".setPermanentlyInvulnerable(", ".setInvulnerable(")
    source = source.replace("mc.player.getZ(),0);", "mc.player.getZ());")
    if version == "26.2":
        source = source.replace("mc.player.getZ(),0,0)", "mc.player.getZ())")
    else:
        source = source.replace("mc.gui.overlay()", "mc.getOverlay()")
        source = source.replace("EntityTypes.", "EntityType.")
        source = source.replace("WorldPresets::createTestWorldDimensions", "WorldPresets::createFlatWorldDimensions")
        source = source.replace("net.minecraft.client.Screenshot.grab(mc,false)", "net.minecraft.client.Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{})")
        source = source.replace("net.minecraft.client.renderer.extract.LevelExtractor", "net.minecraft.client.renderer.LevelRenderer")
        source = source.replace("var extractor=new LevelExtractor(mc,new LevelRenderState(),mc.levelRenderer);", "var extractor=mc.levelRenderer;")
    if version == "26.1.2":
        source = re.sub(r'  var extractor=mc.levelRenderer;.*?"actual LevelExtractor boundary rejects hidden entity"\);\n', '', source)
    if not legacy:
        return source
    v = tuple(map(int, version.split(".")))
    source = source.replace("new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),true,WorldDataConfiguration.DEFAULT", "false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT")
    rules = "new net.minecraft.world.level.gamerules.GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())" if v >= (1,21,11) else "new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())" if v > (1,21,1) else "new GameRules()"
    source = source.replace("new GameRules()", rules)
    source = source.replace("net.minecraft.client.renderer.state.level.LevelRenderState", "net.minecraft.client.renderer.state.LevelRenderState")
    # Old renderers have no public isEntityVisible boundary. Their dispatcher
    # and actual world render path are still exercised below.
    source = re.sub(r'  var extractor=mc.levelRenderer;.*?"actual LevelExtractor boundary rejects hidden entity"\);\n', '', source)
    if v < (1,21,11):
        source = source.replace("Identifier", "ResourceLocation")
        source = source.replace("net.minecraft.world.entity.animal.cow.Cow", "net.minecraft.world.entity.animal.Cow")
    if v < (1,21,5):
        source = source.replace("player.level().getEntity(", "((net.minecraft.server.level.ServerLevel)player.level()).getEntity(")
        source = source.replace("getFirst()", "get(0)")
        server_level = "mc.getSingleplayerServer().getPlayerList().getPlayers().get(0).level()"
        source = source.replace(server_level + ".getEntity(", "((net.minecraft.server.level.ServerLevel)" + server_level + ").getEntity(")
    if v == (1,21,1):
        source = source.replace("EntitySpawnReason.COMMAND", "MobSpawnType.COMMAND")
        source = source.replace(".create(player.level(),MobSpawnType.COMMAND)", ".create(player.level())")
        source = source.replace(".build(ResourceKey.create(Registries.ENTITY_TYPE,id))", ".build(id.toString())")
        source = source.replace("WorldPresets::createFlatWorldDimensions", "access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()")
    focus = smoke_remap.field_name(mapping, "net/minecraft/client/Minecraft", "windowActive")
    source = source.replace('"missTime"', '"' + smoke_remap.field_name(mapping, "net/minecraft/client/Minecraft", "missTime") + '"')
    owner = None
    for line in mapping.read_text(encoding="utf-8").splitlines():
        columns = line.split('\t')
        if columns[0] == 'c':
            owner = columns[-1]
        elif owner == 'net/minecraft/client/Minecraft' and len(columns) > 5 and columns[1] == 'm' and columns[-1] == 'startAttack':
            source = source.replace('"startAttack"', '"' + columns[-2] + '"')
            break
    source = source.replace('field(mc.getWindow().getClass(),"focused")', 'field(Minecraft.class,"' + focus + '")')
    source = source.replace("focus.setBoolean(mc.getWindow(),", "focus.setBoolean(mc,")
    if v < (1,21,10):
        source = source.replace('var holdEvent=new net.minecraft.client.input.KeyEvent(holdCode,0,0);', '')
        source = source.replace('var moveEvent=new net.minecraft.client.input.KeyEvent(io.github.halfmasa.xaerobinding.compat.InputCompat.layoutKeyCode(87),0,0);', '')
        source = source.replace('input.onKeyInput(holdEvent,', 'input.onKeyInput(holdCode,0,0,')
        source = source.replace('input.onKeyInput(moveEvent,', 'input.onKeyInput(io.github.halfmasa.xaerobinding.compat.InputCompat.layoutKeyCode(87),0,0,')
        for button in ('selector','arrow'):
            row = 'mode' if button == 'selector' else 'main'
            source = source.replace(f'{row}.onMouseClicked(new net.minecraft.client.input.MouseButtonEvent({button}.getX()+{button}.getWidth()/2.0,{button}.getY()+{button}.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false)', f'{row}.onMouseClicked({button}.getX()+{button}.getWidth()/2,{button}.getY()+{button}.getHeight()/2,0)')
    return source


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--version", default="26.3")
    parser.add_argument("--loader-version")
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--jvm-arg", action="append", default=[])
    parser.add_argument("--extra-mod", action="append", type=Path, default=[])
    parser.add_argument("--with-api", action="store_true")
    parser.add_argument("--malilib", type=Path, help="Override MaLiLib only inside the isolated fixture")
    parser.add_argument("--timeout", type=int, default=480)
    parser.set_defaults(with_api=True)
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    directory = ROOT / "build/entity-render-disable-tests" / datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    directory.mkdir(parents=True)
    classes = directory / "classes"
    classes.mkdir()
    sources = []
    compile_cp = (ROOT / f"build/port-classpath-{args.version}.txt").read_text().strip()
    legacy = args.version.startswith("1.21.")
    mapping = smoke_remap.mappings(args.version, compile_cp) if legacy else None
    probe = PROBE
    if args.version != "26.3":
        probe = probe.replace('import net.minecraft.client.renderer.entity.state.EntityRenderState;', 'import net.minecraft.client.renderer.culling.Frustum;')
        probe = probe.replace('@Inject(method="extractEntity",at=@At("RETURN")) private <E extends Entity> void extracted(E entity,float partial,CallbackInfoReturnable<EntityRenderState> ci){EntityDisableSmoke.rendered(entity);}', '@Inject(method="shouldRender",at=@At("RETURN")) private void visible(Entity entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue())EntityDisableSmoke.rendered(entity);}')
    for name, source in [("EntityDisableSmoke", SOURCE), ("Fixture", FIXTURE), ("FrameMixin", FRAME), ("ProbeMixin", probe), ("ClipboardMixin", CLIPBOARD)]:
        source = port_source(source, args.version, mapping)
        if legacy:
            source = source.replace("import net.minecraft.client.renderer.state.LevelRenderState;", "")
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
    mod = directory / "entity-disable-smoke.jar"
    with zipfile.ZipFile(mod, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_entity_disable_smoke", "version": "1.0", "environment": "*", "entrypoints": {"main": ["io.github.halfmasa.entitydisablesmoke.Fixture"], "client": ["io.github.halfmasa.entitydisablesmoke.EntityDisableSmoke"]}, "mixins": ["entity-disable-smoke.mixins.json"]}))
        jar.writestr("entity-disable-smoke.mixins.json", json.dumps({"required": True, "package": "io.github.halfmasa.entitydisablesmoke.mixin", "compatibilityLevel": "JAVA_21" if legacy else "JAVA_25", "client": ["FrameMixin", "ProbeMixin", "ClipboardMixin"], "injectors": {"defaultRequire": 1}}))
    if legacy:
        mod = smoke_remap.remap(args.java_home, mod, directory / "entity-disable-smoke-remapped.jar", mapping, cp, directory / "remap.log")
    game = directory / "game"
    (game / "mods").mkdir(parents=True)
    (game / "config").mkdir()
    (game / "config/malilib.json").write_text("{}\n")
    (game / "options.txt").write_text("onboardAccessibility:false\nenableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:zh_cn\ntutorialStep:none\nparticles:0\n")
    if args.with_api:
        api_versions = dict(re.findall(r"'([\d.]+)': '([^']+)'", (ROOT / "common.gradle").read_text(encoding="utf-8")))
        api = next(path for path in (Path.home() / f".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/{api_versions[args.version]}").glob("*/*.jar") if not path.name.endswith(("-sources.jar", "-javadoc.jar")))
        shutil.copy2(api, game / "mods" / api.name)
    for artifact in args.extra_mod:
        shutil.copy2(artifact, game / "mods" / artifact.name)
    command = startup.production_command(args, game, mod)
    if args.malilib:
        for installed in (game / "mods").glob("*.jar"):
            with zipfile.ZipFile(installed) as jar:
                if "fabric.mod.json" not in jar.namelist():
                    continue
                metadata = json.loads(jar.read("fabric.mod.json"))
            if metadata.get("id") == "malilib":
                # Replace only our own isolated copy; preserve the user installation.
                assert installed.resolve().parent == (game / "mods").resolve()
                installed.write_bytes(args.malilib.read_bytes())
    command[1:1] = args.jvm_arg
    command[command.index("--width") + 1] = "960"
    command[command.index("--height") + 1] = "540"
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
    passed = code == 0 and not timed_out and "ENTITY_DISABLE_WORLD_PASS" in log
    report = {"result": "PASS" if passed else "FAIL", "artifact": str(args.artifact), "fabric_api_installed": args.with_api, "clipboard_initialization_disabled": True, "input_mode": "Simulated MaLiLib key events; actual integrated-world rendering and attacks", "extra_mods": [str(p.resolve()) for p in args.extra_mod], "jvm_args": args.jvm_arg, "malilib_override": str(args.malilib) if args.malilib else None, "duration_seconds": round(time.monotonic() - started, 1), "exit_code": code, "timed_out": timed_out, "log": str(log_path), "checks": [line for line in log.splitlines() if "ENTITY_DISABLE_" in line], "screenshots": [str(p) for p in (game / "screenshots").glob("*.png")]}
    (directory / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("ENTITY RENDER DISABLE:", report["result"], flush=True)
    print(directory / "report.json", flush=True)
    if not passed:
        print("\n".join(log.splitlines()[-45:]), flush=True)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())

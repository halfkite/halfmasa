"""Exercise vanilla marker generation and real particle sprites in an isolated 26.3 client."""
import argparse
from datetime import datetime
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
package io.github.halfmasa.invisiblesmoke;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.widgets.*;
import fi.dy.masa.malilib.hotkeys.*;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.ConfigScrollMemory;
import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
public class InvisibleSmoke implements ClientModInitializer {
 private static InvisibleSmoke instance;
 public static boolean probe;
 public static final List<Mark> marks=new ArrayList<>();
 public record Mark(BlockState state,double x,double y,double z){}
 private final List<BlockPos> positions=new ArrayList<>();
 private List<BlockState> states;
 private volatile boolean ready;
 private int stage,checks;
 private long since;
 private java.util.UUID attackTarget;private BlockPos leverPos;private volatile float serverHealth;private volatile boolean serverChecked;private int entityWait;
 private static void require(boolean condition,String description){if(!condition)throw new AssertionError(description);}
 private void check(boolean condition,String description){checks++;require(condition,description);System.out.println("INVISIBLE_CHECK "+description);}
 @Override public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 public static void particle(ParticleOptions option,double x,double y,double z){if(probe&&option instanceof BlockParticleOption block&&block.getType()==ParticleTypes.BLOCK_MARKER)marks.add(new Mark(block.getState(),x,y,z));}
 private static Field field(Class<?> type,String name)throws Exception{for(Class<?> c=type;c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
 private void defaults(){Configs.INVISIBLE_HOLD_ONLY.resetToDefault();Configs.INVISIBLE_HOLD_KEY.resetToDefault();Configs.INVISIBLE_BLOCKS.resetToDefault();Configs.INVISIBLE_BLOCKS_EXPANDED.resetToDefault();Configs.INVISIBLE_SHOW_BARRIER.resetToDefault();Configs.INVISIBLE_SHOW_LIGHT.resetToDefault();Configs.INVISIBLE_SHOW_STRUCTURE_VOID.resetToDefault();}
 private void configs(){
  defaults();var children=List.of(Configs.INVISIBLE_SHOW_BARRIER,Configs.INVISIBLE_SHOW_LIGHT,Configs.INVISIBLE_SHOW_STRUCTURE_VOID);
  check(!Configs.INVISIBLE_BLOCKS.getBooleanValue()&&Configs.INVISIBLE_BLOCKS.getKeybind().getStringValue().isEmpty()&&children.stream().allMatch(c->c.getBooleanValue()),"one disabled unbound master, three enabled children");
  check(Configs.HOTKEYS.contains(Configs.INVISIBLE_BLOCKS)&&Configs.getExpansionChildren(Configs.INVISIBLE_BLOCKS).containsAll(children)&&Configs.HOTKEYS.containsAll(children),"master and type child hotkeys registered with folding");
  check(children.stream().allMatch(c->Configs.getExpansionParent(c)==Configs.INVISIBLE_BLOCKS)&&Configs.getExpansionConfig(Configs.INVISIBLE_BLOCKS)==Configs.INVISIBLE_BLOCKS_EXPANDED,"folding relationships registered");
  Configs.CUSTOM_CONFIG_GROUPS.setBooleanValue(false);check(children.stream().noneMatch(c->Configs.getClientView().contains(c)||Configs.getAllView().contains(c)),"collapsed children hidden in both views");
  Configs.INVISIBLE_BLOCKS_EXPANDED.setBooleanValue(true);check(children.stream().allMatch(c->Configs.getClientView().contains(c)&&Configs.getAllView().contains(c)),"expanded children visible in both views");
  var callback=((KeybindMulti)Configs.INVISIBLE_BLOCKS.getKeybind()).getCallback();check(callback.onKeyAction(KeyAction.PRESS,Configs.INVISIBLE_BLOCKS.getKeybind())&&Configs.INVISIBLE_BLOCKS.getBooleanValue(),"native master hotkey toggles feature");
  Configs.INVISIBLE_SHOW_BARRIER.setBooleanValue(false);Configs.INVISIBLE_SHOW_LIGHT.setBooleanValue(false);Configs.INVISIBLE_BLOCKS.getKeybind().setValueFromString("F8");new Configs().save();defaults();new Configs().load();
  check(Configs.INVISIBLE_BLOCKS.getBooleanValue()&&Configs.INVISIBLE_BLOCKS_EXPANDED.getBooleanValue()&&!Configs.INVISIBLE_SHOW_BARRIER.getBooleanValue()&&!Configs.INVISIBLE_SHOW_LIGHT.getBooleanValue()&&Configs.INVISIBLE_SHOW_STRUCTURE_VOID.getBooleanValue()&&Configs.INVISIBLE_BLOCKS.getKeybind().getStringValue().equals("F8"),"all configuration values and binding persist");defaults();
 }
 private Block target(Minecraft mc)throws Exception{var method=mc.level.getClass().getDeclaredMethod("getMarkerParticleTarget");method.setAccessible(true);return (Block)method.invoke(mc.level);}
 private void sample(Minecraft mc,int index,Block target,int expected){
  marks.clear();var pos=positions.get(index);probe=true;
  try{mc.level.doAnimateTick(pos.getX(),pos.getY(),pos.getZ(),1,RandomSource.create(123),target,new BlockPos.MutableBlockPos());}finally{probe=false;}
  check(marks.size()==expected,"sample "+index+" has "+expected+" marker(s), target="+target);
  if(expected==1){var mark=marks.getFirst();check(mark.state.equals(states.get(index))&&mark.x==pos.getX()+0.5&&mark.y==pos.getY()+0.5&&mark.z==pos.getZ()+0.5,"marker retains actual block state and native center");}
 }
 private void sprites(Minecraft mc)throws Exception{
  for(int i=0;i<states.size();i++){
   var pos=positions.get(i);var particle=mc.particleEngine.createParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER,states.get(i)),pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,0,0,0);
   check(particle instanceof BlockMarker,"real vanilla BlockMarker provider "+i);
   var sprite=(net.minecraft.client.renderer.texture.TextureAtlasSprite)field(particle.getClass(),"sprite").get(particle);
   String expected=i==0?"minecraft:item/barrier":i==4?"minecraft:item/structure_void":"minecraft:item/light_"+String.format(Locale.ROOT,"%02d",states.get(i).getValue(LightBlock.LEVEL));
   check(sprite.contents().name().toString().equals(expected),"actual baked particle sprite: "+expected);
   check(particle.getLifetime()==80,"vanilla marker lifetime is 80 ticks");
   for(int tick=0;tick<80;tick++)particle.tick();check(particle.isAlive(),"marker remains through its vanilla final age tick");particle.tick();check(!particle.isAlive(),"marker expires on the next vanilla tick");
  }
 }
 private void next(Minecraft mc,int stage){this.stage=stage;since=mc.level.getGameTime();System.out.println("INVISIBLE_STAGE "+stage);}
 private void held(Minecraft mc,Item item,GameType mode){ready=false;mc.player.getInventory().setSelectedSlot(0);mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.setGameMode(mode);p.getInventory().setItem(0,item==Items.AIR?ItemStack.EMPTY:new ItemStack(item));p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();ready=true;});}
 private boolean heldReady(Minecraft mc,Item item,GameType mode){return ready&&mc.gameMode.getPlayerMode()==mode&&(item==Items.AIR?mc.player.getMainHandItem().isEmpty():mc.player.getMainHandItem().is(item));}
 private void tweakerMore(boolean enabled)throws Exception{var option=Class.forName("me.fallenbreath.tweakermore.config.TweakerMoreConfigs").getField("BARRIER_PARTICLE_ALWAYS_VISIBLE").get(null);option.getClass().getMethod("setBooleanValue",boolean.class).invoke(option,enabled);}
 public static float observedSwing,observedEquip;public static boolean observe;
 @SuppressWarnings("unchecked") private List<Integer> pressedKeys()throws Exception{return (List<Integer>)field(KeybindMulti.class,"PRESSED_KEYS").get(null);}
 private void renderProgress(Minecraft mc,net.minecraft.world.InteractionHand hand)throws Exception{
  var type=net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer.class;var method=Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().equals("submitArmWithItem")).findFirst().orElseThrow();method.setAccessible(true);
  var items=new net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState();items.isScoping=true;items.useItemRemainingTicks=20;
  observe=true;try{method.invoke(new net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer(mc),new net.minecraft.client.renderer.state.level.PlayerRenderState(),items,0F,0F,hand,0.5F,mc.player.getItemInHand(hand),0.7F,new com.mojang.blaze3d.vertex.PoseStack(),null,0);}finally{observe=false;}
  check(items.isScoping&&items.useItemRemainingTicks==20,"use and scope render state preserved");
 }
 private void finishSwing(Minecraft mc)throws Exception{
  var state=field(net.minecraft.world.entity.LivingEntity.class,"swingState").get(mc.player);var tick=state.getClass().getDeclaredMethod("tick");tick.setAccessible(true);for(int i=0;i<40;i++)tick.invoke(state);
 }
 private void extras(Minecraft mc)throws Exception{
  var keys=pressedKeys();var saved=List.copyOf(keys);keys.clear();
  boolean active=mc.isWindowActive();var activeField=field(mc.getWindow().getClass(),"focused");activeField.setBoolean(mc.getWindow(),true);
  try{
   var passive=List.of(Configs.BOSS_BAR_HOLD_KEY,Configs.INVISIBLE_HOLD_KEY);
   Configs.BETTER_BOSS_BAR.setBooleanValue(true);Configs.BOSS_BAR_HOLD_ONLY.setBooleanValue(true);Configs.INVISIBLE_BLOCKS.setBooleanValue(true);Configs.INVISIBLE_HOLD_ONLY.setBooleanValue(true);
   for(var h:passive){h.getKeybind().setValueFromString("F8");check(((KeybindMulti)h.getKeybind()).getCallback()==null,"hold binding has no callback: "+h.getName());check(io.github.halfmasa.xaerobinding.feature.HoldToShow.isPassive(h),"hold binding explicitly passive");}
   var pos=positions.getFirst();var marker=(SingleQuadParticle)mc.particleEngine.createParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER,states.getFirst()),pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,0,0,0);
   var camera=MinecraftClientCompat.getMainCamera(mc);var quad=new net.minecraft.client.renderer.state.level.QuadParticleRenderState();marker.extract(quad,camera,0);
   check(quad.isEmpty(),"existing native marker hidden without held key");check(!io.github.halfmasa.xaerobinding.feature.HoldToShow.bossVisible(),"Boss hidden without held key");sample(mc,0,null,0);
   keys.addAll(Configs.INVISIBLE_HOLD_KEY.getKeybind().getKeys());marker.extract(quad,camera,0);check(!quad.isEmpty(),"same existing native marker appears while held");check(io.github.halfmasa.xaerobinding.feature.HoldToShow.bossVisible(),"shared hold key displays Boss simultaneously");sample(mc,0,null,1);
   keys.clear();quad.clear();marker.extract(quad,camera,0);check(quad.isEmpty()&&marker.isAlive(),"release immediately hides marker without destroying vanilla lifetime");sample(mc,0,null,0);
   keys.addAll(Configs.INVISIBLE_HOLD_KEY.getKeybind().getKeys());activeField.setBoolean(mc.getWindow(),false);check(!io.github.halfmasa.xaerobinding.feature.HoldToShow.invisibleVisible(),"focus loss hides held display");activeField.setBoolean(mc.getWindow(),true);
   MinecraftClientCompat.setScreen(mc,new TitleScreen());check(!io.github.halfmasa.xaerobinding.feature.HoldToShow.invisibleVisible(),"GUI cannot activate passive held display");MinecraftClientCompat.setScreen(mc,null);
   // Both hold keys overlap one regular MaLiLib action; only that action is a wheel candidate.
   var regular=Configs.INVISIBLE_BLOCKS.getKeybind();String original=regular.getStringValue();regular.setValueFromString("F8");fi.dy.masa.malilib.event.InputEventHandler.getKeybindManager().updateUsedKeys();keys.clear();keys.addAll(regular.getKeys());
   var manager=io.github.halfmasa.xaerobinding.feature.KeybindPieManager.getInstance();var inputSet=(Set<Integer>)field(manager.getClass(),"pressedInputKeys").get(manager);var inputSaved=Set.copyOf(inputSet);inputSet.clear();int code=regular.getKeys().getFirst();inputSet.add(code);var inputOrder=(List<Integer>)field(manager.getClass(),"pressedInputOrder").get(manager);var orderSaved=List.copyOf(inputOrder);inputOrder.clear();inputOrder.add(code);
   var wheel=manager.getClass().getDeclaredMethod("wheelCandidates",com.mojang.blaze3d.platform.InputConstants.Key.class,net.minecraft.client.gui.screens.Screen.class);wheel.setAccessible(true);
   var candidates=(List<io.github.halfmasa.xaerobinding.feature.KeybindPieManager.PieAction>)wheel.invoke(manager,io.github.halfmasa.xaerobinding.compat.InputCompat.keyboardKey(code),null);
   check(candidates.stream().noneMatch(a->a.hotkey()!=null&&io.github.halfmasa.xaerobinding.feature.HoldToShow.isPassive(a.hotkey())),"passive display keys excluded from actual conflict candidate collection");
   check(candidates.stream().anyMatch(a->a.hotkey()==Configs.INVISIBLE_BLOCKS),"shared ordinary toggle remains a conflict candidate, code="+code+", binding="+regular.getKeys()+", candidates="+candidates);var browser=new io.github.halfmasa.xaerobinding.gui.KeymapBrowserScreen();var rebuild=browser.getClass().getDeclaredMethod("rebuildEntries");rebuild.setAccessible(true);rebuild.invoke(browser);
   var browserEntries=(List<io.github.halfmasa.xaerobinding.gui.KeymapBrowserScreen.BrowserEntry>)field(browser.getClass(),"allEntries").get(browser);
   for(var entry:browserEntries)if(entry.hotkey()!=null&&io.github.halfmasa.xaerobinding.feature.HoldToShow.isPassive(entry.hotkey()))check(entry.getClass().getMethod("conflictType").invoke(entry).toString().equals("NONE"),"browser excludes passive key from conflict marking: "+entry.hotkey().getName());
   inputSet.clear();inputSet.addAll(inputSaved);inputOrder.clear();inputOrder.addAll(orderSaved);regular.setValueFromString(original);
   defaults();Configs.BETTER_BOSS_BAR.resetToDefault();Configs.BOSS_BAR_HOLD_ONLY.resetToDefault();Configs.BOSS_BAR_HOLD_KEY.resetToDefault();keys.clear();
   var toggles=List.of(Configs.REMOVE_ATTACK_SWING,Configs.REMOVE_INTERACT_SWING,Configs.REMOVE_ITEM_COOLDOWN_OVERLAY,Configs.REMOVE_EQUIP_ANIMATION,Configs.AUTO_EAT_RESTORE_SLOT,Configs.AUTO_EAT_SILENT,Configs.AUTO_EAT_PAUSE_COMBAT,Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH,Configs.BOSS_BAR_MERGE_SHOW_COUNT);
   for(var t:toggles){boolean before=t.getBooleanValue();var callback=((KeybindMulti)t.getKeybind()).getCallback();check(Configs.HOTKEYS.contains(t)&&callback!=null,"child toggle registered: "+t.getName());callback.onKeyAction(KeyAction.PRESS,t.getKeybind());check(t.getBooleanValue()!=before,"child callback switches independently: "+t.getName());t.resetToDefault();}
   Configs.REMOVE_COOLDOWN_ANIMATIONS.setBooleanValue(true);
   for(var animation:List.of(net.minecraft.world.item.component.SwingAnimation.DEFAULT,new net.minecraft.world.item.component.SwingAnimation(net.minecraft.world.item.SwingAnimationType.STAB,12))){
    for(var hand:net.minecraft.world.InteractionHand.values()){
     for(var origin:List.of(io.github.halfmasa.xaerobinding.feature.SwingVisualTracker.Source.ATTACK,io.github.halfmasa.xaerobinding.feature.SwingVisualTracker.Source.INTERACT)){
      finishSwing(mc);var previous=io.github.halfmasa.xaerobinding.feature.SwingVisualTracker.enter(origin);boolean started;try{started=mc.player.swing(hand,animation,true);}finally{io.github.halfmasa.xaerobinding.feature.SwingVisualTracker.restore(previous);}
      check(started&&mc.player.isSwinging(),"native real swing still starts: "+animation.type()+"/"+hand+"/"+origin);
      renderProgress(mc,hand);check(observedSwing==0&&observedEquip==.7F,"first-person swing hidden, equip preserved");
      var toggle=origin==io.github.halfmasa.xaerobinding.feature.SwingVisualTracker.Source.ATTACK?Configs.REMOVE_ATTACK_SWING:Configs.REMOVE_INTERACT_SWING;toggle.setBooleanValue(false);renderProgress(mc,hand);check(observedSwing==.5F,"source-specific child restores progress");toggle.setBooleanValue(true);
      renderProgress(mc,hand==net.minecraft.world.InteractionHand.MAIN_HAND?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND);check(observedSwing==.5F,"opposite hand unaffected");
     }
    }
   }
   Configs.REMOVE_EQUIP_ANIMATION.setBooleanValue(true);renderProgress(mc,net.minecraft.world.InteractionHand.MAIN_HAND);check(observedEquip==0,"equip visual offset removed");
   Configs.REMOVE_COOLDOWN_ANIMATIONS.setBooleanValue(false);renderProgress(mc,net.minecraft.world.InteractionHand.MAIN_HAND);check(observedSwing==.5F&&observedEquip==.7F,"master off restores both native visual parameters");
   // Actual Minecraft attack entry must record its source while retaining the native swing.
   finishSwing(mc);field(Minecraft.class,"missTime").setInt(mc,0);mc.hitResult=net.minecraft.world.phys.BlockHitResult.miss(mc.player.getEyePosition(),Direction.UP,mc.player.blockPosition());var attack=Minecraft.class.getDeclaredMethod("startAttack");attack.setAccessible(true);Configs.REMOVE_COOLDOWN_ANIMATIONS.setBooleanValue(true);attack.invoke(mc);check(mc.player.isSwinging(),"Minecraft attack still starts a native swing");renderProgress(mc,net.minecraft.world.InteractionHand.MAIN_HAND);check(observedSwing==0,"actual Minecraft attack source tracked");
   finishSwing(mc);check(!io.github.halfmasa.xaerobinding.feature.SwingVisualTracker.hide(net.minecraft.world.InteractionHand.MAIN_HAND),"finished swing expires tracking");
   var items=new net.minecraft.client.player.FirstPersonHandsAndItems();int slot=mc.player.getInventory().getSelectedSlot();var originalStack=mc.player.getInventory().getItem(slot).copy();
   for(var item:List.of(Items.DIAMOND_SWORD,Items.BOW,Items.SHIELD,Items.BREAD)){mc.player.getInventory().setItem(slot,new ItemStack(item));for(int i=0;i<30;i++)items.tick(mc.player);var state=new net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState();items.extractRenderState(mc.player,0,state);check(state.mainHandItem.is(item),"native switching state updates: "+item);}
   mc.player.getInventory().setItem(slot,originalStack);
   var overlay=net.minecraft.client.gui.GuiGraphicsExtractor.class.getDeclaredMethod("itemCooldown",ItemStack.class,int.class,int.class);overlay.setAccessible(true);var pearl=new ItemStack(Items.ENDER_PEARL);mc.player.getCooldowns().addCooldown(pearl,100);
   for(boolean remove:new boolean[]{false,true}){Configs.REMOVE_ITEM_COOLDOWN_OVERLAY.setBooleanValue(remove);var state=new net.minecraft.client.renderer.state.gui.GuiRenderState();var graphics=new net.minecraft.client.gui.GuiGraphicsExtractor(mc,state,0,0);overlay.invoke(graphics,pearl,0,0);var elements=new ArrayList<net.minecraft.client.renderer.state.gui.GuiElementRenderState>();state.forEachElement(elements::add,net.minecraft.client.renderer.state.gui.GuiRenderState.TraverseRange.ALL);check(remove?elements.isEmpty():!elements.isEmpty(),"actual cooldown overlay extraction: "+remove);check(mc.player.getCooldowns().isOnCooldown(pearl),"actual pearl cooldown preserved");}
   for(var t:List.of(Configs.REMOVE_COOLDOWN_ANIMATIONS,Configs.REMOVE_ATTACK_SWING,Configs.REMOVE_INTERACT_SWING,Configs.REMOVE_ITEM_COOLDOWN_OVERLAY,Configs.REMOVE_EQUIP_ANIMATION))t.resetToDefault();
  }finally{keys.clear();keys.addAll(saved);activeField.setBoolean(mc.getWindow(),active);}
 }
 private void tick(Minecraft mc){try{
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;stage=1;
   mc.createWorldOpenFlows().createFreshLevel("invisible-fixture",new LevelSettings("Invisible Marker Fixture",GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;}
  if(mc.player==null||mc.level==null)return;
  if(stage==1){if(mc.level.getGameTime()<50)return;MinecraftClientCompat.setScreen(mc,null);configs();
   states=List.of(Blocks.BARRIER.defaultBlockState(),Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,0),Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,7),Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,15),Blocks.STRUCTURE_VOID.defaultBlockState());
   for(int i=0;i<5;i++)positions.add(mc.player.blockPosition().offset(i-2,1,5));
   ready=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.getFoodData().setSaturation(20);for(int i=0;i<5;i++)p.level().setBlock(positions.get(i),states.get(i),3);ready=true;});next(mc,2);return;}
  if(mc.level.getGameTime()-since>300)throw new AssertionError("stage timeout "+stage);
  switch(stage){
   case 2->{if(!ready||java.util.stream.IntStream.range(0,5).anyMatch(i->!mc.level.getBlockState(positions.get(i)).equals(states.get(i))))return;
    check(mc.gameMode.getPlayerMode()==GameType.SURVIVAL&&target(mc)==null,"vanilla survival has no held-item marker target");
    for(int i=0;i<5;i++)sample(mc,i,target(mc),0);
    Configs.INVISIBLE_BLOCKS.setBooleanValue(true);for(int i=0;i<5;i++)sample(mc,i,target(mc),1);
    Configs.INVISIBLE_SHOW_BARRIER.setBooleanValue(false);Configs.INVISIBLE_SHOW_LIGHT.setBooleanValue(false);Configs.INVISIBLE_SHOW_STRUCTURE_VOID.setBooleanValue(false);for(int i=0;i<5;i++)sample(mc,i,target(mc),0);
    Configs.INVISIBLE_SHOW_BARRIER.setBooleanValue(true);sample(mc,0,null,1);sample(mc,1,null,0);sample(mc,4,null,0);
    Configs.INVISIBLE_SHOW_BARRIER.setBooleanValue(false);Configs.INVISIBLE_SHOW_LIGHT.setBooleanValue(true);sample(mc,0,null,0);for(int i=1;i<=3;i++)sample(mc,i,null,1);sample(mc,4,null,0);
    Configs.INVISIBLE_SHOW_LIGHT.setBooleanValue(false);Configs.INVISIBLE_SHOW_STRUCTURE_VOID.setBooleanValue(true);sample(mc,0,null,0);sample(mc,1,null,0);sample(mc,4,null,1);
    held(mc,Items.BARRIER,GameType.CREATIVE);next(mc,3);}
   case 3->{if(!heldReady(mc,Items.BARRIER,GameType.CREATIVE))return;check(target(mc)==Blocks.BARRIER,"actual creative main-hand barrier chooses native barrier target");
    Configs.INVISIBLE_SHOW_BARRIER.setBooleanValue(true);sample(mc,0,target(mc),1);sample(mc,4,target(mc),1);
    Configs.INVISIBLE_SHOW_BARRIER.setBooleanValue(false);sample(mc,0,target(mc),1);Configs.INVISIBLE_BLOCKS.setBooleanValue(false);sample(mc,0,target(mc),1);sample(mc,4,target(mc),0);
    held(mc,Items.LIGHT,GameType.CREATIVE);next(mc,4);}
   case 4->{if(!heldReady(mc,Items.LIGHT,GameType.CREATIVE))return;check(target(mc)==Blocks.LIGHT,"actual creative main-hand light chooses native light target");
    Configs.INVISIBLE_BLOCKS.setBooleanValue(true);Configs.INVISIBLE_SHOW_LIGHT.setBooleanValue(true);for(int i=1;i<=3;i++)sample(mc,i,target(mc),1);
    Configs.INVISIBLE_SHOW_LIGHT.setBooleanValue(false);sample(mc,1,target(mc),1);Configs.INVISIBLE_BLOCKS.setBooleanValue(false);sample(mc,3,target(mc),1);
    held(mc,Items.STRUCTURE_VOID,GameType.CREATIVE);next(mc,5);}
   case 5->{if(!heldReady(mc,Items.STRUCTURE_VOID,GameType.CREATIVE))return;check(target(mc)==null,"vanilla structure void item is not a held marker target");sample(mc,4,target(mc),0);Configs.INVISIBLE_BLOCKS.setBooleanValue(true);sample(mc,4,target(mc),1);
    held(mc,Items.BARRIER,GameType.SURVIVAL);next(mc,6);}
   case 6->{if(!heldReady(mc,Items.BARRIER,GameType.SURVIVAL))return;check(target(mc)==null,"survival held barrier still has no vanilla target");Configs.INVISIBLE_SHOW_BARRIER.setBooleanValue(true);sample(mc,0,target(mc),1);
    sprites(mc);if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("tweakermore")){tweakerMore(true);held(mc,Items.AIR,GameType.CREATIVE);next(mc,13);}else{held(mc,Items.AIR,GameType.SURVIVAL);next(mc,7);}}
   case 13->{if(!heldReady(mc,Items.AIR,GameType.CREATIVE))return;check(target(mc)==Blocks.BARRIER,"TweakerMore supplies native barrier target while empty-handed");
    defaults();Configs.INVISIBLE_BLOCKS.setBooleanValue(true);for(int i=0;i<5;i++)sample(mc,i,target(mc),1);
    Configs.INVISIBLE_BLOCKS.setBooleanValue(false);sample(mc,0,target(mc),1);sample(mc,1,target(mc),0);sample(mc,4,target(mc),0);
    tweakerMore(false);held(mc,Items.AIR,GameType.SURVIVAL);next(mc,7);}
   case 7->{if(!heldReady(mc,Items.AIR,GameType.SURVIVAL))return;defaults();extras(mc);ready=false;leverPos=mc.player.blockPosition().offset(0,0,2);
    mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var cow=net.minecraft.world.entity.EntityTypes.COW.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);cow.setPos(p.getX()+2,p.getY(),p.getZ());cow.setNoAi(true);cow.setHealth(20);p.level().addFreshEntity(cow);attackTarget=cow.getUUID();p.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));p.inventoryMenu.broadcastChanges();p.level().setBlock(leverPos.below(),Blocks.STONE.defaultBlockState(),3);p.level().setBlock(leverPos,Blocks.LEVER.defaultBlockState().setValue(net.minecraft.world.level.block.LeverBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR),3);ready=true;});next(mc,14);}
   case 14->{if(!ready)return;net.minecraft.world.entity.LivingEntity target=null;for(var e:mc.level.entitiesForRendering())if(e.getUUID().equals(attackTarget))target=(net.minecraft.world.entity.LivingEntity)e;if(target==null||!mc.player.getMainHandItem().is(Items.IRON_SWORD)||mc.player.getAttackStrengthScale(0)<.95F)return;
    Configs.REMOVE_COOLDOWN_ANIMATIONS.setBooleanValue(true);finishSwing(mc);mc.hitResult=new net.minecraft.world.phys.EntityHitResult(target);field(Minecraft.class,"missTime").setInt(mc,0);var attack=Minecraft.class.getDeclaredMethod("startAttack");attack.setAccessible(true);attack.invoke(mc);check(mc.player.isSwinging(),"actual entity attack retains native swing");renderProgress(mc,net.minecraft.world.InteractionHand.MAIN_HAND);check(observedSwing==0,"actual entity attack visually hidden");serverChecked=false;next(mc,15);}
   case 15->{if(mc.level.getGameTime()-since<8)return;if(!serverChecked){mc.getSingleplayerServer().execute(()->{var level=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().level();var e=(net.minecraft.world.entity.LivingEntity)level.getEntity(attackTarget);serverHealth=e.getHealth();serverChecked=true;});return;}check(serverHealth<20,"actual server-confirmed attack damage retained: "+serverHealth);
    if(!mc.level.getBlockState(leverPos).is(Blocks.LEVER))return;finishSwing(mc);mc.hitResult=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(leverPos),Direction.UP,leverPos,false);var use=Minecraft.class.getDeclaredMethod("startUseItem");use.setAccessible(true);use.invoke(mc);check(mc.player.isSwinging(),"actual lever interaction retains native swing");renderProgress(mc,net.minecraft.world.InteractionHand.MAIN_HAND);check(observedSwing==0,"actual interaction source visually hidden");next(mc,16);}
   case 16->{if(!mc.level.getBlockState(leverPos).getValue(net.minecraft.world.level.block.LeverBlock.POWERED))return;check(true,"server-confirmed lever interaction retained");Configs.REMOVE_COOLDOWN_ANIMATIONS.resetToDefault();Configs.REMOVE_EQUIP_ANIMATION.resetToDefault();Configs.INVISIBLE_BLOCKS.setBooleanValue(true);mc.player.setYRot(0);mc.player.setXRot(0);mc.particleEngine.clearParticles();for(int repeat=0;repeat<12;repeat++)for(int i=0;i<5;i++)sample(mc,i,null,1);next(mc,8);}
   case 8->{if(mc.level.getGameTime()-since<15)return;net.minecraft.client.Screenshot.grab(mc,false);Configs.INVISIBLE_BLOCKS.setBooleanValue(false);ready=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();for(var pos:positions)p.level().setBlock(pos,Blocks.AIR.defaultBlockState(),3);ready=true;});next(mc,9);}
   case 9->{if(!ready||positions.stream().anyMatch(p->!mc.level.getBlockState(p).isAir()))return;marks.clear();probe=true;try{for(var p:positions)mc.level.doAnimateTick(p.getX(),p.getY(),p.getZ(),1,RandomSource.create(123),null,new BlockPos.MutableBlockPos());}finally{probe=false;}check(marks.isEmpty(),"removed blocks emit no markers");next(mc,10);}
   case 10->{if(mc.level.getGameTime()-since<85)return;net.minecraft.client.Screenshot.grab(mc,false);Configs.INVISIBLE_BLOCKS_EXPANDED.setBooleanValue(true);ConfigScrollMemory.saveSelectedTab("ALL");GuiBase.openGui(new HalfMasaConfigScreen());
    var getList=GuiListBase.class.getDeclaredMethod("getListWidget");getList.setAccessible(true);var list=(WidgetListConfigOptions)getList.invoke(MinecraftClientCompat.getScreen(mc));var search=list.getSearchBarWidget();search.setSearchOpen(true);var box=WidgetSearchBar.class.getDeclaredField("searchBox");box.setAccessible(true);((GuiTextFieldGeneric)box.get(search)).setValue("invisible");list.refreshEntries();next(mc,11);}
   case 11->{if(mc.level.getGameTime()-since<10)return;net.minecraft.client.Screenshot.grab(mc,false);defaults();new Configs().save();System.out.println("INVISIBLE_WORLD_PASS checks="+checks);stage=12;mc.stop();}
  }
 }catch(Throwable failure){throw new IllegalStateException("Invisible block smoke failed stage="+stage,failure);}}
}
'''
FRAME = r'''package io.github.halfmasa.invisiblesmoke.mixin;
import io.github.halfmasa.invisiblesmoke.InvisibleSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class FrameMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void invisible$frame(CallbackInfo ci){InvisibleSmoke.frame((Minecraft)(Object)this);}
}'''
PROBE = r'''package io.github.halfmasa.invisiblesmoke.mixin;
import io.github.halfmasa.invisiblesmoke.InvisibleSmoke;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class) public class ProbeMixin {
 @Inject(method="addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",at=@At("HEAD")) private void invisible$particle(ParticleOptions option,double x,double y,double z,double vx,double vy,double vz,CallbackInfo ci){InvisibleSmoke.particle(option,x,y,z);}
}'''


RENDER_PROBE = r'''package io.github.halfmasa.invisiblesmoke.mixin;
import io.github.halfmasa.invisiblesmoke.InvisibleSmoke;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FirstPersonHandsAndItemsRenderer.class) public class RenderProbeMixin {
 @Inject(method="submitArmWithItem",at=@At("HEAD")) private void probe(PlayerRenderState p,FirstPersonHandsAndItemsRenderState items,float partial,float pitch,InteractionHand hand,float swing,ItemStack stack,float equip,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,int light,CallbackInfo ci){if(InvisibleSmoke.observe){InvisibleSmoke.observedSwing=swing;InvisibleSmoke.observedEquip=equip;}}
}
'''

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--jvm-arg", action="append", default=[])
    parser.add_argument("--extra-mod", action="append", type=Path, default=[])
    parser.add_argument("--with-api", action="store_true")
    parser.add_argument("--timeout", type=int, default=480)
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    args.version = "26.3"
    directory = ROOT / "build/invisible-block-tests" / datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    directory.mkdir(parents=True)
    classes = directory / "classes"
    classes.mkdir()
    sources = []
    for name, source in [("InvisibleSmoke", SOURCE), ("FrameMixin", FRAME), ("ProbeMixin", PROBE), ("RenderProbeMixin", RENDER_PROBE)]:
        path = directory / (name + ".java")
        path.write_text(source, encoding="utf-8")
        sources.append(str(path))
    cp = str(args.artifact) + os.pathsep + (ROOT / "build/port-classpath-26.3.txt").read_text().strip()
    subprocess.run([str(args.java_home / "bin/javac.exe"), "-proc:none", "-encoding", "UTF-8", "-cp", cp, "-d", str(classes), *sources], check=True)
    mod = directory / "invisible-smoke.jar"
    with zipfile.ZipFile(mod, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_invisible_smoke", "version": "1.0", "environment": "client", "entrypoints": {"client": ["io.github.halfmasa.invisiblesmoke.InvisibleSmoke"]}, "mixins": ["invisible-smoke.mixins.json"]}))
        jar.writestr("invisible-smoke.mixins.json", json.dumps({"required": True, "package": "io.github.halfmasa.invisiblesmoke.mixin", "compatibilityLevel": "JAVA_25", "client": ["FrameMixin", "ProbeMixin", "RenderProbeMixin"], "injectors": {"defaultRequire": 1}}))
    game = directory / "game"
    (game / "mods").mkdir(parents=True)
    (game / "config").mkdir()
    (game / "config/malilib.json").write_text("{}\n")
    (game / "options.txt").write_text("onboardAccessibility:false\nenableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:3\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:zh_cn\ntutorialStep:none\nparticles:0\n")
    if args.with_api:
        api = next((Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.161.0+26.3").glob("*/*.jar"))
        shutil.copy2(api, game / "mods" / api.name)
    for artifact in args.extra_mod:
        shutil.copy2(artifact, game / "mods" / artifact.name)
    command = startup.production_command(args, game, mod)
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
    passed = code == 0 and not timed_out and "INVISIBLE_WORLD_PASS" in log
    report = {"result": "PASS" if passed else "FAIL", "artifact": str(args.artifact), "fabric_api_installed": args.with_api, "extra_mods": [str(p.resolve()) for p in args.extra_mod], "jvm_args": args.jvm_arg, "duration_seconds": round(time.monotonic() - started, 1), "exit_code": code, "timed_out": timed_out, "log": str(log_path), "checks": [line for line in log.splitlines() if "INVISIBLE_" in line], "screenshots": [str(p) for p in (game / "screenshots").glob("*.png")]}
    (directory / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("INVISIBLE BLOCKS:", report["result"], flush=True)
    print(directory / "report.json", flush=True)
    if not passed:
        print("\n".join(log.splitlines()[-45:]), flush=True)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())

"""Exercise cage metadata and asset codecs against Minecraft 26.3 itself."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
HARNESS = r'''
import java.nio.file.*;
import java.io.*;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import io.github.halfmasa.xaerobinding.feature.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.renderer.blockentity.SpawnerRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
public class SpawnerAppearanceCheck {
 static class PoseProbe extends EntityRenderDispatcher {
  Matrix4f submitted;
  PoseProbe(){super(null,null,null,null,null,null,null,null,null,null,null);}
  @Override public <S extends EntityRenderState> void submit(S state,CameraRenderState camera,double x,double y,double z,PoseStack pose,SubmitNodeCollector collector){submitted=new Matrix4f(pose.last().pose());}
 }
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
 static CompoundTag mob(String id){var t=new CompoundTag();t.putString("id",id);return t;}
 static ItemStack stack(Item item,BlockEntityType<?> type,CompoundTag data){
  var s=new ItemStack(item);s.set(DataComponents.BLOCK_ENTITY_DATA,TypedEntityData.of(type,data));return s;
 }
 static CompoundTag spawn(CompoundTag mob){var t=new CompoundTag();t.put("entity",mob);return t;}
 static BlockEntityType<?> type(String id){return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));}
 static void modelMatchesBlockstate(ItemStack stack,String block,Map<String,String> properties)throws Exception{
  var info=SpawnerItemAppearance.read(stack,null,100);
  try(var reader=new InputStreamReader(SpawnerAppearanceCheck.class.getResourceAsStream("/assets/minecraft/blockstates/"+block+".json"))){
   var variants=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("variants");int matches=0;
   for(var entry:variants.entrySet()){
    boolean match=true;
    for(String property:entry.getKey().split(",")){
     var pair=property.split("=");if(properties.containsKey(pair[0])&&!properties.get(pair[0]).equals(pair[1]))match=false;
    }
    if(match){
     var expected=entry.getValue().getAsJsonObject().get("model").getAsString().replace("minecraft:block/","");
     check(info.model().getPath().equals("cages/"+expected),"model differs from vanilla block state: "+properties);matches++;
    }
   }
   check(matches>0,"oracle has no matching state");
  }
 }
 public static void main(String[] args)throws Exception{
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  var lookup=VanillaRegistries.createWorldLookup();
  for(var pending:BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup))pending.apply();
  var items=TrialCreativeItems.getItems();check(items.size()==44,"lost tab items");
  for(int i=0;i<items.size();i++){
   var stack=items.get(i);var before=stack.copy();var info=SpawnerItemAppearance.read(stack,lookup,100);
   var fallback=SpawnerItemAppearance.read(stack,null,100);
   check(info.mobs().equals(fallback.mobs()),"client fallback differs from registered config at "+i);
   check(info.ominous()==(i<42?i%3==1:i==43),"wrong ominous flag");
   if(i<42){check(!info.mobs().isEmpty(),"missing entity at "+i);check(info.cooling()==(i%3==2),"wrong cooldown");}
   var lines=new ArrayList<Component>();SpawnerItemAppearance.addTooltip(stack,lookup,100,lines);
   check(lines.size()==(i<42?4:2),"lost type/mob/state/cooldown tooltip");
   check(ItemStack.isSameItemSameComponents(before,stack),"reader mutated placement data");
  }
  check(SpawnerItemAppearance.read(items.get(12),lookup,0).mobs().getFirst().getStringOr("id","").equals("minecraft:bogged"),"poison skeleton must show bogged");
  var baby=SpawnerItemAppearance.read(items.get(30),lookup,0).mobs().getFirst();
  check(baby.getBooleanOr("IsBaby",false),"baby preview lost NBT");
  check(SpawnerItemAppearance.mobName(baby).getString().contains("halfmasa.spawner_item.baby"),"baby tooltip lost variant");
  for(boolean ominous:List.of(false,true)){
   for(String state:List.of("inactive","waiting_for_players","active","waiting_for_reward_ejection","ejecting_reward","cooldown")){
    var props=Map.of("ominous",Boolean.toString(ominous),"trial_spawner_state",state);
    var stack=new ItemStack(Items.TRIAL_SPAWNER);stack.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(props));
    modelMatchesBlockstate(stack,"trial_spawner",props);
   }
   for(String state:List.of("inactive","active","unlocking","ejecting")){
    var props=Map.of("ominous",Boolean.toString(ominous),"vault_state",state);
    var stack=new ItemStack(Items.VAULT);stack.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(props));
    modelMatchesBlockstate(stack,"vault",props);
   }
  }
  // Natural pick-block items carry real entity NBT, with no Halfmasa display name.
  var natural=new CompoundTag();natural.put("spawn_data",spawn(baby.copy()));natural.putString("normal_config","minecraft:trial_chamber/melee/husk/normal");
  natural.putLong("cooldown_ends_at",1000);
  var copied=stack(Items.TRIAL_SPAWNER,type("trial_spawner"),natural);
  var info=SpawnerItemAppearance.read(copied,lookup,100);
  check(info.mobs().equals(List.of(baby)),"selected natural entity must override config");
  check(info.cooling(),"natural cooldown not inferred");
  check(!SpawnerItemAppearance.read(copied,lookup,1001).cooling(),"expired inferred cooldown never ends");
  check(!copied.has(DataComponents.CUSTOM_NAME),"fixture accidentally relies on custom name");
  var propertiesMethod=SpawnerPickCapture.class.getDeclaredMethod("propertiesFor",BlockState.class);propertiesMethod.setAccessible(true);
  var restoreMethod=SpawnerPickCapture.class.getDeclaredMethod("restoreCopiedItem",ItemStack.class,Item.class,BlockItemStateProperties.class,CompoundTag.class);restoreMethod.setAccessible(true);
  var sourceState=Blocks.TRIAL_SPAWNER.defaultBlockState().setValue(TrialSpawnerBlock.OMINOUS,true).setValue(TrialSpawnerBlock.STATE,TrialSpawnerState.COOLDOWN);
  var captured=(BlockItemStateProperties)propertiesMethod.invoke(null,sourceState);
  check(captured.apply(Blocks.TRIAL_SPAWNER.defaultBlockState()).equals(sourceState),"source state capture differs from block");
  var original=copied.copy();var restored=(ItemStack)restoreMethod.invoke(null,copied,Items.TRIAL_SPAWNER,captured,baby);
  check(!restored.isEmpty()&&SpawnerItemAppearance.read(restored,lookup,100).ominous(),"natural copy lost ominous state");
  check(restored.get(DataComponents.BLOCK_ENTITY_DATA).equals(copied.get(DataComponents.BLOCK_ENTITY_DATA)),"copy changed server-owned entity/config data");
  check(ItemStack.isSameItemSameComponents(original,copied),"copy restoration mutated server response");
  check(((ItemStack)restoreMethod.invoke(null,copied,Items.VAULT,captured,baby)).isEmpty(),"wrong returned item accepted");
  check(((ItemStack)restoreMethod.invoke(null,new ItemStack(Items.TRIAL_SPAWNER),Items.TRIAL_SPAWNER,captured,baby)).isEmpty(),"plain pick accepted as data copy");
  check(((ItemStack)restoreMethod.invoke(null,copied,Items.TRIAL_SPAWNER,captured,mob("minecraft:husk"))).isEmpty(),"unrelated occupant accepted");
  var vaultState=Blocks.VAULT.defaultBlockState().setValue(VaultBlock.OMINOUS,true).setValue(VaultBlock.STATE,VaultState.UNLOCKING).setValue(VaultBlock.FACING,Direction.WEST);
  var vaultProps=(BlockItemStateProperties)propertiesMethod.invoke(null,vaultState);
  check(vaultProps.apply(Blocks.VAULT.defaultBlockState()).equals(vaultState),"vault state/facing lost in copy");
  var vanilla=new CompoundTag();vanilla.put("SpawnData",spawn(mob("minecraft:cave_spider")));
  check(SpawnerItemAppearance.read(stack(Items.SPAWNER,type("mob_spawner"),vanilla),lookup,0).mobs().getFirst().getStringOr("id","").equals("minecraft:cave_spider"),"ordinary natural cage lost mob");
  var potentials=new ListTag();var potential=new CompoundTag();potential.put("data",spawn(mob("minecraft:slime")));potential.putInt("weight",1);potentials.add(potential);potentials.add(potential.copy());
  var inline=new CompoundTag();inline.put("spawn_potentials",potentials);var custom=new CompoundTag();custom.put("normal_config",inline);
  check(SpawnerItemAppearance.read(stack(Items.TRIAL_SPAWNER,type("trial_spawner"),custom),lookup,0).mobs().size()==1,"inline config duplicates or lost mobs");
  var invalid=new CompoundTag();invalid.put("spawn_data",spawn(mob("bad:id")));invalid.putString("normal_config","invalid id");
  var broken=stack(Items.TRIAL_SPAWNER,type("trial_spawner"),invalid);
  broken.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(Map.of("trial_spawner_state","invalid")));
  check(SpawnerItemAppearance.read(broken,null,0).mobs().isEmpty(),"invalid mob leaked into renderer");
  check(SpawnerItemAppearance.read(broken,null,0).state().equals("inactive"),"invalid state not bounded");
  check(SpawnerItemAppearance.read(new ItemStack(Items.STICK),lookup,0)==null,"unrelated item intercepted");
  var slimeType=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("slime"));
  var zombieType=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("zombie"));
  check(SpawnerItemPresentation.guiScale(slimeType,0.4F)==0.4F,"slime unexpectedly enlarged");
  check(Math.abs(SpawnerItemPresentation.guiScale(zombieType,0.4F)-0.48F)<0.00001F,"other mob not enlarged 20 percent");
  for(var gui:List.of(new ItemTransform(new Vector3f(30,225,0),new Vector3f(),new Vector3f(0.625F)),ItemTransform.NO_TRANSFORM)){
  for(var local:List.of(new Matrix4f(),new Matrix4f().translate(0.1F,0.05F,-0.1F).scale(0.8F))){
  var cagePose=new PoseStack();gui.apply(false,cagePose.last());cagePose.mulPose(local);
  float cageFront=Float.NEGATIVE_INFINITY;
  for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)cageFront=Math.max(cageFront,cagePose.last().pose().transformPosition(new Vector3f(x,y,z)).z);
  for(var dimensions:List.of(new float[]{0.3F,0.3F},new float[]{0.6F,1.95F},new float[]{1.4F,0.9F},new float[]{4,4})){
   float w=dimensions[0],h=dimensions[1],scale=0.53125F/Math.max(1,Math.max(w,h));
   var fg=SpawnerItemPresentation.foregroundTransform(gui,local,w,h,scale,cageFront);
   // LayerRenderState calls ItemTransform.apply before multiplying localTransform.
   // Even NO_TRANSFORM applies native block centering; do not add another centering offset.
   var foreground=new PoseStack();ItemTransform.NO_TRANSFORM.apply(false,foreground.last());foreground.mulPose(fg);
   var probe=new PoseProbe();probe.submit(new EntityRenderState(),null,0,0,0,foreground,null);
   var cageCenter=cagePose.last().pose().transformPosition(new Vector3f(0.5F));
   var mobCenter=probe.submitted.transformPosition(new Vector3f(0,h/2,0));
   check(Math.abs(mobCenter.x-cageCenter.x)<0.00001F&&Math.abs(mobCenter.y-cageCenter.y)<0.00001F,"standing model moved outside icon center");
   var vertical=probe.submitted.transformDirection(new Vector3f(0,1,0));
   check(Math.abs(vertical.x)<0.00001F&&Math.abs(vertical.z)<0.00001F&&vertical.y>0,"GUI model still inherits spawner tilt");
   for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++){
    var point=new Vector3f((x-0.5F)*w,y*h,(z-0.5F)*w);
    var actual=probe.submitted.transformPosition(new Vector3f(point));
    check(actual.z>cageFront,"native entity geometry still behind cage: "+Arrays.toString(dimensions));
   }
  }
  }}
  ItemModels.bootstrap();int assets=0;
  try(var paths=Files.list(Path.of(args[0],"src/main/resources/assets/halfmasa/items/cages"))){
   for(var path:paths.toList()){
    var json=JsonParser.parseString(Files.readString(path));ClientItem.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
    var model=json.getAsJsonObject().getAsJsonObject("model").get("model").getAsString().replace("minecraft:","/assets/minecraft/models/")+".json";
    check(SpawnerAppearanceCheck.class.getResource(model)!=null,"missing vanilla block model: "+model);assets++;
   }
  }
  check(assets==15,"missing state assets");
  System.out.println("SPAWNER_APPEARANCE_PASS variants=44 states=20 models=15 natural=true boundaries=true");
 }
}
'''

class SpawnerItemAppearanceTests(unittest.TestCase):
 def test_metadata_natural_copies_state_oracle_and_real_asset_codecs(self):
  java = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-25.0.3"))
  classpath = os.environ.get("HALFMASA_MC_CLASSPATH") or (ROOT / "build/review/mc-test-classpath.txt").read_text(encoding="utf-8")
  with tempfile.TemporaryDirectory(prefix="halfmasa-spawner-appearance-") as folder:
   work = Path(folder)
   harness = work / "SpawnerAppearanceCheck.java"
   harness.write_text(HARNESS, encoding="utf-8")
   stub = work / "XaeroWorldBinding.java"
   stub.write_text('package io.github.halfmasa.xaerobinding; public class XaeroWorldBinding { public static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("halfmasa-test"); }', encoding="utf-8")
   # Only the external tick service and render-cache hook are isolated; state/NBT APIs are real Minecraft.
   tick = work / "IClientTickHandler.java"
   tick.write_text('package fi.dy.masa.malilib.interfaces; public interface IClientTickHandler { void onClientTick(net.minecraft.client.Minecraft client); }', encoding="utf-8")
   model = work / "SpawnerItemModel.java"
   model.write_text('package io.github.halfmasa.xaerobinding.feature; public class SpawnerItemModel { public static void clearIfWorldChanged(net.minecraft.client.multiplayer.ClientLevel level) {} }', encoding="utf-8")
   sources = [ROOT / "src/main/java/io/github/halfmasa/xaerobinding/feature" / name for name in ("TrialCreativeItems.java", "SpawnerItemAppearance.java", "SpawnerPickCapture.java", "SpawnerItemPresentation.java")]
   result = subprocess.run([str(java / "bin/javac.exe"), "-encoding", "UTF-8", "-cp", classpath, "-d", folder, *map(str, sources), str(stub), str(tick), str(model), str(harness)], capture_output=True)
   self.assertEqual(result.returncode, 0, result.stderr.decode(errors="replace"))
   result = subprocess.run([str(java / "bin/java.exe"), "-Dfile.encoding=UTF-8", "-cp", folder + os.pathsep + classpath, "SpawnerAppearanceCheck", str(ROOT)], cwd=folder, capture_output=True, timeout=60)
   self.assertEqual(result.returncode, 0, (result.stdout + result.stderr).decode(errors="replace"))
   self.assertIn(b"SPAWNER_APPEARANCE_PASS", result.stdout)

if __name__ == "__main__":
 unittest.main()

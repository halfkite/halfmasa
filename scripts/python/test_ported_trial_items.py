"""Validate preprocessed cage items against each supported Minecraft's native codecs.

Run after portTestClasspath (build/port-validation.init.gradle) and preprocessing.
Uses real Minecraft classes and vanilla structure resources; no model/codec stubs.
"""
import os
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
VERSIONS = ["26.3", "26.2", "26.1.2", "1.21.11", "1.21.10", "1.21.8", "1.21.5", "1.21.4", "1.21.3", "1.21.1"]
HARNESS = r'''
import io.github.halfmasa.xaerobinding.feature.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import com.google.gson.JsonParser;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerConfig;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import com.mojang.serialization.Codec;
import java.util.*;
public class PortedTrialCheck {
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static CompoundTag tag(Object data)throws Exception {
  try{return (CompoundTag)data.getClass().getMethod("copyTagWithoutId").invoke(data);}
  catch(NoSuchMethodException e){return (CompoundTag)data.getClass().getMethod("copyTag").invoke(data);}
 }
 static TrialSpawnerConfig decode(Object data,net.minecraft.core.HolderLookup.Provider lookup)throws Exception {
  Codec codec=(Codec)TrialSpawnerConfig.class.getField("CODEC").get(null);
  Object value=codec.parse(lookup.createSerializationContext(NbtOps.INSTANCE),data).getOrThrow();
  return value instanceof net.minecraft.core.Holder<?> holder ? (TrialSpawnerConfig)holder.value() : (TrialSpawnerConfig)value;
 }
 static void modelMatches(ItemStack stack,String block,Map<String,String> properties)throws Exception {
  var info=SpawnerItemAppearance.read(stack,null,0);
  try(var stream=PortedTrialCheck.class.getResourceAsStream("/assets/minecraft/blockstates/"+block+".json")) {
   check(stream!=null,"vanilla blockstate missing");
   var variants=JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject().getAsJsonObject("variants");int matches=0;
   for(var entry:variants.entrySet()) {
    boolean match=true;
    for(String property:entry.getKey().split(",")) {
     var pair=property.split("=");if(properties.containsKey(pair[0])&&!properties.get(pair[0]).equals(pair[1]))match=false;
    }
    if(match) {
     String expected=entry.getValue().getAsJsonObject().get("model").getAsString().replace("minecraft:block/","").replace("block/","");
     check(info.model().getPath().equals("cages/"+expected),"wrong vanilla cage material for "+properties);matches++;
    }
   }
   check(matches>0,"no matching vanilla material");
  }
 }
 public static void main(String[] args)throws Exception {
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  HolderLookup.Provider lookup;
  try {lookup=(HolderLookup.Provider)VanillaRegistries.class.getMethod("createWorldLookup").invoke(null);}
  catch(NoSuchMethodException e){lookup=(HolderLookup.Provider)VanillaRegistries.class.getMethod("createLookup").invoke(null);}
  try {
   Object initializers=BuiltInRegistries.class.getField("DATA_COMPONENT_INITIALIZERS").get(null);
   var pending=(Iterable<?>)initializers.getClass().getMethod("build",HolderLookup.Provider.class).invoke(initializers,lookup);
   for(Object entry:pending){var apply=entry.getClass().getMethod("apply");apply.setAccessible(true);apply.invoke(entry);}
  }catch(NoSuchFieldException e){}
  var items=TrialCreativeItems.getItems();check(items.size()==44,"lost variants");
  for(int i=0;i<42;i++) {
   var stack=items.get(i);var data=tag(stack.get(DataComponents.BLOCK_ENTITY_DATA));var before=data.copy();
   for(String key:List.of("normal_config","ominous_config")) {
    var config=decode(data.get(key),lookup);
    check(!config.spawnPotentialsDefinition().isEmpty(),"empty "+key+" at "+i);
    check(config.totalMobs()>0&&config.simultaneousMobs()>0,"spawner cannot spawn at "+i);
   }
   check(!data.contains("spawn_data"),"invalid selected spawn data");
   var state=stack.get(DataComponents.BLOCK_STATE).apply(Blocks.TRIAL_SPAWNER.defaultBlockState());
   check(state.getValue(TrialSpawnerBlock.OMINOUS)==(i%3==1),"wrong ominous state "+i);
   check(state.getValue(TrialSpawnerBlock.STATE)==(i%3==2?TrialSpawnerState.COOLDOWN:TrialSpawnerState.WAITING_FOR_PLAYERS),"wrong waiting/cooldown state "+i);
   check(CageNbt.longValue(data,"cooldown_ends_at",0)==(i%3==2?72000:0),"bad cooldown time");
   var info=SpawnerItemAppearance.read(stack,lookup,0);
   check(!info.mobs().isEmpty(),"lost standing occupant "+i);
   check(info.mobs().equals(SpawnerItemAppearance.read(stack,null,0).mobs()),"unsynced config preview differs "+i);
   check(info.ominous()==(i%3==1)&&info.cooling()==(i%3==2),"wrong tooltip state "+i);
   check(before.equals(tag(stack.get(DataComponents.BLOCK_ENTITY_DATA))),"preview mutated placement data");
  }
  var vault=items.get(43);var data=tag(vault.get(DataComponents.BLOCK_ENTITY_DATA));
  var field=VaultConfig.class.getDeclaredField("CODEC");field.setAccessible(true);
  var config=(VaultConfig)((Codec)field.get(null)).parse(lookup.createSerializationContext(NbtOps.INSTANCE),CageNbt.compound(data,"config")).getOrThrow();
  check(config.keyItem().is(Items.OMINOUS_TRIAL_KEY),"ominous vault uses normal key");
  check(vault.get(DataComponents.BLOCK_STATE).apply(Blocks.VAULT.defaultBlockState()).getValue(VaultBlock.OMINOUS),"lost vault state");
  for(boolean ominous:List.of(false,true)) {
   for(String state:List.of("inactive","waiting_for_players","active","waiting_for_reward_ejection","ejecting_reward","cooldown")) {
    var properties=Map.of("ominous",Boolean.toString(ominous),"trial_spawner_state",state);
    var stack=new ItemStack(Items.TRIAL_SPAWNER);stack.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(properties));
    modelMatches(stack,"trial_spawner",properties);
   }
   for(String state:List.of("inactive","active","unlocking","ejecting")) {
    var properties=Map.of("ominous",Boolean.toString(ominous),"vault_state",state,"facing","north");
    var stack=new ItemStack(Items.VAULT);stack.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(properties));
    modelMatches(stack,"vault",properties);
   }
  }
  String defaults=args[0].equals("26.3")?"-3,-2,-1,4,7,22,26,224,225,226,228,229,230":"-3,-2,-1,65,68,83,87,340,341,342,344,345,346";
  check(IgnoredKeySelection.DEFAULT_KEYS.equals(defaults),"wrong native default key codes");
  check(IgnoredKeySelection.migrateDefaults("87,65,83,68,340").equals(defaults),"default migration failed");
  check(IgnoredKeySelection.migrateDefaults("70,71").equals("70,71"),"custom ignored keys overwritten");
  TrialCreativeItems.invalidate();check(TrialCreativeItems.getItems().size()==44,"cache reset loses variants");
  System.out.println("PORT_NATIVE_PASS version="+args[0]+" spawners=42 vaults=2 native_material_states=20 key_defaults=true");
 }
}
'''

def run(version):
    cp_file=ROOT/f"build/port-classpath-{version}.txt"
    if not cp_file.exists(): raise RuntimeError(f"Missing {cp_file}; generate portTestClasspath first")
    cp=cp_file.read_text(encoding="utf-8")
    base=ROOT/"src/main/java" if version=="26.3" else ROOT/f"versions/{version}/build/preprocessed/main/java"
    package=base/"io/github/halfmasa/xaerobinding/feature"
    classes=ROOT/f"versions/{version}/build/classes/java/main"
    cp=str(classes)+os.pathsep+cp
    java=Path(os.environ.get("JAVA_HOME","C:/Program Files/Java/jdk-25.0.3"))/"bin"
    with tempfile.TemporaryDirectory(prefix="halfmasa-port-codec-") as tmp:
        work=Path(tmp);harness=work/"PortedTrialCheck.java";harness.write_text(HARNESS,encoding="utf-8")
        sources=[package/(name+".java") for name in ["TrialCreativeItems","CageNbt","SpawnerItemAppearance","IgnoredKeySelection"]]
        # Compile the actual preprocessed feature code, isolated from unrelated GUI classes.
        result=subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",cp,"-d",tmp,*map(str,sources),str(harness)],capture_output=True)
        if result.returncode: raise RuntimeError(result.stderr.decode(errors="replace"))
        result=subprocess.run([str(java/"java.exe"),"-Dfile.encoding=UTF-8","-cp",tmp+os.pathsep+cp,"PortedTrialCheck",version],cwd=tmp,capture_output=True,timeout=90)
        if result.returncode:raise RuntimeError((result.stdout+result.stderr).decode(errors="replace"))
        print(next(line for line in result.stdout.decode(errors="replace").splitlines() if "PORT_NATIVE_PASS" in line),flush=True)

if __name__=="__main__":
    for version in sys.argv[1:] or VERSIONS:run(version)

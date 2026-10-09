"""Decode the production trial items using Minecraft 26.3's own registries and codecs."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
HARNESS = r'''
import io.github.halfmasa.xaerobinding.feature.TrialCreativeItems;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
public class TrialItemCheck {
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 public static void main(String[] args) {
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  var lookup=VanillaRegistries.createWorldLookup();
  for(var pending:BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup))pending.apply();
  var ops=lookup.createSerializationContext(NbtOps.INSTANCE);
  var items=TrialCreativeItems.getItems();check(items.size()==44,"trial tab lost variants");
  for(int i=0;i<42;i++){
   var item=items.get(i);var tag=item.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId();
   var config=TrialSpawner.FullConfig.MAP_CODEC.codec().parse(ops,tag).getOrThrow();
   check(!config.normal().value().spawnPotentialsDefinition().isEmpty(),"normal config is empty at "+i);
   check(!config.ominous().value().spawnPotentialsDefinition().isEmpty(),"ominous config is empty at "+i);
   check(tag.getStringOr("normal_config","").endsWith("/normal")&&tag.getStringOr("ominous_config","").endsWith("/ominous"),"wrong config reference");
   check(!tag.contains("spawn_data"),"invalid spawn data overrides the registered vanilla config");
   var state=item.get(DataComponents.BLOCK_STATE).apply(Blocks.TRIAL_SPAWNER.defaultBlockState());
   check(state.getValue(TrialSpawnerBlock.OMINOUS)==(i%3==1),"ominous variant only changes its display name");
   check(state.getValue(TrialSpawnerBlock.STATE)==(i%3==2?TrialSpawnerState.COOLDOWN:TrialSpawnerState.WAITING_FOR_PLAYERS),"cooldown variant ignores block state");
   var packed=TrialSpawnerStateData.Packed.MAP_CODEC.codec().parse(ops,tag).getOrThrow();
   check(packed.cooldownEndsAt()==(i%3==2?72000:0),"cooldown timestamp failed to decode");
  }
  var vault=items.get(43);var tag=vault.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId();
  var config=VaultConfig.CODEC.parse(ops,tag.getCompoundOrEmpty("config")).getOrThrow();
  check(config.keyItem().is(Items.OMINOUS_TRIAL_KEY),"ominous vault accepts the normal key");
  check(config.lootTable().identifier().toString().equals("minecraft:chests/trial_chambers/reward_ominous"),"ominous vault has normal loot");
  check(vault.get(DataComponents.BLOCK_STATE).apply(Blocks.VAULT.defaultBlockState()).getValue(VaultBlock.OMINOUS),"ominous vault state not applied");
  check(!items.get(42).get(DataComponents.BLOCK_STATE).apply(Blocks.VAULT.defaultBlockState()).getValue(VaultBlock.OMINOUS),"normal vault became ominous");
  TrialCreativeItems.invalidate();check(TrialCreativeItems.getItems().size()==44,"item cache reset loses content");
  System.out.println("TRIAL_ITEM_CODEC_PASS spawners=42 vaults=2");
 }
}
'''

class TrialCreativeItemTests(unittest.TestCase):
 def test_all_variants_decode_with_real_minecraft_config_and_state_codecs(self):
  home=Path(os.environ.get("JAVA_HOME","C:/Program Files/Java/jdk-25.0.3"))
  classpath=os.environ.get("HALFMASA_MC_CLASSPATH")
  if not classpath:
   cache=ROOT/"build/review/mc-test-classpath.txt"
   if not cache.exists():self.skipTest("Set HALFMASA_MC_CLASSPATH to Minecraft 26.3 and its libraries")
   classpath=cache.read_text(encoding="utf-8")
  with tempfile.TemporaryDirectory(prefix="halfmasa-trial-codec-") as folder:
   work=Path(folder);harness=work/"TrialItemCheck.java";harness.write_text(HARNESS,encoding="utf-8")
   source=ROOT/"src/main/java/io/github/halfmasa/xaerobinding/feature/TrialCreativeItems.java"
   result=subprocess.run([str(home/"bin/javac.exe"),"-encoding","UTF-8","-cp",classpath,"-d",folder,str(source),str(harness)],capture_output=True)
   self.assertEqual(result.returncode,0,result.stderr.decode(errors="replace"))
   result=subprocess.run([str(home/"bin/java.exe"),"-Dfile.encoding=UTF-8","-cp",folder+os.pathsep+classpath,"TrialItemCheck"],cwd=folder,capture_output=True,timeout=60)
   self.assertEqual(result.returncode,0,(result.stdout+result.stderr).decode(errors="replace"))
   self.assertIn(b"TRIAL_ITEM_CODEC_PASS",result.stdout)

if __name__=="__main__":unittest.main()

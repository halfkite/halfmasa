"""Exercise the production age model and tick service with controlled world/network boundaries."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
PACKAGE = "io/github/halfmasa/xaerobinding"
STUBS = {
    f"{PACKAGE}/config/Configs.java": '''package io.github.halfmasa.xaerobinding.config;
public class Configs {
 public static class Value {public boolean flag;public int number;public Object option;public boolean getBooleanValue(){return flag;}public int getIntegerValue(){return number;}public Object getOptionListValue(){return option;}}
 public static final Value ITEM_DESPAWN_TIMER=new Value(),ITEM_DESPAWN_SOURCE=new Value(),ITEM_DESPAWN_FORMAT=new Value(),ITEM_DESPAWN_DISTANCE=new Value(),ITEM_DESPAWN_MAX_LABELS=new Value(),ITEM_DESPAWN_SYNC_INTERVAL=new Value();
 static {ITEM_DESPAWN_TIMER.flag=true;ITEM_DESPAWN_SOURCE.option=ItemDespawnDataSource.AUTO;ITEM_DESPAWN_FORMAT.option=ItemDespawnTimeFormat.MINUTES;ITEM_DESPAWN_DISTANCE.number=64;ITEM_DESPAWN_MAX_LABELS.number=64;ITEM_DESPAWN_SYNC_INTERVAL.number=20;}
}''',
    f"{PACKAGE}/config/ItemDespawnDataSource.java": "package io.github.halfmasa.xaerobinding.config;public enum ItemDespawnDataSource {AUTO,SERVUX,ESTIMATE}",
    f"{PACKAGE}/config/ItemDespawnTimeFormat.java": 'package io.github.halfmasa.xaerobinding.config;public enum ItemDespawnTimeFormat {MINUTES;public String getStringValue(){return "minutes";}}',
    "fi/dy/masa/malilib/interfaces/IClientTickHandler.java": "package fi.dy.masa.malilib.interfaces;public interface IClientTickHandler {void onClientTick(net.minecraft.client.Minecraft client);}",
    "net/minecraft/network/chat/Component.java": "package net.minecraft.network.chat;public record Component(String text){public static Component literal(String text){return new Component(text);}public String getString(){return text;}}",
    "net/minecraft/world/entity/Entity.java": "package net.minecraft.world.entity;public class Entity {public int id;public double x;public java.util.UUID uuid=java.util.UUID.randomUUID();public Entity(int id){this.id=id;}public int getId(){return id;}public java.util.UUID getUUID(){return uuid;}public double distanceToSqr(Entity e){return (x-e.x)*(x-e.x);}}",
    "net/minecraft/world/entity/item/ItemEntity.java": "package net.minecraft.world.entity.item;public class ItemEntity extends net.minecraft.world.entity.Entity {public boolean removed;public ItemEntity(int id){super(id);}public boolean isRemoved(){return removed;}public Stack getItem(){return new Stack();}public static class Stack {public boolean isEmpty(){return false;}}}",
    "net/minecraft/client/multiplayer/ClientLevel.java": "package net.minecraft.client.multiplayer;import java.util.*;import net.minecraft.world.entity.Entity;public class ClientLevel {public final Map<Integer,Entity> entities=new LinkedHashMap<>();public Iterable<Entity> entitiesForRendering(){return entities.values();}public Entity getEntity(int id){return entities.get(id);}}",
    "net/minecraft/client/Minecraft.java": "package net.minecraft.client;import net.minecraft.client.multiplayer.ClientLevel;import net.minecraft.world.entity.Entity;public class Minecraft {private static final Minecraft INSTANCE=new Minecraft();public ClientLevel level;public boolean paused;public Entity camera=new Entity(-1);public static Minecraft getInstance(){return INSTANCE;}public Entity getCameraEntity(){return camera;}public boolean isPaused(){return paused;}}",
    f"{PACKAGE}/feature/EntityRenderAggregation.java": "package io.github.halfmasa.xaerobinding.feature;import java.util.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.item.ItemEntity;public class EntityRenderAggregation {static final EntityRenderAggregation INSTANCE=new EntityRenderAggregation();public final Map<Integer,List<Integer>> groups=new HashMap<>();public static EntityRenderAggregation getInstance(){return INSTANCE;}public boolean shouldHide(Entity entity){return groups.values().stream().anyMatch(g->g.contains(entity.getId())&&g.getFirst()!=entity.getId());}public List<Integer> getItemMemberIds(ItemEntity e){return groups.getOrDefault(e.getId(),List.of(e.getId()));}}",
    f"{PACKAGE}/feature/ItemDespawnDataBridge.java": "package io.github.halfmasa.xaerobinding.feature;import java.util.*;import net.minecraft.client.Minecraft;import net.minecraft.world.entity.item.ItemEntity;import io.github.halfmasa.xaerobinding.config.ItemDespawnDataSource;public class ItemDespawnDataBridge {public record CachedAge(int age,long elapsedTicks,Object token){}public record Request(long generation,int id,UUID uuid){}public static final List<Request> requests=new ArrayList<>();public static boolean available=true;public static void clear(){}public static void tick(Minecraft m,long tick){}public static CachedAge pollMiniHud(ItemEntity i){return null;}public static boolean request(Minecraft m,ItemEntity i,ItemDespawnDataSource s,long g){if(!available)return false;requests.add(new Request(g,i.getId(),i.getUUID()));return true;}}",
}
HARNESS = r'''
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.item.ItemEntity;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.feature.ItemDespawnAgeModel.*;
public class DespawnCheck {
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
 static ItemEntity add(Minecraft mc,int id,double distance){var item=new ItemEntity(id);item.x=distance;mc.level.entities.put(id,item);return item;}
 static void reply(ItemDespawnDataBridge.Request r,int age){ItemDespawnTimerService.getInstance().accept(r.generation(),r.id(),r.uuid(),age);}
 static void ticks(int count){for(int i=0;i<count;i++)ItemDespawnTimerService.getInstance().onClientTick(Minecraft.getInstance());}
 public static void main(String[] args){
  var mc=Minecraft.getInstance();mc.level=new ClientLevel();var service=ItemDespawnTimerService.getInstance();
  switch(args[0]){
   case "age" -> {
    var age=new ItemDespawnAgeModel(100);
    check(age.read(100,60,false,false).quality()==Quality.ESTIMATED,"initial observation is not exact");
    check(age.read(100,60,false,true).quality()==Quality.UNKNOWN,"strict mode without data");
    age.accept(5900,100);
    check(age.read(110,60,false,false).remaining()==90,"sample progression");
    check(age.read(170,60,false,false).remaining()==30,"fallback reset the timer");
    check(age.read(170,60,false,false).quality()==Quality.ESTIMATED,"stale sample is exact");
    check(age.read(170,60,false,true).quality()==Quality.UNKNOWN,"strict stale data");
    check(age.read(1000,60,false,false).remaining()==0,"negative countdown");
    age.accept(-6000,100);check(age.read(100,60,false,false).remaining()==12000,"extended negative age");
    age.accept(-32768,100);check(age.read(110,60,false,false).quality()==Quality.INFINITE,"never despawn");
    check(age.read(170,60,false,false).quality()==Quality.UNKNOWN,"stale immortality");
    check(age.read(110,60,true,false).quality()==Quality.ESTIMATED,"estimate mode consumed server data");
    age.accept(20,100);check(age.read(100,600,false,false).quality()==Quality.SAMPLED,"long configured interval");
   }
   case "combine" -> {
    var unknown=new Reading(0,Quality.UNKNOWN);var infinite=new Reading(0,Quality.INFINITE);var finite=new Reading(19,Quality.SAMPLED);
    check(ItemDespawnAgeModel.combine(List.of(),false).quality()==Quality.UNKNOWN,"empty group immortal");
    check(ItemDespawnAgeModel.combine(List.of(unknown,infinite),false).quality()==Quality.UNKNOWN,"missing member immortal");
    check(ItemDespawnAgeModel.combine(List.of(infinite,infinite),false).quality()==Quality.INFINITE,"persistent group");
    check(ItemDespawnAgeModel.combine(List.of(finite,new Reading(50,Quality.SAMPLED)),false).remaining()==19,"group minimum");
    check(ItemDespawnAgeModel.combine(List.of(finite,unknown),false).quality()==Quality.ESTIMATED,"missing member exact");
    check(ItemDespawnAgeModel.combine(List.of(finite,unknown),true).quality()==Quality.UNKNOWN,"partial strict group");
    check(ItemDespawnAgeModel.format(finite,"minutes").equals("00:01"),"round seconds up");
    check(ItemDespawnAgeModel.format(new Reading(6000,Quality.ESTIMATED),"seconds").equals("≈300s"),"estimate seconds");
    check(ItemDespawnAgeModel.format(finite,"ticks").equals("19t"),"ticks");
   }
   case "independence" -> {
    Configs.ITEM_DESPAWN_SOURCE.option=ItemDespawnDataSource.ESTIMATE;var item=add(mc,1,1);ticks(1);
    check(service.labelFor(item).getString().equals("≈05:00"),"independent label without aggregation");
    ticks(20);String before=service.labelFor(item).getString();mc.paused=true;ticks(60);
    check(service.labelFor(item).getString().equals(before),"paused timer advances");mc.paused=false;
    item.x=100;ticks(30);check(service.labelFor(item)==null,"range filtering");item.x=1;ticks(1);
    check(!service.labelFor(item).getString().equals("≈05:00"),"range change resets loaded item");
    Configs.ITEM_DESPAWN_TIMER.flag=false;ticks(1);check(service.labelFor(item)==null,"disabled still visible");
   }
   case "identity" -> {
    var item=add(mc,1,1);ticks(1);var old=ItemDespawnDataBridge.requests.getFirst();
    mc.level=new ClientLevel();var replacement=add(mc,1,1);ticks(1);reply(old,5999);ticks(1);
    check(service.labelFor(replacement).getString().startsWith("≈"),"old world reply accepted");
    var fresh=ItemDespawnDataBridge.requests.getLast();reply(fresh,5900);ticks(1);
    check(!service.labelFor(replacement).getString().startsWith("≈"),"valid response rejected");
    var reused=add(mc,1,1);ticks(1);reply(fresh,5999);ticks(1);
    check(service.labelFor(reused).getString().startsWith("≈"),"reused entity ID accepted");
   }
   case "fairness" -> {
    for(int id=1;id<=160;id++)add(mc,id,1);
    var group=new ArrayList<Integer>();for(int id=1;id<=160;id++)group.add(id);
    EntityRenderAggregation.getInstance().groups.put(1,group);
    var seen=new HashSet<Integer>();int consumed=0;
    for(int tick=0;tick<80;tick++){
     ticks(1);int size=ItemDespawnDataBridge.requests.size();check(size-consumed<=4,"budget exceeded");
     for(int i=consumed;i<size;i++){var r=ItemDespawnDataBridge.requests.get(i);seen.add(r.id());reply(r,100);}consumed=size;
    }
    check(seen.size()==160,"members starved after closer entries became due again: "+seen.size());
   }
   case "limits" -> {
    Configs.ITEM_DESPAWN_SOURCE.option=ItemDespawnDataSource.ESTIMATE;Configs.ITEM_DESPAWN_MAX_LABELS.number=8;
    for(int i=1;i<=12;i++)add(mc,i,i);ticks(1);
    for(int i=1;i<=12;i++)check((service.labelFor((ItemEntity)mc.level.getEntity(i))!=null)==(i<=8),"nearest selection");
    Configs.ITEM_DESPAWN_SOURCE.option=ItemDespawnDataSource.SERVUX;ItemDespawnDataBridge.available=false;ticks(1);
    check(service.labelFor((ItemEntity)mc.level.getEntity(1)).getString().equals("--:--"),"strict unavailable source");
   }
   default -> throw new AssertionError(args[0]);
  }
 }
}
'''


class ItemDespawnTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        work = Path(cls.temp.name)
        home = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-25.0.3"))
        cls.java = str(home / "bin/java.exe")
        cls.classpath = str(work)
        sources = []
        for name, code in STUBS.items():
            path = work / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(code, encoding="utf-8")
            sources.append(path)
        for name in ("ItemDespawnAgeModel.java", "ItemDespawnTimerService.java"):
            sources.append(ROOT / "src/main/java" / PACKAGE / "feature" / name)
        harness = work / "DespawnCheck.java"
        harness.write_text(HARNESS, encoding="utf-8")
        result = subprocess.run([str(home / "bin/javac.exe"), "-encoding", "UTF-8", "-d", str(work), *map(str, sources), str(harness)], capture_output=True, text=True)
        if result.returncode:
            raise AssertionError(result.stdout + result.stderr)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def scenario(self, name):
        result = subprocess.run([self.java, "-classpath", self.classpath, "DespawnCheck", name], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_server_prediction_and_expiration(self): self.scenario("age")
    def test_group_uncertainty_and_formatting(self): self.scenario("combine")
    def test_independent_labels_pause_and_range(self): self.scenario("independence")
    def test_world_generation_and_reused_entity_ids(self): self.scenario("identity")
    def test_large_group_fairness_and_request_budget(self): self.scenario("fairness")
    def test_nearest_label_limit_and_strict_source(self): self.scenario("limits")


if __name__ == "__main__":
    unittest.main()

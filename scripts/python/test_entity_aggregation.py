"""Exercise production grouping and label projection with Minecraft boundary stubs and real JOML."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
HARNESS = r'''
import java.util.*;
import java.lang.reflect.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.*;
import net.minecraft.network.chat.Component;
public class AggregationCheck {
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static EntityRenderAggregation aggregation=EntityRenderAggregation.getInstance();
 static void awaitJobs()throws Exception{
  for(String name:List.of("entityScan","itemScan")){
   var field=EntityRenderAggregation.class.getDeclaredField(name);field.setAccessible(true);var channel=field.get(aggregation);
   var pending=channel.getClass().getDeclaredField("pending");pending.setAccessible(true);var job=(java.util.concurrent.Future<?>)pending.get(channel);if(job!=null)job.get();
  }
  aggregation.onClientTick(Minecraft.getInstance());aggregation.beginRenderFrame();
 }
 static EntityRenderState state(double x,double y,double z){var s=new EntityRenderState();s.x=x;s.y=y;s.z=z;s.nameTag=Component.literal("Different creature * 12345");s.nameTagAttachment=Vec3.ZERO;s.distanceToCameraSq=x*x+y*y+z*z;return s;}
 static void rebuild(List<Entity> entities)throws Exception{
  aggregation.clear();var client=Minecraft.getInstance();client.level=new ClientLevel(entities);
  aggregation.onClientTick(client);
  awaitJobs();
 }
 public static void main(String[] args)throws Exception{switch(args[0]){
  case "named_items" -> {
   var a=new ItemEntity(1,new ItemStack("stone","",12));a.named=true;
   var b=new ItemEntity(2,new ItemStack("stone","",23));b.named=true;
   var c=new ItemEntity(3,new ItemStack("stone","named stack",4));c.named=true;
   var d=new ItemEntity(4,new ItemStack("dirt","",9));d.named=true;
   var mob=new LivingEntity(5);mob.named=true;
   rebuild(List.of(a,b,c,d,mob));
   check(!aggregation.shouldHide(a)&&aggregation.shouldHide(b),"timer/custom entity names prevent item aggregation");
   check(!aggregation.shouldHide(c)&&!aggregation.shouldHide(d)&&!aggregation.shouldHide(mob),"different components/items or named mobs were merged");
   check(aggregation.getLabel(a).text.equals("stone * 35"),"group does not sum stack quantities");
   Configs.ITEM_RENDER_AGGREGATION.flag=false;rebuild(List.of(a,b));
   check(!aggregation.shouldHide(a)&&!aggregation.shouldHide(b),"explicitly disabled item switch is ignored");
  }
  case "mob_threshold" -> {
   Configs.ENTITY_RENDER_AGGREGATION.flag=true;Configs.ENTITY_AGGREGATION_THRESHOLD.number=3;
   var a=new LivingEntity(1);var b=new LivingEntity(2);var c=new LivingEntity(3);
   rebuild(List.of(a,b));check(!aggregation.shouldHide(a)&&!aggregation.shouldHide(b),"mob minimum count is ignored");
   rebuild(List.of(a,b,c));check(!aggregation.shouldHide(a)&&aggregation.shouldHide(b)&&aggregation.shouldHide(c),"eligible mob group not merged");
   var client=Minecraft.getInstance();client.level=new ClientLevel(List.of());aggregation.onClientTick(client);
   check(!aggregation.shouldHide(a),"old world group survives reconnect");
  }
  case "sizes" -> {
   Configs.ENTITY_RENDER_AGGREGATION.flag=true;Configs.ENTITY_AGGREGATION_THRESHOLD.number=3;
   check(Configs.ENTITY_AGGREGATION_SEPARATE_SIZES.flag,"size separation does not default on");
   var mobs=new ArrayList<Entity>();
   for(int i=1;i<=9;i++){var mob=new LivingEntity(i);mob.baby=i>=4&&i<=6;if(i>=7){mob.width=2;mob.height=4;}mobs.add(mob);}
   rebuild(mobs);
   for(int i=0;i<9;i++)check(aggregation.shouldHide(mobs.get(i))==(i%3!=0),"small/large groups do not render separate representatives");
   check(aggregation.getLabel(mobs.get(0)).text.equals("mob * 3"),"adult group label is wrong");
   check(aggregation.getLabel(mobs.get(3)).text.equals("Baby mob * 3"),"baby group lacks its own display name");
   check(aggregation.getLabel(mobs.get(6)).text.equals("mob * 3"),"large group label is wrong");
   Configs.ENTITY_AGGREGATION_THRESHOLD.number=4;rebuild(mobs);
   for(var mob:mobs)check(!aggregation.shouldHide(mob),"size groups share the threshold");
   Configs.ENTITY_AGGREGATION_SEPARATE_SIZES.flag=false;rebuild(mobs);
   check(!aggregation.shouldHide(mobs.getFirst()),"joint group loses its representative");
   check(aggregation.getLabel(mobs.getFirst()).text.equals("mob * 9"),"turning size separation off does not restore joint counting");
  }
  case "cube_sizes_and_babies" -> {
   Configs.ENTITY_RENDER_AGGREGATION.flag=true;Configs.ENTITY_AGGREGATION_THRESHOLD.number=3;
   var cubes=new ArrayList<Entity>();
   for(int size:new int[]{1,2,4})for(int i=0;i<3;i++)cubes.add(new net.minecraft.world.entity.monster.cubemob.AbstractCubeMob(cubes.size()+1,size));
   for(int i=0;i<3;i++){
    var baby=new net.minecraft.world.entity.monster.cubemob.AbstractCubeMob(cubes.size()+1,1);baby.baby=true;cubes.add(baby);
   }
   rebuild(cubes);
   for(int i=0;i<cubes.size();i++)check(aggregation.shouldHide(cubes.get(i))==(i%3!=0),"cube size and age groups are merged");
   check(aggregation.getLabel(cubes.get(0)).text.equals("Small slime * 3"),"small cube label is wrong");
   check(aggregation.getLabel(cubes.get(3)).text.equals("Medium slime * 3"),"medium cube label is wrong");
   check(aggregation.getLabel(cubes.get(6)).text.equals("Large slime * 3"),"large cube label is wrong");
   check(aggregation.getLabel(cubes.get(9)).text.equals("Baby Small slime * 3"),"baby cube loses age or size");
   var pigs=new ArrayList<Entity>();
   for(int i=0;i<3;i++){var pig=new LivingEntity(i+20);pig.type=new EntityType<>("pig");pig.baby=true;pigs.add(pig);}
   for(int i=0;i<3;i++){var pig=new LivingEntity(i+23);pig.type=new EntityType<>("pig");pigs.add(pig);}
   rebuild(pigs);
   check(!aggregation.shouldHide(pigs.get(0))&&!aggregation.shouldHide(pigs.get(3)),"other ages lose representatives");
   check(aggregation.getLabel(pigs.get(0)).text.equals("Baby pig * 3"),"other baby mobs are not distinguished");
   check(aggregation.getLabel(pigs.get(3)).text.equals("pig * 3"),"other adult mobs are not distinguished");
  }
  case "layout" -> {
   var labels=new ArrayList<AggregationLabelLayout.Label>();var random=new Random(42);
   for(int i=0;i<200;i++)labels.add(new AggregationLabelLayout.Label(i,100+random.nextDouble()*200,100+random.nextDouble()*80,20+random.nextDouble()*160,6+random.nextDouble()*30,i));
   var placements=AggregationLabelLayout.arrange(labels,3);var offsets=new HashMap<Integer,Double>();for(var p:placements)offsets.put(p.id(),p.offsetY());
   check(placements.equals(AggregationLabelLayout.arrange(labels.reversed(),3)),"iteration order makes label positions unstable");
   for(int i=0;i<labels.size();i++)for(int j=i+1;j<labels.size();j++){
    var a=labels.get(i);var b=labels.get(j);double ay=a.y()+offsets.get(a.id()),by=b.y()+offsets.get(b.id());
    boolean x=Math.abs(a.x()-b.x())<(a.width()+b.width())/2;
    check(!(x&&ay<by+b.height()-1e-7&&by<ay+a.height()-1e-7),"dense mixed-width labels overlap");
   }
   check(AggregationLabelLayout.arrange(List.of(),3).isEmpty(),"empty frame fails");
   var isolated=List.of(new AggregationLabelLayout.Label(1,0,0,20,10,1),new AggregationLabelLayout.Label(2,100,0,20,10,2));
   check(AggregationLabelLayout.arrange(isolated,3).stream().allMatch(p->p.offsetY()==0),"separate labels move unnecessarily");
   var frameA=List.of(new AggregationLabelLayout.Label(1,0,0,20,10,10),new AggregationLabelLayout.Label(2,0,0,20,10,11));
   var frameB=List.of(new AggregationLabelLayout.Label(1,0,0,20,10,11),new AggregationLabelLayout.Label(2,0,0,20,10,10));
   check(AggregationLabelLayout.arrange(frameA,3).equals(AggregationLabelLayout.arrange(frameB,3)),"small camera movement swaps overlapping labels");
   for(double start:new double[]{-0.1,0.1}){
    var initial=List.of(new AggregationLabelLayout.Label(1,0,0,80,10,1),new AggregationLabelLayout.Label(2,0,start,80,10,1));
    var history=new HashMap<Integer,Double>();var old=AggregationLabelLayout.arrange(initial,3);
    for(var p:old)history.put(p.id(),p.offsetY());
    for(int frame=0;frame<100;frame++){
     double y=frame%2==0?-start:start;
     var current=AggregationLabelLayout.arrange(List.of(initial.getFirst(),new AggregationLabelLayout.Label(2,0,y,80,10,1)),3,history);
     check(Math.abs(y+current.get(1).offsetY()-(start+old.get(1).offsetY()))<1e-7,"subpixel motion flips the collision side");
     history.clear();for(var p:current)history.put(p.id(),p.offsetY());
    }
    var separated=AggregationLabelLayout.arrange(List.of(initial.getFirst(),new AggregationLabelLayout.Label(2,100,0,80,10,1)),3,history);
    check(separated.get(1).offsetY()==0,"collision history displaces an isolated label");
   }
  }
  case "representative" -> {
   Configs.ENTITY_RENDER_AGGREGATION.flag=true;Configs.ENTITY_AGGREGATION_THRESHOLD.number=3;
   var client=Minecraft.getInstance();client.cameraEntity=new Entity(99);
   var mobs=new ArrayList<LivingEntity>();
   for(int i=0;i<4;i++){var mob=new LivingEntity(i+1);mob.pos=new Vec3(i*0.2,0,0);mobs.add(mob);}
   client.cameraEntity.pos=Vec3.ZERO;rebuild(new ArrayList<Entity>(mobs));
   check(!aggregation.shouldHide(mobs.get(0)),"initial representative is wrong");
   client.cameraEntity.pos=new Vec3(0.6,0,0);client.level.time=10;aggregation.onClientTick(client);
   awaitJobs();
   check(!aggregation.shouldHide(mobs.get(0)),"same-block camera movement replaces the label anchor");
   mobs.get(0).pos=new Vec3(5,0,0);client.level.time=20;aggregation.onClientTick(client);
   awaitJobs();
   long visible=mobs.subList(1,4).stream().filter(mob->!aggregation.shouldHide(mob)).count();
   check(visible==1,"removed representative prevents the remaining group from rendering");
  }
  case "independent_settings" -> {
   Configs.ENTITY_RENDER_AGGREGATION.flag=true;Configs.ENTITY_AGGREGATION_THRESHOLD.number=3;
   Configs.ITEM_AGGREGATION_THRESHOLD.number=2;Configs.ENTITY_AGGREGATION_RADIUS.decimal=0.25;Configs.ITEM_AGGREGATION_RADIUS.decimal=2;
   var mobs=new ArrayList<Entity>();for(int i=1;i<=3;i++){var m=new LivingEntity(i);m.pos=new Vec3((i-1)*0.2,0,0);mobs.add(m);}
   var stoneA=new ItemEntity(4,new ItemStack("stone","",12));var stoneB=new ItemEntity(5,new ItemStack("stone","",23));stoneB.pos=new Vec3(1.5,0,0);
   var dirtA=new ItemEntity(6,new ItemStack("dirt","",9));var dirtB=new ItemEntity(7,new ItemStack("dirt","",9));
   Configs.ENTITY_AGGREGATION_LIST_MODE.option=io.github.halfmasa.xaerobinding.config.EntityAggregationListMode.WHITELIST;
   Configs.ENTITY_AGGREGATION_WHITELIST.strings=List.of("minecraft:mob");
   Configs.ITEM_AGGREGATION_LIST_MODE.option=io.github.halfmasa.xaerobinding.config.EntityAggregationListMode.WHITELIST;
   Configs.ITEM_AGGREGATION_WHITELIST.strings=List.of(" minecraft:stone ");
   mobs.addAll(List.of(stoneA,stoneB,dirtA,dirtB));rebuild(mobs);
   check(aggregation.shouldHide(mobs.get(1))&&aggregation.shouldHide(stoneB),"separate radii/lists fail to merge matching groups");
   check(!aggregation.shouldHide(dirtA)&&!aggregation.shouldHide(dirtB),"item whitelist uses entity type instead of item ID");
   Configs.ENTITY_AGGREGATION_COUNT_ONLY.flag=true;
   check(aggregation.shouldHideModel(mobs.getFirst())&&!aggregation.shouldHideModel(stoneA),"mob count-only leaks into items");
   Configs.ITEM_AGGREGATION_LABEL_POSITION.option=io.github.halfmasa.xaerobinding.config.EntityLabelPosition.SIDE;
   check(EntityRenderAggregation.isSideLabel(stoneA)&&!EntityRenderAggregation.isSideLabel(mobs.getFirst()),"label position is shared");
   Configs.ITEM_AGGREGATION_THRESHOLD.number=3;rebuild(mobs);
   check(!aggregation.shouldHide(stoneB)&&aggregation.shouldHide(mobs.get(1)),"item threshold changes mob grouping");
   Configs.ITEM_AGGREGATION_LIST_MODE.option=io.github.halfmasa.xaerobinding.config.EntityAggregationListMode.BLACKLIST;
   Configs.ITEM_AGGREGATION_BLACKLIST.strings=List.of("minecraft:stone");Configs.ITEM_AGGREGATION_THRESHOLD.number=2;rebuild(mobs);
   check(!aggregation.shouldHide(stoneB)&&aggregation.shouldHide(dirtB),"item blacklist is ignored");
  }
  case "independent_refresh" -> {
   Configs.ENTITY_RENDER_AGGREGATION.flag=true;Configs.ENTITY_AGGREGATION_THRESHOLD.number=3;
   Configs.ENTITY_AGGREGATION_SCAN_INTERVAL.number=20;Configs.ITEM_AGGREGATION_SCAN_INTERVAL.number=1;
   var entities=new ArrayList<Entity>();for(int i=1;i<=3;i++)entities.add(new LivingEntity(i));
   var a=new ItemEntity(4,new ItemStack("stone","",1));entities.add(a);entities.add(new ItemEntity(5,new ItemStack("stone","",1)));rebuild(entities);
   var client=Minecraft.getInstance();var extra=new LivingEntity(6);entities.add(extra);entities.add(new ItemEntity(7,new ItemStack("stone","",1)));
   client.level.time=1;aggregation.onClientTick(client);awaitJobs();
   check(aggregation.getLabel(a).text.equals("stone * 3"),"item refresh waits for mob interval");
   check(aggregation.getLabel(entities.getFirst()).text.equals("mob * 3"),"item publication clears or refreshes mobs");
   client.level.time=20;aggregation.onClientTick(client);awaitJobs();
   check(aggregation.getLabel(entities.getFirst()).text.equals("mob * 4"),"mob refresh never publishes");
   aggregation.clearItems();check(aggregation.getLabel(entities.getFirst())!=null&&!aggregation.shouldHide(entities.get(4)),"clearing items clears mobs");
   client.level.time=21;aggregation.onClientTick(client);awaitJobs();aggregation.clearEntities();
   check(aggregation.getLabel(a)!=null&&!aggregation.shouldHide(entities.get(1)),"clearing mobs clears items");
  }
  case "render_filter" -> {
   Configs.ENTITY_RENDER_AGGREGATION.flag=false;Configs.ITEM_RENDER_AGGREGATION.flag=false;
   var a=new LivingEntity(1);var b=new LivingEntity(2);var c=new LivingEntity(3);var entities=List.<Entity>of(a,b,c);rebuild(entities);
   check(aggregation.filterForRendering(entities)==entities,"inactive aggregation and filtering must preserve the native iterable");
   EntityRenderDisableService.active=true;EntityRenderDisableService.hidden=Set.of(b.getId());
   var visible=new ArrayList<Entity>();aggregation.filterForRendering(entities).forEach(visible::add);
   check(visible.equals(List.of(a,c)),"custom filtering is skipped when aggregation is disabled");
   Configs.ENTITY_RENDER_AGGREGATION.flag=true;Configs.ENTITY_AGGREGATION_THRESHOLD.number=3;rebuild(entities);
   EntityRenderDisableService.hidden=Set.of(a.getId());visible.clear();aggregation.filterForRendering(entities).forEach(visible::add);
   check(visible.isEmpty(),"an aggregation representative bypasses the custom rendering filter");
   EntityRenderDisableService.active=false;visible.clear();aggregation.filterForRendering(entities).forEach(visible::add);
   check(visible.equals(List.of(a)),"disabling custom filtering does not restore the aggregation representative");
  }
  case "anchor_removed" -> {
   Configs.ENTITY_RENDER_AGGREGATION.flag=true;Configs.ENTITY_AGGREGATION_THRESHOLD.number=3;
   var a=new LivingEntity(1);var b=new LivingEntity(2);var c=new LivingEntity(3);rebuild(List.of(a,b,c));
   a.removed=true;aggregation.beginRenderFrame();
   check(aggregation.getLabel(b)!=null,"label depends on a previous shouldRender side effect");
   check(!aggregation.shouldHide(b)&&aggregation.shouldHide(c),"dead anchor hides the entire group until the next scan");
  }
  case "projection" -> {
   for(float pitch:new float[]{0,0.5f,1.55f})for(float yaw:new float[]{0,1.2f}){
    aggregation.beginRenderFrame();var level=new LevelRenderState();var camera=level.cameraRenderState;
    camera.projectionMatrix=new Matrix4f().perspective((float)Math.toRadians(70),16f/9f,0.05f,256f);
    camera.orientation=new Quaternionf().rotateY(yaw).rotateX(pitch);
    camera.viewRotationMatrix=new Matrix4f().rotation(new Quaternionf(camera.orientation).conjugate());
    var pos=camera.orientation.transform(new Vector3f(0,0,-10));
    var a=state(pos.x,pos.y-0.5,pos.z);var b=state(pos.x,pos.y-0.5,pos.z);
    var behind=camera.orientation.transform(new Vector3f(0,0,10));var hidden=state(behind.x,behind.y-0.5,behind.z);
    level.entityRenderStates.addAll(List.of(a,b,hidden));aggregation.trackLabel(new Entity(1),a);aggregation.trackLabel(new Entity(2),b);aggregation.trackLabel(new Entity(3),hidden);
    aggregation.layoutLabels(level);
    check(a.nameTagAttachment.equals(Vec3.ZERO)&&!b.nameTagAttachment.equals(Vec3.ZERO),"same-screen labels are not separated");
    check(hidden.nameTagAttachment.equals(Vec3.ZERO),"behind-camera label participates in layout");
    var matrix=new Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix);
    var clipA=matrix.transform(new Vector4f((float)(a.x+a.nameTagAttachment.x),(float)(a.y+a.nameTagAttachment.y+0.5),(float)(a.z+a.nameTagAttachment.z),1));
    var clipB=matrix.transform(new Vector4f((float)(b.x+b.nameTagAttachment.x),(float)(b.y+b.nameTagAttachment.y+0.5),(float)(b.z+b.nameTagAttachment.z),1));
    double pixels=Math.abs(clipA.y/clipA.w-clipB.y/clipB.w)*1080/2;
    double textHeight=11*0.025*camera.projectionMatrix.m11()*1080/(2*clipA.w);
    check(pixels>=textHeight+2.9,"projection fails at camera pitch="+pitch+" yaw="+yaw);
    aggregation.beginRenderFrame();var fresh=state(pos.x,pos.y-0.5,pos.z);level.entityRenderStates.clear();level.entityRenderStates.add(fresh);aggregation.trackLabel(new Entity(4),fresh);aggregation.layoutLabels(level);
    check(fresh.nameTagAttachment.equals(Vec3.ZERO),"previous frame offsets leak into next frame");
   }
  }
 }}
}
'''

STUBS = {
 "io/github/halfmasa/xaerobinding/feature/EntityRenderDisableService.java": "package io.github.halfmasa.xaerobinding.feature;import java.util.Set;import net.minecraft.world.entity.Entity;public class EntityRenderDisableService {public static boolean active;public static Set<Integer> hidden=Set.of();public static boolean isFiltering(){return active;}public static boolean shouldHide(Entity entity){return active&&hidden.contains(entity.getId());}}",
 "io/github/halfmasa/xaerobinding/config/Configs.java": '''package io.github.halfmasa.xaerobinding.config; import java.util.*;public class Configs {
 public static class Value {public boolean flag;public int number=10;public double decimal=1;public Object option=EntityAggregationListMode.NONE;public List<String> strings=List.of();public boolean getBooleanValue(){return flag;}public int getIntegerValue(){return number;}public double getDoubleValue(){return decimal;}public Object getOptionListValue(){return option;}public List<String> getStrings(){return strings;}}
 public static final Value ENTITY_RENDER_AGGREGATION=new Value(),ITEM_RENDER_AGGREGATION=new Value(),ENTITY_AGGREGATION_SEPARATE_SIZES=new Value(),ENTITY_AGGREGATION_COUNT_ONLY=new Value(),ENTITY_AGGREGATION_SCAN_INTERVAL=new Value(),ENTITY_AGGREGATION_RADIUS=new Value(),ENTITY_AGGREGATION_THRESHOLD=new Value(),ENTITY_AGGREGATION_LIST_MODE=new Value(),ENTITY_AGGREGATION_WHITELIST=new Value(),ENTITY_AGGREGATION_BLACKLIST=new Value(),ITEM_AGGREGATION_THRESHOLD=new Value(),ITEM_AGGREGATION_RADIUS=new Value(),ITEM_AGGREGATION_SCAN_INTERVAL=new Value(),ITEM_AGGREGATION_LIST_MODE=new Value(),ITEM_AGGREGATION_WHITELIST=new Value(),ITEM_AGGREGATION_BLACKLIST=new Value(),ITEM_AGGREGATION_LABEL_POSITION=new Value(),ENTITY_AGGREGATION_LABEL_POSITION=new Value();static{ITEM_RENDER_AGGREGATION.flag=true;ENTITY_AGGREGATION_SEPARATE_SIZES.flag=true;ITEM_AGGREGATION_THRESHOLD.number=2;ITEM_AGGREGATION_LABEL_POSITION.option=EntityLabelPosition.TOP;ENTITY_AGGREGATION_LABEL_POSITION.option=EntityLabelPosition.TOP;}}''',
 "io/github/halfmasa/xaerobinding/config/EntityAggregationListMode.java": "package io.github.halfmasa.xaerobinding.config;public enum EntityAggregationListMode {NONE,WHITELIST,BLACKLIST}",
 "io/github/halfmasa/xaerobinding/config/EntityLabelPosition.java": "package io.github.halfmasa.xaerobinding.config;public enum EntityLabelPosition {TOP,SIDE}",
 "io/github/halfmasa/xaerobinding/XaeroWorldBinding.java": "package io.github.halfmasa.xaerobinding;public class XaeroWorldBinding {public static final Log LOGGER=new Log();public static class Log {public void warn(String message,Throwable error){throw new AssertionError(message,error);}}}",
 "net/fabricmc/loader/api/FabricLoader.java": "package net.fabricmc.loader.api;public class FabricLoader {public static FabricLoader getInstance(){return new FabricLoader();}public boolean isModLoaded(String id){return false;}}",
 "fi/dy/masa/malilib/config/options/ConfigBooleanHotkeyed.java": "package fi.dy.masa.malilib.config.options;public class ConfigBooleanHotkeyed {public boolean getBooleanValue(){return false;}}",
 "fi/dy/masa/malilib/interfaces/IClientTickHandler.java": "package fi.dy.masa.malilib.interfaces;import net.minecraft.client.Minecraft;public interface IClientTickHandler {void onClientTick(Minecraft client);}",
 "net/minecraft/network/chat/Component.java": "package net.minecraft.network.chat;public class Component {public String text;public Component(String s){text=s;}public static Component empty(){return new Component(\"\");}public static Component literal(String s){return new Component(s);}public static Component translatable(String key,Object... args){String prefix=switch(key){case \"halfmasa.entity_aggregation.baby\"->\"Baby \";case \"halfmasa.entity_aggregation.baby_cube\"->\"Baby \";case \"halfmasa.entity_aggregation.size.small\"->\"Small \";case \"halfmasa.entity_aggregation.size.medium\"->\"Medium \";case \"halfmasa.entity_aggregation.size.large\"->\"Large \";default->throw new IllegalArgumentException(key);};return new Component(prefix+((Component)args[0]).text);}public Component append(Component c){text+=c.text;return this;}}",
 "net/minecraft/world/phys/Vec3.java": "package net.minecraft.world.phys;public record Vec3(double x,double y,double z) {public static final Vec3 ZERO=new Vec3(0,0,0);public Vec3 add(double a,double b,double c){return new Vec3(x+a,y+b,z+c);}public double distanceToSqr(Vec3 b){return (x-b.x)*(x-b.x)+(y-b.y)*(y-b.y)+(z-b.z)*(z-b.z);}}",
 "net/minecraft/world/entity/EntityType.java": "package net.minecraft.world.entity;import net.minecraft.network.chat.Component;public class EntityType<T> {private final String name;public EntityType(){this(\"mob\");}public EntityType(String name){this.name=name;}public Component getDescription(){return Component.literal(name);}public boolean equals(Object other){return other instanceof EntityType<?> type&&name.equals(type.name);}public int hashCode(){return name.hashCode();}}",
 "net/minecraft/world/entity/Entity.java": "package net.minecraft.world.entity;import net.minecraft.world.phys.Vec3;public class Entity {public boolean named,removed;public Vec3 pos=Vec3.ZERO;private int id;public static final EntityType<?> TYPE=new EntityType<>();public EntityType<?> type=TYPE;public Entity(){this(0);}public Entity(int id){this.id=id;}public int getId(){return id;}public boolean isRemoved(){return removed;}public boolean hasCustomName(){return named;}public Vec3 position(){return pos;}public EntityType<?> getType(){return type;}}",
 "net/minecraft/world/entity/LivingEntity.java": "package net.minecraft.world.entity;public class LivingEntity extends Entity {public boolean baby;public float width=1,height=2;public LivingEntity(int id){super(id);}public boolean isBaby(){return baby;}public EntityDimensions getDimensions(Pose pose){return new EntityDimensions(width,height);}}",
 "net/minecraft/world/entity/monster/cubemob/AbstractCubeMob.java": "package net.minecraft.world.entity.monster.cubemob;public class AbstractCubeMob extends net.minecraft.world.entity.LivingEntity {private final int size;public AbstractCubeMob(int id,int size){super(id);this.size=size;this.type=new net.minecraft.world.entity.EntityType<>(\"slime\");}public int getSize(){return size;}}",
 "net/minecraft/world/entity/EntityDimensions.java": "package net.minecraft.world.entity;public record EntityDimensions(float width,float height) {}",
 "net/minecraft/world/entity/Pose.java": "package net.minecraft.world.entity;public enum Pose {STANDING,CROUCHING}",
 "net/minecraft/world/entity/Display.java": "package net.minecraft.world.entity;public class Display extends Entity {}",
 "net/minecraft/world/entity/item/ItemEntity.java": "package net.minecraft.world.entity.item;import net.minecraft.world.entity.Entity;import net.minecraft.world.item.ItemStack;public class ItemEntity extends Entity {private final ItemStack stack;public ItemEntity(int id,ItemStack stack){super(id);this.stack=stack;}public ItemStack getItem(){return stack;}}",
 "net/minecraft/world/item/ItemStack.java": "package net.minecraft.world.item;import net.minecraft.network.chat.Component;public record ItemStack(String item,String components,int count) {public boolean isEmpty(){return count==0;}public ItemStack copy(){return new ItemStack(item,components,count);}public int getCount(){return count;}public Object getItem(){return item;}public Component getHoverName(){return Component.literal(item);}public static boolean isSameItemSameComponents(ItemStack a,ItemStack b){return a.item.equals(b.item)&&a.components.equals(b.components);}}",
 "net/minecraft/core/registries/BuiltInRegistries.java": "package net.minecraft.core.registries;import net.minecraft.world.entity.EntityType;public class BuiltInRegistries {public static final Types ENTITY_TYPE=new Types();public static final Items ITEM=new Items();public static class Types {public String getKey(EntityType<?> t){return \"minecraft:mob\";}}public static class Items {public String getKey(Object item){return \"minecraft:\"+item;}}}",
 "net/minecraft/client/multiplayer/ClientLevel.java": "package net.minecraft.client.multiplayer;import java.util.*;import net.minecraft.world.entity.Entity;public class ClientLevel {private final List<Entity> entities;public long time;public ClientLevel(List<Entity> e){entities=e;}public long getGameTime(){return time;}public Iterable<Entity> entitiesForRendering(){return entities;}public Entity getEntity(int id){return entities.stream().filter(e->e.getId()==id).findFirst().orElse(null);}}",
 "net/minecraft/client/Minecraft.java": "package net.minecraft.client;import net.minecraft.client.multiplayer.ClientLevel;import net.minecraft.world.entity.Entity;import net.minecraft.network.chat.Component;public class Minecraft {static final Minecraft INSTANCE=new Minecraft();public static Minecraft getInstance(){return INSTANCE;}public ClientLevel level;public Entity cameraEntity;public final Font font=new Font();public Entity getCameraEntity(){return cameraEntity;}public Window getWindow(){return new Window();}public static class Font {public int lineHeight=9;public int width(Component c){return c.text.length()*6;}}public static class Window {public int getWidth(){return 1920;}public int getHeight(){return 1080;}}}",
 "net/minecraft/client/renderer/entity/state/EntityRenderState.java": "package net.minecraft.client.renderer.entity.state;import net.minecraft.network.chat.Component;import net.minecraft.world.phys.Vec3;public class EntityRenderState {public double x,y,z,distanceToCameraSq;public Component nameTag;public Vec3 nameTagAttachment;}",
 "net/minecraft/client/renderer/state/level/CameraRenderState.java": "package net.minecraft.client.renderer.state.level;import net.minecraft.world.phys.Vec3;import org.joml.*;public class CameraRenderState {public Vec3 pos=Vec3.ZERO;public Matrix4f projectionMatrix,viewRotationMatrix;public Quaternionf orientation;}",
 "net/minecraft/client/renderer/state/level/LevelRenderState.java": "package net.minecraft.client.renderer.state.level;import java.util.*;import net.minecraft.client.renderer.entity.state.EntityRenderState;public class LevelRenderState {public CameraRenderState cameraRenderState=new CameraRenderState();public List<EntityRenderState> entityRenderStates=new ArrayList<>();}",
}
for qualified in ["boss.enderdragon.EnderDragon", "boss.wither.WitherBoss", "decoration.ArmorStand", "player.Player"]:
 package, name = qualified.rsplit(".", 1)
 STUBS["net/minecraft/world/entity/" + qualified.replace(".", "/") + ".java"] = f"package net.minecraft.world.entity.{package};public class {name} extends net.minecraft.world.entity.Entity {{}}"

class EntityAggregationTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.temp=tempfile.TemporaryDirectory(prefix="halfmasa-aggregation-");cls.addClassCleanup(cls.temp.cleanup);work=Path(cls.temp.name)
  home=Path(os.environ.get("JAVA_HOME","C:/Program Files/Java/jdk-25.0.3"));cls.java=str(home/"bin/java.exe")
  joml=next((Path.home()/".gradle/caches/modules-2/files-2.1/org.joml/joml/1.10.9").glob("*/*.jar"))
  cls.classpath=str(work)+os.pathsep+str(joml);sources=[]
  for name,code in STUBS.items():
   # The production Vec3 exposes fields as well as methods.
   if name.endswith("/Vec3.java"):
    code=code.replace("public record Vec3(double x,double y,double z)","public class Vec3").replace("{public static final", "{public final double x,y,z;public Vec3(double x,double y,double z){this.x=x;this.y=y;this.z=z;}public boolean equals(Object o){return o instanceof Vec3 v&&x==v.x&&y==v.y&&z==v.z;}public static final")
   path=work/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(code,encoding="utf-8");sources.append(path)
  for name in ["EntityRenderAggregation.java","AggregationLabelLayout.java"]:sources.append(ROOT/"src/main/java/io/github/halfmasa/xaerobinding/feature"/name)
  harness=work/"AggregationCheck.java";harness.write_text(HARNESS,encoding="utf-8");sources.append(harness)
  result=subprocess.run([str(home/"bin/javac.exe"),"-encoding","UTF-8","-classpath",str(joml),"-d",str(work),*map(str,sources)],capture_output=True,text=True)
  if result.returncode:raise AssertionError(result.stdout+result.stderr)
 def scenario(self,name):
  result=subprocess.run([self.java,"-classpath",self.classpath,"AggregationCheck",name],capture_output=True,text=True)
  self.assertEqual(result.returncode,0,result.stdout+result.stderr)
 def test_timer_named_items_merge_without_merging_other_items_components_or_named_mobs(self):self.scenario("named_items")
 def test_mob_threshold_and_world_change(self):self.scenario("mob_threshold")
 def test_small_and_large_mobs_have_independent_counts_thresholds_and_representatives(self):self.scenario("sizes")
 def test_cube_sizes_and_other_baby_mobs_keep_distinct_labels_and_counts(self):self.scenario("cube_sizes_and_babies")
 def test_dense_mixed_width_labels_are_deterministic_and_do_not_overlap(self):self.scenario("layout")
 def test_representative_stays_stable_within_one_block_and_recovers_after_removal(self):self.scenario("representative")
 def test_camera_pitch_yaw_projection_and_frame_reset(self):self.scenario("projection")
 def test_mob_and_item_threshold_radius_lists_position_and_count_only_are_independent(self):self.scenario("independent_settings")
 def test_mob_and_item_refresh_and_invalidation_are_independent(self):self.scenario("independent_refresh")
 def test_dead_representative_recovers_before_the_next_scan(self):self.scenario("anchor_removed")
 def test_render_filter_applies_with_or_without_aggregation_and_preserves_native_fast_path(self):self.scenario("render_filter")

if __name__=="__main__":unittest.main()

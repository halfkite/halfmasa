"""Exercise real Jade/TIS inventories and client packets without Servux or a server HalfMasa mod."""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time
import zipfile

import run_startup_smoke as startup
import port_smoke_compat as port_compat

ROOT = Path(__file__).resolve().parents[2]
BASELINE_SOURCE = r'''package io.github.halfmasa.jadebarrelsmoke;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import snownee.jade.addon.universal.*;
import snownee.jade.impl.BlockAccessorImpl;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
public class JadeBarrelSmoke implements ClientModInitializer {
 static JadeBarrelSmoke instance;
 int stage, checks;
 volatile boolean complete;
 volatile Throwable failure;
 public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;System.out.println("JADE_BARREL_CHECK "+why);}
 void tick(Minecraft mc){try{
  if(failure!=null)throw new AssertionError("server fixture",failure);
  if(stage==0){
   if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
   snownee.jade.Jade.config().general().setDisplayTooltip(false);
   stage=1;
   mc.createWorldOpenFlows().createFreshLevel("jade-barrel-fixture",new LevelSettings("Jade Barrel Fixture",GameType.CREATIVE,
    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
    new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;
  }
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  if(stage==1){
   if(mc.level.getGameTime()<30)return;
   MinecraftClientCompat.setScreen(mc,null);stage=2;
   mc.getSingleplayerServer().execute(()->{try{
    carpettisaddition.CarpetTISAdditionSettings.largeBarrel=true;
    var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    var level=player.level();var first=new BlockPos(0,100,0);var second=first.east();
    level.setBlock(first,Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING,Direction.WEST),3);
    level.setBlock(second,Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING,Direction.EAST),3);
    var a=(BarrelBlockEntity)level.getBlockEntity(first);var b=(BarrelBlockEntity)level.getBlockEntity(second);
    a.setItem(0,new ItemStack(Items.STONE,2));b.setItem(0,new ItemStack(Items.DIAMOND,3));
    var accessor=new BlockAccessorImpl.Builder().level(level).player(player).blockState(level.getBlockState(first))
     .blockEntity(()->a).hit(new BlockHitResult(Vec3.atCenterOf(first),Direction.WEST,first,false))
     .serverData(new CompoundTag()).serverConnected(true).showDetails(true).build();
    var inventory=carpettisaddition.helpers.rule.largeBarrel.LargeBarrelHelper.getInventory(level.getBlockState(first),level,first);
    check(inventory!=null&&inventory.getContainerSize()==54,"TIS owns a real 54-slot barrel");
    var nativeGroups=ItemStorageProvider.Extension.INSTANCE.getGroups(accessor);
    int stone=0,diamond=0;
    if(nativeGroups!=null)for(var group:nativeGroups)for(var item:group.views){if(item.is(Items.STONE))stone+=item.getCount();if(item.is(Items.DIAMOND))diamond+=item.getCount();}
    check(stone==2&&(diamond==0||diamond==3),"native Jade inventory observed (stone="+stone+", diamond="+diamond+")");
    System.out.println("JADE_NATIVE_FULL="+(diamond==3));
    System.out.println("JADE_BARREL_BASELINE_PASS checks="+checks);complete=true;
   }catch(Throwable t){failure=t;}});return;
  }
  if(stage==2&&complete){stage=99;mc.stop();}
 }catch(Throwable error){throw new IllegalStateException("Jade barrel smoke stage="+stage,error);}}
}
'''
SOURCE = r'''package io.github.halfmasa.jadebarrelsmoke;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Util;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import snownee.jade.JadeClient;
import snownee.jade.addon.universal.*;
import snownee.jade.api.*;
import snownee.jade.api.view.*;
import snownee.jade.impl.*;
import snownee.jade.network.RequestBlockPacket;
import snownee.jade.overlay.WailaTickHandler;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.jade.JadeBarrelPreview;
import io.github.halfmasa.xaerobinding.mixin.JadeBarrelStorageAccessor;
public class JadeBarrelSmoke implements ClientModInitializer {
 static JadeBarrelSmoke instance;
 static final BlockPos FIRST=new BlockPos(0,100,0);
 static final String NONCE="halfmasa:jade_barrel_request";
 final BlockAccessorClientHandler handler=new BlockAccessorClientHandler();
 record Case(String name,Direction facing,int kind,boolean other,boolean enabled,boolean rule){}
 final List<Case> cases=new ArrayList<>();
 int stage,checks,caseIndex,calls,packets,replies;
 long began,lastRequest,readyAt;
 volatile boolean ready;
 volatile Throwable failure;
 CompoundTag lastReply;
 CompoundTag primaryData=new CompoundTag();
 boolean dropReplies,seenLocked,seenLoot;
 public void onInitializeClient(){instance=this;for(Direction d:Direction.values())cases.add(new Case("facing "+d,d,0,false,true,true));
  cases.add(new Case("target opposite half",Direction.WEST,0,true,true,true));
  cases.add(new Case("empty partner",Direction.WEST,1,false,true,true));
  cases.add(new Case("unequal large totals",Direction.WEST,2,false,true,true));
  cases.add(new Case("large and ordinary totals",Direction.WEST,4,false,true,true));
  cases.add(new Case("ordinary identical items",Direction.WEST,5,false,true,true));
  cases.add(new Case("distinct item components",Direction.WEST,3,false,true,true));
  cases.add(new Case("locked partner in survival",Direction.WEST,6,false,true,true));
  cases.add(new Case("unopened loot partner",Direction.WEST,7,false,true,true));
  cases.add(new Case("disabled switch",Direction.WEST,0,false,false,true));
  cases.add(new Case("disabled TIS rule",Direction.WEST,0,false,true,false));
 }
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 public static void packet(CustomPacketPayload packet){if(instance!=null&&packet instanceof RequestBlockPacket)instance.packets++;}
 public static boolean reply(CompoundTag data){if(instance==null)return false;instance.replies++;instance.lastReply=data.copy();
  instance.seenLocked|=data.getBooleanOr("Locked",false);instance.seenLoot|=data.getBooleanOr("Loot",false);return instance.dropReplies;}
 public static void applied(){if(instance!=null&&JadeClient.tickHandler().state!=null)instance.primaryData=JadeClient.tickHandler().state.data().copy();}
 void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;System.out.println("JADE_BARREL_CHECK "+why);}
 void reset(){Configs.JADE_LARGE_BARREL.setBooleanValue(false);JadeBarrelPreview.tick();JadeClient.tickHandler().clearState();primaryData=new CompoundTag();}
 void prepare(Minecraft mc){
  reset();Case c=cases.get(caseIndex);Configs.JADE_LARGE_BARREL.setBooleanValue(c.enabled);ready=false;seenLocked=seenLoot=false;stage=3;began=Util.getMillis();lastRequest=0;
  mc.getSingleplayerServer().execute(()->{try{
   carpettisaddition.CarpetTISAdditionSettings.largeBarrel=c.rule;
   var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var level=player.level();
   player.setGameMode(c.kind==6?GameType.SURVIVAL:GameType.CREATIVE);
   player.teleportTo(0.5,102,3.5);player.setNoGravity(true);
   for(BlockPos p:BlockPos.betweenClosed(FIRST.offset(-1,-1,-1),FIRST.offset(1,1,1)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
   var other=FIRST.relative(c.facing.getOpposite());
   level.setBlock(FIRST,Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING,c.facing),3);
   level.setBlock(other,Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING,c.facing.getOpposite()),3);
   var a=(BarrelBlockEntity)level.getBlockEntity(FIRST);var b=(BarrelBlockEntity)level.getBlockEntity(other);
   if(c.kind==2||c.kind==4){a.setItem(0,new ItemStack(Items.STONE,64));a.setItem(1,new ItemStack(Items.STONE,64));
    b.setItem(0,new ItemStack(Items.STONE,64));if(c.kind==2){b.setItem(1,new ItemStack(Items.STONE,64));b.setItem(2,new ItemStack(Items.STONE,64));}}
   else {a.setItem(0,new ItemStack(Items.STONE,2));if(c.kind!=1){var item=new ItemStack(c.kind==3||c.kind==5?Items.STONE:Items.DIAMOND,3);
    if(c.kind==3)item.set(DataComponents.CUSTOM_NAME,Component.literal("Different stone"));b.setItem(0,item);}}
   if(c.kind==6){var field=net.minecraft.world.level.block.entity.BaseContainerBlockEntity.class.getDeclaredField("lockKey");field.setAccessible(true);
    field.set(b,new LockCode(net.minecraft.advancements.predicates.ItemPredicate.Builder.item().build()));}
   if(c.kind==7)b.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON);
   if(c.kind==6){check(!player.isCreative(),"locked barrel fixture uses survival");
    var acc=new BlockAccessorImpl.Builder().level(level).player(player).blockState(level.getBlockState(other)).blockEntity(()->b)
     .hit(new BlockHitResult(Vec3.atCenterOf(other),Direction.UP,other,false)).serverData(new CompoundTag()).serverConnected(true).showDetails(true).build();
    ItemStorageProvider.BLOCK.appendServerData(acc.getServerData(),acc);System.out.println("Protected server data "+acc.getServerData());
    check(acc.getServerData().getBooleanOr("Locked",false),"server Jade marks locked partner");}
   a.setChanged();b.setChanged();readyAt=Util.getMillis();ready=true;
  }catch(Throwable t){failure=t;}});
 }
 BlockAccessor accessor(Minecraft mc,BlockPos pos){
  CompoundTag data=primaryData.getIntOr("x",-999)==pos.getX()&&primaryData.getIntOr("y",-999)==pos.getY()&&primaryData.getIntOr("z",-999)==pos.getZ()?primaryData.copy():new CompoundTag();
  data.putBoolean("SortItems",true);data.putInt("MaxItemCount",54);
  return new BlockAccessorImpl.Builder().level(mc.level).player(mc.player).blockState(mc.level.getBlockState(pos))
   .blockEntity(()->mc.level.getBlockEntity(pos)).hit(new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))
   .serverData(data).serverConnected(true).showDetails(true).build();
 }
 List<ItemStack> displayed(BlockAccessor accessor){
  var state=JadeClient.tickHandler().state;if(state!=null)accessor.setServerData(state.data());
  var raw=accessor.getServerData().get(JadeIds.UNIVERSAL_ITEM_STORAGE.toString());
  List<ViewGroup<ItemStack>> groups=List.of();
  if(raw!=null){var decoded=accessor.decodeFromNbt(JadeBarrelStorageAccessor.halfmasa$codec(),raw).orElse(null);if(decoded!=null)groups=decoded.getValue();}
  var clientGroups=ItemStorageProvider.Extension.INSTANCE.getClientGroups(accessor,groups);
  var items=new ArrayList<ItemStack>();if(clientGroups!=null)for(var group:clientGroups)for(var view:group.views)items.add(view.item);return items;
 }
 int total(List<ItemStack> items,Item type){return items.stream().filter(s->s.is(type)).mapToInt(ItemStack::getCount).sum();}
 boolean matches(Case c,List<ItemStack> items){
  if(!c.enabled||!c.rule)return total(items,Items.STONE)==2&&total(items,Items.DIAMOND)==0;
  if(c.kind==2)return items.size()==1&&total(items,Items.STONE)==320;
  if(c.kind==4)return items.size()==1&&total(items,Items.STONE)==192;
  if(c.kind==5)return items.size()==1&&total(items,Items.STONE)==5;
  if(c.kind==3)return items.size()==2&&total(items,Items.STONE)==5&&items.stream().filter(s->s.has(DataComponents.CUSTOM_NAME)).count()==1;
  if(c.kind==6||c.kind==7)return total(items,Items.STONE)==2&&total(items,Items.DIAMOND)==0&&(c.kind==6?seenLocked:seenLoot);
  return total(items,Items.STONE)==2&&total(items,Items.DIAMOND)==(c.kind==1?0:3);
 }
 @SuppressWarnings({"rawtypes","unchecked"})
 void tick(Minecraft mc){try{
  if(failure!=null)throw new AssertionError("server fixture",failure);
  long now=Util.getMillis();
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;
   check(!Configs.JADE_LARGE_BARREL.getBooleanValue(),"compatibility defaults to disabled");snownee.jade.Jade.config().general().setDisplayTooltip(false);stage=1;
   mc.createWorldOpenFlows().createFreshLevel("jade-barrel-fixture",new LevelSettings("Jade Barrel Fixture",GameType.CREATIVE,
    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),
    new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;
  }
  if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
  if(stage==1){if(mc.level.getGameTime()<30)return;MinecraftClientCompat.setScreen(mc,null);prepare(mc);return;}
  if(stage==3){
   if(now-began>15000)throw new AssertionError("case timeout: "+cases.get(caseIndex)+" packets="+packets+" replies="+replies+" last="+lastReply);
   if(!ready||now-readyAt<500)return;Case c=cases.get(caseIndex);var other=FIRST.relative(c.facing.getOpposite());
   if(!mc.level.getBlockState(FIRST).is(Blocks.BARREL)||!mc.level.getBlockState(other).is(Blocks.BARREL)
    ||mc.level.getBlockState(FIRST).getValue(BarrelBlock.FACING)!=c.facing)return;
   var accessor=accessor(mc,c.other?other:FIRST);
   JadeClient.tickHandler().state=new WailaTickHandler.State(accessor,(AccessorClientHandler)handler,accessor.getServerData());
   if(now-lastRequest>=250){lastRequest=now;calls++;handler.requestData(accessor,List.of(ItemStorageProvider.BLOCK));}
   var items=displayed(accessor);
   if(now-readyAt>1500&&matches(c,items)){
    check(true,c.name+" through actual Jade request/reply and preview mixins");
    if(caseIndex==0){
     check(JadeClient.tickHandler().getData().getIntOr("x",-99)==FIRST.getX(),"auxiliary reply preserves primary target metadata");
     CompoundTag old=lastReply.copy();reset();Configs.JADE_LARGE_BARREL.setBooleanValue(true);
     check(JadeBarrelPreview.receive(old),"late reply from cancelled request is discarded");
     check(JadeBarrelPreview.groups(accessor,List.of()).isEmpty(),"cancelled pair has no cached preview");
    }
    caseIndex++;if(caseIndex<cases.size())prepare(mc);else{stage=4;began=now;reset();Configs.JADE_LARGE_BARREL.setBooleanValue(true);carpettisaddition.CarpetTISAdditionSettings.largeBarrel=true;}
   }return;
  }
  if(stage==4){
   // Re-establish both samples, then prevent replies to prove the preview expires.
   var accessor=accessor(mc,FIRST);JadeClient.tickHandler().state=new WailaTickHandler.State(accessor,(AccessorClientHandler)handler,accessor.getServerData());
   if(now-lastRequest>=250){lastRequest=now;calls++;handler.requestData(accessor,List.of(ItemStorageProvider.BLOCK));}
   if(total(displayed(accessor),Items.DIAMOND)==3){dropReplies=true;stage=5;began=now;}
   else if(now-began>15000)throw new AssertionError("timeout waiting for cache expiry fixture");return;
  }
  if(stage==5){
   var accessor=accessor(mc,FIRST);var items=displayed(accessor);
   if(now-began>2500){check(total(items,Items.DIAMOND)==0,"missing replies expire the combined preview");dropReplies=false;
    mc.level.setBlock(FIRST.east(),Blocks.AIR.defaultBlockState(),3);JadeBarrelPreview.tick();
    check(JadeBarrelPreview.groups(accessor,List.of()).isEmpty(),"removing partner immediately invalidates cache");
    check(packets<=calls,"outgoing packets never exceed native request invocations ("+packets+"/"+calls+")");
    System.out.println("JADE_BARREL_PASS checks="+checks+" packets="+packets+" replies="+replies);stage=99;mc.stop();}return;
  }
 }catch(Throwable error){throw new IllegalStateException("Jade barrel smoke stage="+stage+" case="+caseIndex,error);}}
}
'''
PACKET = r'''package io.github.halfmasa.jadebarrelsmoke.mixin;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.jadebarrelsmoke.JadeBarrelSmoke;
@Mixin(targets="snownee.jade.util.ClientProxy",remap=false) public class PacketMixin {
 @Inject(method="sendPacket",at=@At("HEAD")) private static void packet(CustomPacketPayload packet,CallbackInfo ci){JadeBarrelSmoke.packet(packet);}
}
'''
REPLY = r'''package io.github.halfmasa.jadebarrelsmoke.mixin;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.jadebarrelsmoke.JadeBarrelSmoke;
@Mixin(targets="snownee.jade.overlay.WailaTickHandler",remap=false,priority=2000) public class ReplyMixin {
 @Inject(method="setData",at=@At("TAIL")) private void applied(CompoundTag data,CallbackInfo ci){JadeBarrelSmoke.applied();}
}
'''
RECEIVE = r'''package io.github.halfmasa.jadebarrelsmoke.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.network.ReceiveDataPacket;
import snownee.jade.network.ClientPayloadContext;
import io.github.halfmasa.jadebarrelsmoke.JadeBarrelSmoke;
@Mixin(value=ReceiveDataPacket.class,remap=false) public class ReceiveMixin {
 @Inject(method="handle",at=@At("HEAD"),cancellable=true) private static void received(ReceiveDataPacket packet,ClientPayloadContext context,CallbackInfo ci){
  if(JadeBarrelSmoke.reply(packet.tag()))ci.cancel();
 }
}
'''
FRAME = r'''package io.github.halfmasa.jadebarrelsmoke.mixin;
import net.minecraft.client.Minecraft;
import io.github.halfmasa.jadebarrelsmoke.JadeBarrelSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class FrameMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void frame(CallbackInfo ci){JadeBarrelSmoke.frame((Minecraft)(Object)this);}
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--version', default='26.3')
    parser.add_argument('--loader-version', default='0.19.5')
    parser.add_argument('--mods', type=Path, default=ROOT / 'build/compat-fixes-tests/26.3-20261020-153751-141456/game/mods')
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Java/jdk-25.0.3'))
    parser.add_argument('--timeout', type=int, default=180)
    parser.add_argument('--baseline', action='store_true')
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    directory = ROOT / 'build/jade-barrel-tests' / (args.version + '-' + datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
    classes = directory / 'classes'
    classes.mkdir(parents=True)
    selected = [next(args.mods.glob(pattern)) for pattern in (
        'Jade-*.jar', 'carpet-tis-addition-*.jar', 'fabric-carpet-*.jar', 'fabric-api-*.jar')]
    cp = port_compat.classpath(args.version, args.artifact)
    if args.version != '26.3':
        refs = json.loads((ROOT/'build/jade-port-reference/versions.json').read_text())
        server = json.loads((ROOT/'build/jade-port-reference/server-mods.json').read_text())
        api_version = port_compat.api_version(args.version)
        api = next(p for p in (Path.home()/('.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/'+api_version)).glob('*/*.jar') if not p.name.endswith('-sources.jar'))
        selected = [Path(refs[args.version]['path']),Path(server['carpet-tis-addition'][args.version]['path']),Path(server['carpet'][args.version]['path']),api]
    cp += ';' + ';'.join(str(port_compat.named_mod(args.version,args.java_home,p,cp)) for p in selected)
    sources = []
    fixture_sources = [('JadeBarrelSmoke', BASELINE_SOURCE if args.baseline else SOURCE), ('FrameMixin', FRAME)]
    if not args.baseline:
        fixture_sources += [('PacketMixin', PACKET), ('ReplyMixin', REPLY), ('ReceiveMixin', RECEIVE)]
    for name, text in fixture_sources:
        text = port_compat.source(text,args.version,cp)
        if args.version not in ('26.2','26.3'):
            text = text.replace('net.minecraft.advancements.predicates.', 'net.minecraft.advancements.criterion.')
        if args.version.startswith('1.21.') and tuple(map(int,args.version.split('.'))) < (1,21,11):
            text = text.replace('net.minecraft.util.Util','net.minecraft.Util')
        if args.version == '1.21.1':
            text = text.replace('snownee.jade.Jade.config().general()', 'snownee.jade.Jade.CONFIG.get().getGeneral()')
        path = directory / (name + '.java')
        path.write_text(text, encoding='utf-8')
        sources.append(str(path))
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-proc:none', '-encoding', 'UTF-8', '-cp', cp,
                             '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    mod = directory / 'jade-barrel-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_jade_barrel_smoke', 'version': '1.0',
            'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.jadebarrelsmoke.JadeBarrelSmoke']},
            'mixins': ['jade-barrel-smoke.mixins.json']}))
        jar.writestr('jade-barrel-smoke.mixins.json', json.dumps({'required': True,
            'package': 'io.github.halfmasa.jadebarrelsmoke.mixin', 'compatibilityLevel': 'JAVA_21' if args.version.startswith('1.21.') else 'JAVA_25',
            'client': ['FrameMixin'] + ([] if args.baseline else ['PacketMixin', 'ReplyMixin', 'ReceiveMixin']), 'injectors': {'defaultRequire': 1}}))
    mod = port_compat.remap_fixture(args.version,args.java_home,mod,cp,directory)
    game = directory / 'game'
    (game / 'mods').mkdir(parents=True)
    (game / 'config').mkdir()
    for path in selected:
        shutil.copy2(path, game / 'mods' / path.name)
    (game / 'config/malilib.json').write_text('{}\n', encoding='utf-8')
    (game / 'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nnarrator:0\nlang:en_us\n'
        'guiScale:1\nenableVsync:false\nmaxFps:60\nrenderDistance:2\nsimulationDistance:5\n'
        'pauseOnLostFocus:false\nsoundCategory_master:0.0\n', encoding='utf-8')
    command = startup.production_command(args, game, mod)
    command[1:1] = ['-Djava.awt.headless=true', '-Dsun.java2d.d3d=false']
    log_path = directory / 'launcher.log'
    start = time.monotonic()
    with log_path.open('w', encoding='utf-8') as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output,
                                   stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
            code = process.wait(timeout=30)
    log = log_path.read_text(encoding='utf-8', errors='replace')
    marker = 'JADE_BARREL_BASELINE_PASS' if args.baseline else 'JADE_BARREL_PASS'
    passed = code == 0 and marker in log
    report = {'result': 'PASS' if passed else 'FAIL', 'baseline': args.baseline, 'version': args.version,
        'native_full_inventory': ('JADE_NATIVE_FULL=true' in log) if args.baseline else None,
        'server_kind': 'integrated',
        'artifact': str(args.artifact), 'sha256': hashlib.sha256(args.artifact.read_bytes()).hexdigest(),
        'mods': [path.name for path in selected], 'servux': False, 'server_halfmasa': False,
        'duration_seconds': round(time.monotonic() - start, 1), 'exit_code': code, 'log': str(log_path),
        'checks': [line for line in log.splitlines() if 'JADE_BARREL_' in line]}
    report_path = directory / 'report.json'
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('JADE BARREL:', report['result'], report_path, flush=True)
    if not passed:
        print('\n'.join(log.splitlines()[-45:]), flush=True)
    return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())

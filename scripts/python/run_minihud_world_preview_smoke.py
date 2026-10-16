"""Measure the real held world-container callback, not the tooltip callback."""
import json
import sys
import run_minihud_independent_smoke as independent
import run_minihud_preview_smoke as harness

SOURCE = independent.SOURCE.replace(
    'public static int worldCalls,textCalls,checks;',
    'public static int worldCalls,textCalls,checks,parses;'
).replace(
    'box.setItem(0,new ItemStack(Items.DIAMOND,17));',
    '''for(int slot=1;slot<26;slot++){
     var nested=net.minecraft.core.NonNullList.withSize(27,ItemStack.EMPTY);
     for(int i=0;i<27;i++)nested.set(i,new ItemStack(i%2==0?Items.DIAMOND:Items.EMERALD,i+1));
     var stack=new ItemStack(Items.SHULKER_BOX);stack.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(nested));box.setItem(slot,stack);
    }box.setItem(0,new ItemStack(Items.DIAMOND,17));'''
)
SOURCE = SOURCE[:SOURCE.index(' static class MatrixScreen')] + r'''
 static final boolean EXPECT_CACHE=true;
 static class MatrixScreen extends Screen {
  final PreviewSmoke owner;int frame,warmup,startParses,startCalls;long startGc,warmDeadline;boolean ready;
  List<Long> nanos=new ArrayList<>(),bytes=new ArrayList<>();
  final com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
  MatrixScreen(PreviewSmoke owner){super(Component.literal("World preview profiling"));this.owner=owner;}
  public boolean isPauseScreen(){return false;}
  void call(GuiContext ctx){RenderHandler.getInstance().onExtractGuiOverlayPost(ctx,0,InactiveProfiler.INSTANCE);}
  long gc(){return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();}
  void metric(String name){Collections.sort(nanos);Collections.sort(bytes);check(worldCalls-startCalls==120,"one actual world callback each measured frame "+name);System.out.println("PREVIEW_METRIC mode="+name+" frames=120 parses="+(parses-startParses)+" samples="+nanos.size()+" extract_cpu_p50_ns="+nanos.get(nanos.size()/2)+" extract_cpu_p99_ns="+nanos.get(nanos.size()-1)+" allocation_p50_bytes="+bytes.get(bytes.size()/2)+" gc_collections="+(gc()-startGc));if(EXPECT_CACHE&&name.equals("optimized"))check(parses-startParses<=2,"held world path decodes stable contents at most twice across 120 frames");nanos.clear();bytes.clear();}
  public void extractRenderState(GuiGraphicsExtractor gui,int mx,int my,float delta){var mc=Minecraft.getInstance();try{
   var ctx=GuiContext.fromGuiGraphics(gui);MinecraftClientCompat.setScreen(mc,null);var aim=Vec3.atCenterOf(owner.chest).subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-aim.x,aim.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(aim.y,Math.sqrt(aim.x*aim.x+aim.z*aim.z))));MAIN.setBooleanValue(true);PREVIEW.setBooleanValue(true);held(true);
   if(!ready){Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(false);call(ctx);ready=last!=null&&last.inv()!=null&&last.inv().getItem(0).is(Items.DIAMOND)&&last.inv().getItem(26).is(Items.EMERALD)&&last.inv().getItem(1).has(net.minecraft.core.component.DataComponents.CONTAINER);if(warmDeadline==0)warmDeadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);if(!ready&&System.nanoTime()>warmDeadline)throw new AssertionError("native heavy chest never synchronized calls="+worldCalls+" held="+KEY.isKeybindHeld()+" target="+last);if(ready)check(true,"real world chest contains 25 filled shulker boxes");return;}
   if(frame==120)metric("original");if(frame==240){metric("optimized");tests(ctx);owner.stage=4;MAIN.setBooleanValue(false);Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(true);return;}
   if(frame%120==0){Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(frame>=120);io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.clear();startParses=parses;startCalls=worldCalls;startGc=gc();}
   long id=Thread.currentThread().threadId(),allocation=bean.getThreadAllocatedBytes(id),time=System.nanoTime();call(ctx);long elapsed=System.nanoTime()-time,allocated=bean.getThreadAllocatedBytes(id)-allocation;
   if(frame%120>=30){nanos.add(elapsed);bytes.add(allocated);}check(last.inv().getItem(0).getCount()==17&&last.inv().getItem(26).getCount()==5,"world contents preserved frame="+frame);frame++;
  }catch(Throwable e){throw new IllegalStateException("World preview profiling frame="+frame,e);}finally{if(owner.stage==3){MAIN.setBooleanValue(false);try{held(false);}catch(Exception e){throw new IllegalStateException(e);}MinecraftClientCompat.setScreen(mc,this);}}}
  void tests(GuiContext ctx)throws Exception {
   check(!io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.active(),"world scope exits after every callback");
   var data=new fi.dy.masa.malilib.util.data.tag.CompoundData();var items=new fi.dy.masa.malilib.util.data.tag.ListData();
   var entry=new fi.dy.masa.malilib.util.data.tag.CompoundData();entry.putString("id","minecraft:diamond");entry.putInt("count",3);entry.putByte("Slot",(byte)0);items.add(entry);data.put("Items",items);
   var registry=Minecraft.getInstance().level.registryAccess();
   Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(true);
   io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.enter();try{
    var a=fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(data,27,registry);int before=parses;
    var b=fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(data,27,registry);
    check(a.getContainerSize()==27&&b.getItem(0).getCount()==3,"parser preserves size and content");
    System.out.println("PREVIEW_CACHE_REPEAT_PARSES "+(parses-before));
    if(EXPECT_CACHE)check(parses==before,"identical world data reuses parsed inventory");
    entry.putInt("count",9);var c=fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(data,27,registry);check(c.getItem(0).getCount()==9,"same-object data mutation updates without waiting for tick");
    var d=fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(data,54,registry);check(d.getContainerSize()==54,"different requested slot count has separate result");
    items.clear();var empty=fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(data,27,registry);check(empty.isEmpty()&&empty.getContainerSize()==27,"removing all items invalidates cached contents");
    var missing=new fi.dy.masa.malilib.util.data.tag.CompoundData();check(fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(missing,27,registry)==null,"missing inventory data retains upstream null result");
   }finally{io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.exit();}
   int before=parses;fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(data,27,registry);fi.dy.masa.malilib.util.InventoryUtils.getDataInventory(data,27,registry);check(parses==before+2,"calls outside MiniHUD retain original behavior");
   Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(false);before=parses;call(ctx);call(ctx);check(parses>before,"runtime disable restores world parsing immediately");Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(true);
   var mc=Minecraft.getInstance();
   mc.getSingleplayerServer().submit(()->{var actor=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var box=(ChestBlockEntity)actor.level().getBlockEntity(owner.chest);box.setItem(0,new ItemStack(Items.GOLD_INGOT,11));fi.dy.masa.minihud.data.EntityDataManager.getInstance().refreshBlockEntityFromWorld(actor.level(),owner.chest);}).get(5,java.util.concurrent.TimeUnit.SECONDS);
   call(ctx);check(last!=null&&last.inv().getItem(0).is(Items.GOLD_INGOT)&&last.inv().getItem(0).getCount()==11,"actual held preview reflects changed authoritative world data on next call");
   mc.player.setYRot(180);mc.getSingleplayerServer().submit(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setYRot(180)).get(5,java.util.concurrent.TimeUnit.SECONDS);mc.hitResult=BlockHitResult.miss(mc.player.getEyePosition(),Direction.SOUTH,mc.player.blockPosition());call(ctx);check(last==null,"looking away does not retain cached world target");
   mc.player.setYRot(0);mc.getSingleplayerServer().submit(()->mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setYRot(0)).get(5,java.util.concurrent.TimeUnit.SECONDS);mc.hitResult=new BlockHitResult(Vec3.atCenterOf(owner.chest),Direction.NORTH,owner.chest,false);call(ctx);check(last!=null&&last.inv().getItem(0).is(Items.GOLD_INGOT),"looking back resumes current world contents");
  }
 }
}
'''

PARSE_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=NbtInventory.class,remap=false) public class ProbeMixin {
 @Inject(method="fromDataList(Lfi/dy/masa/malilib/util/data/tag/ListData;ZLnet/minecraft/core/RegistryAccess;)Lfi/dy/masa/malilib/util/nbt/NbtInventory;",at=@At("HEAD")) private static void count(CallbackInfoReturnable<?> ci){PreviewSmoke.parses++;}
}
'''


def main():
    baseline = '--baseline' in sys.argv
    if baseline:
        sys.argv.remove('--baseline')
    independent.SOURCE = SOURCE.replace('EXPECT_CACHE=true', 'EXPECT_CACHE=false') if baseline else SOURCE
    independent.TEXT_PROBE = PARSE_PROBE
    folder = harness.ROOT / 'build/minihud-preview-tests'
    before = set(folder.iterdir()) if folder.exists() else set()
    result = independent.main()
    for directory in set(folder.iterdir()) - before:
        report = directory / 'report.json'
        if report.exists():
            data = json.loads(report.read_text(encoding='utf-8'))
            data['test_kind'] = 'MiniHUD held world-container preview: real chest containing 25 filled shulker boxes'
            data['baseline_only'] = baseline
            data['measurement'] = 'CPU extraction and render-thread allocation for actual held world callback; not end-to-end FPS'
            report.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    return result


if __name__ == '__main__':
    raise SystemExit(main())

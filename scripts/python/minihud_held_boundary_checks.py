"""Behavior checks for reusable held screens and mutable content; shared by game fixtures."""

CHECKS = r'''
   heldBoundaries(ctx);
'''

METHOD = r'''
  void heldBoundaries(GuiContext ctx)throws Exception {
   var handler=fi.dy.masa.minihud.renderer.InventoryOverlayHandler.getInstance();
   var cache=io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.getInstance();
   io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.clear();
   call(ctx);int built=builds;int compared=comparisons;for(int i=0;i<30;i++)call(ctx);
   check(builds-built<=2,"stable same-tick frames reuse prepared world context");check(comparisons-compared<10000,"stable same-tick frames avoid full inventory comparisons");
   io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.receivedData();built=builds;call(ctx);check(builds>built,"data reply invalidates prepared context in current tick");
   built=builds;cache.onClientTick(Minecraft.getInstance());call(ctx);check(builds>built,"next tick validates local mutable contents");
   int[] refresh={0};
   var inv=new SimpleContainer(54);inv.setItem(0,new ItemStack(Items.DIAMOND,3));inv.setItem(53,new ItemStack(Items.EMERALD,8));
   var data=new fi.dy.masa.malilib.util.data.tag.CompoundData();
   var context=new InventoryOverlayContext(InventoryOverlayType.GENERIC,inv,null,null,data,(old,level)->{refresh[0]++;return old;});
   io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.enterWorld();try{
    io.github.halfmasa.xaerobinding.feature.minihud.HeldWorldPreview.draw(handler,ctx,context,true,false);
    int x=gridX,y=gridY,columns=gridColumns;check(gridInventory==inv&&gridInventory.getContainerSize()==54&&gridInventory.getItem(53).getCount()==8,"reused held screen keeps all 54 slots and holes");
    InventoryOverlay.getInventoryPropsTemp(InventoryOverlayType.FURNACE,3);
    for(int i=0;i<8;i++)io.github.halfmasa.xaerobinding.feature.minihud.HeldWorldPreview.draw(handler,ctx,context,true,false);
    check(gridX==x&&gridY==y&&gridColumns==columns,"another preview mutating shared scratch layout cannot move held grid");
    check(refresh[0]==0,"screen reuse does not introduce extra native refresh requests");check(inv.getItem(0).getCount()==3&&inv.getItem(53).getCount()==8,"frame drawing does not mutate source inventory");
    inv.getItem(0).setCount(12);io.github.halfmasa.xaerobinding.feature.minihud.HeldWorldPreview.draw(handler,ctx,context,true,false);check(gridInventory.getItem(0).getCount()==12,"live container count changes remain visible");
    var source=inv.getItem(0);var display=io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.drawCopy(source,source::copy);check(display!=source&&io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.drawCopy(source,source::copy)==display,"drawing reuses an independent display snapshot");
    source.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Changed component"));io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.receivedData();display=io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.drawCopy(source,source::copy);check(display.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).getString().equals("Changed component"),"data update refreshes display components within the same tick");
    display.setCount(1);display=io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.drawCopy(source,source::copy);check(source.getCount()==12&&display.getCount()==12,"display count mutation cannot alter inventory and is repaired on next draw");
    for(InventoryOverlayType type:List.of(InventoryOverlayType.FURNACE,InventoryOverlayType.BREWING_STAND,InventoryOverlayType.CRAFTER)){
     int size=type==InventoryOverlayType.FURNACE?3:type==InventoryOverlayType.BREWING_STAND?5:9;
     var special=new SimpleContainer(size);special.setItem(size-1,new ItemStack(Items.DIAMOND,7));var specialData=new fi.dy.masa.malilib.util.data.tag.CompoundData();specialData.putString("id","minecraft:"+(type==InventoryOverlayType.FURNACE?"furnace":type==InventoryOverlayType.BREWING_STAND?"brewing_stand":"crafter"));if(type==InventoryOverlayType.CRAFTER)specialData.putIntArray("disabled_slots",new int[]{1,4});
     var specialContext=new InventoryOverlayContext(type,special,null,null,specialData,(old,level)->old);
     io.github.halfmasa.xaerobinding.feature.minihud.HeldWorldPreview.draw(handler,ctx,specialContext,true,false);
     check(gridType==type&&gridInventory.getContainerSize()==size&&gridInventory.getItem(size-1).getCount()==7,"held special layout retained "+type);
     if(type==InventoryOverlayType.CRAFTER){check(gridDisabled.equals(Set.of(1,4)),"held crafter disabled slots retained");specialData.putIntArray("disabled_slots",new int[]{2});io.github.halfmasa.xaerobinding.feature.minihud.HeldWorldPreview.draw(handler,ctx,specialContext,true,false);check(gridDisabled.equals(Set.of(2)),"held crafter disabled slots update without rebuilding layout");}
    }
    io.github.halfmasa.xaerobinding.feature.minihud.HeldWorldPreview.draw(handler,ctx,context,false,true);check(gridInventory==inv&&gridColumns==9,"background option changes rebuild the screen with correct grid");
   }finally{io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.exitWorld();}
   check(!io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache.worldActive(),"held render scope restored after boundary checks");
  }
'''

GRID_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import java.util.Set;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import fi.dy.masa.malilib.render.*;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=InventoryOverlay.class,remap=false) public class HeldGridProbeMixin {
 @Inject(method="renderInventoryStacks(Lfi/dy/masa/malilib/render/GuiContext;Lfi/dy/masa/malilib/render/InventoryOverlayType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;DD)V",at=@At("HEAD")) private static void grid(GuiContext ctx,InventoryOverlayType type,Container inv,int x,int y,int row,int start,int max,Set<Integer> disabled,double mx,double my,CallbackInfo ci){PreviewSmoke.gridInventory=inv;PreviewSmoke.gridType=type;PreviewSmoke.gridX=x;PreviewSmoke.gridY=y;PreviewSmoke.gridColumns=row;PreviewSmoke.gridDisabled=disabled;}
}
'''


def extend(source):
    source = source.replace('public static int worldCalls,', 'public static net.minecraft.world.Container gridInventory;public static InventoryOverlayType gridType;public static int gridX,gridY,gridColumns;public static Set<Integer> gridDisabled;public static int worldCalls,')
    source = source.replace('void tests(GuiContext ctx)throws Exception {', 'void tests(GuiContext ctx)throws Exception {' + CHECKS)
    source = source.replace('  void tests(GuiContext ctx)', METHOD + '  void tests(GuiContext ctx)')
    return source

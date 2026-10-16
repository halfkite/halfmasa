"""Exercise the combined waypoint grid with real Conflux Map client/server data."""
import argparse
from datetime import datetime
import json, os, shutil, socket, subprocess, time, zipfile
from pathlib import Path
import run_startup_smoke as startup
ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''
package io.github.halfmasa.confluxgridsmoke;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import cn.net.rms.confluxmap.ConfluxMapClient;
import cn.net.rms.confluxmap.core.waypoint.*;
import cn.net.rms.confluxmap.mc.ui.screen.*;
public class ConfluxGridSmoke implements ClientModInitializer {
 private static ConfluxGridSmoke instance;
 public static int hoverX=-1,hoverY=-1;
 private volatile boolean packReady;private int stage,checks;private long since;private volatile boolean ready;private volatile Throwable serverError;
 private WaypointListScreen ui;private WaypointStore store;private UUID personalId;private ConfluxWaypointScreenBridge.Row teleportTarget;
 private final List<UUID> ids=new ArrayList<>();
 @Override public void onInitializeClient(){instance=this;}
 private void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);System.out.println("CONFLUX_GRID_CHECK "+why);}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private static Field field(Class<?> type,String name)throws Exception{for(Class<?> t=type;t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
 private static Method method(Class<?> type,String name,Class<?>...args)throws Exception{for(Class<?> t=type;t!=null;t=t.getSuperclass())try{var m=t.getDeclaredMethod(name,args);m.setAccessible(true);return m;}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(name);}
 private void next(Minecraft mc,int value){stage=value;since=mc.level==null?0:mc.level.getGameTime();System.out.println("CONFLUX_GRID_STAGE "+stage);}
 private void open(Minecraft mc)throws Exception{ui=new WaypointListScreen();MinecraftClientCompat.setScreen(mc,ui);method(ui.getClass(),"rebuild").invoke(ui);check(field(ui.getClass(),"halfmasa$active").getBoolean(ui),"combined native screen adapter active");}
 @SuppressWarnings("unchecked") private List<ConfluxWaypointScreenBridge.Row> rows()throws Exception{return (List<ConfluxWaypointScreenBridge.Row>)field(ui.getClass(),"halfmasa$rows").get(ui);}
 private WaypointGridLayout layout()throws Exception{return (WaypointGridLayout)field(ui.getClass(),"halfmasa$layout").get(ui);}
 private ConfluxWaypointScreenBridge bridge()throws Exception{return (ConfluxWaypointScreenBridge)field(ui.getClass(),"halfmasa$bridge").get(ui);}
 private void rebuild()throws Exception{method(ui.getClass(),"rebuild").invoke(ui);}
 private List<String> textAt(Minecraft mc,int x,int y){hoverX=x;hoverY=y;var state=new GuiRenderState();ui.extractRenderState(new GuiGraphicsExtractor(mc,state,x,y),x,y,0);var texts=new ArrayList<String>();state.forEachText(t->{try{var seq=(net.minecraft.util.FormattedCharSequence)field(t.getClass(),"text").get(t);var b=new StringBuilder();seq.accept((i,s,c)->{b.appendCodePoint(c);return true;});texts.add(b.toString());}catch(Exception e){throw new AssertionError(e);}});return texts;}
 private void pure(){
  for(int count:new int[]{0,1,5,7,107,1000000})for(int width:new int[]{0,60,148,308,920})for(int height:new int[]{0,21,78,360})for(boolean compact:new boolean[]{true,false}){
   var grid=WaypointGridLayout.create(count,16,128,width,height,compact,9,Integer.MAX_VALUE);check(grid.scroll()<=grid.maxScroll(),"scroll clamped");
   int expected=grid.scroll()*grid.columns();for(var cell:grid.cells()){check(cell.index()==expected++,"contiguous positions across sources");check(grid.at(cell.x()+.5,cell.y()+.5)==cell&&cell.x()>=16&&cell.x()+cell.width()<=16+width,"geometry and hit testing agree");}
  }
 }
 private void hover(Minecraft mc,ConfluxWaypointScreenBridge.Row target)throws Exception{textAt(mc,-100,-100);var cell=layout().cells().stream().filter(c->rowsUnchecked().get(c.index()).sameIdentity(target)).findFirst().orElseThrow();hoverX=cell.x()+30;hoverY=cell.y()+10;textAt(mc,hoverX,hoverY);}
 @SuppressWarnings("unchecked") private List<AbstractWidget> actions()throws Exception{return (List<AbstractWidget>)field(ui.getClass(),"halfmasa$actions").get(ui);}
 private Button action(String key)throws Exception{String label=net.minecraft.network.chat.Component.translatable(key).getString();for(var a:actions())if(a instanceof Button b){var fitted=(net.minecraft.network.chat.Component)method(ui.getClass(),"fitButtonLabel",net.minecraft.network.chat.Component.class,int.class).invoke(ui,net.minecraft.network.chat.Component.translatable(key),a.getWidth());if(a.getMessage().getString().equals(label)||a.getMessage().getString().equals(fitted.getString()))return b;}throw new AssertionError("No native action "+label+" "+actions().stream().map(a->a.getMessage().getString()).toList());}
 private List<AbstractWidget> actionsUnchecked(){try{return actions();}catch(Exception e){throw new AssertionError(e);}}
 private void click(Button button)throws Exception{hoverX=button.getX()+Math.min(5,button.getWidth()-1);hoverY=button.getY()+5;ui.mouseClicked(new MouseButtonEvent(hoverX,hoverY,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);}
 private void clickAction(String key)throws Exception{click(action(key));}
 private Button top(String key)throws Exception{String label=net.minecraft.network.chat.Component.translatable(key).getString();return (Button)ui.children().stream().filter(w->w instanceof Button b&&b.visible&&b.getY()==28&&b.getMessage().getString().equals(label)).findFirst().orElseThrow();}
 private void tick(Minecraft mc){try{
  if(serverError!=null)throw new AssertionError(serverError);
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;pure();for(var flag:List.of(Configs.CONFLUX_MAP_EXTENSIONS,Configs.CONFLUX_MAP_ALL_DIMENSIONS,Configs.CONFLUX_MAP_PUBLIC_WAYPOINTS,Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS,Configs.CONFLUX_MAP_WAYPOINT_COMPACT,Configs.CONFLUX_MAP_WAYPOINT_DIMENSION,Configs.CONFLUX_MAP_WAYPOINT_DISTANCE,Configs.CONFLUX_MAP_CLOSE_AFTER_TELEPORT))check(!flag.getDefaultBooleanValue(),"Conflux extension defaults off: "+flag.getName());var c=ConfluxMapClient.get().config();c.predictionEnabled=false;c.updateCheckEnabled=false;c.surveyReminderDismissed=true;c.minimapEnabled=false;
   String address=System.getProperty("halfmasa.conflux.fixture.address");stage=1;ConnectScreen.startConnecting(MinecraftClientCompat.getScreen(mc),mc,ServerAddress.parseString(address),new ServerData("Conflux grid fixture",address,ServerData.Type.OTHER),false,null);return;}
  if(mc.player==null||mc.level==null)return;
  if(stage==1){if(mc.level.getGameTime()<50)return;var app=ConfluxMapClient.get();if(app.sharedWaypoints().list().size()<7||!app.sharedWaypoints().isSynchronized())return;store=app.waypointService().current();if(store==null||!store.persistenceWritable())return;MinecraftClientCompat.setScreen(mc,null);
   var dim=app.gameBridge().session().dimension();for(int i=0;i<100;i++){var id=UUID.randomUUID();ids.add(id);var wp=new Waypoint(id,String.format(Locale.ROOT,"Home %03d",i),dim,mc.player.getX()+i*2,mc.player.getY(),mc.player.getZ()+8,0xFF55BB66,"",true,false,Waypoint.Type.NORMAL,System.currentTimeMillis());if(i%3==0)wp.iconItemId="minecraft:diamond_pickaxe";store.add(wp);}personalId=ids.getFirst();
   Configs.CONFLUX_MAP_EXTENSIONS.setBooleanValue(true);Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(true);Configs.CONFLUX_MAP_WAYPOINT_ORDER.resetToDefault();Configs.CONFLUX_MAP_WAYPOINT_COMPACT.resetToDefault();
   ready=true;next(mc,2);return;}
  if(mc.level.getGameTime()-since>600)throw new AssertionError("stage timeout "+stage);
  switch(stage){
   case 2->{if(!ready||ConfluxMapClient.get().sharedWaypoints().list().size()<7)return;
    check(true,"seven real server waypoints synchronized");Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(false);ui=new WaypointListScreen();MinecraftClientCompat.setScreen(mc,ui);check(!field(ui.getClass(),"halfmasa$active").getBoolean(ui),"initial native list unchanged when extension off");
    Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(true);ui.tick();check(field(ui.getClass(),"halfmasa$active").getBoolean(ui),"live enable initializes adapter");check(rows().size()==107&&field(ui.getClass(),"totalRowCount").getInt(ui)==107,"native count includes both sources");
    check(rows().subList(0,7).stream().allMatch(r->r.source().equals("server"))&&rows().get(7).source().equals("personal"),"default server-first ordering");
    var nativeRows=(List<?>)field(ui.getClass(),"rows").get(ui);check(method(nativeRows.getFirst().getClass(),"id").invoke(nativeRows.getFirst()).equals(rows().getFirst().id()),"detailed native row model uses merged order");
    check(ui.children().stream().anyMatch(w->w instanceof Button b&&b.visible&&b.active&&b.getY()>=128&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("confluxmap.screen.waypoints.locate").getString())),"detailed mode retains original native actions");
    check(textAt(mc,-100,-100).contains(net.minecraft.network.chat.Component.translatable("confluxmap.screen.waypoints.delete").getString()),"detailed mode renders original delete button");
    Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(true);ui.tick();var grid=layout();check(grid.columns()>1&&grid.cells().get(7).x()>grid.left(),"continuous compact grid fills source boundary");
    check(textAt(mc,-100,-100).stream().noneMatch(t->t.equals(net.minecraft.network.chat.Component.translatable("confluxmap.screen.waypoints.teleport").getString())),"idle compact cards hide actions");
    var target=rows().get(7);hover(mc,target);check(actions().size()==7,"personal popup includes selection and all six native actions");check(actions().stream().allMatch(w->w.getClass().getName().endsWith("ConfluxTextButton")),"popup buttons retain native resource theme implementation");
    var panel=(WaypointGridLayout.Cell)field(ui.getClass(),"halfmasa$panel").get(ui);textAt(mc,panel.x()+10,panel.y()+10);check(((ConfluxWaypointScreenBridge.Row)field(ui.getClass(),"halfmasa$hover").get(ui)).sameIdentity(target),"pointer can enter popup without changing waypoint");check(layout().equals(grid),"popup does not shift grid geometry");
    var nativeDimension=bridge().dimensionText(target);var nativeDistance=bridge().distanceText(target);check(!nativeDistance.endsWith("m"),"distance uses original localization instead of hardcoded meters");
    Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.setBooleanValue(true);Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.setBooleanValue(true);ui.tick();check(layout().cells().getFirst().height()>grid.cells().getFirst().height(),"optional information increases card height safely");check(textAt(mc,-100,-100).contains(nativeDimension+" · "+nativeDistance),"dimension and distance options use native text");
    hover(mc,rows().get(7));next(mc,3);}
   case 3->{if(mc.level.getGameTime()-since<5)return;net.minecraft.client.Screenshot.grab(mc,false);
    var orderButton=(Button)ui.children().stream().filter(w->w instanceof Button b&&b.visible&&b.getY()==28&&b.getMessage().getString().equals(ConfluxMapWaypointOrder.SERVER_FIRST.getDisplayName())).findFirst().orElseThrow();click(orderButton);
    check(rows().getFirst().source().equals("personal")&&rows().get(100).source().equals("server"),"top native order button switches source order");
    click(top("halfmasa.conflux_map.compact_on"));check(!Configs.CONFLUX_MAP_WAYPOINT_COMPACT.getBooleanValue(),"native mode button disables compact mode");
    check(field(ui.getClass(),"halfmasa$panel").get(ui)==null&&ui.children().stream().anyMatch(w->w instanceof Button b&&b.visible&&b.getY()>=128&&b.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("confluxmap.screen.waypoints.delete").getString())),"disabling compact restores original full native list");hoverX=-1;hoverY=-1;next(mc,4);}
   case 4->{if(mc.level.getGameTime()-since<5)return;net.minecraft.client.Screenshot.grab(mc,false);
    Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(true);Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.setBooleanValue(false);Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.setBooleanValue(false);Configs.CONFLUX_MAP_WAYPOINT_ORDER.setOptionListValue(ConfluxMapWaypointOrder.SERVER_FIRST);rebuild();
    var search=(EditBox)field(ui.getClass(),"searchField").get(ui);search.setValue("Home 099");ui.tick();check(rows().size()==1&&rows().getFirst().name().equals("Home 099"),"native search filters merged grid");search.setValue("");ui.tick();check(rows().size()==107,"clearing search restores sources");
    var grid=layout();ui.mouseScrolled(grid.left()+10,grid.top()+10,0,-1);check(layout().scroll()==1,"single grid scroll range");field(ui.getClass(),"halfmasa$scroll").setInt(ui,Integer.MAX_VALUE);rebuild();int old=layout().maxScroll();for(int i=60;i<100;i++)store.remove(ids.get(i));rebuild();check(layout().scroll()==layout().maxScroll()&&layout().maxScroll()<old,"deletion clamps and fills grid");field(ui.getClass(),"halfmasa$scroll").setInt(ui,0);rebuild();
    var target=rows().stream().filter(r->r.id().equals(personalId)).findFirst().orElseThrow();hover(mc,target);clickAction("confluxmap.screen.waypoints.hide");check(store.list().stream().anyMatch(w->w.id.equals(personalId)&&!w.visible),"native popup hide persists visibility");check(((ConfluxWaypointScreenBridge.Row)field(ui.getClass(),"halfmasa$hover").get(ui)).id().equals(personalId),"visibility rebuild preserves popup identity");clickAction("confluxmap.screen.waypoints.show");check(store.list().stream().anyMatch(w->w.id.equals(personalId)&&w.visible),"native popup show restores visibility");
    clickAction("confluxmap.screen.waypoints.share");check(MinecraftClientCompat.getScreen(mc).getClass().getSimpleName().equals("WaypointShareConfirmScreen"),"native popup share opens original confirmation");MinecraftClientCompat.setScreen(mc,ui);rebuild();hover(mc,rows().stream().filter(r->r.id().equals(personalId)).findFirst().orElseThrow());clickAction("confluxmap.screen.waypoints.locate");check(MinecraftClientCompat.getScreen(mc) instanceof FullscreenMapScreen,"native popup locate opens fullscreen map");MinecraftClientCompat.setScreen(mc,ui);rebuild();hover(mc,rows().stream().filter(r->r.id().equals(personalId)).findFirst().orElseThrow());
    clickAction("confluxmap.screen.waypoints.edit");check(MinecraftClientCompat.getScreen(mc) instanceof WaypointEditScreen,"native popup edit opens editor");var editor=MinecraftClientCompat.getScreen(mc);check(field(editor.getClass(),"editingId").get(editor).equals(personalId),"native editor has stable identity");((EditBox)field(editor.getClass(),"nameField").get(editor)).setValue("Home Updated");method(editor.getClass(),"onDone").invoke(editor);check(store.list().stream().anyMatch(w->w.id.equals(personalId)&&w.name.equals("Home Updated")),"native editor persists personal change");MinecraftClientCompat.setScreen(mc,ui);rebuild();
    hover(mc,rows().stream().filter(r->r.id().equals(personalId)).findFirst().orElseThrow());click((Button)actions().getFirst());check(((Set<?>)field(ui.getClass(),"selectedWaypointIds").get(ui)).contains(personalId),"popup retains native waypoint selection and batch toolbar");click((Button)actions().getFirst());check(!((Set<?>)field(ui.getClass(),"selectedWaypointIds").get(ui)).contains(personalId),"native popup checkbox clears selection");
    var deleteTarget=rows().stream().filter(r->r.id().equals(ids.get(10))).findFirst().orElseThrow();hover(mc,deleteTarget);clickAction("confluxmap.screen.waypoints.delete");check(store.list().stream().anyMatch(w->w.id.equals(ids.get(10))),"first delete only requests original confirmation");check(((ConfluxWaypointScreenBridge.Row)field(ui.getClass(),"halfmasa$hover").get(ui)).id().equals(ids.get(10)),"delete confirmation stays on same popup");clickAction("confluxmap.screen.waypoints.confirm");check(store.list().stream().noneMatch(w->w.id.equals(ids.get(10))),"confirmed native delete removes correct waypoint");
    Configs.CONFLUX_MAP_WAYPOINT_ORDER.setOptionListValue(ConfluxMapWaypointOrder.PERSONAL_FIRST);ui.init(300,240);rebuild();var narrowTarget=rows().getFirst();hover(mc,narrowTarget);var narrowPanel=(WaypointGridLayout.Cell)field(ui.getClass(),"halfmasa$panel").get(ui);check(narrowPanel.x()>=0&&narrowPanel.x()+narrowPanel.width()<=300&&actions().stream().allMatch(w->w.getX()>=narrowPanel.x()&&w.getX()+w.getWidth()<=narrowPanel.x()+narrowPanel.width()),"narrow popup wraps native controls within window");clickAction("confluxmap.screen.waypoints.hide");check(store.list().stream().anyMatch(w->w.id.equals(narrowTarget.id())&&!w.visible),"popup clicks have priority over underlying narrow-screen controls");clickAction("confluxmap.screen.waypoints.show");Configs.CONFLUX_MAP_WAYPOINT_ORDER.setOptionListValue(ConfluxMapWaypointOrder.SERVER_FIRST);ui.init(960,540);rebuild();
    hover(mc,rows().getFirst());check(actions().size()==5,"shared popup exposes only original shared actions");clickAction("confluxmap.screen.waypoint.edit");check(MinecraftClientCompat.getScreen(mc) instanceof WaypointEditScreen&&field(MinecraftClientCompat.getScreen(mc).getClass(),"editingShared").get(MinecraftClientCompat.getScreen(mc))!=null,"native shared edit uses correct permission path");MinecraftClientCompat.setScreen(mc,ui);rebuild();teleportTarget=rows().getFirst();hover(mc,teleportTarget);check(action("confluxmap.screen.waypoints.teleport").active==bridge().teleportAccess(teleportTarget).available(),"popup native teleport permission preserved");clickAction("confluxmap.screen.waypoints.teleport");next(mc,5);}
   case 5->{var entry=teleportTarget.renderEntry();double x=((Number)method(entry.getClass(),"x").invoke(entry)).doubleValue(),z=((Number)method(entry.getClass(),"z").invoke(entry)).doubleValue();if(Math.abs(mc.player.getX()-x)>.6||Math.abs(mc.player.getZ()-z)>.6)return;check(true,"popup native teleport reaches real server waypoint");MinecraftClientCompat.setScreen(mc,ui);rebuild();
    var target=rows().stream().filter(r->r.id().equals(personalId)).findFirst().orElseThrow();store.remove(personalId);bridge().act(target,true,true);check(MinecraftClientCompat.getScreen(mc)==ui,"deleted stale identity cannot edit neighbor");rebuild();
    Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.setBooleanValue(true);Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.setBooleanValue(true);Configs.CONFLUX_MAP_WAYPOINT_COMPACT.getKeybind().setValueFromString("F6");new Configs().save();Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.setBooleanValue(false);Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.setBooleanValue(false);Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(false);Configs.CONFLUX_MAP_WAYPOINT_COMPACT.getKeybind().setValueFromString("");new Configs().load();check(Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.getBooleanValue()&&Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.getBooleanValue()&&Configs.CONFLUX_MAP_WAYPOINT_COMPACT.getBooleanValue()&&Configs.CONFLUX_MAP_WAYPOINT_COMPACT.getKeybind().getStringValue().equals("F6"),"new options and independent shortcut persist");
    Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(false);ui.tick();check(!field(ui.getClass(),"halfmasa$active").getBoolean(ui)&&ui.children().stream().anyMatch(w->w instanceof AbstractWidget a&&a.getY()>=128&&a.visible&&a.active),"extension off restores original native screen");
    Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS.setBooleanValue(true);Configs.CONFLUX_MAP_WAYPOINT_COMPACT.setBooleanValue(true);Configs.CONFLUX_MAP_WAYPOINT_DIMENSION.setBooleanValue(false);Configs.CONFLUX_MAP_WAYPOINT_DISTANCE.setBooleanValue(false);ui.tick();hoverX=-1;hoverY=-1;next(mc,6);}
   case 6->{if(mc.level.getGameTime()-since<5)return;net.minecraft.client.Screenshot.grab(mc,false);
    packReady=false;mc.getResourcePackRepository().reload();mc.getResourcePackRepository().setSelected(List.of("file/conflux-native-style-test.zip"));mc.reloadResourcePacks().whenComplete((unused,error)->{serverError=error;packReady=true;});next(mc,7);}
   case 7->{if(!packReady||mc.gui.overlay()!=null)return;check(ConfluxMapClient.get().uiResourceTheme().useVanillaButtonStyle(),"actual resource pack reload activates native button skin");MinecraftClientCompat.setScreen(mc,ui);rebuild();hover(mc,rows().stream().filter(r->r.local()!=null).findFirst().orElseThrow());check(actions().stream().allMatch(w->w.getClass().getName().endsWith("ConfluxTextButton")),"all popup controls follow reloaded native button theme");next(mc,8);}
   case 8->{if(mc.level.getGameTime()-since<5)return;net.minecraft.client.Screenshot.grab(mc,false);packReady=false;mc.getResourcePackRepository().setSelected(List.of());mc.reloadResourcePacks().whenComplete((unused,error)->{serverError=error;packReady=true;});next(mc,9);}
   case 9->{if(!packReady||mc.gui.overlay()!=null||mc.level.getGameTime()-since<20)return;check(!ConfluxMapClient.get().uiResourceTheme().useVanillaButtonStyle(),"removing resource pack restores native Conflux style");for(var flag:List.of(Configs.CONFLUX_MAP_EXTENSIONS,Configs.CONFLUX_MAP_SHOW_BOTH_WAYPOINTS,Configs.CONFLUX_MAP_WAYPOINT_COMPACT,Configs.CONFLUX_MAP_WAYPOINT_DIMENSION,Configs.CONFLUX_MAP_WAYPOINT_DISTANCE))flag.resetToDefault();new Configs().save();System.out.println("CONFLUX_GRID_WORLD_PASS checks="+checks);stage=10;mc.stop();}

  }
 }catch(Throwable error){throw new IllegalStateException("Conflux grid smoke stage="+stage,error);}}
 private List<ConfluxWaypointScreenBridge.Row> rowsUnchecked(){try{return rows();}catch(Exception e){throw new AssertionError(e);}}
}
'''
FRAME = r'''package io.github.halfmasa.confluxgridsmoke.mixin;
import io.github.halfmasa.confluxgridsmoke.ConfluxGridSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class) public class FrameMixin { @Inject(method="runTick",at=@At("TAIL")) private void frame(CallbackInfo ci){ConfluxGridSmoke.frame((Minecraft)(Object)this);} }
'''
HOVER = r'''package io.github.halfmasa.confluxgridsmoke.mixin;
import io.github.halfmasa.confluxgridsmoke.ConfluxGridSmoke;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="cn.net.rms.confluxmap.mc.ui.screen.ConfluxScreen",remap=false) public class HoverMixin {
 @ModifyVariable(method="extractRenderState",at=@At("HEAD"),argsOnly=true,ordinal=0,remap=false) private int x(int original){return ConfluxGridSmoke.hoverX>=0?ConfluxGridSmoke.hoverX:original;}
 @ModifyVariable(method="extractRenderState",at=@At("HEAD"),argsOnly=true,ordinal=1,remap=false) private int y(int original){return ConfluxGridSmoke.hoverY>=0?ConfluxGridSmoke.hoverY:original;}
}
'''

SERVER = r'''package io.github.halfmasa.confluxgridsmoke;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
public class ServerFixture implements DedicatedServerModInitializer {
 private boolean ready;
 @Override public void onInitializeServer(){ServerTickEvents.END_SERVER_TICK.register(server->{
  if(ready||server.getPlayerList().getPlayers().isEmpty())return;ready=true;
  var p=server.getPlayerList().getPlayers().getFirst();
  server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"op HalfMasaSmoke");
  var original=p.position();server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"confluxmap waypoints enable");
  for(int i=0;i<7;i++){p.setPos(original.x+2*i,original.y,original.z+4);server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"confluxmap waypoints add Server "+i);}
  p.setPos(original.x,original.y,original.z);System.out.println("CONFLUX_GRID_SERVER_READY");
 });}
}
'''

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--jvm-arg", action="append", default=[])
    parser.add_argument("--extra-mod", action="append", type=Path, default=[])
    parser.add_argument("--conflux", type=Path, default=Path("D:/我的世界/.minecraft/versions/26.3-Fabric 0.19.5/mods/confluxmap-26.3-0.1.11-beta.1.jar"))
    parser.set_defaults(with_api=True)
    parser.add_argument("--timeout", type=int, default=480)
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    args.version = "26.3"
    directory = ROOT / "build/conflux-grid-tests" / datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    directory.mkdir(parents=True)
    classes = directory / "classes"
    classes.mkdir()
    dependencies = directory / "dependencies"
    dependencies.mkdir()
    with zipfile.ZipFile(args.conflux) as jar:
        for name in jar.namelist():
            if name.startswith("META-INF/jars/") and name.endswith(".jar"):
                (dependencies / Path(name).name).write_bytes(jar.read(name))
    sources = []
    for name, source in [("ConfluxGridSmoke", SOURCE), ("FrameMixin", FRAME), ("HoverMixin", HOVER), ("ServerFixture", SERVER)]:
        path = directory / (name + ".java")
        path.write_text(source, encoding="utf-8")
        sources.append(str(path))
    cp = str(args.artifact) + os.pathsep + str(args.conflux) + os.pathsep + os.pathsep.join(str(p) for p in dependencies.glob("*.jar")) + os.pathsep + (ROOT / "build/port-classpath-26.3.txt").read_text().strip()
    subprocess.run([str(args.java_home / "bin/javac.exe"), "-proc:none", "-encoding", "UTF-8", "-cp", cp, "-d", str(classes), *sources], check=True)
    mod = directory / "invisible-smoke.jar"
    with zipfile.ZipFile(mod, "w", zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob("*.class"):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr("fabric.mod.json", json.dumps({"schemaVersion": 1, "id": "halfmasa_conflux_grid_smoke", "version": "1.0", "environment": "*", "entrypoints": {"client": ["io.github.halfmasa.confluxgridsmoke.ConfluxGridSmoke"], "server": ["io.github.halfmasa.confluxgridsmoke.ServerFixture"]}, "mixins": ["conflux-grid-smoke.mixins.json"]}))
        jar.writestr("conflux-grid-smoke.mixins.json", json.dumps({"required": True, "package": "io.github.halfmasa.confluxgridsmoke.mixin", "compatibilityLevel": "JAVA_25", "client": ["FrameMixin", "HoverMixin"], "injectors": {"defaultRequire": 1}}))
    game = directory / "game"
    (game / "mods").mkdir(parents=True)
    (game / "config").mkdir()
    (game / "config/malilib.json").write_text("{}\n")
    (game / "options.txt").write_text("onboardAccessibility:false\nenableVsync:false\nmaxFps:60\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:1\nlang:zh_cn\ntutorialStep:none\nparticles:0\n")
    pack_dir = game / "resourcepacks"
    pack_dir.mkdir()
    minecraft_jar = ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-025f6088cd/26.3/minecraft-merged-025f6088cd-26.3.jar"
    with zipfile.ZipFile(minecraft_jar) as vanilla, zipfile.ZipFile(pack_dir / "conflux-native-style-test.zip", "w") as pack:
        texture = "assets/minecraft/textures/gui/sprites/widget/button.png"
        pack.writestr(texture, vanilla.read(texture))
        pack.writestr("pack.mcmeta", json.dumps({"pack": {"description": "Isolated native theme regression fixture", "min_format": [97, 1], "max_format": [97, 1]}}))
    if args.with_api:
        api = next((Path.home() / ".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.161.0+26.3").glob("*/*.jar"))
        shutil.copy2(api, game / "mods" / api.name)
    shutil.copy2(args.conflux, game / "mods" / args.conflux.name)
    (game / "config/confluxmap").mkdir(exist_ok=True)
    (game / "config/confluxmap/server.json").write_text(json.dumps({"enabled": True, "shareWaypoints": True, "checkForUpdates": False, "shareSeed": False, "allowBiomeMap": False, "allowStructureSearch": False, "shareCorrections": False, "shareChunkLoadState": False, "webMap": {"enabled": False}}))
    (game / "config/confluxmap/config.json").write_text(json.dumps({"predictionEnabled": False, "updateCheckEnabled": False, "surveyReminderDismissed": True, "minimapEnabled": False}))
    for artifact in args.extra_mod:
        shutil.copy2(artifact, game / "mods" / artifact.name)
    command = startup.production_command(args, game, mod)
    server_game = directory / "server"
    (server_game / "mods").mkdir(parents=True)
    (server_game / "config/confluxmap").mkdir(parents=True)
    for dependency in (args.conflux, api):
        shutil.copy2(dependency, server_game / "mods" / dependency.name)
    shutil.copy2(game / "config/confluxmap/server.json", server_game / "config/confluxmap/server.json")
    with socket.socket() as reserved:
        reserved.bind(("127.0.0.1", 0)); port = reserved.getsockname()[1]
    (server_game / "eula.txt").write_text("eula=true\n")
    (server_game / "server.properties").write_text(f'online-mode=false\nwhite-list=false\nenforce-secure-profile=false\nserver-ip=127.0.0.1\nserver-port={port}\nlevel-name=fixture\nview-distance=3\nsimulation-distance=5\ndifficulty=peaceful\ngamemode=creative\nspawn-protection=0\nlevel-type=minecraft:flat\ngenerator-settings={{"layers":[{{"block":"minecraft:bedrock","height":1}}],"biome":"minecraft:plains"}}\n')
    cp = command[command.index("-cp") + 1].split(os.pathsep)
    cp[0] = str(Path.home() / ".gradle/caches/fabric-loom/26.3/minecraft-extracted_server.jar")
    server_command = [str(args.java_home / "bin/java.exe"), "-Xmx2G", "--enable-native-access=ALL-UNNAMED", f"-Dfabric.addMods={mod}", "-cp", os.pathsep.join(cp), "net.fabricmc.loader.impl.launch.knot.KnotServer", "nogui"]
    command[1:1] = [*args.jvm_arg, f"-Dhalfmasa.conflux.fixture.address=127.0.0.1:{port}"]
    command[command.index("--width") + 1] = "960"
    command[command.index("--height") + 1] = "540"
    log_path = directory / "launcher.log"
    started = time.monotonic()
    timed_out = False
    server_log = directory / "server.log"
    with server_log.open("w", encoding="utf-8") as server_output:
        server = subprocess.Popen(server_command, cwd=server_game, stdin=subprocess.PIPE, stdout=server_output, stderr=subprocess.STDOUT, text=True, creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            deadline = time.monotonic() + 150
            while "Done (" not in server_log.read_text(encoding="utf-8", errors="replace"):
                if server.poll() is not None or time.monotonic() > deadline:
                    raise RuntimeError("Conflux fixture server did not start: " + server_log.read_text(encoding="utf-8", errors="replace")[-3000:])
                time.sleep(0.5)
            with log_path.open("w", encoding="utf-8") as output:
                process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
                try:
                    code = process.wait(timeout=args.timeout)
                except subprocess.TimeoutExpired:
                    timed_out = True
                    subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], capture_output=True, check=False)
                    code = process.wait(timeout=30)
        finally:
            if server.poll() is None:
                server.stdin.write("stop\n"); server.stdin.flush()
                try: server.wait(timeout=40)
                except subprocess.TimeoutExpired: server.kill();server.wait(timeout=10)
    log = log_path.read_text(encoding="utf-8", errors="replace")
    passed = code == 0 and not timed_out and "CONFLUX_GRID_WORLD_PASS" in log
    report = {"result": "PASS" if passed else "FAIL", "artifact": str(args.artifact), "fabric_api_installed": args.with_api, "extra_mods": [str(p.resolve()) for p in args.extra_mod], "jvm_args": args.jvm_arg, "duration_seconds": round(time.monotonic() - started, 1), "exit_code": code, "timed_out": timed_out, "log": str(log_path), "server_log": str(server_log), "connection": "127.0.0.1", "checks": [line for line in log.splitlines() if "CONFLUX_GRID_" in line], "screenshots": [str(p) for p in (game / "screenshots").glob("*.png")]}
    (directory / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("CONFLUX GRID:", report["result"], flush=True)
    print(directory / "report.json", flush=True)
    if not passed:
        print("\n".join(log.splitlines()[-45:]), flush=True)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())

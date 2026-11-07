"""26.3 compact layout, actual list clicks, native Ping, privacy and persistence checks."""
import run_better_multiplayer_smoke as base

DRAW = r'''package io.github.halfmasa.multiplayersmoke.mixin;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.multiplayersmoke.MultiplayerSmoke;
@Mixin(GuiGraphicsExtractor.class) public class DrawProbeMixin {
 @Inject(method="centeredText(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",at=@At("HEAD"))
 private void centeredString(Font font,String text,int x,int y,int color,CallbackInfo ci){if(text.equals(Component.translatable("multiplayer.title").getString()))MultiplayerSmoke.nativeTitle=true;}
 @Inject(method="centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V",at=@At("HEAD"))
 private void centeredSequence(Font font,net.minecraft.util.FormattedCharSequence text,int x,int y,int color,CallbackInfo ci){var value=new StringBuilder();text.accept((index,style,codepoint)->{value.appendCodePoint(codepoint);return true;});if(value.toString().equals(Component.translatable("multiplayer.title").getString()))MultiplayerSmoke.nativeTitle=true;}
 @Inject(method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V",at=@At("HEAD"))
 private void sequence(Font font,net.minecraft.util.FormattedCharSequence text,int x,int y,int color,CallbackInfo ci){var value=new StringBuilder();text.accept((index,style,codepoint)->{value.appendCodePoint(codepoint);return true;});if(value.toString().equals(Component.translatable("multiplayer.title").getString()))MultiplayerSmoke.nativeTitle=true;}

 @Inject(method="centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",at=@At("HEAD"))
 private void centered(Font font,Component text,int x,int y,int color,CallbackInfo ci){if(text.getString().equals(Component.translatable("multiplayer.title").getString()))MultiplayerSmoke.nativeTitle=true;}

 @Inject(method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",at=@At("HEAD"))
 private void draw(Font font,Component text,int x,int y,int color,CallbackInfo ci){MultiplayerSmoke.drawn.put(text.getString(),new int[]{x,y,font.width(text)});if(text.getString().equals(Component.translatable("multiplayer.title").getString()))MultiplayerSmoke.nativeTitle=true;}
 @Inject(method="text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",at=@At("HEAD"))
 private void textTitle(Font font,String text,int x,int y,int color,CallbackInfo ci){if(text.equals(Component.translatable("multiplayer.title").getString()))MultiplayerSmoke.nativeTitle=true;}
 @Inject(method="text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",at=@At("HEAD"))
 private void text(Font font,String text,int x,int y,int color,CallbackInfo ci){if(text.equals("Survival"))MultiplayerSmoke.duplicateFolder=true;}

}'''
JOIN = r'''package io.github.halfmasa.multiplayersmoke.mixin;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.multiplayersmoke.MultiplayerSmoke;
@Mixin(JoinMultiplayerScreen.class) public class JoinProbeMixin {
 @Inject(method="join",at=@At("HEAD"),cancellable=true)
 private void join(ServerData data,CallbackInfo ci){MultiplayerSmoke.joined=data;ci.cancel();}
}'''
METHODS = r'''
 public static final Map<String,int[]> drawn=new HashMap<>();
 public static ServerData joined;
 public static boolean duplicateFolder,nativeTitle;
 String compactFolder;List<String> expectedCompact;
 static net.minecraft.client.input.MouseButtonEvent mouse(double x,double y,int button){return new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0));}
 static void shot(Minecraft mc,String file){net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try{image.writeToFile(mc.gameDirectory.toPath().resolve(file));}catch(Exception e){throw new RuntimeException(e);}finally{image.close();}});}
 CompactServerRow compactRow(){return (CompactServerRow)list.children().stream().filter(CompactServerRow.class::isInstance).findFirst().orElseThrow();}
 void clickTile(CompactServerRow row,int index,int action){var tile=row.tile(index);int x=tile.x()+(action<0?3:action>0?29:16);var event=mouse(x,tile.y()+26,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);check(list.mouseClicked(event,false),"compact native list click handled");list.mouseReleased(event);}
 void prepareCompact(Minecraft mc)throws Exception{
  BetterMultiplayerConfig.ENABLED.setBooleanValue(true);BetterMultiplayerConfig.AUTO_HIDE.setBooleanValue(false);BetterMultiplayerConfig.MANUAL_PRIVACY.setBooleanValue(false);
  BetterMultiplayerConfig.COLLAPSED_ICONS.setBooleanValue(true);
  var saved=new ServerList(mc);
  for(int i=0;i<13;i++)saved.add(server(i%2==0?"FGA空岛生存 "+i:"FGA空岛镜像 "+i,"127.0.0.1:"+(21000+i)),false);
  saved.save();
  var manager=ServerFolderManager.getInstance();List<ServerData> entries=new ArrayList<>();for(int i=0;i<saved.size();i++)entries.add(saved.get(i));manager.reconcile(entries);
  compactFolder=manager.create("FGA 图标预览",null);
  for(var entry:entries)manager.move(manager.id(entry),compactFolder,null);
  manager.delete(folder);manager.toggle(compactFolder);
  screen=new JoinMultiplayerScreen(mc.gui.screen());mc.setScreenAndShow(screen);list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);
  expectedCompact=manager.compactServers(compactFolder);
  int i=0;
  for(var id:expectedCompact){var data=manager.server(id);data.name=i%2==0?"FGA空岛生存 "+i:"FGA空岛镜像 "+i;data.motd=Component.literal("FGA FloatingIslands · 完整描述保留在悬停提示中");data.status=Component.literal("18/100");data.ping=86;data.setState(i%3==0?ServerData.State.UNREACHABLE:ServerData.State.SUCCESSFUL);ServerLatencyOverlay.complete(data,ServerLatencyOverlay.begin(data));i++;}
 }
'''
COLLAPSE = r'''
   check(list.children().stream().anyMatch(CompactServerRow.class::isInstance),"collapsed folder shows compact icons by default");
   for(int pass=0;pass<4;pass++)((ServerFolderWidget)list).halfmasa$rebuildFolders();
   check(list.children().stream().filter(FolderEntry.class::isInstance).count()==1,"refresh never duplicates folder headers");
   check(list.children().stream().filter(CompactServerRow.class::isInstance).count()==1,"refresh never duplicates compact rows");
   BetterMultiplayerConfig.COLLAPSED_ICONS.setBooleanValue(false);((ServerFolderWidget)list).halfmasa$rebuildFolders();
   check(list.children().stream().noneMatch(CompactServerRow.class::isInstance),"compact option off restores fully hidden collapsed contents");
   BetterMultiplayerConfig.COLLAPSED_ICONS.setBooleanValue(true);((ServerFolderWidget)list).halfmasa$rebuildFolders();
'''
STAGES = r'''
  if(stage==20&&frames>30){
   var manager=ServerFolderManager.getInstance();
   var rows=list.children().stream().filter(CompactServerRow.class::isInstance).map(CompactServerRow.class::cast).toList();
   check(rows.size()>1,"collapsed icons wrap to several rows");
   check(rows.stream().flatMap(r->r.serverIds().stream()).toList().equals(expectedCompact),"compact rows cover every descendant exactly once in canonical order");
   for(var row:rows){check(row.getHeight()==36,"compact row uses vanilla height");for(int i=0;i<row.serverIds().size();i++){var tile=row.tile(i);check(tile.x()>=row.getContentX()&&tile.x()+32<=row.getContentRight(),"tile stays within actual content width");}}
   shot(mc,"compact-icons.png");
   var row=rows.getFirst();String first=row.serverIds().getFirst();
   check(!manager.canMoveServer(first,false),"first icon has no left move");joined=null;
   clickTile(row,0,-1);check(joined==null&&manager.compactServers(compactFolder).equals(expectedCompact),"disabled left control does not reorder or join");
   clickTile(row,0,1);check(joined==null,"right order control never joins");
   var after=manager.compactServers(compactFolder);check(after.get(1).equals(first)&&after.get(0).equals(expectedCompact.get(1)),"right arrow moves one place within folder");
   row=compactRow();clickTile(row,1,-1);check(manager.compactServers(compactFolder).equals(expectedCompact),"left arrow restores original order");
   row=compactRow();joined=null;var selectTile=row.tile(0);
   var selectionClick=mouse(selectTile.x()+16,selectTile.y()+8,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
   list.mouseClicked(selectionClick,false);list.mouseReleased(selectionClick);
   check(joined==null&&row.selectedId().equals(first),"single click selects server without joining");
   var editButton=(Button)field(JoinMultiplayerScreen.class,"editButton").get(screen);var deleteButton=(Button)field(JoinMultiplayerScreen.class,"deleteButton").get(screen);
   check(editButton.active&&deleteButton.active,"selected compact server enables native edit and delete");
   var secondTile=row.tile(1);var secondClick=mouse(secondTile.x()+16,secondTile.y()+8,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
   list.mouseClicked(secondClick,true);list.mouseReleased(secondClick);
   check(joined==null,"rapid clicks on different icons do not join");
   list.mouseClicked(secondClick,false);list.mouseReleased(secondClick);list.mouseClicked(secondClick,true);list.mouseReleased(secondClick);
   check(joined==manager.server(row.serverIds().get(1)),"double-click joins the same selected server");
   joined=null;row=compactRow();row.select(first);list.setSelected(row);
   row=compactRow();clickTile(row,0,0);check(joined==manager.server(first),"middle icon button delegates to native join for original ServerData");
   check(list.getSelected() instanceof CompactServerRow,"compact selection retained for native Join button");
   joined=null;list.getSelected().join();check(joined==manager.server(first),"native selected-entry join works on compact row");
   row=compactRow();var tile=row.tile(1);mc.mouseHandler.setIgnoreFirstMove();mc.mouseHandler.onMove(mc.getWindow().handle(),tile.x()+16,tile.y()+8,0,0);
   stage=21;frames=0;return;
  }
  if(stage==21&&frames>15){
   shot(mc,"compact-hover.png");
   var manager=ServerFolderManager.getInstance();var row=compactRow();String first=row.serverIds().getFirst();var data=manager.server(first);
   BetterMultiplayerConfig.MANUAL_PRIVACY.setBooleanValue(true);
   check(CompactServerRow.tooltip(data,0,first).stream().noneMatch(c->c.getString().contains(data.ip)),"compact hover tooltip hides address in manual privacy mode");
   BetterMultiplayerConfig.MANUAL_PRIVACY.setBooleanValue(false);
   check(CompactServerRow.tooltip(data,0,first).stream().anyMatch(c->c.getString().equals(data.ip)),"compact hover restores address when privacy clears");
   var tile=row.tile(0);list.mouseClicked(mouse(tile.x()+16,tile.y()+16,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT),false);
   check(mc.gui.screen() instanceof ServerFolderScreen,"compact right-click opens management for correct server");mc.gui.screen().onClose();
   row=compactRow();row.select(first);list.setSelected(row);
   var editButton=(Button)field(JoinMultiplayerScreen.class,"editButton").get(screen);editButton.onPress(mouse(0,0,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT));
   check(mc.gui.screen()!=screen,"native edit button opens vanilla editor");
   var editing=(ServerData)field(JoinMultiplayerScreen.class,"editingServer").get(screen);
   check(editing.ip.equals(data.ip),"vanilla editor receives clicked server");
   String renamed=data.name+" edited";editing.name=renamed;
   var apply=JoinMultiplayerScreen.class.getDeclaredMethod("editServerCallback",boolean.class);apply.setAccessible(true);apply.invoke(screen,true);
   check(manager.server(first).name.equals(renamed),"vanilla edit callback updates original server object");
   list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);
   row=compactRow();row.select(first);list.setSelected(row);
   var deleteButton=(Button)field(JoinMultiplayerScreen.class,"deleteButton").get(screen);deleteButton.onPress(mouse(0,0,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT));
   check(mc.gui.screen() instanceof ConfirmScreen,"native delete opens confirmation for selected compact server");
   var cancelDelete=JoinMultiplayerScreen.class.getDeclaredMethod("deleteCallback",boolean.class);cancelDelete.setAccessible(true);cancelDelete.invoke(screen,false);
   check(manager.server(first)!=null,"canceling native delete preserves compact server");
   screen.resize(220,270);stage=22;frames=0;return;
  }
  if(stage==22&&frames>25){
   list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);
   var rows=list.children().stream().filter(CompactServerRow.class::isInstance).map(CompactServerRow.class::cast).toList();
   check(rows.stream().flatMap(r->r.serverIds().stream()).toList().equals(expectedCompact),"narrow resize preserves complete canonical icon order");
   for(var row:rows)for(int i=0;i<row.serverIds().size();i++)check(row.tile(i).x()>=0&&row.tile(i).x()+32<=mc.getWindow().getGuiScaledWidth()&&row.tile(i).x()+32<=row.getContentRight(),"narrow resize recomputes tile wrapping");
   shot(mc,"compact-narrow.png");
   var manager=ServerFolderManager.getInstance();String id=expectedCompact.getFirst();manager.moveServer(id,true);expectedCompact=manager.compactServers(compactFolder);
   var reloaded=new ServerFolderManager(new ServerFolderStorage(mc.gameDirectory.toPath().resolve("config/halfmasa/server-folders.json")));
   List<ServerData> entries=new ArrayList<>();for(int i=0;i<screen.getServers().size();i++)entries.add(screen.getServers().get(i));reloaded.reconcile(entries);
   check(reloaded.compactServers(compactFolder).equals(expectedCompact),"left/right order survives disk reload");
   ((ServerFolderWidget)list).halfmasa$search("FGA空岛生存");
   check(list.children().stream().noneMatch(CompactServerRow.class::isInstance)&&list.children().stream().anyMatch(ServerSelectionList.OnlineServerEntry.class::isInstance),"search expands matching compact servers as full rows");
   stage=23;frames=0;return;
  }
  if(stage==23&&frames>25){
   shot(mc,"compact-search.png");
   check(list.children().stream().filter(ServerSelectionList.OnlineServerEntry.class::isInstance).allMatch(e->e.getHeight()==36),"search rows retain vanilla height");
   BetterMultiplayerConfig.COLLAPSED_ICONS.setBooleanValue(false);((ServerFolderWidget)list).halfmasa$search("");
   stage=24;frames=0;return;
  }
  if(stage==24&&frames>10){
   check(list.children().stream().noneMatch(CompactServerRow.class::isInstance),"live compact switch removes all compact rows");
   org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
   listener.close();System.out.println("MULTIPLAYER_PASS");stage=99;mc.stop();
  }
'''


def main():
    base.EXTRA_SOURCES = [('DrawProbeMixin', DRAW), ('JoinProbeMixin', JOIN)]
    source = base.SOURCE.replace(' void tick(Minecraft mc)', METHODS + '\n void tick(Minecraft mc)')
    source = source.replace(r'\"protocol\":1', r'\"protocol\":"+net.minecraft.SharedConstants.getProtocolVersion()+"')
    source = source.replace('byte[] json=', 'byte[] json=(').replace('}}".getBytes(java.nio.charset.StandardCharsets.UTF_8)', '}}").getBytes(java.nio.charset.StandardCharsets.UTF_8)')
    source = source.replace('check(probe.getHeight()==60,"enhanced row height");', '''check(probe.getHeight()==36,"expanded server row uses vanilla height");check(!duplicateFolder,"folder path is not drawn again as standalone text below server row");check(screen.getTitle().getString().equals(Component.translatable("multiplayer.title").getString()),"native multiplayer title stays configured");
   var latency=drawn.get(ServerLatencyOverlay.label(probe.getServerData()).text().getString());var players=drawn.get(probe.getServerData().state()==ServerData.State.INCOMPATIBLE?probe.getServerData().version.getString():probe.getServerData().status.getString());
   check(latency!=null&&players!=null&&latency[1]==probe.getContentY()+12&&players[1]==probe.getContentY()+1,"latency is directly below player count");
   check(latency[0]+latency[2]==players[0]+players[2],"latency and player count share a right edge");''')
    source = source.replace('check(list.children().stream().noneMatch(FolderEntry.class::isInstance),"folders off returns flat list");', 'check(list.children().stream().noneMatch(FolderEntry.class::isInstance),"folders off returns flat list");check(((io.github.halfmasa.xaerobinding.draggable.DragList<?,?>)list).draggable_lists$getItemHeight()==36,"flat drag hit testing uses the same vanilla row height");')
    source = source.replace('   ((ServerFolderWidget)list).halfmasa$search("Fabric");', COLLAPSE + '\n   ((ServerFolderWidget)list).halfmasa$search("Fabric");')
    source = source.replace('   listener.close();System.out.println("MULTIPLAYER_PASS");stage=99;mc.stop();', '   prepareCompact(mc);stage=20;frames=0;return;')
    source = source.replace(' }catch(Throwable e)', STAGES + '\n }catch(Throwable e)')
    base.SOURCE = source
    return base.main()


if __name__ == '__main__':
    raise SystemExit(main())

"""Run the multiplayer module against an isolated production 26.3 client."""
import argparse
from datetime import datetime
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import time
import zipfile
import run_startup_smoke as startup
import port_smoke_compat as compat

ROOT = Path(__file__).resolve().parents[2]
EXTRA_SOURCES = []
SOURCE = r'''package io.github.halfmasa.multiplayersmoke;
import java.util.*;
import java.nio.file.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.multiplayer.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.network.chat.Component;
import io.github.halfmasa.xaerobinding.better_multiplayer.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.folder.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.privacy.*;
public final class MultiplayerSmoke implements ClientModInitializer {
 static MultiplayerSmoke instance;
 int stage,frames,checks;
 JoinMultiplayerScreen screen;
 ServerSelectionList list;
 String folder,serverId;
 ServerSocket listener;
 volatile int connections;
 volatile Throwable networkFailure;
 ObsFixture liveObs;
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);System.out.println("MULTIPLAYER_CHECK "+name);}
 public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 static ServerData server(String name,String ip){return new ServerData(name,ip,ServerData.Type.OTHER);}
 static Field field(Class<?> owner,String name)throws Exception{Field f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
 void modelTests()throws Exception{
  check(BetterMultiplayerConfig.ENABLED.getBooleanValue()&&BetterMultiplayerConfig.AUTO_HIDE.getBooleanValue(),"default enabled and auto-hide");
  for(var binding:BetterMultiplayerConfig.HOTKEYS)check(binding.getKeybind().getKeys().isEmpty(),"unbound "+binding.getName());
  for(var binding:BetterMultiplayerConfig.HOTKEYS)check(binding.getKeybind().getSettings().getContext()==fi.dy.masa.malilib.hotkeys.KeybindSettings.Context.ANY,"menu-capable binding "+binding.getName());
  var manualKey=BetterMultiplayerConfig.MANUAL_PRIVACY.getKeybind();
  var callback=(fi.dy.masa.malilib.hotkeys.IHotkeyCallback)field(manualKey.getClass(),"callback").get(manualKey);
  callback.onKeyAction(fi.dy.masa.malilib.hotkeys.KeyAction.PRESS,manualKey);check(BetterMultiplayerConfig.MANUAL_PRIVACY.getBooleanValue(),"privacy shortcut toggles on");
  callback.onKeyAction(fi.dy.masa.malilib.hotkeys.KeyAction.PRESS,manualKey);check(!BetterMultiplayerConfig.MANUAL_PRIVACY.getBooleanValue(),"privacy shortcut toggles off");
  var privacy=new PrivacyManager();privacy.observe(PrivacyManager.State.CHECKING);check(privacy.hidden(),"initial check hides address");
  privacy.observe(PrivacyManager.State.IDLE);check(!privacy.hidden(),"confirmed idle restores");
  privacy.observe(PrivacyManager.State.CAPTURING);privacy.observe(PrivacyManager.State.UNKNOWN);check(privacy.hidden(),"lost recording state stays hidden");
  BetterMultiplayerConfig.AUTO_RESTORE.setBooleanValue(false);privacy.observe(PrivacyManager.State.IDLE);check(privacy.hidden(),"restore disabled latches privacy");
  BetterMultiplayerConfig.AUTO_RESTORE.setBooleanValue(true);privacy.observe(PrivacyManager.State.IDLE);
  privacy.observe(PrivacyManager.State.UNKNOWN);BetterMultiplayerConfig.HIDE_UNKNOWN.setBooleanValue(false);check(!privacy.hidden(),"unknown-state protection has an independent switch");
  BetterMultiplayerConfig.HIDE_UNKNOWN.setBooleanValue(true);privacy.observe(PrivacyManager.State.IDLE);
  BetterMultiplayerConfig.MANUAL_PRIVACY.setBooleanValue(true);check(privacy.hidden(),"manual mode overrides idle");
  BetterMultiplayerConfig.ENABLED.setBooleanValue(false);check(!privacy.hidden(),"master disabled bypasses privacy");
  BetterMultiplayerConfig.ENABLED.setBooleanValue(true);BetterMultiplayerConfig.MANUAL_PRIVACY.setBooleanValue(false);
  var ping=server("Ping","127.0.0.1:1");ping.ping=0;
  check(ServerLatencyOverlay.label(ping).text().getString().equals("--"),"initial zero is not a measurement");
  var old=ServerLatencyOverlay.begin(ping);ping.setState(ServerData.State.SUCCESSFUL);
  check(!ServerLatencyOverlay.label(ping).text().getString().contains("ms"),"success without pong does not use stale value");
  ServerLatencyOverlay.complete(ping,old);check(ServerLatencyOverlay.label(ping).text().getString().equals("0 ms"),"measured zero is valid");
  ServerLatencyOverlay.invalidate(ping);var current=ServerLatencyOverlay.begin(ping);ping.ping=999;ServerLatencyOverlay.complete(ping,old);
  check(!ServerLatencyOverlay.label(ping).text().getString().contains("ms"),"old generation callback ignored");
  ping.ping=32;ServerLatencyOverlay.complete(ping,current);ping.setState(ServerData.State.INCOMPATIBLE);
  check(ServerLatencyOverlay.label(ping).text().getString().contains("32 ms"),"incompatible server retains measured ping");
  ping.setState(ServerData.State.UNREACHABLE);check(!ServerLatencyOverlay.label(ping).text().getString().contains("ms"),"unreachable never shows old ping");
  Path temp=Files.createTempDirectory(Minecraft.getInstance().gameDirectory.toPath(),"folder-model-");Path file=temp.resolve("server-folders.json");
  var manager=new ServerFolderManager(new ServerFolderStorage(file));
  var a=server("A","duplicate.example");var b=server("B","duplicate.example");var c=server("C","other.example");
  manager.reconcile(List.of(a,b,c));String aid=manager.id(a),bid=manager.id(b);
  check(!aid.equals(bid),"duplicate addresses have separate UUIDs");
  String root=manager.create("Root",null),nested=manager.create("Nested",root);
  manager.move(aid,nested,null);manager.move(bid,root,nested);
  check(manager.count(root)==2&&manager.count(nested)==1,"recursive server counts");
  check(!manager.move(root,nested,null),"folder cycle rejected");
  a.name="Renamed";a.ip="new.example:25566";manager.reconcile(List.of(c,a,b));
  check(manager.id(a).equals(aid)&&manager.parent(aid).equals(nested),"rename and address edit retain UUID");
  var loaded=new ServerFolderManager(new ServerFolderStorage(file));loaded.reconcile(List.of(server("C","other.example"),server(a.name,a.ip),server(b.name,b.ip)));
  check(loaded.assignment(aid)!=null&&loaded.count(nested)==1,"restart retains assignments");
  check(loaded.visible("renamed",false).getFirst().path().equals("Root / Nested"),"cross-folder search returns path");
  loaded.toggle(root);check(loaded.visible("",false).stream().noneMatch(n->n.id().equals(aid)),"collapse hides descendants");
  loaded.delete(root);check(loaded.folder(nested).parentId==null&&loaded.parent(bid)==null,"delete moves child folders and servers to parent");
  Path broken=temp.resolve("broken.json");Files.writeString(broken,"{broken");
  var brokenManager=new ServerFolderManager(new ServerFolderStorage(broken));brokenManager.create("Do not overwrite",null);
  check(Files.readString(broken).equals("{broken"),"corrupt organization file preserved");
  Path duplicateFile=temp.resolve("duplicates.json");
  var duplicates=new ServerFolderManager(new ServerFolderStorage(duplicateFile));
  duplicates.reconcile(List.of(server("Same","same.example"),server("Same","same.example")));
  var firstIds=duplicates.visible("",false).stream().map(ServerFolderManager.Node::id).toList();
  var duplicateReload=new ServerFolderManager(new ServerFolderStorage(duplicateFile));
  duplicateReload.reconcile(List.of(server("Same","same.example"),server("Same","same.example")));
  check(firstIds.equals(duplicateReload.visible("",false).stream().map(ServerFolderManager.Node::id).toList()),"unchanged identical entries retain separate UUIDs across restart");
  duplicateReload.reconcile(List.of(server("Same","same.example"),server("Same","same.example"),server("Same","same.example")));
  check(duplicateReload.unresolved().size()==2,"ambiguous external duplicate changes preserve unresolved assignments");
  try(var obs=new ObsFixture()){
   BetterMultiplayerConfig.OBS_URL.setStringValue("ws://127.0.0.1:"+obs.listener.getLocalPort());
   try(var detector=new ObsCaptureDetector("secret")){
    check(detector.poll()==PrivacyManager.State.CAPTURING,"OBS authenticated recording detection");
    obs.state=2;check(detector.poll()==PrivacyManager.State.CAPTURING,"OBS replay buffer protects address");
    obs.state=0;check(detector.poll()==PrivacyManager.State.IDLE,"OBS confirmed idle");
    obs.state=3;check(detector.poll()==PrivacyManager.State.UNKNOWN,"OBS rejected status stays unknown");
    check(obs.authenticated&&obs.onlyStatusRequests,"OBS authenticates and never controls recording");
   }
  }
  BetterMultiplayerConfig.OBS_URL.resetToDefault();
 }
 static final class ObsFixture implements AutoCloseable {
  final ServerSocket listener;
  volatile Socket socket;
  volatile int state=1;
  volatile boolean authenticated,onlyStatusRequests=true;
  ObsFixture()throws Exception{listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress());Thread.ofVirtual().start(()->{try{
   socket=listener.accept();socket.setSoTimeout(10000);var in=socket.getInputStream();var out=socket.getOutputStream();
   var headers=new ByteArrayOutputStream();int c;while((c=in.read())>=0){headers.write(c);if(headers.toString(java.nio.charset.StandardCharsets.US_ASCII).endsWith("\r\n\r\n"))break;}
   String key=Arrays.stream(headers.toString(java.nio.charset.StandardCharsets.US_ASCII).split("\r\n")).filter(s->s.toLowerCase().startsWith("sec-websocket-key:")).findFirst().orElseThrow().split(":",2)[1].strip();
   String accept=Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-1").digest((key+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
   out.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: "+accept+"\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));out.flush();
   send(out,"{\"op\":0,\"d\":{\"rpcVersion\":1,\"authentication\":{\"salt\":\"salt\",\"challenge\":\"challenge\"}}}");
   var identify=com.google.gson.JsonParser.parseString(receive(in)).getAsJsonObject();
   authenticated=identify.getAsJsonObject("d").get("authentication").getAsString().equals(hash(hash("secretsalt")+"challenge"));
   send(out,"{\"op\":2,\"d\":{\"negotiatedRpcVersion\":1}}");
   while(!socket.isClosed()){
    var message=com.google.gson.JsonParser.parseString(receive(in)).getAsJsonObject();var data=message.getAsJsonObject("d");String type=data.get("requestType").getAsString();
    onlyStatusRequests&=Set.of("GetRecordStatus","GetStreamStatus","GetReplayBufferStatus").contains(type);
    boolean active=state==1&&type.equals("GetRecordStatus")||state==2&&type.equals("GetReplayBufferStatus");
    send(out,"{\"op\":7,\"d\":{\"requestId\":\""+data.get("requestId").getAsString()+"\",\"requestStatus\":{\"result\":"+(state!=3)+"},\"responseData\":{\"outputActive\":"+active+",\"outputPaused\":true}}}");
   }
  }catch(EOFException|SocketException expected){}catch(Exception e){e.printStackTrace();}});}
  static String hash(String value)throws Exception{return Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
  static void send(OutputStream out,String value)throws Exception{byte[] b=value.getBytes(java.nio.charset.StandardCharsets.UTF_8);out.write(129);if(b.length<126)out.write(b.length);else{out.write(126);out.write(b.length>>8);out.write(b.length);}out.write(b);out.flush();}
  static String receive(InputStream in)throws Exception{int opcode=in.read();if(opcode<0)throw new EOFException();int second=in.read(),length=second&127;if(length==126)length=in.read()<<8|in.read();if(length>65535)throw new IOException("frame");byte[] mask=(second&128)!=0?in.readNBytes(4):new byte[4];byte[] b=in.readNBytes(length);for(int i=0;i<b.length;i++)b[i]^=mask[i%4];return new String(b,java.nio.charset.StandardCharsets.UTF_8);}
  public void close()throws Exception{listener.close();if(socket!=null)socket.close();}
 }
 static int varint(InputStream in)throws Exception{int n=0;for(int i=0;i<5;i++){int b=in.read();if(b<0)throw new EOFException();n|=(b&127)<<(7*i);if((b&128)==0)return n;}throw new IOException("varint");}
 static void varint(OutputStream out,int n)throws Exception{do{int b=n&127;n>>>=7;out.write(n==0?b:b|128);}while(n!=0);}
 static byte[] packet(InputStream in)throws Exception{return in.readNBytes(varint(in));}
 static void packet(OutputStream out,byte[] bytes)throws Exception{varint(out,bytes.length);out.write(bytes);out.flush();}
 void startServer()throws Exception{
  listener=new ServerSocket(0,8,InetAddress.getLoopbackAddress());
  Thread.ofVirtual().start(()->{try{while(!listener.isClosed()){Socket socket=listener.accept();connections++;Thread.ofVirtual().start(()->{try(socket){socket.setSoTimeout(10000);var in=socket.getInputStream();var out=socket.getOutputStream();packet(in);packet(in);
   byte[] json="{\"version\":{\"name\":\"Smoke\",\"protocol\":1},\"players\":{\"max\":100,\"online\":18},\"description\":{\"text\":\"Vanilla ping fixture\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
   var status=new ByteArrayOutputStream();status.write(0);varint(status,json.length);status.write(json);packet(out,status.toByteArray());byte[] ping=packet(in);packet(out,ping);
  }catch(Exception e){networkFailure=e;}});}}catch(SocketException expected){if(!listener.isClosed())networkFailure=expected;}catch(Exception e){networkFailure=e;}});
 }
 void tick(Minecraft mc){try{
  frames++;
  if(stage==0&&mc.gui.screen() instanceof TitleScreen&&frames>30){
   modelTests();startServer();BetterMultiplayerConfig.AUTO_HIDE.setBooleanValue(false);
   ServerList servers=new ServerList(mc);servers.add(server("Fabric survival","play.example.invalid"),false);
   servers.add(server("Probe server","127.0.0.1:"+listener.getLocalPort()),false);servers.save();
   var manager=ServerFolderManager.getInstance();manager.reconcile(List.of(servers.get(0),servers.get(1)));
   folder=manager.create("Survival",null);serverId=manager.id(servers.get(0));manager.move(serverId,folder,null);
   screen=new JoinMultiplayerScreen(mc.gui.screen());mc.setScreenAndShow(screen);
   list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);
   stage=1;frames=0;return;
  }
  if(stage==1&&frames==300){
   net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try{image.writeToFile(mc.gameDirectory.toPath().resolve("multiplayer-preview.png"));}catch(Exception error){throw new RuntimeException(error);}finally{image.close();}});
  }
  if(stage==1&&frames>420){
   check(networkFailure==null,"status fixture no protocol failure");
   var probe=list.children().stream().filter(e->e instanceof ServerSelectionList.OnlineServerEntry s&&s.getServerData().name.equals("Probe server")).map(e->(ServerSelectionList.OnlineServerEntry)e).findFirst().orElseThrow();
   check(ServerLatencyOverlay.label(probe.getServerData()).text().getString().contains("ms"),"real vanilla pong callback measured latency: state="+probe.getServerData().state()+", label="+ServerLatencyOverlay.label(probe.getServerData()).text().getString()+", connections="+connections);
   check(connections==1,"overlay made no extra connection");
   check(list.children().stream().anyMatch(FolderEntry.class::isInstance),"folder row inserted into vanilla list");
   check(probe.getHeight()==60,"enhanced row height");
   for(int i=1;i<list.children().size();i++)check(list.children().get(i).getY()>=list.children().get(i-1).getY()+list.children().get(i-1).getHeight(),"cached row coordinates never overlap");
   ServerFolderManager.getInstance().toggle(folder);((ServerFolderWidget)list).halfmasa$rebuildFolders();
   check(list.children().stream().filter(e->e instanceof ServerSelectionList.OnlineServerEntry).count()==1,"collapsed folder hides its server");
   ((ServerFolderWidget)list).halfmasa$search("Fabric");
   check(list.children().stream().filter(e->e instanceof ServerSelectionList.OnlineServerEntry).count()==1,"search finds collapsed descendants");
   ((ServerFolderWidget)list).halfmasa$search("");BetterMultiplayerConfig.FOLDERS.setBooleanValue(false);stage=2;frames=0;return;
  }
  if(stage==2&&frames>5){
   list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);
   check(list.children().stream().noneMatch(FolderEntry.class::isInstance),"folders off returns flat list");
   check(list.children().stream().filter(e->e instanceof ServerSelectionList.OnlineServerEntry).count()==2,"folders off preserves all servers");
   BetterMultiplayerConfig.ENABLED.setBooleanValue(false);stage=3;frames=0;return;
  }
  if(stage==3&&frames>5){
   list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);
   check(list.children().stream().noneMatch(FolderEntry.class::isInstance),"master off has no folder rows");
   check(screen.children().stream().noneMatch(EditBox.class::isInstance),"master off removes search controls");
   check(list.children().stream().filter(e->e instanceof ServerSelectionList.OnlineServerEntry).allMatch(e->e.getHeight()==36),"master off restores vanilla row heights");
   check(ServerFolderManager.getInstance().parent(serverId).equals(folder),"master off retains organization");
   BetterMultiplayerConfig.ENABLED.setBooleanValue(true);BetterMultiplayerConfig.FOLDERS.setBooleanValue(true);stage=4;frames=0;return;
  }
  if(stage==4&&frames>5){
   list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);
   check(list.children().stream().anyMatch(FolderEntry.class::isInstance),"re-enable restores folder organization");
   var manager=ServerFolderManager.getInstance();
   var probe=list.children().stream().filter(e->e instanceof ServerSelectionList.OnlineServerEntry s&&s.getServerData().name.equals("Probe server")).map(e->(ServerSelectionList.OnlineServerEntry)e).findFirst().orElseThrow();
   var target=list.children().stream().filter(FolderEntry.class::isInstance).findFirst().orElseThrow();
   var press=new net.minecraft.client.input.MouseButtonEvent(probe.getContentX()+90,probe.getY()+10,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
   var drop=new net.minecraft.client.input.MouseButtonEvent(target.getContentX()+80,target.getY()+8,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
   list.mouseClicked(press,false);list.mouseDragged(drop,drop.x()-press.x(),drop.y()-press.y());list.mouseReleased(drop);
   check(folder.equals(manager.parent(manager.id(probe.getServerData()))),"actual list drag moves server into folder");
   manager.move(manager.id(probe.getServerData()),null,folder);((ServerFolderWidget)list).halfmasa$rebuildFolders();
   target=list.children().stream().filter(FolderEntry.class::isInstance).findFirst().orElseThrow();
   list.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(target.getContentX()+80,target.getY()+8,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,0)),false);
   check(mc.gui.screen() instanceof ServerFolderScreen,"right-click opens folder management");
   stage=5;frames=0;return;
  }
  if(stage==5&&frames>15){
   check(mc.gui.screen().children().stream().anyMatch(EditBox.class::isInstance),"folder management provides rename field");
   mc.gui.screen().onClose();
   BetterMultiplayerConfig.EXPANDED.setBooleanValue(true);
   var configuration=new io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen();mc.setScreenAndShow(configuration);
   var listMethod=fi.dy.masa.malilib.gui.GuiListBase.class.getDeclaredMethod("getListWidget");listMethod.setAccessible(true);
   var configList=(fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions)listMethod.invoke(configuration);var search=configList.getSearchBarWidget();
   ((io.github.halfmasa.xaerobinding.mixin.ConfigSearchBarAccessor)search).halfmasa$getSearchBox().setValueWrapper("betterMultiplayer");
   search.setSearchOpen(true);configList.refreshEntries();
   stage=6;frames=0;return;
  }
  if(stage==6&&frames>30){
   check(io.github.halfmasa.xaerobinding.config.Configs.getClientView().contains(BetterMultiplayerConfig.ENABLED),"main module in client configuration");
   check(io.github.halfmasa.xaerobinding.config.Configs.getClientView().contains(BetterMultiplayerConfig.DETECTION_MODE),"expanded module exposes detection mode");
   net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try{image.writeToFile(mc.gameDirectory.toPath().resolve("multiplayer-config-preview.png"));}catch(Exception error){throw new RuntimeException(error);}finally{image.close();}});
   stage=7;frames=0;return;
  }
  if(stage==7&&frames>15){
   liveObs=new ObsFixture();BetterMultiplayerConfig.OBS_URL.setStringValue("ws://127.0.0.1:"+liveObs.listener.getLocalPort());
   BetterMultiplayerConfig.OBS_PASSWORD.setStringValue("secret");BetterMultiplayerConfig.DETECTION_MODE.setOptionListValue(BetterMultiplayerConfig.Mode.OBS);
   BetterMultiplayerConfig.AUTO_HIDE.setBooleanValue(true);mc.setScreenAndShow(screen);stage=8;frames=0;return;
  }
  if(stage==8){
   check(frames<600,"capture detection completes within deadline");
   if(CaptureDetectionManager.INSTANCE.privacy.state()!=PrivacyManager.State.CAPTURING)return;
   check(CaptureDetectionManager.INSTANCE.privacy.hidden(),"live automatic OBS detection hides addresses");
   liveObs.state=0;stage=9;frames=0;return;
  }
  if(stage==9){
   check(frames<600,"idle confirmation completes within deadline");
   if(CaptureDetectionManager.INSTANCE.privacy.state()!=PrivacyManager.State.IDLE)return;
   check(!CaptureDetectionManager.INSTANCE.privacy.hidden(),"live OBS idle confirmation restores addresses");
   liveObs.close();stage=10;frames=0;return;
  }
  if(stage==10){
   check(frames<600,"disconnection detection completes within deadline");
   if(CaptureDetectionManager.INSTANCE.privacy.state()!=PrivacyManager.State.UNKNOWN)return;
   check(CaptureDetectionManager.INSTANCE.privacy.hidden(),"live OBS disconnect protects addresses");
   BetterMultiplayerConfig.ENABLED.setBooleanValue(false);CaptureDetectionManager.INSTANCE.update();
   check(field(CaptureDetectionManager.class,"worker").get(CaptureDetectionManager.INSTANCE)==null,"master off stops detector worker");
   org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
   listener.close();System.out.println("MULTIPLAYER_PASS");stage=99;mc.stop();
  }
 }catch(Throwable e){e.printStackTrace();System.out.println("MULTIPLAYER_FAIL stage="+stage);stage=99;mc.stop();}}
}
'''
FRAME = r'''package io.github.halfmasa.multiplayersmoke.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.halfmasa.multiplayersmoke.MultiplayerSmoke;
@Mixin(Minecraft.class) public class FrameMixin {
 @Inject(method="runTick",at=@At("TAIL")) private void frame(boolean regularFrame,CallbackInfo ci){if(regularFrame)MultiplayerSmoke.frame((Minecraft)(Object)this);}
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--version', default='26.3')
    parser.add_argument('--extra-mod', action='append', type=Path, default=[], help='Additional mods for an isolated compatibility test')
    parser.add_argument('--loader-version', help='Fabric Loader override for compatibility dependencies')
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Java/jdk-25.0.3'))
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    directory = ROOT / 'build/better-multiplayer-tests' / datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    classes = directory / 'classes'
    classes.mkdir(parents=True)
    cp = compat.classpath(args.version, args.artifact)
    sources = []
    for name, source in [('MultiplayerSmoke', SOURCE), ('FrameMixin', FRAME), *EXTRA_SOURCES]:
        source = source.replace('probe.getHeight()==60', 'probe.getHeight()==36')
        file = directory / (name + '.java')
        file.write_text(compat.source(source, args.version, cp).replace('fi.dy.masa.malilib.util.input.ScanCodes.OFFSET_MOUSE_LEFT', 'com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT') if args.version != '26.3' else source, encoding='utf-8')
        sources.append(str(file))
    result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-proc:none', '-encoding', 'UTF-8', '-cp', cp,
                             '-d', str(classes), *sources], capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    mod = directory / 'multiplayer-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for file in classes.rglob('*.class'):
            jar.write(file, file.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_multiplayer_smoke', 'version': '1.0',
            'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.multiplayersmoke.MultiplayerSmoke']},
            'mixins': ['multiplayer-smoke.mixins.json']}))
        jar.writestr('multiplayer-smoke.mixins.json', json.dumps({'required': True,
            'package': 'io.github.halfmasa.multiplayersmoke.mixin', 'compatibilityLevel': 'JAVA_21' if args.version.startswith('1.21.') else 'JAVA_25',
            'client': ['FrameMixin', *[name for name, _ in EXTRA_SOURCES]], 'injectors': {'defaultRequire': 1}}))
    mod = compat.remap_fixture(args.version, args.java_home, mod, cp, directory)
    game = directory / 'game'
    (game / 'mods').mkdir(parents=True)
    (game / 'config').mkdir()
    for extra in args.extra_mod:
        shutil.copy2(extra, game / 'mods' / extra.name)
    api_version = compat.api_version(args.version)
    api = next(p for p in (Path.home()/('.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/'+api_version)).glob('*/*.jar') if not p.name.endswith('-sources.jar'))
    shutil.copy2(api, game / 'mods' / api.name)
    (game / 'config/malilib.json').write_text('{}\n', encoding='utf-8')
    (game / 'options.txt').write_text('onboardAccessibility:false\ntutorialStep:none\nnarrator:0\nlang:en_us\n'
        'guiScale:1\nenableVsync:false\nmaxFps:60\nrenderDistance:2\nsimulationDistance:5\n'
        'pauseOnLostFocus:false\nsoundCategory_master:0.0\n', encoding='utf-8')
    command = startup.production_command(args, game, mod)
    command[1:1] = ['-Djava.awt.headless=true', '-Dsun.java2d.d3d=false', '-XX:ActiveProcessorCount=2', '-XX:TieredStopAtLevel=1']
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
    passed = code == 0 and 'MULTIPLAYER_PASS' in log and 'MULTIPLAYER_FAIL' not in log
    report = {'result': 'PASS' if passed else 'FAIL', 'version': args.version,
        'extra_mods': [{'path': str(extra.resolve()), 'sha256': hashlib.sha256(extra.read_bytes()).hexdigest()} for extra in args.extra_mod],
        'artifact': str(args.artifact), 'sha256': hashlib.sha256(args.artifact.read_bytes()).hexdigest(),
        'duration_seconds': round(time.monotonic() - start, 1), 'exit_code': code, 'log': str(log_path),
        'checks': [line for line in log.splitlines() if 'MULTIPLAYER_' in line]}
    report_path = directory / 'report.json'
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('MULTIPLAYER:', report['result'], report_path, flush=True)
    if not passed:
        print('\n'.join(log.splitlines()[-90:]), flush=True)
    return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())

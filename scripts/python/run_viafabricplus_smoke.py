import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'scripts/python'))
import run_better_multiplayer_smoke as smoke

click = '''
   mc.options.skipMultiplayerWarning=true;
   var title=(TitleScreen)mc.gui.screen();
   var button=title.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
    .filter(b->b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text&&text.getKey().equals("menu.multiplayer")).findFirst().orElseThrow();
   check(button.active,"native multiplayer button enabled");
   title.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(button.getX()+5,button.getY()+5,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
   title.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(button.getX()+5,button.getY()+5,new net.minecraft.client.input.MouseButtonInfo(0,0)));
   stage=-1;frames=0;return;
  }
  if(stage==-1){
   check(frames<180,"native multiplayer screen opens within deadline; current="+mc.gui.screen());
   if(!(mc.gui.screen() instanceof JoinMultiplayerScreen))return;
   check(true,"native title multiplayer button opens server list");
   screen=(JoinMultiplayerScreen)mc.gui.screen();
'''
if sys.argv[sys.argv.index('--version')+1] == '26.3':
    click = click.replace('title.mouseClicked(', 'button.onClick(').replace('native title multiplayer button opens server list', 'native multiplayer button action opens server list')
smoke.SOURCE = smoke.SOURCE.replace('screen=new JoinMultiplayerScreen(mc.gui.screen());mc.setScreenAndShow(screen);', click)
smoke.SOURCE = smoke.SOURCE.replace(' static MultiplayerSmoke instance;', '''
 public static int legacySplits;
 Object legacyProtocol;
 static MultiplayerSmoke instance;''')
smoke.SOURCE = smoke.SOURCE.replace('   org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();', '''
   legacyProtocol=Class.forName("net.raphimc.vialegacy.api.LegacyProtocolVersion").getField("b1_7tob1_7_3").get(null);
   var legacy=server("Legacy Beta test","legacy.example.invalid");
   legacy.motd=Component.literal("LEGACY_ORIGINAL_DESCRIPTION");
   var force=Arrays.stream(legacy.getClass().getMethods()).filter(m->m.getName().equals("viaFabricPlus$forceVersion")).findFirst().orElseThrow();
   force.invoke(legacy,legacyProtocol);
   check(Arrays.stream(legacy.getClass().getMethods()).filter(m->m.getName().equals("viaFabricPlus$forcedVersion")).findFirst().orElseThrow().invoke(legacy)==legacyProtocol,"ViaFabricPlus forced Beta protocol applied");
   screen.getServers().add(legacy,false);screen.getServers().save();
   BetterMultiplayerConfig.ENABLED.setBooleanValue(true);BetterMultiplayerConfig.AUTO_HIDE.setBooleanValue(false);
   mc.setScreenAndShow(screen);list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);list.updateOnlineServers(screen.getServers());((ServerFolderWidget)list).halfmasa$search("");
   legacySplits=0;frames=0;stage=11;return;
  }
  if(stage==11&&frames>120){
   list=(ServerSelectionList)field(JoinMultiplayerScreen.class,"serverSelectionList").get(screen);
   var legacy=list.children().stream().filter(e->e instanceof ServerSelectionList.OnlineServerEntry entry&&entry.getServerData().name.equals("Legacy Beta test")).map(e->(ServerSelectionList.OnlineServerEntry)e).findFirst().orElseThrow();
   check(field(legacy.getClass(),"viaFabricPlus$disableServerPinging").getBoolean(legacy),"ViaFabricPlus real disableServerPinging branch reached");
   check(legacy.getServerData().version.getString().equals(legacyProtocol.getClass().getMethod("getName").invoke(legacyProtocol)),"legacy row shows selected protocol version");
   check(legacySplits>0,"ViaFabricPlus replacement address passed through Font.split: "+legacySplits);
   org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();''')
smoke.EXTRA_SOURCES.append(('LegacyFontProbe', r'''package io.github.halfmasa.multiplayersmoke.mixin;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Font.class) public class LegacyFontProbe {
 @Inject(method="split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",at=@At("RETURN"))
 private void split(FormattedText text,int width,CallbackInfoReturnable<java.util.List<FormattedCharSequence>> cir){
  if(text.getString().equals("legacy.example.invalid")&&width>0&&!cir.getReturnValue().isEmpty())io.github.halfmasa.multiplayersmoke.MultiplayerSmoke.legacySplits++;
 }
}
'''))
raise SystemExit(smoke.main())

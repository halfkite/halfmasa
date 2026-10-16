"""Verify independent world previews with real MiniHUD, native MaLiLib keys and a test chest."""
import json
from pathlib import Path
import run_minihud_preview_smoke as harness

SOURCE = r'''
package io.github.halfmasa.previewsmoke;
import java.util.*;
import java.lang.reflect.Field;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraft.util.profiling.InactiveProfiler;
import fi.dy.masa.malilib.hotkeys.*;
import fi.dy.masa.malilib.render.*;
import fi.dy.masa.minihud.event.RenderHandler;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
public class PreviewSmoke implements ClientModInitializer {
 public static int worldCalls,textCalls,checks;public static InventoryOverlayContext last;
 private static PreviewSmoke instance;private int stage,realCalls,realText;private long since;private BlockPos chest;
 private static final fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed MAIN=fi.dy.masa.minihud.config.Configs.Generic.MAIN_RENDERING_TOGGLE;
 private static final fi.dy.masa.malilib.config.options.ConfigBoolean PREVIEW=fi.dy.masa.minihud.config.Configs.Generic.INVENTORY_PREVIEW_ENABLED;
 private static final IKeybind KEY=fi.dy.masa.minihud.config.Hotkeys.INVENTORY_PREVIEW.getKeybind();
 public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);System.out.println("PREVIEW_INDEPENDENT_CHECK "+why);}
 static Field field(Class<?> c,String n)throws Exception{for(;c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(n);}
 @SuppressWarnings("unchecked") static List<Integer> keys()throws Exception{return (List<Integer>)field(KeybindMulti.class,"PRESSED_KEYS").get(null);}
 static void held(boolean held)throws Exception{keys().clear();if(held)keys().addAll(KEY.getKeys());KEY.updateIsPressed();}
 private void config()throws Exception{
  check(!Configs.MINIHUD_PREVIEW_INDEPENDENT.getBooleanValue(),"independent preview defaults off");check(Configs.MINIHUD_PREVIEW_INDEPENDENT.getKeybind().getKeys().isEmpty(),"toggle shortcut defaults unbound");
  check(Configs.EXTENSIONS.contains(Configs.MINIHUD_PREVIEW_INDEPENDENT)&&Configs.HOTKEYS.contains(Configs.MINIHUD_PREVIEW_INDEPENDENT),"option registered in Extensions and hotkey manager");
  var callback=((KeybindMulti)Configs.MINIHUD_PREVIEW_INDEPENDENT.getKeybind()).getCallback();check(callback!=null&&callback.onKeyAction(KeyAction.PRESS,Configs.MINIHUD_PREVIEW_INDEPENDENT.getKeybind())&&Configs.MINIHUD_PREVIEW_INDEPENDENT.getBooleanValue(),"native toggle callback enables feature");callback.onKeyAction(KeyAction.PRESS,Configs.MINIHUD_PREVIEW_INDEPENDENT.getKeybind());check(!Configs.MINIHUD_PREVIEW_INDEPENDENT.getBooleanValue(),"native toggle callback disables feature");
  Configs.MINIHUD_PREVIEW_INDEPENDENT.getKeybind().setValueFromString("F8");Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(true);new Configs().save();Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(false);Configs.MINIHUD_PREVIEW_INDEPENDENT.getKeybind().resetToDefault();new Configs().load();check(Configs.MINIHUD_PREVIEW_INDEPENDENT.getBooleanValue()&&Configs.MINIHUD_PREVIEW_INDEPENDENT.getKeybind().getStringValue().equals("F8"),"state and shortcut survive save and reload");Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(false);
  KEY.setValueFromString("LEFT_SHIFT,F9");check(KEY.getKeys().size()==2,"original preview supports configured modifier combination");MAIN.setBooleanValue(false);PREVIEW.setBooleanValue(true);
 }
 private void tick(Minecraft mc){try{
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;config();stage=1;mc.createWorldOpenFlows().createFreshLevel("independent-fixture",new LevelSettings("Independent Preview Fixture",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;}
  if(mc.level==null||mc.player==null)return;
  if(stage==1){if(mc.level.getGameTime()<40)return;chest=mc.player.blockPosition().offset(0,0,3);var pos=chest;double pitch=Math.toDegrees(Math.atan2(mc.player.getEyeY()-(pos.getY()+.5),3));mc.player.setYRot(0);mc.player.setXRot((float)pitch);mc.getSingleplayerServer().submit(()->{var actor=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var world=actor.level();world.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);var box=(ChestBlockEntity)world.getBlockEntity(pos);box.setItem(0,new ItemStack(Items.DIAMOND,17));box.setItem(26,new ItemStack(Items.EMERALD,5));actor.setYRot(0);actor.setXRot((float)pitch);}).get(5,java.util.concurrent.TimeUnit.SECONDS);stage=2;since=mc.level.getGameTime();return;}
  if(stage==2&&mc.level.getGameTime()-since>20){stage=3;MinecraftClientCompat.setScreen(mc,new MatrixScreen(this));return;}
  if(stage==4){realCalls=worldCalls;realText=textCalls;stage=5;return;}
  if(stage==5){check(worldCalls==realCalls+1,"actual registered overlay callback renders exactly once per frame");check(textCalls==realText&&!MAIN.getBooleanValue(),"actual overlay leaves MiniHUD info lines and main state off");net.minecraft.client.Screenshot.grab(mc,false);System.out.println("PREVIEW_WORLD_PASS independent_checks="+checks);stage=6;return;}
  if(stage==6){held(false);mc.stop();stage=7;}
 }catch(Throwable e){throw new IllegalStateException("Independent preview stage="+stage,e);}}
 static class MatrixScreen extends Screen {
  final PreviewSmoke owner;boolean ran;int warmup;
  MatrixScreen(PreviewSmoke owner){super(Component.literal("Independent Preview Matrix"));this.owner=owner;}
  public boolean isPauseScreen(){return false;}
  public void extractRenderState(GuiGraphicsExtractor gui,int mx,int my,float delta){if(ran)return;try{var ctx=GuiContext.fromGuiGraphics(gui);if(!warm(ctx))return;ran=true;tests(ctx);}catch(Throwable e){throw new IllegalStateException("Independent preview matrix",e);}}
  boolean warm(GuiContext ctx)throws Exception{var mc=Minecraft.getInstance();MinecraftClientCompat.setScreen(mc,null);MAIN.setBooleanValue(true);Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(false);PREVIEW.setBooleanValue(true);held(true);call(ctx);MinecraftClientCompat.setScreen(mc,this);boolean ready=last!=null&&last.inv()!=null&&last.inv().getItem(0).is(Items.DIAMOND)&&last.inv().getItem(0).getCount()==17&&last.inv().getItem(26).is(Items.EMERALD);if(!ready&&++warmup>90)throw new AssertionError("native MiniHUD chest data did not synchronize");if(ready)check(true,"native preview has authoritative chest contents before matrix");return ready;}
  void call(GuiContext ctx){RenderHandler.getInstance().onExtractGuiOverlayPost(ctx,0,InactiveProfiler.INSTANCE);}
  void tests(GuiContext ctx)throws Exception {
   var mc=Minecraft.getInstance();MinecraftClientCompat.setScreen(mc,null);field(mc.getWindow().getClass(),"focused").setBoolean(mc.getWindow(),true);mc.hitResult=new BlockHitResult(Vec3.atCenterOf(owner.chest),Direction.NORTH,owner.chest,false);
   held(false);Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(false);fi.dy.masa.malilib.event.InputEventHandler.getKeybindManager().updateUsedKeys();var input=(fi.dy.masa.malilib.event.InputEventHandler)fi.dy.masa.malilib.event.InputEventHandler.getInputManager();int code=Configs.MINIHUD_PREVIEW_INDEPENDENT.getKeybind().getKeys().getFirst();var event=new net.minecraft.client.input.KeyEvent(code,0,0);KeybindMulti.onKeyInputPre(event,com.mojang.blaze3d.platform.InputConstants.PRESS);input.onKeyInput(event,com.mojang.blaze3d.platform.InputConstants.PRESS,mc);check(Configs.MINIHUD_PREVIEW_INDEPENDENT.getBooleanValue(),"registered toggle responds to native MaLiLib key press");KeybindMulti.onKeyInputPre(event,com.mojang.blaze3d.platform.InputConstants.REPEAT);input.onKeyInput(event,com.mojang.blaze3d.platform.InputConstants.REPEAT,mc);KeybindMulti.onKeyInputPre(event,com.mojang.blaze3d.platform.InputConstants.RELEASE);input.onKeyInput(event,com.mojang.blaze3d.platform.InputConstants.RELEASE,mc);check(Configs.MINIHUD_PREVIEW_INDEPENDENT.getBooleanValue(),"repeat and release do not toggle twice");held(false);
   for(boolean main:List.of(false,true))for(boolean independent:List.of(false,true))for(boolean preview:List.of(false,true))for(boolean held:List.of(false,true)){
    MAIN.setBooleanValue(main);Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(independent);PREVIEW.setBooleanValue(preview);held(held);check(KEY.isKeybindHeld()==held,"original key held state "+held);
    int calls=worldCalls,lines=textCalls;call(ctx);int expected=(main||independent)&&preview&&held?1:0;
    check(worldCalls-calls==expected,"native dispatch count main="+main+" independent="+independent+" preview="+preview+" held="+held);
    check(MAIN.getBooleanValue()==main,"main config unchanged main="+main+" independent="+independent);
    if(!main)check(textCalls==lines,"no info lines with main off independent="+independent);
    if(expected==1){check(last!=null&&last.inv()!=null&&last.be()!=null&&last.be().getBlockPos().equals(owner.chest),"native handler targets real world chest");check(last.inv().getItem(0).is(Items.DIAMOND)&&last.inv().getItem(0).getCount()==17&&last.inv().getItem(26).is(Items.EMERALD),"real chest first and last slots preserved");}
   }
   MAIN.setBooleanValue(false);Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(true);PREVIEW.setBooleanValue(true);held(true);
   for(boolean optimized:List.of(false,true)){Configs.MINIHUD_CONTAINER_PREVIEW_OPTIMIZATION.setBooleanValue(optimized);int n=worldCalls;call(ctx);check(worldCalls==n+1&&last!=null,"independent preview works with optimization="+optimized);}
   int n=worldCalls;held(false);call(ctx);check(worldCalls==n,"releasing original preview key stops preview immediately");
   held(true);Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(false);call(ctx);check(worldCalls==n,"disabling independent preview restores original main-off behavior");
   Configs.MINIHUD_PREVIEW_INDEPENDENT.setBooleanValue(true);PREVIEW.setBooleanValue(false);call(ctx);check(worldCalls==n,"original MiniHUD preview switch takes effect immediately");
   PREVIEW.setBooleanValue(true);call(ctx);check(worldCalls==n+1,"runtime re-enabling resumes preview once");
   // Do not add another draw here: the next frames exercise MiniHUD's registered event path.
   owner.stage=4;
  }
 }
}
'''

WORLD_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import fi.dy.masa.minihud.renderer.InventoryOverlayHandler;
import fi.dy.masa.malilib.render.InventoryOverlayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=InventoryOverlayHandler.class,remap=false) public class DrawProbeMixin {
 @Inject(method="getRenderContext(Lfi/dy/masa/malilib/render/GuiContext;Lnet/minecraft/util/profiling/ProfilerFiller;)Lfi/dy/masa/malilib/render/InventoryOverlayContext;",at=@At("HEAD")) private void count(CallbackInfoReturnable<InventoryOverlayContext> ci){PreviewSmoke.worldCalls++;}
 @Inject(method="getRenderContext(Lfi/dy/masa/malilib/render/GuiContext;Lnet/minecraft/util/profiling/ProfilerFiller;)Lfi/dy/masa/malilib/render/InventoryOverlayContext;",at=@At("RETURN")) private void result(CallbackInfoReturnable<InventoryOverlayContext> ci){PreviewSmoke.last=ci.getReturnValue();}
}
'''
TEXT_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import fi.dy.masa.malilib.render.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=RenderUtils.class,remap=false) public class ProbeMixin {
 @Inject(method="renderText(Lfi/dy/masa/malilib/render/GuiContext;IIDIILfi/dy/masa/malilib/config/HudAlignment;ZZZLjava/util/List;)I",at=@At("HEAD")) private static void lines(CallbackInfoReturnable<Integer> ci){PreviewSmoke.textCalls++;}
}
'''
MAP_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import fi.dy.masa.minihud.event.RenderHandler;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=RenderHandler.class,remap=false) public class MapProbeMixin {}
'''


def main():
    harness.SOURCE = SOURCE
    harness.PROBE = TEXT_PROBE
    harness.DRAW_PROBE = WORLD_PROBE
    harness.MAP_PROBE = MAP_PROBE
    # Restrict only this graphics fixture's worker count; leave production rendering unchanged.
    original_command = harness.startup.production_command
    def command(*args, **kwargs):
        result = original_command(*args, **kwargs)
        result[1:1] = ['-XX:ActiveProcessorCount=2']
        return result
    harness.startup.production_command = command
    folder = harness.ROOT / 'build/minihud-preview-tests'
    before = set(folder.iterdir()) if folder.exists() else set()
    result = harness.main()
    for directory in set(folder.iterdir()) - before:
        report = directory / 'report.json'
        if report.exists():
            data = json.loads(report.read_text(encoding='utf-8'))
            data['test_kind'] = 'MiniHUD independent preview: native world chest, key and toggle matrix'
            data.pop('measurement', None)
            report.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    return result


if __name__ == '__main__':
    raise SystemExit(main())

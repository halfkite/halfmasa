"""Exercise 26.3 creative grouping against live vanilla registries, tags and screens."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import zipfile

import run_startup_smoke as startup
import run_entity_render_disable_smoke as entity_smoke

ROOT = Path(__file__).resolve().parents[2]
SOURCE = r'''package io.github.halfmasa.condensedcreativesmoke;
import java.util.*;
import java.lang.reflect.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.item.*;
import io.github.halfmasa.xaerobinding.config.*;
import io.github.halfmasa.xaerobinding.feature.*;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

public class CondensedCreativeSmoke implements ClientModInitializer {
 private static CondensedCreativeSmoke instance;
 private int stage,checks; private long since;
 @Override public void onInitializeClient(){instance=this;}
 public static void frame(Minecraft mc){if(instance!=null)instance.tick(mc);}
 private static Field field(Class<?> c,String n)throws Exception{for(;c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(n);}
 private static Method method(Class<?> c,String n,Class<?>...p)throws Exception{for(;c!=null;c=c.getSuperclass())try{var m=c.getDeclaredMethod(n,p);m.setAccessible(true);return m;}catch(NoSuchMethodException ignored){}throw new NoSuchMethodException(n);}
 private void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);System.out.println("CONDENSED_CHECK "+why);}
 private ItemStack stack(String id){var key=Identifier.withDefaultNamespace(id);if(!BuiltInRegistries.ITEM.containsKey(key))throw new AssertionError("missing fixture "+id);return new ItemStack(BuiltInRegistries.ITEM.getValue(key));}
 private CreativeModeTab tab(String id){return BuiltInRegistries.CREATIVE_MODE_TAB.getValue(Identifier.withDefaultNamespace(id));}
 private CreativeModeInventoryScreen open(Minecraft mc){var s=new CreativeModeInventoryScreen(mc.player,mc.level.enabledFeatures(),true);MinecraftClientCompat.setScreen(mc,s);return s;}
 private void select(CreativeModeInventoryScreen s,String id)throws Exception{method(s.getClass(),"selectTab",CreativeModeTab.class).invoke(s,tab(id));}
 private void query(CreativeModeInventoryScreen s,String text)throws Exception{((EditBox)field(s.getClass(),"searchBox").get(s)).setValue(text);method(s.getClass(),"refreshSearchResults").invoke(s);}
 private String key(String id,String tab)throws Exception{return (String)method(CondensedCreativeManager.class,"groupKey",ItemStack.class,String.class).invoke(null,stack(id),tab);}
 private void keyIs(String id,String tab,String expected)throws Exception{String actual=key(id,tab);check(Objects.equals(actual,expected),id+" / "+tab+" -> "+actual+" expected="+expected);}
 private List<ItemStack> copy(List<ItemStack> items){return items.stream().map(ItemStack::copy).toList();}
 private void sameItems(List<ItemStack> expected,List<ItemStack> actual,String why){
  var remaining=new ArrayList<>(expected);for(var item:actual){int found=-1;for(int i=0;i<remaining.size();i++)if(ItemStack.matches(item,remaining.get(i))){found=i;break;}if(found<0)throw new AssertionError(why+": unexpected "+item);remaining.remove(found);}check(remaining.isEmpty(),why+" count="+actual.size());
 }
 private List<ItemStack> parents(CreativeModeInventoryScreen s){return s.getMenu().items.stream().filter(i->CondensedCreativeManager.isParent(s,i)).map(ItemStack::copy).toList();}
 private void click(CreativeModeInventoryScreen s,int index)throws Exception{method(s.getClass(),"slotClicked",Slot.class,int.class,int.class,ContainerInput.class).invoke(s,s.getMenu().getSlot(index),index,0,ContainerInput.PICKUP);}
 private void configuration(){
  check(!Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.getBooleanValue(),"fresh rotating preview defaults off");
  check(!Configs.CONDENSED_CREATIVE_BACKGROUND.getBooleanValue(),"fresh entry background defaults off");
  check(Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.getBooleanValue(),"session expansion memory defaults on");
  check(Configs.getExpansionChildren(Configs.CONDENSED_CREATIVE).contains(Configs.CONDENSED_CREATIVE_KEEP_EXPANDED)&&Configs.PORTED.contains(Configs.CONDENSED_CREATIVE_KEEP_EXPANDED),"session-memory option is registered for persistence and expandable settings");
  check(Configs.CONDENSED_CREATIVE_GROUPING.getOptionListValue()==CondensedCreativeGrouping.DEFAULT,"existing grouping remains default");
  check(Configs.getExpansionChildren(Configs.CONDENSED_CREATIVE).contains(Configs.CONDENSED_CREATIVE_GROUPING)&&Configs.PORTED.contains(Configs.CONDENSED_CREATIVE_GROUPING),"mode registered for persistence and expandable settings");
  check(CondensedCreativeGrouping.DEFAULT.fromString(null)==CondensedCreativeGrouping.DEFAULT&&CondensedCreativeGrouping.DEFAULT.fromString("unknown")==CondensedCreativeGrouping.DEFAULT,"malformed mode falls back safely");
  Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.MATERIAL);Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.setBooleanValue(true);Configs.CONDENSED_CREATIVE_BACKGROUND.setBooleanValue(true);Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.setBooleanValue(false);new Configs().save();
  Configs.CONDENSED_CREATIVE_GROUPING.resetToDefault();Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.resetToDefault();Configs.CONDENSED_CREATIVE_BACKGROUND.resetToDefault();Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.resetToDefault();new Configs().load();
  check(Configs.CONDENSED_CREATIVE_GROUPING.getOptionListValue()==CondensedCreativeGrouping.MATERIAL&&Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.getBooleanValue(),"saved mode and explicitly enabled rotation survive reload");
  check(Configs.CONDENSED_CREATIVE_BACKGROUND.getBooleanValue()&&!Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.getBooleanValue(),"explicit background and memory preferences survive reload");
  Configs.CONDENSED_CREATIVE_BACKGROUND.resetToDefault();Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.resetToDefault();Configs.CONDENSED_CREATIVE_GROUPING.resetToDefault();Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.resetToDefault();Configs.CONDENSED_CREATIVE.setBooleanValue(true);Configs.KEEP_SCREEN_POSITION.setBooleanValue(false);new Configs().save();
 }
 private void coverage()throws Exception{
  for(String wood:List.of("pale_oak","poplar"))keyIs(wood+"_planks","building_blocks","wood:"+wood);
  for(String id:List.of("bamboo_block","stripped_bamboo_block","bamboo_mosaic","bamboo_mosaic_stairs","bamboo_mosaic_slab"))keyIs(id,"building_blocks","wood:bamboo");
  for(String mat:List.of("resin","sulfur","cinnabar"))keyIs(mat+"_brick_stairs","building_blocks","material:"+mat);
  for(String shape:List.of("wool_stairs","wool_slab","concrete_stairs","concrete_slab","cushion")){
   for(String color:List.of("white","light_gray","light_blue","red","black"))keyIs(color+"_"+shape,"colored_blocks","colored:"+shape);
  }
  keyIs("oak_shelf","functional_blocks","type:shelves");keyIs("pale_oak_shelf","functional_blocks","type:shelves");keyIs("poplar_shelf","functional_blocks","type:shelves");
  keyIs("mangrove_propagule","natural_blocks","type:saplings");keyIs("open_eyeblossom","natural_blocks","type:flowers");keyIs("cactus_flower","natural_blocks","type:flowers");
  keyIs("oxidized_copper_lantern","functional_blocks","type:lanterns");keyIs("oxidized_copper_chain","functional_blocks","type:chains");
  keyIs("red_sandstone_stairs","building_blocks","material:red_sandstone");keyIs("mossy_cobblestone_stairs","building_blocks","material:stone");
  keyIs("cinnabar","search","material:cinnabar");keyIs("copper_pickaxe","search",null);keyIs("copper_ingot","search",null);
  Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.MATERIAL);
  for(String id:List.of("oak_log","oak_planks","oak_stairs","oak_shelf","oak_door","oak_sapling","oak_leaves"))keyIs(id,"search","wood:oak");
  for(String id:List.of("copper_block","waxed_oxidized_copper_bulb","copper_lantern","copper_golem_statue"))keyIs(id,"search","material:copper");
  for(String id:List.of("white_wool","red_wool_stairs","black_wool_slab","blue_carpet"))keyIs(id,"search","material:wool");
  keyIs("copper_axe","search",null);keyIs("iron_ingot","search",null);keyIs("infested_stone","search","type:infested_blocks");
  keyIs("deepslate_iron_ore","natural_blocks","material:iron");keyIs("white_cushion","functional_blocks","colored:cushion");keyIs("red_bundle","tools_and_utilities","colored:bundle");
 }
 private void auditTabs(Minecraft mc)throws Exception{
  var s=open(mc);
  for(var mode:CondensedCreativeGrouping.values()){
   Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(mode);
   for(String id:List.of("building_blocks","colored_blocks","natural_blocks","functional_blocks","redstone_blocks","tools_and_utilities","combat","food_and_drinks","ingredients","spawn_eggs")){
    select(s,id);var source=tab(id).getDisplayItems().stream().map(ItemStack::copy).toList();
    var icons=parents(s);for(var icon:icons)check(CondensedCreativeManager.toggle(s,icon,s.getMenu().items),"expand "+id+" / "+mode);
    var visible=s.getMenu().items.stream().filter(i->!CondensedCreativeManager.isParent(s,i)).toList();sameItems(source,visible,"all original stacks retained exactly once "+id+" / "+mode);
    for(var icon:icons)check(CondensedCreativeManager.toggle(s,icon,s.getMenu().items),"collapse "+id+" / "+mode);
   }
  }
  var one=NonNullList.<ItemStack>create();one.add(stack("white_wool_stairs"));CondensedCreativeManager.rebuild(s,CreativeModeTabs.searchTab(),one);
  check(one.size()==1&&ItemStack.matches(one.getFirst(),stack("white_wool_stairs")),"single search match stays a normal unmodified item");
  var empty=NonNullList.<ItemStack>create();CondensedCreativeManager.rebuild(s,CreativeModeTabs.searchTab(),empty);check(empty.isEmpty(),"empty results do not manufacture icons");
 }
 private void interaction(Minecraft mc)throws Exception{
  var s=open(mc);Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.DEFAULT);select(s,"functional_blocks");
  var normal=parents(s);Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.MATERIAL);s.containerTick();
  check(!parents(s).equals(normal)&&key("oak_shelf","functional_blocks").equals("wood:oak"),"mode switch rebuilds immediately with rotation off");
  select(s,"colored_blocks");click(s,0);check(s.getMenu().getCarried().isEmpty(),"clicking a group expands without carrying its display icon");
  var expected=s.getMenu().getSlot(1).getItem().copy();click(s,1);check(ItemStack.matches(expected,s.getMenu().getCarried()),"expanded child pickup preserves original components and count");s.getMenu().setCarried(ItemStack.EMPTY);
  mc.player.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));mc.player.getInventory().setItem(8,new ItemStack(Items.COPPER_INGOT,29));
  var inventory=new ArrayList<ItemStack>();for(int i=0;i<36;i++)inventory.add(mc.player.getInventory().getItem(i).copy());
  Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.setBooleanValue(true);
  for(int i=0;i<5;i++){s.containerTick();select(s,"functional_blocks");s.containerTick();select(s,"colored_blocks");}
  var after=new ArrayList<ItemStack>();for(int i=0;i<36;i++)after.add(mc.player.getInventory().getItem(i).copy());sameItems(inventory,after,"rotating previews and tab changes preserve player inventory");
  Configs.CONDENSED_CREATIVE_ROTATING_PREVIEW.setBooleanValue(false);s.containerTick();
 }
 private ItemStack parent(CreativeModeInventoryScreen s,String key){return parents(s).stream().filter(i->CondensedCreativeManager.getVisual(s,i).key().equals(key)).findFirst().orElseThrow();}
 private boolean expanded(CreativeModeInventoryScreen s,String key){return CondensedCreativeManager.getVisual(s,parent(s,key)).expanded();}
 private CreativeModeInventoryScreen reopen(Minecraft mc,String tab)throws Exception{MinecraftClientCompat.setScreen(mc,null);var s=open(mc);select(s,tab);return s;}
 private void sessionMemory(Minecraft mc)throws Exception{
  Configs.CONDENSED_CREATIVE_GROUPING.resetToDefault();var s=reopen(mc,"colored_blocks");
  check(!expanded(s,"colored:wool"),"new session starts with collapsed wool entries");
  check(CondensedCreativeManager.toggle(s,parent(s,"colored:wool"),s.getMenu().items),"expand wool before closing inventory");
  var expected=copy(s.getMenu().items);s=reopen(mc,"colored_blocks");
  check(expanded(s,"colored:wool"),"opening a new inventory screen restores expanded entries");sameItems(expected,s.getMenu().items,"reopened expanded list retains original components and counts");
  @SuppressWarnings("unchecked") var tooltip=(List<net.minecraft.network.chat.Component>)method(s.getClass(),"getTooltipFromContainerItem",ItemStack.class).invoke(s,parent(s,"colored:wool"));
  String source=net.minecraft.network.chat.Component.translatable("halfmasa.feature.condensed_creative.source").getString();
  String hint=CondensedCreativeManager.getParentHint(s,parent(s,"colored:wool")).getString();
  check(tooltip.stream().noneMatch(c->c.getString().equals(source))&&tooltip.stream().anyMatch(c->c.getString().equals(hint)),"real inventory tooltip retains the action hint without source attribution");
  CondensedCreativeManager.toggle(s,parent(s,"colored:wool"),s.getMenu().items);s=reopen(mc,"colored_blocks");check(!expanded(s,"colored:wool"),"manually collapsed entries stay collapsed after reopening");
  CondensedCreativeManager.toggle(s,parent(s,"colored:wool"),s.getMenu().items);
  Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.MATERIAL);s.containerTick();check(!expanded(s,"material:wool"),"other grouping mode starts with independent expansion state");
  CondensedCreativeManager.toggle(s,parent(s,"material:wool"),s.getMenu().items);s=reopen(mc,"colored_blocks");check(expanded(s,"material:wool"),"material-mode expansion survives screen closure");
  Configs.CONDENSED_CREATIVE_GROUPING.resetToDefault();s.containerTick();check(expanded(s,"colored:wool"),"default-mode expansion is also retained after reopening");
  Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.MATERIAL);s.containerTick();select(s,"search");query(s,"wool");check(!expanded(s,"material:wool"),"search and colored tabs have independent expansion states");
  CondensedCreativeManager.toggle(s,parent(s,"material:wool"),s.getMenu().items);query(s,"no_such_item_728491");check(s.getMenu().items.isEmpty(),"remembered expansion never manufactures results for an empty query");
  query(s,"wool");check(expanded(s,"material:wool"),"expansion returns when filtered search results reappear");s=reopen(mc,"search");query(s,"wool");check(expanded(s,"material:wool"),"search expansion survives inventory closure");
  Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.setBooleanValue(false);s.containerTick();check(expanded(s,"material:wool"),"disabling memory does not close the currently expanded group");
  s=reopen(mc,"search");query(s,"wool");check(!expanded(s,"material:wool"),"memory disabled makes reopened inventories start collapsed");
  CondensedCreativeManager.toggle(s,parent(s,"material:wool"),s.getMenu().items);Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.setBooleanValue(true);s.containerTick();s=reopen(mc,"search");query(s,"wool");check(expanded(s,"material:wool"),"enabling memory remembers a group already expanded on screen");
  Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.setBooleanValue(false);s=reopen(mc,"colored_blocks");Configs.CONDENSED_CREATIVE_KEEP_EXPANDED.resetToDefault();Configs.CONDENSED_CREATIVE_GROUPING.resetToDefault();s.containerTick();
 }
 private void searchHistory(Minecraft mc)throws Exception{
  Configs.ITEM_SEARCH_HISTORY.setBooleanValue(false);Configs.CONDENSED_CREATIVE.setBooleanValue(false);var s=open(mc);select(s,"search");query(s,"wool");var source=copy(s.getMenu().items);
  check(source.size()>16,"live search includes newly added wool forms");ItemSearchHistoryService.getInstance().record(ItemSearchHistoryService.Channel.CREATIVE,stack("diamond"));
  Configs.CONDENSED_CREATIVE.setBooleanValue(true);Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.DEFAULT);Configs.ITEM_SEARCH_HISTORY.setBooleanValue(true);Configs.ITEM_SEARCH_HISTORY_DURING_SEARCH.setBooleanValue(true);s.containerTick();
  int prefix=Configs.ITEM_SEARCH_HISTORY_ROWS.getIntegerValue()*9;check(CreativeItemSearchHistory.isApplied(s),"history applies alongside condensed results");
  Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.MATERIAL);s.containerTick();check(s.getMenu().items.size()==prefix+1,"material switch retains exactly one history prefix and one wool group");
  click(s,prefix);check(s.getMenu().items.size()==prefix+1+source.size(),"expansion through the real screen keeps all search matches and one history prefix");
  Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.DEFAULT);s.containerTick();check(s.getMenu().items.size()<prefix+source.size(),"default mode has independent collapsed expansion state");
  Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.MATERIAL);s.containerTick();check(s.getMenu().items.size()==prefix+1+source.size(),"returning to material mode restores its expansion state");
  Configs.CONDENSED_CREATIVE.setBooleanValue(false);s.containerTick();check(s.getMenu().items.size()==prefix+source.size(),"disabling grouping keeps one history prefix");
  Configs.ITEM_SEARCH_HISTORY.setBooleanValue(false);s.containerTick();sameItems(source,s.getMenu().items,"disabling both restores the original search results");
  query(s,"no_such_item_728491");check(s.getMenu().items.isEmpty(),"empty live query remains empty");
  Configs.CONDENSED_CREATIVE.setBooleanValue(true);Configs.CONDENSED_CREATIVE_GROUPING.resetToDefault();open(mc);select((CreativeModeInventoryScreen)MinecraftClientCompat.getScreen(mc),"colored_blocks");
 }
 private void tick(Minecraft mc){try{
  if(stage==0){if(!(MinecraftClientCompat.getScreen(mc) instanceof TitleScreen)||mc.gui.overlay()!=null)return;configuration();stage=1;mc.createWorldOpenFlows().createFreshLevel("condensed-creative-fixture",new LevelSettings("Condensed Creative Fixture",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(123L,false,false),WorldPresets::createTestWorldDimensions,MinecraftClientCompat.getScreen(mc));return;}
  if(mc.level==null||mc.player==null)return;
  if(stage==1){if(mc.level.getGameTime()<40)return;coverage();auditTabs(mc);sessionMemory(mc);interaction(mc);searchHistory(mc);since=mc.level.getGameTime();stage=2;return;}
  if(stage==2&&mc.level.getGameTime()-since>=8){net.minecraft.client.Screenshot.grab(mc,false);since=mc.level.getGameTime();stage=3;return;}
  // Screenshot readback is asynchronous: leave the captured screen unchanged until the next stage.
  if(stage==3&&mc.level.getGameTime()-since>=8){Configs.CONDENSED_CREATIVE_GROUPING.setOptionListValue(CondensedCreativeGrouping.MATERIAL);((CreativeModeInventoryScreen)MinecraftClientCompat.getScreen(mc)).containerTick();since=mc.level.getGameTime();stage=4;return;}
  if(stage==4&&mc.level.getGameTime()-since>=12){
   var s=(CreativeModeInventoryScreen)MinecraftClientCompat.getScreen(mc);
   var wool=stack("white_wool").getItem();
   check(s.getMenu().items.stream().anyMatch(i->i.is(wool)&&CondensedCreativeManager.isParent(s,i)),"material mode retains the wool icon after reopening and changing modes");
   check(s.getMenu().getSlot(0).getItem().is(wool),"first visible slot stays aligned with the wool group after list shrink");
   net.minecraft.client.Screenshot.grab(mc,false);since=mc.level.getGameTime();stage=5;return;
  }
  if(stage==5&&mc.level.getGameTime()-since>=8){System.out.println("CONDENSED_CREATIVE_WORLD_PASS checks="+checks);stage=6;mc.stop();}
 }catch(Throwable error){throw new IllegalStateException("Condensed creative smoke stage="+stage,error);}}
}
'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, default=Path('C:/Program Files/Java/jdk-25.0.3'))
    parser.add_argument('--with-api', action='store_true')
    parser.add_argument('--timeout', type=int, default=180)
    args = parser.parse_args()
    args.artifact = args.artifact.resolve()
    if not args.artifact.is_file():
        parser.error(f'Artifact does not exist: {args.artifact}')
    args.version = '26.3'
    directory = ROOT / 'build/condensed-creative-tests' / datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    classes = directory / 'classes'
    classes.mkdir(parents=True)
    frame = entity_smoke.FRAME.replace('entitydisablesmoke', 'condensedcreativesmoke').replace('EntityDisableSmoke', 'CondensedCreativeSmoke')
    clipboard = entity_smoke.CLIPBOARD.replace('entitydisablesmoke', 'condensedcreativesmoke')
    sources = []
    for name, text in [('CondensedCreativeSmoke', SOURCE), ('FrameMixin', frame), ('ClipboardMixin', clipboard)]:
        path = directory / (name + '.java')
        path.write_text(text, encoding='utf-8')
        sources.append(str(path))
    cp = str(args.artifact) + os.pathsep + (ROOT / 'build/port-classpath-26.3.txt').read_text().strip()
    compile_result = subprocess.run([str(args.java_home / 'bin/javac.exe'), '-J-Dfile.encoding=UTF-8', '-J-Duser.language=en', '-proc:none', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), *sources], capture_output=True)
    if compile_result.returncode:
        diagnostic = compile_result.stderr.decode('utf-8', errors='replace')
        (directory / 'compile.log').write_text(diagnostic, encoding='utf-8')
        print(diagnostic)
        return 1
    mod = directory / 'condensed-creative-smoke.jar'
    with zipfile.ZipFile(mod, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in classes.rglob('*.class'):
            jar.write(path, path.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'halfmasa_condensed_creative_smoke', 'version': '1.0', 'environment': 'client', 'entrypoints': {'client': ['io.github.halfmasa.condensedcreativesmoke.CondensedCreativeSmoke']}, 'mixins': ['condensed-creative-smoke.mixins.json']}))
        jar.writestr('condensed-creative-smoke.mixins.json', json.dumps({'required': True, 'package': 'io.github.halfmasa.condensedcreativesmoke.mixin', 'compatibilityLevel': 'JAVA_25', 'client': ['FrameMixin', 'ClipboardMixin'], 'injectors': {'defaultRequire': 1}}))
    game = directory / 'game'
    (game / 'config/halfmasa').mkdir(parents=True)
    (game / 'mods').mkdir()
    (game / 'config/malilib.json').write_text('{}\n')
    (game / 'config/halfmasa/halfmasa.json').write_text('{}\n')
    (game / 'options.txt').write_text('onboardAccessibility:false\nenableVsync:false\nmaxFps:30\nrenderDistance:3\nsimulationDistance:5\npauseOnLostFocus:false\nsoundCategory_master:0.0\nguiScale:2\nlang:en_us\ntutorialStep:none\n')
    if args.with_api:
        api = next(p for p in (Path.home() / '.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.161.0+26.3').glob('*/*.jar') if not p.name.endswith(('-sources.jar', '-javadoc.jar')))
        shutil.copy2(api, game / 'mods' / api.name)
    command = startup.production_command(args, game, mod)
    command[1:1] = ['-Djava.awt.headless=true', '-Dsun.java2d.d3d=false', '-XX:ActiveProcessorCount=4', f'-XX:ErrorFile={directory / "jvm-crash.log"}']
    command[command.index('--width') + 1] = '1120'
    command[command.index('--height') + 1] = '720'
    log = directory / 'launcher.log'
    started = time.monotonic()
    timed_out = False
    with log.open('w', encoding='utf-8') as output:
        process = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.DEVNULL, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            code = process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            timed_out = True
            subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
            code = process.wait(timeout=30)
    text = log.read_text(encoding='utf-8', errors='replace')
    passed = code == 0 and not timed_out and 'CONDENSED_CREATIVE_WORLD_PASS' in text
    report = {'result': 'PASS' if passed else 'FAIL', 'game_version': args.version, 'artifact': str(args.artifact), 'fabric_api_installed': args.with_api, 'world_graphics_enabled': True, 'exit_code': code, 'timed_out': timed_out, 'duration_seconds': round(time.monotonic() - started, 1), 'log': str(log), 'checks': [line for line in text.splitlines() if 'CONDENSED_' in line], 'screenshots': [str(p) for p in (game / 'screenshots').glob('*.png')]}
    report_path = directory / 'report.json'
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print('CONDENSED CREATIVE:', report['result'])
    print(report_path)
    if not passed:
        failures = [line for line in text.splitlines() if 'AssertionError' in line or 'Caused by:' in line]
        print('\n'.join(failures + text.splitlines()[-15:]))
    return 0 if passed else 1


if __name__ == '__main__':
    raise SystemExit(main())

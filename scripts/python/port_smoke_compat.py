"""Version adjustments for test-only fixtures; production compatibility stays in Java."""
from pathlib import Path
import hashlib
import re
import smoke_remap

ROOT = Path(__file__).resolve().parents[2]


def source(text, version, classpath):
    if version == '26.3':
        return text
    text = text.replace('new LevelSettings.DifficultySettings(Difficulty.NORMAL,false,false),true,WorldDataConfiguration.DEFAULT',
                        'new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT')
    if version == '26.1.2' or version.startswith('1.21.'):
        text = text.replace('mc.gui.overlay()', 'mc.getOverlay()')
        text = text.replace('WorldPresets::createTestWorldDimensions', 'WorldPresets::createFlatWorldDimensions')
        text = text.replace('net.minecraft.client.Screenshot.grab(mc,false)', 'net.minecraft.client.Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{})')
    if version.startswith('1.21.'):
        v = tuple(map(int, version.split('.')))
        mapping = smoke_remap.mappings(version, classpath)
        focus = smoke_remap.field_name(mapping, 'net/minecraft/client/Minecraft', 'windowActive')
        text = text.replace('field(mc.getWindow().getClass(),"focused").setBoolean(mc.getWindow(),true)',
                            'field(Minecraft.class,"' + focus + '").setBoolean(mc,true)')
        text = text.replace('onExtractGuiOverlayPost', 'onRenderGameOverlayPostAdvanced')
        text = text.replace('new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT', 'false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT')
        if v >= (1, 21, 11):
            text = text.replace('new GameRules()', 'new net.minecraft.world.level.gamerules.GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())')
        elif v > (1, 21, 1):
            text = text.replace('new GameRules()', 'new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures())')
        else:
            text = text.replace('WorldPresets::createFlatWorldDimensions', 'access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()')
        text = text.replace('GuiGraphicsExtractor', 'GuiGraphics').replace('void extractRenderState(', 'void render(')
        text = text.replace('ContainerInput', 'ClickType')
        text = text.replace('pages.setSelectedTab(', 'select(s,')
        if v < (1, 21, 11):
            text = text.replace('s.resize(s.width,s.height)', 's.resize(mc,s.width,s.height)')
        text = text.replace('net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen', 'net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen')
        text = text.replace('net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab', 'net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup')
        text = text.replace('net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents', 'net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents')
        text = text.replace('FabricCreativeModeTab.builder()', 'FabricItemGroup.builder()').replace('CreativeModeTabEvents.modifyOutputEvent(', 'ItemGroupEvents.modifyEntriesEvent(')
        if v < (1, 21, 11):
            text = text.replace('Identifier', 'ResourceLocation')
            text = text.replace('getValueWrapper()', 'getTextWrapper()').replace('setValueWrapper(', 'setTextWrapper(')
            text = text.replace('GuiContext.fromGuiGraphics(gui)', 'gui')
            text = text.replace('Lfi/dy/masa/malilib/render/GuiContext;', 'Lnet/minecraft/client/gui/GuiGraphics;')
            text = text.replace('Lfi/dy/masa/malilib/render/InventoryOverlayContext;', 'Lfi/dy/masa/malilib/render/InventoryOverlay$Context;')
            text = text.replace('import fi.dy.masa.malilib.render.InventoryOverlayContext;', 'import fi.dy.masa.malilib.render.InventoryOverlay;')
            text = text.replace('InventoryOverlayContext', 'InventoryOverlay.Context').replace('GuiContext', 'GuiGraphics')
            text = text.replace('Lnet/minecraft/util/profiling/ProfilerFiller;)Lfi/dy/masa/malilib/render/InventoryOverlay$Context;', 'Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/Minecraft;)Lfi/dy/masa/malilib/render/InventoryOverlay$Context;')
            text = text.replace('onRenderGameOverlayPostAdvanced(ctx,0,InactiveProfiler.INSTANCE)', 'onRenderGameOverlayPostAdvanced(ctx,0,InactiveProfiler.INSTANCE,Minecraft.getInstance())')
            for old, new in [('fi.dy.masa.malilib.util.data.tag.CompoundData', 'net.minecraft.nbt.CompoundTag'), ('fi.dy.masa.malilib.util.data.tag.ListData', 'net.minecraft.nbt.ListTag'), ('Lfi/dy/masa/malilib/util/data/tag/ListData;', 'Lnet/minecraft/nbt/ListTag;')]:
                text = text.replace(old, new)
            text = text.replace('getDataInventory(', 'getNbtInventory(').replace('fromDataList(', 'fromNbtList(')
            text = text.replace('fi.dy.masa.minihud.data.EntityDataManager', 'fi.dy.masa.minihud.data.EntitiesDataManager')
            refresh_call = 'fi.dy.masa.minihud.data.EntitiesDataManager.getInstance().refreshBlockEntityFromWorld(actor.level(),owner.chest);'
            if refresh_call in text:
                text = text.replace(refresh_call, 'refreshOldWorld(actor.level(),owner.chest);')
                text = text.replace('public class PreviewSmoke implements ClientModInitializer {', '''public class PreviewSmoke implements ClientModInitializer {
 static void refreshOldWorld(Level world,BlockPos pos){try{var refresh=fi.dy.masa.minihud.data.EntitiesDataManager.class.getDeclaredMethod("refreshBlockEntityFromWorld",Level.class,BlockPos.class);refresh.setAccessible(true);refresh.invoke(fi.dy.masa.minihud.data.EntitiesDataManager.getInstance(),world,pos);}catch(ReflectiveOperationException error){throw new IllegalStateException("Native MiniHUD world cache refresh failed",error);}}
''')
        if v < (1, 21, 10):
            text = text.replace('var event=new net.minecraft.client.input.KeyEvent(code,0,0);', '')
            text = text.replace('var event=new net.minecraft.client.input.KeyEvent(code,code,0);', '')
            text = re.sub(r'KeybindMulti\.onKeyInputPre\(event,([^;)]+)\)', r'KeybindMulti.onKeyInputPre(code,0,0,\1)', text)
            text = re.sub(r'input\.onKeyInput\(event,([^;)]+),mc\)', r'input.onKeyInput(code,0,0,\1,mc)', text)
        if v <= (1, 21, 5):
            # These versions preserve a sized empty container when the Items list exists.
            text = text.replace('empty==null', 'empty!=null&&empty.isEmpty()&&empty.getContainerSize()==27')
            text = text.replace('renderText(Lnet/minecraft/client/gui/GuiGraphics;IIDIILfi/dy/masa/malilib/config/HudAlignment;ZZZLjava/util/List;)I',
                                'renderText(IIDIILfi/dy/masa/malilib/config/HudAlignment;ZZZLjava/util/List;Lnet/minecraft/client/gui/GuiGraphics;)I')
            if '@Mixin(value=NbtInventory.class' in text and 'fromNbtList' in text:
                text = text.replace('import fi.dy.masa.malilib.util.nbt.NbtInventory;', 'import fi.dy.masa.malilib.util.InventoryUtils;')
                text = text.replace('value=NbtInventory.class', 'value=InventoryUtils.class')
                text = re.sub(r'method="fromNbtList\([^"\n]+\)[^"\n]+"',
                              'method="getNbtInventory(Lnet/minecraft/nbt/CompoundTag;ILnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/Container;"', text)
        if v < (1, 21, 2):
            text = text.replace('onRenderGameOverlayPostAdvanced(ctx,0,InactiveProfiler.INSTANCE,Minecraft.getInstance())', 'onRenderGameOverlayPost(ctx)')
            text = text.replace('BuiltInRegistries.ITEM.getValue(', 'BuiltInRegistries.ITEM.get(')
            text = text.replace('BuiltInRegistries.CREATIVE_MODE_TAB.getValue(', 'BuiltInRegistries.CREATIVE_MODE_TAB.get(')
        # Reflection string constants are not remapped by TinyRemapper.
        for owner, names in {
            'net/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen': ['selectedTab', 'scrollOffs', 'searchBox', 'selectTab', 'refreshSearchResults', 'slotClicked', 'getTooltipFromContainerItem'],
            'net/minecraft/client/gui/screens/inventory/AbstractContainerScreen': ['getTooltipFromContainerItem', 'slotClicked'],
        }.items():
            lines = mapping.read_text(encoding='utf-8').splitlines()
            current = None
            for line in lines:
                columns = line.split('\t')
                if columns[0] == 'c':
                    current = columns[-1]
                elif current == owner and len(columns) > 5 and columns[1] in ('m', 'f') and columns[-1] in names:
                    text = text.replace('"' + columns[-1] + '"', '"' + columns[-2] + '"')
    return text


def classpath(version, artifact):
    native = (ROOT / f'build/port-classpath-{version}.txt').read_text(encoding='utf-8').strip()
    first = ROOT / f'versions/{version}/build/classes/java/main' if version.startswith('1.21.') else artifact
    return str(first) + ';' + native


def remap_fixture(version, java_home, mod, cp, directory):
    if not version.startswith('1.21.'):
        return mod
    return smoke_remap.remap(java_home, mod, directory / (mod.stem + '-remapped.jar'),
                             smoke_remap.mappings(version, cp), cp, directory / 'remap.log')


def api_version(version):
    return dict(re.findall(r"'([\d.]+)': '([^']+)'", (ROOT / 'common.gradle').read_text(encoding='utf-8')))[version]


def named_mod(version, java_home, mod, cp):
    if not version.startswith('1.21.'):
        return mod
    digest = hashlib.sha256(mod.read_bytes()).hexdigest()[:16]
    directory = ROOT / 'build/compat-test-named' / version
    directory.mkdir(parents=True, exist_ok=True)
    target = directory / (mod.stem + '-' + digest + '.jar')
    if not target.is_file():
        smoke_remap.remap(java_home, mod, target, smoke_remap.mappings(version, cp), cp,
                         directory / (digest + '-remap.log'), 'intermediary', 'named')
    return target

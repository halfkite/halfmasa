"""Test-only reproduction of Gadget's public Mod Menu entry and dropdown lifecycle.

Uses the real MagicLib SelectorDropDownList; this is not a Gadget game port.
"""
import json
from pathlib import Path
import subprocess
import zipfile

SOURCES = {
    'GadgetHooks': '''package io.github.halfmasa.gadgetfixture;
public interface GadgetHooks { default void masa_gadget_mod$addFastSwitcherWidget(){} }
''',
    'GadgetPage': '''package io.github.halfmasa.gadgetfixture;
import java.util.List;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
public class GadgetPage extends GuiConfigsBase {
 public static final ConfigBoolean OPTION = new ConfigBoolean("gadgetFixtureOption", false);
 public GadgetPage(){super(10,52,"masa_gadget_mod",null,"Gadget compatibility fixture");}
 public List<ConfigOptionWrapper> getConfigs(){return List.of(new ConfigOptionWrapper(OPTION));}
}
''',
    'GadgetMenuApi': '''package io.github.halfmasa.gadgetfixture;
import com.terraformersmc.modmenu.api.*;
public class GadgetMenuApi implements ModMenuApi {
 public ConfigScreenFactory<?> getModConfigScreenFactory(){return parent -> new GadgetPage();}
}
''',
    'GadgetBaseMixin': '''package io.github.halfmasa.gadgetfixture.mixin;
import io.github.halfmasa.gadgetfixture.GadgetHooks;
import fi.dy.masa.malilib.gui.GuiBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GuiBase.class,remap=false)
public class GadgetBaseMixin implements GadgetHooks {
 @Inject(method="initGui",at=@At("RETURN")) private void gadgetInit(CallbackInfo ci){this.masa_gadget_mod$addFastSwitcherWidget();}
}
''',
    'GadgetConfigMixin': '''package io.github.halfmasa.gadgetfixture.mixin;
import io.github.halfmasa.gadgetfixture.GadgetHooks;
import java.util.List;
import fi.dy.masa.malilib.gui.*;
import fi.dy.masa.malilib.gui.widgets.*;
import fi.dy.masa.malilib.interfaces.IStringValue;
import top.hendrixshen.magiclib.impl.malilib.config.gui.SelectorDropDownList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=GuiConfigsBase.class,remap=false)
public abstract class GadgetConfigMixin extends GuiListBase<GuiConfigsBase.ConfigOptionWrapper,WidgetConfigOption,WidgetListConfigOptions> implements GadgetHooks {
 @Unique private SelectorDropDownList<IStringValue> masa_gadget$masaModGuiList;
 protected GadgetConfigMixin(int x,int y){super(x,y);}
 public void masa_gadget_mod$addFastSwitcherWidget(){
  this.masa_gadget$masaModGuiList=new SelectorDropDownList<>(this.width-111,10,100,16,200,10,List.of((IStringValue)()->"Gadget fixture"));
  this.addWidget(this.masa_gadget$masaModGuiList);
 }
 @Redirect(method={"initGui","buildConfigSwitcher"},at=@At(value="INVOKE",target="Lfi/dy/masa/malilib/gui/GuiConfigsBase;addWidget(Lfi/dy/masa/malilib/gui/widgets/WidgetBase;)Lfi/dy/masa/malilib/gui/widgets/WidgetBase;"),require=0)
 private WidgetBase suppressNative(GuiConfigsBase screen,WidgetBase widget){return widget;}
}
''',
}


def build(directory: Path, java_home: Path, classpath: str, modmenu: Path, magic_extra: Path) -> Path:
    root = directory / 'gadget-fixture'
    classes = root / 'classes'
    classes.mkdir(parents=True)
    files = []
    for name, text in SOURCES.items():
        file = root / (name + '.java')
        file.write_text(text, encoding='utf-8')
        files.append(str(file))
    result = subprocess.run([str(java_home / 'bin/javac.exe'), '-proc:none', '-encoding', 'UTF-8',
                             '-cp', classpath + ';' + str(modmenu) + ';' + str(magic_extra),
                             '-d', str(classes), *files], capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.decode(errors='replace'))
    artifact = root / 'gadget-compatibility-fixture.jar'
    with zipfile.ZipFile(artifact, 'w', zipfile.ZIP_DEFLATED) as jar:
        for file in classes.rglob('*.class'):
            jar.write(file, file.relative_to(classes).as_posix())
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'masa_gadget_mod',
            'version': '0.0.0-test', 'name': 'Gadget compatibility fixture', 'environment': 'client',
            'entrypoints': {'modmenu': ['io.github.halfmasa.gadgetfixture.GadgetMenuApi']},
            'depends': {'magiclib': '*', 'modmenu': '*'}, 'mixins': ['gadget-fixture.mixins.json']}))
        jar.writestr('gadget-fixture.mixins.json', json.dumps({'required': True,
            'package': 'io.github.halfmasa.gadgetfixture.mixin', 'compatibilityLevel': 'JAVA_25',
            'client': ['GadgetBaseMixin', 'GadgetConfigMixin'], 'injectors': {'defaultRequire': 1}}))
    return artifact

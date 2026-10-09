"""Exercise the production ordered rebind preview and responsive header layout."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
HARNESS = """import java.util.*;
import io.github.halfmasa.xaerobinding.gui.KeymapRebindDraft;
import io.github.halfmasa.xaerobinding.gui.KeymapHeaderLayout;
import io.github.halfmasa.xaerobinding.gui.KeymapBackground;
import io.github.halfmasa.xaerobinding.config.KeymapBindingMode;
import io.github.halfmasa.xaerobinding.config.KeymapLayout;
public class RebindFlowCheck {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){switch(args[0]){
        case "virtual" -> {
            var draft=new KeymapRebindDraft();
            draft.toggle(List.of(225));draft.toggle(List.of(4));draft.toggle(List.of(-2));
            check(draft.keys().equals(List.of(225,4,-2)),"virtual clicks lost their order");
            draft.toggle(List.of(4));
            check(draft.keys().equals(List.of(225,-2)),"second click did not deselect");
            draft.toggle(List.of(4));
            check(draft.keys().equals(List.of(225,-2,4)),"reselected key was not appended in click order");
        }
        case "physical" -> {
            var draft=new KeymapRebindDraft();
            draft.press(224);draft.press(6);draft.press(6);draft.press(-1);
            check(draft.keys().equals(List.of(224,6,-1)),"physical input repeats or loses order/mouse keys");
            draft.clear();
            check(draft.touched() && draft.keys().isEmpty(),"Escape clear did not leave a confirmable empty preview");
            draft.press(42);
            check(draft.keys().equals(List.of(42)),"Backspace cannot be bound after clearing");
            draft.press(40);
            check(draft.keys().equals(List.of(42,40)),"Enter is still reserved as a confirmation shortcut");
        }
        case "isolated" -> {
            var draft=new KeymapRebindDraft();
            check(!draft.touched(),"opening capture changes a binding");
            draft.press(4);var snapshot=draft.keys();draft.press(7);
            check(snapshot.equals(List.of(4)),"preview aliases the confirmed snapshot");
            draft.reset();
            check(!draft.touched() && draft.keys().isEmpty(),"new target inherits old input");
            draft.clear();check(draft.touched(),"clearing an empty preview cannot be confirmed");
        }
        case "terminal" -> {
            var draft=new KeymapRebindDraft();draft.toggle(List.of(224,225,30));
            check(draft.keys().equals(List.of(224,225,30)),"terminal key chord order was lost");
            draft.toggle(List.of(224,225,30));
            check(draft.touched() && draft.keys().isEmpty(),"second terminal click did not clear the complete chord");
        }
        case "layout" -> {
            int[] widths={230,82,155,100,86,82,180};
            for(int screen:new int[]{320,426,640,853,1280,1920}){
                var layout=KeymapHeaderLayout.arrange(screen,widths);
                check(layout.cells().size()==widths.length,"responsive layout drops controls");
                int lastY=0,lastRight=0;
                for(var cell:layout.cells()){
                    check(cell.x()>=10 && cell.x()+cell.width()<=screen-10,"control overflows the screen");
                    check(cell.y()>=24 && cell.y()>=lastY,"control order changes on wrap");
                    if(cell.y()==lastY)check(cell.x()>=lastRight+4,"adjacent controls overlap");
                    lastY=cell.y();lastRight=cell.x()+cell.width();
                }
                check(layout.bottom()>=lastY+24,"keyboard overlaps the wrapped header");
                if(screen<853)check(lastY>24,"narrow header does not wrap");
                if(screen>=1280)check(lastY==24,"wide header wraps unnecessarily");
            }
        }
        case "compact_header" -> {
            // Search, fold, category, conflicts, layout, keyboard, mode, save, discard.
            int[] widths={230,44,155,62,68,62,128,62,80};
            for(int screen:new int[]{240,320,426,640,853,1280}){
                var layout=KeymapHeaderLayout.arrange(screen,widths);
                check(layout.cells().size()==9,"save or discard dropped from the header");
                int lastY=-1,lastRight=0;
                for(int i=0;i<widths.length;i++){
                    var cell=layout.cells().get(i);
                    check(cell.width()==Math.min(screen-20,widths[i]),"text-sized control was shrunk before wrapping");
                    check(cell.x()>=10 && cell.x()+cell.width()<=screen-10,"compact header overflows");
                    if(cell.y()==lastY)check(cell.x()>=lastRight+4,"compact controls overlap");
                    lastY=cell.y();lastRight=cell.x()+cell.width();
                }
                check(layout.bottom()==lastY+24,"keyboard does not follow the last header row");
                if(screen>=Arrays.stream(widths).sum()+4*(widths.length-1)+20)check(lastY==24,"compact buttons wrap despite sufficient space");
            }
        }
        case "background" -> {
            check(KeymapBackground.color(31)==0xB0000000,"default background appearance changed");
            check(KeymapBackground.color(0)==0xFF000000,"zero transparency is not opaque");
            check(KeymapBackground.color(100)==0,"full transparency still dims the world");
            check(KeymapBackground.color(-10)==0xFF000000 && KeymapBackground.color(110)==0,"invalid transparency escapes bounds");
            int previous=255;
            for(int transparency=0;transparency<=100;transparency++){
                int color=KeymapBackground.color(transparency);
                int alpha=color>>>24;
                check(alpha<=previous && (color&0xFFFFFF)==0,"transparency changes color or brightens inconsistently");
                previous=alpha;
            }
        }
        case "mode" -> {
            check(KeymapBindingMode.PHYSICAL.fromString("bad")==KeymapBindingMode.VIRTUAL,"invalid mode does not default to virtual");
            check(KeymapBindingMode.VIRTUAL.fromString("physical")==KeymapBindingMode.PHYSICAL,"physical preference does not reload");
            check(KeymapBindingMode.VIRTUAL.cycle(true)==KeymapBindingMode.PHYSICAL && KeymapBindingMode.PHYSICAL.cycle(true)==KeymapBindingMode.VIRTUAL,"mode switch cannot return to virtual");
        }
        case "layout_option" -> {
            check(!KeymapLayout.STANDARD.isExtended(),"default layout is not 104 keys");
            check(KeymapLayout.EXTENDED.fromString("bad")==KeymapLayout.STANDARD,"invalid layout does not fall back to 104");
            for(var layout:KeymapLayout.values()){
                check(layout.fromString(layout.getStringValue())==layout,"saved layout does not reload");
                check(KeymapLayout.of(layout.isExtended())==layout,"screen layout differs from the config option");
                check(layout.cycle(true).cycle(true)==layout,"layout switch cannot return to its original value");
            }
        }
        default -> throw new AssertionError("unknown scenario");
    }}
}
"""

class KeymapRebindFlowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        java_home=Path(os.environ.get("JAVA_HOME","C:/Program Files/Java/jdk-25.0.3"))
        cls.java=str(java_home/"bin/java.exe") if os.name=="nt" else shutil.which("java")
        javac=str(java_home/"bin/javac.exe") if os.name=="nt" else shutil.which("javac")
        cls.temp=tempfile.TemporaryDirectory(prefix="halfmasa-rebind-flow-")
        cls.addClassCleanup(cls.temp.cleanup)
        cls.work=Path(cls.temp.name)
        harness=cls.work/"RebindFlowCheck.java"
        harness.write_text(HARNESS,encoding="utf-8")
        sources=[ROOT/"src/main/java/io/github/halfmasa/xaerobinding"/name for name in
                 ["gui/KeymapRebindDraft.java","gui/KeymapHeaderLayout.java","gui/KeymapBackground.java","config/KeymapBindingMode.java","config/KeymapLayout.java"]]
        stubs={
            "fi/dy/masa/malilib/config/IConfigOptionListEntry.java":"package fi.dy.masa.malilib.config; public interface IConfigOptionListEntry {String getStringValue();String getDisplayName();IConfigOptionListEntry cycle(boolean forward);IConfigOptionListEntry fromString(String value);}",
            "fi/dy/masa/malilib/util/StringUtils.java":"package fi.dy.masa.malilib.util; public class StringUtils {public static String translate(String key){return key;}}",
        }
        for filename,contents in stubs.items():
            stub=cls.work/filename;stub.parent.mkdir(parents=True,exist_ok=True);stub.write_text(contents,encoding="utf-8");sources.append(stub)
        result=subprocess.run([javac,"-encoding","UTF-8","-d",str(cls.work),*map(str,sources),str(harness)],capture_output=True,text=True)
        if result.returncode:raise AssertionError(result.stdout+result.stderr)

    def scenario(self,name):
        result=subprocess.run([self.java,"-classpath",str(self.work),"RebindFlowCheck",name],capture_output=True,text=True)
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)

    def test_virtual_clicks_toggle_and_preserve_selection_order(self):self.scenario("virtual")
    def test_physical_input_preserves_order_and_clear_requires_confirmation(self):self.scenario("physical")
    def test_preview_is_isolated_from_confirmed_snapshots(self):self.scenario("isolated")
    def test_terminal_chord_clicks_toggle_the_complete_chord(self):self.scenario("terminal")
    def test_header_controls_wrap_and_never_overlap(self):self.scenario("layout")
    def test_compact_header_preserves_text_widths_and_exit_controls(self):self.scenario("compact_header")
    def test_background_default_limits_and_transparency_direction(self):self.scenario("background")
    def test_binding_mode_defaults_and_cycles(self):self.scenario("mode")
    def test_layout_option_round_trips_and_matches_both_virtual_keyboards(self):self.scenario("layout_option")

if __name__=="__main__":unittest.main()

"""Exercise ignored-key drafts and virtual-keyboard status rules in production Java."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
HARNESS = """import java.util.*;
import io.github.halfmasa.xaerobinding.feature.IgnoredKeySelection;
import io.github.halfmasa.xaerobinding.gui.KeymapKeyboardStyle;
import io.github.halfmasa.xaerobinding.gui.KeymapKeyboardStyle.Binding;
import io.github.halfmasa.xaerobinding.gui.KeymapKeyboardLayout;
import io.github.halfmasa.xaerobinding.gui.KeymapKeyboardRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.gui.Font;
public class IgnoredKeysCheck {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        switch (args[0]) {
            case "parse" -> {
                var draft = new IgnoredKeySelection("26,4;4 invalid 225 0 -1", "26,4,22,7,225");
                check(draft.keys().equals(Set.of(26,4,225,-1)), "valid keys were lost or invalid tokens added");
                check(draft.serialize().equals("-1,4,26,225"), "serialization is not canonical");
                check(!draft.hasChanges(), "normalizing order and duplicates caused a warning");
                check(new IgnoredKeySelection("", "4").keys().isEmpty(), "empty ignored list did not load");
            }
            case "draft" -> {
                String live = "4,26";
                var draft = new IgnoredKeySelection(live,"4,7,22,26,225");
                draft.toggle(6);
                check(draft.hasChanges(), "adding a key was not detected");
                check(live.equals("4,26"), "draft mutated the live value");
                draft.toggle(6);
                check(!draft.hasChanges(), "toggling twice still requests confirmation");
                draft.toggle(4);
                check(!draft.keys().contains(4), "second click did not remove a key");
                draft.toggle(4);
                draft.toggle(0);
                check(!draft.hasChanges(), "unchanged or placeholder input requests confirmation");
                var snapshot=draft.keys();
                draft.toggle(-1);
                check(!snapshot.contains(-1), "snapshot aliases the editable draft");
            }
            case "reset" -> {
                var draft = new IgnoredKeySelection("6,-1", IgnoredKeySelection.DEFAULT_KEYS);
                draft.reset();
                check(draft.keys().equals(Set.of(-3,-2,-1,4,7,22,26,224,225,226,228,229,230)), "reset did not restore movement, modifiers and mouse defaults");
                check(draft.hasChanges(), "reset to a different value does not warn");
                var unchanged = new IgnoredKeySelection(IgnoredKeySelection.DEFAULT_KEYS, IgnoredKeySelection.DEFAULT_KEYS);
                unchanged.reset();
                check(!unchanged.hasChanges(), "resetting an already-default list warns");
            }
            case "mouse_filter" -> {
                Set<Integer> keys=IgnoredKeySelection.parse("6,-1,-3");
                check(IgnoredKeySelection.isIgnored(-1,keys,false), "selected left mouse button is not ignored");
                check(IgnoredKeySelection.isIgnored(-3,keys,false), "selected middle mouse button is not ignored");
                check(!IgnoredKeySelection.isIgnored(-2,keys,false), "unselected right button is ignored");
                check(IgnoredKeySelection.isIgnored(6,keys,false), "selected keyboard key is not ignored");
                check(!IgnoredKeySelection.isIgnored(6,keys,true), "inverted list does not allow selected keys");
                check(IgnoredKeySelection.isIgnored(-2,keys,true), "inverted list does not exclude unlisted mouse buttons");
            }
            case "sources" -> {
                var bindings=List.of(new Binding(List.of(4),true),new Binding(List.of(4),false),new Binding(List.of(-1),false));
                var both=KeymapKeyboardStyle.indicators(bindings,List.of(4),false);
                check(both.fill()==KeymapKeyboardStyle.BOUND, "ordinary binding is not green");
                check(both.vanilla() && both.malilib(), "dual source dots were lost");
                var mouse=KeymapKeyboardStyle.indicators(bindings,List.of(-1),false);
                check(mouse.malilib() && !mouse.vanilla(), "MaLiLib mouse source is not shown");
                check(KeymapKeyboardStyle.indicators(bindings,List.of(6),false).fill()==KeymapKeyboardStyle.UNBOUND, "unbound key is green");
                int gray=KeymapKeyboardStyle.UNBOUND;
                check((gray>>>24)>0 && (gray>>>24)<255, "unbound background is not translucent");
                check(((gray>>>16)&255)==((gray>>>8)&255) && ((gray>>>8)&255)==(gray&255), "unbound background is not neutral gray");
                check(KeymapKeyboardStyle.VANILLA_DOT!=KeymapKeyboardStyle.MALILIB_DOT, "source markers use the same color");
            }
            case "combination" -> {
                var bindings=List.of(new Binding(List.of(4),true),new Binding(List.of(225,4),false));
                var key=KeymapKeyboardStyle.indicators(bindings,List.of(4),false);
                check(key.combination() && key.fill()==KeymapKeyboardStyle.BOUND, "a combination binding must keep its green background");
                check(key.vanilla() && key.malilib(), "combination coloring hides source dots");
                check(KeymapKeyboardStyle.COMBINATION_DOT!=KeymapKeyboardStyle.VANILLA_DOT && KeymapKeyboardStyle.COMBINATION_DOT!=KeymapKeyboardStyle.MALILIB_DOT, "purple marker is indistinguishable from source markers");
                var chord=KeymapKeyboardStyle.indicators(bindings,List.of(225,4),false);
                check(chord.malilib() && !chord.vanilla(), "terminal combination matches an unrelated single binding");
                check(!KeymapKeyboardStyle.indicators(bindings,List.of(225,6),false).combination(), "unmatched chord gets purple status");
            }
            case "ignored_style" -> {
                var bindings=List.of(new Binding(List.of(225,4),true),new Binding(List.of(4),false));
                var key=KeymapKeyboardStyle.indicators(bindings,List.of(4),true);
                check(key.fill()==KeymapKeyboardStyle.BOUND, "ignoring a bound key changed its green background");
                check(key.vanilla() && key.malilib() && key.combination(), "ignored marker erases binding metadata");
                check(KeymapKeyboardStyle.indicators(bindings,List.of(6),true).fill()==KeymapKeyboardStyle.UNBOUND, "ignoring an unbound key changed its translucent gray background");
                check(KeymapKeyboardStyle.IGNORED_DOT!=KeymapKeyboardStyle.UNBOUND && KeymapKeyboardStyle.IGNORED_DOT!=KeymapKeyboardStyle.BOUND, "ignored dot is ambiguous");
            }
            case "marker_rendering" -> {
                var key=new KeymapKeyboardLayout.Key(4,"A",10,20,24,16,false);
                for (boolean ignored : List.of(false,true)) {
                    var graphics=new GuiContext();
                    KeymapKeyboardRenderer.drawKey(graphics,new Font(),key,
                        new KeymapKeyboardStyle.Indicators(true,true,true,ignored),false,false);
                    int[] colors={KeymapKeyboardStyle.VANILLA_DOT,KeymapKeyboardStyle.MALILIB_DOT,KeymapKeyboardStyle.COMBINATION_DOT};
                    for (int i=0;i<colors.length;i++) {
                        int color=colors[i];
                        var marks=graphics.fills.stream().filter(r->r[4]==color).toList();
                        check(marks.size()==1,"a status marker is missing or still drawn as a cross");
                        int[] mark=marks.get(0);
                        check(mark[0]==30 && mark[1]==22+i*4 && mark[2]-mark[0]==3 && mark[3]-mark[1]==3,
                            "marker does not use the former right-edge rectangle style");
                    }
                    check(graphics.fills.get(0)[4]==KeymapKeyboardStyle.BOUND,
                        "combination marker replaced the key background");
                }
            }
            case "conflict_status" -> {
                var alone=KeymapKeyboardStyle.withConflicts(List.of(new Binding(List.of(225,4),false)));
                var clean=KeymapKeyboardStyle.indicators(alone,List.of(4),false);
                check(clean.combination() && !clean.singleConflict() && !clean.combinationConflict(),
                    "an unshared combination falsely gets a red conflict marker");
                var shared=KeymapKeyboardStyle.withConflicts(List.of(new Binding(List.of(4),true),
                    new Binding(List.of(4),false),new Binding(List.of(225,4),false)));
                var both=KeymapKeyboardStyle.indicators(shared,List.of(4),true);
                check(both.singleConflict() && both.combinationConflict(), "one of the two conflict types disappeared");
                check(both.vanilla() && both.malilib() && both.combination(), "red indicators erase source/combination dots");
                var chord=KeymapKeyboardStyle.indicators(shared,List.of(225,4),false);
                check(!chord.singleConflict() && chord.combinationConflict(), "a chord has an unrelated single-key indicator");
            }
            case "conflict_rendering" -> {
                var key=new KeymapKeyboardLayout.Key(4,"A",10,20,24,16,false);
                for (boolean selected : List.of(false,true)) {
                    for (boolean ignored : List.of(false,true)) {
                        var graphics=new GuiContext();
                        KeymapKeyboardRenderer.drawKey(graphics,new Font(),key,
                            new KeymapKeyboardStyle.Indicators(true,true,true,ignored,true,true),selected,false);
                        for (int color : new int[]{KeymapKeyboardStyle.VANILLA_DOT,KeymapKeyboardStyle.MALILIB_DOT,
                                KeymapKeyboardStyle.COMBINATION_DOT,KeymapKeyboardStyle.SINGLE_CONFLICT,
                                KeymapKeyboardStyle.COMBINATION_CONFLICT}) {
                            check(graphics.fills.stream().filter(r->r[4]==color).count()==1,
                                "selection or ignoring lost one of the five simultaneous indicators");
                        }
                        var light=graphics.fills.stream().filter(r->r[4]==KeymapKeyboardStyle.SINGLE_CONFLICT).findFirst().orElseThrow();
                        var dark=graphics.fills.stream().filter(r->r[4]==KeymapKeyboardStyle.COMBINATION_CONFLICT).findFirst().orElseThrow();
                        check(light[0]==26 && light[1]==22 && light[2]==29 && light[3]==25, "single conflict lost its fixed slot");
                        check(dark[0]==26 && dark[1]==26 && dark[2]==29 && dark[3]==29, "combination conflict lost its fixed slot");
                        check(light[4]!=dark[4], "both conflict indicators use the same color");
                    }
                }
            }
            case "fixed_slots" -> {
                var key=new KeymapKeyboardLayout.Key(4,"A",10,20,24,16,false);
                int[] colors={KeymapKeyboardStyle.VANILLA_DOT,KeymapKeyboardStyle.MALILIB_DOT,
                    KeymapKeyboardStyle.COMBINATION_DOT,KeymapKeyboardStyle.IGNORED_DOT,
                    KeymapKeyboardStyle.SINGLE_CONFLICT,KeymapKeyboardStyle.COMBINATION_CONFLICT};
                int[][] slots={{30,22},{30,26},{30,30},{26,30},{26,22},{26,26}};
                for (int mask=0;mask<64;mask++) {
                    var graphics=new GuiContext();
                    KeymapKeyboardRenderer.drawKey(graphics,new Font(),key,
                        new KeymapKeyboardStyle.Indicators((mask&1)!=0,(mask&2)!=0,(mask&4)!=0,
                            (mask&8)!=0,(mask&16)!=0,(mask&32)!=0),false,false);
                    check(graphics.fills.size()==5+Integer.bitCount(mask),"an extra ignored background, hatch or x was drawn");
                    for (int i=0;i<colors.length;i++) {
                        int color=colors[i];
                        var marks=graphics.fills.stream().filter(r->r[4]==color).toList();
                        check(marks.size()==((mask&(1<<i))!=0?1:0),"a missing status shifted another marker");
                        if (!marks.isEmpty()) {
                            var mark=marks.get(0);
                            check(mark[0]==slots[i][0] && mark[1]==slots[i][1],"marker position depends on other statuses");
                            check(mark[2]-mark[0]==3 && mark[3]-mark[1]==3,"marker sizes differ");
                        }
                    }
                }
            }
            case "default_migration" -> {
                var defaults=IgnoredKeySelection.parse(IgnoredKeySelection.DEFAULT_KEYS);
                for (int code : new int[]{-1,-2,-3,224,225,226,228,229,230,4,7,22,26})
                    check(defaults.contains(code),"a requested default ignored key is missing");
                check(IgnoredKeySelection.migrateDefaults("225,26,4,7,22").equals(IgnoredKeySelection.DEFAULT_KEYS),"previous SDL defaults were not expanded");
                check(IgnoredKeySelection.migrateDefaults("87,65,83,68,340").equals(IgnoredKeySelection.DEFAULT_KEYS),"legacy defaults were not expanded");
                for (String custom : List.of("", "4,6,-1", "26,4,22,7", IgnoredKeySelection.DEFAULT_KEYS))
                    check(IgnoredKeySelection.migrateDefaults(custom).equals(custom),"a custom list was overwritten");
            }
            default -> throw new AssertionError("unknown scenario");
        }
    }
}
"""


class IgnoredKeysKeyboardTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        java_home = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-25.0.3"))
        cls.java = str(java_home / "bin/java.exe") if os.name == "nt" else shutil.which("java")
        javac = str(java_home / "bin/javac.exe") if os.name == "nt" else shutil.which("javac")
        if not javac:
            raise unittest.SkipTest("A JDK is required")
        cls.temp = tempfile.TemporaryDirectory(prefix="halfmasa-ignored-keyboard-")
        cls.addClassCleanup(cls.temp.cleanup)
        cls.work = Path(cls.temp.name)
        harness = cls.work / "IgnoredKeysCheck.java"
        harness.write_text(HARNESS, encoding="utf-8")
        source_root = ROOT / "src/main/java/io/github/halfmasa/xaerobinding"
        stubs = {
            "io/github/halfmasa/xaerobinding/compat/InputCompat.java": "package io.github.halfmasa.xaerobinding.compat; public class InputCompat { public static int layoutKeyCode(int code) { return code == 32 ? 44 : code; } }",
            "fi/dy/masa/malilib/render/GuiContext.java": """package fi.dy.masa.malilib.render;
                public class GuiContext {
                    public final java.util.List<int[]> fills=new java.util.ArrayList<>();
                    public void fill(int x,int y,int r,int b,int c){fills.add(new int[]{x,y,r,b,c});}
                    public Pose pose(){return new Pose();}
                    public void drawString(net.minecraft.client.gui.Font f,String s,int x,int y,int c,boolean shadow){}
                    public static class Pose {
                        public void pushMatrix(){} public void popMatrix(){}
                        public void translate(float x,float y){} public void scale(float x,float y){}
                    }
                }""",
            "fi/dy/masa/malilib/util/StringUtils.java": """package fi.dy.masa.malilib.util;
                public class StringUtils {public static String translate(String key){return key;}}""",
            "net/minecraft/client/gui/Font.java": """package net.minecraft.client.gui;
                public class Font {public int width(String text){return text.length()*6;}}""",
            "io/github/halfmasa/xaerobinding/gui/KeymapKeyboardLayout.java": """package io.github.halfmasa.xaerobinding.gui;
                public class KeymapKeyboardLayout {
                    public record Key(int code,String label,int x,int y,int width,int height,boolean mouse){}
                }""",
        }
        sources = [source_root / "feature/IgnoredKeySelection.java", source_root / "gui/KeymapKeyboardStyle.java",
                   source_root / "gui/KeymapKeyboardRenderer.java"]
        for filename, contents in stubs.items():
            stub = cls.work / filename
            stub.parent.mkdir(parents=True, exist_ok=True)
            stub.write_text(contents, encoding="utf-8")
            sources.append(stub)
        result = subprocess.run([javac, "-encoding", "UTF-8", "-d", str(cls.work), *map(str, sources), str(harness)],
                                capture_output=True, text=True)
        if result.returncode:
            raise AssertionError(result.stdout + result.stderr)

    def run_scenario(self, name):
        result = subprocess.run([self.java, "-classpath", str(self.work), "IgnoredKeysCheck", name],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_legacy_numeric_values_parse_and_serialize_without_false_changes(self):
        self.run_scenario("parse")

    def test_draft_isolated_and_toggle_back_requires_no_warning(self):
        self.run_scenario("draft")

    def test_reset_restores_movement_keys_and_warns_only_for_changes(self):
        self.run_scenario("reset")

    def test_keyboard_mouse_and_inverted_filter_follow_the_same_rule(self):
        self.run_scenario("mouse_filter")

    def test_bound_keys_use_green_and_independent_source_dots(self):
        self.run_scenario("sources")

    def test_combination_keys_stay_green_with_a_distinct_purple_marker(self):
        self.run_scenario("combination")

    def test_ignored_dot_keeps_background_and_binding_information(self):
        self.run_scenario("ignored_style")

    def test_renderer_keeps_all_three_rectangular_markers_and_green_background(self):
        self.run_scenario("marker_rendering")

    def test_conflict_classification_is_independent_of_source_and_combination_markers(self):
        self.run_scenario("conflict_status")

    def test_both_red_conflict_markers_survive_selection_and_ignoring(self):
        self.run_scenario("conflict_rendering")

    def test_all_six_markers_have_equal_size_and_fixed_slots_for_every_status_subset(self):
        self.run_scenario("fixed_slots")

    def test_new_ignored_defaults_and_migration_preserve_custom_lists(self):
        self.run_scenario("default_migration")


if __name__ == "__main__":
    unittest.main()

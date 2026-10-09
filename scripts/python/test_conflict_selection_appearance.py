"""Test production colors and list hit boxes without starting Minecraft."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
HARNESS = """import io.github.halfmasa.xaerobinding.gui.ConflictSelectionAppearance.*;
public class AppearanceCheck {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        switch (args[0]) {
            case "colors" -> {
                var palette = new Palette(0x204060, 0xFFFFFF, 0xEED202, 144, true, 25);
                check(palette.color(null,0,false,false)==0x90204060, "base color or opacity lost");
                check(palette.color(null,1,false,false)==0x90395979, "alternating brightness lost");
                check(palette.color(0xE8F8FF,1,false,false)==0x90FFFFFF, "custom colors do not saturate correctly");
                check(palette.color(0x010101,0,true,false)==0x90FFFFFF, "selected color did not override custom color");
                check(palette.color(null,1,true,true)==0x90EED202, "held selection highlight lost");
                var flat = new Palette(0x204060,0,0,144,false,25);
                check(flat.color(null,1,false,false)==flat.color(null,0,false,false), "disabled alternation still changes rows");
                check(Palette.textColor(0xFFFFFF)==0xFF181818, "white selected rows have unreadable white text");
                check(Palette.textColor(0x202020)==0xFFF0F0F0, "dark rows need light text");
            }
            case "opacity" -> {
                for (int alpha : new int[]{0,1,144,255}) {
                    var palette = new Palette(0x404040,0xFFFFFF,0xEED202,alpha,true,25);
                    for (int index=0;index<4;index++) {
                        check((palette.color(null,index,index==2,index==3)>>>24)==alpha, "row opacity differs from slider");
                    }
                    check((palette.withAlpha(palette.base())>>>24)==alpha, "frame overrides opacity");
                    check((palette.withAlpha(palette.highlight())>>>24)==alpha, "scrollbar overrides opacity");
                }
            }
            case "size" -> {
                var small = ListLayout.create(1200,700,50,0.2,1.15,1);
                var normal = ListLayout.create(1200,700,50,0.6,1.15,1);
                var large = ListLayout.create(1200,700,50,1.0,1.15,1);
                check(small.width()<normal.width() && normal.width()<large.width(), "size does not scale the panel");
                check(small.rowHeight()<normal.rowHeight() && normal.rowHeight()<large.rowHeight(), "size does not scale rows");
                check(small.textScale()<normal.textScale() && normal.textScale()<large.textScale(), "size does not scale labels");
                check(small.visibleRows()>normal.visibleRows() && normal.visibleRows()>large.visibleRows(), "large rows do not reduce visible rows");
                for (double size : new double[]{0.2,0.6,1}) {
                    var narrow = ListLayout.create(320,240,100,size,2,1);
                    check(narrow.left()-narrow.expansion()>=0, "expanded row clips the left edge");
                    check(narrow.left()+narrow.width()+narrow.expansion()<=320, "expanded row clips the right edge");
                    check(narrow.top()>=0 && narrow.top()+narrow.height()<=240, "list clips the screen vertically");
                }
            }
            case "hitboxes" -> {
                for (double size : new double[]{0.2,0.6,1}) {
                    for (double animation : new double[]{0.1,0.5,1}) {
                        var layout = ListLayout.create(1200,700,50,size,1.5,animation);
                        int x = layout.left()+layout.width()/2;
                        int scroll=7;
                        for (int row=0;row<layout.visibleRows();row++) {
                            int y=layout.rowTop(row)+layout.rowHeight()/2;
                            check(layout.indexAt(x,y,scroll,-1)==scroll+row, "painted row does not match clickable row");
                            check(layout.indexAt(x,layout.rowTop(row)+layout.rowHeight(),scroll,-1)==-1, "gap selected a row");
                        }
                        check(layout.indexAt(x,layout.rowTop(0)-1,scroll,-1)==-1, "top padding selected an action");
                        int y=layout.rowTop(0)+layout.rowHeight()/2;
                        check(layout.indexAt(layout.left()-1,y,scroll,-1)==-1, "outside row selected an action");
                        if (layout.expansion()>0) {
                            check(layout.indexAt(layout.left()-layout.expansion(),y,scroll,scroll)==scroll, "expanded selected row lost its hitbox");
                        }
                    }
                }
            }
            case "animation" -> {
                var opening = ListLayout.create(1000,600,50,0.6,1.15,0.1);
                var opened = ListLayout.create(1000,600,50,0.6,1.15,1);
                check(opening.width()<opened.width() && opening.height()<opened.height(), "opening animation does not scale the list");
                check(opening.visibleRows()==opened.visibleRows(), "animation changes scroll range");
            }
            default -> throw new AssertionError("unknown scenario");
        }
    }
}
"""


class ConflictSelectionAppearanceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        java_home = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-25.0.3"))
        cls.java = str(java_home / "bin/java.exe") if os.name == "nt" else shutil.which("java")
        javac = str(java_home / "bin/javac.exe") if os.name == "nt" else shutil.which("javac")
        if not javac:
            raise unittest.SkipTest("A JDK is required")
        cls.temp = tempfile.TemporaryDirectory(prefix="halfmasa-selection-appearance-")
        cls.addClassCleanup(cls.temp.cleanup)
        cls.work = Path(cls.temp.name)
        harness = cls.work / "AppearanceCheck.java"
        harness.write_text(HARNESS, encoding="utf-8")
        production = ROOT / "src/main/java/io/github/halfmasa/xaerobinding/gui/ConflictSelectionAppearance.java"
        result = subprocess.run([javac, "-encoding", "UTF-8", "-d", str(cls.work), str(production), str(harness)],
                                capture_output=True, text=True)
        if result.returncode:
            raise AssertionError(result.stdout + result.stderr)

    def run_scenario(self, name):
        result = subprocess.run([self.java, "-classpath", str(self.work), "AppearanceCheck", name],
                                capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_shared_colors_alternation_customization_and_text_contrast(self):
        self.run_scenario("colors")

    def test_opacity_applies_to_options_frame_and_scrollbar(self):
        self.run_scenario("opacity")

    def test_size_scales_rows_labels_and_scroll_range_without_clipping(self):
        self.run_scenario("size")

    def test_row_hitboxes_follow_size_animation_expansion_and_scrolling(self):
        self.run_scenario("hitboxes")

    def test_opening_animation_preserves_scroll_range(self):
        self.run_scenario("animation")


if __name__ == "__main__":
    unittest.main()

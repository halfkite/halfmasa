"""Exercise the production customization store without starting Minecraft."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
STUBS = {
    "net/minecraft/network/chat/Component.java": """package net.minecraft.network.chat;
public record Component(String value) {
    public static Component translatable(String name) { return new Component(name); }
    public String getString() { return value; }
}
""",
    "net/minecraft/client/KeyMapping.java": """package net.minecraft.client;
import net.minecraft.network.chat.Component;
public record KeyMapping(String name) {
    public String getName() { return name; }
    public Category getCategory() { return new Category(); }
    public static class Category {
        public Component label() { return Component.translatable("category"); }
    }
}
""",
    "net/minecraft/client/gui/screens/Screen.java": """package net.minecraft.client.gui.screens;
public class Screen {}
""",
    "io/github/halfmasa/xaerobinding/config/Configs.java": """package io.github.halfmasa.xaerobinding.config;
import java.nio.file.Path;
public class Configs {
    public static Path getHalfMasaDirectory() { return Path.of(System.getProperty("test.root")); }
}
""",
    "io/github/halfmasa/xaerobinding/XaeroWorldBinding.java": """package io.github.halfmasa.xaerobinding;
public class XaeroWorldBinding {
    public static final Log LOGGER = new Log();
    public static class Log {
        public void error(String message, Throwable error) { throw new AssertionError(message, error); }
    }
}
""",
    "StoreCheck.java": """import java.nio.file.*;
import net.minecraft.client.KeyMapping;
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
public class StoreCheck {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path file = Path.of(System.getProperty("test.root"), "keybind-pie", "bindings.json");
        Files.createDirectories(file.getParent());
        var store = KeybindCustomizationStore.getInstance();
        var mapping = new KeyMapping("key.test");
        if (args[0].equals("legacy")) {
            Files.writeString(file, "{\\\"bindings\\\":{\\\"key.test\\\":{\\\"requireKeyOrder\\\":true,\\\"comboKeys\\\":[224,6]}}}");
            check(store.reload(), "legacy config failed to load");
            check(store.participatesInWheel(mapping), "old bindings must keep their wheel default");
            check(store.requiresKeyOrder(mapping), "existing order setting lost");
            check(store.comboKeys(mapping).equals(java.util.List.of(224, 6)), "existing chord lost");
        } else if (args[0].equals("persist")) {
            store.get(mapping).disableWheel = true;
            store.get("masa.tool").disableWheel = true;
            store.save();
            check(Files.readString(file).contains("disableWheel"), "wheel-only customization was omitted");
            check(store.reload(), "saved config failed to load");
            check(!store.participatesInWheel(mapping), "vanilla wheel toggle lost");
            check(!store.participatesInWheel("masa.tool"), "MaLiLib wheel toggle lost");
            check(store.isActive(mapping, null), "excluding the wheel disabled gameplay input");
            check(store.participatesInWheel("untouched"), "unrelated binding changed");
        } else if (args[0].equals("reset")) {
            store.get(mapping).disableWheel = true;
            store.get(mapping).requireKeyOrder = true;
            store.save();
            store.get(mapping).disableWheel = false;
            store.save();
            check(store.reload(), "config failed to reload after enabling wheel");
            check(store.participatesInWheel(mapping), "wheel stayed disabled");
            check(store.requiresKeyOrder(mapping), "wheel toggle changed key order");
            store.reset(mapping);
            check(store.reload(), "reset config failed to load");
            check(store.participatesInWheel(mapping), "reset did not restore wheel default");
        } else if (args[0].equals("equality")) {
            var original = store.get(mapping);
            original.comboKeys.addAll(java.util.List.of(224,6));
            var draft = original.copy();
            check(draft.sameAs(original), "untouched draft reports changes");
            draft.wheelEnabled = true;
            check(!draft.sameAs(original), "explicit wheel change was not detected");
            draft.wheelEnabled = original.wheelEnabled;
            check(draft.sameAs(original), "reverted wheel choice still warns");
            draft.requireKeyOrder = !original.requireKeyOrder;
            check(!draft.sameAs(original), "order change was not detected");
            draft.requireKeyOrder = original.requireKeyOrder;
            check(draft.sameAs(original), "reverted order still warns");
            draft.comboKeys.add(7);
            check(!draft.sameAs(original), "combination change was not detected");
        } else if (args[0].equals("defaults")) {
            for (int code : new int[]{224,225,226,228,229,230,-1,-2,-3}) {
                check(!store.participatesInWheel(mapping.getName(), java.util.List.of(code)), "standalone modifier/mouse opened wheel: " + code);
                check(store.participatesInWheel(mapping.getName(), java.util.List.of(code,6)), "modifier chord was excluded: " + code);
            }
            check(store.participatesInWheel(mapping.getName(), java.util.List.of(58)), "F1 was confused with hardware Fn");
            check(store.participatesInWheel(mapping.getName(), java.util.List.of(-4)), "side mouse button default changed");
        } else if (args[0].equals("override")) {
            store.get(mapping).wheelEnabled = true;
            store.save();
            check(store.reload(), "explicit wheel preference failed to load");
            check(store.participatesInWheel(mapping.getName(), java.util.List.of(225)), "explicit enable did not override modifier default");
            store.get(mapping).wheelEnabled = false;
            store.save();
            check(store.reload(), "explicit disable failed to load");
            check(!store.participatesInWheel(mapping.getName(), java.util.List.of(225,6)), "explicit disable did not override chord default");
        } else if (args[0].equals("draft")) {
            store.get(mapping).comboKeys.addAll(java.util.List.of(224,6));
            store.save();
            String originalFile = Files.readString(file);
            var draft = store.get(mapping).copy();
            draft.comboKeys.clear();
            draft.comboKeys.add(225);
            draft.wheelEnabled = true;
            draft.requireKeyOrder = true;
            check(store.comboKeys(mapping).equals(java.util.List.of(224,6)), "editing a draft mutated live keys");
            check(!store.requiresKeyOrder(mapping), "editing a draft mutated live order");
            check(Files.readString(file).equals(originalFile), "editing a draft wrote the file");
            check(store.reload(), "discard failed to reload original state");
            check(store.comboKeys(mapping).equals(java.util.List.of(224,6)), "discard lost original keys");
            store.put(mapping.getName(), draft);
            draft.comboKeys.clear();
            check(store.comboKeys(mapping).equals(java.util.List.of(225)), "saved state aliases the draft");
            store.save();
            check(store.reload(), "committed draft failed to load");
            check(store.requiresKeyOrder(mapping), "committed order lost");
            check(store.participatesInWheel(mapping.getName(), java.util.List.of(225)), "committed explicit modifier setting lost");
        }
    }
}
""",
}


class KeybindWheelStoreTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        java_home = Path(os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-25.0.3"))
        cls.java = str(java_home / "bin/java.exe") if os.name == "nt" else shutil.which("java")
        javac = str(java_home / "bin/javac.exe") if os.name == "nt" else shutil.which("javac")
        gson_files = list((Path.home() / ".gradle/caches/modules-2/files-2.1/com.google.code.gson/gson").glob("*/*/*.jar"))
        gson_files = [path for path in gson_files if not path.name.endswith(("-sources.jar", "-javadoc.jar"))]
        if not javac or not gson_files:
            raise unittest.SkipTest("JDK and cached Gson are required")
        cls.temp = tempfile.TemporaryDirectory(prefix="halfmasa-wheel-store-")
        cls.addClassCleanup(cls.temp.cleanup)
        cls.work = Path(cls.temp.name)
        files = []
        for name, content in STUBS.items():
            path = cls.work / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
            files.append(str(path))
        store_source = ROOT / "src/main/java/io/github/halfmasa/xaerobinding/feature/KeybindCustomizationStore.java"
        cls.classpath = os.pathsep.join((str(cls.work / "classes"), str(sorted(gson_files)[-1])))
        result = subprocess.run([javac, "-encoding", "UTF-8", "-classpath", cls.classpath,
                                 "-d", str(cls.work / "classes"), str(store_source), *files],
                                capture_output=True, text=True)
        if result.returncode:
            raise AssertionError(result.stdout + result.stderr)

    def run_scenario(self, scenario):
        with tempfile.TemporaryDirectory(dir=self.work) as config_root:
            result = subprocess.run([self.java, f"-Dtest.root={config_root}", "-classpath", self.classpath,
                                     "StoreCheck", scenario], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def test_legacy_config_keeps_order_and_wheel_default(self):
        self.run_scenario("legacy")

    def test_disabled_wheel_persists_without_disabling_input(self):
        self.run_scenario("persist")

    def test_enabling_and_reset_preserve_independent_settings(self):
        self.run_scenario("reset")

    def test_modifier_and_primary_mouse_defaults_preserve_chords_and_function_keys(self):
        self.run_scenario("defaults")

    def test_explicit_preference_overrides_key_default_after_reload(self):
        self.run_scenario("override")

    def test_draft_edits_are_isolated_and_commit_copies_the_entry(self):
        self.run_scenario("draft")

    def test_unchanged_and_reverted_customizations_need_no_warning(self):
        self.run_scenario("equality")


if __name__ == "__main__":
    unittest.main()

package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.List;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

/** Runtime MaLiLib bindings for the user-editable glow rules. */
public final class GlowRuleHotkeys
{
    private static volatile List<ConfigHotkey> hotkeys = List.of();

    private GlowRuleHotkeys() {}

    public static List<ConfigHotkey> getHotkeys() { return hotkeys; }

    public static void reload()
    {
        List<ConfigHotkey> updated = new ArrayList<>();
        for (EntityGlowRule rule : EntityGlowRule.loadAll())
        {
            ConfigHotkey hotkey = new ConfigHotkey("glowRule." + rule.key(), "");
            //#if MC >= 1.21.11
            hotkey.setHotkeyStringValue(rule.hotkey());
            //#else
            //$$ hotkey.setValueFromString(rule.hotkey());
            //#endif
            String key = rule.key();
            boolean blocks = rule.type() == EntityGlowRule.TargetType.BLOCK;
            hotkey.getKeybind().setCallback((action, binding) -> toggle(key, blocks));
            updated.add(hotkey);
        }
        hotkeys = List.copyOf(updated);
        var manager = InputEventHandler.getKeybindManager();
        manager.addHotkeysForCategory(XaeroWorldBinding.MOD_NAME, "halfmasa.hotkeys.glow_rules", hotkeys);
        manager.updateUsedKeys();
    }

    private static boolean toggle(String key, boolean blocks)
    {
        var config = EntityGlowRule.configFor(blocks);
        List<String> encoded = new ArrayList<>(config.getStrings());
        for (int index = 0; index < encoded.size(); index++)
        {
            EntityGlowRule rule = EntityGlowRule.parse(encoded.get(index));
            if (rule == null || !rule.key().equals(key)) continue;
            encoded.set(index, rule.withEnabled(!rule.enabled()).encode());
            config.setStrings(encoded);
            if (blocks) BlockGlowService.getInstance().reload();
            else EntityGlowService.reload();
            new Configs().save();
            return true;
        }
        return false;
    }
}
//#endif

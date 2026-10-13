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
            hotkey.getKeybind().setCallback((action, binding) -> toggle(key));
            updated.add(hotkey);
        }
        hotkeys = List.copyOf(updated);
        var manager = InputEventHandler.getKeybindManager();
        manager.addHotkeysForCategory(XaeroWorldBinding.MOD_NAME, "halfmasa.hotkeys.glow_rules", hotkeys);
        manager.updateUsedKeys();
    }

    private static boolean toggle(String key)
    {
        List<String> encoded = new ArrayList<>();
        boolean found = false;
        for (EntityGlowRule rule : EntityGlowRule.loadAll())
        {
            if (rule.key().equals(key))
            {
                rule = new EntityGlowRule(rule.type(), rule.id(), rule.color(), !rule.enabled(), rule.hotkey());
                found = true;
            }
            encoded.add(rule.encode());
        }
        if (!found) return false;
        Configs.ENTITY_GLOW_RULES.setStrings(encoded);
        Configs.BLOCK_GLOW_RULES.setStrings(List.of());
        EntityGlowService.reload();
        BlockGlowService.getInstance().reload();
        new Configs().save();
        return true;
    }
}
//#endif

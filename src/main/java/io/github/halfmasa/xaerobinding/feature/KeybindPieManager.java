package io.github.halfmasa.xaerobinding.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.gui.KeybindPieScreen;
import io.github.halfmasa.xaerobinding.mixin.KeyMappingAccessor;
import io.github.halfmasa.xaerobinding.mixin.MinecraftInputAccessor;

public final class KeybindPieManager implements IClientTickHandler
{
    private static final KeybindPieManager INSTANCE = new KeybindPieManager();
    private final Map<InputConstants.Key, HeldSelection> heldSelections = new HashMap<>();
    private final Map<KeyMapping, Integer> oneShotReleases = new HashMap<>();
    private InputConstants.Key activeKey;
    private KeybindPieScreen activeScreen;

    private KeybindPieManager() {}

    public static KeybindPieManager getInstance()
    {
        return INSTANCE;
    }

    public boolean handleSet(InputConstants.Key key, boolean pressed)
    {
        if (!Configs.KEYBIND_PIE_MENU.getBooleanValue())
        {
            return false;
        }

        HeldSelection held = this.heldSelections.get(key);
        if (held != null)
        {
            setDown(held.mapping, pressed);
            if (!pressed)
            {
                this.heldSelections.remove(key);
            }
            return true;
        }

        if (this.activeKey != null && this.activeKey.equals(key))
        {
            return true;
        }
        if (!pressed || isIgnored(key))
        {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.screen != null)
        {
            return false;
        }

        List<KeyMapping> conflicts = mappingsFor(key);
        if (conflicts.size() < 2)
        {
            return false;
        }

        for (KeyMapping mapping : conflicts)
        {
            setDown(mapping, false);
        }
        this.activeKey = key;
        this.activeScreen = new KeybindPieScreen(key, conflicts);
        client.setScreen(this.activeScreen);
        return true;
    }

    public boolean handleClick(InputConstants.Key key)
    {
        if (!Configs.KEYBIND_PIE_MENU.getBooleanValue() || isIgnored(key))
        {
            return false;
        }
        return (this.activeKey != null && this.activeKey.equals(key)) || mappingsFor(key).size() > 1;
    }

    public void completeSelection(KeyMapping mapping, boolean clickHold)
    {
        InputConstants.Key key = this.activeKey;
        this.activeKey = null;
        this.activeScreen = null;
        Minecraft client = Minecraft.getInstance();
        client.setScreen(null);

        if (mapping == null || key == null)
        {
            return;
        }

        setDown(mapping, true);
        setClicks(mapping, 1);
        if (clickHold)
        {
            this.heldSelections.put(key, new HeldSelection(mapping));
        }
        else
        {
            this.oneShotReleases.put(mapping, 1);
        }

        if (Configs.KEYBIND_ATTACK_WORKAROUND.getBooleanValue() &&
            mapping == client.options.keyAttack)
        {
            ((MinecraftInputAccessor) client).halfmasa$setMissTime(0);
        }
    }

    public void cancel(KeybindPieScreen screen)
    {
        if (this.activeScreen == screen)
        {
            this.activeKey = null;
            this.activeScreen = null;
        }
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        if (!Configs.KEYBIND_PIE_MENU.getBooleanValue())
        {
            this.heldSelections.values().forEach(held -> setDown(held.mapping, false));
            this.heldSelections.clear();
            this.oneShotReleases.keySet().forEach(mapping -> setDown(mapping, false));
            this.oneShotReleases.clear();
            return;
        }

        List<KeyMapping> release = new ArrayList<>();
        this.oneShotReleases.replaceAll((mapping, ticks) -> {
            if (ticks <= 0)
            {
                release.add(mapping);
            }
            return ticks - 1;
        });
        for (KeyMapping mapping : release)
        {
            setDown(mapping, false);
            this.oneShotReleases.remove(mapping);
        }

        int repeatDelay = Math.max(1, Configs.KEYBIND_REPEAT_COOLDOWN.getIntegerValue());
        for (HeldSelection held : this.heldSelections.values())
        {
            if (++held.ticks >= repeatDelay)
            {
                held.ticks = 0;
                setClicks(held.mapping, 1);
            }
        }
    }

    private static List<KeyMapping> mappingsFor(InputConstants.Key key)
    {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null)
        {
            return List.of();
        }

        return java.util.Arrays.stream(client.options.keyMappings)
                .filter(mapping -> key.equals(((KeyMappingAccessor) mapping).halfmasa$getBoundKey()))
                .sorted(Comparator.comparing(KeyMapping::getName))
                .toList();
    }

    private static boolean isIgnored(InputConstants.Key key)
    {
        Set<Integer> ignored = new HashSet<>();
        for (String value : Configs.KEYBIND_IGNORED_KEYS.getStringValue().split("[,;\\s]+"))
        {
            try
            {
                ignored.add(Integer.parseInt(value));
            }
            catch (NumberFormatException ignoredException)
            {
            }
        }
        boolean listed = key.getType() == InputConstants.Type.KEYSYM && ignored.contains(key.getValue());
        return Configs.KEYBIND_INVERT_IGNORED_KEYS.getBooleanValue() ? !listed : listed;
    }

    private static void setDown(KeyMapping mapping, boolean down)
    {
        ((KeyMappingAccessor) mapping).halfmasa$setDownDirect(down);
    }

    private static void setClicks(KeyMapping mapping, int clicks)
    {
        ((KeyMappingAccessor) mapping).halfmasa$setClickCount(clicks);
    }

    private static final class HeldSelection
    {
        private final KeyMapping mapping;
        private int ticks;

        private HeldSelection(KeyMapping mapping)
        {
            this.mapping = mapping;
        }
    }
}

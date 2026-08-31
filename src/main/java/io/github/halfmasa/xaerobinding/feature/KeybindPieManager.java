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
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.gui.KeybindPieScreen;
import io.github.halfmasa.xaerobinding.mixin.KeyMappingAccessor;
import io.github.halfmasa.xaerobinding.mixin.MinecraftInputAccessor;

public final class KeybindPieManager implements IClientTickHandler
{
    private static final KeybindPieManager INSTANCE = new KeybindPieManager();
    private final Map<InputConstants.Key, HeldSelection> heldSelections = new HashMap<>();
    private final Map<KeyMapping, Integer> oneShotReleases = new HashMap<>();
    private final Map<InputConstants.Key, SelectionCooldown> selectionCooldowns = new HashMap<>();
    private InputConstants.Key activeKey;
    private KeybindPieScreen activeScreen;
    private Screen parentScreen;

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

        SelectionCooldown cooldown = this.selectionCooldowns.get(key);
        if (cooldown != null)
        {
            if (!pressed)
            {
                HeldSelection selected = this.heldSelections.remove(key);
                if (selected != null)
                {
                    setDown(selected.mapping, false);
                }
                cooldown.released = true;
            }
            return true;
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
        Screen screen = MinecraftClientCompat.getScreen(client);
        if (screen instanceof KeybindPieScreen || !wouldTriggerNow(client, screen))
        {
            return false;
        }

        List<KeyMapping> conflicts = mappingsFor(key, screen);
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
        this.parentScreen = screen;
        this.activeScreen.setParent(screen);
        MinecraftClientCompat.setScreen(client, this.activeScreen);
        return true;
    }

    public boolean handleClick(InputConstants.Key key)
    {
        if (!Configs.KEYBIND_PIE_MENU.getBooleanValue() || isIgnored(key))
        {
            return false;
        }
        if (this.selectionCooldowns.containsKey(key))
        {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        Screen screen = MinecraftClientCompat.getScreen(client);
        if (!wouldTriggerNow(client, screen))
        {
            return false;
        }
        return (this.activeKey != null && this.activeKey.equals(key)) || mappingsFor(key, screen).size() > 1;
    }

    /**
     * Whether a key press would actually reach and actuate bindings in the
     * current situation. Vanilla dispatches to KeyMapping here (this is called
     * from KeyMapping.set/click), but the bindings only act while in a world
     * with no text field stealing the key as typing input.
     */
    public static boolean wouldTriggerNow(Minecraft client, Screen screen)
    {
        if (screen != null)
        {
            return !isTypingContext(screen);
        }
        return client.player != null;
    }

    private static boolean isTypingContext(Screen screen)
    {
        if (ImeService.getInstance().hasFocusTarget())
        {
            return true;
        }
        return focusedIsEditable(screen.getFocused());
    }

    private static boolean focusedIsEditable(net.minecraft.client.gui.components.events.GuiEventListener listener)
    {
        if (listener == null)
        {
            return false;
        }
        if (listener instanceof net.minecraft.client.gui.components.EditBox)
        {
            return true;
        }
        if (listener instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container)
        {
            return focusedIsEditable(container.getFocused());
        }
        return false;
    }

    public void completeSelection(KeyMapping mapping, boolean clickHold)
    {
        InputConstants.Key key = this.activeKey;
        Screen screen = this.parentScreen;
        this.activeKey = null;
        this.activeScreen = null;
        this.parentScreen = null;
        Minecraft client = Minecraft.getInstance();
        MinecraftClientCompat.setScreen(client, screen);

        if (mapping == null || key == null)
        {
            return;
        }

        int cooldownTicks = Configs.KEYBIND_SELECTION_COOLDOWN.getIntegerValue();
        if (cooldownTicks > 0)
        {
            this.selectionCooldowns.put(key, new SelectionCooldown(cooldownTicks, !clickHold));
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
            this.parentScreen = null;
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
            this.selectionCooldowns.clear();
            return;
        }

        this.selectionCooldowns.entrySet().removeIf(entry -> {
            InputConstants.Key key = entry.getKey();
            SelectionCooldown cooldown = entry.getValue();
            if (!cooldown.released && !isPhysicallyDown(client, key))
            {
                HeldSelection selected = this.heldSelections.remove(key);
                if (selected != null)
                {
                    setDown(selected.mapping, false);
                }
                cooldown.released = true;
            }
            if (cooldown.released && cooldown.ticks-- <= 0)
            {
                return true;
            }
            return false;
        });

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
        for (Map.Entry<InputConstants.Key, HeldSelection> entry : this.heldSelections.entrySet())
        {
            if (this.selectionCooldowns.containsKey(entry.getKey()))
            {
                continue;
            }
            HeldSelection held = entry.getValue();
            if (++held.ticks >= repeatDelay)
            {
                held.ticks = 0;
                setClicks(held.mapping, 1);
            }
        }
    }

    private static List<KeyMapping> mappingsFor(InputConstants.Key key)
    {
        return mappingsFor(key, MinecraftClientCompat.getScreen(Minecraft.getInstance()));
    }

    private static List<KeyMapping> mappingsFor(InputConstants.Key key, Screen screen)
    {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null)
        {
            return List.of();
        }

        return java.util.Arrays.stream(client.options.keyMappings)
                .filter(mapping -> key.equals(((KeyMappingAccessor) mapping).halfmasa$getBoundKey()))
                .filter(mapping -> KeybindCustomizationStore.getInstance().isActive(mapping, screen))
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

    private static boolean isPhysicallyDown(Minecraft client, InputConstants.Key key)
    {
        //#if MC >= 1.21.10
        com.mojang.blaze3d.platform.Window window = client.getWindow();
        long handle = window.handle();
        //#else
        //$$ long window = client.getWindow().getWindow();
        //$$ long handle = window;
        //#endif
        if (key.getType() == InputConstants.Type.MOUSE)
        {
            return GLFW.glfwGetMouseButton(handle, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM)
        {
            return InputConstants.isKeyDown(window, key.getValue());
        }
        return false;
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

    private static final class SelectionCooldown
    {
        private int ticks;
        private boolean released;

        private SelectionCooldown(int ticks, boolean released)
        {
            this.ticks = ticks;
            this.released = released;
        }
    }
}

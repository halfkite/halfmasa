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
//#if MC >= 1.21.10
import net.minecraft.client.input.KeyEvent;
//#endif
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
    private final Set<Integer> pressedInputKeys = new HashSet<>();
    private final Map<KeyMapping, Integer> orderedComboProgress = new HashMap<>();
    private final Set<KeyMapping> activeCustomCombos = new HashSet<>();
    private boolean customMappingsSynchronized;
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
        this.trackInput(key, pressed);

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
        if (hasJeiBinding(conflicts))
        {
            return false;
        }
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
        this.restoreMovementKeys(client);
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
        List<KeyMapping> conflicts = mappingsFor(key, screen);
        if (hasJeiBinding(conflicts))
        {
            return false;
        }
        return (this.activeKey != null && this.activeKey.equals(key)) || conflicts.size() > 1;
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

    public void handleKeyboardEvent(InputConstants.Key key, boolean pressed)
    {
        this.trackInput(key, pressed);
        this.restoreMovementKeys(Minecraft.getInstance());
    }

    /**
     * Screen changes call KeyMapping.releaseAll(), which clears the held
     * movement state even though the physical keys are still down. Keep the
     * movement mappings aligned while the wheel is visible.
     */
    public void restoreMovementKeys(Minecraft client)
    {
        if (this.activeScreen == null || client.options == null)
        {
            return;
        }
        this.restorePhysicalMovementKeys(client);
    }

    private static void restoreMovementKey(Minecraft client, KeyMapping mapping)
    {
        setDown(mapping, isPhysicallyDown(client, ((KeyMappingAccessor) mapping).halfmasa$getBoundKey()));
    }

    private void restorePhysicalMovementKeys(Minecraft client)
    {
        if (client.options == null)
        {
            return;
        }
        restoreMovementKey(client, client.options.keyUp);
        restoreMovementKey(client, client.options.keyDown);
        restoreMovementKey(client, client.options.keyLeft);
        restoreMovementKey(client, client.options.keyRight);
        restoreMovementKey(client, client.options.keyJump);
        restoreMovementKey(client, client.options.keyShift);
        restoreMovementKey(client, client.options.keySprint);
    }

    public void refreshCustomCombos()
    {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null)
        {
            return;
        }
        this.synchronizeCustomMappings(client);
        Screen screen = MinecraftClientCompat.getScreen(client);
        KeybindCustomizationStore store = KeybindCustomizationStore.getInstance();
        for (KeyMapping mapping : client.options.keyMappings)
        {
            List<Integer> combo = store.comboKeys(mapping);
            if (combo.size() < 2 || !isCustomComboActive(store, mapping, screen))
            {
                boolean wasActive = this.activeCustomCombos.remove(mapping);
                this.orderedComboProgress.remove(mapping);
                if (wasActive)
                {
                    setDown(mapping, false);
                }
                continue;
            }

            boolean held = combo.stream().allMatch(code -> isInputCodeDown(client, code));
            boolean active = store.requiresKeyOrder(mapping)
                    ? held && this.orderedComboProgress.getOrDefault(mapping, 0) == combo.size()
                    : held;
            if (active && this.activeCustomCombos.add(mapping))
            {
                this.triggerCustomCombo(mapping, combo);
            }
            else if (!active && this.activeCustomCombos.remove(mapping))
            {
                setDown(mapping, false);
            }
        }
    }

    private void trackInput(InputConstants.Key key, boolean pressed)
    {
        int inputCode = keyCode(key);
        if (pressed) this.pressedInputKeys.add(inputCode);
        else this.pressedInputKeys.remove(inputCode);
        this.updateCustomCombos(inputCode, pressed);
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
        if (MinecraftClientCompat.getScreen(client) == null)
        {
            this.restorePhysicalMovementKeys(client);
        }

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
            this.clearTransientState();
            Minecraft client = Minecraft.getInstance();
            if (MinecraftClientCompat.getScreen(client) == null)
            {
                this.restorePhysicalMovementKeys(client);
            }
        }
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        this.synchronizeCustomMappings(client);
        if (this.activeScreen != null && MinecraftClientCompat.getScreen(client) != this.activeScreen)
        {
            this.clearTransientState();
            if (MinecraftClientCompat.getScreen(client) == null)
            {
                this.restorePhysicalMovementKeys(client);
            }
        }
        this.restoreMovementKeys(client);
        if (!Configs.KEYBIND_PIE_MENU.getBooleanValue())
        {
            this.clearTransientState();
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
                .filter(mapping -> !KeybindCustomizationStore.getInstance().hasCustomCombination(mapping))
                .filter(mapping -> !isDefaultWheelExcludedMapping(client, mapping))
                .filter(mapping -> !isDebugOnlyMapping(mapping) || isDebugModifierDown(client, key))
                .filter(mapping -> KeybindCustomizationStore.getInstance().isActive(mapping, screen))
                .sorted(Comparator.comparing(KeyMapping::getName))
                .toList();
    }

    private static boolean hasJeiBinding(List<KeyMapping> mappings)
    {
        return mappings.stream().anyMatch(KeybindPieManager::isJeiMapping);
    }

    private static boolean isJeiMapping(KeyMapping mapping)
    {
        String name = mapping.getName().toLowerCase(java.util.Locale.ROOT);
        return name.startsWith("key.jei.") || name.startsWith("jei.");
    }

    private static boolean isDefaultWheelExcludedMapping(Minecraft client, KeyMapping mapping)
    {
        InputConstants.Key key = ((KeyMappingAccessor) mapping).halfmasa$getBoundKey();
        if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
        {
            return true;
        }
        //#if MC >= 26.2
        return mapping == client.options.keySaveHotbarActivator ||
                mapping == client.options.keyLoadHotbarActivator ||
                mapping == client.options.keyDebugOverlay ||
                mapping == client.options.keyDebugModifier;
        //#else
        //$$ return false;
        //#endif
    }

    private void updateCustomCombos(int inputCode, boolean pressed)
    {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null)
        {
            return;
        }
        Screen screen = MinecraftClientCompat.getScreen(client);
        for (KeyMapping mapping : client.options.keyMappings)
        {
            KeybindCustomizationStore store = KeybindCustomizationStore.getInstance();
            List<Integer> combo = store.comboKeys(mapping);
            if (combo.size() < 2 || !isCustomComboActive(store, mapping, screen))
            {
                boolean wasActive = this.activeCustomCombos.remove(mapping);
                this.orderedComboProgress.remove(mapping);
                if (wasActive)
                {
                    setDown(mapping, false);
                }
                continue;
            }

            if (pressed && combo.contains(inputCode) && store.requiresKeyOrder(mapping))
            {
                int progress = this.orderedComboProgress.getOrDefault(mapping, 0);
                if (progress < combo.size() && inputCode == combo.get(progress))
                {
                    progress++;
                }
                else if (inputCode == combo.get(0))
                {
                    progress = 1;
                }
                else
                {
                    progress = 0;
                }
                this.orderedComboProgress.put(mapping, progress);
            }

            boolean active = store.requiresKeyOrder(mapping)
                    ? this.orderedComboProgress.getOrDefault(mapping, 0) == combo.size() &&
                            this.pressedInputKeys.containsAll(combo)
                    : this.pressedInputKeys.containsAll(combo);
            if (active && this.activeCustomCombos.add(mapping))
            {
                this.triggerCustomCombo(mapping, combo);
            }
            else if (!active && this.activeCustomCombos.remove(mapping))
            {
                setDown(mapping, false);
            }

            if (!pressed && !combo.stream().anyMatch(this.pressedInputKeys::contains))
            {
                this.orderedComboProgress.remove(mapping);
            }
        }
    }

    private static boolean isCustomComboActive(KeybindCustomizationStore store, KeyMapping mapping, Screen screen)
    {
        KeybindCustomizationStore.ActivationContext context = store.activationContext(mapping);
        if (context == KeybindCustomizationStore.ActivationContext.DISABLED)
        {
            return false;
        }
        if (context == KeybindCustomizationStore.ActivationContext.GAMEPLAY && screen != null)
        {
            return false;
        }
        if (context == KeybindCustomizationStore.ActivationContext.SCREEN &&
                (screen == null || isTypingContext(screen)))
        {
            return false;
        }
        if (context == KeybindCustomizationStore.ActivationContext.AUTO &&
                screen != null && isTypingContext(screen))
        {
            return false;
        }
        return true;
    }

    private void triggerCustomCombo(KeyMapping mapping, List<Integer> combo)
    {
        Minecraft client = Minecraft.getInstance();
        Screen screen = MinecraftClientCompat.getScreen(client);
        if (screen != null && !combo.isEmpty() && combo.get(0) >= 0)
        {
            InputConstants.Key previousKey = ((KeyMappingAccessor) mapping).halfmasa$getBoundKey();
            InputConstants.Key triggerKey = InputConstants.Type.KEYSYM.getOrCreate(combo.get(0));
            mapping.setKey(triggerKey);
            boolean handled;
            try
            {
                //#if MC >= 1.21.10
                handled = screen.keyPressed(new KeyEvent(combo.get(0), 0, 0));
                //#else
                //$$ handled = screen.keyPressed(combo.get(0), 0, 0);
                //#endif
            }
            finally
            {
                mapping.setKey(previousKey);
                KeyMapping.resetMapping();
            }
            if (handled)
            {
                return;
            }
        }
        setDown(mapping, true);
        setClicks(mapping, 1);
    }

    private void synchronizeCustomMappings(Minecraft client)
    {
        if (this.customMappingsSynchronized || client.options == null)
        {
            return;
        }
        KeybindCustomizationStore store = KeybindCustomizationStore.getInstance();
        boolean changed = false;
        for (KeyMapping mapping : client.options.keyMappings)
        {
            if (store.comboKeys(mapping).size() >= 2 &&
                    !InputConstants.UNKNOWN.equals(((KeyMappingAccessor) mapping).halfmasa$getBoundKey()))
            {
                mapping.setKey(InputConstants.UNKNOWN);
                changed = true;
            }
        }
        if (changed)
        {
            KeyMapping.resetMapping();
        }
        this.customMappingsSynchronized = true;
    }

    /** Allows the next tick to re-apply combo unbinding after the store was reloaded from disk. */
    public void invalidateCustomMappingSync()
    {
        this.customMappingsSynchronized = false;
    }

    private static int keyCode(InputConstants.Key key)
    {
        return key.getType() == InputConstants.Type.MOUSE ? -(key.getValue() + 1) : key.getValue();
    }

    private static boolean isInputCodeDown(Minecraft client, int code)
    {
        if (code < 0)
        {
            //#if MC >= 1.21.10
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), -code - 1) == GLFW.GLFW_PRESS;
            //#else
            //$$ return GLFW.glfwGetMouseButton(client.getWindow().getWindow(), -code - 1) == GLFW.GLFW_PRESS;
            //#endif
        }
        //#if MC >= 1.21.10
        return InputConstants.isKeyDown(client.getWindow(), code);
        //#else
        //$$ return InputConstants.isKeyDown(client.getWindow().getWindow(), code);
        //#endif
    }

    private static boolean isDebugOnlyMapping(KeyMapping mapping)
    {
        //#if MC >= 26.2
        Minecraft client = Minecraft.getInstance();
        if (client.options != null && client.options.debugKeys != null)
        {
            for (KeyMapping debugKey : client.options.debugKeys)
            {
                if (debugKey == mapping)
                {
                    return true;
                }
            }
        }
        //#endif
        return false;
    }

    private static boolean isDebugModifierDown(Minecraft client, InputConstants.Key pressedKey)
    {
        //#if MC >= 26.2
        if (client.options == null || client.options.keyDebugModifier == null)
        {
            return false;
        }
        InputConstants.Key debugModifier =
                ((KeyMappingAccessor) client.options.keyDebugModifier).halfmasa$getBoundKey();
        return !debugModifier.equals(pressedKey) && isPhysicallyDown(client, debugModifier);
        //#else
        //$$ return false;
        //#endif
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

    private void clearTransientState()
    {
        this.heldSelections.values().forEach(held -> setDown(held.mapping, false));
        this.heldSelections.clear();
        this.oneShotReleases.keySet().forEach(mapping -> setDown(mapping, false));
        this.oneShotReleases.clear();
        this.selectionCooldowns.clear();
        this.activeCustomCombos.forEach(mapping -> setDown(mapping, false));
        this.activeCustomCombos.clear();
        this.orderedComboProgress.clear();
        this.pressedInputKeys.clear();
        this.activeKey = null;
        this.activeScreen = null;
        this.parentScreen = null;
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

package io.github.halfmasa.xaerobinding.feature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//#if MC >= 26.3
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindCategory;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import io.github.halfmasa.xaerobinding.gui.KeymapBrowserScreen;
import io.github.halfmasa.xaerobinding.gui.IgnoredKeysScreen;
//#endif

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//#if MC >= 1.21.10
import net.minecraft.client.input.KeyEvent;
//#endif
import net.minecraft.client.gui.screens.Screen;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.InputCompat;
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
    //#if MC >= 26.3
    private final List<Integer> pressedInputOrder = new ArrayList<>();
    private Object inputLevel;
    private KeyMapping[] synchronizedMappings;
    //#endif
    private final Map<KeyMapping, Integer> orderedComboProgress = new HashMap<>();
    private final Set<KeyMapping> activeCustomCombos = new HashSet<>();
    private boolean customMappingsSynchronized;
    private InputConstants.Key activeKey;
    private KeybindPieScreen activeScreen;
    private Screen parentScreen;

    //#if MC >= 26.3
    public record PieAction(KeyMapping mapping, IHotkey hotkey)
    {
        public String displayName()
        {
            if (this.mapping != null)
            {
                return KeybindCustomizationStore.getInstance().displayName(this.mapping);
            }
            String translated = this.hotkey.getTranslatedName();
            if (translated != null && !translated.isBlank() && !translated.equals(this.hotkey.getName()))
            {
                return translated;
            }
            String pretty = this.hotkey.getPrettyName();
            return pretty == null || pretty.isBlank() ? this.hotkey.getName() : pretty;
        }
    }
    //#endif

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

        //#if MC >= 26.3
        if (this.activeScreen != null)
        {
            if (pressed && !key.equals(this.activeKey))
            {
                if (!this.transitionSelection(key))
                {
                    this.dismissSelectionForNewInput(key);
                    return false;
                }
            }
            return true;
        }
        //#endif
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

        //#if MC >= 26.3
        List<PieAction> conflicts = wheelCandidates(key, screen);
        if (hasJeiBinding(conflicts.stream().map(PieAction::mapping).filter(java.util.Objects::nonNull).toList()) ||
            hasJadeDetailsBinding(conflicts.stream().map(PieAction::mapping).filter(java.util.Objects::nonNull).toList()))
        //#else
        //$$ List<KeyMapping> conflicts = mappingsFor(key, screen);
        //$$ if (hasJeiBinding(conflicts) || hasJadeDetailsBinding(conflicts))
        //#endif
        {
            return false;
        }
        if (conflicts.size() < 2)
        {
            return false;
        }

        //#if MC >= 26.3
        for (PieAction action : conflicts)
        {
            if (action.mapping() != null)
            {
                setDown(action.mapping(), false);
                if (KeybindCustomizationStore.getInstance().hasCustomCombination(action.mapping()))
                {
                    this.activeCustomCombos.add(action.mapping());
                }
            }
        }
        //#else
        //$$ for (KeyMapping mapping : conflicts)
        //$$ {
        //$$     setDown(mapping, false);
        //$$ }
        //#endif
        this.activeKey = key;
        this.activeScreen = new KeybindPieScreen(key, conflicts);
        this.parentScreen = screen;
        this.activeScreen.setParent(screen);
        MinecraftClientCompat.setScreen(client, this.activeScreen);
        this.restoreMovementKeys(client);
        return true;
    }

    //#if MC >= 26.3
    private boolean transitionSelection(InputConstants.Key key)
    {
        if (isIgnored(key)) return false;
        List<PieAction> conflicts = this.wheelCandidates(key, this.parentScreen);
        List<KeyMapping> vanilla = conflicts.stream().map(PieAction::mapping)
                .filter(java.util.Objects::nonNull).toList();
        if (conflicts.size() < 2 || hasJeiBinding(vanilla) || hasJadeDetailsBinding(vanilla))
        {
            return false;
        }
        for (PieAction action : conflicts)
        {
            if (action.mapping() != null)
            {
                setDown(action.mapping(), false);
                if (KeybindCustomizationStore.getInstance().hasCustomCombination(action.mapping()))
                {
                    this.activeCustomCombos.add(action.mapping());
                }
            }
        }
        this.activeKey = key;
        this.activeScreen.updateSelection(key, conflicts);
        return true;
    }

    private void dismissSelectionForNewInput(InputConstants.Key key)
    {
        Minecraft client = Minecraft.getInstance();
        Screen screen = this.parentScreen;
        int inputCode = keyCode(key);
        KeybindCustomizationStore store = KeybindCustomizationStore.getInstance();
        if (client.options != null)
        {
            for (KeyMapping mapping : client.options.keyMappings)
            {
                List<Integer> combo = store.comboKeys(mapping);
                if (combo.size() >= 2 && store.requiresKeyOrder(mapping) &&
                        combo.get(combo.size() - 1) == inputCode &&
                        this.samePressedChord(combo) && this.matchesPressedOrder(combo))
                {
                    this.orderedComboProgress.put(mapping, combo.size() - 1);
                }
            }
        }
        this.activeKey = null;
        this.activeScreen = null;
        this.parentScreen = null;
        MinecraftClientCompat.setScreen(client, screen);
        if (screen == null) this.restorePhysicalMovementKeys(client);
        this.updateCustomCombos(inputCode, true);
    }
    //#endif

    public boolean handleClick(InputConstants.Key key)
    {
        //#if MC >= 26.3
        if (this.activeScreen != null) return true;
        //#endif
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
        //#if MC >= 26.3
        List<PieAction> conflicts = wheelCandidates(key, screen);
        List<KeyMapping> vanilla = conflicts.stream().map(PieAction::mapping)
                .filter(java.util.Objects::nonNull).toList();
        if (hasJeiBinding(vanilla) || hasJadeDetailsBinding(vanilla))
        //#else
        //$$ List<KeyMapping> conflicts = mappingsFor(key, screen);
        //$$ if (hasJeiBinding(conflicts) || hasJadeDetailsBinding(conflicts))
        //#endif
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
        //#if MC >= 26.3
        if (KeymapBrowserScreen.isEditingBindings(screen) || IgnoredKeysScreen.isEditingKeys(screen)) return false;
        //#endif
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
        //#if MC >= 26.3
        if (pressed) this.handleSet(key, true);
        //#endif
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
        //#if MC >= 26.3
        if (this.activeScreen != null) return;
        //#endif
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
        //#if MC >= 26.3
        Minecraft client = Minecraft.getInstance();
        this.refreshInputSession(client);
        // A GUI can consume releases. Never let an old menu click/Enter become
        // an extra member of every chord after entering the world.
        this.pruneReleasedInputs(client, inputCode);
        //#endif
        boolean changed = pressed ? this.pressedInputKeys.add(inputCode) : this.pressedInputKeys.remove(inputCode);
        //#if MC >= 26.3
        if (changed)
        {
            if (pressed) this.pressedInputOrder.add(inputCode);
            else this.pressedInputOrder.remove(Integer.valueOf(inputCode));
        }
        if (changed && !this.shouldDeferCustomCombo(key, pressed))
        {
            this.updateCustomCombos(inputCode, pressed);
        }
        //#else
        //$$ this.updateCustomCombos(inputCode, pressed);
        //#endif
    }

    //#if MC >= 26.3
    private boolean shouldDeferCustomCombo(InputConstants.Key key, boolean pressed)
    {
        if (!pressed || !Configs.KEYBIND_PIE_MENU.getBooleanValue() || isIgnored(key) ||
                this.activeScreen != null || this.selectionCooldowns.containsKey(key))
        {
            return this.activeScreen != null;
        }
        Minecraft client = Minecraft.getInstance();
        Screen screen = MinecraftClientCompat.getScreen(client);
        if (!wouldTriggerNow(client, screen)) return false;
        List<PieAction> actions = this.wheelCandidates(key, screen);
        List<KeyMapping> vanilla = actions.stream().map(PieAction::mapping)
                .filter(java.util.Objects::nonNull).toList();
        return actions.size() > 1 && !hasJeiBinding(vanilla) && !hasJadeDetailsBinding(vanilla);
    }

    public void handleMasaMouseInput(InputConstants.Key key, boolean pressed)
    {
        this.trackInput(key, pressed);
        if (pressed) this.handleSet(key, true);
    }

    public boolean blockMasaDispatch(int inputCode)
    {
        if (this.activeScreen != null) return true;
        int normalizedCode = normalizeMasaCode(inputCode);
        return this.selectionCooldowns.keySet().stream().anyMatch(key -> keyCode(key) == normalizedCode);
    }
    //#endif

    //#if MC >= 26.3
    public void completeSelection(PieAction action, boolean clickHold)
    //#else
    //$$ public void completeSelection(KeyMapping mapping, boolean clickHold)
    //#endif
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

        //#if MC >= 26.3
        if (action == null || key == null)
        //#else
        //$$ if (mapping == null || key == null)
        //#endif
        {
            return;
        }

        int cooldownTicks = Configs.KEYBIND_SELECTION_COOLDOWN.getIntegerValue();
        if (cooldownTicks > 0)
        {
            this.selectionCooldowns.put(key, new SelectionCooldown(cooldownTicks, !clickHold));
        }

        //#if MC >= 26.3
        if (action.hotkey() != null)
        {
            IKeybind binding = action.hotkey().getKeybind();
            if (binding instanceof KeybindMulti multi && multi.getCallback() != null)
            {
                KeyAction activateOn = binding.getSettings().getActivateOn();
                multi.getCallback().onKeyAction(
                        activateOn == KeyAction.RELEASE ? KeyAction.RELEASE : KeyAction.PRESS, binding);
            }
            return;
        }
        KeyMapping mapping = action.mapping();
        //#endif
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
        //#if MC >= 26.3
        this.refreshInputSession(client);
        this.pruneReleasedInputs(client, null);
        //#endif
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

    //#if MC >= 26.3
    private List<PieAction> wheelCandidates(InputConstants.Key key, Screen screen)
    {
        List<PieAction> actions = new ArrayList<>();
        int inputCode = keyCode(key);
        if (this.pressedInputKeys.size() == 1 && this.pressedInputKeys.contains(inputCode))
        {
            for (KeyMapping mapping : mappingsFor(key, screen))
            {
                if (KeybindCustomizationStore.getInstance().participatesInWheel(mapping.getName(), List.of(inputCode)))
                {
                    actions.add(new PieAction(mapping, null));
                }
            }
        }

        Minecraft client = Minecraft.getInstance();
        KeybindCustomizationStore store = KeybindCustomizationStore.getInstance();
        if (client.options != null)
        {
            for (KeyMapping mapping : client.options.keyMappings)
            {
                List<Integer> combo = store.comboKeys(mapping);
                if (store.participatesInWheel(mapping.getName(), combo) && combo.size() >= 2 && combo.contains(inputCode) &&
                    this.samePressedChord(combo) &&
                    (!store.requiresKeyOrder(mapping) || this.matchesPressedOrder(combo)) &&
                    isCustomComboActive(store, mapping, screen))
                {
                    actions.add(new PieAction(mapping, null));
                }
            }
        }

        for (KeybindCategory category : InputEventHandler.getKeybindManager().getKeybindCategories())
        {
            for (IHotkey hotkey : category.getHotkeys())
            {
                IKeybind binding = hotkey.getKeybind();
                if (!(binding instanceof KeybindMulti multi) || multi.getCallback() == null)
                {
                    continue;
                }
                List<Integer> codes = binding.getKeys().stream()
                        .map(KeybindPieManager::normalizeMasaCode).toList();
                if (!store.participatesInWheel(hotkey.getName(), codes)) continue;
                KeybindSettings settings = binding.getSettings();
                if (codes.isEmpty() || !codes.contains(inputCode) ||
                    !this.samePressedChord(codes) ||
                    (settings.isOrderSensitive() && !this.matchesPressedOrder(codes)) ||
                    !isMasaContextActive(settings, screen))
                {
                    continue;
                }
                actions.add(new PieAction(null, hotkey));
            }
        }
        return actions;
    }

    private boolean samePressedChord(List<Integer> codes)
    {
        return codes.size() == this.pressedInputKeys.size() &&
                this.pressedInputKeys.containsAll(codes);
    }

    private boolean matchesPressedOrder(List<Integer> codes)
    {
        int next = 0;
        for (int pressed : this.pressedInputOrder)
        {
            if (next < codes.size() && pressed == codes.get(next)) next++;
        }
        return next == codes.size();
    }

    private static int normalizeMasaCode(int code)
    {
        // MaLiLib subtracts 100 from MouseButtonEvent.input(), which is the
        // SDL mouse button number on 26.3. The wheel stores compact negatives.
        return code < -80 && code > -100
                ? InputCompat.mouseButtonToLayoutCode(code + 100) : code;
    }

    private static boolean isMasaContextActive(KeybindSettings settings, Screen screen)
    {
        return settings.getContext() == KeybindSettings.Context.ANY ||
                (screen == null && settings.getContext() == KeybindSettings.Context.INGAME) ||
                (screen != null && settings.getContext() == KeybindSettings.Context.GUI);
    }
    //#endif

    private static boolean hasJeiBinding(List<KeyMapping> mappings)
    {
        return mappings.stream().anyMatch(KeybindPieManager::isJeiMapping);
    }

    private static boolean hasJadeDetailsBinding(List<KeyMapping> mappings)
    {
        // Jade's hold-to-show-details action must pass through immediately,
        // including when it shares a key with other mappings.
        return mappings.stream().anyMatch(mapping ->
                "key.jade.show_details".equals(mapping.getName()));
    }

    private static boolean isJeiMapping(KeyMapping mapping)
    {
        String name = mapping.getName().toLowerCase(java.util.Locale.ROOT);
        return name.startsWith("key.jei.") || name.startsWith("jei.");
    }

    private static boolean isDefaultWheelExcludedMapping(Minecraft client, KeyMapping mapping)
    {
        InputConstants.Key key = ((KeyMappingAccessor) mapping).halfmasa$getBoundKey();
        if (key.getType() == InputConstants.Type.MOUSE &&
                key.getValue() == InputConstants.MOUSE_BUTTON_MIDDLE)
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
            InputConstants.Key triggerKey = InputCompat.keyboardKey(combo.get(0));
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
        if (client.options == null)
        {
            return;
        }
        //#if MC >= 26.3
        if (this.customMappingsSynchronized && this.synchronizedMappings == client.options.keyMappings) return;
        this.synchronizedMappings = client.options.keyMappings;
        //#else
        //$$ if (this.customMappingsSynchronized) return;
        //#endif
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

    //#if MC >= 26.3
    private void refreshInputSession(Minecraft client)
    {
        if (this.inputLevel != client.level)
        {
            List<Integer> stillHeld = this.pressedInputOrder.stream()
                    .filter(code -> isInputCodeDown(client, code)).toList();
            this.clearTransientState();
            this.pressedInputKeys.addAll(stillHeld);
            this.pressedInputOrder.addAll(stillHeld);
            this.customMappingsSynchronized = false;
            this.inputLevel = client.level;
        }
    }

    private void pruneReleasedInputs(Minecraft client, Integer currentEvent)
    {
        List<Integer> released = this.pressedInputKeys.stream()
                .filter(code -> !code.equals(currentEvent) && !isInputCodeDown(client, code)).toList();
        for (int code : released)
        {
            this.pressedInputKeys.remove(code);
            this.pressedInputOrder.remove(Integer.valueOf(code));
            this.updateCustomCombos(code, false);
        }
    }
    //#endif

    private static int keyCode(InputConstants.Key key)
    {
        return key.getType() == InputConstants.Type.MOUSE
                ? InputCompat.mouseButtonToLayoutCode(key.getValue()) : key.getValue();
    }

    private static boolean isInputCodeDown(Minecraft client, int code)
    {
        if (code < 0)
        {
            return InputCompat.isMouseButtonDown(client, InputCompat.layoutCodeToMouseButton(code));
        }
        return InputCompat.isKeyDown(client, code);
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
        //#if MC >= 26.3
        return IgnoredKeySelection.isIgnored(keyCode(key),
                IgnoredKeySelection.parse(Configs.KEYBIND_IGNORED_KEYS.getStringValue()),
                Configs.KEYBIND_INVERT_IGNORED_KEYS.getBooleanValue());
        //#else
        //$$ Set<Integer> ignored = new HashSet<>();
        //$$ for (String value : Configs.KEYBIND_IGNORED_KEYS.getStringValue().split("[,;\\s]+"))
        //$$ {
        //$$     try
        //$$     {
        //$$         ignored.add(Integer.parseInt(value));
        //$$     }
        //$$     catch (NumberFormatException ignoredException)
        //$$     {
        //$$     }
        //$$ }
        //$$ boolean listed = InputCompat.isKeyboardKey(key) && ignored.contains(key.getValue());
        //$$ return Configs.KEYBIND_INVERT_IGNORED_KEYS.getBooleanValue() ? !listed : listed;
        //#endif
    }

    private static boolean isPhysicallyDown(Minecraft client, InputConstants.Key key)
    {
        if (key.getType() == InputConstants.Type.MOUSE)
        {
            return InputCompat.isMouseButtonDown(client, key.getValue());
        }
        if (InputCompat.isKeyboardKey(key))
        {
            return InputCompat.isKeyDown(client, key.getValue());
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
        //#if MC >= 26.3
        this.pressedInputOrder.clear();
        //#endif
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

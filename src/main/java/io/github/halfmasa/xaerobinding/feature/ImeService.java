package io.github.halfmasa.xaerobinding.feature;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import city.windmill.ingameime.client.jni.ExternalBaseIME;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.mixin.KeyboardHandlerAccessor;

public final class ImeService implements IClientTickHandler
{
    private static final ImeService INSTANCE = new ImeService();
    private EditBox focusedEdit;
    private Object focusedTarget;
    private Mode mode = Mode.DISABLED;
    private boolean nativeActive;
    private boolean previousMaster;
    private boolean previousFullscreen;
    private long pendingSingleClickAt;
    private long lastHotkeyAt;
    private boolean temporaryCommitted;
    private double previousMouseX;
    private double previousMouseY;

    private ImeService() {}

    public static ImeService getInstance()
    {
        return INSTANCE;
    }

    public boolean onModeHotkey()
    {
        if (!Configs.CONTINGAME_IME.getBooleanValue())
        {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastHotkeyAt <= 300L)
        {
            this.pendingSingleClickAt = 0L;
            this.mode = Mode.ENABLED;
            this.temporaryCommitted = false;
        }
        else
        {
            this.pendingSingleClickAt = now;
        }
        this.lastHotkeyAt = now;
        updateNativeState();
        return true;
    }

    public void onEditFocus(EditBox edit, boolean focused)
    {
        if (focused)
        {
            this.focusedEdit = edit;
            this.focusedTarget = edit;
            if (Configs.CONTINGAME_IME.getBooleanValue())
            {
                this.mode = Mode.ENABLED;
            }
        }
        else if (this.focusedEdit == edit)
        {
            this.focusedEdit = null;
            this.focusedTarget = null;
            this.mode = Mode.DISABLED;
        }
        updateNativeState();
    }

    public void onGenericEditFocus(Object target, boolean focused)
    {
        if (focused)
        {
            this.focusedTarget = target;
            this.focusedEdit = null;
            if (Configs.CONTINGAME_IME.getBooleanValue())
            {
                this.mode = Mode.ENABLED;
            }
            Minecraft client = Minecraft.getInstance();
            ImeOverlay.setCaret(
                    client.getWindow().getGuiScaledWidth() / 2,
                    client.getWindow().getGuiScaledHeight() / 2);
        }
        else if (this.focusedTarget == target)
        {
            this.focusedTarget = null;
            this.focusedEdit = null;
            this.mode = Mode.DISABLED;
        }
        updateNativeState();
    }

    public void updateCaret(EditBox edit, int x, int y)
    {
        if (this.focusedEdit == edit)
        {
            ImeOverlay.setCaret(x, y);
        }
    }

    public void onScreenChanged()
    {
        this.focusedEdit = null;
        this.focusedTarget = null;
        this.mode = Mode.DISABLED;
        this.temporaryCommitted = false;
        ImeOverlay.clear();
        updateNativeState();
    }

    public void commit(String value)
    {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            Object target = this.focusedTarget;
            EditBox edit = this.focusedEdit;
            if (target == null || edit != null && !edit.isFocused())
            {
                return;
            }

            String result = value == null ? "" : value;
            if (Configs.IME_AUTO_REPLACE_SLASH.getBooleanValue() && edit != null &&
                    edit.getCursorPosition() == 0 && !result.isEmpty())
            {
                List<String> replacements = Configs.IME_SLASH_CHARACTERS.getStrings();
                String first = result.substring(0, 1);
                if (replacements.stream().anyMatch(first::equals))
                {
                    result = "/" + result.substring(1);
                }
            }
            //#if MC >= 1.21.10
            long window = client.getWindow().handle();
            //#else
            //$$ long window = client.getWindow().getWindow();
            //#endif
            KeyboardHandlerAccessor keyboard = (KeyboardHandlerAccessor) client.keyboardHandler;
            for (int codePoint : result.codePoints().toArray())
            {
                //#if MC >= 26.1
                keyboard.halfmasa$charTyped(window, new net.minecraft.client.input.CharacterEvent(codePoint));
                //#elseif MC >= 1.21.10
                //$$ keyboard.halfmasa$charTyped(window, new net.minecraft.client.input.CharacterEvent(codePoint, 0));
                //#else
                //$$ keyboard.halfmasa$charTyped(window, codePoint, 0);
                //#endif
            }
            if (this.mode == Mode.TEMPORARY)
            {
                this.temporaryCommitted = true;
            }
            ImeOverlay.setComposition(null, 0);
            updateNativeState();
        });
    }

    public boolean isNativeActive()
    {
        return this.nativeActive;
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        boolean master = Configs.CONTINGAME_IME.getBooleanValue();
        if (master != this.previousMaster)
        {
            this.previousMaster = master;
            if (!master)
            {
                this.mode = Mode.DISABLED;
                this.focusedEdit = null;
                this.focusedTarget = null;
                ImeOverlay.clear();
            }
        }

        if (master && this.pendingSingleClickAt != 0L &&
            System.currentTimeMillis() - this.pendingSingleClickAt > 300L)
        {
            this.pendingSingleClickAt = 0L;
            this.mode = this.nativeActive ? Mode.DISABLED : Mode.TEMPORARY;
            this.temporaryCommitted = false;
        }

        if (master && this.mode == Mode.TEMPORARY && this.temporaryCommitted && client.mouseHandler != null)
        {
            double mouseX = client.mouseHandler.xpos();
            double mouseY = client.mouseHandler.ypos();
            if (mouseX != this.previousMouseX || mouseY != this.previousMouseY)
            {
                this.mode = Mode.DISABLED;
                this.temporaryCommitted = false;
            }
            this.previousMouseX = mouseX;
            this.previousMouseY = mouseY;
        }

        boolean fullscreen = client.getWindow().isFullscreen();
        if (master && fullscreen != this.previousFullscreen && ExternalBaseIME.getInstance().isInitialized())
        {
            this.previousFullscreen = fullscreen;
            ExternalBaseIME.getInstance().setFullScreen(fullscreen);
        }
        updateNativeState();
    }

    private void updateNativeState()
    {
        boolean master = Configs.CONTINGAME_IME.getBooleanValue();
        boolean targetActive = this.focusedTarget != null &&
                (this.focusedEdit == null || this.focusedEdit.isFocused());
        boolean desired = master && targetActive &&
                this.mode != Mode.DISABLED && !commandModeDisabled();

        if (master && !ExternalBaseIME.getInstance().isInitialized())
        {
            ExternalBaseIME.getInstance().initialize();
            this.previousFullscreen = Minecraft.getInstance().getWindow().isFullscreen();
        }
        if (ExternalBaseIME.getInstance().isInitialized() && desired != this.nativeActive)
        {
            this.nativeActive = desired;
            ExternalBaseIME.getInstance().setState(desired);
        }
        else if (!ExternalBaseIME.getInstance().isInitialized())
        {
            this.nativeActive = false;
        }
    }

    private boolean commandModeDisabled()
    {
        if (!Configs.IME_DISABLE_IN_COMMAND_MODE.getBooleanValue() || this.focusedEdit == null ||
            !(MinecraftClientCompat.getScreen(Minecraft.getInstance()) instanceof ChatScreen))
        {
            return false;
        }

        String value = this.focusedEdit.getValue();
        if (!value.startsWith("/"))
        {
            return false;
        }
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        return !(lower.equals("/msg") || lower.startsWith("/msg ") ||
                 lower.equals("/tell") || lower.startsWith("/tell ") ||
                 lower.equals("/tellraw") || lower.startsWith("/tellraw "));
    }

    private enum Mode
    {
        DISABLED,
        TEMPORARY,
        ENABLED
    }
}

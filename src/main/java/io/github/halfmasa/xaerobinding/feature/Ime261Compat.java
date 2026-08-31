package io.github.halfmasa.xaerobinding.feature;

import java.util.List;
import java.lang.invoke.MethodHandle;

//#if MC >= 26.1
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.ImeStyle;
//#endif

import net.minecraft.client.Minecraft;

/**
 * Glue for the vanilla GLFW-based input method stack on Minecraft 26.1+.
 * Provides the composition/candidate style switch and the Chinese/English
 * status badge that queries the system IME conversion mode directly.
 */
public final class Ime261Compat
{
    private static final int IME_CMODE_NATIVE = 0x0001;
    private static final long POLL_INTERVAL_MS = 100L;

    private static volatile boolean typing;
    private static volatile int conversionMode = -1;
    private static long lastPoll;
    private static MethodHandle getForegroundWindow;
    private static MethodHandle immGetContext;
    private static MethodHandle immGetOpenStatus;
    private static MethodHandle immGetConversionStatus;
    private static boolean nativeLookupFailed;
    private static boolean capturedPreeditOnce;

    private Ime261Compat() {}

    //#if MC >= 26.1

    public static boolean isModStyle()
    {
        return Configs.IME_RENDER_STYLE.getOptionListValue() == ImeStyle.MOD;
    }

    public static boolean shouldRenderOverlayLayer()
    {
        if (!typing)
        {
            return false;
        }
        return isModStyle() || Configs.IME_STATUS_INDICATOR.getBooleanValue();
    }

    /**
     * "中"/"英" while an edit target is focused and the system IME state is known.
     */
    public static String getBadgeText()
    {
        if (!Configs.IME_STATUS_INDICATOR.getBooleanValue() || !typing)
        {
            return null;
        }
        int mode = conversionMode;
        if (mode < 0)
        {
            return null;
        }
        return mode == 1 ? "中" : "英";
    }

    public static void onClientTick(Minecraft client)
    {
        typing = ImeService.getInstance().hasFocusTarget();
        if (!typing)
        {
            conversionMode = -1;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPoll < POLL_INTERVAL_MS)
        {
            return;
        }
        lastPoll = now;
        Integer mode = queryConversionMode(client);
        if (mode != null)
        {
            conversionMode = mode;
        }
    }

    /**
     * Called for every vanilla preedit update; only feeds the mod-style overlay.
     */
    public static void onPreedit(String fullText, int caret, List<String> blocks, int focused)
    {
        if (!capturedPreeditOnce)
        {
            capturedPreeditOnce = true;
            XaeroWorldBinding.LOGGER.info(
                    "halfmasa IME preedit captured: style={}, text length={}",
                    Configs.IME_RENDER_STYLE.getOptionListValue().getStringValue(),
                    fullText == null ? 0 : fullText.length());
        }
        if (!isModStyle())
        {
            return;
        }
        if (fullText == null || fullText.isEmpty())
        {
            ImeOverlay.setComposition(null, 0);
            ImeOverlay.setCandidates(null, -1);
        }
        else
        {
            ImeOverlay.setComposition(fullText, caret);
            ImeOverlay.setCandidates(blocks.toArray(new String[0]), focused);
        }
    }

    public static boolean cancelVanillaPreeditRender()
    {
        return isModStyle() || Configs.IME_HIDE_VANILLA_PREEDIT.getBooleanValue();
    }

    private static Integer queryConversionMode(Minecraft client)
    {
        try
        {
            if (!ensureHandles())
            {
                return null;
            }
            MemorySegment hwnd = (MemorySegment) getForegroundWindow.invoke();
            if (hwnd.equals(MemorySegment.NULL))
            {
                return null;
            }
            MemorySegment himc = (MemorySegment) immGetContext.invoke(hwnd);
            if (himc.equals(MemorySegment.NULL))
            {
                return null;
            }
            if ((int) immGetOpenStatus.invoke(himc) == 0)
            {
                return 0;
            }
            try (Arena arena = Arena.ofConfined())
            {
                MemorySegment conversion = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment sentence = arena.allocate(ValueLayout.JAVA_INT);
                if ((int) immGetConversionStatus.invoke(himc, conversion, sentence) == 0)
                {
                    return null;
                }
                return (conversion.get(ValueLayout.JAVA_INT, 0) & IME_CMODE_NATIVE) != 0 ? 1 : 0;
            }
        }
        catch (Throwable throwable)
        {
            nativeLookupFailed = true;
            return null;
        }
    }

    private static synchronized boolean ensureHandles()
    {
        if (nativeLookupFailed)
        {
            return false;
        }
        if (getForegroundWindow != null)
        {
            return true;
        }
        try
        {
            Linker linker = Linker.nativeLinker();
            Arena arena = Arena.global();
            SymbolLookup user32 = SymbolLookup.libraryLookup("user32", arena);
            SymbolLookup imm32 = SymbolLookup.libraryLookup("imm32", arena);
            getForegroundWindow = linker.downcallHandle(
                    user32.find("GetForegroundWindow").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS));
            immGetContext = linker.downcallHandle(
                    imm32.find("ImmGetContext").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            immGetOpenStatus = linker.downcallHandle(
                    imm32.find("ImmGetOpenStatus").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            immGetConversionStatus = linker.downcallHandle(
                    imm32.find("ImmGetConversionStatus").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            return true;
        }
        catch (Throwable throwable)
        {
            nativeLookupFailed = true;
            return false;
        }
    }

    //#else
    //$$ public static boolean isModStyle() { return false; }
    //$$ public static boolean shouldRenderOverlayLayer() { return false; }
    //$$ public static String getBadgeText() { return null; }
    //$$ public static void onClientTick(Minecraft client) {}
    //$$ public static void onPreedit(String fullText, int caret, List<String> blocks, int focused) {}
    //$$ public static boolean cancelVanillaPreeditRender() { return false; }
    //$$ public static void applyWindowHints() {}
    //#endif
}

package io.github.halfmasa.xaerobinding.feature;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.world.item.Item;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

/** Optional Hana API adapter. Keep printer types out of halfmasa's class-loading dependencies. */
public final class PrinterRefillBridge
{
    private static Method state, token;
    private static Constructor<?> reservation;
    private static Object waiting;
    private static boolean failed;
    private static boolean statusLookedUp;
    private static Method enabled, printMode;
    private PrinterRefillBridge() {}

    public static boolean isPrinting(long idleMillis)
    {
        if (!statusLookedUp)
        {
            statusLookedUp = true;
            try
            {
                Class<?> config = Class.forName("me.aleksilassila.litematica.printer.utils.ConfigUtils");
                enabled = config.getMethod("isEnable");
                printMode = config.getMethod("isPrintMode");
            }
            catch (ReflectiveOperationException | LinkageError e) { enabled = printMode = null; }
        }
        if (enabled != null) try { return (boolean) enabled.invoke(null) && (boolean) printMode.invoke(null); }
        catch (ReflectiveOperationException | RuntimeException e) { enabled = printMode = null; }
        // Forks without status helpers use recently observed demand, allowing slow scan intervals.
        return idleMillis < 10_000;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object onReservation(List<Item> accepted, Item preferred, Object source, Object result)
    {
        if (failed || result == null || !(source instanceof Enum<?> origin) || !origin.name().equals("PRINT")) return result;
        try
        {
            if (state == null)
            {
                Class<?> type = result.getClass();
                state = type.getMethod("state");
                token = type.getMethod("token");
                Class<?> stateType = state.getReturnType();
                waiting = Enum.valueOf((Class) stateType, "PENDING");
                reservation = type.getConstructor(long.class, stateType);
            }
            if (((Enum<?>) state.invoke(result)).name().equals("UNAVAILABLE") &&
                    LitematicaMaterialRefill.getInstance().onPrinterMaterialNeeded(accepted, preferred))
                return reservation.newInstance(token.invoke(result), waiting);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            failed = true;
            XaeroWorldBinding.LOGGER.warn("Printer material request API differs from the supported Hana API; refill adapter disabled", e);
        }
        return result;
    }
}

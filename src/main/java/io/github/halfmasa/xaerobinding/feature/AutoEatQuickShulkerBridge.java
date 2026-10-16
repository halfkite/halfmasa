package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Optional adapter for MoRanpcy QuickShulker's 26.3 open protocol. */
public final class AutoEatQuickShulkerBridge
{
    private static boolean checked;
    private static Method canSend;
    private static Method sendOpen;
    private static Object channel;
    private AutoEatQuickShulkerBridge() {}

    public static boolean isAvailable()
    {
        if (!checked)
        {
            checked = true;
            if (FabricLoader.getInstance().isModLoaded("quickshulker"))
            {
                try
                {
                    Class<?> packet = Class.forName("net.kyrptonaught.quickshulker.network.OpenShulkerPacket");
                    channel = packet.getField("OPEN_SHULKER_PACKET").get(null);
                    sendOpen = packet.getMethod("sendOpenPacket", int.class);
                    canSend = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking")
                            //#if MC >= 1.21.11
                            .getMethod("canSend", net.minecraft.resources.Identifier.class);
                            //#else
                            //$$ .getMethod("canSend", net.minecraft.resources.ResourceLocation.class);
                            //#endif
                }
                catch (ReflectiveOperationException | LinkageError exception)
                {
                    XaeroWorldBinding.LOGGER.warn("Auto Eat: incompatible QuickShulker open API", exception);
                    sendOpen = null;
                }
            }
        }
        if (sendOpen == null || Minecraft.getInstance().getConnection() == null) return false;
        try { return Boolean.TRUE.equals(canSend.invoke(null, channel)); }
        catch (ReflectiveOperationException exception)
        {
            XaeroWorldBinding.LOGGER.warn("Auto Eat: QuickShulker capability check failed", exception);
            return false;
        }
    }

    public static boolean open(Minecraft client, int inventoryIndex)
    {
        if (!isAvailable() || client.player == null || client.player.containerMenu != client.player.inventoryMenu) return false;
        for (var slot : client.player.inventoryMenu.slots)
        {
            if (slot.container == client.player.getInventory() && slot.getContainerSlot() == inventoryIndex)
            {
                try { sendOpen.invoke(null, slot.index); return true; }
                catch (IllegalAccessException | InvocationTargetException exception)
                {
                    XaeroWorldBinding.LOGGER.warn("Auto Eat: QuickShulker open request failed", exception);
                    return false;
                }
            }
        }
        return false;
    }
}
//#endif

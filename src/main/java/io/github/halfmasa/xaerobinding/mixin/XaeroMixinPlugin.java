package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.ClassReader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class XaeroMixinPlugin implements IMixinConfigPlugin
{
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        if (mixinClassName.endsWith("PrinterZxyInventoryRefillMixin"))
        {
            return !hasPrinterCoordinator() && !hasPrinterInventoryCheck() && hasPrinterZxyInventoryCheck();
        }
        if (mixinClassName.endsWith("PrinterMaterialRequestMixin") || mixinClassName.endsWith("PrinterInventoryRefillMixin"))
        {
            boolean coordinator = hasPrinterCoordinator();
            return mixinClassName.endsWith("PrinterMaterialRequestMixin") ? coordinator : !coordinator && hasPrinterInventoryCheck();
        }
        if (mixinClassName.endsWith("LitematicaEasyPlaceRefillMixin") ||
                mixinClassName.endsWith("LitematicaLegacyEasyPlaceRefillMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("litematica");
        }
        if (mixinClassName.endsWith("MinimapWorldStateUpdaterMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("xaerominimap");
        }
        if (mixinClassName.endsWith("WorldMapProcessorMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("xaeroworldmap");
        }
        if (mixinClassName.endsWith("SodiumFluidRendererMixin") ||
                mixinClassName.endsWith("SodiumLevelSliceFluidMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("sodium");
        }
        if (mixinClassName.endsWith("KeepModMenuScrollMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("modmenu");
        }
        if (mixinClassName.endsWith("TweakerooConfigListMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("tweakeroo");
        }
        if (mixinClassName.endsWith("ConfluxMapWaypointListMixin") ||
                mixinClassName.endsWith("ConfluxMapDualWaypointListMixin") ||
                mixinClassName.endsWith("ConfluxMapTeleportScreenMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("confluxmap");
        }
        if (mixinClassName.contains("ReiItemHistory"))
        {
            return FabricLoader.getInstance().isModLoaded("roughlyenoughitems");
        }
        if (mixinClassName.contains("JeiItemHistory") || mixinClassName.endsWith("JeiItemGiveHistoryMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("jei");
        }
        return true;
    }

    public static boolean supportsPrinterRefill()
    {
        return hasPrinterCoordinator() || hasPrinterInventoryCheck() || hasPrinterZxyInventoryCheck();
    }

    private static boolean hasPrinterCoordinator()
    {
        return hasPrinterMethod("me/aleksilassila/litematica/printer/integration/inventory/MaterialRequestCoordinator",
                "request", "(Ljava/util/List;L" + itemClassName() +
                ";Lme/aleksilassila/litematica/printer/integration/inventory/MaterialRequest$Source;)Lme/aleksilassila/litematica/printer/integration/inventory/MaterialReservation;");
    }

    private static boolean hasPrinterInventoryCheck()
    {
        return hasPrinterMethod("me/aleksilassila/litematica/printer/utils/InventoryUtils", "playerHasAccessToItems",
                "(L" + localPlayerClassName() + ";[L" + itemClassName() + ";)Z");
    }

    private static String itemClassName()
    {
        //#if MC >= 26.0
        return "net/minecraft/world/item/Item";
        //#else
        //$$ return FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_1792").replace('.', '/');
        //#endif
    }

    private static boolean hasPrinterZxyInventoryCheck()
    {
        return hasPrinterMethod("me/aleksilassila/litematica/printer/printer/zxy/inventory/InventoryUtils", "switchToItems",
                "(L" + localPlayerClassName() + ";[L" + itemClassName() + ";)Z");
    }

    private static String localPlayerClassName()
    {
        //#if MC >= 26.0
        return "net/minecraft/client/player/LocalPlayer";
        //#else
        //$$ return FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_746").replace('.', '/');
        //#endif
    }

    private static boolean hasPrinterMethod(String name, String method, String descriptor)
    {
        try (var stream = XaeroMixinPlugin.class.getClassLoader().getResourceAsStream(name + ".class"))
        {
            if (stream == null) return false;
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node.methods.stream().anyMatch(entry -> entry.name.equals(method) && entry.desc.equals(descriptor));
        }
        catch (java.io.IOException | RuntimeException e) { return false; }
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}

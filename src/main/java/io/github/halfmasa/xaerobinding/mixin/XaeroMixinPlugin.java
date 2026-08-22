package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class XaeroMixinPlugin implements IMixinConfigPlugin
{
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
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

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}

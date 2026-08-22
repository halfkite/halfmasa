package io.github.halfmasa.xaerobinding.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;

import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;

import io.github.halfmasa.xaerobinding.config.Configs;

/** Independently rebuilt early-loading controller */
public final class FastLoadingController implements IClientTickHandler
{
    private static final FastLoadingController INSTANCE = new FastLoadingController();
    private FastLoadingController() {}
    public static FastLoadingController getInstance() { return INSTANCE; }

    @Override
    public void onClientTick(Minecraft client)
    {
        if (Configs.FAST_WORLD_LOADING_SCREEN.getBooleanValue() &&
            MinecraftClientCompat.getScreen(client) instanceof LevelLoadingScreen && client.level != null && client.player != null)
        {
            MinecraftClientCompat.setScreen(client, null);
        }
    }
}

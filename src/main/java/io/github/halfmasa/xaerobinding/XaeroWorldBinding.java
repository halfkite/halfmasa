package io.github.halfmasa.xaerobinding;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

import fi.dy.masa.malilib.event.InitializationHandler;

import io.github.halfmasa.xaerobinding.compat.TweakerooFreeCameraCompat;
import io.github.halfmasa.xaerobinding.feature.ScreenshotClipboard;

public final class XaeroWorldBinding implements ModInitializer
{
    public static final String MOD_ID = "halfmasa";
    public static final String MOD_NAME = "halfmasa";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize()
    {
        ScreenshotClipboard.initialize();
        TweakerooFreeCameraCompat.initialize();
        InitializationHandler.getInstance().registerInitializationHandler(new XaeroWorldBindingInit());
    }
}

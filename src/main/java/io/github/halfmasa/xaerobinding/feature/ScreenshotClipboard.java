package io.github.halfmasa.xaerobinding.feature;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

public final class ScreenshotClipboard
{
    private static final ThreadLocal<Boolean> COPY_REQUESTED = ThreadLocal.withInitial(() -> false);

    private ScreenshotClipboard()
    {
    }

    public static void initialize()
    {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win"))
        {
            System.setProperty("java.awt.headless", "false");
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            XaeroWorldBinding.LOGGER.info("Screenshot clipboard initialized with {}", toolkit.getClass().getSimpleName());
        }
    }

    public static void requestCopy()
    {
        COPY_REQUESTED.set(true);
    }

    public static Consumer<NativeImage> wrapIfRequested(Consumer<NativeImage> original)
    {
        boolean requested = COPY_REQUESTED.get();
        COPY_REQUESTED.remove();
        if (!requested)
        {
            return original;
        }

        return wrap(original);
    }

    public static Consumer<NativeImage> wrap(Consumer<NativeImage> original)
    {
        if (!Configs.SCREENSHOT_TO_CLIPBOARD.getBooleanValue())
        {
            return original;
        }

        return image -> {
            copy(image);
            original.accept(image);
        };
    }

    public static void copyIfEnabled(NativeImage image)
    {
        if (Configs.SCREENSHOT_TO_CLIPBOARD.getBooleanValue())
        {
            copy(image);
        }
    }

    private static void copy(NativeImage nativeImage)
    {
        int width = nativeImage.getWidth();
        int height = nativeImage.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        //#if MC >= 1.21.4
        image.setRGB(0, 0, width, height, nativeImage.getPixels(), 0, width);
        //#else
        //$$ image.setRGB(0, 0, width, height, nativeImage.makePixelArray(), 0, width);
        //#endif

        EventQueue.invokeLater(() -> {
            try
            {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(image), null);
                showMessage("halfmasa.message.screenshot_copied", ChatFormatting.GREEN);
            }
            catch (RuntimeException exception)
            {
                XaeroWorldBinding.LOGGER.error("Failed to copy screenshot to clipboard", exception);
                showMessage("halfmasa.message.screenshot_copy_failed", ChatFormatting.RED);
            }
        });
    }

    private static void showMessage(String translationKey, ChatFormatting color)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null)
            {
                //#if MC >= 26.0
                minecraft.player.sendSystemMessage(Component.translatable(translationKey).withStyle(color));
                //#else
                //$$ minecraft.player.displayClientMessage(Component.translatable(translationKey).withStyle(color), false);
                //#endif
            }
        });
    }

    private record ImageSelection(Image image) implements Transferable
    {
        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[] {DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
        {
            if (!this.isDataFlavorSupported(flavor))
            {
                throw new UnsupportedFlavorException(flavor);
            }
            return this.image;
        }
    }
}

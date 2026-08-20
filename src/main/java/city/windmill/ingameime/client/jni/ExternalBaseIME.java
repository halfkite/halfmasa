package city.windmill.ingameime.client.jni;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.ImeOverlay;
import io.github.halfmasa.xaerobinding.feature.ImeService;

public final class ExternalBaseIME
{
    private static final ExternalBaseIME INSTANCE = new ExternalBaseIME();
    private boolean initialized;
    private boolean initializationAttempted;

    private ExternalBaseIME() {}

    public static ExternalBaseIME getInstance()
    {
        return INSTANCE;
    }

    public synchronized boolean initialize()
    {
        if (this.initialized || this.initializationAttempted)
        {
            return this.initialized;
        }
        this.initializationAttempted = true;

        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!operatingSystem.contains("windows"))
        {
            XaeroWorldBinding.LOGGER.warn("ContingameIME is only available on Windows");
            return false;
        }

        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        String libraryName = architecture.contains("64") ? "jni.dll" : "jni-x86.dll";
        String resource = "/assets/halfmasa/natives/ime/" + libraryName;
        try (InputStream input = ExternalBaseIME.class.getResourceAsStream(resource))
        {
            if (input == null)
            {
                throw new IllegalStateException("Missing native resource " + resource);
            }
            byte[] bytes = input.readAllBytes();
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            Path directory = Configs.getHalfMasaDirectory()
                    .resolve("cache")
                    .resolve("ime")
                    .resolve(digest.substring(0, 16));
            Files.createDirectories(directory);
            Path library = directory.resolve(libraryName);
            if (!Files.isRegularFile(library) || Files.size(library) != bytes.length)
            {
                Path temporary = directory.resolve(libraryName + ".tmp");
                Files.write(temporary, bytes);
                try
                {
                    Files.move(temporary, library,
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
                catch (Exception ignored)
                {
                    Files.move(temporary, library, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            System.load(library.toAbsolutePath().toString());
            Minecraft client = Minecraft.getInstance();
            //#if MC >= 1.21.10
            long window = client.getWindow().handle();
            //#else
            //$$ long window = client.getWindow().getWindow();
            //#endif
            this.nInitialize(GLFWNativeWin32.glfwGetWin32Window(window));
            this.initialized = true;
            this.setFullScreen(client.getWindow().isFullscreen());
            XaeroWorldBinding.LOGGER.info("ContingameIME native bridge initialized");
        }
        catch (Throwable throwable)
        {
            XaeroWorldBinding.LOGGER.error("Failed to initialize ContingameIME", throwable);
        }
        return this.initialized;
    }

    public boolean isInitialized()
    {
        return this.initialized;
    }

    public void setState(boolean state)
    {
        if (this.initialized)
        {
            this.nSetState(state);
        }
    }

    public void setFullScreen(boolean fullscreen)
    {
        if (this.initialized)
        {
            this.nSetFullScreen(fullscreen);
        }
    }

    private native void nInitialize(long handle);
    @SuppressWarnings("unused") private native void nUninitialize();
    private native void nSetState(boolean state);
    private native void nSetFullScreen(boolean fullscreen);

    @SuppressWarnings("unused")
    private void onCandidateList(String[] candidates)
    {
        Minecraft.getInstance().execute(() -> ImeOverlay.setCandidates(candidates));
    }

    @SuppressWarnings("unused")
    private void onComposition(String value, int caret, CompositionState state)
    {
        if (state == CompositionState.Commit)
        {
            ImeService.getInstance().commit(value);
        }
        else
        {
            Minecraft.getInstance().execute(() -> ImeOverlay.setComposition(value, caret));
        }
    }

    @SuppressWarnings("unused")
    private int[] onGetCompExt()
    {
        return ImeOverlay.compositionExtent();
    }

    @SuppressWarnings("unused")
    private void onAlphaMode(boolean alphaMode)
    {
        Minecraft.getInstance().execute(() -> ImeOverlay.setAlphaMode(alphaMode));
    }

    private enum CompositionState
    {
        Start,
        Update,
        End,
        Commit
    }
}

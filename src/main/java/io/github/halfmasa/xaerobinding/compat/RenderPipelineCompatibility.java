package io.github.halfmasa.xaerobinding.compat;

//#if MC >= 1.21.1
import net.fabricmc.loader.api.FabricLoader;
import java.util.Set;

public final class RenderPipelineCompatibility
{
    private RenderPipelineCompatibility() {}

    // Releases sharing the block meshing, occlusion and fluid rendering injection points.
    //#if MC >= 26.3
    private static final Set<String> SODIUM_CHUNK_VERSIONS = Set.of(
            "0.9.2", "0.9.3-beta.5", "0.9.3", "0.9.4-beta.1", "0.9.4-beta.2");
    //#elseif MC >= 26.1
    //$$ private static final Set<String> SODIUM_CHUNK_VERSIONS = Set.of("0.9.2");
    //#elseif MC >= 1.21.11
    //$$ private static final Set<String> SODIUM_CHUNK_VERSIONS = Set.of("0.8.14");
    //#elseif MC >= 1.21.8
    //$$ private static final Set<String> SODIUM_CHUNK_VERSIONS = Set.of("0.7.3");
    //#else
    //$$ private static final Set<String> SODIUM_CHUNK_VERSIONS = Set.of("0.6.13");
    //#endif

    public static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");
    public static final boolean SUPPORTED_SODIUM_CHUNKS = FabricLoader.getInstance().getModContainer("sodium")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString().split("\\+", 2)[0])
            .filter(SODIUM_CHUNK_VERSIONS::contains)
            .isPresent();
    public static final boolean SUPPORTED_TWEAKEROO = FabricLoader.getInstance().getModContainer("tweakeroo")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            //#if MC >= 26.3
            .filter("0.30.1"::equals)
            //#elseif MC >= 26.2
            //$$ .filter("0.29.4"::equals)
            //#elseif MC >= 26.1
            //$$ .filter("0.28.7"::equals)
            //#elseif MC >= 1.21.11
            //$$ .filter(Set.of("0.27.8", "0.27.12")::contains)
            //#elseif MC >= 1.21.10
            //$$ .filter("0.26.2"::equals)
            //#elseif MC >= 1.21.8
            //$$ .filter("0.25.6"::equals)
            //#elseif MC >= 1.21.5
            //$$ .filter("0.24.3"::equals)
            //#elseif MC >= 1.21.4
            //$$ .filter("0.23.4"::equals)
            //#elseif MC >= 1.21.3
            //$$ .filter("0.22.8"::equals)
            //#else
            //$$ .filter("0.21.59"::equals)
            //#endif
            .isPresent();

    public static boolean supportsChunkRendering()
    {
        return !SODIUM_LOADED || SUPPORTED_SODIUM_CHUNKS;
    }
}
//#endif

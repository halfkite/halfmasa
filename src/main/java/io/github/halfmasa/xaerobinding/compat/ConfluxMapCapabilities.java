package io.github.halfmasa.xaerobinding.compat;

//#if MC >= 1.21.1
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;

/** Inspect optional contracts without loading a class before Mixin transforms it. */
public final class ConfluxMapCapabilities
{
    private static final String ROOT = "cn/net/rms/confluxmap/";
    private static final Map<String, ClassNode> CLASSES = new HashMap<>();
    private ConfluxMapCapabilities() {}

    public static boolean event(String name)
    {
        ClassNode node = read("api/ConfluxMapEvents");
        return node != null && node.methods.stream().anyMatch(m -> m.name.equals(name)
                && m.desc.equals("()L" + ROOT + "api/Event;"));
    }

    public static boolean legacyTeleport()
    {
        ClassNode service = read("mc/teleport/ClientGroundTeleportService");
        String lookup = "L" + ROOT + "core/store/ColumnStore$SurfaceLookup;";
        // 0.1.10 introduced the captured + predicted lookup pair. Do not guess older ABIs.
        return service != null && service.methods.stream().anyMatch(m -> m.name.equals("teleport")
                && m.desc.startsWith("(II" + lookup + lookup))
                && has("mc/ui/screen/FullscreenMapScreen", "capturedGroundAt", "viewingLiveSession", "runLocationAction")
                && has("mc/ui/screen/FullscreenMapLocationMenu", "teleportPositionKnown");
    }

    public static boolean combinedList()
    {
        return has("mc/ui/screen/WaypointListScreen", "rebuild", "buildRows", "matchesSearch",
                "addRowWidgets", "teleport", "teleportAccess", "openEdit", "openSharedEdit", "formatDistance");
    }

    private static boolean has(String type, String... names)
    {
        ClassNode node = read(type);
        if (node == null) return false;
        for (String name : names)
            if (node.methods.stream().noneMatch(m -> m.name.equals(name))) return false;
        return true;
    }

    private static synchronized ClassNode read(String name)
    {
        if (CLASSES.containsKey(name)) return CLASSES.get(name);
        ClassNode node = null;
        try (var stream = ConfluxMapCapabilities.class.getClassLoader().getResourceAsStream(ROOT + name + ".class"))
        {
            if (stream != null)
            {
                node = new ClassNode();
                new ClassReader(stream).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        catch (IOException | RuntimeException exception)
        {
            LoggerFactory.getLogger("HalfMasa/ConfluxMap").warn("Cannot inspect optional Conflux Map contract: {}", name, exception);
        }
        CLASSES.put(name, node);
        return node;
    }
}
//#endif

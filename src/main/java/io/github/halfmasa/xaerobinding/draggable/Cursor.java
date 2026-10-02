package io.github.halfmasa.xaerobinding.draggable;

import net.minecraft.client.Minecraft;
//#if MC < 26.3
//$$ import org.lwjgl.glfw.GLFW;
//#else
import com.mojang.blaze3d.platform.cursor.CursorType;
import org.lwjgl.sdl.SDLMouse;
//#endif

public class Cursor {
    private static boolean isDragging;

    public static void setDragging() {
        isDragging = true;
        //#if MC < 26.3
        //#if MC >= 1.21.10
        //$$ GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().handle(),
                //$$ GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR));
        //#else
        //$$ GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR));
        //#endif
        //#else
        CursorType.createStandardCursor(
                SDLMouse.SDL_SYSTEM_CURSOR_NS_RESIZE, "halfmasa_resize", CursorType.DEFAULT).select();
        //#endif
    }

    public static void reset() {
        if (!isDragging) return;
        isDragging = false;
        //#if MC < 26.3
        //#if MC >= 1.21.10
        //$$ GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().handle(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        //#else
        //$$ GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        //#endif
        //#else
        CursorType.DEFAULT.select();
        //#endif
    }
}

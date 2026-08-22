package io.github.halfmasa.xaerobinding.draggable;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class Cursor {
    private static boolean isDragging;

    public static void setDragging() {
        isDragging = true;
        //#if MC >= 1.21.10
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().handle(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR));
        //#else
        //$$ GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR));
        //#endif
    }

    public static void reset() {
        if (!isDragging) return;
        isDragging = false;
        //#if MC >= 1.21.10
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().handle(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        //#else
        //$$ GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        //#endif
    }
}

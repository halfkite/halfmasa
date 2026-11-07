package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

import org.lwjgl.glfw.GLFW;
/** Input adapter compiled only for legacy GLFW targets. */
public record ServerKey(int input,int modifiers) {
    public boolean hasShiftDown(){return (modifiers & GLFW.GLFW_MOD_SHIFT)!=0;}
    public boolean isConfirmation(){return input==GLFW.GLFW_KEY_ENTER || input==GLFW.GLFW_KEY_KP_ENTER || input==GLFW.GLFW_KEY_SPACE;}
}

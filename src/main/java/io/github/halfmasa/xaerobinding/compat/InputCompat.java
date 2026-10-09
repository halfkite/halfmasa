package io.github.halfmasa.xaerobinding.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

//#if MC < 26.3
//$$ import org.lwjgl.glfw.GLFW;
//#else
import java.nio.FloatBuffer;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.system.MemoryStack;
//#endif

public final class InputCompat
{
    private InputCompat() {}

    public static InputConstants.Key keyboardKey(int code)
    {
        //#if MC < 26.3
        //$$ return InputConstants.Type.KEYSYM.getOrCreate(code);
        //#else
        return InputConstants.Type.KEYBOARD.getOrCreate(code);
        //#endif
    }

    public static boolean isKeyboardKey(InputConstants.Key key)
    {
        //#if MC < 26.3
        //$$ return key.getType() == InputConstants.Type.KEYSYM;
        //#else
        return key.getType() == InputConstants.Type.KEYBOARD;
        //#endif
    }

    public static boolean isKeyDown(Minecraft client, int code)
    {
        //#if MC < 26.3
        //#if MC >= 1.21.10
        //$$ return InputConstants.isKeyDown(client.getWindow(), code);
        //#else
        //$$ return InputConstants.isKeyDown(client.getWindow().getWindow(), code);
        //#endif
        //#else
        return InputConstants.isKeyDown(code);
        //#endif
    }

    public static boolean isMouseButtonDown(Minecraft client, int button)
    {
        //#if MC < 26.3
        //#if MC >= 1.21.10
        //$$ return GLFW.glfwGetMouseButton(client.getWindow().handle(), button) == GLFW.GLFW_PRESS;
        //#else
        //$$ return GLFW.glfwGetMouseButton(client.getWindow().getWindow(), button) == GLFW.GLFW_PRESS;
        //#endif
        //#else
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            FloatBuffer x = stack.mallocFloat(1);
            FloatBuffer y = stack.mallocFloat(1);
            int state = SDLMouse.SDL_GetMouseState(x, y);
            return button > 0 && (state & (1 << (button - 1))) != 0;
        }
        //#endif
    }

    public static boolean isPrimaryMouseButton(int button)
    {
        return button == InputConstants.MOUSE_BUTTON_LEFT;
    }

    /** Stable negative codes for the on-screen layout: -1 left, -2 right, -3 middle. */
    public static int mouseButtonToLayoutCode(int button)
    {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) return -1;
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) return -2;
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE) return -3;
        //#if MC < 26.3
        //$$ return -(button + 1);
        //#else
        return -button;
        //#endif
    }

    public static int layoutCodeToMouseButton(int code)
    {
        if (code == -1) return InputConstants.MOUSE_BUTTON_LEFT;
        if (code == -2) return InputConstants.MOUSE_BUTTON_RIGHT;
        if (code == -3) return InputConstants.MOUSE_BUTTON_MIDDLE;
        //#if MC < 26.3
        //$$ return -code - 1;
        //#else
        return -code;
        //#endif
    }

    public static int escapeKeyCode()
    {
        //#if MC < 26.3
        //$$ return 256;
        //#else
        return 41;
        //#endif
    }

    public static int backspaceKeyCode()
    {
        //#if MC < 26.3
        //$$ return 259;
        //#else
        return 42;
        //#endif
    }

    /** Converts the legacy GLFW values used by the on-screen keyboard layout. */
    public static int layoutKeyCode(int legacyCode)
    {
        //#if MC < 26.3
        //$$ return legacyCode;
        //#else
        return switch (legacyCode)
        {
            case 256 -> 41;
            case 290 -> 58;
            case 291 -> 59;
            case 292 -> 60;
            case 293 -> 61;
            case 294 -> 62;
            case 295 -> 63;
            case 296 -> 64;
            case 297 -> 65;
            case 298 -> 66;
            case 299 -> 67;
            case 300 -> 68;
            case 301 -> 69;
            case 302 -> 104;
            case 303 -> 105;
            case 304 -> 106;
            case 305 -> 107;
            case 306 -> 108;
            case 307 -> 109;
            case 308 -> 110;
            case 309 -> 111;
            case 310 -> 112;
            case 311 -> 113;
            case 312 -> 114;
            case 313 -> 115;
            case 96 -> 53;
            case 48 -> 39;
            case 49 -> 30;
            case 50 -> 31;
            case 51 -> 32;
            case 52 -> 33;
            case 53 -> 34;
            case 54 -> 35;
            case 55 -> 36;
            case 56 -> 37;
            case 57 -> 38;
            case 45 -> 45;
            case 61 -> 46;
            case 259 -> 42;
            case 258 -> 43;
            case 81 -> 20;
            case 87 -> 26;
            case 69 -> 8;
            case 82 -> 21;
            case 84 -> 23;
            case 89 -> 28;
            case 85 -> 24;
            case 73 -> 12;
            case 79 -> 18;
            case 80 -> 19;
            case 91 -> 47;
            case 93 -> 48;
            case 92 -> 49;
            case 280 -> 57;
            case 65 -> 4;
            case 83 -> 22;
            case 68 -> 7;
            case 70 -> 9;
            case 71 -> 10;
            case 72 -> 11;
            case 74 -> 13;
            case 75 -> 14;
            case 76 -> 15;
            case 59 -> 51;
            case 39 -> 52;
            case 257 -> 40;
            case 340 -> 225;
            case 344 -> 229;
            case 90 -> 29;
            case 88 -> 27;
            case 67 -> 6;
            case 86 -> 25;
            case 66 -> 5;
            case 78 -> 17;
            case 77 -> 16;
            case 44 -> 54;
            case 46 -> 55;
            case 47 -> 56;
            case 341 -> 224;
            case 345 -> 228;
            case 343 -> 227;
            case 347 -> 231;
            case 342 -> 226;
            case 346 -> 230;
            case 32 -> 44;
            case 348 -> 118;
            case 283 -> 70;
            case 281 -> 71;
            case 284 -> 72;
            case 260 -> 73;
            case 268 -> 74;
            case 266 -> 75;
            case 261 -> 76;
            case 269 -> 77;
            case 267 -> 78;
            case 264 -> 82;
            case 263 -> 80;
            case 265 -> 81;
            case 262 -> 79;
            case 282 -> 83;
            case 331 -> 84;
            case 332 -> 85;
            case 333 -> 86;
            case 327 -> 95;
            case 328 -> 96;
            case 329 -> 97;
            case 334 -> 87;
            case 324 -> 92;
            case 325 -> 93;
            case 326 -> 94;
            case 321 -> 89;
            case 322 -> 90;
            case 323 -> 91;
            case 335 -> 88;
            case 320 -> 98;
            case 330 -> 99;
            default -> legacyCode;
        };
        //#endif
    }

    public static boolean isFullscreen(Minecraft client)
    {
        //#if MC < 26.3
        //$$ return client.getWindow().isFullscreen();
        //#else
        return client.getWindow().isExclusiveFullscreen();
        //#endif
    }
}

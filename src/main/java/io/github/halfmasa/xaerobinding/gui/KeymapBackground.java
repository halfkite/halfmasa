package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
/** Converts the user-facing transparency percentage to a black screen overlay. */
public final class KeymapBackground
{
    private KeymapBackground() {}

    public static int color(int transparency)
    {
        int percentage = Math.max(0, Math.min(100, transparency));
        int alpha = Math.round(255 * (100 - percentage) / 100.0f);
        return alpha << 24;
    }
}
//#endif

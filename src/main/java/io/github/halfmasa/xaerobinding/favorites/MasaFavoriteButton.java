//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import fi.dy.masa.malilib.util.StringUtils;
import io.github.halfmasa.xaerobinding.config.Configs;
//#if MC >= 26.3
import fi.dy.masa.malilib.util.input.ScanCodes;
//#else
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif

/** Reads membership on render so duplicate presentations of the same config stay in sync. */
public final class MasaFavoriteButton extends ButtonGeneric
{
    //#if MC >= 26.3
    public static final int LEFT_MOUSE = ScanCodes.OFFSET_MOUSE_LEFT;
    //#else
    //$$ public static final int LEFT_MOUSE = InputConstants.MOUSE_BUTTON_LEFT;
    //#endif
    public static int size() { return Configs.MASA_FAVORITES_STAR_SIZE.getIntegerValue(); }
    public static int space() { return size() + 2; }
    private final MasaConfigCatalog.Entry entry;
    private boolean saveFailed;

    public MasaFavoriteButton(int x, int y, MasaConfigCatalog.Entry entry, Runnable onChanged)
    {
        super(x, y, size(), size(), "");
        this.setRenderDefaultBackground(false);
        this.entry = entry;
        this.updateLabel();
        this.setActionListener((button, mouse) -> {
            if (mouse != LEFT_MOUSE) return;
            this.saveFailed = !MasaFavoritesStore.get().setFavorite(entry.key(), !MasaFavoritesStore.get().contains(entry.key()));
            this.updateLabel();
            if (!this.saveFailed) onChanged.run();
        });
    }

    public static boolean isShown(fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui host)
    {
        return io.github.halfmasa.xaerobinding.config.Configs.MASA_FAVORITES.getBooleanValue() &&
                (host instanceof MasaFavoritesScreen ||
                        io.github.halfmasa.xaerobinding.config.Configs.MASA_FAVORITES_SHOW_STARS.getBooleanValue());
    }

    private boolean updateLabel()
    {
        boolean favorite = MasaFavoritesStore.get().contains(this.entry.key());
        this.setDisplayString(favorite ? "\u2605" : "\u2606");
        this.setHoverStrings(this.entry.modId() + ": " + this.entry.identity().getConfigGuiDisplayName(),
                StringUtils.translate(this.saveFailed ? "halfmasa.gui.favorites.save_failed" : favorite
                        ? "halfmasa.gui.favorites.remove" : "halfmasa.gui.favorites.add"));
        return favorite;
    }

    @Override
    //#if MC >= 1.21.11
    public void render(GuiContext context, int mouseX, int mouseY, boolean selected)
    //#elseif MC >= 1.21.8
    //$$ public void render(GuiGraphics context, int mouseX, int mouseY, boolean selected)
    //#else
    //$$ public void render(int mouseX,int mouseY,boolean selected,GuiGraphics context)
    //#endif
    {
        boolean favorite = this.updateLabel();
        int color = (favorite ? Configs.MASA_FAVORITES_STAR_COLOR : Configs.MASA_FAVORITES_EMPTY_STAR_COLOR).getIntegerValue();
        String glyph = favorite ? "★" : "☆";
        float scale = this.getWidth() / 20.0F;
        float x = this.getX() + (this.getWidth() - this.getStringWidth(glyph) * scale) / 2;
        float y = this.getY() + (this.getHeight() - 8 * scale) / 2;
        var pose = context.pose();
        //#if MC >= 1.21.8
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        //#else
        //$$ pose.pushPose();
        //$$ pose.translate(x, y, 0);
        //$$ pose.scale(scale, scale, 1);
        //#endif
        try
        {
            //#if MC >= 1.21.11
            this.drawStringWithShadow(context, 0, 0, color, glyph);
            //#else
            //$$ context.drawString(net.minecraft.client.Minecraft.getInstance().font, glyph, 0, 0, color, true);
            //#endif
        }
        finally
        {
            //#if MC >= 1.21.8
            pose.popMatrix();
            //#else
            //$$ pose.popPose();
            //#endif
        }
    }
}
//#endif

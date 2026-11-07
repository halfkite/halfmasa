//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import fi.dy.masa.malilib.gui.button.ButtonGeneric;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import fi.dy.masa.malilib.util.StringUtils;
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
    public static final int WIDTH = 20;
    public static final int SPACE = WIDTH + 2;
    private final MasaConfigCatalog.Entry entry;
    private boolean saveFailed;

    public MasaFavoriteButton(int x, int y, MasaConfigCatalog.Entry entry, Runnable onChanged)
    {
        super(x, y, WIDTH, 20, "");
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

    private void updateLabel()
    {
        boolean favorite = MasaFavoritesStore.get().contains(this.entry.key());
        this.setDisplayString(favorite ? "\u2605" : "\u2606");
        this.setHoverStrings(this.entry.modId() + ": " + this.entry.identity().getConfigGuiDisplayName(),
                StringUtils.translate(this.saveFailed ? "halfmasa.gui.favorites.save_failed" : favorite
                        ? "halfmasa.gui.favorites.remove" : "halfmasa.gui.favorites.add"));
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
        this.updateLabel();
        //#if MC >= 1.21.8
        super.render(context, mouseX, mouseY, selected);
        //#else
        //$$ super.render(mouseX,mouseY,selected,context);
        //#endif
    }
}
//#endif

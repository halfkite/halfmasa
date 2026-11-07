//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.data.ModInfo;
//#if MC >= 26.3
import fi.dy.masa.malilib.util.input.ScanCodes;
//#else
//$$ import com.mojang.blaze3d.platform.InputConstants;
//#endif
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;

/** Keeps MaLiLib's dropdown/search/scroll behavior, with available-space sizing and pinned pages. */
public final class MasaConfigSwitcher extends WidgetDropDownList<ModInfo>
{
    private static final int STAR_SPACE = 16;
    private final List<ModInfo> nativeOrder;

    public MasaConfigSwitcher(int right, int y, int nativeWidth, int rowHeight, List<ModInfo> mods, ModInfo selected)
    {
        super(Math.max(6, right - requiredWidth(nativeWidth, mods)), y, requiredWidth(nativeWidth, mods), rowHeight,
                Math.max(rowHeight, GuiUtils.getScaledWindowHeight() - y - rowHeight - 6), Math.max(1, mods.size()), ordered(mods));
        this.nativeOrder = List.copyOf(mods);
        this.setSelectedEntry(selected);
    }

    public static boolean enabled() { return Configs.MASA_FAVORITES.getBooleanValue() && Configs.MASA_BETTER_SWITCHER.getBooleanValue(); }
    public static boolean canFavorite() { return enabled() && Configs.MASA_SWITCHER_FAVORITES.getBooleanValue(); }
    public static String favoriteKey(ModInfo mod) { return "screen:" + io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modId(mod); }

    //#if MC >= 1.21.11
    public void renderOverlay(fi.dy.masa.malilib.render.GuiContext context, int mouseX, int mouseY)
    //#else
    //$$ public void renderOverlay(net.minecraft.client.gui.GuiGraphics context, int mouseX, int mouseY)
    //#endif
    {
        //#if MC >= 1.21.8
        if (this.isOpen) this.render(context, mouseX, mouseY, this.isMouseOver(mouseX, mouseY));
        //#else
        //$$ if (this.isOpen) this.render(mouseX,mouseY,this.isMouseOver(mouseX,mouseY),context);
        //#endif
    }

    private static List<ModInfo> ordered(List<ModInfo> mods)
    {
        List<ModInfo> result = new ArrayList<>(mods);
        if (canFavorite()) result.sort(Comparator.comparing(mod -> !MasaFavoritesStore.get().contains(favoriteKey(mod))));
        return result;
    }

    private static int requiredWidth(int nativeWidth, List<ModInfo> mods)
    {
        int text = mods.stream().mapToInt(mod -> Minecraft.getInstance().font.width(io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modName(mod))).max().orElse(0);
        return Math.min(Math.max(nativeWidth, text + 24 + (canFavorite() ? STAR_SPACE : 0)),
                Math.max(30, GuiUtils.getScaledWindowWidth() - 12));
    }

    @Override
    protected String getDisplayString(ModInfo mod)
    {
        if (mod == null) return "-";
        return (canFavorite() ? (MasaFavoritesStore.get().contains(favoriteKey(mod)) ? "\u2605  " : "\u2606  ") : "") + io.github.halfmasa.xaerobinding.favorites.MasaGadgetCompat.modName(mod);
    }

    @Override
    //#if MC >= 1.21.10
    protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick)
    {
        double mouseX=click.x(), mouseY=click.y(); int button=click.input();
    //#else
    //$$ protected boolean onMouseClickedImpl(int mouseX,int mouseY,int button) {
    //#endif
        if (canFavorite() && this.isOpen && mouseY >= this.y + this.height + 1 &&
                mouseX >= this.x && mouseX < this.x + STAR_SPACE)
        {
            int row = ((int) mouseY - this.y - this.height - 1) / this.height;
            int index = this.scrollBar.getValue() + row;
            if (row >= 0 && row < this.maxVisibleEntries && index < this.filteredEntries.size() && button == MasaFavoriteButton.LEFT_MOUSE)
            {
                String key = favoriteKey(this.filteredEntries.get(index));
                if (MasaFavoritesStore.get().setFavorite(key, !MasaFavoritesStore.get().contains(key)))
                {
                    int scroll = this.scrollBar.getValue();
                    this.entries.clear();
                    this.entries.addAll(ordered(this.nativeOrder));
                    this.updateFilteredEntries();
                    this.scrollBar.setValue(scroll);
                }
            }
            return true;
        }
        //#if MC >= 1.21.10
        return super.onMouseClickedImpl(click, doubleClick);
        //#else
        //$$ return super.onMouseClickedImpl(mouseX,mouseY,button);
        //#endif
    }

    @Override
    protected void setSelectedEntry(int index)
    {
        super.setSelectedEntry(index);
        if (this.selectedEntry != null && MasaGadgetCompat.supplier(this.getSelectedEntry()) != null)
            MinecraftClientCompat.setScreen(Minecraft.getInstance(), MasaGadgetCompat.supplier(this.getSelectedEntry()).get());
    }
}
//#endif

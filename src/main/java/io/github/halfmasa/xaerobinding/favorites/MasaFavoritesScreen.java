//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.favorites;

import java.util.List;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.halfmasa.xaerobinding.feature.MaLiLibConfigScrollAccess;
import io.github.halfmasa.xaerobinding.gui.ActionConfigListWidget;
import io.github.halfmasa.xaerobinding.gui.ScrollCategoryKeyProvider;

public final class MasaFavoritesScreen extends GuiConfigsBase implements ScrollCategoryKeyProvider
{
    public static final String SCREEN_ID = "halfmasa_favorites";
    private final MasaFavoritesSession session = new MasaFavoritesSession();
    private final MasaFavoritesExpansionProvider expansion = new MasaFavoritesExpansionProvider(this.session);

    public MasaFavoritesScreen()
    {
        super(10, 64, SCREEN_ID, null, "halfmasa.gui.favorites.title");
    }

    public MasaFavoritesSession session() { return this.session; }

    @Override
    public void initGui()
    {
        super.initGui();
        this.addLabel(10, 28, -1, 12, 0xFFCCCCCC,
                StringUtils.translate("halfmasa.gui.favorites.source"));
        this.addLabel(10, 40, -1, 12, 0xFFCCCCCC,
                StringUtils.translate(!io.github.halfmasa.xaerobinding.config.Configs.MASA_FAVORITES.getBooleanValue()
                        ? "halfmasa.gui.favorites.disabled" : io.github.halfmasa.xaerobinding.config.Configs.MASA_FAVORITES_GLOBAL_SEARCH.getBooleanValue()
                        ? "halfmasa.gui.favorites.global_hint" : this.session.snapshot().isEmpty()
                        ? "halfmasa.gui.favorites.empty" : "halfmasa.gui.favorites.hint"));
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        if (!io.github.halfmasa.xaerobinding.config.Configs.MASA_FAVORITES.getBooleanValue()) return List.of();
        return this.session.snapshot().stream().map(entry -> new ConfigOptionWrapper(entry.editable())).toList();
    }

    @Override
    protected WidgetListConfigOptions createListWidget(int x, int y)
    {
        return new ActionConfigListWidget(x, y, this.getBrowserWidth(), this.getBrowserHeight(),
                Math.min(204, Math.max(90, this.getBrowserWidth() / 3)), 0.0F, true, this,
                this.expansion);
    }

    @Override
    public void removed()
    {
        ((MaLiLibConfigScrollAccess) (Object) this).halfmasa$saveConfigScroll();
        if (this.getListWidget() != null)
        {
            this.getListWidget().applyPendingModifications();
            this.session.saveChanges();
            this.getListWidget().clearConfigsModifiedFlag();
        }
    }

    @Override
    protected void onSettingsChanged() { this.session.saveChanges(); }

    @Override
    protected int getBrowserHeight() { return this.getScreenHeight() - this.getListY() - 10; }

    @Override
    public String halfmasa$getScrollCategoryKey() { return "favorites"; }
}
//#endif

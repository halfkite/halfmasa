package io.github.halfmasa.xaerobinding.gui;

import java.util.List;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.MaLiLibConfigScrollAccess;

public final class HalfMasaConfigScreen extends GuiConfigsBase implements ScrollCategoryKeyProvider
{
    private static Tab tab = Tab.ALL;

    public HalfMasaConfigScreen()
    {
        super(10, 52, XaeroWorldBinding.MOD_ID, null, "halfmasa.gui.title");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        int x = 10;
        for (Tab candidate : Tab.values())
        {
            ButtonGeneric button = new ButtonGeneric(x, 26, -1, 20, candidate.getDisplayName());
            button.setEnabled(tab != candidate);
            this.addButton(button, new TabListener(candidate, this));
            x += button.getWidth() + 2;
        }

    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        List<? extends IConfigBase> configs = switch (tab)
        {
            case ALL -> Configs.getAllView();
            case RECOMMENDED -> Configs.getRecommendedView();
            case WAYPOINT_TOOLS -> Configs.getWaypointView();
            case CREATIVE_UTILITIES -> Configs.getCreativeView();
            case PORTED_FEATURES -> Configs.getPortedView();
            case EXTENSIONS -> Configs.getExtensionsView();
            case DISABLED -> Configs.getDisabledView();
        };
        return ConfigOptionWrapper.createFor(configs);
    }

    void refreshExpandedConfigs()
    {
        int scrollPosition = this.getListWidget() != null
                ? this.getListWidget().getScrollbar().getValue()
                : 0;
        MaLiLibConfigScrollAccess scrollAccess =
                (MaLiLibConfigScrollAccess) (Object) this;
        scrollAccess.halfmasa$saveConfigScroll();
        this.reCreateListWidget();
        this.initGui();
        if (this.getListWidget() != null)
        {
            this.getListWidget().getScrollbar().setValue(scrollPosition);
        }
    }

    @Override
    protected WidgetListConfigOptions createListWidget(int listX, int listY)
    {
        return new ActionConfigListWidget(
                listX,
                listY,
                this.getBrowserWidth(),
                this.getBrowserHeight(),
                this.getConfigWidth(),
                0.0F,
                this.useKeybindSearch(),
                this);
    }

    @Override
    protected boolean useKeybindSearch()
    {
        return true;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.getScreenHeight() - this.getListY() - 10;
    }

    @Override
    public String halfmasa$getScrollCategoryKey()
    {
        return tab.name();
    }

    private record TabListener(Tab selected, HalfMasaConfigScreen screen) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            tab = this.selected;
            this.screen.reCreateListWidget();
            if (this.screen.getListWidget() != null)
            {
                this.screen.getListWidget().resetScrollbarPosition();
            }
            this.screen.initGui();
        }
    }

    private enum Tab
    {
        ALL("halfmasa.gui.tab.all"),
        RECOMMENDED("halfmasa.gui.tab.recommended"),
        WAYPOINT_TOOLS("halfmasa.gui.tab.waypoint_tools"),
        CREATIVE_UTILITIES("halfmasa.gui.tab.creative_utilities"),
        PORTED_FEATURES("halfmasa.gui.tab.ported_features"),
        EXTENSIONS("halfmasa.gui.tab.extensions"),
        DISABLED("halfmasa.gui.tab.disabled");

        private final String translationKey;

        Tab(String translationKey)
        {
            this.translationKey = translationKey;
        }

        String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }

}

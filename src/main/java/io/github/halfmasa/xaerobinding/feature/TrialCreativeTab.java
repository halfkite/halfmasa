package io.github.halfmasa.xaerobinding.feature;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import io.github.halfmasa.xaerobinding.config.Configs;

/**
 * Holds a single, client-side only creative tab that is never registered into
 * {@code BuiltInRegistries.CREATIVE_MODE_TAB}. The vanilla tab list is extended at runtime by
 * {@code TrialCreativeTabMixin}, which appends this instance to the results of
 * {@code CreativeModeTabs.tabs()} and {@code allTabs()}.
 *
 * <p>The tab only carries a title and an icon; its contents are filled by the mixin when the
 * tab is selected, the same way {@link CondensedCreativeManager} rewrites the item list.</p>
 */
public final class TrialCreativeTab
{
    private static CreativeModeTab tab;

    private TrialCreativeTab() {}

    /** Returns the shared tab instance, creating it on first use. */
    public static CreativeModeTab get()
    {
        if (tab == null)
        {
            tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("halfmasa.creative_tab.trial"))
                    .icon(TrialCreativeTab::icon)
                    .build();
        }
        return tab;
    }

    public static boolean isEnabled()
    {
        return Configs.TRIAL_CREATIVE_TAB.getBooleanValue();
    }

    /** True when the given tab is our synthetic tab. */
    public static boolean isTrialTab(CreativeModeTab candidate)
    {
        return candidate != null && candidate == tab;
    }

    /** Fills the given item list with this tab's contents. */
    public static void fill(List<ItemStack> target)
    {
        target.clear();
        for (ItemStack stack : TrialCreativeItems.getItems())
        {
            target.add(stack.copy());
        }
    }

    private static ItemStack icon()
    {
        List<ItemStack> items = TrialCreativeItems.getItems();
        return items.isEmpty() ? ItemStack.EMPTY : items.getFirst().copy();
    }
}

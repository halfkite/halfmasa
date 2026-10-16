package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.Locale;
import io.github.halfmasa.xaerobinding.config.AutoEatFilterMode;
import io.github.halfmasa.xaerobinding.config.AutoEatFoodPriority;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.item.ItemStack;

public final class AutoEatFoodRules
{
    private AutoEatFoodRules() {}

    public static String normalizeId(String text)
    {
        if (text == null) return "";
        //#if MC >= 1.21.11
        Identifier id = Identifier.tryParse(text.strip().toLowerCase(Locale.ROOT));
        //#else
        //$$ ResourceLocation id = ResourceLocation.tryParse(text.strip().toLowerCase(Locale.ROOT));
        //#endif
        return id == null ? "" : id.toString();
    }

    public static boolean isAllowed(ItemStack stack)
    {
        if (stack == null || stack.isEmpty()) return false;
        var food = stack.get(DataComponents.FOOD);
        if (food == null || food.nutrition() <= 0) return false;
        //#if MC >= 1.21.3
        if (!stack.has(DataComponents.CONSUMABLE)) return false;
        //#endif
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        boolean whitelist = Configs.AUTO_EAT_FILTER_MODE.getOptionListValue() == AutoEatFilterMode.WHITELIST;
        var rules = whitelist ? Configs.AUTO_EAT_WHITELIST.getStrings() : Configs.AUTO_EAT_BLACKLIST.getStrings();
        boolean listed = rules.stream().map(AutoEatFoodRules::normalizeId).anyMatch(id::equals);
        return whitelist == listed;
    }

    public static ItemStack useRemainder(ItemStack food)
    {
        //#if MC >= 1.21.3
        var remainder = food.get(DataComponents.USE_REMAINDER);
        //#if MC >= 26.0
        return remainder == null ? ItemStack.EMPTY : remainder.convertInto().create();
        //#else
        //$$ return remainder == null ? ItemStack.EMPTY : remainder.convertInto().copy();
        //#endif
        //#else
        //$$ var properties = food.get(DataComponents.FOOD);
        //$$ if (properties != null && properties.usingConvertsTo().isPresent()) return properties.usingConvertsTo().get().copy();
        //$$ var item = food.getItem().getCraftingRemainingItem();
        //$$ return item == null ? ItemStack.EMPTY : new ItemStack(item);
        //#endif
    }

    /** Stable ordering: ties keep the earlier slot; no arbitrary score weights. */
    public static boolean isBetter(ItemStack candidate, ItemStack current, int missing)
    {
        if (current.isEmpty()) return true;
        var strategy = (AutoEatFoodPriority) Configs.AUTO_EAT_PRIORITY.getOptionListValue();
        if (strategy == AutoEatFoodPriority.LEFT_TO_RIGHT) return false;
        var next = candidate.get(DataComponents.FOOD);
        var previous = current.get(DataComponents.FOOD);
        int nutritionOrder;
        if (strategy == AutoEatFoodPriority.SMART)
        {
            boolean nextFits = next.nutrition() <= missing;
            boolean previousFits = previous.nutrition() <= missing;
            if (nextFits != previousFits) return nextFits;
            nutritionOrder = nextFits ? Integer.compare(next.nutrition(), previous.nutrition())
                    : Integer.compare(previous.nutrition(), next.nutrition());
        }
        else nutritionOrder = Integer.compare(next.nutrition(), previous.nutrition());
        return nutritionOrder > 0 || nutritionOrder == 0 && next.saturation() > previous.saturation();
    }
}
//#endif

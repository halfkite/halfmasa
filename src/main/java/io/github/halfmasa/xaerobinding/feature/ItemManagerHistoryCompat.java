package io.github.halfmasa.xaerobinding.feature;

import java.lang.reflect.Method;
import java.util.Collection;

import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.ItemSearchHistoryService.Channel;

/** Reflection-only capture bridge for optional REI and JEI classes */
public final class ItemManagerHistoryCompat
{
    private static boolean reiFailureLogged;
    private static boolean jeiFailureLogged;

    private ItemManagerHistoryCompat() {}

    public static void recordReiEntry(Object entry)
    {
        if (!enabled()) return;
        try
        {
            ItemStack stack = reiItemStack(entry);
            if (!stack.isEmpty())
            {
                ItemSearchHistoryService.getInstance().record(Channel.REI, stack);
            }
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.REI, throwable);
        }
    }

    public static void recordReiView(Object builder)
    {
        if (!enabled()) return;
        try
        {
            recordReiCollection(invoke(builder, "getRecipesFor"));
            recordReiCollection(invoke(builder, "getUsagesFor"));
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.REI, throwable);
        }
    }

    public static void recordJeiBookmark(Object bookmark)
    {
        if (!enabled()) return;
        try
        {
            Object element = invoke(bookmark, "getElement");
            Object typedIngredient = invoke(element, "getTypedIngredient");
            Object ingredient = invoke(typedIngredient, "getIngredient");
            if (ingredient instanceof ItemStack stack && !stack.isEmpty())
            {
                ItemSearchHistoryService.getInstance().record(Channel.JEI, stack);
            }
        }
        catch (Throwable throwable)
        {
            logFailure(Channel.JEI, throwable);
        }
    }

    public static void recordJeiGive(ItemStack stack)
    {
        if (enabled() && Minecraft.getInstance().player != null && stack != null && !stack.isEmpty())
        {
            ItemSearchHistoryService.getInstance().record(Channel.JEI, stack);
        }
    }

    private static void recordReiCollection(Object value) throws Exception
    {
        if (!(value instanceof Collection<?> entries))
        {
            return;
        }
        for (Object entry : entries)
        {
            ItemStack stack = reiItemStack(entry);
            if (!stack.isEmpty())
            {
                ItemSearchHistoryService.getInstance().record(Channel.REI, stack);
            }
        }
    }

    private static ItemStack reiItemStack(Object entry) throws Exception
    {
        if (entry == null)
        {
            return ItemStack.EMPTY;
        }
        Object value = invoke(entry, "getValue");
        if (value instanceof ItemStack stack)
        {
            return stack.copyWithCount(1);
        }
        Object converted = invoke(entry, "cheatsAs");
        Object convertedValue = invoke(converted, "getValue");
        return convertedValue instanceof ItemStack stack ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }

    private static Object invoke(Object target, String name) throws Exception
    {
        if (target == null)
        {
            throw new IllegalArgumentException("Missing optional API target for " + name);
        }
        for (Method method : target.getClass().getMethods())
        {
            if (method.getName().equals(name) && method.getParameterCount() == 0)
            {
                return method.invoke(target);
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name);
    }

    private static boolean enabled()
    {
        return Configs.ITEM_MANAGER_RECIPE_HISTORY.getBooleanValue();
    }

    private static void logFailure(Channel channel, Throwable throwable)
    {
        if (channel == Channel.REI)
        {
            if (reiFailureLogged) return;
            reiFailureLogged = true;
        }
        else
        {
            if (jeiFailureLogged) return;
            jeiFailureLogged = true;
        }
        XaeroWorldBinding.LOGGER.warn("Disabled {} item history capture after an API mismatch", channel, throwable);
    }
}

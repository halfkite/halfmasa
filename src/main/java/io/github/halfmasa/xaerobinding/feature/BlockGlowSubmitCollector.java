package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.10
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;

/** Adds an outline color to the model submissions made by a matched block entity. */
public final class BlockGlowSubmitCollector
{
    private BlockGlowSubmitCollector() {}

    public static SubmitNodeCollector wrap(SubmitNodeCollector delegate, int color)
    {
        return (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[] { SubmitNodeCollector.class },
                (proxy, method, args) -> invoke(delegate, color, method, args));
    }

    private static OrderedSubmitNodeCollector wrapOrdered(OrderedSubmitNodeCollector delegate, int color)
    {
        return (OrderedSubmitNodeCollector) Proxy.newProxyInstance(
                OrderedSubmitNodeCollector.class.getClassLoader(),
                new Class<?>[] { OrderedSubmitNodeCollector.class },
                (proxy, method, args) -> invoke(delegate, color, method, args));
    }

    private static Object invoke(Object delegate, int color, Method method, Object[] args) throws Throwable
    {
        String name = method.getName();
        if (args != null)
        {
            if ((name.equals("submitModel") || name.equals("submitBlockModel")
                    || name.equals("submitModelPart") || name.equals("submitMovingBlock"))
                    && args.length > 0 && method.getParameterTypes()[args.length - 1] == int.class)
            {
                args[args.length - 1] = color;
            }
            else if (name.equals("submitItem") && (args.length == 8 || args.length == 9))
            {
                args[4] = color;
            }
        }
        try
        {
            Object result = method.invoke(delegate, args);
            if (name.equals("order") && result instanceof OrderedSubmitNodeCollector ordered)
            {
                return wrapOrdered(ordered, color);
            }
            return result;
        }
        catch (InvocationTargetException e)
        {
            throw e.getCause();
        }
    }
}
//#endif

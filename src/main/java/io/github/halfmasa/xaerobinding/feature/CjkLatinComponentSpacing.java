package io.github.halfmasa.xaerobinding.feature;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class CjkLatinComponentSpacing
{
    private CjkLatinComponentSpacing()
    {
    }

    public static Component apply(Component component)
    {
        MutableComponent result = Component.empty();
        int[] previous = {-1};
        component.visit((Style style, String text) -> {
            if (!text.isEmpty())
            {
                int first = text.codePointAt(0);
                String spaced = CjkLatinSpacing.apply(text);
                if (previous[0] >= 0 && CjkLatinSpacing.shouldSeparate(previous[0], first))
                {
                    spaced = " " + spaced;
                }
                result.append(Component.literal(spaced).setStyle(style));
                previous[0] = text.codePointBefore(text.length());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}

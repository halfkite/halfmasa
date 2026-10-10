package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import java.util.Collection;
import java.util.stream.Collectors;
import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.halfmasa.xaerobinding.compat.InputCompat;
import io.github.halfmasa.xaerobinding.feature.IgnoredKeySelection;

public final class KeymapInputNames
{
    private KeymapInputNames() {}

    public static String name(int code)
    {
        if (code < -3) return InputConstants.Type.MOUSE.getOrCreate(InputCompat.layoutCodeToMouseButton(code))
                .getDisplayName().getString();
        return code < 0 ? StringUtils.translate("halfmasa.gui.keymap_browser.mouse." + (-code - 1))
                : InputCompat.keyboardKey(code).getDisplayName().getString();
    }

    public static String names(Collection<Integer> keys)
    {
        return keys.isEmpty() ? StringUtils.translate("halfmasa.gui.ignored_keys.none")
                : keys.stream().sorted().map(KeymapInputNames::name)
                        .collect(Collectors.joining(StringUtils.translate("halfmasa.gui.ignored_keys.separator")));
    }

    public static String names(String value)
    {
        return names(IgnoredKeySelection.parse(value));
    }
}
//#endif

package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import fi.dy.masa.malilib.event.InputEventHandler;
import io.github.halfmasa.xaerobinding.compat.InputCompat;
import io.github.halfmasa.xaerobinding.feature.KeybindCustomizationStore;
import io.github.halfmasa.xaerobinding.mixin.KeyMappingAccessor;
import net.minecraft.client.Minecraft;

public final class KeymapKeyboardBindings
{
    private KeymapKeyboardBindings() {}

    public static List<KeymapKeyboardStyle.Binding> current(Minecraft client)
    {
        List<KeymapKeyboardStyle.Binding> bindings = new ArrayList<>();
        if (client.options != null)
        {
            var store = KeybindCustomizationStore.getInstance();
            for (var mapping : client.options.keyMappings)
            {
                List<Integer> codes = store.comboKeys(mapping);
                if (codes.isEmpty())
                {
                    var key = ((KeyMappingAccessor) mapping).halfmasa$getBoundKey();
                    if (InputConstants.UNKNOWN.equals(key)) continue;
        //#if MC >= 1.21.11
                    if (client.options.debugKeys != null && mapping != client.options.keyDebugModifier &&
                            Arrays.asList(client.options.debugKeys).contains(mapping) && client.options.keyDebugModifier != null)
                    {
                        var modifier = ((KeyMappingAccessor) client.options.keyDebugModifier).halfmasa$getBoundKey();
                        if (InputCompat.isKeyboardKey(modifier)) codes.add(modifier.getValue());
                    }
        //#endif
                    codes.add(key.getType() == InputConstants.Type.MOUSE
                            ? InputCompat.mouseButtonToLayoutCode(key.getValue()) : key.getValue());
                }
                bindings.add(new KeymapKeyboardStyle.Binding(codes, true));
            }
        }
        for (var category : InputEventHandler.getKeybindManager().getKeybindCategories())
        {
            for (var hotkey : category.getHotkeys())
            {
                var codes = hotkey.getKeybind().getKeys().stream().map(code -> code < -80 && code >= -100
                        ? InputCompat.mouseButtonToLayoutCode(code + 100) : code).toList();
                bindings.add(new KeymapKeyboardStyle.Binding(codes, false));
            }
        }
        return KeymapKeyboardStyle.withConflicts(bindings);
    }
}
//#endif

package io.github.halfmasa.xaerobinding.picker;

//#if MC >= 1.21.1
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.mixin.GuiListBaseAccessor;
import io.github.halfmasa.xaerobinding.mixin.RegistryPickerVisibleRowsAccessor;
import net.minecraft.client.Minecraft;

public final class PickerScrollMemory
{
    private static final Map<GuiStringListEdit, Draft> DRAFTS = new WeakHashMap<>();

    private static final class Draft
    {
        final Map<Integer, String> values = new HashMap<>();
        int scroll;
        boolean suspended;
        boolean insertedEntry;
    }

    private PickerScrollMemory() {}

    static Map<Integer, String> remember(GuiStringListEdit editor, WidgetListStringListEdit list)
    {
        var draft = DRAFTS.computeIfAbsent(editor, ignored -> new Draft());
        draft.scroll = list.getScrollbar().getValue();
        draft.suspended = true;
        for (var widget : ((RegistryPickerVisibleRowsAccessor) list).halfmasa$pickerRows())
        {
            if (widget instanceof RegistryListEntry row && row.wasConfigModified())
                draft.values.put(row.index(), row.draftValue());
        }
        return draft.values;
    }

    public static boolean suspended(GuiStringListEdit editor)
    {
        var draft = DRAFTS.get(editor);
        return draft != null && draft.suspended;
    }

    static void addFirstEntry(GuiStringListEdit editor, IConfigStringList config, String id)
    {
        var draft = DRAFTS.get(editor);
        if (draft == null || !config.getStrings().isEmpty()) return;
        config.getStrings().add(id);
        //#if MC >= 1.21.10
        config.markDirty();
        //#endif
        config.setModified();
        draft.insertedEntry = true;
    }

    public static void restore(GuiStringListEdit editor)
    {
        var draft = DRAFTS.get(editor);
        if (draft == null || !draft.suspended || MinecraftClientCompat.getScreen(Minecraft.getInstance()) != editor) return;
        var list = ((GuiListBaseAccessor) editor).halfmasa$getListWidget();
        if (list == null) return;
        draft.suspended = false;
        list.getScrollbar().setValue(draft.scroll);
        var rows = (RegistryPickerVisibleRowsAccessor) list;
        rows.halfmasa$setPickerScroll(list.getScrollbar().getValue());
        rows.halfmasa$rebuildPickerRows();
    }

    public static void restoreRow(GuiStringListEdit editor, RegistryListEntry row)
    {
        var draft = DRAFTS.get(editor);
        if (draft == null || draft.suspended) return;
        String value = draft.values.remove(row.index());
        if (value != null) row.restoreValue(value);
    }

    public static boolean applyHidden(GuiStringListEdit editor, IConfigStringList config)
    {
        var draft = DRAFTS.remove(editor);
        if (draft == null) return false;
        var values = config.getStrings();
        boolean changed = draft.insertedEntry;
        for (var entry : draft.values.entrySet())
        {
            int index = entry.getKey();
            if (index >= 0 && index < values.size() && !entry.getValue().equals(values.get(index)))
            {
                values.set(index, entry.getValue());
                changed = true;
            }
        }
        if (changed)
        {
            //#if MC >= 1.21.10
            config.markDirty();
            //#endif
            config.setModified();
        }
        return changed;
    }
}
//#endif

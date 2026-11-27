//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.picker;

import java.util.Set;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetStringListEditEntry;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import com.mojang.blaze3d.platform.InputConstants;

public final class RegistryListEntry extends WidgetStringListEditEntry {
    private final Set<RegistryIndex.Kind> kinds;
    private final GuiStringListEdit screen;
    private String lastValue;
    private RegistryIndex.Entry lastIcon;
    public RegistryListEntry(int x,int y,int width,int height,int index,boolean odd,String initial,String def,WidgetListStringListEdit parent,GuiStringListEdit screen,Set<RegistryIndex.Kind> kinds){
        super(x,y,width,height,index,odd,initial,def,parent);
        this.screen=screen;this.kinds=kinds;
        addButton(new RegistryPickerButton(x + width - 18, y + 1, 16, 20),
                (button, mouse) -> open());
    }
    @Override protected int addTextField(int x,int y,int resetX,int width,int height,String value){
        return super.addTextField(x+20,y,resetX-18,width-38,height,value);
    }
    //#if MC >= 1.21.11
    @Override public void render(GuiContext ctx,int mx,int my,boolean selected){
    //#elseif MC >= 1.21.8
    //$$ @Override public void render(GuiGraphics ctx,int mx,int my,boolean selected){
    //#else
    //$$ @Override public void render(int mx, int my, boolean selected, GuiGraphics ctx) {
    //#endif
        //#if MC >= 1.21.8
        super.render(ctx,mx,my,selected);
        //#else
        //$$ super.render(mx, my, selected, ctx);
        //#endif
        if(isDummy()||textField==null)return;
        //#if MC >= 1.21.11
        String value=textField.textField().getValue();
        //#else
        //$$ String value=textField.getTextField().getValue();
        //#endif
        if(!value.equals(lastValue)){lastValue=value;lastIcon=RegistryIndex.first(value,kinds);}
        if(lastIcon!=null)ctx.renderItem(lastIcon.icon(),x+22,y+3);
    }
    //#if MC >= 1.21.10
    @Override protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick)
    {
        return clickIcon(click.x(), click.y(), click.button()) || super.onMouseClickedImpl(click, doubleClick);
    }
    //#else
    //$$ @Override protected boolean onMouseClickedImpl(int mouseX, int mouseY, int button) {
    //$$     return clickIcon(mouseX, mouseY, button) || super.onMouseClickedImpl(mouseX, mouseY, button);
    //$$ }
    //#endif

    private boolean clickIcon(double mouseX, double mouseY, int button)
    {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && this.lastIcon != null
                && mouseX >= x + 22 && mouseX < x + 38 && mouseY >= y + 3 && mouseY < y + 19)
        {
            open();
            return true;
        }
        return false;
    }

    int index() { return listIndex; }
    //#if MC >= 1.21.11
    String draftValue() { return textField.textField().getValue(); }
    //#else
    //$$ String draftValue() { return textField.getTextField().getValue(); }
    //#endif
    //#if MC >= 1.21.11
    void restoreValue(String value) { textField.textField().setValue(value); }
    //#else
    //$$ void restoreValue(String value) { textField.getTextField().setValue(value); }
    //#endif

    private void open()
    {
        var draft = PickerScrollMemory.remember(screen, parent);
        RegistryPickerScreen.openSingle(screen, kinds, entry -> {
            if (isDummy()) PickerScrollMemory.addFirstEntry(screen, parent.getConfig(), entry.id());
            else draft.put(listIndex, entry.id());
        });
    }
}
//#endif

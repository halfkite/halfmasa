//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.picker;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#endif
import fi.dy.masa.malilib.render.RenderUtils;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.client.multiplayer.ClientLevel;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//#if MC >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent;
//#endif
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
//#if MC >= 1.21.3
import net.minecraft.world.entity.EntitySpawnReason;
//#endif
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

public final class RegistryPickerScreen extends GuiListBase<RegistryPickerScreen.Row,RegistryPickerScreen.RowWidget,RegistryPickerScreen.Grid> {
    private static final int CELL=20;
    private final Set<RegistryIndex.Kind> kinds;
    private final Predicate<RegistryIndex.Entry> allowed;
    private final Consumer<RegistryIndex.Entry> single;
    private final Consumer<Collection<String>> multi;
    private final Set<String> selected=new LinkedHashSet<>();
    private int left,top,w,h;
    private GuiTextFieldGeneric search;
    private String query="";
    private RegistryIndex.Entry hovered;
    private ClientLevel previewLevel;
    private final Map<String, Optional<LivingEntity>> entityPreviews = new HashMap<>();
    public record Row(RegistryIndex.Kind heading,List<RegistryIndex.Entry> cells) {}
    public static void openSingle(Screen parent, Set<RegistryIndex.Kind> kinds, Consumer<RegistryIndex.Entry> selected)
    {
        GuiBase.openGui(new RegistryPickerScreen(parent, kinds, entry -> true, selected, null));
    }

    public RegistryPickerScreen(Screen parent,Set<RegistryIndex.Kind> kinds,Predicate<RegistryIndex.Entry> allowed,Consumer<RegistryIndex.Entry> single,Consumer<Collection<String>> multi) {
        super(0,0); this.setParent(parent); this.kinds=Set.copyOf(kinds); this.allowed=allowed; this.single=single; this.multi=multi;
        this.title=Component.translatable("halfmasa.picker.title").getString();
    }
    @Override public void initGui() {
        w=Math.min(320,Math.max(80,width-12)); h=Math.max(76,height-90); left=(width-w)/2; top=(height-h)/2;
        if(height<166){ h=Math.max(48,height-12); top=(height-h)/2; }
        reCreateListWidget(); super.initGui();
        search=new GuiTextFieldGeneric(left+10,top+20,w-22,16,font);
        search.setValue(query);
        addTextField(search,field->{query=field.getValue(); getListWidget().resetScrollbarPosition(); getListWidget().refreshEntries(); return false;});
        if(multi!=null) {
            addButton(new ButtonGeneric(left+10,top+h-25,65,20,Component.translatable("malilib.gui.button.ok").getString()),(b,m)->multi.accept(List.copyOf(selected)));
            addButton(new ButtonGeneric(left+80,top+h-25,65,20,Component.translatable("malilib.gui.button.cancel").getString()),(b,m)->GuiBase.openGui(getParent()));
        }
    }
    @Override protected int getBrowserWidth(){return w-14;}
    @Override protected int getBrowserHeight(){return Math.max(1,h-(multi==null?46:72));}
    @Override protected Grid createListWidget(int x,int y){return new Grid(left+10,top+42,getBrowserWidth(),getBrowserHeight());}
    //#if MC >= 26.1
    @Override public void extractRenderState(@NotNull GuiGraphicsExtractor ctx, int mx, int my, float ticks)
    //#else
    //$$ @Override public void render(@NotNull GuiGraphics ctx, int mx, int my, float ticks)
    //#endif
    {
        hovered = null;
        //#if MC >= 26.1
        if (getParent() != null) getParent().extractRenderState(ctx, -1, -1, ticks);
        super.extractRenderState(ctx, mx, my, ticks);
        //#else
        //$$ if (getParent() != null) getParent().render(ctx, -1, -1, ticks);
        //$$ super.render(ctx, mx, my, ticks);
        //#endif
        if (hovered != null)
        {
            List<Component> tooltip = List.of(Component.literal(hovered.name()),
                    Component.literal(hovered.id()).withStyle(ChatFormatting.DARK_GRAY));
            //#if MC >= 1.21.11
            ctx.setComponentTooltipForNextFrame(font, tooltip, mx, my);
            //#elseif MC >= 1.21.8
            //$$ ctx.setTooltipForNextFrame(font, tooltip, Optional.empty(), mx, my);
            //#else
            //$$ ctx.renderTooltip(font, tooltip, Optional.empty(), mx, my);
            //#endif
        }
    }

    //#if MC >= 1.21.11
    private boolean renderEntityPreview(GuiContext graphics, RegistryIndex.Entry entry, int x, int y)
    //#else
    //$$ private boolean renderEntityPreview(GuiGraphics graphics, RegistryIndex.Entry entry, int x, int y)
    //#endif
    {
        if (entry.kind() != RegistryIndex.Kind.ENTITY || !Configs.REGISTRY_PICKER_ENTITY_MODELS.getBooleanValue()) return false;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != previewLevel)
        {
            entityPreviews.clear();
            previewLevel = level;
        }
        if (level == null) return false;
        var entity = entityPreviews.computeIfAbsent(entry.id(), key -> BuiltInRegistries.ENTITY_TYPE
                //#if MC >= 1.21.11
                .getOptional(Identifier.parse(key))
                //#else
                //$$ .getOptional(ResourceLocation.parse(key))
                //#endif
                //#if MC >= 1.21.3
                .map(type -> type.create(level, EntitySpawnReason.COMMAND))
                //#else
                //$$ .map(type -> type.create(level))
                //#endif
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .map(preview -> {
                    // The inventory renderer uses entity IDs for item models. Previews stay detached from the world.
                    preview.setId(-entityPreviews.size() - 1);
                    return preview;
                })).orElse(null);
        if (entity == null) return false;
        float extent = Math.max(entity.getBbWidth(), entity.getBbHeight());
        int scale = Math.max(1, Math.round((CELL - 4) / Math.max(0.8F, extent)));
        //#if MC >= 26.1
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics.getGuiGraphics(), x, y, x + CELL, y + CELL,
        //#else
        //#if MC >= 1.21.11
        //$$ InventoryScreen.renderEntityInInventoryFollowsMouse(graphics.getGuiGraphics(), x, y, x + CELL, y + CELL,
        //#else
        //$$ InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x, y, x + CELL, y + CELL,
        //#endif
        //#endif
                scale, 0.0F, x + CELL * 3.0F, y + CELL * 0.2F, entity);
        return true;
    }
    //#if MC >= 1.21.11
    @Override protected void drawScreenBackground(GuiContext ctx,int mx,int my){RenderUtils.drawOutlinedBox(ctx,left,top,w,h,0xff000000,0xff999999);}
    //#elseif MC >= 1.21.8
    //$$ @Override protected void drawScreenBackground(GuiGraphics ctx,int mx,int my){RenderUtils.drawOutlinedBox(ctx,left,top,w,h,0xff000000,0xff999999);}
    //#else
    //$$ @Override protected void drawScreenBackground(GuiGraphics ctx, int mx, int my) {
    //$$     ctx.fill(left, top, left + w, top + h, 0xff999999);
    //$$     ctx.fill(left + 1, top + 1, left + w - 1, top + h - 1, 0xff000000);
    //$$ }
    //#endif
    //#if MC >= 1.21.11
    @Override protected void drawTitle(GuiContext ctx,int mx,int my,float ticks){drawStringWithShadow(ctx,title,left+10,top+6,0xffffffff);}
    //#else
    //$$ @Override protected void drawTitle(GuiGraphics ctx,int mx,int my,float ticks){drawStringWithShadow(ctx,title,left+10,top+6,0xffffffff);}
    //#endif
    private void choose(RegistryIndex.Entry e){
        if(multi!=null){if(!selected.add(e.id()))selected.remove(e.id());}
        else {single.accept(e);GuiBase.openGui(getParent());}
    }
    public final class Grid extends WidgetListBase<Row,RowWidget> {
        Grid(int x,int y,int width,int height){super(x,y,width,height,null);this.browserEntryHeight=CELL;this.shouldSortList=false;}
        @Override protected Collection<Row> getAllEntries(){
            var entries=RegistryIndex.search(kinds,query,allowed);
            int columns=Math.max(1,(browserEntryWidth-2)/CELL);
            var rows=new ArrayList<Row>();
            for(var k:RegistryIndex.Kind.values()) {
                var group=entries.stream().filter(e->e.kind()==k).toList();
                if(group.isEmpty())continue;
                rows.add(new Row(k,List.of()));
                for(int i=0;i<group.size();i+=columns)rows.add(new Row(null,List.copyOf(group.subList(i,Math.min(i+columns,group.size())))));
            }
            return rows;
        }
        @Override protected RowWidget createListEntryWidget(int x,int y,int idx,boolean odd,Row row){return new RowWidget(x,y,browserEntryWidth,CELL,idx,row);}
    }
    public final class RowWidget extends WidgetListEntryBase<Row> {
        RowWidget(int x,int y,int w,int h,int index,Row row){super(x,y,w,h,row,index);}
        private RegistryIndex.Entry at(double mx,double my){
            if(entry==null||entry.heading()!=null||my<y||my>=y+CELL||mx<x)return null;
            int i=(int)(mx-x)/CELL; return i<entry.cells().size()?entry.cells().get(i):null;
        }
        //#if MC >= 1.21.10
        @Override protected boolean onMouseClickedImpl(MouseButtonEvent click,boolean dbl){
            return chooseAt(click.x(), click.y(), click.button());
        }
        //#else
        //$$ @Override protected boolean onMouseClickedImpl(int mouseX, int mouseY, int button) {
        //$$     return chooseAt(mouseX, mouseY, button);
        //$$ }
        //#endif
        private boolean chooseAt(double mouseX, double mouseY, int button) {
            if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return false;
            var candidate = at(mouseX, mouseY);
            if (candidate == null) return false;
            choose(candidate);
            return true;
        }
        //#if MC >= 1.21.11
        @Override public void render(GuiContext ctx,int mx,int my,boolean ignored){
        //#elseif MC >= 1.21.8
        //$$ @Override public void render(GuiGraphics ctx,int mx,int my,boolean ignored){
        //#else
        //$$ @Override public void render(int mx, int my, boolean ignored, GuiGraphics ctx) {
        //#endif
            if(entry==null)return;
            if(entry.heading()!=null){
                String text=Component.translatable("halfmasa.picker.kind."+entry.heading().name().toLowerCase(Locale.ROOT)).getString();
                int start=x+getStringWidth(text)+8;
                //#if MC >= 1.21.8
                drawString(ctx,x+2,y+5,0xffbbbbbb,text);
                if(start<x+width-2)RenderUtils.drawRect(ctx,start,y+10,x+width-start-2,1,0xff555555);
                //#else
                //$$ ctx.drawString(font, text, x + 2, y + 5, 0xffbbbbbb);
                //$$ if (start < x + width - 2) ctx.fill(start, y + 10, x + width - 2, y + 11, 0xff555555);
                //#endif
                return;
            }
            var underMouse = my >= top + 42 && my < top + 42 + getBrowserHeight() ? at(mx, my) : null;
            if (underMouse != null) hovered = underMouse;
            int i=0;for(var e:entry.cells()){
                int px=x+CELL*i++;
                //#if MC >= 1.21.8
                if(selected.contains(e.id()))RenderUtils.drawRect(ctx,px,y,CELL,CELL,0x80509040);
                if(e==underMouse)RenderUtils.drawRect(ctx,px,y,CELL,CELL,0x50ffffff);
                //#else
                //$$ if (selected.contains(e.id())) ctx.fill(px, y, px + CELL, y + CELL, 0x80509040);
                //$$ if (e == underMouse) ctx.fill(px, y, px + CELL, y + CELL, 0x50ffffff);
                //#endif
                if (!renderEntityPreview(ctx, e, px, y)) ctx.renderItem(e.icon(),px+2,y+2);
            }
        }
    }
}
//#endif

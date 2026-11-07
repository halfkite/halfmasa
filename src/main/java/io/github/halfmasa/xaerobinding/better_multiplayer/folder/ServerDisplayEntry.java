package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

public abstract class ServerDisplayEntry extends ServerSelectionList.Entry implements ServerEntryLayout {
    protected abstract boolean matches(ServerSelectionList.Entry other);
    public abstract void join();
    protected ServerSelectionList halfmasa$list;
    public void halfmasa$bind(ServerSelectionList list){halfmasa$list=list;}
//#if MC >= 1.21.10

//#else
//$$     private int halfmasa$x, halfmasa$y, halfmasa$width, halfmasa$height;
//$$     @Override public final void render(net.minecraft.client.gui.GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float delta) {
//$$         halfmasa$x=x; halfmasa$y=y; halfmasa$width=width; halfmasa$height=height+4;
//$$         halfmasa$renderContent(graphics,mouseX,mouseY,hovered,delta);
//$$     }
//$$     public abstract void halfmasa$renderContent(net.minecraft.client.gui.GuiGraphics graphics,int mouseX,int mouseY,boolean hovered,float delta);
//$$     public int getX(){return halfmasa$list==null ? halfmasa$x : halfmasa$list.getRowLeft();} public int getY(){return halfmasa$list==null ? halfmasa$y : ((io.github.halfmasa.xaerobinding.draggable.DragList<?,?>)(Object)halfmasa$list).draggable_lists$getRowTop(halfmasa$list.children().indexOf(this));}
//$$     public int getWidth(){return halfmasa$list==null ? halfmasa$width : halfmasa$list.getRowWidth();} public int getHeight(){return halfmasa$list==null ? halfmasa$height : ((io.github.halfmasa.xaerobinding.draggable.DragList<?,?>)(Object)halfmasa$list).draggable_lists$getItemHeight();}
//$$     public int getContentX(){return getX();} public int getContentY(){return getY();}
//$$     public int getContentWidth(){return getWidth();} public int getContentRight(){return getX()+getWidth();}
//#endif
}

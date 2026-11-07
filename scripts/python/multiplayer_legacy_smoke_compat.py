"""Adapt only the new multiplayer/favorites fixtures to the pre-event input API."""
import re


def source(text):
    multiplayer = 'class MultiplayerSmoke implements' in text
    favorites = 'class FavoritesSmoke implements' in text
    if not (multiplayer or favorites):
        return text
    text = text.replace('mc.setScreenAndShow(', 'mc.forceSetScreen(').replace('mc.getWindow().handle()', 'mc.getWindow().getWindow()')
    text = text.replace('import net.minecraft.client.input.*;', '')
    text = text.replace('net.minecraft.client.input.MouseButtonEvent', 'io.github.halfmasa.xaerobinding.better_multiplayer.folder.ServerPointer')
    text = re.sub(r'\bMouseButtonEvent\b', 'io.github.halfmasa.xaerobinding.better_multiplayer.folder.ServerPointer', text)
    text = re.sub(r'new (?:net\.minecraft\.client\.input\.)?MouseButtonInfo\(([^,]+),0\)\)', r'\1,false)', text)
    pointer = 'io.github.halfmasa.xaerobinding.better_multiplayer.folder.ServerPointer'
    if multiplayer:
        text = text.replace('list.getSelected().join()', 'screen.joinSelectedServer()')
        for target in ('list', 'row'):
            text = text.replace(target+'.mouseClicked(', 'legacyClick('+target+',')
            text = text.replace(target+'.mouseReleased(', 'legacyRelease('+target+',')
            text = text.replace(target+'.mouseDragged(', 'legacyDrag('+target+',')
        text = re.sub(r'\.onPress\(mouse\(0,0,com\.mojang\.blaze3d\.platform\.InputConstants\.MOUSE_BUTTON_LEFT\)\)', '.onPress()', text)
        text = re.sub(r'\b(probe|e|target)\.(getX|getY|getWidth|getHeight|getContentX|getContentY|getContentWidth|getContentRight)\(', r'((ServerEntryLayout)\1).\2(', text)
        text = text.replace('list.children().get(i).getY()', '((io.github.halfmasa.xaerobinding.draggable.DragList<?,?>)list).draggable_lists$getRowTop(i)')
        text = text.replace('list.children().get(i-1).getY()+list.children().get(i-1).getHeight()', '((io.github.halfmasa.xaerobinding.draggable.DragList<?,?>)list).draggable_lists$getRowBottom(i-1)')
        methods = '''
 static boolean legacyClick(net.minecraft.client.gui.components.events.GuiEventListener widget,POINTER event,boolean doubleClick){return widget.mouseClicked(event.x(),event.y(),event.button());}
 static boolean legacyRelease(net.minecraft.client.gui.components.events.GuiEventListener widget,POINTER event){return widget.mouseReleased(event.x(),event.y(),event.button());}
 static boolean legacyDrag(net.minecraft.client.gui.components.events.GuiEventListener widget,POINTER event,double dx,double dy){return widget.mouseDragged(event.x(),event.y(),event.button(),dx,dy);}
'''.replace('POINTER', pointer)
        text = text.replace(' static MultiplayerSmoke instance;', methods+'\n static MultiplayerSmoke instance;')
    if favorites:
        for target in ('widget', 'favorite', 'dropdown', 'large'):
            text = text.replace(target+'.onMouseClicked(', 'legacyWidgetClick('+target+',')
        text = re.sub(r'\.onKeyTyped\(new KeyEvent\(([^,]+),([^,]+),([^)]*)\)\)', r'.onKeyTyped(\1,\2,\3)', text)
        method = ' static boolean legacyWidgetClick(WidgetBase widget,POINTER event,boolean doubleClick){return widget.onMouseClicked((int)event.x(),(int)event.y(),event.button());}\n'.replace('POINTER', pointer)
        text = text.replace(' static final boolean BASE_ONLY=', method+' static final boolean BASE_ONLY=')
    return text

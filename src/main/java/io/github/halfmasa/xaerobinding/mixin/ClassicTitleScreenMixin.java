package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.List;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.gui.ClassicModsButton;
import io.github.halfmasa.xaerobinding.gui.ClassicPauseLayout;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep vanilla actions and mod-added controls while restoring classic title rows. */
@Mixin(TitleScreen.class)
public abstract class ClassicTitleScreenMixin extends Screen
{
    @Unique private List<AbstractButton> halfmasa$buttons = List.of();
    @Unique private ClassicModsButton halfmasa$mods;
    @Unique private int halfmasa$width, halfmasa$height;

    protected ClassicTitleScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("HEAD"))
    private void halfmasa$reset(CallbackInfo ci)
    {
        halfmasa$buttons = List.of();
        halfmasa$mods = null;
    }

    //#if MC >= 26.0
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"))
    //#endif
    private void halfmasa$layout(CallbackInfo ci)
    {
        if (!Configs.CLASSIC_PAUSE_MENU.getBooleanValue())
        {
            if (!halfmasa$buttons.isEmpty()) this.rebuildWidgets();
            return;
        }
        if (halfmasa$mods != null) halfmasa$mods.refreshState();
        var buttons = this.children().stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).toList();
        if (buttons.equals(halfmasa$buttons) && this.width == halfmasa$width && this.height == halfmasa$height) return;
        var rows = new ArrayList<AbstractButton>();
        AbstractButton options = null, quit = null, mods = null;
        for (var button : buttons)
        {
            if (button instanceof ClassicModsButton || button.getClass().getName().startsWith("com.terraformersmc.modmenu.gui.widget.")) mods = button;
            if (button.getMessage().getContents() instanceof TranslatableContents text)
            {
                switch (text.getKey())
                {
                    case "menu.singleplayer", "menu.multiplayer", "menu.online", "menu.playdemo", "menu.resetdemo" -> rows.add(button);
                    case "menu.options" -> options = button;
                    case "menu.quit" -> quit = button;
                    default -> {}
                }
            }
        }
        if (mods instanceof Button original && !(mods instanceof ClassicModsButton) && mods.getWidth() <= 20)
        {
            halfmasa$mods = new ClassicModsButton(original);
            this.removeWidget(original);
            this.addRenderableWidget(halfmasa$mods);
            mods = halfmasa$mods;
            buttons = this.children().stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).toList();
        }
        if (mods != null) rows.add(mods);
        var extras = new ArrayList<>(buttons);
        extras.removeAll(rows); extras.remove(options); extras.remove(quit);
        var layout = ClassicPauseLayout.arrangeTitle(this.width, this.height, mods != null,
                extras.stream().map(button -> new ClassicPauseLayout.Size(button.getWidth(), button.getHeight())).toList());
        var anchor = layout.main().get(ClassicPauseLayout.Role.RETURN);
        for (int i = 0; i < rows.size(); i++) halfmasa$place(rows.get(i), anchor.x(), anchor.y() + i * 24, anchor.width(), 20);
        int half = Math.max(1, (anchor.width() - 4) / 2), y = anchor.y() + rows.size() * 24;
        if (options != null) halfmasa$place(options, anchor.x(), y, half, 20);
        if (quit != null) halfmasa$place(quit, anchor.x() + half + 4, y, Math.max(1, anchor.width() - half - 4), 20);
        for (int i = 0; i < extras.size(); i++)
        {
            var rect = layout.extras().get(i);
            halfmasa$place(extras.get(i), rect.x(), rect.y(), rect.width(), rect.height());
        }
        halfmasa$buttons = buttons;
        halfmasa$width = this.width; halfmasa$height = this.height;
    }

    @Unique private static void halfmasa$place(AbstractButton button, int x, int y, int width, int height)
    {
        button.setPosition(x, y); button.setSize(width, height);
    }
}
//#endif

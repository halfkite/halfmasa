package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import java.util.List;
import java.util.EnumMap;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.gui.ClassicPauseLayout;
import io.github.halfmasa.xaerobinding.gui.ClassicPauseLayout.Role;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import io.github.halfmasa.xaerobinding.gui.ClassicModsButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class ClassicPauseScreenMixin extends Screen
{
    @Shadow @Final private boolean showPauseMenu;
    @Shadow private Button disconnectButton;
    @Unique private List<AbstractButton> halfmasa$laidOut = List.of();
    @Unique private ClassicModsButton halfmasa$modsButton;
    @Unique private int halfmasa$layoutWidth, halfmasa$layoutHeight;

    protected ClassicPauseScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("HEAD"))
    private void halfmasa_resetClassicLayout(CallbackInfo ci)
    {
        halfmasa$laidOut = List.of();
        halfmasa$modsButton = null;
    }

    // Wait until all init injections, including recording and Mod Menu, have added their controls.
    //#if MC >= 26.0
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "render", at = @At("HEAD"))
    //#endif
    private void halfmasa_applyClassicLayout(CallbackInfo ci)
    {
        if (!showPauseMenu) return;
        if (!Configs.CLASSIC_PAUSE_MENU.getBooleanValue())
        {
            if (!halfmasa$laidOut.isEmpty()) this.rebuildWidgets();
            return;
        }
        if (halfmasa$modsButton != null)
        {
            halfmasa$modsButton.refreshState();
        }
        List<AbstractButton> buttons = this.children().stream().filter(AbstractButton.class::isInstance)
                .map(AbstractButton.class::cast).toList();
        if (buttons.equals(halfmasa$laidOut) && this.width == halfmasa$layoutWidth && this.height == halfmasa$layoutHeight) return;
        var main = new EnumMap<Role, AbstractButton>(Role.class);
        for (AbstractButton button : buttons)
        {
            Role role = halfmasa$role(button);
            if (role != null) main.putIfAbsent(role, button);
        }
        if (main.get(Role.MODS) instanceof Button mods && !(mods instanceof ClassicModsButton) && mods.getWidth() <= 20)
        {
            halfmasa$modsButton = new ClassicModsButton(mods);
            this.removeWidget(mods);
            this.addRenderableWidget(halfmasa$modsButton);
            main.put(Role.MODS, halfmasa$modsButton);
            buttons = this.children().stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).toList();
        }
        List<AbstractButton> extras = buttons.stream().filter(button -> !main.containsValue(button)).toList();
        var layout = ClassicPauseLayout.arrange(this.width, this.height, main.containsKey(Role.MODS),
                extras.stream().map(button -> new ClassicPauseLayout.Size(button.getWidth(), button.getHeight())).toList());
        main.forEach((role, button) -> halfmasa$place(button, layout.main().get(role)));
        for (int i = 0; i < extras.size(); i++)
        {
            AbstractButton button = extras.get(i);
            var rectangle = layout.extras().get(i);
            halfmasa$place(button, rectangle);
        }
        halfmasa$laidOut = buttons;
        halfmasa$layoutWidth = this.width; halfmasa$layoutHeight = this.height;
    }

    @Unique
    private Role halfmasa$role(AbstractButton button)
    {
        if (button == this.disconnectButton) return Role.EXIT;
        if (button instanceof ClassicModsButton || button.getClass().getName().startsWith("com.terraformersmc.modmenu.gui.widget.")) return Role.MODS;
        if (button.getMessage().getContents() instanceof TranslatableContents text)
        {
            return switch (text.getKey()) {
                case "menu.returnToGame" -> Role.RETURN;
                case "gui.advancements" -> Role.ADVANCEMENTS;
                case "gui.stats" -> Role.STATS;
                case "menu.options" -> Role.OPTIONS;
                case "options.worldOptions.button", "menu.shareToLan" -> Role.WORLD_OPTIONS;
                default -> null;
            };
        }
        return null;
    }

    @Unique
    private static void halfmasa$place(AbstractButton button, ClassicPauseLayout.Rect rectangle)
    {
        button.setPosition(rectangle.x(), rectangle.y());
        button.setSize(rectangle.width(), rectangle.height());
    }

}
//#endif

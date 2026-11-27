package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.1
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
//#if MC >= 26.2
import net.minecraft.client.gui.Hud;
//#else
//$$ import net.minecraft.client.gui.Gui;
//#endif
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.2
@Mixin(Hud.class)
//#else
//$$ @Mixin(Gui.class)
//#endif
public abstract class AllModeHudMixin
{
    //#if MC >= 26.2
    private static final String HALF_MASA_CAMERA_PLAYER = "Lnet/minecraft/client/gui/Hud;getCameraPlayer()Lnet/minecraft/world/entity/player/Player;";
    //#else
    //$$ private static final String HALF_MASA_CAMERA_PLAYER = "Lnet/minecraft/client/gui/Gui;getCameraPlayer()Lnet/minecraft/world/entity/player/Player;";
    //#endif
    @Unique private static final int halfmasa$ICON_SIZE = 9;
    @Unique private static final int halfmasa$ICON_BOTTOM_OFFSET = 49;
    @Unique private static final int halfmasa$SPECTATOR_TEXT_SHIFT = 24;
    @Unique private ItemStack halfmasa$modeIcon = ItemStack.EMPTY;

    //#if MC >= 26.1
    @Shadow private void extractItemHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker)
    //#else
    //$$ @Shadow private void renderItemHotbar(GuiGraphics graphics, DeltaTracker deltaTracker)
    //#endif
    {
        throw new AssertionError();
    }

    @Unique
    private static boolean halfmasa$showSpectatorHotbar(Minecraft mc)
    {
        return Configs.ALL_MODE_HUD.getBooleanValue() && Configs.SPECTATOR_HOTBAR.getBooleanValue()
                && mc.player != null && mc.player.isSpectator();
    }

    //#if MC >= 26.1
    @WrapOperation(method = "extractHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;extractHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void halfmasa$showInventoryWhenMenuClosed(SpectatorGui spectatorGui, GuiGraphicsExtractor graphics,
                                                       Operation<Void> original, GuiGraphicsExtractor hudGraphics,
                                                       DeltaTracker deltaTracker)
    //#else
    //$$ @WrapOperation(method = "renderHotbarAndDecorations",
    //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;renderHotbar(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    //$$ private void halfmasa$showInventoryWhenMenuClosed(SpectatorGui spectatorGui, GuiGraphics graphics,
    //$$ Operation<Void> original, GuiGraphics hudGraphics,
    //$$ DeltaTracker deltaTracker)
    //#endif
    {
        original.call(spectatorGui, graphics);
        if (!spectatorGui.isMenuActive() && halfmasa$showSpectatorHotbar(Minecraft.getInstance()))
        {
            //#if MC >= 26.1
            this.extractItemHotbar(hudGraphics, deltaTracker);
            //#else
            //$$ this.renderItemHotbar(hudGraphics, deltaTracker);
            //#endif
        }
    }

    //#if MC >= 26.1
    @ModifyExpressionValue(method = "extractItemHotbar",
            at = @At(value = "INVOKE", target = HALF_MASA_CAMERA_PLAYER))
    //#else
    //$$ @ModifyExpressionValue(method = "renderItemHotbar",
    //$$ at = @At(value = "INVOKE", target = HALF_MASA_CAMERA_PLAYER))
    //#endif
    private Player halfmasa$ownHotbarPlayer(Player cameraPlayer)
    {
        Minecraft mc = Minecraft.getInstance();
        return halfmasa$showSpectatorHotbar(mc) ? mc.player : cameraPlayer;
    }

    @Unique
    private static boolean halfmasa$showExtraStatus()
    {
        Minecraft mc = Minecraft.getInstance();
        if (!Configs.ALL_MODE_HUD.getBooleanValue() || mc.player == null || mc.gameMode == null)
        {
            return false;
        }
        return switch (mc.gameMode.getPlayerMode())
        {
            case CREATIVE -> Configs.CREATIVE_STATUS_BAR.getBooleanValue();
            case SPECTATOR -> Configs.SPECTATOR_STATUS_BAR.getBooleanValue();
            default -> false;
        };
    }

    // Only the HUD observes this modified condition; gameplay is unchanged.
    //#if MC >= 26.1
    @ModifyExpressionValue(method = {"extractHotbarAndDecorations", "extractSelectedItemName"},
    //#else
    //$$ @ModifyExpressionValue(method = {"renderHotbarAndDecorations", "renderSelectedItemName"},
    //#endif
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;canHurtPlayer()Z"))
    private boolean halfmasa$showStatusBars(boolean original)
    {
        return original || halfmasa$showExtraStatus();
    }

    //#if MC >= 26.1
    @ModifyExpressionValue(method = {"extractHotbarAndDecorations", "nextContextualInfoState"},
    //#elseif MC >= 1.21.8
    //$$ @ModifyExpressionValue(method = {"renderHotbarAndDecorations", "nextContextualInfoState"},
    //#else
    //$$ @ModifyExpressionValue(method = "isExperienceBarVisible",
    //#endif
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z"))
    private boolean halfmasa$showExperience(boolean original)
    {
        return original || (halfmasa$showExtraStatus() && Configs.ALL_MODE_EXPERIENCE_BAR.getBooleanValue());
    }

    // Keep personal health/food/armor coherent when the spectator camera is attached to another entity.
    //#if MC >= 26.1
    @ModifyExpressionValue(method = {"extractPlayerHealth", "getPlayerVehicleWithHealth"},
    //#else
    //$$ @ModifyExpressionValue(method = {"renderPlayerHealth", "getPlayerVehicleWithHealth"},
    //#endif
            at = @At(value = "INVOKE", target = HALF_MASA_CAMERA_PLAYER))
    private Player halfmasa$ownStatusPlayer(Player original)
    {
        Minecraft mc = Minecraft.getInstance();
        if (halfmasa$showExtraStatus() && mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
        {
            return mc.player;
        }
        return original;
    }

    // Move only the spectator action label, never the actual spectator menu.
    //#if MC >= 26.1
    @WrapOperation(method = "extractHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;extractAction(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void halfmasa$moveSpectatorAction(SpectatorGui spectatorGui, GuiGraphicsExtractor graphics,
                                               Operation<Void> original)
    //#else
    //$$ @WrapOperation(method = "renderHotbarAndDecorations",
    //#if MC >= 1.21.8
    //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;renderAction(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    //#else
    //$$ at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    //#endif
    //$$ private void halfmasa$moveSpectatorAction(SpectatorGui spectatorGui, GuiGraphics graphics,
    //$$ Operation<Void> original)
    //#endif
    {
        if (!halfmasa$showExtraStatus())
        {
            original.call(spectatorGui, graphics);
            return;
        }
        //#if MC >= 1.21.8
        graphics.pose().pushMatrix();
        //#else
        //$$ graphics.pose().pushPose();
        //#endif
        try
        {
            //#if MC >= 1.21.8
            graphics.pose().translate(0.0F, -halfmasa$SPECTATOR_TEXT_SHIFT);
            //#else
            //$$ graphics.pose().translate(0.0F, -halfmasa$SPECTATOR_TEXT_SHIFT, 0.0F);
            //#endif
            original.call(spectatorGui, graphics);
        }
        finally
        {
            //#if MC >= 1.21.8
            graphics.pose().popMatrix();
            //#else
            //$$ graphics.pose().popPose();
            //#endif
        }
    }

    //#if MC >= 26.1
    @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
    private void halfmasa$drawModeIcon(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci)
    //#else
    //$$ @Inject(method = "renderHotbarAndDecorations", at = @At("TAIL"))
    //$$ private void halfmasa$drawModeIcon(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci)
    //#endif
    {
        Minecraft mc = Minecraft.getInstance();
        if (!Configs.ALL_MODE_HUD.getBooleanValue() || !Configs.GAME_MODE_ICON.getBooleanValue()
                || mc.player == null || mc.gameMode == null)
        {
            return;
        }
        Item icon = switch (mc.gameMode.getPlayerMode())
        {
            case CREATIVE -> halfmasa$showExtraStatus() ? Items.GRASS_BLOCK : null;
            case SPECTATOR -> halfmasa$showExtraStatus() ? Items.ENDER_EYE : null;
            //#if MC >= 26.3
            case ADVENTURE -> Configs.ADVENTURE_MODE_ICON.getBooleanValue() ? Items.BURIED_TREASURE_MAP : null;
            //#else
            //$$ case ADVENTURE -> Configs.ADVENTURE_MODE_ICON.getBooleanValue() ? Items.FILLED_MAP : null;
            //#endif
            default -> null;
        };
        if (icon == null)
        {
            return;
        }
        if (!halfmasa$modeIcon.is(icon)) halfmasa$modeIcon = icon.getDefaultInstance();
        int x = (graphics.guiWidth() - halfmasa$ICON_SIZE) / 2;
        int y = graphics.guiHeight() - halfmasa$ICON_BOTTOM_OFFSET;
        float scale = halfmasa$ICON_SIZE / 16.0F;
        //#if MC >= 1.21.8
        graphics.pose().pushMatrix();
        //#else
        //$$ graphics.pose().pushPose();
        //#endif
        try
        {
            //#if MC >= 1.21.8
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            //#else
            //$$ graphics.pose().translate(x, y, 0F);
            //$$ graphics.pose().scale(scale, scale, 1F);
            //#endif
            //#if MC >= 26.1
            graphics.fakeItem(halfmasa$modeIcon, 0, 0);
            //#else
            //$$ graphics.renderFakeItem(halfmasa$modeIcon, 0, 0);
            //#endif
        }
        finally
        {
            //#if MC >= 1.21.8
            graphics.pose().popMatrix();
            //#else
            //$$ graphics.pose().popPose();
            //#endif
        }
    }
}
//#else
//$$ public abstract class AllModeHudMixin {}
//#endif

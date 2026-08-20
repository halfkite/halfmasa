package io.github.halfmasa.xaerobinding.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import io.github.halfmasa.xaerobinding.compat.TweakerooFreeCameraCompat;
import io.github.halfmasa.xaerobinding.compat.LitematicaEasyPlaceCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.MultiPlayerGameModeAccessor;

public final class FreeCameraInteraction
{
    private FreeCameraInteraction() {}

    public static boolean tryUseTargetedBlock(Minecraft client)
    {
        boolean containerInteraction = Configs.FREE_CAMERA_INTERACTION.getBooleanValue() &&
                Configs.FREE_CAMERA_CONTAINER_INTERACTION.getBooleanValue();
        boolean itemUse = Configs.FREE_CAMERA_INTERACTION.getBooleanValue() &&
                Configs.FREE_CAMERA_ITEM_USE.getBooleanValue();
        boolean litematicaEasyPlace = Configs.FREE_CAMERA_INTERACTION.getBooleanValue() &&
                Configs.FREE_CAMERA_LITEMATICA_EASY_PLACE.getBooleanValue();
        if (!containerInteraction && !itemUse && !litematicaEasyPlace)
        {
            return false;
        }

        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        Entity camera = TweakerooFreeCameraCompat.getActiveCamera(client);
        if (player == null || gameMode == null || camera == null || player.isHandsBusy())
        {
            return false;
        }
        if (litematicaEasyPlace && LitematicaEasyPlaceCompat.tryHandleEasyPlace())
        {
            return true;
        }
        if (!containerInteraction && handsAreEmpty(player))
        {
            return false;
        }

        double reach = TweakerooFreeCameraCompat.getBlockReach(client);
        HitResult hitResult = camera.pick(reach, 1.0F, itemUse && hasBucket(player));
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK)
        {
            return false;
        }

        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isItemEnabled(player.level().enabledFeatures()))
            {
                return true;
            }

            InteractionResult result = gameMode.useItemOn(player, hand, blockHitResult);
            if (result.consumesAction())
            {
                //#if MC >= 1.21.4
                if (result instanceof InteractionResult.Success success &&
                    success.swingSource() == InteractionResult.SwingSource.CLIENT)
                //#else
                //$$ if (result.shouldSwing())
                //#endif
                {
                    player.swing(hand);
                }
                return true;
            }
            if (result == InteractionResult.FAIL)
            {
                return true;
            }
            if (itemUse && stack.getItem() instanceof BucketItem)
            {
                useBucketFromTarget(client, gameMode, player, hand, blockHitResult);
                return true;
            }
        }

        return true;
    }

    private static boolean handsAreEmpty(LocalPlayer player)
    {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    public static boolean tryStartMining(Minecraft client)
    {
        if (!Configs.FREE_CAMERA_INTERACTION.getBooleanValue() || !Configs.FREE_CAMERA_MINING.getBooleanValue()) return false;
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        Entity camera = TweakerooFreeCameraCompat.getActiveCamera(client);
        if (player == null || gameMode == null || camera == null || player.isHandsBusy()) return false;
        HitResult hit = camera.pick(TweakerooFreeCameraCompat.getBlockReach(client), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return false;
        if (client.getConnection() == null || client.level == null) return false;
        ((MultiPlayerGameModeAccessor) gameMode).halfmasa_startPrediction(
                client.level,
                sequence -> new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                        blockHit.getBlockPos(), blockHit.getDirection(), sequence));
        return true;
    }

    public static boolean tryContinueMining(Minecraft client)
    {
        if (!Configs.FREE_CAMERA_INTERACTION.getBooleanValue() || !Configs.FREE_CAMERA_MINING.getBooleanValue()) return false;
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        Entity camera = TweakerooFreeCameraCompat.getActiveCamera(client);
        if (player == null || gameMode == null || camera == null || player.isHandsBusy()) return false;
        HitResult hit = camera.pick(TweakerooFreeCameraCompat.getBlockReach(client), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return false;
        if (client.getConnection() == null || client.level == null) return false;
        ((MultiPlayerGameModeAccessor) gameMode).halfmasa_startPrediction(
                client.level,
                sequence -> new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        blockHit.getBlockPos(), blockHit.getDirection(), sequence));
        return true;
    }

    private static boolean hasBucket(LocalPlayer player)
    {
        return player.getMainHandItem().getItem() instanceof BucketItem ||
               player.getOffhandItem().getItem() instanceof BucketItem;
    }

    private static void useBucketFromTarget(
            Minecraft client,
            MultiPlayerGameMode gameMode,
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult target)
    {
        Vec3 direction = target.getLocation().subtract(player.getEyePosition());
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0D);
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontalLength));

        ((MultiPlayerGameModeAccessor) gameMode).halfmasa_startPrediction(
                client.level,
                sequence -> new ServerboundUseItemPacket(hand, sequence, yaw, pitch));
        player.swing(hand);
    }
}

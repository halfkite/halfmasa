package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

/** Tracks only successfully started local swings; never changes entity or packet state. */
public final class SwingVisualTracker
{
    public enum Source { UNKNOWN, ATTACK, INTERACT }
    private static final ThreadLocal<Source> CONTEXT = ThreadLocal.withInitial(() -> Source.UNKNOWN);
    private static java.lang.ref.WeakReference<LocalPlayer> actor = new java.lang.ref.WeakReference<>(null);
    //#if MC >= 26.3
    private static LivingEntity.SwingDescription swing;
    //#else
    //$$ private static InteractionHand swingHand;
    //#endif
    private static Source source = Source.UNKNOWN;
    private static int started;

    private SwingVisualTracker() {}
    public static Source enter(Source next) { Source previous = CONTEXT.get(); CONTEXT.set(next); return previous; }
    public static void restore(Source previous) { if (previous == Source.UNKNOWN) CONTEXT.remove(); else CONTEXT.set(previous); }

    public static void started(LivingEntity entity)
    {
        Source origin = CONTEXT.get();
        if (entity != Minecraft.getInstance().player) return;
        if (origin == Source.UNKNOWN)
        {
            //#if MC < 26.3
            //$$ actor.clear(); swingHand = null; source = Source.UNKNOWN;
            //#endif
            return;
        }
        actor = new java.lang.ref.WeakReference<>((LocalPlayer)entity);
        //#if MC >= 26.3
        swing = entity.getCurrentSwing();
        //#else
        //$$ swingHand = entity.swingingArm;
        //#endif
        source = origin;
        started = entity.tickCount;
    }

    public static boolean hide(InteractionHand hand)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        //#if MC >= 26.3
        if (player == null || player != actor.get() || !player.isAlive() || swing == null ||
                !player.isSwinging() || player.getCurrentSwing() != swing ||
                player.tickCount < started || (long)player.tickCount - started > (long)swing.durationTicks() + 1)
        {
            actor.clear(); swing = null; source = Source.UNKNOWN;
            return false;
        }
        boolean matchesHand = swing.hand() == hand;
        //#else
        //$$ if (player == null || player != actor.get() || !player.isAlive() || swingHand == null ||
        //$$         !player.swinging || player.swingingArm != swingHand || player.tickCount < started)
        //$$ {
        //$$     actor.clear(); swingHand = null; source = Source.UNKNOWN;
        //$$     return false;
        //$$ }
        //$$ boolean matchesHand = swingHand == hand;
        //#endif
        return Configs.REMOVE_COOLDOWN_ANIMATIONS.getBooleanValue() && matchesHand &&
                (source == Source.ATTACK && Configs.REMOVE_ATTACK_SWING.getBooleanValue()
                        || source == Source.INTERACT && Configs.REMOVE_INTERACT_SWING.getBooleanValue());
    }
}
//#endif

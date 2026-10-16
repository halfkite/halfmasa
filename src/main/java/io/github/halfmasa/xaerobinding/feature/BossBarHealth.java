package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.LerpingBossEventAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;

/** Health is shown only for an authoritative association; progress is never multiplied by a guessed maximum. */
public final class BossBarHealth
{
    public record Health(float current, float maximum) {
        public boolean valid() { return Float.isFinite(current) && Float.isFinite(maximum) && current >= 0 && maximum > 0; }
    }
    private static final Map<UUID, LivingEntity> ENTITIES = new HashMap<>();
    private BossBarHealth() {}
    public static void clear() { ENTITIES.clear(); }
    public static void beginFrame()
    {
        ENTITIES.clear();
        var client = Minecraft.getInstance();
        if (!Configs.BETTER_BOSS_BAR.getBooleanValue() || !Configs.BOSS_BAR_SHOW_HEALTH.getBooleanValue() || client.level == null) return;
        for (var entity : client.level.entitiesForRendering())
            if (entity instanceof WitherBoss living) ENTITIES.put(entity.getUUID(), living);
    }
    private static Health health(LerpingBossEvent bar)
    {
        Health remote = HalfMasaSupportClient.health(bar.getId());
        if (remote != null && remote.valid()) return remote;
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        bar.getName().visit((style, text) -> {
            //#if MC >= 1.21.5
            if (style.getHoverEvent() instanceof HoverEvent.ShowEntity hover) ids.add(hover.entity().uuid);
            //#else
            //$$ if (style.getHoverEvent() != null)
            //$$ {
            //$$     var entity = style.getHoverEvent().getValue(HoverEvent.Action.SHOW_ENTITY);
            //$$     if (entity != null) ids.add(entity.id);
            //$$ }
            //#endif
            return java.util.Optional.empty();
        }, Style.EMPTY);
        if (ids.size() == 1)
        {
            var entity = ENTITIES.get(ids.iterator().next());
            if (entity != null && entity.isAlive())
            {
                var value = new Health(entity.getHealth(), entity.getMaxHealth());
                if (value.valid() && Float.compare(value.current / value.maximum,
                        ((LerpingBossEventAccessor)(Object)bar).halfmasa$getTargetPercent()) == 0) return value;
            }
        }
        HalfMasaSupportClient.queryHealth(bar.getId());
        return null;
    }
    public static Component label(LerpingBossEvent bar)
    {
        Component name = bar.getName();
        String value = value(bar);
        return value.isEmpty() ? name : Component.empty().append(name.copy()).append(Component.literal("  " + value));
    }
    public static String value(LerpingBossEvent bar)
    {
        if (!Configs.BETTER_BOSS_BAR.getBooleanValue() || !Configs.BOSS_BAR_SHOW_HEALTH.getBooleanValue()) return "";
        double current = 0, maximum = 0;
        boolean complete = true;
        for (var member : BossBarGrouper.members(bar))
        {
            Health value = health(member);
            if (value == null) { complete = false; continue; }
            current += value.current; maximum += value.maximum;
        }
        float progress = bar.getProgress();
        return complete ? number(current) + "/" + number(maximum)
                : Float.isFinite(progress) ? Math.round(Math.max(0, Math.min(1, progress)) * 100) + "%" : "?";
    }
    private static String number(double value)
    {
        return value > Long.MAX_VALUE ? String.format(java.util.Locale.ROOT, "%.3g", value)
                : value == Math.rint(value) ? Long.toString((long)value)
                : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
//#endif

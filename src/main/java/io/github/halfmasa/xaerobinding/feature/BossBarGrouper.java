package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.config.BossBarMergeMatchMode;
import io.github.halfmasa.xaerobinding.config.BossBarMergeProgressMode;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.mixin.LerpingBossEventAccessor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Builds a frame-local render view; never mutates the server's boss events. */
public final class BossBarGrouper
{
    private BossBarGrouper() {}

    private record TextCharacter(int codepoint, Style style) {}
    private record GroupKey(java.util.List<TextCharacter> name, BossEvent.BossBarColor color,
                            BossEvent.BossBarOverlay overlay, int healthBits, UUID unmergeableId) {}
    private static final Map<UUID, java.util.List<LerpingBossEvent>> MEMBERS = new java.util.HashMap<>();
    public static void clear() { MEMBERS.clear(); BossBarHealth.clear(); }

    public static java.util.List<LerpingBossEvent> members(LerpingBossEvent bar)
    { return MEMBERS.getOrDefault(bar.getId(), java.util.List.of(bar)); }

    private static java.util.List<TextCharacter> visibleName(Component name)
    {
        var characters = new ArrayList<TextCharacter>();
        name.visit((style, text) -> {
            Style visible = style.withHoverEvent(null).withClickEvent(null).withInsertion(null);
            text.codePoints().forEach(codepoint -> characters.add(new TextCharacter(codepoint, visible)));
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return java.util.List.copyOf(characters);
    }

    public static Collection<LerpingBossEvent> merge(Collection<LerpingBossEvent> original)
    {
        MEMBERS.clear();
        for (var bar : original) MEMBERS.put(bar.getId(), java.util.List.of(bar));
        BossBarHealth.beginFrame();
        if (!Configs.BETTER_BOSS_BAR.getBooleanValue() || !Configs.BOSS_BAR_MERGE.getBooleanValue() || original.size() < 2) return original;

        boolean allowDifferent = Configs.BOSS_BAR_MERGE_DIFFERENT_HEALTH.getBooleanValue();
        boolean nameOnly = Configs.BOSS_BAR_MERGE_MATCH_MODE.getOptionListValue() == BossBarMergeMatchMode.NAME;
        BossBarMergeProgressMode progressMode =
                (BossBarMergeProgressMode) Configs.BOSS_BAR_MERGE_PROGRESS_MODE.getOptionListValue();
        boolean showCount = Configs.BOSS_BAR_MERGE_SHOW_COUNT.getBooleanValue();
        int maxBars = Configs.BOSS_BAR_MERGE_MAX_BARS.getIntegerValue();
        Map<GroupKey, Group> groups = new LinkedHashMap<>();

        for (LerpingBossEvent bar : original)
        {
            float target = ((LerpingBossEventAccessor) (Object) bar).halfmasa$getTargetPercent();
            // Sample each animation once. The target is used only for membership.
            float progress = bar.getProgress();
            boolean valid = isValidProgress(target) && isValidProgress(progress);
            // +0 and -0 represent the same health. Invalid bars stay independent.
            int healthBits = allowDifferent || target == 0.0F ? 0 : Float.floatToIntBits(target);
            GroupKey key = new GroupKey(visibleName(bar.getName()), nameOnly ? null : bar.getColor(),
                    nameOnly ? null : bar.getOverlay(), healthBits, valid ? null : bar.getId());
            groups.computeIfAbsent(key, ignored -> new Group(bar)).add(bar, progress);
        }

        Collection<LerpingBossEvent> result = new ArrayList<>(groups.size());
        for (Group group : groups.values())
        {
            if (maxBars > 0 && result.size() >= maxBars) break;
            var rendered = group.renderEvent(progressMode, showCount);
            MEMBERS.put(rendered.getId(), java.util.List.copyOf(group.members));
            result.add(rendered);
        }
        return result;
    }

    private static boolean isValidProgress(float progress)
    {
        return Float.isFinite(progress) && progress >= 0.0F && progress <= 1.0F;
    }

    private static final class Group
    {
        private final LerpingBossEvent first;
        private int count;
        private final java.util.List<LerpingBossEvent> members = new ArrayList<>();
        private double sum;
        private float min = Float.POSITIVE_INFINITY;
        private float max = Float.NEGATIVE_INFINITY;

        private Group(LerpingBossEvent first) { this.first = first; }

        private void add(LerpingBossEvent bar, float progress)
        {
            this.members.add(bar);
            this.count++;
            this.sum += progress;
            this.min = Math.min(this.min, progress);
            this.max = Math.max(this.max, progress);
        }

        private LerpingBossEvent renderEvent(BossBarMergeProgressMode mode, boolean showCount)
        {
            if (this.count == 1) return this.first;
            float progress = switch (mode)
            {
                case AVERAGE -> (float) (this.sum / this.count);
                case MIN -> this.min;
                case MAX -> this.max;
            };
            Component name = this.first.getName();
            if (showCount) name = Component.empty().append(name.copy()).append(Component.literal(" ×" + this.count));
            // Initial and target progress are equal, so this view does not restart lerping.
            return new LerpingBossEvent(this.first.getId(), name, progress,
                    this.first.getColor(), this.first.getOverlay(), this.first.shouldDarkenScreen(),
                    this.first.shouldPlayBossMusic(), this.first.shouldCreateWorldFog());
        }
    }
}
//#endif

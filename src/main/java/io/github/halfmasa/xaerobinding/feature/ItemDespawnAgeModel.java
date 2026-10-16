package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.Collection;
import java.util.Locale;

/** Vanilla lifespan prediction; a missing sample is never evidence of immortality. */
public final class ItemDespawnAgeModel
{
    public static final int VANILLA_LIFESPAN = 6000;
    public static final int NEVER_DESPAWN = Short.MIN_VALUE;
    public enum Quality { SAMPLED, ESTIMATED, UNKNOWN, INFINITE }
    public record Reading(long remaining, Quality quality) {}
    private final long firstSeen;
    private Integer age;
    private long sampledAt;

    public ItemDespawnAgeModel(long firstSeen) { this.firstSeen = firstSeen; }

    public void accept(int age, long sampledAt)
    {
        this.age = age;
        this.sampledAt = sampledAt;
    }

    public Reading read(long tick, long freshness, boolean estimateOnly, boolean requireSample)
    {
        if (!estimateOnly && this.age != null)
        {
            long elapsed = Math.max(0, tick - this.sampledAt);
            if (elapsed <= freshness)
            {
                return this.age == NEVER_DESPAWN ? new Reading(0, Quality.INFINITE)
                        : new Reading(Math.max(0, VANILLA_LIFESPAN - (long) this.age - elapsed), Quality.SAMPLED);
            }
            if (requireSample || this.age == NEVER_DESPAWN) return new Reading(0, Quality.UNKNOWN);
            // Continue the last server prediction rather than resetting to first observation.
            return new Reading(Math.max(0, VANILLA_LIFESPAN - (long) this.age - elapsed), Quality.ESTIMATED);
        }
        return requireSample ? new Reading(0, Quality.UNKNOWN)
                : new Reading(Math.max(0, VANILLA_LIFESPAN - Math.max(0, tick - this.firstSeen)), Quality.ESTIMATED);
    }

    public static Reading combine(Collection<Reading> members, boolean requireSample)
    {
        if (members.isEmpty()) return new Reading(0, Quality.UNKNOWN);
        long minimum = Long.MAX_VALUE;
        boolean uncertain = false;
        for (Reading member : members)
        {
            if (member.quality == Quality.UNKNOWN) uncertain = true;
            else if (member.quality != Quality.INFINITE)
            {
                minimum = Math.min(minimum, member.remaining);
                uncertain |= member.quality == Quality.ESTIMATED;
            }
        }
        if (minimum == Long.MAX_VALUE) return new Reading(0, uncertain ? Quality.UNKNOWN : Quality.INFINITE);
        if (uncertain && requireSample) return new Reading(0, Quality.UNKNOWN);
        return new Reading(minimum, uncertain ? Quality.ESTIMATED : Quality.SAMPLED);
    }

    public static String format(Reading reading, String format)
    {
        if (reading.quality == Quality.UNKNOWN) return "--:--";
        if (reading.quality == Quality.INFINITE) return "∞";
        long seconds = (reading.remaining + 19) / 20;
        String time = switch (format)
        {
            case "ticks" -> reading.remaining + "t";
            case "seconds" -> seconds + "s";
            default -> String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
        };
        return reading.quality == Quality.ESTIMATED ? "≈" + time : time;
    }
}
//#endif

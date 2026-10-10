package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** A local draft; opening, toggling and resetting never write the live config. */
public final class IgnoredKeySelection
{
    //#if MC >= 26.3
    // SDL keyboard scancodes and the shared negative mouse-button codes.
    public static final String DEFAULT_KEYS = "-3,-2,-1,4,7,22,26,224,225,226,228,229,230";
    //#else
    //$$ public static final String DEFAULT_KEYS = "-3,-2,-1,65,68,83,87,340,341,342,344,345,346";
    //#endif
    private final Set<Integer> original;
    private final Set<Integer> defaults;
    private final Set<Integer> selected;

    public IgnoredKeySelection(String original, String defaults)
    {
        this.original = Set.copyOf(parse(original));
        this.defaults = Set.copyOf(parse(defaults));
        this.selected = new TreeSet<>(this.original);
    }

    public static Set<Integer> parse(String value)
    {
        Set<Integer> keys = new TreeSet<>();
        for (String token : value.split("[,;\\s]+"))
        {
            try
            {
                int code = Integer.parseInt(token);
                if (code != 0) keys.add(code);
            }
            catch (NumberFormatException ignored) {}
        }
        return keys;
    }

    public static boolean isIgnored(int code, Collection<Integer> keys, boolean inverted)
    {
        return inverted != keys.contains(code);
    }

    public static String migrateDefaults(String value)
    {
        Set<Integer> keys = parse(value);
        return keys.equals(Set.of(26, 4, 22, 7, 225)) || keys.equals(Set.of(87, 65, 83, 68, 340))
                ? DEFAULT_KEYS : value;
    }

    public Set<Integer> keys()
    {
        return Set.copyOf(this.selected);
    }

    public void toggle(int code)
    {
        if (code != 0 && !this.selected.add(code)) this.selected.remove(code);
    }

    public void reset()
    {
        this.selected.clear();
        this.selected.addAll(this.defaults);
    }

    public boolean hasChanges()
    {
        return !this.selected.equals(this.original);
    }

    public String serialize()
    {
        return this.selected.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
//#endif

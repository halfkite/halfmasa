package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 26.3
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

public final class KeymapKeyboardStyle
{
    public static final int BOUND = 0xB02D6942;
    public static final int COMBINATION_DOT = 0xFFB57AEE;
    public static final int IGNORED_DOT = 0xFFA3A2AD;
    public static final int UNBOUND = 0x66606060;
    public static final int VANILLA_DOT = 0xFF55A7FF;
    public static final int MALILIB_DOT = 0xFFFFAB48;
    public static final int SELECTED_BORDER = 0xFFFFD36A;
    public static final int SINGLE_CONFLICT = 0xFFE09090;
    public static final int COMBINATION_CONFLICT = 0xFFB04040;

    private KeymapKeyboardStyle() {}

    public record Binding(List<Integer> keys, boolean vanilla, boolean conflicted)
    {
        public Binding { keys = List.copyOf(keys); }
        public Binding(List<Integer> keys, boolean vanilla) { this(keys, vanilla, false); }
    }

    public record Indicators(boolean vanilla, boolean malilib, boolean combination, boolean ignored,
            boolean singleConflict, boolean combinationConflict)
    {
        public Indicators(boolean vanilla, boolean malilib, boolean combination, boolean ignored)
        {
            this(vanilla, malilib, combination, ignored, false, false);
        }

        public int fill()
        {
            return this.vanilla || this.malilib ? BOUND : UNBOUND;
        }
    }

    /** Keep the browser's existing shared-key conflict classification in both keyboards. */
    public static List<Binding> withConflicts(List<Binding> bindings)
    {
        Map<Integer, Integer> uses = new HashMap<>();
        for (Binding binding : bindings)
        {
            for (int code : new HashSet<>(binding.keys())) uses.merge(code, 1, Integer::sum);
        }
        return bindings.stream().map(binding -> new Binding(binding.keys(), binding.vanilla(),
                binding.keys().stream().anyMatch(code -> uses.getOrDefault(code, 0) > 1))).toList();
    }

    public static Indicators indicators(List<Binding> bindings, List<Integer> codes, boolean ignored)
    {
        boolean vanilla = false;
        boolean malilib = false;
        boolean combination = false;
        boolean singleConflict = false;
        boolean combinationConflict = false;
        for (Binding binding : bindings)
        {
            if (binding.keys().containsAll(codes))
            {
                if (binding.vanilla()) vanilla = true;
                else malilib = true;
                if (binding.keys().size() > 1) combination = true;
                if (binding.conflicted())
                {
                    if (binding.keys().size() > 1) combinationConflict = true;
                    else singleConflict = true;
                }
            }
        }
        return new Indicators(vanilla, malilib, combination, ignored, singleConflict, combinationConflict);
    }
}
//#endif

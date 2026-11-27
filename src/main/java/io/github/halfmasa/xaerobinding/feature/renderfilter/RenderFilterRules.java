package io.github.halfmasa.xaerobinding.feature.renderfilter;

import java.util.*;
import java.util.function.Function;

/** Pure immutable rules. A blacklist is a set of exceptions to hiding. */
public final class RenderFilterRules {
    private RenderFilterRules() {}
    public enum Mode { WHITELIST, BLACKLIST, NONE;
        public Mode next() { return values()[(ordinal()+1)%values().length]; }
    }
    public record Group(String id, String name, boolean enabled, Mode mode, String hotkey,
                        List<String> whitelist, List<String> blacklist) {
        public Group {
            Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(mode);
            hotkey = Objects.requireNonNullElse(hotkey, "");
            whitelist = List.copyOf(whitelist == null ? List.of() : whitelist);
            blacklist = List.copyOf(blacklist == null ? List.of() : blacklist);
        }
        public static Group create(String name, boolean enabled) {
            return new Group(UUID.randomUUID().toString(), name, enabled, Mode.WHITELIST, "", List.of(), List.of());
        }
        public Group toggle() { return new Group(id,name,!enabled,mode,hotkey,whitelist,blacklist); }
        public Group withMode(Mode m) { return new Group(id,name,enabled,m,hotkey,whitelist,blacklist); }
        public List<String> activeList() { return mode == Mode.BLACKLIST ? blacklist : whitelist; }
        public Group withList(List<String> list) {
            return new Group(id,name,enabled,mode,hotkey,mode==Mode.WHITELIST?list:whitelist,mode==Mode.BLACKLIST?list:blacklist);
        }
    }
    public record Snapshot<T>(boolean inverse, Set<T> values) {
        public Snapshot { values = Set.copyOf(values); }
        public static <T> Snapshot<T> empty() { return new Snapshot<>(false, Set.of()); }
        public boolean hides(T value) { return inverse != values.contains(value); }
        public boolean active() { return inverse || !values.isEmpty(); }
    }
    public static <T> Snapshot<T> compile(List<Group> groups, Function<String, Optional<T>> resolve) {
        Set<T> hidden = new HashSet<>(); Set<T> exceptions = null;
        for (Group g : groups) {
            if (!g.enabled()) continue;
            if (g.mode() == Mode.NONE) return new Snapshot<>(true,Set.of());
            Set<T> resolved = new HashSet<>();
            for (String id : g.activeList()) resolve.apply(id).ifPresent(resolved::add);
            if (g.mode() == Mode.WHITELIST) hidden.addAll(resolved);
            else if (exceptions == null) exceptions = resolved;
            else exceptions.retainAll(resolved);
        }
        if (exceptions == null) return new Snapshot<>(false,hidden);
        exceptions.removeAll(hidden);
        return new Snapshot<>(true,exceptions);
    }
}

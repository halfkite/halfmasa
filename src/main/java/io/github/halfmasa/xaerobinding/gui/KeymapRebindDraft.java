package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.List;

/** Ordered input preview; only an explicit confirm applies these keys to a binding. */
public final class KeymapRebindDraft
{
    private final List<Integer> keys = new ArrayList<>();
    private boolean touched;
    public void reset() { this.keys.clear(); this.touched = false; }
    public List<Integer> keys() { return List.copyOf(this.keys); }
    public boolean touched() { return this.touched; }
    public void clear() { this.keys.clear(); this.touched = true; }
    public void press(int code)
    {
        if (code != 0 && !this.keys.contains(code)) { this.keys.add(code); this.touched = true; }
    }
    public void toggle(List<Integer> codes)
    {
        if (this.keys.containsAll(codes)) this.keys.removeAll(codes);
        else for (int code : codes) if (code != 0 && !this.keys.contains(code)) this.keys.add(code);
        this.touched = true;
    }
}
//#endif

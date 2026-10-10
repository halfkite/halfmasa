package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Packs projected nameplate rectangles without depending on world axes or entity type. */
public final class AggregationLabelLayout
{
    public record Label(int id, double x, double y, double width, double height, double distance) {}
    public record Placement(int id, double offsetY) {}
    private record Range(double start, double end) {}
    private AggregationLabelLayout() {}

    public static List<Placement> arrange(List<Label> labels, double gap)
    {
        var ordered = new ArrayList<>(labels);
        ordered.sort(Comparator.comparingDouble(Label::distance).thenComparingInt(Label::id));
        var placed = new ArrayList<Label>();
        var result = new ArrayList<Placement>();
        for (Label label : ordered)
        {
            var blocked = new ArrayList<Range>();
            for (Label other : placed)
            {
                if (Math.abs(label.x() - other.x()) < (label.width() + other.width()) / 2 + gap)
                    blocked.add(new Range(other.y() - label.height() - gap, other.y() + other.height() + gap));
            }
            blocked.sort(Comparator.comparingDouble(Range::start));
            var merged = new ArrayList<Range>();
            for (Range range : blocked)
            {
                if (!merged.isEmpty() && range.start() <= merged.getLast().end())
                {
                    Range previous = merged.removeLast();
                    merged.add(new Range(previous.start(), Math.max(previous.end(), range.end())));
                }
                else merged.add(range);
            }
            double y = label.y();
            for (Range range : merged)
            {
                if (y > range.start() && y < range.end())
                {
                    y = y - range.start() <= range.end() - y ? range.start() : range.end();
                    break;
                }
            }
            placed.add(new Label(label.id(), label.x(), y, label.width(), label.height(), label.distance()));
            result.add(new Placement(label.id(), y - label.y()));
        }
        return List.copyOf(result);
    }
}
//#endif

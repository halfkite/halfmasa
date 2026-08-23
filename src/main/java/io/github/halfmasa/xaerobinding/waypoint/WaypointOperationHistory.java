package io.github.halfmasa.xaerobinding.waypoint;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

final class WaypointOperationHistory
{
    private static final int MAX_STEPS = 5;
    private final Deque<Entry> undo = new ArrayDeque<>();
    private final Deque<Entry> redo = new ArrayDeque<>();
    private Object context;

    void record(
            String operationTranslationKey,
            WaypointBundleService.Snapshot before,
            WaypointBundleService.Snapshot after)
    {
        this.ensureContext(before);
        if (WaypointBundleService.sameState(before, after))
        {
            return;
        }

        this.undo.addFirst(new Entry(operationTranslationKey, before, after));
        while (this.undo.size() > MAX_STEPS)
        {
            this.undo.removeLast();
        }
        this.redo.clear();
    }

    HistoryResult undo() throws IOException
    {
        WaypointBundleService.Snapshot current = WaypointBundleService.captureSnapshot();
        this.ensureContext(current);
        Entry entry = this.undo.peekFirst();
        if (entry == null)
        {
            throw new IOException("halfmasa.error.waypoint_nothing_to_undo");
        }
        if (!WaypointBundleService.sameState(current, entry.after()))
        {
            this.clear();
            throw new IOException("halfmasa.error.waypoint_history_diverged");
        }

        WaypointBundleService.restoreSnapshot(entry.before());
        this.undo.removeFirst();
        this.redo.addFirst(entry);
        return new HistoryResult(entry.operationTranslationKey(), this.undo.size(), this.redo.size());
    }

    HistoryResult redo() throws IOException
    {
        WaypointBundleService.Snapshot current = WaypointBundleService.captureSnapshot();
        this.ensureContext(current);
        Entry entry = this.redo.peekFirst();
        if (entry == null)
        {
            throw new IOException("halfmasa.error.waypoint_nothing_to_redo");
        }
        if (!WaypointBundleService.sameState(current, entry.before()))
        {
            this.clear();
            throw new IOException("halfmasa.error.waypoint_history_diverged");
        }

        WaypointBundleService.restoreSnapshot(entry.after());
        this.redo.removeFirst();
        this.undo.addFirst(entry);
        return new HistoryResult(entry.operationTranslationKey(), this.undo.size(), this.redo.size());
    }

    private void ensureContext(WaypointBundleService.Snapshot snapshot)
    {
        if (this.context != snapshot.root())
        {
            this.clear();
            this.context = snapshot.root();
        }
    }

    private void clear()
    {
        this.undo.clear();
        this.redo.clear();
    }

    record HistoryResult(String operationTranslationKey, int undoSteps, int redoSteps) {}

    private record Entry(
            String operationTranslationKey,
            WaypointBundleService.Snapshot before,
            WaypointBundleService.Snapshot after) {}
}

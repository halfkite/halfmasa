package io.github.halfmasa.xaerobinding.better_multiplayer.privacy;

import io.github.halfmasa.xaerobinding.better_multiplayer.BetterMultiplayerConfig;

/** Unknown and stale observations never become a positive idle confirmation. */
public final class PrivacyManager {
    public enum State { CHECKING, IDLE, CAPTURING, UNKNOWN, UNAVAILABLE }
    private volatile State state = State.CHECKING;
    private volatile boolean captureLatch;
    private volatile boolean uncertaintyLatch;
    public synchronized void observe(State observation) {
        state = observation;
        if (observation == State.CAPTURING) captureLatch = true;
        if (observation == State.UNKNOWN && BetterMultiplayerConfig.HIDE_UNKNOWN.getBooleanValue()) uncertaintyLatch = true;
        if (observation == State.IDLE && BetterMultiplayerConfig.AUTO_RESTORE.getBooleanValue()) { captureLatch = false; uncertaintyLatch = false; }
    }
    public synchronized void reset() { state = State.CHECKING; captureLatch = false; uncertaintyLatch = false; }
    public boolean hidden() {
        if (!BetterMultiplayerConfig.active()) return false;
        if (BetterMultiplayerConfig.MANUAL_PRIVACY.getBooleanValue()) return true;
        if (!BetterMultiplayerConfig.AUTO_HIDE.getBooleanValue()) return false;
        return state == State.CAPTURING || state == State.CHECKING || captureLatch ||
                (state == State.UNKNOWN || uncertaintyLatch) && BetterMultiplayerConfig.HIDE_UNKNOWN.getBooleanValue();
    }
    public State state() { return state; }
}

package io.github.halfmasa.xaerobinding.better_multiplayer.privacy;

import java.util.concurrent.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.BetterMultiplayerConfig;

public final class CaptureDetectionManager {
    public static final CaptureDetectionManager INSTANCE = new CaptureDetectionManager();
    public final PrivacyManager privacy = new PrivacyManager();
    private ScheduledExecutorService worker;
    private volatile ObsCaptureDetector obs;
    private String settings = "";
    private volatile long generation;
    private volatile long observedAt;
    private volatile boolean wasCapturing;
    public synchronized void update() {
        if (!BetterMultiplayerConfig.active() || !BetterMultiplayerConfig.AUTO_HIDE.getBooleanValue()) { stop(); return; }
        String signature = BetterMultiplayerConfig.DETECTION_MODE.getStringValue() + '\n' + BetterMultiplayerConfig.OBS_URL.getStringValue()
                + '\n' + BetterMultiplayerConfig.OBS_PASSWORD.getStringValue() + '\n' + BetterMultiplayerConfig.DETECTION_INTERVAL.getIntegerValue();
        if (worker == null || !signature.equals(settings)) {
            stop(); settings = signature; privacy.reset(); privacy.observe(PrivacyManager.State.CHECKING);
            long token = ++generation;
            worker = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "halfmasa-capture-status"); t.setDaemon(true); return t; });
            worker.scheduleWithFixedDelay(() -> detect(token), 0, BetterMultiplayerConfig.DETECTION_INTERVAL.getIntegerValue(), TimeUnit.MILLISECONDS);
        }
        ObsCaptureDetector detector = obs;
        if (detector != null && detector.eventActive()) privacy.observe(PrivacyManager.State.CAPTURING);
        long freshnessMillis = Math.max(10000L, 3L * BetterMultiplayerConfig.DETECTION_INTERVAL.getIntegerValue());
        if (observedAt != 0 && System.nanoTime() - observedAt > TimeUnit.MILLISECONDS.toNanos(freshnessMillis)) privacy.observe(PrivacyManager.State.UNKNOWN);
    }
    private void detect(long token) {
        if (token != generation) return;
        var mode = BetterMultiplayerConfig.DETECTION_MODE.getOptionListValue();
        var processes = new WindowsCaptureDetector().detect();
        PrivacyManager.State state;
        if (mode == BetterMultiplayerConfig.Mode.PROCESS) {
            state = processes.obs() || processes.other() || processes.failed() ? PrivacyManager.State.UNKNOWN : PrivacyManager.State.IDLE;
        } else {
            ObsCaptureDetector detector = obs;
            if (detector == null || detector.closed()) {
                if (detector != null) detector.close();
                synchronized (this) {
                    if (token != generation) return;
                    detector = new ObsCaptureDetector(BetterMultiplayerConfig.OBS_PASSWORD.getStringValue(), () -> {
                        if (token == generation) { wasCapturing = true; privacy.observe(PrivacyManager.State.CAPTURING); }
                    }); obs = detector;
                }
            }
            state = detector.poll();
            if (state == PrivacyManager.State.UNKNOWN && !processes.obs() && !processes.failed() && !wasCapturing && mode == BetterMultiplayerConfig.Mode.HYBRID)
                state = PrivacyManager.State.IDLE;
            if (mode == BetterMultiplayerConfig.Mode.HYBRID && (processes.other() || processes.failed()) && state != PrivacyManager.State.CAPTURING)
                state = PrivacyManager.State.UNKNOWN;
        }
        if (token != generation) return;
        wasCapturing |= state == PrivacyManager.State.CAPTURING;
        final PrivacyManager.State observed = state;
        ObsCaptureDetector detector = obs;
        if (mode != BetterMultiplayerConfig.Mode.PROCESS && detector != null) detector.publish(observed, value -> {
            if (token == generation) privacy.observe(value);
        });
        else privacy.observe(state);
        observedAt = System.nanoTime();
    }
    public synchronized void stop() {
        if (worker == null) return;
        generation++; worker.shutdownNow(); worker = null;
        var old = obs; obs = null;
        if (old != null) { // HttpClient.close may await its selector: never block a render tick.
            Thread.ofVirtual().start(old::close);
        }
        observedAt = 0; wasCapturing = false; privacy.reset();
    }
}

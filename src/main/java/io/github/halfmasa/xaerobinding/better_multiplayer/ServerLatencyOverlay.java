package io.github.halfmasa.xaerobinding.better_multiplayer;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

public final class ServerLatencyOverlay {
    public static final class Probe {
        final long started = System.nanoTime();
        Long milliseconds;
    }
    public record Label(Component text, int color) {}
    private static final Map<ServerData, Probe> PROBES = new WeakHashMap<>();
    public static synchronized Probe begin(ServerData server) { Probe probe = new Probe(); PROBES.put(server, probe); return probe; }
    public static synchronized void invalidate(ServerData server) { PROBES.remove(server); }
    public static synchronized void complete(ServerData server, Probe probe) {
        if (PROBES.get(server) == probe && server.ping >= 0) probe.milliseconds = server.ping;
    }
    public static synchronized Label label(ServerData server) {
        if (server.state() == ServerData.State.INITIAL) return status("initial");
        Probe probe = PROBES.get(server);
        if (server.state() == ServerData.State.UNREACHABLE) return status("unreachable");
        boolean incompatible = server.state() == ServerData.State.INCOMPATIBLE;
        if (probe != null && probe.milliseconds != null) {
            long ping = probe.milliseconds;
            int green = BetterMultiplayerConfig.GREEN_LIMIT.getIntegerValue();
            int yellow = Math.max(green, BetterMultiplayerConfig.YELLOW_LIMIT.getIntegerValue());
            int color = ping <= green ? 0xff55ff55 : ping <= yellow ? 0xffffff55 : 0xffff5555;
            Component text = Component.literal(ping + " ms");
            return new Label(text, color);
        }
        if (probe != null && System.nanoTime() - probe.started >= TimeUnit.MILLISECONDS.toNanos(BetterMultiplayerConfig.PING_TIMEOUT.getIntegerValue())) return status("timeout");
        return status(incompatible ? "incompatible" : "pinging");
    }
    private static Label status(String key) { return new Label(Component.translatable("halfmasa.multiplayer.latency." + key), 0xffaaaaaa); }
}

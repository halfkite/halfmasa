package io.github.halfmasa.xaerobinding.better_multiplayer.privacy;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import io.github.halfmasa.xaerobinding.better_multiplayer.BetterMultiplayerConfig;

/** OBS 5.x, read-only requests. Credentials and responses never enter logs. */
public final class ObsCaptureDetector implements WebSocket.Listener, AutoCloseable {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final Map<String, CompletableFuture<JsonObject>> requests = new ConcurrentHashMap<>();
    private final CompletableFuture<Void> identified = new CompletableFuture<>();
    private final StringBuilder fragments = new StringBuilder();
    private final String password;
    private volatile WebSocket socket;
    private volatile boolean closed;
    private volatile boolean eventActive;
    private long eventVersion;
    private final Runnable onCapture;
    public ObsCaptureDetector(String password) { this(password, () -> {}); }
    public ObsCaptureDetector(String password, Runnable onCapture) { this.password = password; this.onCapture = onCapture; }
    public PrivacyManager.State poll() {
        try {
            if (socket == null) {
                URI endpoint = URI.create(BetterMultiplayerConfig.OBS_URL.getStringValue());
                if (!("ws".equals(endpoint.getScheme()) || "wss".equals(endpoint.getScheme())) || endpoint.getUserInfo() != null)
                    return PrivacyManager.State.UNKNOWN;
                socket = client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(2)).buildAsync(endpoint, this).get(2500, TimeUnit.MILLISECONDS);
            }
            identified.get(2500, TimeUnit.MILLISECONDS);
            // Send all queries before waiting: this is one coherent poll, not three UI-thread calls.
            long version;
            synchronized (this) { version = eventVersion; }
            var recording = request("GetRecordStatus");
            var streaming = request("GetStreamStatus");
            var replay = request("GetReplayBufferStatus");
            boolean active = active(recording.get(2, TimeUnit.SECONDS)) | active(streaming.get(2, TimeUnit.SECONDS)) |
                    active(replay.get(2, TimeUnit.SECONDS));
            synchronized (this) {
                eventActive = active || eventVersion != version;
                return eventActive ? PrivacyManager.State.CAPTURING : PrivacyManager.State.IDLE;
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return PrivacyManager.State.UNKNOWN;
        } catch (ExecutionException | TimeoutException | IllegalArgumentException error) {
            fail(error);
            if (socket != null) socket.abort();
            return PrivacyManager.State.UNKNOWN;
        } finally {
            requests.entrySet().removeIf(e -> { if (!e.getValue().isDone()) e.getValue().cancel(false); return true; });
        }
    }
    public boolean eventActive() { return eventActive; }
    public synchronized void publish(PrivacyManager.State state, java.util.function.Consumer<PrivacyManager.State> receiver) {
        receiver.accept(eventActive ? PrivacyManager.State.CAPTURING : state);
    }
    private static boolean active(JsonObject data) {
        if (!data.has("outputActive") || !data.get("outputActive").isJsonPrimitive())
            throw new IllegalArgumentException("Missing output state");
        return data.get("outputActive").getAsBoolean();
    }
    private CompletableFuture<JsonObject> request(String type) {
        String id = UUID.randomUUID().toString();
        var future = new CompletableFuture<JsonObject>();
        requests.put(id, future);
        JsonObject data = new JsonObject();
        data.addProperty("requestType", type); data.addProperty("requestId", id);
        send(6, data);
        return future;
    }
    private void send(int opcode, JsonObject data) {
        JsonObject message = new JsonObject(); message.addProperty("op", opcode); message.add("d", data);
        socket.sendText(message.toString(), true).whenComplete((ignored, error) -> { if (error != null) fail(error); });
    }
    @Override public void onOpen(WebSocket webSocket) { socket = webSocket; webSocket.request(1); }
    @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence text, boolean last) {
        fragments.append(text);
        if (fragments.length() > 1024 * 1024) { fail(new IllegalArgumentException("Oversized OBS message")); webSocket.abort(); return null; }
        if (last) {
            try {
                JsonObject message = JsonParser.parseString(fragments.toString()).getAsJsonObject();
                JsonObject data = message.getAsJsonObject("d");
                switch (message.get("op").getAsInt()) {
                    case 0 -> {
                        JsonObject identify = new JsonObject(); identify.addProperty("rpcVersion", 1);
                        identify.addProperty("eventSubscriptions", 1 << 6); // Outputs
                        if (data.has("authentication")) {
                            JsonObject auth = data.getAsJsonObject("authentication");
                            String secret = hash(password + auth.get("salt").getAsString());
                            identify.addProperty("authentication", hash(secret + auth.get("challenge").getAsString()));
                        }
                        send(1, identify);
                    }
                    case 2 -> identified.complete(null);
                    case 5 -> {
                        String type = data.get("eventType").getAsString();
                        if ((type.equals("RecordStateChanged") || type.equals("StreamStateChanged") || type.equals("ReplayBufferStateChanged"))
                                && active(data.getAsJsonObject("eventData"))) {
                            synchronized (this) { eventVersion++; eventActive = true; onCapture.run(); }
                        }
                        // Only a full successful status poll may clear the privacy latch.
                    }
                    case 7 -> {
                        var future = requests.remove(data.get("requestId").getAsString());
                        if (future != null) {
                            if (data.getAsJsonObject("requestStatus").get("result").getAsBoolean()) future.complete(data.getAsJsonObject("responseData"));
                            else future.completeExceptionally(new IllegalStateException("OBS status unavailable"));
                        }
                    }
                    default -> { }
                }
            } catch (RuntimeException error) { fail(error); }
            finally { fragments.setLength(0); }
        }
        webSocket.request(1);
        return null;
    }
    private static String hash(String value) {
        try { return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private void fail(Throwable error) {
        identified.completeExceptionally(error);
        requests.values().forEach(f -> f.completeExceptionally(error));
        closed = true;
    }
    @Override public void onError(WebSocket socket, Throwable error) { fail(error); }
    @Override public CompletionStage<?> onClose(WebSocket socket, int code, String reason) { fail(new IllegalStateException("OBS disconnected")); return null; }
    public boolean closed() { return closed; }
    @Override public void close() { closed = true; if (socket != null) socket.abort(); client.close(); }
}

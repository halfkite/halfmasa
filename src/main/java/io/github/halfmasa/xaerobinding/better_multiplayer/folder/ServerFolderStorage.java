package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

public final class ServerFolderStorage {
    public static final class Assignment {
        public String entryId;
        public String folderId;
        public int order;
        public String lastName;
        public String lastAddress;
        public int lastIndex;
        public Assignment(String id, String name, String address, int index) {
            entryId = id; lastName = name; lastAddress = address; lastIndex = index; order = index;
        }
    }
    public static final class Document {
        public int schemaVersion = 1;
        public List<ServerFolder> folders = new ArrayList<>();
        public List<Assignment> serverAssignments = new ArrayList<>();
        public List<String> lastActiveIds = new ArrayList<>();
    }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private boolean writable = true;
    private String error = "";
    public ServerFolderStorage(Path file) { this.file = file; }
    public Document load() {
        if (!Files.exists(file)) return new Document();
        try {
            if (Files.size(file) > 8 * 1024 * 1024) throw new IOException("Folder file exceeds size limit");
            Document data = GSON.fromJson(Files.readString(file), Document.class);
            if (data == null || data.schemaVersion != 1 || data.folders == null || data.serverAssignments == null)
                throw new IOException("Unsupported folder data schema");
            return data;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            writable = false; error = "halfmasa.multiplayer.storage_unavailable";
            XaeroWorldBinding.LOGGER.error("Unable to read server folders; original file preserved", e);
            return new Document();
        }
    }
    public boolean save(Document data) {
        if (!writable) return false;
        Path temp = null;
        try {
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(), "server-folders-", ".tmp");
            Files.writeString(temp, GSON.toJson(data), StandardCharsets.UTF_8);
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
            error = ""; return true;
        } catch (IOException e) {
            error = "halfmasa.multiplayer.storage_unavailable";
            XaeroWorldBinding.LOGGER.error("Unable to save server folders", e); return false;
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException e) { XaeroWorldBinding.LOGGER.warn("Unable to remove folder temporary file", e); }
        }
    }
    public String error() { return error; }
}

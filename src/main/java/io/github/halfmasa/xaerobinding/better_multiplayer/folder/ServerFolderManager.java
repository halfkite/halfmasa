package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ServerData;
import io.github.halfmasa.xaerobinding.better_multiplayer.folder.ServerFolderStorage.*;

/** Canonical servers remain in servers.dat. This model only owns organization. */
public final class ServerFolderManager {
    private static final class Holder {
        static final ServerFolderManager INSTANCE = new ServerFolderManager(new ServerFolderStorage(
                FabricLoader.getInstance().getConfigDir().resolve("halfmasa/server-folders.json")));
    }
    public static ServerFolderManager getInstance() { return Holder.INSTANCE; }
    public record Node(String id, boolean folder, int depth, String path) {}
    private final ServerFolderStorage storage;
    private final Document data;
    private final Map<ServerData, String> identities = new IdentityHashMap<>();
    private final Map<String, ServerData> active = new LinkedHashMap<>();
    private long revision;
    public ServerFolderManager(ServerFolderStorage storage) {
        this.storage = storage; data = storage.load(); validate();
    }
    private void validate() {
        if (data.lastActiveIds == null) data.lastActiveIds = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        data.folders.removeIf(f -> f == null || f.id == null || !ids.add(f.id));
        for (ServerFolder f : data.folders) {
            f.name = cleanName(f.name);
            if (f.parentId != null && (!ids.contains(f.parentId) || isDescendant(f.parentId, f.id))) f.parentId = null;
        }
        ids.clear();
        data.serverAssignments.removeIf(a -> a == null || a.entryId == null || !ids.add(a.entryId));
        for (Assignment a : data.serverAssignments) {
            if (folder(a.folderId) == null) a.folderId = null;
            if (a.lastName == null) a.lastName = "";
            if (a.lastAddress == null) a.lastAddress = "";
        }
    }
    public void reconcile(List<ServerData> servers) {
        Map<String, ServerData> previous = new LinkedHashMap<>(active);
        active.clear(); Set<String> used = new HashSet<>();
        // Reserve live objects first; edits made through the game retain their UUID.
        for (ServerData server : servers) {
            String id = identities.get(server);
            if (assignment(id) != null && used.add(id)) active.put(id, server);
        }
        boolean unchangedSnapshot = data.lastActiveIds.size() == servers.size();
        if (unchangedSnapshot) for (int i = 0; i < servers.size(); i++) {
            Assignment a = assignment(data.lastActiveIds.get(i));
            if (a == null || !same(a, servers.get(i))) { unchangedSnapshot = false; break; }
        }
        for (int i = 0; i < servers.size(); i++) {
            ServerData server = servers.get(i);
            if (active.containsValue(server)) continue;
            List<Assignment> matches = matches(a -> same(a, server), used);
            long identicalInputs = servers.stream().filter(s -> s.name.equals(server.name) && s.ip.equals(server.ip)).count();
            Assignment match = matches.size() == 1 && identicalInputs == 1 ? matches.getFirst() : null;
            if (match == null && unchangedSnapshot) {
                Assignment saved = assignment(data.lastActiveIds.get(i));
                if (saved != null && !used.contains(saved.entryId)) match = saved;
            }
            // Rename/address edits outside the game are inferred only when unique on both sides.
            if (match == null && matches.isEmpty()) {
                List<Assignment> address = matches(a -> a.lastAddress.equals(server.ip), used);
                if (address.size() == 1 && servers.stream().filter(s -> s.ip.equals(server.ip)).count() == 1) match = address.getFirst();
                if (match == null) {
                    List<Assignment> name = matches(a -> a.lastName.equals(server.name), used);
                    if (name.size() == 1 && servers.stream().filter(s -> s.name.equals(server.name)).count() == 1) match = name.getFirst();
                }
            }
            if (match == null) {
                match = new Assignment(UUID.randomUUID().toString(), server.name, server.ip, i);
                match.order = nextOrder(null); data.serverAssignments.add(match);
            }
            used.add(match.entryId); active.put(match.entryId, server); identities.put(server, match.entryId);
        }
        boolean changed = !previous.equals(active);
        for (int i = 0; i < servers.size(); i++) {
            ServerData server = servers.get(i); Assignment a = assignment(id(server));
            changed |= !same(a, server) || a.lastIndex != i;
            a.lastName = server.name; a.lastAddress = server.ip; a.lastIndex = i;
        }
        identities.keySet().removeIf(s -> !servers.contains(s));
        List<String> snapshot = servers.stream().map(this::id).toList();
        changed |= !data.lastActiveIds.equals(snapshot);
        data.lastActiveIds = snapshot;
        if (changed) persist();
    }
    private List<Assignment> matches(Predicate<Assignment> test, Set<String> used) {
        return data.serverAssignments.stream().filter(a -> !used.contains(a.entryId) && test.test(a)).toList();
    }
    private static boolean same(Assignment a, ServerData s) { return a.lastName.equals(s.name) && a.lastAddress.equals(s.ip); }
    public String id(ServerData server) { return active.entrySet().stream().filter(e -> e.getValue() == server).map(Map.Entry::getKey).findFirst().orElse(null); }
    public ServerData server(String id) { return active.get(id); }
    public ServerFolder folder(String id) { return id == null ? null : data.folders.stream().filter(f -> f.id.equals(id)).findFirst().orElse(null); }
    public Assignment assignment(String id) { return id == null ? null : data.serverAssignments.stream().filter(a -> a.entryId.equals(id)).findFirst().orElse(null); }
    public List<ServerFolder> folders() { return List.copyOf(data.folders); }
    public List<Assignment> unresolved() { return data.serverAssignments.stream().filter(a -> !active.containsKey(a.entryId)).toList(); }
    public String create(String name, String parent) {
        if (parent != null && folder(parent) == null) return null;
        String id = UUID.randomUUID().toString();
        data.folders.add(new ServerFolder(id, cleanName(name), parent, nextOrder(parent))); persist(); return id;
    }
    public void rename(String id, String name) { ServerFolder f = folder(id); if (f != null) { f.name = cleanName(name); persist(); } }
    public void toggle(String id) { ServerFolder f = folder(id); if (f != null) { f.expanded = !f.expanded; persist(); } }
    public void delete(String id) {
        ServerFolder f = folder(id); if (f == null) return;
        int order = nextOrder(f.parentId);
        for (String child : children(id)) { setParent(child, f.parentId); setOrder(child, order++); }
        // Also preserve missing servers' assignments when deleting their parent.
        for (Assignment a : data.serverAssignments) if (Objects.equals(a.folderId, id)) { a.folderId = f.parentId; a.order = order++; }
        data.folders.remove(f); persist();
    }
    public boolean move(String id, String parent, String before) {
        if (folder(id) == null && assignment(id) == null || parent != null && folder(parent) == null || id == null || id.equals(parent) || isDescendant(parent, id)) return false;
        List<String> siblings = new ArrayList<>(children(parent)); siblings.remove(id);
        int index = siblings.indexOf(before); if (index < 0) index = siblings.size();
        siblings.add(index, id); setParent(id, parent);
        for (int i = 0; i < siblings.size(); i++) setOrder(siblings.get(i), i);
        persist(); return true;
    }
    public boolean relink(String liveId, String missingId) {
        Assignment live = assignment(liveId), missing = assignment(missingId);
        if (live == null || missing == null || !active.containsKey(liveId) || active.containsKey(missingId)) return false;
        ServerData server = active.remove(liveId); active.put(missingId, server); identities.put(server, missingId);
        missing.lastName = server.name; missing.lastAddress = server.ip; missing.lastIndex = live.lastIndex;
        data.lastActiveIds = data.lastActiveIds.stream().map(id -> id.equals(liveId) ? missingId : id).toList();
        data.serverAssignments.remove(live); persist(); return true;
    }
    public String parent(String id) { ServerFolder f = folder(id); Assignment a = assignment(id); return f != null ? f.parentId : a == null ? null : a.folderId; }
    public boolean isDescendant(String candidate, String ancestor) {
        Set<String> seen = new HashSet<>();
        for (String p = candidate; p != null && seen.add(p); p = parent(p)) if (p.equals(ancestor)) return true;
        return false;
    }
    public String path(String id) {
        List<String> path = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String p = parent(id); p != null && seen.add(p); p = parent(p)) { ServerFolder f = folder(p); if (f == null) break; path.add(f.name); }
        Collections.reverse(path); return String.join(" / ", path);
    }
    public int count(String folder) { return (int) active.keySet().stream().filter(id -> isDescendant(parent(id), folder)).count(); }
    /** Canonical order, including nested folders, shared by compact rendering and hit testing. */
    public List<String> compactServers(String folderId) {
        List<String> result = new ArrayList<>();
        Deque<String> pending = new ArrayDeque<>();
        List<String> roots = children(folderId);
        for (int i = roots.size() - 1; i >= 0; i--) pending.push(roots.get(i));
        while (!pending.isEmpty()) {
            String id = pending.pop();
            if (active.containsKey(id)) result.add(id);
            else if (folder(id) != null) {
                List<String> branch = children(id);
                for (int i = branch.size() - 1; i >= 0; i--) pending.push(branch.get(i));
            }
        }
        return result;
    }
    public boolean canMoveServer(String id, boolean right) {
        List<String> siblings = children(parent(id)).stream().filter(active::containsKey).toList();
        int index = siblings.indexOf(id);
        return index >= 0 && (right ? index + 1 < siblings.size() : index > 0);
    }
    public boolean moveServer(String id, boolean right) {
        if (!canMoveServer(id, right)) return false;
        List<String> siblings = children(parent(id)).stream().filter(active::containsKey).toList();
        int index = siblings.indexOf(id);
        if (!right) return move(id, parent(id), siblings.get(index - 1));
        List<String> all = children(parent(id));
        int destination = all.indexOf(siblings.get(index + 1));
        return move(id, parent(id), destination + 1 < all.size() ? all.get(destination + 1) : null);
    }
    public List<Node> visible(String query, boolean searchAddresses) {
        String text = query.strip().toLowerCase(Locale.ROOT); List<Node> rows = new ArrayList<>();
        if (text.isEmpty()) append(null, 0, rows);
        else for (var entry : active.entrySet()) {
            ServerData s = entry.getValue(); String path = path(entry.getKey());
            if ((s.name + ' ' + path + (searchAddresses ? ' ' + s.ip : "")).toLowerCase(Locale.ROOT).contains(text))
                rows.add(new Node(entry.getKey(), false, 0, path));
        }
        return rows;
    }
    private void append(String parent, int depth, List<Node> rows) {
        record Pending(String id, int depth) {}
        Deque<Pending> pending = new ArrayDeque<>();
        List<String> roots = children(parent);
        for (int i = roots.size() - 1; i >= 0; i--) pending.push(new Pending(roots.get(i), depth));
        while (!pending.isEmpty()) {
            Pending next = pending.pop(); ServerFolder f = folder(next.id());
            rows.add(new Node(next.id(), f != null, next.depth(), path(next.id())));
            if (f != null && f.expanded) {
                List<String> children = children(f.id);
                for (int i = children.size() - 1; i >= 0; i--) pending.push(new Pending(children.get(i), next.depth() + 1));
            }
        }
    }
    private List<String> children(String parent) {
        List<String> ids = new ArrayList<>();
        data.folders.stream().filter(f -> Objects.equals(f.parentId, parent)).forEach(f -> ids.add(f.id));
        active.keySet().stream().filter(id -> Objects.equals(parent(id), parent)).forEach(ids::add);
        ids.sort(Comparator.comparingInt(this::order).thenComparing(String::toString)); return ids;
    }
    private int order(String id) { ServerFolder f = folder(id); return f != null ? f.order : assignment(id).order; }
    private int nextOrder(String parent) {
        List<String> siblings = children(parent); int maximum = siblings.stream().mapToInt(this::order).max().orElse(-1);
        if (maximum == Integer.MAX_VALUE) {
            for (int i = 0; i < siblings.size(); i++) setOrder(siblings.get(i), i);
            return siblings.size();
        }
        return maximum + 1;
    }
    private void setOrder(String id, int value) { ServerFolder f = folder(id); if (f != null) f.order = value; else assignment(id).order = value; }
    private void setParent(String id, String parent) { ServerFolder f = folder(id); if (f != null) f.parentId = parent; else assignment(id).folderId = parent; }
    private static String cleanName(String name) { String value = name == null ? "" : name.strip().replaceAll("[\\p{Cntrl}]", ""); return value.isEmpty() ? "Folder" : value.substring(0, Math.min(128, value.length())); }
    public long revision() { return revision; }
    public String error() { return storage.error(); }
    private void persist() { revision++; storage.save(data); }
}

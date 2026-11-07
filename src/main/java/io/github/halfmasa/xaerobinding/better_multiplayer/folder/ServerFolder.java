package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

public final class ServerFolder {
    public String id;
    public String name;
    public String parentId;
    public boolean expanded = true;
    public int order;
    public ServerFolder(String id, String name, String parentId, int order) {
        this.id = id; this.name = name; this.parentId = parentId; this.order = order;
    }
}

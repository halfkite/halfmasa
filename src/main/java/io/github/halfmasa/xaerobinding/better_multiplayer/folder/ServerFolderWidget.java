package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

public interface ServerFolderWidget {
    void halfmasa$rebuildFolders();
    void halfmasa$search(String query);
    String halfmasa$entryId(Object entry);
    boolean halfmasa$folderDragged(double mouseX, double mouseY, int button, boolean shift, double dx, double dy);
    boolean halfmasa$folderReleased(double mouseX, double mouseY, int button, boolean shift);
    String halfmasa$dragLabel();
}

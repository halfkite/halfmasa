package io.github.halfmasa.xaerobinding.better_multiplayer.folder;

/** Coordinates of the current native list render; never persisted across list rebuilds. */
public interface ServerEntryLayout {
    int getX(); int getY(); int getWidth(); int getHeight();
    int getContentX(); int getContentY(); int getContentWidth(); int getContentRight();
}

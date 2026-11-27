package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.nio.file.Path;

public interface WorldCreationListAccess
{
    Path halfmasa$creationRoot();
    void halfmasa$showCreatedWorld(Path path);
}
//#endif

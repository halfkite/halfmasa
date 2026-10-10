package io.github.halfmasa.xaerobinding.feature;

public interface MaLiLibConfigScrollAccess
{
    void halfmasa$saveConfigScroll();

    void halfmasa$restoreConfigScroll();

    //#if MC >= 1.21.1
    void halfmasa$beforeConfigCategoryChange();
    //#endif
}

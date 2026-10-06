package io.github.halfmasa.xaerobinding.feature;

public interface MaLiLibConfigScrollAccess
{
    void halfmasa$saveConfigScroll();

    void halfmasa$restoreConfigScroll();

    //#if MC >= 26.3
    void halfmasa$beforeConfigCategoryChange();
    //#endif
}

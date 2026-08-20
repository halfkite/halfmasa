package io.github.halfmasa.xaerobinding.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import io.github.halfmasa.xaerobinding.gui.HalfMasaConfigScreen;

public final class ModMenuIntegration implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return parent -> new HalfMasaConfigScreen().setParent(parent);
    }
}

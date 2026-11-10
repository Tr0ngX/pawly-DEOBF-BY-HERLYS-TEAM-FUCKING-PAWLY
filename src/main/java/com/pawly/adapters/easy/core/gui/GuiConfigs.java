package com.pawly.adapters.easy.core.gui;

import com.pawly.adapters.easy.core.config.Configs;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import java.util.List;

/**
 * MaLiLib in-game configuration screen for Litematica Lore Pick Block.
 * Reconstructed from {@code com.pawly.adapters.easy.core.gui.GuiConfigs}.
 */
public class GuiConfigs extends GuiConfigsBase {

    public GuiConfigs() {
        super(10, 50, "litematica_lore_pick_block", null, "Litematica Lore Pick Block Configs");
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(Configs.ALL_OPTIONS);
    }
}

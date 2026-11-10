package com.pawly.adapters.easy.core.event;

import com.pawly.adapters.easy.core.config.Configs;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

/**
 * MaLiLib keybind provider registering all Litematica Lore Pick Block hotkeys.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.easy.core.event.a}.
 */
public final class KeybindProviderImpl implements IKeybindProvider {

    private static final KeybindProviderImpl INSTANCE = new KeybindProviderImpl();

    private KeybindProviderImpl() {
    }

    public static KeybindProviderImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (ConfigHotkey hotkey : Configs.HOTKEYS) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(
            "Litematica Lore Pick Block",
            "litematica_lore_pick_block.hotkeys.category.main",
            Configs.HOTKEYS
        );
    }
}

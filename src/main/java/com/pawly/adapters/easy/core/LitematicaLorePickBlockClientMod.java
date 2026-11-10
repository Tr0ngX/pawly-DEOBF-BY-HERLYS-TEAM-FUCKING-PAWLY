package com.pawly.adapters.easy.core;

import com.pawly.adapters.easy.core.config.Configs;
import com.pawly.adapters.easy.core.event.KeybindProviderImpl;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import net.fabricmc.api.ClientModInitializer;

/**
 * Standalone Fabric ClientModInitializer entrypoint for Litematica Lore Pick Block.
 * Reconstructed from obfuscated classes {@code com.pawly.adapters.easy.core.e} and {@code e$a}.
 */
public final class LitematicaLorePickBlockClientMod implements ClientModInitializer {

    public static final String MOD_ID = "litematica_lore_pick_block";

    private static final class ModInitHandler implements IInitializationHandler {
        private ModInitHandler() {
        }

        @Override
        public void registerModHandlers() {
            Configs.getInstance().load();
            ConfigManager.getInstance().registerConfigHandler(MOD_ID, Configs.getInstance());
            InputEventHandler.getKeybindManager().registerKeybindProvider(KeybindProviderImpl.getInstance());
            Configs.initCallbacks();
        }
    }

    public LitematicaLorePickBlockClientMod() {
    }

    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(new ModInitHandler());
    }
}

package com.pawly.adapters.easy.core;

import net.minecraft.class_2561;
import net.minecraft.class_746;

/**
 * Helper for dispatching client-side action-bar and chat messages to the local player.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.easy.core.b}.
 */
final class PlayerMessageHelper {

    private PlayerMessageHelper() {
    }

    static void sendActionBarMessage(class_746 player, String message) {
        if (player != null) {
            player.method_7353(class_2561.method_43470(message), true);
        }
    }

    static void sendChatMessage(class_746 player, String message) {
        if (player != null) {
            player.method_7353(class_2561.method_43470(message), false);
        }
    }
}

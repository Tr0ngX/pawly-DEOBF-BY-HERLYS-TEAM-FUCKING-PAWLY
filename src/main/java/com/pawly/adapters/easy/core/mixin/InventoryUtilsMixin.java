package com.pawly.adapters.easy.core.mixin;

import com.pawly.adapters.easy.core.LoreStackResolver;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook handler intercepting Litematica's {@code InventoryUtils#schematicWorldPickBlock}
 * to substitute server-lore-compatible stacks from the player's inventory.
 * Reconstructed from {@code com.pawly.adapters.easy.core.mixin.InventoryUtilsMixin}.
 */
public class InventoryUtilsMixin {

    public InventoryUtilsMixin() {
    }

    public static class_1799 lorePickBlock$useServerProvidedStack(Object self, class_1799 stack) {
        return LoreStackResolver.prepareGenericPick(stack);
    }

    public static void lorePickBlock$watchForCorrection(
            Object self,
            class_1799 stack,
            class_2338 pos,
            class_1937 schematicWorld,
            class_310 mc,
            CallbackInfo ci) {
        LoreStackResolver.finishGenericPick();
    }
}

package com.pawly.adapters.easy.core.mixin;

import com.pawly.adapters.easy.core.SchematicPlacementGuard;
import com.pawly.adapters.shared.ReflectionBridge;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook handler guarding and throttling {@code Minecraft#startUseItem()}.
 * Reconstructed from {@code com.pawly.adapters.easy.core.mixin.SchematicPickBlockMixin}.
 */
public class SchematicPickBlockMixin {

    public SchematicPickBlockMixin() {
    }

    public void lorePickBlock$onStartUseItem(Object self, CallbackInfo ci) {
        if (SchematicPlacementGuard.shouldRejectRightClick((class_310) self)) {
            ci.cancel();
        }
    }

    public void lorePickBlock$afterStartUseItem(Object self, CallbackInfo ci) {
        int customDelay = SchematicPlacementGuard.finishRightClick((class_310) self);
        if (customDelay >= 0) {
            ReflectionBridge.set((class_310) self, "rightClickDelay", customDelay);
        }
    }
}

package com.pawly.adapters.easy.core.mixin;

import com.pawly.adapters.easy.core.CrafterSlotSynchronizer;
import net.minecraft.class_8898;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook handler invoked at the tail of {@code CrafterScreen#init()}.
 * Reconstructed from {@code com.pawly.adapters.easy.core.mixin.CrafterScreenMixin}.
 */
public class CrafterScreenMixin {

    public CrafterScreenMixin() {
    }

    public void lorePickBlock$onCrafterInit(Object self, CallbackInfo ci) {
        CrafterSlotSynchronizer.onCrafterScreenOpened((class_8898) self);
    }
}

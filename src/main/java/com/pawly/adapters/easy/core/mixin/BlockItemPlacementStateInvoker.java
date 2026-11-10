package com.pawly.adapters.easy.core.mixin;

import com.pawly.adapters.shared.ReflectionBridge;
import net.minecraft.class_1750;
import net.minecraft.class_2680;

/**
 * Reflective invoker wrapper calling {@code BlockItem#getPlacementState(BlockPlaceContext)}.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.easy.core.mixin.a}.
 */
public final class BlockItemPlacementStateInvoker {

    private final Object target;

    private BlockItemPlacementStateInvoker(Object target) {
        this.target = target;
    }

    public static BlockItemPlacementStateInvoker wrap(Object target) {
        return new BlockItemPlacementStateInvoker(target);
    }

    public class_2680 lorePickBlock$invokeGetPlacementState(class_1750 context) {
        return (class_2680) ReflectionBridge.call(this.target, "getPlacementState", context);
    }
}

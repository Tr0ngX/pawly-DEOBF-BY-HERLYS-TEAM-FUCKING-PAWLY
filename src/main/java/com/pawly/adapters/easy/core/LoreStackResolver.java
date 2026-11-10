package com.pawly.adapters.easy.core;

import com.pawly.adapters.easy.EasyModule;
import com.pawly.adapters.easy.core.config.Configs;
import net.minecraft.class_1661;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2480;
import net.minecraft.class_310;
import net.minecraft.class_746;
import net.minecraft.class_9288;
import net.minecraft.class_9334;

/**
 * Resolves lore-augmented or custom-component server item stacks from the player's
 * inventory during Litematica pick-block operations and guards against inventory-swap
 * rejections while moving.
 * Reconstructed from {@code com.pawly.adapters.easy.core.LoreStackResolver},
 * {@code LoreStackResolver$a}, and {@code LoreStackResolver$b}.
 */
public final class LoreStackResolver {

    private static final long SERVER_SETTLE_NANOS = 1_000_000_000L;
    private static ResolvedPickStack genericAttempt;
    private static PendingManualPick pendingManualPick;
    private static boolean movementBlocked;

    public record ResolvedPickStack(class_1799 stack, boolean fromMainInventory) {
        private class_1792 item() {
            return this.stack.method_7909();
        }
    }

    public record PendingManualPick(class_1792 item, long startedAtNanos) {
    }

    private LoreStackResolver() {
    }

    public static void resetMovementBlockedFlag() {
        movementBlocked = false;
    }

    public static boolean wasMovementBlocked() {
        return movementBlocked;
    }

    public static void confirmSuccessfulPlacement() {
        resetState();
    }

    public static class_1799 prepareGenericPick(class_1799 requestedStack) {
        genericAttempt = null;
        if (requestedStack == null || requestedStack.method_7960()) {
            return requestedStack;
        }
        ResolvedPickStack resolved = resolveStackFromInventory(requestedStack);
        if (resolved == null) {
            return requestedStack;
        }
        class_310 client = class_310.method_1551();
        class_746 player = client != null ? client.field_1724 : null;
        if (resolved.fromMainInventory()
                && !Configs.BYPASS_REFUSES.getKeybind().isKeybindHeld()
                && isPlayerMovingOrJumping(player)) {
            if (!EasyModule.legitInventory()) {
                movementBlocked = true;
                sendChatNotification("§c[Litematica] §fStop moving before swapping items from inventory.");
            }
            return class_1799.field_8037;
        }
        genericAttempt = resolved;
        return resolved.stack();
    }

    public static void finishGenericPick() {
        if (genericAttempt == null) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null) {
            genericAttempt = null;
            return;
        }
        if (genericAttempt.fromMainInventory() && !client.method_1569()) {
            pendingManualPick = new PendingManualPick(genericAttempt.item(), System.nanoTime());
        } else {
            pendingManualPick = null;
        }
        genericAttempt = null;
    }

    public static void tick(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            resetState();
            return;
        }
        if (pendingManualPick == null) {
            return;
        }
        long elapsedNanos = System.nanoTime() - pendingManualPick.startedAtNanos();
        if (elapsedNanos < SERVER_SETTLE_NANOS) {
            return;
        }
        if (!isItemInHotbar(client.field_1724.method_31548(), pendingManualPick.item())) {
            sendChatNotification("§c[Litematica] §fServer rejected inventory swap (likely moving). Stop moving and try again.");
        }
        pendingManualPick = null;
    }

    private static ResolvedPickStack resolveStackFromInventory(class_1799 requestedStack) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null) {
            return null;
        }
        class_1661 inventory = client.field_1724.method_31548();
        int hotbarSlot = findExactMatchingSlot(inventory, requestedStack, 0, 9);
        if (hotbarSlot != -1) {
            return new ResolvedPickStack(inventory.method_5438(hotbarSlot).method_7972(), false);
        }
        int mainInvSlot = findExactMatchingSlot(inventory, requestedStack, 9, 36);
        if (mainInvSlot != -1) {
            return new ResolvedPickStack(inventory.method_5438(mainInvSlot).method_7972(), true);
        }
        int compatibleHotbarSlot = findCompatibleSlot(inventory, requestedStack, 0, 9);
        if (compatibleHotbarSlot != -1) {
            return new ResolvedPickStack(inventory.method_5438(compatibleHotbarSlot).method_7972(), false);
        }
        int compatibleMainSlot = findCompatibleSlot(inventory, requestedStack, 9, 36);
        if (compatibleMainSlot != -1) {
            return new ResolvedPickStack(inventory.method_5438(compatibleMainSlot).method_7972(), true);
        }
        return null;
    }

    private static int findExactMatchingSlot(class_1661 inventory, class_1799 target, int startInclusive, int endExclusive) {
        int limit = Math.min(endExclusive, inventory.method_5439());
        for (int slot = startInclusive; slot < limit; slot++) {
            class_1799 candidate = inventory.method_5438(slot);
            if (!candidate.method_7960() && class_1799.method_31577(candidate, target)) {
                return slot;
            }
        }
        return -1;
    }

    private static int findCompatibleSlot(class_1661 inventory, class_1799 target, int startInclusive, int endExclusive) {
        int limit = Math.min(endExclusive, inventory.method_5439());
        for (int slot = startInclusive; slot < limit; slot++) {
            class_1799 candidate = inventory.method_5438(slot);
            if (!candidate.method_7960()
                    && BlockFamilyMatcher.stacksAreCompatibleVariants(candidate, target)
                    && areShulkerContainersCompatible(candidate, target)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isItemInHotbar(class_1661 inventory, class_1792 targetItem) {
        int hotbarLimit = Math.min(9, inventory.method_5439());
        for (int slot = 0; slot < hotbarLimit; slot++) {
            class_1799 candidate = inventory.method_5438(slot);
            if (!candidate.method_7960() && candidate.method_7909() == targetItem) {
                return true;
            }
        }
        return false;
    }

    private static boolean areShulkerContainersCompatible(class_1799 candidate, class_1799 target) {
        if (!isShulkerBox(candidate) || !isShulkerBox(target)) {
            return true;
        }
        boolean targetHasContents = hasNonEmptyContainerContents(target);
        boolean candidateHasContents = hasNonEmptyContainerContents(candidate);
        if (!targetHasContents) {
            return !candidateHasContents;
        }
        if (!candidateHasContents) {
            return false;
        }
        class_9288 candidateContents = (class_9288) candidate.method_58695(class_9334.field_49622, class_9288.field_49334);
        class_9288 targetContents = (class_9288) target.method_58695(class_9334.field_49622, class_9288.field_49334);
        return candidateContents.equals(targetContents);
    }

    private static boolean isShulkerBox(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        class_1792 item = stack.method_7909();
        return item instanceof class_1747 blockItem && blockItem.method_7711() instanceof class_2480;
    }

    private static boolean hasNonEmptyContainerContents(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        class_9288 contents = (class_9288) stack.method_58695(class_9334.field_49622, class_9288.field_49334);
        for (class_1799 nested : contents.method_59714()) {
            if (!nested.method_7960()) {
                return true;
            }
        }
        if (stack.method_57826(class_9334.field_49626)) {
            return true;
        }
        return stack.method_57826(class_9334.field_50075);
    }

    private static boolean isPlayerMovingOrJumping(class_746 player) {
        if (player == null) {
            return false;
        }
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1690 != null) {
            if (SchematicPlacementGuard.isKeyDown(client.field_1690.field_1894)
                    || SchematicPlacementGuard.isKeyDown(client.field_1690.field_1881)
                    || SchematicPlacementGuard.isKeyDown(client.field_1690.field_1913)
                    || SchematicPlacementGuard.isKeyDown(client.field_1690.field_1849)
                    || SchematicPlacementGuard.isKeyDown(client.field_1690.field_1903)) {
                return true;
            }
        }
        if (!player.method_24828() || player.method_5624()) {
            return true;
        }
        return player.method_18798().method_37268() > 1.0E-4;
    }

    private static void sendChatNotification(String message) {
        class_310 client = class_310.method_1551();
        if (client != null && client.field_1724 != null) {
            PlayerMessageHelper.sendChatMessage(client.field_1724, message);
        }
    }

    private static void resetState() {
        genericAttempt = null;
        pendingManualPick = null;
        movementBlocked = false;
    }
}

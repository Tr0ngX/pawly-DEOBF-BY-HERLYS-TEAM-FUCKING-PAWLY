package com.pawly.adapters.easy.core;

import com.pawly.adapters.easy.EasyModule;
import com.pawly.adapters.shared.ReflectionBridge;
import com.pawly.api.AutomationTiming;
import com.pawly.api.GameBindings;
import com.pawly.api.HotbarSelectionDelay;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import net.minecraft.class_10185;
import net.minecraft.class_1661;
import net.minecraft.class_1713;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_241;
import net.minecraft.class_2480;
import net.minecraft.class_2649;
import net.minecraft.class_2653;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_3965;
import net.minecraft.class_490;
import net.minecraft.class_744;
import net.minecraft.class_746;
import net.minecraft.class_9288;
import net.minecraft.class_9334;
import net.minecraft.class_9835;

/**
 * Orchestrates legitimate server-safe inventory swaps by briefly opening the player's
 * inventory screen, issuing a hotbar swap click, waiting for server container-slot
 * acknowledgments, closing the screen, and replaying the placement right-click.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.easy.core.d}.
 */
public final class LegitInventorySwapController {

    private static final Method START_USE = resolveAccessibleMethod(class_310.class, "startUseItem");
    private static final Field MOVE = resolveAccessibleField(class_744.class, "moveVector");
    private static final HotbarSelectionDelay HOTBAR = new HotbarSelectionDelay();

    private static class_310 client;
    private static class_746 player;
    private static Object level;
    private static Object connection;
    private static class_490 screen;
    private static class_1799 wanted = class_1799.field_8037;
    private static class_1799 displaced = class_1799.field_8037;
    private static class_2338 target;
    private static class_2680 expected;
    private static class_3965 hit;
    private static int source = -1;
    private static int destination = -1;
    private static int acks;
    private static boolean swapping;
    private static boolean replaying;
    private static long deadline;
    private static long warningAt;

    static LongSupplier clock = System::nanoTime;
    static Function<class_746, class_490> screens = class_490::new;
    static BiConsumer<class_310, class_490> showScreen = LegitInventorySwapController::openInventoryScreen;
    static BiPredicate<class_2338, class_2680> schematicCurrent = LegitInventorySwapController::matchesSchematicState;
    static Consumer<class_310> replayClick = LegitInventorySwapController::invokeStartUseItem;

    private LegitInventorySwapController() {
    }

    public static boolean active() {
        return swapping || replaying;
    }

    public static boolean blockUse() {
        return swapping && !replaying;
    }

    public static boolean prepare(
            class_310 mc,
            class_2338 pos,
            class_2680 schematicState,
            class_3965 hitResult,
            boolean waterlogTarget) {
        if (!EasyModule.legitInventory() || mc == null || mc.field_1724 == null || mc.field_1687 == null
                || pos == null || schematicState == null || hitResult == null) {
            return false;
        }
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            return false;
        }
        class_1799 requiredStack = MaterialCache.getInstance().getRequiredBuildItemForState(schematicState, schematicWorld, pos);
        return prepareSwapForStack(mc, pos, schematicState, hitResult, requiredStack);
    }

    static boolean prepareSwapForStack(
            class_310 mc,
            class_2338 pos,
            class_2680 schematicState,
            class_3965 hitResult,
            class_1799 requiredStack) {
        if (!EasyModule.legitInventory() || mc == null || mc.field_1724 == null || mc.field_1687 == null
                || mc.field_1761 == null || pos == null || schematicState == null || hitResult == null
                || requiredStack == null || requiredStack.method_7960()) {
            return false;
        }
        if (swapping) {
            return true;
        }
        class_746 localPlayer = mc.field_1724;
        int srcSlot = findBestSourceSlot(localPlayer, requiredStack);
        if (srcSlot == -1) {
            return false;
        }
        long now = clock.getAsLong();
        if (srcSlot >= 0 && srcSlot < 9) {
            int currentSelected = localPlayer.method_31548().method_67532();
            if (currentSelected == srcSlot) {
                HOTBAR.clear();
                return false;
            }
            HOTBAR.step(currentSelected, localPlayer.method_31548(), srcSlot, now, 25_000_000L, 60_000_000L);
            return true;
        }
        HOTBAR.clear();
        if (!isPlayerStationaryAndIdle()) {
            sendRateLimitedWarning(mc, "§c[Litematica] §fStop moving before swapping items from inventory.");
            return true;
        }
        if (!AutomationTiming.inventoryReady("easy", now)) {
            return true;
        }
        int dstHotbarSlot = findBestHotbarDestinationSlot(localPlayer, requiredStack, pos);
        if (dstHotbarSlot < 0 || dstHotbarSlot >= 9) {
            return false;
        }

        client = mc;
        player = localPlayer;
        level = mc.field_1687;
        connection = mc.method_1562();
        wanted = localPlayer.method_31548().method_5438(srcSlot).method_7972();
        displaced = localPlayer.method_31548().method_5438(dstHotbarSlot).method_7972();
        target = pos.method_10062();
        expected = schematicState;
        hit = hitResult;
        source = srcSlot;
        destination = dstHotbarSlot;
        acks = 0;
        swapping = true;
        deadline = now + 1_000_000_000L;

        suppressMovementInputs();
        screen = screens.apply(localPlayer);
        showScreen.accept(mc, screen);
        localPlayer.method_31548().method_61496(dstHotbarSlot);
        mc.field_1761.method_2906(
            localPlayer.field_7498.field_7763,
            srcSlot,
            dstHotbarSlot,
            class_1713.field_7791,
            localPlayer
        );
        AutomationTiming.inventoryClicked("easy", now);
        return true;
    }

    static boolean isMatchingEmptyContainerStack(class_1799 candidate, class_1799 required) {
        if (candidate == null || required == null || candidate.method_7960() || required.method_7960()) {
            return false;
        }
        if (!BlockFamilyMatcher.stacksAreCompatibleVariants(candidate, required)) {
            return false;
        }
        class_1792 item = candidate.method_7909();
        if (item instanceof class_1747 blockItem && blockItem.method_7711() instanceof class_2480) {
            class_9288 contents = (class_9288) candidate.method_58695(class_9334.field_49622, class_9288.field_49334);
            for (class_1799 nested : contents.method_59714()) {
                if (!nested.method_7960()) {
                    return false;
                }
            }
            if (candidate.method_57826(class_9334.field_49626) || candidate.method_57826(class_9334.field_50075)) {
                return false;
            }
        }
        return true;
    }

    static int findBestSourceSlot(class_746 localPlayer, class_1799 requiredStack) {
        if (localPlayer == null || requiredStack == null || requiredStack.method_7960()) {
            return -1;
        }
        class_1661 inv = localPlayer.method_31548();
        for (int i = 0; i < 9; i++) {
            class_1799 stack = inv.method_5438(i);
            if (!stack.method_7960() && class_1799.method_31577(stack, requiredStack)) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            class_1799 stack = inv.method_5438(i);
            if (!stack.method_7960() && class_1799.method_31577(stack, requiredStack)) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            class_1799 stack = inv.method_5438(i);
            if (isMatchingEmptyContainerStack(stack, requiredStack)) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            class_1799 stack = inv.method_5438(i);
            if (isMatchingEmptyContainerStack(stack, requiredStack)) {
                return i;
            }
        }
        return -1;
    }

    static int findBestHotbarDestinationSlot(class_746 localPlayer, class_1799 requiredStack, class_2338 pos) {
        class_1661 inv = localPlayer.method_31548();
        int currentSelected = inv.method_67532();
        if (currentSelected >= 0 && currentSelected < 9 && inv.method_5438(currentSelected).method_7960()) {
            return currentSelected;
        }
        for (int i = 0; i < 9; i++) {
            if (inv.method_5438(i).method_7960()) {
                return i;
            }
        }
        if (currentSelected >= 0 && currentSelected < 9) {
            return currentSelected;
        }
        return 0;
    }

    private static boolean isSessionContextValid() {
        return client != null
            && player != null
            && client.field_1724 == player
            && client.field_1687 == level
            && client.method_1562() == connection
            && player.method_5805();
    }

    private static boolean isPlayerStationaryAndIdle() {
        class_310 mc = client != null ? client : class_310.method_1551();
        if (mc == null || mc.field_1724 == null || mc.field_1761 == null) {
            return false;
        }
        class_746 p = mc.field_1724;
        if (mc.field_1755 != null && mc.field_1755 != screen) {
            return false;
        }
        if (!p.method_24828() || p.method_5624() || p.method_6115() || p.method_7325() || p.method_21823()) {
            return false;
        }
        if (mc.field_1761.method_2923()) {
            return false;
        }
        if (p.field_7512 != p.field_7498 || !p.field_7498.method_34255().method_7960()) {
            return false;
        }
        class_315 opts = mc.field_1690;
        if (opts != null) {
            if (SchematicPlacementGuard.isKeyDown(opts.field_1894)
                    || SchematicPlacementGuard.isKeyDown(opts.field_1881)
                    || SchematicPlacementGuard.isKeyDown(opts.field_1913)
                    || SchematicPlacementGuard.isKeyDown(opts.field_1849)
                    || SchematicPlacementGuard.isKeyDown(opts.field_1903)
                    || SchematicPlacementGuard.isKeyDown(opts.field_1886)) {
                return false;
            }
        }
        return p.method_18798().method_37268() <= 1.0E-4;
    }

    public static void tick(class_310 mc) {
        if (!swapping) {
            return;
        }
        if (mc != client || !isSessionContextValid()) {
            clear();
            return;
        }
        long now = clock.getAsLong();
        if (now > deadline || !isPlayerStationaryAndIdle()) {
            clear();
            return;
        }
        suppressMovementInputs();
        class_1799 currentDest = player.method_31548().method_5438(destination);
        class_1799 currentSrc = player.method_31548().method_5438(source);
        boolean transferConfirmed = (acks == 3)
            || (acks != 0 && class_1799.method_7973(currentDest, wanted) && class_1799.method_7973(currentSrc, displaced))
            || mc.method_1569();
        if (!transferConfirmed) {
            return;
        }
        if (mc.field_1755 == screen && screen != null) {
            player.method_7346();
            mc.method_1507(null);
        }
        screen = null;

        if (!(mc.field_1765 instanceof class_3965 currentHit)
                || !hitResultsMatch(currentHit, hit)
                || !schematicCurrent.test(target, expected)) {
            clear();
            return;
        }

        swapping = false;
        replaying = true;
        try {
            replayClick.accept(mc);
        } finally {
            replaying = false;
            clear();
        }
    }

    public static void observe(Object packet) {
        if (packet instanceof class_9835 heldSlotPacket) {
            HOTBAR.switched(clock.getAsLong());
            if (swapping && heldSlotPacket.comp_2891() != destination) {
                clear();
            }
            return;
        }
        if (!swapping || player == null) {
            return;
        }
        if (packet instanceof class_2653 slotPacket) {
            int windowId = slotPacket.method_11452();
            if (windowId == 0 || windowId == player.field_7498.field_7763) {
                int slot = slotPacket.method_11450();
                class_1799 stack = slotPacket.method_11449();
                if (slot == source) {
                    verifySlotTransferAck(1, stack, displaced);
                } else if (slot == 36 + destination || (windowId != 0 && slot == destination)) {
                    verifySlotTransferAck(2, stack, wanted);
                }
            }
        } else if (packet instanceof class_2649 contentPacket) {
            int windowId = contentPacket.comp_3837();
            if (windowId == 0 || windowId == player.field_7498.field_7763) {
                List<class_1799> items = contentPacket.comp_3839();
                int hotbarContainerSlot = 36 + destination;
                if (source < items.size() && hotbarContainerSlot < items.size()) {
                    if (class_1799.method_7973(items.get(source), displaced)
                            && class_1799.method_7973(items.get(hotbarContainerSlot), wanted)) {
                        acks = 3;
                    } else {
                        clear();
                    }
                }
            }
        }
    }

    private static void verifySlotTransferAck(int bit, class_1799 received, class_1799 expectedStack) {
        if (class_1799.method_7973(received, expectedStack)) {
            acks |= bit;
        } else {
            clear();
        }
    }

    public static Object event(String eventName, Object self, Object[] args) {
        if (eventName == null) {
            return null;
        }
        switch (eventName) {
            case "container_slot_state", "container_content", "hotbar_slot" -> {
                if (args != null && args.length > 0) {
                    observe(args[0]);
                }
                return null;
            }
            case "click_container" -> {
                if (args != null && args.length >= 5 && player != null && args[4] == player) {
                    AutomationTiming.inventoryClicked("easy", clock.getAsLong());
                }
                return Boolean.TRUE;
            }
            default -> {
                return null;
            }
        }
    }

    private static void suppressMovementInputs() {
        if (client == null || player == null) {
            return;
        }
        class_315 opts = client.field_1690;
        if (opts != null) {
            ReflectionBridge.set(opts.field_1894, "isDown", false);
            ReflectionBridge.set(opts.field_1881, "isDown", false);
            ReflectionBridge.set(opts.field_1913, "isDown", false);
            ReflectionBridge.set(opts.field_1849, "isDown", false);
            ReflectionBridge.set(opts.field_1903, "isDown", false);
            ReflectionBridge.set(opts.field_1867, "isDown", false);
        }
        player.method_5728(false);
        class_744 input = player.field_3913;
        if (input != null) {
            class_10185 keyPresses = input.field_54155;
            boolean shiftDown = keyPresses != null && keyPresses.comp_3164();
            input.field_54155 = new class_10185(false, false, false, false, false, shiftDown, false);
            if (MOVE != null) {
                try {
                    MOVE.set(input, class_241.field_1340);
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    public static void clear() {
        HOTBAR.clear();
        if (client != null && screen != null && client.field_1755 == screen) {
            if (player != null) {
                player.method_7346();
            }
            client.method_1507(null);
        }
        client = null;
        player = null;
        level = null;
        connection = null;
        screen = null;
        wanted = class_1799.field_8037;
        displaced = class_1799.field_8037;
        target = null;
        expected = null;
        hit = null;
        source = -1;
        destination = -1;
        acks = 0;
        swapping = false;
        deadline = 0L;
    }

    private static boolean hitResultsMatch(class_3965 first, class_3965 second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null || first.method_17783() != second.method_17783()) {
            return false;
        }
        return Objects.equals(first.method_17777(), second.method_17777())
            && first.method_17780() == second.method_17780()
            && first.method_17784().method_1025(second.method_17784()) <= 1.0E-4;
    }

    private static void sendRateLimitedWarning(class_310 mc, String message) {
        long now = clock.getAsLong();
        if (now >= warningAt && mc != null && mc.field_1724 != null) {
            warningAt = now + 1_000_000_000L;
            PlayerMessageHelper.sendChatMessage(mc.field_1724, message);
        }
    }

    private static Method resolveAccessibleMethod(Class<?> owner, String name) {
        try {
            Method method = GameBindings.getDeclaredMethod(owner, name);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Field resolveAccessibleField(Class<?> owner, String name) {
        try {
            Field field = GameBindings.getDeclaredField(owner, name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void invokeStartUseItem(class_310 mc) {
        if (START_USE != null && mc != null) {
            try {
                START_USE.invoke(mc);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static boolean matchesSchematicState(class_2338 pos, class_2680 state) {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        return schematicWorld != null && pos != null && Objects.equals(schematicWorld.method_8320(pos), state);
    }

    private static void openInventoryScreen(class_310 mc, class_490 inventoryScreen) {
        if (mc != null) {
            mc.method_1507(inventoryScreen);
        }
    }
}

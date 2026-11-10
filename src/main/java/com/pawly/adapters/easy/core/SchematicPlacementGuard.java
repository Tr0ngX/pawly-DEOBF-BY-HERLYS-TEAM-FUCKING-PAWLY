package com.pawly.adapters.easy.core;

import com.pawly.adapters.easy.EasyModule;
import com.pawly.adapters.easy.core.config.Configs;
import com.pawly.adapters.easy.core.mixin.BlockItemPlacementStateInvoker;
import com.pawly.adapters.shared.ReflectionBridge;
import com.pawly.api.AutomationTiming;
import com.pawly.api.GameBindings;
import com.pawly.api.PlacementActions;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerRange;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_10185;
import net.minecraft.class_1268;
import net.minecraft.class_1747;
import net.minecraft.class_1750;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2277;
import net.minecraft.class_2281;
import net.minecraft.class_2304;
import net.minecraft.class_2306;
import net.minecraft.class_2309;
import net.minecraft.class_2315;
import net.minecraft.class_2323;
import net.minecraft.class_2325;
import net.minecraft.class_2328;
import net.minecraft.class_2331;
import net.minecraft.class_2333;
import net.minecraft.class_2336;
import net.minecraft.class_2338;
import net.minecraft.class_2349;
import net.minecraft.class_2350;
import net.minecraft.class_2377;
import net.minecraft.class_2380;
import net.minecraft.class_239;
import net.minecraft.class_2401;
import net.minecraft.class_2423;
import net.minecraft.class_2428;
import net.minecraft.class_2462;
import net.minecraft.class_2480;
import net.minecraft.class_2482;
import net.minecraft.class_2533;
import net.minecraft.class_2680;
import net.minecraft.class_2741;
import net.minecraft.class_2769;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3708;
import net.minecraft.class_3711;
import net.minecraft.class_3713;
import net.minecraft.class_3714;
import net.minecraft.class_3715;
import net.minecraft.class_3716;
import net.minecraft.class_3717;
import net.minecraft.class_3718;
import net.minecraft.class_3830;
import net.minecraft.class_3965;
import net.minecraft.class_5556;
import net.minecraft.class_746;
import net.minecraft.class_7712;
import net.minecraft.class_8886;
import net.minecraft.class_9334;

/**
 * Validates right-click schematic placements, triggers auto-pick-block, enforces
 * orientation/wrong-block/extra-block guards, manages temporary sneak against
 * interactive support blocks, and controls placement click rate.
 * Reconstructed from obfuscated classes {@code com.pawly.adapters.easy.core.g} and {@code g$a}.
 */
public final class SchematicPlacementGuard {

    private static final String LITEMATICA = "litematica";
    private static final String WORLD_UTILS = "fi.dy.masa.litematica.util.WorldUtils";

    private static boolean reflectionWarningShown;
    private static boolean inputWarningShown;
    private static boolean acceptedSchematicPlacement;
    private static class_2338 pendingPlacementPos;
    private static class_2680 pendingStateBeforePlacement;
    private static class_2680 pendingExpectedState;
    private static boolean pendingWaterlogPlacement;
    private static int lastPlacementRate;
    private static int placementRateRemainder;
    private static boolean temporarySneakActive;
    private static class_746 temporarySneakPlayer;
    private static class_10185 inputBeforeTemporarySneak;

    public record PlacementTarget(
        class_2338 pos,
        class_2680 schematicState,
        boolean waterlogTarget
    ) {}

    private SchematicPlacementGuard() {
    }

    public static boolean shouldRejectRightClick(class_310 client) {
        acceptedSchematicPlacement = false;
        clearPendingPlacementState();
        CrafterSlotSynchronizer.setLastInteractedBlockPos(null);

        if (LegitInventorySwapController.blockUse()) {
            return true;
        }
        if (client == null || client.field_1724 == null || client.field_1687 == null
                || hasOpenScreen(client) || !isLitematicaLoaded()) {
            return false;
        }
        if (Configs.BYPASS_REFUSES.getKeybind().isKeybindHeld()) {
            return false;
        }

        class_746 player = client.field_1724;
        if (isUsingOrHoldingConsumableItem(player)) {
            return false;
        }

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            return false;
        }
        if (!(client.field_1765 instanceof class_3965 hitResult)
                || hitResult.method_17783() != class_239.class_240.field_1332) {
            return false;
        }

        class_2338 clickedPos = hitResult.method_17777();
        class_2680 realClickedState = client.field_1687.method_8320(clickedPos);
        if (realClickedState.method_27852(class_2246.field_46797)) {
            CrafterSlotSynchronizer.setLastInteractedBlockPos(clickedPos);
        }

        boolean clickedInteractive = isInteractiveBlock(realClickedState.method_26204());
        boolean needsStateCorrection = clickedInteractive
            && isPositionWithinRenderedSchematic(clickedPos)
            && doesInteractiveBlockNeedStateCorrection(client, schematicWorld, clickedPos, realClickedState);

        if (needsStateCorrection) {
            return false;
        }

        if (Configs.REFUSE_CORRECT_BLOCK_INTERACTION.getBooleanValue()
                && clickedInteractive
                && !player.method_5715()
                && isPositionWithinRenderedSchematic(clickedPos)
                && isBlockAlreadyMatchingSchematic(schematicWorld, clickedPos, realClickedState)) {
            if (!Configs.PLACE_AGAINST_INTERACTIVE_BLOCKS.getBooleanValue()) {
                return true;
            }
        }

        PlacementTarget target = resolvePlacementTarget(client, schematicWorld, hitResult, player.method_6047());
        if (target == null) {
            return false;
        }

        if (!isPositionWithinRenderedSchematic(target.pos())) {
            return false;
        }

        if (target.schematicState().method_26215()) {
            if (Configs.REFUSE_EXTRA_PLACEMENT.getBooleanValue()) {
                return true;
            }
            return false;
        }

        if (Configs.AUTO_PICK_BLOCK_ON_USE.getBooleanValue()
                && !isHeldStackValidForSchematic(schematicWorld, target.pos(), target.schematicState(), player.method_6047(), target.waterlogTarget())) {
            if (LegitInventorySwapController.prepare(client, target.pos(), target.schematicState(), hitResult, target.waterlogTarget())) {
                return true;
            }
            LoreStackResolver.resetMovementBlockedFlag();
            try {
                invokeLitematicaPickBlock(true, client);
            } catch (ReflectiveOperationException e) {
                logReflectionWarningOnce("Failed invoking Litematica doSchematicWorldPickBlock", e);
            }
            if (LoreStackResolver.wasMovementBlocked()) {
                return true;
            }
        }

        class_1799 heldAfterPick = player.method_6047();
        if (!isHeldStackValidForSchematic(schematicWorld, target.pos(), target.schematicState(), heldAfterPick, target.waterlogTarget())) {
            if (Configs.CANCEL_WHEN_UNABLE_TO_PICK.getBooleanValue() || Configs.REFUSE_WRONG_PLACEMENT.getBooleanValue()) {
                return true;
            }
            return false;
        }

        if (Configs.ONLY_FACING_CORRECTLY.getBooleanValue() && !target.waterlogTarget() && !EasyModule.facingBypass()) {
            if (heldAfterPick.method_7909() instanceof class_1747 blockItem) {
                class_2680 predicted = predictPlacedBlockState(player, heldAfterPick, hitResult, blockItem);
                if (predicted != null && !doOrientationPropertiesMatch(predicted, target.schematicState())) {
                    return true;
                }
            }
        }

        if (clickedInteractive && !player.method_5715() && Configs.PLACE_AGAINST_INTERACTIVE_BLOCKS.getBooleanValue()) {
            beginTemporarySneak(client);
        }

        acceptedSchematicPlacement = true;
        pendingPlacementPos = target.pos().method_10062();
        pendingStateBeforePlacement = client.field_1687.method_8320(target.pos());
        pendingExpectedState = target.schematicState();
        pendingWaterlogPlacement = target.waterlogTarget();
        return false;
    }

    public static int finishRightClick(class_310 client) {
        endTemporarySneak(client);
        if (!acceptedSchematicPlacement || client == null || client.field_1687 == null || pendingPlacementPos == null) {
            clearPendingPlacementState();
            return -1;
        }
        class_2680 stateAfter = client.field_1687.method_8320(pendingPlacementPos);
        boolean stateChanged = !Objects.equals(stateAfter, pendingStateBeforePlacement);
        clearPendingPlacementState();
        if (stateChanged) {
            LoreStackResolver.confirmSuccessfulPlacement();
            PlacementActions.validatedUse();
        }
        int cps = Configs.PLACEMENT_CLICKS_PER_SECOND.getIntegerValue();
        if (cps <= 0) {
            lastPlacementRate = 0;
            placementRateRemainder = 0;
            return -1;
        }
        if (cps != lastPlacementRate) {
            lastPlacementRate = cps;
            placementRateRemainder = 0;
        }
        int totalTicks = 20 + placementRateRemainder;
        int intervalTicks = totalTicks / cps;
        placementRateRemainder = totalTicks % cps;
        int delay = Math.max(0, intervalTicks - 1) + AutomationTiming.placeExtraTicks("easy");
        return delay;
    }

    private static void clearPendingPlacementState() {
        acceptedSchematicPlacement = false;
        pendingPlacementPos = null;
        pendingStateBeforePlacement = null;
        pendingExpectedState = null;
        pendingWaterlogPlacement = false;
    }

    private static PlacementTarget resolvePlacementTarget(
            class_310 client,
            WorldSchematic schematicWorld,
            class_3965 hitResult,
            class_1799 heldStack) {
        class_2338 clickedPos = hitResult.method_17777();
        class_2680 realClickedState = client.field_1687.method_8320(clickedPos);
        class_2680 schemClickedState = schematicWorld.method_8320(clickedPos);

        if (schemClickedState.method_28498(class_2741.field_12508)
                && Boolean.TRUE.equals(schemClickedState.method_11654(class_2741.field_12508))
                && realClickedState.method_28498(class_2741.field_12508)
                && Boolean.FALSE.equals(realClickedState.method_11654(class_2741.field_12508))
                && BlockFamilyMatcher.blocksMatch(realClickedState.method_26204(), schemClickedState.method_26204())) {
            return new PlacementTarget(clickedPos, schemClickedState, true);
        }

        class_1750 placeCtx = new class_1750(client.field_1724, class_1268.field_5808, heldStack, hitResult);
        class_2338 targetPos = realClickedState.method_26166(placeCtx)
                ? clickedPos
                : clickedPos.method_10093(hitResult.method_17780());
        return new PlacementTarget(targetPos, schematicWorld.method_8320(targetPos), false);
    }

    private static boolean isHeldStackValidForSchematic(
            WorldSchematic schematicWorld,
            class_2338 pos,
            class_2680 schematicState,
            class_1799 heldStack,
            boolean waterlogTarget) {
        if (heldStack == null || heldStack.method_7960()) {
            return false;
        }
        if (waterlogTarget) {
            return heldStack.method_7909() == class_1802.field_8705;
        }
        class_1799 required = fi.dy.masa.litematica.materials.MaterialCache.getInstance()
                .getRequiredBuildItemForState(schematicState, schematicWorld, pos);
        if (required == null || required.method_7960()) {
            return false;
        }
        return BlockFamilyMatcher.stacksAreCompatibleVariants(heldStack, required);
    }

    private static class_2680 predictPlacedBlockState(
            class_746 player,
            class_1799 stack,
            class_3965 hitResult,
            class_1747 blockItem) {
        try {
            class_1750 baseCtx = new class_1750(player, class_1268.field_5808, stack, hitResult);
            class_1750 updatedCtx = blockItem.method_16356(baseCtx);
            if (updatedCtx == null) {
                return null;
            }
            return BlockItemPlacementStateInvoker.wrap(blockItem).lorePickBlock$invokeGetPlacementState(updatedCtx);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean doesInteractiveBlockNeedStateCorrection(
            class_310 client,
            WorldSchematic schematicWorld,
            class_2338 pos,
            class_2680 realState) {
        class_2680 schematicState = schematicWorld.method_8320(pos);
        if (!BlockFamilyMatcher.blocksMatch(realState.method_26204(), schematicState.method_26204())) {
            return false;
        }
        class_2248 block = realState.method_26204();
        if (block instanceof class_8886) {
            return CrafterSlotSynchronizer.needsCorrection(client, schematicWorld, pos);
        }
        if (block instanceof class_2323 || block instanceof class_2349 || block instanceof class_2533) {
            return doesPropertyDiffer(realState, schematicState, "open");
        }
        if (block instanceof class_2462) {
            return doesPropertyDiffer(realState, schematicState, "delay");
        }
        if (block instanceof class_2306) {
            return doesPropertyDiffer(realState, schematicState, "mode");
        }
        if (block instanceof class_2428) {
            return doesPropertyDiffer(realState, schematicState, "note");
        }
        if (block instanceof class_2315) {
            return doesPropertyDiffer(realState, schematicState, "inverted");
        }
        if (block instanceof class_2401) {
            return doesPropertyDiffer(realState, schematicState, "powered");
        }
        return false;
    }

    private static boolean isBlockAlreadyMatchingSchematic(
            WorldSchematic schematicWorld,
            class_2338 pos,
            class_2680 realState) {
        class_2680 schematicState = schematicWorld.method_8320(pos);
        if (schematicState.method_26215()) {
            return false;
        }
        return BlockFamilyMatcher.blocksMatch(realState.method_26204(), schematicState.method_26204())
            && doOrientationPropertiesMatch(realState, schematicState);
    }

    private static boolean isPositionWithinRenderedSchematic(class_2338 pos) {
        if (!fi.dy.masa.litematica.config.Configs.Visuals.ENABLE_RENDERING.getBooleanValue()
                || !fi.dy.masa.litematica.config.Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue()) {
            return false;
        }
        LayerRange layerRange = DataManager.getRenderLayerRange();
        if (layerRange != null && !layerRange.isPositionWithinRange(pos)) {
            return false;
        }
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        if (manager == null) {
            return false;
        }
        List<SchematicPlacementManager.PlacementPart> parts = manager.getAllPlacementsTouchingChunk(pos);
        for (SchematicPlacementManager.PlacementPart part : parts) {
            SchematicPlacement placement = part.getPlacement();
            if (placement != null && placement.isEnabled() && placement.isRenderingEnabled()
                    && part.getBox() != null && part.getBox().containsPos(pos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean beginTemporarySneak(class_310 client) {
        if (client == null || client.field_1724 == null || client.field_1724.field_3913 == null) {
            return false;
        }
        try {
            temporarySneakPlayer = client.field_1724;
            inputBeforeTemporarySneak = temporarySneakPlayer.field_3913.field_54155;
            if (inputBeforeTemporarySneak != null) {
                temporarySneakPlayer.field_3913.field_54155 = withShiftKeyDown(inputBeforeTemporarySneak, true);
            }
            temporarySneakPlayer.method_5660(true);
            temporarySneakActive = true;
            return true;
        } catch (Exception e) {
            logInputWarningOnce("Unable to apply temporary sneak input", e);
            return false;
        }
    }

    private static void endTemporarySneak(class_310 client) {
        if (!temporarySneakActive) {
            return;
        }
        try {
            if (temporarySneakPlayer != null) {
                if (temporarySneakPlayer.field_3913 != null && inputBeforeTemporarySneak != null) {
                    temporarySneakPlayer.field_3913.field_54155 = inputBeforeTemporarySneak;
                }
                boolean shiftPhysicallyDown = client != null && client.field_1690 != null
                        && isKeyDown(client.field_1690.field_1867);
                temporarySneakPlayer.method_5660(shiftPhysicallyDown);
            }
        } catch (Exception e) {
            logInputWarningOnce("Unable to restore temporary sneak input", e);
        } finally {
            temporarySneakActive = false;
            temporarySneakPlayer = null;
            inputBeforeTemporarySneak = null;
        }
    }

    private static class_10185 withShiftKeyDown(class_10185 original, boolean shiftDown) {
        return new class_10185(
            original.comp_3159(),
            original.comp_3160(),
            original.comp_3161(),
            original.comp_3162(),
            original.comp_3163(),
            shiftDown,
            original.comp_3165()
        );
    }

    public static boolean isKeyDown(class_304 keyMapping) {
        if (keyMapping == null) {
            return false;
        }
        return Boolean.TRUE.equals(ReflectionBridge.call(keyMapping, "isDown"));
    }

    private static boolean isPlayerMoving(class_746 player) {
        return player != null && player.method_18798().method_37268() > 1.0E-4;
    }

    private static boolean isUsingOrHoldingConsumableItem(class_746 player) {
        if (player == null) {
            return false;
        }
        if (player.method_6115()) {
            return true;
        }
        return isRightClickUsableItem(player.method_6047()) || isRightClickUsableItem(player.method_6079());
    }

    private static boolean isRightClickUsableItem(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        if (stack.method_57826(class_9334.field_50075)) {
            return true;
        }
        class_1792 item = stack.method_7909();
        return item == class_1802.field_8102
            || item == class_1802.field_8399
            || item == class_1802.field_8255
            || item == class_1802.field_8547
            || item == class_1802.field_8378
            || item == class_1802.field_8634
            || item == class_1802.field_8574
            || item == class_1802.field_8436
            || item == class_1802.field_8469
            || item == class_1802.field_8884
            || item == class_1802.field_8814
            || item == class_1802.field_8360
            || item == class_1802.field_8449
            || item == class_1802.field_8187
            || item == class_1802.field_8639
            || item == class_1802.field_8324
            || item == class_1802.field_8150
            || item == class_1802.field_8674
            || item == class_1802.field_8868
            || item == class_1802.field_8103
            || item == class_1802.field_20414
            || item == class_1802.field_20417
            || item == class_1802.field_27070
            || item == class_1802.field_27876
            || item == class_1802.field_39057
            || item == class_1802.field_42716
            || item == class_1802.field_49098;
    }

    private static boolean isInteractiveBlock(class_2248 block) {
        if (block == null) {
            return false;
        }
        return isNonEnderChest(block)
            || block instanceof class_2377
            || block instanceof class_2480
            || block instanceof class_3708
            || block instanceof class_2336
            || block instanceof class_3717
            || block instanceof class_3718
            || block instanceof class_2281
            || block instanceof class_2325
            || block instanceof class_2309
            || block instanceof class_3713
            || block instanceof class_3715
            || block instanceof class_3714
            || block instanceof class_3711
            || block instanceof class_3830
            || block instanceof class_3716
            || block instanceof class_2331
            || block instanceof class_2323
            || block instanceof class_2349
            || block instanceof class_2533
            || block instanceof class_2277
            || block instanceof class_2401
            || block instanceof class_2462
            || block instanceof class_2306
            || block instanceof class_2428
            || block instanceof class_2315
            || block instanceof class_2380
            || block instanceof class_2328
            || block instanceof class_2333
            || block instanceof class_2423
            || block instanceof class_2482
            || block instanceof class_7712
            || block instanceof class_5556
            || block instanceof class_2304
            || block instanceof class_8886;
    }

    private static boolean doOrientationPropertiesMatch(class_2680 predicted, class_2680 expectedState) {
        for (class_2769<?> prop : expectedState.method_28501()) {
            String propName = prop.method_11899();
            if (isOrientationPropertyName(propName) && predicted.method_28498(prop)) {
                Comparable<?> predictedVal = getPropertyValue(predicted, prop);
                Comparable<?> expectedVal = getPropertyValue(expectedState, prop);
                if (!Objects.equals(predictedVal, expectedVal)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean doesPropertyDiffer(class_2680 first, class_2680 second, String propertyName) {
        class_2769<?> prop = findPropertyByName(first, propertyName);
        if (prop == null || !second.method_28498(prop)) {
            return false;
        }
        return !Objects.equals(getPropertyValue(first, prop), getPropertyValue(second, prop));
    }

    private static class_2769<?> findPropertyByName(class_2680 state, String propertyName) {
        for (class_2769<?> prop : state.method_28501()) {
            if (prop.method_11899().equals(propertyName)) {
                return prop;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparable<?> getPropertyValue(class_2680 state, class_2769<?> prop) {
        return state.method_11654((class_2769) prop);
    }

    private static boolean isOrientationPropertyName(String name) {
        return switch (name) {
            case "facing", "axis", "half", "type", "face", "orientation", "rotation", "hinge", "part", "attachment" -> true;
            default -> false;
        };
    }

    private static boolean isNonEnderChest(class_2248 block) {
        return block == class_2246.field_10164
            || block == class_2246.field_10382
            || block == class_2246.field_27879;
    }

    private static Object invokeLitematicaPickBlock(boolean withFluids, class_310 client) throws ReflectiveOperationException {
        Class<?> worldUtilsClass = GameBindings.forName(WORLD_UTILS, true, SchematicPlacementGuard.class.getClassLoader());
        Method pickMethod = GameBindings.getDeclaredMethod(worldUtilsClass, "doSchematicWorldPickBlock", boolean.class, class_310.class);
        pickMethod.setAccessible(true);
        return pickMethod.invoke(null, withFluids, client);
    }

    public static boolean hasOpenScreen(class_310 client) {
        return client != null && client.field_1755 != null;
    }

    private static boolean isLitematicaLoaded() {
        try {
            return FabricLoader.getInstance().isModLoaded(LITEMATICA);
        } catch (Throwable t) {
            return true;
        }
    }

    private static void logReflectionWarningOnce(String msg, Throwable t) {
        if (!reflectionWarningShown) {
            reflectionWarningShown = true;
        }
    }

    private static void logInputWarningOnce(String msg, Throwable t) {
        if (!inputWarningShown) {
            inputWarningShown = true;
        }
    }
}

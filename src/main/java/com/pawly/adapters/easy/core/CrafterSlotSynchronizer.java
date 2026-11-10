package com.pawly.adapters.easy.core;

import com.pawly.adapters.easy.core.config.Configs;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.class_1703;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_2815;
import net.minecraft.class_310;
import net.minecraft.class_8881;
import net.minecraft.class_8887;
import net.minecraft.class_8898;

/**
 * Synchronizes Crafter block disabled-slot states with the Litematica schematic.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.easy.core.c}.
 */
public final class CrafterSlotSynchronizer {

    private static class_2338 lastInteractedCrafterPos;
    private static Object lastCorrectedRealLevel;
    private static WorldSchematic lastCorrectedSchematicWorld;
    private static class_2338 lastCorrectedCrafterPos;
    private static int lastCorrectedDisabledMask = -1;

    private CrafterSlotSynchronizer() {
    }

    public static void setLastInteractedBlockPos(class_2338 pos) {
        lastInteractedCrafterPos = pos == null ? null : pos.method_10062();
    }

    public static boolean needsCorrection(class_310 client, WorldSchematic schematicWorld, class_2338 pos) {
        if (client == null || client.field_1687 == null || schematicWorld == null || pos == null) {
            return false;
        }
        class_2680 realState = client.field_1687.method_8320(pos);
        class_2680 schematicState = schematicWorld.method_8320(pos);
        if (!realState.method_27852(class_2246.field_46797) || !schematicState.method_27852(class_2246.field_46797)) {
            return false;
        }
        class_2586 schematicBe = schematicWorld.method_8321(pos);
        if (!(schematicBe instanceof class_8887 schematicCrafter)) {
            return false;
        }
        int expectedMask = computeDisabledSlotsBitmask(schematicCrafter);
        if (client.field_1687 == lastCorrectedRealLevel
                && schematicWorld == lastCorrectedSchematicWorld
                && pos.equals(lastCorrectedCrafterPos)
                && expectedMask == lastCorrectedDisabledMask) {
            return false;
        }
        class_2586 realBe = client.field_1687.method_8321(pos);
        if (realBe instanceof class_8887 realCrafter && computeDisabledSlotsBitmask(realCrafter) == expectedMask) {
            return false;
        }
        return true;
    }

    public static void onCrafterScreenOpened(class_8898 crafterScreen) {
        if (!Configs.AUTO_CRAFTER_DISABLER.getBooleanValue()) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || client.field_1687 == null
                || client.field_1761 == null || lastInteractedCrafterPos == null) {
            return;
        }
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            return;
        }
        class_2680 schematicState = schematicWorld.method_8320(lastInteractedCrafterPos);
        if (!schematicState.method_27852(class_2246.field_46797)) {
            return;
        }
        class_2586 schematicBe = schematicWorld.method_8321(lastInteractedCrafterPos);
        if (!(schematicBe instanceof class_8887 schematicCrafter)) {
            return;
        }
        class_1703 menu = crafterScreen.method_17577();
        if (!(menu instanceof class_8881 crafterMenu)) {
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            boolean shouldBeDisabled = schematicCrafter.method_54483(slot);
            boolean currentlyDisabled = crafterMenu.method_54461(slot);
            if (currentlyDisabled != shouldBeDisabled) {
                crafterMenu.method_54458(slot, shouldBeDisabled);
                client.field_1761.method_54634(slot, crafterMenu.field_7763, shouldBeDisabled);
            }
        }
        lastCorrectedRealLevel = client.field_1687;
        lastCorrectedSchematicWorld = schematicWorld;
        lastCorrectedCrafterPos = lastInteractedCrafterPos.method_10062();
        lastCorrectedDisabledMask = computeDisabledSlotsBitmask(schematicCrafter);
        lastInteractedCrafterPos = null;

        if (client.field_1724.field_3944 != null) {
            client.field_1724.field_3944.method_52787(new class_2815(crafterMenu.field_7763));
        }
        client.method_29970(null);
        PlayerMessageHelper.sendActionBarMessage(client.field_1724, "§a[Litematica] §fMatched Crafter slots to schematic.");
    }

    private static int computeDisabledSlotsBitmask(class_8887 crafterBlockEntity) {
        int mask = 0;
        for (int slot = 0; slot < 9; slot++) {
            if (crafterBlockEntity.method_54483(slot)) {
                mask |= (1 << slot);
            }
        }
        return mask;
    }
}

package com.pawly.adapters.easy.core;

import com.pawly.adapters.easy.core.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.LayerRange;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.class_1661;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_9288;
import net.minecraft.class_9334;

/**
 * Scans a configurable cubic radius around the player for missing or misplaced
 * Litematica schematic blocks, subtracts items currently carried in the player's
 * inventory or shulker boxes, and prints a formatted summary to chat.
 * Reconstructed from obfuscated classes {@code com.pawly.adapters.easy.core.f} and {@code f$a}.
 */
public final class NearbyMaterialScanner {

    private static final int MAX_DISPLAYED_TYPES = 50;

    private static final class MaterialRequirementEntry {
        private final class_1799 stack;
        private long amount;

        private MaterialRequirementEntry(class_1799 stack, long amount) {
            this.stack = stack;
            this.amount = amount;
        }
    }

    private NearbyMaterialScanner() {
    }

    public static void showNearbyMaterials() {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null || client.field_1687 == null) {
            return;
        }
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            sendPrefixedMessage(client, "§cNo schematic world loaded.");
            return;
        }
        class_746 player = client.field_1724;
        class_638 clientLevel = client.field_1687;
        int radius = Configs.MATERIAL_SCAN_RADIUS.getIntegerValue();
        class_2338 center = player.method_24515();
        int minY = Math.max(clientLevel.method_31607(), center.method_10264() - radius);
        int maxY = Math.min(clientLevel.method_31600() - 1, center.method_10264() + radius);
        class_2338 minPos = new class_2338().method_10069(center.method_10263() - radius, minY, center.method_10260() - radius);
        class_2338 maxPos = new class_2338().method_10069(center.method_10263() + radius, maxY, center.method_10260() + radius);

        LayerRange layerRange = DataManager.getRenderLayerRange();
        MaterialCache materialCache = MaterialCache.getInstance();
        Map<class_1792, MaterialRequirementEntry> requiredByItem = new LinkedHashMap<>();

        for (class_2338 pos : class_2338.method_10097(minPos, maxPos)) {
            if (layerRange != null && !layerRange.isPositionWithinRange(pos)) {
                continue;
            }
            if (!clientLevel.method_22340(pos)) {
                continue;
            }
            class_2680 schematicState = schematicWorld.method_8320(pos);
            if (schematicState.method_26215()) {
                continue;
            }
            class_2680 realState = clientLevel.method_8320(pos);
            if (realState.equals(schematicState)) {
                continue;
            }
            class_1799 buildItem = materialCache.getRequiredBuildItemForState(schematicState, schematicWorld, pos);
            if (buildItem == null || buildItem.method_7960()) {
                continue;
            }
            int count = Math.max(1, buildItem.method_7947());
            MaterialRequirementEntry entry = requiredByItem.computeIfAbsent(
                buildItem.method_7909(),
                item -> createEntry(buildItem, item)
            );
            entry.amount += count;
        }

        if (requiredByItem.isEmpty()) {
            sendPrefixedMessage(client, "§aNo missing schematic blocks within §e" + radius + "§a blocks!");
            return;
        }

        Object2LongOpenHashMap<class_1792> availableCounts = new Object2LongOpenHashMap<>();
        class_1661 inventory = player.method_31548();
        for (int slot = 0; slot < inventory.method_5439(); slot++) {
            class_1799 stack = inventory.method_5438(slot);
            if (stack.method_7960()) {
                continue;
            }
            availableCounts.addTo(stack.method_7909(), stack.method_7947());
            class_9288 containerContents = (class_9288) stack.method_58695(class_9334.field_49622, class_9288.field_49334);
            for (class_1799 nested : containerContents.method_59714()) {
                if (!nested.method_7960()) {
                    availableCounts.addTo(nested.method_7909(), nested.method_7947());
                }
            }
        }
        class_1799 offhand = player.method_6079();
        if (!offhand.method_7960()) {
            availableCounts.addTo(offhand.method_7909(), offhand.method_7947());
        }

        List<MaterialRequirementEntry> missingEntries = new ArrayList<>();
        for (Map.Entry<class_1792, MaterialRequirementEntry> mapEntry : requiredByItem.entrySet()) {
            long have = availableCounts.getOrDefault(mapEntry.getKey(), 0L);
            long remaining = mapEntry.getValue().amount - have;
            if (remaining > 0L) {
                missingEntries.add(new MaterialRequirementEntry(mapEntry.getValue().stack, remaining));
            }
        }

        if (missingEntries.isEmpty()) {
            sendPrefixedMessage(
                client,
                "§aYou already have all §e" + requiredByItem.size() + "§a material type(s) needed within §e" + radius + "§a blocks!"
            );
            return;
        }

        missingEntries.sort(
            Comparator.comparingLong(NearbyMaterialScanner::getAmount)
                .reversed()
                .thenComparing(NearbyMaterialScanner::getDisplayName)
        );

        long totalMissingBlocks = 0L;
        for (MaterialRequirementEntry entry : missingEntries) {
            totalMissingBlocks += entry.amount;
        }

        sendPrefixedMessage(
            client,
            "§6Missing materials within §e" + radius + "§6 blocks (§e" + missingEntries.size() + "§6 types, §e" + totalMissingBlocks + "§6 total):"
        );

        int displayCount = Math.min(MAX_DISPLAYED_TYPES, missingEntries.size());
        for (int i = 0; i < displayCount; i++) {
            MaterialRequirementEntry entry = missingEntries.get(i);
            String itemName = entry.stack.method_7964().getString();
            PlayerMessageHelper.sendChatMessage(
                player,
                " §7- §f" + itemName + "§7: §e" + entry.amount + " §8(" + formatStacksAndRemainder(entry.amount, 64) + ")"
            );
        }
        if (missingEntries.size() > MAX_DISPLAYED_TYPES) {
            PlayerMessageHelper.sendChatMessage(
                player,
                " §8... and " + (missingEntries.size() - MAX_DISPLAYED_TYPES) + " more type(s)."
            );
        }
    }

    private static String formatStacksAndRemainder(long amount, int maxStackSize) {
        long stacks = amount / maxStackSize;
        long remainder = amount % maxStackSize;
        if (stacks <= 0L) {
            return remainder + "";
        }
        if (remainder == 0L) {
            return stacks + "st";
        }
        return stacks + "st + " + remainder;
    }

    private static void sendPrefixedMessage(class_310 client, String message) {
        if (client != null && client.field_1724 != null) {
            PlayerMessageHelper.sendChatMessage(client.field_1724, "§b[Litematica] §r" + message);
        }
    }

    private static String getDisplayName(MaterialRequirementEntry entry) {
        return entry.stack.method_7964().getString();
    }

    private static long getAmount(MaterialRequirementEntry entry) {
        return entry.amount;
    }

    private static MaterialRequirementEntry createEntry(class_1799 buildItem, class_1792 item) {
        class_1799 singleCopy = buildItem.method_7972();
        return new MaterialRequirementEntry(singleCopy, 0L);
    }
}

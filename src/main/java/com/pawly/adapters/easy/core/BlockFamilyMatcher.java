package com.pawly.adapters.easy.core;

import com.pawly.adapters.easy.core.config.Configs;
import java.util.List;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2248;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_3481;
import net.minecraft.class_6862;
import net.minecraft.class_7923;

/**
 * Matches blocks and item stacks belonging to equivalent structural block families
 * (e.g., planks, stairs, slabs, doors, glass, wool) when {@code allowSameBlockFamily} is enabled.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.easy.core.a}.
 */
public final class BlockFamilyMatcher {

    private static final List<class_6862<class_2248>> SUBSTITUTE_FAMILIES = List.of(
        class_3481.field_15471, // PLANKS
        class_3481.field_15481, // WOODEN_BUTTONS
        class_3481.field_15479, // BUTTONS
        class_3481.field_15493, // WOODEN_DOORS
        class_3481.field_15495, // WOODEN_STAIRS
        class_3481.field_15459, // WOODEN_SLABS
        class_3481.field_15469, // WOODEN_FENCES
        class_3481.field_15504, // FENCE_GATES
        class_3481.field_16584, // WOODEN_PRESSURE_PLATES
        class_3481.field_25147, // PRESSURE_PLATES
        class_3481.field_24076, // WOODEN_TRAPDOORS
        class_3481.field_15487, // DOORS
        class_3481.field_16443, // STANDING_SIGNS
        class_3481.field_36265, // WALL_SIGNS
        class_3481.field_21490, // CEILING_HANGING_SIGNS
        class_3481.field_15501, // WALL_HANGING_SIGNS
        class_3481.field_15500, // SLABS
        class_3481.field_40105  // STAIRS
    );

    private static final List<String> PATH_FAMILIES = List.of(
        "glass_pane",
        "glass",
        "concrete_powder",
        "concrete",
        "terracotta",
        "wool_carpet",
        "carpet",
        "wool",
        "planks",
        "log",
        "stem",
        "wood",
        "hyphae"
    );

    private BlockFamilyMatcher() {
    }

    public static boolean blocksMatch(class_2248 first, class_2248 second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null || !Configs.ALLOW_SAME_BLOCK_FAMILY.getBooleanValue()) {
            return false;
        }
        return areInSameSubstituteFamily(first, second);
    }

    public static boolean stacksAreCompatibleVariants(class_1799 firstStack, class_1799 secondStack) {
        if (firstStack == null || secondStack == null || firstStack.method_7960() || secondStack.method_7960()) {
            return false;
        }
        if (firstStack.method_7909() == secondStack.method_7909()) {
            return true;
        }
        if (!Configs.ALLOW_SAME_BLOCK_FAMILY.getBooleanValue()) {
            return false;
        }
        class_1792 firstItem = firstStack.method_7909();
        if (!(firstItem instanceof class_1747 firstBlockItem)) {
            return false;
        }
        class_1792 secondItem = secondStack.method_7909();
        if (!(secondItem instanceof class_1747 secondBlockItem)) {
            return false;
        }
        return areInSameSubstituteFamily(firstBlockItem.method_7711(), secondBlockItem.method_7711());
    }

    private static boolean matchesPathFamily(String path, String family) {
        if ("glass".equals(family) && path.endsWith("_glass_pane")) {
            return false;
        }
        if ("terracotta".equals(family) && path.endsWith("_glazed_terracotta")) {
            return false;
        }
        return path.equals(family) || path.endsWith("_" + family);
    }

    private static boolean areInSameSubstituteFamily(class_2248 first, class_2248 second) {
        if (first.getClass() == second.getClass() && first.getClass() != class_2248.class) {
            return true;
        }
        class_2680 firstState = first.method_9564();
        class_2680 secondState = second.method_9564();
        for (class_6862<class_2248> tag : SUBSTITUTE_FAMILIES) {
            if (firstState.method_26164(tag) && secondState.method_26164(tag)) {
                return true;
            }
        }
        class_2960 firstId = class_7923.field_41175.method_10221(first);
        class_2960 secondId = class_7923.field_41175.method_10221(second);
        if (firstId == null || secondId == null) {
            return false;
        }
        String firstPath = firstId.method_12832();
        String secondPath = secondId.method_12832();
        for (String family : PATH_FAMILIES) {
            if (matchesPathFamily(firstPath, family) && matchesPathFamily(secondPath, family)) {
                return true;
            }
        }
        return false;
    }
}

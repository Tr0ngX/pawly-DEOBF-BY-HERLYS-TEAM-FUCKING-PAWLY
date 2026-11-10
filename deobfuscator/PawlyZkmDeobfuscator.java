import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import jdk.internal.org.objectweb.asm.commons.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * Custom Automated ASM Deobfuscation, String/Constant Inlining, Control-Flow Purification,
 * and Semantic Remapping Tool for ZKM-obfuscated Pawly Easy (Litematica Lore Pick Block) 1.21.11.
 *
 * Execute via:
 *   java --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED \
 *        --add-exports java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED \
 *        --add-exports java.base/jdk.internal.org.objectweb.asm.commons=ALL-UNNAMED \
 *        PawlyZkmDeobfuscator <input.jar> <output.jar>
 */
public class PawlyZkmDeobfuscator {

    // Complete semantic class remapping table (Obfuscated Internal Name -> Purified Semantic Internal Name)
    public static final Map<String, String> CLASS_MAP = new LinkedHashMap<>();
    public static final Map<String, String> MEMBER_MAP = new LinkedHashMap<>();
    public static final Set<String> OPAQUE_PREDICATE_FIELDS = new HashSet<>(Arrays.asList(
        "com/pawly/adapters/easy/EasyModule.b:Z",
        "com/pawly/adapters/easy/core/a.a:I",
        "com/pawly/adapters/easy/core/config/Configs.a:I",
        "com/pawly/adapters/easy/core/event/a.a:I",
        "com/pawly/adapters/easy/core/gui/GuiConfigs.a:Z",
        "com/pawly/adapters/easy/core/mixin/a.a:Z",
        "com/pawly/adapters/shared/a.a:I",
        "com/pawly/adapters/shared/b.a:Z"
    ));

    static {
        CLASS_MAP.put("com/pawly/adapters/easy/EasyModule$a", "com/pawly/adapters/easy/EasyModule$PawlyKeybindProvider");
        CLASS_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver$a", "com/pawly/adapters/easy/core/LoreStackResolver$ResolvedPickStack");
        CLASS_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver$b", "com/pawly/adapters/easy/core/LoreStackResolver$PendingManualPick");
        CLASS_MAP.put("com/pawly/adapters/easy/core/a", "com/pawly/adapters/easy/core/BlockFamilyMatcher");
        CLASS_MAP.put("com/pawly/adapters/easy/core/b", "com/pawly/adapters/easy/core/PlayerMessageHelper");
        CLASS_MAP.put("com/pawly/adapters/easy/core/c", "com/pawly/adapters/easy/core/CrafterSlotSynchronizer");
        CLASS_MAP.put("com/pawly/adapters/easy/core/d", "com/pawly/adapters/easy/core/LegitInventorySwapController");
        CLASS_MAP.put("com/pawly/adapters/easy/core/e", "com/pawly/adapters/easy/core/LitematicaLorePickBlockClientMod");
        CLASS_MAP.put("com/pawly/adapters/easy/core/e$a", "com/pawly/adapters/easy/core/LitematicaLorePickBlockClientMod$ModInitHandler");
        CLASS_MAP.put("com/pawly/adapters/easy/core/event/a", "com/pawly/adapters/easy/core/event/KeybindProviderImpl");
        CLASS_MAP.put("com/pawly/adapters/easy/core/f", "com/pawly/adapters/easy/core/NearbyMaterialScanner");
        CLASS_MAP.put("com/pawly/adapters/easy/core/f$a", "com/pawly/adapters/easy/core/NearbyMaterialScanner$MaterialRequirementEntry");
        CLASS_MAP.put("com/pawly/adapters/easy/core/g", "com/pawly/adapters/easy/core/SchematicPlacementGuard");
        CLASS_MAP.put("com/pawly/adapters/easy/core/g$a", "com/pawly/adapters/easy/core/SchematicPlacementGuard$PlacementTarget");
        CLASS_MAP.put("com/pawly/adapters/easy/core/mixin/a", "com/pawly/adapters/easy/core/mixin/BlockItemPlacementStateInvoker");
        CLASS_MAP.put("com/pawly/adapters/shared/a", "com/pawly/adapters/shared/ReflectionBridge");
        CLASS_MAP.put("com/pawly/adapters/shared/b", "com/pawly/adapters/shared/BasePawlyModuleAdapter");
        CLASS_MAP.put("com/pawly/adapters/shared/b$a", "com/pawly/adapters/shared/BasePawlyModuleAdapter$HookSpecification");
        CLASS_MAP.put("com/pawly/adapters/shared/c", "com/pawly/adapters/shared/ModuleSettingsSynchronizer");
        CLASS_MAP.put("com/pawly/adapters/shared/c$a", "com/pawly/adapters/shared/ModuleSettingsSynchronizer$SettingBinding");

        // Semantic method & field remappings
        MEMBER_MAP.put("com/pawly/adapters/easy/EasyModule$a.a(Lfi/dy/masa/malilib/config/options/ConfigHotkey;)Z", "isNotOpenConfigGuiHotkey");
        MEMBER_MAP.put("com/pawly/adapters/easy/EasyModule.a(Lnet/minecraft/class_310;Ljava/lang/String;)Z", "areAllKeysDown");
        MEMBER_MAP.put("com/pawly/adapters/easy/EasyModule.a(Lfi/dy/masa/malilib/hotkeys/KeyAction;Lfi/dy/masa/malilib/hotkeys/IKeybind;)Z", "onShowNearbyMaterialsKey");
        MEMBER_MAP.put("com/pawly/adapters/easy/EasyModule.b(Lfi/dy/masa/malilib/hotkeys/KeyAction;Lfi/dy/masa/malilib/hotkeys/IKeybind;)Z", "onOpenConfigGuiKey");

        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver$a.a()Lnet/minecraft/class_1792;", "item");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.a(Lnet/minecraft/class_1799;)Lcom/pawly/adapters/easy/core/LoreStackResolver$a;", "resolveStackFromInventory");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.a(Lnet/minecraft/class_1661;Lnet/minecraft/class_1799;II)I", "findExactMatchingSlot");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.b(Lnet/minecraft/class_1661;Lnet/minecraft/class_1799;II)I", "findCompatibleSlot");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.a(Lnet/minecraft/class_1661;Lnet/minecraft/class_1792;)Z", "isItemInHotbar");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.a(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;)Z", "areShulkerContainersCompatible");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.b(Lnet/minecraft/class_1799;)Z", "isShulkerBox");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.c(Lnet/minecraft/class_1799;)Z", "hasNonEmptyContainerContents");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.a(Lnet/minecraft/class_746;)Z", "isPlayerMovingOrJumping");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.a(Ljava/lang/String;)V", "sendChatNotification");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/LoreStackResolver.a()V", "resetState");

        MEMBER_MAP.put("com/pawly/adapters/easy/core/a.a(Ljava/lang/String;Ljava/lang/String;)Z", "matchesPathFamily");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/a.a(Lnet/minecraft/class_2248;Lnet/minecraft/class_2248;)Z", "areInSameSubstituteFamily");

        MEMBER_MAP.put("com/pawly/adapters/easy/core/b.a(Lnet/minecraft/class_746;Ljava/lang/String;)V", "sendActionBarMessage");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/b.b(Lnet/minecraft/class_746;Ljava/lang/String;)V", "sendChatMessage");

        MEMBER_MAP.put("com/pawly/adapters/easy/core/c.a(Lnet/minecraft/class_8887;)I", "computeDisabledSlotsBitmask");

        MEMBER_MAP.put("com/pawly/adapters/easy/core/config/Configs.a(Lfi/dy/masa/malilib/hotkeys/KeyAction;Lfi/dy/masa/malilib/hotkeys/IKeybind;)Z", "onShowNearbyMaterialsHotkey");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/config/Configs.b(Lfi/dy/masa/malilib/hotkeys/KeyAction;Lfi/dy/masa/malilib/hotkeys/IKeybind;)Z", "onOpenConfigGuiHotkey");

        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_310;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lnet/minecraft/class_3965;Lnet/minecraft/class_1799;)Z", "prepareSwapForStack");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;)Z", "isMatchingEmptyContainerStack");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_746;Lnet/minecraft/class_1799;)I", "findBestSourceSlot");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_746;Lnet/minecraft/class_1799;Lnet/minecraft/class_2338;)I", "findBestHotbarDestinationSlot");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a()Z", "isSessionContextValid");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.b()Z", "isPlayerStationaryAndIdle");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(ILnet/minecraft/class_1799;Lnet/minecraft/class_1799;)V", "verifySlotTransferAck");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.c()V", "suppressMovementInputs");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_3965;Lnet/minecraft/class_3965;)Z", "hitResultsMatch");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_310;Ljava/lang/String;)V", "sendRateLimitedWarning");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Method;", "resolveAccessibleMethod");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.b(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;", "resolveAccessibleField");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_310;)V", "invokeStartUseItem");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;)Z", "matchesSchematicState");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/d.a(Lnet/minecraft/class_310;Lnet/minecraft/class_490;)V", "openInventoryScreen");

        MEMBER_MAP.put("com/pawly/adapters/easy/core/f.a(Lnet/minecraft/class_310;Ljava/lang/String;)V", "sendPrefixedMessage");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/f.a(Lcom/pawly/adapters/easy/core/f$a;)Ljava/lang/String;", "getDisplayName");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/f.b(Lcom/pawly/adapters/easy/core/f$a;)J", "getAmount");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/f.a(Lnet/minecraft/class_1799;Lnet/minecraft/class_1792;)Lcom/pawly/adapters/easy/core/f$a;", "createEntry");

        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a()V", "clearPendingPlacementState");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_310;Lfi/dy/masa/litematica/world/WorldSchematic;Lnet/minecraft/class_3965;Lnet/minecraft/class_1799;)Lcom/pawly/adapters/easy/core/g$a;", "resolvePlacementTarget");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lfi/dy/masa/litematica/world/WorldSchematic;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;Lnet/minecraft/class_1799;Z)Z", "isHeldStackValidForSchematic");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_746;Lnet/minecraft/class_1799;Lnet/minecraft/class_3965;Lnet/minecraft/class_1747;)Lnet/minecraft/class_2680;", "predictPlacedBlockState");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_310;Lfi/dy/masa/litematica/world/WorldSchematic;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;)Z", "doesInteractiveBlockNeedStateCorrection");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lfi/dy/masa/litematica/world/WorldSchematic;Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;)Z", "isBlockAlreadyMatchingSchematic");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_2338;)Z", "isPositionWithinRenderedSchematic");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_310;)Z", "beginTemporarySneak");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.b(Lnet/minecraft/class_310;)V", "endTemporarySneak");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_10185;Z)Lnet/minecraft/class_10185;", "withShiftKeyDown");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_746;)Z", "isPlayerMoving");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.b(Lnet/minecraft/class_746;)Z", "isUsingOrHoldingConsumableItem");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_1799;)Z", "isRightClickUsableItem");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_2248;)Z", "isInteractiveBlock");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_2680;Lnet/minecraft/class_2680;)Z", "doOrientationPropertiesMatch");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_2680;Lnet/minecraft/class_2680;Ljava/lang/String;)Z", "doesPropertyDiffer");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_2680;Ljava/lang/String;)Lnet/minecraft/class_2769;", "findPropertyByName");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Lnet/minecraft/class_2680;Lnet/minecraft/class_2769;)Ljava/lang/Comparable;", "getPropertyValue");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Ljava/lang/String;)Z", "isOrientationPropertyName");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.b(Lnet/minecraft/class_2248;)Z", "isNonEnderChest");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(ZLnet/minecraft/class_310;)Ljava/lang/Object;", "invokeLitematicaPickBlock");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.b()Z", "isLitematicaLoaded");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.a(Ljava/lang/String;Ljava/lang/Throwable;)V", "logReflectionWarningOnce");
        MEMBER_MAP.put("com/pawly/adapters/easy/core/g.b(Ljava/lang/String;Ljava/lang/Throwable;)V", "logInputWarningOnce");

        MEMBER_MAP.put("com/pawly/adapters/shared/a.a(Ljava/lang/Class;)Ljava/lang/Class;", "boxPrimitiveType");
        MEMBER_MAP.put("com/pawly/adapters/shared/a.a([Ljava/lang/Class;[Ljava/lang/Object;)Z", "areArgumentsCompatible");
        MEMBER_MAP.put("com/pawly/adapters/shared/a.a(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/reflect/Field;", "resolveDeclaredFieldInHierarchy");

        MEMBER_MAP.put("com/pawly/adapters/shared/b.a(Ljava/lang/String;)Ljava/lang/Object;", "instantiateHelper");
        MEMBER_MAP.put("com/pawly/adapters/shared/b.a(Ljava/lang/Object;)Ljava/util/Map;", "createHelperCache");
        MEMBER_MAP.put("com/pawly/adapters/shared/b.a([Ljava/lang/String;Ljava/lang/reflect/Method;)Z", "matchesTargetMethodName");
        MEMBER_MAP.put("com/pawly/adapters/shared/b.a(Ljava/lang/String;Lcom/pawly/adapters/shared/b$a;)Z", "matchesHookEvent");
        MEMBER_MAP.put("com/pawly/adapters/shared/b.a(Lcom/pawly/adapters/shared/b$a;Ljava/lang/reflect/Method;)Z", "matchesMappedMethod");

        MEMBER_MAP.put("com/pawly/adapters/shared/c$a.a()Lcom/pawly/api/Setting;", "toPawlySetting");
        MEMBER_MAP.put("com/pawly/adapters/shared/c.a()Ljava/util/List;", "buildSettingsList");
        MEMBER_MAP.put("com/pawly/adapters/shared/c.b()V", "loadAndInitializeSettings");
        MEMBER_MAP.put("com/pawly/adapters/shared/c.a(Ljava/lang/String;)V", "onSettingValueChanged");
        MEMBER_MAP.put("com/pawly/adapters/shared/c.c()V", "syncNativeValuesToContext");
        MEMBER_MAP.put("com/pawly/adapters/shared/c.a(Lcom/pawly/adapters/shared/c$a;)Ljava/lang/String;", "readNativeFieldValueAsString");
        MEMBER_MAP.put("com/pawly/adapters/shared/c.b(Lcom/pawly/adapters/shared/c$a;)V", "applyContextValueToNativeField");
    }

    public static int[] extractZkmKey(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (mn.desc.equals("([C)Ljava/lang/String;") && (mn.access & Opcodes.ACC_STATIC) != 0) {
                for (AbstractInsnNode insn : mn.instructions) {
                    if (insn instanceof TableSwitchInsnNode tsw && tsw.labels.size() == 6) {
                        int[] key = new int[7];
                        for (int i = 0; i < 6; i++) {
                            key[i] = extractIntPush(tsw.labels.get(i));
                        }
                        key[6] = extractIntPush(tsw.dflt);
                        return key;
                    }
                }
            }
        }
        return null;
    }

    private static int extractIntPush(LabelNode label) {
        AbstractInsnNode cur = label.getNext();
        while (cur != null && (cur.getOpcode() == -1)) {
            cur = cur.getNext();
        }
        return getIntValue(cur);
    }

    private static Integer getIntValue(AbstractInsnNode insn) {
        if (insn == null) return null;
        int op = insn.getOpcode();
        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
            return op - Opcodes.ICONST_0;
        }
        if (insn instanceof IntInsnNode iin && (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH)) {
            return iin.operand;
        }
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer iv) {
            return iv;
        }
        return null;
    }

    public static String zkmDecrypt(String cipher, int[] key) {
        char[] chars = cipher.toCharArray();
        if (chars.length < 2 && chars.length > 0) {
            chars[0] = (char) (chars[0] ^ key[6]);
        }
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] ^ key[i % 7]);
        }
        return new String(chars);
    }

    public static List<String> decryptClinitStrings(ClassNode cn, int[] key) {
        List<String> decrypted = new ArrayList<>();
        if (key == null) return decrypted;
        MethodNode clinit = null;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("<clinit>")) {
                clinit = mn;
                break;
            }
        }
        if (clinit == null) return decrypted;

        AbstractInsnNode[] insns = clinit.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
            if (insns[i] instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                AbstractInsnNode next = insns[i].getNext();
                while (next != null && next.getOpcode() == -1) next = next.getNext();
                if (next instanceof MethodInsnNode min && min.desc.equals("(Ljava/lang/String;)[C")) {
                    decrypted.add(zkmDecrypt(s, key));
                } else if (next != null && next.getOpcode() == Opcodes.DUP) {
                    Integer firstLen = null;
                    for (int j = i + 1; j < Math.min(i + 15, insns.length); j++) {
                        Integer val = getIntValue(insns[j]);
                        if (val != null && insns[j + 1] instanceof VarInsnNode && getIntValue(insns[j + 2]) != null && getIntValue(insns[j + 2]) == -1) {
                            firstLen = val;
                            break;
                        }
                    }
                    if (firstLen != null) {
                        int pos = 0;
                        int curLen = firstLen;
                        int totalLen = s.length();
                        while (true) {
                            String sub = s.substring(pos, pos + curLen);
                            decrypted.add(zkmDecrypt(sub, key));
                            pos += curLen;
                            if (pos >= totalLen) break;
                            curLen = s.charAt(pos);
                            pos += 1;
                        }
                    }
                }
            }
        }
        return decrypted;
    }

    public static void purifyClassNode(ClassNode cn) {
        int[] key = extractZkmKey(cn);
        List<String> strings = decryptClinitStrings(cn, key);

        Set<String> zkmStringArrayFields = new HashSet<>();
        Set<String> zkmSingleStringFields = new HashSet<>();
        for (FieldNode fn : cn.fields) {
            if ((fn.access & Opcodes.ACC_STATIC) != 0) {
                if (fn.desc.equals("[Ljava/lang/String;") && fn.name.length() == 1) {
                    zkmStringArrayFields.add(fn.name);
                } else if (fn.desc.equals("Ljava/lang/String;") && fn.name.length() == 1) {
                    zkmSingleStringFields.add(fn.name);
                }
            }
        }

        for (MethodNode mn : cn.methods) {
            if (!mn.name.equals("<clinit>") && !strings.isEmpty()) {
                inlineStringsInMethod(cn.name, mn, strings, zkmStringArrayFields, zkmSingleStringFields);
            }
            stripOpaquePredicateFieldRefs(mn);
        }

        purifyClinit(cn, strings, zkmStringArrayFields);

        // Remove ZKM helper methods
        cn.methods.removeIf(mn ->
            ((mn.access & Opcodes.ACC_STATIC) != 0 && (mn.access & Opcodes.ACC_PRIVATE) != 0) &&
            (mn.desc.equals("(Ljava/lang/String;)[C") ||
             mn.desc.equals("([C)Ljava/lang/String;") ||
             mn.desc.equals("(IJ)J"))
        );

        // Remove ZKM fields (encrypted String[], long[], Long[], single-char String, and opaque predicates a/b)
        cn.fields.removeIf(fn ->
            ((fn.access & Opcodes.ACC_STATIC) != 0 && fn.name.length() == 1) &&
            (fn.desc.equals("[Ljava/lang/String;") ||
             fn.desc.equals("[J") ||
             fn.desc.equals("[Ljava/lang/Long;") ||
             fn.desc.equals("Ljava/lang/String;") ||
             fn.desc.equals("I") ||
             fn.desc.equals("Z"))
        );
    }

    private static void stripOpaquePredicateFieldRefs(MethodNode mn) {
        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            AbstractInsnNode next = insn.getNext();
            if (insn instanceof FieldInsnNode fin) {
                String sig = fin.owner + "." + fin.name + ":" + fin.desc;
                if (OPAQUE_PREDICATE_FIELDS.contains(sig)) {
                    if (fin.getOpcode() == Opcodes.GETSTATIC) {
                        mn.instructions.insertBefore(fin, new InsnNode(Opcodes.ICONST_0));
                        mn.instructions.remove(fin);
                    } else if (fin.getOpcode() == Opcodes.PUTSTATIC) {
                        mn.instructions.insertBefore(fin, new InsnNode(Opcodes.POP));
                        mn.instructions.remove(fin);
                    }
                }
            }
            insn = next;
        }
    }

    private static void inlineStringsInMethod(
            String owner,
            MethodNode mn,
            List<String> strings,
            Set<String> zkmStringArrayFields,
            Set<String> zkmSingleStringFields) {

        Set<Integer> stringArrayVars = new HashSet<>();
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode fin
                    && fin.getOpcode() == Opcodes.GETSTATIC
                    && fin.owner.equals(owner)
                    && zkmStringArrayFields.contains(fin.name)
                    && fin.desc.equals("[Ljava/lang/String;")) {
                AbstractInsnNode next = fin.getNext();
                if (next instanceof VarInsnNode vin && vin.getOpcode() == Opcodes.ASTORE) {
                    stringArrayVars.add(vin.var);
                    mn.instructions.remove(fin);
                    mn.instructions.remove(vin);
                    break;
                }
            }
        }

        AbstractInsnNode insn = mn.instructions.getFirst();
        while (insn != null) {
            AbstractInsnNode nextInsn = insn.getNext();
            if (insn instanceof VarInsnNode vin
                    && vin.getOpcode() == Opcodes.ALOAD
                    && stringArrayVars.contains(vin.var)) {
                AbstractInsnNode idxNode = vin.getNext();
                Integer idx = getIntValue(idxNode);
                if (idx != null && idxNode.getNext() != null && idxNode.getNext().getOpcode() == Opcodes.AALOAD) {
                    AbstractInsnNode aaload = idxNode.getNext();
                    nextInsn = aaload.getNext();
                    mn.instructions.insertBefore(vin, new LdcInsnNode(strings.get(idx)));
                    mn.instructions.remove(vin);
                    mn.instructions.remove(idxNode);
                    mn.instructions.remove(aaload);
                    insn = nextInsn;
                    continue;
                }
            } else if (insn instanceof FieldInsnNode fin
                    && fin.getOpcode() == Opcodes.GETSTATIC
                    && fin.owner.equals(owner)) {
                if (zkmStringArrayFields.contains(fin.name) && fin.desc.equals("[Ljava/lang/String;")) {
                    AbstractInsnNode idxNode = fin.getNext();
                    Integer idx = getIntValue(idxNode);
                    if (idx != null && idxNode.getNext() != null && idxNode.getNext().getOpcode() == Opcodes.AALOAD) {
                        AbstractInsnNode aaload = idxNode.getNext();
                        nextInsn = aaload.getNext();
                        mn.instructions.insertBefore(fin, new LdcInsnNode(strings.get(idx)));
                        mn.instructions.remove(fin);
                        mn.instructions.remove(idxNode);
                        mn.instructions.remove(aaload);
                        insn = nextInsn;
                        continue;
                    }
                } else if (zkmSingleStringFields.contains(fin.name) && fin.desc.equals("Ljava/lang/String;")) {
                    mn.instructions.insertBefore(fin, new LdcInsnNode(strings.get(0)));
                    mn.instructions.remove(fin);
                    insn = nextInsn;
                    continue;
                }
            }
            insn = nextInsn;
        }
    }

    private static void purifyClinit(ClassNode cn, List<String> strings, Set<String> zkmStringArrayFields) {
        MethodNode clinit = null;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("<clinit>")) {
                clinit = mn;
                break;
            }
        }
        if (clinit == null) return;

        switch (cn.name) {
            case "com/pawly/adapters/easy/EasyModule$a":
            case "com/pawly/adapters/easy/EasyModule":
            case "com/pawly/adapters/easy/core/e$a":
            case "com/pawly/adapters/easy/core/gui/GuiConfigs":
            case "com/pawly/adapters/easy/core/mixin/SchematicPickBlockMixin":
            case "com/pawly/adapters/easy/core/mixin/a":
            case "com/pawly/adapters/shared/b":
            case "com/pawly/adapters/shared/c":
                cn.methods.remove(clinit);
                return;
            case "com/pawly/adapters/easy/core/e":
                for (FieldNode fn : cn.fields) {
                    if (fn.name.equals("MOD_ID")) fn.value = "litematica_lore_pick_block";
                }
                cn.methods.remove(clinit);
                return;
            case "com/pawly/adapters/easy/core/f":
                for (FieldNode fn : cn.fields) {
                    if (fn.name.equals("MAX_DISPLAYED_TYPES")) fn.value = Integer.valueOf(50);
                }
                clinit.instructions.clear();
                clinit.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 50));
                clinit.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, "MAX_DISPLAYED_TYPES", "I"));
                clinit.instructions.add(new InsnNode(Opcodes.RETURN));
                return;
            case "com/pawly/adapters/easy/core/g":
                clinit.instructions.clear();
                clinit.instructions.add(new LdcInsnNode("litematica"));
                clinit.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, "LITEMATICA", "Ljava/lang/String;"));
                clinit.instructions.add(new LdcInsnNode("fi.dy.masa.litematica.util.WorldUtils"));
                clinit.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, "WORLD_UTILS", "Ljava/lang/String;"));
                clinit.instructions.add(new InsnNode(Opcodes.RETURN));
                return;
            case "com/pawly/adapters/easy/core/LoreStackResolver":
                clinit.instructions.clear();
                clinit.instructions.add(new LdcInsnNode(Long.valueOf(1000000000L)));
                clinit.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, "SERVER_SETTLE_NANOS", "J"));
                clinit.instructions.add(new InsnNode(Opcodes.RETURN));
                return;
        }

        LabelNode businessStart = null;
        for (AbstractInsnNode insn = clinit.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode fin && fin.getOpcode() == Opcodes.PUTSTATIC && fin.owner.equals(cn.name)) {
                if ((fin.name.equals("c") && fin.desc.equals("[Ljava/lang/Long;"))
                        || (fin.name.equals("CONFIG_FILE_NAME"))
                        || (zkmStringArrayFields.contains(fin.name) && !cn.name.endsWith("/Configs") && !cn.name.endsWith("/d"))) {
                    AbstractInsnNode probe = fin.getNext();
                    while (probe != null && !(probe instanceof JumpInsnNode)) {
                        probe = probe.getNext();
                    }
                    if (probe instanceof JumpInsnNode jin && jin.getOpcode() == Opcodes.GOTO) {
                        businessStart = jin.label;
                        break;
                    }
                }
            }
        }

        if (businessStart != null) {
            AbstractInsnNode cur = clinit.instructions.getFirst();
            while (cur != null && cur != businessStart) {
                AbstractInsnNode next = cur.getNext();
                clinit.instructions.remove(cur);
                cur = next;
            }
            if (cn.name.equals("com/pawly/adapters/easy/core/config/Configs")) {
                InsnList prefix = new InsnList();
                prefix.add(new LdcInsnNode("litematica_lore_pick_block.json"));
                prefix.add(new FieldInsnNode(Opcodes.PUTSTATIC, cn.name, "CONFIG_FILE_NAME", "Ljava/lang/String;"));
                clinit.instructions.insertBefore(businessStart, prefix);
            }
            inlineStringsInMethod(cn.name, clinit, strings, zkmStringArrayFields, Collections.emptySet());
        }
    }

    public static void deobfuscateJar(Path inputJar, Path outputJar) throws Exception {
        Map<String, String> combinedMap = new HashMap<>();
        combinedMap.putAll(CLASS_MAP);
        combinedMap.putAll(MEMBER_MAP);
        Remapper remapper = new SimpleRemapper(combinedMap);

        try (JarFile jar = new JarFile(inputJar.toFile());
             JarOutputStream jos = new JarOutputStream(Files.newOutputStream(outputJar))) {

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                byte[] rawBytes;
                try (InputStream is = jar.getInputStream(entry)) {
                    rawBytes = is.readAllBytes();
                }

                if (name.endsWith(".class")) {
                    byte origMajor = rawBytes[7];
                    if (origMajor > 61) {
                        rawBytes[7] = 61; // Allow JDK 17 internal ASM to read ClassFile major version 65
                    }
                    ClassReader cr = new ClassReader(rawBytes);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);

                    // Step 1: Decrypt ZKM strings, strip ZKM methods/fields/opaque predicates, purify <clinit>
                    purifyClassNode(cn);

                    // Step 2: Apply deep semantic renaming via ASM ClassRemapper
                    ClassNode remappedNode = new ClassNode();
                    ClassRemapper classRemapper = new ClassRemapper(remappedNode, remapper);
                    cn.accept(classRemapper);

                    ClassWriter cw = new ClassWriter(0);
                    remappedNode.accept(cw);
                    byte[] outBytes = cw.toByteArray();

                    String newEntryName = remappedNode.name + ".class";
                    JarEntry outEntry = new JarEntry(newEntryName);
                    jos.putNextEntry(outEntry);
                    jos.write(outBytes);
                    jos.closeEntry();
                } else if (name.equals("easy/hooks.json")) {
                    String json = new String(rawBytes, StandardCharsets.UTF_8);
                    json = json.replace(
                        "com.pawly.adapters.easy.core.mixin.a",
                        "com.pawly.adapters.easy.core.mixin.BlockItemPlacementStateInvoker"
                    );
                    JarEntry outEntry = new JarEntry(name);
                    jos.putNextEntry(outEntry);
                    jos.write(json.getBytes(StandardCharsets.UTF_8));
                    jos.closeEntry();
                } else {
                    JarEntry outEntry = new JarEntry(name);
                    jos.putNextEntry(outEntry);
                    jos.write(rawBytes);
                    jos.closeEntry();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: PawlyZkmDeobfuscator <input.jar> <output.jar>");
            return;
        }
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        deobfuscateJar(in, out);
        System.out.println("Successfully deobfuscated and remapped " + in + " -> " + out);
    }
}

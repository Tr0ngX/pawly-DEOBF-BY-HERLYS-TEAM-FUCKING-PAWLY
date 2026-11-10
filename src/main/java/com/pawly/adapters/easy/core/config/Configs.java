package com.pawly.adapters.easy.core.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pawly.adapters.easy.core.NearbyMaterialScanner;
import com.pawly.adapters.easy.core.SchematicPlacementGuard;
import com.pawly.adapters.easy.core.gui.GuiConfigs;
import com.pawly.adapters.shared.ReflectionBridge;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.class_310;

/**
 * Persistent MaLiLib configuration handler and option definitions.
 * Reconstructed from {@code com.pawly.adapters.easy.core.config.Configs}.
 */
public final class Configs implements IConfigHandler {

    private static final String CONFIG_FILE_NAME = "litematica_lore_pick_block.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final ConfigHotkey OPEN_CONFIG_GUI = new ConfigHotkey(
        "openConfigGui",
        "L,P",
        "Keybind to open the Litematica Lore Pick Block in-game configuration menu (Default: L + P)."
    );

    public static final ConfigHotkey SHOW_NEARBY_MATERIALS = new ConfigHotkey(
        "showNearbyMaterials",
        "M,R",
        "Shows the schematic materials still needed around the player within materialScanRadius (Default: M + R)."
    );

    public static final ConfigHotkey BYPASS_REFUSES = new ConfigHotkey(
        "bypassRefuses",
        "LEFT_SHIFT",
        KeybindSettings.MODIFIER_INGAME,
        "Hold this key to bypass wrong-block, extra-block, facing, movement-swap, and interaction-placement refusals."
    );

    public static final ConfigBoolean AUTO_PICK_BLOCK_ON_USE = new ConfigBoolean(
        "autoPickBlockOnUse",
        true,
        "Automatically pick the required schematic block when right-clicking to place."
    );

    public static final ConfigBoolean CANCEL_WHEN_UNABLE_TO_PICK = new ConfigBoolean(
        "cancelWhenUnableToPick",
        true,
        "Cancel right-click if unable to pick the required schematic block into hand (e.g. missing item or moving)."
    );

    public static final ConfigBoolean REFUSE_WRONG_PLACEMENT = new ConfigBoolean(
        "refuseWrongPlacement",
        true,
        "Refuse right-click if placing a block that does not match the schematic block at that position."
    );

    public static final ConfigBoolean REFUSE_EXTRA_PLACEMENT = new ConfigBoolean(
        "refuseExtraPlacement",
        true,
        "Refuse right-click if placing a block where the schematic expects air."
    );

    public static final ConfigBoolean ONLY_FACING_CORRECTLY = new ConfigBoolean(
        "onlyFacingCorrectly",
        true,
        "Refuse right-click unless the block would be placed matching the schematic's facing and orientation."
    );

    public static final ConfigBoolean AUTO_CRAFTER_DISABLER = new ConfigBoolean(
        "autoCrafterDisabler",
        true,
        "Automatically match open Crafter disabled slots to the schematic."
    );

    public static final ConfigBoolean PLACE_AGAINST_INTERACTIVE_BLOCKS = new ConfigBoolean(
        "placeAgainstInteractiveBlocks",
        true,
        "Briefly uses vanilla sneak input to place against a correct chest, hopper, or other interactive support block. Correctable gates, doors, repeaters, note blocks, and Crafters still open or toggle when their schematic state needs correction."
    );

    public static final ConfigBoolean REFUSE_CORRECT_BLOCK_INTERACTION = new ConfigBoolean(
        "refuseCorrectBlockInteraction",
        true,
        "Ignores right-click interaction with a correct schematic block while building. A gate, door, repeater, note block, or Crafter that needs a correctable state change is still allowed to open or toggle."
    );

    public static final ConfigBoolean ALLOW_SAME_BLOCK_FAMILY = new ConfigBoolean(
        "allowSameBlockFamily",
        false,
        "Allows a structurally equivalent variant, such as a spruce fence gate instead of an oak fence gate. Unrelated block families remain refused."
    );

    public static final ConfigInteger PLACEMENT_CLICKS_PER_SECOND = new ConfigInteger(
        "placementClicksPerSecond",
        0,
        0,
        20,
        "Repeated schematic placements per second while right-click is held. 0 keeps vanilla hold timing; 10 means one ordinary placement attempt every 2 ticks. Only valid placements are accelerated."
    );

    public static final ConfigInteger MATERIAL_SCAN_RADIUS = new ConfigInteger(
        "materialScanRadius",
        20,
        1,
        32,
        "Radius in blocks used by showNearbyMaterials (Default: 20, maximum: 32)."
    );

    public static final List<ConfigBoolean> BOOLEAN_OPTIONS = ImmutableList.of(
        AUTO_PICK_BLOCK_ON_USE,
        CANCEL_WHEN_UNABLE_TO_PICK,
        REFUSE_WRONG_PLACEMENT,
        REFUSE_EXTRA_PLACEMENT,
        ONLY_FACING_CORRECTLY,
        AUTO_CRAFTER_DISABLER,
        PLACE_AGAINST_INTERACTIVE_BLOCKS,
        REFUSE_CORRECT_BLOCK_INTERACTION,
        ALLOW_SAME_BLOCK_FAMILY
    );

    public static final List<IConfigBase> GENERIC_OPTIONS = ImmutableList.<IConfigBase>builder()
        .addAll(BOOLEAN_OPTIONS)
        .add(PLACEMENT_CLICKS_PER_SECOND)
        .add(MATERIAL_SCAN_RADIUS)
        .build();

    public static final List<ConfigHotkey> HOTKEYS = ImmutableList.of(
        OPEN_CONFIG_GUI,
        SHOW_NEARBY_MATERIALS,
        BYPASS_REFUSES
    );

    public static final List<IConfigBase> ALL_OPTIONS = ImmutableList.<IConfigBase>builder()
        .addAll(GENERIC_OPTIONS)
        .addAll(HOTKEYS)
        .build();

    private static final Configs INSTANCE = new Configs();

    private Configs() {
    }

    public static void initCallbacks() {
        OPEN_CONFIG_GUI.getKeybind().setCallback(Configs::onOpenConfigGuiHotkey);
        SHOW_NEARBY_MATERIALS.getKeybind().setCallback(Configs::onShowNearbyMaterialsHotkey);
    }

    public static Configs getInstance() {
        return INSTANCE;
    }

    public static Path getConfigFile() {
        return ReflectionBridge.configDir().resolve(CONFIG_FILE_NAME);
    }

    @Override
    public void load() {
        Path configFile = getConfigFile();
        if (!Files.isRegularFile(configFile)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject root = parsed.getAsJsonObject();
            ConfigUtils.readConfigBase(root, "Generic", GENERIC_OPTIONS);
            ConfigUtils.readHotkeys(root, "Hotkeys", HOTKEYS);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void save() {
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Generic", GENERIC_OPTIONS);
            ConfigUtils.writeHotkeys(root, "Hotkeys", HOTKEYS);
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static boolean onShowNearbyMaterialsHotkey(KeyAction action, IKeybind key) {
        if (SchematicPlacementGuard.hasOpenScreen(class_310.method_1551())) {
            return false;
        }
        NearbyMaterialScanner.showNearbyMaterials();
        return true;
    }

    private static boolean onOpenConfigGuiHotkey(KeyAction action, IKeybind key) {
        if (SchematicPlacementGuard.hasOpenScreen(class_310.method_1551())) {
            return false;
        }
        GuiBase.openGui(new GuiConfigs());
        return true;
    }
}

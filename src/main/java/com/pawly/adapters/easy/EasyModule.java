package com.pawly.adapters.easy;

import com.pawly.adapters.easy.core.LegitInventorySwapController;
import com.pawly.adapters.easy.core.LoreStackResolver;
import com.pawly.adapters.easy.core.NearbyMaterialScanner;
import com.pawly.adapters.easy.core.SchematicPlacementGuard;
import com.pawly.adapters.easy.core.config.Configs;
import com.pawly.adapters.shared.BasePawlyModuleAdapter;
import com.pawly.api.AutomationTiming;
import com.pawly.api.CrafterActions;
import com.pawly.api.Hook;
import com.pawly.api.PlacementActions;
import com.pawly.api.Setting;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.util.KeyCodes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_310;
import org.lwjgl.glfw.GLFW;

/**
 * Main Pawly adapter entry point for the Easy (Litematica Lore Pick Block) module.
 * Reconstructed from {@code com.pawly.adapters.easy.EasyModule} and inner class {@code EasyModule$a}.
 */
public final class EasyModule extends BasePawlyModuleAdapter {

    private boolean materialsHeld;
    private IKeybindProvider keys;
    private boolean registered;
    private static volatile EasyModule active;

    private static final class PawlyKeybindProvider implements IKeybindProvider {
        PawlyKeybindProvider(EasyModule module) {
        }

        @Override
        public void addKeysToMap(IKeybindManager manager) {
            for (ConfigHotkey hotkey : Configs.HOTKEYS) {
                if (hotkey != Configs.OPEN_CONFIG_GUI) {
                    manager.addKeybindToMap(hotkey.getKeybind());
                }
            }
        }

        @Override
        public void addHotkeys(IKeybindManager manager) {
            List<ConfigHotkey> filtered = Configs.HOTKEYS.stream()
                    .filter(PawlyKeybindProvider::isNotOpenConfigGuiHotkey)
                    .toList();
            manager.addHotkeysForCategory("Litematica Lore Pick Block", "litematica_lore_pick_block.hotkeys.category.main", filtered);
        }

        private static boolean isNotOpenConfigGuiHotkey(ConfigHotkey hotkey) {
            return hotkey != Configs.OPEN_CONFIG_GUI;
        }
    }

    public EasyModule() {
        super(
            "easy",
            "Easy",
            "0.1.0",
            "Lore-compatible Litematica pick block, placement guards, and nearby schematic material scanner",
            "litematica_lore_pick_block",
            "com.pawly.adapters.easy.core.",
            "com.pawly.adapters.easy.core.gui.GuiConfigs"
        );
        active = this;
    }

    public static String value(String settingId) {
        EasyModule instance = active;
        if (instance == null || instance.context == null) {
            return null;
        }
        return instance.context.value(settingId);
    }

    public static boolean legitInventory() {
        String raw = value("legitInventorySwap");
        return raw == null || Boolean.parseBoolean(raw);
    }

    public static boolean facingBypass() {
        return Boolean.parseBoolean(value("facingBypass"));
    }

    public static boolean leaveOnGhostBlocks() {
        return Boolean.parseBoolean(value("leaveOnGhostBlocks"));
    }

    @Override
    public List<Setting> settings() {
        List<Setting> allSettings = new ArrayList<>();
        allSettings.add(Setting.toggle("legitInventorySwap", "Open inventory to swap items", true));
        allSettings.add(Setting.toggle("facingBypass", "Spoof directional placement facing", false));
        allSettings.add(Setting.toggle("leaveOnGhostBlocks", "Disconnect when ghost block detected", false));
        allSettings.addAll(super.settings());
        return List.copyOf(allSettings);
    }

    @Override
    protected List<Hook> extraHooks() {
        return List.of(
            new Hook(
                "easy:container_slot_state",
                "net.minecraft.client.multiplayer.ClientPacketListener",
                "handleContainerSetSlot",
                "(Lnet/minecraft/network/protocol/game/ClientboundContainerSetSlotPacket;)V",
                Hook.Kind.HEAD,
                "",
                -1,
                0,
                true,
                new int[]{1}
            ),
            new Hook(
                "easy:container_content",
                "net.minecraft.client.multiplayer.ClientPacketListener",
                "handleContainerContent",
                "(Lnet/minecraft/network/protocol/game/ClientboundContainerSetContentPacket;)V",
                Hook.Kind.HEAD,
                "",
                -1,
                0,
                true,
                new int[]{1}
            ),
            new Hook(
                "easy:hotbar_slot",
                "net.minecraft.client.multiplayer.ClientPacketListener",
                "handleSetHeldSlot",
                "(Lnet/minecraft/network/protocol/game/ClientboundSetHeldSlotPacket;)V",
                Hook.Kind.HEAD,
                "",
                -1,
                0,
                true,
                new int[]{1}
            ),
            new Hook(
                "easy:click_container",
                "net.minecraft.client.multiplayer.MultiPlayerGameMode",
                "handleInventoryMouseClick",
                "(III Lnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",
                Hook.Kind.GUARD,
                "",
                -1,
                0,
                true,
                new int[]{1, 2, 3, 4, 5}
            ),
            new Hook(
                "easy:interact_block",
                "net.minecraft.client.multiplayer.MultiPlayerGameMode",
                "useItemOn",
                "(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
                Hook.Kind.RETURN_IF,
                "",
                -1,
                0,
                true,
                new int[]{1, 2, 3}
            )
        );
    }

    @Override
    protected Object extraEvent(String eventName, Object targetInstance, Object[] args) {
        if ("interact_block".equals(eventName) && facingBypass()) {
            return PlacementActions.manualFacing(eventName, targetInstance, args);
        }
        Object crafterResult = CrafterActions.event(eventName, args);
        if (crafterResult != null) {
            return crafterResult;
        }
        return LegitInventorySwapController.event(eventName, targetInstance, args);
    }

    @Override
    protected void initialize() {
        Configs.getInstance().load();
        ConfigManager.getInstance().registerConfigHandler("litematica_lore_pick_block", Configs.getInstance());
        Configs.OPEN_CONFIG_GUI.getKeybind().setCallback(EasyModule::onOpenConfigGuiKey);
        Configs.SHOW_NEARBY_MATERIALS.getKeybind().setCallback(EasyModule::onShowNearbyMaterialsKey);
        this.keys = new PawlyKeybindProvider(this);
    }

    @Override
    public void onEnable() {
        active = this;
        if (this.keys != null && !this.registered) {
            InputEventHandler.getKeybindManager().registerKeybindProvider(this.keys);
            InputEventHandler.getKeybindManager().updateUsedKeys();
            this.registered = true;
        }
    }

    @Override
    public void onSettingChanged(String settingId) {
        super.onSettingChanged(settingId);
        if (this.registered) {
            InputEventHandler.getKeybindManager().updateUsedKeys();
        }
    }

    @Override
    public void onDisable() {
        this.materialsHeld = false;
        LegitInventorySwapController.clear();
        if (this.keys != null && this.registered) {
            InputEventHandler.getKeybindManager().unregisterKeybindProvider(this.keys);
            InputEventHandler.getKeybindManager().updateUsedKeys();
            this.registered = false;
        }
    }

    @Override
    public void onTick(Object clientObj) {
        super.onTick(clientObj);
        if (!(clientObj instanceof class_310 client)) {
            return;
        }
        if (this.context != null && !this.context.enabled("easy")) {
            this.materialsHeld = false;
            LegitInventorySwapController.clear();
            return;
        }
        AutomationTiming.observeInventory("easy", client, client.field_1755, client.field_1724 != null ? client.field_1724.field_7512 : null, System.nanoTime());
        LegitInventorySwapController.tick(client);
        LoreStackResolver.tick(client);

        if (SchematicPlacementGuard.hasOpenScreen(client)) {
            this.materialsHeld = false;
            return;
        }
        boolean currentlyHeld = areAllKeysDown(client, Configs.SHOW_NEARBY_MATERIALS.getStringValue());
        if (currentlyHeld && !this.materialsHeld) {
            NearbyMaterialScanner.showNearbyMaterials();
        }
        this.materialsHeld = currentlyHeld;
    }

    private boolean areAllKeysDown(class_310 client, String keybindCombo) {
        if (client.method_22683() == null || keybindCombo == null || keybindCombo.isBlank()) {
            return false;
        }
        long windowHandle = client.method_22683().method_4490();
        String[] parts = keybindCombo.split(",");
        boolean hasAnyValidKey = false;
        for (String part : parts) {
            String keyName = part.trim();
            if (!keyName.isEmpty()) {
                int keyCode = KeyCodes.getKeyCodeFromName(keyName);
                if (keyCode < 0 || GLFW.glfwGetKey(windowHandle, keyCode) != GLFW.GLFW_PRESS) {
                    return false;
                }
                hasAnyValidKey = true;
            }
        }
        return hasAnyValidKey;
    }

    private static boolean onShowNearbyMaterialsKey(KeyAction action, IKeybind key) {
        if (EasyModule.active == null || EasyModule.active.context == null || !EasyModule.active.context.enabled("easy")) {
            return false;
        }
        if (SchematicPlacementGuard.hasOpenScreen(class_310.method_1551())) {
            return false;
        }
        NearbyMaterialScanner.showNearbyMaterials();
        return true;
    }

    private static boolean onOpenConfigGuiKey(KeyAction action, IKeybind key) {
        if (EasyModule.active != null && EasyModule.active.context != null) {
            EasyModule.active.context.openSettings("easy");
            return true;
        }
        return false;
    }
}

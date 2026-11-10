package com.pawly.adapters.shared;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pawly.api.ClientText;
import com.pawly.api.GameBindings;
import com.pawly.api.Setting;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Synchronizes module settings declared in {@code /<id>/settings.json} between
 * Pawly's {@code ModuleContext} and native MaLiLib config fields.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.shared.c}.
 */
final class ModuleSettingsSynchronizer {

    private final List<SettingBinding> bindings = new ArrayList<>();
    private final BasePawlyModuleAdapter module;
    private boolean syncing;

    record SettingBinding(
        String id,
        String label,
        String field,
        String owner,
        String kind,
        String defaultValue,
        double minimum,
        double maximum,
        double step,
        List<String> choices,
        String setter,
        boolean malilib
    ) {
        Setting toPawlySetting() {
            return new Setting(
                this.id,
                ClientText.translate(this.label),
                Setting.Kind.valueOf(this.kind),
                this.defaultValue,
                this.minimum,
                this.maximum,
                this.step,
                this.choices
            );
        }
    }

    ModuleSettingsSynchronizer(BasePawlyModuleAdapter module, String moduleId) {
        this.module = module;
        String resourcePath = "/" + moduleId + "/settings.json";
        try (InputStream is = ModuleSettingsSynchronizer.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                JsonArray array = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonArray();
                for (JsonElement element : array) {
                    JsonObject obj = element.getAsJsonObject();
                    List<String> choices = new ArrayList<>();
                    if (obj.has("choices")) {
                        for (JsonElement choice : obj.getAsJsonArray("choices")) {
                            choices.add(choice.getAsString());
                        }
                    }
                    this.bindings.add(new SettingBinding(
                        obj.get("id").getAsString(),
                        obj.get("label").getAsString(),
                        obj.get("field").getAsString(),
                        obj.has("owner") ? obj.get("owner").getAsString() : "",
                        obj.get("kind").getAsString(),
                        obj.get("defaultValue").getAsString(),
                        obj.has("minimum") ? obj.get("minimum").getAsDouble() : 0.0,
                        obj.has("maximum") ? obj.get("maximum").getAsDouble() : 0.0,
                        obj.has("step") ? obj.get("step").getAsDouble() : 0.0,
                        List.copyOf(choices),
                        obj.has("setter") ? obj.get("setter").getAsString() : "",
                        obj.has("malilib") && obj.get("malilib").getAsBoolean()
                    ));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read settings for " + moduleId, e);
        }
    }

    List<Setting> buildSettingsList() {
        return this.bindings.stream()
                .map(SettingBinding::toPawlySetting)
                .toList();
    }

    void loadAndInitializeSettings() {
        for (SettingBinding binding : this.bindings) {
            if ("ACTION".equals(binding.kind())) {
                continue;
            }
            String nativeVal = readNativeFieldValueAsString(binding);
            String contextVal = this.module.context.value(binding.id());
            if (Objects.equals(contextVal, binding.defaultValue())
                    && nativeVal != null
                    && !nativeVal.isBlank()
                    && !Objects.equals(nativeVal, binding.defaultValue())) {
                this.syncing = true;
                try {
                    this.module.context.set(binding.id(), nativeVal);
                } finally {
                    this.syncing = false;
                }
            } else {
                applyContextValueToNativeField(binding);
            }
        }
    }

    void onSettingValueChanged(String settingId) {
        if (this.syncing) {
            return;
        }
        for (SettingBinding binding : this.bindings) {
            if (binding.id().equals(settingId)) {
                applyContextValueToNativeField(binding);
                this.module.saveNativeSettings();
                return;
            }
        }
    }

    void syncNativeValuesToContext() {
        if (this.module.context == null) {
            return;
        }
        this.syncing = true;
        try {
            for (SettingBinding binding : this.bindings) {
                if ("ACTION".equals(binding.kind())) {
                    continue;
                }
                String nativeVal = readNativeFieldValueAsString(binding);
                if (nativeVal != null && !Objects.equals(nativeVal, this.module.context.value(binding.id()))) {
                    this.module.context.set(binding.id(), nativeVal);
                }
            }
        } finally {
            this.syncing = false;
        }
    }

    private String readNativeFieldValueAsString(SettingBinding binding) {
        try {
            Class<?> ownerClass = GameBindings.forName(this.module.prefix + binding.owner(), true, getClass().getClassLoader());
            Field field = ReflectionBridge.field(ownerClass, binding.field());
            Object fieldObj = field.get(null);
            if (fieldObj == null) {
                return null;
            }
            if (binding.malilib()) {
                return switch (binding.kind()) {
                    case "TOGGLE" -> Boolean.toString((Boolean) ReflectionBridge.call(fieldObj, "getBooleanValue"));
                    case "NUMBER" -> Double.toString(((Number) ReflectionBridge.call(fieldObj, "getIntegerValue")).doubleValue());
                    case "CHOICE" -> String.valueOf(ReflectionBridge.call(fieldObj, "getOptionListValue"));
                    case "TEXT", "KEY" -> {
                        try {
                            yield (String) ReflectionBridge.call(fieldObj, "getStringValue");
                        } catch (Exception ignored) {
                            Object keybind = ReflectionBridge.call(fieldObj, "getKeybind");
                            yield (String) ReflectionBridge.call(keybind, "getStringValue");
                        }
                    }
                    default -> null;
                };
            }
            return String.valueOf(fieldObj);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void applyContextValueToNativeField(SettingBinding binding) {
        if (this.module.context == null || "ACTION".equals(binding.kind())) {
            return;
        }
        String rawValue = this.module.context.value(binding.id());
        try {
            Class<?> ownerClass = GameBindings.forName(this.module.prefix + binding.owner(), true, getClass().getClassLoader());
            Field field = ReflectionBridge.field(ownerClass, binding.field());
            if (binding.malilib()) {
                Object configOption = field.get(null);
                switch (binding.kind()) {
                    case "TOGGLE" -> ReflectionBridge.call(configOption, "setBooleanValue", Boolean.parseBoolean(rawValue));
                    case "NUMBER" -> ReflectionBridge.call(configOption, "setIntegerValue", (int) Math.round(Double.parseDouble(rawValue)));
                    case "CHOICE", "TEXT" -> ReflectionBridge.call(configOption, "setValueFromString", rawValue);
                    case "KEY" -> {
                        Object keybind = ReflectionBridge.call(configOption, "getKeybind");
                        ReflectionBridge.call(keybind, "setValueFromString", Setting.normalize(rawValue));
                    }
                    default -> {}
                }
                return;
            }
            Class<?> fieldType = field.getType();
            Object convertedValue;
            if (fieldType == boolean.class || fieldType == Boolean.class) {
                convertedValue = Boolean.parseBoolean(rawValue);
            } else if (fieldType == int.class || fieldType == Integer.class) {
                convertedValue = (int) Math.round(Double.parseDouble(rawValue));
            } else if (fieldType == double.class || fieldType == Double.class) {
                convertedValue = Double.parseDouble(rawValue);
            } else if (fieldType == float.class || fieldType == Float.class) {
                convertedValue = (float) Double.parseDouble(rawValue);
            } else if (fieldType.isEnum()) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object enumConstant = Enum.valueOf((Class<Enum>) fieldType, rawValue);
                convertedValue = enumConstant;
            } else {
                convertedValue = rawValue;
            }
            if (binding.setter() != null && !binding.setter().isBlank()) {
                ReflectionBridge.call(ownerClass, binding.setter(), convertedValue);
            } else {
                field.set(null, convertedValue);
            }
        } catch (Exception ignored) {
        }
    }
}

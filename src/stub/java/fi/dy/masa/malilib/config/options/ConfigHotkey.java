package fi.dy.masa.malilib.config.options;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
public class ConfigHotkey implements IConfigBase {
    public ConfigHotkey(String name, String defaultStorageString, String comment) {}
    public ConfigHotkey(String name, String defaultStorageString, KeybindSettings settings, String comment) {}
    public IKeybind getKeybind() { return null; }
    public String getStringValue() { return ""; }
    public void setValueFromString(String value) {}
}

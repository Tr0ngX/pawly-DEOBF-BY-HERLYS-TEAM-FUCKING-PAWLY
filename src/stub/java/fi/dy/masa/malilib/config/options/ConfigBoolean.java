package fi.dy.masa.malilib.config.options;
import fi.dy.masa.malilib.config.IConfigBase;
public class ConfigBoolean implements IConfigBase {
    public ConfigBoolean(String name, boolean defaultValue, String comment) {}
    public boolean getBooleanValue() { return false; }
    public void setBooleanValue(boolean value) {}
}

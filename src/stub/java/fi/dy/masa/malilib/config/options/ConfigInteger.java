package fi.dy.masa.malilib.config.options;
import fi.dy.masa.malilib.config.IConfigBase;
public class ConfigInteger implements IConfigBase {
    public ConfigInteger(String name, int defaultValue, int minValue, int maxValue, String comment) {}
    public int getIntegerValue() { return 0; }
    public void setIntegerValue(int value) {}
}

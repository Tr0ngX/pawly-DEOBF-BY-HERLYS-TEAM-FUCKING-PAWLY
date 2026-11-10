package fi.dy.masa.malilib.config;
import com.google.gson.JsonObject;
import java.util.List;
public class ConfigUtils {
    public static void readConfigBase(JsonObject root, String category, List<? extends IConfigBase> options) {}
    public static void readHotkeys(JsonObject root, String category, List<? extends IConfigBase> hotkeys) {}
    public static void writeConfigBase(JsonObject root, String category, List<? extends IConfigBase> options) {}
    public static void writeHotkeys(JsonObject root, String category, List<? extends IConfigBase> hotkeys) {}
}

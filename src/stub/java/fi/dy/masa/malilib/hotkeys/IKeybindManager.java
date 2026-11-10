package fi.dy.masa.malilib.hotkeys;
import java.util.List;
public interface IKeybindManager {
    void addKeybindToMap(IKeybind keybind);
    void addHotkeysForCategory(String modName, String keyCategory, List<? extends Object> hotkeys);
    void registerKeybindProvider(IKeybindProvider provider);
    void unregisterKeybindProvider(IKeybindProvider provider);
    void updateUsedKeys();
}

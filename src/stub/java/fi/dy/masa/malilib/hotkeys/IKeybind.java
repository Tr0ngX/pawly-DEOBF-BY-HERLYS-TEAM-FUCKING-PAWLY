package fi.dy.masa.malilib.hotkeys;
public interface IKeybind {
    boolean isKeybindHeld();
    void setCallback(IHotkeyCallback callback);
    String getStringValue();
    void setValueFromString(String value);
}

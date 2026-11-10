package fi.dy.masa.malilib.config;
public interface IConfigManager {
    void registerConfigHandler(String modId, IConfigHandler handler);
}

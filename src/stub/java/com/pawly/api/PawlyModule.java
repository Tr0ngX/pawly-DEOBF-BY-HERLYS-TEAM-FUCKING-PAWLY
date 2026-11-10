package com.pawly.api;
import java.util.List;

public interface PawlyModule extends AutoCloseable {
    ModuleInfo info();
    List<Setting> settings();
    String unavailableReason();
    List<Hook> hooks();
    void onLoad(ModuleContext context) throws Exception;
    void onEnable();
    void onDisable();
    void onSettingChanged(String id);
    void onTick(Object client);
    Object onHook(String event, Object self, Object[] args) throws Exception;
    void onAction(String id);
    @Override
    void close() throws Exception;
}

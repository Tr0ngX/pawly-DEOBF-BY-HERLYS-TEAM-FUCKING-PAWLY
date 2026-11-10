package com.pawly.api;
import java.nio.file.Path;

public interface ModuleContext {
    Path configDirectory();
    boolean enabled(String id);
    String value(String id);
    void set(String id, String value);
    void log(String message);
    void error(String message, Throwable t);
    void openSettings(String moduleId);
}

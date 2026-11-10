package fi.dy.masa.malilib.gui;
import fi.dy.masa.malilib.config.IConfigBase;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
public abstract class GuiConfigsBase extends GuiBase {
    public GuiConfigsBase(int listX, int listY, String modId, net.minecraft.class_437 parent, String titleKey, Object... args) {}
    public abstract List<ConfigOptionWrapper> getConfigs();
    public static class ConfigOptionWrapper {
        public static List<ConfigOptionWrapper> createFor(Collection<? extends IConfigBase> configs) {
            return Collections.emptyList();
        }
    }
}

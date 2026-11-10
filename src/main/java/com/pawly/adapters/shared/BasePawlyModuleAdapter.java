package com.pawly.adapters.shared;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pawly.api.ClientText;
import com.pawly.api.GameBindings;
import com.pawly.api.Hook;
import com.pawly.api.ModuleContext;
import com.pawly.api.ModuleInfo;
import com.pawly.api.PawlyModule;
import com.pawly.api.Setting;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_310;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Abstract base adapter bridging Pawly's module lifecycle, JSON hook specifications,
 * and MaLiLib settings synchronization.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.shared.b}.
 */
public abstract class BasePawlyModuleAdapter implements PawlyModule {

    protected ModuleContext context;
    protected boolean initialized;
    private class_437 previous;
    private final ModuleSettingsSynchronizer nativeSettings;
    private boolean nativeScreenOpen;
    private final String group;
    protected final String prefix;
    private final ModuleInfo info;
    private final String conflict;
    private final String settingsScreen;
    private final Map<String, HookSpecification> specs = new LinkedHashMap<>();
    private final Map<Object, Map<String, Object>> helpers = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Object staticOwner = new Object();

    public record HookSpecification(
        String owner,
        String method,
        String descriptor,
        String kind,
        String event,
        String callback,
        String target,
        int ordinal,
        int argument,
        boolean optional,
        int[] parameters
    ) {}

    protected BasePawlyModuleAdapter(
            String id,
            String name,
            String version,
            String description,
            String conflictModId,
            String prefix,
            String settingsScreenClass) {
        this.group = id;
        this.prefix = prefix;
        this.info = new ModuleInfo(id, name, version, description);
        this.conflict = conflictModId;
        this.settingsScreen = settingsScreenClass;
        this.nativeSettings = new ModuleSettingsSynchronizer(this, id);

        String resourcePath = "/" + id + "/hooks.json";
        try (InputStream is = BasePawlyModuleAdapter.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                JsonArray array = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonArray();
                for (JsonElement element : array) {
                    JsonObject obj = element.getAsJsonObject();
                    JsonArray paramsJson = obj.has("parameters") ? obj.getAsJsonArray("parameters") : new JsonArray();
                    int[] params = new int[paramsJson.size()];
                    for (int i = 0; i < paramsJson.size(); i++) {
                        params[i] = paramsJson.get(i).getAsInt();
                    }
                    HookSpecification spec = new HookSpecification(
                        obj.get("owner").getAsString(),
                        obj.get("method").getAsString(),
                        obj.get("descriptor").getAsString(),
                        obj.get("kind").getAsString(),
                        obj.get("event").getAsString(),
                        obj.get("callback").getAsString(),
                        obj.has("target") ? obj.get("target").getAsString() : "",
                        obj.has("ordinal") ? obj.get("ordinal").getAsInt() : -1,
                        obj.has("argument") ? obj.get("argument").getAsInt() : 0,
                        obj.has("optional") && obj.get("optional").getAsBoolean(),
                        params
                    );
                    this.specs.put(spec.event(), spec);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read hooks for " + id, e);
        }
    }

    @Override
    public ModuleInfo info() {
        return this.info;
    }

    @Override
    public List<Setting> settings() {
        return this.nativeSettings.buildSettingsList();
    }

    @Override
    public String unavailableReason() {
        if (this.conflict != null && !this.conflict.isBlank()
                && FabricLoader.getInstance().isModLoaded(this.conflict)) {
            return "Standalone " + this.info.name() + " mod is loaded (" + this.conflict + ")";
        }
        return null;
    }

    @Override
    public List<Hook> hooks() {
        List<Hook> result = new ArrayList<>();
        for (HookSpecification spec : this.specs.values()) {
            String eventId = this.group + ":" + spec.event();
            result.add(new Hook(
                eventId,
                spec.owner(),
                spec.method(),
                spec.descriptor(),
                Hook.Kind.valueOf(spec.kind()),
                spec.target(),
                spec.ordinal(),
                spec.argument(),
                spec.optional(),
                spec.parameters()
            ));
        }
        result.addAll(extraHooks());
        return List.copyOf(result);
    }

    protected List<Hook> extraHooks() {
        return List.of();
    }

    @Override
    public void onLoad(ModuleContext moduleContext) throws Exception {
        this.context = moduleContext;
        ReflectionBridge.context = moduleContext;
        if (!this.initialized) {
            initialize();
            this.initialized = true;
        }
        this.nativeSettings.loadAndInitializeSettings();
    }

    @Override
    public void onSettingChanged(String settingId) {
        ReflectionBridge.context = this.context;
        this.nativeSettings.onSettingValueChanged(settingId);
    }

    @Override
    public void onTick(Object clientObj) {
        if (!this.nativeScreenOpen) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client == null) {
            return;
        }
        class_437 currentScreen = client.field_1755;
        if (currentScreen == null || !currentScreen.getClass().getName().equals(this.settingsScreen)) {
            this.nativeScreenOpen = false;
            saveNativeSettings();
            this.nativeSettings.syncNativeValuesToContext();
        }
    }

    protected void saveNativeSettings() {
    }

    protected abstract void initialize() throws Exception;

    @Override
    public Object onHook(String eventName, Object targetInstance, Object[] args) throws Exception {
        ReflectionBridge.context = this.context;
        String localEvent = eventName.startsWith(this.group + ":")
                ? eventName.substring(this.group.length() + 1)
                : eventName;
        HookSpecification spec = this.specs.get(localEvent);
        if (spec == null) {
            return extraEvent(localEvent, targetInstance, args);
        }

        String callbackClassName = this.prefix + spec.callback().substring(0, spec.callback().indexOf('#'));
        String callbackMethodName = spec.callback().substring(spec.callback().indexOf('#') + 1);
        Class<?> callbackClass = GameBindings.forName(callbackClassName, true, getClass().getClassLoader());
        Method callbackMethod = Arrays.stream(callbackClass.getDeclaredMethods())
                .filter(m -> matchesMappedMethod(spec, m))
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException(spec.callback()));
        callbackMethod.setAccessible(true);

        Object ownerKey = targetInstance != null ? targetInstance : this.staticOwner;
        Object helperInstance = Modifier.isStatic(callbackMethod.getModifiers())
                ? null
                : this.helpers
                    .computeIfAbsent(ownerKey, BasePawlyModuleAdapter::createHelperCache)
                    .computeIfAbsent(callbackClassName, BasePawlyModuleAdapter::instantiateHelper);

        Class<?>[] paramTypes = callbackMethod.getParameterTypes();
        Object[] invokeArgs = new Object[paramTypes.length];
        int argCursor = 0;

        if (paramTypes.length > 0 && paramTypes[0] == Object.class
                && (args.length == 0 || !CallbackInfo.class.isAssignableFrom(paramTypes[0]))) {
            invokeArgs[0] = targetInstance;
            argCursor = 1;
        }

        CallbackInfo callbackInfo = null;
        CallbackInfoReturnable<Object> callbackReturnable = null;
        int userArgIndex = 0;

        for (int i = argCursor; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            if (paramType == CallbackInfo.class) {
                callbackInfo = new CallbackInfo();
                invokeArgs[i] = callbackInfo;
            } else if (paramType == CallbackInfoReturnable.class) {
                callbackReturnable = new CallbackInfoReturnable<>();
                invokeArgs[i] = callbackReturnable;
            } else if (userArgIndex < args.length) {
                invokeArgs[i] = args[userArgIndex++];
            }
        }

        Object directReturn = callbackMethod.invoke(helperInstance, invokeArgs);
        if ("GUARD".equals(spec.kind())) {
            if (callbackInfo != null) {
                return Boolean.valueOf(!callbackInfo.isCancelled());
            }
            if (callbackReturnable != null) {
                return Boolean.valueOf(!callbackReturnable.isCancelled());
            }
            return directReturn instanceof Boolean ? directReturn : Boolean.TRUE;
        }
        if ("RETURN_IF".equals(spec.kind())) {
            if (callbackReturnable != null && callbackReturnable.isCancelled()) {
                return callbackReturnable.getReturnValue();
            }
            return directReturn;
        }
        return directReturn;
    }

    protected Object extraEvent(String eventName, Object targetInstance, Object[] args) throws Exception {
        return null;
    }

    protected Object core(String simpleClassName, String methodName, Object... args) {
        try {
            Class<?> targetClass = GameBindings.forName(this.prefix + simpleClassName, true, getClass().getClassLoader());
            String[] candidateNames = methodName.split("\\|");
            Method resolvedMethod = Arrays.stream(targetClass.getDeclaredMethods())
                    .filter(m -> matchesTargetMethodName(candidateNames, m))
                    .findFirst()
                    .orElseThrow();
            resolvedMethod.setAccessible(true);
            return resolvedMethod.invoke(null, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onAction(String actionId) {
        try {
            ReflectionBridge.context = this.context;
            if (!"open_native_settings".equals(actionId)
                    || this.settingsScreen == null
                    || this.settingsScreen.isBlank()) {
                return;
            }
            class_310 client = class_310.method_1551();
            if (client == null) {
                return;
            }
            this.previous = client.field_1755;
            Class<?> screenClass = GameBindings.forName(this.settingsScreen, true, getClass().getClassLoader());
            class_437 screenInstance = null;
            for (Constructor<?> ctor : screenClass.getDeclaredConstructors()) {
                ctor.setAccessible(true);
                if (ctor.getParameterCount() == 1 && class_437.class.isAssignableFrom(ctor.getParameterTypes()[0])) {
                    screenInstance = (class_437) ctor.newInstance(this.previous);
                    break;
                }
                if (ctor.getParameterCount() == 0) {
                    screenInstance = (class_437) ctor.newInstance();
                }
            }
            if (screenInstance != null) {
                this.nativeScreenOpen = true;
                client.method_1507(screenInstance);
            }
        } catch (Exception e) {
            if (this.context != null) {
                this.context.error("Failed opening native settings for " + this.group, e);
            }
        }
    }

    @Override
    public void close() throws Exception {
    }

    private static Object instantiateHelper(String className) {
        try {
            Constructor<?> ctor = GameBindings.forName(className, true, BasePawlyModuleAdapter.class.getClassLoader())
                    .getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> createHelperCache(Object owner) {
        return new ConcurrentHashMap<>();
    }

    private static boolean matchesTargetMethodName(String[] candidateNames, Method method) {
        return Arrays.stream(candidateNames).anyMatch(name -> GameBindings.methodNamed(method, name));
    }

    private static boolean matchesHookEvent(String methodName, HookSpecification spec) {
        return GameBindings.methodNamed(
            Arrays.stream(
                Class.class.cast(ReflectionBridge.type(spec.owner())).getDeclaredMethods()
            ).findFirst().orElseThrow(),
            methodName
        );
    }

    private static boolean matchesMappedMethod(HookSpecification spec, Method method) {
        String targetMethodName = spec.callback().substring(spec.callback().indexOf('#') + 1);
        return GameBindings.methodNamed(method, targetMethodName);
    }
}

package com.pawly.adapters.shared;

import com.pawly.api.GameBindings;
import com.pawly.api.ModuleContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_304;

/**
 * Reflection and game-binding helper bridge for Pawly module adapters.
 * Reconstructed from obfuscated class {@code com.pawly.adapters.shared.a}.
 */
public final class ReflectionBridge {

    public static volatile ModuleContext context;
    private static final Map<String, Field> fields = new ConcurrentHashMap<>();

    public ReflectionBridge() {
    }

    public static Path configDir() {
        ModuleContext activeContext = context;
        return activeContext != null ? activeContext.configDirectory() : Paths.get(".");
    }

    public static class_304 key(class_304 keyMapping) {
        return keyMapping;
    }

    public static Field field(Class<?> ownerType, String fieldName) {
        String cacheKey = ownerType.getName() + "#" + fieldName;
        return fields.computeIfAbsent(cacheKey, key -> resolveDeclaredFieldInHierarchy(ownerType, fieldName, key));
    }

    public static Object get(Object target, String fieldName) {
        try {
            Class<?> ownerType = target instanceof Class<?> clazz ? clazz : target.getClass();
            Object instance = target instanceof Class<?> ? null : target;
            return field(ownerType, fieldName).get(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void set(Object target, String fieldName, Object value) {
        try {
            Class<?> ownerType = target instanceof Class<?> clazz ? clazz : target.getClass();
            Object instance = target instanceof Class<?> ? null : target;
            field(ownerType, fieldName).set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> boxPrimitiveType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static boolean areArgumentsCompatible(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                if (parameterTypes[i].isPrimitive()) {
                    return false;
                }
            } else if (!boxPrimitiveType(parameterTypes[i]).isInstance(arg)) {
                return false;
            }
        }
        return true;
    }

    public static Object call(Object target, String methodName, Object... args) {
        Object[] safeArgs = args == null ? new Object[0] : args;
        Class<?> ownerType = target instanceof Class<?> clazz ? clazz : target.getClass();
        Object instance = target instanceof Class<?> ? null : target;
        try {
            for (Class<?> current = ownerType; current != null; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (GameBindings.methodNamed(method, methodName)
                            && areArgumentsCompatible(method.getParameterTypes(), safeArgs)) {
                        method.setAccessible(true);
                        return method.invoke(instance, safeArgs);
                    }
                }
            }
            throw new NoSuchMethodException(ownerType.getName() + "#" + methodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class<?> type(String className) {
        try {
            return GameBindings.forName(className, false, ReflectionBridge.class.getClassLoader());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object create(String className, Object... args) {
        Object[] safeArgs = args == null ? new Object[0] : args;
        try {
            Class<?> targetType = type(className);
            for (Constructor<?> constructor : targetType.getDeclaredConstructors()) {
                if (areArgumentsCompatible(constructor.getParameterTypes(), safeArgs)) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(safeArgs);
                }
            }
            throw new NoSuchMethodException(className + ".<init>");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field resolveDeclaredFieldInHierarchy(Class<?> ownerType, String fieldName, String cacheKey) {
        Class<?> current = ownerType;
        while (current != null) {
            try {
                Field resolved = GameBindings.getDeclaredField(current, fieldName);
                resolved.setAccessible(true);
                return resolved;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalStateException("Missing field " + cacheKey);
    }
}

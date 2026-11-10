package com.pawly.api;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class GameBindings {
    public static Class<?> forName(String name) throws ClassNotFoundException { return Class.forName(name); }
    public static Class<?> forName(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException {
        return Class.forName(name, initialize, loader);
    }
    public static Field getDeclaredField(Class<?> type, String name) throws NoSuchFieldException {
        return type.getDeclaredField(name);
    }
    public static Method getDeclaredMethod(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return type.getDeclaredMethod(name, parameterTypes);
    }
    public static boolean methodNamed(Method method, String name) {
        return method.getName().equals(name);
    }
}

package com.pawly.api;
import java.util.List;

public record Setting(
    String id,
    String label,
    Kind kind,
    String defaultValue,
    double minimum,
    double maximum,
    double step,
    List<String> choices
) {
    public enum Kind {
        TOGGLE, NUMBER, RANGE, CHOICE, TEXT, KEY, ACTION
    }
    public static Setting toggle(String id, String label, boolean defaultValue) {
        return new Setting(id, label, Kind.TOGGLE, Boolean.toString(defaultValue), 0, 0, 0, List.of());
    }
    public static Setting range(String id, String label, double defMin, double defMax, double min, double max, double step) {
        return new Setting(id, label, Kind.RANGE, defMin + ".." + defMax, min, max, step, List.of());
    }
    public static double[] endpoints(String value) {
        return new double[]{0.0, 0.0};
    }
    public static String normalize(String value) {
        return value == null ? "" : value;
    }
}

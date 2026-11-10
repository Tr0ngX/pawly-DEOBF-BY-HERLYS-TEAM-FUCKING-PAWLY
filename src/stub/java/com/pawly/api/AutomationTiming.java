package com.pawly.api;

public final class AutomationTiming {
    public static void inventoryClicked(String group) {}
    public static void inventoryClicked(String group, long now) {}
    public static boolean inventoryReady(String group) { return true; }
    public static boolean inventoryReady(String group, long now) { return true; }
    public static void observeInventory(String group, Object client, Object screen, Object handler, long now) {}
    public static int placeExtraTicks(String group) { return 0; }
}

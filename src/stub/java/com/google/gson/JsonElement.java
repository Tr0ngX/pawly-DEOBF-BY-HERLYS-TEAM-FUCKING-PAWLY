package com.google.gson;

public class JsonElement {
    public boolean isJsonObject() { return false; }
    public JsonObject getAsJsonObject() { return new JsonObject(); }
    public boolean isJsonPrimitive() { return false; }
    public String getAsString() { return ""; }
    public int getAsInt() { return 0; }
    public double getAsDouble() { return 0.0; }
    public boolean getAsBoolean() { return false; }
    public JsonArray getAsJsonArray() { return new JsonArray(); }
}

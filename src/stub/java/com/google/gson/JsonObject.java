package com.google.gson;

public class JsonObject extends JsonElement {
    public boolean has(String memberName) { return false; }
    public JsonElement get(String memberName) { return null; }
    public JsonArray getAsJsonArray(String memberName) { return new JsonArray(); }
}

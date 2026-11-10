package com.google.gson;
import java.util.*;

public class JsonArray extends JsonElement implements Iterable<JsonElement> {
    public int size() { return 0; }
    public JsonElement get(int i) { return null; }
    @Override
    public Iterator<JsonElement> iterator() { return Collections.emptyIterator(); }
}

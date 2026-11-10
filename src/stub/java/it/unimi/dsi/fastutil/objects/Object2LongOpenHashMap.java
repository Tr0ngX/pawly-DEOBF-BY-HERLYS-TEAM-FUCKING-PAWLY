package it.unimi.dsi.fastutil.objects;
import java.util.HashMap;

public class Object2LongOpenHashMap<K> extends HashMap<K, Long> {
    public long addTo(K k, long incr) {
        long cur = getOrDefault(k, 0L) + incr;
        put(k, cur);
        return cur;
    }
}

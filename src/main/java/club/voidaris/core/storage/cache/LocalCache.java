package club.voidaris.core.storage.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalCache<K, V> implements Cache<K, V> {

    private final Map<K, V> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<V> get(K key) {
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public void put(K key, V value) {
        entries.put(key, value);
    }

    @Override
    public void invalidate(K key) {
        entries.remove(key);
    }

    @Override
    public void invalidateAll() {
        entries.clear();
    }
}

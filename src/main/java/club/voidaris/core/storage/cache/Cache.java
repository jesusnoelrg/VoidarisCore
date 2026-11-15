package club.voidaris.core.storage.cache;

import java.util.Optional;

/**
 * Backend-agnostic cache, so a Redis implementation can replace {@link LocalCache} if Voidaris becomes a network.
 */
public interface Cache<K, V> {

    Optional<V> get(K key);

    void put(K key, V value);

    void invalidate(K key);

    void invalidateAll();
}

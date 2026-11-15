package club.voidaris.core.cosmetics;

import club.voidaris.core.storage.Database;
import club.voidaris.core.storage.cache.Cache;
import club.voidaris.core.storage.cache.LocalCache;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Equipped cosmetic per slot, cached for online players and persisted in cosmetic_selections.
 */
final class Selections {

    private static final String SELECT = "SELECT slot, cosmetic_id FROM cosmetic_selections WHERE player_uuid = ?";
    private static final String UPSERT = """
            INSERT INTO cosmetic_selections (player_uuid, slot, cosmetic_id) VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE cosmetic_id = VALUES(cosmetic_id)""";

    private final Database database;
    private final Logger logger;
    private final Cache<UUID, Map<String, String>> cache = new LocalCache<>();

    Selections(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    /**
     * Never completes exceptionally: on failure the player is cached with no selections, so defaults apply.
     */
    CompletableFuture<Void> load(UUID player) {
        if (!database.isConnected()) {
            cache.put(player, new ConcurrentHashMap<>());
            return CompletableFuture.completedFuture(null);
        }
        return database.query(connection -> {
                    Map<String, String> selections = new ConcurrentHashMap<>();
                    try (var statement = connection.prepareStatement(SELECT)) {
                        statement.setString(1, player.toString());
                        try (var result = statement.executeQuery()) {
                            while (result.next()) {
                                selections.put(result.getString("slot"), result.getString("cosmetic_id"));
                            }
                        }
                    }
                    return selections;
                })
                .exceptionally(e -> {
                    logger.log(Level.WARNING, "Could not load cosmetic selections of " + player, e);
                    return new ConcurrentHashMap<>();
                })
                .thenAccept(selections -> cache.put(player, selections));
    }

    void unload(UUID player) {
        cache.invalidate(player);
    }

    void clear() {
        cache.invalidateAll();
    }

    Optional<String> get(UUID player, String slot) {
        return cache.get(player).map(selections -> selections.get(slot));
    }

    CompletableFuture<Void> select(UUID player, String slot, String cosmeticId) {
        return database.<Void>query(connection -> {
            try (var statement = connection.prepareStatement(UPSERT)) {
                statement.setString(1, player.toString());
                statement.setString(2, slot);
                statement.setString(3, cosmeticId);
                statement.executeUpdate();
            }
            return null;
        }).whenComplete((ignored, e) -> {
            if (e != null && database.isConnected()) {
                logger.log(Level.WARNING, "Could not save the " + slot + " cosmetic of " + player, e);
            }
        // Only updates a cached entry, so a player who quit before the write finished is not cached again.
        }).thenRun(() -> cache.get(player).ifPresent(selections -> selections.put(slot, cosmeticId)));
    }
}

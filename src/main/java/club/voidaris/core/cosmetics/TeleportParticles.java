package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Teleport particle cosmetics defined under teleport-particles in cosmetics.yml.
 */
final class TeleportParticles {

    private static final String SECTION = "teleport-particles.";
    private static final String COSMETICS = SECTION + "cosmetics";

    private final ConfigManager configs;
    private final Logger logger;

    TeleportParticles(ConfigManager configs, Logger logger) {
        this.configs = configs;
        this.logger = logger;
    }

    Set<String> ids() {
        // Read from the file on disk only (no bundled defaults), so a cosmetic deleted by the admin stops existing.
        var cosmetics = configs.cosmetics().get().getConfigurationSection(COSMETICS);
        return cosmetics == null ? Set.of() : cosmetics.getKeys(false);
    }

    String defaultSelection() {
        return configs.cosmetics().get().getString(SECTION + "default-selection", "");
    }

    /**
     * Empty when the cosmetic does not exist or one of its layers is invalid.
     */
    Optional<TeleportParticle> find(String id) {
        if (!ids().contains(id)) {
            return Optional.empty();
        }
        String path = COSMETICS + "." + id;
        var cosmetic = configs.cosmetics().get().getConfigurationSection(path);
        if (cosmetic == null) {
            return Optional.empty();
        }
        String name = cosmetic.isSet("name") ? cosmetic.getString("name") : id;
        List<ParticleLayer> layers = new ArrayList<>();
        var raw = cosmetic.getMapList("layers");
        for (int i = 0; i < raw.size(); i++) {
            var layer = ParticleLayer.parse(path + ".layers[" + i + "]", raw.get(i), logger);
            if (layer.isEmpty()) {
                return Optional.empty();
            }
            layers.add(layer.get());
        }
        return Optional.of(new TeleportParticle(id, name, List.copyOf(layers)));
    }

    Optional<TeleportParticle> usable(Player player, String id) {
        return find(id).filter(particle -> player.hasPermission(Keys.Permission.COSMETICS_TELEPORT_PARTICLE + id));
    }

    List<TeleportParticle> usable(Player player) {
        return ids().stream().flatMap(id -> usable(player, id).stream()).toList();
    }
}

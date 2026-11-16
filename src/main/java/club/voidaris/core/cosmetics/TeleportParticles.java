package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            var layer = layer(path + ".layers[" + i + "]", raw.get(i));
            if (layer.isEmpty()) {
                return Optional.empty();
            }
            layers.add(layer.get());
        }
        return Optional.of(new TeleportParticle(id, name, List.copyOf(layers)));
    }

    private Optional<ParticleLayer> layer(String path, Map<?, ?> raw) {
        var section = new MemoryConfiguration();
        raw.forEach((key, value) -> section.set(String.valueOf(key), value));
        String type = section.getString("particle", "");
        Particle particle;
        ParticleLayer.Shape shape;
        try {
            particle = Particle.valueOf(type.toUpperCase(Locale.ROOT));
            shape = ParticleLayer.Shape.valueOf(section.getString("shape", "burst").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown particle or shape in cosmetics.yml at " + path);
            return Optional.empty();
        }
        Class<?> data = particle.getDataType();
        if (data != Void.class && data != Particle.DustOptions.class && data != Particle.DustTransition.class) {
            logger.warning("Particle " + particle + " at " + path + " needs data other than colors and is not supported.");
            return Optional.empty();
        }
        List<Color> colors = new ArrayList<>();
        for (String hex : section.getStringList("colors")) {
            try {
                colors.add(Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16)));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid color " + hex + " in cosmetics.yml at " + path + " (expected #RRGGBB)");
                return Optional.empty();
            }
        }
        return Optional.of(new ParticleLayer(particle, shape,
                section.getInt("count", shape == ParticleLayer.Shape.HELIX ? 1 : 30),
                section.getDouble("spread", shape == ParticleLayer.Shape.BURST ? 0.5 : 0),
                section.getDouble("speed", 0),
                List.copyOf(colors),
                (float) section.getDouble("size", 1),
                section.getDouble("radius", 0.8),
                section.getDouble("height", 2),
                section.getInt("duration", shape == ParticleLayer.Shape.HELIX ? 20 : 0),
                Math.max(1, section.getInt("strands", 2))));
    }

    Optional<TeleportParticle> usable(Player player, String id) {
        return find(id).filter(particle -> player.hasPermission(Keys.Permission.COSMETICS_TELEPORT_PARTICLE + id));
    }

    List<TeleportParticle> usable(Player player) {
        return ids().stream().flatMap(id -> usable(player, id).stream()).toList();
    }
}

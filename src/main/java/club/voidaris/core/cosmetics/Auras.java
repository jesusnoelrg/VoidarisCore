package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.MonthDay;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Aura cosmetics defined under auras in cosmetics.yml, parsed once per {@link #load()} because they are looked up on
 * every player movement.
 */
final class Auras {

    private static final String SECTION = "auras.";
    private static final String COSMETICS = SECTION + "cosmetics";
    private static final int DEFAULT_INTERVAL = 2;

    private final ConfigManager configs;
    private final Logger logger;
    private volatile Map<String, Definition> definitions = Map.of();

    /**
     * @param season null for the layers used outside every season
     */
    private record Variant(Season season, List<ParticleLayer> layers) {
    }

    private record Definition(String name, boolean premium, int interval, List<Variant> variants) {
    }

    Auras(ConfigManager configs, Logger logger) {
        this.configs = configs;
        this.logger = logger;
    }

    void load() {
        var cosmetics = configs.cosmetics().get().getConfigurationSection(COSMETICS);
        Map<String, Definition> loaded = new LinkedHashMap<>();
        int invalid = 0;
        if (cosmetics != null) {
            for (String id : cosmetics.getKeys(false)) {
                var cosmetic = cosmetics.getConfigurationSection(id);
                var definition = cosmetic == null ? Optional.<Definition>empty() : definition(COSMETICS + "." + id, id, cosmetic);
                if (definition.isPresent()) {
                    loaded.put(id, definition.get());
                } else {
                    invalid++;
                }
            }
        }
        definitions = Map.copyOf(loaded);
        logger.info("Loaded " + loaded.size() + " aura cosmetics" + (invalid > 0 ? " (" + invalid + " invalid)." : "."));
    }

    void clear() {
        definitions = Map.of();
    }

    Set<String> ids() {
        return definitions.keySet();
    }

    String defaultSelection() {
        return configs.cosmetics().get().getString(SECTION + "default-selection", "");
    }

    /**
     * Empty when the aura does not exist or has no layers today (e.g. a seasonal one out of season).
     */
    Optional<Aura> find(String id) {
        var definition = definitions.get(id);
        if (definition == null) {
            return Optional.empty();
        }
        MonthDay today = MonthDay.now();
        return definition.variants().stream()
                .filter(variant -> variant.season() == null || variant.season().contains(today))
                .findFirst()
                .map(variant -> new Aura(id, definition.name(), definition.premium(), definition.interval(), variant.layers()));
    }

    Optional<Aura> usable(Player player, String id) {
        return find(id).filter(aura -> player.hasPermission(Keys.Permission.COSMETICS_AURA + id));
    }

    List<Aura> usable(Player player) {
        return ids().stream().flatMap(id -> usable(player, id).stream()).toList();
    }

    private Optional<Definition> definition(String path, String id, ConfigurationSection cosmetic) {
        List<Variant> variants = new ArrayList<>();
        var seasons = cosmetic.getConfigurationSection("seasons");
        if (seasons != null) {
            for (String name : seasons.getKeys(false)) {
                String seasonPath = path + ".seasons." + name;
                var season = seasons.getConfigurationSection(name);
                if (season == null) {
                    continue;
                }
                var range = Season.parse(season, seasonPath, logger);
                var layers = layers(seasonPath, season);
                if (range.isEmpty() || layers.isEmpty()) {
                    return Optional.empty();
                }
                variants.add(new Variant(range.get(), layers.get()));
            }
        }
        if (cosmetic.isSet("layers")) {
            var layers = layers(path, cosmetic);
            if (layers.isEmpty()) {
                return Optional.empty();
            }
            variants.add(new Variant(null, layers.get()));
        }
        String name = cosmetic.isSet("name") ? cosmetic.getString("name") : id;
        int interval = Math.max(1, cosmetic.getInt("interval", DEFAULT_INTERVAL));
        return Optional.of(new Definition(name, cosmetic.getBoolean("premium"), interval, List.copyOf(variants)));
    }

    private Optional<List<ParticleLayer>> layers(String path, ConfigurationSection section) {
        List<ParticleLayer> layers = new ArrayList<>();
        var raw = section.getMapList("layers");
        for (int i = 0; i < raw.size(); i++) {
            String layerPath = path + ".layers[" + i + "]";
            var layer = ParticleLayer.parse(layerPath, raw.get(i), logger);
            if (layer.isEmpty()) {
                return Optional.empty();
            }
            if (layer.get().shape() != ParticleLayer.Shape.TRAIL && layer.get().shape() != ParticleLayer.Shape.ORBIT) {
                logger.warning("Auras only support the trail and orbit shapes (cosmetics.yml at " + layerPath + ").");
                return Optional.empty();
            }
            layers.add(layer.get());
        }
        return Optional.of(List.copyOf(layers));
    }
}

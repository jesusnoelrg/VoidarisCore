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
 * Join effect cosmetics defined under join-effects in cosmetics.yml, parsed once per {@link #load()}.
 */
final class JoinEffects {

    private static final String SECTION = "join-effects.";
    private static final String COSMETICS = SECTION + "cosmetics";

    private final ConfigManager configs;
    private final Logger logger;
    private volatile Map<String, Definition> definitions = Map.of();

    /**
     * @param season null for the steps used outside every season
     */
    private record Variant(Season season, List<JoinEffectStep> steps) {
    }

    private record Definition(String name, boolean premium, List<Variant> variants) {
    }

    JoinEffects(ConfigManager configs, Logger logger) {
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
        logger.info("Loaded " + loaded.size() + " join effect cosmetics" + (invalid > 0 ? " (" + invalid + " invalid)." : "."));
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
     * Ticks between joining and the start of the effect.
     */
    int delay() {
        return Math.max(1, configs.cosmetics().get().getInt(SECTION + "delay", 20));
    }

    int range() {
        return Math.max(1, configs.cosmetics().get().getInt(SECTION + "range", 32));
    }

    /**
     * Seconds between two previews from the menu by the same player.
     */
    int previewCooldown() {
        return Math.max(0, configs.cosmetics().get().getInt(SECTION + "preview-cooldown", 10));
    }

    /**
     * Empty when the effect does not exist or has no steps today (e.g. a seasonal one out of season).
     */
    Optional<JoinEffect> find(String id) {
        var definition = definitions.get(id);
        if (definition == null) {
            return Optional.empty();
        }
        MonthDay today = MonthDay.now();
        return definition.variants().stream()
                .filter(variant -> variant.season() == null || variant.season().contains(today))
                .findFirst()
                .map(variant -> new JoinEffect(id, definition.name(), definition.premium(), variant.steps()));
    }

    Optional<JoinEffect> usable(Player player, String id) {
        return find(id).filter(effect -> player.hasPermission(Keys.Permission.COSMETICS_JOIN_EFFECT + id));
    }

    List<JoinEffect> usable(Player player) {
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
                var steps = steps(seasonPath, season);
                if (range.isEmpty() || steps.isEmpty()) {
                    return Optional.empty();
                }
                variants.add(new Variant(range.get(), steps.get()));
            }
        }
        if (cosmetic.isSet("effects")) {
            var steps = steps(path, cosmetic);
            if (steps.isEmpty()) {
                return Optional.empty();
            }
            variants.add(new Variant(null, steps.get()));
        }
        String name = cosmetic.isSet("name") ? cosmetic.getString("name") : id;
        return Optional.of(new Definition(name, cosmetic.getBoolean("premium"), List.copyOf(variants)));
    }

    private Optional<List<JoinEffectStep>> steps(String path, ConfigurationSection section) {
        List<JoinEffectStep> steps = new ArrayList<>();
        var raw = section.getMapList("effects");
        for (int i = 0; i < raw.size(); i++) {
            var step = JoinEffectStep.parse(path + ".effects[" + i + "]", raw.get(i), logger);
            if (step.isEmpty()) {
                return Optional.empty();
            }
            steps.add(step.get());
        }
        return Optional.of(List.copyOf(steps));
    }
}

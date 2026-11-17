package club.voidaris.core.cosmetics;

import club.voidaris.core.config.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.MonthDay;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The cosmetics of one {@link MessageCategory}.
 */
final class MessageCosmetics {

    private final ConfigManager configs;
    private final Logger logger;
    private final MessageCategory category;
    private final String path;

    MessageCosmetics(ConfigManager configs, Logger logger, MessageCategory category) {
        this.configs = configs;
        this.logger = logger;
        this.category = category;
        this.path = category.section + ".cosmetics";
    }

    MessageCategory category() {
        return category;
    }

    Set<String> ids() {
        // Read from the file on disk only (no bundled defaults), so a cosmetic deleted by the admin stops existing.
        var cosmetics = configs.cosmetics().get().getConfigurationSection(path);
        return cosmetics == null ? Set.of() : cosmetics.getKeys(false);
    }

    String defaultSelection() {
        return configs.cosmetics().get().getString(category.section + ".default-selection", "");
    }

    /**
     * Empty when the cosmetic does not exist or has no format today (e.g. a seasonal one out of season).
     */
    Optional<MessageCosmetic> find(String id) {
        if (!ids().contains(id)) {
            return Optional.empty();
        }
        var cosmetic = configs.cosmetics().get().getConfigurationSection(path + "." + id);
        if (cosmetic == null) {
            return Optional.empty();
        }
        String name = cosmetic.isSet("name") ? cosmetic.getString("name") : id;
        return format(id, cosmetic).map(format -> new MessageCosmetic(id, name, format));
    }

    Optional<MessageCosmetic> usable(Player player, String id) {
        return find(id).filter(message -> player.hasPermission(category.permission + id));
    }

    List<MessageCosmetic> usable(Player player) {
        return ids().stream().flatMap(id -> usable(player, id).stream()).toList();
    }

    private Optional<String> format(String id, ConfigurationSection cosmetic) {
        var seasons = cosmetic.getConfigurationSection("seasons");
        if (seasons != null) {
            MonthDay today = MonthDay.now();
            for (String name : seasons.getKeys(false)) {
                var season = seasons.getConfigurationSection(name);
                if (season != null && season.isSet("format")
                        && Season.parse(season, path + "." + id + ".seasons." + name, logger)
                        .filter(range -> range.contains(today)).isPresent()) {
                    return Optional.of(season.getString("format"));
                }
            }
        }
        return cosmetic.isSet("format") ? Optional.of(cosmetic.getString("format")) : Optional.empty();
    }
}

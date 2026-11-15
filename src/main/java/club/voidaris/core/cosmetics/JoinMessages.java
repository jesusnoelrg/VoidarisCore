package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Join message cosmetics defined under join-messages in cosmetics.yml.
 */
final class JoinMessages {

    private static final String SECTION = "join-messages.";
    private static final String COSMETICS = SECTION + "cosmetics";

    private final ConfigManager configs;
    private final Logger logger;

    JoinMessages(ConfigManager configs, Logger logger) {
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
     * Empty when the cosmetic does not exist or has no format today (e.g. a seasonal one out of season).
     */
    Optional<JoinMessage> find(String id) {
        if (!ids().contains(id)) {
            return Optional.empty();
        }
        var cosmetic = configs.cosmetics().get().getConfigurationSection(COSMETICS + "." + id);
        if (cosmetic == null) {
            return Optional.empty();
        }
        String name = cosmetic.isSet("name") ? cosmetic.getString("name") : id;
        return format(id, cosmetic).map(format -> new JoinMessage(id, name, format));
    }

    Optional<JoinMessage> usable(Player player, String id) {
        return find(id).filter(message -> player.hasPermission(Keys.Permission.COSMETICS_JOIN_MESSAGE + id));
    }

    List<JoinMessage> usable(Player player) {
        return ids().stream().flatMap(id -> usable(player, id).stream()).toList();
    }

    private Optional<String> format(String id, ConfigurationSection cosmetic) {
        var seasons = cosmetic.getConfigurationSection("seasons");
        if (seasons != null) {
            MonthDay today = MonthDay.now();
            for (String name : seasons.getKeys(false)) {
                var season = seasons.getConfigurationSection(name);
                if (season != null && season.isSet("format") && isActive(id + ".seasons." + name, season, today)) {
                    return Optional.of(season.getString("format"));
                }
            }
        }
        return cosmetic.isSet("format") ? Optional.of(cosmetic.getString("format")) : Optional.empty();
    }

    private boolean isActive(String path, ConfigurationSection season, MonthDay today) {
        try {
            MonthDay from = MonthDay.parse("--" + season.getString("from"));
            MonthDay to = MonthDay.parse("--" + season.getString("to"));
            // A range such as 12-01 to 01-06 wraps around the new year.
            return from.isAfter(to)
                    ? !today.isBefore(from) || !today.isAfter(to)
                    : !today.isBefore(from) && !today.isAfter(to);
        } catch (DateTimeParseException e) {
            logger.warning("Invalid date in cosmetics.yml at join-messages.cosmetics." + path + " (expected MM-dd): " + e.getParsedString());
            return false;
        }
    }
}

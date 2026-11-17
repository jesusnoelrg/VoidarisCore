package club.voidaris.core.cosmetics;

import org.bukkit.configuration.ConfigurationSection;

import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * An inclusive MM-dd range of a seasonal cosmetic, which may wrap around the new year.
 */
record Season(MonthDay from, MonthDay to) {

    /**
     * Empty (and logged) when from or to is not a valid MM-dd date.
     */
    static Optional<Season> parse(ConfigurationSection season, String path, Logger logger) {
        try {
            return Optional.of(new Season(MonthDay.parse("--" + season.getString("from")),
                    MonthDay.parse("--" + season.getString("to"))));
        } catch (DateTimeParseException e) {
            logger.warning("Invalid date in cosmetics.yml at " + path + " (expected MM-dd): " + e.getParsedString());
            return Optional.empty();
        }
    }

    boolean contains(MonthDay day) {
        return from.isAfter(to)
                ? !day.isBefore(from) || !day.isAfter(to)
                : !day.isBefore(from) && !day.isAfter(to);
    }
}

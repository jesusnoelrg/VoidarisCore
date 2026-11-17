package club.voidaris.core.cosmetics;

import club.voidaris.core.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Color cosmetics of one {@link ColorCategory}, parsed once per {@link #load()}, plus the custom hex colors players
 * pick with its command.
 */
final class ColorCosmetics {

    private static final Pattern HEX = Pattern.compile("(?:&?#)?([0-9a-fA-F]{6})");

    private final ConfigManager configs;
    private final Logger logger;
    private final ColorCategory category;
    private final String cosmeticsPath;
    private volatile Map<String, ColorCosmetic> definitions = Map.of();

    ColorCosmetics(ConfigManager configs, Logger logger, ColorCategory category) {
        this.configs = configs;
        this.logger = logger;
        this.category = category;
        this.cosmeticsPath = category.section + ".cosmetics";
    }

    ColorCategory category() {
        return category;
    }

    void load() {
        var cosmetics = configs.cosmetics().get().getConfigurationSection(cosmeticsPath);
        Map<String, ColorCosmetic> loaded = new LinkedHashMap<>();
        int invalid = 0;
        if (cosmetics != null) {
            for (String id : cosmetics.getKeys(false)) {
                var cosmetic = cosmetics.getConfigurationSection(id);
                // Custom picks are stored as #rrggbb in the same selection slot, so ids must never look like one.
                if (cosmetic == null || isCustom(id)) {
                    logger.warning("Invalid " + category.label + " " + cosmeticsPath + "." + id + ".");
                    invalid++;
                    continue;
                }
                String color = cosmetic.getString("color", "");
                if (!isValid(color)) {
                    logger.warning("Invalid MiniMessage color '" + color + "' in " + cosmeticsPath + "." + id + ".");
                    invalid++;
                    continue;
                }
                String name = cosmetic.isSet("name") ? cosmetic.getString("name") : id;
                loaded.put(id, new ColorCosmetic(id, name, cosmetic.getBoolean("premium"), color));
            }
        }
        definitions = Map.copyOf(loaded);
        logger.info("Loaded " + loaded.size() + " " + category.label + " cosmetics"
                + (invalid > 0 ? " (" + invalid + " invalid)." : "."));
    }

    void clear() {
        definitions = Map.of();
    }

    Set<String> ids() {
        return definitions.keySet();
    }

    /**
     * In the order of cosmetics.yml, since Map.copyOf does not keep it.
     */
    List<String> orderedIds() {
        var cosmetics = configs.cosmetics().get().getConfigurationSection(cosmeticsPath);
        return cosmetics == null ? List.of()
                : cosmetics.getKeys(false).stream().filter(definitions::containsKey).toList();
    }

    String defaultSelection() {
        return configs.cosmetics().get().getString(category.section + ".default-selection", "");
    }

    Optional<ColorCosmetic> find(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    Optional<ColorCosmetic> usable(Player player, String id) {
        return find(id).filter(color -> player.hasPermission(category.permission + id));
    }

    List<ColorCosmetic> usable(Player player) {
        return orderedIds().stream().flatMap(id -> usable(player, id).stream()).toList();
    }

    /**
     * @return the color as stored ({@code #rrggbb}) when the text is a hex color (#RRGGBB, &#RRGGBB or RRGGBB)
     */
    static Optional<String> parseCustom(String text) {
        var matcher = HEX.matcher(text);
        return matcher.matches() ? Optional.of("#" + matcher.group(1).toLowerCase(Locale.ROOT)) : Optional.empty();
    }

    static boolean isCustom(String id) {
        return id.length() == 7 && id.charAt(0) == '#' && parseCustom(id).isPresent();
    }

    // MiniMessage keeps unknown tags as literal text instead of failing, so any leftover text means a typo.
    private static boolean isValid(String color) {
        try {
            return PlainTextComponentSerializer.plainText()
                    .serialize(MiniMessage.miniMessage().deserialize(color + "x")).equals("x");
        } catch (ParsingException e) {
            return false;
        }
    }
}

package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
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
 * Chat color cosmetics defined under chat-colors in cosmetics.yml, parsed once per {@link #load()}, plus the
 * custom hex colors players pick with /chatcolor.
 */
final class ChatColors {

    private static final String SECTION = "chat-colors.";
    private static final String COSMETICS = SECTION + "cosmetics";
    private static final Pattern HEX = Pattern.compile("(?:&?#)?([0-9a-fA-F]{6})");

    private final ConfigManager configs;
    private final Logger logger;
    private volatile Map<String, ChatColor> definitions = Map.of();

    ChatColors(ConfigManager configs, Logger logger) {
        this.configs = configs;
        this.logger = logger;
    }

    void load() {
        var cosmetics = configs.cosmetics().get().getConfigurationSection(COSMETICS);
        Map<String, ChatColor> loaded = new LinkedHashMap<>();
        int invalid = 0;
        if (cosmetics != null) {
            for (String id : cosmetics.getKeys(false)) {
                var cosmetic = cosmetics.getConfigurationSection(id);
                // Custom picks are stored as #rrggbb in the same selection slot, so ids must never look like one.
                if (cosmetic == null || isCustom(id)) {
                    logger.warning("Invalid chat color " + COSMETICS + "." + id + ".");
                    invalid++;
                    continue;
                }
                String color = cosmetic.getString("color", "");
                if (!isValid(color)) {
                    logger.warning("Invalid MiniMessage color '" + color + "' in " + COSMETICS + "." + id + ".");
                    invalid++;
                    continue;
                }
                String name = cosmetic.isSet("name") ? cosmetic.getString("name") : id;
                loaded.put(id, new ChatColor(id, name, cosmetic.getBoolean("premium"), color));
            }
        }
        definitions = Map.copyOf(loaded);
        logger.info("Loaded " + loaded.size() + " chat color cosmetics" + (invalid > 0 ? " (" + invalid + " invalid)." : "."));
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
        var cosmetics = configs.cosmetics().get().getConfigurationSection(COSMETICS);
        return cosmetics == null ? List.of()
                : cosmetics.getKeys(false).stream().filter(definitions::containsKey).toList();
    }

    String defaultSelection() {
        return configs.cosmetics().get().getString(SECTION + "default-selection", "");
    }

    Optional<ChatColor> find(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    Optional<ChatColor> usable(Player player, String id) {
        return find(id).filter(color -> player.hasPermission(Keys.Permission.COSMETICS_CHAT_COLOR + id));
    }

    List<ChatColor> usable(Player player) {
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

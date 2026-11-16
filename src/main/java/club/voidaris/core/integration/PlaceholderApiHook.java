package club.voidaris.core.integration;

import club.voidaris.core.message.LegacyColors;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;

// Kept apart from Integrations so PlaceholderAPI classes are only loaded when the plugin is actually present.
final class PlaceholderApiHook {

    String apply(Player player, String text) {
        // Each placeholder is resolved on its own so only its value has & codes converted, not the surrounding format.
        return PlaceholderAPI.getPlaceholderPattern().matcher(text).replaceAll(match -> Matcher.quoteReplacement(
                LegacyColors.toMiniMessage(PlaceholderAPI.setPlaceholders(player, match.group()))));
    }
}

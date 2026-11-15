package club.voidaris.core.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

// Kept apart from Integrations so PlaceholderAPI classes are only loaded when the plugin is actually present.
final class PlaceholderApiHook {

    String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}

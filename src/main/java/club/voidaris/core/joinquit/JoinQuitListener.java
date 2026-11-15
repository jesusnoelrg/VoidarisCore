package club.voidaris.core.joinquit;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.PlayerFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class JoinQuitListener implements Listener {

    private static final String SECTION = "join-quit.";
    static final String FORMATS = SECTION + "groups";

    private final ConfigManager configs;
    private final PlayerFormatter formatter;

    JoinQuitListener(ConfigManager configs, PlayerFormatter formatter) {
        this.configs = configs;
        this.formatter = formatter;
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String raw = player.hasPlayedBefore()
                ? format(player, "join")
                : configs.chatFormat().get().getString(SECTION + "first-join");
        event.joinMessage(render(player, raw));
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.quitMessage(render(player, format(player, "quit")));
    }

    private String format(Player player, String type) {
        var config = configs.chatFormat().get();
        // Read from the file on disk only (no bundled defaults), so a format deleted by the admin stops applying.
        var formats = config.getConfigurationSection(FORMATS);
        if (formats != null) {
            String best = null;
            int bestPriority = Integer.MAX_VALUE;
            for (String name : formats.getKeys(false)) {
                if (!formats.isSet(name + "." + type) || !player.hasPermission(Keys.Permission.JOIN_QUIT_FORMAT + name)) {
                    continue;
                }
                int priority = formats.getInt(name + ".priority", Integer.MAX_VALUE);
                // Strict comparison keeps the first listed format on ties.
                if (best == null || priority < bestPriority) {
                    best = formats.getString(name + "." + type);
                    bestPriority = priority;
                }
            }
            if (best != null) {
                return best;
            }
        }
        return config.getString(SECTION + type);
    }

    private Component render(Player player, String raw) {
        // A blank format hides the message entirely instead of falling back to vanilla.
        return raw == null || raw.isBlank() ? null : formatter.format(player, raw);
    }
}

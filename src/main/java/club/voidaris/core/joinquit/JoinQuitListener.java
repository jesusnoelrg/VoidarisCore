package club.voidaris.core.joinquit;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.integration.Integrations;
import club.voidaris.core.message.PlayerFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class JoinQuitListener implements Listener {

    private static final String SECTION = "join-quit.";
    static final String FORMATS = SECTION + "groups";

    private final ConfigManager configs;
    private final PlayerFormatter formatter;
    private final Integrations integrations;

    JoinQuitListener(ConfigManager configs, PlayerFormatter formatter, Integrations integrations) {
        this.configs = configs;
        this.formatter = formatter;
        this.integrations = integrations;
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String raw = player.hasPlayedBefore()
                ? joinFormat(player)
                : configs.chatFormat().get().getString(SECTION + "first-join");
        event.joinMessage(render(player, raw));
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
    }

    private String joinFormat(Player player) {
        var config = configs.chatFormat().get();
        // Read from the file on disk only (no bundled defaults), so a format deleted by the admin stops applying.
        var formats = config.getConfigurationSection(FORMATS);
        if (formats != null) {
            String best = null;
            int bestPriority = Integer.MAX_VALUE;
            for (String name : formats.getKeys(false)) {
                if (!formats.isSet(name + ".join") || !player.hasPermission(Keys.Permission.JOIN_QUIT_FORMAT + name)) {
                    continue;
                }
                int priority = formats.getInt(name + ".priority", Integer.MAX_VALUE);
                // Strict comparison keeps the first listed format on ties.
                if (best == null || priority < bestPriority) {
                    best = formats.getString(name + ".join");
                    bestPriority = priority;
                }
            }
            if (best != null) {
                return best;
            }
        }
        return config.getString(SECTION + "join");
    }

    private Component render(Player player, String raw) {
        // A blank format hides the message entirely instead of falling back to vanilla.
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Component message = formatter.format(player, raw);
        var lines = configs.chatFormat().get().getStringList(SECTION + "hover.lines");
        if (lines.isEmpty()) {
            return message;
        }
        Component hover = formatter.format(player, String.join("<newline>", lines),
                Placeholder.unparsed("group", integrations.primaryGroup(player).orElse("")),
                Placeholder.unparsed("first_join", firstJoin(player)));
        return message.hoverEvent(HoverEvent.showText(hover));
    }

    private String firstJoin(Player player) {
        String pattern = configs.chatFormat().get().getString(SECTION + "hover.date-format", "dd/MM/yyyy");
        return DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(player.getFirstPlayed()));
    }
}

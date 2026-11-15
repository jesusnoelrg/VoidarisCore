package club.voidaris.core.joinquit;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.cosmetics.CosmeticsModule;
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

    private final ConfigManager configs;
    private final PlayerFormatter formatter;
    private final Integrations integrations;
    private final CosmeticsModule cosmetics;

    JoinQuitListener(ConfigManager configs, PlayerFormatter formatter, Integrations integrations, CosmeticsModule cosmetics) {
        this.configs = configs;
        this.formatter = formatter;
        this.integrations = integrations;
        this.cosmetics = cosmetics;
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
        return cosmetics.joinMessage(player).orElseGet(() -> configs.chatFormat().get().getString(SECTION + "join"));
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

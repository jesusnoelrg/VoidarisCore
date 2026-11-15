package club.voidaris.core.joinquit;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.integration.Integrations;
import club.voidaris.core.message.PlayerFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

final class JoinQuitListener implements Listener {

    private static final String SECTION = "join-quit.";

    private final ConfigManager configs;
    private final Integrations integrations;
    private final PlayerFormatter formatter;

    JoinQuitListener(ConfigManager configs, Integrations integrations, PlayerFormatter formatter) {
        this.configs = configs;
        this.integrations = integrations;
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
        var groupPath = integrations.primaryGroup(player).map(group -> SECTION + "groups." + group + "." + type);
        // isSet() ignores the bundled defaults, so a group deleted from the file on disk stops overriding.
        if (groupPath.isPresent() && config.isSet(groupPath.get())) {
            return config.getString(groupPath.get());
        }
        return config.getString(SECTION + type);
    }

    private Component render(Player player, String raw) {
        // A blank format hides the message entirely instead of falling back to vanilla.
        return raw == null || raw.isBlank() ? null : formatter.format(player, raw);
    }
}

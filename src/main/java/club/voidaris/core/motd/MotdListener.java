package club.voidaris.core.motd;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

final class MotdListener implements Listener {

    private final MotdModule module;

    MotdListener(MotdModule module) {
        this.module = module;
    }

    // The join message is broadcast after every handler returns, so sending at MONITOR places the MOTD
    // after other join-time messages and right before the player's own join message.
    @EventHandler(priority = EventPriority.MONITOR)
    void onJoin(PlayerJoinEvent event) {
        if (module.showOnJoin()) {
            module.send(event.getPlayer());
        }
    }
}

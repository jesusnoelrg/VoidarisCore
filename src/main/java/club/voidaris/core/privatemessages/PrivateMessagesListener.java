package club.voidaris.core.privatemessages;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

final class PrivateMessagesListener implements Listener {

    private final PrivateMessagesModule module;

    PrivateMessagesListener(PrivateMessagesModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onChat(AsyncChatEvent event) {
        var sender = event.getPlayer().getUniqueId();
        event.viewers().removeIf(viewer -> viewer instanceof Player player
                && module.store().level(player.getUniqueId(), sender) == IgnoreLevel.ALL);
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        module.forgetReply(event.getPlayer().getUniqueId());
    }
}

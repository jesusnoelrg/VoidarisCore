package club.voidaris.core.kits;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

final class KitsListener implements Listener {

    private final KitsModule module;
    private final Messages messages;

    KitsListener(KitsModule module, Messages messages) {
        this.module = module;
        this.messages = messages;
    }

    // The kit counts as claimed, so a one-time starter kit cannot be claimed a second time from the menu.
    @EventHandler
    void onFirstJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }
        module.firstJoinKit().ifPresent(kit -> {
            module.give(player, kit, true);
            messages.send(player, Keys.Message.KITS_RECEIVED, kit.placeholders());
            module.storage().claim(player.getUniqueId(), kit, System.currentTimeMillis());
        });
    }
}

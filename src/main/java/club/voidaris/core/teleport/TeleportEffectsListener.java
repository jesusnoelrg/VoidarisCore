package club.voidaris.core.teleport;

import net.kyori.adventure.sound.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

final class TeleportEffectsListener implements Listener {

    private final JavaPlugin plugin;
    private final TeleportEffectsModule module;

    TeleportEffectsListener(JavaPlugin plugin, TeleportEffectsModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        var settings = module.settings();
        if (!settings.causes().contains(event.getCause())) {
            return;
        }
        var player = event.getPlayer();
        // One tick later so the effects play where the player arrived, after the client has moved there.
        player.getScheduler().runDelayed(plugin, task -> {
            if (settings.sound() != null) {
                player.playSound(settings.sound(), Sound.Emitter.self());
            }
            module.cosmetics().teleportParticle(player)
                    .ifPresent(particle -> particle.play(plugin, player, settings.visibleToOthers()));
        }, null, 1L);
    }
}

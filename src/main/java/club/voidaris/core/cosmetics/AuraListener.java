package club.voidaris.core.cosmetics;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws the equipped aura behind moving players, at most once every {@link Aura#interval()} ticks.
 */
final class AuraListener implements Listener {

    private final CosmeticsModule module;
    private final Map<UUID, Integer> lastFrame = new ConcurrentHashMap<>();

    AuraListener(CosmeticsModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) {
            return;
        }
        var player = event.getPlayer();
        // An aura would give away a player who is meant to be hidden.
        if (player.getGameMode() == GameMode.SPECTATOR || player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return;
        }
        var aura = module.aura(player);
        if (aura.isEmpty()) {
            return;
        }
        // Ticks lived instead of the server tick: it is per entity, so it is safe on any region thread.
        int now = player.getTicksLived();
        Integer last = lastFrame.get(player.getUniqueId());
        if (last != null && now - last < aura.get().interval() && now >= last) {
            return;
        }
        lastFrame.put(player.getUniqueId(), now);
        aura.get().frame(player, now);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onQuit(PlayerQuitEvent event) {
        lastFrame.remove(event.getPlayer().getUniqueId());
    }
}

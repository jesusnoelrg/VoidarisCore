package club.voidaris.core.cosmetics;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

/**
 * Plays the equipped join effect and keeps the entities it spawns from hurting anyone or being picked up.
 */
final class JoinEffectListener implements Listener {

    private final Plugin plugin;
    private final CosmeticsModule module;

    JoinEffectListener(Plugin plugin, CosmeticsModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        // Delayed so the joining client has loaded the world and vanish plugins have already hidden the player.
        player.getScheduler().runDelayed(plugin, task -> {
            // An effect would give away a player who is meant to be hidden.
            if (player.getGameMode() == GameMode.SPECTATOR || player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                return;
            }
            module.joinEffect(player).ifPresent(effect -> effect.play(module.joinEffectStage(), player));
        }, null, module.joinEffects().delay());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onLightning(LightningStrikeEvent event) {
        JoinEffectStep.Lightning.claim(event.getLightning());
    }

    // Firework explosions damage nearby entities.
    @EventHandler(ignoreCancelled = true)
    void onDamage(EntityDamageByEntityEvent event) {
        if (isEffect(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onHopperPickup(InventoryPickupItemEvent event) {
        if (isEffect(event.getItem())) {
            event.setCancelled(true);
        }
    }

    // A merged item would outlive its removal task or absorb a real drop.
    @EventHandler(ignoreCancelled = true)
    void onMerge(ItemMergeEvent event) {
        if (isEffect(event.getEntity()) || isEffect(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    private boolean isEffect(Entity entity) {
        return entity.getPersistentDataContainer().has(module.joinEffectStage().marker(), PersistentDataType.BOOLEAN);
    }
}

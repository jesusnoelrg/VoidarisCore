package club.voidaris.core.spawn;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.Messages;
import club.voidaris.core.storage.StoredLocation;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class SpawnListener implements Listener {

    private final SpawnModule module;
    private final ConfigManager configs;
    private final Messages messages;
    // Players already being sent back, so further move events during the async teleport do not queue more.
    private final Set<UUID> returning = ConcurrentHashMap.newKeySet();

    SpawnListener(SpawnModule module, ConfigManager configs, Messages messages) {
        this.module = module;
        this.configs = configs;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFirstSpawn(AsyncPlayerSpawnLocationEvent event) {
        if (event.isNewPlayer() && settings().getBoolean("teleport-on-first-join")) {
            spawnLocation().ifPresent(event::setSpawnLocation);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        var settings = settings();
        if (!settings.getBoolean("teleport-on-respawn")
                || event.getRespawnReason() == PlayerRespawnEvent.RespawnReason.PLUGIN) {
            return;
        }
        if (settings.getBoolean("respect-bed-spawn") && (event.isBedSpawn() || event.isAnchorSpawn())) {
            return;
        }
        spawnLocation().ifPresent(event::setRespawnLocation);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) {
            return;
        }
        var voidReturn = settings().getConfigurationSection("void-return");
        if (voidReturn == null || !voidReturn.getBoolean("enabled") || event.getTo().getY() >= voidReturn.getInt("min-y")) {
            return;
        }
        var worlds = voidReturn.getStringList("worlds");
        if (!worlds.isEmpty() && !worlds.contains(event.getTo().getWorld().getName())) {
            return;
        }
        var target = spawnLocation();
        var player = event.getPlayer();
        if (target.isEmpty() || !returning.add(player.getUniqueId())) {
            return;
        }
        // Without this the fall accumulated in the void would kill the player on arrival.
        player.setFallDistance(0);
        player.teleportAsync(target.get()).whenComplete((success, error) -> {
            returning.remove(player.getUniqueId());
            if (Boolean.TRUE.equals(success)) {
                player.setFallDistance(0);
                messages.send(player, Keys.Message.SPAWN_VOID_RETURN);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        returning.remove(event.getPlayer().getUniqueId());
    }

    private ConfigurationSection settings() {
        return configs.teleports().get().getConfigurationSection("spawn");
    }

    private Optional<Location> spawnLocation() {
        return module.spawn().flatMap(StoredLocation::toLocation);
    }
}

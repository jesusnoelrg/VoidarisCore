package club.voidaris.core.teleport;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import club.voidaris.core.storage.CooldownFile;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared teleport flow for spawn, warps and future teleport modules: warmup, cancellation and cooldown.
 */
public final class Teleporter implements Listener {

    /**
     * @param cooldownBypass permission that skips only the cooldown (the warmup still applies), or {@code null}
     */
    public record Settings(int warmupSeconds, int cooldownSeconds, boolean cancelOnMove, boolean cancelOnDamage,
                           String cooldownBypass) {

        /**
         * @param timing   section holding {@code warmup-seconds} and {@code cooldown-seconds}
         * @param defaults the {@code defaults} section of teleports.yml
         */
        public static Settings of(ConfigurationSection timing, ConfigurationSection defaults) {
            return new Settings(timing.getInt("warmup-seconds"), timing.getInt("cooldown-seconds"),
                    defaults.getBoolean("cancel-on-move"), defaults.getBoolean("cancel-on-damage"), null);
        }

        public Settings withCooldownBypass(String permission) {
            return new Settings(warmupSeconds, cooldownSeconds, cancelOnMove, cancelOnDamage, permission);
        }
    }

    private record Pending(BukkitTask task, Settings settings) {
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final CooldownFile cooldowns;

    public Teleporter(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.cooldowns = new CooldownFile(plugin, "teleport-cooldowns.yml");
    }

    public CooldownFile cooldowns() {
        return cooldowns;
    }

    /**
     * @param type    cooldown bucket, so a spawn cooldown does not block warps
     * @param arrived message key sent once the player arrives
     * @return false when the player is still on cooldown
     */
    public boolean teleport(Player player, String type, Location target, Settings settings, String arrived,
                            TagResolver... resolvers) {
        boolean bypass = player.hasPermission(Keys.Permission.TELEPORT_BYPASS);
        boolean skipCooldown = bypass
                || (settings.cooldownBypass() != null && player.hasPermission(settings.cooldownBypass()));
        if (!skipCooldown) {
            long remaining = cooldowns.remaining(player.getUniqueId(), type);
            if (remaining > 0) {
                messages.send(player, Keys.Message.TELEPORT_COOLDOWN,
                        Placeholder.unparsed("seconds", String.valueOf((remaining + 999) / 1000)));
                return false;
            }
        }
        cancel(player.getUniqueId());

        Runnable go = () -> {
            pending.remove(player.getUniqueId());
            player.teleportAsync(target).thenAccept(success -> {
                if (!success) {
                    messages.send(player, Keys.Message.TELEPORT_FAILED);
                    return;
                }
                if (!skipCooldown && settings.cooldownSeconds() > 0) {
                    cooldowns.start(player.getUniqueId(), type, settings.cooldownSeconds() * 1000L);
                }
                messages.send(player, arrived, resolvers);
            });
        };
        if (bypass || settings.warmupSeconds() <= 0) {
            go.run();
            return true;
        }
        messages.send(player, Keys.Message.TELEPORT_WARMUP,
                Placeholder.unparsed("seconds", String.valueOf(settings.warmupSeconds())));
        var task = plugin.getServer().getScheduler().runTaskLater(plugin, go, settings.warmupSeconds() * 20L);
        pending.put(player.getUniqueId(), new Pending(task, settings));
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Block granularity so looking around does not cancel the warmup.
        if (!event.hasChangedBlock()) {
            return;
        }
        var current = pending.get(event.getPlayer().getUniqueId());
        if (current != null && current.settings().cancelOnMove()) {
            cancel(event.getPlayer().getUniqueId());
            messages.send(event.getPlayer(), Keys.Message.TELEPORT_CANCELLED_MOVE);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        var current = pending.get(player.getUniqueId());
        if (current != null && current.settings().cancelOnDamage()) {
            cancel(player.getUniqueId());
            messages.send(player, Keys.Message.TELEPORT_CANCELLED_DAMAGE);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }

    private void cancel(UUID player) {
        var current = pending.remove(player);
        if (current != null) {
            current.task().cancel();
        }
    }
}

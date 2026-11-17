package club.voidaris.core.announcements;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.Messages;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.Module;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class AnnouncementsModule extends Module {

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final Messages messages;
    private final PlayerFormatter formatter;
    private final AnnouncementsStore store;

    private BukkitTask task;
    private int nextIndex;

    public AnnouncementsModule(JavaPlugin plugin, ConfigManager configs, Messages messages, PlayerFormatter formatter) {
        super("announcements");
        this.plugin = plugin;
        this.configs = configs;
        this.messages = messages;
        this.formatter = formatter;
        this.store = new AnnouncementsStore(plugin);
    }

    @Override
    protected void onEnable() {
        store.load();
        schedule();
    }

    @Override
    protected void onDisable() {
        store.saveNow();
        cancel();
    }

    @Override
    protected void onReload() {
        schedule();
    }

    private void schedule() {
        cancel();
        var config = configs.announces().get();
        if (!config.getBoolean("enabled", true)) {
            return;
        }
        long ticks = Math.max(1, config.getLong("interval-seconds", 300)) * 20L;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::broadcastNext, ticks, ticks);
    }

    private void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void broadcastNext() {
        var config = configs.announces().get();
        List<List<String>> announcements = readAnnouncements(config.getMapList("announcements"));
        if (announcements.isEmpty() || Bukkit.getOnlinePlayers().size() < config.getInt("min-players", 1)) {
            return;
        }

        int index;
        if ("random".equalsIgnoreCase(config.getString("order", "sequential"))) {
            index = ThreadLocalRandom.current().nextInt(announcements.size());
        } else {
            // The list may shrink on reload, so the stored cursor is wrapped on every use.
            index = nextIndex % announcements.size();
            nextIndex = index + 1;
        }

        String text = String.join("<newline>", announcements.get(index));
        Sound sound = readSound(config.getConfigurationSection("sound"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (store.disabled(player.getUniqueId())) {
                continue;
            }
            player.sendMessage(formatter.format(player, text));
            if (sound != null) {
                player.playSound(sound);
            }
        }
    }

    AnnouncementsStore store() {
        return store;
    }

    Messages messages() {
        return messages;
    }

    // Checked here instead of requires() so the sender gets the Spanish message rather than "unknown command".
    Player requirePlayer(CommandSender sender, String permission) {
        if (!isEnabled()) {
            messages.send(sender, Keys.Message.GENERAL_MODULE_DISABLED);
            return null;
        }
        if (!sender.hasPermission(permission)) {
            messages.send(sender, Keys.Message.GENERAL_NO_PERMISSION);
            return null;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, Keys.Message.GENERAL_PLAYER_ONLY);
            return null;
        }
        return player;
    }

    private static List<List<String>> readAnnouncements(List<Map<?, ?>> entries) {
        List<List<String>> result = new ArrayList<>();
        for (Map<?, ?> entry : entries) {
            if (!(entry.get("lines") instanceof List<?> raw)) {
                continue;
            }
            List<String> lines = raw.stream().map(String::valueOf).toList();
            if (!lines.isEmpty()) {
                result.add(lines);
            }
        }
        return result;
    }

    private Sound readSound(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enabled", true)) {
            return null;
        }
        try {
            return Sound.sound(Key.key(section.getString("key", "minecraft:block.note_block.pling")),
                    Sound.Source.MASTER, (float) section.getDouble("volume", 1.0), (float) section.getDouble("pitch", 1.0));
        } catch (InvalidKeyException e) {
            plugin.getLogger().warning("announces.yml: invalid sound key '" + section.getString("key") + "'");
            return null;
        }
    }
}

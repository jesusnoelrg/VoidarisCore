package club.voidaris.core.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;

/**
 * Every YAML file of the plugin. A file whose structure changes in a way that cannot be fixed by adding the new
 * keys bumps its {@code config-version} and registers a {@link ConfigMigration} here.
 */
public final class ConfigManager {

    private final ConfigFile main;
    private final ConfigFile messages;
    private final ConfigFile teleports;
    private final ConfigFile homes;
    private final ConfigFile warps;
    private final ConfigFile kits;
    private final ConfigFile chatFormat;
    private final ConfigFile announces;
    private final ConfigFile motd;
    private final ConfigFile privateMessages;
    private final ConfigFile database;
    private final ConfigFile cosmetics;
    private final ConfigFile joinMessagesGui;
    private final ConfigFile teleportParticlesGui;

    public ConfigManager(JavaPlugin plugin) {
        main = new ConfigFile(plugin, "config.yml");
        messages = new ConfigFile(plugin, "messages.yml");
        teleports = new ConfigFile(plugin, "teleports.yml");
        homes = new ConfigFile(plugin, "homes.yml", Set.of("limits"), List.of());
        warps = new ConfigFile(plugin, "warps.yml");
        kits = new ConfigFile(plugin, "kits.yml", Set.of("kits"), List.of());
        chatFormat = new ConfigFile(plugin, "chat-format.yml", Set.of("chat.groups"), List.of());
        announces = new ConfigFile(plugin, "announces.yml");
        motd = new ConfigFile(plugin, "motd.yml");
        privateMessages = new ConfigFile(plugin, "private-messages.yml");
        database = new ConfigFile(plugin, "database.yml");
        cosmetics = new ConfigFile(plugin, "cosmetics.yml",
                Set.of("join-messages.cosmetics", "teleport-particles.cosmetics"), List.of());
        joinMessagesGui = new ConfigFile(plugin, "guis/join-messages.yml",
                Set.of("items", "join_messages.icons"), List.of());
        teleportParticlesGui = new ConfigFile(plugin, "guis/teleport-particles.yml",
                Set.of("items", "teleport_particles.icons"), List.of());
    }

    public void reload() {
        for (ConfigFile file : new ConfigFile[]{main, messages, teleports, homes, warps, kits, chatFormat, announces, motd, privateMessages, database, cosmetics, joinMessagesGui, teleportParticlesGui}) {
            file.reload();
        }
    }

    public ConfigFile main() { return main; }
    public ConfigFile messages() { return messages; }
    public ConfigFile teleports() { return teleports; }
    public ConfigFile homes() { return homes; }
    public ConfigFile warps() { return warps; }
    public ConfigFile kits() { return kits; }
    public ConfigFile chatFormat() { return chatFormat; }
    public ConfigFile announces() { return announces; }
    public ConfigFile motd() { return motd; }
    public ConfigFile privateMessages() { return privateMessages; }
    public ConfigFile database() { return database; }
    public ConfigFile cosmetics() { return cosmetics; }
    public ConfigFile joinMessagesGui() { return joinMessagesGui; }
    public ConfigFile teleportParticlesGui() { return teleportParticlesGui; }
}

package club.voidaris.core.config;

import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {

    private final ConfigFile main;
    private final ConfigFile messages;
    private final ConfigFile teleports;
    private final ConfigFile homes;
    private final ConfigFile warps;
    private final ConfigFile kits;
    private final ConfigFile chatFormat;
    private final ConfigFile announces;

    public ConfigManager(JavaPlugin plugin) {
        main = new ConfigFile(plugin, "config.yml");
        messages = new ConfigFile(plugin, "messages.yml");
        teleports = new ConfigFile(plugin, "teleports.yml");
        homes = new ConfigFile(plugin, "homes.yml");
        warps = new ConfigFile(plugin, "warps.yml");
        kits = new ConfigFile(plugin, "kits.yml");
        chatFormat = new ConfigFile(plugin, "chat-format.yml");
        announces = new ConfigFile(plugin, "announces.yml");
    }

    public void reload() {
        for (ConfigFile file : new ConfigFile[]{main, messages, teleports, homes, warps, kits, chatFormat, announces}) {
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
}

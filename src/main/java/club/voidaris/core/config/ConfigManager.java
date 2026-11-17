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
    private final ConfigFile killMessagesGui;
    private final ConfigFile aurasGui;
    private final ConfigFile joinEffectsGui;
    private final ConfigFile chatColorsGui;
    private final ConfigFile nickColorsGui;
    private final ConfigFile badgesGui;
    private final ConfigFile badgeListGui;
    private final ConfigFile homesGui;
    private final ConfigFile kitsGui;
    private final ConfigFile kitPreviewGui;

    public ConfigManager(JavaPlugin plugin) {
        main = new ConfigFile(plugin, "config.yml");
        // Version 2 drops homes.list and kits.list, left behind when their modules replaced the chat lists with menus.
        messages = new ConfigFile(plugin, "messages.yml", Set.of(), List.of(disk -> {
            disk.set("homes.list", null);
            disk.set("kits.list", null);
        }));
        teleports = new ConfigFile(plugin, "teleports.yml");
        homes = new ConfigFile(plugin, "homes.yml", Set.of("limits"), List.of());
        warps = new ConfigFile(plugin, "warps.yml");
        // Version 2 moved kits to data/kits/, created in-game with /createkit; the old item lists cannot be converted.
        kits = new ConfigFile(plugin, "kits.yml", Set.of(), List.of(disk -> disk.set("kits", null)));
        chatFormat = new ConfigFile(plugin, "chat-format.yml", Set.of("chat.groups"), List.of());
        announces = new ConfigFile(plugin, "announces.yml", Set.of("announcements"), List.of());
        motd = new ConfigFile(plugin, "motd.yml");
        privateMessages = new ConfigFile(plugin, "private-messages.yml");
        database = new ConfigFile(plugin, "database.yml");
        cosmetics = new ConfigFile(plugin, "cosmetics.yml",
                Set.of("join-messages.cosmetics", "teleport-particles.cosmetics", "kill-messages.cosmetics",
                        "auras.cosmetics", "join-effects.cosmetics", "chat-colors.cosmetics", "nick-colors.cosmetics", "badges.cosmetics"), List.of());
        joinMessagesGui = new ConfigFile(plugin, "guis/join-messages.yml",
                Set.of("items", "join_messages.icons"), List.of());
        teleportParticlesGui = new ConfigFile(plugin, "guis/teleport-particles.yml",
                Set.of("items", "teleport_particles.icons"), List.of());
        killMessagesGui = new ConfigFile(plugin, "guis/kill-messages.yml",
                Set.of("items", "kill_messages.icons"), List.of());
        aurasGui = new ConfigFile(plugin, "guis/auras.yml", Set.of("items", "auras.icons"), List.of());
        joinEffectsGui = new ConfigFile(plugin, "guis/join-effects.yml", Set.of("items", "join_effects.icons"), List.of());
        chatColorsGui = new ConfigFile(plugin, "guis/chat-colors.yml", Set.of("items", "chat_colors.icons"), List.of());
        nickColorsGui = new ConfigFile(plugin, "guis/nick-colors.yml", Set.of("items", "nick_colors.icons"), List.of());
        badgesGui = new ConfigFile(plugin, "guis/badges.yml", Set.of("items", "sections"), List.of());
        badgeListGui = new ConfigFile(plugin, "guis/badge-list.yml", Set.of("items"), List.of());
        homesGui = new ConfigFile(plugin, "guis/homes.yml", Set.of("items"), List.of());
        kitsGui = new ConfigFile(plugin, "guis/kits.yml", Set.of("items"), List.of());
        kitPreviewGui = new ConfigFile(plugin, "guis/kit-preview.yml", Set.of("items"), List.of());
    }

    public void reload() {
        for (ConfigFile file : new ConfigFile[]{main, messages, teleports, homes, warps, kits, chatFormat, announces, motd, privateMessages, database, cosmetics, joinMessagesGui, teleportParticlesGui, killMessagesGui, aurasGui, joinEffectsGui, chatColorsGui, nickColorsGui, badgesGui, badgeListGui, homesGui, kitsGui, kitPreviewGui}) {
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
    public ConfigFile killMessagesGui() { return killMessagesGui; }
    public ConfigFile aurasGui() { return aurasGui; }
    public ConfigFile joinEffectsGui() { return joinEffectsGui; }
    public ConfigFile chatColorsGui() { return chatColorsGui; }
    public ConfigFile nickColorsGui() { return nickColorsGui; }
    public ConfigFile badgesGui() { return badgesGui; }
    public ConfigFile badgeListGui() { return badgeListGui; }
    public ConfigFile homesGui() { return homesGui; }
    public ConfigFile kitsGui() { return kitsGui; }
    public ConfigFile kitPreviewGui() { return kitPreviewGui; }
}

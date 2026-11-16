package club.voidaris.core.chat;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.integration.Integrations;
import club.voidaris.core.message.LegacyColors;
import club.voidaris.core.message.PlayerFormatter;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

final class ChatListener implements Listener {

    private static final String SECTION = "chat.";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ConfigManager configs;
    private final PlayerFormatter formatter;
    private final Integrations integrations;

    ChatListener(ConfigManager configs, PlayerFormatter formatter, Integrations integrations) {
        this.configs = configs;
        this.formatter = formatter;
        this.integrations = integrations;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = configs.chatFormat().get();
        String group = integrations.primaryGroup(player).orElse("");
        // Rendered once: the line is the same for every viewer, so the renderer just hands it out.
        Component line = formatter.format(player, name(player, config, group), format(config, group),
                Placeholder.component("message", message(player, config, event.message())));
        event.renderer(ChatRenderer.viewerUnaware((source, displayName, message) -> line));
    }

    private String format(FileConfiguration config, String group) {
        String format = group.isEmpty() ? null : config.getString(SECTION + "groups." + group);
        if (format == null) {
            format = config.getString(SECTION + "default-format");
        }
        return format != null ? format : "<player> <dark_gray>»</dark_gray> <white><message>";
    }

    private Component name(Player player, FileConfiguration config, String group) {
        Component name = Component.text(player.getName());
        var lines = config.getStringList(SECTION + "hover.lines");
        if (!lines.isEmpty()) {
            name = name.hoverEvent(HoverEvent.showText(formatter.format(player, String.join("<newline>", lines),
                    Placeholder.unparsed("group", group))));
        }
        String command = config.getString(SECTION + "click.suggest-command");
        if (command != null && !command.isBlank()) {
            name = name.clickEvent(ClickEvent.suggestCommand(command.replace("<player>", player.getName())));
        }
        return name;
    }

    private Component message(Player player, FileConfiguration config, Component original) {
        String text = PlainTextComponentSerializer.plainText().serialize(original);
        boolean codes = config.getBoolean(SECTION + "colors.legacy") && player.hasPermission(Keys.Permission.CHAT_COLOR);
        boolean hex = config.getBoolean(SECTION + "colors.hex") && player.hasPermission(Keys.Permission.CHAT_HEX);
        if (!codes && !hex) {
            return Component.text(text);
        }
        // Tags typed by the player are escaped first so only the & codes they are allowed to use become formatting.
        return MINI_MESSAGE.deserialize(LegacyColors.toMiniMessage(MINI_MESSAGE.escapeTags(text), codes, hex));
    }
}

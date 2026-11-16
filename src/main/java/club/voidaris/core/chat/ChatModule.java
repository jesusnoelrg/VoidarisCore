package club.voidaris.core.chat;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.integration.Integrations;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.Module;
import org.bukkit.event.Listener;

import java.util.List;

public final class ChatModule extends Module {

    private final ConfigManager configs;
    private final PlayerFormatter formatter;
    private final Integrations integrations;

    public ChatModule(ConfigManager configs, PlayerFormatter formatter, Integrations integrations) {
        super("chat");
        this.configs = configs;
        this.formatter = formatter;
        this.integrations = integrations;
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new ChatListener(configs, formatter, integrations));
    }
}

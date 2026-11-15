package club.voidaris.core.joinquit;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.integration.Integrations;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.Module;
import org.bukkit.event.Listener;

import java.util.List;

public final class JoinQuitModule extends Module {

    private final ConfigManager configs;
    private final Integrations integrations;
    private final PlayerFormatter formatter;

    public JoinQuitModule(ConfigManager configs, Integrations integrations, PlayerFormatter formatter) {
        super("join-quit");
        this.configs = configs;
        this.integrations = integrations;
        this.formatter = formatter;
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new JoinQuitListener(configs, integrations, formatter));
    }
}

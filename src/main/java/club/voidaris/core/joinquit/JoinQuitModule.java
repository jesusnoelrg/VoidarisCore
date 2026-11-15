package club.voidaris.core.joinquit;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.cosmetics.CosmeticsModule;
import club.voidaris.core.integration.Integrations;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.Module;
import org.bukkit.event.Listener;

import java.util.List;

public final class JoinQuitModule extends Module {

    private final ConfigManager configs;
    private final PlayerFormatter formatter;
    private final Integrations integrations;
    private final CosmeticsModule cosmetics;

    public JoinQuitModule(ConfigManager configs, PlayerFormatter formatter, Integrations integrations, CosmeticsModule cosmetics) {
        super("join-quit");
        this.configs = configs;
        this.formatter = formatter;
        this.integrations = integrations;
        this.cosmetics = cosmetics;
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new JoinQuitListener(configs, formatter, integrations, cosmetics));
    }
}

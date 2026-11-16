package club.voidaris.core.motd;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.Module;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;

public final class MotdModule extends Module {

    private final ConfigManager configs;
    private final PlayerFormatter formatter;

    public MotdModule(ConfigManager configs, PlayerFormatter formatter) {
        super("motd");
        this.configs = configs;
        this.formatter = formatter;
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new MotdListener(this));
    }

    boolean showOnJoin() {
        return configs.motd().get().getBoolean("show-on-join", true);
    }

    /**
     * @return {@code false} when the MOTD has no lines, so nothing was sent
     */
    public boolean send(Player player) {
        var config = configs.motd().get();
        List<String> lines = config.getStringList("lines");
        if (lines.isEmpty()) {
            return false;
        }
        player.sendMessage(formatter.format(player, String.join("<newline>", lines),
                Placeholder.parsed("separator", config.getString("separator", ""))));
        return true;
    }
}

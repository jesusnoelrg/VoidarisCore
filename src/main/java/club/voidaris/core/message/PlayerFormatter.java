package club.voidaris.core.message;

import club.voidaris.core.integration.Integrations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

public final class PlayerFormatter {

    private final Messages messages;
    private final Integrations integrations;

    public PlayerFormatter(Messages messages, Integrations integrations) {
        this.messages = messages;
        this.integrations = integrations;
    }

    public Component format(Player player, String raw, TagResolver... extra) {
        // PlaceholderAPI runs first so its values can carry MiniMessage tags; the name stays unparsed to block tag injection.
        return messages.parse(integrations.setPlaceholders(player, raw),
                Placeholder.unparsed("player", player.getName()),
                Placeholder.component("displayname", player.displayName()),
                Placeholder.parsed("prefix_lp", integrations.prefix(player)),
                Placeholder.parsed("suffix_lp", integrations.suffix(player)),
                TagResolver.resolver(extra));
    }
}

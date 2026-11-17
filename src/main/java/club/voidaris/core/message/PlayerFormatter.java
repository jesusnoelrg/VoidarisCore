package club.voidaris.core.message;

import club.voidaris.core.integration.Integrations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.function.Function;

public final class PlayerFormatter {

    private final Messages messages;
    private final Integrations integrations;
    private final Function<Player, Component> nickname;

    /**
     * @param nickname what {@code <nickname>} resolves to: the player's name in their nick color cosmetic
     */
    public PlayerFormatter(Messages messages, Integrations integrations, Function<Player, Component> nickname) {
        this.messages = messages;
        this.integrations = integrations;
        this.nickname = nickname;
    }

    public Component format(Player player, String raw, TagResolver... extra) {
        return format(player, Component.text(player.getName()), raw, extra);
    }

    /**
     * @param name what {@code <player>} resolves to, so callers can attach hover or click events to the name
     */
    public Component format(Player player, Component name, String raw, TagResolver... extra) {
        // PlaceholderAPI runs first so its values can carry MiniMessage tags; the name stays unparsed to block tag injection.
        return messages.parse(integrations.setPlaceholders(player, raw),
                Placeholder.component("player", name),
                Placeholder.component("nickname", nickname.apply(player)),
                Placeholder.component("displayname", player.displayName()),
                Placeholder.parsed("prefix_lp", integrations.prefix(player)),
                Placeholder.parsed("suffix_lp", integrations.suffix(player)),
                TagResolver.resolver(extra));
    }
}

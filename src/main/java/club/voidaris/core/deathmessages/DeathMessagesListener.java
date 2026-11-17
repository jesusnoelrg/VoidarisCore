package club.voidaris.core.deathmessages;

import club.voidaris.core.Keys;
import club.voidaris.core.cosmetics.CosmeticsModule;
import club.voidaris.core.cosmetics.KillMessagePlaceholders;
import club.voidaris.core.message.Messages;
import club.voidaris.core.message.PlayerFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;

final class DeathMessagesListener implements Listener {

    private static final String VANILLA_PREFIX = "death.";
    // Vanilla death messages pass the victim, the attacker and the item as positional arguments, in this order.
    private static final String[] ARGUMENTS = {"victim", "killer", "item"};

    private final Messages messages;
    private final PlayerFormatter formatter;
    private final CosmeticsModule cosmetics;

    DeathMessagesListener(Messages messages, PlayerFormatter formatter, CosmeticsModule cosmetics) {
        this.messages = messages;
        this.formatter = formatter;
        this.cosmetics = cosmetics;
    }

    // Only vanilla messages are touched, so a message already replaced by another plugin is left as is.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onDeath(PlayerDeathEvent event) {
        if (!(event.deathMessage() instanceof TranslatableComponent vanilla) || !vanilla.key().startsWith(VANILLA_PREFIX)) {
            return;
        }
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            var killMessage = cosmetics.killMessage(killer);
            if (killMessage.isPresent()) {
                event.deathMessage(formatter.format(killer, killMessage.get(),
                        KillMessagePlaceholders.of(messages, killer, Component.text(victim.getName()))));
                return;
            }
        }
        translate(vanilla).ifPresent(event::deathMessage);
    }

    /**
     * Empty when messages.yml has no translation for the cause, so the vanilla message (rendered in the client's
     * language) is kept.
     */
    private Optional<Component> translate(TranslatableComponent vanilla) {
        // YAML paths split on dots, and vanilla keys such as death.attack.player and death.attack.player.item would collide.
        String key = Keys.Message.DEATH_MESSAGES_CAUSE + vanilla.key().substring(VANILLA_PREFIX.length()).replace('.', '_');
        if (!messages.has(key)) {
            return Optional.empty();
        }
        var arguments = vanilla.arguments();
        TagResolver.Builder resolvers = TagResolver.builder();
        for (int i = 0; i < ARGUMENTS.length; i++) {
            resolvers.resolver(Placeholder.component(ARGUMENTS[i],
                    i < arguments.size() ? arguments.get(i).asComponent() : Component.empty()));
        }
        var message = messages.get(key, resolvers.build());
        return Optional.of(messages.get(Keys.Message.DEATH_MESSAGES_FORMAT, Placeholder.component("message", message)));
    }
}

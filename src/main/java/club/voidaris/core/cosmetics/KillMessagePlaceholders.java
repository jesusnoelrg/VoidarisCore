package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

/**
 * Placeholders of the kill message formats, shared by the death broadcast and the menu preview.
 */
public final class KillMessagePlaceholders {

    private KillMessagePlaceholders() {
    }

    public static TagResolver of(Messages messages, Player killer, Component victim) {
        var weapon = killer.getInventory().getItemInMainHand();
        return TagResolver.resolver(
                Placeholder.component("victim", victim),
                Placeholder.component("killer", Component.text(killer.getName())),
                Placeholder.component("weapon", weapon.getType().isAir()
                        ? messages.get(Keys.Message.COSMETICS_KILL_MESSAGE_NO_WEAPON)
                        : weapon.displayName()));
    }
}

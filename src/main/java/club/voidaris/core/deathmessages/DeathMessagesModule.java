package club.voidaris.core.deathmessages;

import club.voidaris.core.cosmetics.CosmeticsModule;
import club.voidaris.core.message.Messages;
import club.voidaris.core.message.PlayerFormatter;
import club.voidaris.core.module.Module;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Replaces the vanilla death messages with the translations of messages.yml, and the kill messages with the
 * cosmetic the killer has equipped.
 */
public final class DeathMessagesModule extends Module {

    private final Messages messages;
    private final PlayerFormatter formatter;
    private final CosmeticsModule cosmetics;

    public DeathMessagesModule(Messages messages, PlayerFormatter formatter, CosmeticsModule cosmetics) {
        super("death-messages");
        this.messages = messages;
        this.formatter = formatter;
        this.cosmetics = cosmetics;
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new DeathMessagesListener(messages, formatter, cosmetics));
    }
}

package club.voidaris.core.privatemessages;

import club.voidaris.core.Keys;
import club.voidaris.core.command.CommandUsage;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.message.Messages;
import club.voidaris.core.module.Module;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PrivateMessagesModule extends Module {

    private final ConfigManager configs;
    private final Messages messages;
    private final PrivateMessageStore store;
    private final Map<UUID, UUID> replyTargets = new HashMap<>();

    public PrivateMessagesModule(JavaPlugin plugin, ConfigManager configs, Messages messages) {
        super("private-messages");
        this.configs = configs;
        this.messages = messages;
        this.store = new PrivateMessageStore(plugin, "private-messages.yml");
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new PrivateMessagesListener(this));
    }

    @Override
    protected void onEnable() {
        store.load();
    }

    @Override
    protected void onDisable() {
        store.saveNow();
        replyTargets.clear();
    }

    PrivateMessageStore store() {
        return store;
    }

    Messages messages() {
        return messages;
    }

    int ignoreListPageSize() {
        return Math.max(1, configs.privateMessages().get().getInt("ignore-list.page-size", 10));
    }

    /**
     * Both players reply to each other from now on, so {@code /reply} works for whoever spoke last.
     */
    void recordConversation(Player sender, Player target) {
        replyTargets.put(sender.getUniqueId(), target.getUniqueId());
        replyTargets.put(target.getUniqueId(), sender.getUniqueId());
    }

    UUID replyTarget(Player player) {
        return replyTargets.get(player.getUniqueId());
    }

    void forgetReply(UUID player) {
        replyTargets.remove(player);
    }

    // Checked here instead of requires() so the sender gets the Spanish message rather than "unknown command".
    boolean checkAccess(CommandSender sender, String permission) {
        if (!isEnabled()) {
            messages.send(sender, Keys.Message.GENERAL_MODULE_DISABLED);
            return false;
        }
        if (!sender.hasPermission(permission)) {
            messages.send(sender, Keys.Message.GENERAL_NO_PERMISSION);
            return false;
        }
        return true;
    }

    /**
     * Sends the usage of an incomplete command, once the sender is known to be allowed to run it.
     */
    int usage(CommandSender sender, String permission, String command, String argumentsKey) {
        return checkAccess(sender, permission) ? CommandUsage.send(messages, sender, command, argumentsKey) : 0;
    }

    Player requirePlayer(CommandSender sender, String permission) {
        if (!checkAccess(sender, permission)) {
            return null;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, Keys.Message.GENERAL_PLAYER_ONLY);
            return null;
        }
        return player;
    }
}

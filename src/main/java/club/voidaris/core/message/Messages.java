package club.voidaris.core.message;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ConfigManager configs;

    public Messages(ConfigManager configs) {
        this.configs = configs;
    }

    public Component get(String key, TagResolver... resolvers) {
        TagResolver prefix = Placeholder.parsed("prefix", raw(Keys.Message.PREFIX));
        return MINI_MESSAGE.deserialize(raw(key), TagResolver.resolver(prefix, TagResolver.resolver(resolvers)));
    }

    public String raw(String key) {
        // getString(key, def) ignores the bundled defaults, so the fallback is applied manually.
        String value = configs.messages().get().getString(key);
        return value != null ? value : key;
    }

    public void send(CommandSender target, String key, TagResolver... resolvers) {
        target.sendMessage(get(key, resolvers));
    }
}

package club.voidaris.core.cosmetics;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * PlaceholderAPI expansion (%voidaris_...%) so other plugins such as TAB can show cosmetics. Only instantiated when
 * PlaceholderAPI is present, so its classes are never loaded otherwise.
 */
public final class CosmeticsPlaceholders extends PlaceholderExpansion {

    private static final String IDENTIFIER = "voidaris";
    private static final String NICKNAME = "nickname";
    private static final String BADGE = "badge";
    // &#RRGGBB is understood by TAB and turned back into MiniMessage by our own PlaceholderAPI hook.
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private final JavaPlugin plugin;
    private final CosmeticsModule module;

    public CosmeticsPlaceholders(JavaPlugin plugin, CosmeticsModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    // Registered by the plugin itself, so a /papi reload must not drop it.
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public List<String> getPlaceholders() {
        return List.of("%" + IDENTIFIER + "_" + NICKNAME + "%", "%" + IDENTIFIER + "_" + BADGE + "%");
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return null;
        }
        // Cosmetic selections are only cached for online players.
        var online = player.getPlayer();
        return switch (params) {
            case NICKNAME -> online != null ? LEGACY.serialize(module.nickname(online))
                    : player.getName() == null ? "" : player.getName();
            case BADGE -> online != null ? LEGACY.serialize(module.badge(online)) : "";
            default -> null;
        };
    }
}

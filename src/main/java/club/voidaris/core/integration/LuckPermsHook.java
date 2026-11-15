package club.voidaris.core.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;

// Kept apart from Integrations so LuckPerms classes are only loaded when the plugin is actually present.
final class LuckPermsHook {

    private final LuckPerms api = LuckPermsProvider.get();

    String primaryGroup(Player player) {
        return meta(player).getPrimaryGroup();
    }

    String prefix(Player player) {
        String prefix = meta(player).getPrefix();
        return prefix != null ? prefix : "";
    }

    String suffix(Player player) {
        String suffix = meta(player).getSuffix();
        return suffix != null ? suffix : "";
    }

    private CachedMetaData meta(Player player) {
        return api.getPlayerAdapter(Player.class).getMetaData(player);
    }
}

package club.voidaris.core.homes;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.module.Module;
import club.voidaris.core.teleport.Teleporter;
import org.bukkit.World;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;

public final class HomesModule extends Module {

    /** Entry of {@code limits} in homes.yml that applies to everyone, without a permission. */
    private static final String DEFAULT_LIMIT = "default";

    private final ConfigManager configs;
    private final HomeStore store;
    private Pattern namePattern;

    public HomesModule(JavaPlugin plugin, ConfigManager configs) {
        super("homes");
        this.configs = configs;
        this.store = new HomeStore(plugin);
    }

    @Override
    protected void onEnable() {
        store.open();
        compileNamePattern();
    }

    @Override
    protected void onReload() {
        compileNamePattern();
    }

    @Override
    protected void onDisable() {
        store.close();
    }

    private void compileNamePattern() {
        namePattern = Pattern.compile(configs.homes().get().getString("name-pattern"));
    }

    HomeStore store() {
        return store;
    }

    boolean isValidName(String name) {
        return namePattern.matcher(name).matches();
    }

    String defaultHomeName() {
        return configs.homes().get().getString("default-home-name");
    }

    boolean isBlocked(World world) {
        return configs.homes().get().getStringList("blocked-worlds").contains(world.getName());
    }

    /**
     * @return the highest limit among {@code default} and the {@code limits} entries the player has the permission of
     */
    int limit(Permissible permissible) {
        if (permissible.hasPermission(Keys.Permission.HOMES_LIMIT_UNLIMITED)) {
            return Integer.MAX_VALUE;
        }
        var limits = configs.homes().get().getConfigurationSection("limits");
        int limit = 0;
        if (limits != null) {
            for (String group : limits.getKeys(false)) {
                if (group.equals(DEFAULT_LIMIT) || permissible.hasPermission(Keys.Permission.HOMES_LIMIT + group)) {
                    limit = Math.max(limit, limits.getInt(group));
                }
            }
        }
        return limit;
    }

    Teleporter.Settings teleportSettings() {
        return Teleporter.Settings.of(configs.homes().get(), configs.teleports().get().getConfigurationSection("defaults"))
                .withCooldownBypass(Keys.Permission.HOMES_COOLDOWN_BYPASS);
    }
}

package club.voidaris.core.teleport;

import club.voidaris.core.config.ConfigManager;
import club.voidaris.core.cosmetics.CosmeticsModule;
import club.voidaris.core.module.Module;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TeleportEffectsModule extends Module {

    /**
     * @param sound {@code null} when disabled or misconfigured
     */
    record Settings(Set<TeleportCause> causes, Sound sound, boolean visibleToOthers) {
    }

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private final CosmeticsModule cosmetics;
    private volatile Settings settings;

    public TeleportEffectsModule(JavaPlugin plugin, ConfigManager configs, CosmeticsModule cosmetics) {
        super("teleport-effects");
        this.plugin = plugin;
        this.configs = configs;
        this.cosmetics = cosmetics;
    }

    @Override
    protected void onEnable() {
        settings = read();
    }

    @Override
    protected void onReload() {
        settings = read();
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new TeleportEffectsListener(plugin, this));
    }

    CosmeticsModule cosmetics() {
        return cosmetics;
    }

    Settings settings() {
        return settings;
    }

    private Settings read() {
        var effects = configs.teleports().get().getConfigurationSection("effects");
        var log = plugin.getLogger();

        var causes = EnumSet.noneOf(TeleportCause.class);
        for (String name : effects.getStringList("causes")) {
            try {
                causes.add(TeleportCause.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                log.warning("Unknown teleport cause in teleports.yml effects.causes: " + name);
            }
        }

        Sound sound = null;
        if (effects.getBoolean("sound.enabled")) {
            try {
                sound = Sound.sound(Key.key(effects.getString("sound.key")), Sound.Source.PLAYER,
                        (float) effects.getDouble("sound.volume"), (float) effects.getDouble("sound.pitch"));
            } catch (InvalidKeyException | NullPointerException e) {
                log.warning("Invalid sound key in teleports.yml effects.sound.key; the sound is disabled.");
            }
        }

        return new Settings(causes, sound, effects.getBoolean("particles.visible-to-others"));
    }
}

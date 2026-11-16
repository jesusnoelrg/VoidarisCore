package club.voidaris.core.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * @param layers empty for a cosmetic that shows nothing (such as none)
 */
public record TeleportParticle(String id, String name, List<ParticleLayer> layers) {

    public void play(Plugin plugin, Player player, boolean visibleToOthers) {
        layers.forEach(layer -> layer.play(plugin, player, visibleToOthers));
    }
}

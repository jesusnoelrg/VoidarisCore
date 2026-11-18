package club.voidaris.core.cosmetics;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * A trail drawn while the player moves, with the layers of today's season already resolved.
 *
 * @param interval ticks between two frames while moving
 * @param layers   empty for a cosmetic that shows nothing (such as none)
 */
public record Aura(String id, String name, boolean premium, int interval, List<ParticleLayer> layers)
        implements Cosmetic {

    void frame(Player player, int tick) {
        layers.forEach(layer -> layer.frame(player, tick));
    }
}

package club.voidaris.core.cosmetics;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * One part of a teleport particle cosmetic, drawn around the player in a shape.
 *
 * @param colors   only used by DUST (one color per particle, cycled) and DUST_COLOR_TRANSITION (first to second)
 * @param duration ticks the shape takes to rise to {@code height}; 0 draws it once
 */
public record ParticleLayer(Particle particle, Shape shape, int count, double spread, double speed, List<Color> colors,
                            float size, double radius, double height, int duration, int strands) {

    public enum Shape {
        /** count particles at once around the body. */
        BURST,
        /** A horizontal circle of count points, rising from the feet when duration is set. */
        RING,
        /** strands spirals rising around the player over duration ticks. */
        HELIX
    }

    private static final double TURNS = 2;

    void play(Plugin plugin, Player player, boolean visibleToOthers) {
        if (shape == Shape.BURST || duration <= 0) {
            draw(player, visibleToOthers, duration <= 0 ? 1 : duration, duration <= 0 ? 1 : duration);
            return;
        }
        int[] tick = {0};
        player.getScheduler().runAtFixedRate(plugin, task -> {
            tick[0]++;
            draw(player, visibleToOthers, tick[0], duration);
            if (tick[0] >= duration) {
                task.cancel();
            }
        }, null, 1L, 1L);
    }

    private void draw(Player player, boolean visibleToOthers, int tick, int ticks) {
        var feet = player.getLocation();
        double progress = (double) tick / ticks;
        switch (shape) {
            case BURST -> {
                var body = feet.clone().add(0, 1, 0);
                // Dust colors are split evenly so every color of the palette shows up.
                int palette = Math.max(1, colors.size());
                for (int i = 0; i < palette; i++) {
                    spawn(player, visibleToOthers, body, Math.max(1, count / palette), spread, data(i));
                }
            }
            case RING -> {
                double y = height * progress;
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count;
                    spawn(player, visibleToOthers, point(feet, angle, y), 1, 0, data(i));
                }
            }
            case HELIX -> {
                double y = height * progress;
                double turn = 2 * Math.PI * TURNS * progress;
                for (int strand = 0; strand < strands; strand++) {
                    double angle = turn + 2 * Math.PI * strand / strands;
                    spawn(player, visibleToOthers, point(feet, angle, y), Math.max(1, count), spread, data(strand));
                }
            }
        }
    }

    private Location point(Location feet, double angle, double y) {
        return feet.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private void spawn(Player player, boolean visibleToOthers, Location at, int amount, double offset, Object data) {
        if (visibleToOthers) {
            at.getWorld().spawnParticle(particle, at, amount, offset, offset, offset, speed, data, true);
        } else {
            player.spawnParticle(particle, at, amount, offset, offset, offset, speed, data);
        }
    }

    private Object data(int index) {
        Color first = colors.isEmpty() ? Color.WHITE : colors.get(index % colors.size());
        if (particle.getDataType() == Particle.DustOptions.class) {
            return new Particle.DustOptions(first, size);
        }
        if (particle.getDataType() == Particle.DustTransition.class) {
            return new Particle.DustTransition(colors.isEmpty() ? Color.WHITE : colors.getFirst(),
                    colors.size() > 1 ? colors.get(1) : first, size);
        }
        return null;
    }
}

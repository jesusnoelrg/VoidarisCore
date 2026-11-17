package club.voidaris.core.cosmetics;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * One part of a particle cosmetic (teleport particles and auras), drawn around the player in a shape.
 *
 * @param colors   only used by DUST (one color per particle, cycled) and DUST_COLOR_TRANSITION (first to second)
 * @param duration ticks the shape takes to rise to {@code height} (one turn for orbit); 0 draws it once
 */
public record ParticleLayer(Particle particle, Shape shape, int count, double spread, double speed, List<Color> colors,
                            float size, double radius, double height, int duration, int strands) {

    public enum Shape {
        /** count particles at once around the body. */
        BURST,
        /** A horizontal circle of count points, rising from the feet when duration is set. */
        RING,
        /** strands spirals rising around the player over duration ticks. */
        HELIX,
        /** count particles at the feet (raised by height), each of a random color of the palette. */
        TRAIL,
        /** strands points circling the player at height, one turn every duration ticks. */
        ORBIT
    }

    private static final double TURNS = 2;
    private static final int AURA_RANGE = 32;

    @FunctionalInterface
    private interface Emitter {
        void emit(Location at, int amount, double offset, Object data);
    }

    /**
     * Empty (and logged) when the particle, shape or a color is invalid.
     */
    static Optional<ParticleLayer> parse(String path, Map<?, ?> raw, Logger logger) {
        var section = new MemoryConfiguration();
        raw.forEach((key, value) -> section.set(String.valueOf(key), value));
        String type = section.getString("particle", "");
        Particle particle;
        Shape shape;
        try {
            particle = Particle.valueOf(type.toUpperCase(Locale.ROOT));
            shape = Shape.valueOf(section.getString("shape", "burst").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown particle or shape in cosmetics.yml at " + path);
            return Optional.empty();
        }
        Class<?> data = particle.getDataType();
        if (data != Void.class && data != Particle.DustOptions.class && data != Particle.DustTransition.class) {
            logger.warning("Particle " + particle + " at " + path + " needs data other than colors and is not supported.");
            return Optional.empty();
        }
        List<Color> colors = new ArrayList<>();
        for (String hex : section.getStringList("colors")) {
            try {
                colors.add(Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16)));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid color " + hex + " in cosmetics.yml at " + path + " (expected #RRGGBB)");
                return Optional.empty();
            }
        }
        boolean pointShape = shape == Shape.HELIX || shape == Shape.TRAIL || shape == Shape.ORBIT;
        return Optional.of(new ParticleLayer(particle, shape,
                section.getInt("count", pointShape ? 1 : 30),
                section.getDouble("spread", shape == Shape.BURST ? 0.5 : shape == Shape.TRAIL ? 0.15 : 0),
                section.getDouble("speed", 0),
                List.copyOf(colors),
                (float) section.getDouble("size", 1),
                section.getDouble("radius", 0.8),
                section.getDouble("height", shape == Shape.TRAIL ? 0.3 : shape == Shape.ORBIT ? 1 : 2),
                section.getInt("duration", shape == Shape.HELIX ? 20 : shape == Shape.ORBIT ? 40 : 0),
                Math.max(1, section.getInt("strands", 2))));
    }

    void play(Plugin plugin, Player player, boolean visibleToOthers) {
        play(plugin, player, (at, amount, offset, data) -> {
            if (visibleToOthers) {
                at.getWorld().spawnParticle(particle, at, amount, offset, offset, offset, speed, data, true);
            } else {
                player.spawnParticle(particle, at, amount, offset, offset, offset, speed, data);
            }
        });
    }

    /**
     * Plays the layer for the players within {@code range} blocks who can see {@code player}.
     */
    void play(Plugin plugin, Player player, int range) {
        play(plugin, player, (at, amount, offset, data) -> particle.builder()
                .location(at)
                .count(amount)
                .offset(offset, offset, offset)
                .speed(speed)
                .data(data)
                .source(player)
                .receivers(range, true)
                .spawn());
    }

    private void play(Plugin plugin, Player player, Emitter emitter) {
        if (shape == Shape.BURST || shape == Shape.TRAIL || duration <= 0) {
            draw(player, duration <= 0 ? 1 : duration, duration <= 0 ? 1 : duration, emitter);
            return;
        }
        int[] tick = {0};
        player.getScheduler().runAtFixedRate(plugin, task -> {
            tick[0]++;
            draw(player, tick[0], duration, emitter);
            if (tick[0] >= duration) {
                task.cancel();
            }
        }, null, 1L, 1L);
    }

    /**
     * Draws one frame of an aura. {@code tick} only moves orbit points, so any steadily increasing counter works.
     */
    void frame(Player player, int tick) {
        // source hides the particles from players who cannot see the wearer (vanish), and the normal range
        // instead of force keeps a constant effect from reaching players far away.
        draw(player, tick, Math.max(1, duration), (at, amount, offset, data) -> particle.builder()
                .location(at)
                .count(amount)
                .offset(offset, offset, offset)
                .speed(speed)
                .data(data)
                .source(player)
                .receivers(AURA_RANGE, true)
                .spawn());
    }

    private void draw(Player player, int tick, int ticks, Emitter emitter) {
        var feet = player.getLocation();
        double progress = (double) tick / ticks;
        switch (shape) {
            case BURST -> {
                var body = feet.clone().add(0, 1, 0);
                // Dust colors are split evenly so every color of the palette shows up.
                int palette = Math.max(1, colors.size());
                for (int i = 0; i < palette; i++) {
                    emitter.emit(body, Math.max(1, count / palette), spread, data(i));
                }
            }
            case RING -> {
                double y = height * progress;
                for (int i = 0; i < count; i++) {
                    double angle = 2 * Math.PI * i / count;
                    emitter.emit(point(feet, angle, y), 1, 0, data(i));
                }
            }
            case HELIX -> {
                double y = height * progress;
                double turn = 2 * Math.PI * TURNS * progress;
                for (int strand = 0; strand < strands; strand++) {
                    double angle = turn + 2 * Math.PI * strand / strands;
                    emitter.emit(point(feet, angle, y), Math.max(1, count), spread, data(strand));
                }
            }
            // A trail is drawn a few particles at a time, so a random color spreads the palette along it.
            case TRAIL -> emitter.emit(feet.clone().add(0, height, 0), Math.max(1, count), spread,
                    data(ThreadLocalRandom.current().nextInt(Math.max(1, colors.size()))));
            case ORBIT -> {
                double turn = 2 * Math.PI * (tick % ticks) / ticks;
                for (int strand = 0; strand < strands; strand++) {
                    double angle = turn + 2 * Math.PI * strand / strands;
                    emitter.emit(point(feet, angle, height), Math.max(1, count), spread, data(strand));
                }
            }
        }
    }

    private Location point(Location feet, double angle, double y) {
        return feet.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
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

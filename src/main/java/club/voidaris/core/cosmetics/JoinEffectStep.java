package club.voidaris.core.cosmetics;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * One part of a join effect, played {@link #delay()} ticks after the effect starts. Everything it shows is limited
 * to the players within {@link Stage#range()} blocks who can see the joining player.
 */
sealed interface JoinEffectStep {

    /**
     * @param marker set on every entity a step spawns, so {@link JoinEffectListener} can keep them harmless
     */
    record Stage(Plugin plugin, NamespacedKey marker, int range) {
    }

    int delay();

    void play(Stage stage, Player player);

    /**
     * Empty (and logged) when the type or one of its values is invalid.
     */
    static Optional<JoinEffectStep> parse(String path, Map<?, ?> raw, Logger logger) {
        var section = new MemoryConfiguration();
        raw.forEach((key, value) -> section.set(String.valueOf(key), value));
        int delay = Math.max(0, section.getInt("delay"));
        String type = section.getString("type", "");
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "lightning" -> Optional.of(new Lightning(delay, Math.max(1, section.getInt("count", 3)),
                    Math.max(1, section.getInt("interval", 8)), section.getDouble("radius", 3)));
            case "firework" -> Fireworks.parse(path, section, delay, logger);
            case "items" -> Items.parse(path, section, delay, logger);
            case "particles" -> ParticleLayer.parse(path, raw, logger).map(layer -> new Particles(delay, layer));
            case "sound" -> SoundStep.parse(path, section, delay, logger);
            default -> {
                logger.warning("Unknown join effect type '" + type + "' in cosmetics.yml at " + path
                        + " (expected lightning, firework, items, particles or sound).");
                yield Optional.empty();
            }
        };
    }

    private static Optional<Sound> parseSound(String path, MemoryConfiguration section, Logger logger) {
        try {
            return Optional.of(Sound.sound(Key.key(section.getString("key", "")), Sound.Source.PLAYER,
                    (float) section.getDouble("volume", 1), (float) section.getDouble("pitch", 1)));
        } catch (InvalidKeyException e) {
            logger.warning("Invalid sound key in cosmetics.yml at " + path + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private static List<Player> viewers(Player player, int range) {
        return player.getLocation().getNearbyPlayers(range).stream()
                .filter(viewer -> viewer.equals(player) || viewer.canSee(player))
                .toList();
    }

    /**
     * Must run before the entity is added to the world, or players out of range would already be tracking it.
     */
    private static void restrict(Stage stage, Entity entity, List<Player> viewers) {
        entity.setPersistent(false);
        entity.getPersistentDataContainer().set(stage.marker(), PersistentDataType.BOOLEAN, true);
        entity.setVisibleByDefault(false);
        viewers.forEach(viewer -> viewer.showEntity(stage.plugin(), entity));
    }

    /**
     * Runs {@code shot} {@code count} times, {@code interval} ticks apart; stops if the player quits.
     */
    private static void repeat(Stage stage, Player player, int count, int interval, Runnable shot) {
        shot.run();
        for (int i = 1; i < count; i++) {
            player.getScheduler().runDelayed(stage.plugin(), task -> shot.run(), null, (long) i * interval);
        }
    }

    private static Location around(Player player, double radius) {
        double angle = ThreadLocalRandom.current().nextDouble(2 * Math.PI);
        return player.getLocation().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    private static List<Color> parseColors(String path, List<String> hexes, Logger logger) {
        List<Color> colors = new ArrayList<>();
        for (String hex : hexes) {
            try {
                colors.add(Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16)));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid color " + hex + " in cosmetics.yml at " + path + " (expected #RRGGBB)");
                return null;
            }
        }
        return List.copyOf(colors);
    }

    /**
     * Visual-only bolts struck {@code radius} blocks around the player. Their thunder is played by each client
     * that sees them, so it is only heard within the range.
     */
    record Lightning(int delay, int count, int interval, double radius) implements JoinEffectStep {

        // World#strikeLightningEffect adds the bolt before returning it, so it is restricted from the
        // LightningStrikeEvent fired just before that, on the same thread.
        private static final ThreadLocal<Consumer<LightningStrike>> PENDING = new ThreadLocal<>();

        static void claim(LightningStrike bolt) {
            var pending = PENDING.get();
            if (pending != null) {
                PENDING.remove();
                pending.accept(bolt);
            }
        }

        @Override
        public void play(Stage stage, Player player) {
            repeat(stage, player, count, interval, () -> {
                var viewers = viewers(player, stage.range());
                var at = around(player, radius);
                PENDING.set(bolt -> restrict(stage, bolt, viewers));
                try {
                    at.getWorld().strikeLightningEffect(at);
                } finally {
                    PENDING.remove();
                }
            });
        }
    }

    /**
     * Rockets launched {@code radius} blocks around the player that explode after {@code flight} ticks.
     * Each one uses two colors of the palette, in turn.
     */
    record Fireworks(int delay, int count, int interval, double radius, int flight, FireworkEffect.Type type,
                     List<Color> colors, boolean flicker, boolean trail) implements JoinEffectStep {

        static Optional<JoinEffectStep> parse(String path, MemoryConfiguration section, int delay, Logger logger) {
            FireworkEffect.Type type;
            try {
                type = FireworkEffect.Type.valueOf(section.getString("shape", "ball_large").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown firework shape in cosmetics.yml at " + path
                        + " (expected ball, ball_large, star, burst or creeper).");
                return Optional.empty();
            }
            var colors = parseColors(path, section.getStringList("colors"), logger);
            if (colors == null) {
                return Optional.empty();
            }
            return Optional.of(new Fireworks(delay, Math.max(1, section.getInt("count", 3)),
                    Math.max(1, section.getInt("interval", 10)), section.getDouble("radius", 1.5),
                    Math.max(1, section.getInt("flight", 20)), type,
                    colors.isEmpty() ? List.of(Color.WHITE) : colors,
                    section.getBoolean("flicker"), section.getBoolean("trail")));
        }

        @Override
        public void play(Stage stage, Player player) {
            int[] shot = {0};
            repeat(stage, player, count, interval, () -> {
                var viewers = viewers(player, stage.range());
                int first = shot[0]++;
                var effect = FireworkEffect.builder()
                        .with(type)
                        .withColor(colors.get(first % colors.size()), colors.get((first + 1) % colors.size()))
                        .flicker(flicker)
                        .trail(trail)
                        .build();
                var at = around(player, radius);
                at.getWorld().spawn(at, Firework.class, firework -> {
                    restrict(stage, firework, viewers);
                    var meta = firework.getFireworkMeta();
                    meta.addEffect(effect);
                    firework.setFireworkMeta(meta);
                    firework.setTicksToDetonate(flight);
                });
            });
        }
    }

    /**
     * {@code count} items of random {@code materials} thrown out of the player's head over {@code duration} ticks,
     * each removed {@code lifetime} ticks later. Nobody can pick them up.
     *
     * @param sound      played with each item for the players within {@code soundRange} blocks; null for none
     */
    record Items(int delay, List<Material> materials, int count, int duration, int lifetime, double speed,
                 Sound sound, int soundRange) implements JoinEffectStep {

        static Optional<JoinEffectStep> parse(String path, MemoryConfiguration section, int delay, Logger logger) {
            List<Material> materials = new ArrayList<>();
            for (String name : section.getStringList("materials")) {
                var material = Material.matchMaterial(name);
                if (material == null || !material.isItem() || material.isAir()) {
                    logger.warning("Unknown item " + name + " in cosmetics.yml at " + path);
                    return Optional.empty();
                }
                materials.add(material);
            }
            if (materials.isEmpty()) {
                logger.warning("Join effect items need at least one material (cosmetics.yml at " + path + ").");
                return Optional.empty();
            }
            Sound sound = null;
            int soundRange = 0;
            if (section.get("sound") instanceof Map<?, ?> rawSound) {
                var soundSection = new MemoryConfiguration();
                rawSound.forEach((key, value) -> soundSection.set(String.valueOf(key), value));
                var parsed = parseSound(path + ".sound", soundSection, logger);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                sound = parsed.get();
                soundRange = Math.max(1, soundSection.getInt("range", 12));
            }
            int count = Math.max(1, section.getInt("count", 12));
            return Optional.of(new Items(delay, List.copyOf(materials), count,
                    Math.max(1, section.getInt("duration", 20)), Math.max(1, section.getInt("lifetime", 40)),
                    section.getDouble("speed", 0.4), sound, soundRange));
        }

        @Override
        public void play(Stage stage, Player player) {
            repeat(stage, player, count, Math.max(1, duration / count), () -> {
                var random = ThreadLocalRandom.current();
                var viewers = viewers(player, stage.range());
                var stack = new ItemStack(materials.get(random.nextInt(materials.size())));
                double angle = random.nextDouble(2 * Math.PI);
                double outward = speed * random.nextDouble(0.25, 0.5);
                var velocity = new Vector(Math.cos(angle) * outward, speed * random.nextDouble(0.8, 1.2),
                        Math.sin(angle) * outward);
                var head = player.getEyeLocation().add(0, 0.3, 0);
                var item = head.getWorld().dropItem(head, stack, dropped -> {
                    restrict(stage, dropped, viewers);
                    dropped.setCanPlayerPickup(false);
                    dropped.setCanMobPickup(false);
                    dropped.setPickupDelay(Integer.MAX_VALUE);
                    dropped.setWillAge(false);
                    dropped.setVelocity(velocity);
                });
                item.getScheduler().runDelayed(stage.plugin(), task -> item.remove(), null, lifetime);
                if (sound != null) {
                    // Same pitch spread as vanilla, so a burst of items does not sound like one repeated note.
                    float pitch = sound.pitch() + (random.nextFloat() - random.nextFloat()) * 0.2f;
                    var played = Sound.sound(sound).pitch(pitch).build();
                    viewers(player, soundRange).forEach(viewer -> viewer.playSound(played, head.getX(), head.getY(), head.getZ()));
                }
            });
        }
    }

    record Particles(int delay, ParticleLayer layer) implements JoinEffectStep {

        @Override
        public void play(Stage stage, Player player) {
            layer.play(stage.plugin(), player, stage.range());
        }
    }

    /**
     * @param shared heard by every player within the range instead of only by the joining player
     */
    record SoundStep(int delay, Sound sound, boolean shared) implements JoinEffectStep {

        static Optional<JoinEffectStep> parse(String path, MemoryConfiguration section, int delay, Logger logger) {
            return parseSound(path, section, logger).map(sound -> new SoundStep(delay, sound, section.getBoolean("public")));
        }

        @Override
        public void play(Stage stage, Player player) {
            if (!shared) {
                player.playSound(sound, Sound.Emitter.self());
                return;
            }
            var at = player.getLocation();
            viewers(player, stage.range()).forEach(viewer -> viewer.playSound(sound, at.getX(), at.getY(), at.getZ()));
        }
    }
}

package club.voidaris.core.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;

/**
 * A location kept by world name, so it survives the world being unloaded when the data file is read.
 */
public record StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public static StoredLocation of(Location location) {
        return new StoredLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    public static Optional<StoredLocation> read(ConfigurationSection section) {
        String world = section.getString("world");
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new StoredLocation(world, section.getDouble("x"), section.getDouble("y"),
                section.getDouble("z"), (float) section.getDouble("yaw"), (float) section.getDouble("pitch")));
    }

    public void write(ConfigurationSection section) {
        section.set("world", world);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
    }

    /**
     * @return empty when the world is not loaded
     */
    public Optional<Location> toLocation() {
        var bukkitWorld = Bukkit.getWorld(world);
        return bukkitWorld == null ? Optional.empty()
                : Optional.of(new Location(bukkitWorld, x, y, z, yaw, pitch));
    }
}

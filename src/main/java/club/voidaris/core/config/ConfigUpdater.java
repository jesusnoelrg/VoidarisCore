package club.voidaris.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Brings a file on disk up to date with the bundled copy: runs {@code config-version} migrations, adds missing keys
 * (with their comments) and warns about unknown keys and values of the wrong type.
 */
final class ConfigUpdater {

    static final String VERSION = "config-version";

    private final String name;
    private final Set<String> userSections;
    private final List<ConfigMigration> migrations;
    private final Logger logger;

    /**
     * @param userSections paths whose children are created by the admin (cosmetics, kits, menu items...), so
     *                     entries removed or added under them are not reported or restored
     * @param migrations   {@code migrations.get(i)} turns version {@code i + 1} into {@code i + 2}
     */
    ConfigUpdater(String name, Set<String> userSections, List<ConfigMigration> migrations, Logger logger) {
        this.name = name;
        this.userSections = userSections;
        this.migrations = migrations;
        this.logger = logger;
    }

    /**
     * @return true when {@code disk} was changed and must be saved
     */
    boolean update(YamlConfiguration disk, YamlConfiguration bundled) {
        boolean changed = migrate(disk, bundled);
        changed |= addMissing(disk, bundled);
        reportUnknownAndInvalid(disk, bundled);
        return changed;
    }

    private boolean migrate(YamlConfiguration disk, YamlConfiguration bundled) {
        if (!bundled.isInt(VERSION)) {
            return false;
        }
        int target = bundled.getInt(VERSION);
        // Files written before config-version existed (or with a nonsense value) are treated as version 1.
        int current = Math.max(1, disk.getInt(VERSION, 1));
        if (current > target) {
            logger.warning(name + " has config-version " + current + ", newer than this plugin (" + target
                    + "); it was not migrated.");
            return false;
        }
        while (current < target) {
            if (current - 1 < migrations.size()) {
                migrations.get(current - 1).migrate(disk);
            }
            current++;
            logger.info("Migrated " + name + " to config-version " + current + ".");
        }
        if (disk.isInt(VERSION) && disk.getInt(VERSION) == target) {
            return false;
        }
        disk.set(VERSION, target);
        return true;
    }

    private boolean addMissing(YamlConfiguration disk, YamlConfiguration bundled) {
        List<String> added = new ArrayList<>();
        for (String path : bundled.getKeys(true)) {
            // Only the topmost missing key is copied; its children come with it in copy().
            if (disk.contains(path, true) || isUserEntry(path) || !parentExists(disk, path)) {
                continue;
            }
            copy(disk, bundled, path);
            added.add(path);
        }
        if (!added.isEmpty()) {
            logger.warning("Added missing keys to " + name + " with their default values: " + String.join(", ", added));
        }
        return !added.isEmpty();
    }

    private void copy(YamlConfiguration disk, YamlConfiguration bundled, String root) {
        copyNode(disk, bundled, root);
        var section = bundled.getConfigurationSection(root);
        if (section != null) {
            for (String child : section.getKeys(true)) {
                copyNode(disk, bundled, root + "." + child);
            }
        }
    }

    private static void copyNode(YamlConfiguration disk, YamlConfiguration bundled, String path) {
        if (bundled.isConfigurationSection(path)) {
            disk.createSection(path);
        } else {
            disk.set(path, bundled.get(path));
        }
        disk.setComments(path, bundled.getComments(path));
        disk.setInlineComments(path, bundled.getInlineComments(path));
    }

    private void reportUnknownAndInvalid(YamlConfiguration disk, YamlConfiguration bundled) {
        List<String> unknown = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        for (String path : disk.getKeys(true)) {
            if (isUserEntry(path) || !parentExists(bundled, path)) {
                continue;
            }
            if (!bundled.contains(path, true)) {
                unknown.add(path);
                continue;
            }
            String expected = kind(bundled.get(path));
            if (!expected.equals(kind(disk.get(path)))) {
                invalid.add(path + " (expected " + expected + ")");
            }
        }
        if (!unknown.isEmpty()) {
            logger.warning("Unknown keys in " + name + " (typo?), they are ignored: " + String.join(", ", unknown));
        }
        if (!invalid.isEmpty()) {
            logger.warning("Invalid values in " + name + ", the default is used instead: " + String.join(", ", invalid));
        }
    }

    private static String kind(Object value) {
        return switch (value) {
            case ConfigurationSection ignored -> "section";
            case Boolean ignored -> "true/false";
            case Number ignored -> "number";
            case List<?> ignored -> "list";
            case null -> "nothing";
            default -> "text";
        };
    }

    // An unknown key inside an unknown section is reported once, through its topmost unknown ancestor.
    private static boolean parentExists(ConfigurationSection config, String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 || config.isConfigurationSection(path.substring(0, dot));
    }

    private boolean isUserEntry(String path) {
        for (String section : userSections) {
            if (path.startsWith(section + ".")) {
                return true;
            }
        }
        return false;
    }
}

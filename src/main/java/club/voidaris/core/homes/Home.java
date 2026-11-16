package club.voidaris.core.homes;

import club.voidaris.core.storage.StoredLocation;

/**
 * @param icon item key shown in the homes menu (such as {@code minecraft:red_bed}), or {@code null} for the default one
 */
public record Home(String name, StoredLocation location, String icon) {

    public Home withIcon(String icon) {
        return new Home(name, location, icon);
    }
}

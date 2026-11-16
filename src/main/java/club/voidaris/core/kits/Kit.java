package club.voidaris.core.kits;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * @param cooldownSeconds {@link #NO_COOLDOWN}, {@link #ONE_TIME} or the seconds between claims
 * @param permission      whether claiming it requires {@code voidaris.kits.kit.<name>}
 * @param icon            item key shown in the menu, or {@code null} for the menu's default icon
 * @param items           player inventory slot (as in {@code PlayerInventory#getContents()}) to item
 * @param created         epoch millis of its creation; older claims belong to a deleted kit with the same name
 */
record Kit(String name, long cooldownSeconds, boolean permission, String icon, Map<Integer, ItemStack> items,
           long created) {

    static final long NO_COOLDOWN = 0;
    static final long ONE_TIME = -1;

    Kit {
        items = Map.copyOf(items);
    }

    boolean oneTime() {
        return cooldownSeconds == ONE_TIME;
    }

    Kit withItems(Map<Integer, ItemStack> items) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created);
    }

    Kit withCooldown(long cooldownSeconds) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created);
    }

    Kit withPermission(boolean permission) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created);
    }

    Kit withIcon(String icon) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created);
    }
}

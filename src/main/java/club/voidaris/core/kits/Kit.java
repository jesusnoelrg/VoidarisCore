package club.voidaris.core.kits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * @param cooldownSeconds {@link #NO_COOLDOWN}, {@link #ONE_TIME} or the seconds between claims
 * @param permission      whether claiming it requires {@code voidaris.kits.kit.<name>}
 * @param icon            item key shown in the menu, or {@code null} for the menu's default icon
 * @param items           player inventory slot (as in {@code PlayerInventory#getContents()}) to item
 * @param created         epoch millis of its creation; older claims belong to a deleted kit with the same name
 * @param displayName     MiniMessage name shown to players instead of {@code name}, or {@code null}
 */
record Kit(String name, long cooldownSeconds, boolean permission, String icon, Map<Integer, ItemStack> items,
           long created, String displayName) {

    static final long NO_COOLDOWN = 0;
    static final long ONE_TIME = -1;

    Kit {
        items = Map.copyOf(items);
    }

    boolean oneTime() {
        return cooldownSeconds == ONE_TIME;
    }

    /**
     * @return {@code <kit>} as the display name (the id when there is none) and {@code <id>} as the id, which is what
     * commands accept
     */
    TagResolver placeholders() {
        // Written by staff, so its tags are trusted; player names never reach it.
        Component shown = displayName == null ? Component.text(name) : MiniMessage.miniMessage().deserialize(displayName);
        return TagResolver.resolver(Placeholder.component("kit", shown), Placeholder.unparsed("id", name));
    }

    Kit withItems(Map<Integer, ItemStack> items) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created, displayName);
    }

    Kit withCooldown(long cooldownSeconds) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created, displayName);
    }

    Kit withPermission(boolean permission) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created, displayName);
    }

    Kit withIcon(String icon) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created, displayName);
    }

    Kit withDisplayName(String displayName) {
        return new Kit(name, cooldownSeconds, permission, icon, items, created, displayName);
    }
}

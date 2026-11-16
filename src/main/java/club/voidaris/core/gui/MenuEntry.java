package club.voidaris.core.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * A paginated item of a menu's content area.
 */
public record MenuEntry(ItemStack item, Consumer<InventoryClickEvent> handler) {
}

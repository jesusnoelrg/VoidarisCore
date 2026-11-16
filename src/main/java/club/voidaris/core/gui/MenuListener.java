package club.voidaris.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MenuListener implements Listener {

    // Cancels every click while a menu is open, including shift-clicks from the player's own inventory into it.
    @EventHandler(priority = EventPriority.LOW)
    void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == menu.getInventory()) {
            menu.click(event);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu) {
            event.setCancelled(true);
        }
    }

    /**
     * Closes open menus so their items cannot be taken once this listener is gone.
     */
    public static void closeAll() {
        for (var player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
                player.closeInventory();
            }
        }
    }
}

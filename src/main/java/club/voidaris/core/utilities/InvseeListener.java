package club.voidaris.core.utilities;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Keeps /invsee views read-only for viewers without the modify permission.
 */
final class InvseeListener implements Listener {

    private final UtilitiesModule module;

    InvseeListener(UtilitiesModule module) {
        this.module = module;
    }

    // Every click is cancelled, also in the viewer's own inventory, because a shift-click there moves items into the target's.
    @EventHandler(priority = EventPriority.LOW)
    void onClick(InventoryClickEvent event) {
        if (module.isReadOnlyView(event.getWhoClicked(), event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    void onDrag(InventoryDragEvent event) {
        if (module.isReadOnlyView(event.getWhoClicked(), event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onClose(InventoryCloseEvent event) {
        module.forgetReadOnlyView(event.getPlayer().getUniqueId());
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        module.forgetReadOnlyView(event.getPlayer().getUniqueId());
    }
}

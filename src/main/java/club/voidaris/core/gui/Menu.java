package club.voidaris.core.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * An inventory whose clicks are always cancelled and routed to the handler set for each slot.
 */
public final class Menu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    public Menu(Component title, int size) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public void set(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        inventory.setItem(slot, item);
        if (handler == null) {
            handlers.remove(slot);
        } else {
            handlers.put(slot, handler);
        }
    }

    void click(InventoryClickEvent event) {
        var handler = handlers.get(event.getSlot());
        if (handler != null) {
            handler.accept(event);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

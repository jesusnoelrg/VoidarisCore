package club.voidaris.core.utilities;

import club.voidaris.core.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class UtilitiesModule extends Module {

    private final Set<UUID> godPlayers = new HashSet<>();
    /** Viewer to the inventory they opened with /invsee without the modify permission. */
    private final Map<UUID, Inventory> readOnlyViews = new HashMap<>();

    public UtilitiesModule() {
        super("utilities");
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new GodListener(this), new InvseeListener(this));
    }

    @Override
    protected void onDisable() {
        godPlayers.clear();
        // Closed because their listener is unregistered with the module, which would make them editable.
        for (UUID viewer : List.copyOf(readOnlyViews.keySet())) {
            var player = Bukkit.getPlayer(viewer);
            if (player != null) {
                player.closeInventory();
            }
        }
        readOnlyViews.clear();
    }

    void addReadOnlyView(Player viewer, Inventory inventory) {
        readOnlyViews.put(viewer.getUniqueId(), inventory);
    }

    boolean isReadOnlyView(HumanEntity viewer, Inventory top) {
        return readOnlyViews.get(viewer.getUniqueId()) == top;
    }

    void forgetReadOnlyView(UUID viewer) {
        readOnlyViews.remove(viewer);
    }

    public boolean isGod(Player player) {
        return godPlayers.contains(player.getUniqueId());
    }

    /**
     * @return the new god state
     */
    public boolean toggleGod(Player player) {
        if (godPlayers.remove(player.getUniqueId())) {
            return false;
        }
        godPlayers.add(player.getUniqueId());
        return true;
    }

    void removeGod(Player player) {
        godPlayers.remove(player.getUniqueId());
    }
}

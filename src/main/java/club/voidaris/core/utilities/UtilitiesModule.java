package club.voidaris.core.utilities;

import club.voidaris.core.module.Module;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UtilitiesModule extends Module {

    private final Set<UUID> godPlayers = new HashSet<>();

    public UtilitiesModule() {
        super("utilities");
    }

    @Override
    protected List<Listener> listeners() {
        return List.of(new GodListener(this));
    }

    @Override
    protected void onDisable() {
        godPlayers.clear();
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

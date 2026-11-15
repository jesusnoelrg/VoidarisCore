package club.voidaris.core.cosmetics;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

final class SelectionListener implements Listener {

    private static final long LOAD_TIMEOUT_SECONDS = 5;

    private final Selections selections;
    private final Logger logger;

    SelectionListener(Selections selections, Logger logger) {
        this.selections = selections;
        this.logger = logger;
    }

    // Loaded before login so the selection is cached by the time PlayerJoinEvent builds the join message.
    @EventHandler(priority = EventPriority.MONITOR)
    void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        try {
            selections.load(event.getUniqueId()).get(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warning("Cosmetic selections of " + event.getName() + " took over " + LOAD_TIMEOUT_SECONDS
                    + " seconds to load; the default join message may be shown.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Selections#load must not complete exceptionally", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onQuit(PlayerQuitEvent event) {
        selections.unload(event.getPlayer().getUniqueId());
    }
}

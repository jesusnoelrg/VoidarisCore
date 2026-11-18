package club.voidaris.core.cosmetics;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Played around the player when they join, with the steps of today's season already resolved.
 *
 * @param steps empty for a cosmetic that shows nothing (such as none)
 */
public record JoinEffect(String id, String name, boolean premium, List<JoinEffectStep> steps)
        implements Cosmetic {

    void play(JoinEffectStep.Stage stage, Player player) {
        for (JoinEffectStep step : steps) {
            if (step.delay() == 0) {
                step.play(stage, player);
            } else {
                player.getScheduler().runDelayed(stage.plugin(), task -> step.play(stage, player), null, step.delay());
            }
        }
    }
}

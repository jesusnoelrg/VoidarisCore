package club.voidaris.core.kits;

import club.voidaris.core.Keys;
import club.voidaris.core.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Kit cooldowns as typed by admins ({@code 0}, {@code once}, {@code 90}, {@code 1h30m}, {@code 1d}) and as shown
 * to players.
 */
final class KitTime {

    static final String ONCE = "once";

    private static final Pattern DURATION = Pattern.compile("(?:\\d+[dhms])+");
    private static final Pattern PART = Pattern.compile("(\\d+)([dhms])");

    private KitTime() {
    }

    /**
     * @return the cooldown in seconds ({@link Kit#ONE_TIME} for {@code once}), or empty when the text is invalid
     */
    static OptionalLong parse(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (value.equals(ONCE)) {
            return OptionalLong.of(Kit.ONE_TIME);
        }
        try {
            if (value.chars().allMatch(Character::isDigit)) {
                return OptionalLong.of(Long.parseLong(value));
            }
            if (!DURATION.matcher(value).matches()) {
                return OptionalLong.empty();
            }
            long seconds = 0;
            var matcher = PART.matcher(value);
            while (matcher.find()) {
                long amount = Long.parseLong(matcher.group(1));
                seconds = Math.addExact(seconds, Math.multiplyExact(amount, switch (matcher.group(2)) {
                    case "d" -> TimeUnit.DAYS.toSeconds(1);
                    case "h" -> TimeUnit.HOURS.toSeconds(1);
                    case "m" -> TimeUnit.MINUTES.toSeconds(1);
                    default -> 1L;
                }));
            }
            return OptionalLong.of(seconds);
        } catch (NumberFormatException | ArithmeticException e) {
            return OptionalLong.empty();
        }
    }

    /**
     * Rounds up to the next second, so a cooldown that is about to end never reads as zero.
     */
    static Component format(Messages messages, long millis) {
        long total = Math.ceilDiv(Math.max(millis, 0), 1000L);
        long days = total / 86_400;
        long hours = total % 86_400 / 3_600;
        long minutes = total % 3_600 / 60;
        long seconds = total % 60;
        List<Component> parts = new ArrayList<>(4);
        add(parts, messages, Keys.Message.KITS_TIME_DAY, days);
        add(parts, messages, Keys.Message.KITS_TIME_HOUR, hours);
        add(parts, messages, Keys.Message.KITS_TIME_MINUTE, minutes);
        if (seconds > 0 || parts.isEmpty()) {
            parts.add(unit(messages, Keys.Message.KITS_TIME_SECOND, seconds));
        }
        return Component.join(JoinConfiguration.separator(messages.get(Keys.Message.KITS_TIME_SEPARATOR)), parts);
    }

    /**
     * @return the kit's cooldown as shown to players, including the once and no-cooldown texts
     */
    static Component describe(Messages messages, Kit kit) {
        if (kit.oneTime()) {
            return messages.get(Keys.Message.KITS_COOLDOWN_ONCE);
        }
        if (kit.cooldownSeconds() <= Kit.NO_COOLDOWN) {
            return messages.get(Keys.Message.KITS_COOLDOWN_NONE);
        }
        return format(messages, TimeUnit.SECONDS.toMillis(kit.cooldownSeconds()));
    }

    private static void add(List<Component> parts, Messages messages, String key, long value) {
        if (value > 0) {
            parts.add(unit(messages, key, value));
        }
    }

    private static Component unit(Messages messages, String key, long value) {
        return messages.get(key, Placeholder.unparsed("value", String.valueOf(value)));
    }
}

package club.voidaris.core.cosmetics;

/**
 * @param color MiniMessage opening tag(s) placed before the message, such as {@code <red>} or {@code <gradient:...>};
 *              empty for a cosmetic that keeps the chat format's color (such as none)
 */
public record ChatColor(String id, String name, boolean premium, String color) {
}

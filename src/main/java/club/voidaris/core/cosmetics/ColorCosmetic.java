package club.voidaris.core.cosmetics;

/**
 * @param color MiniMessage opening tag(s) placed before the text, such as {@code <red>} or {@code <gradient:...>};
 *              empty for a cosmetic that keeps the surrounding format's color (such as none)
 */
public record ColorCosmetic(String id, String name, boolean premium, String color) {
}

package club.voidaris.core.cosmetics;

/**
 * A cosmetic listed as one icon in a {@link CosmeticMenu}.
 */
interface Cosmetic {

    String id();

    String name();

    /**
     * Adds premium_lore to its icon.
     */
    default boolean premium() {
        return false;
    }

    /**
     * Whether right-clicking its icon previews it, in a menu whose {@link CosmeticMenu.Type} has a preview.
     */
    default boolean previewable() {
        return false;
    }
}

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
}

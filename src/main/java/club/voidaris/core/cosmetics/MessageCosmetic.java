package club.voidaris.core.cosmetics;

/**
 * @param format the MiniMessage format for today's date; what an empty one means depends on its {@link MessageCategory}
 */
record MessageCosmetic(String id, String name, String format) {
}

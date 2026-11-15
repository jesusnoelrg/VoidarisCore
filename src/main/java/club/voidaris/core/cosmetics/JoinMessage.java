package club.voidaris.core.cosmetics;

/**
 * @param format the MiniMessage format for today's date; empty means the join is not announced
 */
record JoinMessage(String id, String name, String format) {
}

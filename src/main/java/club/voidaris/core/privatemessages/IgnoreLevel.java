package club.voidaris.core.privatemessages;

enum IgnoreLevel {
    /** Blocks private messages only ({@code /ignore}). */
    PRIVATE,
    /** Blocks private messages and public chat ({@code /superignore}); takes precedence over {@link #PRIVATE}. */
    ALL
}

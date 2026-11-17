package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigFile;
import club.voidaris.core.config.ConfigManager;

import java.util.function.Function;

/**
 * Cosmetic categories made of MiniMessage formats (badges are formats too, shown before the name) under {@code <section>.cosmetics} in cosmetics.yml.
 *
 * @param slot       the selection slot stored in the database
 * @param guiSection the section of the menu file that holds the icon template
 */
enum MessageCategory {

    JOIN("join-messages", "join-message", Keys.Permission.COSMETICS_JOIN_MESSAGE,
            ConfigManager::joinMessagesGui, "join_messages",
            Keys.Message.COSMETICS_JOIN_MESSAGE_SELECTED, Keys.Message.COSMETICS_JOIN_MESSAGE_NOT_FOUND,
            Keys.Message.COSMETICS_JOIN_MESSAGE_LOCKED, Keys.Message.COSMETICS_JOIN_MESSAGE_PREVIEW_HIDDEN),
    KILL("kill-messages", "kill-message", Keys.Permission.COSMETICS_KILL_MESSAGE,
            ConfigManager::killMessagesGui, "kill_messages",
            Keys.Message.COSMETICS_KILL_MESSAGE_SELECTED, Keys.Message.COSMETICS_KILL_MESSAGE_NOT_FOUND,
            Keys.Message.COSMETICS_KILL_MESSAGE_LOCKED, Keys.Message.COSMETICS_KILL_MESSAGE_PREVIEW_DEFAULT),
    BADGE("badges", "badge", Keys.Permission.COSMETICS_BADGE,
            ConfigManager::badgeListGui, "badges",
            Keys.Message.COSMETICS_BADGE_SELECTED, Keys.Message.COSMETICS_BADGE_NOT_FOUND,
            Keys.Message.COSMETICS_BADGE_LOCKED, Keys.Message.COSMETICS_BADGE_PREVIEW_NONE);

    final String section;
    final String slot;
    final String permission;
    final Function<ConfigManager, ConfigFile> gui;
    final String guiSection;
    final String selectedMessage;
    final String notFoundMessage;
    final String lockedMessage;
    /** Menu preview of a cosmetic whose format is empty. */
    final String emptyPreviewMessage;

    MessageCategory(String section, String slot, String permission, Function<ConfigManager, ConfigFile> gui,
                    String guiSection, String selectedMessage, String notFoundMessage, String lockedMessage,
                    String emptyPreviewMessage) {
        this.section = section;
        this.slot = slot;
        this.permission = permission;
        this.gui = gui;
        this.guiSection = guiSection;
        this.selectedMessage = selectedMessage;
        this.notFoundMessage = notFoundMessage;
        this.lockedMessage = lockedMessage;
        this.emptyPreviewMessage = emptyPreviewMessage;
    }
}

package club.voidaris.core.cosmetics;

import club.voidaris.core.Keys;
import club.voidaris.core.config.ConfigFile;
import club.voidaris.core.config.ConfigManager;

import java.util.function.Function;

/**
 * Cosmetic categories made of MiniMessage colors under {@code <section>.cosmetics} in cosmetics.yml, which players
 * can also replace with any hex color.
 *
 * @param slot           the selection slot stored in the database
 * @param guiSection     the section of the menu file that holds the icon template
 * @param command        the root command that opens the menu, also a subcommand of /cosmetics
 * @param previewMessage sample text of each menu icon, or null to preview the player's own name
 */
enum ColorCategory {

    CHAT("chat-colors", "chat-color", "chat color", Keys.Permission.COSMETICS_CHAT_COLOR,
            Keys.Permission.COSMETICS_CUSTOM_CHAT_COLOR, ConfigManager::chatColorsGui, "chat_colors", "chatcolor",
            "colorchat", Keys.Message.COMMAND_CHATCOLOR_DESCRIPTION, Keys.Message.HELP_CHATCOLOR_ARGUMENTS,
            Keys.Message.HELP_CHATCOLOR_DESCRIPTION,
            Keys.Message.COSMETICS_CHAT_COLOR_SELECTED, Keys.Message.COSMETICS_CHAT_COLOR_NOT_FOUND,
            Keys.Message.COSMETICS_CHAT_COLOR_LOCKED, Keys.Message.COSMETICS_CHAT_COLOR_CUSTOM_SELECTED,
            Keys.Message.COSMETICS_CHAT_COLOR_CUSTOM_LOCKED, Keys.Message.COSMETICS_CHAT_COLOR_CUSTOM_HINT,
            Keys.Message.COSMETICS_CHAT_COLOR_PREVIEW),
    NICK("nick-colors", "nick-color", "nick color", Keys.Permission.COSMETICS_NICK_COLOR,
            Keys.Permission.COSMETICS_CUSTOM_NICK_COLOR, ConfigManager::nickColorsGui, "nick_colors", "nickcolor",
            "colornick", Keys.Message.COMMAND_NICKCOLOR_DESCRIPTION, Keys.Message.HELP_NICKCOLOR_ARGUMENTS,
            Keys.Message.HELP_NICKCOLOR_DESCRIPTION,
            Keys.Message.COSMETICS_NICK_COLOR_SELECTED, Keys.Message.COSMETICS_NICK_COLOR_NOT_FOUND,
            Keys.Message.COSMETICS_NICK_COLOR_LOCKED, Keys.Message.COSMETICS_NICK_COLOR_CUSTOM_SELECTED,
            Keys.Message.COSMETICS_NICK_COLOR_CUSTOM_LOCKED, Keys.Message.COSMETICS_NICK_COLOR_CUSTOM_HINT,
            null);

    final String section;
    final String slot;
    /** Used in console logs. */
    final String label;
    final String permission;
    final String customPermission;
    final Function<ConfigManager, ConfigFile> gui;
    final String guiSection;
    final String command;
    final String alias;
    final String commandDescription;
    final String helpArguments;
    final String helpDescription;
    final String selectedMessage;
    final String notFoundMessage;
    final String lockedMessage;
    final String customSelectedMessage;
    final String customLockedMessage;
    final String customHintMessage;
    final String previewMessage;

    ColorCategory(String section, String slot, String label, String permission, String customPermission,
                  Function<ConfigManager, ConfigFile> gui, String guiSection, String command, String alias,
                  String commandDescription, String helpArguments, String helpDescription,
                  String selectedMessage, String notFoundMessage, String lockedMessage, String customSelectedMessage,
                  String customLockedMessage, String customHintMessage, String previewMessage) {
        this.section = section;
        this.slot = slot;
        this.label = label;
        this.permission = permission;
        this.customPermission = customPermission;
        this.gui = gui;
        this.guiSection = guiSection;
        this.command = command;
        this.alias = alias;
        this.commandDescription = commandDescription;
        this.helpArguments = helpArguments;
        this.helpDescription = helpDescription;
        this.selectedMessage = selectedMessage;
        this.notFoundMessage = notFoundMessage;
        this.lockedMessage = lockedMessage;
        this.customSelectedMessage = customSelectedMessage;
        this.customLockedMessage = customLockedMessage;
        this.customHintMessage = customHintMessage;
        this.previewMessage = previewMessage;
    }
}

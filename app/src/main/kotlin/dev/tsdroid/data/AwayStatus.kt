package dev.tsdroid.data

/**
 * The three entries of the desktop client's status menu.
 *
 * TeamSpeak has no "do not disturb" bit on the wire: a client is either online
 * or away (`clientupdate client_away`), with a free-form away message next to
 * it. Do-not-disturb is therefore an away state with its own default message —
 * plus a local microphone gate in the app, which other clients cannot see and
 * which is what makes the state mean anything on the phone.
 */
enum class AwayStatus(
    /** Value for `clientupdate client_away`. */
    val away: Boolean,
    /** Message used when the user did not type one; null for online. */
    val defaultMessage: String?,
) {
    ONLINE(away = false, defaultMessage = null),
    AWAY(away = true, defaultMessage = "Отошёл"),
    DND(away = true, defaultMessage = "Не беспокоить"),
    ;

    /**
     * Away message to send: the user's text if there is one, otherwise the
     * default for this status. Always null while online — going back online
     * has to clear the message the server still holds.
     */
    fun message(custom: String): String? {
        if (!away) return null
        val trimmed = custom.trim()
        return trimmed.ifEmpty { defaultMessage }
    }
}

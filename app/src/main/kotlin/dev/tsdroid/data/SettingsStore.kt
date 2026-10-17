package dev.tsdroid.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val KEY_AUDIO_GAIN = floatPreferencesKey("audio_gain")
private val KEY_SHOW_LINK_THUMBNAILS = booleanPreferencesKey("show_link_thumbnails")
private val KEY_AUTO_LOAD_IMAGES = booleanPreferencesKey("auto_load_images")
private val KEY_LANGUAGE = stringPreferencesKey("language")
private val KEY_ENABLE_FLOATING_WINDOW = booleanPreferencesKey("enable_floating_window")
private val KEY_ANIME_BACKGROUND = booleanPreferencesKey("anime_background")
private val KEY_NOISE_SUPPRESSION = booleanPreferencesKey("noise_suppression")
private val KEY_PUSH_TO_TALK = booleanPreferencesKey("push_to_talk")

class SettingsStore(private val context: Context) {

    val audioGain: Flow<Float> = context.settingsDataStore.data
        .map { it[KEY_AUDIO_GAIN] ?: 1.0f }

    val showLinkThumbnails: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_SHOW_LINK_THUMBNAILS] ?: false }

    val autoLoadImages: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_AUTO_LOAD_IMAGES] ?: true }

    val language: Flow<String> = context.settingsDataStore.data
        .map { it[KEY_LANGUAGE] ?: "ru" }

    val enableFloatingWindow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_ENABLE_FLOATING_WINDOW] ?: true }

    /**
     * Off by default: the app is a normal Material surface until the user asks for the wallpaper,
     * which also keeps the first launch free of a network fetch for an image.
     */
    val animeBackground: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_ANIME_BACKGROUND] ?: false }

    suspend fun setAudioGain(gain: Float) {
        context.settingsDataStore.edit { it[KEY_AUDIO_GAIN] = gain }
    }

    suspend fun setShowLinkThumbnails(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SHOW_LINK_THUMBNAILS] = enabled }
    }

    suspend fun setAutoLoadImages(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_AUTO_LOAD_IMAGES] = enabled }
    }

    suspend fun setLanguage(language: String) {
        context.settingsDataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setEnableFloatingWindow(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_ENABLE_FLOATING_WINDOW] = enabled }
    }

    suspend fun setAnimeBackground(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_ANIME_BACKGROUND] = enabled }
    }

    val noiseSuppression: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_NOISE_SUPPRESSION] ?: true }

    /**
     * Push-to-talk instead of voice activity.
     *
     * Persisted (rather than living in the server screen's view model) because the floating window
     * needs it too: without knowing the mode it cannot offer a talk button that does not fight the
     * voice-activity setting.
     */
    val pushToTalk: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_PUSH_TO_TALK] ?: true }

    suspend fun setPushToTalk(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_PUSH_TO_TALK] = enabled }
    }

    suspend fun setNoiseSuppression(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_NOISE_SUPPRESSION] = enabled }
    }
}

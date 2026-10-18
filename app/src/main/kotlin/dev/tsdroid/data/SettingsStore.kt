package dev.tsdroid.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.tsdroid.bridge.MicMode
import dev.tsdroid.bridge.VadGate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private val KEY_AUDIO_GAIN = floatPreferencesKey("audio_gain")
private val KEY_MIC_GAIN = floatPreferencesKey("mic_gain")
private val KEY_SHOW_LINK_THUMBNAILS = booleanPreferencesKey("show_link_thumbnails")
private val KEY_AUTO_LOAD_IMAGES = booleanPreferencesKey("auto_load_images")
private val KEY_LANGUAGE = stringPreferencesKey("language")
private val KEY_ENABLE_FLOATING_WINDOW = booleanPreferencesKey("enable_floating_window")
private val KEY_ANIME_BACKGROUND = booleanPreferencesKey("anime_background")
private val KEY_NOISE_SUPPRESSION = booleanPreferencesKey("noise_suppression")
// Legacy key (this fork stored a plain PTT boolean before microphone modes
// existed); kept only so pre-2.3 installs migrate to KEY_MIC_MODE.
private val KEY_PUSH_TO_TALK = booleanPreferencesKey("push_to_talk")
private val KEY_PTT_MODE = booleanPreferencesKey("ptt_mode")
private val KEY_MIC_MODE = stringPreferencesKey("mic_mode")
private val KEY_VAD_THRESHOLD_DB = floatPreferencesKey("vad_threshold_db")

class SettingsStore(private val context: Context) {

    val audioGain: Flow<Float> = context.settingsDataStore.data
        .map { it[KEY_AUDIO_GAIN] ?: 1.0f }

    /** Manual mic gain multiplier: 0 = mic off, 4 = +12 dB boost. */
    val micGain: Flow<Float> = context.settingsDataStore.data
        .map { it[KEY_MIC_GAIN] ?: 1.0f }

    val showLinkThumbnails: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_SHOW_LINK_THUMBNAILS] ?: false }

    // Default off: auto-fetching remote images from chat links leaks the
    // user's IP to arbitrary servers; users can opt in
    val autoLoadImages: Flow<Boolean> = context.settingsDataStore.data
        .map { it[KEY_AUTO_LOAD_IMAGES] ?: false }

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

    suspend fun setMicGain(gain: Float) {
        context.settingsDataStore.edit { it[KEY_MIC_GAIN] = gain }
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

    /**
     * Microphone mode. Migrates the legacy boolean: true was push-to-talk;
     * false was an ungated open microphone (the old UI mislabeled it
     * "voice activation", but mapping it to VAD would silence users after
     * upgrade, so it maps to OPEN to keep behavior unchanged).
     *
     * Two legacy keys are honoured: [KEY_PTT_MODE] from the upstream fork and
     * [KEY_PUSH_TO_TALK], which is what this fork stored before microphone
     * modes existed. Both being absent means a fresh install, i.e. PTT.
     */
    val micMode: Flow<MicMode> = context.settingsDataStore.data
        .map { prefs ->
            prefs[KEY_MIC_MODE]?.let { MicMode.from(it) }
                ?: run {
                    val ptt = prefs[KEY_PTT_MODE] ?: prefs[KEY_PUSH_TO_TALK] ?: true
                    if (ptt) MicMode.PTT else MicMode.OPEN
                }
        }

    suspend fun setMicMode(mode: MicMode) {
        context.settingsDataStore.edit { it[KEY_MIC_MODE] = mode.toRaw() }
    }

    /** VAD activation threshold in dBFS. */
    val vadThresholdDb: Flow<Float> = context.settingsDataStore.data
        .map { it[KEY_VAD_THRESHOLD_DB] ?: VadGate.DEFAULT_THRESHOLD_DB }

    suspend fun setVadThresholdDb(db: Float) {
        context.settingsDataStore.edit { it[KEY_VAD_THRESHOLD_DB] = db }
    }
}

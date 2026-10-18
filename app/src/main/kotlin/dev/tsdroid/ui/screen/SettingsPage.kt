package dev.tsdroid.ui.screen

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mikepenz.markdown.m3.Markdown
import coil.compose.AsyncImage
import dev.tsdroid.bridge.IdentityFileStore
import dev.tsdroid.bridge.MicMode
import dev.tsdroid.bridge.VadGate
import dev.tsdroid.data.SettingsStore
import dev.tsdroid.han.R
import dev.tsdroid.ui.component.AnimeWallpaperState
import dev.tsdroid.ui.component.GlassCard
import dev.tsdroid.ui.component.GlassDialog
import dev.tsdroid.ui.component.SettingsCacheSizeHelper
import dev.tsdroid.ui.component.WallpaperCacheManager
import dev.tsdroid.ui.component.WallpaperExporter
import dev.tsdroid.ui.component.rememberGlassBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SettingsPage(
    onNavigateToAbout: () -> Unit,
    autoReconnect: Boolean,
    onAutoReconnectChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val showLinkThumbnails by settingsStore.showLinkThumbnails.collectAsStateWithLifecycle(initialValue = false)
    val autoLoadImages by settingsStore.autoLoadImages.collectAsStateWithLifecycle(initialValue = false)
    // Default matches the store default (enabled) so the switch does not
    // flash off→on on first composition.
    val enableFloatingWindow by settingsStore.enableFloatingWindow.collectAsStateWithLifecycle(initialValue = true)
    // Stays false: v2.1.5 turned the anime background off by default.
    val animeBackground by settingsStore.animeBackground.collectAsStateWithLifecycle(initialValue = false)
    val noiseSuppression by settingsStore.noiseSuppression.collectAsStateWithLifecycle(initialValue = true)
    val audioGain by settingsStore.audioGain.collectAsStateWithLifecycle(initialValue = 1.0f)
    val micGain by settingsStore.micGain.collectAsStateWithLifecycle(initialValue = 1.0f)
    val micMode by settingsStore.micMode.collectAsStateWithLifecycle(initialValue = MicMode.PTT)
    val vadThreshold by settingsStore.vadThresholdDb.collectAsStateWithLifecycle(initialValue = VadGate.DEFAULT_THRESHOLD_DB)

    val languageOptions = listOf(
        "ru" to stringResource(R.string.language_russian),
        "zh" to stringResource(R.string.language_simplified_chinese),
        "en" to stringResource(R.string.language_english),
        "fr" to stringResource(R.string.language_french),
    )
    val selectedLanguageTag by settingsStore.language.collectAsStateWithLifecycle(initialValue = "zh")
    val selectedLanguageLabel = languageOptions.firstOrNull { it.first == selectedLanguageTag }?.second
        ?: stringResource(R.string.language_simplified_chinese)
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var pendingLanguageTag by remember { mutableStateOf<String?>(null) }
    var showLogViewer by remember { mutableStateOf(false) }
    val activity = context as? Activity

    pendingLanguageTag?.let { languageTag ->
        val label = languageOptions.firstOrNull { it.first == languageTag }?.second ?: languageTag
        GlassDialog(
            onDismissRequest = { pendingLanguageTag = null },
            title = stringResource(R.string.language_change_title),
            wallpaper = rememberGlassBackdrop(animeBackground),
            body = {
                Text(
                    text = stringResource(R.string.language_change_message, label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingLanguageTag = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        settingsStore.setLanguage(languageTag)
                        activity?.recreate()
                    }
                    pendingLanguageTag = null
                }) {
                    Text(stringResource(R.string.restart))
                }
            },
        )
    }

    if (showLogViewer) {
        LogViewerDialog(onDismiss = { showLogViewer = false })
    }

    // ── 身份导出/导入 ──
    val identityStore = remember { IdentityFileStore(context) }
    var identityUid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        identityUid = withContext(Dispatchers.IO) { identityStore.readUniqueId() }
    }
    val exportIdentityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            identityStore.exportTo(out)
                        } ?: false
                    } catch (_: Exception) {
                        false
                    }
                }
                Toast.makeText(
                    context,
                    if (ok) R.string.identity_export_success else R.string.identity_export_failed,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
    val importIdentityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            identityStore.importFrom(input)
                        } ?: false
                    } catch (_: Exception) {
                        false
                    }
                }
                if (ok) {
                    identityUid = withContext(Dispatchers.IO) { identityStore.readUniqueId() }
                    Toast.makeText(context, R.string.identity_import_success, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, R.string.identity_import_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // ── 外观 ──
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            wallpaper = rememberGlassBackdrop(animeBackground),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsSectionTitle(stringResource(R.string.section_appearance))

                // 悬浮窗
                SettingsSwitchRow(
                    label = stringResource(R.string.enable_floating_window),
                    checked = enableFloatingWindow,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsStore.setEnableFloatingWindow(enabled) }
                        if (enabled && !canDrawOverlays(context)) {
                            // The window cannot appear without this, and only the system settings can
                            // grant it — so say why, and open that page. Previously the app asked for
                            // it on the way to the background, where the prompt came out of nowhere.
                            Toast.makeText(
                                context,
                                context.getString(R.string.overlay_permission_needed),
                                Toast.LENGTH_LONG,
                            ).show()
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    },
                )

                // 动漫背景
                SettingsSwitchRow(
                    label = stringResource(R.string.anime_background),
                    checked = animeBackground,
                    onCheckedChange = { scope.launch { settingsStore.setAnimeBackground(it) } },
                )

                if (animeBackground) {
                    // 自定义背景
                    CustomBackgroundSection(context, rememberGlassBackdrop(animeBackground))

                    // 壁纸缓存
                    WallpaperCacheSection(context, rememberGlassBackdrop(animeBackground))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 音频 ──
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            wallpaper = rememberGlassBackdrop(animeBackground),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsSectionTitle(stringResource(R.string.section_audio))

                // 扬声器音量增益（0% = 静音）
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    var gainValue by remember(audioGain) { mutableFloatStateOf(audioGain) }
                    Text(
                        text = "${stringResource(R.string.audio_gain)} : ${stringResource(R.string.gain_percent, (gainValue * 100).roundToInt())}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = gainValue,
                        onValueChange = { gainValue = it },
                        onValueChangeFinished = { scope.launch { settingsStore.setAudioGain(gainValue) } },
                        valueRange = 0.0f..8.0f,
                        steps = 31,
                    )
                }

                // 麦克风增益（0% = 关麦；超过 100% 由软限幅防破音）
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    var micGainValue by remember(micGain) { mutableFloatStateOf(micGain) }
                    Text(
                        text = "${stringResource(R.string.mic_gain)} : ${stringResource(R.string.gain_percent, (micGainValue * 100).roundToInt())}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = micGainValue,
                        onValueChange = { micGainValue = it },
                        onValueChangeFinished = { scope.launch { settingsStore.setMicGain(micGainValue) } },
                        valueRange = 0.0f..4.0f,
                        steps = 15,
                    )
                }

                // 麦克风降噪
                SettingsSwitchRow(
                    label = stringResource(R.string.noise_suppression),
                    checked = noiseSuppression,
                    onCheckedChange = { scope.launch { settingsStore.setNoiseSuppression(it) } },
                )

                // ── 麦克风模式：按住说话 / 语音激活 / 开放麦克风 ──
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.mic_mode),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = micMode == MicMode.PTT,
                            onClick = { scope.launch { settingsStore.setMicMode(MicMode.PTT) } },
                            label = { Text(stringResource(R.string.push_to_talk)) },
                        )
                        FilterChip(
                            selected = micMode == MicMode.VAD,
                            onClick = { scope.launch { settingsStore.setMicMode(MicMode.VAD) } },
                            label = { Text(stringResource(R.string.mic_mode_vad)) },
                        )
                        FilterChip(
                            selected = micMode == MicMode.OPEN,
                            onClick = { scope.launch { settingsStore.setMicMode(MicMode.OPEN) } },
                            label = { Text(stringResource(R.string.mic_mode_open)) },
                        )
                    }
                    if (micMode == MicMode.VAD) {
                        var thresholdValue by remember(vadThreshold) { mutableFloatStateOf(vadThreshold) }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.vad_threshold, thresholdValue.roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Slider(
                            value = thresholdValue,
                            onValueChange = { thresholdValue = it },
                            onValueChangeFinished = {
                                scope.launch { settingsStore.setVadThresholdDb(thresholdValue) }
                            },
                            valueRange = -70f..-15f,
                            steps = 54,
                        )
                        Text(
                            text = stringResource(R.string.vad_threshold_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 身份 ──
        // Same frosted surface as the other groups: this card arrived with the
        // identity export/import feature and was still a plain Material Card,
        // which is the one place the settings page did not match.
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            wallpaper = rememberGlassBackdrop(animeBackground),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsSectionTitle(stringResource(R.string.identity_section))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = identityUid?.let { stringResource(R.string.identity_uid, it) }
                            ?: stringResource(R.string.identity_uid_none),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { exportIdentityLauncher.launch("ts6droid_identity.ini") },
                            enabled = identityUid != null,
                        ) {
                            Text(stringResource(R.string.identity_export))
                        }
                        OutlinedButton(onClick = { importIdentityLauncher.launch(arrayOf("*/*")) }) {
                            Text(stringResource(R.string.identity_import))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.identity_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 聊天 ──
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            wallpaper = rememberGlassBackdrop(animeBackground),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsSectionTitle(stringResource(R.string.section_chat))

                SettingsSwitchRow(
                    label = stringResource(R.string.auto_reconnect),
                    checked = autoReconnect,
                    onCheckedChange = onAutoReconnectChange,
                )
                SettingsSwitchRow(
                    label = stringResource(R.string.show_link_thumbnails),
                    checked = showLinkThumbnails,
                    onCheckedChange = { scope.launch { settingsStore.setShowLinkThumbnails(it) } },
                )
                SettingsSwitchRow(
                    label = stringResource(R.string.auto_load_images),
                    checked = autoLoadImages,
                    onCheckedChange = { scope.launch { settingsStore.setAutoLoadImages(it) } },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 更多 ──
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            wallpaper = rememberGlassBackdrop(animeBackground),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsSectionTitle(stringResource(R.string.section_more))

                // 语言切换：就地展开选项，而不是弹 DropdownMenu —— 下拉菜单是独立窗口，背后没有
                // 壁纸可模糊；三个固定选项铺开在卡片上也更好点。
                SettingsClickableRow(
                    label = stringResource(R.string.language_change_title),
                    onClick = { languageMenuExpanded = !languageMenuExpanded },
                    trailing = {
                        Text(
                            text = selectedLanguageLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.72f),
                        )
                    },
                )
                if (languageMenuExpanded) {
                    languageOptions.forEach { (tag, label) ->
                        LanguageOptionRow(
                            label = label,
                            selected = tag == selectedLanguageTag,
                            onClick = {
                                languageMenuExpanded = false
                                pendingLanguageTag = tag
                            },
                        )
                    }
                }

                // 检查更新
                UpdateCheckRow(context, rememberGlassBackdrop(animeBackground))

                // 后台运行：电池优化白名单（避免系统限制后台录音与网络）
                BatteryOptimizationRow(context)

                // 应用详情（自启动 / 后台运行，国产 ROM 专用开关）
                SettingsClickableRow(
                    label = stringResource(R.string.app_details_settings),
                    subtitle = stringResource(R.string.app_details_settings_desc),
                    onClick = { openAppDetailsSettings(context) },
                )

                // 诊断日志 — reads this process's own logcat, so it also covers
                // the native tslib output; the viewer handles refresh itself.
                SettingsClickableRow(
                    label = stringResource(R.string.diag_log_title),
                    subtitle = stringResource(R.string.diag_log_hint),
                    onClick = { showLogViewer = true },
                )

                // 关于软件 — last row of the page, where a footer item belongs.
                SettingsClickableRow(
                    label = stringResource(R.string.about_software),
                    onClick = onNavigateToAbout,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // 版本号
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }
        Text(
            text = "TS6 Droid v$versionName",
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.72f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

// ── 可复用组件 ──

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        // Derive from the glass backdrop instead of assuming a light surface: `primary` on a
        // light wallpaper washed the section labels out until they were barely readable.
        color = LocalContentColor.current.copy(alpha = 0.75f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsClickableRow(
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {
        Icon(
            Icons.AutoMirrored.Filled.NavigateNext,
            null,
            tint = LocalContentColor.current.copy(alpha = 0.6f),
        )
    },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.72f),
                )
            }
        }
        trailing()
    }
}

/**
 * One language choice, unfolded under the "语言切换" row.
 *
 * Drawn as part of the card so it is frosted like everything else on it — a `DropdownMenu` would be
 * a window of its own, with no wallpaper behind it to blur.
 */
@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // Indented one step further than the row it belongs to, so the choice reads as a
            // sub-item rather than as another setting.
            .padding(start = 32.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else LocalContentColor.current.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── 后台运行相关（电池优化 / 应用详情） ──

/** Whether the app may draw over other apps, i.e. whether the floating window can appear at all. */
private fun canDrawOverlays(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val manager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return try {
        manager.isIgnoringBatteryOptimizations(context.packageName)
    } catch (_: Throwable) {
        false
    }
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val direct = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val opened = try {
        context.startActivity(direct)
        true
    } catch (_: Throwable) {
        false
    }
    if (!opened) {
        // Some ROMs refuse the direct dialog; fall back to the exemption list.
        try {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
        }
    }
}

private fun openAppDetailsSettings(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Throwable) {
    }
}

@Composable
private fun BatteryOptimizationRow(context: Context) {
    var ignoring by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Re-read the exemption state when the user comes back from the system page.
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoring = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The state is on the subtitle; the row itself keeps the same trailing chevron as every other
    // navigable row, instead of a second label competing with it.
    SettingsClickableRow(
        label = stringResource(R.string.battery_optimization),
        subtitle = stringResource(
            if (ignoring) R.string.battery_optimization_allowed
            else R.string.battery_optimization_desc
        ),
        onClick = { requestIgnoreBatteryOptimizations(context) },
    )
}

// ── 自定义背景区 ──

@Composable
private fun CustomBackgroundSection(context: Context, wallpaper: ImageBitmap?) {
    val scope = rememberCoroutineScope()
    var showCropScreen by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var hasCustom by remember { mutableStateOf(dev.tsdroid.background.CustomBackgroundManager.hasCustomBackground(context)) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    cropBitmap = bitmap
                    showCropScreen = true
                }
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.custom_bg_load_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showCropScreen && cropBitmap != null) {
        Dialog(
            onDismissRequest = {
                cropBitmap?.recycle()
                cropBitmap = null
                showCropScreen = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            dev.tsdroid.background.CropScreen(
                bitmap = cropBitmap!!,
                onConfirm = { left, top, right, bottom ->
                    val success = dev.tsdroid.background.CustomBackgroundManager.cropAndSave(context, cropBitmap!!, left, top, right, bottom)
                    if (success) {
                        hasCustom = true
                        // Decoding the freshly written file happens off the main thread now, so the
                        // new background appears a frame later instead of stalling this one.
                        scope.launch {
                            dev.tsdroid.ui.component.AnimeWallpaperState.refreshCustomBackground(context)
                        }
                        Toast.makeText(context, context.getString(R.string.custom_bg_saved), Toast.LENGTH_SHORT).show()
                    }
                    cropBitmap?.recycle()
                    cropBitmap = null
                    showCropScreen = false
                },
                onDismiss = {
                    cropBitmap?.recycle()
                    cropBitmap = null
                    showCropScreen = false
                },
            )
        }
    }

    if (showDeleteConfirm) {
        GlassDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = stringResource(R.string.custom_bg_delete),
            wallpaper = wallpaper,
            body = {
                Text(
                    text = stringResource(R.string.custom_bg_delete_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dev.tsdroid.background.CustomBackgroundManager.deleteBackground(context)
                    hasCustom = false
                    showDeleteConfirm = false
                    // Drops the deleted image from memory too: it is no longer on disk, so the state
                    // has to fall back to the online wallpaper instead of painting it until restart.
                    scope.launch {
                        dev.tsdroid.ui.component.AnimeWallpaperState.refreshCustomBackground(context)
                    }
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    Text(
        text = stringResource(R.string.custom_bg_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp),
    )
    Text(
        text = if (hasCustom) stringResource(R.string.custom_bg_active) else stringResource(R.string.custom_bg_inactive),
        style = MaterialTheme.typography.bodySmall,
        color = if (hasCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.custom_bg_upload))
        }
        if (hasCustom) {
            OutlinedButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.custom_bg_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── 壁纸缓存区 ──

@Composable
private fun WallpaperCacheSection(context: Context, wallpaper: ImageBitmap?) {
    val cacheSizeMB = remember { mutableFloatStateOf(WallpaperCacheManager.getCacheSizeMB()) }
    val cacheCount = remember { mutableIntStateOf(WallpaperCacheManager.getCachedFilesCount()) }
    val maxSize = remember { mutableLongStateOf(SettingsCacheSizeHelper.getMaxCacheSize(context)) }
    var showCacheViewer by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        GlassDialog(
            onDismissRequest = { showClearConfirm = false },
            title = stringResource(R.string.wallpaper_clear_cache),
            wallpaper = wallpaper,
            body = {
                Text(
                    text = stringResource(R.string.wallpaper_clear_cache_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    WallpaperCacheManager.clearCache()
                    cacheSizeMB.floatValue = 0f
                    cacheCount.intValue = 0
                    showClearConfirm = false
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (showCacheViewer) {
        val cachedFiles = remember { mutableStateListOf(*WallpaperCacheManager.getCachedFiles().toTypedArray()) }
        val scope = rememberCoroutineScope()

        // Exports report through a toast: the copy itself is silent, and a failure (no space left)
        // would otherwise look exactly like a tap that did nothing.
        fun export(files: List<File>) {
            if (files.isEmpty()) return
            scope.launch {
                val saved = WallpaperExporter.export(context, files)
                val message = if (saved > 0) {
                    context.getString(R.string.wallpaper_export_done, saved)
                } else {
                    context.getString(R.string.wallpaper_export_failed)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        GlassDialog(
            onDismissRequest = { showCacheViewer = false },
            title = "${stringResource(R.string.wallpaper_view_cache)} (${cachedFiles.size})",
            wallpaper = wallpaper,
            body = {
                if (cachedFiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.wallpaper_cache_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        // A grid needs a bounded height inside the dialog's column.
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .heightIn(max = 360.dp),
                    ) {
                        items(cachedFiles.size, key = { cachedFiles[it].name }) { index ->
                            val file = cachedFiles[index]
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    // Tapping an image saves it to the gallery; deleting is the
                                    // explicit ✕, so a thumbnail can no longer be thrown away by a
                                    // stray tap.
                                    .clickable { export(listOf(file)) },
                            ) {
                                AsyncImage(model = file, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Icon(Icons.Default.Download, stringResource(R.string.wallpaper_export),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(22.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), CircleShape).padding(3.dp))
                                Icon(Icons.Default.Close, stringResource(R.string.remove),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), CircleShape).padding(3.dp)
                                        .clickable {
                                            WallpaperCacheManager.deleteFile(file)
                                            cachedFiles.removeAt(index)
                                            cacheSizeMB.floatValue = WallpaperCacheManager.getCacheSizeMB()
                                            cacheCount.intValue = WallpaperCacheManager.getCachedFilesCount()
                                        })
                            }
                        }
                    }
                }
            },
            dismissButton = {
                if (cachedFiles.isNotEmpty()) {
                    TextButton(onClick = { export(cachedFiles.toList()) }) {
                        Text(stringResource(R.string.wallpaper_export_all))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCacheViewer = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    Text(
        text = stringResource(R.string.wallpaper_cache_size, String.format("%.1f", cacheSizeMB.floatValue), cacheCount.intValue),
        style = MaterialTheme.typography.bodySmall,
        color = LocalContentColor.current.copy(alpha = 0.72f),
        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = stringResource(R.string.wallpaper_max_cache, maxSize.longValue),
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.72f),
        )
        Slider(
            value = maxSize.longValue.toFloat(),
            onValueChange = { maxSize.longValue = it.toLong() },
            onValueChangeFinished = { SettingsCacheSizeHelper.setMaxCacheSize(context, maxSize.longValue) },
            valueRange = 10f..500f,
            steps = 48,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = { showCacheViewer = true }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.wallpaper_view_cache))
        }
        OutlinedButton(onClick = { showClearConfirm = true }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.wallpaper_clear_cache), color = MaterialTheme.colorScheme.error)
        }
    }
}

// ── 检查更新 ──

@Composable
private fun UpdateCheckRow(context: Context, wallpaper: ImageBitmap?) {
    val scope = rememberCoroutineScope()
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (_: Exception) { "" }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<dev.tsdroid.update.UpdateInfo?>(null) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isLatestVersion by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    if (showUpdateDialog && updateInfo != null) {
        GlassDialog(
            onDismissRequest = { if (!isDownloading) { showUpdateDialog = false } },
            title = stringResource(R.string.update_available, updateInfo!!.versionName),
            wallpaper = wallpaper,
            body = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (isDownloading) {
                        if (downloadProgress >= 0f) {
                            Text("${stringResource(R.string.update_downloading)} ${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth())
                        } else {
                            // Total size unknown (chunked transfer) — indeterminate progress
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.update_downloading), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        downloadError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        val changelog = updateInfo!!.changelog
                        if (changelog.isBlank()) {
                            Text(
                                text = stringResource(R.string.update_no_changelog),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            Markdown(
                                content = changelog.take(4000),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text(stringResource(R.string.update_later)) }
            },
            confirmButton = {
                if (!isDownloading) {
                    FilledTonalButton(onClick = {
                        isDownloading = true
                        downloadError = null
                        downloadProgress = 0f
                        scope.launch {
                            dev.tsdroid.update.InAppUpdater.downloadAndInstall(
                                context = context,
                                downloadUrl = updateInfo!!.downloadUrl,
                                onProgress = { progress ->
                                    downloadProgress = progress.progress
                                    if (progress.state == dev.tsdroid.update.InAppUpdater.DownloadState.FAILED) {
                                        downloadError = progress.error
                                        isDownloading = false
                                    } else if (progress.state == dev.tsdroid.update.InAppUpdater.DownloadState.DONE) {
                                        showUpdateDialog = false
                                    }
                                }
                            )
                            isDownloading = false
                        }
                    }) { Text(stringResource(R.string.update_download)) }
                }
            },
        )
    }

    SettingsClickableRow(
        label = stringResource(R.string.update_check),
        onClick = {
            if (!isCheckingUpdate) {
                isCheckingUpdate = true; isLatestVersion = false; updateInfo = null; updateError = null
                scope.launch {
                    val result = dev.tsdroid.update.UpdateChecker.checkForUpdate(versionName)
                    updateInfo = result.update
                    updateError = result.error
                    if (result.update != null) showUpdateDialog = true
                    else if (result.error == null) isLatestVersion = true
                    isCheckingUpdate = false
                }
            }
        },
        trailing = {
            when {
                isCheckingUpdate -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                updateInfo != null -> Text(stringResource(R.string.update_found), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                updateError != null -> Text(updateError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                isLatestVersion -> Text(stringResource(R.string.update_already_latest), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {}
            }
        },
    )
}

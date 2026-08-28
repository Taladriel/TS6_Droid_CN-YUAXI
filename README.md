TS6 Droid Simplified Chinese version

This is a free, lightweight TeamSpeak 3/6 Android client, built with Jetpack Compose, driven at the underlying layer by a 'tslib' written in Rust.

---

## Project Demo

<div align="center">
<table>
  <tr>
    <td align="center"><img src="img/screenshow1.jpg" width="240"/></td>
    <td align="center"><img src="img/screenshow2.jpg" width="240"/></td>
  </tr>
  <tr>
    <td align="center"><img src="img/screenshow3.jpg" width="240"/></td>
    <td align="center"><img src="img/screenshow4.jpg" width="240"/></td>
  </tr>
</table>
</div>

---

## Changelog

### v2.1.4-Han (2026-08-18)

**Bug Fixes**
- **Nickname length validation**: Added a minimum nickname length check when connecting to a server; the nickname must be at least 3 characters (PR #4 from IMito-iron)
- Extracted a `getValidatedConnectionInput()` method to unify input validation before connecting
- Added a new error message: "Nickname must be at least 3 characters" (supports Chinese/English/French)

### v2.1.3-Han (2026-07-28)

**New Features**
- **TS3 Spacer channel rendering**: Parses `[cspacer]`, `[lspacer]`, `[rspacer]`, `[*spacer]` tags, and the channel list correctly displays separators and decorative text (PR #3 from XuVIIJay)

### v2.1.2-Han (2026-07-15)

**New Features**
- **Custom background**: Supports uploading an image from the gallery as a background, with crop preview (two-finger zoom + one-finger drag)
- Full-screen preview in the crop interface, rule-of-thirds grid lines, drag handles in four corners
- Settings page adds custom background management (upload/delete)
- Custom background takes effect immediately after saving, no restart required

**UI Refactor**
- Settings page regrouped into a card-style layout: Appearance, Audio, Chat, More
- The anime background toggle expands a sub-area (custom background + wallpaper cache)
- Language switching and About are placed in the "More" group
- Version number is displayed at the bottom of the settings page
- Settings page cards are semi-transparent and do not obscure the anime background

**Bug Fixes**
- Fixed the crop feature being unusable (the crop box could only be moved but not resized)
- Fixed the issue where a custom background only took effect after a restart

### v2.1.0-Han (2026-06-27)

**New Features**
- **In-app update**: After tapping update check, the APK can be downloaded and installed directly in the app, no need to open a browser
- Download progress bar shows the percentage in real time, and the system install screen automatically pops up when the download is complete
- Added a "Downloading" state; the dialog cannot be closed during download, and an error message is shown on failure

**Bug Fixes**
- Fixed the issue where version detection could not recognize a new version: the `-Han` suffix is removed before comparing version numbers
- Fixed the issue where an API request failure was mistakenly judged as "Already up to date"; now a specific error message is shown
- Fixed a crash when a Release had no APK uploaded; now it falls back to the Releases page

**Build Signing**
- Added a unified signing file `release.keystore`; both debug and release builds use the same signature
- For multi-computer collaboration, simply copy `release.keystore` to the project root directory

### v2.1.0-Han (2026-06-27)

**In-app Update**
- In-app APK download and installation: download the new version APK directly in the update dialog, show a real-time progress bar, and automatically invoke the system installer after download completes
- Added FileProvider support, so Android 8+ devices can install APKs downloaded in the app normally
- Unified build signing across multiple computers: use the unified `release.keystore` signing file in the project to ensure APKs built on different computers have the same signature and can be directly installed over the previous version
- Added the `REQUEST_INSTALL_PACKAGES` permission declaration

**Fixes**
- Fixed the issue where the volume gain slider adjustment did not take effect; added a Flow observer to sync to the audio bridge in real time
- Microphone noise suppression implementation verification: uses Android's native NoiseSuppressor API and adds logging to help troubleshoot device compatibility
- Changed the logo background color from blue to pink (#2962FF → #FF69B4)
- Fixed the issue where in-app version detection could not detect a new version: the `-Han` suffix is removed before version comparison, and an error message is shown on API failure instead of incorrectly showing "Already up to date"
- Fixed the issue where the GitHub API being unavailable in mainland China network conditions caused "Already up to date" to be shown persistently

**New Features**
- In-app version detection: tapping the version number on the settings page queries the latest GitHub Release and shows an update dialog
- Shows a specific error message on network errors, and shows "Already up to date" when there is no update
- Automatically falls back to the GitHub Releases page when a Release has no APK uploaded

### v2.0.5-Han (2026-06-27)

**Fixes**
- Fixed the issue where the volume gain slider adjustment did not take effect; added a Flow observer to sync to the audio bridge in real time
- Microphone noise suppression implementation verification: uses Android's native NoiseSuppressor API and adds logging to help troubleshoot device compatibility
- Changed the logo background color from blue to pink (#2962FF → #FF69B4)
- Fixed the issue where in-app version detection could not detect a new version: the `-Han` suffix is removed before version comparison, and an error message is shown on API failure instead of incorrectly showing "Already up to date"
- Fixed the issue where the GitHub API being unavailable in mainland China network conditions caused "Already up to date" to be shown persistently

**New Features**
- In-app version detection: tapping the version number on the settings page queries the latest GitHub Release and shows an update dialog
- Shows a specific error message on network errors, and shows "Already up to date" when there is no update
- Automatically falls back to the GitHub Releases page when a Release has no APK uploaded

### v2.0.1-Han (2026-06-26)

**Compose Performance Optimization**
- Migrated 54 Flow collections across the project from `collectAsState` to `collectAsStateWithLifecycle`, automatically pausing UI collection when the app goes to the background, reducing CPU usage and battery consumption
- Migrated the background image fade-in animation from `Modifier.alpha()` to `Modifier.graphicsLayer {}`, so animation frames skip the Composition phase and reduce frame drops
- Added stable `key`s to the cached wallpaper grid to prevent the scroll position from jumping back when adding or deleting wallpapers

**Bug Fixes**
- Fixed the issue where viewing the wallpaper cache did nothing; tapping now opens a thumbnail grid dialog
- Added a confirmation dialog before clearing the wallpaper cache
- The volume gain slider on the settings page can now be adjusted normally
- Settings page switches no longer jump or flicker when switching pages
- The file manager now supports in-app full-screen preview when tapping an image file

### v2.0.0-Han (2026-06-26)

**Material3 UI Comprehensive Refactor**
- Adopted Google Material Design 3 specifications and completely refactored colors, typography, and component styles
- Dynamic Color (Android 12+), with theme colors automatically extracted from the wallpaper image to generate a complete color scheme
- 15-level typography system, Shape corner radius tokens aligned with M3 standards
- All components (buttons, input fields, cards, dialogs, bottom bar) unified in M3 style

**Splash Screen and Theme Adaptation**
- Added a SplashScreen launch screen that displays the brand logo during loading
- After the wallpaper image is downloaded, the dominant color is automatically extracted and the theme colors adapt in real time
- 3-second timeout protection: when the network is abnormal, a wallpaper is randomly selected from the cache as the background

**Bottom Navigation Bar + Settings Page**
- Added a bottom navigation bar to the home page (Home + Settings) with page switching support
- Language switching, auto-reconnect, volume gain, floating window, anime background, microphone noise suppression, and About are all integrated into the settings page
- The server no longer shows a settings dialog, making the interface cleaner

**Wallpaper Cache System**
- Wallpaper images are automatically cached locally and the cache is used first at startup
- Configurable maximum cache capacity (10MB - 500MB slider adjustment)
- View a grid of cached wallpaper thumbnails, with support for deleting a single image
- Clearing the cache has a confirmation dialog
- The above settings are only available when "I'm an otaku" is enabled

**Animated Background Optimization**
- Wallpaper switching no longer flickers: cache mechanism + 600ms fade-in animation
- Switching pages no longer triggers re-fetching; the same wallpaper is shared globally
- Empty home page list is centered with "No connections"

**File Manager Image Preview**
- Tapping an image file now opens a full-screen preview in the app instead of showing an external open dialog

**Bug Fixes**
- Fixed a crash caused by Config#HARDWARE bitmap being unable to call getPixel
- Fixed settings page switches jumping and flickering when switching pages
- Fixed a compilation error caused by leftover SettingsDialog code
- Fixed the gray background color issue caused by the window background color
- Unified all components to use M3 color tokens

---

## Localization and Enhancement Features

1. **Simplified Chinese localization**: 100% complete Simplified Chinese translation of all text (`zh-rCN`).
2. **Language switching**: Supports one-tap switching among Chinese, English, and Français, without changing the phone's system language.
3. **Built-in core voice driver**: All-architecture core binary libraries (jniLibs) are built in directly, ready to use out of the box.
4. **CI/CD deep optimization**: Adapted for the AndroidX/Jetifier compatibility environment and optimized the Gradle JVM memory limit.

---

## Multi-computer Build Signing Instructions

This project uses a unified `release.keystore` signing file to ensure that APKs built on all computers have the same signature and do not report signature conflicts when installed over previous versions.

- The signing file is located in the project root directory: `release.keystore`
- Password/alias: `ts6droid`
- This file is excluded by `.gitignore` and will not be committed to GitHub
- For multi-computer collaboration, copy `release.keystore` to the project root directory on the other computers

### Generate a new signing file

If you need to replace the signature (for example, for an official release), run the following in the project root directory:

```bash
keytool -genkey -v -keystore release.keystore -alias ts6droid -keyalg RSA -keysize 2048 -validity 10000
```

---

## How to Perform Cloud Builds (GitHub Actions)

1. **Fork this repository** to your own GitHub account.
2. Go to the repository page, click the **Actions** tab at the top, and click the green button to activate Actions.
3. Every code push or manually triggered workflow will cause GitHub to build automatically.
4. After the build is complete, download `app-debug.apk` in the **Assets** section.

---

## Technical Architecture and Configuration

For technical details such as the underlying Rust architecture and local build environment setup, please refer to the original author's repository:

[flamme-demon/TS6_Droid](https://github.com/flamme-demon/TS6_Droid)

## Open Source License

This project is licensed under the GNU GPLv3 open source license. See the [LICENSE](LICENSE) file for details.

---

## Contributors

Thanks to all developers who have contributed to this project!

<a href="https://github.com/YUAXI/TS6_Droid_CN/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=YUAXI/TS6_Droid_CN&v=20260729" />
</a>

<div align="center">

<img src="docs/icon/neru_logo.png" alt="Neru Logo" width="140" style="border-radius: 28px;" />

# Neru · ねる

**The smart, privacy-first sleep & inactivity timer for Android.**

[![Version](https://img.shields.io/badge/version-v0.0.1-78B5EB.svg?style=flat-square)](https://github.com/kainotch/Neru/releases)
[![Package](https://img.shields.io/badge/package-com.notch.neru-173A5B.svg?style=flat-square)](https://github.com/kainotch/Neru)
[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-green.svg?style=flat-square)](https://developer.android.com)
[![Privacy](https://img.shields.io/badge/network-100%25%20Offline-brightgreen.svg?style=flat-square)](#-privacy--security)

<br/>

*Named after the Japanese word **ねる** (neru – "to sleep"), Neru is an open-source sleep timer engineered to detect when you drift off and peacefully wind down your device.*

</div>

---

## ✨ Highlights

* **😴 Smart Inactivity Watchdog Mode**  
  Falling asleep while scrolling Instagram Reels, TikTok, or watching YouTube? Neru detects touch and scroll events globally across all apps. Every time you interact, the timer quietly resets so it never cuts off your entertainment while you're still awake. When you stop interacting, it counts down to shut off.

* **🔊 Smart 30-Second Audio Fade & Wake-Up Recovery**  
  During the final 30 seconds, Neru gently fades out system media volume so sleep isn't startled by a sudden silence. If you hear the volume dipping and tap or scroll your screen, Neru immediately **cancels the fade, restores your original volume level**, and resets the timer back to full duration.

* **⏱️ Precision Fixed Timer**  
  Prefer a strict schedule? Switch to Fixed Deadline Mode. Choose instant presets (**5 min**, **10 min**, **30 min**, **45 min**) or adjust smoothly using the circular dial and `−` / `+` step buttons.

* **🌙 OLED Dark Slate Design**  
  Crafted specifically for nightstand viewing. Pure minimalist dark slate and electric sky-blue accents (`#0C0F14` / `#161A22` / `#78B5EB`) ensure zero eye strain in dark bedrooms.

* **🔒 Biometric-Friendly Screen Lock**  
  * **Device Admin Lock**: Standard Android screen lock.
  * **Shizuku Lock (Soft)**: Simulates a power button press via Shizuku (biometric unlock stays active).

* **🔇 Universal Media Playback Pause**  
  Gracefully pauses music, podcasts, audiobooks, and videos across Spotify, YouTube, Apple Music, browsers, and background players.

* **📶 Automated Radios Off (Optional via Shizuku)**  
  Optionally turn off Wi-Fi, Bluetooth, or mobile data when the timer completes to save battery and reduce overnight radiation.

* **⚡ Quick Settings & Homescreen Widgets**  
  Start your bedtime routine in a single tap directly from your Quick Settings tiles or homescreen widgets.

---

## 🛡️ Privacy & Security

Neru is built from the ground up on strict privacy principles:

* **100% Offline**: The app does **not** declare the `android.permission.INTERNET` permission. It is physically impossible for the app to connect to the internet, transmit data, or leak information.
* **No Trackers or Analytics**: Zero telemetry, no Firebase, no crash loggers, and no advertisements.
* **Local Storage**: All preferences are saved exclusively on your device using Jetpack DataStore.

---

## 📱 Permissions Breakdown

| Permission | Purpose |
|---|---|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keeps the countdown and audio controller reliably active in the background without being terminated by OS battery optimization. |
| `POST_NOTIFICATIONS` | Displays the active countdown and quick notification actions (`+step`, `−step`, `Cancel`). |
| `PACKAGE_USAGE_STATS` *(Usage Access)* | Detects whether selected target apps (Spotify, Instagram, YouTube, etc.) are open on screen so the timer runs or pauses accordingly. |
| `BIND_DEVICE_ADMIN` *(Optional)* | Enables standard screen lock when screen off is enabled. |
| `Shizuku` *(Optional)* | Allows soft screen lock and toggling Wi-Fi / Bluetooth / Mobile Data on Android 10+. Completely optional. |

---

## 🛠️ Tech Stack & Architecture

Neru is engineered using modern Android development practices:

* **Language**: [Kotlin](https://kotlinlang.org/) (Coroutines, StateFlow)
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
* **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
* **Persistence**: Jetpack DataStore (Preferences)
* **Architecture**: Clean Architecture + MVI/MVVM with unidirectional data flow
* **Background Processing**: Android Foreground Service + Accessibility Service + AIDL Shizuku IPC

---

## 🚀 Building from Source

### Prerequisites
* JDK 17 (or Android Studio JBR)
* Android SDK (API 36 `compileSdk`, API 26 `minSdk`)

### Build Commands

Clone the repository:
```bash
git clone https://github.com/kainotch/Neru.git
cd Neru
```

Build the debug APK:
```bash
./gradlew assembleDebug
```

Run unit tests:
```bash
./gradlew testDebugUnitTest
```

The compiled APK will be output to `app/build/outputs/apk/debug/app-debug.apk`.

---

## 👤 Author & Community

Built with care by **Notch**:

* **GitHub**: [@kainotch](https://github.com/kainotch)
* **Repository**: [github.com/kainotch/Neru](https://github.com/kainotch/Neru)
* **Instagram**: [@kainotch](https://instagram.com/kainotch)

Contributions, feature requests, and bug reports are welcome via [GitHub Issues](https://github.com/kainotch/Neru/issues)!

---

## 📄 License

```text
Neru — The smart, privacy-first sleep & inactivity timer for Android
Copyright (C) 2026 Notch

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```

See [LICENSE](LICENSE) for the full text.

# Netify (project folder/package still named NetPulse internally)

Android network speed & Wi-Fi info app — plain single-module Jetpack Compose
app. Display name and app icon are "Netify"; the Gradle module names,
package (`com.netpulse.android`), and repo folder are still
`NetPulse`/`netpulse` under the hood. Renaming those too is just a
find-and-replace across the project if you want full consistency later.

## Stack

- **Kotlin (Android only)** + **Jetpack Compose** for the UI.
- **Ktor Client (OkHttp engine)** for the real download/upload/ping speed
  test — same technique as the HTML prototype, now running natively.
- **WifiManager / ConnectivityManager** for network info (SSID, signal,
  online status).
- **GitHub Actions** — `.github/workflows/build-android.yml` builds a debug
  APK on every push using `gradle/actions/setup-gradle`, no committed
  `gradlew` wrapper required. Grab the APK from the Actions tab → latest
  run → Artifacts.

## Project layout

```
NetPulse/
├── androidApp/               # the whole app — one module, no multiplatform
│   └── src/main/kotlin/com/netpulse/android/
│       ├── MainActivity.kt
│       └── core/              # SpeedTestEngine, NetworkInfoProvider, Models
└── .github/workflows/         # CI
```

## Why this is simpler than the earlier version

An earlier version of this scaffold used Kotlin Multiplatform + Compose
Multiplatform to target Android and iOS from one codebase. Since the
current goal is Android-only, that's been removed — it was also the
source of the last build failure (`org.jetbrains.kotlin.plugin.compose`
only exists for Kotlin 2.0+, and this project pins Kotlin 1.9.24 for
stability). This version uses plain Android Gradle + standard Jetpack
Compose, which has far fewer moving parts and version-compatibility
traps. If you want iOS back later, that's a separate, deliberate step —
not a checkbox to flip.

## Wi-Fi password — still the same hard limit

No Android API, rooted or not, hands a saved network's password to an app
directly. `NetworkInfoProvider` reads the **connected** SSID/signal/IP via
`WifiManager`, but there is no `password` field anywhere in this codebase
on purpose. The only real paths for the password itself are root shell
access or the user's own QR-code export (Settings → Wi-Fi → network →
Share, Android 10+).

## Getting started

1. Open the `NetPulse/` folder in Android Studio (Hedgehog+) and let it
   sync Gradle.
2. Run the `androidApp` configuration on an emulator/device.
3. Push to GitHub — CI builds the APK automatically.

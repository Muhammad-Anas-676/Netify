# Netify — Android WiFi & Speed Test App

Pure Kotlin + Jetpack Compose. **No Python anywhere in the app or build
pipeline** — Gradle/Kotlin builds it end to end. (Python was used only once,
outside this project, to resize the provided icon PNG into the launcher
icon sizes Android needs — a one-time image-prep step, not app code.)

## What's implemented

| Feature | Where | Notes |
|---|---|---|
| Real speed test (down/up/ping/jitter) | `domain/SpeedTestEngine.kt` | Multi-stream transfers against Cloudflare's public speed-test endpoints. Real network calls, not simulated. |
| Live WiFi info (SSID, signal, band, channel, link speed) | `data/repository/WifiRepository.kt` | Uses `WifiManager`. Needs `ACCESS_FINE_LOCATION` granted at runtime (an OS requirement on API 27+, not this app's choice). |
| Channel congestion analyzer | `domain/ChannelAnalyzer.kt` | Reads nearby AP scan results and recommends a quieter 2.4 GHz channel. |
| Local device scanner | `domain/DeviceScanner.kt` | Best-effort subnet reachability sweep — see limitations below. |
| Speed test history | `data/local/` (Room) | Local SQLite via Room. |
| Password vault | `data/local/vault/VaultStorage.kt` | `EncryptedSharedPreferences` (AES-256, Android Keystore-backed). Separate from, and not able to read, the OS's own saved WiFi passwords. |
| QR generate / scan | `domain/QrHelper.kt`, `ui/vault/QrScanScreen.kt` | Standard `WIFI:S:...;T:...;P:...;;` format. Scanning uses CameraX + ML Kit; connecting uses the Android 10+ `WifiNetworkSuggestion` API. |
| Scheduled auto-test + alert | `worker/ScheduledTestWorker.kt` | WorkManager, self-rescheduling daily at a chosen hour. |
| Home screen widget | `widget/NetifyWidgetProvider.kt` | Classic `RemoteViews` widget showing the last result. |
| Settings (theme/units/language/ISP plan) | `data/datastore/SettingsDataStore.kt` | Jetpack DataStore. **Theme defaults to Light on first launch**, then always remembers the user's last choice, exactly as requested. |

## Two Android platform limits worth knowing up front

These are OS restrictions, not gaps in this app:

1. **Reading passwords already saved in the phone's WiFi settings.** No
   third-party app can do this since Android 10 (a deliberate privacy
   change). Netify's Vault is its own separate, encrypted store for
   networks *you add or scan into Netify* — it can't read what's already
   saved elsewhere on the phone.
2. **Listing every connected device with vendor/MAC info.** That needs the
   router's ARP table, which requires root. Netify's scanner does a
   best-effort reachability sweep of the subnet instead (same approach
   used by most non-root "network scanner" apps) — it will find most
   active devices but can miss ones that block ping or are asleep, and
   won't show manufacturer names.

## Building it

**Option A — GitHub Actions (recommended, matches what you asked for):**
Push this repo to GitHub and the included workflow
(`.github/workflows/android-build.yml`) builds a debug APK automatically
and attaches it to the run as a downloadable artifact. No local Android
Studio needed.

**Option B — Android Studio:**
Open the folder as a project. There is no `gradlew` checked in (kept the
zip small); Android Studio will offer to generate the wrapper on first
sync — accept that, then Run.

## Being upfront about "100% error-free"

Every file here was written by hand against the real, current Android/
Kotlin APIs, and the logic in each feature is real (not placeholder) — but
this was written without a local Android SDK/emulator to compile against,
so I can't personally guarantee a zero-error first build. The GitHub
Actions workflow will actually compile it and tell you exactly where, if
anywhere, something needs a small fix (version mismatch, a renamed API,
etc.) — send me that build log and I'll fix it directly.

## Suggested next steps

- Run the GitHub Actions build and share the log if it fails.
- Test the QR scan/connect flow on a real device (the system "approve this
  network" prompt on Android 10+ can't be simulated in an emulator
  reliably).
- Swap the Cloudflare test endpoints for your own server later if you want
  a branded "Netify Node" experience instead.

// Standard Android + Kotlin plugins only — no Kotlin Multiplatform, no
// Compose Multiplatform. Jetpack Compose (Android-only) uses the classic
// composeOptions { kotlinCompilerExtensionVersion } approach, which is
// simpler and doesn't have the Kotlin-2.0-only plugin dependency that
// caused the previous build failure.
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

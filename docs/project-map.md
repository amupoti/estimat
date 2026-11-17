# Project Map

## Purpose

Estimat is an offline-first Android trivia game where every question has a
numeric answer and the closest guess wins. It was created from the
`android-app-template` repository. The package/application id is
`com.marcel.estimat`.

## Stack

- Android application module: `app`
- Kotlin, Jetpack Compose, Material 3
- Gradle Kotlin DSL with version catalog in `gradle/libs.versions.toml`
- JDK 17, compile/target SDK 36, min SDK 26
- AndroidX WorkManager for daily reminders
- JUnit unit tests under `app/src/test`

## Build And Test

- Build debug APK: `./build-apk.sh`
- Unit tests: `./gradlew test`
- Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK publishing is handled by `.github/workflows/release-apk.yml` for
  tags matching `v*`.
- Start a new app: `scripts/new-app.sh --package <id> --name "<Name>" --repo <owner/name>`

## App-Specific Configuration

- `app/build.gradle.kts`: `namespace` and `applicationId`.
- `settings.gradle.kts`: `rootProject.name`.
- `gradle.properties`: `appGithubRepository`, exposed as
  `BuildConfig.GITHUB_REPOSITORY` for the update checker. Release builds
  override it with the repository running the workflow.
- `res/values*/strings.xml`: `app_name` and all UI text.
- `res/drawable/app_icon.xml` and the colors in `MainActivity.kt` /
  `res/values/themes.xml`.
- `SettingsScreen.kt`: `changelogEntries`.

## Main Structure

- `app/src/main/java/com/marcel/estimat/MainActivity.kt` is the Compose
  entry point, theme setup, navigation shell, and notification permission handoff.
- `app/src/main/java/com/marcel/estimat/ui/AppViewModel.kt`
  coordinates reminder settings and theme preference state.
- `app/src/main/java/com/marcel/estimat/data/SettingsRepository.kt`
  stores reminder settings and theme preference in `SharedPreferences`.
- `app/src/main/java/com/marcel/estimat/SettingsScreen.kt` contains
  appearance, reminders, changelog, and update/install UI.
- `app/src/main/java/com/marcel/estimat/ReminderScheduler.kt` schedules
  two daily reminder notifications with WorkManager.
- `app/src/main/java/com/marcel/estimat/AppUpdateChecker.kt` and
  `ApkUpdateInstaller.kt` implement the GitHub Releases update check and APK
  install flow.
- `app/src/main/java/com/marcel/estimat/model/` contains reminder and
  theme preference model code.
- `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ca/strings.xml`
  hold localized strings.
- `docs/backlog.md` tracks future ideas and known issues.

## Main Features

- Home tab preserved as a reusable starting point.
- Settings tab with light, dark, and system theme preference.
- Reminder scheduling with two configurable reminder times.
- Settings update check and APK install flow backed by GitHub Releases.

## Notes For Future Changes

- Keep edits targeted; this is a small single-module Android app.
- Prefer updating model tests when changing `model/`.
- Run `./gradlew test` after logic changes.
- Update this map when adding a major feature, module, package, or new build
  command.

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

- `app/src/main/assets/questions/` holds the bundled question bank
  (`questions.json`) and tag taxonomy (`tags.json`).
- `questions/README.md` documents the question format and the editorial
  process; `questions/validate.py` validates the bank.
- `app/src/main/java/com/marcel/estimat/MainActivity.kt` is the Compose entry point, theme setup, the
  per-tab top app bar, and the four-tab navigation (Play, Sessions, Filters,
  Settings).
- `app/src/main/java/com/marcel/estimat/QuestionScreen.kt` is the Play tab: a compact header (active
  session, progress, active-filter summary), then the question, the answer,
  and the next one. Tapping the filter summary expands every filter inline
  (`QuickFilters` in `FiltersScreen.kt`), so players can switch question types
  between rounds without leaving the game.
- `app/src/main/java/com/marcel/estimat/FiltersScreen.kt` selects audience, tag filters, and "only verified".
- `app/src/main/java/com/marcel/estimat/ui/AppViewModel.kt` holds the game state (bank, filters, played
  questions, current question) plus the theme preference.
- `app/src/main/java/com/marcel/estimat/data/QuestionRepository.kt` loads the bank from assets;
  `data/QuestionParser.kt` parses the JSON.
- `app/src/main/java/com/marcel/estimat/data/SettingsRepository.kt` stores settings, filters, question
  language, and game sessions (name, creation time, played question ids, active
  session, last played time) in `SharedPreferences`. The pre-sessions `played_questions` key is
  migrated into a first session on launch.
- `app/src/main/java/com/marcel/estimat/model/GameSession.kt` holds `GameSession` and the immutable
  `GameSessions` collection (add, select, rename, remove, update active).
- `app/src/main/java/com/marcel/estimat/SessionsScreen.kt` is the Sessions tab: session cards with
  play, rename, reset played questions, and delete, plus a "new session" button.
- `app/src/main/java/com/marcel/estimat/model/` contains the question model, `QuestionFilter` (OR inside a
  tag group, AND across groups), `QuestionDeck` (no repeats), and theme
  preference code.
- `app/src/main/java/com/marcel/estimat/SettingsScreen.kt` contains question language, appearance,
  changelog, and update/install UI.
- `app/src/main/java/com/marcel/estimat/AppUpdateChecker.kt` and
  `ApkUpdateInstaller.kt` come from the app template.
- `app/src/main/res/values*/strings.xml` hold English, Catalan, and Spanish
  strings.
- `docs/backlog.md` tracks future ideas and known issues.

## Main Features

- Play tab: opens straight on a random question in Catalan or Spanish, answer
  hidden until revealed, then answer with unit, explanation, reference year,
  and source. Skipping is a secondary text button.
- Game sessions: each session (e.g. one group of friends) remembers its own
  played questions, so the group can play over several days without repeats.
  A new session starts with every question available. The Sessions tab
  creates, switches, renames, resets, or deletes sessions and shows when each
  was last played; the Play tab header shows the active one. Questions are marked as played
  as soon as they are drawn, until the player starts over for the current
  filters (which only affects the active session).
- Filters tab: match count (fixed at the top), clear button in the top bar,
  audience (everyone hides +18, kids shows only `nens`, +18 shows all), scope
  (Catalunya, Espanya, Europa, Món), difficulty, tone, and topics.
- Delete question: a button under each question asks for confirmation, then
  hides that question on this device in every session (ids stored in
  `SettingsRepository` as `deleted_questions`; the bundled bank is untouched)
  and draws the next one.
- Filters tab also holds "only verified questions" (off by default).
- Settings: question language, theme, changelog, and GitHub Releases update/install flow.
- Works fully offline.

## Notes For Future Changes

- Keep edits targeted; this is a small single-module Android app.
- Prefer updating model tests when changing `model/`.
- Run `python3 questions/validate.py` after editing the question bank;
  `QuestionParserTest` also parses the bundled bank.
- Run `./gradlew test` after logic changes.
- Update this map when adding a major feature, module, package, or new build
  command.

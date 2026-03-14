# Flit - Android

Android app for Flit: voice notes, transcription, and a local knowledge graph with relationships and categories.

## Features

- Voice note recorder and transcription (on-device)
- Knowledge graph with relationships and categorization
- Semantic embedding for search
- Append, edit, delete notes
- Export knowledge graph
- Sync with Flit Core (backend)

## Prerequisites

- Android Studio (or compatible IDE) with Android SDK
- JDK 11+
- For device/emulator: minSdk 31, targetSdk 36

## Build and run

```bash
# Debug (install on connected device or emulator)
./gradlew installDebug

# Assemble debug APK only
./gradlew assembleDebug
```

Release builds use environment variables for signing (see below). **Do not commit signing secrets (keystore paths, passwords, aliases) to the repository.**

## Tests

```bash
# Unit tests (JVM)
./gradlew test

# Instrumented tests (device/emulator)
./gradlew connectedAndroidTest
```

## Architecture

The project uses a layered structure: **data** (Room, API, sync) → **domain** (errors, repository interfaces) → **ui** (Compose screens and ViewModels), with Hilt for dependency injection. For a concise overview for contributors and agents, see [AGENTS.md](AGENTS.md).

## Building a signed release

To produce a signed release APK, set these environment variables before running `./gradlew assembleRelease` (or use Android Studio's release build):

- `RELEASE_STORE_FILE` – path to the keystore (optional; if unset, defaults to `$HOME/android_keystores/flit-release.keystore`)
- `RELEASE_STORE_PASSWORD` – keystore password
- `RELEASE_KEY_ALIAS` – key alias
- `RELEASE_KEY_PASSWORD` – key password

If any of these are missing or the keystore file does not exist, the release build is unsigned (and may not be installable on some devices). Do not add signing credentials to the project or commit them; use env vars or a secure CI secret store.

Remove any release signing override in **File → Project Structure → Modules → app → Signing Configs** so the build uses this Gradle config.

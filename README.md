# Ekho Explorer

A community biodiversity observation app for Android: record sightings with a photo, location, and timestamp, browse them on a map and feed, and confirm species identifications by voting.

## Features (MVP)
1. Account creation and sign-in
2. Species search (biodiversity API)
3. Observation creation: photo, GPS location, timestamp
4. Firebase persistence
5. Interactive observation map (OpenStreetMap)
6. Community observation feed
7. Upvote / downvote for species confirmation
8. Two external API integrations

## Requirements
Everyone must match these versions.

| Tool | Version |
|---|---|
| Android Studio | Quail 4 / 2026.1.4 Patch 1 (build AI-261.26222.65.2614.16379836) |
| Android Gradle Plugin | 9.4.1 |
| Gradle (wrapper) | 9.6.0 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Java language level (app code) | 11 |
| JDK that runs Gradle | 25 (pinned in `gradle/gradle-daemon-jvm.properties`) |
| minSdk / targetSdk / compileSdk | 24 / 37 / 37 |
| Package / applicationId | com.comp350sec001group2.ekhoexplorer |
| Test emulator | Pixel-class image with Google Play services, API 24 or newer |

### Keeping everyone on the same platform
- **Do not accept Android Studio prompts to upgrade AGP, Gradle, or Kotlin.** Upgrades are a deliberate pull request from the build owner.
- Version numbers live in `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties`. Only the build owner edits them.
- Use the same Android Studio version listed above, or a newer one in the same release line.
- Leave Android Studio's Gradle JDK setting at its default. Gradle uses the JDK pinned in `gradle/gradle-daemon-jvm.properties`.
- Always build through the committed Gradle wrapper (`./gradlew`), not a separately installed Gradle.

## Quick start
1. Clone the repo and check out `develop`.
2. Follow [docs/SETUP.md](docs/SETUP.md) to add your private files (`google-services.json`).
3. Open the project folder in Android Studio and let Gradle sync.
4. Run on an emulator or device.

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md).

## Map data
Map data © OpenStreetMap contributors.

## License
No license. All rights reserved.

## Team
- Ethan Hook
- Drew Wishengrad
- Alejandra Salazar
- Alex Walker
- Anthony Rodriguez

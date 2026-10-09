# Developer setup

## 1. Prerequisites
- Android Studio at the version listed in the README (do not upgrade AGP/Gradle when prompted).
- Android SDK platform and an emulator image **with Google Play services**.
- Added as a collaborator on the team Firebase project (ask the project owner).

## 2. Private files (never committed)
| File | Where | How to get it |
|---|---|---|
| `google-services.json` | `app/` | Ask the project owner via private channel, or download from Firebase Console > Project settings > Your apps |
| `local.properties` | project root | Generated automatically by Android Studio |

## 3. Build and run
1. Open the project folder (the one containing `settings.gradle.kts`).
2. Wait for Gradle sync.
3. Run the `app` configuration.

## 4. Emulator notes
- Location: Extended Controls (...) > Location > set a point.
- Camera: Extended Controls > Camera, or use a virtual scene.

## 5. External APIs
| API | Purpose | Key needed? |
|---|---|---|
| TBD (e.g. GBIF / iNaturalist) | Species search | TBD |
| TBD (e.g. Wikipedia / Open-Meteo) | Species details / weather | TBD |

## Troubleshooting
- Sync fails on `google-services.json`: file missing or in the wrong folder (`app/`).
- Map is blank: check internet access on the emulator and that the osmdroid/MapLibre user agent is set.

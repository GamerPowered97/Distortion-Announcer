# Distortion Announcer (Distortion Tracker)

An Android application designed to track the hourly active and upcoming Distortion Zones in Destiny 2 and schedule alarms to notify you when a specific destination becomes active.

---

## Features

- **Hourly Tracking:** Displays the current active Distortion Zone and the next scheduled location based on hourly rotation cycles.
- **Support for All Destinations:** Cycle includes Dreaming City, Savathun's Throne World, Moon, Europa, Nessus, Cosmodrome, and EDZ.
- **Accurate Calibration:** Calibrate the rotation cycles to align perfectly with in-game servers.
- **Custom Notifications:** Schedule alarms for when a target Distortion Zone becomes active.
- **Warning System:** Enable an optional 5-minute pre-activation warning alarm.

---

## Downloading the App (APK)

Instead of compiling the project yourself or using a USB cable, you can download the fully optimized release version of the app directly onto your phone:

1. Go to the [Releases](https://github.com/GamerPowered97/Distortion-Announcer/releases/tag/latest) page of this repository.
2. Under the **Assets** section, tap the **`app-release.apk`** file to download it directly.
3. Once downloaded, open your file manager, tap the `.apk` file, and proceed with the installation. *(If prompted, enable installation from "Unknown Sources" in your browser/file manager settings).*

---

## Development & Automation

This repository is configured with a automated CI/CD pipeline using **GitHub Actions**:
- Every push to the `main` or `master` branch triggers the build job.
- The pipeline compiles a fully optimized release build (`./gradlew assembleRelease`).
- The raw compiled **`app-release.apk`** is automatically uploaded directly to the [Latest Build Release](https://github.com/GamerPowered97/Distortion-Announcer/releases/tag/latest), replacing the previous version so that the link always contains the newest version of the app.

---

## Tech Stack

- **Framework:** Jetpack Compose (Kotlin)
- **Architecture:** MVVM Pattern
- **Android APIs:** AlarmManager (for precise alarm scheduling), NotificationManager
- **Build System:** Gradle Kotlin DSL (with Version Catalog)

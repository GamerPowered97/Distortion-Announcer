# Distortion Announcer

A simple Android app to track active Destiny 2 Distortion Zones. It displays the current active zone, what's coming up next, and lets you set alarms (with a 5-minute warning) so you don't miss them.

## Installing the App
Go to the [Releases](https://github.com/GamerPowered97/Distortion-Announcer/releases/latest) page on your phone and download `app-release.apk` directly to install it.

## How it works
- **Jetpack Compose (Kotlin):** Powers the MVVM-structured UI.
- **AlarmManager:** Handles scheduling the background notifications.
- **GitHub Actions:** Automatically builds the optimized release APK and updates the GitHub release every time code is pushed.

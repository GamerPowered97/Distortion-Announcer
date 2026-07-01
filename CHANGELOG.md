# Changelog

All notable changes to the Destiny Distortion Tracker will be documented in this file.

## [9.8.0.0] - 2026-07-01
### Added
- **Dynamic Themes**: Implemented responsive visual themes in the Distortion Tracker that dynamically adjust based on active tracking states, using prototype-matched colors and gradients.
- **24h Timeline**: Integrated a comprehensive 24-hour timeline display tracking future distortion events.
- **Info Cards**: Introduced details cards to present status information and tracking metrics cleanly.
- **Custom Alerts**: Added customizable alert parameters for notifications and custom warning thresholds.
- **Rift Borders**: Added crawling red electric border effects wrapping the Timer and Up Next UI cards.
- **High-Fidelity Anomaly Ring**: Upgraded the central planetary distortion animation with high-definition fractal pathing, breathing background halo overlays, and orbiting spark particles.

---

## [9.7.0.3] - 2026-07-01
### Added
- **Battery Optimization Onboarding**: Added background check at startup prompting users to disable battery restrictions (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).
- **Device-Specific Instructions**: Added footnotes with setup guidelines for Samsung, Xiaomi, and OnePlus auto-start/unrestricted background configurations.
- **Shared Signing Keystore**: Embedded a custom, repository-level debug keystore to sign both debug and release configurations, resolving Android package/signature conflict installation errors during upgrades.
- **Automatic Multi-Alarm Scheduling**: Setting a 20-minute alert now automatically schedules the 5-minute warning alert as well.

### Changed
- **UI Contrast Enhancements**: All text fields on the `CalibrationScreen` are colored black for optimal contrast against the bright traveler space background.
- **Notification & Dialog Phrasing**:
  - Updated notification titles and alert phrasing to sound more authentic and urgent.
  - Corrected grammar in the in-app calibration guide helper text and 5-minute warning pop-up.
- **Animation Optimizations**: Upgraded keyframe easing calls from deprecated `.with()` to the modern `.using()` infix function, resolving all Kotlin compiler warnings.

---

## [1.0.0] - 2026-06-30
### Added
- First release of the Destiny Distortion Tracker.
- Implemented tracking for active Destiny 2 Distortion Zones (Dreaming City, Savathûn's Throne World, Moon, Europa, Nessus, Cosmodrome, EDZ).
- Integrated AlarmManager for background scheduling of notifications.
- Configured automated GitHub Actions workflows for release APK compilation.

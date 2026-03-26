# Contributing to MangaForge

Thanks for your interest in contributing to MangaForge!

Looking to report a bug or make a feature request? Please use [GitHub Issues](https://github.com/crimznexus/MangaForge/issues).

---

## Code contributions

Pull requests are welcome!

If you're interested in taking on [an open issue](https://github.com/crimznexus/MangaForge/issues),
please comment on it so others are aware. You do not need to ask for permission or assignment.

### Prerequisites

Before you start, please note that the ability to use the following technologies is **required**.
Existing contributors will not actively teach them.

- Basic [Android development](https://developer.android.com/)
- [Kotlin](https://kotlinlang.org/)

### Tools

- [Android Studio](https://developer.android.com/studio) (Meerkat or newer recommended)
- Emulator or Android device with developer options enabled

### Getting started

1. Fork the repository and clone your fork
2. Open the project in Android Studio
3. Build and run: `./gradlew :app:installDebug`
4. Make your changes on a new branch
5. Open a pull request against `main`

### Code style

- Follow existing patterns in the file you are editing
- Keep changes focused — one concern per pull request

---

## Forks

Forks are allowed so long as they abide by the [Apache License 2.0](LICENSE).

When creating a fork, remember to:

- Avoid confusion with MangaForge:
  - Change the app name and icon
  - Change or disable the [app update checker](app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateChecker.kt)
- Avoid installation conflicts:
  - Change the `applicationId` in [`app/build.gradle.kts`](app/build.gradle.kts)
- Replace any analytics/crash-reporting keys with your own

---

## Questions

Open an issue on [GitHub](https://github.com/crimznexus/MangaForge/issues) for any questions about the project.

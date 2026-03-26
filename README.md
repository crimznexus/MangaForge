<div align="center">

<img src="./.github/assets/logo.svg" alt="MangaForge logo" title="MangaForge logo" width="100"/>

# MangaForge

**A modern Android manga & webtoon reader**

Built with Kotlin + Jetpack Compose · Based on [Mihon](https://github.com/mihonapp/mihon)

[![GitHub release](https://img.shields.io/github/v/release/crimznexus/MangaForge?style=flat-square)](https://github.com/crimznexus/MangaForge/releases/latest)
[![License](https://img.shields.io/github/license/crimznexus/MangaForge?style=flat-square)](LICENSE)
[![Issues](https://img.shields.io/github/issues/crimznexus/MangaForge?style=flat-square)](https://github.com/crimznexus/MangaForge/issues)

> Under active development — features may be incomplete and behavior can change frequently.

</div>

---

## Features

- Browse, read, and manage manga & webtoons from extension-based sources
- Multiple reader modes: left-to-right, right-to-left, vertical webtoon
- Library management with categories and filters
- Chapter download for offline reading
- Tracking integration (AniList, MyAnimeList, and more)
- Extension repository support to install community sources
- Material You dynamic theming + AMOLED mode

## Download

Get the latest APK from the [Releases page](https://github.com/crimznexus/MangaForge/releases/latest).

Choose the APK matching your device:

| File | Architecture |
|------|-------------|
| `mangaforge-*-arm64-v8a.apk` | Most modern Android phones (recommended) |
| `mangaforge-*-armeabi-v7a.apk` | Older 32-bit ARM devices |
| `mangaforge-*-x86_64.apk` | x86_64 emulators / Chromebooks |
| `mangaforge-*-universal.apk` | All devices (largest file) |

> Enable **Install unknown apps** in Android settings before sideloading.

## Build from source

### Prerequisites

- JDK 17
- Android SDK (`compileSdk = 36`)
- Android Studio Meerkat or newer (recommended)

### Commands

```bash
# Debug build
./gradlew :app:assembleDebug

# Install directly to connected device
./gradlew :app:installDebug
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before opening a pull request.

Report bugs or request features via [GitHub Issues](https://github.com/crimznexus/MangaForge/issues).

## License

MangaForge is released under the [Apache License 2.0](LICENSE).

MangaForge is a fork of [Mihon](https://github.com/mihonapp/mihon), which is also licensed under Apache 2.0. All original copyright notices are retained.

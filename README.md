<div align="center">

<img src="./.github/assets/logo.svg" alt="MangaForge" width="120"/>

# MangaForge

**A modern Android manga & webtoon reader**

Built with Kotlin · Jetpack Compose · Material You

[![Release](https://img.shields.io/github/v/release/crimznexus/MangaForge?style=flat-square&logo=github&color=7B2FBE)](https://github.com/crimznexus/MangaForge/releases/latest)
[![License](https://img.shields.io/github/license/crimznexus/MangaForge?style=flat-square&color=3A0075)](LICENSE)
[![Issues](https://img.shields.io/github/issues/crimznexus/MangaForge?style=flat-square&color=CC44FF)](https://github.com/crimznexus/MangaForge/issues)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green?style=flat-square&logo=android)](https://github.com/crimznexus/MangaForge/releases/latest)

> Under active development — features may be incomplete and behavior may change between releases.

</div>

---

## Overview

MangaForge is a free, open-source manga and webtoon reader for Android, built on top of [Mihon](https://github.com/mihonapp/mihon). It extends the Mihon experience with a redesigned **Discover** screen powered by AniList — giving readers a curated, visually rich way to find new manga and manhwa to read.

---

## Features

### Discover
- Curated hero carousel with featured, trending, popular, newly released, and top-rated shelves
- Separate **Manga** (JP) and **Manhwa** (KR) tabs with swipe navigation
- Detailed manga pages with synopsis, genres, stats, and "You Might Also Like" recommendations
- Filter by genre, status, format, and year
- One-tap search across all installed extensions

### Reader
- Multiple reading modes: Left-to-Right, Right-to-Left, Vertical Webtoon
- Chapter download for offline reading
- Continuous scroll and page-by-page modes

### Library
- Organize manga into custom categories
- Filter and sort by read status, score, and more
- Background update checks for new chapters

### Extensions
- Community-sourced extension repository support
- Browse and install sources from any compatible repository

### Customization
- Material You dynamic color theming
- OLED pitch-black dark mode (default)
- Per-source reader settings

---

## Download

Get the latest release from the [**Releases page**](https://github.com/crimznexus/MangaForge/releases/latest).

Select the APK that matches your device architecture:

| APK | Target |
|-----|--------|
| `app-arm64-v8a-debug.apk` | Modern Android phones — **recommended** |
| `app-armeabi-v7a-debug.apk` | Older 32-bit ARM devices |
| `app-x86_64-debug.apk` | x86_64 emulators / Chromebooks |
| `app-universal-debug.apk` | All architectures (largest file) |

> **Note:** Enable **Install unknown apps** in Android Settings before sideloading.

---

## Building from Source

### Requirements

| Tool | Version |
|------|---------|
| JDK | 17 |
| Android SDK | compileSdk 36 |
| Android Studio | Meerkat or newer |

### Steps

```bash
# Clone the repository
git clone https://github.com/crimznexus/MangaForge.git
cd MangaForge

# Debug build
./gradlew :app:assembleDebug

# Install directly to a connected device
./gradlew :app:installDebug
```

> On Windows, use `gradlew.bat` instead of `./gradlew`.

---

## Roadmap

- [ ] Persistent library sync with AniList / MyAnimeList
- [ ] In-app extension browser & installer
- [ ] Functional Settings screen (appearance, reader, downloads)
- [ ] Updates & Downloads tabs
- [ ] CI/CD pipeline for automated release builds

---

## Contributing

Contributions are welcome. Please review [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before opening a pull request.

Bug reports and feature requests go through [GitHub Issues](https://github.com/crimznexus/MangaForge/issues).

---

## License

MangaForge is released under the [Apache License 2.0](LICENSE).

This project is a fork of [Mihon](https://github.com/mihonapp/mihon), also licensed under Apache 2.0. All original copyright notices are retained in accordance with the license terms.

---

<div align="center">

Made with ❤️ for manga readers

</div>

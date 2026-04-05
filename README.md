<div align="center">

<img src="./.github/assets/logo.svg" alt="MangaForge" width="120"/>

# MangaForge

**The modern Android reader for manga, manhwa, and light novels**

Built with Kotlin · Jetpack Compose · Material You

[![Release](https://img.shields.io/github/v/release/crimznexus/MangaForge?style=flat-square&logo=github&color=7B2FBE)](https://github.com/crimznexus/MangaForge/releases/latest)
[![License](https://img.shields.io/github/license/crimznexus/MangaForge?style=flat-square&color=3A0075)](LICENSE)
[![Issues](https://img.shields.io/github/issues/crimznexus/MangaForge?style=flat-square&color=CC44FF)](https://github.com/crimznexus/MangaForge/issues)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green?style=flat-square&logo=android)](https://github.com/crimznexus/MangaForge/releases/latest)

</div>

---

## Overview

MangaForge is a free, open-source manga and webtoon reader for Android, built on top of [Mihon](https://github.com/mihonapp/mihon). It extends the Mihon foundation with a fully redesigned visual identity, a curated **Discover** experience powered by AniList, and a growing ecosystem of reader-focused features — all wrapped in a dark, cohesive Material You interface.

---

## Features

### Discover
- Curated hero carousel with featured, trending, popular, newly released, and top-rated shelves
- Separate **Manga** (JP) and **Manhwa** (KR) tabs with swipe navigation
- Detailed series pages with synopsis, genres, stats, and *You Might Also Like* recommendations
- Filter by genre, status, format, and year
- One-tap search across all installed extensions

### Reader
- Left-to-Right, Right-to-Left, Vertical, and Webtoon reading modes
- Chapter download for fully offline reading
- Continuous scroll and page-by-page modes
- Per-source reader preferences

### Library
- Organize series into custom categories
- Filter and sort by read status, score, update date, and more
- Background update checks for new chapters

### Extensions
- Community-sourced extension repositories — add any compatible repo URL
- Browse, install, and update sources directly inside the app
- Manga, manhwa, and manhua sources from a global extension ecosystem

### Themes
- Three hand-tuned brand themes: **Forge Cyan**, **Forge Navy**, **Forge Violet**
- Deep OLED dark surfaces with vivid accent colors
- Consistent gradient design language across all screens

---

## Download

Get the latest release from the [**Releases page**](https://github.com/crimznexus/MangaForge/releases/latest).

Select the APK for your device:

| APK | Target |
|-----|--------|
| `app-arm64-v8a` | Modern Android phones — **recommended** |
| `app-armeabi-v7a` | Older 32-bit ARM devices |
| `app-x86_64` | x86_64 emulators / Chromebooks |
| `app-universal` | All architectures (largest file) |

> **Sideloading:** Enable **Install unknown apps** for your browser or file manager in Android Settings → Apps before installing.

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
git clone https://github.com/crimznexus/MangaForge.git
cd MangaForge

# Debug build
./gradlew :app:assembleDebug

# Install directly to a connected device
./gradlew :app:installDebug
```

> On Windows, use `gradlew.bat` instead of `./gradlew`.

---

## Upcoming

| Feature | Status |
|---------|--------|
| **Light Novel support** — JavaScript plugin system (LNReader-compatible repos), plugin browser, and a dedicated text reader | In development |
| Persistent library sync with AniList / MyAnimeList | Planned |
| Novel library tab with reading progress tracking | Planned |
| In-app extension update notifications | Planned |
| CI/CD pipeline for automated signed release builds | Planned |

---

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before opening a pull request.

Bug reports and feature requests go through [GitHub Issues](https://github.com/crimznexus/MangaForge/issues).

---

## License

MangaForge is released under the [Apache License 2.0](LICENSE).

This project is a fork of [Mihon](https://github.com/mihonapp/mihon), also licensed under Apache 2.0. All original copyright notices are retained in accordance with the license terms.

---

<div align="center">

Made for readers, by readers

</div>

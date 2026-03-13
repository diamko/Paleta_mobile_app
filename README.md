# Paleta Mobile

<p align="right">
  <strong>Language:</strong>
  English |
  <a href="README.ru.md">Русский</a>
</p>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Status](https://img.shields.io/badge/status-active-success.svg)](#)

<p align="center">
  <a href="https://diamko.ru">
    <img src="https://img.shields.io/badge/Website-diamko.ru-ff6a00?style=for-the-badge&logo=googlechrome&logoColor=white" alt="diamko.ru website">
  </a>
</p>

<p align="center">
  <strong>Visit my website:</strong> <a href="https://diamko.ru">diamko.ru</a>
</p>

Paleta Mobile is an Android companion app for [Paleta](https://github.com/diamko/Paleta) — a tool for generating, editing, saving, and exporting color palettes.
Upload an image to extract dominant colors, generate random palettes, fine-tune them with a built-in color wheel and pipette, and export to design-ready formats.

The app is aimed at designers, mobile developers, and anyone who works with colors and needs a fast, on-the-go workflow from image to HEX codes.

## Table of Contents

1. [Why Paleta Mobile](#why-paleta-mobile)
2. [Key Features](#key-features)
3. [Tech Stack](#tech-stack)
4. [How It Works](#how-it-works)
5. [Installation and Setup](#installation-and-setup)
6. [Run the Project](#run-the-project)
7. [Configuration](#configuration)
8. [Usage Guide](#usage-guide)
9. [Project Structure](#project-structure)
10. [Related Projects](#related-projects)
11. [Contributing](#contributing)
12. [Authors](#authors)
13. [License](#license)

## Why Paleta Mobile

### Goal

To bring the full Paleta palette workflow to Android — generate, edit, save, and export color palettes from your phone.

### Problem It Solves

- Desktop-only palette tools are inconvenient when you need to capture colors from a photo on the spot.
- Transferring palettes between devices often requires extra steps.
- Most mobile color tools lack export to professional formats (GPL, ASE, ACO).

### What Makes It Different

- Two generation modes: from image (with on-image pipette) and random.
- Built-in color wheel, RGB sliders, and color harmonies.
- Export to 6 formats: JSON, GPL, ASE, CSV, PNG, ACO.
- Guest mode — works without registration for quick tasks.
- Syncs with the Paleta backend for palette storage and recent uploads.

## Key Features

- Generate palette from image with dominant color extraction (server-side KMeans).
- Random palette generation with harmony modes (complementary, triadic, analogous, split-complementary).
- On-image pipette: drag markers on the uploaded image to pick exact pixel colors.
- Color wheel picker with brightness slider for precise color adjustments.
- RGB channel sliders for fine control.
- Export palettes: `JSON`, `GPL`, `ASE`, `CSV`, `PNG`, `ACO`.
- User authentication (register, login, password reset via email).
- Personal palette library with search, filters, and sorting.
- Recent image uploads (last 7 days) for quick reuse.
- Guest mode — full generation, editing, and export without login.
- Dark/light/system theme support.
- Bilingual interface: Russian and English.

## Tech Stack

- `Kotlin 2.0+`
- `Jetpack Compose` (Material 3)
- `Single-module MVVM` architecture
- `Retrofit 2` + `OkHttp` for networking
- `Kotlin Serialization` for JSON
- `DataStore Preferences` for local settings
- `Kotlin Coroutines` for async operations
- `Navigation Compose` for screen routing
- `Android SDK 35` (minSdk 26)

## How It Works

1. User uploads an image or generates a random palette.
2. For images: the backend runs KMeans clustering and returns dominant colors.
3. User edits colors via the color wheel, RGB sliders, or on-image pipette.
4. The palette can be exported to a file or saved to the user's account.
5. Saved palettes are available in "My Palettes" with search, filters, and sorting.

## Installation and Setup

### Prerequisites

- `git`
- `JDK 17` (required by AGP)
- `Android SDK` (API level 35)
- Android Studio (recommended) or Gradle CLI

### 1) Clone repository

```bash
git clone https://github.com/diamko/Paleta_mobile_app.git
cd Paleta_mobile_app
```

### 2) Install JDK 17 (if not installed)

Ubuntu/Debian:

```bash
sudo apt install openjdk-17-jdk
```

macOS (Homebrew):

```bash
brew install openjdk@17
```

### 3) Configure Android SDK path

Create `local.properties` in the project root:

```properties
sdk.dir=/path/to/your/Android/Sdk
```

### 4) Accept SDK licenses (if not done)

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

## Run the Project

### Option A: Android Studio

1. Open the project folder in Android Studio.
2. Wait for Gradle sync to complete.
3. Select the `app` configuration and click **Run**.

### Option B: Command line

```bash
chmod +x gradlew
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Configuration

### Repository mode

By default, the app uses `FakeRepository` (offline, no backend required).

To connect to a real Paleta backend, edit `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "REPOSITORY_MODE", "\"remote\"")
buildConfigField("String", "API_BASE_URL", "\"http://your-server:5000/\"")
```

### Theme

The app supports light, dark, and system themes. Change it in Settings.

### Language

Russian and English are supported. Change it in Settings — the app will restart to apply the new locale.

## Usage Guide

### Guest mode (without account)

You can:

- generate palettes from images and random,
- edit colors with the color wheel, sliders, and pipette,
- export palettes to all supported formats.

### Authenticated mode

You also get:

- saving palettes to your personal library,
- rename and delete palettes,
- search, filter, and sort in "My Palettes",
- recent image uploads for quick reuse.

## Project Structure

```text
Paleta_mobile_app/
├── app/
│   └── src/main/java/ru/diamko/paleta/
│       ├── core/           # DI, networking, palette logic, storage
│       ├── data/           # API clients, DTOs, repository implementations
│       ├── domain/         # Models, repository interfaces, use cases
│       ├── ui/
│       │   ├── auth/       # Login, register, password screens
│       │   ├── components/ # Reusable UI components
│       │   ├── navigation/ # NavHost, routes
│       │   ├── palettes/   # Palette list, editor, generator screens
│       │   ├── settings/   # Settings, FAQ, profile screens
│       │   └── theme/      # Colors, typography, theme
│       └── MainActivity.kt
├── gradle/
├── build.gradle.kts
├── LICENCE
├── README.md
└── README.ru.md
```

## Related Projects

- [Paleta](https://github.com/diamko/Paleta) — the web application and backend that powers this mobile app.

## Contributing

Contributions are welcome.

1. Fork the repo.
2. Create a branch: `git checkout -b feature/your-feature-name`.
3. Commit changes: `git commit -m "Add: your feature"`.
4. Push branch: `git push origin feature/your-feature-name`.
5. Open a Pull Request with a clear description and test steps.

## Authors

- Diana Konanerova
- Yuliya Tyurina

## License

This project is licensed under the MIT License.

See: [`LICENCE`](LICENCE)

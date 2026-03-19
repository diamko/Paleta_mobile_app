# Paleta Mobile

<p align="right">
  🌍 <strong>Language:</strong>
  🇬🇧 English |
  🇷🇺 <a href="README.ru.md">Русский</a>
</p>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/Android-API_26+-34A853.svg)](https://developer.android.com/)
[![Status](https://img.shields.io/badge/status-active-success.svg)](#)

<p align="center">
  <a href="https://diamko.ru">
    <img src="https://img.shields.io/badge/Website-diamko.ru-ff6a00?style=for-the-badge&logo=googlechrome&logoColor=white" alt="diamko.ru website">
  </a>
</p>

Paleta Mobile is an Android companion app for [Paleta](https://github.com/diamko/Paleta_web_app) — a tool for generating, editing, saving, and exporting color palettes.

Upload an image to extract dominant colors, generate random palettes, fine-tune them with a built-in color wheel and pipette, and export to design-ready formats — all from your phone.

## Table of Contents

1. [Why Paleta Mobile](#why-paleta-mobile)
2. [Key Features](#key-features)
3. [Tech Stack](#tech-stack)
4. [Architecture](#architecture)
5. [How It Works](#how-it-works)
6. [Installation and Setup](#installation-and-setup)
7. [Run the Project](#run-the-project)
8. [Configuration](#configuration)
9. [Usage Guide](#usage-guide)
10. [Project Structure](#project-structure)
11. [Related Projects](#related-projects)
12. [Contributing](#contributing)
13. [Authors](#authors)
14. [License](#license)

## Why Paleta Mobile

### Goal

Bring the full Paleta palette workflow to Android — generate, edit, save, and export color palettes directly from your phone.

### Problem It Solves

- Desktop-only palette tools are inconvenient when you need to capture colors from a photo on the spot.
- Transferring palettes between devices requires extra steps.
- Most mobile color tools lack export to professional formats (GPL, ASE, ACO).

### What Makes It Different

- **Two generation modes**: from image (with on-image pipette) and random.
- **Built-in color wheel** with brightness slider, HEX input, Copy HEX, and 9 color harmony modes.
- **Export to 6 formats**: JSON, GPL, ASE, CSV, PNG, ACO.
- **Guest mode** — full generation, editing, and export without registration.
- **Offline support** — saved palettes are cached locally and synced when connection is restored.

## Key Features

| Category | Features |
|---|---|
| **Generation** | Palette from image (KMeans on server), random palette with harmony modes |
| **Editing** | HSV color wheel with brightness slider, on-image pipette with magnifier, HEX input, Copy HEX, HSV gradient between first and last color |
| **Harmonies** | Analogous, complementary, triadic, square, split-complementary, monochromatic, sequential, tetradic, random |
| **Export** | JSON, GPL, ASE, CSV, PNG, ACO |
| **Library** | Personal palette library with search, filtering by color count, and sorting (name, date, color count) |
| **Auth** | Register, login, password reset via email, profile editing, password change |
| **Offline** | Room database cache with WorkManager background sync |
| **UI** | Dark / light / system theme, Russian and English interface |

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0+ |
| UI | Jetpack Compose (Material 3) |
| Architecture | Single-module MVVM + Clean Architecture layers |
| Networking | Retrofit 2 + OkHttp + Kotlin Serialization |
| Local storage | Room (SQLite) + DataStore Preferences |
| Background sync | WorkManager |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| Target | Android SDK 35 (minSdk 26, Android 8.0+) |

## Architecture

The project follows a single-module **Clean Architecture** layout with three layers:

```
core/       → DI container, networking, palette algorithms, local storage interfaces
data/       → Retrofit API clients, DTOs, Room database, repository implementations, sync worker
domain/     → Domain models, repository interfaces, use cases
ui/         → Compose screens, ViewModels, theme, navigation
```

Data flow: **UI → ViewModel → UseCase → Repository → API / Room**

## How It Works

1. User uploads an image or generates a random palette.
2. For images: the backend runs KMeans clustering and returns dominant colors.
3. User edits colors via the color wheel, HEX input, or on-image pipette.
4. Colors can be harmonized using 9 harmony modes (analogous, triadic, random, etc.) or blended into an HSV gradient.
5. The palette can be exported to a file or saved to the user's account.
6. Saved palettes are cached locally (Room) and synced with the server in the background.

## Installation and Setup

### Prerequisites

- `git`
- `JDK 17` (required by Android Gradle Plugin)
- `Android SDK` (API level 35)
- Android Studio (recommended) or Gradle CLI

### 1) Clone the repository

```bash
git clone https://github.com/diamko/Paleta_mobile_app.git
cd Paleta_mobile_app
```

### 2) Install JDK 17 (if not installed)

Ubuntu / Debian:

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
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Configuration

### Backend connection

By default, the app connects to the remote Paleta backend. The base URL and repository mode are configured in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "REPOSITORY_MODE", "\"remote\"")
buildConfigField("String", "API_BASE_URL", "\"https://diamko.ru/\"")
```

To develop offline without a backend, switch to the built-in fake repository:

```kotlin
buildConfigField("String", "REPOSITORY_MODE", "\"fake\"")
```

### Theme

The app supports light, dark, and system themes. Change it in **Settings**.

### Language

Russian and English are supported. Change it in **Settings** — the app will restart to apply the new locale.

## Usage Guide

### Guest mode (without account)

Available:

- generate palettes from images and random
- edit colors with the color wheel, HEX input, and pipette
- apply color harmonies
- export palettes to all supported formats

### Authenticated mode

Additionally available:

- save palettes to your personal library
- rename and delete palettes
- search, filter, and sort in "My Palettes"
- reuse recent image uploads (last 7 days)
- change password and edit profile

## Project Structure

```
Paleta_mobile_app/
├── app/
│   ├── build.gradle.kts              # App-level build config
│   └── src/main/
│       ├── java/ru/diamko/paleta/
│       │   ├── MainActivity.kt       # Entry point
│       │   ├── PaletaApplication.kt  # App init, DI container
│       │   ├── core/
│       │   │   ├── di/               # AppContainer (dependency injection)
│       │   │   ├── network/          # Retrofit, OkHttp, auth interceptor
│       │   │   ├── palette/          # Color tools, harmonies, export, extraction
│       │   │   ├── storage/          # DataStore interfaces and implementations
│       │   │   └── validation/       # Auth field validation
│       │   ├── data/
│       │   │   ├── local/            # Room database, DAO, entities
│       │   │   ├── remote/           # Retrofit API interfaces, DTOs
│       │   │   ├── repository/       # Repository implementations (Remote, Offline, Fake)
│       │   │   └── sync/             # WorkManager palette sync
│       │   ├── domain/
│       │   │   ├── model/            # Domain models (Palette, User, etc.)
│       │   │   ├── repository/       # Repository interfaces
│       │   │   └── usecase/          # Business logic use cases
│       │   └── ui/
│       │       ├── auth/             # Login, register, forgot/reset password
│       │       ├── components/       # Reusable UI: color wheel, buttons, cards
│       │       ├── navigation/       # NavHost, route definitions
│       │       ├── palettes/         # Palette list, editor, generator
│       │       ├── settings/         # Settings, FAQ, profile, password change
│       │       └── theme/            # Material 3 colors, typography, theme
│       └── res/
│           ├── values/               # Default strings (English)
│           ├── values-ru/            # Russian strings
│           └── values-en/            # English strings (explicit)
├── gradle/
├── build.gradle.kts                  # Root build config
├── LICENCE
├── README.md
├── README.ru.md
├── CONTRIBUTING.md
└── CONTRIBUTING.ru.md
```

## Related Projects

- [Paleta](https://github.com/diamko/Paleta_web_app) — the web application and Flask backend that powers this mobile app.

## Contributing

Contributions are welcome. Please read the full guidelines:

- [`CONTRIBUTING.md`](CONTRIBUTING.md) (English)
- [`CONTRIBUTING.ru.md`](CONTRIBUTING.ru.md) (Russian)

Quick start:

1. Fork the repo.
2. Create a branch: `git checkout -b feature/your-feature-name`.
3. Commit changes: `git commit -m "Add: your feature"`.
4. Push branch: `git push origin feature/your-feature-name`.
5. Open a Pull Request with a clear description and test steps.

## Authors

- Diana Konanerova
- Yuliya Tyurina

## License

This project is licensed under the MIT License — see [`LICENCE`](LICENCE).

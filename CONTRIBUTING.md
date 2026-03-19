# Contributing to Paleta Mobile

<p align="right">
  🌍 <strong>Language:</strong>
  🇬🇧 English |
  🇷🇺 <a href="CONTRIBUTING.ru.md">Русский</a>
</p>

Thanks for your interest in contributing to Paleta Mobile! We want to make every contribution transparent and effortless.

Please note that we have a Code of Conduct and expect respectful communication in all project interactions.

## Ways to Contribute

- Report a bug
- Discuss the current state of the code
- Submit a fix
- Propose new features
- Become a maintainer

## Repositories

Paleta consists of two repositories:

| Repository | Description |
| --- | --- |
| [Paleta](https://github.com/diamko/Paleta_web_app) | Web application and Flask backend (Python) |
| [Paleta Mobile](https://github.com/diamko/Paleta_mobile_app) | Android app (Kotlin, Jetpack Compose) |

Contributions are welcome to both.

## GitHub Flow

All code changes happen through Pull Requests:

1. Fork the repository and create your branch from `main`.
2. If you added code that should be tested — add tests.
3. If you changed APIs or behavior — update the relevant README.
4. Ensure the build passes: `./gradlew assembleDebug`.
5. Open a Pull Request with a clear description and test steps.

## Reporting Bugs

We use GitHub Issues to track bugs. Open a new issue and include:

- A quick summary and context.
- Steps to reproduce (Android version, device/emulator).
- What you expected vs. what actually happened.
- Relevant screenshots or logcat output if available.

## Coding Style

### Kotlin

- Follow standard [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use Jetpack Compose best practices (stateless composables, state hoisting).
- Add a module-level header comment to new `.kt` files (see existing files for the pattern).
- Keep composables small and focused; extract reusable pieces to `ui/components/`.

### Strings

- All user-visible strings must go into `res/values/strings.xml`, `values-ru/strings.xml`, and `values-en/strings.xml`.
- Do not hardcode text in Compose code.

## Building

Requirements: JDK 17, Android SDK (API 35).

```bash
chmod +x gradlew
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

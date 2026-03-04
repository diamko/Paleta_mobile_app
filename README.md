# Paleta Android (MVP scaffold)

Отдельный Android-проект для мобильной части Paleta.

## Технический стек
- Kotlin 2.0+
- Jetpack Compose (Material 3)
- Single-module MVVM
- Retrofit + OkHttp + Kotlin Serialization
- DataStore Preferences

## Подготовка (для Ubuntu/Linux)
1. Убедитесь, что установлена **JDK 17**:
   ```bash
   sudo apt install openjdk-17-jdk
   ```
2. Создайте файл `local.properties` в корне проекта и укажите путь к Android SDK:
   ```properties
   sdk.dir=/home/di/Android/Sdk
   ```
3. Примите лицензии SDK (если не сделано ранее):
   ```bash
   ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
   ```

## Важно
- По умолчанию включён `FakeRepository` (без реального backend API).
- Для переключения на backend-заготовку поменяйте `REPOSITORY_MODE` в `app/build.gradle.kts` на `"remote"`.
- Базовый URL API задаётся через `API_BASE_URL` в `app/build.gradle.kts`.

## Запуск
1. Откройте папку `PaletaAndroid` в Android Studio.
2. Дождитесь завершения Gradle Sync.
3. Выберите конфигурацию `app` и нажмите **Run**.

## Проверка сборки через терминал
```bash
chmod +x gradlew
./gradlew assembleDebug
```

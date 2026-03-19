# Вклад в Paleta Mobile

<p align="right">
  🌍 <strong>Язык:</strong>
  🇬🇧 <a href="CONTRIBUTING.md">English</a> |
  🇷🇺 Русский
</p>

Спасибо за интерес к развитию Paleta Mobile! Мы хотим, чтобы участие в проекте было максимально прозрачным и простым.

Пожалуйста, придерживайтесь Кодекса поведения и уважительного общения во всех взаимодействиях с проектом.

## Как можно помочь

- Сообщить об ошибке
- Обсудить текущее состояние кода
- Отправить исправление
- Предложить новую функциональность
- Стать мейнтейнером

## Репозитории

Paleta состоит из двух репозиториев:

| Репозиторий | Описание |
| --- | --- |
| [Paleta](https://github.com/diamko/Paleta_web_app) | Веб-приложение и Flask-бэкенд (Python) |
| [Paleta Mobile](https://github.com/diamko/Paleta_mobile_app) | Android-приложение (Kotlin, Jetpack Compose) |

Контрибьюции приветствуются в оба репозитория.

## GitHub Flow

Все изменения в кодовой базе вносятся через Pull Request:

1. Сделайте fork репозитория и создайте ветку от `main`.
2. Если добавили код, который нужно тестировать — добавьте тесты.
3. Если изменили API или поведение — обновите соответствующий README.
4. Убедитесь, что сборка проходит: `./gradlew assembleDebug`.
5. Откройте Pull Request с понятным описанием и шагами проверки.

## Сообщения об ошибках

Для отслеживания ошибок используются GitHub Issues. Создайте новый issue и укажите:

- Краткое описание и контекст.
- Шаги для воспроизведения (версия Android, устройство/эмулятор).
- Ожидаемое поведение vs. фактическое.
- Скриншоты или вывод logcat, если применимо.

## Стиль кода

### Kotlin

- Следуйте стандартным [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Используйте best practices Jetpack Compose (stateless composables, state hoisting).
- Добавляйте заголовочный комментарий к новым `.kt` файлам (см. существующие файлы).
- Держите composable-функции небольшими; переиспользуемые компоненты выносите в `ui/components/`.

### Строки

- Все строки, видимые пользователю, должны быть в `res/values/strings.xml`, `values-ru/strings.xml` и `values-en/strings.xml`.
- Не хардкодьте текст в Compose-коде.

## Сборка

Требования: JDK 17, Android SDK (API 35).

```bash
chmod +x gradlew
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew assembleDebug
```

Готовый APK: `app/build/outputs/apk/debug/app-debug.apk`

## Лицензия

Внося вклад, вы соглашаетесь, что ваши изменения распространяются по MIT License.

# 🗡️ Манчкин Трекер

Android-приложение на Kotlin для отслеживания уровней игроков во время настольной игры Манчкин.

## Стек технологий

| Слой | Библиотека |
|---|---|
| UI | Jetpack Compose + Material Design 3 |
| DI | Hilt 2.51 |
| База данных | Room 2.6.1 |
| Реактивность | Kotlin Coroutines 1.7.3 + Flow |
| Навигация | Navigation Compose 2.7.7 |
| Настройки | DataStore Preferences 1.1.1 |
| Голос | SpeechRecognizer API + TTS |

## Архитектура

```
com.munchkin.tracker/
├── data/
│   ├── dao/          – Room DAO интерфейсы
│   ├── entity/       – Room Entity классы
│   ├── export/       – CsvExportHelper
│   └── repository/   – MunchkinRepository
├── domain/
│   └── model/        – доменные модели, enum-ы
├── presentation/
│   ├── components/   – переиспользуемые UI-компоненты
│   ├── game/         – GameScreen + ViewModel + карточки
│   ├── history/      – HistoryScreen + ViewModel
│   ├── navigation/   – NavGraph
│   ├── players/      – PlayerManagement + Detail
│   ├── settings/     – SettingsScreen + ViewModel
│   └── statistics/   – StatisticsScreen + ViewModel
├── ui/theme/         – Palette, Typography, Theme
├── voice/            – VoiceManager, CommandParser
└── di/               – DatabaseModule (Hilt)
```

## Цветовая палитра

| Токен | HEX | Назначение |
|---|---|---|
| Background | `#0B0F1A` | Основной фон (тёмный сине-чёрный) |
| Surface | `#131827` | Поверхность карточек |
| Primary | `#00E5CC` | Electric Teal – акцент |
| Secondary | `#FFB930` | Золотой – уровни, победитель |
| Tertiary | `#FF5C8A` | Розовый – женский пол |
| LevelUp | `#39FF14` | Неоновый зелёный – вспышка +1 |
| LevelDown | `#FF3D5A` | Неоновый красный – вспышка -1 |
| GoldGlow | `#FFD700` | Золотое свечение (уровни 9–10) |

## Функционал

### Главный экран (GameScreen)
- Карточки игроков с анимированной вспышкой при изменении уровня
- Прогресс-бар до уровня победы
- Золотая обводка + ⚡ на предпобедном уровне
- Корона 👑 на победном уровне
- Таймер игры
- Свайп карточки вправо → отмена последнего действия

### Голосовое управление
- Кнопка микрофона: мгновенный ввод
- Режим «всегда слушает»: «Эй Манчкин» → активация
- Команды:
  - `«Вася плюс один»` — увеличить уровень
  - `«Маша минус два»` — уменьшить уровень
  - `«Петя уровень пять»` — установить уровень
  - `«Новая игра»` — новая игра
  - `«Конец игры Петя победил»` — завершить
  - `«Отмена»` — отменить последнее
- Индикатор: серый/зелёный/красный/жёлтый
- TTS-подтверждение: «Вася, уровень 7»

### Статистика
- Победы по полу (визуальные бары)
- Топ-10 игроков с медалями
- История последних игр
- Экспорт в CSV через системный шаринг

### Настройки
- Уровень победы (5–15, по умолчанию 10)
- Голосовой режим и TTS
- Тёмная / светлая тема

## Сборка

```bash
./gradlew assembleDebug
```

Требования:
- Android minSdk 26 (Android 8.0+)
- Java 17
- AGP 8.5+

## Разрешения

- `RECORD_AUDIO` — голосовое управление (запрашивается в рантайме)
- `INTERNET` — SpeechRecognizer (Google STT)

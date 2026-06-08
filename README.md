# 🗡️ Манчкин Трекер

Android-приложение на Kotlin для отслеживания уровней игроков во время настольной игры Манчкин.

## Стек технологий

| Слой | Библиотека |
|---|---|
| UI | Jetpack Compose + Material Design 3 |
| DI | Hilt |
| База данных | Room |
| Реактивность | Kotlin Coroutines + Flow |
| Навигация | Navigation Compose |
| Настройки | DataStore Preferences |
| Голос | SpeechRecognizer API + TTS + Vosk |

## Архитектура


```
com.munchkin.tracker/
├── data/
│ ├── dao/ – PlayerDao, GameDao, GamePlayerDao, LevelChangeDao, StatsDao
│ ├── entity/ – PlayerEntity, GameEntity, GamePlayerEntity, LevelChangeEntity
│ ├── export/ – CsvExportHelper
│ └── repository/ – MunchkinRepository
├── domain/
│ └── model/ – Enums, Models, StatsModels
├── presentation/
│ ├── components/ – AppTopBar, AppBottomBar, VoiceIndicator, MagicCircleBackground...
│ ├── game/ – GameScreen, GameViewModel, PlayerCard, EndGameDialog...
│ ├── history/ – HistoryScreen, HistoryViewModel
│ ├── navigation/ – MunchkinNavGraph, Routes
│ ├── players/ – PlayerManagementScreen, PlayerDetailScreen, ViewModel
│ ├── settings/ – SettingsScreen, SettingsViewModel
│ └── statistics/ – StatisticsScreen, ChartsTab, RecentGamesTab, PlayerStatsTab...
├── ui/theme/ – Color, Theme, Typography
├── voice/ – VoiceManager, CommandParser, HotwordManager
└── di/ – DatabaseModule (Hilt)
```


## Цветовая палитра «Frozen North»

| Токен | HEX | Назначение |
|---|---|---|
| Background | `#0D1B2A` | Ледяной фон |
| Surface | `#1B2D41` | Карточки |
| Primary | `#64B5F6` | Голубой акцент |
| Secondary | `#FFB74D` | Тёплый оранжевый |
| MaleColor | `#448AFF` | Синий (мужской пол) |
| FemaleColor | `#E91E63` | Розовый (женский пол) |
| GoldGlow | `#FFD700` | Победное золото |
| LevelUp | `#4CAF50` | Зелёная вспышка +1 |
| LevelDown | `#F44336` | Красная вспышка -1 |

## Функционал

### Главный экран (GameScreen)
- Карточки игроков с анимированной вспышкой при изменении уровня
- Прогресс-бар до уровня победы
- Золотая обводка и корона на победном уровне
- Таймер игры
- Кнопка отмены последнего действия
- Добавление игроков из списка или создание новых

### Голосовое управление
- Кнопка микрофона: мгновенный ввод
- Режим «всегда слушает»: «Эй Манчкин» → активация
- Команды:
  - `«Имя плюс два»` — увеличить уровень
  - `«Имя минус один»` — уменьшить уровень
  - `«Имя уровень пять»` — установить уровень
  - `«Новая игра»` — начать новую игру
  - `«Конец игры Имя победил»` — завершить
  - `«Отмена»` — отменить последнее
- TTS-подтверждение действий

### Редактирование персонажа
- Изменение имени по тапу
- Выбор расы и класса из выпадающих списков
- Кнопка «Убрать» для сброса класса/расы
- Смена пола по тапу на иконку ♂/♀

### Статистика
- **9 графиков**:
  - Победы по полу
  - Победы по классам и расам
  - Популярность классов и рас
  - Эффективность классов и рас (% побед)
  - Средняя длительность игр
  - Топ комбинаций класс+раса
- Топ-10 игроков
- История завершённых игр
- Детали игры с распределением уровней
- Свайп для удаления игры
- Экспорт в CSV

### Настройки
- Голосовой режим «всегда слушает»
- TTS-подтверждения
- Горячее слово

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

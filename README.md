# LiveSplits - Android Speedrun Timer

A native Android speedrun timer application built with Kotlin, following modern Android development best practices.

## Features

- **Games Management**: Create and organize your games library
- **Categories**: Add categories for each game with PB tracking
- **Splits**: Create and manage split segments with drag-and-drop reordering
- **Overlay Timer**: Floating timer overlay that works over other apps
- **API Integration**: speedrun.com for game/category suggestions and leaderboards
- **Customization**: Configurable timer colors, display options, and timing settings

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Local Database**: Room
- **Settings Storage**: DataStore Preferences
- **Networking**: Retrofit + Gson
- **Navigation**: Navigation Compose
- **Async**: Coroutines + Flow

## Project Structure

```
app/src/main/java/com/livesplits/
├── data/
│   ├── local/
│   │   ├── entity/          # Room entities (Game, Category, Segment)
│   │   ├── dao/             # Data Access Objects
│   │   └── LiveSplitsDatabase.kt
│   └── settings/
│       └── SettingsRepository.kt
├── domain/
│   ├── model/               # Domain models
│   └── usecase/             # Use cases organized by feature
│       ├── game/
│       ├── category/
│       └── segment/
├── network/
│   ├── api/                 # Retrofit API interfaces
│   ├── model/               # API response models
│   └── SpeedrunRepository.kt
├── service/
│   └── TimerOverlayService.kt
├── ui/
│   ├── navigation/          # Navigation graph and routes
│   ├── screens/
│   │   ├── gameslist/
│   │   ├── game/
│   │   ├── splits/
│   │   └── settings/
│   └── theme/
├── di/                      # Hilt dependency injection modules
├── LiveSplitsApplication.kt
└── MainActivity.kt
```

## Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 26+ (minimum)
- Android SDK 34 (target)
- Kotlin 1.9.20+

### Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run

```bash
./gradlew assembleDebug
```

## Permissions

The app requires the following permissions:

- `FOREGROUND_SERVICE` - For running the timer service
- `SYSTEM_ALERT_WINDOW` - For displaying the timer overlay
- `POST_NOTIFICATIONS` - For timer notifications (Android 13+)
- `INTERNET` - For API integration
- `QUERY_ALL_PACKAGES` - For importing installed games (Android 11+)

### Overlay Permission

On first launch, you'll need to grant the "Draw over other apps" permission for the timer overlay to work.

## Usage

### Adding a Game

1. Tap the FAB (+) button on the games list
2. Enter the game name
3. Tap "Add"

### Adding a Category

1. Open a game
2. Go to the "Categories" tab
3. Tap the FAB (+) button
4. Enter category name (optional: search speedrun.com for suggestions)
5. Tap "Add"

### Managing Splits

1. Open a category (tap or long-press)
2. Select "View & Edit Splits"
3. Use FAB to add splits
4. Drag to reorder splits
5. Long-press to edit/delete

### Using the Timer

1. Open a category
2. Tap on the category card
3. Select "Launch Timer"
4. Grant overlay permission if needed
5. Tap the timer to start/split/stop
6. Long-press to reset (while running) or save PB (when stopped)

### Timer Controls

| Action | Result |
|--------|--------|
| Tap (stopped) | Start timer |
| Tap (running, with splits) | Record split |
| Tap (last split/no splits) | Stop timer |
| Long-press (running) | Reset without saving |
| Long-press (stopped) | Save new PB if improved |
| Drag | Reposition timer |

## Settings

### App Behavior
- **Launch Games**: Attempt to open the game app when starting timer

### Timer Colors
- **Color Ahead**: Displayed when ahead of PB pace
- **Color Behind**: Displayed when behind PB pace
- **Color PB**: Displayed when achieving a new PB

### Timing Settings
- **Comparison**: Choose between PB or Best Segments for delta comparison
- **Countdown**: Set a countdown timer before starting (in milliseconds)

### Display
- **Show Split Name**: Display current split name under timer
- **Show Delta**: Show time difference from PB
- **Show Milliseconds**: Display milliseconds in timer
- **Timer Size**: Adjust timer text size
- **Show Background**: Show semi-transparent background behind timer

## API Integration

### speedrun.com
- Game search for suggestions
- Category suggestions per game
- Leaderboard data

## Data Models

### Game
```kotlin
@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packageName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Category
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val name: String,
    val pbTimeMs: Long = 0L,
    val runCount: Int = 0,
    val speedrunCategoryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

### Segment
```kotlin
@Entity(tableName = "segments")
data class Segment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val position: Int,
    val pbTimeMs: Long = 0L,
    val bestTimeMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
```

## Calculations

### PB Total
Sum of all segment PB times in a category.

### Sum of Bests
Sum of all segment best times in a category.

### Delta
Difference between current split time and the comparison time (PB or Best Segment).

## Known Limitations

1. Timer position is saved per category, not per game
2. Importing installed games may not work on all Android versions
3. speedrun.com API has rate limits

## Future Enhancements

- [ ] Drag-and-drop reordering for splits
- [ ] Real-time sync with speedrun.com
- [ ] Multiple timer layouts/themes
- [ ] Audio cues for splits
- [ ] Auto-split functionality
- [ ] Backup/restore functionality
- [ ] Widget support
- [ ] Wear OS support

## License

This project is open source and available under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

- LiveSplit team for inspiration
- speedrun.com for their API

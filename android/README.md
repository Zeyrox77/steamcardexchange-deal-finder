# Steam Card Deal Finder — Android

A native Android port of the [Steam Card Exchange Deal Finder](../README.md) desktop tool,
built with **Kotlin** and **Jetpack Compose**.

It does exactly what the Python desktop app does:

- Fetches the full inventory list from the steamcardexchange.net API
- Keeps only games where a **complete set** is available to buy
- Scrapes each game's page **in parallel** (up to 20 at a time) to read the real "You Pay" prices
- Shows the **Top 50 cheapest sets**, sorted by total credit cost
- Tap any row to open the listing in your browser

## Tech stack

| Concern        | Choice                                      |
|----------------|---------------------------------------------|
| Language       | Kotlin                                      |
| UI             | Jetpack Compose (Material 3, dark theme)    |
| Networking     | OkHttp                                      |
| HTML parsing   | Jsoup                                       |
| Concurrency    | Kotlin Coroutines (`async` + `Semaphore`)   |
| Architecture   | MVVM (`ViewModel` + `StateFlow`)            |
| Min / Target   | API 24 (Android 7.0) / API 35               |

## Project layout

```
android/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/net/steamcardexchange/dealfinder/
│       │   ├── MainActivity.kt              # Entry point, opens links in browser
│       │   ├── data/
│       │   │   ├── GameDeal.kt              # Data model
│       │   │   └── SteamCardRepository.kt   # API fetch + parallel scrape
│       │   └── ui/
│       │       ├── DealFinderViewModel.kt   # State + fetch pipeline
│       │       ├── DealFinderScreen.kt      # Compose UI
│       │       └── theme/Theme.kt           # Material 3 Steam-style theme
│       └── res/                             # Icons, strings, themes
├── build.gradle.kts
└── settings.gradle.kts
```

## Build & run

### Option A — Android Studio (recommended)
1. Open the `android/` folder in Android Studio (Ladybug or newer).
2. Let Gradle sync. Android Studio creates `local.properties` with your SDK path automatically.
3. Press **Run** on an emulator or a connected device.

### Option B — Command line
Requires the Android SDK and JDK 17–21.

```bash
cd android
# Point Gradle at your SDK (or set the ANDROID_HOME env var instead)
echo "sdk.dir=/path/to/Android/sdk" > local.properties

./gradlew assembleDebug
```

The built APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

Install it on a device with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Note: `local.properties` is machine-specific and is intentionally git-ignored.

## Disclaimer

For personal use only. It accesses publicly available data from Steam Card Exchange — please
be respectful of their servers and avoid raising the parallel request count to extreme values.

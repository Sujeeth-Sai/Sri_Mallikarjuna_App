# Sri Mallikarjuna — Android Operations App

A Kotlin and Jetpack Compose companion app for managing inventory, contacts, expenses, and attendance. It includes Supabase account/data access and an optional Gemini report integration.

[Web companion](https://github.com/Sujeeth-Sai/Sri_Mallikarjuna_Webiste) · [Data layer](app/src/main/java/com/example/data) · [View model](app/src/main/java/com/example/ui/FactoryViewModel.kt)

## What is in the app

- Product and stock transaction management.
- Supplier/customer contacts and expense records.
- Employees and attendance workflows.
- Supabase sign-in and data requests.
- AI-assisted CFO report generation.
- Localized UI strings and Compose screens.

## Stack

Kotlin, Jetpack Compose, Material 3, coroutines, OkHttp/Retrofit, and Moshi. The project also includes JUnit, Robolectric, and Roborazzi test dependencies.

## Set up a development build

1. Clone `https://github.com/Sujeeth-Sai/Sri_Mallikarjuna_App.git` and open it in Android Studio.
2. Install the Android SDK and Gradle/JDK versions compatible with the checked-in Android Gradle Plugin. The project targets SDK 36 and supports Android API 24+.
3. Copy `.env.example` to `.env` in the repository root and configure your development `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and optional `GEMINI_API_KEY`.
4. Complete Gradle and debug-signing setup before running on an emulator or device.

**Checkout gaps:** Gradle wrapper files and the root `debug.keystore` referenced by `app/build.gradle.kts` are absent. This repository therefore needs local build-tool and signing setup before a fresh clone can produce an APK.

## Code map

| Path | Responsibility |
| --- | --- |
| `app/src/main/java/com/example/MainActivity.kt` | Compose application entry |
| `app/src/main/java/com/example/ui/FactoryViewModel.kt` | UI state and operations |
| `app/src/main/java/com/example/data/FactoryRepository.kt` | Business record operations |
| `app/src/main/java/com/example/data/SupabaseService.kt` | Authentication and remote persistence |
| `app/src/main/java/com/example/data/GeminiService.kt` | AI report requests |
| `app/src/main/java/com/example/data/Models.kt` | Data models |

## Backend and release notes

Database migrations are not included. Check the expected tables and RLS policies against your own Supabase project. Some Android table names differ from the web companion; test the schema mapping before claiming shared real-time sync.

The current Gemini integration reads a key from `BuildConfig`. Build-time values are recoverable from an APK, so a production release needs a server-side proxy for secret API credentials. Use synthetic business records for public demos.

Existing test files provide a starting point. Broader inventory, authentication, and cross-client synchronization tests would strengthen the project.

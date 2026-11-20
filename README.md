# Zodiary

**A zodiac-inspired space for journaling, reflection, and self-discovery.**

Zodiary is a personal journaling application that combines daily reflections with astrology-inspired insights. It provides a space to document thoughts, track emotions, and explore daily prompts based on your zodiac sign.

## Features

- **Zodiac Personalization** – Choose your zodiac sign and personalize your experience.
- **Daily Horoscope** – Discover daily astrology-inspired insights.
- **Daily Journaling** – Write thoughts, reflections, and personal experiences.
- **Journal Prompts** – Find inspiration when you don't know what to write.
- **Mood Tracking** – Track emotions throughout your journaling journey.
- **Daily Reflection** – Build a habit of self-reflection and personal growth.

## Tech Stack

- Kotlin and Jetpack Compose
- Single-activity Android app
- On-device journal persistence for the first frontend pass

## Current frontend

The native Android app is in `app/`; its screens use Kotlin and Jetpack Compose rather than XML layouts. It includes onboarding, Home, Journal, Calendar, Profile, and daily reading screens. Without backend settings, daily content comes from CosmyDay's all-signs endpoint and is cached on-device. With Supabase settings, the app reads the shared daily cache and offers optional email/password account sync. Journaling remains available as a guest, and entries keep a local copy after sync.

The supplied flattened Zodiary wordmark is used in the Android app and localhost mirror. Display and heading text use Fraunces; body text and controls use DM Sans. Both fonts are bundled for offline use, with their SIL Open Font Licenses in `font-licenses/`.

Copy `.env.example` to `.env` and add the Supabase project URL and publishable key to enable Supabase in local Android builds. Gradle accepts either `SUPABASE_URL` / `SUPABASE_PUBLISHABLE_KEY` or their `NEXT_PUBLIC_` aliases. The `.env` file is ignored by Git. The database migration, secured CosmyDay import function, and schedule script are under `supabase/`; see [supabase/README.md](supabase/README.md) before deploying them. The hosted schema and function still need to be deployed to the configured project.

## VS Code Android workflow

Open the repository root in VS Code. Android Studio does not need to be open. The workspace uses the existing Android Studio JBR. Gradle reads the SDK location from your local, ignored `local.properties` file (or your Android SDK environment variables); the app targets API 37.

Build the debug APK from the VS Code terminal:

```powershell
.\gradlew.bat :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install and run it on an already connected Android device, use:

```powershell
.\gradlew.bat :app:installDebug
```

This install command uses an available ADB device; it does not start an emulator. To run the local unit tests, use `.\gradlew.bat :app:testDebugUnitTest`. The same build, test, and install actions are available from **Terminal → Run Task** as `Android: Build debug APK`, `Android: Run unit tests`, and `Android: Install debug app on connected device`.

### Open a localhost screen preview

The repository also includes a lightweight browser mirror of the app screens. From the project root, run:

```powershell
npm.cmd run dev
```

Then open [http://localhost:4173](http://localhost:4173). It uses Node's built-in HTTP server and has no package-install step. You can switch among the onboarding, Home, Journal, Calendar, Profile, and reading screens, and try sample-only controls. Stop the server with **Ctrl+C**.

This browser mirror is a visual prototype with fixture content. It is separate from the Android app, does not execute Compose or update automatically from Kotlin edits, and does not access Supabase, CosmyDay, or the device journal. It is useful for browsing the current screen layout without starting an emulator; it is not an interactive Android runtime.

### Compose screen previews

The screens have static `@Preview` examples in `app/src/debug/java/com/zodiary/app/ZodiaryPreviews.kt`. Their sample model uses fixture data and does not call Supabase, CosmyDay, or the device journal store. The preview source is debug-only and is excluded from release builds.

The workspace recommends the free **Compose Preview** VS Code extension (`yuri-schimke.compose-preview`) and sets it to `minimal` mode to avoid its background daemon and automatic rendering on every save. Open the Compose Preview view in VS Code, then use **Compose Preview: Render All Previews** or **Refresh Previews** to request static images. These images are not interactive app sessions. Use the Gradle install task with an already connected device only when you need to exercise navigation, persistence, or API flows.

Preview status on this development machine: Gradle successfully compiled the debug sources and assembled the APK, and the Compose Preview extension discovered the annotated functions. The render step then aborted with `Failed to find required symbol ANativeWindow_fromSurface!`, so no Compose preview image has been verified. Google's Android CLI preview renderer was also blocked by the local Windows App Control policy. Treat the VS Code preview pane as pending a renderer/environment fix; do not rely on it for visual review until it completes a render successfully.

Gradle is configured to use a 2 GB JVM heap, at most two workers, and no parallel Gradle execution to reduce CPU and RAM use. The VS Code tasks use `--no-daemon` so Gradle stops after each task.

The project files are based on the visual and product guidance in [design.md](design.md) and [project.md](project.md).

See [PROJECT_STATUS.md](PROJECT_STATUS.md) for what is implemented, current verification, known limitations, and recommended next steps.

## Project Status

Zodiary is currently in its initial development stage.

## Project Goal

Create a simple, personal, and engaging journaling experience that makes daily reflection feel meaningful and enjoyable.

## Disclaimer

Astrology content is intended for entertainment and personal reflection purposes only. It should not be considered professional, medical, financial, or psychological advice.

## License

This project is currently private and not licensed for redistribution.

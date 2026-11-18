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

The Android Studio project is in `app/`. It includes onboarding, Home, Journal, Calendar, Profile, and daily reading screens. Daily content comes from CosmyDay's public all-signs endpoint and is cached on-device with its provider date and fetch time. Preferences and entries are also stored on-device; account authentication and backend sync are not connected.

Open the project root in Android Studio and sync Gradle. The build configuration targets Android API 37 and requires the matching SDK platform and Build Tools. The project files are based on the visual and product guidance in [design.md](design.md) and [project.md](project.md).

## Project Status

Zodiary is currently in its initial development stage.

## Project Goal

Create a simple, personal, and engaging journaling experience that makes daily reflection feel meaningful and enjoyable.

## Disclaimer

Astrology content is intended for entertainment and personal reflection purposes only. It should not be considered professional, medical, financial, or psychological advice.

## License

This project is currently private and not licensed for redistribution.

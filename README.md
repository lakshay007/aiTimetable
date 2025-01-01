# Schedify

A smart Android app that uses Google's Gemini Vision API to automatically extract class schedules from timetable images.

## Features

- 📸 Upload timetable images
- 🤖 AI-powered timetable extraction
- 📅 View daily class schedules
- 🔔 Home screen widget for quick access
- 🌙 Dark theme
- ✏️ Edit and manage classes
- 🎨 Color-coded subjects

## Setup

1. Clone the repository
2. Get a Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
3. Create a `secrets.properties` file in the root directory and add your API key:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
4. Build and run the project

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Google Gemini Vision API
- Glance Widgets
- Coroutines
- ViewModel

## Requirements

- Android Studio Hedgehog | 2023.1.1
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- JDK 17

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. 
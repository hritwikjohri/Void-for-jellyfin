# Void

**Void** is an experimental native Android client for **Jellyfin**, built entirely with **Kotlin** and **Jetpack Compose**.  
Its goal is to offer a modern, minimal interface for browsing, streaming, and downloading media from a self-hosted Jellyfin server.

---

## ✨ Features

- Jellyfin server integration with user authentication
- Library browsing, search, and detailed media views
- Playback through **MPV** with optional **Android Media3 ExoPlayer** support
- Offline downloads with foreground service notifications
- Jetpack Compose UI with **Material 3**, dynamic themes, and responsive layouts
- Clean architecture separating **data, domain, and presentation layers**
- Local persistence using **Room database** & **DataStore**
- Dependency injection powered by **Hilt**
- Asynchronous work with **Kotlin coroutines**
- Testing stack: JUnit, MockK, Turbine, Espresso, and Compose testing utilities

---

## 🛠️ Platform & Tech Stack

- **Android**: `compileSdk 35`, `targetSdk 35`, `minSdk 24`
- **Kotlin & Jetpack Compose**: Compose enabled with compiler extension `1.5.15`
- **UI Libraries**: Material 3, animations, icons, navigation-compose
- **Image Loading**: Coil with GIF and SVG support
- **Networking**: Retrofit, OkHttp, and Kotlinx Serialization
- **Media Playback**: Jellyfin Core SDK, MPV, and AndroidX Media3
- **Persistence**: Room DB, WorkManager, DataStore
- **Dependency Injection**: Hilt with custom modules (OkHttp, preferences)
- **Utilities**: Accompanist (permissions, system UI helpers)

---

## 📸 Screenshots

<p align="left">
  <strong>Screenshots</strong>
</p>
<p align="center">
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_011221.png?raw=true" alt="Screenshot 1" width="100"/>
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_011901.png?raw=true" alt="Screenshot 2" width="100"/>
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_011952.png?raw=true" alt="Screenshot 3" width="100"/>
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_012425.png?raw=true" alt="Screenshot 4" width="100"/>
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_012433.png?raw=true" alt="Screenshot 5" width="100"/>
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_012453.png?raw=true" alt="Screenshot 7" width="100"/>
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_012555.png?raw=true" alt="Screenshot 8" width="100"/>
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_012725.png?raw=true" alt="Screenshot 9" width="100"/>
  <img src="https://github.com/hritwikjohri/Void-for-jellyfin/blob/main/screenshots/Screenshot_20250818_012845.png?raw=true" alt="Screenshot 10" width="100"/>
</p>
<p align="center">
  👉 See more screenshots in the <a href="https://github.com/hritwikjohri/Void-for-jellyfin/tree/main/screenshots">screenshots folder</a>.
</p>

---

## 🤝 Collaborators

- **Hritwik Johri** – Lead Developer  
- **KHazard**

---

## 🙌 Special Thanks

- **Jellyfin Project** – for the amazing open-source media server  
- **Findroid & Streamyfin devs** – inspiration and feature references  
- **MPV & AndroidX Media3 teams** – for the playback magic  
- **mpv-compose** by [@nitanmarcel](https://central.sonatype.com/artifact/dev.marcelsoftware.mpvcompose/mpv-compose) – used for MPV integration in Compose  

---

## 📄 License

⚠️ No license file is currently present in this repository.  
Please confirm licensing details with the project author before distribution or modification.
# SlideshowApp

An Android application for automatic media slideshow playback (images and video) fetched from a remote server using a `screenKey`.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Configuration](#configuration)
- [Tests](#tests)
- [Changelog](#changelog)

---

## Features

- Automatic slideshow playback with images and video
- Cross-fade transition between slides (3 seconds)
- Playback controls: pause / resume, skip slide
- Periodic server polling every 60 seconds to refresh the playlist
- On-disk media file caching — works offline after the first load
- Loading progress indicator with status text (Loading / Extracting N of M)
- In-session `screenKey` dialog — change the key at runtime without restarting the app
- Slideshow position preserved on screen rotation
- UI localisation: English, Russian, Ukrainian

---

## Architecture

The project follows **Clean Architecture** with clear layer separation:

```
┌──────────────────────────────────────────┐
│                   View                   │  Compose UI (MainActivity, SlideshowPlayer)
├──────────────────────────────────────────┤
│                ViewModel                 │  MainViewModel (state, polling, dialog)
├──────────────────────────────────────────┤
│               Interactor                 │  Business logic: download + cache
├──────────────────────────────────────────┤
│               Use Case                   │  FetchPlaylistUseCase (response mapping)
├──────────────────────────────────────────┤
│              Repository                  │  Abstraction over network and storage
├──────────────────────────────────────────┤
│          Network / Storage               │  Retrofit (ApiService), FileStorage
└──────────────────────────────────────────┘
```

### Data Flow

```
Server API
    │
    ▼
PlaylistRepository ──► FetchPlaylistUseCase
                                │
                                ▼
                    SlideshowInteractor
                    (downloads files, saves manifest.json)
                                │
                           (on failure)
                                ▼
                    ReadSlideshowInteractor
                    (reads manifest.json from cache)
                                │
                                ▼
                        MainViewModel
                        (StateFlow → UI)
                                │
                                ▼
                        SlideshowPlayer
                        (Compose, ExoPlayer, Coil)
```

### Key Design Decisions

| Decision | Rationale |
|---|---|
| `ViewModel` + `StateFlow` | State survives screen rotation |
| `rememberSaveable` for slide index | Playlist position is preserved across Activity recreation |
| `playlistFingerprint` in `LaunchedEffect` | Resets position only when playlist content actually changes, not on rotation |
| `ScreenKeyProvider` (singleton) | Session-scoped key storage without persistence |
| `LoadingProgress` (singleton + StateFlow) | Loading progress is propagated from interactors to ViewModel without changing interfaces |
| `pollingEnabled` (`@VisibleForTesting`) | Controls the infinite polling loop in tests |

---

## Project Structure

```
app/src/main/java/com/my/slideshowapp/
│
├── App.kt                          # Application class (Hilt entry point)
│
├── di/                             # Hilt modules
│   ├── NetworkModule.kt            # Retrofit, OkHttp
│   ├── RepositoryModule.kt         # PlaylistRepository, SlideshowRepository
│   ├── StorageModule.kt            # FileStorage
│   └── InteractorModule.kt         # SlideshowInteractor, ReadSlideshowInteractor
│
├── model/
│   ├── ScreenKeyProvider.kt        # Singleton: session screenKey
│   ├── LoadingState.kt             # Sealed class for loading states + LoadingProgress
│   │
│   ├── entity/
│   │   ├── MediaItem.kt            # Slide model for UI
│   │   ├── PlaylistItemsResponse.kt # DTO for playlist API response
│   │   └── CreativeResponse.kt     # DTO for creative API response
│   │
│   ├── network/
│   │   ├── ApiService.kt           # Retrofit endpoints
│   │   └── RetrofitClient.kt       # HTTP client setup
│   │
│   ├── storage/
│   │   └── FileStorage.kt          # File storage interface
│   │
│   ├── repository/
│   │   ├── PlaylistRepository.kt   # Interface
│   │   ├── PlaylistRepositoryImpl.kt
│   │   ├── SlideshowRepository.kt  # Interface
│   │   └── SlideshowRepositoryImpl.kt
│   │
│   ├── usecase/
│   │   ├── BaseUseCase.kt
│   │   └── FetchPlaylistUseCase.kt # Maps playlist response → list of PlaylistItem
│   │
│   └── interactor/
│       ├── SlideshowInteractor.kt          # Interface (write path)
│       ├── SlideshowInteractorImpl.kt      # Downloads + saves to disk
│       ├── ReadSlideshowInteractor.kt      # Interface (read path)
│       └── ReadSlideshowInteractorImpl.kt  # Reads from cache (manifest.json)
│
├── view/
│   ├── MainActivity.kt             # Single Activity
│   ├── SlideshowPlayer.kt          # Compose: slideshow, animations, controls
│   ├── utils/
│   │   └── FileStorageImpl.kt      # FileStorage implementation
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
└── viewmodel/
    └── MainViewModel.kt            # StateFlow: mediaItems, isPlaying, skipCount,
                                    #   showScreenKeyDialog, loadingState
```

---

## Tech Stack

| Category | Library | Version |
|---|---|---|
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Video | ExoPlayer (Media3) | 1.5.1 |
| Images | Coil | 2.7.0 |
| Network | Retrofit 2 + OkHttp | 2.11.0 / 4.12.0 |
| Serialization | kotlinx.serialization | 1.7.3 |
| DI | Hilt | 2.54 |
| Logging | Timber | 5.0.1 |
| Testing | JUnit 4 + kotlinx-coroutines-test | 4.13.2 / 1.9.0 |
| Kotlin | — | 2.1.0 |
| Min SDK | — | 24 (Android 7.0) |
| Target SDK | — | 35 (Android 15) |

---

## Configuration

The `screenKey` is set in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "SCREEN_KEY", "\"your-screen-key-here\"")
```

The value from `BuildConfig.SCREEN_KEY` is used as the pre-filled default on app start. During runtime, the key can be changed via the 🔑 button in the control bar — without restarting the app, for the current session only.

---

## Tests

All tests are **unit tests** (JVM), with no mocks. Dependencies are simulated via **fake implementations of interfaces**.

### Test Structure

```
app/src/test/java/com/my/slideshowapp/
│
├── model/repository/
│   ├── FakeApiService.kt                   # Fake: ApiService
│   ├── PlaylistRepositoryImplTest.kt       # 4 tests
│   └── SlideshowRepositoryImplTest.kt      # 4 tests
│
├── model/usecase/
│   ├── FakePlaylistRepository.kt           # Fake: PlaylistRepository
│   └── FetchPlaylistUseCaseTest.kt         # 7 tests
│
├── model/interactor/
│   ├── FakeFileStorage.kt                  # Fake: FileStorage (in-memory)
│   ├── FakeSlideshowRepository.kt          # Fake: SlideshowRepository
│   ├── FakeFetchPlaylistUseCase.kt         # Fake: FetchPlaylistUseCase
│   ├── SlideshowInteractorImplTest.kt      # 8 tests
│   └── ReadSlideshowInteractorImplTest.kt  # 6 tests
│
└── viewmodel/
    ├── FakeSlideshowInteractor.kt          # Fake: SlideshowInteractor
    ├── FakeReadSlideshowInteractor.kt      # Fake: ReadSlideshowInteractor
    └── MainViewModelTest.kt               # 17 tests
```

**Total: 46 unit tests**

### Coverage by Layer

| Layer | Class | Tests | What is covered |
|---|---|---|---|
| Repository | `PlaylistRepositoryImpl` | 4 | key forwarding, response mapping, exceptions |
| Repository | `SlideshowRepositoryImpl` | 4 | bytes from response body, key forwarding, exceptions |
| Use Case | `FetchPlaylistUseCase` | 7 | flatMap across playlists, null keys, empty lists |
| Interactor | `SlideshowInteractorImpl` | 8 | download, skip existing, manifest, partial failure |
| Interactor | `ReadSlideshowInteractorImpl` | 6 | cache reading, null keys, default duration, malformed JSON |
| ViewModel | `MainViewModel` | 17 | dialog state, playback controls, polling write/read paths, LoadingState |

### ViewModel Testing Notes

- `Dispatchers.setMain(StandardTestDispatcher())` + `runTest(testDispatcher)` — shared scheduler for `viewModelScope` and test coroutines
- `runCurrent()` instead of `advanceUntilIdle()` for a single polling iteration — prevents infinite `delay` advancement
- `pollingEnabled = false` (`@VisibleForTesting`) — controls exit from `while(true)` without changing production logic

---

## Changelog

### Features
- ✅ Slideshow with cross-fade animation (images via Coil, video via ExoPlayer)
- ✅ Periodic server polling (60 s), playlist updated only when keys change
- ✅ Fallback to local cache (`manifest.json`) when network is unavailable
- ✅ Loading indicator with progress (Loading N of M / Extracting N of M)
- ✅ In-session `screenKey` change dialog without app restart
- ✅ Control bar: pause, skip slide, screen key button

### UX / Stability
- ✅ Slideshow position preserved on screen rotation (`rememberSaveable` + fingerprint comparison)
- ✅ Position reset only on actual playlist content change
- ✅ Localisation: English / Russian / Ukrainian (all strings in `strings.xml`)

### Code Quality
- ✅ 46 unit tests with no mocks — fake interface implementations only
- ✅ Full layer coverage: Repository → UseCase → Interactor → ViewModel
- ✅ KDoc in English for all public entity classes




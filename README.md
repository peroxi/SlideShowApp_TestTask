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
- Error screen on initial load failure with a **Retry** button
- `screenKey` dialog — change the key at runtime; the new key is **persisted across restarts** via DataStore Preferences
- Slideshow position preserved on screen rotation
- UI localisation: English, Russian, Ukrainian

---

## Architecture

The project follows **Clean Architecture** with clear layer separation:

```
┌──────────────────────────────────────────┐
│                   View                   │  Compose UI (MainActivity, SlideshowPlayer)
├──────────────────────────────────────────┤
│                ViewModel                 │  MainViewModel (state, polling, dialog, retry)
├──────────────────────────────────────────┤
│               Interactor                 │  Business logic: download + cache + screen key
├──────────────────────────────────────────┤
│               Use Case                   │  Fetch/Update playlist, Fetch/Update screenKey
├──────────────────────────────────────────┤
│              Repository                  │  Abstraction over network, storage and key store
├──────────────────────────────────────────┤
│          Network / Storage               │  Retrofit (ApiService), FileStorage, DataStore
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
                           │        │
                     (success)   (error / empty)
                           │        │
                           ▼        ▼
                   SlideshowPlayer  Error screen
                   (Compose,        (message + Retry button)
                    ExoPlayer, Coil)
```

### Screen Key Flow

```
App start
    │
    ▼
FetchScreenKeyUsecase
    ├── DataStore has saved key? ──► use saved key
    └── no? ──────────────────────► use BuildConfig.SCREEN_KEY (default)
                                            │
                                    User changes key in dialog
                                            │
                                    UpdateScreenKeyUsecase
                                            │
                                    DataStore (persisted)
```

### Key Design Decisions

| Decision | Rationale |
|---|---|
| `ViewModel` + `StateFlow` | State survives screen rotation |
| `rememberSaveable` for slide index | Playlist position is preserved across Activity recreation |
| `playlistFingerprint` in `LaunchedEffect` | Resets position only when playlist content actually changes, not on rotation |
| `DataStore Preferences` (`KeyStorageImpl`) | `screenKey` is persisted across app restarts; replaces the former session-only `ScreenKeyProvider` |
| `LoadingState.Error` | Initial load failures (empty playlist, no network, no cache) are surfaced to the UI instead of showing an infinite spinner |
| `retryLoading()` in ViewModel | Cancels the current polling job and restarts it; resets `LoadingState` to `Idle` |
| `LoadingProgress` (singleton + StateFlow) | Loading progress is propagated from interactors to ViewModel without changing interfaces |
| `pollingEnabled` (`@VisibleForTesting`) | Controls the infinite polling loop in tests without changing production logic |

---

## Project Structure

```
app/src/main/java/com/my/slideshowapp/
│
├── App.kt                          # Application class (Hilt entry point)
│
├── di/                             # Hilt modules
│   ├── NetworkModule.kt            # Retrofit, OkHttp
│   ├── RepositoryModule.kt         # PlaylistRepository, SlideshowRepository, ScreenKeyRepository
│   ├── StorageModule.kt            # FileStorage, KeyStorage
│   └── InteractorModule.kt         # SlideshowInteractor, ReadSlideshowInteractor,
│                                   #   ScreenKeyInteractor, ScreenKeySaveInteractor
│
├── model/
│   ├── ScreenKeyProvider.kt        # Legacy singleton (kept for reference; superseded by DataStore)
│   ├── LoadingState.kt             # Sealed class: Idle / Loading / Extracting / Done / Error
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
│   │   ├── FileStorage.kt          # File storage interface
│   │   └── KeyStorage.kt           # Screen key storage interface
│   │
│   ├── repository/
│   │   ├── PlaylistRepository.kt
│   │   ├── PlaylistRepositoryImpl.kt
│   │   ├── SlideshowRepository.kt
│   │   ├── SlideshowRepositoryImpl.kt
│   │   ├── ScreenKeyRepository.kt  # Interface: get/save screenKey
│   │   └── ScreenKeyRepositoryImpl.kt
│   │
│   ├── usecase/
│   │   ├── BaseFetchUseCase.kt
│   │   ├── BaseUpdateUseCase.kt
│   │   ├── FetchPlaylistUseCase.kt     # Maps playlist response → list of PlaylistItem
│   │   ├── FetchScreenKeyUsecase.kt    # Returns saved key or BuildConfig default
│   │   └── UpdateScreenKeyUsecase.kt   # Persists new key to DataStore
│   │
│   └── interactor/
│       ├── SlideshowInteractor.kt
│       ├── SlideshowInteractorImpl.kt      # Downloads + saves to disk
│       ├── ReadSlideshowInteractor.kt
│       ├── ReadSlideshowInteractorImpl.kt  # Reads from cache (manifest.json)
│       ├── ScreenKeyInteractor.kt          # Interface: fetch current key
│       ├── ScreenKeyFetchInteractorImpl.kt
│       ├── ScreenKeySaveInteractor.kt      # Interface: persist new key
│       └── ScreenKeySaveInteractorImpl.kt
│
├── view/
│   ├── MainActivity.kt             # Single Activity
│   ├── SlideshowPlayer.kt          # Compose: slideshow, animations, controls, error screen
│   ├── utils/
│   │   ├── FileStorageImpl.kt      # FileStorage implementation
│   │   └── KeyStorageImpl.kt       # KeyStorage implementation (DataStore Preferences)
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
└── viewmodel/
    └── MainViewModel.kt            # StateFlow: mediaItems, isPlaying, skipCount,
                                    #   showScreenKeyDialog, loadingState,
                                    #   currentScreenIdState
                                    # Actions: togglePlayback, skip, confirmScreenKey,
                                    #   retryLoading
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
| Persistence | DataStore Preferences | 1.1.x |
| Logging | Timber | 5.0.1 |
| Testing | JUnit 4 + kotlinx-coroutines-test | 4.13.2 / 1.9.0 |
| Kotlin | — | 2.1.0 |
| Min SDK | — | 24 (Android 7.0) |
| Target SDK | — | 35 (Android 15) |

---

## Configuration

The default `screenKey` is set in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "SCREEN_KEY", "\"your-screen-key-here\"")
```

`BuildConfig.SCREEN_KEY` is used as the fallback when no key has been saved yet. Once the user sets a key via the 🔑 button in the control bar, it is saved to **DataStore Preferences** and used on all subsequent launches. The key can be changed again at any time without restarting the app.

---

## Tests

All tests are **unit tests** (JVM), with no mocks. Dependencies are simulated via **fake implementations of interfaces**.

### Test Structure

```
app/src/test/java/com/my/slideshowapp/
│
├── model/repository/
│   ├── FakeApiService.kt                   # Fake: ApiService
│   ├── FakeScreenKeyRepository.kt          # Fake: ScreenKeyRepository
│   ├── PlaylistRepositoryImplTest.kt       # 4 tests
│   └── SlideshowRepositoryImplTest.kt      # 4 tests
│
├── model/usecase/
│   ├── FakePlaylistRepository.kt           # Fake: PlaylistRepository
│   ├── FetchPlaylistUseCaseTest.kt         # 7 tests
│   ├── FetchScreenKeyUsecaseTest.kt        # 4 tests
│   └── UpdateScreenKeyUsecaseTest.kt       # 3 tests
│
├── model/interactor/
│   ├── FakeFileStorage.kt                  # Fake: FileStorage (in-memory)
│   ├── FakeSlideshowRepository.kt          # Fake: SlideshowRepository
│   ├── FakeFetchPlaylistUseCase.kt         # Fake: FetchPlaylistUseCase
│   ├── SlideshowInteractorImplTest.kt      # 9 tests
│   └── ReadSlideshowInteractorImplTest.kt  # 6 tests
│
└── viewmodel/
    ├── FakeSlideshowInteractor.kt          # Fake: SlideshowInteractor
    ├── FakeReadSlideshowInteractor.kt      # Fake: ReadSlideshowInteractor
    ├── FakeScreenKeySaveInteractor.kt      # Fake: ScreenKeySaveInteractor
    ├── FakeScreenKeyInteractor.kt          # Fake: ScreenKeyInteractor
    └── MainViewModelTest.kt               # 23 tests
```

**Total: 60 unit tests**

### Coverage by Layer

| Layer | Class | Tests | What is covered |
|---|---|---|---|
| Repository | `PlaylistRepositoryImpl` | 4 | key from repository, response mapping, exceptions |
| Repository | `SlideshowRepositoryImpl` | 4 | bytes from response body, key forwarding, exceptions |
| Use Case | `FetchPlaylistUseCase` | 7 | flatMap across playlists, null keys, empty lists |
| Use Case | `FetchScreenKeyUsecase` | 4 | stored key returned; null/empty falls back to `BuildConfig.SCREEN_KEY` |
| Use Case | `UpdateScreenKeyUsecase` | 3 | saves to repository, overwrites existing, propagates exceptions |
| Interactor | `SlideshowInteractorImpl` | 9 | download, skip existing, manifest, partial failure |
| Interactor | `ReadSlideshowInteractorImpl` | 6 | cache reading, null keys, default duration, malformed JSON |
| ViewModel | `MainViewModel` | 23 | dialog state, key persistence, `currentScreenIdState`, playback controls, polling write/read/error paths, `LoadingState`, `retryLoading` |

### ViewModel Testing Notes

- `Dispatchers.setMain(StandardTestDispatcher())` + `runTest(testDispatcher)` — shared scheduler for `viewModelScope` and test coroutines
- `runCurrent()` for a single polling

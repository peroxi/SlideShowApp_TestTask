package com.my.slideshowapp.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.slideshowapp.model.LoadingProgress
import com.my.slideshowapp.model.LoadingState
import com.my.slideshowapp.model.entity.MediaItem
import com.my.slideshowapp.model.interactor.ReadSlideshowInteractor
import com.my.slideshowapp.model.interactor.ScreenKeyInteractor
import com.my.slideshowapp.model.interactor.ScreenKeySaveInteractor
import com.my.slideshowapp.model.interactor.SlideshowInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val writeInteractor: SlideshowInteractor,
    private val readInteractor: ReadSlideshowInteractor,
    private val screenKeySaveInteractor: ScreenKeySaveInteractor,
    private val screenKeyInteractor: ScreenKeyInteractor
) : ViewModel() {

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _skipCount = MutableStateFlow(0)
    val skipCount: StateFlow<Int> = _skipCount.asStateFlow()

    val loadingState = LoadingProgress.state

    private val _showScreenKeyDialog = MutableStateFlow(false)
    val showScreenKeyDialog: StateFlow<Boolean> = _showScreenKeyDialog.asStateFlow()

    private val _currentScreenIdState = MutableStateFlow("")
    val currentScreenIdState: StateFlow<String> = _currentScreenIdState.asStateFlow()

    init {
        startPolling()
        viewModelScope.launch {
            _currentScreenIdState.value = screenKeyInteractor.invoke()
        }
    }

    fun openScreenKeyDialog() {
        _showScreenKeyDialog.value = true
    }

    fun confirmScreenKey(key: String) {
        viewModelScope.launch {
            screenKeySaveInteractor.invoke(key)
            _showScreenKeyDialog.value = false
        }
    }

    fun dismissScreenKeyDialog() {
        _showScreenKeyDialog.value = false
    }

    fun togglePlayback() {
        _isPlaying.value = !_isPlaying.value
    }

    fun skip() {
        _skipCount.value++
    }

    @VisibleForTesting
    var pollingEnabled = true

    private var pollingJob: Job? = null

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val newItems = writeInteractor()
                    val isInitialLoad = _mediaItems.value.isEmpty()
                    if (newItems.isEmpty() && isInitialLoad) {
                        // Server returned empty playlist on first load – treat as error
                        LoadingProgress.update(LoadingState.Error("Empty playlist"))
                        Timber.w("Initial load returned empty playlist")
                    } else {
                        LoadingProgress.update(LoadingState.Done)
                        val newKeys = newItems.map { it.creativeKey }.toSet()
                        val currentKeys = _mediaItems.value.map { it.creativeKey }.toSet()
                        if (newKeys != currentKeys) {
                            _mediaItems.value = newItems
                            Timber.d("Playlist updated: ${newItems.size} items")
                        } else {
                            Timber.d("No new files, playlist unchanged")
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Write failed, reading from cache")
                    if (_mediaItems.value.isEmpty()) {
                        // Initial load – try cache as fallback
                        val cached = try {
                            readInteractor()
                        } catch (cacheEx: Exception) {
                            Timber.w(cacheEx, "Cache read also failed")
                            emptyList()
                        }
                        if (cached.isNotEmpty()) {
                            _mediaItems.value = cached
                            LoadingProgress.update(LoadingState.Done)
                            Timber.d("Loaded ${cached.size} items from cache")
                        } else {
                            // Both network and cache failed on initial load → surface error to UI
                            LoadingProgress.update(LoadingState.Error(e.message ?: "Unknown error"))
                            Timber.e(e, "Initial load failed: no cache available")
                        }
                    }
                    // Periodic update with existing items: already logged, silently ignore
                }
                if (!pollingEnabled) break
                delay(60_000L)
            }
        }
    }

    fun retryLoading() {
        LoadingProgress.update(LoadingState.Idle)
        startPolling()
    }
}

package com.my.slideshowapp.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.slideshowapp.BuildConfig
import com.my.slideshowapp.model.LoadingProgress
import com.my.slideshowapp.model.LoadingState
import com.my.slideshowapp.model.ScreenKeyProvider
import com.my.slideshowapp.model.entity.MediaItem
import com.my.slideshowapp.model.interactor.ReadSlideshowInteractor
import com.my.slideshowapp.model.interactor.SlideshowInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val readInteractor: ReadSlideshowInteractor
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

    init {
        ScreenKeyProvider.screenKey = BuildConfig.SCREEN_KEY
        startPolling()
    }

    fun openScreenKeyDialog() {
        _showScreenKeyDialog.value = true
    }

    fun confirmScreenKey(key: String) {
        ScreenKeyProvider.screenKey = key
        _showScreenKeyDialog.value = false
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

    fun startPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    val newItems = writeInteractor()
                    LoadingProgress.update(LoadingState.Done)
                    val newKeys = newItems.map { it.creativeKey }.toSet()
                    val currentKeys = _mediaItems.value.map { it.creativeKey }.toSet()
                    if (newKeys != currentKeys) {
                        _mediaItems.value = newItems
                        Timber.d("Playlist updated: ${newItems.size} items")
                    } else {
                        Timber.d("No new files, playlist unchanged")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Write failed, reading from cache")
                    if (_mediaItems.value.isEmpty()) {
                        _mediaItems.value = readInteractor()
                        LoadingProgress.update(LoadingState.Done)
                        Timber.d("Loaded ${_mediaItems.value.size} items from cache")
                    }
                }
                if (!pollingEnabled) break
                delay(60_000L)
            }
        }
    }
}

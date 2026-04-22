package com.my.slideshowapp.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class LoadingState {
    data object Idle : LoadingState()
    data class Loading(val current: Int, val total: Int) : LoadingState()
    data class Extracting(val current: Int, val total: Int) : LoadingState()
    data object Done : LoadingState()
}

object LoadingProgress {
    private val _state = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val state: StateFlow<LoadingState> = _state.asStateFlow()

    fun update(state: LoadingState) {
        _state.value = state
    }
}


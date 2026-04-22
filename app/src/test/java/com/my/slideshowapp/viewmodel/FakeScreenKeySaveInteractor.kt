package com.my.slideshowapp.viewmodel

import com.my.slideshowapp.model.interactor.ScreenKeySaveInteractor

internal class FakeScreenKeySaveInteractor(
    private val throwOn: Throwable? = null
) : ScreenKeySaveInteractor {

    var lastSavedKey: String? = null

    override suspend fun invoke(key: String) {
        throwOn?.let { throw it }
        lastSavedKey = key
    }
}


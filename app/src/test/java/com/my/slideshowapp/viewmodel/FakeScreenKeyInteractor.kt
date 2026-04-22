package com.my.slideshowapp.viewmodel

import com.my.slideshowapp.model.interactor.ScreenKeyInteractor

internal class FakeScreenKeyInteractor(
    private val key: String = "default-key",
    private val throwOn: Throwable? = null
) : ScreenKeyInteractor {

    override suspend fun invoke(): String {
        throwOn?.let { throw it }
        return key
    }
}


package com.my.slideshowapp.model.repository

internal class FakeScreenKeyRepository(
    private val screenKey: String? = null,
    private val throwOnGet: Throwable? = null,
    private val throwOnSave: Throwable? = null
) : ScreenKeyRepository {

    var lastSavedScreenKey: String? = null

    override suspend fun getScreenKey(): String? {
        throwOnGet?.let { throw it }
        return screenKey
    }

    override suspend fun saveScreenKey(key: String) {
        throwOnSave?.let { throw it }
        lastSavedScreenKey = key
    }
}
package com.my.slideshowapp.model.storage

interface KeyStorage {
    /** Returns the persisted screen key, or null if none has been saved yet. */
    suspend fun getScreenKey(): String?

    /** Persists the screen key for future sessions. */
    suspend fun saveScreenKey(key: String)
}
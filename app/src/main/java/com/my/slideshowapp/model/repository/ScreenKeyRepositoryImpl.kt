package com.my.slideshowapp.model.repository

import com.my.slideshowapp.model.storage.KeyStorage
import javax.inject.Inject

class ScreenKeyRepositoryImpl @Inject constructor(
    private val keyStorage: KeyStorage
): ScreenKeyRepository {

        override suspend fun getScreenKey(): String? {
            return keyStorage.getScreenKey()
        }

        override suspend fun saveScreenKey(key: String) {
            keyStorage.saveScreenKey(key)
        }
}
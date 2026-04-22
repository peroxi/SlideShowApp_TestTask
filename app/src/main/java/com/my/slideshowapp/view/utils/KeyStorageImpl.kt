package com.my.slideshowapp.view.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.my.slideshowapp.model.storage.KeyStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val DATASTORE_NAME = "key_storage"

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class KeyStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : KeyStorage {

    private val SCREEN_KEY = stringPreferencesKey("screen_key")

    override suspend fun getScreenKey(): String? {
        return context.dataStore.data
            .first()[SCREEN_KEY]
    }

    override suspend fun saveScreenKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[SCREEN_KEY] = key
        }
    }
}
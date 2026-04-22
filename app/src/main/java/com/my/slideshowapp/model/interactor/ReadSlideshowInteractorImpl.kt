package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.LoadingProgress
import com.my.slideshowapp.model.LoadingState
import com.my.slideshowapp.model.entity.MediaItem
import com.my.slideshowapp.model.entity.PlaylistItem
import com.my.slideshowapp.model.storage.FileStorage
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class ReadSlideshowInteractorImpl @Inject constructor(
    private val fileStorage: FileStorage
) : ReadSlideshowInteractor {

    companion object {
        private const val MANIFEST_FILE = "manifest.json"
        private const val MEDIA_PREFIX = "media_"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend operator fun invoke(): List<MediaItem> {
        val content = fileStorage.readFile(MANIFEST_FILE)
        if (content == null) {
            Timber.d("No manifest found, returning empty result")
            return emptyList()
        }

        return try {
            val items = json.decodeFromString<List<PlaylistItem>>(content)
            val total = items.size
            items.mapNotNull { item ->
                val key = item.creativeKey ?: return@mapNotNull null
                MediaItem(
                    filePath = fileStorage.getFilePath("$MEDIA_PREFIX$key"),
                    duration = item.duration ?: 5,
                    creativeKey = key
                )
            }.also { result ->
                result.forEachIndexed { index, _ ->
                    LoadingProgress.update(LoadingState.Extracting(index + 1, total))
                }
                Timber.d("Read ${result.size} media items from cache")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read manifest")
            emptyList()
        }
    }
}

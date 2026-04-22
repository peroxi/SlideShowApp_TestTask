package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.LoadingProgress
import com.my.slideshowapp.model.LoadingState
import com.my.slideshowapp.model.entity.MediaItem
import com.my.slideshowapp.model.entity.PlaylistItem
import com.my.slideshowapp.model.interactor.SlideshowInteractor
import com.my.slideshowapp.model.repository.SlideshowRepository
import com.my.slideshowapp.model.storage.FileStorage
import com.my.slideshowapp.model.usecase.FetchPlaylistUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class SlideshowInteractorImpl @Inject constructor(
    private val useCase: FetchPlaylistUseCase,
    private val slideshowRepository: SlideshowRepository,
    private val fileStorage: FileStorage
) : SlideshowInteractor {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        private const val MANIFEST_FILE = "manifest.json"
        private const val MEDIA_PREFIX = "media_"
    }

    override suspend operator fun invoke(): List<MediaItem> {
        val items = useCase.execute()
        if (items.isEmpty()) return emptyList()

        val filteredItems = items.filter { !it.creativeKey.isNullOrEmpty() }
        val total = filteredItems.size
        var completed = 0

        // Download each media file in parallel and save to disk
        val mediaItems = coroutineScope {
            filteredItems
                .map { item ->
                    async {
                        val key = item.creativeKey ?: return@async null
                        val fileName = "$MEDIA_PREFIX$key"
                        try {
                            if (!fileStorage.fileExists(fileName)) {
                                val bytes = slideshowRepository.fetchCreative(key)
                                fileStorage.saveFile(fileName, bytes)
                                Timber.d("Saved media file: $fileName (${bytes.size} bytes)")
                            } else {
                                Timber.d("Media file already exists, skipping: $fileName")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to download creative: $key")
                        }
                        synchronized(this@SlideshowInteractorImpl) { completed++ }
                        LoadingProgress.update(LoadingState.Loading(completed, total))
                        item
                    }
                }
                .awaitAll()
        }

        // Save manifest only if the set of creativeKeys has changed
        val newKeys = mediaItems.mapNotNull { it?.creativeKey }
        val existingKeys = loadExistingManifestKeys()
        if (newKeys != existingKeys) {
            try {
                val manifestContent = json.encodeToString(mediaItems)
                fileStorage.saveFile(MANIFEST_FILE, manifestContent.toByteArray())
                Timber.d("Manifest updated with ${mediaItems.size} items")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save manifest")
            }
        } else {
            Timber.d("CreativeKeys unchanged, skipping manifest update")
        }

        return mediaItems.mapNotNull { item ->
            val key = item?.creativeKey ?: return@mapNotNull null
            MediaItem(
                filePath = fileStorage.getFilePath("$MEDIA_PREFIX$key"),
                duration = item.duration ?: 5,
                creativeKey = key
            )
        }
    }

    private fun loadExistingManifestKeys(): List<String> {
        val content = fileStorage.readFile(MANIFEST_FILE) ?: return emptyList()
        return try {
            json.decodeFromString<List<PlaylistItem>>(content).mapNotNull { it.creativeKey }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

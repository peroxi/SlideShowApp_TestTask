package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.LoadingProgress
import com.my.slideshowapp.model.LoadingState
import com.my.slideshowapp.model.entity.MediaItem
import com.my.slideshowapp.model.entity.PlaylistItem
import com.my.slideshowapp.model.repository.SlideshowRepository
import com.my.slideshowapp.model.storage.FileStorage
import com.my.slideshowapp.model.usecase.FetchPlaylistFetchUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class SlideshowInteractorImpl @Inject constructor(
    private val useCase: FetchPlaylistFetchUseCase,
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

        // Download each media file in parallel, track failures
        data class DownloadResult(val item: PlaylistItem, val success: Boolean)

        val firstPassResults: List<DownloadResult> = coroutineScope {
            filteredItems.map { item ->
                async {
                    val key = item.creativeKey ?: return@async DownloadResult(item, false)
                    val fileName = "$MEDIA_PREFIX$key"
                    val success = try {
                        if (!fileStorage.fileExists(fileName)) {
                            val tmpFile = "$key.tmp"
                            val stream = slideshowRepository.fetchCreative(key)
                            fileStorage.writeStream(tmpFile, stream)
                            val renamed = fileStorage.renameFile(tmpFile, fileName)
                            if (!renamed) {
                                fileStorage.deleteFile(tmpFile)
                                Timber.w("Failed to rename tmp file for: $key")
                            }
                            Timber.d("Saved media file: $fileName")
                            renamed
                        } else {
                            Timber.d("Media file already exists, skipping: $fileName")
                            true
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to download creative: $key")
                        false
                    }
                    synchronized(this@SlideshowInteractorImpl) { completed++ }
                    LoadingProgress.update(LoadingState.Loading(completed, total))
                    DownloadResult(item, success)
                }
            }.awaitAll()
        }

        val failedItems = firstPassResults.filter { !it.success }

        if (failedItems.isNotEmpty()) {
            Timber.w("${failedItems.size} file(s) failed on first attempt, retrying…")

            // Retry failed items once
            val retryResults: List<DownloadResult> = coroutineScope {
                failedItems.map { (item, _) ->
                    async {
                        val key = item.creativeKey ?: return@async DownloadResult(item, false)
                        val fileName = "$MEDIA_PREFIX$key"
                        val success = try {
                            val tmpFile = "$key.tmp"
                            val stream = slideshowRepository.fetchCreative(key)
                            fileStorage.writeStream(tmpFile, stream)
                            val renamed = fileStorage.renameFile(tmpFile, fileName)
                            if (!renamed) {
                                fileStorage.deleteFile(tmpFile)
                                Timber.w("Retry: failed to rename tmp file for: $key")
                            }
                            Timber.d("Retry succeeded for: $fileName")
                            renamed
                        } catch (e: Exception) {
                            Timber.e(e, "Retry failed for creative: $key")
                            false
                        }
                        DownloadResult(item, success)
                    }
                }.awaitAll()
            }

            val stillFailed = retryResults.filter { !it.success }
            if (stillFailed.isNotEmpty()) {
                val failedKeys = stillFailed.mapNotNull { it.item.creativeKey }
                Timber.e("Aborting playlist update — ${stillFailed.size} file(s) still failed after retry: $failedKeys")
                // Do not save manifest, do not update dataset — wait for next polling cycle
                return emptyList()
            }
        }

        // All files are available — save manifest if playlist items changed
        val allItems = filteredItems
        val existingItems = loadExistingManifestItems()
        if (allItems != existingItems) {
            try {
                val manifestContent = json.encodeToString(allItems)
                fileStorage.saveFile(MANIFEST_FILE, manifestContent.toByteArray())
                Timber.d("Manifest updated with ${allItems.size} items")
            } catch (e: Exception) {
                Timber.e(e, "Failed to save manifest")
            }
        } else {
            Timber.d("CreativeKeys unchanged, skipping manifest update")
        }

        return allItems.mapNotNull { item ->
            val key = item.creativeKey ?: return@mapNotNull null
            MediaItem(
                filePath = fileStorage.getFilePath("$MEDIA_PREFIX$key"),
                duration = item.duration ?: 5,
                creativeKey = key
            )
        }
    }

    private fun loadExistingManifestItems(): List<PlaylistItem> {
        val content = fileStorage.readFile(MANIFEST_FILE) ?: return emptyList()
        return try {
            json.decodeFromString<List<PlaylistItem>>(content)
        } catch (_: Exception) {
            emptyList()
        }
    }
}

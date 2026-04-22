package com.my.slideshowapp.view.utils

import android.content.Context
import com.my.slideshowapp.model.storage.FileStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class FileStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileStorage {

    private fun getFile(fileName: String): File =
        File(context.filesDir, fileName)

    override fun saveFile(fileName: String, data: ByteArray): Boolean {
        return try {
            getFile(fileName).writeBytes(data)
            Timber.d("File '$fileName' saved (${data.size} bytes)")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving file '$fileName'")
            false
        }
    }

    override fun getFilePath(fileName: String): String =
        getFile(fileName).absolutePath

    override fun fileExists(fileName: String): Boolean =
        getFile(fileName).exists()

    override fun readFile(fileName: String): String? {
        return try {
            val file = getFile(fileName)
            if (!file.exists()) {
                Timber.w("File '$fileName' not found")
                return null
            }
            val content = file.readText()
            Timber.d("File '$fileName' read (${content.length} chars)")
            content
        } catch (e: Exception) {
            Timber.e(e, "Error reading file '$fileName'")
            null
        }
    }

    override fun deleteFile(fileName: String) {
        try {
            val file = getFile(fileName)
            if (file.exists()) {
                file.delete()
                Timber.d("File '$fileName' deleted")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting file '$fileName'")
        }
    }

    override fun listFiles(prefix: String): List<String> {
        return try {
            context.filesDir
                .listFiles { file -> file.name.startsWith(prefix) }
                ?.map { it.name }
                ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error listing files with prefix '$prefix'")
            emptyList()
        }
    }
}
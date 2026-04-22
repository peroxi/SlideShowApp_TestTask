package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.storage.FileStorage
import java.io.InputStream

internal class FakeFileStorage : FileStorage {

    private val files = mutableMapOf<String, ByteArray>()
    val savedFiles: Map<String, ByteArray> get() = files

    override fun saveFile(fileName: String, data: ByteArray): Boolean {
        files[fileName] = data
        return true
    }

    override fun writeStream(fileName: String, stream: InputStream): Boolean {
        files[fileName] = stream.readBytes()
        return true
    }

    override fun renameFile(from: String, to: String): Boolean {
        val data = files.remove(from) ?: return false
        files[to] = data
        return true
    }

    override fun readFile(fileName: String): String? =
        files[fileName]?.toString(Charsets.UTF_8)

    override fun getFilePath(fileName: String): String = "/fake/$fileName"

    override fun fileExists(fileName: String): Boolean = files.containsKey(fileName)

    override fun deleteFile(fileName: String) {
        files.remove(fileName)
    }

    override fun listFiles(prefix: String): List<String> =
        files.keys.filter { it.startsWith(prefix) }
}

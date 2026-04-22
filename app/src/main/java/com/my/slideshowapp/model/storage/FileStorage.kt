package com.my.slideshowapp.model.storage

import java.io.InputStream

interface FileStorage {
    /** Saves bytes to a file. Returns true on success. */
    fun saveFile(fileName: String, data: ByteArray): Boolean

    /** Streams an InputStream to a file. Returns true on success. */
    fun writeStream(fileName: String, stream: InputStream): Boolean

    /** Renames (moves) a file. Returns true on success. */
    fun renameFile(from: String, to: String): Boolean

    /** Reads file content. Returns null if file not found. */
    fun readFile(fileName: String): String?

    /** Returns absolute path to the file (may not exist yet). */
    fun getFilePath(fileName: String): String

    /** Returns true if the file exists. */
    fun fileExists(fileName: String): Boolean

    /** Deletes a file. */
    fun deleteFile(fileName: String)

    /** Returns names of files whose names start with the given prefix. */
    fun listFiles(prefix: String): List<String>
}

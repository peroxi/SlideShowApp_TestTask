package com.my.slideshowapp.model.entity

/**
 * Model representing a single media slide.
 * @param filePath absolute path to the locally saved file
 * @param duration display duration in seconds
 * @param creativeKey creative key (file name on the server)
 */
data class MediaItem(
    val filePath: String,
    val duration: Int,
    val creativeKey: String
)


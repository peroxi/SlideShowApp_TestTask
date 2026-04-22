package com.my.slideshowapp.model.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from the screen/playlistItems/{screenKey} endpoint.
 */
@Serializable
data class PlaylistItemsResponse(
    @SerialName("screenKey")
    val screenKey: String? = null,
    @SerialName("playlists")
    val playlists: List<Playlist>? = null
)

@Serializable
data class Playlist(
    @SerialName("playlistKey")
    val playlistKey: String? = null,
    @SerialName("playlistItems")
    val playlistItems: List<PlaylistItem>? = null
)

@Serializable
data class PlaylistItem(
    @SerialName("creativeKey")
    val creativeKey: String? = null,
    @SerialName("duration")
    val duration: Int? = null
)

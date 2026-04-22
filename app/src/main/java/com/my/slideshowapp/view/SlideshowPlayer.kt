package com.my.slideshowapp.view

import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.my.slideshowapp.R
import com.my.slideshowapp.model.LoadingState
import com.my.slideshowapp.view.theme.SlideshowAppTheme
import kotlinx.coroutines.delay
import java.io.File
import com.my.slideshowapp.model.entity.MediaItem as AppMediaItem

private const val FADE_DURATION_MS = 3_000
private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "avi", "mov")

private fun String.isVideo(): Boolean =
    substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

@Composable
fun SlideshowPlayer(
    mediaItems: List<AppMediaItem>,
    isPlaying: Boolean = true,
    skipCount: Int = 0,
    loadingState: LoadingState = LoadingState.Idle,
    onTogglePlayback: () -> Unit = {},
    onSkip: () -> Unit = {},
    onEditScreenKey: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Black)) {
        if (mediaItems.isEmpty()) {
            val statusText = when (loadingState) {
                is LoadingState.Loading ->
                    stringResource(R.string.loading_progress, loadingState.current, loadingState.total)
                is LoadingState.Extracting ->
                    stringResource(R.string.extracting_progress, loadingState.current, loadingState.total)
                else -> stringResource(R.string.loading_default)
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Box
        }

        var currentIndex by rememberSaveable { mutableIntStateOf(0) }
        var nextIndex by rememberSaveable { mutableIntStateOf(0) }
        var isTransitioning by remember { mutableStateOf(false) }
        val fadeAlpha = remember { Animatable(0f) }
        var prevSkipCount by rememberSaveable { mutableIntStateOf(skipCount) }

        // Tracks the playlist identity across recompositions (survives rotation)
        val playlistFingerprint = mediaItems.joinToString(",") { it.creativeKey }
        var savedFingerprint by rememberSaveable { mutableStateOf("") }

        // Reset to beginning only when the playlist content actually changes,
        // not on every recomposition (e.g. screen rotation)
        LaunchedEffect(mediaItems) {
            if (playlistFingerprint != savedFingerprint) {
                savedFingerprint = playlistFingerprint
                currentIndex = 0
                nextIndex = 0
                isTransitioning = false
                fadeAlpha.snapTo(0f)
            }
        }

        // Wait for duration (or skip), then cross-fade to next item
        LaunchedEffect(currentIndex, mediaItems, isPlaying, skipCount) {
            if (!isPlaying) return@LaunchedEffect

            val item = mediaItems[currentIndex % mediaItems.size]
            val isSkip = skipCount != prevSkipCount
            prevSkipCount = skipCount

            if (!isSkip) {
                delay(item.duration * 1000L)
            }

            nextIndex = (currentIndex + 1) % mediaItems.size
            isTransitioning = true
            fadeAlpha.snapTo(0f)
            fadeAlpha.animateTo(1f, animationSpec = tween(durationMillis = FADE_DURATION_MS))

            currentIndex = nextIndex
            isTransitioning = false
            fadeAlpha.snapTo(0f)
        }

        val current = mediaItems[currentIndex % mediaItems.size]
        val next = if (isTransitioning) mediaItems[nextIndex % mediaItems.size] else null

        // Bottom layer – current item fades out
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isTransitioning) 1f - fadeAlpha.value else 1f)
        ) {
            MediaItemView(item = current)
        }

        // Top layer – next item fades in (only exists during transition)
        if (next != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(fadeAlpha.value)
            ) {
                MediaItemView(item = next)
            }
        }

        // Controls overlay
        PlaybackControls(
            isPlaying = isPlaying,
            onTogglePlayback = onTogglePlayback,
            onSkip = onSkip,
            onEditScreenKey = onEditScreenKey,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MediaItemView(item: AppMediaItem) {
    if (item.creativeKey.isVideo()) {
        VideoItemView(filePath = item.filePath)
    } else {
        ImageItemView(filePath = item.filePath)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoItemView(filePath: String) {
    val context = LocalContext.current

    val player = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(filePath))))
            prepare()
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(filePath) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.view_player, null) as PlayerView).apply {
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ImageItemView(filePath: String) {
    AsyncImage(
        model = File(filePath),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onSkip: () -> Unit,
    onEditScreenKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 64.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onTogglePlayback,
            modifier = Modifier
                .size(56.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(
            onClick = onSkip,
            modifier = Modifier
                .size(56.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.cd_skip),
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(
            onClick = onEditScreenKey,
            modifier = Modifier
                .size(56.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.VpnKey,
                contentDescription = stringResource(R.string.cd_screen_key),
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewItems = listOf(
    AppMediaItem(filePath = "/data/sample.jpg",  duration = 5,  creativeKey = "sample.jpg"),
    AppMediaItem(filePath = "/data/sample.mp4",  duration = 8,  creativeKey = "sample.mp4"),
    AppMediaItem(filePath = "/data/sample2.jpg", duration = 5,  creativeKey = "sample2.jpg"),
)

@Preview(name = "Slideshow – playing", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SlideshowPlayerPlayingPreview() {
    SlideshowAppTheme {
        SlideshowPlayer(mediaItems = previewItems, isPlaying = true, modifier = Modifier.fillMaxSize())
    }
}

@Preview(name = "Slideshow – paused", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SlideshowPlayerPausedPreview() {
    SlideshowAppTheme {
        SlideshowPlayer(mediaItems = previewItems, isPlaying = false, modifier = Modifier.fillMaxSize())
    }
}

@Preview(name = "Slideshow – empty", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SlideshowPlayerEmptyPreview() {
    SlideshowAppTheme {
        SlideshowPlayer(mediaItems = emptyList(), modifier = Modifier.fillMaxSize())
    }
}

@Preview(name = "Playback controls – playing", showBackground = true, widthDp = 360, heightDp = 100)
@Composable
private fun ControlsPlayingPreview() {
    SlideshowAppTheme {
        Box(modifier = Modifier.background(Color.DarkGray)) {
            PlaybackControls(isPlaying = true, onTogglePlayback = {}, onSkip = {}, onEditScreenKey = {})
        }
    }
}

@Preview(name = "Playback controls – paused", showBackground = true, widthDp = 360, heightDp = 100)
@Composable
private fun ControlsPausedPreview() {
    SlideshowAppTheme {
        Box(modifier = Modifier.background(Color.DarkGray)) {
            PlaybackControls(isPlaying = false, onTogglePlayback = {}, onSkip = {}, onEditScreenKey = {})
        }
    }
}





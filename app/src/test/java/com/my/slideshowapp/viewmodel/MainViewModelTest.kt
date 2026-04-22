package com.my.slideshowapp.viewmodel

import com.my.slideshowapp.model.LoadingProgress
import com.my.slideshowapp.model.LoadingState
import com.my.slideshowapp.model.ScreenKeyProvider
import com.my.slideshowapp.model.entity.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    // Shared scheduler: viewModelScope and runTest advance together
    private val testDispatcher = StandardTestDispatcher()

    private fun viewModel(
        writeItems: List<MediaItem> = emptyList(),
        writeThrow: Throwable? = null,
        readItems: List<MediaItem> = emptyList(),
        readThrow: Throwable? = null,
        polling: Boolean = false   // single-shot by default in tests
    ): Triple<MainViewModel, FakeSlideshowInteractor, FakeReadSlideshowInteractor> {
        val write = FakeSlideshowInteractor(writeItems, writeThrow)
        val read = FakeReadSlideshowInteractor(readItems, readThrow)
        val vm = MainViewModel(write, read).also { it.pollingEnabled = polling }
        return Triple(vm, write, read)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        LoadingProgress.update(LoadingState.Idle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Dialog state ─────────────────────────────────────────────────────────

    @Test
    fun `showScreenKeyDialog is false by default`() {
        val (vm) = viewModel()
        assertFalse(vm.showScreenKeyDialog.value)
    }

    @Test
    fun `openScreenKeyDialog sets showScreenKeyDialog to true`() {
        val (vm) = viewModel()
        vm.openScreenKeyDialog()
        assertTrue(vm.showScreenKeyDialog.value)
    }

    @Test
    fun `dismissScreenKeyDialog sets showScreenKeyDialog to false`() {
        val (vm) = viewModel()
        vm.openScreenKeyDialog()
        vm.dismissScreenKeyDialog()
        assertFalse(vm.showScreenKeyDialog.value)
    }

    @Test
    fun `confirmScreenKey closes dialog and updates ScreenKeyProvider`() {
        val (vm) = viewModel()
        vm.openScreenKeyDialog()
        vm.confirmScreenKey("new-key")
        assertFalse(vm.showScreenKeyDialog.value)
        assertEquals("new-key", ScreenKeyProvider.screenKey)
    }

    // ── Playback controls ─────────────────────────────────────────────────────

    @Test
    fun `isPlaying is true by default`() {
        val (vm) = viewModel()
        assertTrue(vm.isPlaying.value)
    }

    @Test
    fun `togglePlayback switches isPlaying from true to false`() {
        val (vm) = viewModel()
        vm.togglePlayback()
        assertFalse(vm.isPlaying.value)
    }

    @Test
    fun `togglePlayback switches isPlaying from false to true`() {
        val (vm) = viewModel()
        vm.togglePlayback()
        vm.togglePlayback()
        assertTrue(vm.isPlaying.value)
    }

    @Test
    fun `skipCount starts at zero`() {
        val (vm) = viewModel()
        assertEquals(0, vm.skipCount.value)
    }

    @Test
    fun `skip increments skipCount`() {
        val (vm) = viewModel()
        vm.skip()
        assertEquals(1, vm.skipCount.value)
    }

    @Test
    fun `skip increments skipCount on each call`() {
        val (vm) = viewModel()
        repeat(3) { vm.skip() }
        assertEquals(3, vm.skipCount.value)
    }

    // ── startPolling – write path ─────────────────────────────────────────────

    @Test
    fun `startPolling loads items from writeInteractor on first poll`() = runTest(testDispatcher) {
        val items = listOf(MediaItem(filePath = "/a.jpg", duration = 5, creativeKey = "a.jpg"))
        val (vm) = viewModel(writeItems = items)

        vm.startPolling()
        advanceUntilIdle()

        assertEquals(items, vm.mediaItems.value)
    }

    @Test
    fun `startPolling updates mediaItems only when keys change`() = runTest(testDispatcher) {
        val items = listOf(MediaItem("/a.jpg", 5, "a.jpg"))
        val write = FakeSlideshowInteractor(items)
        val vm = MainViewModel(write, FakeReadSlideshowInteractor()).also {
            it.pollingEnabled = true
        }

        vm.startPolling()
        runCurrent()                        // first poll body runs, stops at delay(60_000)
        val afterFirst = vm.mediaItems.value

        vm.pollingEnabled = false           // break after next body
        advanceTimeBy(60_001)               // wake up delay, second poll runs → break

        // Keys are the same — mediaItems must not change
        assertEquals(afterFirst, vm.mediaItems.value)
        assertTrue(write.callCount >= 2)
    }

    @Test
    fun `startPolling sets LoadingState to Done after successful write`() = runTest(testDispatcher) {
        val (vm) = viewModel(writeItems = listOf(MediaItem("/a.jpg", 5, "a.jpg")))

        vm.startPolling()
        advanceUntilIdle()

        assertEquals(LoadingState.Done, vm.loadingState.value)
    }

    // ── startPolling – read (fallback) path ───────────────────────────────────

    @Test
    fun `startPolling falls back to readInteractor when write throws`() = runTest(testDispatcher) {
        val cached = listOf(MediaItem("/cached.jpg", 5, "cached.jpg"))
        val (vm) = viewModel(
            writeThrow = RuntimeException("network error"),
            readItems = cached
        )

        vm.startPolling()
        advanceUntilIdle()

        assertEquals(cached, vm.mediaItems.value)
    }

    @Test
    fun `startPolling does not overwrite existing items when write throws`() = runTest(testDispatcher) {
        val initial = listOf(MediaItem("/a.jpg", 5, "a.jpg"))
        var callCount = 0
        val smartWrite = object : com.my.slideshowapp.model.interactor.SlideshowInteractor {
            override suspend fun invoke(): List<MediaItem> {
                callCount++
                return if (callCount == 1) initial else throw RuntimeException("fail")
            }
        }
        val vm = MainViewModel(smartWrite, FakeReadSlideshowInteractor()).also {
            it.pollingEnabled = true
        }

        vm.startPolling()
        runCurrent()                        // first poll succeeds, stops at delay

        vm.pollingEnabled = false           // stop after next iteration
        advanceTimeBy(60_001)
        runCurrent()                        // second poll fails → !pollingEnabled → break

        assertEquals(initial, vm.mediaItems.value)
    }

    @Test
    fun `startPolling sets LoadingState to Done after successful read fallback`() = runTest(testDispatcher) {
        val (vm) = viewModel(
            writeThrow = RuntimeException("fail"),
            readItems = listOf(MediaItem("/a.jpg", 5, "a.jpg"))
        )

        vm.startPolling()
        advanceUntilIdle()

        assertEquals(LoadingState.Done, vm.loadingState.value)
    }
}




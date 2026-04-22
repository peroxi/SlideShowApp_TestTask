package com.my.slideshowapp.model.usecase

import com.my.slideshowapp.BuildConfig
import com.my.slideshowapp.model.repository.FakeScreenKeyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FetchScreenKeyUsecaseTest {

    @Test
    fun `returns stored key when repository has a non-empty value`() = runBlocking {
        val usecase = FetchScreenKeyUsecase(FakeScreenKeyRepository("my-screen-key"))
        assertEquals("my-screen-key", usecase.execute())
    }

    @Test
    fun `returns BuildConfig SCREEN_KEY when repository returns null`() = runBlocking {
        val usecase = FetchScreenKeyUsecase(FakeScreenKeyRepository(screenKey = null))
        assertEquals(BuildConfig.SCREEN_KEY, usecase.execute())
    }

    @Test
    fun `returns BuildConfig SCREEN_KEY when repository returns empty string`() = runBlocking {
        val usecase = FetchScreenKeyUsecase(FakeScreenKeyRepository(screenKey = ""))
        assertEquals(BuildConfig.SCREEN_KEY, usecase.execute())
    }

    @Test
    fun `returns different stored keys correctly`() = runBlocking {
        listOf("abc", "xyz-123", "7d47b6d7-0000-0000-0000-000000000000").forEach { key ->
            val usecase = FetchScreenKeyUsecase(FakeScreenKeyRepository(key))
            assertEquals(key, usecase.execute())
        }
    }
}

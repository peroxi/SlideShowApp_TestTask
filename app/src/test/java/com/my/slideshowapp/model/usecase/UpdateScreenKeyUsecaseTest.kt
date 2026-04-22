package com.my.slideshowapp.model.usecase

import com.my.slideshowapp.model.repository.FakeScreenKeyRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateScreenKeyUsecaseTest {

    @Test
    fun `execute saves key to repository`() = runBlocking {
        val repo = FakeScreenKeyRepository()
        val usecase = UpdateScreenKeyUsecase(repo)

        usecase.execute("new-key")

        assertEquals("new-key", repo.lastSavedScreenKey)
    }

    @Test
    fun `execute overwrites previous key in repository`() = runBlocking {
        val repo = FakeScreenKeyRepository("old-key")
        val usecase = UpdateScreenKeyUsecase(repo)

        usecase.execute("updated-key")

        assertEquals("updated-key", repo.lastSavedScreenKey)
    }

    @Test
    fun `execute propagates exception from repository`() = runBlocking {
        val repo = FakeScreenKeyRepository(throwOnSave = RuntimeException("save failed"))
        val usecase = UpdateScreenKeyUsecase(repo)

        val ex = runCatching { usecase.execute("any-key") }.exceptionOrNull()

        assertEquals("save failed", ex?.message)
    }
}


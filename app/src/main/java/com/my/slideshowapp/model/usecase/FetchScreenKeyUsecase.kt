package com.my.slideshowapp.model.usecase

import com.my.slideshowapp.BuildConfig
import com.my.slideshowapp.model.repository.ScreenKeyRepository
import javax.inject.Inject

class FetchScreenKeyUsecase @Inject constructor(
    private val screenKeyRepository: ScreenKeyRepository
): BaseFetchUseCase<String> {
    override suspend fun execute(): String {
        screenKeyRepository.getScreenKey().let { screenKey ->
            return if (screenKey.isNullOrEmpty()) {
                BuildConfig.SCREEN_KEY
            } else {
                screenKey
            }
        }
    }
}
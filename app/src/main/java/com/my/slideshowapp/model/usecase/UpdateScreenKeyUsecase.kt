package com.my.slideshowapp.model.usecase

import com.my.slideshowapp.model.repository.ScreenKeyRepository
import javax.inject.Inject

class UpdateScreenKeyUsecase @Inject constructor(
    private val screenKeyRepository: ScreenKeyRepository
): BaseUpdateUseCase<String> {
    override suspend fun execute(param: String) {
        screenKeyRepository.saveScreenKey(param)
    }
}
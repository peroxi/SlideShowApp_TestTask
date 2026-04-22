package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.usecase.UpdateScreenKeyUsecase
import javax.inject.Inject

class ScreenKeySaveInteractorImpl @Inject constructor(
    private val screenKeyUsecase: UpdateScreenKeyUsecase
): ScreenKeySaveInteractor {
    override suspend fun invoke(key: String) {
        screenKeyUsecase.execute(key)
    }
}
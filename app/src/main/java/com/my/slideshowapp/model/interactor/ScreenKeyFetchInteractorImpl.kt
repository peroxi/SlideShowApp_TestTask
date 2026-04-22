package com.my.slideshowapp.model.interactor

import com.my.slideshowapp.model.usecase.FetchScreenKeyUsecase
import javax.inject.Inject

class ScreenKeyFetchInteractorImpl
    @Inject constructor(
        private val screenKeyUsecase: FetchScreenKeyUsecase
): ScreenKeyInteractor {
    override suspend fun invoke(): String {
        return screenKeyUsecase.execute()
    }
}
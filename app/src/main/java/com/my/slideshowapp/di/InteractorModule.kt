package com.my.slideshowapp.di

import com.my.slideshowapp.model.interactor.ReadSlideshowInteractor
import com.my.slideshowapp.model.interactor.SlideshowInteractor
import com.my.slideshowapp.model.interactor.ReadSlideshowInteractorImpl
import com.my.slideshowapp.model.interactor.SlideshowInteractorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InteractorModule {

    @Binds
    @Singleton
    abstract fun bindSlideshowInteractor(impl: SlideshowInteractorImpl): SlideshowInteractor

    @Binds
    @Singleton
    abstract fun bindReadSlideshowInteractor(impl: ReadSlideshowInteractorImpl): ReadSlideshowInteractor
}

package com.my.slideshowapp.di

import com.my.slideshowapp.model.repository.PlaylistRepository
import com.my.slideshowapp.model.repository.PlaylistRepositoryImpl
import com.my.slideshowapp.model.repository.SlideshowRepository
import com.my.slideshowapp.model.repository.SlideshowRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSlideshowRepository(impl: SlideshowRepositoryImpl): SlideshowRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository
}


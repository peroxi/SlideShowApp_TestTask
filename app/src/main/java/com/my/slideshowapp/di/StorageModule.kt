package com.my.slideshowapp.di

import com.my.slideshowapp.model.storage.FileStorage
import com.my.slideshowapp.model.storage.KeyStorage
import com.my.slideshowapp.view.utils.FileStorageImpl
import com.my.slideshowapp.view.utils.KeyStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindFileStorage(impl: FileStorageImpl): FileStorage

    @Binds
    @Singleton
    abstract fun bindScreenKeyStorage(impl: KeyStorageImpl): KeyStorage
}


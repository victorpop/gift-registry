package com.giftregistry.di

import com.giftregistry.data.preferences.LastRegistryPreferencesDataStore
import com.giftregistry.data.store.StoreRepositoryImpl
import com.giftregistry.domain.preferences.LastRegistryPreferencesRepository
import com.giftregistry.domain.store.StoreRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StoresModule {

    @Binds
    @Singleton
    abstract fun bindStoreRepository(impl: StoreRepositoryImpl): StoreRepository

    @Binds
    @Singleton
    abstract fun bindLastRegistryPreferences(
        impl: LastRegistryPreferencesDataStore
    ): LastRegistryPreferencesRepository
}

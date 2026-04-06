package com.giftregistry.di

import com.giftregistry.data.auth.AuthRepositoryImpl
import com.giftregistry.data.preferences.LanguagePreferencesDataStore
import com.giftregistry.data.registry.ItemRepositoryImpl
import com.giftregistry.data.registry.RegistryRepositoryImpl
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.item.ItemRepository
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import com.giftregistry.domain.registry.RegistryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLanguagePreferences(impl: LanguagePreferencesDataStore): LanguagePreferencesRepository

    @Binds
    @Singleton
    abstract fun bindRegistryRepository(impl: RegistryRepositoryImpl): RegistryRepository

    @Binds
    @Singleton
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository
}

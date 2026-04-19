package com.giftregistry.di

import com.giftregistry.data.auth.AuthRepositoryImpl
import com.giftregistry.data.preferences.GuestPreferencesDataStore
import com.giftregistry.data.preferences.LanguagePreferencesDataStore
import com.giftregistry.data.preferences.OnboardingPreferencesDataStore
import com.giftregistry.data.registry.ItemRepositoryImpl
import com.giftregistry.data.registry.RegistryRepositoryImpl
import com.giftregistry.data.reservation.ReservationRepositoryImpl
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.item.ItemRepository
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import com.giftregistry.domain.preferences.OnboardingPreferencesRepository
import com.giftregistry.domain.registry.RegistryRepository
import com.giftregistry.domain.reservation.ReservationRepository
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

    @Binds
    @Singleton
    abstract fun bindReservationRepository(impl: ReservationRepositoryImpl): ReservationRepository

    @Binds
    @Singleton
    abstract fun bindGuestPreferences(impl: GuestPreferencesDataStore): GuestPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingPreferences(impl: OnboardingPreferencesDataStore): OnboardingPreferencesRepository
}

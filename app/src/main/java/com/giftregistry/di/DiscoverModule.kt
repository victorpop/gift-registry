package com.giftregistry.di

import com.giftregistry.data.discover.DiscoverRepositoryImpl
import com.giftregistry.domain.discover.DiscoverRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Phase 17 — Hilt binding for the Discover repository.
 *
 * `FirebaseFunctions` (the only dependency of `DiscoverRepositoryImpl`) is
 * already provided by `AppModule.provideFirebaseFunctions()` pinned to
 * `europe-west3`. No per-module provider is needed; the existing singleton
 * satisfies the constructor injection.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoverModule {
    @Binds
    @Singleton
    abstract fun bindDiscoverRepository(impl: DiscoverRepositoryImpl): DiscoverRepository
}

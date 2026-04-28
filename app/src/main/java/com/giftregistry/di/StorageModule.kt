package com.giftregistry.di

import android.content.ContentResolver
import android.content.Context
import com.giftregistry.BuildConfig
import com.giftregistry.data.storage.CoverImageProcessor
import com.giftregistry.data.storage.CoverImageProcessorImpl
import com.giftregistry.data.storage.StorageRepositoryImpl
import com.giftregistry.domain.storage.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Phase 12 — Hilt module for cover-photo upload (D-04 / D-05 / D-07).
 *
 * Mirrors the [StoresModule] pattern — separate module so Phase 12's bindings
 * are discoverable and DataModule stays untouched.
 *
 * Provides:
 *   - [FirebaseStorage] singleton (with emulator wiring gated on
 *     `BuildConfig.USE_FIREBASE_EMULATOR` per the Firebase emulator pattern
 *     from Phase 02 quick task 260420-gv2).
 *   - [ContentResolver] for [CoverImageProcessorImpl] (Photo Picker URIs).
 *
 * Binds:
 *   - [StorageRepository] → [StorageRepositoryImpl]
 *   - [CoverImageProcessor] → [CoverImageProcessorImpl]
 *
 * Storage emulator port 9199 is declared in `firebase.json` (Plan 01).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindCoverImageProcessor(impl: CoverImageProcessorImpl): CoverImageProcessor

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseStorage(): FirebaseStorage =
            FirebaseStorage.getInstance().also { storage ->
                if (BuildConfig.USE_FIREBASE_EMULATOR) {
                    storage.useEmulator("10.0.2.2", 9199)
                }
            }

        @Provides
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
            context.contentResolver
    }
}

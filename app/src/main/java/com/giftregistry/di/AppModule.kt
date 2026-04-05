package com.giftregistry.di

import com.giftregistry.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().also { auth ->
        if (BuildConfig.DEBUG) {
            auth.useEmulator("10.0.2.2", 9099)
        }
    }
}

package com.giftregistry.di

import com.giftregistry.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
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

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance().also { db ->
            if (BuildConfig.DEBUG) {
                db.useEmulator("10.0.2.2", 8080)
            }
        }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance().also { fns ->
            if (BuildConfig.DEBUG) {
                fns.useEmulator("10.0.2.2", 5001)
            }
        }
}

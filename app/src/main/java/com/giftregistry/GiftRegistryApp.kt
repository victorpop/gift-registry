package com.giftregistry

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import com.giftregistry.domain.usecase.RegisterFcmTokenUseCase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

@HiltAndroidApp(Application::class)
class GiftRegistryApp : Hilt_GiftRegistryApp() {

    @Inject lateinit var languagePrefsRepo: LanguagePreferencesRepository
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // App Check provider wiring. Debug builds use the DebugAppCheckProvider;
        // each device's auto-generated debug token must be registered in Firebase
        // Console (App Check → Apps → Manage debug tokens). Release builds use
        // PlayIntegrity. MUST be installed before any other Firebase SDK call.
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance(),
            )
        }

        // Restore persisted locale before any activity is launched to prevent flicker.
        // Hilt field injection completes during super.onCreate() above, so the repo is
        // available here (unlike in MainActivity.onCreate before its super call).
        val savedLocale = runBlocking { languagePrefsRepo.getLanguageTag() }
        if (savedLocale != null) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(savedLocale)
            )
        }

        // FCM token registration on sign-in. FirebaseMessagingService.onNewToken only
        // fires on first install + token rotation — not on subsequent sign-ins. Without
        // this listener, users who sign in after the FCM token already exists never
        // have their token written to users/{uid}/fcmTokens, so invitePush/purchasePush
        // find no tokens and silently no-op.
        firebaseAuth.addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                appScope.launch {
                    runCatching {
                        val token = FirebaseMessaging.getInstance().token.await()
                        registerFcmTokenUseCase(token)
                    }.onFailure { e ->
                        Log.w("FcmTokenRegister", "Failed to register FCM token post sign-in", e)
                    }
                }
            }
        }
    }
}

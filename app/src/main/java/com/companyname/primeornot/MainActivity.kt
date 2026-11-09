package com.companyname.primeornot

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.companyname.primeornot.audio.SoundPlayer
import com.companyname.primeornot.data.DataStoreGameStore
import com.companyname.primeornot.data.GameStore
import com.companyname.primeornot.ui.AppRoot
import com.companyname.primeornot.ui.AppViewModel
import com.companyname.primeornot.ui.GameEffect
import com.companyname.primeornot.ui.GameViewModel
import com.companyname.primeornot.ui.theme.PrimeOrNotTheme
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {

    private val gameStore: GameStore by lazy { DataStoreGameStore(applicationContext) }

    private val gameViewModel: GameViewModel by viewModels {
        GameViewModel.factory(gameStore)
    }
    private val appViewModel: AppViewModel by viewModels {
        AppViewModel.factory(gameStore)
    }

    private var soundPlayer: SoundPlayer? = null

    /** Set by the UMP callbacks; the retry banner is composed only when true. */
    private var canRequestAds by mutableStateOf(false)

    /** True while Google requires an entry point for changing consent. */
    private var privacyOptionsRequired by mutableStateOf(false)

    private var adsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        soundPlayer = SoundPlayer(this)
        requestAdsConsent()

        setContent {
            PrimeOrNotTheme {
                LaunchedEffect(Unit) {
                    gameViewModel.effects.collect { effect ->
                        val player = soundPlayer ?: return@collect
                        when (effect) {
                            GameEffect.Swipe -> player.playSwipe()
                            is GameEffect.Correct -> player.playCorrect(effect.streak)
                            GameEffect.Wrong -> player.playWrong()
                            GameEffect.Tick -> player.playTick()
                            GameEffect.NewBest,
                            is GameEffect.TierUp,
                            is GameEffect.Milestone -> player.playLevelUp()
                        }
                    }
                }

                AppRoot(
                    store = gameStore,
                    appViewModel = appViewModel,
                    gameViewModel = gameViewModel,
                    canRequestAds = canRequestAds,
                    onPrivacyOptions = if (privacyOptionsRequired) {
                        ::showPrivacyOptions
                    } else {
                        null
                    },
                )
            }
        }
    }

    /**
     * Consent comes first: UMP decides whether ad requests are legal, and the
     * Mobile Ads SDK is initialised only once that answer is yes. A stored
     * decision from a previous session is honoured immediately, and a failed
     * update re-reads it instead of silently disabling ads for the session.
     */
    private fun requestAdsConsent() {
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        updateConsentState(consentInformation)

        consentInformation.requestConsentInfoUpdate(
            this,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) {
                    updateConsentState(consentInformation)
                }
            },
            {
                // Keep the last known decision when the update fails.
                updateConsentState(consentInformation)
            },
        )
    }

    private fun updateConsentState(consentInformation: ConsentInformation) {
        canRequestAds = consentInformation.canRequestAds()
        privacyOptionsRequired =
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (canRequestAds) {
            initializeAdsIfAllowed()
        }
    }

    private fun initializeAdsIfAllowed() {
        if (adsInitialized) return
        adsInitialized = true
        MobileAds.initialize(this)
    }

    /** Reopens the UMP form so consent can be changed or withdrawn later. */
    private fun showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(this) {
            updateConsentState(UserMessagingPlatform.getConsentInformation(this))
        }
    }

    override fun onResume() {
        super.onResume()
        gameViewModel.onResume()
    }

    override fun onPause() {
        gameViewModel.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        soundPlayer?.release()
        soundPlayer = null
        super.onDestroy()
    }
}

package com.companyname.primeornot

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.companyname.primeornot.ads.BannerAd
import com.companyname.primeornot.audio.SoundPlayer
import com.companyname.primeornot.ui.GameEffect
import com.companyname.primeornot.ui.GameScreen
import com.companyname.primeornot.ui.GameViewModel
import com.companyname.primeornot.ui.theme.PrimeOrNotTheme
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels { GameViewModel.factory(this) }
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
                val state by viewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.effects.collect { effect ->
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

                GameScreen(
                    state = state,
                    onSwipe = viewModel::onSwipe,
                    onRestart = viewModel::restart,
                    onStart = viewModel::begin,
                    effects = viewModel.effects,
                    gameOverAd = if (canRequestAds) {
                        { BannerAd(Modifier.fillMaxWidth()) }
                    } else {
                        null
                    },
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
        viewModel.onResume()
    }

    override fun onPause() {
        viewModel.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        soundPlayer?.release()
        soundPlayer = null
        super.onDestroy()
    }
}

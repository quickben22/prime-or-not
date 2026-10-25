package com.primetinder

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
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.primetinder.ads.BannerAd
import com.primetinder.audio.SoundPlayer
import com.primetinder.ui.GameEffect
import com.primetinder.ui.GameScreen
import com.primetinder.ui.GameViewModel
import com.primetinder.ui.theme.PrimeTinderTheme
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels { GameViewModel.factory(this) }
    private var soundPlayer: SoundPlayer? = null

    /** Set by the UMP callbacks; the retry banner is composed only when true. */
    private var canRequestAds by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        soundPlayer = SoundPlayer(this)
        MobileAds.initialize(this)
        requestAdsConsent()

        setContent {
            PrimeTinderTheme {
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
                )
            }
        }
    }

    /**
     * AdMob requires a consent signal before the first ad request. The current
     * state covers most regions; otherwise the UMP flow runs and re-checks.
     * A failed update leaves banners off rather than risk a bad request.
     */
    private fun requestAdsConsent() {
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        if (consentInformation.canRequestAds()) {
            canRequestAds = true
        }

        consentInformation.requestConsentInfoUpdate(
            this,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) {
                    if (consentInformation.canRequestAds()) {
                        canRequestAds = true
                    }
                }
            },
            {
                // No consent information: keep the retry banner hidden.
            },
        )
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

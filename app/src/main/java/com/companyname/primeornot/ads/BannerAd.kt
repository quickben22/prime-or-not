package com.companyname.primeornot.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.companyname.primeornot.BuildConfig

/** AdMob's minimum supported banner width; narrower requests fall back to 320×50. */
private const val MIN_AD_WIDTH_DP = 320

/**
 * A single anchored adaptive banner, sized from the width it is given.
 *
 * The slot reserves the banner's height while the request is in flight, so a
 * slow fill never shifts the panel above it. A no-fill collapses the slot to
 * zero height instead of leaving an empty strip.
 *
 * Only composed on the game-over screen; the view is destroyed with the
 * composition so a finished retry panel does not keep a banner alive.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(MIN_AD_WIDTH_DP)
        val adSize = remember(context, widthDp) {
            // AdMob 25.x deprecates the AdSize factory helpers, but an anchored
            // adaptive banner is still the supported way to fill this slot.
            @Suppress("DEPRECATION")
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        var failed by remember(adSize) { mutableStateOf(false) }

        if (failed) return@BoxWithConstraints

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adSize.height.dp),
                factory = { ctx ->
                    AdView(ctx).apply {
                        adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                        setAdSize(adSize)
                        adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                failed = true
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                onRelease = { view -> view.destroy() },
            )
        }
    }
}

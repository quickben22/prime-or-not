package com.companyname.primeornot.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import com.companyname.primeornot.R
import com.companyname.primeornot.ui.theme.PrimeAccent

/**
 * The shared in-app mark: the `1||0` glyph layer over the app accent.
 *
 * The adaptive foreground is a 108 dp layer. Scaling it to 1.5x crops the same
 * central 72 dp that a launcher applies, so the glyph keeps its store-icon
 * proportions at any [modifier] size.
 */
@Composable
fun PrimeLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(PrimeAccent),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.5f),
        )
    }
}

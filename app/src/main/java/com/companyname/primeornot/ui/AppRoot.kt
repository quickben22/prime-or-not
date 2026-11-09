package com.companyname.primeornot.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companyname.primeornot.R
import com.companyname.primeornot.ads.BannerAd
import com.companyname.primeornot.data.CalendarDay
import com.companyname.primeornot.data.GameStore
import com.companyname.primeornot.ui.theme.PrimeBackground
import com.companyname.primeornot.ui.theme.PrimeBackgroundTop
import com.companyname.primeornot.ui.theme.PrimeTextPrimary

/**
 * Top-level navigation. Kept as a tiny explicit state machine instead of pulling
 * in Navigation Compose: there are only five destinations and the transition
 * rules (abandon, resume, onboarding) are easier to read here.
 */
@Composable
fun AppRoot(
    store: GameStore,
    appViewModel: AppViewModel,
    gameViewModel: GameViewModel,
    canRequestAds: Boolean,
    onPrivacyOptions: (() -> Unit)?,
) {
    val appState by appViewModel.state.collectAsStateWithLifecycle()
    val gameState by gameViewModel.state.collectAsStateWithLifecycle()

    var showResumeDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showAbandonDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(
        enabled = appState.screen != AppScreen.Home &&
            !showResumeDialog && !showDiscardDialog && !showAbandonDialog,
    ) {
        when (appState.screen) {
            AppScreen.Home -> Unit
            AppScreen.Classic -> {
                if (gameState.phase == GamePhase.GameOver || gameState.phase == GamePhase.Idle) {
                    appViewModel.openHome()
                } else {
                    showAbandonDialog = true
                }
            }
            AppScreen.Practice, AppScreen.HowToPlay, AppScreen.Records -> appViewModel.openHome()
        }
    }

    if (!appState.recordsLoaded) {
        LoadingScreen()
        return
    }

    val today = CalendarDay.today(System.currentTimeMillis())

    when (appState.screen) {
        AppScreen.Home -> HomeScreen(
            records = appState.records,
            activeSession = appState.activeSession,
            today = today,
            onPlay = {
                val session = appState.activeSession
                if (session != null) {
                    showResumeDialog = true
                } else {
                    gameViewModel.startNewGame()
                    appViewModel.openClassic()
                }
            },
            onResume = { session ->
                gameViewModel.resume(session)
                appViewModel.openClassic()
            },
            onDiscard = { showDiscardDialog = true },
            onPractice = appViewModel::openPractice,
            onHowToPlay = appViewModel::openHowToPlay,
            onRecords = appViewModel::openRecords,
            onPrivacyOptions = onPrivacyOptions,
        )

        AppScreen.Classic -> {
            LaunchedEffect(gameState.phase) {
                if (gameState.phase == GamePhase.Idle) {
                    gameViewModel.startNewGame()
                }
            }
            GameScreen(
                state = gameState,
                onSwipe = gameViewModel::onSwipe,
                onRestart = gameViewModel::restart,
                onStart = gameViewModel::begin,
                effects = gameViewModel.effects,
                gameOverAd = if (canRequestAds) {
                    { BannerAd(Modifier.fillMaxWidth()) }
                } else {
                    null
                },
                onExit = {
                    if (gameState.phase == GamePhase.GameOver) {
                        appViewModel.openHome()
                    } else {
                        showAbandonDialog = true
                    }
                },
            )
        }

        AppScreen.Practice -> {
            val practiceViewModel: PracticeViewModel = viewModel(
                key = "practice-${appState.practiceLaunchId}",
                factory = PracticeViewModel.factory(store, appState.onboardingFlow),
            )
            val practiceState by practiceViewModel.state.collectAsStateWithLifecycle()

            PracticeScreen(
                state = practiceState,
                onAnswer = practiceViewModel::answer,
                onSkip = practiceViewModel::skip,
                onNext = practiceViewModel::next,
                onRestart = practiceViewModel::restart,
                onExit = appViewModel::openHome,
                onPlayClassic = {
                    if (appState.activeSession != null) {
                        // Never throw away a saved Classic run from a Practice
                        // completion; let Home offer Resume or Discard.
                        appViewModel.openHome()
                    } else {
                        gameViewModel.startNewGame()
                        appViewModel.openClassic()
                    }
                },
            )
        }

        AppScreen.HowToPlay -> HowToPlayScreen(
            onboarding = appState.onboardingFlow,
            onBack = appViewModel::openHome,
            onStartPractice = {
                if (appState.onboardingFlow) {
                    appViewModel.openOnboardingPractice()
                } else {
                    appViewModel.openPractice()
                }
            },
            onPrivacyOptions = onPrivacyOptions,
        )

        AppScreen.Records -> MyRecordsScreen(
            records = appState.records,
            today = today,
            onBack = appViewModel::openHome,
        )
    }

    if (showResumeDialog && appState.activeSession != null) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text(stringResource(R.string.resume_dialog_title)) },
            text = { Text(stringResource(R.string.resume_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResumeDialog = false
                        appState.activeSession?.let { session ->
                            gameViewModel.resume(session)
                            appViewModel.openClassic()
                        }
                    },
                ) {
                    Text(stringResource(R.string.resume))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResumeDialog = false
                        appViewModel.discardActiveSession()
                        gameViewModel.startNewGame()
                        appViewModel.openClassic()
                    },
                ) {
                    Text(stringResource(R.string.start_new_run))
                }
            },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_dialog_title)) },
            text = { Text(stringResource(R.string.discard_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        appViewModel.discardActiveSession()
                    },
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text(stringResource(R.string.abandon_dialog_title)) },
            text = { Text(stringResource(R.string.abandon_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAbandonDialog = false
                        gameViewModel.abandon()
                        appViewModel.openHome()
                    },
                ) {
                    Text(stringResource(R.string.abandon))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimeBackgroundTop, PrimeBackground))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            color = PrimeTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            letterSpacing = 2.sp,
        )
    }
}

package com.companyname.primeornot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.companyname.primeornot.data.ActiveClassicSession
import com.companyname.primeornot.data.ClassicRecords
import com.companyname.primeornot.data.GameStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    Home,
    Classic,
    Practice,
    HowToPlay,
    Records,
}

data class AppUiState(
    val screen: AppScreen = AppScreen.Home,
    /** True while Practice is the second half of the first-time onboarding flow. */
    val onboardingFlow: Boolean = false,
    val records: ClassicRecords = ClassicRecords(),
    val recordsLoaded: Boolean = false,
    val activeSession: ActiveClassicSession? = null,
    /** Changes with every Practice entry so each session starts with a fresh deck. */
    val practiceLaunchId: Int = 0,
)

/** App-wide navigation, records and the first-launch onboarding decision. */
class AppViewModel(private val store: GameStore) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    private var onboardingEvaluated = false

    init {
        viewModelScope.launch { runCatching { store.migrateIfNeeded() } }

        viewModelScope.launch {
            store.records.collect { records ->
                _state.update { it.copy(records = records, recordsLoaded = true) }
                if (!onboardingEvaluated) {
                    onboardingEvaluated = true
                    if (!records.onboardingComplete) {
                        _state.update {
                            it.copy(
                                screen = AppScreen.HowToPlay,
                                onboardingFlow = true,
                            )
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            store.activeSession.collect { session ->
                _state.update { it.copy(activeSession = session) }
            }
        }
    }

    fun openClassic() {
        _state.update { it.copy(screen = AppScreen.Classic, onboardingFlow = false) }
    }

    fun openHome() {
        _state.update { it.copy(screen = AppScreen.Home, onboardingFlow = false) }
    }

    fun openHowToPlay() {
        _state.update { it.copy(screen = AppScreen.HowToPlay, onboardingFlow = false) }
    }

    /** Opens Practice from the main menu; never sets onboardingComplete. */
    fun openPractice() {
        _state.update {
            it.copy(
                screen = AppScreen.Practice,
                onboardingFlow = false,
                practiceLaunchId = it.practiceLaunchId + 1,
            )
        }
    }

    /** Opens the Practice half of the onboarding flow. */
    fun openOnboardingPractice() {
        _state.update {
            it.copy(
                screen = AppScreen.Practice,
                onboardingFlow = true,
                practiceLaunchId = it.practiceLaunchId + 1,
            )
        }
    }

    fun openRecords() {
        _state.update { it.copy(screen = AppScreen.Records, onboardingFlow = false) }
    }

    fun discardActiveSession() {
        viewModelScope.launch { store.clearActiveSession() }
    }

    companion object {
        fun factory(store: GameStore): ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(store) }
        }
    }
}

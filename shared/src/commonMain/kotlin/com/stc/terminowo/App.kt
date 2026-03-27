package com.stc.terminowo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.stc.terminowo.domain.repository.AppSettingsRepository
import com.stc.terminowo.presentation.consent.ConsentScreen
import com.stc.terminowo.presentation.navigation.NavGraph
import com.stc.terminowo.presentation.theme.TerminowoTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App() {
    TerminowoTheme(darkTheme = isSystemInDarkTheme()) {
        val appSettingsRepository: AppSettingsRepository = koinInject()
        val termsAccepted by appSettingsRepository.isTermsAccepted().collectAsState(initial = null)
        val scope = rememberCoroutineScope()

        when (termsAccepted) {
            null -> { /* Loading — show nothing while reading DB */ }
            false -> ConsentScreen(
                onAccepted = {
                    scope.launch { appSettingsRepository.setTermsAccepted(true) }
                }
            )
            true -> NavGraph()
        }
    }
}

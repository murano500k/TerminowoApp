package com.stc.terminowo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.stc.terminowo.data.local.db.DocumentDatabase
import com.stc.terminowo.domain.repository.AppSettingsRepository
import com.stc.terminowo.domain.usecase.ResyncRemindersUseCase
import com.stc.terminowo.platform.AppLogger
import com.stc.terminowo.platform.ErrorReporter
import com.stc.terminowo.presentation.consent.ConsentScreen
import com.stc.terminowo.presentation.error.StartupErrorScreen
import com.stc.terminowo.presentation.navigation.NavGraph
import com.stc.terminowo.presentation.theme.TerminowoTheme
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.koinInject

@Composable
fun App() {
    TerminowoTheme(darkTheme = isSystemInDarkTheme()) {
        val koin = getKoin()
        var attempt by remember { mutableIntStateOf(0) }
        val databaseError = remember(attempt) {
            runCatching { koin.get<DocumentDatabase>() }
                .onFailure {
                    AppLogger.e("App", "Database unavailable", it)
                    ErrorReporter.capture(it)
                }
                .exceptionOrNull()
        }
        if (databaseError != null) {
            StartupErrorScreen(error = databaseError, onRetry = { attempt++ })
            return@TerminowoTheme
        }

        val appSettingsRepository: AppSettingsRepository = koinInject()
        val termsAccepted by appSettingsRepository.isTermsAccepted().collectAsState(initial = null)
        val scope = rememberCoroutineScope()
        val resyncReminders: ResyncRemindersUseCase = koinInject()

        LaunchedEffect(Unit) {
            // Refresh scheduled notification text in the current device language
            try { resyncReminders() } catch (_: Exception) {}
        }

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

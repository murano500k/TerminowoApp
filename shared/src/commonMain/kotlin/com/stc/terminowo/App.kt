package com.stc.terminowo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.stc.terminowo.presentation.navigation.NavGraph
import com.stc.terminowo.presentation.theme.TerminowoTheme

@Composable
fun App() {
    TerminowoTheme(darkTheme = isSystemInDarkTheme()) {
        NavGraph()
    }
}

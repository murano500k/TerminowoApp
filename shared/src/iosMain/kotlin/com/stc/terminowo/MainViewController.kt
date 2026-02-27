package com.stc.terminowo

import androidx.compose.ui.window.ComposeUIViewController
import com.stc.terminowo.data.remote.ProxyConfig
import com.stc.terminowo.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun MainViewController() = ComposeUIViewController { App() }

fun initKoin(proxyUrl: String, apiKey: String) {
    startKoin {
        modules(
            appModules + module {
                single { ProxyConfig(url = proxyUrl, apiKey = apiKey) }
            }
        )
    }
}

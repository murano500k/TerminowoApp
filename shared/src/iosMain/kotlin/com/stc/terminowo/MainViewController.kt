package com.stc.terminowo

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.ui.window.ComposeUIViewController
import com.stc.terminowo.data.remote.ProxyConfig
import com.stc.terminowo.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSProcessInfo

@OptIn(ExperimentalComposeApi::class)
fun MainViewController() = ComposeUIViewController(
    configure = {
        // XCUITest isn't an accessibility service, so Compose would otherwise expose no elements to it
        if (NSProcessInfo.processInfo.arguments.contains("-uiTesting")) {
            accessibilitySyncOptions = AccessibilitySyncOptions.Always(debugLogger = null)
        }
    }
) { App() }

fun initKoin(proxyUrl: String, apiKey: String) {
    startKoin {
        modules(
            appModules + module {
                single { ProxyConfig(url = proxyUrl, apiKey = apiKey) }
            }
        )
    }
}

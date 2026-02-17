package com.stc.terminowo.android

import android.app.Application
import com.stc.terminowo.data.remote.DocumentAiConfig
import com.stc.terminowo.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

class TerminowoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@TerminowoApp)
            modules(
                appModules + module {
                    single {
                        DocumentAiConfig(
                            projectId = BuildConfig.DOCUMENT_AI_PROJECT_ID,
                            location = BuildConfig.DOCUMENT_AI_LOCATION,
                            processorId = BuildConfig.DOCUMENT_AI_PROCESSOR_ID,
                            apiKey = BuildConfig.DOCUMENT_AI_API_KEY
                        )
                    }
                }
            )
        }
    }
}

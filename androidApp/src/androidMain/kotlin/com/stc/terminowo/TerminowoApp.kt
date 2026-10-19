package com.stc.terminowo

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.stc.terminowo.data.remote.ProxyConfig
import com.stc.terminowo.di.appModules
import com.stc.terminowo.platform.ErrorReporter
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class TerminowoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startCrashReporting()

        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, "Terminowo", "$tag: $message", t)
            }

            override fun createStackElementTag(element: StackTraceElement): String {
                return "${element.className.substringAfterLast('.')}.${element.methodName}"
            }
        })

        FacebookSdk.fullyInitialize()
        AppEventsLogger.activateApp(this)

        startKoin {
            androidLogger()
            androidContext(this@TerminowoApp)
            modules(
                appModules + module {
                    single {
                        ProxyConfig(
                            url = BuildConfig.PROXY_URL,
                            apiKey = BuildConfig.PROXY_API_KEY
                        )
                    }
                }
            )
        }
    }

    private fun startCrashReporting() {
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            options.isSendDefaultPii = false
            options.tracesSampleRate = 0.1
        }
        ErrorReporter.sink = { Sentry.captureException(it) }
    }
}

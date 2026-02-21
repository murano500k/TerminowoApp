package com.stc.terminowo.android

import android.app.Application
import com.stc.terminowo.data.remote.ProxyConfig
import com.stc.terminowo.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class TerminowoApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, "Terminowo", "$tag: $message", t)
            }

            override fun createStackElementTag(element: StackTraceElement): String {
                return "${element.className.substringAfterLast('.')}.${element.methodName}"
            }
        })

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
}

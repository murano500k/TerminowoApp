package com.stc.terminowo.di

import com.stc.terminowo.data.local.DatabaseDriverFactory
import com.stc.terminowo.platform.ImageStorage
import com.stc.terminowo.platform.GoogleAuthProvider
import com.stc.terminowo.platform.NotificationPermissionHandler
import com.stc.terminowo.platform.NotificationScheduler
import com.stc.terminowo.platform.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { PlatformContext() }
    single { DatabaseDriverFactory() }
    single { ImageStorage() }
    single { NotificationScheduler() }
    single { NotificationPermissionHandler() }
    single { GoogleAuthProvider() }
}

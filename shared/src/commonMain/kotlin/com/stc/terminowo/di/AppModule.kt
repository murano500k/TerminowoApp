package com.stc.terminowo.di

import org.koin.core.module.Module

expect fun platformModule(): Module

val appModules: List<Module>
    get() = listOf(
        dataModule,
        domainModule,
        presentationModule,
        platformModule()
    )

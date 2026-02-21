package com.stc.terminowo.di

import com.stc.terminowo.domain.usecase.ScanDocumentUseCase
import com.stc.terminowo.domain.usecase.ScheduleRemindersUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { ScanDocumentUseCase(get()) }
    factory { ScheduleRemindersUseCase(get()) }
}

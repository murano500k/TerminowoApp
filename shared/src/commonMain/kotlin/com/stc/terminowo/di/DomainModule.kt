package com.stc.terminowo.di

import com.stc.terminowo.domain.usecase.DeleteDocumentUseCase
import com.stc.terminowo.domain.usecase.GetDocumentsUseCase
import com.stc.terminowo.domain.usecase.SaveDocumentUseCase
import com.stc.terminowo.domain.usecase.ScanDocumentUseCase
import com.stc.terminowo.domain.usecase.ScheduleRemindersUseCase
import com.stc.terminowo.domain.usecase.UpdateDocumentUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetDocumentsUseCase(get()) }
    factory { SaveDocumentUseCase(get()) }
    factory { UpdateDocumentUseCase(get()) }
    factory { DeleteDocumentUseCase(get()) }
    factory { ScanDocumentUseCase(get()) }
    factory { ScheduleRemindersUseCase(get()) }
}

package com.stc.terminowo.di

import com.stc.terminowo.presentation.detail.DetailViewModel
import com.stc.terminowo.presentation.main.MainViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::DetailViewModel)
}

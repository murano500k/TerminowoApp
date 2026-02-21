package com.stc.terminowo.di

import com.stc.terminowo.presentation.auth.AuthViewModel
import com.stc.terminowo.presentation.categories.CategoryListViewModel
import com.stc.terminowo.presentation.detail.DetailViewModel
import com.stc.terminowo.presentation.main.DocumentListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::DocumentListViewModel)
    viewModelOf(::CategoryListViewModel)
    viewModelOf(::DetailViewModel)
    viewModelOf(::AuthViewModel)
}

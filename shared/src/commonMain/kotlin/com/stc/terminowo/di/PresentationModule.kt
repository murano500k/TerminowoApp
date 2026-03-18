package com.stc.terminowo.di

import com.stc.terminowo.presentation.auth.AuthViewModel
import com.stc.terminowo.presentation.detail.DetailViewModel
import com.stc.terminowo.presentation.main.DocumentsViewModel
import com.stc.terminowo.presentation.notifications.NotificationsViewModel
import com.stc.terminowo.presentation.pulpit.DashboardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::DocumentsViewModel)
    viewModelOf(::DetailViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::NotificationsViewModel)
}

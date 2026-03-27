package com.stc.terminowo.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun isTermsAccepted(): Flow<Boolean>
    suspend fun setTermsAccepted(accepted: Boolean)
}

package com.stc.terminowo.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.stc.terminowo.data.local.db.DocumentDatabase
import com.stc.terminowo.domain.repository.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppSettingsRepositoryImpl(
    private val database: DocumentDatabase
) : AppSettingsRepository {

    companion object {
        private const val KEY_TERMS_ACCEPTED = "terms_accepted"
    }

    override fun isTermsAccepted(): Flow<Boolean> {
        return database.documentQueries.getSetting(KEY_TERMS_ACCEPTED)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it == "true" }
    }

    override suspend fun setTermsAccepted(accepted: Boolean) {
        withContext(Dispatchers.Default) {
            database.documentQueries.upsertSetting(KEY_TERMS_ACCEPTED, accepted.toString())
        }
    }
}

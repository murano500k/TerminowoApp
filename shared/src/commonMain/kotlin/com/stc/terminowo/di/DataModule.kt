package com.stc.terminowo.di

import com.stc.terminowo.data.local.DatabaseDriverFactory
import com.stc.terminowo.data.local.db.DocumentDatabase
import com.stc.terminowo.data.remote.DocumentAiMapper
import com.stc.terminowo.data.remote.DocumentAiService
import com.stc.terminowo.data.remote.ProxyConfig
import com.stc.terminowo.data.remote.UserProfileService
import com.stc.terminowo.data.repository.DocumentRepositoryImpl
import com.stc.terminowo.data.repository.OcrRepositoryImpl
import com.stc.terminowo.domain.repository.DocumentRepository
import com.stc.terminowo.domain.repository.OcrRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val dataModule = module {

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }
    }

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                level = LogLevel.HEADERS
                logger = object : Logger {
                    override fun log(message: String) {
                        com.stc.terminowo.platform.AppLogger.d("KtorHttp", message)
                    }
                }
            }
        }
    }

    single {
        val driverFactory: DatabaseDriverFactory = get()
        DocumentDatabase(driverFactory.createDriver())
    }

    single { DocumentAiMapper() }

    single { DocumentAiService(get(), get<ProxyConfig>()) }

    single<DocumentRepository> { DocumentRepositoryImpl(get()) }

    single<OcrRepository> { OcrRepositoryImpl(get(), get(), get()) }

    single { UserProfileService(get()) }
}

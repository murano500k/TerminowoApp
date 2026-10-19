package com.stc.terminowo.platform

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Hands an uncaught Kotlin exception to [onCrash] before the runtime aborts, so the native
 * crash reporter can attach the Kotlin type, message and stack trace to the SIGABRT report
 * (otherwise it only sees `abort()` with no exception details).
 */
private const val MAX_CRASH_STACK_TRACE_LENGTH = 3000

@OptIn(ExperimentalNativeApi::class)
fun installKotlinCrashHook(onCrash: (type: String, message: String, stackTrace: String) -> Unit) {
    setUnhandledExceptionHook { throwable ->
        try {
            // The crash-time scope must stay small (SentryCrash rejects oversized user info).
            val stackTrace = throwable.stackTraceToString()
                .substringBefore("    Suppressed:")
                .take(MAX_CRASH_STACK_TRACE_LENGTH)
            onCrash(throwable::class.qualifiedName ?: "Throwable", throwable.message.orEmpty().take(500), stackTrace)
        } catch (_: Throwable) {
        }
        terminateWithUnhandledException(throwable)
    }
}

/** Lets Swift receive [ErrorReporter] events as plain strings. */
fun installErrorReporterSink(sink: (type: String, message: String, stackTrace: String) -> Unit) {
    ErrorReporter.sink = { throwable ->
        sink(throwable::class.qualifiedName ?: "Throwable", throwable.message.orEmpty(), throwable.stackTraceToString())
    }
}

/** Debug helper: crashes the same way a failing coroutine on the main dispatcher does. */
@OptIn(DelicateCoroutinesApi::class)
fun triggerTestCrash() {
    GlobalScope.launch(Dispatchers.Main) {
        throw IllegalStateException("Sentry test crash from Kotlin")
    }
}

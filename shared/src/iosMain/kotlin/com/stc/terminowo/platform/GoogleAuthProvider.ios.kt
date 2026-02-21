package com.stc.terminowo.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class GoogleAuthProvider {
    companion object {
        var signInLauncher: (() -> Unit)? = null
        var onSignInResult: ((String?) -> Unit)? = null
    }

    private var cachedToken: String? = null

    actual fun clearToken() {
        cachedToken = null
    }

    actual suspend fun getAccessToken(): String {
        cachedToken?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            val launcher = signInLauncher
            if (launcher == null) {
                continuation.resumeWithException(
                    IllegalStateException("No signInLauncher registered. Ensure the iOS app has configured GoogleAuthProvider.")
                )
                return@suspendCancellableCoroutine
            }

            onSignInResult = { token ->
                if (token != null) {
                    cachedToken = token
                    continuation.resume(token)
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Google Sign-In failed or was cancelled")
                    )
                }
            }

            launcher()
        }
    }
}

package com.stc.terminowo.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class GoogleAuthProvider {
    companion object {
        /**
         * Callback for triggering Google Sign-In from the Swift side.
         * Swift sets this on app start to a closure that calls GIDSignIn.
         */
        var signInLauncher: (() -> Unit)? = null

        /**
         * Continuation callback for receiving the sign-in result.
         * Set by [getAccessToken] before calling [signInLauncher];
         * Swift calls this with the access token (or null on failure).
         */
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

package com.stc.terminowo.platform

import android.app.PendingIntent
import android.content.Context
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class GoogleAuthProvider(
    private val context: Context
) {
    companion object {
        /**
         * Callback for launching the consent intent from the Activity.
         * Set by MainActivity on creation, cleared on destruction.
         */
        var consentLauncher: ((IntentSenderRequest) -> Unit)? = null

        /**
         * Continuation callback for receiving the consent result.
         * Set by [getAccessToken] when consent is needed, called by MainActivity
         * after the user completes the consent flow.
         */
        var onConsentResult: ((Result<AuthorizationResult>) -> Unit)? = null
    }

    private var cachedToken: String? = null

    actual fun clearToken() {
        cachedToken = null
    }

    actual suspend fun getAccessToken(): String {
        cachedToken?.let { return it }

        return suspendCancellableCoroutine { continuation ->
            val scopes = listOf(
                Scope("https://www.googleapis.com/auth/cloud-platform"),
                Scope("openid"),
                Scope("https://www.googleapis.com/auth/userinfo.email"),
                Scope("https://www.googleapis.com/auth/userinfo.profile")
            )
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(scopes)
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        val pendingIntent: PendingIntent? = result.pendingIntent
                        val launcher = consentLauncher
                        if (pendingIntent != null && launcher != null) {
                            onConsentResult = { consentResult ->
                                consentResult.fold(
                                    onSuccess = { authResult ->
                                        val token = authResult.accessToken
                                        if (token != null) {
                                            cachedToken = token
                                            continuation.resume(token)
                                        } else {
                                            continuation.resumeWithException(
                                                IllegalStateException("Access token is null after consent")
                                            )
                                        }
                                    },
                                    onFailure = { e ->
                                        continuation.resumeWithException(e)
                                    }
                                )
                            }
                            launcher(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("No consent launcher registered. Ensure MainActivity is active.")
                            )
                        }
                    } else {
                        val token = result.accessToken
                        if (token != null) {
                            cachedToken = token
                            continuation.resume(token)
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("Access token is null")
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }
}

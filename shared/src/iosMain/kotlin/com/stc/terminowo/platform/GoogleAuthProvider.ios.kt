package com.stc.terminowo.platform

actual class GoogleAuthProvider {
    actual suspend fun getAccessToken(): String {
        throw NotImplementedError("iOS auth not implemented yet")
    }
}

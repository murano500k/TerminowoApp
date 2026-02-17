package com.stc.terminowo.platform

expect class GoogleAuthProvider {
    suspend fun getAccessToken(): String
}

package com.stc.terminowo.domain.model

data class UserProfile(
    val name: String,
    val email: String,
    val givenName: String? = null,
    val familyName: String? = null
) {
    val initials: String
        get() {
            val first = givenName?.firstOrNull() ?: name.firstOrNull()
            val last = familyName?.firstOrNull()
            return buildString {
                first?.uppercase()?.let { append(it) }
                last?.uppercase()?.let { append(it) }
            }.ifEmpty { "?" }
        }
}

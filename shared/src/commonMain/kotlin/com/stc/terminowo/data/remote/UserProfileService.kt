package com.stc.terminowo.data.remote

import com.stc.terminowo.domain.model.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class UserInfoResponse(
    val sub: String? = null,
    val name: String? = null,
    val email: String? = null,
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null
)

class UserProfileService(
    private val httpClient: HttpClient
) {
    suspend fun fetchProfile(accessToken: String): UserProfile {
        val response: UserInfoResponse = httpClient.get(USERINFO_URL) {
            header("Authorization", "Bearer $accessToken")
        }.body()

        return UserProfile(
            name = response.name ?: response.email ?: "Unknown",
            email = response.email ?: "",
            givenName = response.givenName,
            familyName = response.familyName
        )
    }

    companion object {
        private const val USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo"
    }
}

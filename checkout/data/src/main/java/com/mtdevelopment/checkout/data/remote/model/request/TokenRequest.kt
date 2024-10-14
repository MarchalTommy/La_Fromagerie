package com.mtdevelopment.checkout.data.remote.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("code")
    val authCode: String,
    @SerialName("grant_type")
    val grantType: GrantType,
    @SerialName("refresh_token")
    val refreshToken: String?
)

enum class GrantType(val value: String) {
    AUTH_CODE("authorization_code"), REFRESH_TOKEN("refresh_token")
}

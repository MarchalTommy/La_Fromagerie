package com.mtdevelopment.delivery.presentation.model

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val userName: String,
    val userAddress: String
)

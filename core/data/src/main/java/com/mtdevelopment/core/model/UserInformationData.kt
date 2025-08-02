package com.mtdevelopment.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserInformationData(
    val name: String,
    val address: String,
    val lastSelectedPath: String,
    val billingAddress: String
)

// Data to domain
fun UserInformationData.toUserInformation() = UserInformation(
    name = name,
    address = address,
    lastSelectedPath = lastSelectedPath,
    billingAddress = billingAddress
)

// Domain to data
fun UserInformation.toUserInformationData() = UserInformationData(
    name = name,
    address = address,
    lastSelectedPath = lastSelectedPath,
    billingAddress = billingAddress
)

package com.mtdevelopment.admin.domain.repository

import android.net.Uri

interface CloudinaryRepository {

    suspend fun uploadImageToCloudinary(
        imageUri: Uri
    ): Result<String>

    suspend fun saveImageUrlToFirestore(
        cloudinaryUrl: String,
        productId: String,
        collectionPath: String
    ): Result<Unit>

}
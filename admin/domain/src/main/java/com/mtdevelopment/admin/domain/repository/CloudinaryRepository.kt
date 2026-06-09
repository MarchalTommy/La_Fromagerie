package com.mtdevelopment.admin.domain.repository

import android.net.Uri

/**
 * Repository interface for handling image uploads to Cloudinary and saving the resulting URLs to Firestore.
 */
interface CloudinaryRepository {

    /**
     * Uploads an image from a local URI to Cloudinary.
     * @param imageUri The local URI of the image to upload.
     * @return Result containing the Cloudinary URL of the uploaded image if successful.
     */
    suspend fun uploadImageToCloudinary(
        imageUri: Uri
    ): Result<String>

    /**
     * Saves a Cloudinary image URL to a specific product document in Firestore.
     * @param cloudinaryUrl The URL of the image on Cloudinary.
     * @param productId The ID of the product to associate the image with.
     * @param collectionPath The Firestore collection path where the product is stored.
     * @return Result indicating success or failure.
     */
    suspend fun saveImageUrlToFirestore(
        cloudinaryUrl: String,
        productId: String,
        collectionPath: String
    ): Result<Unit>

}
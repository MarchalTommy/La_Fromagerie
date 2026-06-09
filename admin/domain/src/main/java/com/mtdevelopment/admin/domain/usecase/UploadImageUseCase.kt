package com.mtdevelopment.admin.domain.usecase

import android.net.Uri
import com.mtdevelopment.admin.domain.repository.CloudinaryRepository

/**
 * Use case to upload an image to Cloudinary.
 */
class UploadImageUseCase(
    private val cloudinaryRepository: CloudinaryRepository
) {
    /**
     * Executes the use case.
     * @param imageUri The local URI of the image to upload.
     * @param onResult Callback invoked with the result containing the Cloudinary URL or an exception.
     */
    suspend operator fun invoke(
        imageUri: Uri,
        onResult: (Result<String>) -> Unit
    ) {
        val uploadResult = cloudinaryRepository.uploadImageToCloudinary(imageUri)

        if (uploadResult.isSuccess) {

            onResult.invoke(uploadResult)
        } else {
            // L'upload Cloudinary a échoué
            onResult(
                Result.failure(
                    uploadResult.exceptionOrNull() ?: Exception("Cloudinary upload failed")
                )
            )
        }
    }
}
package com.mtdevelopment.admin.domain.usecase

import android.net.Uri
import com.mtdevelopment.admin.domain.repository.CloudinaryRepository

class UploadImageUseCase(
    private val cloudinaryRepository: CloudinaryRepository
) {
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
package com.mtdevelopment.admin.data.repository

import com.cloudinary.Cloudinary
import com.mtdevelopment.admin.data.BuildConfig
import com.mtdevelopment.admin.domain.repository.SignatureProvider

/**
 * Implementation of [SignatureProvider] that generates signatures locally using BuildConfig keys.
 * WARNING: This is less secure than backend generation and should only be used as a fallback.
 */
class LocalSignatureProvider : SignatureProvider {
    override fun generateCloudinarySignature(params: Map<String, Any>): String {
        return Cloudinary().apiSignRequest(
            params,
            BuildConfig.CLOUDINARY_PRIVATE,
            1
        )
    }
}

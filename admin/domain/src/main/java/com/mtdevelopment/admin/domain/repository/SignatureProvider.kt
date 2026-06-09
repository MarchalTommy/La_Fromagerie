package com.mtdevelopment.admin.domain.repository

/**
 * Interface for providing secure signatures for remote services.
 * This abstraction allows swapping local generation with remote API calls.
 */
interface SignatureProvider {
    /**
     * Generates a signature for Cloudinary upload.
     * @param params The parameters to sign.
     * @return The generated signature string.
     */
    fun generateCloudinarySignature(params: Map<String, Any>): String
}

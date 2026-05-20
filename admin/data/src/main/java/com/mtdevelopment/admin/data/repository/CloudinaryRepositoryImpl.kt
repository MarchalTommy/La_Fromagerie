package com.mtdevelopment.admin.data.repository

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.mtdevelopment.admin.data.BuildConfig
import com.mtdevelopment.admin.data.source.FirestoreAdminDatasource
import com.mtdevelopment.admin.domain.repository.CloudinaryRepository
import com.mtdevelopment.admin.domain.repository.SignatureProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementation of [CloudinaryRepository] using the Cloudinary Android SDK.
 * This class handles secure image uploads by generating a client-side signature.
 */
class CloudinaryRepositoryImpl(
    private val firestoreAdminDatasource: FirestoreAdminDatasource,
    private val signatureProvider: SignatureProvider
) : CloudinaryRepository {

    /**
     * Uploads an image to Cloudinary using [MediaManager].
     * The process involves:
     * 1. Generating a signature using the provided [SignatureProvider].
     * 2. Configuring [MediaManager] with options.
     * 3. Handling the upload lifecycle via [UploadCallback].
     * 4. Resuming the coroutine with the resulting image URL.
     */
    override suspend fun uploadImageToCloudinary(
        imageUri: Uri
    ): Result<String> = suspendCancellableCoroutine { continuation ->

        Log.d("CloudinaryRepo", "Attempting to upload URI: $imageUri")

        val timestamp = System.currentTimeMillis() / 1000

        val paramsToSign = mutableMapOf<String, Any>(
            "timestamp" to timestamp
        )

        // Delegate signature generation to the provider (can be local or remote)
        val signature = signatureProvider.generateCloudinarySignature(paramsToSign)

        val options = mutableMapOf<String, Any>(
            "resource_type" to "image",
            "signature" to signature,
            "timestamp" to timestamp,
            "api_key" to BuildConfig.CLOUDINARY_PUBLIC
        )

        // Requesting upload via MediaManager
        val requestId = MediaManager.get().upload(imageUri)
            .options(options)
            .option("resource_type", "image") // Best practice: specify resource type
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d("CloudinaryRepo", "Upload started ($requestId)")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    val progress =
                        if (totalBytes > 0) ((bytes.toDouble() / totalBytes.toDouble()) * 100).toInt() else 0
                    Log.d("CloudinaryRepo", "Upload progress ($requestId): $progress%")
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val secureUrl = resultData?.get("secure_url") as? String
                    val url = resultData?.get("url") as? String

                    if (continuation.isActive) {
                        if (secureUrl != null) {
                            Log.i(
                                "CloudinaryRepo",
                                "Upload Success ($requestId). Secure URL: $secureUrl"
                            )
                            continuation.resume(Result.success(secureUrl))
                        } else if (url != null) {
                            Log.w(
                                "CloudinaryRepo",
                                "Upload Success ($requestId) but using non-secure URL: $url"
                            )
                            continuation.resume(Result.success(url))
                        } else {
                            Log.e(
                                "CloudinaryRepo",
                                "Upload Success ($requestId) but URL not found in result."
                            )
                            continuation.resume(Result.failure(Exception("URL not found in Cloudinary response")))
                        }
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e(
                        "CloudinaryRepo",
                        "Upload Error ($requestId): Code=${error?.code}, Desc=${error?.description}"
                    )
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Cloudinary upload error: ${error?.description} (${error?.code})")))
                    }
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    // Upload rescheduled (e.g., network loss), managed by SDK
                    Log.w(
                        "CloudinaryRepo",
                        "Upload Rescheduled ($requestId): ${error?.description}"
                    )
                }
            })
            .dispatch()

        // Handle coroutine cancellation by cancelling the Cloudinary upload request
        continuation.invokeOnCancellation {
            Log.d("CloudinaryRepo", "Coroutine cancelled, cancelling Cloudinary request $requestId")
            MediaManager.get().cancelRequest(requestId)
        }
    }

    /**
     * Saves the resulting Cloudinary URL to Firestore for the specified product.
     */
    override suspend fun saveImageUrlToFirestore(
        cloudinaryUrl: String,
        productId: String,
        collectionPath: String
    ): Result<Unit> {
        return firestoreAdminDatasource.saveImageUrlToFirestore(
            cloudinaryUrl,
            productId,
            collectionPath
        )
    }
}

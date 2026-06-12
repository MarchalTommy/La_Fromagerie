package com.mtdevelopment.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.compose.currentKoinScope
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Triggers a standard haptic click feedback on the device.
 * Adapts to different Android API levels for consistent feel.
 */
fun vibratePhoneClick(context: Context) {
    if (Build.VERSION.SDK_INT < 34) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 30) {
            vibrator.vibrate(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK).compose()
            )
        } else {
            vibrator.vibrate(50)
        }
    } else {
        val vibratorManager: VibratorManager by lazy {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        }

        vibratorManager.vibrate(
            CombinedVibration.createParallel(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK).compose()
            )
        )
    }
}

/**
 * Triggers a strong haptic feedback (heavy click), usually for significant actions like deletions.
 */
fun vibratePhoneClickBig(context: Context) {
    if (Build.VERSION.SDK_INT < 34) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 29) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else {
            vibrator.vibrate(100)
        }
    } else {
        val vibratorManager: VibratorManager by lazy {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        }

        vibratorManager.vibrate(
            CombinedVibration.createParallel(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        )
    }
}

/**
 * Helper to create a [NavType] for serializable objects using Kotlin Serialization.
 * Allows passing complex data classes as navigation arguments.
 */
inline fun <reified T : Any> serializableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
    override fun get(bundle: Bundle, key: String) =
        bundle.getString(key)?.let<String, T>(json::decodeFromString)

    override fun parseValue(value: String): T = json.decodeFromString(value)

    override fun serializeAsValue(value: T): String = json.encodeToString(value)

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, json.encodeToString(value))
    }
}

/**
 * Composable utility to inject a Koin ViewModel directly into a Composable.
 */
@Composable
inline fun <reified T : ViewModel> koinViewModel(): T {
    val scope = currentKoinScope()
    return viewModel {
        scope.get<T>()
    }
}

/**
 * Utility for compressing images before upload.
 * It handles:
 * 1. Resizing based on megapixel limits.
 * 2. Intelligent downsampling to save memory.
 * 3. EXIF orientation correction (rotation).
 * 4. Iterative quality compression to meet file size limits.
 */
object ImageCompressor {

    private const val MAX_MEGAPIXELS = 25.0
    private const val MAX_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB
    private const val MIN_COMPRESSION_QUALITY = 50 

    /**
     * Compresses an image from a given Uri to meet resolution and size constraints.
     * Runs operations on Dispatchers.IO.
     *
     * @param context Context.
     * @param imageUri Uri of the original image.
     * @return Uri of the compressed temporary file, or null if compression failed.
     */
    suspend fun compressImage(context: Context, imageUri: Uri): Uri? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var tempFile: File? = null
        var bitmap: Bitmap?
        val rotatedBitmap: Bitmap?

        try {
            // --- 1. Read dimensions without loading into memory ---
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e("ImageCompressor", "Invalid image dimensions for URI: $imageUri")
                return@withContext null
            }
            val originalMegapixels = (originalWidth * originalHeight) / 1_000_000.0

            // --- 2. Calculate scale factor ---
            var targetWidth = originalWidth
            var targetHeight = originalHeight
            if (originalMegapixels > MAX_MEGAPIXELS) {
                val scaleFactor = sqrt(MAX_MEGAPIXELS / originalMegapixels)
                targetWidth = (originalWidth * scaleFactor).roundToInt()
                targetHeight = (originalHeight * scaleFactor).roundToInt()
            }

            // --- 3. Calculate inSampleSize for efficient decoding ---
            options.inSampleSize =
                calculateInSampleSize(originalWidth, originalHeight, targetWidth, targetHeight)

            // --- 4. Decode the Bitmap ---
            options.inJustDecodeBounds = false
            inputStream = context.contentResolver.openInputStream(imageUri) 
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmap == null) {
                return@withContext null
            }


            // --- 5. Precise scaling if necessary ---
            if (bitmap.width > targetWidth || bitmap.height > targetHeight) {
                val scaledBitmap =
                    bitmap.scale(targetWidth, targetHeight)
                if (scaledBitmap != bitmap) {
                    bitmap.recycle() 
                }
                bitmap = scaledBitmap
            }


            // --- 6. Handle EXIF Orientation ---
            inputStream =
                context.contentResolver.openInputStream(imageUri) 
            val orientation =
                inputStream?.use { ExifUtil.getOrientation(it) } ?: ExifInterface.ORIENTATION_NORMAL
            inputStream?.close()

            if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                rotatedBitmap = ExifUtil.rotateBitmap(
                    bitmap,
                    orientation
                )
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle() 
                    bitmap = rotatedBitmap
                }
            }


            // --- 7. Iterative compression to target size ---
            val byteStream = ByteArrayOutputStream()
            var currentQuality = 95 
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.JPEG
            }

            do {
                byteStream.reset()
                bitmap.compress(format, currentQuality, byteStream)
                val size = byteStream.size()

                if (size <= MAX_SIZE_BYTES) {
                    break 
                }
                currentQuality -= 5 
            } while (currentQuality >= MIN_COMPRESSION_QUALITY)

            // --- 8. Save to temporary file ---
            val outputDir = context.cacheDir 
            val extension = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                when (format) {
                    Bitmap.CompressFormat.WEBP_LOSSY -> ".webp"
                    else -> ".jpg"
                }
            } else {
                ".jpg"
            }
            tempFile = File.createTempFile("compressed_", extension, outputDir)
            outputStream = FileOutputStream(tempFile)
            outputStream.write(byteStream.toByteArray())

            // --- 9. Provide URI via FileProvider ---
            val authority = "${context.packageName}.provider"
            val tempUri = FileProvider.getUriForFile(context, authority, tempFile)

            return@withContext tempUri 

        } catch (e: Exception) {
            Log.e("ImageCompressor", "Error during image compression", e)
            try {
                tempFile?.takeIf { it.exists() }?.delete()
            } catch (cleanEx: Exception) {
                Log.w("ImageCompressor", "Could not delete temp file after failure", cleanEx)
            }
            return@withContext null 
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (e: Exception) {
                Log.w("ImageCompressor", "Could not close streams", e)
            }
        }
    }

    private fun calculateInSampleSize(
        currentWidth: Int,
        currentHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (currentHeight > reqHeight || currentWidth > reqWidth) {
            val halfHeight: Int = currentHeight / 2
            val halfWidth: Int = currentWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        if (currentWidth / inSampleSize < reqWidth || currentHeight / inSampleSize < reqHeight) {
            if (inSampleSize > 1) inSampleSize /= 2 
        }

        return max(1, inSampleSize) 
    }
}

/**
 * Utility for reading EXIF data and rotating bitmaps accordingly.
 */
object ExifUtil {
    fun getOrientation(inputStream: InputStream): Int {
        return try {
            val exifInterface = ExifInterface(inputStream)
            exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            Log.e("ExifUtil", "Could not read EXIF orientation", e)
            ExifInterface.ORIENTATION_UNDEFINED
        }
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> return bitmap
            else -> return bitmap
        }

        return try {
            val rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            rotatedBitmap
        } catch (e: OutOfMemoryError) {
            Log.e("ExifUtil", "OOM while rotating bitmap", e)
            bitmap 
        } catch (e: Exception) {
            Log.e("ExifUtil", "Error rotating bitmap", e)
            bitmap 
        }
    }
}
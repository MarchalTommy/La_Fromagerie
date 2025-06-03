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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.currentKoinScope
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

/*
Thanks to this article for that method :
https://medium.com/mercadona-tech/type-safety-in-navigation-compose-23c03e3d74a5
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

@Composable
inline fun <reified T : ViewModel> koinViewModel(): T {
    val scope = currentKoinScope()
    return viewModel {
        scope.get<T>()
    }
}

object ImageCompressor {

    private const val MAX_MEGAPIXELS = 25.0
    private const val MAX_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB
    private const val MIN_COMPRESSION_QUALITY = 50 // Qualité minimale JPEG/WEBP

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
        val rotatedBitmap: Bitmap?  // Pour gérer la rotation

        try {
            // --- 1. Lire les dimensions et le type MIME sans charger l'image ---
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            inputStream = context.contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close() // Fermer le premier flux

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e("ImageCompressor", "Invalid image dimensions for URI: $imageUri")
                return@withContext null
            }
            val originalMegapixels = (originalWidth * originalHeight) / 1_000_000.0
            Log.d(
                "ImageCompressor",
                "Original dims: ${originalWidth}x${originalHeight} (${
                    String.format(
                        "%.2f",
                        originalMegapixels
                    )
                } MPx)"
            )

            // --- 2. Calculer le facteur de scale et les dimensions cibles ---
            var targetWidth = originalWidth
            var targetHeight = originalHeight
            if (originalMegapixels > MAX_MEGAPIXELS) {
                val scaleFactor = sqrt(MAX_MEGAPIXELS / originalMegapixels)
                targetWidth = (originalWidth * scaleFactor).roundToInt()
                targetHeight = (originalHeight * scaleFactor).roundToInt()
                Log.d(
                    "ImageCompressor",
                    "Scaling needed. Target dims: ${targetWidth}x$targetHeight"
                )
            } else {
                Log.d("ImageCompressor", "No scaling needed based on megapixels.")
            }

            // --- 3. Calculer inSampleSize pour économiser la mémoire au décodage ---
            options.inSampleSize =
                calculateInSampleSize(originalWidth, originalHeight, targetWidth, targetHeight)
            Log.d("ImageCompressor", "Using inSampleSize: ${options.inSampleSize}")

            // --- 4. Décoder le Bitmap (potentiellement sous-échantillonné) ---
            options.inJustDecodeBounds = false
            // Note: On API 21+, BitmapFactory peut souvent réutiliser le Bitmap si mutable
            // options.inMutable = true
            inputStream = context.contentResolver.openInputStream(imageUri) // Réouvrir le flux
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close() // Fermer le flux

            if (bitmap == null) {
                Log.e("ImageCompressor", "Failed to decode bitmap for URI: $imageUri")
                return@withContext null
            }
            Log.d("ImageCompressor", "Decoded bitmap size: ${bitmap.width}x${bitmap.height}")


            // --- 5. Redimensionner précisément si nécessaire ---
            // (Si inSampleSize n'a pas donné la taille exacte ou si on réduisait sans inSampleSize > 1)
            if (bitmap.width > targetWidth || bitmap.height > targetHeight) {
                Log.d(
                    "ImageCompressor",
                    "Performing precise scaling to ${targetWidth}x$targetHeight"
                )
                val scaledBitmap =
                    bitmap.scale(targetWidth, targetHeight)
                if (scaledBitmap != bitmap) { // createScaledBitmap peut retourner l'original si pas de scaling
                    bitmap.recycle() // Libérer l'ancien bitmap
                }
                bitmap = scaledBitmap
            }


            // --- 6. Gérer l'Orientation EXIF ---
            inputStream =
                context.contentResolver.openInputStream(imageUri) // Encore une fois pour EXIF
            val orientation =
                inputStream?.use { ExifUtil.getOrientation(it) } ?: ExifInterface.ORIENTATION_NORMAL
            inputStream?.close()

            if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                Log.d("ImageCompressor", "Applying EXIF orientation: $orientation")
                rotatedBitmap = ExifUtil.rotateBitmap(
                    bitmap,
                    orientation
                ) // Utilise la fonction helper ci-dessous
                if (rotatedBitmap != bitmap) { // Si une rotation a eu lieu
                    bitmap.recycle() // Libérer l'ancien
                    bitmap = rotatedBitmap
                }
            } else {
                Log.d("ImageCompressor", "No EXIF rotation needed.")
            }


            // --- 7. Compresser en ajustant la qualité pour la taille ---
            val byteStream = ByteArrayOutputStream()
            var currentQuality = 95 // Commencer avec une haute qualité
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.JPEG
            }

            do {
                byteStream.reset() // Vider le flux avant chaque tentative
                Log.d(
                    "ImageCompressor",
                    "Compressing with format $format, quality $currentQuality..."
                )
                bitmap.compress(format, currentQuality, byteStream)
                val size = byteStream.size()
                Log.d("ImageCompressor", "Compressed size: ${size / 1024} KB")

                if (size <= MAX_SIZE_BYTES) {
                    break // Taille OK
                }
                currentQuality -= 5 // Réduire la qualité
            } while (currentQuality >= MIN_COMPRESSION_QUALITY)

            if (byteStream.size() > MAX_SIZE_BYTES) {
                Log.w(
                    "ImageCompressor",
                    "Could not compress below ${MAX_SIZE_BYTES / 1024 / 1024}MB even at quality $MIN_COMPRESSION_QUALITY. Using result anyway."
                )
            }

            // --- 8. Sauvegarder dans un fichier temporaire ---
            val outputDir = context.cacheDir // Utiliser le dossier cache
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
            outputStream.write(byteStream.toByteArray()) // Écrire les données compressées
            Log.d("ImageCompressor", "Compressed file saved to: ${tempFile.absolutePath}")

            // --- 9. Obtenir l'URI via FileProvider ---
            // !! Nécessite la configuration de FileProvider dans AndroidManifest.xml !!
            // et un fichier res/xml/provider_paths.xml
            val authority = "${context.packageName}.provider"
            val tempUri = FileProvider.getUriForFile(context, authority, tempFile)
            Log.d("ImageCompressor", "Compressed file URI: $tempUri")

            return@withContext tempUri // Succès !

        } catch (e: Exception) {
            Log.e("ImageCompressor", "Error during image compression", e)
            // Tenter de supprimer le fichier temporaire en cas d'erreur
            try {
                tempFile?.takeIf { it.exists() }?.delete()
            } catch (cleanEx: Exception) {
                Log.e("ImageCompressor", "Error cleaning up temp file", cleanEx)
            }
            return@withContext null // Échec
        } finally {
            // --- 10. Nettoyage ---
            try {
                inputStream?.close()
                outputStream?.close()
                // Le bitmap original est recyclé lors du scaling ou de la rotation si nécessaire.
                // Le dernier bitmap en usage (bitmap ou rotatedBitmap) n'a pas besoin d'être recyclé ici
                // car il est géré par le système une fois qu'il n'est plus référencé.
                // Explicitement appeler bitmap.recycle() ici pourrait causer des problèmes si l'URI
                // est utilisée immédiatement. Laissons le GC faire son travail.
            } catch (e: Exception) {
                Log.e("ImageCompressor", "Error closing streams", e)
            }
        }
    }

    // Fonction helper pour calculer inSampleSize
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
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than or equal to the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        // S'assurer qu'on ne réduit pas trop si une seule dimension est très grande
        if (currentWidth / inSampleSize < reqWidth || currentHeight / inSampleSize < reqHeight) {
            // Peut nécessiter un ajustement plus fin, mais cette base est standard
            if (inSampleSize > 1) inSampleSize /= 2 // Revenir en arrière si on a trop réduit
        }

        return max(1, inSampleSize) // Assurer au moins 1
    }
}

object ExifUtil {
    fun getOrientation(inputStream: InputStream): Int {
        return try {
            // Utiliser androidx.exifinterface
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
            Log.d("ExifUtil", "Bitmap rotated successfully for orientation $orientation")
            // Ne recyclez pas l'original ici si createBitmap retourne la même instance (pas de rotation)
            // Le code appelant gérera le recyclage si une nouvelle instance est créée
            rotatedBitmap
        } catch (e: OutOfMemoryError) {
            Log.e("ExifUtil", "OOM while rotating bitmap", e)
            bitmap // Retourner l'original en cas d'erreur mémoire
        } catch (e: Exception) {
            Log.e("ExifUtil", "Error rotating bitmap", e)
            bitmap // Retourner l'original en cas d'autre erreur
        }
    }
}
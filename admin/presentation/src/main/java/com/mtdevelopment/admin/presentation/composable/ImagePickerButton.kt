package com.mtdevelopment.admin.presentation.composable

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.mtdevelopment.admin.presentation.R
import com.mtdevelopment.core.util.ImageCompressor
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Composable
fun ImagePickerButton(
    existingImageUri: Uri? = null,
    onImagePicked: (Uri) -> Unit = {},
    shouldShowLoading: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {}
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(existingImageUri) }
    var tempNewImageUri by remember { mutableStateOf<Uri>("".toUri()) }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            tempNewImageUri = uri
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    LaunchedEffect(
        tempNewImageUri
    ) {
        if (tempNewImageUri != "".toUri()) {
            // Lancer une coroutine pour la compression
            coroutineScope.launch { // Ou rememberCoroutineScope().launch
                Log.d("ImageProcessing", "Starting compression for $tempNewImageUri")
                // Mettre à jour l'UI pour indiquer le traitement
                shouldShowLoading.invoke(true)

                val compressedUri = ImageCompressor.compressImage(context, tempNewImageUri)

                // Mettre à jour l'UI
                shouldShowLoading.invoke(false)

                if (compressedUri != null) {
                    Log.d("ImageProcessing", "Compression successful: $compressedUri")
                    selectedImageUri = compressedUri
                    onImagePicked.invoke(compressedUri)
                } else {
                    Log.e("ImageProcessing", "Compression failed.")
                    onError.invoke("Une erreur est survenue pendant la compression de l'image")
                }
            }
        }
    }

    Column {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = {
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(id = R.drawable.image_24px),
                    contentDescription = "Add Image",
                    tint = MaterialTheme.colorScheme.onPrimary
                )

                Text(
                    text = if (selectedImageUri == null || selectedImageUri == "".toUri()) "Ajouter une image" else "Modifier l'image",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }


        selectedImageUri?.let { uri ->
            if (selectedImageUri != "".toUri()) {
                GlideImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .size(250.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    imageModel = {
                        uri
                    },
                    imageOptions = ImageOptions(contentScale = ContentScale.FillWidth),
                    requestBuilder = {
                        Glide.with(LocalContext.current)
                            .asBitmap()
                            .apply(
                                RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                            ).thumbnail(0.6f)
                            .transition(withCrossFade())
                    },
                    loading = {
                        Box(modifier = Modifier.matchParentSize()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    failure = {
                        Text(text = "image request failed.")
                    })
            }
        }
    }
}
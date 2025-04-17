package com.mtdevelopment.core.presentation.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mtdevelopment.core.presentation.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Preview
@Composable
fun ErrorOverlay(
    isShown: Boolean = true,
    duration: Long? = null,
    message: String? = null,
    onDismiss: () -> Unit = {}
) {

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.error_lottie))
    val progress by animateLottieCompositionAsState(
        composition,
        speed = 1f
    )

    val scope = rememberCoroutineScope()

    LaunchedEffect(duration) {
        if (duration != null) {
            scope.launch {
                delay(duration)
                onDismiss.invoke()
            }
        }
    }

    if (isShown) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.TopCenter
        ) {
            LottieAnimation(
                modifier = Modifier.padding(top = 128.dp),
                composition = composition,
                progress = { progress },
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 64.dp, top = 256.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    text =
                        message
                            ?: "Une erreur semble être survenue, merci de réessayer.\nSi le problème persiste, merci de contacter l'équipe depuis notre site !",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Button(
                    modifier = Modifier
                        .padding(top = 32.dp, bottom = 16.dp),
                    onClick = {
                        onDismiss.invoke()
                    },
                    colors = ButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "D'accord",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

}
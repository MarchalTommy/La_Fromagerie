package com.mtdevelopment.checkout.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mtdevelopment.checkout.presentation.R
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.util.rememberScreenSize
import org.koin.androidx.compose.koinViewModel

@Composable
fun AfterPaymentScreen(
    clientName: String?,
    onHomeClick: () -> Unit,
) {

    val viewModel = koinViewModel<CheckoutViewModel>()

    LaunchedEffect(Unit) {
        viewModel.resetAppStateAfterSuccess()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center
            ) {
                SuccessAnim()

                Text(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .offset(y = 72.dp),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    text = "Merci pour votre achat\n${clientName} !"
                )
            }

        }

        Text(
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            text = "Vous recevrez votre ticket par email dans les prochaines minutes"
        )

        Text(
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            text = "Merci de le conserver, il ne pourra pas vous être dupliqué"
        )

        Button(
            modifier = Modifier.padding(32.dp),
            onClick = {
                onHomeClick.invoke()
            }
        ) {
            Text(text = "Revenir au catalogue")
        }
    }
}

@Composable
fun SuccessAnim() {

    val screenSize = rememberScreenSize()
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.payment_success_lottie))
    val progress by animateLottieCompositionAsState(composition)
    LottieAnimation(
        modifier = Modifier
            .height(screenSize.height / 3)
            .offset(y = (-52).dp),
        composition = composition,
        progress = { progress },
    )
}
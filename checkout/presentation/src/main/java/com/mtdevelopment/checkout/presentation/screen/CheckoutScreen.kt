package com.mtdevelopment.checkout.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.google.pay.button.PayButton
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CheckoutScreen(
    onGooglePayButtonClick: () -> Unit = {}
) {

    val checkoutViewModel = koinViewModel<CheckoutViewModel>()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {

        PayButton(
            modifier = Modifier
                .testTag("payButton")
                .fillMaxWidth(),
            onClick = onGooglePayButtonClick,
            allowedPaymentMethods = checkoutViewModel.allowedPaymentMethods
        )

    }

}
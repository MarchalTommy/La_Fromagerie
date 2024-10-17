package com.mtdevelopment.checkout.presentation.screen

import android.content.ContentValues.TAG
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.google.pay.button.PayButton
import com.mapbox.maps.logE
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.util.toUiPrice
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun CheckoutScreen(
    onGooglePayButtonClick: (priceCents: Long) -> Unit = {}
) {

    val checkoutViewModel = koinViewModel<CheckoutViewModel>()
    val uiData = checkoutViewModel.checkoutScreenObject.collectAsState()

    fun onPricingError() {
        logE(TAG, "Pricing error")
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Confirmer la commande avec Google Pay")
        uiData.value?.buyerName?.let { Text(it) }
        Text("Pour la date du :")
        uiData.value?.deliveryDate?.let {
            Text(
                SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(
                    it
                )
            )
        }
        Text("A votre adresse :")
        uiData.value?.buyerAddress?.let { Text(it) }
        Text("Pour un prix total de :")
        uiData.value?.totalPrice?.let { Text(it.toUiPrice()) }
        Text("Confirmer la commande avec Google Pay")

        PayButton(
            modifier = Modifier
                .testTag("payButton")
                .fillMaxWidth(),
            onClick = {
                uiData.value?.totalPrice?.let { onGooglePayButtonClick.invoke(it) } ?: run {
                    onPricingError()
                }
            },
            allowedPaymentMethods = checkoutViewModel.allowedPaymentMethods
        )

    }
}
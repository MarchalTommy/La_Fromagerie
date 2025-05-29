package com.mtdevelopment.checkout.presentation.screen

import android.app.Activity.RESULT_OK
import android.content.ContentValues.TAG
import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.pay.button.PayButton
import com.mtdevelopment.checkout.presentation.BuildConfig
import com.mtdevelopment.checkout.presentation.composable.UserInfoFormComposable
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.domain.toStringPrice
import com.mtdevelopment.core.presentation.composable.ErrorOverlay
import com.mtdevelopment.core.presentation.composable.RiveAnimation
import com.mtdevelopment.core.util.ScreenSize
import com.mtdevelopment.core.util.rememberScreenSize
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun CheckoutScreen(
    onNavigatePaymentSuccess: (String) -> Unit
) {

    val screenSize: ScreenSize = rememberScreenSize()
    val checkoutViewModel = koinViewModel<CheckoutViewModel>()
    val uiData = checkoutViewModel.paymentScreenState.collectAsState()

    val googlePayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val isSuccess = when (activityResult.resultCode) {
            RESULT_OK -> {
                activityResult.data.let {
                    true
                }
            }

            else -> {
                false
            }
            //CommonStatusCodes.CANCELED -> The user canceled
            //CommonStatusCodes.DEVELOPER_ERROR -> The API returned an error (it.status: Status)
            //else -> Handle internal and other unexpected errors
        }
        if (isSuccess) {
            checkoutViewModel.setGooglePaySuccess(true)
        } else {
            checkoutViewModel.setGooglePaySuccess(false)
        }
    }

    fun onPricingError() {
        Log.e(TAG, "Pricing error")
    }

    if (uiData.value.isPaymentSuccess) {
        onNavigatePaymentSuccess.invoke(uiData.value.buyerName ?: "")
    }

    LaunchedEffect(Unit) {
        if (uiData.value.isPaymentSuccess.not()) {
            checkoutViewModel.updateUiState()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Card(
            modifier = Modifier
                .heightIn(min = 0.dp, max = (screenSize.height / 5) * 2)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusable(true),
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surfaceContainer),
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.secondaryContainer)
            ),
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                text = "Votre commande",
                style = MaterialTheme.typography.titleLarge
            )
            LazyColumn(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
            ) {
                items(
                    items = uiData.value.cartItems?.cartItems ?: emptyList(),
                    key = { it?.name.hashCode() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            text = "- ${it?.name}",
                            style = MaterialTheme.typography.displaySmall,
                            fontSize = 20.sp
                        )
                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            text = "x ${it?.quantity} ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 20.sp
                        )
                    }
                }

                item {
                    uiData.value.deliveryDate?.let {
                        UserInfoFormComposable(
                            field = "Date de livraison",
                            value = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(
                                it
                            )
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .heightIn(min = 0.dp, max = (screenSize.height / 5) * 2)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusable(true),
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surfaceContainer),
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.secondaryContainer)
            ),
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Text(
                modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                text = "Vos informations",
                style = MaterialTheme.typography.titleLarge
            )

            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
            ) {
                Text(
                    text = "${uiData.value.buyerName},",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 20.sp
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = "${uiData.value.buyerAddress}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 20.sp
                )

                // TODO: Add a mapBoxComposable with given address (need state rework first)
            }
        }

        Text(
            modifier = Modifier.padding(16.dp),
            text = "Montant Total : ${uiData.value.totalPrice?.toStringPrice()}",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )

        // TODO: DEBUG dELIVERY PATH, CAN'T TEST BECAUSE EVERYTHING SEEMS TOO FAR ?
        PayButton(
            modifier = Modifier
                .testTag("payButton")
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            onClick = {
                checkoutViewModel.createOrder() { isSuccess ->
                    if (isSuccess) {
//                checkoutViewModel.createCheckout {
                        uiData.value.totalPrice?.let { price ->
                            val task = checkoutViewModel.getLoadPaymentDataTask(price)
                            task.addOnCompleteListener { completedTask ->
                                if (completedTask.isSuccessful) {
                                    completedTask.result.let {
                                        Log.i("Google Pay result", it.toJson())
                                        checkoutViewModel.setGooglePaySuccess(true)
                                    }
                                } else {
                                    when (val exception = completedTask.exception) {
                                        is ResolvableApiException -> {
                                            googlePayLauncher.launch(
                                                IntentSenderRequest.Builder(exception.resolution)
                                                    .build()
                                            )
                                        }

                                        is ApiException -> {
                                            Log.e(
                                                "Google Pay API error",
                                                "Error code: ${exception.statusCode}, Message: ${exception.message}"
                                            )
                                        }

                                        else -> {
                                            Log.e(
                                                "Google Pay API error",
                                                "Unexpected non API exception"
                                            )
                                        }
                                    }
                                }
                            }

                        }
                            ?: run {
                                onPricingError()
                            }
//                }
                    } else {
                        checkoutViewModel.setPaymentError("Une erreur est survenue lors de la création de la commande.\nMerci de réessayer ultérieurement et de contacter nos équipes si le problème persiste !")
                    }
                }
            },
            allowedPaymentMethods = checkoutViewModel.allowedPaymentMethods,
            enabled = uiData.value.isGooglePayAvailable
        )

        if (BuildConfig.DEBUG) {
            Button(
                onClick = {
                    checkoutViewModel.setPaymentError("This is an error message")
                }
            ) {
                Text("Debug go to error")
            }
        }
    }

    if (uiData.value.error != null && uiData.value.error != "") {
        ErrorOverlay(
            message = uiData.value.error,
            onDismiss = {
                checkoutViewModel.setPaymentError(null)
            }
        )
    }

    RiveAnimation(
        isLoading = uiData.value.isLoading,
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Loading animation"
    )

}
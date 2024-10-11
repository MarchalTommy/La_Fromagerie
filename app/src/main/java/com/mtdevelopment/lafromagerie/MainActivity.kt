package com.mtdevelopment.lafromagerie

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.contract.TaskResultContracts
import com.mtdevelopment.checkout.presentation.viewmodel.CheckoutViewModel
import com.mtdevelopment.core.presentation.theme.ui.AppTheme
import com.mtdevelopment.lafromagerie.navigation.NavGraph
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val cartViewModel: com.mtdevelopment.cart.presentation.viewmodel.CartViewModel by viewModel()
    private val checkoutViewModel: CheckoutViewModel by viewModel()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController: NavHostController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            modifier = Modifier,
                            colors = TopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.primary,
                                actionIconContentColor = MaterialTheme.colorScheme.primary,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            title = {
                                Text("La Fromagerie")
                            }
                        )
                    }
                ) { paddingValues ->
                    NavGraph(
                        cartViewModel = cartViewModel,
                        checkoutViewModel = checkoutViewModel,
                        paddingValues = paddingValues,
                        navController = navController,
                        onGooglePayButtonClick = {
                            val paymentDataLauncher =
                                registerForActivityResult(TaskResultContracts.GetPaymentDataResult()) { taskResult ->
                                    when (taskResult.status.statusCode) {
                                        CommonStatusCodes.SUCCESS -> {
                                            taskResult.result!!.let {
                                                Log.i("Google Pay result:", it.toJson())
                                                checkoutViewModel.setGooglePayResult(it)
                                            }
                                        }
                                        //CommonStatusCodes.CANCELED -> The user canceled
                                        //CommonStatusCodes.DEVELOPER_ERROR -> The API returned an error (it.status: Status)
                                        //else -> Handle internal and other unexpected errors
                                    }
                                }
                        }
                    )
                }
            }
        }
    }
}
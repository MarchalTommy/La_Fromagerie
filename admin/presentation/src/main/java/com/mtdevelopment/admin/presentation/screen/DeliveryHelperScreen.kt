package com.mtdevelopment.admin.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtdevelopment.admin.presentation.composable.DeliveryHelperItem
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import com.mtdevelopment.core.util.koinViewModel
import com.mtdevelopment.core.util.toTimeStamp
import com.mtdevelopment.core.util.vibratePhoneClick
import java.time.LocalDate

@Composable
fun DeliveryHelperScreen(

) {
    val viewModel = koinViewModel<AdminViewModel>()
    val context = LocalContext.current

    val todayDate = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).plusDays(1)

    val state = viewModel.orderScreenState.collectAsState()
    val dailyOrders = state.value.orders.filter {
        it.deliveryDate.toTimeStamp() == todayDate.toInstant().toEpochMilli()
    }

    LaunchedEffect(Unit) {
        viewModel.getAllOrders()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voici la livraison du jour !",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
            ) {
                items(items = dailyOrders) {
                    DeliveryHelperItem(
                        name = it.customerName,
                        address = it.customerAddress
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            // Here would come the static map
        }

        Button(
            modifier = Modifier,
            onClick = {
                vibratePhoneClick(context = context)
                viewModel.getOptimisedPath(
                    listOf(
                        "34 Rue de l'Étang, 25560 Frasne",
                        "1 Rue du Moulin, 25560 Courvières",
                        "2 Rue André Roz, 25560 Bannans"
                    )
                ) { latlngList ->
                    val test = "https://www.google.com/maps/dir/"

                    latlngList.forEach {
                        test.plus(it.first).plus(",").plus(it.second).plus("/")
                    }
                }
            },
            shape = Shapes().medium
        ) {
            Icon(imageVector = Icons.Default.Place, contentDescription = "Livraison")

            Text(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                text = "Démarrer la livraison"
            )
        }
    }
}
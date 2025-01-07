package com.mtdevelopment.checkout.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.composable.InfiniteCircularList
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AdminContent(
    pathList: List<AdminUiDeliveryPath>,
    onPathSelected: (AdminUiDeliveryPath) -> Unit
) {
    val adminViewModel = koinViewModel<AdminViewModel>()
    val initialItem = pathList.getOrElse(0) {
        AdminUiDeliveryPath(
            id = "new",
            name = "",
            cities = emptyList(),
            deliveryDay = ""
        )
    }

    if (pathList.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            InfiniteCircularList(
                itemHeight = 250.dp,
                numberOfDisplayedItems = 3,
                items = pathList,
                initialItem = initialItem,
                textColor = MaterialTheme.colorScheme.onBackground,
                onItemSelected = { _, item ->
                    item?.let { onPathSelected(it) }
                }
            )
        }
    }
}
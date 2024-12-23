package com.mtdevelopment.checkout.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mtdevelopment.admin.presentation.composable.InfiniteCircularList
import com.mtdevelopment.admin.presentation.composable.PathEditDialog
import com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath
import com.mtdevelopment.admin.presentation.viewmodel.AdminViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AdminContent(
    pathList: List<AdminUiDeliveryPath>,
) {

    val adminViewModel = koinViewModel<AdminViewModel>()

    val showEditDialog = remember { mutableStateOf(false) }
    val isAddPath = remember { mutableStateOf(false) }
    val selectedPath = remember { mutableStateOf<AdminUiDeliveryPath?>(null) }

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
                onItemSelected = { index, item ->
                    selectedPath.value = item
                    showEditDialog.value = true
                }
            )
        }
    }

    if (showEditDialog.value) {
        PathEditDialog(
            path = selectedPath.value,
            onValidate = {
                showEditDialog.value = false
            },
            onDelete = {
                showEditDialog.value = false
            },
            onDismiss = {
                showEditDialog.value = false
            },
            onError = {

            }
        )
    }
}
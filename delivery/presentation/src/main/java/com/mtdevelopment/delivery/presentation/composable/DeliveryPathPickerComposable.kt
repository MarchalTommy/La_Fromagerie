package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DeliveryPathPickerComposable(
    allPaths: List<com.mtdevelopment.delivery.presentation.model.UiDeliveryPath>,
    selectedPath: com.mtdevelopment.delivery.presentation.model.UiDeliveryPath?,
    onPathSelected: (com.mtdevelopment.delivery.presentation.model.UiDeliveryPath) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val radioOptions = allPaths
    val (selectedOption, onOptionSelected) = remember {
        mutableStateOf(
            selectedPath ?: radioOptions[0]
        )
    }

    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = {
            onDismiss()
        },
        content = {
            Card(
                modifier = Modifier
                    .selectableGroup()
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.CenterVertically)
            ) {
                radioOptions.forEach {
                    PathCardComposable(
                        allPaths,
                        it.name,
                        it.cities.map { it.first }.toTypedArray(),
                        selectedOption,
                        onOptionSelected
                    )
                }

                Button(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally),
                    onClick = {
                        onPathSelected(selectedOption)
                        onDismiss()
                    }
                ) {
                    Text(text = "Valider")
                }
            }
        }
    )

}

@Composable
fun PathCardComposable(
    allPaths: List<com.mtdevelopment.delivery.presentation.model.UiDeliveryPath>,
    pathName: String,
    availableCities: Array<out String>,
    selectedOption: com.mtdevelopment.delivery.presentation.model.UiDeliveryPath?,
    onOptionSelected: (com.mtdevelopment.delivery.presentation.model.UiDeliveryPath) -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .selectable(
                selected = (selectedOption?.name == pathName),
                onClick = { onOptionSelected(allPaths.find { it.name == pathName }!!) },
                role = Role.RadioButton
            )
            .padding(16.dp),
        shape = ShapeDefaults.Medium
    ) {
        Row(modifier = Modifier) {
            RadioButton(
                modifier = Modifier.padding(end = 8.dp),
                selected = (selectedOption?.name == pathName),
                onClick = null
            )

            Column {
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = pathName,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = availableCities.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = TextUnit(
                        14f,
                        TextUnitType.Sp
                    )
                )
            }
        }
    }
}
package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtdevelopment.core.model.DeliveryCity
import com.mtdevelopment.core.presentation.composable.PrimaryButton
import com.mtdevelopment.core.presentation.theme.ui.AppTheme
import com.mtdevelopment.delivery.presentation.model.plusStreet

/**
 * Editor for the street restriction of one city of one path.
 *
 * It replaces a single comma-separated text field, which was unreadable past two streets and gave
 * no clue that leaving it empty is what makes the path cover the whole commune. Here the two
 * options are stated outright, and streets are added one at a time.
 *
 * Choosing "toute la ville" clears the list, because an empty street list *is* how whole-commune
 * coverage is expressed — see [DeliveryCity.coversWholeCity]. Keeping the streets around while the
 * toggle says otherwise would let the shop save a path whose meaning differs from what it reads.
 *
 * @param onConfirm Receives the final street list; empty means the whole commune.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityStreetsBottomSheet(
    city: DeliveryCity,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var streets by remember(city) { mutableStateOf(city.streets) }
    var restrictToStreets by remember(city) { mutableStateOf(city.streets.isNotEmpty()) }
    var newStreet by remember(city) { mutableStateOf("") }

    fun commitNewStreet() {
        streets = streets.plusStreet(newStreet)
        newStreet = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = city.postcode.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !restrictToStreets,
                    onClick = { restrictToStreets = false },
                    label = { Text("Toute la ville") }
                )
                FilterChip(
                    selected = restrictToStreets,
                    onClick = { restrictToStreets = true },
                    label = { Text("Certaines rues") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (restrictToStreets) {
                    "Seules les adresses de ces rues seront livrées sur ce parcours."
                } else {
                    "Toutes les adresses de ${city.name} seront livrées sur ce parcours."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (restrictToStreets) {
                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    streets.forEach { street ->
                        InputChip(
                            selected = false,
                            onClick = { streets = streets - street },
                            label = { Text(street) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Retirer $street"
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = newStreet,
                    onValueChange = { newStreet = it },
                    label = { Text("Ajouter une rue") },
                    singleLine = true,
                    shape = ShapeDefaults.Medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitNewStreet() }),
                    trailingIcon = {
                        IconButton(
                            onClick = { commitNewStreet() },
                            enabled = newStreet.isNotBlank()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Ajouter la rue")
                        }
                    }
                )

                if (streets.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ajoutez au moins une rue, sinon la ville sera livrée en entier.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Terminé",
                trailingIcon = null,
                onClick = {
                    // Anything typed but not yet added would otherwise be silently lost.
                    val finalStreets = if (restrictToStreets) {
                        streets.plusStreet(newStreet)
                    } else {
                        emptyList()
                    }
                    onConfirm(finalStreets)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
private fun CityStreetsBottomSheetRestrictedPreview() {
    AppTheme {
        CityStreetsBottomSheet(
            city = DeliveryCity("Boujailles", 25560, listOf("Rue du Moulin", "Grande Rue")),
            onConfirm = {},
            onDismiss = {}
        )
    }
}

package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.mtdevelopment.core.model.AutoCompleteSuggestion

@Composable
fun CityPostalCodeAutocompleteTextField(
    searchQuery: String,
    suggestions: List<AutoCompleteSuggestion>,
    showDropdown: Boolean,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    onFocusChange: (Boolean) -> Unit,
    onDropDownDismiss: () -> Unit,
    onSuggestionSelected: (AutoCompleteSuggestion) -> Unit,
    onValueChange: (String) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(showDropdown) {
        isDropdownExpanded = showDropdown
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                onValueChange.invoke(it)
            },
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChange.invoke(it.isFocused) },
            label = { Text(text = "Entrez une ville ou un code postal") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Clear,
                        contentDescription = "Effacer",
                        modifier = Modifier.clickable {
                            onValueChange.invoke("")
                        }
                    )
                }
            },
            shape = ShapeDefaults.Medium,
            colors = OutlinedTextFieldDefaults.colors(
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            maxLines = 1,
            leadingIcon = {
                Icon(
                    Icons.Rounded.Place,
                    contentDescription = "Ville/Code Postal"
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            )
        )

        // Dropdown pour les suggestions
        DropdownMenu(
            expanded = isDropdownExpanded && suggestions.isNotEmpty(),
            onDismissRequest = {
                isDropdownExpanded = false
                onDropDownDismiss.invoke()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            // Important pour que le dropdown s'aligne avec le TextField
            properties = PopupProperties(focusable = false)
        ) {
            suggestions.forEach { suggestion ->
                // Combine city and postal code for display, handling potential nulls
                val displayText = buildString {
                    append(suggestion.city ?: "Ville inconnue")
                    suggestion.postCode?.let { append(" (${it})") }
                }.trim()

                if (displayText.isNotEmpty()) { // Only show item if there's text to display
                    DropdownMenuItem(
                        text = { Text(displayText) }, // Display city and postal code
                        onClick = {
                            onSuggestionSelected.invoke(suggestion)
                            isDropdownExpanded = false
                            focusManager.clearFocus()
                            // Optionally update the text field with the selected city/postal code
                            // onValueChange(displayText)
                        }
                    )
                }
            }
        }
    }
}
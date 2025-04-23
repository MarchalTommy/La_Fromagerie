package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.CircularProgressIndicator
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
import com.mtdevelopment.delivery.domain.model.AutoCompleteSuggestion

@Composable
fun AddressAutocompleteTextField(
    searchQuery: String,
    suggestions: List<AutoCompleteSuggestion>,
    isLoading: Boolean,
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


    Box(modifier = Modifier.fillMaxWidth()) {
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
            label = { Text(text = "Entrez une adresse") },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Rounded.Clear, "", modifier = Modifier.clickable {
                        onValueChange.invoke("")
                    })
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
                Icon(Icons.Rounded.Place, "")
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
            modifier = Modifier.fillMaxWidth(),
            // Important pour que le dropdown s'aligne avec le TextField
            properties = PopupProperties(focusable = false)
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion.fulltext ?: "") },
                    onClick = {
                        onSuggestionSelected.invoke(suggestion)
                        isDropdownExpanded = false
                        focusManager.clearFocus()
                    }
                )
            }
        }
    }
}
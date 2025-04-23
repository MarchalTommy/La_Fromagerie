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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.mtdevelopment.delivery.domain.model.AutoCompleteSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onAddressValidated: (address: String, suggestion: AutoCompleteSuggestion?) -> Unit,
    onValueChange: (String) -> Unit
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current // Needed for hiding keyboard

    LaunchedEffect(showDropdown, suggestions) {
        isDropdownExpanded = showDropdown && suggestions.isNotEmpty()
    }

    // Helper function to validate the current input text
    val validateCurrentInput = {
        if (searchQuery.isNotBlank()) {
            onAddressValidated(searchQuery, null) // Use current text
            isDropdownExpanded = false
            keyboardController?.hide() // Hide keyboard
            focusManager.clearFocus()
        } else {
            // Optionally clear focus even if input is blank on IME Done/Focus Lost
            isDropdownExpanded = false
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                onValueChange(it)
            },
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        // Delay validation slightly on focus loss
                        // to allow DropdownMenuItem click to process first
                        coroutineScope.launch {
                            delay(350)
                            // Check if dropdown wasn't just closed by a click
                            if (isDropdownExpanded) {
                                validateCurrentInput()
                            }
                        }
                    } else {
                        onFocusChange.invoke(focusState.isFocused)
                    }
                },
            label = { Text(text = "Entrez une adresse") },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Clear,
                        contentDescription = "Clear text",
                        modifier = Modifier.clickable {
                            onValueChange("")
                            focusRequester.requestFocus()
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
                Icon(Icons.Rounded.Place, contentDescription = "Address icon")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search // Use Done for final input
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    validateCurrentInput() // Validate on IME action
                }
            )
        )

        // Dropdown pour les suggestions
        DropdownMenu(
            expanded = isDropdownExpanded, // Use internal state driven by LaunchedEffect
            onDismissRequest = {
                isDropdownExpanded = false
                onDropDownDismiss() // Notify caller
            },
            modifier = Modifier.fillMaxWidth(),
            properties = PopupProperties(focusable = false) // Prevents dropdown stealing focus
        ) {
            suggestions.forEach { suggestion ->
                val suggestionText = suggestion.fulltext ?: ""
                DropdownMenuItem(
                    text = { Text(suggestionText) },
                    onClick = {
                        onValueChange(suggestionText) // Update the text field state via caller
                        onAddressValidated(
                            suggestionText,
                            suggestion
                        ) // Notify validation with selected text
                        isDropdownExpanded = false
                        keyboardController?.hide() // Hide keyboard
                        focusManager.clearFocus() // Clear focus
                    }
                )
            }
        }
    }
}
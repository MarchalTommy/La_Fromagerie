package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun UserInfoComposable(
    fieldText: String,
    label: String,
    updateText: (String) -> Unit,
    leadingIcon: @Composable () -> Unit,
    imeAction: ImeAction,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
) {
    OutlinedTextField(
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp)
            .fillMaxWidth()
            .focusRequester(focusRequester),
        value = fieldText,
        onValueChange = { text ->
            updateText.invoke(text)
        },
        label = { Text(label) },
        shape = ShapeDefaults.Medium,
        colors = OutlinedTextFieldDefaults.colors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledSupportingTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        maxLines = 1,
        singleLine = true,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            },
            onDone = {
                focusManager.clearFocus()
            }
        )
    )
}
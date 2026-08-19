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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * One line of the customer form.
 *
 * @param isError Marks the field as rejected. Paired with [supportingText] rather than shown on
 *   its own: a red border that does not say what is wrong leaves the customer guessing.
 * @param supportingText Reason shown under the field. Null keeps the field's height unchanged,
 *   so a form only grows once something is actually wrong with it.
 */
@Composable
fun UserInfoComposable(
    fieldText: String,
    label: String,
    updateText: (String) -> Unit,
    leadingIcon: @Composable () -> Unit,
    imeAction: ImeAction,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null,
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
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
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
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
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
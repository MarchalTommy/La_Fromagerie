package com.mtdevelopment.admin.presentation.model

import com.mtdevelopment.core.model.AutoCompleteSuggestion
import com.mtdevelopment.core.model.Order

data class DeliverHelperDialogState(
    val autocompleteSearchQuery: String,
    val suggestions: List<AutoCompleteSuggestion?>,
    val autocompleteShowDropdown: Boolean,

    val shouldShowDialog: Boolean,

    val onDismissError: () -> Unit,
    val dialogOnConfirm: (Order) -> Unit,
    val autocompleteOnValueChange: (String) -> Unit,
    val autocompleteOnSuggestionSelected: (AutoCompleteSuggestion) -> Unit,
    val dialogOnDismiss: () -> Unit,
    val dialogOnShow: () -> Unit,
    val autocompleteOnDropDownDismiss: () -> Unit,
)
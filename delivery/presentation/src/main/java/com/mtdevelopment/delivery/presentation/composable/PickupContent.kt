package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.presentation.composable.PrimaryButton
import com.mtdevelopment.delivery.domain.usecase.SelectablePickupDate
import com.mtdevelopment.delivery.presentation.state.DeliveryUiDataState
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

/**
 * Picks how the customer wants their order.
 *
 * Always visible, including in delivery mode, so collecting is discoverable rather than
 * something only offered once an address has failed.
 */
@Composable
fun FulfillmentTypeSelector(
    selected: FulfillmentType,
    onSelect: (FulfillmentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember {
        listOf(
            FulfillmentType.DELIVERY to "Livraison",
            FulfillmentType.PICKUP_SHOP to "Boutique",
            FulfillmentType.PICKUP_MARKET to "Marché"
        )
    }

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

/**
 * The collected-order form: who is coming, how to reach them, and when.
 *
 * No address field, deliberately. It serves no purpose here — there is nothing to drive to —
 * and the phone number takes its place, because a customer who does not turn up is the one
 * failure mode this mode has and a number is the only way to resolve it.
 *
 * The chosen date is hoisted into [state]: it is what the order is built from, and holding it
 * here let it outlive both a change of mode and a rotation.
 */
@Composable
fun PickupContent(
    state: DeliveryUiDataState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDateChosen: (SelectablePickupDate) -> Unit,
    onDateSelected: (SelectablePickupDate) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val chosen = state.selectedPickupDate

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {

            UserInfoComposable(
                fieldText = state.userNameFieldText,
                label = "Nom et prénom",
                imeAction = ImeAction.Next,
                focusRequester = focusRequester,
                focusManager = focusManager,
                updateText = onNameChange,
                leadingIcon = { Icon(Icons.Rounded.Person, "") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            UserInfoComposable(
                fieldText = state.userPhoneFieldText,
                label = "Téléphone",
                imeAction = ImeAction.Done,
                focusRequester = focusRequester,
                focusManager = focusManager,
                updateText = onPhoneChange,
                leadingIcon = { Icon(Icons.Rounded.Phone, "") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                // Never rendered as "there is nowhere to collect": the read failed, and
                // telling the customer the shop does not do this would cost a sale.
                state.pickupPointsUnavailable -> PickupMessage(
                    "Impossible de récupérer les points de retrait pour le moment.\n" +
                            "Vérifiez votre connexion et réessayez."
                )

                state.pickupDates.isEmpty() && !state.isLoading -> PickupMessage(
                    when (state.fulfillmentType) {
                        FulfillmentType.PICKUP_MARKET ->
                            "Aucune date de marché n'est programmée pour l'instant."

                        else -> "Aucune date de retrait n'est disponible pour l'instant."
                    }
                )

                else -> {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = "Quand venez-vous ?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    state.pickupDates.forEach { pickupDate ->
                        PickupDateCard(
                            pickupDate = pickupDate,
                            isSelected = chosen == pickupDate,
                            onClick = { onDateChosen(pickupDate) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = "Continuer",
                enabled = chosen != null &&
                        state.userNameFieldText.isNotBlank() &&
                        state.userPhoneFieldText.isNotBlank(),
                onClick = { chosen?.let(onDateSelected) }
            )
        }
    }
}

@Composable
private fun PickupMessage(message: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PickupDateCard(
    pickupDate: SelectablePickupDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            // Past the cut-off the tile stays listed but inert, exactly as the delivery
            // picker does: removing it would make the list shift and read as a bug.
            .clickable(enabled = !pickupDate.isPastDeadline, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = pickupDate.date.format(DATE_LABEL_FORMAT)
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                if (pickupDate.timeRange.isNotBlank()) {
                    Text(
                        text = pickupDate.timeRange,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                text = pickupDate.pointLabel,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = pickupDate.pointAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (pickupDate.isPastDeadline) {
                Text(
                    text = "Commandes closes pour cette date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

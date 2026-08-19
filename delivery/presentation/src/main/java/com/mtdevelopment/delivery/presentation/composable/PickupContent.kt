package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Storefront
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mtdevelopment.core.domain.isValidFrenchPhoneNumber
import com.mtdevelopment.core.domain.toStringPrice
import com.mtdevelopment.core.model.FulfillmentType
import com.mtdevelopment.core.presentation.composable.PrimaryButton
import com.mtdevelopment.delivery.domain.usecase.SelectablePickupDate
import com.mtdevelopment.delivery.presentation.state.DeliveryUiDataState
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LABEL_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

private const val PHONE_ERROR = "Numéro invalide : 10 chiffres attendus, par exemple 06 12 34 56 78."

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
 * Tells the customer, in money, what collecting at the shop is worth on the basket they are
 * holding right now.
 *
 * Sits directly under [FulfillmentTypeSelector] because that is the control it is arguing
 * about: the saving exists in the catalogue and in the total, and nowhere the customer looks
 * while deciding. Silent when [savingInCents] is zero — no basket, or nothing priced
 * differently — since "économisez 0,00 €" is worse than saying nothing.
 *
 * The wording changes once the customer has taken the offer: it stops being an argument and
 * becomes a confirmation of what they already chose.
 */
@Composable
fun ShopPickupSavingNotice(
    savingInCents: Long,
    fulfillmentType: FulfillmentType,
    modifier: Modifier = Modifier
) {
    if (savingInCents <= 0L) return

    val amount = savingInCents.toStringPrice()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Storefront, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (fulfillmentType == FulfillmentType.PICKUP_SHOP) {
                    "Retrait en boutique : vous économisez $amount sur cette commande."
                } else {
                    "En venant chercher votre commande à la boutique, vous payez " +
                            "$amount de moins."
                },
                style = MaterialTheme.typography.bodyMedium
            )
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
 *
 * The rule for "this order can go to checkout" lives here and nowhere else. The ViewModel used
 * to carry a second copy that nothing called and that had already drifted from this one.
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

    // Only complain once there is something to complain about: a form that is red before the
    // customer has typed anything reads as broken rather than as helpful.
    val phoneError = remember(state.userPhoneFieldText) {
        val phone = state.userPhoneFieldText
        PHONE_ERROR.takeIf { phone.isNotBlank() && !phone.isValidFrenchPhoneNumber() }
    }
    val canContinue = chosen != null &&
            state.userNameFieldText.isNotBlank() &&
            state.userPhoneFieldText.isValidFrenchPhoneNumber()

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
                keyboardType = KeyboardType.Phone,
                focusRequester = focusRequester,
                focusManager = focusManager,
                updateText = onPhoneChange,
                leadingIcon = { Icon(Icons.Rounded.Phone, "") },
                isError = phoneError != null,
                supportingText = phoneError
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                // Never rendered as "there is nowhere to collect": the read failed, and
                // telling the customer the shop does not do this would cost a sale.
                state.pickupPointsUnavailable -> PickupMessage(
                    message = "Impossible de récupérer les points de retrait pour le moment.\n" +
                            "Vérifiez votre connexion et réessayez.",
                    isFailure = true
                )

                // Nothing has gone wrong here: the shop simply has no date open yet. Saying
                // so in the error colour would read as a fault of the app, and send a
                // customer chasing a problem that is not theirs.
                state.pickupDates.isEmpty() && !state.isLoading -> PickupMessage(
                    message = when (state.fulfillmentType) {
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
                enabled = canContinue,
                onClick = { chosen?.let(onDateSelected) }
            )
        }
    }
}

/**
 * @param isFailure True when something went wrong and the customer may be able to fix it.
 *   False when the message merely states how things are — no market scheduled yet is a fact
 *   about the shop's calendar, not a fault, and colouring it as one would misdirect.
 */
@Composable
private fun PickupMessage(message: String, isFailure: Boolean = false) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isFailure) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
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
            // Every card here is orderable: BuildSelectablePickupDatesUseCase starts the list
            // at the next date still inside its window, so there is no closed state to render.
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        // Formatting a date allocates; the cards redraw on every keystroke in the form above.
        val dateLabel = remember(pickupDate.date) {
            pickupDate.date.format(DATE_LABEL_FORMAT).replaceFirstChar { it.uppercase() }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateLabel,
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
            // Full body weight and full contrast: this is the address the customer has to
            // find on the day, not a caption. It was set in the muted secondary style, which
            // read as decoration next to the label above it.
            Text(
                text = pickupDate.pointAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

package com.mtdevelopment.delivery.presentation.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mtdevelopment.delivery.domain.usecase.BuildSelectableDeliveryDatesUseCase
import com.mtdevelopment.delivery.presentation.model.toDomainDeliveryPath
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Delivery-date calendar.
 *
 * Takes every path that serves the customer rather than a single one: a commune covered by two
 * tournées produces one merged, chronological list, and the date the customer picks is what assigns
 * them a path. The tournée name is shown on the tiles only when there is actually something to
 * choose between — labelling a single-path list would just be noise.
 *
 * @param paths Paths serving the address. One entry reproduces the previous behaviour exactly.
 * @param onDateSelected Receives the chosen date and the path it belongs to; the caller must apply
 *   that path before persisting the order.
 */
@Composable
fun DatePickerComposable(
    paths: List<com.mtdevelopment.delivery.presentation.model.UiDeliveryPath>,
    shouldRemoveDatePicker: () -> Unit,
    newDateFieldText: (String) -> Unit,
    onDateSelected: (Long, com.mtdevelopment.delivery.presentation.model.UiDeliveryPath) -> Unit = { _, _ -> }
) {
    val now = remember { LocalDateTime.now(ZoneId.systemDefault()) }

    val availableDates = remember(paths, now) {
        BuildSelectableDeliveryDatesUseCase().invoke(
            paths = paths.map { it.toDomainDeliveryPath() },
            now = now
        )
    }
    val showPathLabels = paths.size > 1

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Dialog(
        onDismissRequest = { shouldRemoveDatePicker() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Date de livraison",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Body: list of tiles
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (availableDates.isEmpty()) {
                        Text(
                            text = "Aucune date de livraison disponible.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        availableDates.forEachIndexed { index, option ->
                            val date = option.date
                            val isPastDeadline = option.isPastDeadline

                            val isSelected = selectedDate == date
                            val dayName =
                                date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRANCE)
                                    .replaceFirstChar { it.uppercase() }
                            val dateText = "${date.dayOfMonth} ${
                                date.month.getDisplayName(
                                    TextStyle.FULL,
                                    Locale.FRANCE
                                ).replaceFirstChar { it.uppercase() }
                            }"

                            val dayNameLower =
                                date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRANCE)
                                    .lowercase()
                            val subtitle = if (isPastDeadline) {
                                "Commandes clôturées"
                            } else {
                                when (index) {
                                    0 -> "Livraison ce $dayNameLower"
                                    1 -> "Livraison $dayNameLower prochain"
                                    else -> {
                                        val daysBetween =
                                            ChronoUnit.DAYS.between(LocalDate.now(), date)
                                        "Livraison dans $daysBetween jours"
                                    }
                                }
                            }

                            // Card tile
                            val tileBg = if (isPastDeadline) {
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }

                            val tileBorderColor = if (isPastDeadline) {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }

                            val tileBorderWidth =
                                if (isSelected && !isPastDeadline) 1.5.dp else 1.dp

                            val textColor = if (isPastDeadline) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }

                            val rowModifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(tileBg)
                                .border(
                                    width = tileBorderWidth,
                                    color = tileBorderColor,
                                    shape = RoundedCornerShape(16.dp)
                                )

                            val clickableModifier = if (isPastDeadline) {
                                rowModifier
                            } else {
                                rowModifier.clickable { selectedDate = date }
                            }

                            Row(
                                modifier = clickableModifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isPastDeadline) {
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        } else if (isSelected) {
                                            textColor.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                // Only meaningful when several tournées are on offer — otherwise
                                // every tile would carry the same name.
                                if (showPathLabels) {
                                    Text(
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (isPastDeadline) {
                                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                                } else {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        text = option.pathName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isPastDeadline) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    )
                                }
                                if (isSelected && !isPastDeadline) {
                                    Icon(
                                        modifier = Modifier.padding(start = 8.dp),
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Actions Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { shouldRemoveDatePicker() }
                    ) {
                        Text(
                            text = "Annuler",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val chosen = selectedDate?.let { date ->
                                availableDates.firstOrNull { it.date == date }
                            }
                            val chosenPath = chosen?.let { option ->
                                paths.firstOrNull { it.id == option.pathId }
                            }
                            if (chosen != null && chosenPath != null) {
                                val formattedDate =
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy").format(chosen.date)
                                newDateFieldText(formattedDate)
                                val epochMillis =
                                    chosen.date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                        .toEpochMilli()
                                onDateSelected(epochMillis, chosenPath)
                            }
                            shouldRemoveDatePicker()
                        },
                        enabled = selectedDate != null,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Valider",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
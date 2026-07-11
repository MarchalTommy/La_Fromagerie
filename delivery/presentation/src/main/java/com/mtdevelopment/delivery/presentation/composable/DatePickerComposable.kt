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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun DatePickerComposable(
    selectedPath: com.mtdevelopment.delivery.presentation.model.UiDeliveryPath?,
    shouldRemoveDatePicker: () -> Unit,
    newDateFieldText: (String) -> Unit,
    onDateSelected: (Long) -> Unit = {}
) {
    // Generate the next 4 available delivery dates
    val availableDates = remember(selectedPath) {
        if (selectedPath != null) {
            val targetDay = try {
                DayOfWeek.valueOf(selectedPath.deliveryDay.uppercase())
            } catch (e: Exception) {
                DayOfWeek.FRIDAY
            }
            val dates = mutableListOf<LocalDate>()
            var current = LocalDate.now()
            while (dates.size < 4) {
                if (current.dayOfWeek == targetDay) {
                    dates.add(current)
                }
                current = current.plusDays(1)
            }
            dates
        } else {
            emptyList()
        }
    }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val now = remember { LocalDateTime.now(ZoneId.systemDefault()) }

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
                        availableDates.forEachIndexed { index, date ->
                            // Rule: orders allowed up to the day before at 12:00 PM
                            val limitDateTime = date.minusDays(1).atTime(12, 0)
                            val isPastDeadline = now.isAfter(limitDateTime)

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
                                if (isSelected && !isPastDeadline) {
                                    Icon(
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
                            selectedDate?.let { date ->
                                val formattedDate =
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date)
                                newDateFieldText(formattedDate)
                                val epochMillis =
                                    date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                        .toEpochMilli()
                                onDateSelected(epochMillis)
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
package com.mtdevelopment.admin.presentation.composable

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

private enum class PermissionStep {
    INITIAL_PERMISSIONS,
    EXPLAIN_BACKGROUND_LOCATION,
    REQUEST_BACKGROUND_LOCATION, // This step might directly lead to settings
    CHECK_BATTERY_OPTIMIZATION,
    COMPLETED
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissionsScreen(
    onPermissionsGrantedAndConfigured: () -> Unit, // All set, including potential battery optimization
    onPermissionsProcessSkippedOrDenied: () -> Unit // User chose to skip or critical permissions denied
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(PermissionStep.INITIAL_PERMISSIONS) }

    // Initial Permissions (Fine Location, Notifications)
    val initialPermissionsList = remember {
        listOfNotNull(
            Manifest.permission.ACCESS_FINE_LOCATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
            // FOREGROUND_SERVICE and FOREGROUND_SERVICE_LOCATION are manifest declared
            // and their runtime effect for location is tied to ACCESS_FINE_LOCATION being granted
            // and the service being started with the correct foregroundServiceType.
        ).distinct()
    }
    val initialPermissionsState =
        rememberMultiplePermissionsState(permissions = initialPermissionsList)

    // Background Location Permission (Android Q+)
    val backgroundLocationPermissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    } else null

    val backgroundLocationPermissionState = backgroundLocationPermissionString?.let {
        rememberPermissionState(permission = it)
    }

    // --- LaunchedEffects to manage state transitions ---

    LaunchedEffect(initialPermissionsState.allPermissionsGranted) {
        if (initialPermissionsState.allPermissionsGranted) {
            if (currentStep == PermissionStep.INITIAL_PERMISSIONS) { // Ensure this transition happens once
                currentStep =
                    if (backgroundLocationPermissionString != null && backgroundLocationPermissionState?.status?.isGranted == false) {
                        PermissionStep.EXPLAIN_BACKGROUND_LOCATION
                    } else {
                        PermissionStep.CHECK_BATTERY_OPTIMIZATION
                    }
            }
        }
    }

    LaunchedEffect(backgroundLocationPermissionState?.status) {
        if (backgroundLocationPermissionState?.status?.isGranted == true) {
            if (currentStep == PermissionStep.REQUEST_BACKGROUND_LOCATION || currentStep == PermissionStep.EXPLAIN_BACKGROUND_LOCATION) {
                currentStep = PermissionStep.CHECK_BATTERY_OPTIMIZATION
            }
        }
    }

    // --- UI based on currentStep ---

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (currentStep) {
            PermissionStep.INITIAL_PERMISSIONS -> {
                val showRationale =
                    initialPermissionsState.permissions.any { it.status is PermissionStatus.Denied && it.status.shouldShowRationale }
                val textToShow = if (showRationale) {
                    "Pour le suivi de livraison, l'accès à la localisation précise et aux notifications est requis. Veuillez accorder ces autorisations."
                } else {
                    "L'application a besoin des autorisations de localisation et de notification."
                }
                Text(
                    textToShow,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { initialPermissionsState.launchMultiplePermissionRequest() }) {
                    Text("Accorder les autorisations")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onPermissionsProcessSkippedOrDenied) {
                    Text("Continuer sans suivi")
                }
            }

            PermissionStep.EXPLAIN_BACKGROUND_LOCATION -> {
                Text(
                    "Pour un suivi continu même lorsque l'application est en arrière-plan, veuillez autoriser la localisation \"Toujours\" ou \"Seulement si l'appli est en cours d'utilisation\" dans l'écran suivant.",
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    currentStep = PermissionStep.REQUEST_BACKGROUND_LOCATION
                    // Small delay to ensure state change before launching, if needed, though usually direct is fine.
                    backgroundLocationPermissionState?.launchPermissionRequest()
                }) {
                    Text("Compris, ouvrir les paramètres")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { currentStep = PermissionStep.CHECK_BATTERY_OPTIMIZATION }) {
                    Text("Continuer sans localisation en arrière-plan")
                }
            }

            PermissionStep.REQUEST_BACKGROUND_LOCATION -> {
                // This state is mostly transient as launchPermissionRequest often navigates away.
                // If the user comes back without granting:
                if (backgroundLocationPermissionState?.status?.isGranted == false) {
                    Text(
                        "La localisation en arrière-plan n'a pas été accordée. Vous pouvez l'activer manuellement dans les paramètres de l'application pour un meilleur suivi.",
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { openAppSettings(context) }) {
                        Text("Ouvrir les paramètres de l'application")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        currentStep = PermissionStep.CHECK_BATTERY_OPTIMIZATION
                    }) {
                        Text("Continuer quand même")
                    }
                } else if (backgroundLocationPermissionState?.status?.isGranted == true) {
                    // Should be caught by LaunchedEffect, but as a safeguard
                    LaunchedEffect(Unit) { currentStep = PermissionStep.CHECK_BATTERY_OPTIMIZATION }
                } else {
                    Text(
                        "Redirection vers les paramètres de localisation en arrière-plan...",
                        textAlign = TextAlign.Center
                    )
                }
            }

            PermissionStep.CHECK_BATTERY_OPTIMIZATION -> {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoringOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                } else {
                    true // Not applicable before Marshmallow
                }

                if (!isIgnoringOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Text(
                        "Pour assurer la fiabilité du suivi de livraison, il est recommandé de désactiver l'optimisation de la batterie pour cette application.",
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        requestIgnoreBatteryOptimizations(context)
                        // After this, we assume the user handled it or we proceed anyway.
                        currentStep = PermissionStep.COMPLETED
                        onPermissionsGrantedAndConfigured()
                    }) {
                        Text("Ouvrir les paramètres d'optimisation")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        currentStep = PermissionStep.COMPLETED
                        onPermissionsGrantedAndConfigured() // Proceed even if they don't optimize
                    }) {
                        Text("Ne pas optimiser pour l'instant")
                    }
                } else {
                    // Already ignoring or not applicable
                    LaunchedEffect(Unit) {
                        currentStep = PermissionStep.COMPLETED
                        onPermissionsGrantedAndConfigured()
                    }
                }
            }

            PermissionStep.COMPLETED -> {
                // This state means the flow is done. The parent composable should typically
                // stop showing RequestPermissionsScreen once onPermissionsGrantedAndConfigured or
                // onPermissionsProcessSkippedOrDenied is called.
                // If shown, it means the parent didn't react.
                Text("Configuration des permissions terminée.", textAlign = TextAlign.Center)
                LaunchedEffect(Unit) {
                    // Ensure callback is triggered if somehow stuck here
                    if (initialPermissionsState.allPermissionsGranted &&
                        (backgroundLocationPermissionString == null || backgroundLocationPermissionState?.status?.isGranted == true)
                    ) {
                        onPermissionsGrantedAndConfigured()
                    } else {
                        onPermissionsProcessSkippedOrDenied()
                    }
                }
            }
        }
    }

    // Initial request for non-background permissions when the composable first appears
    // and currentStep is INITIAL_PERMISSIONS
    LaunchedEffect(currentStep, initialPermissionsState.allPermissionsGranted) {
        if (currentStep == PermissionStep.INITIAL_PERMISSIONS && !initialPermissionsState.allPermissionsGranted) {
            // Check if we should show rationale before the first launch,
            // though Accompanist handles this well.
            // This ensures the dialog appears on first display if permissions are missing.
            initialPermissionsState.launchMultiplePermissionRequest()
        }
    }
}

// Helper to open app-specific settings
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("AppSettings", "Could not open app settings", e)
    }
}

// Helper to request ignoring battery optimizations
@SuppressLint("BatteryLife")
private fun requestIgnoreBatteryOptimizations(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("BatteryOpt", "Could not open battery optimization settings", e)
            // As a fallback, open general app settings if specific intent fails
            openAppSettings(context)
        }
    }
}
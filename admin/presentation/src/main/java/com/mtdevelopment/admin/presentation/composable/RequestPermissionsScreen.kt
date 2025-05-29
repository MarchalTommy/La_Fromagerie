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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

private enum class PermissionStep {
    INITIAL_PERMISSIONS,
    EXPLAIN_BACKGROUND_LOCATION,
    REQUEST_BACKGROUND_LOCATION,
    CHECK_BATTERY_OPTIMIZATION,
    COMPLETED
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissionsScreen(
    shouldShowOptimization: Boolean,
    onPermissionsGrantedAndConfigured: () -> Unit,
    onPermissionsProcessSkippedOrDenied: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(PermissionStep.INITIAL_PERMISSIONS) }

    val initialPermissionsList = remember {
        listOfNotNull(
            Manifest.permission.ACCESS_FINE_LOCATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        ).distinct()
    }
    val initialPermissionsState =
        rememberMultiplePermissionsState(permissions = initialPermissionsList)

    val backgroundLocationPermissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    } else null

    val backgroundLocationPermissionState = backgroundLocationPermissionString?.let {
        rememberPermissionState(permission = it)
    }

    // State to trigger re-check of battery optimization on resume
    var triggerBatteryRecheck by remember { mutableIntStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Use DisposableEffect to manage lifecycle observer
    DisposableEffect(lifecycleOwner, currentStep) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("RequestPermissionsScreen", "ON_RESUME triggered. Current step: $currentStep")
                if (currentStep == PermissionStep.CHECK_BATTERY_OPTIMIZATION) {
                    Log.d(
                        "RequestPermissionsScreen",
                        "ON_RESUME: Triggering battery optimization re-check."
                    )
                    triggerBatteryRecheck++ // Increment to trigger recomposition for the remember block
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(
                "RequestPermissionsScreen",
                "ON_DISPOSE: Removing lifecycle observer for step $currentStep"
            )
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(initialPermissionsState.allPermissionsGranted) {
        if (initialPermissionsState.allPermissionsGranted) {
            if (currentStep == PermissionStep.INITIAL_PERMISSIONS) {
                currentStep =
                    if (backgroundLocationPermissionString != null && backgroundLocationPermissionState?.status?.isGranted == false) {
                        PermissionStep.EXPLAIN_BACKGROUND_LOCATION
                    } else {
                        PermissionStep.CHECK_BATTERY_OPTIMIZATION
                    }
                Log.d(
                    "RequestPermissionsScreen",
                    "Initial permissions granted. New step: $currentStep"
                )
            }
        }
    }

    LaunchedEffect(backgroundLocationPermissionState?.status) {
        val bgStatus = backgroundLocationPermissionState?.status
        if (bgStatus?.isGranted == true) {
            if (currentStep == PermissionStep.REQUEST_BACKGROUND_LOCATION || currentStep == PermissionStep.EXPLAIN_BACKGROUND_LOCATION) {
                currentStep = PermissionStep.CHECK_BATTERY_OPTIMIZATION
                Log.d(
                    "RequestPermissionsScreen",
                    "Background location granted. New step: $currentStep"
                )
            }
        }
    }

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
                    "Pour un suivi continu même lorsque l'application est en arrière-plan, veuillez autoriser la localisation \"Toujours\" dans l'écran suivant.",
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    currentStep = PermissionStep.REQUEST_BACKGROUND_LOCATION
                    backgroundLocationPermissionState?.launchPermissionRequest()
                }) {
                    Text("Compris, ouvrir les paramètres")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    currentStep = PermissionStep.CHECK_BATTERY_OPTIMIZATION
                    Log.d(
                        "RequestPermissionsScreen",
                        "Skipping background location. New step: $currentStep"
                    )
                }) {
                    Text("Continuer sans localisation en arrière-plan")
                }
            }

            PermissionStep.REQUEST_BACKGROUND_LOCATION -> {
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
                        Log.d(
                            "RequestPermissionsScreen",
                            "Continuing without background location after failed attempt. New step: $currentStep"
                        )
                    }) {
                        Text("Continuer quand même")
                    }
                } else if (backgroundLocationPermissionState?.status?.isGranted == true) {
                    // This case should ideally be caught by the LaunchedEffect on backgroundLocationPermissionState.status
                    // If somehow reached, ensure transition.
                    LaunchedEffect(Unit) {
                        if (currentStep != PermissionStep.CHECK_BATTERY_OPTIMIZATION && currentStep != PermissionStep.COMPLETED) {
                            currentStep = PermissionStep.CHECK_BATTERY_OPTIMIZATION
                            Log.d(
                                "RequestPermissionsScreen",
                                "Background location detected as granted in REQUEST_BACKGROUND_LOCATION step (safeguard). New step: $currentStep"
                            )
                        }
                    }
                } else {
                    Text(
                        "Redirection vers les paramètres de localisation en arrière-plan...",
                        textAlign = TextAlign.Center
                    )
                }
            }

            PermissionStep.CHECK_BATTERY_OPTIMIZATION -> {
                if (shouldShowOptimization) {
                    val isIgnoringOptimizations = remember(triggerBatteryRecheck) {
                        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Log.d(
                                "RequestPermissionsScreen",
                                "Checking battery optimization status. Trigger count: $triggerBatteryRecheck. Package: ${context.packageName}"
                            )
                            val status = pm.isIgnoringBatteryOptimizations(context.packageName)
                            Log.d(
                                "RequestPermissionsScreen",
                                "pm.isIgnoringBatteryOptimizations returned: $status for package ${context.packageName}"
                            )
                            status
                        } else {
                            true // Not applicable before Marshmallow
                        }
                    }

                    // Log the evaluated status outside the remember block to see it on each recomposition of this step
                    LaunchedEffect(isIgnoringOptimizations) {
                        Log.d(
                            "RequestPermissionsScreen",
                            "CHECK_BATTERY_OPTIMIZATION (recomposition): isIgnoringOptimizations = $isIgnoringOptimizations"
                        )
                    }

                    if (!isIgnoringOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Text(
                            "Pour assurer la fiabilité du suivi de livraison, il est recommandé de désactiver l'optimisation de la batterie pour cette application.",
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            requestIgnoreBatteryOptimizations(context)
                            Log.d(
                                "RequestPermissionsScreen",
                                "Requested to ignore battery optimizations. Waiting for resume to re-check."
                            )
                        }) {
                            Text("Ouvrir les paramètres d'optimisation")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            Log.d(
                                "RequestPermissionsScreen",
                                "User chose not to optimize battery. Proceeding to COMPLETED."
                            )
                            currentStep =
                                PermissionStep.COMPLETED // This will trigger COMPLETED step logic
                        }) {
                            Text("Ne pas optimiser pour l'instant")
                        }
                    } else { // Battery optimization is already ignored or not applicable
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Text(
                                "Vérification de l'optimisation de la batterie : OK.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        // Transition to COMPLETED. The callback will be handled there.
                        // Key with currentStep to ensure this runs if we enter this 'else' branch and are not yet COMPLETED.
                        LaunchedEffect(currentStep) {
                            if (currentStep != PermissionStep.COMPLETED) {
                                Log.d(
                                    "RequestPermissionsScreen",
                                    "Battery optimization OK or not needed. Setting step to COMPLETED."
                                )
                                currentStep = PermissionStep.COMPLETED
                            }
                        }
                    }
                } else {
                    currentStep = PermissionStep.COMPLETED
                    Log.d(
                        "RequestPermissionsScreen",
                        "Battery optimization not needed. Setting step to COMPLETED."
                    )
                }
            }

            PermissionStep.COMPLETED -> {
                Text("Configuration des permissions terminée.", textAlign = TextAlign.Center)
                // This LaunchedEffect will run once when currentStep becomes COMPLETED,
                // or if permission states change while in COMPLETED state.
                LaunchedEffect(
                    initialPermissionsState.allPermissionsGranted,
                    backgroundLocationPermissionState?.status // This key can be null
                ) {
                    Log.d(
                        "RequestPermissionsScreen",
                        "In COMPLETED step. Evaluating final callback. Initial granted: ${initialPermissionsState.allPermissionsGranted}, BG status: ${backgroundLocationPermissionState?.status}"
                    )

                    val bgPermissionGrantedOrNotApplicable =
                        backgroundLocationPermissionString == null || backgroundLocationPermissionState?.status?.isGranted == true

                    if (initialPermissionsState.allPermissionsGranted && bgPermissionGrantedOrNotApplicable) {
                        Log.d(
                            "RequestPermissionsScreen",
                            "COMPLETED: Calling onPermissionsGrantedAndConfigured."
                        )
                        onPermissionsGrantedAndConfigured()
                    } else {
                        Log.d(
                            "RequestPermissionsScreen",
                            "COMPLETED: Calling onPermissionsProcessSkippedOrDenied due to permission state. Initial: ${initialPermissionsState.allPermissionsGranted}, BG relevant: ${backgroundLocationPermissionString != null}, BG granted: ${backgroundLocationPermissionState?.status?.isGranted}"
                        )
                        onPermissionsProcessSkippedOrDenied()
                    }
                }
            }
        }
    }

    // Initial request for non-background permissions
    LaunchedEffect(
        initialPermissionsState.allPermissionsGranted,
        currentStep
    ) { // Added currentStep as key
        if (currentStep == PermissionStep.INITIAL_PERMISSIONS && !initialPermissionsState.allPermissionsGranted &&
            !initialPermissionsState.permissions.any { it.status.shouldShowRationale } // More precise rationale check
        ) {
            Log.d(
                "RequestPermissionsScreen",
                "Initial launch of permission request for initial permissions."
            )
            initialPermissionsState.launchMultiplePermissionRequest()
        }
    }
}

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
            openAppSettings(context) // Fallback
        }
    }
}

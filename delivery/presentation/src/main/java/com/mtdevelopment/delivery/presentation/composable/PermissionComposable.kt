package com.mtdevelopment.delivery.presentation.composable

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    // Initialize the state for managing multiple location permissions.
    val permissions = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    val permissionState = rememberMultiplePermissionsState(
        permissions
    ) { permissionsMap ->
        val arePermissionsGranted = permissionsMap.values.reduce { acc, next ->
            acc && next
        }

        if (arePermissionsGranted) {
            onPermissionGranted.invoke()
        } else {
            onPermissionDenied.invoke()
        }
    }

    // Use LaunchedEffect to handle permissions logic when the composition is launched.
    LaunchedEffect(key1 = permissionState) {
        val permissionsToRequest = permissionState.permissions.filter {
            !it.status.isGranted
        }

        if (permissionState.allPermissionsGranted) {
            onPermissionGranted.invoke()
            return@LaunchedEffect
        }

        if (permissionsToRequest.isNotEmpty()) permissionState.launchMultiplePermissionRequest()

    }
}
package com.mtdevelopment.core.repository

import android.net.ConnectivityManager
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {

    val connectivityManager: ConnectivityManager

    val isConnected: Flow<Boolean>

}
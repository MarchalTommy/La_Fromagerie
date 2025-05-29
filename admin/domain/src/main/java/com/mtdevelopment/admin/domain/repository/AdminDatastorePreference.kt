package com.mtdevelopment.admin.domain.repository

import com.mtdevelopment.admin.domain.model.OptimizedRouteWithOrders
import kotlinx.coroutines.flow.Flow

interface AdminDatastorePreference {

    val dailyDeliveryPathGeocodedFlow: Flow<OptimizedRouteWithOrders?>

    suspend fun setDailyDeliveryPathGeocoded(delivery: OptimizedRouteWithOrders)

    val shouldShowBatterieOptimizationFlow: Flow<Boolean>

    suspend fun updateShouldShowBatterieOptimization(shouldShow: Boolean)

    suspend fun resetDailyDelivery()
}
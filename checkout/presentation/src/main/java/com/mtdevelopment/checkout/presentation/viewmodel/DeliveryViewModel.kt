package com.mtdevelopment.checkout.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.mtdevelopment.cart.presentation.model.UiBasketObject
import com.mtdevelopment.checkout.presentation.model.DeliveryPath
import com.mtdevelopment.checkout.presentation.model.UserInfo
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent

class DeliveryViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
) : ViewModel(), KoinComponent {

    private val _deliveryUiState: MutableStateFlow<DeliveryUiState> =
        MutableStateFlow(DeliveryUiState.Starting)
    val deliveryUiState: StateFlow<DeliveryUiState> = _deliveryUiState.asStateFlow()


    private val _deliveryDataState: MutableStateFlow<DeliveryDataState> =
        MutableStateFlow(DeliveryDataState())
    val deliveryDataState: StateFlow<DeliveryDataState> = _deliveryDataState.asStateFlow()

    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT THIS
    ///////////////////////////////////////////////////////////////////////////
    private val _cartObjects =
        MutableStateFlow(UiBasketObject("1", flowOf(emptyList()), flowOf("0,00€")))
    val cartObjects: StateFlow<UiBasketObject> = _cartObjects.asStateFlow()
    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT THIS
    ///////////////////////////////////////////////////////////////////////////

    fun manageScreenState(
        userLocation: Pair<Double, Double>? = null,
        path: DeliveryPath? = null,
        deliveryDate: Long? = null,
        userName: String? = null,
        userAddress: String? = null,
        localisationSuccess: Boolean? = null,
        localisationError: LOCALISATION_ERROR? = null,
        isLoading: Boolean = false,
        shouldShowDateSelection: Boolean = false,
        shouldShowPathSelection: Boolean = false
    ) {
        when {
            userLocation != null -> {
                updateUserLocation(userLocation)
            }

            path != null -> {
                updatePathSelected(path)
            }

            deliveryDate != null -> {
                updateDeliveryDate(deliveryDate)
            }

            userName != null || userAddress != null -> {
                updateUserInfo(userAddress = userAddress, userName = userName)
            }

            localisationSuccess != null -> {
                _deliveryUiState.value = DeliveryUiState.LocationSuccess
            }

            localisationError != null -> {
                setLocalisationInError(localisationError)
            }

            isLoading -> {
                _deliveryUiState.value = DeliveryUiState.Loading
            }

            shouldShowDateSelection -> {
                _deliveryUiState.value = DeliveryUiState.DateSelection
            }

            shouldShowPathSelection -> {
                _deliveryUiState.value = DeliveryUiState.PathSelection
            }

            else -> {
                _deliveryUiState.value = DeliveryUiState.Idle
            }
        }
    }

    private fun updateUserLocation(userLocation: Pair<Double, Double>) {
        _deliveryUiState.update { previousState ->
            if (previousState is DeliveryUiState.DeliveryDataState) {
                previousState.copy(userLocation = userLocation)
            } else {
                DeliveryUiState.DeliveryDataState(
                    userLocation = userLocation,
                    path = null,
                    deliveryDate = 0L,
                    userInfo = UserInfo("", "")
                )
            }
        }
    }

    private fun updatePathSelected(path: DeliveryPath) {
        _deliveryUiState.update { previousState ->
            if (previousState is DeliveryUiState.DeliveryDataState) {
                previousState.copy(path = path)
            } else {
                DeliveryUiState.DeliveryDataState(
                    userLocation = null,
                    path = path,
                    deliveryDate = 0L,
                    userInfo = UserInfo("", "")
                )
            }
        }
    }

    private fun updateDeliveryDate(deliveryDate: Long) {
        _deliveryUiState.update { previousState ->
            if (previousState is DeliveryUiState.DeliveryDataState) {
                previousState.copy(deliveryDate = deliveryDate)
            } else {
                DeliveryUiState.DeliveryDataState(
                    userLocation = null,
                    path = null,
                    deliveryDate = deliveryDate,
                    userInfo = UserInfo("", "")
                )
            }
        }
    }

    private fun updateUserInfo(
        userAddress: String? = null,
        userName: String? = null
    ) {
        _deliveryUiState.update { previousState ->
            if (previousState is DeliveryUiState.DeliveryDataState) {
                previousState.copy(
                    userInfo = UserInfo(
                        userName ?: previousState.userInfo.userName,
                        userAddress ?: previousState.userInfo.userAddress
                    )
                )
            } else {
                DeliveryUiState.DeliveryDataState(
                    userLocation = null,
                    path = null,
                    deliveryDate = 0L,
                    userInfo = UserInfo(userName ?: "", userAddress ?: "")
                )
            }
        }
    }

    private fun setLocalisationInError(localisationError: LOCALISATION_ERROR) {
        when (localisationError) {
            LOCALISATION_ERROR.NOT_ON_PATH -> {
                _deliveryUiState.value =
                    DeliveryUiState.LocalisationNotOnPath
            }

            LOCALISATION_ERROR.CANNOT_GET_LOCATION -> {
                _deliveryUiState.value =
                    DeliveryUiState.LocalisationInError
            }

            LOCALISATION_ERROR.PERMISSION_REFUSED -> {
                _deliveryUiState.value =
                    DeliveryUiState.LocalisationPermissionRefused
            }

            LOCALISATION_ERROR.UNKNOWN -> {
                _deliveryUiState.value =
                    DeliveryUiState.LocalisationInError
            }

            LOCALISATION_ERROR.NONE -> {
                // todo IDK what to do here to be honest
            }
        }
    }
}

abstract class DeliveryUiState internal constructor() {
    object Starting : DeliveryUiState()

    object Loading : DeliveryUiState()

    object Ready : DeliveryUiState()

    object Idle : DeliveryUiState()

    object DateSelection : DeliveryUiState()

    object PathSelection : DeliveryUiState()

    object LocationSuccess : DeliveryUiState()

    object LocalisationPermissionRefused : DeliveryUiState()

    object LocalisationNotOnPath : DeliveryUiState()

    object LocalisationInError : DeliveryUiState()

    data class DeliveryDataState(
        val userLocation: Pair<Double, Double>?,
        val path: DeliveryPath?,
        val deliveryDate: Long,
        val userInfo: UserInfo
    ) : DeliveryUiState()

    class Error(
        val error: LOCALISATION_ERROR = LOCALISATION_ERROR.UNKNOWN,
        val message: String? = null
    ) : DeliveryUiState()
}

data class DeliveryDataState(
    val userLocation: Pair<Double, Double>? = null,
    val path: DeliveryPath? = null,
    val deliveryDate: Long? = null,
    val userInfo: UserInfo? = null
) : DeliveryUiState()


enum class LOCALISATION_ERROR {
    NOT_ON_PATH, CANNOT_GET_LOCATION, PERMISSION_REFUSED, UNKNOWN, NONE
}
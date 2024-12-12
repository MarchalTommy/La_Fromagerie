package com.mtdevelopment.checkout.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtdevelopment.checkout.domain.usecase.GetAllDeliveryPathsUseCase
import com.mtdevelopment.checkout.domain.usecase.GetDeliveryPathUseCase
import com.mtdevelopment.checkout.domain.usecase.GetUserInfoFromDatastoreUseCase
import com.mtdevelopment.checkout.presentation.model.UiDeliveryPath
import com.mtdevelopment.checkout.presentation.model.toUiDeliveryPath
import com.mtdevelopment.checkout.presentation.state.DeliveryUiDataState
import com.mtdevelopment.core.model.UserInformation
import com.mtdevelopment.core.usecase.GetIsNetworkConnectedUseCase
import com.mtdevelopment.core.usecase.SaveToDatastoreUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class DeliveryViewModel(
    getIsConnectedUseCase: GetIsNetworkConnectedUseCase,
    getUserInfoFromDatastoreUseCase: GetUserInfoFromDatastoreUseCase,
    private val saveToDatastoreUseCase: SaveToDatastoreUseCase,
    private val getDeliveryPathsUseCase: GetDeliveryPathUseCase,
    private val getAllDeliveryPathsUseCase: GetAllDeliveryPathsUseCase
) : ViewModel(), KoinComponent {

    val isConnected: StateFlow<Boolean> = getIsConnectedUseCase.invoke().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    var deliveryUiDataState by mutableStateOf(DeliveryUiDataState())
        private set

    init {
        viewModelScope.launch {
            deliveryUiDataState = deliveryUiDataState.copy(isLoading = true)
            getAllDeliveryPaths()
            val userInfo = getUserInfoFromDatastoreUseCase.invoke().firstOrNull()
            deliveryUiDataState = deliveryUiDataState.copy(
                userAddressFieldText = userInfo?.address ?: "",
                userNameFieldText = userInfo?.name ?: "",
                selectedPath = deliveryUiDataState.deliveryPaths.firstOrNull { it.name == userInfo?.lastSelectedPath }
            )

        }
    }

    // TODO: Manage error state
    fun saveUserInfo(onError: () -> Unit = {}) {
        viewModelScope.launch {
            if (deliveryUiDataState.selectedPath == null || deliveryUiDataState.userAddressFieldText.isBlank()) {
                onError.invoke()
                return@launch
            }
            saveToDatastoreUseCase.invoke(
                userInformation = UserInformation(
                    name = deliveryUiDataState.userNameFieldText,
                    address = deliveryUiDataState.userAddressFieldText,
                    lastSelectedPath = deliveryUiDataState.selectedPath?.name ?: ""
                )
            )
        }
    }

    fun saveSelectedDate(date: Long) {
        viewModelScope.launch {
            saveToDatastoreUseCase.invoke(deliveryDate = date)
        }
    }

    private suspend fun getAllDeliveryPaths() {
        getAllDeliveryPathsUseCase.invoke(scope = viewModelScope,
            onSuccess = { pathsList ->
                deliveryUiDataState = deliveryUiDataState.copy(
                    deliveryPaths = pathsList.mapNotNull { path ->
                        path?.toUiDeliveryPath()
                    },
                    isLoading = false
                )
            },
            onFailure = {
                deliveryUiDataState = deliveryUiDataState.copy(isLoading = false)
                // TODO: Manage error state
            })
    }


    ///////////////////////////////////////////////////////////////////////////
    // DELIVERY STATE
    ///////////////////////////////////////////////////////////////////////////
    fun setIsDatePickerClickable(isClickable: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(shouldDatePickerBeClickable = isClickable)
    }

    fun setIsDatePickerShown(isShown: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(datePickerVisibility = isShown)
    }

    fun setDateFieldText(text: String) {
        deliveryUiDataState = deliveryUiDataState.copy(dateFieldText = text)
    }

    fun setUserNameFieldText(name: String) {
        deliveryUiDataState = deliveryUiDataState.copy(userNameFieldText = name)
    }

    fun setUserAddressFieldText(address: String) {
        deliveryUiDataState = deliveryUiDataState.copy(userAddressFieldText = address)
    }

    fun updateShowDeliveryPathPicker(shouldShow: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(showDeliveryPathPicker = shouldShow)
    }

    fun setIsLoading(isLoading: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(isLoading = isLoading)
    }

    fun setColumnScrollingEnabled(isEnabled: Boolean) {
        deliveryUiDataState = deliveryUiDataState.copy(columnScrollingEnabled = isEnabled)
    }

    fun updateUserCityLocation(location: Pair<Double, Double>) {
        deliveryUiDataState = deliveryUiDataState.copy(userCityLocation = location)
    }

    fun updateUserCity(city: String) {
        deliveryUiDataState = deliveryUiDataState.copy(userCity = city)
    }

    fun updateSelectedPath(path: UiDeliveryPath) {
        deliveryUiDataState = deliveryUiDataState.copy(selectedPath = path)
    }

    ///////////////////////////////////////////////////////////////////////////
    // PERMISSION MANAGER STATE
    ///////////////////////////////////////////////////////////////////////////
    fun updateShouldShowLocalisationPermission(isGranted: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(shouldShowLocalisationPermission = isGranted)
    }

    fun updateLocalisationState(isAcquired: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(localisationSuccess = isAcquired)
    }

    fun updateUserLocationOnPath(isOnPath: Boolean) {
        deliveryUiDataState =
            deliveryUiDataState.copy(userLocationOnPath = isOnPath)
    }
}